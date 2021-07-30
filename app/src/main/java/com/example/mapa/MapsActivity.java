package com.example.mapa;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Pair;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.mapa.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView label;

    private Marker userMarker;

    private ArrayList<Marker> busMarkers;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private List<Pair<String, LatLng>> paradas;

    private static final int LOCATION_REQUEST_CODE = 99;

    private static final int UPDATE_INTERVAL = 1000 * 60;
    private static final int FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                updateLocation(locationResult);
            }
        };

        if (permissionsGranted()) {
            startLocationUpdates();
        } else {
            requestPermissions();
        }

        label = (TextView)findViewById(R.id.label);

        paradas = new ArrayList<>();
        paradas.add(new Pair<>("Av. Pío Jaramillo y Adán Smith.", new LatLng(-4.02375846, -79.20313641)));
        paradas.add(new Pair<>("Pío Jaramillo y Abrahán Lincon.", new LatLng(-4.0153842, -79.20395404)));
        paradas.add(new Pair<>("Gonzalo Montesdeoca y Giovanni Calles", new LatLng(-4.02636708, -79.20658976)));
        paradas.add(new Pair<>("Av. Pio Jaramillo y Chile.", new LatLng(-4.01011502, -79.20456111)));
        paradas.add(new Pair<>("Av. Zoilo Rodríguez y Praga", new LatLng(-3.98970467, -79.20019202)));

        busMarkers = new ArrayList<>(paradas.size());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));



        for (Pair<String, LatLng> punto : paradas) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(punto.second)
                    .title(punto.first)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_blue)));

            busMarkers.add(marker);
        }

        PolylineOptions lineOptions = new PolylineOptions();

        InputStream inputStream = getResources().openRawResource(R.raw.coordenadas);

        Stream<String> lines = (new BufferedReader(new InputStreamReader(inputStream))).lines();

        lines.forEach(x -> {
            String[] parts = x.split(",");

            lineOptions.add(new LatLng(Double.parseDouble(parts[1]), Double.parseDouble(parts[0])));
        });

        Polyline path = mMap.addPolyline(lineOptions);

        path.setStartCap(new RoundCap());
        path.setEndCap(new RoundCap());
        path.setJointType(JointType.ROUND);
        path.setWidth(3);
        path.setColor(Color.BLUE);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(paradas.get(0).second, 12));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    private boolean permissionsGranted() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQUEST_CODE);
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL)
                .setInterval(UPDATE_INTERVAL);

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {

        }
    }

    private void updateLocation(LocationResult locationResult) {
        Location location = locationResult.getLastLocation();

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userMarker == null) {
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
            userMarker.setTitle("Tu ubicacion");
        } else {
            userMarker.setPosition(latLng);
        }

        int closerIndex = getCloser(latLng);

        busMarkers.get(closerIndex)
                //.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus_red));
    }

    private int getCloser(LatLng userLocation) {
        double minDistance = 0;
        int index = -1;

        for (int i = 0; i < paradas.size(); i++) {
            double currentDistance = calculateDistance(userLocation, paradas.get(i).second);

            if (minDistance == 0 || currentDistance < minDistance) {
                minDistance = currentDistance;
                index = i;
            }
        }

        return index;
    }

    private double calculateDistance(LatLng p1, LatLng p2) {
        double latitude = Math.abs(p1.latitude - p2.latitude);
        double longitude = Math.abs(p1.longitude - p2.longitude);

        return Math.sqrt(Math.pow(latitude, 2) + Math.pow(longitude, 2));
    }
}