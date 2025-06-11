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
    @GET("https://world.openfoodfacts.org/cgi/search.pl") // Este es el endpoint de búsqueda general de Open Food Facts
    Call<JsonElement> getFoodsOpenFoodFactsByText(
            @Query("search_terms") String query, // El término de búsqueda
            @Query("json") int jsonFormat // Para que la respuesta sea JSON (valor 1)
    );


    @POST("/delete/meal/{id}")
    Call<Void> deleteMeal(@Path ("id")String id);

    @GET("https://world.openfoodfacts.org/api/v0/product/{barcode}.json")
    Call<JsonElement> getProductByBarcode(
            @Path("barcode") String barcode
    );

    @POST("/add/meal")
    Call<JsonElement> addMeal(@Body RequestBody body);

}
