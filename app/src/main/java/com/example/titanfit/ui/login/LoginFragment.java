package com.example.titanfit.ui.login;

import static android.content.ContentValues.TAG;

import androidx.lifecycle.ViewModelProvider;

import android.util.Patterns;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Base64;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentLoginBinding;
import com.example.titanfit.models.User;
import com.example.titanfit.network.ApiClient;
import com.example.titanfit.network.ApiServiceUser;
import com.example.titanfit.ui.SharedPreferencesManager;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private LoginViewModel mViewModel;
    private FragmentLoginBinding binding;
    private SharedPreferencesManager sharedPreferences;
    private Gson gson;
    private int num_clicks=0;

    public LoginFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        sharedPreferences = new SharedPreferencesManager(requireContext());
        gson = new Gson();

        NavController navController = NavHostFragment.findNavController(LoginFragment.this);

        // Limpiar errores al escribir en campos
        binding.editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.textViewErrorEmail.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.textViewErrorPassword.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.buttonLogin.setOnClickListener(v -> {
            if (num_clicks==0){
                String email = binding.editTextEmail.getText().toString().trim();
                String password = binding.editTextPassword.getText().toString().trim();

                // Limpiar errores previos
                binding.textViewErrorEmail.setVisibility(View.GONE);
                binding.textViewErrorPassword.setVisibility(View.GONE);

                boolean isValid = true;

                if (email.isEmpty()) {
                    binding.textViewErrorEmail.setText("Campo obligatorio");
                    binding.textViewErrorEmail.setVisibility(View.VISIBLE);
                    num_clicks=0;
                    isValid = false;
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.textViewErrorEmail.setText("Email inválido");
                    binding.textViewErrorEmail.setVisibility(View.VISIBLE);
                    num_clicks=0;
                    isValid = false;
                }

                if (password.isEmpty()) {
                    binding.textViewErrorPassword.setText("Campo obligatorio");
                    binding.textViewErrorPassword.setVisibility(View.VISIBLE);
                    num_clicks=0;
                    isValid = false;
                }

                if (!isValid) {
                    num_clicks=0;
                    return;
                }

                ApiServiceUser apiService = ApiClient.getClient().create(ApiServiceUser.class);
                Call<User> usuarioCall = apiService.getUser(email);

                usuarioCall.enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            User fetchedUser = response.body();

                            // Aquí debes comparar la contraseña correctamente (texto plano o hashed)
                            if (!fetchedUser.getPassword().equals(password)) {
                                binding.textViewErrorPassword.setVisibility(View.VISIBLE);
                                num_clicks=0;
                                binding.textViewErrorPassword.setText("La contraseña debe de contener mínimo 8 caracteres de los cuales, uno en mayúscula, otro en minúscula y sun símbolo");
                                return;
                            }

                            if (!fetchedUser.getEmail().equals(email)) {
                                binding.textViewErrorEmail.setVisibility(View.VISIBLE);
                                num_clicks=0;
                                binding.textViewErrorEmail.setText("Email incorrecto");
                                return;
                            }

                            fetchedUser.setPassword(PasswordUtils.generateSecurePassword(fetchedUser.getPassword()));

                            Call<ResponseBody> tokenCall = apiService.generateToken(fetchedUser);
                            tokenCall.enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    sharedPreferences.saveUser(fetchedUser);

                                    NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
                                    View headerView = navigationView.getHeaderView(0);

                                    TextView usernameTextView = headerView.findViewById(R.id.username);
                                    TextView emailTextView = headerView.findViewById(R.id.textView);

                                    usernameTextView.setText(fetchedUser.getName());
                                    emailTextView.setText(fetchedUser.getEmail());

                                    Toast.makeText(requireContext(), "Usuario logueado correctamente", Toast.LENGTH_LONG).show();
                                    navController.navigate(R.id.action_login_to_main);
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Log.e(TAG, "Error al generar el token: " + t.getMessage());
                                    num_clicks=0;
                                    Toast.makeText(requireContext(), "Error al generar token", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            binding.textViewErrorPassword.setVisibility(View.VISIBLE);
                            num_clicks=0;
                            binding.textViewErrorPassword.setText("La contraseña debe de contener mínimo 8 caracteres de los cuales, uno en mayúscula, otro en minúscula y sun símbolo");
                            binding.textViewErrorEmail.setVisibility(View.VISIBLE);
                            binding.textViewErrorEmail.setText("Email incorrecto");
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        binding.textViewErrorPassword.setVisibility(View.VISIBLE);
                        num_clicks=0;
                        binding.textViewErrorPassword.setText("La contraseña debe de contener mínimo 8 caracteres de los cuales, uno en mayúscula, otro en minúscula y sun símbolo");
                        binding.textViewErrorEmail.setVisibility(View.VISIBLE);
                        binding.textViewErrorEmail.setText("Email incorrecto");
                    }
                });
            }
            num_clicks++;
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

class PasswordUtils {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256; // bits

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    public static String hashPassword(final char[] password, final byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateSecurePassword(String password) {
        byte[] salt = generateSalt();
        String hash = hashPassword(password.toCharArray(), salt);
        String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        return saltBase64 + ":" + hash;
    }
}
