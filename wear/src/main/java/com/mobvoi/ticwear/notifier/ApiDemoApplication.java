package com.mobvoi.ticwear.notifier;

import android.app.Application;
import android.util.Log;

public class ApiDemoApplication extends Application {
    private static final String TAG = "TicwearApiDemo";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "app start...");
    }
}
