package com.mediatek.flightmode.sanitytest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.jayway.android.robotium.solo.Solo;

public class FlightModeTest
        extends
        ActivityInstrumentationTestCase2<com.android.settings.Settings.WirelessSettingsActivity> {

    private static final String TAG = "FlightModeTest";
    private static final String KEY_PROPERTY_OPERATOR = "ro.operator.optr";
    private static final String VALUE_PROPERTY_OP01 = "OP01";
    private static final String KEY_AIR_PALNE_MODE_STATE = "state";
    private static final int TIME_OUT = 1000;
    private static final int EVENT_SERVICE_STATE_CHANGED = 3;
    private static final int MAX_TRY_COUNT = 40;

    private Instrumentation mIns;
    private Activity mActivity;
    private Solo mSolo;
    private Context mContext;

    private boolean mCurrentState = false;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private ITelephony mPhoneMgr;
    private boolean mIsPhoneStateReady = false;
    private boolean mIsSimStateReady = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_SERVICE_STATE_CHANGED:
                mCurrentState = isAirplaneModeOn();
                break;
            }
        }
    };

    public FlightModeTest() {
        super("com.android.settings", com.android.settings.Settings.WirelessSettingsActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        mIns = getInstrumentation();
        mContext = mIns.getTargetContext();
        mActivity = getActivity();
        mSolo = new Solo(mIns, mActivity);

        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        mPhoneStateReceiver.registerIntent();
        mPhoneMgr = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));

        assertNotNull(mIns);
        assertNotNull(mContext);
        assertNotNull(mActivity);
        assertNotNull(mSolo);
    }

    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
        }
        mPhoneStateReceiver.unregisterIntent();

        super.tearDown();
    }

    public void test01_AirplaneModeEnable() {
        Log.d(TAG, "test01_AirplaneModeEnable() start");

        // /M: Set Air_Plane_Mode ON.
        if (!isAirplaneModeOn()) {
            setAirplaneModeOn(true);
        } else {
            Log.d(TAG, "Air plane state is already on");
        }

        // /M: Check the set result.
        checkCurrentState(true);
        Log.d(TAG, "test01_AirplaneModeEnable() end");
    }

    public void test02_AirplaneModeDisable() {
        Log.d(TAG, "test02_AirplaneModeDisable() start");

        // /M: Set Air_Plane_Mode OFF.
        if (isAirplaneModeOn()) {
            setAirplaneModeOn(false);
        } else {
            Log.d(TAG, "Air plane state is already off");
            return;
        }

        // /M: Check phone state.
        checkCurrentState(false);

        // /M: Check SIM state.
        checkSimCardState();
    }

    private void checkCurrentState(boolean exceptedState) {
        int tryCount = 0;
        do {
            mSolo.sleep(TIME_OUT);
            tryCount++;
            mIsPhoneStateReady = (exceptedState == mCurrentState);
            Log.d(TAG, "tryCount : " + tryCount + " mIsPhoneStateReady : "
                    + mIsPhoneStateReady);
        } while (!mIsPhoneStateReady && (tryCount < MAX_TRY_COUNT));

        Log.d(TAG, "exceptedState : " + exceptedState + " mCurrentState : "
                + mCurrentState);

        assertTrue("Change Airplane Mode State, phone state is ready ?  ",
                mIsPhoneStateReady);
    }

    private void checkSimCardState() {
        int tryCount = 0;
        do {
            mSolo.sleep(TIME_OUT);
            tryCount++;
            try {
                mIsSimStateReady = mPhoneMgr.isRadioOn();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "tryCount : " + tryCount + " mIsSimStateReady : "
                    + mIsSimStateReady);
        } while (!mIsSimStateReady && (tryCount < MAX_TRY_COUNT));

        assertTrue("Change Airplane Mode State, SIM state is ready ?  ",
                mIsSimStateReady);
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {
        Log.d(TAG, "setAirplaneModeOn : " + enabling);
        // /M: Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, enabling ? 1 : 0);

        // /M: Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra(KEY_AIR_PALNE_MODE_STATE, enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);

        // /M: Fix OP01 Pop Up Dialog
        dealWithOp01Customization();
    }

    private void dealWithOp01Customization() {
        if (VALUE_PROPERTY_OP01.equals(android.os.SystemProperties
                .get(KEY_PROPERTY_OPERATOR))) {
            Log.d(TAG, "op01 load, we should dismiss popup dialog");
            mSolo.sleep(TIME_OUT);
            if (!mActivity.hasWindowFocus()) {
                Log.d(TAG, "dismiss op01 dialog, go back");
                mSolo.goBack();
            }
        }
    }
}
