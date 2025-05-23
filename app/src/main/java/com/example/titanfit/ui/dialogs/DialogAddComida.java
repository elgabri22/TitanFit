package com.example.titanfit.ui.dialogs;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.titanfit.R;
import com.example.titanfit.adapters.AdapterComida;
import com.example.titanfit.databinding.DialogAddComidaBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceFood;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.goals.GoalsFragment;
import com.google.gson.Gson;
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
    private EditText editTextSearch;
    private ImageButton imageButtonSearch;
    private RecyclerView recyclerViewComidas;
    private List<Food> comidas;
    private Map<Integer, String> tipos;
    private AdapterComida adapter;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private FragmentManager fragmentManager;

    public DialogAddComida(ArrayList<Food> foods, FragmentManager supportFragmentManager) {
        this.comidas=foods;
        this.fragmentManager=supportFragmentManager;
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        binding = DialogAddComidaBinding.inflate(inflater);
        View view = binding.getRoot();
        comidas = new ArrayList<>();

        // Inicializar el RecyclerView
        recyclerViewComidas = binding.recyclerViewComidas;
        recyclerViewComidas.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdapterComida(new ArrayList<Food>(),fragmentManager);
        recyclerViewComidas.setAdapter(adapter);

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

        // Configurar el EditText y el botón de búsqueda
        editTextSearch = binding.editTextSearch;
        imageButtonSearch = binding.imageButtonSearch;

        imageButtonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                busqueda(editTextSearch.getText().toString());
            }
        });

        // Configurar el TextWatcher para la búsqueda
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancelar búsqueda anterior si existe
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = new Runnable() {
                    @Override
                    public void run() {
                        busqueda(s.toString());
                    }
                };

                // Ejecutar después de 500ms de que el usuario deje de escribir
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
            adapter.notifyDataSetChanged();
            return;
        }

        // Limpiar la lista antes de la nueva búsqueda
        comidas.clear();
        adapter.notifyDataSetChanged();

        ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("query", comida);

        Log.d(TAG, "Enviando JSON Body: " + jsonObject.toString());

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
                    if (rawResponse != null) {
                        Log.d(TAG, "Respuesta completa: " + rawResponse.toString());

                        if (rawResponse.isJsonObject()) {
                            JsonObject jsonObject = rawResponse.getAsJsonObject();

                            if (jsonObject.has("foods") && jsonObject.get("foods").isJsonArray()) {

                                for (JsonElement hintElement : jsonObject.getAsJsonArray("foods")) {
                                    if (hintElement.isJsonObject()) {
                                        JsonObject hintObject = hintElement.getAsJsonObject();

                                        String name = hintObject.has("food_name") && !hintObject.get("food_name").isJsonNull() ? hintObject.get("food_name").getAsString() : "N/A";

                                        // Corregir el manejo de calorías para evitar errores con decimales
                                        int calories = 0;
                                        if (hintObject.has("nf_calories") && !hintObject.get("nf_calories").isJsonNull()) {
                                            try {
                                                calories = (int) Math.round(hintObject.get("nf_calories").getAsDouble());
                                            } catch (Exception e) {
                                                Log.w(TAG, "Error al convertir calorías: " + e.getMessage());
                                            }
                                        }

                                        double proteinas = hintObject.has("nf_protein") && !hintObject.get("nf_protein").isJsonNull() ? hintObject.get("nf_protein").getAsDouble() : 0.0;
                                        double carbs = hintObject.has("nf_total_carbohydrate") && !hintObject.get("nf_total_carbohydrate").isJsonNull() ? hintObject.get("nf_total_carbohydrate").getAsDouble() : 0.0;
                                        double fats = hintObject.has("nf_total_fat") && !hintObject.get("nf_total_fat").isJsonNull() ? hintObject.get("nf_total_fat").getAsDouble() : 0.0;
                                        String foto = "";
                                        String tipo = "Desconocido";

                                        if (hintObject.has("photo") && hintObject.get("photo").isJsonObject()) {
                                            JsonObject fotojson = hintObject.getAsJsonObject("photo");
                                            if (fotojson.has("thumb") && !fotojson.get("thumb").isJsonNull()) {
                                                foto = fotojson.get("thumb").getAsString();
                                            }
                                        }

                                        if (hintObject.has("tags") && hintObject.get("tags").isJsonObject()) {
                                            JsonObject tagsjson = hintObject.getAsJsonObject("tags");
                                            if (tagsjson.has("food_group") && !tagsjson.get("food_group").isJsonNull()) {
                                                int tipocomida = tagsjson.get("food_group").getAsInt();
                                                tipo = tipos.getOrDefault(tipocomida, "Otro");
                                            }
                                        }

                                        Food food = new Food(name, calories, proteinas, carbs, fats, foto, tipo);
                                        comidas.add(food);
                                    }
                                }
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.actualizarLista(comidas);
                                        }
                                    });
                                }

                            } else {
                                Log.d(TAG, "La respuesta JSON no contiene un array 'foods' o no es un array.");
                            }
                        } else {
                            Log.w(TAG, "La respuesta no es un objeto JSON principal.");
                        }
                    } else {
                        Log.e(TAG, "El cuerpo de la respuesta es nulo.");
                    }
                } else {
                    Log.e(TAG, "La llamada a la API falló: " + response.code() + " - " + response.message());
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            Log.e(TAG, "Cuerpo de error: " + errorBodyString);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer el cuerpo de error: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Error inesperado al procesar el cuerpo de error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e(TAG, "Error de red o fallo de la llamada: " + t.getMessage(), t);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Limpiar el handler para evitar memory leaks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}