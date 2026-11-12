package com.resqmeal.app.ui.receiver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentMapBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.GeoUtils;
import com.resqmeal.app.utils.LocationHelper;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.FoodPostViewModel;
import com.resqmeal.app.viewmodel.ProfileViewModel;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment {
    private static final String PREF_PERMISSION_REQUESTED = "location_permission_requested";

    private FragmentMapBinding binding;
    private FoodPostViewModel viewModel;
    private ProfileViewModel profileViewModel;
    private LocationHelper locationHelper;
    private MapView map;
    private IMapController mapController;
    private final List<Marker> postMarkers = new ArrayList<>();
    private Double savedLatitude;
    private Double savedLongitude;
    private boolean profileLocationLoaded;
    private boolean waitingForProfileLocation;
    private boolean mapCentered;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    getCurrentLocation();
                } else {
                    useSavedLocationOrShowMessage();
                }
            });

    private final ActivityResultLauncher<Intent> appSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    getCurrentLocation());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(FoodPostViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        locationHelper = new LocationHelper(requireContext());

        setupMap();
        setupObservers();
        binding.fabMyLocation.setOnClickListener(v -> getCurrentLocationOrRequestPermission(true));
        profileViewModel.fetchCurrentUser();

        if (viewModel.hasNearbySearch()) {
            centerMap(
                    viewModel.getNearbyCenterLat(),
                    viewModel.getNearbyCenterLng(),
                    false);
        } else {
            getCurrentLocationOrRequestPermission(false);
        }
    }

    private void setupMap() {
        map = binding.mapView;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);
        mapController = map.getController();
        mapController.setZoom(13.5);
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint point) {
                hidePostPreview();
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint point) {
                return false;
            }
        }));
    }

    private void setupObservers() {
        viewModel.getNearbyPosts().observe(getViewLifecycleOwner(), this::renderPosts);
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                User user = resource.data;
                savedLatitude = user == null ? null : user.getLat();
                savedLongitude = user == null ? null : user.getLng();
                profileLocationLoaded = true;
                if (waitingForProfileLocation) {
                    useSavedLocationOrShowMessage();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                profileLocationLoaded = true;
                if (waitingForProfileLocation) {
                    useSavedLocationOrShowMessage();
                }
            }
        });
    }

    private void getCurrentLocationOrRequestPermission(boolean explicitRequest) {
        if (!LocationHelper.hasLocationPermissions(requireContext())) {
            if (explicitRequest) {
                showLocationPermissionDialog();
                return;
            }
            if (savedLocationIsValid()) {
                useSavedLocation();
            } else if (profileLocationLoaded) {
                showLocationPermissionDialog();
            } else {
                waitingForProfileLocation = true;
                showMessage(getString(R.string.map_loading_posts), false);
            }
            return;
        }
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (binding == null) {
            return;
        }
        showMessage(getString(R.string.map_loading_posts), true);
        locationHelper.getCurrentLocation(new LocationHelper.LocationListener() {
            @Override
            public void onLocationReceived(Location location) {
                if (binding == null) {
                    return;
                }
                fetchPostsAt(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onError(String error) {
                if (binding == null) {
                    return;
                }
                if (profileLocationLoaded) {
                    useSavedLocationOrShowMessage();
                } else {
                    waitingForProfileLocation = true;
                }
            }
        });
    }

    private void useSavedLocationOrShowMessage() {
        waitingForProfileLocation = false;
        if (!profileLocationLoaded) {
            waitingForProfileLocation = true;
            showMessage(getString(R.string.map_loading_posts), true);
            return;
        }
        if (savedLocationIsValid()) {
            useSavedLocation();
        } else {
            hideLoading();
            showMessage(getString(R.string.map_location_needed), false);
        }
    }

    private boolean savedLocationIsValid() {
        return savedLatitude != null && savedLongitude != null
                && GeoUtils.isValidCoordinates(savedLatitude, savedLongitude);
    }

    private void useSavedLocation() {
        showMessage(getString(R.string.map_location_unavailable), true);
        fetchPostsAt(savedLatitude, savedLongitude);
    }

    private void fetchPostsAt(double latitude, double longitude) {
        if (binding == null || !GeoUtils.isValidCoordinates(latitude, longitude)) {
            return;
        }
        centerMap(latitude, longitude, true);
        viewModel.fetchNearbyPosts(
                latitude,
                longitude,
                viewModel.getNearbyRadiusKm(),
                viewModel.getNearbyFoodType(),
                viewModel.getNearbyCategory());
    }

    private void centerMap(double latitude, double longitude, boolean animate) {
        if (mapController == null || !GeoUtils.isValidCoordinates(latitude, longitude)) {
            return;
        }
        GeoPoint point = new GeoPoint(latitude, longitude);
        if (animate && mapCentered) {
            mapController.animateTo(point);
        } else {
            mapController.setCenter(point);
        }
        mapCentered = true;
    }

    private void renderPosts(Resource<List<FoodPost>> resource) {
        if (binding == null) {
            return;
        }
        switch (resource.status) {
            case LOADING:
                showMessage(getString(R.string.map_loading_posts), true);
                break;
            case SUCCESS:
                hideLoading();
                List<FoodPost> posts = resource.data == null ? new ArrayList<>() : resource.data;
                displayMarkers(posts);
                if (posts.isEmpty()) {
                    showMessage(getString(R.string.map_no_nearby_posts), false);
                } else {
                    binding.tvMapMessage.setVisibility(View.GONE);
                }
                break;
            case ERROR:
                hideLoading();
                removePostMarkers();
                showMessage(resource.message == null
                        ? getString(R.string.map_location_needed)
                        : resource.message, false);
                Log.d("MapFragment", "index :"+resource.message+"");
                break;
        }
    }

    private void displayMarkers(List<FoodPost> posts) {
        removePostMarkers();

        for (FoodPost post : posts) {
            if (post.getLat() == null || post.getLng() == null
                    || !GeoUtils.isValidCoordinates(post.getLat(), post.getLng())
                    || !Constants.STATUS_AVAILABLE.equals(post.getStatus())) {
                continue;
            }
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(post.getLat(), post.getLng()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(post.getTitle() == null ? "" : post.getTitle());
            marker.setSnippet(post.getPickupAddress() == null ? "" : post.getPickupAddress());
            marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                showPostPreview(post);
                return true;
            });
            map.getOverlays().add(marker);
            postMarkers.add(marker);
        }
        map.invalidate();
    }

    private void removePostMarkers() {
        for (Marker marker : postMarkers) {
            map.getOverlays().remove(marker);
        }
        postMarkers.clear();
        map.invalidate();
    }

    private void showPostPreview(FoodPost post) {
        if (binding == null) {
            return;
        }
        binding.layoutPostPreview.getRoot().setVisibility(View.VISIBLE);
        binding.layoutPostPreview.tvFoodTitle.setText(post.getTitle() == null ? "" : post.getTitle());
        binding.layoutPostPreview.tvQuantityServings.setText(String.format(
                Locale.getDefault(), "%s %s • feeds ~%d",
                post.getQuantity(), post.getUnit() == null ? "" : post.getUnit(), post.getServings()));
        binding.layoutPostPreview.tvDistance.setText(
                post.getPickupAddress() == null ? "" : post.getPickupAddress());
        binding.layoutPostPreview.chipStatus.setStatus(post.getStatus());
        if (post.getExpiryTime() != null) {
            long remainingMillis = post.getExpiryTime().toDate().getTime() - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                binding.layoutPostPreview.tvExpiryCountdown.setText("Expired");
            } else {
                long remainingMinutes = (remainingMillis + 59_999L) / 60_000L;
                long hours = remainingMinutes / 60;
                long minutes = remainingMinutes % 60;
                String countdown = hours > 0
                        ? String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
                        : String.format(Locale.getDefault(), "%dm", minutes);
                binding.layoutPostPreview.tvExpiryCountdown.setText(
                        getString(R.string.expiry_countdown, countdown));
            }
        } else {
            binding.layoutPostPreview.tvExpiryCountdown.setText("");
        }

        Glide.with(this)
                .load(post.getPhotoUrl())
                .centerCrop()
                .placeholder(R.color.shimmer_base)
                .into(binding.layoutPostPreview.ivFoodPhoto);

        binding.layoutPostPreview.getRoot().setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.role_fragment_container, FoodDetailFragment.newInstance(post))
                        .addToBackStack(null)
                        .commit());
    }

    private void hidePostPreview() {
        if (binding != null) {
            binding.layoutPostPreview.getRoot().setVisibility(View.GONE);
            binding.layoutPostPreview.getRoot().setOnClickListener(null);
        }
    }

    private void showMessage(String message, boolean loading) {
        if (binding == null) {
            return;
        }
        binding.tvMapMessage.setText(message);
        binding.tvMapMessage.setVisibility(View.VISIBLE);
        binding.mapLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void hideLoading() {
        if (binding != null) {
            binding.mapLoading.setVisibility(View.GONE);
        }
    }

    private void showLocationPermissionDialog() {
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
                .setTitle(R.string.map_location_permission_title)
                .setMessage(R.string.map_location_permission_message)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    hideLoading();
                    showMessage(getString(R.string.map_location_needed), false);
                })
                .setPositiveButton(R.string.allow_location, (dialog, which) -> {
                    preferences.edit().putBoolean(PREF_PERMISSION_REQUESTED, true).apply();
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
        if (viewModel != null && viewModel.hasNearbySearch()) {
            centerMap(
                    viewModel.getNearbyCenterLat(),
                    viewModel.getNearbyCenterLng(),
                    false);
        }
    }

    @Override
    public void onPause() {
        if (map != null) {
            map.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (map != null) {
            map.onDetach();
        }
        postMarkers.clear();
        super.onDestroyView();
        binding = null;
        map = null;
        mapController = null;
    }
}
