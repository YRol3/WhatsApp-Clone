package com.chat.app.mychatapp.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.chat.app.mychatapp.R;

import static android.content.Context.MODE_PRIVATE;

public class SettingsDialog extends DialogFragment {

    public static final String PREFS = "phone";
    public static final String IP = "ip";
    public static final String PHONE_NUMBER = "number";
    public static final String DEFAULT_IP = "10.0.2.2:8080";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_dialog_fragment, container, false);
        return view;
    }
    public static SettingsDialog newInstance() {
        SettingsDialog f = new SettingsDialog();
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        EditText ipEditText = getView().findViewById(R.id.ipEditText);
        EditText portEditText = getView().findViewById(R.id.portEditText);
        String ip = getActivity().getSharedPreferences(PREFS, MODE_PRIVATE).getString(IP, DEFAULT_IP);
        String[] parts = ip.split(":");
        ipEditText.setText(parts[0]);
        portEditText.setText(parts[1]);
        getView().findViewById(R.id.confirmButton).setOnClickListener(v -> {
            if(!ipEditText.getText().toString().isEmpty() && !portEditText.getText().toString().isEmpty()) {
                String fullIp = ipEditText.getText().toString() + ":" + portEditText.getText().toString();
                getActivity().getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(IP, fullIp).commit();
                Toast.makeText(getContext(), getString(R.string.ip_set_to) + " " + fullIp, Toast.LENGTH_SHORT).show();
                Toast.makeText(getContext(), R.string.we_suggest_reopen_app, Toast.LENGTH_SHORT).show();
                dismiss();
            }
            else
                Toast.makeText(getContext(), R.string.ip_or_port_can_not_be_empty, Toast.LENGTH_SHORT).show();
        });
        getView().findViewById(R.id.cancelButton).setOnClickListener(v -> dismiss());
    }
}
