package com.lcgt.uberclonedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public void redirectToActivity() {
        if (ParseUser.getCurrentUser().getString(   "userType") == "Rider") {
            Intent intent = new Intent(getApplicationContext(), RiderActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        setUser();
    }

    public void getStarted(View view) {
        String userType;

        Switch userTypeSwitch = findViewById(R.id.userTypeSwitch);

        if(userTypeSwitch.isChecked()) {
            userType = "Driver";
        } else {
            userType = "Rider";
        }

        ParseUser.getCurrentUser().put("userType", userType);
        Log.i("UserType", userType);
        redirectToActivity();
    }

    public void setUser() {
        if(ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if(e == null) {
                        Log.i("Info", "Anonymous successful");
                    } else {
                        Log.i("Info", "Anonymous fail");
                    }
                }
            });
        } else if (ParseUser.getCurrentUser().get("userType") != null) {
            Log.i("UserType", (String) ParseUser.getCurrentUser().get("userType"));
            redirectToActivity();
        }
    }
}
