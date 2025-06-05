package com.example.titanfit.ui.goals;

import static android.content.ContentValues.TAG;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentGoalsBinding;
import com.example.titanfit.models.Favoritos;
import com.example.titanfit.models.User;
import com.example.titanfit.models.UserGoal;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.Metodos;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoalsFragment extends Fragment {

    private GoalsViewModel mViewModel;
    private FragmentGoalsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGoalsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar TextViews
        TextView textViewDatosPersonales = binding.textViewDatosPersonales;
        TextView textViewObjetivo = binding.textViewObjetivo;
        TextView textViewNivelActividad = binding.textViewNivelActividad;
        TextView textViewGenero = binding.textViewGenero;

        // Rellenar Spinners
        ArrayAdapter<CharSequence> objetivoAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.objetivos_array,
                android.R.layout.simple_spinner_item
        );
        objetivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.objetivoSpinner.setAdapter(objetivoAdapter);

        ArrayAdapter<CharSequence> actividadAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.actividad_array,
                android.R.layout.simple_spinner_item
        );
        actividadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.nivelActividadSpinner.setAdapter(actividadAdapter);

        ArrayAdapter<CharSequence> generoAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.generos,
                android.R.layout.simple_spinner_item
        );
        generoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.genero.setAdapter(generoAdapter);

        // Acción al pulsar el botón
        binding.btnContinuar.setOnClickListener(v -> {
            String usuario = binding.usuario.getText().toString().trim();
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString();
            String edadStr = binding.edadInput.getText().toString().trim();
            String alturaStr = binding.alturaInput.getText().toString().trim();
            String pesoStr = binding.pesoInput.getText().toString().trim();

            if (usuario.isEmpty()) {
                binding.usuario.setError("Campo obligatorio");
                binding.usuario.requestFocus();
                return;
            }
            if (email.isEmpty()) {
                binding.email.setError("Campo obligatorio");
                binding.email.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.email.setError("Email inválido");
                binding.email.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                binding.password.setError("Campo obligatorio");
                binding.password.requestFocus();
                return;
            }
            if (!isPasswordValid(password)) {
                binding.password.setError("Debe tener mínimo 8 caracteres, mayúscula, minúscula y símbolo");
                binding.password.requestFocus();
                return;
            }
            if (edadStr.isEmpty()) {
                binding.edadInput.setError("Campo obligatorio");
                binding.edadInput.requestFocus();
                return;
            }
            if (alturaStr.isEmpty()) {
                binding.alturaInput.setError("Campo obligatorio");
                binding.alturaInput.requestFocus();
                return;
            }
            if (pesoStr.isEmpty()) {
                binding.pesoInput.setError("Campo obligatorio");
                binding.pesoInput.requestFocus();
                return;
            }

            int edad = Integer.parseInt(edadStr);
            double altura = Double.parseDouble(alturaStr);
            double peso = Double.parseDouble(pesoStr);

            String objetivoSeleccionado = binding.objetivoSpinner.getSelectedItem().toString();
            String factorActividad = binding.nivelActividadSpinner.getSelectedItem().toString();
            String genero = binding.genero.getSelectedItem().toString();

            double factor_act = 0;
            switch (factorActividad.toLowerCase()) {
                case "sedentario": factor_act = 1.2; break;
                case "ligero": factor_act = 1.375; break;
                case "moderado": factor_act = 1.55; break;
                case "activo": factor_act = 1.725; break;
                case "muy activo": factor_act = 1.9; break;
            }

            UserGoal userGoal = Metodos.calculaMacros(peso, altura, edad, genero, factor_act, objetivoSeleccionado);
            User user = new User(usuario, email, password, edad, peso, altura, userGoal, null, new Favoritos());
            SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(requireContext());
            ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);

            Call<User> call = apiService.addUser(user);
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful()) {
                        User user1 = response.body();
                        Toast.makeText(requireContext(), "Usuario creado correctamente", Toast.LENGTH_LONG).show();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("user", user1);
                        sharedPreferencesManager.saveUser(user1);

                        // Actualizar header del NavigationView
                        NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
                        View headerView = navigationView.getHeaderView(0);
                        TextView usernameTextView = headerView.findViewById(R.id.username);
                        TextView emailTextView = headerView.findViewById(R.id.textView);
                        usernameTextView.setText(user1.getName());
                        emailTextView.setText(user1.getEmail());

                        NavController navController = NavHostFragment.findNavController(GoalsFragment.this);
                        navController.navigate(R.id.action_goals_to_main, bundle);
                    } else {
                        Toast.makeText(requireContext(), "Error al crear usuario", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Log.e(TAG, "Error: " + t.getMessage());
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        return root;
    }

    private boolean isPasswordValid(String password) {
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\W).{8,}$";
        return password.matches(passwordPattern);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(GoalsViewModel.class);
    }
}
