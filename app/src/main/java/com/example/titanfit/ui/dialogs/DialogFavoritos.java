package com.example.titanfit.ui.dialogs;

import static android.content.ContentValues.TAG;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.titanfit.R;
import com.example.titanfit.adapters.AdapterComida;
import com.example.titanfit.databinding.DialogAddComidaBinding;
import com.example.titanfit.databinding.DialogFavoritosBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceFood;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DialogFavoritos extends DialogFragment{
        private DialogFavoritosBinding binding;
        private List<Food> comidas;
        private Map<Integer, String> tipos;
        private AdapterComida adapter;
        private Handler searchHandler = new Handler();
        private Runnable searchRunnable;
        private FragmentManager fragmentManager;
        private DialogComida.OnMealAddedListener listener;

        public DialogFavoritos(List<Food> foods, FragmentManager supportFragmentManager,DialogComida.OnMealAddedListener listener) {
            this.comidas = foods != null ? foods : new ArrayList<>();
            this.fragmentManager = supportFragmentManager;
            this.listener=listener;
        }

    public void setListener(DialogComida.OnMealAddedListener listener) {
        this.listener = listener;
    }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            binding = DialogFavoritosBinding.inflate(getLayoutInflater());
            View view = binding.getRoot();

            String fecha=getArguments().getString("fecha");
            String tipo=getArguments().getString("tipo");

            addFoodsFavs(this.comidas,fecha);


            builder.setView(view);
            return builder.create();
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


    private void addFoodsFavs(List<Food> favoritos, String fecha) {
        Log.d("comidas", favoritos.toString());
        binding.containerFavoritos.removeAllViews(); // Limpiar vistas anteriores

        // --- Contenedor del selector de tipo ---
        LinearLayout headerLayout = new LinearLayout(requireContext());
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(16, 32, 16, 16);

        // Etiqueta
        TextView tipoLabel = new TextView(requireContext());
        tipoLabel.setText("Selecciona tipo de comida:");
        tipoLabel.setTextSize(16);
        tipoLabel.setTextColor(Color.DKGRAY);
        tipoLabel.setPadding(0, 20, 0, 70);

        // Spinner
        Spinner tipoSpinner = new Spinner(requireContext(), Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Desayuno", "Almuerzo")
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoSpinner.setAdapter(adapter);

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.setMargins(0, 0, 0, 24); // Más espacio debajo del spinner
        tipoSpinner.setLayoutParams(spinnerParams);

        headerLayout.addView(tipoLabel);
        headerLayout.addView(tipoSpinner);
        binding.containerFavoritos.addView(headerLayout);

        // --- Lista de comidas favoritas ---
        for (int i = 0; i < favoritos.size(); i++) {
            Food food = favoritos.get(i);
            Log.d("food", food.toString());

            // Tarjeta contenedora
            CardView cardView = new CardView(requireContext());
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT,
                    CardView.LayoutParams.WRAP_CONTENT
            );

            if (i == 0) {
                cardParams.setMargins(16, 32, 16, 32); // Más espacio para la primera
            } else {
                cardParams.setMargins(16, 24, 16, 8); // Espaciado entre tarjetas
            }

            cardView.setLayoutParams(cardParams);
            cardView.setRadius(24);
            cardView.setCardElevation(6);
            cardView.setUseCompatPadding(true);

            // Contenido de la tarjeta
            LinearLayout itemLayout = new LinearLayout(requireContext());
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(24, 24, 24, 24);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);

            // Imagen
            ImageView imageView = new ImageView(requireContext());
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(120, 120);
            imgParams.setMarginEnd(24);
            imageView.setLayoutParams(imgParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            String image = food.getImagen();
            if (image != null && !image.isEmpty()) {
                try {
                    if (image.matches("\\d+")) {
                        Glide.with(requireContext())
                                .load(Integer.parseInt(image))
                                .into(imageView);
                    } else {
                        Glide.with(requireContext())
                                .load(image)
                                .into(imageView);
                    }
                } catch (NumberFormatException e) {
                    Log.e("DialogFavoritos", "Invalid image ID: " + image, e);
                }
            }

            // Contenedor de texto
            LinearLayout textContainer = new LinearLayout(requireContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            textContainer.setLayoutParams(textParams);

            TextView nombre = new TextView(requireContext());
            nombre.setText(food.getName() != null ? food.getName() : "Nombre desconocido");
            nombre.setTextSize(18);
            nombre.setTypeface(null, Typeface.BOLD);
            nombre.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

            TextView calorias = new TextView(requireContext());
            calorias.setText(Math.round(food.getCalories()) + " kcal");
            calorias.setTextSize(14);
            calorias.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            calorias.setPadding(0, 8, 0, 0);

            textContainer.addView(nombre);
            textContainer.addView(calorias);

            // Botón "+"
            Button plusButton = new Button(requireContext());
            plusButton.setText("+");
            plusButton.setTextSize(22);
            plusButton.setTextColor(Color.WHITE);
            plusButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.teal_700)));
            plusButton.setTypeface(null, Typeface.BOLD);
            plusButton.setElevation(6);
            plusButton.setPadding(24, 12, 24, 12);
            plusButton.setAllCaps(false);

            plusButton.setOnClickListener(v -> {
                if (!isAdded() || getActivity() == null) {
                    Log.e("DialogFavoritos", "Fragment not attached to activity");
                    return;
                }

                String tipoSeleccionado = tipoSpinner.getSelectedItem().toString();
                if (fecha == null || tipoSeleccionado == null) {
                    Toast.makeText(requireContext(), "Fecha o tipo no disponibles", Toast.LENGTH_SHORT).show();
                    return;
                }

                DialogComida dialogComida = DialogComida.newInstance(food, tipoSeleccionado, fecha);
                dialogComida.setOnMealAddedListener(this.listener);
                dialogComida.show(fragmentManager, "DialogComida");
            });

            // Agregar elementos al layout
            itemLayout.addView(imageView);
            itemLayout.addView(textContainer);
            itemLayout.addView(plusButton);

            cardView.addView(itemLayout);
            binding.containerFavoritos.addView(cardView);
        }
    }





}
