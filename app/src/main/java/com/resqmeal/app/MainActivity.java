package com.resqmeal.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.resqmeal.app.databinding.ActivityMainBinding;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.views.ConfirmDialog;
import com.resqmeal.app.views.PhotoPicker;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStatusChips();
        setupPrimaryButtonDemo();
        setupSampleFoodCard();
        setupDialogDemo();
    }

    private void setupStatusChips() {
        binding.chipAvailable.setStatus(Constants.STATUS_AVAILABLE);
        binding.chipClaimed.setStatus(Constants.STATUS_CLAIMED);
        binding.chipCompleted.setStatus(Constants.STATUS_COMPLETED);
        binding.chipExpired.setStatus(Constants.STATUS_EXPIRED);
        binding.chipCancelled.setStatus(Constants.STATUS_CANCELLED);
    }

    private void setupPrimaryButtonDemo() {
        binding.btnDemoPrimary.setOnClickListener(v -> {
            binding.btnDemoPrimary.setLoading(true);
            mainHandler.postDelayed(() -> {
                binding.btnDemoPrimary.setLoading(false);
                Toast.makeText(this, "Action completed!", Toast.LENGTH_SHORT).show();
            }, 2000);
        });
    }

    private void setupSampleFoodCard() {
        binding.sampleFoodCard.tvFoodTitle.setText("Veg Biryani (Cooked)");
        binding.sampleFoodCard.tvQuantityServings.setText("15 meals • feeds ~15 people");
        binding.sampleFoodCard.tvDistance.setText("📍 2.4 km away");
        binding.sampleFoodCard.tvExpiryCountdown.setText("⏱ Expires in 45m");
        binding.sampleFoodCard.chipStatus.setStatus(Constants.STATUS_AVAILABLE);
    }

    private void setupDialogDemo() {
        binding.btnTestConfirmDialog.setOnClickListener(v -> {
            ConfirmDialog.show(
                    this,
                    "Claim Surplus Food?",
                    "Are you sure you want to claim this food? You will receive a 4-digit pickup code.",
                    "Yes, Claim",
                    "Cancel",
                    () -> Toast.makeText(this, "Confirmed!", Toast.LENGTH_SHORT).show(),
                    () -> Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            );
        });

        binding.btnTestPhotoPicker.setOnClickListener(v -> {
            PhotoPicker.show(this, new PhotoPicker.OnPhotoSourceSelectedListener() {
                @Override
                public void onCameraSelected() {
                    Toast.makeText(MainActivity.this, "Camera selected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onGallerySelected() {
                    Toast.makeText(MainActivity.this, "Gallery selected", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
