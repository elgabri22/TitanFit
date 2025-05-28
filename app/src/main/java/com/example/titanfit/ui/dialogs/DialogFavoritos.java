package com.example.titanfit.ui.dialogs;

import static android.content.ContentValues.TAG;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

            addFoodsFavs(this.comidas,fecha,tipo);


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


    private void addFoodsFavs(List<Food> favoritos, String fecha, String tipo) {
        binding.containerFavoritos.removeAllViews(); // Clear previous views

        for (Food food : favoritos) {
            // Create horizontal layout for the item
            Log.d("food",food.toString());
            LinearLayout itemLayout = new LinearLayout(requireContext());
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            itemParams.setMargins(0, 0, 0, 12);
            itemLayout.setLayoutParams(itemParams);
            itemLayout.setPadding(16, 16, 16, 16);
            itemLayout.setClickable(true);
            itemLayout.setFocusable(true);

            // ImageView for the food image
            ImageView imageView = new ImageView(requireContext());
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(100, 100);
            imgParams.setMarginEnd(12);
            imageView.setLayoutParams(imgParams);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackgroundColor(Color.LTGRAY); // Debug visibility

            // Load image with Glide
            String image = food.getImagen();
            if (image != null && !image.isEmpty()) {
                try {

                    if (image.matches("\\d+")) {
                        Glide.with(requireContext())
                                .load(Integer.parseInt(image))
                                .into(imageView);
                        Log.d("DialogFavoritos", "Loading drawable ID: " + image + " for food: " + food.getName());
                    } else { // Handle URL
                        Glide.with(requireContext())
                                .load(image)
                                .into(imageView);
                        Log.d("DialogFavoritos", "Loading image URL: " + image + " for food: " + food.getName());
                    }
                } catch (NumberFormatException e) {
                    Log.e("DialogFavoritos", "Invalid drawable ID for food: " + food.getName(), e);
                }
            }

            // Vertical container for text
            LinearLayout textContainer = new LinearLayout(requireContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textContainer.setLayoutParams(textParams);

            // Food name
            TextView nombre = new TextView(requireContext());
            nombre.setText(food.getName() != null ? food.getName() : "Unknown Food");
            nombre.setTextSize(18);
            nombre.setTextColor(Color.BLACK);
            nombre.setTypeface(null, Typeface.BOLD);

            // Calories
            TextView calorias = new TextView(requireContext());
            calorias.setText(Math.round(food.getCalories()) + " kcal");
            calorias.setTextSize(14);
            calorias.setTextColor(Color.DKGRAY);
            LinearLayout.LayoutParams calParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            calParams.topMargin = 4;
            calorias.setLayoutParams(calParams);

            // Add texts to vertical container
            textContainer.addView(nombre);
            textContainer.addView(calorias);

            // Add image and texts to item layout
            itemLayout.addView(imageView);
            itemLayout.addView(textContainer);

            // Plus button
            Button plusButton = new Button(requireContext());
            plusButton.setText("+");
            plusButton.setTextSize(24);
            plusButton.setTextColor(Color.WHITE);
            plusButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.teal_700));
            plusButton.setPadding(12, 8, 12, 8);

            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(requireContext(), R.color.teal_700));
            background.setCornerRadius(100);
            plusButton.setBackground(background);

            // Plus button click listener to open DialogComida
            plusButton.setOnClickListener(v -> {
                if (getActivity() == null || !isAdded()) {
                    Log.e("DialogFavoritos", "Fragment not attached to activity");
                    return;
                }
                if (fecha == null || tipo == null) {
                    Toast.makeText(requireContext(), "Fecha o tipo no disponibles", Toast.LENGTH_SHORT).show();
                    Log.e("DialogFavoritos", "Fecha or tipo is null");
                    return;
                }
                DialogComida dialogComida = DialogComida.newInstance(food, tipo, fecha);
                dialogComida.setOnMealAddedListener(this.listener);
                dialogComida.show(fragmentManager, "DialogComida");
            });

            itemLayout.addView(plusButton);
            binding.containerFavoritos.addView(itemLayout);
        }
    }

}
