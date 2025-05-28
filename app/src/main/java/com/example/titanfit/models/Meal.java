package com.example.titanfit.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Meal {

    private String id;
    private String name;
    private int calories;
    private double protein;
    private double carbs;
    private double fats;
    private String tipo;
    private String fecha;
    private double gramos;
    private String foto;
    @SerializedName("user")
    private User usuario;

    // Constructor vacío
    public Meal() {}

    public User getUsuario() {
        return usuario;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public Meal(String name, int calories, double protein, double carbs, double fats, String tipo, String fecha, double gramos, String foto, User user) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.tipo=tipo;
        this.usuario=user;
        this.fecha=fecha;
        this.gramos=gramos;
        this.foto=foto;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getGramos() {
        return gramos;
    }

    public void setGramos(double gramos) {
        this.gramos = gramos;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    // Getters y Setters


    @Override
    public String toString() {
        return "Meal{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", calories=" + calories +
                ", protein=" + protein +
                ", carbs=" + carbs +
                ", fats=" + fats +
                ", tipo='" + tipo + '\'' +
                ", fecha='" + fecha + '\'' +
                ", gramos=" + gramos +
                ", foto='" + foto + '\'' +
                ", usuario=" + usuario +
                '}';
    }
}
