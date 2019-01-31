package com.chat.app.mychatapp.Profile;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import com.chat.app.mychatapp.R;

public class EditNameActivity extends AppCompatActivity {
    EditText name;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);
        name = findViewById(R.id.edit_name_text);
        findViewById(R.id.edit_name_button_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.edit_name_button_ok).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("userName", name.getText().toString());
            setResult(RESULT_OK, intent);
            finish();
        });
    }
}
