package com.example.titanfit.network;

import com.example.titanfit.models.Food;
import com.example.titanfit.models.User;
import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiServiceFood {
    @POST("https://trackapi.nutritionix.com/v2/natural/nutrients")
    Call<JsonElement> getFoods(@Header("x-app-id") String appId,
                               @Header("x-app-key") String appKey,
                               @Body RequestBody body);

}
