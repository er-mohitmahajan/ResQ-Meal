package com.resqmeal.app.ui.receiver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.LayoutFilterBottomSheetBinding;
import com.resqmeal.app.utils.Constants;

public class FeedFilterBottomSheet extends BottomSheetDialogFragment {
    public static final String REQUEST_KEY = "feed_filter_result";
    public static final String KEY_RADIUS_KM = "radius_km";
    public static final String KEY_FOOD_TYPE = "food_type";
    public static final String KEY_CATEGORY = "category";

    private static final String ARG_RADIUS_KM = "arg_radius_km";
    private static final String ARG_FOOD_TYPE = "arg_food_type";
    private static final String ARG_CATEGORY = "arg_category";

    private LayoutFilterBottomSheetBinding binding;

    public static FeedFilterBottomSheet newInstance(
            double radiusKm,
            @Nullable String foodType,
            @Nullable String category
    ) {
        FeedFilterBottomSheet sheet = new FeedFilterBottomSheet();
        Bundle arguments = new Bundle();
        arguments.putDouble(ARG_RADIUS_KM, radiusKm);
        arguments.putString(ARG_FOOD_TYPE, foodType);
        arguments.putString(ARG_CATEGORY, category);
        sheet.setArguments(arguments);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = LayoutFilterBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        selectDistance(getArguments() == null
                ? Constants.DEFAULT_RADIUS_KM
                : getArguments().getDouble(ARG_RADIUS_KM, Constants.DEFAULT_RADIUS_KM));
        selectFoodType(getArguments() == null ? null : getArguments().getString(ARG_FOOD_TYPE));
        selectCategory(getArguments() == null ? null : getArguments().getString(ARG_CATEGORY));

        binding.btnApplyFilters.setOnClickListener(v -> applyFilters());
    }

    private void selectDistance(double radiusKm) {
        int chipId;
        if (radiusKm == 2) {
            chipId = R.id.chip2km;
        } else if (radiusKm == 5) {
            chipId = R.id.chip5km;
        } else if (radiusKm == 25) {
            chipId = R.id.chip25km;
        } else {
            chipId = R.id.chip10km;
        }
        binding.chipGroupDistance.check(chipId);
    }

    private void selectFoodType(@Nullable String foodType) {
        int chipId;
        if (Constants.FOOD_TYPE_VEG.equals(foodType)) {
            chipId = R.id.chipVeg;
        } else if (Constants.FOOD_TYPE_NON_VEG.equals(foodType)) {
            chipId = R.id.chipNonVeg;
        } else if (Constants.FOOD_TYPE_BOTH.equals(foodType)) {
            chipId = R.id.chipFoodBoth;
        } else {
            chipId = R.id.chipFoodAny;
        }
        binding.chipGroupFoodType.check(chipId);
    }

    private void selectCategory(@Nullable String category) {
        int chipId;
        if (Constants.CATEGORY_COOKED_MEAL.equals(category)) {
            chipId = R.id.chipCategoryCookedMeal;
        } else if (Constants.CATEGORY_BAKERY.equals(category)) {
            chipId = R.id.chipCategoryBakery;
        } else if (Constants.CATEGORY_FRUITS_VEG.equals(category)) {
            chipId = R.id.chipCategoryFruitsVeg;
        } else if (Constants.CATEGORY_PACKAGED.equals(category)) {
            chipId = R.id.chipCategoryPackaged;
        } else if (Constants.CATEGORY_OTHER.equals(category)) {
            chipId = R.id.chipCategoryOther;
        } else {
            chipId = R.id.chipCategoryAny;
        }
        binding.chipGroupCategory.check(chipId);
    }

    private void applyFilters() {
        double radiusKm;
        int selectedDistanceId = binding.chipGroupDistance.getCheckedChipId();
        if (selectedDistanceId == R.id.chip2km) {
            radiusKm = 2;
        } else if (selectedDistanceId == R.id.chip5km) {
            radiusKm = 5;
        } else if (selectedDistanceId == R.id.chip25km) {
            radiusKm = 25;
        } else {
            radiusKm = 10;
        }

        String foodType = null;
        int selectedFoodTypeId = binding.chipGroupFoodType.getCheckedChipId();
        if (selectedFoodTypeId == R.id.chipVeg) {
            foodType = Constants.FOOD_TYPE_VEG;
        } else if (selectedFoodTypeId == R.id.chipNonVeg) {
            foodType = Constants.FOOD_TYPE_NON_VEG;
        } else if (selectedFoodTypeId == R.id.chipFoodBoth) {
            foodType = Constants.FOOD_TYPE_BOTH;
        }

        String category = null;
        int selectedCategoryId = binding.chipGroupCategory.getCheckedChipId();
        if (selectedCategoryId == R.id.chipCategoryCookedMeal) {
            category = Constants.CATEGORY_COOKED_MEAL;
        } else if (selectedCategoryId == R.id.chipCategoryBakery) {
            category = Constants.CATEGORY_BAKERY;
        } else if (selectedCategoryId == R.id.chipCategoryFruitsVeg) {
            category = Constants.CATEGORY_FRUITS_VEG;
        } else if (selectedCategoryId == R.id.chipCategoryPackaged) {
            category = Constants.CATEGORY_PACKAGED;
        } else if (selectedCategoryId == R.id.chipCategoryOther) {
            category = Constants.CATEGORY_OTHER;
        }

        Bundle result = new Bundle();
        result.putDouble(KEY_RADIUS_KM, radiusKm);
        result.putString(KEY_FOOD_TYPE, foodType);
        result.putString(KEY_CATEGORY, category);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
