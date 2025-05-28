package com.example.titanfit.models;

import java.util.ArrayList;
import java.util.List;

public class Favoritos {
    private List<Food>comidas;

    public Favoritos() {
        this.comidas = new ArrayList<>();
    }

    public List<Food> getComidas() {
        return comidas;
    }

    public void setComidas(List<Food> comidas) {
        this.comidas = comidas;
    }

    public void addComida(Food comida){
        this.comidas.add(comida);
    }

    public void removeComida(Food comida){
        this.comidas.remove(comida);
    }
}
