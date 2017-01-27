package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;

public class PedometerEnabler extends SensorHubEnabler {
    private static final String TAG = "PedometerEnabler";
    private static PedometerEnabler sPedometerEnabler;
    
    private PedometerEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getPedometer();
    }

    public synchronized static PedometerEnabler getInstance(Context context) {
        if (sPedometerEnabler == null) {
            sPedometerEnabler = new PedometerEnabler(context);
        }

        return sPedometerEnabler;
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }
        
        if (sPedometerEnabler == null) {
            sPedometerEnabler = getInstance(context);
        }
        sPedometerEnabler.addSwitch(switch_);
    }
    
    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sPedometerEnabler == null) {
            return;
        }
        sPedometerEnabler.removeSwitch(switch_);
    }
    
    public synchronized static void unregisterAllSwitches() {
        if (sPedometerEnabler == null) {
            return;
        }
        sPedometerEnabler.removeAllSwitches();
    }
    
    @Override
    protected void setPreference() {
        mPreferences.setPedometer(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = mSensorHubClient.addRequest(ContextInfo.Type.PEDOMETER);
        if(DEBUG_LOG) Log.d(TAG, "enableSensor: Pedometer. rid=" + mRequestId);
    }
}
