package com.example.titanfit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.NavOptions; // ¡Asegúrate de tener esta importación!
import com.example.titanfit.R;
import com.example.titanfit.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

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
        // Una verificación extra para 'navController' por si acaso,
        // aunque con .post() debería ser menos probable que sea null.
        if (navController == null) {
            Log.e("MainActivity", "NavController es null en handleIntentNavigation (después de post). No se puede navegar.");
            return;
        }

        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            int destinationId = bundle.getInt(EXTRA_NAV_DESTINATION, 0);

            if (destinationId != 0 && destinationId != -1) {
                try {
                    // Evitar navegar si el destino ya es el destino actual
                    // Ahora sí es seguro llamar a getCurrentDestination() porque estamos en .post()
                    if (navController.getCurrentDestination() == null ||
                            navController.getCurrentDestination().getId() != destinationId) {

                        // Opciones de navegación para limpiar la pila al ir a Login o Home
                        NavOptions navOptions = null;
                        if (destinationId == R.id.login || destinationId == R.id.home) {
                            // Se puede usar try-catch aquí también para getGraph().getStartDestinationId()
                            // aunque es muy poco probable que falle si el NavController está inicializado.
                            try {
                                navOptions = new NavOptions.Builder()
                                        .setPopUpTo(navController.getGraph().getStartDestinationId(), true) // Pop up to start destination, inclusive
                                        .build();
                            } catch (IllegalStateException e) {
                                Log.e("MainActivity", "Error obteniendo el ID de startDestination: " + e.getMessage());
                                // Si hay un error aquí, la navegación se hará sin popUpTo
                            }
                        }

                        navController.navigate(destinationId, null, navOptions);
                        Log.d("MainActivity", "Navegando a destino ID: " + getResources().getResourceEntryName(destinationId));
                    } else {
                        Log.d("MainActivity", "Ya estamos en el destino " + getResources().getResourceEntryName(destinationId));
                    }
                } catch (IllegalArgumentException e) {
                    Log.e("MainActivity", "ID de destino de navegación no válido: " + destinationId, e);
                    // Esto ocurre si el ID no existe en tu nav_graph.xml
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
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                    || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}