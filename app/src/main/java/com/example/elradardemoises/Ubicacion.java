package com.example.elradardemoises;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Ubicacion {
    private static final String TAG = "Ubicacion";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private Fragment fragment;
    private Context context;
    private TextView lblUbicacion;
    private FusedLocationProviderClient fusedLocationClient;

    public interface LocationCallback {
        void onLocationObtained(String locationText);
        void onLocationError(String error);
        void onPermissionDenied();
    }

    private LocationCallback callback;

    public Ubicacion(Fragment fragment, TextView lblUbicacion) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.lblUbicacion = lblUbicacion;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(fragment.requireActivity());
    }

    public Ubicacion(Fragment fragment, TextView lblUbicacion, LocationCallback callback) {
        this(fragment, lblUbicacion);
        this.callback = callback;
    }

    public void initializeLocation() {
        checkLocationPermissionAndGetLocation();
    }

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(fragment.requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(fragment.requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            String errorText = "Ubicación no disponible";
                            lblUbicacion.setText(errorText);
                            if (callback != null) {
                                callback.onLocationError(errorText);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener ubicación: " + e.getMessage());
                    String errorText = "Error al obtener ubicación";
                    lblUbicacion.setText(errorText);
                    if (callback != null) {
                        callback.onLocationError(errorText);
                    }
                });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        if (!Geocoder.isPresent()) {
            fragment.requireActivity().runOnUiThread(() -> {
                String errorText = "Servicio de geocodificación no disponible";
                lblUbicacion.setText(errorText);
                if (callback != null) {
                    callback.onLocationError(errorText);
                }
            });
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String locationText = buildLocationText(address);

                        fragment.requireActivity().runOnUiThread(() -> {
                            lblUbicacion.setText(locationText);
                            if (callback != null) {
                                callback.onLocationObtained(locationText);
                            }
                        });

                        Log.d(TAG, "Ubicación obtenida: " + locationText);
                    } else {
                        fragment.requireActivity().runOnUiThread(() -> {
                            String errorText = "Ubicación no disponible";
                            lblUbicacion.setText(errorText);
                            if (callback != null) {
                                callback.onLocationError(errorText);
                            }
                        });
                    }
                }

                @Override
                public void onError(@Nullable String errorMessage) {
                    Log.e(TAG, "Error al obtener dirección: " + errorMessage);
                    fragment.requireActivity().runOnUiThread(() -> {
                        String errorText = "Error al obtener dirección";
                        lblUbicacion.setText(errorText);
                        if (callback != null) {
                            callback.onLocationError(errorText);
                        }
                    });
                }
            });
        } else {
            new Thread(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String locationText = buildLocationText(address);

                        fragment.requireActivity().runOnUiThread(() -> {
                            lblUbicacion.setText(locationText);
                            if (callback != null) {
                                callback.onLocationObtained(locationText);
                            }
                        });

                        Log.d(TAG, "Ubicación obtenida: " + locationText);
                    } else {
                        fragment.requireActivity().runOnUiThread(() -> {
                            String errorText = "Ubicación no disponible";
                            lblUbicacion.setText(errorText);
                            if (callback != null) {
                                callback.onLocationError(errorText);
                            }
                        });
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Error al obtener dirección: " + e.getMessage());
                    fragment.requireActivity().runOnUiThread(() -> {
                        String errorText = "Error al obtener dirección";
                        lblUbicacion.setText(errorText);
                        if (callback != null) {
                            callback.onLocationError(errorText);
                        }
                    });
                }
            }).start();
        }
    }

    private String buildLocationText(Address address) {
        String cityName = address.getLocality();
        String countryName = address.getCountryName();
        String adminArea = address.getAdminArea();
        String subAdminArea = address.getSubAdminArea();

        String locationText = "";
        if (cityName != null && !cityName.isEmpty()) {
            locationText += cityName;
        } else if (subAdminArea != null && !subAdminArea.isEmpty()) {
            locationText += subAdminArea;
        } else if (adminArea != null && !adminArea.isEmpty()) {
            locationText += adminArea;
        } else if (countryName != null && !countryName.isEmpty()) {
            locationText += countryName;
        } else {
            locationText += "Ubicación desconocida";
        }

        return locationText;
    }

    public void handlePermissionResult(int requestCode, @NonNull String[] permissions,
                                       @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                String errorText = "Permisos de ubicación denegados";
                lblUbicacion.setText(errorText);
                Toast.makeText(context,
                        "Se necesitan permisos de ubicación para mostrar tu ubicación",
                        Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onPermissionDenied();
                }
            }
        }
    }

    public void refreshLocation() {
        initializeLocation();
    }

    public static int getLocationPermissionRequestCode() {
        return LOCATION_PERMISSION_REQUEST_CODE;
    }
}