package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;

public class InPocketEnabler extends SensorHubEnabler {
    private static final String TAG = "InPocketEnabler";
    private static InPocketEnabler sInPocketEnabler;
    
    private InPocketEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getInPocket();
    }

    public synchronized static InPocketEnabler getInstance(Context context) {
        if (sInPocketEnabler == null) {
            sInPocketEnabler = new InPocketEnabler(context);
        }

        return sInPocketEnabler;
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }
        
        if (sInPocketEnabler == null) {
            sInPocketEnabler = getInstance(context);
        }
        sInPocketEnabler.addSwitch(switch_);
    }
    
    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sInPocketEnabler == null) {
            return;
        }
        sInPocketEnabler.removeSwitch(switch_);
    }
    
    public synchronized static void unregisterAllSwitches() {
        if (sInPocketEnabler == null) {
            return;
        }
        sInPocketEnabler.removeAllSwitches();
    }
    
    @Override
    protected void setPreference() {
        mPreferences.setInPocket(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = mSensorHubClient.addRequest(ContextInfo.Type.CARRY);
        if(DEBUG_LOG) Log.d(TAG, "enableSensor: IN_POCKET. rid=" + mRequestId);
    }
}
