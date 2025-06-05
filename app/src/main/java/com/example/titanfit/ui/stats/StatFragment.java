package com.example.titanfit.ui.stats;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter; // Asegúrate de que esta importación exista
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.titanfit.databinding.FragmentStatBinding;
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.SharedPreferencesManager;

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

    public static StatFragment newInstance() {
        return new StatFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Cargar los datos de la semana actual al iniciar el fragmento ---
        loadCurrentWeekMeals();

        binding.calendar.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);

            // Usa el método auxiliar para obtener las fechas de la semana seleccionada
            String[] weekDates = getWeekStartAndEndDate(selectedDate);
            String startDate = weekDates[0];
            String endDate = weekDates[1];

            SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(requireContext());
            User user = sharedPreferencesManager.getUser();

            fetchMealsForWeek(startDate, endDate, user.getId());

            Toast.makeText(requireContext(), "Semana:\nInicio: " + startDate + "\nFin: " + endDate, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Nuevo método para cargar los datos de la semana actual ---
    private void loadCurrentWeekMeals() {
        Calendar currentCalendar = Calendar.getInstance(); // Obtiene la fecha y hora actuales

        // Calcula el inicio y fin de la semana actual
        String[] weekDates = getWeekStartAndEndDate(currentCalendar);
        String startDate = weekDates[0];
        String endDate = weekDates[1];

        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(requireContext());
        User user = sharedPreferencesManager.getUser();

        fetchMealsForWeek(startDate, endDate, user.getId());

        Toast.makeText(requireContext(), "Cargando semana actual:\nInicio: " + startDate + "\nFin: " + endDate, Toast.LENGTH_LONG).show();
    }

    // --- Método auxiliar para calcular el lunes y domingo de cualquier semana ---
    private String[] getWeekStartAndEndDate(Calendar date) {
        Calendar startOfWeek = (Calendar) date.clone();
        Calendar endOfWeek = (Calendar) date.clone();

        // Aseguramos que el lunes sea el primer día de la semana para los cálculos
        startOfWeek.setFirstDayOfWeek(Calendar.MONDAY);
        endOfWeek.setFirstDayOfWeek(Calendar.MONDAY);

        // Ir al lunes de la semana actual/seleccionada
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        // Si el día original era un domingo y al setear a Monday el calendario retrocede (comportamiento de algunos locales),
        // avanzamos una semana para asegurarnos de que sea el lunes de la semana correcta.
        if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && startOfWeek.after(date)) {
            startOfWeek.add(Calendar.WEEK_OF_YEAR, -1);
        }
        // Este ajuste final asegura que siempre estemos en el lunes de la semana correcta
        while (startOfWeek.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            startOfWeek.add(Calendar.DATE, -1);
        }

        // Ir al domingo de la semana actual/seleccionada
        endOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        // Similar al lunes, si al setear a domingo el calendario retrocede (ej. de lunes a domingo de la semana anterior),
        // avanzamos una semana para que sea el domingo de la semana correcta.
        if (date.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY && endOfWeek.before(date)) {
            endOfWeek.add(Calendar.WEEK_OF_YEAR, 1);
        }
        // Este ajuste final asegura que siempre estemos en el domingo de la semana correcta
        while (endOfWeek.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            endOfWeek.add(Calendar.DATE, 1);
        }


        // Formatear las fechas en dd-MM-yyyy
        String startDate = String.format(Locale.getDefault(), "%02d-%02d-%d",
                startOfWeek.get(Calendar.DAY_OF_MONTH),
                startOfWeek.get(Calendar.MONTH) + 1, // Calendar.MONTH es base 0
                startOfWeek.get(Calendar.YEAR));

        String endDate = String.format(Locale.getDefault(), "%02d-%02d-%d",
                endOfWeek.get(Calendar.DAY_OF_MONTH),
                endOfWeek.get(Calendar.MONTH) + 1, // Calendar.MONTH es base 0
                endOfWeek.get(Calendar.YEAR));

        return new String[]{startDate, endDate};
    }

    private void fetchMealsForWeek(String startDate, String endDate, String userId) {
        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);

        Call<List<Meal>> call = apiService.getMealsWeek(startDate, endDate, userId);

        call.enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(Call<List<Meal>> call, Response<List<Meal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> mealsWeek = response.body();
                    Log.d("MealsWeek", "Datos recibidos: " + mealsWeek.size());

                    int totalCalories = 0;
                    int totalProtein = 0;
                    int totalCarbs = 0;
                    int totalFat = 0;

                    // Mapa para contar alimentos
                    Map<String, Integer> foodCounts = new HashMap<>();

                    for (Meal meal : mealsWeek) {
                        totalCalories += meal.getCalories();
                        totalProtein += meal.getProtein();
                        totalCarbs += meal.getCarbs();
                        totalFat += meal.getFats();

                        // Contar alimentos más consumidos, normalizando el nombre a minúsculas
                        if (meal.getName() != null && !meal.getName().isEmpty()) {
                            String foodName = meal.getName().toLowerCase(Locale.getDefault());
                            int count = foodCounts.containsKey(foodName) ? foodCounts.get(foodName) : 0;
                            foodCounts.put(foodName, count + 1);
                        }
                    }

                    // Actualizar UI con totales
                    binding.totalCalories.setText(String.valueOf(totalCalories));
                    binding.totalProtein.setText(totalProtein + "g");
                    binding.totalCarbs.setText(totalCarbs + "g");
                    // Assuming you have a totalFat TextView in your layout, update it as well
                    // binding.totalFat.setText(totalFat + "g");

                    // Calcular porcentajes
                    int totalMacros = totalProtein + totalCarbs + totalFat;
                    int proteinPercent = totalMacros > 0 ? (totalProtein * 100 / totalMacros) : 0;
                    int carbsPercent = totalMacros > 0 ? (totalCarbs * 100 / totalMacros) : 0;
                    int fatPercent = totalMacros > 0 ? (totalFat * 100 / totalMacros) : 0;

                    binding.proteinProgress.setProgress(proteinPercent);
                    binding.carbsProgress.setProgress(carbsPercent);
                    binding.fatProgress.setProgress(fatPercent);

                    // Generar lista ordenada de alimentos más consumidos
                    List<Map.Entry<String, Integer>> sortedFoods = new ArrayList<>(foodCounts.entrySet());
                    Collections.sort(sortedFoods, (a, b) -> b.getValue() - a.getValue());

                    // Preparar lista para el ListView (Top 3 alimentos)
                    List<String> topFoodsList = new ArrayList<>();
                    int topLimit = 3;
                    int count = 0;
                    for (Map.Entry<String, Integer> entry : sortedFoods) {
                        if (count >= topLimit) break;
                        topFoodsList.add(entry.getKey() + " - " + entry.getValue() + " veces");
                        count++;
                    }

                    // Adapter para el ListView
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_list_item_1, topFoodsList);
                    binding.topFoodsList.setAdapter(adapter);

                } else {
                    Log.e("MealsWeek", "Respuesta no exitosa: " + response.code());
                    Toast.makeText(requireContext(), "Error al cargar las comidas: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Meal>> call, Throwable t) {
                Log.e("MealsWeek", "Error en la llamada: " + t.getMessage());
                Toast.makeText(requireContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}