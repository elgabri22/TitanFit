package com.example.titanfit.ui.login;

import static android.content.ContentValues.TAG;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Base64;
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
import com.example.titanfit.ui.SharedPreferencesManager;
import com.example.titanfit.ui.home.HomeFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

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


    public LoginFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        sharedPreferences=new SharedPreferencesManager(requireContext());

        gson = new Gson(); // Inicializar Gson

        NavController navController = NavHostFragment.findNavController(LoginFragment.this);


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
                            fetchedUser.setPassword(PasswordUtils.generateSecurePassword(fetchedUser.getPassword()));

                            Log.d("user",fetchedUser.toString());
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





class PasswordUtils {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256; // bits

    // Genera un salt aleatorio
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 16 bytes = 128 bits
        random.nextBytes(salt);
        return salt;
    }

    // Genera un hash de la contraseña con el salt
    public static String hashPassword(final char[] password, final byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // Codifica el hash en Base64 para almacenarlo o enviarlo
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    // Ejemplo para generar hash + salt juntos como string (para almacenar)
    public static String generateSecurePassword(String password) {
        byte[] salt = generateSalt();
        String hash = hashPassword(password.toCharArray(), salt);
        String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        return saltBase64 + ":" + hash; // Guarda salt y hash separados por ':'
    }

    // Para validar una contraseña, tendrás que extraer el salt y el hash guardados
    // y repetir el hash con la contraseña introducida, comparando el resultado.
}
