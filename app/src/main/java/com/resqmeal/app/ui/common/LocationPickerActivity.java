package com.resqmeal.app.ui.common;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.ActivityLocationPickerBinding;
import com.resqmeal.app.utils.GeoUtils;
import com.resqmeal.app.utils.LocationHelper;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LocationPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";
    public static final String EXTRA_ADDRESS = "extra_address";
    public static final String EXTRA_GEOHASH = "extra_geohash";
    public static final String EXTRA_INITIAL_LAT = "extra_initial_lat";
    public static final String EXTRA_INITIAL_LNG = "extra_initial_lng";

    private static final String PREF_LOCATION_PERMISSION_REQUESTED = "location_permission_requested";

    private ActivityLocationPickerBinding binding;
    private LocationHelper locationHelper;
    private MapView map;
    private IMapController mapController;
    private Geocoder geocoder;
    private final ExecutorService geocodingExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger geocodeRequestId = new AtomicInteger();

    private double selectedLat;
    private double selectedLng;
    private String selectedAddress;
    private boolean addressResolved;
    private boolean waitingForSettings;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineLocationGranted = Boolean.TRUE.equals(
                        result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseLocationGranted = Boolean.TRUE.equals(
                        result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fineLocationGranted || coarseLocationGranted) {
                    showMap();
                    getCurrentLocation();
                } else {
                    showPermissionDeniedState();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        binding = ActivityLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        locationHelper = new LocationHelper(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        setupMap();
        setupListeners();
        checkPermissions();
    }

    private void setupMap() {
        map = binding.mapView;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapController = map.getController();
        mapController.setZoom(15.0);

        map.addMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateSelectedLocation();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateSelectedLocation();
                return true;
            }
        }, 500));
    }

    private void checkPermissions() {
        if (LocationHelper.hasLocationPermissions(this)) {
            showMap();
            if (hasInitialLocation()) {
                GeoPoint initialPoint = new GeoPoint(
                        getIntent().getDoubleExtra(EXTRA_INITIAL_LAT, 0),
                        getIntent().getDoubleExtra(EXTRA_INITIAL_LNG, 0)
                );
                mapController.setCenter(initialPoint);
                updateSelectedLocation();
            } else {
                getCurrentLocation();
            }
        } else {
            showPermissionLayout();
        }
    }

    private boolean hasInitialLocation() {
        if (!getIntent().hasExtra(EXTRA_INITIAL_LAT) || !getIntent().hasExtra(EXTRA_INITIAL_LNG)) {
            return false;
        }
        return GeoUtils.isValidCoordinates(
                getIntent().getDoubleExtra(EXTRA_INITIAL_LAT, 0),
                getIntent().getDoubleExtra(EXTRA_INITIAL_LNG, 0)
        );
    }

    private void showPermissionLayout() {
        binding.layoutPermission.setVisibility(View.VISIBLE);
        binding.mapView.setVisibility(View.GONE);
        binding.cardAddress.setVisibility(View.GONE);
        binding.btnConfirmLocation.setVisibility(View.GONE);
        binding.ivCenterPin.setVisibility(View.GONE);
        binding.fabCurrentLocation.setVisibility(View.GONE);
    }

    private void showMap() {
        binding.layoutPermission.setVisibility(View.GONE);
        binding.mapView.setVisibility(View.VISIBLE);
        binding.cardAddress.setVisibility(View.VISIBLE);
        binding.btnConfirmLocation.setVisibility(View.VISIBLE);
        binding.ivCenterPin.setVisibility(View.VISIBLE);
        binding.fabCurrentLocation.setVisibility(View.VISIBLE);
    }

    private void getCurrentLocation() {
        binding.fabCurrentLocation.setEnabled(false);
        locationHelper.getCurrentLocation(new LocationHelper.LocationListener() {
            @Override
            public void onLocationReceived(Location location) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                binding.fabCurrentLocation.setEnabled(true);
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapController.animateTo(point);
                updateSelectedLocation();
            }

            @Override
            public void onError(String error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                binding.fabCurrentLocation.setEnabled(true);
                binding.tvSelectedAddress.setText(getString(R.string.location_current_failed, error));
                binding.btnConfirmLocation.setEnabled(false);
            }
        });
    }

    private void setupListeners() {
        binding.btnConfirmLocation.setEnabled(false);
        binding.btnAllowLocation.setOnClickListener(v -> {
            if (binding.btnAllowLocation.getTag() == Boolean.TRUE) {
                openAppSettings();
                return;
            }
            getPreferences(MODE_PRIVATE).edit()
                    .putBoolean(PREF_LOCATION_PERMISSION_REQUESTED, true)
                    .apply();
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        });

        binding.fabCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        binding.btnConfirmLocation.setOnClickListener(v -> {
            GeoPoint center = (GeoPoint) map.getMapCenter();
            if (!addressResolved
                    || Math.abs(center.getLatitude() - selectedLat) > 0.000001
                    || Math.abs(center.getLongitude() - selectedLng) > 0.000001
                    || selectedAddress == null
                    || selectedAddress.trim().isEmpty()) {
                updateSelectedLocation();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_LAT, selectedLat);
            resultIntent.putExtra(EXTRA_LNG, selectedLng);
            resultIntent.putExtra(EXTRA_ADDRESS, selectedAddress);
            resultIntent.putExtra(EXTRA_GEOHASH, GeoUtils.getGeohash(selectedLat, selectedLng));
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }

    private void showPermissionDeniedState() {
        showPermissionLayout();
        boolean previouslyRequested = getPreferences(MODE_PRIVATE)
                .getBoolean(PREF_LOCATION_PERMISSION_REQUESTED, false);
        boolean shouldExplain = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (previouslyRequested && !shouldExplain) {
            binding.tvPermissionMessage.setText(R.string.location_permission_denied_permanently);
            binding.btnAllowLocation.setText(getString(R.string.open_settings));
            binding.btnAllowLocation.setTag(Boolean.TRUE);
        } else {
            binding.tvPermissionMessage.setText(R.string.location_permission_denied);
            binding.btnAllowLocation.setText(getString(R.string.allow_location));
            binding.btnAllowLocation.setTag(Boolean.FALSE);
        }
    }

    private void openAppSettings() {
        waitingForSettings = true;
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)
        );
        startActivity(intent);
    }

    private void updateSelectedLocation() {
        GeoPoint center = (GeoPoint) map.getMapCenter();
        selectedLat = center.getLatitude();
        selectedLng = center.getLongitude();
        double requestLat = selectedLat;
        double requestLng = selectedLng;
        addressResolved = false;
        binding.btnConfirmLocation.setEnabled(false);
        binding.tvSelectedAddress.setText(R.string.location_finding_address);

        int requestId = geocodeRequestId.incrementAndGet();
        geocodingExecutor.execute(() -> {
            String addressText;
            try {
                addressText = reverseGeocode(requestLat, requestLng);
            } catch (IOException | IllegalArgumentException e) {
                addressText = getString(R.string.location_address_failed);
            }

            String resolvedAddress = addressText;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || requestId != geocodeRequestId.get()) {
                    return;
                }
                if (resolvedAddress == null || resolvedAddress.trim().isEmpty()) {
                    binding.tvSelectedAddress.setText(R.string.location_address_failed);
                    return;
                }
                selectedAddress = resolvedAddress;
                addressResolved = true;
                binding.tvSelectedAddress.setText(resolvedAddress);
                binding.btnConfirmLocation.setEnabled(true);
            });
        });
    }

    @SuppressWarnings("deprecation")
    private String reverseGeocode(double lat, double lng) throws IOException {
        if (!Geocoder.isPresent()) {
            return null;
        }
        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        Address address = addresses.get(0);
        String line = address.getAddressLine(0);
        if (line != null && !line.trim().isEmpty()) {
            return line.trim();
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            String addressLine = address.getAddressLine(i);
            if (addressLine != null && !addressLine.trim().isEmpty()) {
                if (formatted.length() > 0) {
                    formatted.append(", ");
                }
                formatted.append(addressLine.trim());
            }
        }
        return formatted.length() == 0 ? null : formatted.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (waitingForSettings) {
            waitingForSettings = false;
            if (LocationHelper.hasLocationPermissions(this)) {
                showMap();
                getCurrentLocation();
            } else {
                showPermissionDeniedState();
            }
        }
    }

    @Override
    protected void onPause() {
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        geocodeRequestId.incrementAndGet();
        geocodingExecutor.shutdownNow();
        map.onDetach();
        super.onDestroy();
    }
}
