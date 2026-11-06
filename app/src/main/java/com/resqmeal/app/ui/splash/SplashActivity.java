package com.resqmeal.app.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.resqmeal.app.R;
import com.resqmeal.app.model.User;
import com.resqmeal.app.databinding.ActivitySplashBinding;
import com.resqmeal.app.ui.auth.AuthActivity;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.RoleRouter;
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

        if (!onboardingCompleted) {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.putExtra(AuthActivity.EXTRA_START_DESTINATION, AuthActivity.DESTINATION_ONBOARDING);
            startActivity(intent);
            finish();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            RoleRouter.routeToLogin(this);
            return;
        }

        currentUser.reload().addOnCompleteListener(this, reloadTask -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (!reloadTask.isSuccessful()) {
                Toast.makeText(this, R.string.session_refresh_failed, Toast.LENGTH_LONG).show();
                RoleRouter.routeToLogin(this);
                return;
            }
            if (!currentUser.isEmailVerified()) {
                Intent intent = new Intent(this, AuthActivity.class);
                intent.putExtra(AuthActivity.EXTRA_START_DESTINATION, AuthActivity.DESTINATION_VERIFY_EMAIL);
                startActivity(intent);
                finish();
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection(Constants.COLLECTION_USERS)
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(this, snapshot -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (!snapshot.exists()) {
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(this, R.string.account_profile_missing, Toast.LENGTH_LONG).show();
                            RoleRouter.routeToLogin(this);
                            return;
                        }

                        User user = snapshot.toObject(User.class);
                        if (user == null) {
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(this, R.string.account_profile_invalid, Toast.LENGTH_LONG).show();
                            RoleRouter.routeToLogin(this);
                            return;
                        }
                        RoleRouter.route(this, user);
                    })
                    .addOnFailureListener(this, error -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(this, getString(R.string.session_load_failed, error.getMessage()),
                                    Toast.LENGTH_LONG).show();
                            RoleRouter.routeToLogin(this);
                        }
                    });
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
