package com.example.titanfit.ui.dialogs;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.titanfit.R;
import com.example.titanfit.adapters.AdapterComida;
import com.example.titanfit.databinding.DialogAddComidaBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceFood;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DialogAddComida extends DialogFragment {
    private DialogAddComidaBinding binding;
    private List<Food> comidas;
    private Map<Integer, String> tipos;
    private AdapterComida adapter;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private FragmentManager fragmentManager;
    private DialogComida.OnMealAddedListener mealAddedListener;

    public DialogAddComida(ArrayList<Food> foods, FragmentManager supportFragmentManager, DialogComida.OnMealAddedListener listener) {
        this.comidas = foods != null ? foods : new ArrayList<>();
        this.fragmentManager = supportFragmentManager;
        this.mealAddedListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        binding = DialogAddComidaBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();

        // Initialize RecyclerView
        binding.recyclerViewComidas.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterComida(comidas, fragmentManager, mealAddedListener);
        String tipo = getArguments() != null ? getArguments().getString("tipo", "Desconocido") : "Desconocido";
        adapter.actualizaTipo(tipo);
        binding.recyclerViewComidas.setAdapter(adapter);

        // Initialize tipos map
        tipos = new HashMap<>();
        tipos.put(1, "Dairy and Egg Products");
        tipos.put(2, "Spices, Herbs, and Other Products");
        tipos.put(3, "Fruits");
        tipos.put(4, "Vegetables");
        tipos.put(5, "Legumes and Legume Products");
        tipos.put(6, "Nuts and Seed Products");
        tipos.put(7, "Poultry Products");
        tipos.put(8, "Sausages and Lunch Meats");
        tipos.put(9, "Cereal Grains and Pasta");
        tipos.put(10, "Pork Products");
        tipos.put(11, "Vegetables and Vegetable Products");
        tipos.put(12, "Nut and Seed Products");
        tipos.put(13, "Beef Products");
        tipos.put(14, "Beverages");
        tipos.put(15, "Finfish and Shellfish Products");
        tipos.put(16, "Sweets");
        tipos.put(17, "Fast Foods");
        tipos.put(18, "Mixed Dishes");
        tipos.put(19, "Baked Products");
        tipos.put(20, "Snacks");
        tipos.put(21, "Soups, Sauces, and Gravies");
        tipos.put(22, "Breakfast Cereals");
        tipos.put(23, "Fats and Oils");
        tipos.put(24, "Baby Foods");
        tipos.put(25, "Restaurant Foods");
        tipos.put(35, "Breakfast Foods");

        // Configure search
        binding.imageButtonSearch.setOnClickListener(v -> busqueda(binding.editTextSearch.getText().toString()));

        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> busqueda(s.toString());
                searchHandler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        builder.setView(view);
        return builder.create();
    }

    private void busqueda(String comida) {
        if (comida == null || comida.trim().isEmpty()) {
            comidas.clear();
            adapter.actualizarLista(comidas);
            return;
        }

        ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("query", comida);
        Log.d(TAG, "Enviando JSON Body: " + jsonObject.toString());

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
        Call<JsonElement> call = apiService.getFoods("655d5ed6", "e26f024577ee0c93c3383d9ff0cdb948", requestBody);

        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonElement rawResponse = response.body();
                    Log.d(TAG, "Respuesta completa: " + rawResponse.toString());

                    if (rawResponse.isJsonObject()) {
                        JsonObject jsonObject = rawResponse.getAsJsonObject();
                        if (jsonObject.has("foods") && jsonObject.get("foods").isJsonArray()) {
                            comidas.clear();
                            JsonArray foodsArray = jsonObject.getAsJsonArray("foods");
                            for (JsonElement hintElement : foodsArray) {
                                if (hintElement.isJsonObject()) {
                                    JsonObject hintObject = hintElement.getAsJsonObject();
                                    String name = hintObject.has("food_name") && !hintObject.get("food_name").isJsonNull()
                                            ? hintObject.get("food_name").getAsString() : "N/A";

                                    int calories = 0;
                                    if (hintObject.has("nf_calories") && !hintObject.get("nf_calories").isJsonNull()) {
                                        try {
                                            calories = (int) Math.round(hintObject.get("nf_calories").getAsDouble());
                                        } catch (Exception e) {
                                            Log.w(TAG, "Error al convertir calorías: " + e.getMessage());
                                        }
                                    }

                                    double proteinas = hintObject.has("nf_protein") && !hintObject.get("nf_protein").isJsonNull()
                                            ? hintObject.get("nf_protein").getAsDouble() : 0.0;
                                    double carbs = hintObject.has("nf_total_carbohydrate") && !hintObject.get("nf_total_carbohydrate").isJsonNull()
                                            ? hintObject.get("nf_total_carbohydrate").getAsDouble() : 0.0;
                                    double fats = hintObject.has("nf_total_fat") && !hintObject.get("nf_total_fat").isJsonNull()
                                            ? hintObject.get("nf_total_fat").getAsDouble() : 0.0;

                                    String foto = hintObject.has("photo") && hintObject.get("photo").isJsonObject()
                                            && hintObject.getAsJsonObject("photo").has("thumb")
                                            && !hintObject.getAsJsonObject("photo").get("thumb").isJsonNull()
                                            ? hintObject.getAsJsonObject("photo").get("thumb").getAsString() : "";

                                    String tipo = "Desconocido";
                                    if (hintObject.has("tags") && hintObject.get("tags").isJsonObject()) {
                                        JsonObject tagsJson = hintObject.getAsJsonObject("tags");
                                        if (tagsJson.has("food_group") && !tagsJson.get("food_group").isJsonNull()) {
                                            int tipoComida = tagsJson.get("food_group").getAsInt();
                                            tipo = tipos.getOrDefault(tipoComida, "Otro");
                                        }
                                    }

                                    Food food = new Food(name, calories, proteinas, carbs, fats, foto, tipo);
                                    comidas.add(food);
                                }
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> adapter.actualizarLista(comidas));
                            }
                        } else {
                            Log.w(TAG, "La respuesta JSON no contiene un array 'foods'.");
                        }
                    } else {
                        Log.w(TAG, "La respuesta no es un objeto JSON principal.");
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
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e(TAG, "Error de red o fallo de la llamada: " + t.getMessage(), t);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}