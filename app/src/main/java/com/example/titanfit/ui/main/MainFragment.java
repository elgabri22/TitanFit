package com.example.titanfit.ui.main;

import static android.health.connect.datatypes.MealType.MEAL_TYPE_BREAKFAST;
import static android.health.connect.datatypes.MealType.MEAL_TYPE_LUNCH;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import com.example.titanfit.models.Food; // Asegúrate de que esta clase existe si la usas en otro lado
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.models.UserGoal;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceFood;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.example.titanfit.ui.dialogs.DialogAddComida;
import com.example.titanfit.ui.dialogs.DialogComida;
import com.example.titanfit.ui.dialogs.DialogFavoritos;
import com.google.android.material.navigation.NavigationView; // Asegúrate de que esta clase existe si la usas en otro lado
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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
    private MainViewModel mViewModel; // Asegúrate de que esta clase MainViewModel existe
    private FragmentMainBinding binding;
    private SharedPreferencesManager manager;
    private Context context;
    private User usuario;
    private String fecha;
    private String currentMealScanning;
    private static final String MEAL_TYPE_BREAKFAST = "breakfast";
    private static final String MEAL_TYPE_LUNCH = "lunch";

    // Variables para acumular las macros y calorías
    private int currentTotalCalories = 0;
    private double currentTotalProteins = 0;
    private double currentTotalCarbs = 0;
    private double currentTotalFats = 0;

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

        // Inicializar la fecha con la fecha actual
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        fecha = sdf.format(calendar.getTime());

        // Inicializar la UI con los datos del usuario y sus objetivos
        UserGoal goals = initializeUserData();
        if (goals == null) {
            Toast.makeText(context, "Error loading user data", Toast.LENGTH_SHORT).show();
            return binding.getRoot();
        }

        // Obtener comidas iniciales para la fecha actual
        fetchMealsForDate(fecha, goals);

        // Configurar el listener del calendario
        binding.calendar.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            fecha = String.format(Locale.getDefault(), "%02d-%02d-%d", dayOfMonth, month + 1, year);
            // Al cambiar la fecha, resetear los acumuladores y volver a cargar las comidas
            resetMacroTotals();
            fetchMealsForDate(fecha, goals);
            Toast.makeText(context, fecha, Toast.LENGTH_SHORT).show();
        });

        binding.btnScanBarcodeBreakfast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMealScanning = MEAL_TYPE_BREAKFAST;
                startBarcodeScanner();
            }
        });

        binding.btnScanBarcodeLunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMealScanning = MEAL_TYPE_LUNCH;
                startBarcodeScanner();
            }
        });

        binding.fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferencesManager sharedPreferencesManager=new SharedPreferencesManager(requireContext());
                User user=sharedPreferencesManager.getUser(); // Obtener el usuario actualizado
                DialogFavoritos dialogFavoritos = new DialogFavoritos(user.getFavoritos().getComidas(), getParentFragmentManager(), MainFragment.this);
                Bundle bundle=new Bundle();
                bundle.putString("fecha",fecha);
                bundle.putString("tipo","Desayuno"); // Considera hacer esto dinámico si tienes más tipos
                dialogFavoritos.setArguments(bundle);
                dialogFavoritos.show(getParentFragmentManager(), "DialogFavoritos");
            }
        });

        // Configurar los listeners de los botones para añadir comidas
        setupButtonListeners(goals);

        return binding.getRoot();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Log.d(TAG, "Escaneo Cancelado");
                Toast.makeText(getContext(), "Escaneo Cancelado", Toast.LENGTH_SHORT).show();
            } else {
                String barcodeContent = result.getContents();
                String formatName = result.getFormatName();

                Log.d(TAG, "Contenido del código: " + barcodeContent);
                Log.d(TAG, "Formato: " + formatName);
            }
        }
    }

    private void startBarcodeScanner() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setPrompt("Escanea un código de barras");
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    // Método para reiniciar los acumuladores de macros a cero
    private void resetMacroTotals() {
        currentTotalCalories = 0;
        currentTotalProteins = 0;
        currentTotalCarbs = 0;
        currentTotalFats = 0;
        // Opcional: Actualizar la UI inmediatamente a cero o al estado inicial
        if (usuario != null && usuario.getGoals() != null) {
            updateNutritionUI(0, 0, 0, 0, usuario.getGoals());
        }
    }


    private UserGoal initializeUserData() {
        usuario = getArguments() != null ? (User) getArguments().getSerializable("user") : null;
        if (usuario == null && manager.isLoggedIn()) {
            usuario = manager.getUser();
        }

        if (usuario != null && usuario.getGoals() != null) {
            // No actualizar la UI aquí con 0s si fetchMealsForDate va a hacerlo después
            // updateNutritionUI(0, 0, 0, 0, goals); // Esto ya no es necesario aquí
            return usuario.getGoals();
        }
        Log.e(TAG, "User or goals are null");
        return null;
    }

    private void setupButtonListeners(UserGoal goals) {
        binding.addbreakfast.setOnClickListener(v -> openDialogAddComida("Desayuno"));
        binding.addlunch.setOnClickListener(v -> openDialogAddComida("Comida"));
        // Aquí podrías añadir más botones como adddinner, addsnack, etc.
    }

    private void openDialogAddComida(String tipo) {
        dismissAllDialogFragments(getParentFragmentManager());
        Bundle bundle = new Bundle();
        bundle.putString("tipo", tipo);
        bundle.putSerializable("user", usuario);
        bundle.putString("fecha", fecha);
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

                    // Reiniciar los acumuladores antes de sumar las comidas de la fecha
                    currentTotalCalories = 0;
                    currentTotalProteins = 0;
                    currentTotalCarbs = 0;
                    currentTotalFats = 0;

                    for (Meal meal : mealList) {
                        currentTotalCalories += meal.getCalories();
                        currentTotalProteins += meal.getProtein();
                        currentTotalCarbs += meal.getCarbs();
                        currentTotalFats += meal.getFats();

                        LinearLayout targetLayout = meal.getTipo().equalsIgnoreCase("Desayuno")
                                ? binding.llBreakfastItems : binding.llLunchItems;
                        // Pasar 'false' para saveToBackend ya que estas comidas ya están en el backend
                        addMealView(targetLayout, meal, false);
                    }

                    // Actualizar la UI con los totales calculados
                    updateNutritionUI(currentTotalCalories, currentTotalProteins, currentTotalCarbs, currentTotalFats, goals);
                } else {
                    Log.e(TAG, "Request failed. Code: " + response.code() + " Message: " + response.message());
                    Toast.makeText(context, "Failed to load meals", Toast.LENGTH_SHORT).show();
                    // Si no hay comidas o la llamada falla, asegúrate de que la UI refleje cero o un estado inicial
                    updateNutritionUI(0, 0, 0, 0, goals);
                }
            }

            @Override
            public void onFailure(Call<List<Meal>> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                // En caso de error de red, resetear la UI a cero o un estado inicial
                updateNutritionUI(0, 0, 0, 0, goals);
            }
        });
    }

    private void updateNutritionUI(int calories, double proteins, double carbs, double fats, UserGoal goals) {
        if (calories > goals.getDailyCalories()){
            binding.tvCaloriesLabel.setTextColor(Color.parseColor("#FF0000")); // Rojo si se excede
        } else {
            binding.tvCaloriesLabel.setTextColor(Color.BLACK); // Negro si está dentro del límite
        }
        binding.cpiCalories.setMax(goals.getDailyCalories());
        binding.cpiCalories.setProgress(calories);
        binding.tvCaloriesLabel.setText(String.format(Locale.getDefault(), "%d/%d kcal", calories, goals.getDailyCalories()));

        // Asegúrate de que las barras de progreso no se pasen de su máximo si los objetivos no están definidos
        // O define los máximos de las barras de progreso si son distintos a los objetivos
        binding.proteinbar.setMax((int) Math.round(goals.getProteinPercentage()));
        binding.carbbar.setMax((int) Math.round(goals.getCarbsPercentage()));
        binding.fatbar.setMax((int) Math.round(goals.getFatsPercentage()));

        binding.proteinbar.setProgress((int) proteins);
        binding.carbbar.setProgress((int) carbs);
        binding.fatbar.setProgress((int) fats);

        binding.proteinas.setText(String.format(Locale.getDefault(), "Proteinas: %.1f/%d g", proteins, Math.round(goals.getProteinPercentage())));
        binding.carbohidratos.setText(String.format(Locale.getDefault(), "Carbohidratos: %.1f/%d g", carbs, Math.round(goals.getCarbsPercentage())));
        binding.grasas.setText(String.format(Locale.getDefault(), "Grasas: %.1f/%d g", fats, Math.round(goals.getFatsPercentage())));
    }

    @Override
    public void onMealAdded(Meal meal, String tipo) {
        // Calcular los nuevos totales antes de decidir si añadir o no
        int newTotalCalories = currentTotalCalories + meal.getCalories();
        double newTotalProteins = currentTotalProteins + meal.getProtein();
        double newTotalCarbs = currentTotalCarbs + meal.getCarbs();
        double newTotalFats = currentTotalFats + meal.getFats();

        if (newTotalCalories <= usuario.getGoals().getDailyCalories()){
            // Si no se excede, actualizar los acumuladores y la UI
            currentTotalCalories = newTotalCalories;
            currentTotalProteins = newTotalProteins;
            currentTotalCarbs = newTotalCarbs;
            currentTotalFats = newTotalFats;

            LinearLayout targetLayout = meal.getTipo().equalsIgnoreCase("Desayuno")
                    ? binding.llBreakfastItems : binding.llLunchItems;
            addMealView(targetLayout, meal, true); // Pasar 'true' para saveToBackend

            Log.d(TAG, "Meal added: " + meal.getName() + ", Calories: " + meal.getCalories());

            // Actualizar la UI con los nuevos totales
            updateNutritionUI(currentTotalCalories, currentTotalProteins, currentTotalCarbs, currentTotalFats, usuario.getGoals());
        } else {
            // Si se excede, mostrar el diálogo de confirmación
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Confirm Action");
            builder.setMessage("Te estás pasando de tu ingesta diaria. ¿Estás seguro de querer añadirla?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                // Si el usuario confirma, actualizar los acumuladores y la UI
                currentTotalCalories = newTotalCalories;
                currentTotalProteins = newTotalProteins;
                currentTotalCarbs = newTotalCarbs;
                currentTotalFats = newTotalFats;

                LinearLayout targetLayout = meal.getTipo().equalsIgnoreCase("Desayuno")
                        ? binding.llBreakfastItems : binding.llLunchItems;
                addMealView(targetLayout, meal, true); // Pasar 'true' para saveToBackend

                Log.d(TAG, "Meal added: " + meal.getName() + ", Calories: " + meal.getCalories());

                // Actualizar la UI con los nuevos totales (ya excedidos)
                updateNutritionUI(currentTotalCalories, currentTotalProteins, currentTotalCarbs, currentTotalFats, usuario.getGoals());
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    // Se eliminó el parámetro 'calorias' ya que se usa currentTotalCalories
    private void addMealView(LinearLayout linearLayout, Meal meal, boolean saveToBackend) {
        createMealCard(linearLayout, meal, saveToBackend);
    }

    // Se eliminó el parámetro 'calorias' ya que se usa currentTotalCalories
    private void createMealCard(LinearLayout linearLayout, Meal meal, boolean saveToBackend) {
        LinearLayout mealCard = new LinearLayout(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 8, 0, 8);
        mealCard.setLayoutParams(cardParams);
        mealCard.setOrientation(LinearLayout.HORIZONTAL);
        mealCard.setPadding(16, 16, 16, 16);
        mealCard.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); // O un color de fondo de tu tema
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
        nameText.setTextColor(Color.BLACK); // O un color de texto de tu tema
        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setPadding(8, 0, 8, 0);

        LinearLayout nutritionLayout = new LinearLayout(context);
        nutritionLayout.setOrientation(LinearLayout.VERTICAL);
        TextView caloriesText = new TextView(context);
        caloriesText.setText(String.format(Locale.getDefault(), "%d kcal", meal.getCalories()));
        caloriesText.setTextSize(20);
        caloriesText.setTextColor(Color.parseColor("#4CAF50")); // Un color para las calorías
        TextView macrosText = new TextView(context);
        macrosText.setText(String.format(Locale.getDefault(), "P: %.1fg C: %.1fg F: %.1fg",
                meal.getProtein(), meal.getCarbs(), meal.getFats()));
        macrosText.setTextSize(12);
        macrosText.setTextColor(Color.GRAY); // Un color para las macros
        nutritionLayout.addView(caloriesText);
        nutritionLayout.addView(macrosText);

        ImageView deleteImage = new ImageView(context);
        deleteImage.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
        deleteImage.setImageResource(R.drawable.ic_delete); // Asegúrate de tener ic_delete
        deleteImage.setContentDescription("Delete meal");

        mealCard.addView(mealImage);
        mealCard.addView(nameText);
        mealCard.addView(nutritionLayout);
        mealCard.addView(deleteImage);
        linearLayout.addView(mealCard);

        if (saveToBackend) {
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
                        Log.e(TAG, "Failed to save meal: " + response.code() + " Message: " + response.message());
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
            if (meal.getId() != null) {
                deleteImage.setOnClickListener(v -> deleteMeal(meal, mealCard));
            }
        }
    }

    private void deleteMeal(Meal meal, View mealCard) {
        // Ya no necesitamos obtener currentCalories de la UI aquí
        if (meal.getId() == null) return;

        ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
        apiService.deleteMeal(meal.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    ((ViewGroup) mealCard.getParent()).removeView(mealCard);
                    updateTotalsAfterDeletion(meal);
                } else {
                    Log.e(TAG, "Failed to delete meal: " + response.code() + " Message: " + response.message());
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
        // Restar los valores de la comida eliminada de los totales acumulados
        currentTotalCalories -= meal.getCalories();
        currentTotalProteins -= meal.getProtein();
        currentTotalCarbs -= meal.getCarbs();
        currentTotalFats -= meal.getFats();

        // Asegurarse de que los valores no sean negativos
        currentTotalCalories = Math.max(0, currentTotalCalories);
        currentTotalProteins = Math.max(0, currentTotalProteins);
        currentTotalCarbs = Math.max(0, currentTotalCarbs);
        currentTotalFats = Math.max(0, currentTotalFats);

        // Actualizar la UI con los nuevos totales
        // Se pasa usuario.getGoals() directamente, ya que goals es una propiedad del usuario
        if (usuario != null && usuario.getGoals() != null) {
            updateNutritionUI(currentTotalCalories, currentTotalProteins, currentTotalCarbs, currentTotalFats, usuario.getGoals());
        } else {
            SharedPreferencesManager sharedPreferencesManager=new SharedPreferencesManager(requireContext());
            User user=sharedPreferencesManager.getUser();
            Log.e(TAG, "User goals are null after deletion, cannot update UI accurately.");
            // Considerar resetear la UI a cero o mostrar un mensaje de error
            updateNutritionUI(0, 0, 0, 0, user.getGoals()); // O un UserGoal por defecto
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