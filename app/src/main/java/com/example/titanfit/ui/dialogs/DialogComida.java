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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.titanfit.R;
import com.example.titanfit.databinding.DialogComidaBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceFood;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DialogComida extends DialogFragment {
    private static final String ARG_COMIDA = "comida";
    private static final String ARG_TIPO = "tipo";
    private static final String TAG = "DialogComida";
    private Food comida;
    private DialogComidaBinding binding;
    private double baseCalories, baseCarbs, baseProteins, baseFats;
    private OnMealAddedListener mealAddedListener;
    private boolean added = false;

    public void setOnMealAddedListener(OnMealAddedListener listener) {
        this.mealAddedListener = listener;
    }

    public static DialogComida newInstance(Food comida, String tipo, String fecha) {
        DialogComida dialog = new DialogComida();
        Bundle args = new Bundle();
        args.putSerializable(ARG_COMIDA, comida);
        args.putString(ARG_TIPO, tipo);
        args.putString("fecha", fecha);
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

    public interface OnMealAddedListener {
        void onMealAdded(Meal meal,String tipo);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogComidaBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.textViewTitle.setText(comida != null && comida.getName() != null ? comida.getName() : "Alimento");
        binding.editTextGrams.setText("100");

        SharedPreferencesManager manager = new SharedPreferencesManager(requireContext());
        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
        Call<User> call = apiService.getUser(manager.getUser().getEmail());
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    manager.saveUser(user);
                    List<Food> comidasfavoritos = user.getFavoritos().getComidas();
                    Log.d(TAG, "Tamaño de comidasfavoritos: " + comidasfavoritos.size());
                    added = false;
                    if (comida != null && comida.getName() != null) {
                        for (Food food : comidasfavoritos) {
                            if (food != null && food.getName() != null &&
                                    food.getName().trim().equalsIgnoreCase(comida.getName().trim())) {
                                Log.d(TAG, "Comida encontrada en favoritos: " + food.getName());
                                Glide.with(requireContext())
                                        .load(R.drawable.added)
                                        .into(binding.buttonFavorite);
                                added = true;
                                break;
                            }
                        }
                    }
                    if (!added) {
                        Log.d(TAG, "Comida no está en favoritos");
                        Glide.with(requireContext())
                                .load(R.drawable.estrella)
                                .into(binding.buttonFavorite);
                    }
                } else {
                    Log.e(TAG, "Error al obtener usuario: Código " + response.code());
                    loadLocalFavorites(manager);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "Error de red al obtener usuario: " + t.getMessage());
                loadLocalFavorites(manager);
            }
        });

        if (comida != null && comida.getName() != null) {
            busqueda(comida.getName(), 100);
        } else {
            Toast.makeText(getContext(), "Nombre del alimento no válido", Toast.LENGTH_SHORT).show();
        }

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
                        if (comida != null && comida.getName() != null) {
                            busqueda(comida.getName(), grams);
                        }
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Entrada no válida para gramos: " + s.toString());
                        updateMacros(0, 0, 0, 0);
                    }
                } else {
                    updateMacros(0, 0, 0, 0);
                }
            }
        });

        binding.buttonAdd.setOnClickListener(v -> {
            String gramosStr = binding.editTextGrams.getText().toString();
            double gramos = Double.parseDouble(gramosStr);
            if (gramos<=0){
                Toast.makeText(requireContext(),"Debe de ser mayor a 0 gramos",Toast.LENGTH_LONG).show();
            }else{
                String tipo = getArguments() != null ? getArguments().getString(ARG_TIPO, "Desconocido") : "Desconocido";

                SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(requireContext());
                Log.d("user", sharedPreferencesManager.getUser().toString());

                String fecha = (String) getArguments().get("fecha");
                Meal meal = new Meal(comida.getName(), (int) Math.round((baseCalories/100)*gramos), (int) Math.round((baseProteins/100)*gramos), (int) Math.round((baseCarbs/100)*gramos), (int) Math.round((baseFats/100)*gramos), tipo, fecha, gramos, comida.getImagen(), sharedPreferencesManager.getUser());

                if (mealAddedListener != null) {
                    mealAddedListener.onMealAdded(meal, tipo);
                }

                dismiss();

                Fragment parentDialog = getParentFragmentManager().findFragmentByTag("DialogAddComida");
                if (parentDialog instanceof DialogFragment) {
                    ((DialogFragment) parentDialog).dismiss();
                    Log.d(TAG, "Dismissed DialogAddComida");
                }
            }

        });

        binding.buttonFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferencesManager manager = new SharedPreferencesManager(requireContext());
                User user = manager.getUser();
                boolean wasAdded = added;

                if (!added) {
                    user.getFavoritos().addComida(comida);
                    added = true;
                } else {
                    user.getFavoritos().removeComida(comida);
                    added = false;
                }

                String jsonPayload = new Gson().toJson(user);
                Log.d("API_REQUEST", "User JSON: " + jsonPayload);

                RequestBody requestBody = RequestBody.create(
                        MediaType.parse("application/json"),
                        jsonPayload
                );

                ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
                Call<Void> call = apiService.updateUser(requestBody);
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            manager.saveUser(user);
                            Glide.with(requireContext())
                                    .load(added ? R.drawable.added : R.drawable.estrella)
                                    .into(binding.buttonFavorite);
                            Toast.makeText(requireContext(), added ? "Añadido a favoritos" : "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                        } else {
                            added = wasAdded;
                            if (!added) {
                                user.getFavoritos().removeComida(comida);
                            } else {
                                user.getFavoritos().addComida(comida);
                            }
                            String errorMessage = "Error al actualizar usuario. Código: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    errorMessage += ", Error: " + response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e("API_ERROR", "Error al leer cuerpo de error: " + e.getMessage());
                            }
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                            Log.e("API_ERROR", errorMessage);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        added = wasAdded;
                        if (!added) {
                            user.getFavoritos().removeComida(comida);
                        } else {
                            user.getFavoritos().addComida(comida);
                        }
                        Toast.makeText(requireContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("API_FAILURE", "Error: ", t);
                    }
                });
            }
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

    private void busqueda(String foodName, double grams) {
        ApiServiceFood apiService = ApiClient.getClient().create(ApiServiceFood.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("query", foodName);

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
                            JsonArray foodsArray = jsonObject.getAsJsonArray("foods");
                            if (foodsArray.size() > 0) {
                                JsonObject foodObject = foodsArray.get(0).getAsJsonObject();
                                baseCalories = foodObject.has("nf_calories") && !foodObject.get("nf_calories").isJsonNull()
                                        ? foodObject.get("nf_calories").getAsDouble() : 0.0;
                                baseCarbs = foodObject.has("nf_total_carbohydrate") && !foodObject.get("nf_total_carbohydrate").isJsonNull()
                                        ? foodObject.get("nf_total_carbohydrate").getAsDouble() : 0.0;
                                baseProteins = foodObject.has("nf_protein") && !foodObject.get("nf_protein").isJsonNull()
                                        ? foodObject.get("nf_protein").getAsDouble() : 0.0;
                                baseFats = foodObject.has("nf_total_fat") && !foodObject.get("nf_total_fat").isJsonNull()
                                        ? foodObject.get("nf_total_fat").getAsDouble() : 0.0;

                                calculateForGrams(grams);
                            } else {
                                Log.w(TAG, "No se encontraron alimentos en la respuesta.");
                                updateMacros(0, 0, 0, 0);
                                showError("No se encontraron datos para este alimento.");
                            }
                        } else {
                            Log.w(TAG, "La respuesta JSON no contiene un array 'foods'.");
                            updateMacros(0, 0, 0, 0);
                            showError("Formato de respuesta inválido.");
                        }
                    } else {
                        Log.w(TAG, "La respuesta no es un objeto JSON principal.");
                        updateMacros(0, 0, 0, 0);
                        showError("Respuesta inválida del servidor.");
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
                    showError("Error en la llamada a la API: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e(TAG, "Error de red o fallo de la llamada: " + t.getMessage(), t);
                showError("Error de conexión: " + t.getMessage());
                updateMacros(0, 0, 0, 0);
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
                binding.textViewCarbs.setText(String.format(Locale.getDefault(), "%.1f g", carbs));
                binding.textViewProteins.setText(String.format(Locale.getDefault(), "%.1f g", proteins));
                binding.textViewFats.setText(String.format(Locale.getDefault(), "%.1f g", fats));
                binding.textViewCalories.setText(String.format(Locale.getDefault(), "%.1f kcal", calories));
            });
        }
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadLocalFavorites(SharedPreferencesManager manager) {
        User user = manager.getUser();
        List<Food> comidasfavoritos = user.getFavoritos().getComidas();
        Log.d(TAG, "Tamaño de comidasfavoritos (local): " + comidasfavoritos.size());
        added = false;
        if (comida != null && comida.getName() != null) {
            for (Food food : comidasfavoritos) {
                if (food != null && food.getName() != null &&
                        food.getName().trim().equalsIgnoreCase(comida.getName().trim())) {
                    Log.d(TAG, "Comida encontrada en favoritos (local): " + food.getName());
                    Glide.with(requireContext())
                            .load(R.drawable.added)
                            .into(binding.buttonFavorite);
                    added = true;
                    break;
                }
            }
        }
        if (!added) {
            Log.d(TAG, "Comida no está en favoritos (local)");
            Glide.with(requireContext())
                    .load(R.drawable.estrella)
                    .into(binding.buttonFavorite);
        }
    }
}