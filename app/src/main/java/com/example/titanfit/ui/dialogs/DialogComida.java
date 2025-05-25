package com.example.titanfit.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.titanfit.R;
import com.example.titanfit.databinding.DialogComidaBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.models.FoodDialog;
import com.example.titanfit.models.Meal;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceFood;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class DialogComida extends DialogFragment {

    private static final String ARG_COMIDA = "comida";
    private static final String TAG = "DialogComida";
    private Food comida;
    private Meal meal;
    private DialogComidaBinding binding;
    private double baseCalories, baseCarbs, baseProteins, baseFats; // Valores base por 100g

    public static DialogComida newInstance(Food comida) {
        DialogComida dialog = new DialogComida();
        Bundle args = new Bundle();
        args.putSerializable(ARG_COMIDA, comida);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            comida = (Food) getArguments().getSerializable(ARG_COMIDA);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogComidaBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Configura el nombre del alimento
        binding.textViewTitle.setText(comida != null && comida.getName() != null ? comida.getName() : "Alimento");

        // Establece los gramos iniciales
        binding.editTextGrams.setText("100");

        // Realiza la búsqueda inicial para obtener los valores base
        if (comida != null && comida.getName() != null) {
            busqueda();
        }

        // Añade un TextWatcher para recalcular macros cuando cambien los gramos
        binding.editTextGrams.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    try {
                        double grams = Double.parseDouble(s.toString());
                        calculateForGrams(grams);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Entrada no válida para gramos: " + s.toString());
                    }
                }
            }
        });

        // Configura el botón de agregar
        binding.buttonAdd.setOnClickListener(v -> {
            String gramosStr = binding.editTextGrams.getText().toString();
            double gramos = gramosStr.isEmpty() ? 100 : Double.parseDouble(gramosStr);

            double factor = gramos / 100.0;
            double calories = baseCalories * factor;
            double carbs = baseCarbs * factor;
            double proteins = baseProteins * factor;
            double fats = baseFats * factor;
            String tipo=getArguments().getString("tipo");

            // Crear la comida (Meal)
            Meal meal = new Meal(comida.getName(), (int)calories, proteins,carbs, fats,tipo, LocalDate.now().toString(), gramos);

            FoodDialog.metecomida(meal);
            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void busqueda() {

        ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("query", binding.editTextGrams.getText().toString()+" g of " + comida.getName());

        Log.d("json",jsonObject.toString());
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                jsonObject.toString()
        );

        Call<JsonElement> call = apiService.getFoods("655d5ed6", "e26f024577ee0c93c3383d9ff0cdb948", requestBody);

        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful()) {
                    JsonElement rawResponse = response.body();
                    Log.d("s",rawResponse.toString());
                    if (rawResponse != null) {
                        Log.d(TAG, "Respuesta completa: " + rawResponse.toString());

                        if (rawResponse.isJsonObject()) {
                            JsonObject jsonObject = rawResponse.getAsJsonObject();

                            if (jsonObject.has("foods") && jsonObject.get("foods").isJsonArray()) {
                                JsonArray foodsArray = jsonObject.getAsJsonArray("foods");
                                if (foodsArray.size() > 0) {
                                    JsonObject foodObject = foodsArray.get(0).getAsJsonObject();

                                    // Extrae las macros y calorías base (para 100g)
                                    baseCalories = foodObject.has("nf_calories") && !foodObject.get("nf_calories").isJsonNull()
                                            ? foodObject.get("nf_calories").getAsDouble() : 0.0;
                                    baseCarbs = foodObject.has("nf_total_carbohydrate") && !foodObject.get("nf_total_carbohydrate").isJsonNull()
                                            ? foodObject.get("nf_total_carbohydrate").getAsDouble() : 0.0;
                                    baseProteins = foodObject.has("nf_protein") && !foodObject.get("nf_protein").isJsonNull()
                                            ? foodObject.get("nf_protein").getAsDouble() : 0.0;
                                    baseFats = foodObject.has("nf_total_fat") && !foodObject.get("nf_total_fat").isJsonNull()
                                            ? foodObject.get("nf_total_fat").getAsDouble() : 0.0;

                                    // Calcula para los gramos iniciales (100g)
                                    calculateForGrams(100);
                                } else {
                                    Log.w(TAG, "No se encontraron alimentos en la respuesta.");
                                    updateMacros(0, 0, 0, 0);
                                }
                            } else {
                                Log.d(TAG, "La respuesta JSON no contiene un array 'foods'.");
                                updateMacros(0, 0, 0, 0);
                            }
                        } else {
                            Log.w(TAG, "La respuesta no es un objeto JSON principal.");
                            updateMacros(0, 0, 0, 0);
                        }
                    } else {
                        Log.e(TAG, "El cuerpo de la respuesta es nulo.");
                        updateMacros(0, 0, 0, 0);
                    }
                } else {
                    Log.e(TAG, "La llamada a la API falló: " + response.code() + " - " + response.message());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Cuerpo de error: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer el cuerpo de error: " + e.getMessage());
                    }
                    updateMacros(0, 0, 0, 0);
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e(TAG, "Error de red o fallo de la llamada: " + t.getMessage(), t);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        updateMacros(0, 0, 0, 0);
                    });
                }
            }
        });
    }

    private void calculateForGrams(double grams) {
        double factor = grams / 100.0;
        double newCalories = baseCalories * factor;
        double newCarbs = baseCarbs * factor;
        double newProteins = baseProteins * factor;
        double newFats = baseFats * factor;

        updateMacros(newCalories, newCarbs, newProteins, newFats);
    }

    private void updateMacros(double calories, double carbs, double proteins, double fats) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                binding.textViewCarbs.setText(String.format("%.1f g", carbs));
                binding.textViewProteins.setText(String.format("%.1f g", proteins));
                binding.textViewFats.setText(String.format("%.1f g", fats));
                binding.textViewCalories.setText(calories+" kcal");
            });
        }
    }
}