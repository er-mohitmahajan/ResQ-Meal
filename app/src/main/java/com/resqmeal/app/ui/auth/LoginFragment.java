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

import com.google.firebase.auth.FirebaseUser;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentLoginBinding;
import com.resqmeal.app.utils.Validators;
import com.resqmeal.app.viewmodel.AuthViewModel;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.btnGoToRegister.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
        });

        binding.btnForgotPassword.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_forgotPasswordFragment);
        });
    }

    private void performLogin() {
        String email = binding.inputEmail.getText();
        String password = binding.inputPassword.getText();

        binding.inputEmail.setError(null);
        binding.inputPassword.setError(null);

        boolean hasError = false;

        if (!Validators.isValidEmail(email)) {
            binding.inputEmail.setError("Enter a valid email address");
            hasError = true;
        }

        if (password.isEmpty()) {
            binding.inputPassword.setError("Password cannot be empty");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        binding.btnLogin.setLoading(true);

        authViewModel.login(email, password).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isLoading()) {
                binding.btnLogin.setLoading(true);
            } else if (resource.isError()) {
                binding.btnLogin.setLoading(false);
                Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_LONG).show();
            } else if (resource.isSuccess()) {
                binding.btnLogin.setLoading(false);
                FirebaseUser fbUser = authViewModel.getCurrentFirebaseUser();
                if (fbUser != null && !fbUser.isEmailVerified()) {
                    Toast.makeText(requireContext(), "Please verify your email address to continue.", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_verifyEmailFragment);
                } else {
                    String name = resource.getData() != null ? resource.getData().getName() : "User";
                    Toast.makeText(requireContext(), "Signed in successfully! Welcome, " + name, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
