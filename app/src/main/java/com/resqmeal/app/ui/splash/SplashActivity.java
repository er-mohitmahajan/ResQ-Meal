package com.resqmeal.app.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.resqmeal.app.databinding.ActivitySplashBinding;
import com.resqmeal.app.ui.auth.AuthActivity;
import com.resqmeal.app.utils.SharedPrefsHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        handler.postDelayed(this::routeToNextScreen, SPLASH_DURATION_MS);
    }

    private void routeToNextScreen() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        boolean onboardingCompleted = SharedPrefsHelper.isOnboardingCompleted(this);

        Intent intent = new Intent(this, AuthActivity.class);
        if (!onboardingCompleted) {
            intent.putExtra(AuthActivity.EXTRA_START_DESTINATION, AuthActivity.DESTINATION_ONBOARDING);
        } else {
            intent.putExtra(AuthActivity.EXTRA_START_DESTINATION, AuthActivity.DESTINATION_LOGIN);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
