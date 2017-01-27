package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;

public class FacedownEnabler extends SensorHubEnabler {
    private static final String TAG = "FacedownEnabler";
    private static FacedownEnabler sFacedownEnabler;
    
    private FacedownEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getFacedown();
    }

    public synchronized  static FacedownEnabler getInstance(Context context) {
        if (sFacedownEnabler == null) {
            sFacedownEnabler = new FacedownEnabler(context);
        }

        return sFacedownEnabler;
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }
        
        if (sFacedownEnabler == null) {
            sFacedownEnabler = getInstance(context);
        }
        sFacedownEnabler.addSwitch(switch_);
    }
    
    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sFacedownEnabler == null) {
            return;
        }
        sFacedownEnabler.removeSwitch(switch_);
    }
    
    public synchronized static void unregisterAllSwitches() {
        if (sFacedownEnabler == null) {
            return;
        }
        sFacedownEnabler.removeAllSwitches();
    }
    
    @Override
    protected void setPreference() {
        mPreferences.setFacedown(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = mSensorHubClient.addRequest(ContextInfo.Type.FACING);
        if(DEBUG_LOG) Log.d(TAG, "enableSensor: FACE_DOWN. rid=" + mRequestId);
    }
}
