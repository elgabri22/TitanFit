package com.example.titanfit.network;

import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;

import java.util.List;
import java.util.Objects;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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

    @GET("meals/{fecha}/{id}")
    Call<List<Meal>> getMeals(@Path("fecha") String fecha,@Path("id") String id);

    @POST("auth/generateToken")
    Call<ResponseBody> generateToken(@Body User user);
    @POST("api/update")
    Call<Void> updateUser(@Body RequestBody user);

    @POST("/api/delete/{id}")
    Call<Void> deleteUser(@Path("id")String id);

    @GET("/meals/{fecha_inicio}/{fecha_fin}/{id_user}")
    Call<List<Meal>> getMealsWeek(@Path("fecha_inicio") String fecha_inicio,@Path("fecha_fin") String fecha_fin,@Path("id_user") String id_user);
}
