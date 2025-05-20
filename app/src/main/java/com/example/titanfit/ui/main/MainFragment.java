package com.example.titanfit.ui.main;

import static android.content.ContentValues.TAG;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentHomeBinding;
import com.example.titanfit.databinding.FragmentMainBinding;
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.MainActivity;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.example.titanfit.ui.dialogs.DialogAddComida;
import com.example.titanfit.ui.goals.GoalsFragment;
import com.example.titanfit.ui.home.HomeFragment;
import com.example.titanfit.ui.home.HomeViewModel;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener{

    private MainViewModel mViewModel;
    private FragmentMainBinding binding;
    private Calendar calendar;
    private SharedPreferencesManager manager;
    private Context context;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        MainViewModel mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        binding = FragmentMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context=requireContext();
        Bundle args = getArguments();
        if (args != null) {
            User user = (User) args.getSerializable("user");
            if (user != null) {
                binding.tvCaloriesLabel.setText(0 + "/" + user.getGoals().getDailyCalories()+"kcal");
                binding.proteinas.setText("Proteinas: " + 0 + "/" + Math.round(user.getGoals().getProteinPercentage())+"g");
                binding.carbohidratos.setText("Carbohidratos: " + 0 + "/" + Math.round(user.getGoals().getCarbsPercentage())+"g");
                binding.grasas.setText("Grasas: " + 0 + "/" + Math.round(user.getGoals().getFatsPercentage())+"g");
                binding.cpiCalories.setMax(user.getGoals().getDailyCalories());
                binding.cpiCalories.setMin(0);
                binding.cpiCalories.setProgress(0);
            }
        }
        calendar = Calendar.getInstance();
        binding.btnPreviousDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Extrae el texto actual del TextView
                String currentDayText = binding.tvCurrentDay.getText().toString();

                // Extrae el número del día (por ejemplo, "07" de "Lunes 07")
                String dayNumber = currentDayText.replaceAll("\\D+", ""); // Extrae "07"
                int day = Integer.parseInt(dayNumber);

                // Crea un objeto LocalDate con el día actual
                LocalDate today = LocalDate.now().withDayOfMonth(day);
                // Obtiene el día siguiente
                LocalDate nextDay = today.plusDays(-1);

                // Obtiene el nombre del día de la semana en español
                DayOfWeek dayOfWeek = nextDay.getDayOfWeek();
                String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
                // Capitaliza la primera letra
                dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

                // Formatea la fecha para enviarla a la API
                String fecha = String.format("%02d-%02d-%d", nextDay.getDayOfMonth(), nextDay.getMonthValue(), nextDay.getYear());
                binding.tvCurrentDay.setText(dayName + " " + nextDay.getDayOfMonth());

                // Realiza la llamada a la API para obtener las comidas para la fecha seleccionada
                ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
                Toast.makeText(requireContext(), fecha, Toast.LENGTH_LONG).show();
                Call<List<Meal>> meals = apiService.getMeals(fecha);
                meals.enqueue(new Callback<List<Meal>>() {
                    @Override
                    public void onResponse(Call<List<Meal>> call, Response<List<Meal>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Meal> mealList = response.body();
                            Log.d("body", response.body().toString());
                            binding.llBreakfastItems.removeAllViews();
                            binding.llLunchItems.removeAllViews();
                            if (mealList.size() > 0) {
                                // Limpia los elementos anteriores antes de agregar nuevos

                                // Recorre la lista de comidas y agrega las nuevas vistas
                                for (Meal comida : mealList) {
                                    if (comida.getTipo().equalsIgnoreCase("DESAYUNO")) {
                                        LinearLayout linearLayout = binding.llBreakfastItems;
                                        LinearLayout newItem = new LinearLayout(requireContext());
                                        newItem.setLayoutParams(new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                        newItem.setOrientation(LinearLayout.HORIZONTAL);
                                        newItem.setPadding(8, 8, 8, 8);  // Añadir padding
                                        newItem.setGravity(Gravity.CENTER_VERTICAL);

                                        TextView newText = new TextView(requireContext());
                                        newText.setLayoutParams(new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                        newText.setText(comida.getName());
                                        newText.setTextSize(14);
                                        newText.setTextColor(Color.BLACK);
                                        newText.setPadding(12, 0, 0, 0);

                                        newItem.addView(newText);  // Añadir el TextView al LinearLayout
                                        linearLayout.addView(newItem);  // Añadir el LinearLayout al contenedor
                                    }
                                    // Aquí puedes agregar más condiciones si quieres manejar otros tipos de comidas
                                }
                            }
                        } else {
                            Log.e(TAG, "Request failed. Code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Meal>> call, Throwable t) {
                        Log.e(TAG, "Error: " + t.getMessage());
                    }
                });
            }
        });
        binding.btnNextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Extrae el texto actual del TextView
                String currentDayText = binding.tvCurrentDay.getText().toString();

                // Extrae el número del día (por ejemplo, "07" de "Lunes 07")
                String dayNumber = currentDayText.replaceAll("\\D+", ""); // Extrae "07"
                int day = Integer.parseInt(dayNumber);

                // Crea un objeto LocalDate con el día actual
                LocalDate today = LocalDate.now().withDayOfMonth(day);
                // Obtiene el día siguiente
                LocalDate nextDay = today.plusDays(1);

                // Obtiene el nombre del día de la semana en español
                DayOfWeek dayOfWeek = nextDay.getDayOfWeek();
                String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
                // Capitaliza la primera letra
                dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

                // Formatea la fecha para enviarla a la API
                String fecha = String.format("%02d-%02d-%d", nextDay.getDayOfMonth(), nextDay.getMonthValue(), nextDay.getYear());


                // Realiza la llamada a la API para obtener las comidas para la fecha seleccionada
                ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
                Toast.makeText(requireContext(), fecha, Toast.LENGTH_LONG).show();
                Call<List<Meal>> meals = apiService.getMeals(fecha);
                meals.enqueue(new Callback<List<Meal>>() {
                    @Override
                    public void onResponse(Call<List<Meal>> call, Response<List<Meal>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Meal> mealList = response.body();
                            Log.d("body", response.body().toString());
                            binding.llBreakfastItems.removeAllViews();
                            binding.llLunchItems.removeAllViews();
                            if (mealList.size() > 0) {
                                // Limpia los elementos anteriores antes de agregar nuevos

                                // Recorre la lista de comidas y agrega las nuevas vistas
                                for (Meal comida : mealList) {
                                    if (comida.getTipo().equalsIgnoreCase("DESAYUNO")) {
                                        LinearLayout linearLayout = binding.llBreakfastItems;
                                        LinearLayout newItem = new LinearLayout(requireContext());
                                        newItem.setLayoutParams(new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                        newItem.setOrientation(LinearLayout.HORIZONTAL);
                                        newItem.setPadding(8, 8, 8, 8);  // Añadir padding
                                        newItem.setGravity(Gravity.CENTER_VERTICAL);

                                        TextView newText = new TextView(requireContext());
                                        newText.setLayoutParams(new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                        newText.setText(comida.getName());
                                        newText.setTextSize(14);
                                        newText.setTextColor(Color.BLACK);
                                        newText.setPadding(12, 0, 0, 0);

                                        newItem.addView(newText);  // Añadir el TextView al LinearLayout
                                        linearLayout.addView(newItem);  // Añadir el LinearLayout al contenedor
                                    }
                                    // Aquí puedes agregar más condiciones si quieres manejar otros tipos de comidas
                                }
                            }
                        } else {
                            Log.e(TAG, "Request failed. Code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Meal>> call, Throwable t) {
                        Log.e(TAG, "Error: " + t.getMessage());
                    }
                });

                // Actualiza el TextView con el día siguiente
                binding.tvCurrentDay.setText(dayName + " " + String.format("%02d", nextDay.getDayOfMonth()));
            }
        });

        binding.addbreakfast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogAddComida dialog=new DialogAddComida();
                dialog.show(requireActivity().getSupportFragmentManager(), "DialogAddComida");
            }
        });
        binding.addlunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


        return root;
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd", Locale.getDefault());
        String formattedDate = sdf.format(calendar.getTime());
        binding.tvCurrentDay.setText(formattedDate);  // Establecemos la fecha en el TextView
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        // TODO: Use the ViewModel
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_cerrar_sesion) {
            manager=new SharedPreferencesManager(requireContext());
            manager.clearUser();
            Toast.makeText(requireContext(), "Cerrando sesión desde Fragment...", Toast.LENGTH_SHORT).show();
            // Perform logout logic here, similar to the MainActivity example
            // You'd still need an Intent to go to LoginActivity and clear tasks
            Intent intent = new Intent(requireActivity(), MainActivity.class); // Assuming LoginActivity
            startActivity(intent);
            requireActivity().finish();

        }
        return false;
    }

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0); // Get the first header view

        //TextView navUserName = headerView.findViewById(R.id.nav_header_name);
        //TextView navUserEmail = headerView.findViewById(R.id.nav_header_email);

        // Retrieve data from SharedPreferencesManager
        //String userName = manager.getUserName();
        //String userEmail = manager.getUserEmail();

        // Set the retrieved data to the TextViews
        //navUserName.setText(userName);
        //navUserEmail.setText(userEmail);
    }
}