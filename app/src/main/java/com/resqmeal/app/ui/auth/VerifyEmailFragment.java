package com.resqmeal.app.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseUser;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentVerifyEmailBinding;
import com.resqmeal.app.viewmodel.AuthViewModel;

public class VerifyEmailFragment extends Fragment {

    private FragmentVerifyEmailBinding binding;
    private AuthViewModel authViewModel;
    private CountDownTimer cooldownTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVerifyEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        displayUserEmail();
        setupListeners();
    }

    private void displayUserEmail() {
        FirebaseUser user = authViewModel.getCurrentFirebaseUser();
        if (user != null && user.getEmail() != null) {
            String message = "We have sent a verification link to:\n" + user.getEmail()
                    + "\n\nPlease click the link to activate your account before proceeding.";
            binding.tvVerifyMessage.setText(message);
        }
    }

    private void setupListeners() {
        binding.btnCheckVerified.setOnClickListener(v -> checkVerificationStatus());

        binding.btnResendEmail.setOnClickListener(v -> resendVerificationEmail());

        binding.btnBackToLogin.setOnClickListener(v -> {
            authViewModel.logout();
            Navigation.findNavController(v).navigate(R.id.action_verifyEmailFragment_to_loginFragment);
        });
    }

    private void checkVerificationStatus() {
        binding.btnCheckVerified.setLoading(true);

        authViewModel.checkEmailVerified().observe(getViewLifecycleOwner(), resource -> {
            binding.btnCheckVerified.setLoading(false);

            if (resource.isError()) {
                Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
            } else if (resource.isSuccess()) {
                Boolean isVerified = resource.getData();
                if (Boolean.TRUE.equals(isVerified)) {
                    Toast.makeText(requireContext(), "Email verified! You can now sign in.", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigate(R.id.action_verifyEmailFragment_to_loginFragment);
                } else {
                    Toast.makeText(requireContext(), "Your email is not verified yet. Please check your inbox.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void resendVerificationEmail() {
        binding.btnResendEmail.setEnabled(false);

        authViewModel.sendEmailVerification().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                Toast.makeText(requireContext(), "Verification email resent. Check your inbox and spam folder.", Toast.LENGTH_SHORT).show();
                startResendCooldown();
            } else if (resource.isError()) {
                binding.btnResendEmail.setEnabled(true);
                Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startResendCooldown() {
        cooldownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isAdded() && binding != null) {
                    binding.btnResendEmail.setText("Resend in " + (millisUntilFinished / 1000) + "s");
                }
            }

            @Override
            public void onFinish() {
                if (isAdded() && binding != null) {
                    binding.btnResendEmail.setEnabled(true);
                    binding.btnResendEmail.setText("Resend Verification Email");
                }
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
        }
        super.onDestroyView();
        binding = null;
    }
}
