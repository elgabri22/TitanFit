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
import com.example.titanfit.models.User;
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
        String fecha = getArguments() != null ? getArguments().getString("fecha", "Desconocido") : "Desconocido";
        User user= getArguments() != null ? (User) getArguments().getSerializable("user"): null;
        adapter.actualizaTipo(tipo,user,fecha);
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

        Call<JsonElement> call = apiService.getFoodsOpenFoodFactsByText(comida, 1); // 'query' es el texto de búsqueda, '1' para JSON

        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonElement rawResponse = response.body();
                    Log.d(TAG, "Respuesta Open Food Facts (texto): " + rawResponse.toString());

                    if (rawResponse.isJsonObject()) {
                        JsonObject jsonObject = rawResponse.getAsJsonObject();
                        // La búsqueda por texto de Open Food Facts devuelve un array "products"
                        if (jsonObject.has("products") && jsonObject.get("products").isJsonArray()) {
                            comidas.clear(); // Limpia la lista de comidas existente
                            JsonArray productsArray = jsonObject.getAsJsonArray("products");

                            if (productsArray.size() > 0) {
                                for (JsonElement productElement : productsArray) {
                                    if (productElement.isJsonObject()) {
                                        JsonObject productObject = productElement.getAsJsonObject();

                                        String name = productObject.has("product_name") && !productObject.get("product_name").isJsonNull()
                                                ? productObject.get("product_name").getAsString() : "Nombre desconocido";

                                        String imageUrl = productObject.has("image_url") && !productObject.get("image_url").isJsonNull()
                                                ? productObject.get("image_url").getAsString() : "";

                                        // Valores por 100g (nutriments)
                                        double calories = 0.0;
                                        double protein = 0.0;
                                        double carbs = 0.0;
                                        double fats = 0.0;

                                        if (productObject.has("nutriments") && productObject.get("nutriments").isJsonObject()) {
                                            JsonObject nutriments = productObject.getAsJsonObject("nutriments");

                                            calories = nutriments.has("energy-kcal_100g") && !nutriments.get("energy-kcal_100g").isJsonNull()
                                                    ? nutriments.get("energy-kcal_100g").getAsDouble() : 0.0;
                                            protein = nutriments.has("proteins_100g") && !nutriments.get("proteins_100g").isJsonNull()
                                                    ? nutriments.get("proteins_100g").getAsDouble() : 0.0;
                                            carbs = nutriments.has("carbohydrates_100g") && !nutriments.get("carbohydrates_100g").isJsonNull()
                                                    ? nutriments.get("carbohydrates_100g").getAsDouble() : 0.0;
                                            fats = nutriments.has("fat_100g") && !nutriments.get("fat_100g").isJsonNull()
                                                    ? nutriments.get("fat_100g").getAsDouble() : 0.0;
                                        }

                                        // Open Food Facts no tiene un "tipo" de alimento directo como Nutrionix.
                                        // Puedes intentar inferirlo de las categorías o tags si lo necesitas,
                                        // o simplemente dejarlo como "Otro" o "Desconocido".
                                        String tipo = "Otro"; // O un mapeo más complejo si lo desarrollas

                                        Food food = new Food(name, (int) Math.round(calories), protein, carbs, fats, imageUrl, tipo);
                                        comidas.add(food);
                                    }
                                }
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> adapter.actualizarLista(comidas));
                                }
                                if (comidas.isEmpty()) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), "No se encontraron resultados para su búsqueda.", Toast.LENGTH_SHORT).show());
                                    }
                                }
                            } else {
                                Log.w(TAG, "No se encontraron productos en la respuesta de Open Food Facts.");
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "No se encontraron resultados para su búsqueda.", Toast.LENGTH_SHORT).show());
                                }
                                comidas.clear(); // Asegúrate de limpiar la lista si no hay resultados
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> adapter.actualizarLista(comidas));
                                }
                            }
                        } else {
                            Log.w(TAG, "La respuesta JSON de Open Food Facts no contiene un array 'products'.");
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Error en el formato de la respuesta de la API.", Toast.LENGTH_SHORT).show());
                            }
                        }
                    } else {
                        Log.w(TAG, "La respuesta no es un objeto JSON principal.");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Respuesta inválida del servidor.", Toast.LENGTH_SHORT).show());
                        }
                    }
                } else {
                    Log.e(TAG, "La llamada a la API de Open Food Facts falló: " + response.code() + " - " + response.message());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Cuerpo de error: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer el cuerpo de error: " + e.getMessage());
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error en la llamada a la API: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e(TAG, "Error de red o fallo de la llamada a Open Food Facts: " + t.getMessage(), t);
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