package com.example.titanfit.network;

import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiServiceUser {
    @POST("api/user")
    Call<User> addUser(@Body User user);

    @GET("api/user/{email}")
    Call<User> getUser(@Path("email") String email);

    @GET("meals/{fecha}")
    Call<List<Meal>> getMeals(@Path("fecha") String fecha);

    @POST("auth/generateToken")
    Call<String> generateToken(@Body User user);
    @POST("user/update")
    Call<User> updateUser(@Body User user);
}
