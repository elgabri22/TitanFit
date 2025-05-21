package com.example.titanfit.ui.home;

// Elimina esta importación estática si no la usas en otro lugar
// import static androidx.navigation.fragment.FragmentKt.findNavController;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // Asegúrate de que esta importación sea necesaria si usas TextView

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
// Usa esta importación para findNavController(this)
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation; // Importa Navigation para findNavController(View) si lo prefieres

import com.example.titanfit.R;
import com.example.titanfit.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Obtener el NavController de la forma más común y recomendada dentro de un Fragment
        // Puedes usar cualquiera de estas dos, ambas son válidas y comunes:
        // Opción 1: Usando NavHostFragment
        NavController navController = NavHostFragment.findNavController(this);
        // Opción 2: Usando Navigation (más concisa si solo necesitas el NavController de la vista)
        // NavController navController = Navigation.findNavController(root);


        binding.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navega de Home a Goals
                navController.navigate(R.id.action_home_to_goals);
            }
        });

        binding.loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navega al destino de Login
                // Asegúrate de que R.id.action_login esté definido en tu nav_graph.xml
                // como una acción global o una acción desde el destino actual.
                navController.navigate(R.id.action_login);
            }
        });

        // Observa los cambios en el texto del ViewModel (si se usa)
        // homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText); // Descomenta si usas un TextView en tu layout y quieres que se actualice

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
