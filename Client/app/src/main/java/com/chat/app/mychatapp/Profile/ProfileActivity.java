package com.chat.app.mychatapp.Profile;



import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.app.mychatapp.R;
import com.chat.app.mychatapp.main.MainActivity;
import com.chat.app.mychatapp.main.SettingsDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.chat.app.mychatapp.DrawCalculations.bimapToTempFile;
import static com.chat.app.mychatapp.DrawCalculations.getBitmapFromURL;
import static com.chat.app.mychatapp.DrawCalculations.rotateBitmap;
import static com.chat.app.mychatapp.DrawCalculations.rotateImage;
import static com.chat.app.mychatapp.DrawCalculations.scaleImage;
import static com.chat.app.mychatapp.Profile.CameraButtonDialog.CAMERA_REQUEST_CODE;
import static com.chat.app.mychatapp.Profile.CameraButtonDialog.GALLERY_REQUEST_CODE;
import static com.chat.app.mychatapp.Profile.CameraButtonDialog.photoPath;

public class ProfileActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 4;
    public static final int EDIT_NAME_REQUEST = 505;
    ImageView profilePic;
    ImageView cameraButton;
    ImageView editNameButton;
    TextView profileName;
    TextView profileStatus;
    TextView profilePhoneNumber;
    String ip;
    String phoneNumber;
    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profilePic = findViewById(R.id.profilePicture);
        cameraButton = findViewById(R.id.profile_camera_button);
        editNameButton = findViewById(R.id.profileNameEdit);
        profileName = findViewById(R.id.profileName);
        profileStatus = findViewById(R.id.profileStatus);
        profilePhoneNumber = findViewById(R.id.profilePhoneNumber);

        editNameButton.setOnClickListener(this::showEditNameDialog);

        cameraButton.setOnClickListener(this::showDialog);

        findViewById(R.id.backButton).setOnClickListener(v -> goToMainActivity());

        phoneNumber = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.PHONE_NUMBER, "");
        profilePhoneNumber.setText(phoneNumber);

        userName = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString("userName", "");
        profileName.setText(userName);

        ip = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.IP, SettingsDialog.DEFAULT_IP);

        setProfilePic(ip, phoneNumber);
    }

    public void setProfilePic(String ip, String phoneNumber){
        AsyncTask<String, Void, Bitmap> as = new AsyncTask<String, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(String... strings) {
                if(strings.length == 2){
                    String ip = strings[0];
                    String phoneNumber = strings[1];
                    Bitmap bitmap;
                    bitmap = getBitmapFromURL("http://"+ ip+"/ChatApp_war_exploded/request?option=ask_for_image&user="+phoneNumber);
                    return bitmap;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if(bitmap != null)
                    profilePic.setImageBitmap(bitmap);
            }
        };
        as.execute(ip, phoneNumber);
    }
    public void showEditNameDialog(View view){
        Intent intent = new Intent(this, EditNameActivity.class);
        startActivityForResult(intent, EDIT_NAME_REQUEST);
    }
    public void showDialog(View view){
        showDialog(true);
    }
    public void showDialog(boolean checkPermissions){
        if(checkPermissions){
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                DialogFragment cameraDialog = CameraButtonDialog.newInstance();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                cameraDialog.show(ft, "Picture Dialog");
            }else{
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);

            }
        }else{
            DialogFragment cameraDialog = CameraButtonDialog.newInstance();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            cameraDialog.show(ft, "Picture Dialog");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                showDialog(false);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == EDIT_NAME_REQUEST && resultCode == Activity.RESULT_OK){
            if(data.hasExtra("userName")){
                String userName = data.getStringExtra("userName");
                if(userName != null && !userName.isEmpty()){
                    getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).edit().putString("userName", userName).apply();
                    profileName.setText(userName);
                }
            }
        }
        if(requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            InputStream inputStream = null;
            try {
                //TODO Detect if landspace or portrait picture for rotatiton, Currently rotating every time -90 for portrait pictures
                inputStream = this.getContentResolver().openInputStream(data.getData());
                Bitmap picBitmap = BitmapFactory.decodeStream(inputStream);
                String filePath = bimapToTempFile(this, picBitmap,"image");
                picBitmap = scaleImage(filePath, 200, 200);
                picBitmap = rotateImage(picBitmap, -90);
                setPic(picBitmap);
                sendUserPicToServer(picBitmap, phoneNumber);

            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        if(requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            try {

                Bitmap bitmap = scaleImage(photoPath, 200, 200);
                bitmap = rotateBitmap(bitmap, photoPath);
                setPic(bitmap);
                sendUserPicToServer(bitmap, phoneNumber);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendUserPicToServer(Bitmap picBitmap, String phoneNumber) {
        Thread thread = new Thread(new BitmapRunnable(picBitmap, phoneNumber) {
            @Override
            public void run() {
                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                if(this.bitmap.getByteCount() > 5000)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bStream);
                else
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bStream);

                byte[] byteArray = bStream.toByteArray();
                String encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
                URL url = null;
                HttpURLConnection connection = null;
                OutputStream outputStream = null;
                try {
                    Log.d("Yan", ip);
                    String urlString = "http://" + ip + "/ChatApp_war_exploded/request?option=upload_user_profile_pic";
                    url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Accept-Charset", "UTF-8" );
                    connection.setRequestProperty("charset", "UTF-8" );
                    connection.setRequestMethod("POST" );
                    connection.setUseCaches(false);
                    Log.d("Yan", "in json");

                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("phone_number", phoneNumber);
                        jsonObject.put("user_pic", encodedString);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    outputStream = connection.getOutputStream();
                    outputStream.write(jsonObject.toString().getBytes());
                    connection.connect();
                    Log.d("Yan", ""+connection.getResponseCode());

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }

    private void setPic(Bitmap bitmap){
        profilePic.setImageBitmap(bitmap);
    }

    @Override
    public void onBackPressed() {
        goToMainActivity();
    }
    public void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.transition.enter_backward, R.transition.exit_backward);
        finish();
    }
    abstract class BitmapRunnable implements Runnable{
        public Bitmap bitmap;
        public String phoneNumber;
        public BitmapRunnable(Bitmap bitmap, String phoneNumber){
            this.bitmap = bitmap;
            this.phoneNumber = phoneNumber;
        }
    }

}
