package com.resqmeal.app.ui.donor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentAddFoodBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.ui.common.LocationPickerActivity;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.GeoUtils;
import com.resqmeal.app.utils.Validators;
import com.resqmeal.app.viewmodel.FoodPostViewModel;
import com.resqmeal.app.views.PhotoPicker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AddFoodFragment extends Fragment {

    private static final String ARG_POST = "arg_post";

    private FragmentAddFoodBinding binding;
    private FoodPostViewModel viewModel;
    private FoodPost foodPost;
    private boolean isEditMode = false;
    private Uri photoUri;
    private String currentPhotoPath;

    private Calendar preparedCalendar = Calendar.getInstance();
    private Calendar expiryCalendar = Calendar.getInstance();
    private boolean defaultTimesInitialized;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public static AddFoodFragment newInstance(FoodPost post) {
        AddFoodFragment fragment = new AddFoodFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) openCamera();
                else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && photoUri != null) {
                    showSelectedPhoto(photoUri);
                } else {
                    photoUri = null;
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    photoUri = uri;
                    showSelectedPhoto(uri);
                }
            });

    private final ActivityResultLauncher<Intent> locationPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    double lat = data.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, Double.NaN);
                    double lng = data.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, Double.NaN);
                    String address = data.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS);
                    if (!GeoUtils.isValidCoordinates(lat, lng) || address == null || address.trim().isEmpty()) {
                        Toast.makeText(requireContext(), R.string.error_location_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    foodPost.setLat(lat);
                    foodPost.setLng(lng);
                    foodPost.setGeohash(GeoUtils.getGeohash(lat, lng));
                    foodPost.setPickupAddress(address.trim());
                    binding.inputPickupAddress.setText(foodPost.getPickupAddress());
                    binding.inputPickupAddress.setError(null);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(ARG_POST)) {
            foodPost = (FoodPost) getArguments().getSerializable(ARG_POST);
            isEditMode = true;
        } else {
            foodPost = new FoodPost();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddFoodBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FoodPostViewModel.class);

        setupDropdowns();
        setupDateTimePickers();
        setupListeners();
        setupObservers();

        if (isEditMode) {
            populateFields();
        }
    }

    private void setupDropdowns() {
        String[] units = {getString(R.string.unit_meals), getString(R.string.unit_kg), getString(R.string.unit_packets)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, units);
        binding.spinnerUnit.setAdapter(adapter);
        binding.spinnerUnit.setThreshold(0);
        binding.spinnerUnit.setOnClickListener(v -> binding.spinnerUnit.showDropDown());

        String[] categories = {
                getString(R.string.category_cooked_meal),
                getString(R.string.category_bakery),
                getString(R.string.category_fruits_veg),
                getString(R.string.category_packaged),
                getString(R.string.category_other)
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        binding.spinnerCategory.setAdapter(categoryAdapter);
        binding.spinnerCategory.setThreshold(0);
        binding.spinnerCategory.setOnClickListener(v -> binding.spinnerCategory.showDropDown());
    }

    private void setupDateTimePickers() {
        binding.inputPreparedAt.getEditText().setFocusable(false);
        binding.inputPreparedAt.getEditText().setOnClickListener(v -> showDateTimePicker(true));

        binding.inputExpiryTime.getEditText().setFocusable(false);
        binding.inputExpiryTime.getEditText().setOnClickListener(v -> showDateTimePicker(false));

        if (!isEditMode && !defaultTimesInitialized) {
            preparedCalendar = Calendar.getInstance();
            expiryCalendar = (Calendar) preparedCalendar.clone();
            expiryCalendar.add(Calendar.HOUR_OF_DAY, 4);
            defaultTimesInitialized = true;
            binding.inputPreparedAt.setText(dateTimeFormat.format(preparedCalendar.getTime()));
            binding.inputExpiryTime.setText(dateTimeFormat.format(expiryCalendar.getTime()));
        }
    }

    private void populateFields() {
        binding.inputTitle.setText(foodPost.getTitle());
        binding.inputDescription.setText(foodPost.getDescription());
        binding.inputQuantity.setText(String.valueOf(foodPost.getQuantity()));
        binding.inputServings.setText(String.valueOf(foodPost.getServings()));
        binding.spinnerUnit.setText(foodPost.getUnit(), false);
        binding.spinnerCategory.setText(getCategoryLabel(foodPost.getCategory()), false);

        if ("veg".equals(foodPost.getFoodType())) binding.chipVeg.setChecked(true);
        else if ("non_veg".equals(foodPost.getFoodType())) binding.chipNonVeg.setChecked(true);
        else if ("both".equals(foodPost.getFoodType())) binding.chipBoth.setChecked(true);

        if (foodPost.getPreparedAt() != null) {
            preparedCalendar.setTime(foodPost.getPreparedAt().toDate());
        }
        if (foodPost.getExpiryTime() != null) {
            expiryCalendar.setTime(foodPost.getExpiryTime().toDate());
        }
        binding.inputPreparedAt.setText(dateTimeFormat.format(preparedCalendar.getTime()));
        binding.inputExpiryTime.setText(dateTimeFormat.format(expiryCalendar.getTime()));

        binding.inputPickupAddress.setText(foodPost.getPickupAddress());
        binding.inputPickupInstructions.setText(foodPost.getPickupInstructions());

        if (foodPost.getPhotoUrl() != null && !foodPost.getPhotoUrl().trim().isEmpty()) {
            binding.ivFoodPhoto.setVisibility(View.VISIBLE);
            binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
            Glide.with(this).load(foodPost.getPhotoUrl()).into(binding.ivFoodPhoto);
        }

        binding.btnPublish.setButtonText("Update Post");
    }

    private void showDateTimePicker(boolean isPreparedAt) {
        Calendar selectedCalendar = (Calendar) (isPreparedAt ? preparedCalendar : expiryCalendar).clone();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(getUtcDateSelection(selectedCalendar))
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar selectedDateUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selectedDateUtc.setTimeInMillis(selection);
            Calendar calendar = (Calendar) selectedCalendar.clone();
            calendar.set(
                    selectedDateUtc.get(Calendar.YEAR),
                    selectedDateUtc.get(Calendar.MONTH),
                    selectedDateUtc.get(Calendar.DAY_OF_MONTH)
            );
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText("Select Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                
                if (isPreparedAt) {
                    preparedCalendar = calendar;
                    binding.inputPreparedAt.setText(dateTimeFormat.format(calendar.getTime()));
                } else {
                    expiryCalendar = calendar;
                    binding.inputExpiryTime.setText(dateTimeFormat.format(calendar.getTime()));
                }
            });
            timePicker.show(getParentFragmentManager(), "TIME_PICKER");
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private long getUtcDateSelection(Calendar calendar) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.clear();
        utcCalendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        return utcCalendar.getTimeInMillis();
    }

    private void setupListeners() {
        binding.cardPhoto.setOnClickListener(v -> PhotoPicker.show(requireContext(), new PhotoPicker.OnPhotoSourceSelectedListener() {
            @Override
            public void onCameraSelected() { checkCameraPermission(); }
            @Override
            public void onGallerySelected() { galleryLauncher.launch("image/*"); }
        }));

        binding.inputPickupAddress.getEditText().setFocusable(false);
        binding.inputPickupAddress.getEditText().setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LocationPickerActivity.class);
            if (foodPost.getLat() != null && foodPost.getLng() != null) {
                intent.putExtra(LocationPickerActivity.EXTRA_INITIAL_LAT, foodPost.getLat());
                intent.putExtra(LocationPickerActivity.EXTRA_INITIAL_LNG, foodPost.getLng());
            }
            locationPickerLauncher.launch(intent);
        });

        binding.btnPublish.setOnClickListener(v -> validateAndPublish());
    }

    private void setupObservers() {
        viewModel.getPublishStatus().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.btnPublish.setLoading(true);
                    break;
                case SUCCESS:
                    binding.btnPublish.setLoading(false);
                    showSuccessDialog();
                    break;
                case ERROR:
                    binding.btnPublish.setLoading(false);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });

        viewModel.getActionStatus().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.btnPublish.setLoading(true);
                    break;
                case SUCCESS:
                    binding.btnPublish.setLoading(false);
                    Toast.makeText(requireContext(), "Post updated successfully", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    break;
                case ERROR:
                    binding.btnPublish.setLoading(false);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void showSuccessDialog() {
        SuccessDialogFragment dialog = new SuccessDialogFragment();
        dialog.setOnDismissListener(() -> {
            getParentFragmentManager().popBackStack();
        });
        dialog.show(getParentFragmentManager(), "SUCCESS_DIALOG");
    }

    private void validateAndPublish() {
        String title = binding.inputTitle.getText();
        String description = binding.inputDescription.getText();
        String quantityStr = binding.inputQuantity.getText();
        String servingsStr = binding.inputServings.getText();
        String unit = getUnitValue(binding.spinnerUnit.getText().toString());
        String category = getCategoryValue(binding.spinnerCategory.getText().toString());

        int selectedTypeId = binding.chipGroupFoodType.getCheckedChipId();
        String foodType = "";
        if (selectedTypeId == R.id.chipVeg) foodType = Constants.FOOD_TYPE_VEG;
        else if (selectedTypeId == R.id.chipNonVeg) foodType = Constants.FOOD_TYPE_NON_VEG;
        else if (selectedTypeId == R.id.chipBoth) foodType = Constants.FOOD_TYPE_BOTH;
        if (foodType.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_food_type_required, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.inputTitle.setError(null);
        binding.inputDescription.setError(null);
        binding.inputQuantity.setError(null);
        binding.inputServings.setError(null);
        binding.inputPreparedAt.setError(null);
        binding.inputExpiryTime.setError(null);
        binding.inputPickupAddress.setError(null);
        binding.categoryInputLayout.setError(null);
        binding.unitInputLayout.setError(null);

        boolean isValid = true;
        if (!Validators.isValidFoodTitle(title)) {
            binding.inputTitle.setError(getString(R.string.error_invalid_title));
            isValid = false;
        }
        if (!Validators.isValidDescription(description)) {
            binding.inputDescription.setError(getString(R.string.error_invalid_description));
            isValid = false;
        }
        if (category == null) {
            binding.categoryInputLayout.setError(getString(R.string.error_category_required));
            isValid = false;
        }
        if (unit == null) {
            binding.unitInputLayout.setError(getString(R.string.error_unit_required));
            isValid = false;
        }

        double quantity = Double.NaN;
        try {
            quantity = Double.parseDouble(quantityStr);
        } catch (NumberFormatException ignored) {
        }
        if (!Validators.isValidQuantity(quantity)) {
            binding.inputQuantity.setError(getString(R.string.error_invalid_quantity));
            isValid = false;
        }

        long servings = 0;
        try {
            servings = Long.parseLong(servingsStr);
        } catch (NumberFormatException ignored) {
        }
        if (!Validators.isValidServings(servings)) {
            binding.inputServings.setError(getString(R.string.error_invalid_servings));
            isValid = false;
        }

        long now = System.currentTimeMillis();
        if (preparedCalendar.getTimeInMillis() > now) {
            binding.inputPreparedAt.setError(getString(R.string.error_invalid_prepared_at));
            isValid = false;
        }
        if (!Validators.isValidExpiryTime(
                preparedCalendar.getTimeInMillis(),
                expiryCalendar.getTimeInMillis(),
                now
        )) {
            binding.inputExpiryTime.setError(getString(R.string.error_invalid_expiry));
            isValid = false;
        }

        if (photoUri == null && (foodPost.getPhotoUrl() == null || foodPost.getPhotoUrl().trim().isEmpty())) {
            Toast.makeText(requireContext(), R.string.error_photo_required, Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String pickupAddress = binding.inputPickupAddress.getText();
        if (pickupAddress.isEmpty()
                || !GeoUtils.isValidCoordinates(
                        foodPost.getLat() == null ? Double.NaN : foodPost.getLat(),
                        foodPost.getLng() == null ? Double.NaN : foodPost.getLng()
                )) {
            binding.inputPickupAddress.setError(getString(R.string.error_location_required));
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        foodPost.setTitle(title);
        foodPost.setDescription(description);
        foodPost.setCategory(category);
        foodPost.setQuantity(quantity);
        foodPost.setServings(servings);
        foodPost.setUnit(unit);
        foodPost.setFoodType(foodType);
        foodPost.setPickupAddress(pickupAddress);
        foodPost.setGeohash(GeoUtils.getGeohash(foodPost.getLat(), foodPost.getLng()));
        foodPost.setPreparedAt(new Timestamp(preparedCalendar.getTime()));
        foodPost.setExpiryTime(new Timestamp(expiryCalendar.getTime()));
        foodPost.setPickupInstructions(binding.inputPickupInstructions.getText());

        if (isEditMode) {
            viewModel.updatePost(foodPost, photoUri);
        } else {
            viewModel.publishPost(foodPost, photoUri);
        }
    }

    private String getUnitValue(String label) {
        if (getString(R.string.unit_meals).equals(label)) return Constants.UNIT_MEALS;
        if (getString(R.string.unit_kg).equals(label)) return Constants.UNIT_KG;
        if (getString(R.string.unit_packets).equals(label)) return Constants.UNIT_PACKETS;
        return null;
    }

    private String getCategoryValue(String label) {
        if (getString(R.string.category_cooked_meal).equals(label)) return Constants.CATEGORY_COOKED_MEAL;
        if (getString(R.string.category_bakery).equals(label)) return Constants.CATEGORY_BAKERY;
        if (getString(R.string.category_fruits_veg).equals(label)) return Constants.CATEGORY_FRUITS_VEG;
        if (getString(R.string.category_packaged).equals(label)) return Constants.CATEGORY_PACKAGED;
        if (getString(R.string.category_other).equals(label)) return Constants.CATEGORY_OTHER;
        return null;
    }

    private String getCategoryLabel(String category) {
        if (Constants.CATEGORY_COOKED_MEAL.equals(category)) return getString(R.string.category_cooked_meal);
        if (Constants.CATEGORY_BAKERY.equals(category)) return getString(R.string.category_bakery);
        if (Constants.CATEGORY_FRUITS_VEG.equals(category)) return getString(R.string.category_fruits_veg);
        if (Constants.CATEGORY_PACKAGED.equals(category)) return getString(R.string.category_packaged);
        if (Constants.CATEGORY_OTHER.equals(category)) return getString(R.string.category_other);
        return "";
    }

    private void showSelectedPhoto(Uri uri) {
        binding.ivFoodPhoto.setVisibility(View.VISIBLE);
        binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
        Glide.with(this).load(uri).into(binding.ivFoodPhoto);
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
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(requireContext(), "com.resqmeal.app.fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(takePictureIntent);
        } catch (IOException ex) {
            Toast.makeText(requireContext(), "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = requireActivity().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("Unable to access picture storage");
        }
        return File.createTempFile("FOOD_" + timeStamp + "_", ".jpg", storageDir);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
