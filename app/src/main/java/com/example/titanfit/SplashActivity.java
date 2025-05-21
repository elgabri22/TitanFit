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
    public static final int DESTINATION_HOME_FRAGMENT = R.id.home; // ID de tu LoginFragment en nav_graph

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        manager = new SharedPreferencesManager(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                int destinationId;

                if (manager.isLoggedIn()) {
                    // Si el usuario está logueado, ir al fragmento principal
                    destinationId = DESTINATION_MAIN_FRAGMENT;
                    Log.d("SplashActivity", "Usuario logueado. Navegando a MainFragment.");
                } else {
                    // Si el usuario NO está logueado, ir al fragmento de login
                    destinationId = DESTINATION_HOME_FRAGMENT;
                    Log.d("SplashActivity", "Usuario NO logueado. Navegando a LoginFragment.");
                }

                // Pasa el ID del destino a MainActivity
                intent.putExtra(EXTRA_NAV_DESTINATION, destinationId);
                startActivity(intent);

                // Finaliza SplashActivity para que el usuario no pueda volver a ella con el botón atrás
                finish();
            }
        }, 2000);
    }
}