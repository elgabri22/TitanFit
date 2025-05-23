package com.example.titanfit.adapters;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.titanfit.R;
import com.example.titanfit.databinding.ItemComidaBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.ui.dialogs.DialogAddComida;
import com.example.titanfit.ui.dialogs.DialogComida;

public class ViewHolderComida extends RecyclerView.ViewHolder {

    private final ItemComidaBinding binding;
    private FragmentManager fragmentManager;

    public ViewHolderComida(@NonNull View itemView, FragmentManager fragmentManager) {
        super(itemView);
        this.binding = ItemComidaBinding.bind(itemView);
        this.fragmentManager = fragmentManager;
    }

    public void renderize(Food comida) {
        if (comida == null) {
            return; // Evita excepciones si comida es null
        }

        // Configura las vistas con manejo de null
        binding.textViewNombreComida.setText(comida.getName() != null ? comida.getName() : "Sin nombre");
        binding.tipo.setText(comida.getTipo() != null ? comida.getTipo() : "Sin tipo");

        // Carga la imagen con Glide, con manejo de errores
        Glide.with(itemView.getContext())
                .load(comida.getImagen())
                .into(binding.imageViewComida);

        setOnClickListeners(comida);
    }

    private void setOnClickListeners(final Food comida) {
        if (binding.btnadd != null) {
            binding.btnadd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragmentManager != null && comida != null) {
                        DialogComida dialog = DialogComida.newInstance(comida);
                        dialog.show(fragmentManager, "DialogAddComida");
                    } else {
                        android.util.Log.e("ViewHolderComida", "FragmentManager es nulo o comida es nula");
                    }
                }
            });
        }
    }
}