package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.example.mediatek.sensorhub.Config;
import com.example.mediatek.sensorhub.sensor.SensorHubClient;

import java.util.ArrayList;
import java.util.List;

public abstract class SensorHubEnabler implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "SensorHubEnabler";
    protected static final boolean DEBUG_LOG = Config.ENABLE_DEBUG_LOG;
    protected Preferences mPreferences;
    protected List<Switch> mSwitches;
    protected Context mContext;
    
    protected boolean mIsChecked;
    
    protected SensorHubClient mSensorHubClient;
    protected int mRequestId = -1;
    
    protected SensorHubEnabler(Context context) {
        mContext = context;
        mPreferences = Preferences.getPreferences(mContext);
        mSwitches = new ArrayList<Switch>();
        mSensorHubClient = new SensorHubClient(mContext);
    }
    
    protected void addSwitch(Switch switch_) {
        if (!mSwitches.contains(switch_)) {
            switch_.setOnCheckedChangeListener(this);
            switch_.setChecked(mIsChecked);
            mSwitches.add(switch_);
        }
    }
    
    protected void removeSwitch(Switch switch_) {
        switch_.setOnCheckedChangeListener(null);
        mSwitches.remove(switch_);
    }
    
    protected void changeSwitchesState(boolean isChecked) {
        if(DEBUG_LOG) Log.d(TAG, "changeSwitchesState: switches=" + mSwitches.size());
        for (Switch switch_ : mSwitches) {
            switch_.setChecked(isChecked);
        }
    }
    
    protected void removeAllSwitches() {
        for (Switch switch_ : mSwitches) {
            switch_.setOnCheckedChangeListener(null);
        }
        mSwitches.clear();
    }

    private void setSensor() {
        if (mIsChecked) {
            enableSensor();
        } else {
            disableSensor();
        }
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mIsChecked == isChecked) {
            if (DEBUG_LOG) Log.d(TAG, "onCheckedChanged: no change. value=" + mIsChecked);
            return;
        }

        mIsChecked = isChecked;
        setPreference();
        changeSwitchesState(isChecked);

        setSensor();
    }

    /**
     * Sets up sensor hub requests when the host is being started.
     */
    public void onStart() {
        if (DEBUG_LOG) Log.d(TAG, "onStart: checked=" + mIsChecked + ",rid=" + mRequestId);
        if (mIsChecked && mRequestId <= 0) {
            enableSensor();
        }
    }

    /**
     * Cancels the request when the host is being stopped.
     */
    public void onStop() {
        disableSensor();
    }

    protected abstract void setPreference();
    protected abstract void enableSensor();
    
    protected void disableSensor() {
        if (DEBUG_LOG) Log.d(TAG, "disableSensor: rid=" + mRequestId);
        if (mRequestId > 0) {
            mSensorHubClient.cancelRequest(mRequestId);
            mRequestId = -1;
        }
    }

}
