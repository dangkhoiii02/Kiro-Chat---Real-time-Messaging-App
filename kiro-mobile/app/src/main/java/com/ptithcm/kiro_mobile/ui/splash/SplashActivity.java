package com.ptithcm.kiro_mobile.ui.splash;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.kiro_mobile.data.auth.TokenManager;
import com.ptithcm.kiro_mobile.ui.auth.LoginActivity;
import com.ptithcm.kiro_mobile.ui.main.MainActivity;

/**
 * Entry point — routes to Login or Main based on session state.
 * No layout needed; finishes immediately.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TokenManager tokenManager = TokenManager.getInstance(this);

        if (tokenManager.hasSession()) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
