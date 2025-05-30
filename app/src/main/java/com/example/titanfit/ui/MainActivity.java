package com.example.titanfit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.NavOptions; // ¡Asegúrate de tener esta importación!
import com.example.titanfit.R;
import com.example.titanfit.databinding.ActivityMainBinding;
import com.example.titanfit.models.Food;
import com.example.titanfit.models.User;
import com.example.titanfit.ui.dialogs.DialogAddComida;
import com.example.titanfit.ui.dialogs.DialogFavoritos;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;

    public static final String EXTRA_NAV_DESTINATION = "nav_destination";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);


        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.main)
                    .setOpenableLayout(drawer)
                    .build();



            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                    drawer.close();

                    // Maneja la selección del ítem del menú
                    int id = item.getItemId();

                    if (id == R.id.nav_cerrar_sesion) {
                        Log.d("MainActivity", "Cerrar sesión seleccionado.");
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.mobile_navigation, true)
                                .build();
                        navController.navigate(R.id.home, null, navOptions);
                        SharedPreferencesManager sharedPreferencesManager=new SharedPreferencesManager(getApplicationContext());
                        sharedPreferencesManager.clearUser();
                        Toast.makeText(getApplicationContext(),"Sesión cerrada correctamente",Toast.LENGTH_LONG).show();

                        return true;
                    } else {
                        return NavigationUI.onNavDestinationSelected(item, navController);
                    }
                }
            });
            // *******************************************************************

            binding.getRoot().post(() -> {
                Log.d("MainActivity", "Post-layout: Llamando a handleIntentNavigation.");
                handleIntentNavigation(getIntent());
            });

            binding.getRoot().post(() -> {
                Log.d("MainActivity", "Post-layout: Llamando a handleIntentNavigation.");
                handleIntentNavigation(getIntent());
            });

        } else {
            Log.e("MainActivity", "ERROR: NavHostFragment no encontrado. Revisa tu layout XML.");
        }
    }


    private void handleIntentNavigation(Intent intent) {
        if (navController == null) {
            Log.e("MainActivity", "NavController es null en handleIntentNavigation (después de post). No se puede navegar.");
            return;
        }

        SharedPreferencesManager manager = new SharedPreferencesManager(getApplicationContext());
        User user = manager.getUser();

        // Validamos si usuario o goals son null; si es así, limpiamos SharedPreferences
        if (user == null || user.getGoals() == null) {
            Log.w("MainActivity", "Usuario o goals null, limpiando SharedPreferences.");
            manager.clearUser();
        }

        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            int destinationId = bundle.getInt(EXTRA_NAV_DESTINATION, 0);

            // Validación: si quieren ir a main pero user o goals son null, redirigir a home (o login)
            if (destinationId == R.id.main) {
                user = manager.getUser();  // Volvemos a obtener después de limpiar, por si acaso
                if (user == null || user.getGoals() == null) {
                    Log.w("MainActivity", "Después de limpiar, usuario o goals siguen null, redirigiendo a home en vez de main");
                    destinationId = R.id.home;  // O a login, según tu flujo
                }
            }

            if (destinationId != 0 && destinationId != -1) {
                try {
                    if (navController.getCurrentDestination() == null ||
                            navController.getCurrentDestination().getId() != destinationId) {

                        NavOptions navOptions = null;
                        if (destinationId == R.id.login || destinationId == R.id.home) {
                            try {
                                navOptions = new NavOptions.Builder()
                                        .setPopUpTo(navController.getGraph().getStartDestinationId(), true)
                                        .build();
                            } catch (IllegalStateException e) {
                                Log.e("MainActivity", "Error obteniendo el ID de startDestination: " + e.getMessage());
                            }
                        }

                        navController.navigate(destinationId, null, navOptions);
                        Log.d("MainActivity", "Navegando a destino ID: " + getResources().getResourceEntryName(destinationId));
                    } else {
                        Log.d("MainActivity", "Ya estamos en el destino " + getResources().getResourceEntryName(destinationId));
                    }
                } catch (IllegalArgumentException e) {
                    Log.e("MainActivity", "ID de destino de navegación no válido: " + destinationId, e);
                }
            } else {
                Log.d("MainActivity", "No se encontró un ID de navegación válido en el Intent.");
            }
        } else {
            Log.d("MainActivity", "Intent o sus extras son nulos. El NavController cargará el startDestination por defecto.");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle hamburger menu (navigation drawer toggle)
        if (id == android.R.id.home) {
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout); // Replace with your DrawerLayout ID
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            } else {
                Log.e("MainActivity", "DrawerLayout is null");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}