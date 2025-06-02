package com.example.titanfit.ui.modificadatos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.titanfit.R;
import com.example.titanfit.SplashActivity;
import com.example.titanfit.databinding.FragmentModificaDatosBinding;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.MainActivity;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModificaDatosFragment extends Fragment {

    private ModificaDatosViewModel mViewModel;
    private FragmentModificaDatosBinding binding;
    private User user;

    public static ModificaDatosFragment newInstance() {
        return new ModificaDatosFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModificaDatosBinding.inflate(inflater, container, false);

        SharedPreferencesManager sharedPreferencesManager=new SharedPreferencesManager(requireContext());
        user=sharedPreferencesManager.getUser();

        binding.etUsername.setText(user.getName());
        binding.etPassword.setText(user.getPassword());
        binding.etPassword.setText(user.getPassword());
        binding.etEmail.setText(user.getEmail());

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.etPassword.getText().toString().equalsIgnoreCase(binding.etRepeatPassword.getText().toString())){
                    user.setPassword(binding.etPassword.getText().toString());
                    user.setName(binding.etUsername.getText().toString());
                    user.setEmail(binding.etEmail.getText().toString());
                    ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);

                    // Convierte el objeto User a JSON usando Gson
                    Gson gson = new Gson();
                    String json = gson.toJson(user);

                    // Crea el RequestBody con el JSON y el tipo MIME adecuado
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

                    Call<Void> call = apiService.updateUser(requestBody);

                    call.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                // Actualización exitosa
                                System.out.println("Usuario actualizado correctamente");
                                sharedPreferencesManager.clearUser();
                                sharedPreferencesManager.saveUser(user);
                                NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
                                View headerView = navigationView.getHeaderView(0);

                                TextView usernameTextView = headerView.findViewById(R.id.username);
                                TextView emailTextView = headerView.findViewById(R.id.textView);

                                usernameTextView.setText(user.getName());
                                emailTextView.setText(user.getEmail());
                            } else {
                                // Error en la respuesta
                                System.err.println("Error al actualizar usuario: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            // Error en la llamada (red, etc)
                            t.printStackTrace();
                        }
                    });
                }
            }
        });
        binding.btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);

                // Convierte el objeto User a JSON usando Gson
                Gson gson = new Gson();
                String json = gson.toJson(user);

                // Crea el RequestBody con el JSON y el tipo MIME adecuado
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

                Call<Void> call = apiService.deleteUser(user.getId());

                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            // Actualización exitosa
                            System.out.println("Usuario actualizado correctamente");
                            sharedPreferencesManager.clearUser();
                            Intent intent = new Intent(getContext(), SplashActivity.class);
                            startActivity(intent);
                        } else {
                            // Error en la respuesta
                            System.err.println("Error al actualizar usuario: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        // Error en la llamada (red, etc)
                        t.printStackTrace();
                    }
                });
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(ModificaDatosViewModel.class);

        // TODO: Usa mViewModel para observar datos y bindear a la UI
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Evita memory leaks
    }
}
