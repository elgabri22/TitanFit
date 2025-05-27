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
import android.widget.Toast;

import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentGoalsBinding;
import com.example.titanfit.models.User;
import com.example.titanfit.models.UserGoal;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.Metodos;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.example.titanfit.ui.home.HomeFragment;

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
        //Inicializar binding correctamente
        binding = FragmentGoalsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //Rellenar el Spinner de objetivos
        ArrayAdapter<CharSequence> objetivoAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.objetivos_array,
                android.R.layout.simple_spinner_item
        );
        objetivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.objetivoSpinner.setAdapter(objetivoAdapter);

        // Rellenar el Spinner de nivel de actividad
        ArrayAdapter<CharSequence> actividadAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.actividad_array,
                android.R.layout.simple_spinner_item
        );

        ArrayAdapter<CharSequence> generoAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.generos,
                android.R.layout.simple_spinner_item
        );
        actividadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.nivelActividadSpinner.setAdapter(actividadAdapter);
        binding.genero.setAdapter(generoAdapter);

        NavController navController = NavHostFragment.findNavController(GoalsFragment.this);

        binding.btnContinuar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double peso=Double.parseDouble(binding.pesoInput.getText().toString());
                double altura=Double.parseDouble(binding.alturaInput.getText().toString());
                int edad=Integer.parseInt(binding.edadInput.getText().toString());
                String objetivoseleccionado=binding.objetivoSpinner.getSelectedItem().toString();
                String factor_actividad=binding.nivelActividadSpinner.getSelectedItem().toString();
                String genero=binding.genero.getSelectedItem().toString();
                String usuario=binding.usuario.getText().toString();
                String email=binding.email.getText().toString();
                String password=binding.password.getText().toString();
                double factor_act = 0;
                if (factor_actividad.equalsIgnoreCase("Sedentario")){
                    factor_act=1.2;
                }else if (factor_actividad.equalsIgnoreCase("Ligero")){
                    factor_act=1.375;
                }else if (factor_actividad.equalsIgnoreCase("Moderado")){
                    factor_act=1.55;
                }else if (factor_actividad.equalsIgnoreCase("Activo")){
                    factor_act=1.725;
                }else if (factor_actividad.equalsIgnoreCase("Muy activo")){
                    factor_act=1.9;
                }
                UserGoal userGoal= Metodos.calculaMacros(peso,altura,edad,genero,factor_act,objetivoseleccionado);
                User user=new User(usuario,email,password,edad,peso,altura,userGoal,null);
                SharedPreferencesManager sharedPreferencesManager=new SharedPreferencesManager(requireContext());
                ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
                Call<User> call = apiService.addUser(user);
                call.enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Usuario creado correctamente", Toast.LENGTH_LONG).show();
                            Bundle bundle=new Bundle();
                            bundle.putSerializable("user",user);
                            sharedPreferencesManager.saveUser(user);
                            NavController navController = NavHostFragment.findNavController(GoalsFragment.this);
                            navController.navigate(R.id.action_goals_to_main,bundle);
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Log.e(TAG, "Error: " + t.getMessage());
                    }
                });
            }
        });

        return root;
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
