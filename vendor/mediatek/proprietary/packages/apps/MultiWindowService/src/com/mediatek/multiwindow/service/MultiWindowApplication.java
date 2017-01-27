package com.mediatek.multiwindow.service;

import android.app.Application;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

public class MultiWindowApplication extends Application {

    private static final String TAG = "MultiWindowApplication";
    public static Context sContext;

    public MultiWindowApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        Intent startIntentV1 = new Intent(this, MultiWindowServices.class);
        startService(startIntentV1);
    }

    public static Context getContext() {
        return sContext;
    }
}

