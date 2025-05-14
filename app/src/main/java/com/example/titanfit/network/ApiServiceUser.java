package com.example.titanfit.network;

import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiServiceUser {
    @POST("api/user")
    Call<User> addUser(@Body User user);

    @GET("api/users/{id}")
    Call<User> getUser(@Path("id") String id);

    @GET("meals/{fecha}")
    Call<List<Meal>> getMeals(@Path("fecha") String fecha);
}
