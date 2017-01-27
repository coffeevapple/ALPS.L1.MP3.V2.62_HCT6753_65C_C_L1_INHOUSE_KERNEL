package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;

public class PickupEnabler extends SensorHubEnabler {
    private static final String TAG = "PickupEnabler";
    private static PickupEnabler sPickupEnabler;
    
    private PickupEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getPickup();
    }

    public synchronized static PickupEnabler getInstance(Context context) {
        if (sPickupEnabler == null) {
            sPickupEnabler = new PickupEnabler(context);
        }

        return  sPickupEnabler;
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }
        
        if (sPickupEnabler == null) {
            sPickupEnabler = getInstance(context);
        }
        sPickupEnabler.addSwitch(switch_);
    }
    
    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sPickupEnabler == null) {
            return;
        }
        sPickupEnabler.removeSwitch(switch_);
    }
    
    public synchronized static void unregisterAllSwitches() {
        if (sPickupEnabler == null) {
            return;
        }
        sPickupEnabler.removeAllSwitches();
    }
    
    @Override
    protected void setPreference() {
        mPreferences.setPickup(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = mSensorHubClient.addRequest(ContextInfo.Type.PICK_UP);
        if(DEBUG_LOG) Log.d(TAG, "enableSensor: Pickup. rid=" + mRequestId);
    }
}
