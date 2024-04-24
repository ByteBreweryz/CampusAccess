package com.nteksystem.campusaccess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashScreen extends AppCompatActivity {
    private static final String PREF_NAME = "MyPrefs";
    private static final String DEVICE_SETUP_KEY = "device_setup";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
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

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceSetupStatus = sharedPreferences.getString(DEVICE_SETUP_KEY, "");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                if (deviceSetupStatus.equals("entry")) {
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                } else if (deviceSetupStatus.equals("exit")) {
                    intent = new Intent(SplashScreen.this, ExitOperation.class); // Replace ExitScreen with your exit activity
                } else {
                    intent = new Intent(SplashScreen.this, WelcomeScreen.class);
                }
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}