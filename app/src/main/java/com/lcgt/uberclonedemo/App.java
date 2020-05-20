package com.lcgt.uberclonedemo;

import android.app.Application;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(new Parse.Configuration.Builder(this)
            .applicationId(BuildConfig.APP_ID)
            // if defined
            .clientKey(BuildConfig.CLIENT_ID)
            .server(BuildConfig.SERVER)
            .build()
        );

//        ParseObject object = new ParseObject("ExampleObject");
//        object.put("myNumber", "666");
//        object.put("myString", "Christina ");
//
//        object.saveInBackground(new SaveCallback() {
//          @Override
//          public void done(ParseException ex) {
//            if (ex == null) {
//              Log.i("Parse Result", "Successful!");
//            } else {
//              Log.i("Parse Result", "Failed" + ex.toString());
//            }
//          }
//        });


//      To allow anonymous user
//        ParseUser.enableAutomaticUser();

        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        defaultACL.setPublicWriteAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);

    }
}
