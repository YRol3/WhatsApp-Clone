package com.chat.app.mychatapp.main;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;

import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.chat.app.mychatapp.Credentials.PhoneCredential;
import com.chat.app.mychatapp.Profile.ProfileActivity;
import com.chat.app.mychatapp.R;
import com.google.firebase.iid.FirebaseInstanceId;

import static com.chat.app.mychatapp.FCM.FirebaseCouldMessageService.sendNewToken;
import static com.chat.app.mychatapp.main.ContactsFragment.REQUEST_CODE;
import static com.chat.app.mychatapp.main.SettingsDialog.DEFAULT_IP;


public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private String userPhoneNumber;
    private ProgressBar loadingContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Broadcast to toggle progressBar visible/unvisitable while contacts loading
        LocalBroadcastManager.getInstance(this).registerReceiver(
                contactsLoad, new IntentFilter("contactsLoading"));
        userPhoneNumber = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.PHONE_NUMBER, "");
        if(userPhoneNumber.equals("")){
            Intent intent = new Intent(this, PhoneCredential.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);
        TabLayout tabLayout = findViewById(R.id.activityTabs);
        loadingContacts = findViewById(R.id.loadingContacts);
        //sets progress bar color to white
        loadingContacts.getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.MULTIPLY);

        viewPager = findViewById(R.id.activityPager);
        ViewPagerFragmentsAdapter viewPagerFragmentsAdapter = new ViewPagerFragmentsAdapter(getSupportFragmentManager(), this);
        viewPager.setAdapter(viewPagerFragmentsAdapter);
        viewPager.setCurrentItem(1);
        viewPager.setOffscreenPageLimit(2);

        tabLayout.setupWithViewPager(viewPager);

        findViewById(R.id.searchButton).setOnClickListener(v -> Toast.makeText(this, R.string.in_development, Toast.LENGTH_SHORT).show());

        findViewById(R.id.settingsButton).setOnClickListener(this::showPopupMenu);

        //Each time  user in MainActivity send his updated Token to the server
        //In case tokened changed and server was offline/User were offline
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener((task) -> {
            if(task.isSuccessful()){
                String ip = getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).getString(SettingsDialog.IP, DEFAULT_IP);
                sendNewToken(task.getResult().getToken(), ip, userPhoneNumber);
            }
        });
    }
    public void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener((item) -> {
            switch (item.getItemId()){
                case R.id.action_profile:{
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.transition.enter_forward, R.transition.exit_forward);
                    finish();
                    break;
                }
                case R.id.action_settings:
                    showDialog();
                    break;

            }
            return false;
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.settings_popup, popup.getMenu());
        popup.show();
    }
    private BroadcastReceiver contactsLoad = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if(intent.hasExtra("loading")){
                boolean loading = intent.getBooleanExtra("loading", false);
                if(loading){
                    loadingContacts.setVisibility(View.VISIBLE);
                }else{
                    Intent br = new Intent("reloadConversations");
                    intent.putExtra("loading", false);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(br);
                    loadingContacts.setVisibility(View.INVISIBLE);
                }
            }
        }

    };
    public void showDialog(){
        DialogFragment settingsDialog =  SettingsDialog.newInstance();
        settingsDialog.show(getSupportFragmentManager(), "settingsDialog");
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                //Permission request from contacts fragment, Broadcast to the fragment with permission result
                Intent intent = new Intent("permissionReceived");
                intent.putExtra("permission", true);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onBackPressed() {
        //if back pressed, Set page to the Conversations page, If in conversations page, close the app
        if(viewPager.getCurrentItem() != 1){
            viewPager.setCurrentItem(1);
            return;
        }
        super.onBackPressed();
    }
}
