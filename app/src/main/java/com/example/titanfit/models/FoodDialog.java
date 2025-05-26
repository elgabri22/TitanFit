package com.example.titanfit.models;

public class FoodDialog {
    private static Meal food;
    private static boolean abierto;

    public static Meal getFood() {
        return food;
    }

    public static void metecomida(Meal comida) {
        food = comida;
        abierto = false;
    }

    public static boolean isAbierto() {
        return abierto;
    }

    public static void setAbierto(boolean abierto1) {
        abierto = abierto1;
    }
}