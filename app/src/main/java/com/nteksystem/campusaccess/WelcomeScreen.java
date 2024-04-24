package com.nteksystem.campusaccess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class WelcomeScreen extends AppCompatActivity {
    private static final String PREF_NAME = "MyPrefs";
    private static final String DEVICE_SETUP_KEY = "device_setup";
    private static final String DEVICE_SETUP_ENTRY = "entry";
    private static final String DEVICE_SETUP_EXIT = "exit";
    private Button btnentry, btnexit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_screen);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnentry = findViewById(R.id.btnEntryOperation);
        btnexit = findViewById(R.id.btnExitOperation);

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceSetupStatus = sharedPreferences.getString(DEVICE_SETUP_KEY, "");

        if (deviceSetupStatus.equals(DEVICE_SETUP_ENTRY)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (deviceSetupStatus.equals(DEVICE_SETUP_EXIT)) {
            startActivity(new Intent(this, ExitOperation.class));
            finish();
        }

        btnentry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(DEVICE_SETUP_KEY, DEVICE_SETUP_ENTRY);
                editor.apply();

                startActivity(new Intent(WelcomeScreen.this, MainActivity.class));
                finish();
            }
        });

        btnexit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(DEVICE_SETUP_KEY, DEVICE_SETUP_EXIT);
                editor.apply();

                startActivity(new Intent(WelcomeScreen.this, ExitOperation.class));
                finish();
            }
        });
    }
}
