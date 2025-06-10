package com.example.titanfit.ui.stats;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentStatBinding;
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.SharedPreferencesManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatFragment extends Fragment {

    private FragmentStatBinding binding;
    private String currentStartDate;
    private String currentEndDate;

    // UI Components from XML (as per your original XML)
    private CalendarView calendarView;
    private TextView totalCalories;
    private TextView totalProtein;
    private TextView totalCarbs;
    private ProgressBar proteinProgress;
    private ProgressBar carbsProgress;
    private ProgressBar fatProgress;
    private ListView topFoodsList;
    private TextView statisticsTitle;

    public static StatFragment newInstance() {
        return new StatFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Initialize all UI components
        initViews(view);

        return view;
    }

    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendar);
        totalCalories = view.findViewById(R.id.total_calories);
        totalProtein = view.findViewById(R.id.total_protein);
        totalCarbs = view.findViewById(R.id.total_carbs);
        proteinProgress = view.findViewById(R.id.protein_progress);
        carbsProgress = view.findViewById(R.id.carbs_progress);
        fatProgress = view.findViewById(R.id.fat_progress);
        topFoodsList = view.findViewById(R.id.top_foods_list);
        statisticsTitle = view.findViewById(R.id.statistics_title);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load meals for the current week when the fragment is first displayed
        loadCurrentWeekMeals();

        calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);

            String[] weekDates = getWeekStartAndEndDate(selectedDate);
            String newStartDate = weekDates[0];
            String newEndDate = weekDates[1];

            Log.d("StatFragment", "Date selected: " + dayOfMonth + "-" + (month + 1) + "-" + year);
            Log.d("StatFragment", "Calculated week: " + newStartDate + " to " + newEndDate);
            Log.d("StatFragment", "Current displayed week: " + currentStartDate + " to " + currentEndDate);


            // Crucial Change: Only fetch if the week has actually changed.
            // This condition is the most critical for preventing redundant calls
            // while ensuring updates when the week truly differs.
            if (!newStartDate.equals(currentStartDate) || !newEndDate.equals(currentEndDate)) {
                Log.d("StatFragment", "Week change detected. Fetching new data.");
                currentStartDate = newStartDate;
                currentEndDate = newEndDate;

                SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(requireContext());
                User user = sharedPreferencesManager.getUser();
                if (user != null && user.getId() != null) {
                    fetchMealsForWeek(currentStartDate, currentEndDate, user.getId());
                    Toast.makeText(requireContext(),
                            "Semana del " + currentStartDate + " al " + currentEndDate,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("StatFragment", "User ID not found in SharedPreferences.");
                    Toast.makeText(requireContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
                    updateUIWithMealData(new ArrayList<>()); // Clear UI
                }
            } else {
                Log.d("StatFragment", "No week change. Data will not be re-fetched.");
                // If you *still* want to force a refresh on *any* date selection within the same week,
                // you would remove the 'if' condition completely. But for performance, this is better.
                // If the issue persists, the problem is likely *not* in this condition.
            }
        });
    }

    private void loadCurrentWeekMeals() {
        Calendar currentCalendar = Calendar.getInstance();
        String[] weekDates = getWeekStartAndEndDate(currentCalendar);
        currentStartDate = weekDates[0];
        currentEndDate = weekDates[1];

        Log.d("StatFragment", "Initial load: " + currentStartDate + " to " + currentEndDate);

        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(requireContext());
        User user = sharedPreferencesManager.getUser();

        if (user != null && user.getId() != null) {
            fetchMealsForWeek(currentStartDate, currentEndDate, user.getId());
        } else {
            Log.e("StatFragment", "User ID not found on initial load.");
            Toast.makeText(requireContext(), "Error: Usuario no autenticado al iniciar.", Toast.LENGTH_SHORT).show();
            updateUIWithMealData(new ArrayList<>()); // Clear UI
        }
    }

    private String[] getWeekStartAndEndDate(Calendar date) {
        try {
            LocalDate selectedDate = LocalDate.of(
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-indexed
                    date.get(Calendar.DAY_OF_MONTH)
            );

            // Get Monday of the week
            LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            // Get Sunday of the week
            LocalDate sunday = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            return new String[]{
                    monday.format(formatter),
                    sunday.format(formatter)
            };
        } catch (Exception e) {
            Log.e("DateCalculation", "Error calculating week dates: " + e.getMessage(), e);
            // Fallback to current week if an error occurs
            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            return new String[]{
                    monday.format(formatter),
                    sunday.format(formatter)
            };
        }
    }

    private void fetchMealsForWeek(String startDate, String endDate, String userId) {
        Log.d("id",userId);
        showLoading(true); // Show loading indicator (if implemented)
        Log.d("StatFragment", "Fetching meals for userId: " + userId + " from " + startDate + " to " + endDate);


        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
        Call<List<Meal>> call = apiService.getMealsWeek(startDate, endDate, userId);

        call.enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(Call<List<Meal>> call, Response<List<Meal>> response) {
                showLoading(false); // Hide loading indicator

                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> mealsWeek = response.body();
                    Log.d("API_SUCCESS", "Received " + mealsWeek.size() + " meals for week: " + startDate + " to " + endDate);
                    updateUIWithMealData(mealsWeek);
                } else {
                    // Log the full error body if available for more detailed debugging
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("API_ERROR", "Error parsing error body: " + e.getMessage());
                    }
                    Log.e("API_ERROR", "Response not successful. Code: " + response.code() + ", Message: " + response.message() + ", Error Body: " + errorBody);
                    Toast.makeText(requireContext(),
                            "Error al cargar datos: " + response.code() + " - " + response.message(),
                            Toast.LENGTH_LONG).show();
                    updateUIWithMealData(new ArrayList<>()); // Clear UI on error
                }
            }

            @Override
            public void onFailure(Call<List<Meal>> call, Throwable t) {
                showLoading(false); // Hide loading indicator
                Log.e("API_FAILURE", "Network or API call failed: " + t.getMessage(), t);
                Toast.makeText(requireContext(),
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                updateUIWithMealData(new ArrayList<>()); // Clear UI on failure
            }
        });
    }

    private void showLoading(boolean isLoading) {
        // Placeholder for loading indicator.
        // If you have a ProgressBar with ID 'progressBarLoading' in your XML,
        // you would uncomment and use this:
        // ProgressBar loadingProgressBar = requireView().findViewById(R.id.progressBarLoading);
        // if (loadingProgressBar != null) {
        //     loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // }
    }

    private void updateUIWithMealData(List<Meal> mealsWeek) {
        requireActivity().runOnUiThread(() -> {
            try {
                int totalCaloriesValue = 0;
                int totalProteinValue = 0;
                int totalCarbsValue = 0;
                int totalFatValue = 0;
                Map<String, Integer> foodCounts = new HashMap<>();

                // Calculate totals
                for (Meal meal : mealsWeek) {
                    totalCaloriesValue += meal.getCalories();
                    totalProteinValue += meal.getProtein();
                    totalCarbsValue += meal.getCarbs();
                    totalFatValue += meal.getFats();

                    if (meal.getName() != null) {
                        String foodName = meal.getName().toLowerCase(Locale.getDefault());
                        foodCounts.put(foodName, foodCounts.getOrDefault(foodName, 0) + 1);
                    }
                }

                // Log calculated totals before updating UI
                Log.d("StatFragment", "Calculated Totals - Calories: " + totalCaloriesValue +
                        ", Protein: " + totalProteinValue + "g, Carbs: " + totalCarbsValue + "g, Fat: " + totalFatValue + "g");

                // Update nutrient totals for available TextViews
                totalCalories.setText(String.valueOf(totalCaloriesValue));
                totalProtein.setText(totalProteinValue + "g");
                totalCarbs.setText(totalCarbsValue + "g");

                // Calculate and update macro percentages
                int totalMacros = totalProteinValue + totalCarbsValue + totalFatValue;
                if (totalMacros > 0) {
                    proteinProgress.setProgress(totalProteinValue * 100 / totalMacros);
                    carbsProgress.setProgress(totalCarbsValue * 100 / totalMacros);
                    fatProgress.setProgress(totalFatValue * 100 / totalMacros);
                    Log.d("StatFragment", "Macro Progress - Protein: " + (totalProteinValue * 100 / totalMacros) +
                            "%, Carbs: " + (carbsProgress.getProgress()) + "%, Fat: " + (fatProgress.getProgress()) + "%");
                } else {
                    proteinProgress.setProgress(0);
                    carbsProgress.setProgress(0);
                    fatProgress.setProgress(0);
                    Log.d("StatFragment", "Total macros is 0. Progress bars reset.");
                }

                // Update top foods list
                List<Map.Entry<String, Integer>> sortedFoods = new ArrayList<>(foodCounts.entrySet());
                Collections.sort(sortedFoods, (a, b) -> b.getValue().compareTo(a.getValue()));

                List<String> topFoodsListItems = new ArrayList<>();
                int limit = Math.min(3, sortedFoods.size()); // Show top 3 foods
                for (int i = 0; i < limit; i++) {
                    Map.Entry<String, Integer> entry = sortedFoods.get(i);
                    topFoodsListItems.add(capitalize(entry.getKey()) + " (" + entry.getValue() + " veces)");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        topFoodsListItems
                );
                topFoodsList.setAdapter(adapter);
                Log.d("StatFragment", "Top foods list updated with " + topFoodsListItems.size() + " items.");

            } catch (Exception e) {
                Log.e("UI_UPDATE", "Error updating UI: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error al actualizar la UI de estadísticas.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}