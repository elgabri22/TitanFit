package com.example.titanfit.ui;

// SharedPreferencesManager.java (la misma clase que definimos antes)
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.titanfit.models.User;
import com.google.gson.Gson;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "MyAppPrefs";
    private static final String KEY_CURRENT_USER = "current_user";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveUser(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (user != null) {
            String userJson = gson.toJson(user);
            editor.putString(KEY_CURRENT_USER, userJson);
            Log.d("bien","usuario bien");
        } else {
            editor.remove(KEY_CURRENT_USER);
        }
        editor.apply();
    }

    public User getUser() {
        String userJson = sharedPreferences.getString(KEY_CURRENT_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public boolean isLoggedIn() {
        User currentUser = getUser();
        return currentUser != null;
    }

    public void clearUser() {
        saveUser(null);
    }
}
