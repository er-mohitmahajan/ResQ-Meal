package com.resqmeal.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {

    public interface LocationListener {
        void onLocationReceived(Location location);
        void onError(String error);
    }

    private final FusedLocationProviderClient fusedLocationClient;
    private final Context appContext;

    public LocationHelper(Context context) {
        this.appContext = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext);
    }

    public void getLastLocation(LocationListener listener) {
        getCurrentLocation(listener);
    }

    public void getCurrentLocation(@NonNull LocationListener listener) {
        if (!hasLocationPermissions(appContext)) {
            listener.onError("Location permission is not granted");
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                )
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        listener.onLocationReceived(location);
                    } else {
                        listener.onError("Current location is unavailable. Check that location services are enabled and try again.");
                    }
                })
                .addOnFailureListener(e -> listener.onError(
                        e.getLocalizedMessage() == null
                                ? "Unable to get the current location"
                                : e.getLocalizedMessage()
                ));
    }

    public static boolean hasLocationPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }
}
