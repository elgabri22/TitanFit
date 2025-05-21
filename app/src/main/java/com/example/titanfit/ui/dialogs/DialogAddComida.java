package com.example.titanfit.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable; // Importar Editable
import android.text.TextWatcher; // Importar TextWatcher
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.titanfit.R;
import com.example.titanfit.databinding.DialogAddComidaBinding;

public class DialogAddComida extends DialogFragment {
    private DialogAddComidaBinding binding;
    private EditText editTextSearch;
    private ImageButton imageButtonSearch;
    private RecyclerView recyclerViewComidas;



    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();


        binding = DialogAddComidaBinding.inflate(inflater);
        View view = binding.getRoot();


        editTextSearch = binding.editTextSearch;
        imageButtonSearch = binding.imageButtonSearch;
        recyclerViewComidas = binding.recyclerViewComidas;
        String tipo= getArguments().getString("tipo");

        recyclerViewComidas.setLayoutManager(new LinearLayoutManager(getContext()));

        //Adapter adapter = new Adapter();
        // recyclerViewComidas.setAdapter(adapter);

        editTextSearch.setFocusable(true);
        editTextSearch.setFocusableInTouchMode(true);

        imageButtonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                busqueda(editTextSearch.getText().toString());
            }
        });

        editTextSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    busqueda(v.getText().toString());
                    return true; // Indica que se ha manejado el evento
                }
                return false;
            }
        });

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                busqueda(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        builder.setView(view);
        return builder.create();
    }


    private void busqueda(String comida) {

        editTextSearch.clearFocus(); // Quitar foco al EditText
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Limpiar el binding para evitar fugas de memoria
    }
}
