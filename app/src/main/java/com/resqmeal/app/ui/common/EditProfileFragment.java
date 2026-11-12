package com.resqmeal.app.ui.common;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentEditProfileBinding;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.GeoUtils;
import com.resqmeal.app.utils.Validators;
import com.resqmeal.app.viewmodel.ProfileViewModel;
import com.resqmeal.app.views.PhotoPicker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private ProfileViewModel viewModel;
    private Uri photoUri;
    private String currentPhotoPath;

    private Double selectedLat;
    private Double selectedLng;
    private String selectedGeohash;
    private boolean profileFieldsLoaded;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && photoUri != null) {
                    viewModel.uploadProfilePhoto(photoUri);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    viewModel.uploadProfilePhoto(uri);
                }
            });

    private final ActivityResultLauncher<Intent> locationPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra(LocationPickerActivity.EXTRA_LAT, Double.NaN);
                    double lng = result.getData().getDoubleExtra(LocationPickerActivity.EXTRA_LNG, Double.NaN);
                    String geohash = result.getData().getStringExtra(LocationPickerActivity.EXTRA_GEOHASH);
                    String address = result.getData().getStringExtra(LocationPickerActivity.EXTRA_ADDRESS);
                    if (!GeoUtils.isValidCoordinates(lat, lng)
                            || geohash == null
                            || !geohash.equals(GeoUtils.getGeohash(lat, lng))
                            || address == null
                            || address.trim().isEmpty()) {
                        Toast.makeText(requireContext(), R.string.location_required, Toast.LENGTH_LONG).show();
                        return;
                    }
                    selectedLat = lat;
                    selectedLng = lng;
                    selectedGeohash = geohash;
                    binding.inputAddress.setText(address);
                    binding.inputAddress.setError(null);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupObservers();
        setupListeners();

        viewModel.fetchCurrentUser();
    }

    private void setupObservers() {
        viewModel.getUserData().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == com.resqmeal.app.utils.Resource.Status.SUCCESS && resource.data != null) {
                updateUI(resource.data);
            }
        });

        viewModel.getUpdateStatus().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.loadingOverlay.setVisibility(View.VISIBLE);
                    binding.btnSave.setEnabled(false);
                    binding.fabChangePhoto.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.loadingOverlay.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    binding.fabChangePhoto.setEnabled(true);
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    break;
                case ERROR:
                    binding.loadingOverlay.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    binding.fabChangePhoto.setEnabled(true);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getProfilePhotoStatus().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.loadingOverlay.setVisibility(View.VISIBLE);
                    binding.btnSave.setEnabled(false);
                    binding.fabChangePhoto.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.loadingOverlay.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    binding.fabChangePhoto.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    binding.loadingOverlay.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    binding.fabChangePhoto.setEnabled(true);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void setupListeners() {
        binding.fabChangePhoto.setOnClickListener(v -> PhotoPicker.show(requireContext(), new PhotoPicker.OnPhotoSourceSelectedListener() {
            @Override
            public void onCameraSelected() {
                checkCameraPermission();
            }

            @Override
            public void onGallerySelected() {
                galleryLauncher.launch("image/*");
            }
        }));

        binding.inputAddress.getEditText().setFocusable(false);
        binding.inputAddress.getEditText().setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LocationPickerActivity.class);
            if (selectedLat != null && selectedLng != null
                    && GeoUtils.isValidCoordinates(selectedLat, selectedLng)) {
                intent.putExtra(LocationPickerActivity.EXTRA_INITIAL_LAT, selectedLat);
                intent.putExtra(LocationPickerActivity.EXTRA_INITIAL_LNG, selectedLng);
            }
            locationPickerLauncher.launch(intent);
        });

        binding.btnSave.setOnClickListener(v -> {
            String name = binding.inputName.getText();
            String phone = binding.inputPhone.getText();
            String address = binding.inputAddress.getText();

            binding.inputName.setError(null);
            binding.inputPhone.setError(null);
            binding.inputAddress.setError(null);
            if (!Validators.isValidName(name)) {
                binding.inputName.setError(getString(R.string.error_invalid_name));
                return;
            }
            if (!Validators.isValidPhone(phone)) {
                binding.inputPhone.setError(getString(R.string.error_invalid_phone));
                return;
            }
            if (address.isEmpty() || selectedLat == null || selectedLng == null
                    || selectedGeohash == null
                    || !GeoUtils.isValidCoordinates(selectedLat, selectedLng)
                    || !selectedGeohash.equals(GeoUtils.getGeohash(selectedLat, selectedLng))) {
                binding.inputAddress.setError(getString(R.string.location_required));
                return;
            }

            viewModel.updateProfile(name, phone, address, selectedLat, selectedLng, selectedGeohash);
        });
    }

    private void updateUI(User user) {
        if (!profileFieldsLoaded) {
            binding.inputName.setText(user.getName());
            binding.inputPhone.setText(user.getPhone());
            binding.inputAddress.setText(user.getAddress());

            selectedLat = user.getLat();
            selectedLng = user.getLng();
            selectedGeohash = user.getGeohash();
            profileFieldsLoaded = true;
        }

        Glide.with(this)
                .load(user.getProfilePhotoUrl())
                .placeholder(R.drawable.ic_nav_profile)
                .error(R.drawable.ic_nav_profile)
                .into(binding.ivProfilePhoto);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(requireContext(), "Error creating file", Toast.LENGTH_SHORT).show();
        }
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(requireContext(),
                    "com.resqmeal.app.fileprovider",
                    photoFile);
            if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) == null) {
                Toast.makeText(requireContext(), R.string.camera_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
