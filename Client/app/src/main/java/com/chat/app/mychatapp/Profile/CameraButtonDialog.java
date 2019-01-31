package com.chat.app.mychatapp.Profile;


import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.chat.app.mychatapp.R;
import com.chat.app.mychatapp.main.SettingsDialog;

import java.io.File;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class CameraButtonDialog extends DialogFragment {

    public static final int CAMERA_REQUEST_CODE = 1;
    public static final int GALLERY_REQUEST_CODE = 2;
    static String photoPath;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_button_dialog, container, false);
        Window window = getDialog().getWindow();
        window.setGravity(Gravity.BOTTOM);
        return view;
    }


    public static CameraButtonDialog newInstance() {
        CameraButtonDialog f = new CameraButtonDialog();
        f.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_Dialog);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();

        getView().findViewById(R.id.gallery_button).setOnClickListener(v -> {
            dismiss();


            Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getIntent.setType("image/*");

            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");

            Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

            getActivity().startActivityForResult(chooserIntent, GALLERY_REQUEST_CODE);

        });

        getView().findViewById(R.id.camera_button_dialog).setOnClickListener(v -> {
            dismiss();
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(takePhotoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                File file = createImageFile();
                Uri photoUri = FileProvider.getUriForFile(getContext(), "com.chat.app.mychatapp.fileprovider", file);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                getActivity().startActivityForResult(takePhotoIntent, CAMERA_REQUEST_CODE);
            }
        });

        getView().findViewById(R.id.remove_button).setOnClickListener(v -> {
            Toast.makeText(getContext(), "In Development", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.getWindow().setLayout(width, height);
        }

    }
    private File createImageFile(){
        //creates photo.jpg file after camera intent its writes the bytes to this file
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(storageDir, "photo.jpg");
        photoPath = file.getAbsolutePath();
        return file;
    }

}
