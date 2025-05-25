package com.example.titanfit.models;

public class FoodDialog {
    private static Meal food;

    public FoodDialog(Meal food) {
        this.food = food;
    }

    public static Meal getFood() {
        return food;
    }

    public void setFood(Meal food) {
        this.food = food;
    }

    public static void  metecomida(Meal comida){
        food=comida;
    }

    @Override
    public String toString() {
        return "FoodDialog{" +
                "food=" + food +
                '}';
    }
}
