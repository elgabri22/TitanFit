package com.example.titanfit.ui.login;

import static android.content.ContentValues.TAG;

import androidx.lifecycle.ViewModelProvider;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentLoginBinding;
import com.example.titanfit.models.Meal;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.home.HomeFragment;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private LoginViewModel mViewModel;
    private FragmentLoginBinding binding;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        NavController navController = NavHostFragment.findNavController(LoginFragment.this);

        // Ahora puedes acceder a las vistas directamente a través del objeto 'binding'
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString();
            String password = binding.editTextPassword.getText().toString();

            // Aquí podrías implementar la lógica de validación y autenticación
            Toast.makeText(getContext(), "Intentando iniciar sesión con: " + email, Toast.LENGTH_SHORT).show();
            // Por ejemplo: mViewModel.login(email, password);
        });

        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = binding.editTextEmail.getText().toString().trim();
                String password = binding.editTextPassword.getText().toString().trim();
                ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
                Call<User> usuario = apiService.getUser(user);
                usuario.enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            User fetchedUser = (User) response.body();
                            Log.d("user",fetchedUser.toString());
                            Call<ResponseBody> tokenCall = apiService.generateToken(fetchedUser);
                            tokenCall.enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    Toast.makeText(requireContext(),"Usuario logueado correctamente",Toast.LENGTH_LONG).show();
                                    navController.navigate(R.id.action_login_to_main);
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Log.e(TAG, "Error al generar el token: " + t.getMessage());
                                }
                            });
                        } else {
                            Log.d(TAG, "Código de error al obtener el usuario: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Log.e(TAG, "Error en la petición para obtener el usuario: " + t.getMessage());
                    }
                });
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Importante para evitar fugas de memoria
    }
}