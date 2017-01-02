package com.ashwin.vault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public class CreateVaultFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final PasswordDBHelper passwordDbHelper = new PasswordDBHelper(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View v = inflater.inflate(R.layout.create_vault, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v).setCancelable(false)
                // Add action buttons
                .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText e = (EditText)v.findViewById(R.id.create_password);
                        if(e.getText()!=null){
                            passwordDbHelper.addPassword(e.getText().toString());
                        }
                    }
                });
        /*
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        LoginDialogFragment.this.getDialog().cancel();
                    }
                });
                */
        return builder.create();
    }
}

