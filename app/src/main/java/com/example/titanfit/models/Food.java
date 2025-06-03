package com.example.titanfit.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class Food implements Serializable {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    private int calories;
    private double protein;
    private double carbs;
    private double fats;
    @SerializedName("imagen")
    private String imagen;
    private String tipo;

    // Empty Constructor
    public Food() {}

    public Food(String name, int calories, double protein, double carbs, double fats,String imagen,String tipo) {
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.imagen=imagen;
        this.tipo=tipo;
    }

    //Getters and setters


    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }

    public double getFats() {
        return fats;
    }

    public void setFats(double fats) {
        this.fats = fats;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Food food = (Food) o;
        if (id != null && food.id != null) {
            return id.equals(food.id);
        }
        return name != null && food.name != null &&
                name.trim().equalsIgnoreCase(food.name.trim());
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return name != null ? name.toLowerCase().trim().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Food{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", calories=" + calories +
                ", protein=" + protein +
                ", carbs=" + carbs +
                ", fats=" + fats +
                ", imagen='" + imagen + '\'' +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}
