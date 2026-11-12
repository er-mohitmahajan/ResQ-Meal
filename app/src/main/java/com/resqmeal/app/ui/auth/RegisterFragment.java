package com.resqmeal.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentRegisterBinding;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.Validators;
import com.resqmeal.app.viewmodel.AuthViewModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    private String selectedRole = Constants.ROLE_DONOR;
    private String selectedSubtype = "";

    private final Map<String, String> donorTypes = new LinkedHashMap<>();
    private final Map<String, String> receiverTypes = new LinkedHashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initTypeMaps();
        setupRoleToggle();
        setupDropdown();
        setupListeners();
    }

    private void initTypeMaps() {
        donorTypes.put("Restaurant", Constants.DONOR_TYPE_RESTAURANT);
        donorTypes.put("Hostel Mess", Constants.DONOR_TYPE_HOSTEL_MESS);
        donorTypes.put("Event Organizer", Constants.DONOR_TYPE_EVENT);
        donorTypes.put("Individual Donor", Constants.DONOR_TYPE_INDIVIDUAL);

        receiverTypes.put("NGO", Constants.RECEIVER_TYPE_NGO);
        receiverTypes.put("Shelter", Constants.RECEIVER_TYPE_SHELTER);
        receiverTypes.put("Volunteer", Constants.RECEIVER_TYPE_VOLUNTEER);
    }

    private void setupRoleToggle() {
        binding.toggleGroupRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnRoleDonor) {
                    selectedRole = Constants.ROLE_DONOR;
                    binding.tvTypeLabel.setText("Donor Category:");
                } else if (checkedId == R.id.btnRoleReceiver) {
                    selectedRole = Constants.ROLE_RECEIVER;
                    binding.tvTypeLabel.setText("Receiver Category:");
                }
                setupDropdown();
            }
        });
    }

    private void setupDropdown() {
        Map<String, String> currentMap = Constants.ROLE_DONOR.equals(selectedRole) ? donorTypes : receiverTypes;
        String[] displayLabels = currentMap.keySet().toArray(new String[0]);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayLabels
        );
        binding.autoCompleteType.setAdapter(adapter);

        if (displayLabels.length > 0) {
            binding.autoCompleteType.setText(displayLabels[0], false);
            selectedSubtype = currentMap.get(displayLabels[0]);
        }

        binding.autoCompleteType.setOnItemClickListener((parent, view, position, id) -> {
            String label = (String) parent.getItemAtPosition(position);
            selectedSubtype = currentMap.get(label);
        });
    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> performRegistration());

        binding.btnGoToLogin.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }

    private void performRegistration() {
        String name = binding.inputName.getText();
        String email = binding.inputEmail.getText();
        String phone = binding.inputPhone.getText();
        String password = binding.inputPassword.getText();

        binding.inputName.setError(null);
        binding.inputEmail.setError(null);
        binding.inputPhone.setError(null);
        binding.inputPassword.setError(null);

        boolean hasError = false;

        if (!Validators.isValidName(name)) {
            binding.inputName.setError("Name must be between 3 and 50 characters");
            hasError = true;
        }

        if (!Validators.isValidEmail(email)) {
            binding.inputEmail.setError("Enter a valid email address");
            hasError = true;
        }

        if (!Validators.isValidPhone(phone)) {
            binding.inputPhone.setError("Phone must be exactly 10 digits");
            hasError = true;
        }

        if (!Validators.isValidPassword(password)) {
            binding.inputPassword.setError("Password must be at least 8 chars with 1 letter & 1 number");
            hasError = true;
        }

        if (selectedSubtype == null || selectedSubtype.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            hasError = true;
        }

        if (hasError) {
            return;
        }

        binding.btnRegister.setLoading(true);

        authViewModel.register(name, email, phone, password, selectedRole, selectedSubtype)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource.isLoading()) {
                        binding.btnRegister.setLoading(true);
                    } else if (resource.isError()) {
                        binding.btnRegister.setLoading(false);
                        Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_LONG).show();
                    } else if (resource.isSuccess()) {
                        binding.btnRegister.setLoading(false);
                        Toast.makeText(requireContext(), "Account created! Verification email sent.", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigate(R.id.action_registerFragment_to_verifyEmailFragment);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

