package com.resqmeal.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentForgotPasswordBinding;
import com.resqmeal.app.utils.Validators;
import com.resqmeal.app.viewmodel.AuthViewModel;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnSendReset.setOnClickListener(v -> performPasswordReset());

        binding.btnBackToLogin.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_forgotPasswordFragment_to_loginFragment);
        });
    }

    private void performPasswordReset() {
        String email = binding.inputResetEmail.getText();
        binding.inputResetEmail.setError(null);

        if (!Validators.isValidEmail(email)) {
            binding.inputResetEmail.setError("Enter a valid email address");
            return;
        }

        binding.btnSendReset.setLoading(true);

        authViewModel.sendPasswordReset(email).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isLoading()) {
                binding.btnSendReset.setLoading(true);
            } else if (resource.isError()) {
                binding.btnSendReset.setLoading(false);
                Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_LONG).show();
            } else if (resource.isSuccess()) {
                binding.btnSendReset.setLoading(false);
                Toast.makeText(requireContext(), "Password reset link sent to your email.", Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).navigate(R.id.action_forgotPasswordFragment_to_loginFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

