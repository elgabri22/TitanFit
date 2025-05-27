package com.example.titanfit.ui.main;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
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

    private MainViewModel mViewModel;
    private FragmentMainBinding binding;
    private Calendar calendar;
    private SharedPreferencesManager manager;
    private Context context;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        binding = FragmentMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = requireContext();
        manager = new SharedPreferencesManager(requireContext());

        // Initialize UI with user data
        UserGoal goals=initializeUserData();

        String fecha="";

        try {
            // Obtener la fecha actual (hoy es 26 de mayo de 2025)
            LocalDate today = LocalDate.now();
            int expectedDay = today.getDayOfMonth(); // Debería ser 26

            // Obtener el texto del TextView y depurarlo
            String currentDayText = binding.tvCurrentDay.getText().toString();
            Log.d("fecha", "Contenido del TextView: " + currentDayText);

            // Extraer el número del día del TextView
            String dayNumber = currentDayText.replaceAll("\\D+", "");
            int day;
            if (!dayNumber.isEmpty()) {
                try {
                    day = Integer.parseInt(dayNumber);
                    if (day != expectedDay) {
                        Log.w("fecha", "El día extraído (" + day + ") no coincide con el día actual (" + expectedDay + "). Usando el día actual.");
                        day = expectedDay; // Usar el día actual (26) si no coincide
                    }
                } catch (NumberFormatException e) {
                    Log.w("fecha", "No se pudo parsear el día del TextView: " + currentDayText);
                    day = expectedDay; // Usar el día actual (26) en caso de error
                }
            } else {
                Log.w("fecha", "El TextView no contiene un número válido: " + currentDayText);
                day = expectedDay; // Usar el día actual (26) si no hay número
            }

            // Ajustar la fecha al día especificado
            try {
                today = today.withDayOfMonth(day);
            } catch (DateTimeException e) {
                Log.e("fecha", "Día inválido para el mes actual: " + day);
                Toast.makeText(context, "Día inválido: " + day, Toast.LENGTH_SHORT).show();
            }

            // Obtener el nombre del día de la semana en español
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            // Formatear la fecha al formato YYYY-MM-DD
            fecha = String.format("%d-%02d-%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            Log.d("fecha", "Fecha generada: " + fecha); // Debería mostrar "2025-05-26"

        } catch (Exception e) {
            Log.e("fecha", "Error inesperado: " + e.getMessage());
            Toast.makeText(context, "Error al procesar la fecha", Toast.LENGTH_SHORT).show();
        }
        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
        Toast.makeText(context, fecha, Toast.LENGTH_LONG).show();
        Call<List<Meal>> meals = apiService.getMeals(fecha);
        meals.enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(Call<List<Meal>> call, Response<List<Meal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> mealList = response.body();
                    int calorias=0;
                    double proteinas=0;
                    double carbs=0;
                    double fats=0;
                    Log.d("body", response.body().toString());
                    binding.llBreakfastItems.removeAllViews();
                    binding.llLunchItems.removeAllViews();
                    if (!mealList.isEmpty()) {
                        for (Meal comida : mealList) {
                            LinearLayout targetLayout = comida.getTipo().equalsIgnoreCase("Desayuno")
                                    ? binding.llBreakfastItems : binding.llLunchItems;
                            addMealView(targetLayout, comida);
                            calorias+=comida.getCalories();
                            proteinas+=comida.getProtein();
                            carbs+=comida.getCarbs();
                            fats+=comida.getFats();
                        }
                    }
                    binding.fatbar.setProgress((int)fats);
                    binding.carbbar.setProgress((int) carbs);
                    binding.proteinbar.setProgress((int) proteinas);
                    binding.proteinas.setText("Proteinas: " + proteinas + "/" + Math.round(goals.getProteinPercentage()) + "g");
                    binding.carbohidratos.setText("Carbohidratos: " + carbs + "/" + Math.round(goals.getCarbsPercentage()) + "g");
                    binding.grasas.setText("Grasas: " + fats + "/" + Math.round(goals.getFatsPercentage()) + "g");
                    binding.cpiCalories.setMax(goals.getDailyCalories());
                    binding.cpiCalories.setMin(0);
                    binding.cpiCalories.setProgress(calorias);
                    binding.tvCaloriesLabel.setText(calorias + "/" + goals.getDailyCalories() + "kcal");

                } else {
                    Log.e(TAG, "Request failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Meal>> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });

        calendar = Calendar.getInstance();
        updateDateText();

        // Set up button listeners
        setupButtonListeners(goals.getProteinPercentage(),goals.getCarbsPercentage(),goals.getFatsPercentage(),goals.getDailyCalories());

        return root;
    }

    private UserGoal initializeUserData() {
        Bundle args = getArguments();
        User user = null;
        if (args != null) {
            user = (User) args.getSerializable("user");
        }
        if (user == null && manager.isLoggedIn()) {
            user = manager.getUser();
        }

        if (user != null) {
            binding.tvCaloriesLabel.setText(0 + "/" + user.getGoals().getDailyCalories() + "kcal");
            binding.proteinas.setText("Proteinas: " + 0 + "/" + Math.round(user.getGoals().getProteinPercentage()) + "g");
            binding.carbohidratos.setText("Carbohidratos: " + 0 + "/" + Math.round(user.getGoals().getCarbsPercentage()) + "g");
            binding.grasas.setText("Grasas: " + 0 + "/" + Math.round(user.getGoals().getFatsPercentage()) + "g");
            binding.cpiCalories.setMax(user.getGoals().getDailyCalories());
            binding.cpiCalories.setMin(0);
            return user.getGoals();
        } else {
            // Usuario no encontrado, maneja este caso (retorna null o un valor por defecto)
            Log.e("MainFragment", "Usuario es null en initializeUserData");
            return null;  // o new UserGoal() si tienes un constructor por defecto
        }
    }


    private void setupButtonListeners(double proteinastotal,double carbstotal, double fatstotal,int calstotal) {
        binding.btnPreviousDay.setOnClickListener(v -> changeDay(-1,proteinastotal,carbstotal,fatstotal,calstotal));
        binding.btnNextDay.setOnClickListener(v -> changeDay(1,proteinastotal,carbstotal,fatstotal,calstotal));
        binding.addbreakfast.setOnClickListener(v -> openDialogAddComida("Desayuno"));
        binding.addlunch.setOnClickListener(v -> openDialogAddComida("Comida"));
    }

    private void openDialogAddComida(String tipo) {
        // Dismiss all existing dialogs
        dismissAllDialogFragments(getParentFragmentManager());

        Bundle args=getArguments();

        Bundle bundle = new Bundle();
        bundle.putString("tipo", tipo);
        DialogAddComida dialog = new DialogAddComida(new ArrayList<>(), getParentFragmentManager(), this);
        dialog.setArguments(bundle);
        dialog.show(getParentFragmentManager(), "DialogAddComida");
    }

    private void changeDay(int days,double proteinastotal,double carbstotal, double fatstotal,int calstotal) {
        String currentDayText = binding.tvCurrentDay.getText().toString();
        String dayNumber = currentDayText.replaceAll("\\D+", "");
        int day = Integer.parseInt(dayNumber);

        LocalDate today = LocalDate.now().withDayOfMonth(day);
        LocalDate newDay = today.plusDays(days);

        DayOfWeek dayOfWeek = newDay.getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

        String fecha = String.format("%d-%02d-%02d", newDay.getYear(), newDay.getMonthValue(), newDay.getDayOfMonth());
        binding.tvCurrentDay.setText(dayName + " " + String.format("%02d", newDay.getDayOfMonth()));

        fetchMealsForDate(fecha,proteinastotal,carbstotal,fatstotal,calstotal);
    }

    private void fetchMealsForDate(String fecha,double proteinastotal,double carbstotal, double fatstotal,int calstotal) {
        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
        Toast.makeText(context, fecha, Toast.LENGTH_LONG).show();
        Call<List<Meal>> meals = apiService.getMeals(fecha);
        meals.enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(Call<List<Meal>> call, Response<List<Meal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> mealList = response.body();
                    int calorias=0;
                    double proteinas=0;
                    double carbs=0;
                    double fats=0;
                    Log.d("body", response.body().toString());
                    binding.llBreakfastItems.removeAllViews();
                    binding.llLunchItems.removeAllViews();
                    if (!mealList.isEmpty()) {
                        for (Meal comida : mealList) {
                            LinearLayout targetLayout = comida.getTipo().equalsIgnoreCase("Desayuno")
                                    ? binding.llBreakfastItems : binding.llLunchItems;
                            addMealView(targetLayout, comida);
                            calorias+=comida.getCalories();
                            proteinas+=comida.getProtein();
                            carbs+=comida.getCarbs();
                            fats+=comida.getFats();
                        }
                    }
                    binding.fatbar.setProgress((int)fats);
                    binding.carbbar.setProgress((int) carbs);
                    binding.proteinbar.setProgress((int) proteinas);
                    binding.proteinas.setText("Proteinas: " + proteinas + "/" + Math.round(proteinastotal) + "g");
                    binding.carbohidratos.setText("Carbohidratos: " + carbs + "/" + Math.round(carbstotal) + "g");
                    binding.grasas.setText("Grasas: " + fats + "/" + Math.round(fatstotal) + "g");
                    binding.cpiCalories.setMax(calstotal);
                    binding.cpiCalories.setMin(0);
                    binding.cpiCalories.setProgress(calorias);
                    binding.tvCaloriesLabel.setText(calorias + "/" + calstotal + "kcal");

                } else {
                    Log.e(TAG, "Request failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Meal>> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }

    @Override
    public void onMealAdded(Meal meal,String tipo) {
        // Update UI with new Meal data
        int currentCalories = binding.cpiCalories.getProgress();
        int newCalories = currentCalories + meal.getCalories();
        binding.cpiCalories.setProgress(newCalories);
        binding.tvCaloriesLabel.setText(newCalories + "/" + binding.cpiCalories.getMax() + "kcal");

        // Update macros
        double currentProteins = parseMacro(binding.proteinas.getText().toString());
        double currentCarbs = parseMacro(binding.carbohidratos.getText().toString());
        double currentFats = parseMacro(binding.grasas.getText().toString());

        binding.proteinas.setText("Proteinas: " + (currentProteins + meal.getProtein()) + "/" +
                Math.round(parseMacroGoal(binding.proteinas.getText().toString())) + "g");
        binding.carbohidratos.setText("Carbohidratos: " + (currentCarbs + meal.getCarbs()) + "/" +
                Math.round(parseMacroGoal(binding.carbohidratos.getText().toString())) + "g");
        binding.grasas.setText("Grasas: " + (currentFats + meal.getFats()) + "/" +
                Math.round(parseMacroGoal(binding.grasas.getText().toString())) + "g");

        binding.proteinbar.setProgress((int) (binding.proteinbar.getProgress()+(meal.getProtein())));

        binding.carbbar.setProgress((int) (binding.proteinbar.getProgress()+(meal.getCarbs())));

        binding.fatbar.setProgress((int) (binding.proteinbar.getProgress()+(meal.getFats())));

        LinearLayout targetLayout = meal.getTipo().equalsIgnoreCase("Desayuno")
                ? binding.llBreakfastItems : binding.llLunchItems;
        addMealView(targetLayout, meal);




        Log.d("MainFragment", "Meal added: " + meal.getName() + ", Calories: " + meal.getCalories());
    }

    private void addMealView(LinearLayout linearLayout, Meal meal) {
        // Crear la tarjeta de comida inmediatamente para mostrarla localmente
        LinearLayout mealCard = new LinearLayout(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 8, 0, 8);
        mealCard.setLayoutParams(cardParams);
        mealCard.setOrientation(LinearLayout.HORIZONTAL);
        mealCard.setPadding(16, 16, 16, 16);
        mealCard.setGravity(Gravity.CENTER_VERTICAL);

        // Configurar fondo redondeado con sombra
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#FFFFFF"));
        background.setCornerRadius(16f);
        mealCard.setBackground(background);
        mealCard.setElevation(4f);

        // Imagen de la comida
        ImageView mealImage = new ImageView(context);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(140, 140);
        imageParams.setMargins(0, 0, 8, 0);
        mealImage.setLayoutParams(imageParams);
        mealImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        String fotoUrl = meal.getFoto();
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(context)
                    .load(fotoUrl)
                    .into(mealImage);
        }

        // Nombre de la comida
        TextView nameText = new TextView(context);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameText.setLayoutParams(nameParams);
        nameText.setText(meal.getName());
        nameText.setTextSize(20);
        nameText.setTextColor(Color.BLACK);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        nameText.setPadding(8, 0, 8, 0);

        // Información nutricional
        LinearLayout nutritionLayout = new LinearLayout(context);
        nutritionLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        nutritionLayout.setOrientation(LinearLayout.VERTICAL);

        TextView caloriesText = new TextView(context);
        caloriesText.setText(meal.getCalories() + " kcal");
        caloriesText.setTextSize(20);
        caloriesText.setTextColor(Color.parseColor("#4CAF50"));

        TextView macrosText = new TextView(context);
        macrosText.setText(String.format("P: %.1fg  C: %.1fg  F: %.1fg",
                meal.getProtein(), meal.getCarbs(), meal.getFats()));
        macrosText.setTextSize(12);
        macrosText.setTextColor(Color.GRAY);

        nutritionLayout.addView(caloriesText);
        nutritionLayout.addView(macrosText);

        // Ícono de eliminación
        ImageView deleteImage = new ImageView(context);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(100, 100);
        deleteParams.setMargins(20, 0, 0, 0);
        deleteImage.setLayoutParams(deleteParams);
        deleteImage.setImageResource(R.drawable.ic_delete);
        deleteImage.setContentDescription("Delete meal");

        // Añadir vistas a la tarjeta
        mealCard.addView(mealImage);
        mealCard.addView(nameText);
        mealCard.addView(nutritionLayout);
        mealCard.addView(deleteImage);
        linearLayout.addView(mealCard);

        // Realizar la llamada a la API para guardar la comida
        ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.toJsonTree(meal).getAsJsonObject();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
        Call<JsonElement> call = apiService.addMeal(requestBody);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isJsonNull()) {
                    JsonElement jsonElement = response.body();
                    Meal savedMeal = null;
                    try {
                        savedMeal = gson.fromJson(jsonElement, Meal.class);
                        if (jsonElement.isJsonObject()) {
                            JsonObject jsonObj = jsonElement.getAsJsonObject();
                            if (jsonObj.has("id")) {
                                String id = jsonObj.get("id").getAsString();
                                meal.setId(id); // Actualizar el ID de la comida original
                            }
                        }
                        Log.d("MainFragment", "Meal added: " + meal.getName() + ", ID: " + meal.getId());
                        Toast.makeText(context, "Meal added successfully", Toast.LENGTH_SHORT).show();

                        // Configurar el listener de eliminación con el ID actualizado
                        deleteImage.setOnClickListener(view -> {
                            if (meal.getId() != null) {
                                Log.d("id", meal.getId());
                                ApiServiceFood apiServiceDelete = ApiClient.getClient().create(ApiServiceFood.class);
                                Call<Void> deleteCall = apiServiceDelete.deleteMeal(meal.getId());
                                deleteCall.enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) {
                                            linearLayout.removeView(mealCard);

                                            // Actualizar calorías
                                            int currentCalories = binding.cpiCalories.getProgress();
                                            binding.cpiCalories.setProgress(currentCalories - meal.getCalories());
                                            binding.tvCaloriesLabel.setText((currentCalories - meal.getCalories()) + "/" +
                                                    binding.cpiCalories.getMax() + " kcal");

                                            // Actualizar macros
                                            double currentProteins = parseMacro(binding.proteinas.getText().toString());
                                            double currentCarbs = parseMacro(binding.carbohidratos.getText().toString());
                                            double currentFats = parseMacro(binding.grasas.getText().toString());

                                            binding.proteinas.setText("Proteinas: " + String.format("%.1f", currentProteins - meal.getProtein()) + "/" +
                                                    Math.round(parseMacroGoal(binding.proteinas.getText().toString())) + "g");
                                            binding.carbohidratos.setText("Carbohidratos: " + String.format("%.1f", currentCarbs - meal.getCarbs()) + "/" +
                                                    Math.round(parseMacroGoal(binding.carbohidratos.getText().toString())) + "g");
                                            binding.grasas.setText("Grasas: " + String.format("%.1f", currentFats - meal.getFats()) + "/" +
                                                    Math.round(parseMacroGoal(binding.grasas.getText().toString())) + "g");
                                        } else {
                                            Log.e("MainFragment", "Delete failed: " + response.code());
                                            Toast.makeText(context, "Failed to delete meal", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        Log.e("MainFragment", "Delete error: " + t.getMessage());
                                        Toast.makeText(context, "Error deleting meal", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            Toast.makeText(context, "Meal removed", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        Log.w("MainFragment", "Could not parse response, using original meal: " + e.getMessage());
                        savedMeal = meal; // Usar la comida original en caso de error
                    }
                } else {
                    Log.e("MainFragment", "Add meal failed: " + response.code());
                    Toast.makeText(context, "Failed to add meal", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e("MainFragment", "Add meal error: " + t.getMessage());
                Toast.makeText(context, "Error adding meal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseMacro(String text) {
        try {
            return Double.parseDouble(text.split(": ")[1].split("/")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseMacroGoal(String text) {
        try {
            return Double.parseDouble(text.split("/")[1].replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd", Locale.getDefault());
        String formattedDate = sdf.format(calendar.getTime());
        binding.tvCurrentDay.setText(formattedDate);
    }

    private void dismissAllDialogFragments(FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            Log.e("DialogDismiss", "FragmentManager is null");
            return;
        }
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof DialogFragment) {
                DialogFragment dialogFragment = (DialogFragment) fragment;
                if (dialogFragment.getDialog() != null && dialogFragment.getDialog().isShowing()) {
                    dialogFragment.dismiss();
                }
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