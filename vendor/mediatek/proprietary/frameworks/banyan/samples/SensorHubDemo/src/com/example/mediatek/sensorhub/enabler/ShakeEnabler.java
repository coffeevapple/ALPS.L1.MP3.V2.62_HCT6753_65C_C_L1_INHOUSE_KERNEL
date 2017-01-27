package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;

public class ShakeEnabler extends SensorHubEnabler {
    private static final String TAG = "ShakeEnabler";
    private static ShakeEnabler sShakeEnabler;
    
    private ShakeEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getShake();
        if (DEBUG_LOG) Log.d(TAG, "init: isChecked=" + mIsChecked);
    }

    public synchronized static ShakeEnabler getInstance(Context context) {
        if (sShakeEnabler == null) {
            sShakeEnabler = new ShakeEnabler(context);
        }

        return sShakeEnabler;
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            if (DEBUG_LOG) Log.d(TAG, "registerSwitch: context=" + context + ", switch=" + switch_);
            return;
        }
        
        if (sShakeEnabler == null) {
            sShakeEnabler = getInstance(context);
        }
        sShakeEnabler.addSwitch(switch_);
    }
    
    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sShakeEnabler == null) {
            if (DEBUG_LOG) Log.d(TAG, "unregisterSwitch: null ShakeEnabler!");
            return;
        }
        sShakeEnabler.removeSwitch(switch_);
    }
    
    public synchronized static void unregisterAllSwitches() {
        if (sShakeEnabler == null) {
            if (DEBUG_LOG) Log.d(TAG, "unregisterAllSwitches: null ShakeEnabler!");
            return;
        }
        sShakeEnabler.removeAllSwitches();
    }
    
    @Override
    protected void setPreference() {
        mPreferences.setShake(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = mSensorHubClient.addRequest(ContextInfo.Type.SHAKE);
        if(DEBUG_LOG) Log.d(TAG, "enableSensor: Shake. rid=" + mRequestId);
    }
}
