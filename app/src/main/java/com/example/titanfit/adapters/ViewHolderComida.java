package com.example.titanfit.adapters;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.titanfit.R;
import com.example.titanfit.databinding.ItemComidaBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.ui.dialogs.DialogComida;

public class ViewHolderComida extends RecyclerView.ViewHolder {

    private final ItemComidaBinding binding;
    private FragmentManager fragmentManager;
    private DialogComida.OnMealAddedListener mealAddedListener;

    public ViewHolderComida(@NonNull View itemView, FragmentManager fragmentManager, DialogComida.OnMealAddedListener listener) {
        super(itemView);
        this.binding = ItemComidaBinding.bind(itemView);
        this.fragmentManager = fragmentManager;
        this.mealAddedListener = listener;
    }

    public void renderize(Food comida, String tipo) {
        if (comida == null) {
            return;
        }

        binding.textViewNombreComida.setText(comida.getName() != null ? comida.getName() : "Sin nombre");
        binding.tipo.setText(comida.getTipo() != null ? comida.getTipo() : "Sin tipo");

        Glide.with(itemView.getContext())
                .load(comida.getImagen())
                .into(binding.imageViewComida);

        setOnClickListeners(comida, tipo);
    }

    private void setOnClickListeners(final Food comida, String tipo) {
        if (binding.btnadd != null) {
            binding.btnadd.setOnClickListener(v -> {
                if (fragmentManager != null && comida != null) {
                    // Dismiss existing DialogComida instances (but keep DialogAddComida)
                    for (Fragment fragment : fragmentManager.getFragments()) {
                        if (fragment instanceof DialogComida) {
                            DialogFragment dialogFragment = (DialogFragment) fragment;
                            if (dialogFragment.getDialog() != null && dialogFragment.getDialog().isShowing()) {
                                dialogFragment.dismiss();
                            }
                        }
                    }
                    DialogComida dialog = DialogComida.newInstance(comida, tipo);
                    dialog.setOnMealAddedListener(mealAddedListener);
                    dialog.show(fragmentManager, "DialogComida");
                } else {
                    android.util.Log.e("ViewHolderComida", "FragmentManager es nulo o comida es nula");
                }
            });
        }
    }
}