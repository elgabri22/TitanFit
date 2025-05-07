package com.example.titanfit.network;

import com.example.titanfit.models.Food;
import com.example.titanfit.models.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiServiceFood {
    @GET("v2/search/instant")
    Call<String> getFoods(@Header("x-app-id") String appId,
                          @Header("x-app-key") String appKey,
                          @Path("query") String query);

    // Endpoint para obtener detalles nutricionales de un alimento específico (POST)
    @POST("v2/natural/nutrients")
    Call<String> getFoodDetails(@Header("x-app-id") String appId,
                                @Header("x-app-key") String appKey,
                                @Body Map<String, String> body);
}
