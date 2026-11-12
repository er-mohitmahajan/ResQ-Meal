package com.resqmeal.app.ui.receiver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.adapter.FoodPostAdapter;
import com.resqmeal.app.databinding.FragmentFeedBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.GeoUtils;
import com.resqmeal.app.utils.LocationHelper;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.FoodPostViewModel;
import com.resqmeal.app.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {
    private static final String PREF_PERMISSION_REQUESTED = "location_permission_requested";

    private FragmentFeedBinding binding;
    private FoodPostViewModel foodPostViewModel;
    private ProfileViewModel profileViewModel;
    private LocationHelper locationHelper;
    private FoodPostAdapter adapter;

    private double radiusKm = Constants.DEFAULT_RADIUS_KM;
    private String selectedFoodType;
    private String selectedCategory;
    private Double savedLatitude;
    private Double savedLongitude;
    private double searchLatitude;
    private double searchLongitude;
    private boolean profileLocationLoaded;
    private boolean waitingForProfileLocation;
    private boolean firstResume = true;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    loadNearbyFeed();
                } else {
                    loadSavedLocationOrShowError();
                }
            });

    private final ActivityResultLauncher<Intent> appSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    loadNearbyFeed());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        foodPostViewModel = new ViewModelProvider(requireActivity()).get(FoodPostViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        locationHelper = new LocationHelper(requireContext());
        radiusKm = foodPostViewModel.getNearbyRadiusKm();
        selectedFoodType = foodPostViewModel.getNearbyFoodType();
        selectedCategory = foodPostViewModel.getNearbyCategory();

        setupFeed();
        setupObservers();
        setupFilterResultListener();

        profileViewModel.fetchCurrentUser();
        loadNearbyFeed();
    }

    private void setupFeed() {
        adapter = new FoodPostAdapter(post -> openFoodDetail(post));
        binding.rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFeed.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::loadNearbyFeed);
        binding.fabFilter.setOnClickListener(v -> FeedFilterBottomSheet
                .newInstance(radiusKm, selectedFoodType, selectedCategory)
                .show(getParentFragmentManager(), "feed_filters"));
    }

    private void setupObservers() {
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                User user = resource.data;
                savedLatitude = user == null ? null : user.getLat();
                savedLongitude = user == null ? null : user.getLng();
                profileLocationLoaded = true;
                if (waitingForProfileLocation) {
                    loadSavedLocationOrShowError();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                profileLocationLoaded = true;
                if (waitingForProfileLocation) {
                    loadSavedLocationOrShowError();
                }
            }
        });

        foodPostViewModel.getNearbyPosts().observe(getViewLifecycleOwner(), this::renderPosts);
    }

    private void setupFilterResultListener() {
        getParentFragmentManager().setFragmentResultListener(
                FeedFilterBottomSheet.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    radiusKm = result.getDouble(
                            FeedFilterBottomSheet.KEY_RADIUS_KM,
                            Constants.DEFAULT_RADIUS_KM);
                    selectedFoodType = result.getString(FeedFilterBottomSheet.KEY_FOOD_TYPE);
                    selectedCategory = result.getString(FeedFilterBottomSheet.KEY_CATEGORY);
                    if (GeoUtils.isValidCoordinates(searchLatitude, searchLongitude)) {
                        fetchPostsAtCurrentLocation();
                    } else {
                        loadNearbyFeed();
                    }
                });
    }

    private void loadNearbyFeed() {
        if (binding == null) {
            return;
        }
        beginLoading();
        if (LocationHelper.hasLocationPermissions(requireContext())) {
            locationHelper.getCurrentLocation(new LocationHelper.LocationListener() {
                @Override
                public void onLocationReceived(android.location.Location location) {
                    if (binding == null) {
                        return;
                    }
                    searchLatitude = location.getLatitude();
                    searchLongitude = location.getLongitude();
                    fetchPostsAtCurrentLocation();
                }

                @Override
                public void onError(String error) {
                    if (binding == null) {
                        return;
                    }
                    if (profileLocationLoaded) {
                        loadSavedLocationOrShowError(error);
                    } else {
                        waitingForProfileLocation = true;
                    }
                }
            });
        } else {
            loadSavedLocationOrShowError();
        }
    }

    private void loadSavedLocationOrShowError() {
        loadSavedLocationOrShowError(null);
    }

    private void loadSavedLocationOrShowError(@Nullable String locationError) {
        waitingForProfileLocation = false;
        if (savedLatitude != null && savedLongitude != null
                && GeoUtils.isValidCoordinates(savedLatitude, savedLongitude)) {
            searchLatitude = savedLatitude;
            searchLongitude = savedLongitude;
            if (locationError != null && isAdded()) {
                Toast.makeText(requireContext(), "Using your saved profile location", Toast.LENGTH_SHORT).show();
            }
            fetchPostsAtCurrentLocation();
            return;
        }

        if (!profileLocationLoaded) {
            waitingForProfileLocation = true;
            return;
        }
        showLocationRequired(locationError);
    }

    private void showLocationRequired(@Nullable String locationError) {
        if (binding == null) {
            return;
        }
        stopLoading();
        binding.swipeRefresh.setRefreshing(false);
        binding.rvFeed.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.emptyState.setContent(
                "Location needed",
                locationError == null
                        ? "Allow location access to find nearby food, or add a location to your profile."
                        : "Could not determine your location. Add a location to your profile or try again.");
        binding.emptyState.setAction(
                permissionActionText(),
                v -> requestLocationPermissionOrOpenSettings());
    }

    private String permissionActionText() {
        SharedPreferences preferences = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean previouslyRequested = preferences.getBoolean(PREF_PERMISSION_REQUESTED, false);
        boolean canExplain = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (previouslyRequested && !canExplain) {
            return "Open Settings";
        }
        return "Allow Location";
    }

    private void requestLocationPermissionOrOpenSettings() {
        SharedPreferences preferences = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean previouslyRequested = preferences.getBoolean(PREF_PERMISSION_REQUESTED, false);
        boolean canExplain = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (previouslyRequested && !canExplain) {
            Intent settingsIntent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireContext().getPackageName(), null));
            appSettingsLauncher.launch(settingsIntent);
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Allow location access")
                .setMessage("Location helps show available food near you. Your location is used to find nearby posts.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.allow_location, (dialog, which) -> {
                    preferences.edit().putBoolean(PREF_PERMISSION_REQUESTED, true).apply();
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                })
                .show();
    }

    private void fetchPostsAtCurrentLocation() {
        if (binding == null) {
            return;
        }
        adapter.setDistanceOrigin(searchLatitude, searchLongitude);
        foodPostViewModel.fetchNearbyPosts(
                searchLatitude,
                searchLongitude,
                radiusKm,
                selectedFoodType,
                selectedCategory);
    }

    private void renderPosts(Resource<List<FoodPost>> resource) {
        if (binding == null) {
            return;
        }
        switch (resource.status) {
            case LOADING:
                binding.emptyState.hideAction();
                binding.emptyState.setVisibility(View.GONE);
                if (!binding.swipeRefresh.isRefreshing()) {
                    binding.rvFeed.setVisibility(View.GONE);
                    binding.shimmerLoading.getRoot().setVisibility(View.VISIBLE);
                    binding.shimmerLoading.shimmerLayout.startShimmer();
                }
                break;
            case SUCCESS:
                stopLoading();
                binding.swipeRefresh.setRefreshing(false);
                List<FoodPost> posts = resource.data == null ? new ArrayList<>() : resource.data;
                adapter.setPosts(posts);
                if (posts.isEmpty()) {
                    binding.rvFeed.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.emptyState.setContent(
                            getString(R.string.feed_no_food_title),
                            getString(R.string.feed_no_food_message));
                    binding.emptyState.setAction(
                            radiusKm < 25 ? getString(R.string.feed_increase_distance) : "Clear filters",
                            v -> increaseDistanceOrClearFilters());
                } else {
                    binding.emptyState.setVisibility(View.GONE);
                    binding.emptyState.hideAction();
                    binding.rvFeed.setVisibility(View.VISIBLE);
                }
                break;
            case ERROR:
                stopLoading();
                binding.swipeRefresh.setRefreshing(false);
                binding.rvFeed.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.emptyState.setContent("Could not load nearby food", resource.message);
                Log.d("Feed Index", "Index Link :"+resource.message);
                binding.emptyState.setAction("Try Again", v -> loadNearbyFeed());
                break;
        }
    }

    private void increaseDistanceOrClearFilters() {
        if (radiusKm < 25) {
            radiusKm = radiusKm < 5 ? 5 : radiusKm < 10 ? 10 : 25;
        } else {
            selectedFoodType = null;
            selectedCategory = null;
            radiusKm = Constants.DEFAULT_RADIUS_KM;
        }
        fetchPostsAtCurrentLocation();
    }

    private void stopLoading() {
        if (binding == null) {
            return;
        }
        binding.shimmerLoading.shimmerLayout.stopShimmer();
        binding.shimmerLoading.getRoot().setVisibility(View.GONE);
    }

    private void beginLoading() {
        if (binding == null || binding.swipeRefresh.isRefreshing()) {
            return;
        }
        binding.rvFeed.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.GONE);
        binding.shimmerLoading.getRoot().setVisibility(View.VISIBLE);
        binding.shimmerLoading.shimmerLayout.startShimmer();
    }

    private void openFoodDetail(FoodPost post) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.role_fragment_container, FoodDetailFragment.newInstance(post))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.startExpiryUpdates();
        }
        if (firstResume) {
            firstResume = false;
            return;
        }
        if (profileViewModel != null) {
            profileViewModel.fetchCurrentUser();
        }
        loadNearbyFeed();
    }

    @Override
    public void onPause() {
        if (adapter != null) {
            adapter.stopExpiryUpdates();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (adapter != null) {
            adapter.stopExpiryUpdates();
        }
        if (binding != null) {
            binding.shimmerLoading.shimmerLayout.stopShimmer();
        }
        super.onDestroyView();
        binding = null;
    }
}