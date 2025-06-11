package com.example.titanfit.ui.modificadatos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
    private int num_clicks_save=0;

    private TextView tvUsernameError, tvEmailError, tvPasswordError, tvRepeatPasswordError;

    public static ModificaDatosFragment newInstance() {
        return new ModificaDatosFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModificaDatosBinding.inflate(inflater, container, false);

        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(requireContext());
        user = sharedPreferencesManager.getUser();

        binding.etUsername.setText(user.getName());
        binding.etPassword.setText(user.getPassword());
        binding.etRepeatPassword.setText(user.getPassword());
        binding.etEmail.setText(user.getEmail());

        // Inicializar los TextView de error
        tvUsernameError = binding.getRoot().findViewById(R.id.tvUsernameError);
        tvEmailError = binding.getRoot().findViewById(R.id.tvEmailError);
        tvPasswordError = binding.getRoot().findViewById(R.id.tvPasswordError);
        tvRepeatPasswordError = binding.getRoot().findViewById(R.id.tvRepeatPasswordError);

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (num_clicks_save==0){
                    String username = binding.etUsername.getText().toString().trim();
                    String email = binding.etEmail.getText().toString().trim();
                    String password = binding.etPassword.getText().toString();
                    String repeatPassword = binding.etRepeatPassword.getText().toString();

                    boolean isValid = true;

                    // Resetear visibilidad de errores
                    tvUsernameError.setVisibility(View.GONE);
                    tvEmailError.setVisibility(View.GONE);
                    tvPasswordError.setVisibility(View.GONE);
                    tvRepeatPasswordError.setVisibility(View.GONE);

                    // Validar campos vacíos
                    if (username.isEmpty()) {
                        tvUsernameError.setText("El nombre de usuario no puede estar vacío");
                        tvUsernameError.setVisibility(View.VISIBLE);
                        num_clicks_save=0;
                        isValid = false;
                    }

                    if (email.isEmpty()) {
                        tvEmailError.setText("El correo electrónico no puede estar vacío");
                        tvEmailError.setVisibility(View.VISIBLE);
                        num_clicks_save=0;
                        isValid = false;
                    }

                    if (password.isEmpty()) {
                        tvPasswordError.setText("La contraseña no puede estar vacía");
                        tvPasswordError.setVisibility(View.VISIBLE);
                        num_clicks_save=0;
                        isValid = false;
                    }

                    if (repeatPassword.isEmpty()) {
                        tvRepeatPasswordError.setText("Repite la contraseña");
                        tvRepeatPasswordError.setVisibility(View.VISIBLE);
                        num_clicks_save=0;
                        isValid = false;
                    }

                    // Validar que las contraseñas coincidan
                    if (!password.equals(repeatPassword)) {
                        tvRepeatPasswordError.setText("Las contraseñas no coinciden");
                        tvRepeatPasswordError.setVisibility(View.VISIBLE);
                        num_clicks_save=0;
                        isValid = false;
                    }

                    if (isValid) {
                        user.setPassword(password);
                        user.setName(username);
                        user.setEmail(email);
                        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);

                        Gson gson = new Gson();
                        String json = gson.toJson(user);

                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

                        Call<Void> call = apiService.updateUser(requestBody);

                        call.enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    sharedPreferencesManager.clearUser();
                                    sharedPreferencesManager.saveUser(user);

                                    NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
                                    View headerView = navigationView.getHeaderView(0);

                                    TextView usernameTextView = headerView.findViewById(R.id.username);
                                    TextView emailTextView = headerView.findViewById(R.id.textView);

                                    usernameTextView.setText(user.getName());
                                    emailTextView.setText(user.getEmail());

                                    Toast.makeText(requireContext(), "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
                                } else {
                                    num_clicks_save=0;
                                    Toast.makeText(requireContext(), "Error al actualizar usuario: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                t.printStackTrace();
                                num_clicks_save=0;
                                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    num_clicks_save++;
                }

            }
        });

        binding.btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Eliminar Cuenta") // Título del diálogo
                        .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer.") // Mensaje
                        .setPositiveButton("Sí, Eliminar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // El usuario hizo clic en "Sí, Eliminar"
                                // Aquí se ejecuta la lógica de eliminación de la cuenta
                                deleteUserAccount();
                            }
                        })
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // El usuario hizo clic en "Cancelar"
                                dialog.dismiss(); // Cierra el diálogo
                                Toast.makeText(getContext(), "Eliminación de cuenta cancelada.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert) // Opcional: un icono de advertencia
                        .show();
            }
        });

        return binding.getRoot();
    }

    private void deleteUserAccount() {
        ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);

        // Asegúrate de que user.getId() devuelva un ID válido
        if (user.getId() == null) {
            Toast.makeText(getContext(), "Error: ID de usuario no disponible para eliminar.", Toast.LENGTH_SHORT).show();
            Log.e("DeleteAccount", "User ID is null when attempting to delete.");
            return;
        }

        Call<Void> call = apiService.deleteUser(user.getId());

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    SharedPreferencesManager sharedPreferencesManager=new SharedPreferencesManager(requireContext());
                    sharedPreferencesManager.clearUser(); // Limpia los datos del usuario local
                    Intent intent = new Intent(getContext(), SplashActivity.class);
                    // Para limpiar la pila de actividades y no poder volver atrás
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    // Si este fragmento es parte de una Activity, puedes finalizarla
                    // if (getActivity() != null) {
                    //     getActivity().finish();
                    // }
                    Toast.makeText(getContext(), "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMessage = "Error al eliminar usuario: " + response.code();
                    try {
                        // Intenta leer el cuerpo del error si está disponible
                        if (response.errorBody() != null) {
                            errorMessage += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("DeleteAccount", "Error parsing error body: " + e.getMessage());
                    }
                    Log.e("DeleteAccount", errorMessage);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("DeleteAccount", "Network error: " + t.getMessage(), t);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ModificaDatosViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
