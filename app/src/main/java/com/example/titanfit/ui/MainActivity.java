package com.example.titanfit.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.NavOptions;
import com.example.titanfit.R;
import com.example.titanfit.databinding.ActivityMainBinding;
import com.example.titanfit.models.User;
import com.google.android.material.navigation.NavigationView;

import java.util.Collections; // Para Collections.singleton
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private User user;
    private DrawerLayout drawer;
    private final Set<Integer> destinationsWithActiveDrawer = Set.of(
            R.id.main,
            R.id.stat,
            R.id.modifica
    );

    public static final String EXTRA_NAV_DESTINATION = "nav_destination";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(this);
        user = sharedPreferencesManager.getUser();

        drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Configura AppBarConfiguration para que solo R.id.main sea un destino "top-level"
            mAppBarConfiguration = new AppBarConfiguration.Builder(destinationsWithActiveDrawer)
                    .setOpenableLayout(drawer)
                    .build();

            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            // *** REFUERZO DEL BLOQUEO INICIAL DEL DRAWER ***
            // Bloquea el drawer por defecto, especialmente si el startDestination no es R.id.main
            if (!destinationsWithActiveDrawer.contains(navController.getGraph().getStartDestinationId())) {
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                Log.d("DrawerControl", "Drawer bloqueado al inicio (startDestination no es main).");
            }
            // ************************************************

            // Añade un listener para controlar el modo de bloqueo del Drawer al cambiar de destino.
            navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                @SuppressLint("RestrictedApi")
                @Override
                public void onDestinationChanged(@NonNull NavController controller,
                                                 @NonNull androidx.navigation.NavDestination destination,
                                                 @androidx.annotation.Nullable Bundle arguments) {
                    if (destinationsWithActiveDrawer.contains(destination.getId())) {
                        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                        Log.d("DrawerControl", "Drawer UNLOCKED para: " + destination.getDisplayName() + " (ID: " + destination.getId() + ")");
                    } else {
                        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                        Log.d("DrawerControl", "Drawer LOCKED_CLOSED para: " + destination.getDisplayName() + " (ID: " + destination.getId() + ")");
                    }

                    if (destination.getId() == R.id.home) {
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    } else {
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    }
                }
            });

            navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    drawer.close();

                    int id = item.getItemId();

                    if (id == R.id.nav_cerrar_sesion) {
                        Log.d("MainActivity", "Cerrar sesión seleccionado.");
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.mobile_navigation, true)
                                .build();
                        navController.navigate(R.id.home, null, navOptions);
                        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(getApplicationContext());
                        sharedPreferencesManager.clearUser();
                        Toast.makeText(getApplicationContext(), "Sesión cerrada correctamente", Toast.LENGTH_LONG).show();
                        return true;
                    } else {
                        return NavigationUI.onNavDestinationSelected(item, navController);
                    }
                }
            });

            binding.getRoot().post(() -> {
                Log.d("MainActivity", "Post-layout: Llamando a handleIntentNavigation.");
                handleIntentNavigation(getIntent());
            });

            View headerView = navigationView.getHeaderView(0);
            TextView usernameTextView = headerView.findViewById(R.id.username);
            TextView emailTextView = headerView.findViewById(R.id.textView);

            if (user != null) {
                usernameTextView.setText(user.getName());
                emailTextView.setText(user.getEmail());
            }

        } else {
            Log.e("MainActivity", "ERROR: NavHostFragment no encontrado. Revisa tu layout XML.");
        }
    }

    private void handleIntentNavigation(Intent intent) {
        if (navController == null) {
            Log.e("MainActivity", "NavController es null en handleIntentNavigation. No se puede navegar.");
            return;
        }

        SharedPreferencesManager manager = new SharedPreferencesManager(getApplicationContext());
        User user = manager.getUser();

        if (user == null || user.getGoals() == null) {
            Log.w("MainActivity", "Usuario o goals null, limpiando SharedPreferences.");
            manager.clearUser();
        }

        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            int destinationId = bundle.getInt(EXTRA_NAV_DESTINATION, 0);

            if (destinationId == R.id.main) {
                user = manager.getUser();
                if (user == null || user.getGoals() == null) {
                    Log.w("MainActivity", "Después de limpiar, usuario o goals siguen null, redirigiendo a home en vez de main");
                    destinationId = R.id.home;
                }
            }

            if (destinationId != 0 && destinationId != -1) {
                try {
                    // Solo navega si no estás ya en el destino
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

        if (id == android.R.id.home) {
            if (drawer != null) {
                // Solo abre el drawer si está UNLOCKED (permitido)
                if (drawer.getDrawerLockMode(GravityCompat.START) == DrawerLayout.LOCK_MODE_UNLOCKED) {
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        drawer.closeDrawer(GravityCompat.START);
                    } else {
                        drawer.openDrawer(GravityCompat.START);
                    }
                } else {
                    // Si el drawer está bloqueado, el botón home debería actuar como "atrás"
                    // NavigationUI.navigateUp ya lo maneja por onSupportNavigateUp
                    // o puedes forzar un onBackPressed() si no quieres depender de NavigationUI aquí
                    onBackPressed();
                }
            } else {
                Log.e("MainActivity", "DrawerLayout es null en onOptionsItemSelected");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Este método es llamado cuando se pulsa el botón de "atrás" de la Toolbar
        // o el icono de hamburguesa (si es un destino top-level para NavigationUI).
        // NavigationUI se encarga de la navegación hacia atrás o de abrir el drawer.
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}