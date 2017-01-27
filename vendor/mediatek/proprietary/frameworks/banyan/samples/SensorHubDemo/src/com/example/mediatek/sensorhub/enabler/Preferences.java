package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    public static final String PREFERENCES_FILE = "SensorHubDemo.Main";
    
    private static final String ENABLE_PICKUP = "enablePickup";
    private static final String ENABLE_SHAKE = "enableShake";
    private static final String ENABLE_FACE_DOWN = "enableFaceDown";
    private static final String ENABLE_IN_POCKET= "enableInPocket";
    private static final String ENABLE_PEDOMETER = "enablePedometer";
    private static final String ENABLE_USER_ACTIVITY = "enableUserActivity";

    private static Preferences sPreferences;
    private final SharedPreferences mSharedPreferences;

    private Preferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }
    
    public static synchronized Preferences getPreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = new Preferences(context);
        }
        return sPreferences;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return getPreferences(context).mSharedPreferences;
    }
    
    public void setPickup(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_PICKUP, value).apply();
    }

    public boolean getPickup() {
        return mSharedPreferences.getBoolean(ENABLE_PICKUP, false);
    }
    
    public void setShake(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_SHAKE, value).apply();
    }

    public boolean getShake() {
        return mSharedPreferences.getBoolean(ENABLE_SHAKE, false);
    }

    public void setInPocket(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_IN_POCKET, value).apply();
    }

    public boolean getInPocket() {
        return mSharedPreferences.getBoolean(ENABLE_IN_POCKET, false);
    }
    
    public void setFacedown(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_FACE_DOWN, value).apply();
    }

    public boolean getFacedown() {
        return mSharedPreferences.getBoolean(ENABLE_FACE_DOWN, false);
    }

    public void setPedometer(boolean value) {
       mSharedPreferences.edit().putBoolean(ENABLE_PEDOMETER, value).apply();
    }

    public boolean getPedometer() {
        return mSharedPreferences.getBoolean(ENABLE_PEDOMETER, false);
    }

    public void setUserActivity(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_USER_ACTIVITY, value).apply();
    }

    public boolean getUserActivity() {
        return mSharedPreferences.getBoolean(ENABLE_USER_ACTIVITY, false);
    }
}
