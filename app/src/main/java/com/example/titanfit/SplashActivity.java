package com.example.titanfit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.titanfit.ui.MainActivity;
import com.example.titanfit.ui.SharedPreferencesManager;

public class SplashActivity extends AppCompatActivity {
    private SharedPreferencesManager manager;
    public static final String EXTRA_NAV_DESTINATION = "nav_destination";
    public static final int DESTINATION_MAIN_FRAGMENT = R.id.main; // ID de tu MainFragment en nav_graph
    public static final int DESTINATION_LOGIN_FRAGMENT = R.id.login; // ID de tu LoginFragment en nav_graph

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        manager = new SharedPreferencesManager(this);
        if (manager.isLoggedIn()){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (manager.isLoggedIn()) {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        intent.putExtra(EXTRA_NAV_DESTINATION, DESTINATION_MAIN_FRAGMENT);
                        Log.d("si","si");
                        startActivity(intent);
                    }else{
                        Intent intent2 = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent2);
                    }

                    finish();
                }
            }, 2000);
        }
    }
}