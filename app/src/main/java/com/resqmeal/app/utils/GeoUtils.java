package com.resqmeal.app.utils;

import android.location.Location;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
import java.util.Locale;

public class GeoUtils {

    public static String getGeohash(double lat, double lng) {
        validateCoordinates(lat, lng);
        String fullHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));
        if (fullHash.length() > Constants.GEOHASH_PRECISION) {
            return fullHash.substring(0, Constants.GEOHASH_PRECISION);
        }
        return fullHash;
    }

    public static List<GeoQueryBounds> getGeoHashQueryBounds(
            double lat,
            double lng,
            double radiusInMeters
    ) {
        validateCoordinates(lat, lng);
        if (!Double.isFinite(radiusInMeters) || radiusInMeters <= 0) {
            throw new IllegalArgumentException("Radius must be a positive finite number");
        }
        return GeoFireUtils.getGeoHashQueryBounds(
                new GeoLocation(lat, lng),
                radiusInMeters
        );
    }

    public static boolean isValidCoordinates(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng)
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180;
    }

    private static void validateCoordinates(double lat, double lng) {
        if (!isValidCoordinates(lat, lng)) {
            throw new IllegalArgumentException("Latitude or longitude is out of range");
        }
    }

    public static float getDistanceBetween(double startLat, double startLng, double endLat, double endLng) {
        validateCoordinates(startLat, startLng);
        validateCoordinates(endLat, endLng);
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLng, endLat, endLng, results);
        return results[0]; // in meters
    }

    public static String formatDistance(float distanceInMeters) {
        if (distanceInMeters < 1000) {
            return String.format(Locale.getDefault(), "%d m", (int) distanceInMeters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000f);
        }
    }

    public static GeoPoint getGeoPoint(double lat, double lng) {
        validateCoordinates(lat, lng);
        return new GeoPoint(lat, lng);
    }
}
