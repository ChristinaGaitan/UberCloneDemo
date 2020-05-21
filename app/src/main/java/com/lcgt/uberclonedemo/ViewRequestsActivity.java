package com.lcgt.uberclonedemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {
    ListView listView;
    ArrayList<String> requests = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    LocationManager locationManager;
    LocationListener locationListener;
    Integer locationRequestCode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        setTitle("Nearby Requests");
        requests.clear();

        listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requests.add("Getting nearby requests");
        listView.setAdapter(arrayAdapter);

        setLocationManagerAndListener();
        validatePermissionGranted();
    }

    public void setLocationManagerAndListener() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListViewMethod(location);
                Log.i("Method", "setLocationManagerAndListener");
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
    }

    public void validatePermissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(lastKnownLocation != null) {
                Log.i("Method", "validatePermissionGranted");
                updateListViewMethod(lastKnownLocation);
            }
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == locationRequestCode) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                validatePermissionGranted();
                Log.i("Method", "onRequestPermissionsResult");
            }
        }
    }

    public void updateListViewMethod(Location location) {
        if (location != null) {
            requests.clear();
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
            query.whereNear("location", geoPointLocation);
            query.setLimit(10);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size()> 0) {
                            for (ParseObject requestObject : objects) {
                                ParseGeoPoint requestObjectLocation = (ParseGeoPoint) requestObject.get("location");
                                Double distanceInMiles = geoPointLocation.distanceInKilometersTo(requestObjectLocation);
                                Double distanceInMilesOneDP = (double) Math.round(distanceInMiles * 10) / 10;

                                requests.add(distanceInMilesOneDP.toString() + " Km");
                            }

                        } else {
                            requests.add("No active requests nearby");
                        }

                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

    }


}
