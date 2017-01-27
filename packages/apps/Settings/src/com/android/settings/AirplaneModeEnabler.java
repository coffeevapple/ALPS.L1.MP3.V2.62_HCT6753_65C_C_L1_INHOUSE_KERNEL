/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.settings.SubscriberPowerStateListener;
import com.mediatek.settings.SubscriberPowerStateListener.onRadioPowerStateChangeListener;
import com.mediatek.settings.cdma.CdmaAirplaneModeManager;


public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {

    private final Context mContext;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    
    private final SwitchPreference mSwitchPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;
    
    /// M : add for Bug fix ALPS01772247
    private static final String TAG = "AirplaneModeEnabler";
    private SubscriberPowerStateListener mListener;
    private CdmaAirplaneModeManager mCdmaAirModeManager;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onAirplaneModeChanged();
                    break;
            }
        }
    };

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onAirplaneModeChanged();
        }
    };

    public AirplaneModeEnabler(Context context, SwitchPreference airplaneModeSwitchPreference) {
        
        mContext = context;
        mSwitchPref = airplaneModeSwitchPreference;

        airplaneModeSwitchPreference.setPersistent(false);
    
        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        
        /// M : bug fix for ALPS01772247 / ALPS01810660 @{
        initListener(context);
        /// @}
    }

    // Only for phone, tablet no need to monitor SIM state
    private void initListener(Context context) {
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            mCdmaAirModeManager = new CdmaAirplaneModeManager(mContext, mSwitchPref);
        } else {
            if (!Utils.isWifiOnly(mContext)) {
                mListener = new SubscriberPowerStateListener(context);
                mListener.setRadioPowerStateChangeListener(new onRadioPowerStateChangeListener() {
                    @Override
                    public void onAllPoweredOff() {
                        mSwitchPref.setEnabled(true);
                    }
                    @Override
                    public void onAllPoweredOn() {
                        mSwitchPref.setEnabled(true);
                    }
                });
            }
        }
    }

    public void resume() {
        
        mSwitchPref.setChecked(isAirplaneModeOn(mContext));

        mPhoneStateReceiver.registerIntent();
        mSwitchPref.setOnPreferenceChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true,
                mAirplaneModeObserver);
        // M: For CDMA LTE only
        if (mCdmaAirModeManager != null) {
            mCdmaAirModeManager.setEnable();
            mCdmaAirModeManager.registerBroadCastReceiver();
        }
        //
    }
    
    public void pause() {
        mPhoneStateReceiver.unregisterIntent();
        mSwitchPref.setOnPreferenceChangeListener(null);
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
        if (mCdmaAirModeManager != null) {
            mCdmaAirModeManager.unRegisterBroadCastReceiver();
        }
    }

    /// M : add for bug fix ALPS01772247@{
    public void destroy() {
        if (mListener != null) {
            mListener.unRegisterListener();
        }
    }
    /// @}
    
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 
                                enabling ? 1 : 0);
        // Update the UI to reflect system setting
        mSwitchPref.setChecked(enabling);

        /// M : bug fix for ALPS01772247 / ALPS01810660 @{
        registerSubState();
        /// @}

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    //Only for phone, tablet no need to register
    private void registerSubState() {
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            mSwitchPref.setEnabled(false);
        } else {
            if (!Utils.isWifiOnly(mContext)) {
                mSwitchPref.setEnabled(false);
                if (mListener != null) {
                    mListener.registerListener();
                }
            }
        }
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified
     * that mobile radio is powered up/down. We should not have dependency
     * on one radio alone. We need to do the following:
     * - handle the case of wifi/bluetooth failures
     * - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
        mSwitchPref.setChecked(isAirplaneModeOn(mContext));
    }
    
    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode, do not update database at this point
        } else {
            setAirplaneModeOn((Boolean) newValue);
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }
    


    /*
     * M: Bug fix for ALPS01899413
     * When hot swap happend, need to update listeners for changed subscribers
     */
    public void updateSubscribers() {
        if (mListener != null) {
            Log.d(TAG, "updateSubscribers");
            mListener.updateSubscribers();
        }
    }

}
