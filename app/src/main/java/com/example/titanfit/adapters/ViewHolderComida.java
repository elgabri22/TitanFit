package com.example.titanfit.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.titanfit.databinding.ItemComidaBinding;
import com.example.titanfit.models.Food;

public class ViewHolderComida extends RecyclerView.ViewHolder {

    private final ItemComidaBinding binding;

    public ViewHolderComida(@NonNull View itemView) {
        super(itemView);
        // Inicializa el View Binding
        this.binding = ItemComidaBinding.bind(itemView);
    }

    public void renderize(Food comida) {
        String imagenSource = comida.getImagen();
        boolean isDrawableResource = !imagenSource.startsWith("http");

        if (isDrawableResource) {
            // Si la imagen es un recurso drawable, obtener su ID
            int imageId = itemView.getContext().getResources().getIdentifier(
                    imagenSource, "drawable", itemView.getContext().getPackageName()
            );

            if (imageId != 0) {
                Glide.with(itemView.getContext())
                        .load(imageId)
                        .centerCrop()
                        .into(binding.ivTrivia);
            } else {
                // Cargar imagen por defecto si el drawable no se encuentra
                Glide.with(itemView.getContext())
                        .load(R.drawable.bg_futbol) // Asegúrate de tener este drawable
                        .centerCrop()
                        .into(binding.ivTrivia);
            }
        } else {
            // Si la imagen es una URL, usarla directamente
            Glide.with(itemView.getContext())
                    .load(imagenSource)
                    .centerCrop()
                    .into(binding.ivTrivia);
        }

        //TODO meter los diferentes textview para mostrar cada comida filtrada
        setOnClickListeners(comida);
    }

    private void setOnClickListeners(final Food comida) {
        // Botón de editar
        if (binding.btnadd != null) { // Asegúrate de que el botón existe en el layout
            binding.btnadd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }
}