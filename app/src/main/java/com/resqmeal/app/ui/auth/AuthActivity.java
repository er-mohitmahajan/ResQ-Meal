package com.resqmeal.app.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.ActivityAuthBinding;

public class AuthActivity extends AppCompatActivity {

    public static final String EXTRA_START_DESTINATION = "extra_start_destination";
    public static final String DESTINATION_ONBOARDING = "destination_onboarding";
    public static final String DESTINATION_LOGIN = "destination_login";

    private ActivityAuthBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_auth);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            String target = getIntent().getStringExtra(EXTRA_START_DESTINATION);

            if (DESTINATION_LOGIN.equals(target)) {
                navController.navigate(R.id.action_onboardingFragment_to_loginFragment);
            }
        }
    }
}
