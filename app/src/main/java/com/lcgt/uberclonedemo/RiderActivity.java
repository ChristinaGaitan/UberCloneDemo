package com.lcgt.uberclonedemo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callUberButton;
    Integer locationRequestCode = 1;
    Boolean requestActive = false;

    Handler handler = new Handler();
    TextView infoTextView;

    Boolean driverActive = false;


    public void logOut(View view) {
        ParseUser.logOut();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    public void callUber(View view) {
        Log.i("Info", "Call uber");

        if (requestActive == true) {
            cancelActiveRequest();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                    saveRequest(lastKnownLocation);
                } else {
                    Toast.makeText(this, "Could not find location. Please try again later!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void cancelActiveRequest() {
        String username = ParseUser.getCurrentUser().getUsername();
        ParseQuery<ParseObject> query = new ParseQuery<>("Request");
        query.whereEqualTo("username", username);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {

                    // Delete active request in Parse
                    for (ParseObject object : objects) {
                        object.deleteInBackground();
                    }

                    requestActive = false;
                    callUberButton.setText("Call an Uber");
                }
            }
        });
    }

    public void saveRequest(Location location) {
        ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        String username = ParseUser.getCurrentUser().getUsername();

        ParseObject request = new ParseObject("Request");
        request.put("username", username);
        request.put("location", parseGeoPoint);
        request.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    requestActive = true;
                    callUberButton.setText("Cancel Uber");

                    checkForUpdates();
                }
            }
        });
    }

    public void checkForUpdates() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null && objects.size() > 0) {

                    driverActive = true;

                    String driverUsername = objects.get(0).getString("driverUsername");
                    ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                    userQuery.whereEqualTo("username", driverUsername);

                    userQuery.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if(e == null && objects.size() > 0) {
                                final ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");

                                if (ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    // Permission already granted

                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                    if (lastKnownLocation != null) {
                                        // Convert lastKnownLocation to GeoPoint
                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                        Double distanceInKilometers = driverLocation.distanceInKilometersTo(userLocation);

                                        if(distanceInKilometers < 0.1) {
                                            infoTextView.setText("Your driver is here!");

                                            deleteActiveRequests();
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    infoTextView.setText("");
                                                    callUberButton.setVisibility(View.VISIBLE);
                                                    callUberButton.setText("Call An Uber");
                                                    requestActive = false;
                                                    driverActive = false;
                                                }
                                            }, 5000);

                                        } else {
                                            Double distanceInMilesOneDP = (double) Math.round(distanceInKilometers * 10) / 10;
                                            infoTextView.setText("Your driver is "+ distanceInMilesOneDP +" Km way!");

                                            showDriverInMap(driverLocation, userLocation);

                                            callUberButton.setVisibility(View.INVISIBLE);

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdates();
                                                }
                                            }, 2000);
                                        }
                                    }
                                }
                            }
                        }
                    });

                }
            }
        });
    }

    public void deleteActiveRequests() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    for(ParseObject object : objects) {
                        object.deleteInBackground();
                    }
                }
            }
        });
    }


    public void showDriverInMap(ParseGeoPoint driverLocation, ParseGeoPoint userLocation) {
        mMap.clear();
        LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());

        LatLng requestLocationLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

        ArrayList<Marker> markers = new ArrayList<>();
        MarkerOptions driverMarkerOptions = new MarkerOptions().
            position(driverLocationLatLng).
            title("Driver location");
        MarkerOptions requesterMarkerOptions = new MarkerOptions().
            position(requestLocationLatLng).
            title("Your location").
            icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        markers.add(mMap.addMarker(driverMarkerOptions));
        markers.add(mMap.addMarker(requesterMarkerOptions));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(Marker marker : markers) {
            builder.include(marker.getPosition());
        }

        LatLngBounds bounds = builder.build();
        int padding = 50;
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == locationRequestCode) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                validatePermissionGranted();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        infoTextView = findViewById(R.id.infoTextView);
        callUberButton = findViewById(R.id.callUberButton);

        findActiveRequest();


    }

    public void findActiveRequest() {
        String username = ParseUser.getCurrentUser().getUsername();
        ParseQuery<ParseObject> query = new ParseQuery<>("Request");
        query.whereEqualTo("username", username);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    requestActive = true;
                    callUberButton.setText("Cancel Uber");

                    checkForUpdates();
                }
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateMap(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        validatePermissionGranted();

    }

    public void validatePermissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(lastKnownLocation != null) {
                updateMap(lastKnownLocation);
            }
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationRequestCode);
        }
    }

    public void updateMap(Location location) {
        if(driverActive == false) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }
    }

}