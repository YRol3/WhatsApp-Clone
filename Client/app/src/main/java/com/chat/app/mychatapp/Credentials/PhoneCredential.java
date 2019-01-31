package com.chat.app.mychatapp.Credentials;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chat.app.mychatapp.R;
import com.chat.app.mychatapp.main.MainActivity;
import com.chat.app.mychatapp.main.SettingsDialog;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneCredential extends AppCompatActivity {

    Button confirmButton;
    EditText phoneNumberText;
    TextView retryCounter, restartMessage;
    Button verificationConfirmButton;
    EditText verificationCodeText;
    static String verificationID;
    String userPhoneNumber;
    CountDownTimer countDownTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_credential);
        findViewById(R.id.settingsButton).setOnClickListener(this::showDialog);
        confirmButton = findViewById(R.id.confirmButton);
        phoneNumberText = findViewById(R.id.phoneNumberText);
        retryCounter = findViewById(R.id.retryCounter);
        restartMessage = findViewById(R.id.restartMessage);
        verificationCodeText = findViewById(R.id.verificationCode);
        verificationConfirmButton = findViewById(R.id.confirmVerificationCode);
        verificationConfirmButton.setEnabled(false);

        confirmButton.setOnClickListener(v -> onSendVerificationClicked());

        verificationConfirmButton.setOnClickListener(v -> {
            if(!verificationCodeText.getText().toString().isEmpty() && verificationID != null && !verificationID.isEmpty()){
                PhoneAuthCredential credential = getCredential(verificationID, verificationCodeText.getText().toString());
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener((task) -> {
                            if(task.isSuccessful()){
                                moveToMainActivity();
                            }else{
                                restartActivity(getString(R.string.failed_to_verify_phone), getColor(R.color.colorWrong));
                            }
                        });
            }
        });

    }
    private String buildStringForCounter(long timeUntilFinish){
        String text = getString(R.string.try_again_in);
        if(timeUntilFinish/60000 > 0){
            if(((timeUntilFinish/1000)-(60 * (timeUntilFinish/60000))) >= 10)
                text += (timeUntilFinish/60000) + " :" + ((timeUntilFinish/1000)-(60 * (timeUntilFinish/60000)));
            else
                text += (timeUntilFinish/60000) + " :0" + ((timeUntilFinish/1000)-(60 * (timeUntilFinish/60000)));
        }else{
            if(timeUntilFinish >= 10000)
                text += " 0:" + (timeUntilFinish/1000);
            else
                text += " 0:0" + (timeUntilFinish/1000);
        }
        return text;
    }
    private void onSendVerificationClicked(){
        if(phoneNumberText.getText().toString().isEmpty()) {
            setRestartMessage(getString(R.string.phone_number_empty), getColor(R.color.colorWrong));
            return;
        }
        if(phoneNumberText.getText().toString().contains("+972")) {
            setRestartMessage(getString(R.string.no_coutry_code), getColor(R.color.colorWrong));
            return;
        }
        restartMessage.setVisibility(View.INVISIBLE);
        retryCounter.setVisibility(View.VISIBLE);
        countDownTimer = new CountDownTimer(31000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                retryCounter.setText(buildStringForCounter(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                restartActivity(getString(R.string.try_again_verification), getColor(R.color.colorPrimary));
            }
        }.start();
        phoneNumberText.setEnabled(false);
        confirmButton.setVisibility(View.INVISIBLE);

        verificationConfirmButton.setVisibility(View.VISIBLE);
        verificationCodeText.setVisibility(View.VISIBLE);
        String phoneNum = phoneNumberText.getText().toString();
        if(phoneNum.charAt(0) == '0'){
            userPhoneNumber = phoneNum;
        }else{
            userPhoneNumber = "0" + phoneNum;
        }
        phoneNum = "+972" + phoneNum.substring(1, phoneNum.length());

        Log.d("Yan", phoneNum);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNum, 30L , TimeUnit.SECONDS,
                this, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        verificationConfirmButton.setEnabled(true);
                        verificationID = verificationId;
                    }

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        moveToMainActivity();
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {

                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            restartActivity(getString(R.string.sms_fail_send), getColor(R.color.colorWrong));
                            // wrong phone number
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            Toast.makeText(PhoneCredential.this, R.string.send_message_developers, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                });
    }
    private void restartActivity(String messageOnRestart, int color) {
        retryCounter.setVisibility(View.INVISIBLE);
        phoneNumberText.setEnabled(true);
        confirmButton.setVisibility(View.VISIBLE);

        verificationCodeText.setText("");
        verificationConfirmButton.setVisibility(View.INVISIBLE);
        verificationCodeText.setVisibility(View.INVISIBLE);

        setRestartMessage(messageOnRestart, color);
        countDownTimer.cancel();
    }

    private void setRestartMessage(String message, int color){
        restartMessage.setText(message);
        restartMessage.setTextColor(color);
        restartMessage.setVisibility(View.VISIBLE);

    }

    public PhoneAuthCredential getCredential(String verificationID, String code){
        return PhoneAuthProvider.getCredential(verificationID, code);
    }
    public void showDialog(View view){
        DialogFragment settingsDialog =  SettingsDialog.newInstance();
        settingsDialog.show(getSupportFragmentManager(), "settingsDialog");
    }
    public void moveToMainActivity(){
        getSharedPreferences(SettingsDialog.PREFS, MODE_PRIVATE).edit().putString(SettingsDialog.PHONE_NUMBER, userPhoneNumber).apply();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
