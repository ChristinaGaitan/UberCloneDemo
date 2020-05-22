package com.lcgt.uberclonedemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

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

    ArrayList<Double> requestLatitudes = new ArrayList<>();
    ArrayList<Double> requestLongitudes = new ArrayList<>();

    ArrayList<String> usernames = new ArrayList<>();

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

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ActivityCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if(requestLatitudes.size() > position &&
                        requestLongitudes.size() > position &&
                        usernames.size() > position &&
                        lastKnownLocation != null)
                    {
                        Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                        intent.putExtra("requestLatitude", requestLatitudes.get(position));
                        intent.putExtra("requestLongitude", requestLongitudes.get(position));
                        intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());
                        intent.putExtra("username", usernames.get(position));

                        startActivity(intent);
                    }
                }
            }
        });

        setLocationManagerAndListener();
        validatePermissionGranted();
    }


    public void updateListViewMethod(Location location) {
        if (location != null) {
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
            query.whereNear("location", geoPointLocation);
            query.whereDoesNotExist("driverUsername");
            query.setLimit(10);

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        requests.clear();
                        requestLatitudes.clear();
                        requestLongitudes.clear();

                        if (objects.size()> 0) {
                            for (ParseObject requestObject : objects) {
                                ParseGeoPoint requestObjectLocation = (ParseGeoPoint) requestObject.get("location");
                                Double distanceInKilometers = geoPointLocation.distanceInKilometersTo(requestObjectLocation);
                                Double distanceInMilesOneDP = (double) Math.round(distanceInKilometers * 10) / 10;

                                requests.add(distanceInMilesOneDP.toString() + " Km");

                                requestLatitudes.add(requestObjectLocation.getLatitude());
                                requestLongitudes.add(requestObjectLocation.getLongitude());

                                usernames.add(requestObject.get("username").toString());
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

    public void setLocationManagerAndListener() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListViewMethod(location);
                Log.i("Method", "setLocationManagerAndListener");

                ParseGeoPoint driverGeopoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                ParseUser.getCurrentUser().put("location", driverGeopoint);
                ParseUser.getCurrentUser().saveInBackground();
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
        if (ActivityCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

}
