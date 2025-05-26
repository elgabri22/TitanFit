package com.example.titanfit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.titanfit.R;
import com.example.titanfit.models.Food;
import com.example.titanfit.ui.dialogs.DialogComida;
import java.util.ArrayList;
import java.util.List;

public class AdapterComida extends RecyclerView.Adapter<ViewHolderComida> {

    private List<Food> listComidas;
    private FragmentManager fragmentManager;
    private String tipo = "Desconocido";
    private DialogComida.OnMealAddedListener mealAddedListener;

    public AdapterComida(List<Food> listfoods, FragmentManager fragmentManager, DialogComida.OnMealAddedListener listener) {
        this.listComidas = (listfoods != null) ? new ArrayList<>(listfoods) : new ArrayList<>();
        this.fragmentManager = fragmentManager;
        this.mealAddedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolderComida onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comida, parent, false);
        return new ViewHolderComida(view, fragmentManager, mealAddedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderComida holder, int position) {
        Food comida = listComidas.get(position);
        holder.renderize(comida, tipo);
    }

    @Override
    public int getItemCount() {
        return listComidas.size();
    }

    public void actualizarLista(List<Food> nuevaLista) {
        this.listComidas.clear();
        if (nuevaLista != null) {
            this.listComidas.addAll(nuevaLista);
        }
        notifyDataSetChanged();
    }

    public void actualizaTipo(String tipo) {
        this.tipo = (tipo != null) ? tipo : "Desconocido";
        notifyDataSetChanged();
    }
}