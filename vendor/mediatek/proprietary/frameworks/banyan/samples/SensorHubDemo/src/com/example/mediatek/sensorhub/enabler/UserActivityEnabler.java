package com.example.mediatek.sensorhub.enabler;

import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import com.mediatek.sensorhub.ContextInfo;

public class UserActivityEnabler extends SensorHubEnabler {
    private static final String TAG = "UserActivityEnabler";
    private static UserActivityEnabler sUserActivityEnabler;
    
    private UserActivityEnabler(Context context) {
        super(context);
        mIsChecked = mPreferences.getUserActivity();
    }

    public synchronized static UserActivityEnabler getInstance(Context context) {
        if (sUserActivityEnabler == null) {
            sUserActivityEnabler = new UserActivityEnabler(context);
        }

        return sUserActivityEnabler;
    }

    public synchronized static void registerSwitch(Context context, Switch switch_) {
        if (context == null || switch_ == null) {
            return;
        }
        
        if (sUserActivityEnabler == null) {
            sUserActivityEnabler = getInstance(context);
        }
        sUserActivityEnabler.addSwitch(switch_);
    }
    
    public synchronized static void unregisterSwitch(Switch switch_) {
        if (sUserActivityEnabler == null) {
            return;
        }
        sUserActivityEnabler.removeSwitch(switch_);
    }
    
    public synchronized static void unregisterAllSwitches() {
        if (sUserActivityEnabler == null) {
            return;
        }
        sUserActivityEnabler.removeAllSwitches();
    }
    
    @Override
    protected void setPreference() {
        mPreferences.setUserActivity(mIsChecked);
    }

    @Override
    protected void enableSensor() {
        mRequestId = mSensorHubClient.addRequest(ContextInfo.Type.USER_ACTIVITY);
        if(DEBUG_LOG) Log.d(TAG, "enableSensor: UserActivity. rid=" + mRequestId);
    }
}
