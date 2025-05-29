package com.example.titanfit.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentMainBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.models.UserGoal;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceFood;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.example.titanfit.ui.dialogs.DialogAddComida;
import com.example.titanfit.ui.dialogs.DialogComida;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainFragment extends Fragment implements DialogComida.OnMealAddedListener {
    private static final String TAG = "MainFragment";
    private MainViewModel mViewModel;
    private FragmentMainBinding binding;
    private SharedPreferencesManager manager;
    private Context context;
    private User usuario;
    private String fecha;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        binding = FragmentMainBinding.inflate(inflater, container, false);
        context = requireContext();
        manager = new SharedPreferencesManager(context);

        // Initialize date with current date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        fecha = sdf.format(calendar.getTime());

        // Initialize UI with user data
        UserGoal goals = initializeUserData();
        if (goals == null) {
            Toast.makeText(context, "Error loading user data", Toast.LENGTH_SHORT).show();
            return binding.getRoot();
        }

        // Fetch initial meals
        fetchMealsForDate(fecha, goals);

        // Set up calendar listener
        binding.calendar.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            fecha = String.format(Locale.getDefault(), "%02d-%02d-%d", dayOfMonth, month + 1, year);
            fetchMealsForDate(fecha, goals);
            Toast.makeText(context, fecha, Toast.LENGTH_SHORT).show();
        });

        // Set up button listeners
        setupButtonListeners(goals);

        return binding.getRoot();
    }

    private UserGoal initializeUserData() {
        usuario = getArguments() != null ? (User) getArguments().getSerializable("user") : null;
        if (usuario == null && manager.isLoggedIn()) {
            usuario = manager.getUser();
        }

        if (usuario != null && usuario.getGoals() != null) {
            UserGoal goals = usuario.getGoals();
            updateNutritionUI(0, 0, 0, 0, goals);
            return goals;
        }
        Log.e(TAG, "User or goals are null");
        return null;
    }

    private void setupButtonListeners(UserGoal goals) {
        binding.addbreakfast.setOnClickListener(v -> openDialogAddComida("Desayuno"));
        binding.addlunch.setOnClickListener(v -> openDialogAddComida("Comida"));
    }

    private void openDialogAddComida(String tipo) {
        dismissAllDialogFragments(getParentFragmentManager());
        Bundle bundle = new Bundle();
        bundle.putString("tipo", tipo);
        bundle.putSerializable("user", usuario);
        bundle.putString("fecha",fecha);
        DialogAddComida dialog = new DialogAddComida(new ArrayList<>(), getParentFragmentManager(), this);
        dialog.setArguments(bundle);
        dialog.show(getParentFragmentManager(), "DialogAddComida");
    }

    private void fetchMealsForDate(String fecha, UserGoal goals) {
        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
        Call<List<Meal>> meals = apiService.getMeals(fecha, usuario.getId());
        meals.enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(Call<List<Meal>> call, Response<List<Meal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> mealList = response.body();
                    binding.llBreakfastItems.removeAllViews();
                    binding.llLunchItems.removeAllViews();

                    int calories = 0;
                    double proteins = 0, carbs = 0, fats = 0;

                    for (Meal meal : mealList) {
                        LinearLayout targetLayout = meal.getTipo().equalsIgnoreCase("Desayuno")
                                ? binding.llBreakfastItems : binding.llLunchItems;
                        addMealView(targetLayout, meal, false); // Pass false to avoid saving
                        calories += meal.getCalories();
                        proteins += meal.getProtein();
                        carbs += meal.getCarbs();
                        fats += meal.getFats();
                    }

                    updateNutritionUI(calories, proteins, carbs, fats, goals);
                } else {
                    Log.e(TAG, "Request failed. Code: " + response.code());
                    Toast.makeText(context, "Failed to load meals", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Meal>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNutritionUI(int calories, double proteins, double carbs, double fats, UserGoal goals) {
        binding.cpiCalories.setMax(goals.getDailyCalories());
        binding.cpiCalories.setProgress(calories);
        binding.tvCaloriesLabel.setText(String.format(Locale.getDefault(), "%d/%d kcal", calories, goals.getDailyCalories()));
        binding.proteinbar.setProgress((int) proteins);
        binding.carbbar.setProgress((int) carbs);
        binding.fatbar.setProgress((int) fats);
        binding.proteinas.setText(String.format(Locale.getDefault(), "Proteinas: %.1f/%d g", proteins, Math.round(goals.getProteinPercentage())));
        binding.carbohidratos.setText(String.format(Locale.getDefault(), "Carbohidratos: %.1f/%d g", carbs, Math.round(goals.getCarbsPercentage())));
        binding.grasas.setText(String.format(Locale.getDefault(), "Grasas: %.1f/%d g", fats, Math.round(goals.getFatsPercentage())));
    }

    @Override
    public void onMealAdded(Meal meal, String tipo) {
        // Update UI
        int currentCalories = binding.cpiCalories.getProgress() + meal.getCalories();
        double currentProteins = parseMacro(binding.proteinas.getText().toString()) + meal.getProtein();
        double currentCarbs = parseMacro(binding.carbohidratos.getText().toString()) + meal.getCarbs();
        double currentFats = parseMacro(binding.grasas.getText().toString()) + meal.getFats();
        LinearLayout targetLayout = meal.getTipo().equalsIgnoreCase("Desayuno")
                ? binding.llBreakfastItems : binding.llLunchItems;
        addMealView(targetLayout, meal,true,currentCalories,currentProteins,currentCarbs,currentFats);

        Log.d("meal",meal.toString());



        binding.cpiCalories.setProgress(currentCalories);
        binding.tvCaloriesLabel.setText(String.format(Locale.getDefault(), "%d/%d kcal", currentCalories, binding.cpiCalories.getMax()));
        binding.proteinbar.setProgress((int) currentProteins);
        binding.carbbar.setProgress((int) currentCarbs);
        binding.fatbar.setProgress((int) currentFats);
        binding.proteinas.setText(String.format(Locale.getDefault(), "Proteinas: %.1f/%s g", currentProteins, binding.proteinas.getText().toString().split("/")[1]));
        binding.carbohidratos.setText(String.format(Locale.getDefault(), "Carbohidratos: %.1f/%s g", currentCarbs, binding.carbohidratos.getText().toString().split("/")[1]));
        binding.grasas.setText(String.format(Locale.getDefault(), "Grasas: %.1f/%s g", currentFats, binding.grasas.getText().toString().split("/")[1]));

        Log.d(TAG, "Meal added: " + meal.getName() + ", Calories: " + meal.getCalories());
    }

    private void addMealView(LinearLayout linearLayout, Meal meal, boolean saveToBackend,int calorias,double proteinas,double carbs,double grasas) {
        if (meal.getUsuario().getGoals().getDailyCalories()>=calorias){
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Confirm Action");
                builder.setMessage("Te estás pasando de tu ingesta diaria. ¿Estás seguro de querer añadirla?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LinearLayout mealCard = new LinearLayout(context);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        cardParams.setMargins(0, 8, 0, 8);
                        mealCard.setLayoutParams(cardParams);
                        mealCard.setOrientation(LinearLayout.HORIZONTAL);
                        mealCard.setPadding(16, 16, 16, 16);
                        mealCard.setGravity(Gravity.CENTER_VERTICAL);

                        GradientDrawable background = new GradientDrawable();
                        background.setColor(Color.WHITE);
                        background.setCornerRadius(16f);
                        mealCard.setBackground(background);
                        mealCard.setElevation(4f);

                        ImageView mealImage = new ImageView(context);
                        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(140, 140);
                        imageParams.setMargins(0, 0, 8, 0);
                        mealImage.setLayoutParams(imageParams);
                        mealImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        if (meal.getFoto() != null && !meal.getFoto().isEmpty()) {
                            Glide.with(context).load(meal.getFoto()).into(mealImage);
                        }

                        TextView nameText = new TextView(context);
                        nameText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        nameText.setText(meal.getName());
                        nameText.setTextSize(20);
                        nameText.setTextColor(Color.BLACK);
                        nameText.setTypeface(null, Typeface.BOLD);
                        nameText.setPadding(8, 0, 8, 0);

                        LinearLayout nutritionLayout = new LinearLayout(context);
                        nutritionLayout.setOrientation(LinearLayout.VERTICAL);
                        TextView caloriesText = new TextView(context);
                        caloriesText.setText(String.format(Locale.getDefault(), "%d kcal", meal.getCalories()));
                        caloriesText.setTextSize(20);
                        caloriesText.setTextColor(Color.parseColor("#4CAF50"));
                        TextView macrosText = new TextView(context);
                        macrosText.setText(String.format(Locale.getDefault(), "P: %.1fg C: %.1fg F: %.1fg",
                                meal.getProtein(), meal.getCarbs(), meal.getFats()));
                        macrosText.setTextSize(12);
                        macrosText.setTextColor(Color.GRAY);
                        nutritionLayout.addView(caloriesText);
                        nutritionLayout.addView(macrosText);

                        ImageView deleteImage = new ImageView(context);
                        deleteImage.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                        deleteImage.setImageResource(R.drawable.ic_delete);
                        deleteImage.setContentDescription("Delete meal");

                        mealCard.addView(mealImage);
                        mealCard.addView(nameText);
                        mealCard.addView(nutritionLayout);
                        mealCard.addView(deleteImage);
                        linearLayout.addView(mealCard);

                        if (saveToBackend) {
                            // API call to save meal
                            JsonObject mealJson = new JsonObject();
                            mealJson.addProperty("name", meal.getName());
                            mealJson.addProperty("calories", meal.getCalories());
                            mealJson.addProperty("protein", meal.getProtein());
                            mealJson.addProperty("carbs", meal.getCarbs());
                            mealJson.addProperty("fats", meal.getFats());
                            mealJson.addProperty("tipo", meal.getTipo());
                            mealJson.addProperty("fecha", meal.getFecha());
                            mealJson.addProperty("gramos", meal.getGramos());
                            mealJson.addProperty("foto", meal.getFoto());

                            if (meal.getUsuario() != null) {
                                JsonObject userJson = new JsonObject();
                                userJson.addProperty("id", meal.getUsuario().getId());
                                userJson.addProperty("name", meal.getUsuario().getName());
                                userJson.addProperty("email", meal.getUsuario().getEmail());
                                userJson.addProperty("age", meal.getUsuario().getAge());
                                userJson.addProperty("weight", meal.getUsuario().getWeight());
                                userJson.addProperty("height", meal.getUsuario().getHeight());
                                mealJson.add("user", userJson);
                            }

                            ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
                            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), mealJson.toString());
                            Call<JsonElement> call = apiService.addMeal(requestBody);
                            call.enqueue(new Callback<JsonElement>() {
                                @Override
                                public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        JsonObject responseJson = response.body().getAsJsonObject();
                                        if (responseJson.has("id")) {
                                            meal.setId(responseJson.get("id").getAsString());
                                            deleteImage.setOnClickListener(v -> deleteMeal(meal, mealCard));
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to save meal: " + response.code());
                                        Toast.makeText(context, "Failed to save meal", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JsonElement> call, Throwable t) {
                                    Log.e(TAG, "Network error: " + t.getMessage());
                                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Only set delete listener if meal already has an ID (i.e., it exists in the backend)
                            if (meal.getId() != null) {
                                deleteImage.setOnClickListener(v -> deleteMeal(meal, mealCard));
                            }
                        }
                    }
                });

                // Negative button (e.g., "Cancel")
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle the negative action (e.g., cancel)
                        dialog.dismiss();
                    }
                });
        }else{
            LinearLayout mealCard = new LinearLayout(context);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 8, 0, 8);
            mealCard.setLayoutParams(cardParams);
            mealCard.setOrientation(LinearLayout.HORIZONTAL);
            mealCard.setPadding(16, 16, 16, 16);
            mealCard.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.WHITE);
            background.setCornerRadius(16f);
            mealCard.setBackground(background);
            mealCard.setElevation(4f);

            ImageView mealImage = new ImageView(context);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(140, 140);
            imageParams.setMargins(0, 0, 8, 0);
            mealImage.setLayoutParams(imageParams);
            mealImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (meal.getFoto() != null && !meal.getFoto().isEmpty()) {
                Glide.with(context).load(meal.getFoto()).into(mealImage);
            }

            TextView nameText = new TextView(context);
            nameText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            nameText.setText(meal.getName());
            nameText.setTextSize(20);
            nameText.setTextColor(Color.BLACK);
            nameText.setTypeface(null, Typeface.BOLD);
            nameText.setPadding(8, 0, 8, 0);

            LinearLayout nutritionLayout = new LinearLayout(context);
            nutritionLayout.setOrientation(LinearLayout.VERTICAL);
            TextView caloriesText = new TextView(context);
            caloriesText.setText(String.format(Locale.getDefault(), "%d kcal", meal.getCalories()));
            caloriesText.setTextSize(20);
            caloriesText.setTextColor(Color.parseColor("#4CAF50"));
            TextView macrosText = new TextView(context);
            macrosText.setText(String.format(Locale.getDefault(), "P: %.1fg C: %.1fg F: %.1fg",
                    meal.getProtein(), meal.getCarbs(), meal.getFats()));
            macrosText.setTextSize(12);
            macrosText.setTextColor(Color.GRAY);
            nutritionLayout.addView(caloriesText);
            nutritionLayout.addView(macrosText);

            ImageView deleteImage = new ImageView(context);
            deleteImage.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            deleteImage.setImageResource(R.drawable.ic_delete);
            deleteImage.setContentDescription("Delete meal");

            mealCard.addView(mealImage);
            mealCard.addView(nameText);
            mealCard.addView(nutritionLayout);
            mealCard.addView(deleteImage);
            linearLayout.addView(mealCard);

            if (saveToBackend) {
                // API call to save meal
                JsonObject mealJson = new JsonObject();
                mealJson.addProperty("name", meal.getName());
                mealJson.addProperty("calories", meal.getCalories());
                mealJson.addProperty("protein", meal.getProtein());
                mealJson.addProperty("carbs", meal.getCarbs());
                mealJson.addProperty("fats", meal.getFats());
                mealJson.addProperty("tipo", meal.getTipo());
                mealJson.addProperty("fecha", meal.getFecha());
                mealJson.addProperty("gramos", meal.getGramos());
                mealJson.addProperty("foto", meal.getFoto());

                if (meal.getUsuario() != null) {
                    JsonObject userJson = new JsonObject();
                    userJson.addProperty("id", meal.getUsuario().getId());
                    userJson.addProperty("name", meal.getUsuario().getName());
                    userJson.addProperty("email", meal.getUsuario().getEmail());
                    userJson.addProperty("age", meal.getUsuario().getAge());
                    userJson.addProperty("weight", meal.getUsuario().getWeight());
                    userJson.addProperty("height", meal.getUsuario().getHeight());
                    mealJson.add("user", userJson);
                }

                ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), mealJson.toString());
                Call<JsonElement> call = apiService.addMeal(requestBody);
                call.enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject responseJson = response.body().getAsJsonObject();
                            if (responseJson.has("id")) {
                                meal.setId(responseJson.get("id").getAsString());
                                deleteImage.setOnClickListener(v -> deleteMeal(meal, mealCard));
                            }
                        } else {
                            Log.e(TAG, "Failed to save meal: " + response.code());
                            Toast.makeText(context, "Failed to save meal", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        Log.e(TAG, "Network error: " + t.getMessage());
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Only set delete listener if meal already has an ID (i.e., it exists in the backend)
                if (meal.getId() != null) {
                    deleteImage.setOnClickListener(v -> deleteMeal(meal, mealCard));
                }
            }
        }
    }

    private void deleteMeal(Meal meal, View mealCard) {
        if (meal.getId() == null) return;

        ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
        apiService.deleteMeal(meal.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    ((ViewGroup) mealCard.getParent()).removeView(mealCard);
                    updateTotalsAfterDeletion(meal);
                } else {
                    Log.e(TAG, "Failed to delete meal: " + response.code());
                    Toast.makeText(context, "Failed to delete meal", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotalsAfterDeletion(Meal meal) {
        int currentCalories = binding.cpiCalories.getProgress();
        binding.cpiCalories.setProgress(currentCalories - meal.getCalories());
        binding.tvCaloriesLabel.setText(String.format(Locale.getDefault(), "%d/%d kcal",
                currentCalories - meal.getCalories(), binding.cpiCalories.getMax()));

        updateMacroText(binding.proteinas, meal.getProtein());
        updateMacroText(binding.carbohidratos, meal.getCarbs());
        updateMacroText(binding.grasas, meal.getFats());

        binding.proteinbar.setProgress((int) parseMacro(binding.proteinas.getText().toString()));
        binding.carbbar.setProgress((int) parseMacro(binding.carbohidratos.getText().toString()));
        binding.fatbar.setProgress((int) parseMacro(binding.grasas.getText().toString()));
    }

    private void updateMacroText(TextView textView, double value) {
        String text = textView.getText().toString();
        double current = parseMacro(text);
        String goal = text.split("/")[1];
        textView.setText(String.format(Locale.getDefault(), "%s: %.1f/%s",
                text.split(":")[0], current - value, goal));
    }

    private double parseMacro(String text) {
        try {
            return Double.parseDouble(text.split(": ")[1].split("/")[0]);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing macro: " + e.getMessage());
            return 0;
        }
    }

    private void dismissAllDialogFragments(FragmentManager fragmentManager) {
        if (fragmentManager == null) return;
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof DialogFragment) {
                ((DialogFragment) fragment).dismissAllowingStateLoss();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissAllDialogFragments(getParentFragmentManager());
        binding = null;
    }
}