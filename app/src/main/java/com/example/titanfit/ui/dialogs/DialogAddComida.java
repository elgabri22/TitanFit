package com.example.titanfit.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.titanfit.R;
import com.example.titanfit.databinding.DialogAddComidaBinding;

public class DialogAddComida extends DialogFragment {
    private DialogAddComidaBinding binding;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_comida, null);

        builder.setView(view);
        return builder.create();
    }
}

