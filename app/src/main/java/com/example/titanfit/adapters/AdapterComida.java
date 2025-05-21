package com.example.proyecto.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.titanfit.adapters.ViewHolderComida;
import com.example.titanfit.models.Food;

import java.util.List;
import java.util.ArrayList;

public class AdapterComida extends RecyclerView.Adapter<ViewHolderComida> {

    private List<Food> listComidas;

    // Constructor del Adapter
    public AdapterComida(
            List<Food> listfoods) {
        this.listComidas = new ArrayList<>(listfoods);
    }

    @NonNull
    @Override
    public ViewHolderComida onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_partido, parent, false);
        // Pasamos los listeners al ViewHolder para que este los use al renderizar
        return new ViewHolderComida(view);
    }

    // Vinculamos los datos del 'Partido' en la posición actual con el ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ViewHolderComida holder, int position) {
        Food comida = listComidas.get(position);
        holder.renderize(comida); // Llamamos al método renderize del ViewHolder
    }

    // Devuelve la cantidad de elementos en la lista
    @Override
    public int getItemCount() {
        return listComidas.size();
    }

    public void actualizarLista(List<Food> nuevaLista) {
        this.listComidas.clear();
        this.listComidas.addAll(nuevaLista);
        notifyDataSetChanged(); // Notificar que los datos han cambiado para que el RecyclerView se redibuje
    }
}