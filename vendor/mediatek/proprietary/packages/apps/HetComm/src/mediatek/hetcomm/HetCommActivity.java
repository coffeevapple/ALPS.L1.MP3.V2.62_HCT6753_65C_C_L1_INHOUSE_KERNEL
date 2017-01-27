package com.mediatek.hetcomm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Common UI activity for HetComm.
 *
 * @hide
 */
public class HetCommActivity extends Activity implements OnClickListener {
    private static final String TAG = "HetCommActivity";

    public static final String ALL_SETTINGS = "settings";
    public static final String HET_COMMON_SETTING = "het_common_setting";

    private AlertDialog mDialog;
    private Context mContext;
    private SharedPreferences mSharedPref;
    private Switch mEnableSwitch;
    private TextView mEnableSwitchStatus;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnManager;
    private static final NetworkRequest VPN_REQUEST = new NetworkRequest.Builder()
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
        .build();
    private boolean mIsVpnOn = false;
    private boolean mIsTetherOn = false;
    private boolean mIsRoaming = false;
    private boolean mAlreadyOn = false;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.hetcomm_activity);
        mContext = this.getBaseContext();

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mTelephonyManager = (TelephonyManager)
                            mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mConnManager = (ConnectivityManager)
                            mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mEnableSwitchStatus = (TextView) findViewById(R.id.enable_switch_status);
        mEnableSwitch = (Switch) findViewById(R.id.enable_switch);

        mSharedPref = this.getSharedPreferences(ALL_SETTINGS, 0);
        mAlreadyOn = mSharedPref.getBoolean(HET_COMMON_SETTING, false);
        preCheckStatus();
        //attach a listener to check for changes in state
        mEnableSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(TAG, "onCheckedChanged = " + isChecked);
                //onCheckedChanged would be called while first time set the listener
                if (mAlreadyOn) {
                    mAlreadyOn = false;
                    Log.i(TAG, "onCheckedChanged return");
                    return;
                }

                if (isChecked) {
                    showConnectionPrompt();
                }
                updateHetCommSetting(isChecked);
                showHetCommSetting(isChecked);
                runHetCommService(isChecked);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            Intent intent = new Intent(HetCommActivity.this, HetCommHelpActivity.class);            
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        boolean isEnabled = mSharedPref.getBoolean(HET_COMMON_SETTING, false);
        showHetCommSetting(isEnabled);
        runHetCommService(isEnabled);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        //finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        mConnManager.unregisterNetworkCallback(mNetworkCallback);
    }

    /**
     * display the HetComm settings status.
     *
     */
     private void showHetCommSetting(boolean isEnabled) {
        Log.i(TAG, "showHetCommSetting enable = " + isEnabled);
        mEnableSwitch.setChecked(isEnabled);

        if (isEnabled) {
            mEnableSwitchStatus.setText(R.string.enable_switch_on);
        } else {
            mEnableSwitchStatus.setText(R.string.enable_switch_off);
        }
     }

    /**
     * Save the HetComm enable setting into shared preference.
     *
     */
    private void updateHetCommSetting(boolean isEnabled) {
        Log.i(TAG, "updateHetCommSetting enable = " + isEnabled);
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(HET_COMMON_SETTING, isEnabled);
        editor.commit();
        int value  = (isEnabled) ? 1 : 0;

        Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.HETCOMM_ENABLED, value);
    }

    /**
     * Start/Stop HetComm service.
     *
     */
    private void runHetCommService(boolean isEnabled) {
        Intent serviceIntent = new Intent(HetCommActivity.this, HetCommService.class);

        if (isEnabled) {
            Log.i(TAG, "Start HetComm Service");
            startService(serviceIntent);
        } else {
            Log.i(TAG, "Stop HetComm Service");
            stopService(serviceIntent);
        }
    }

    /**
     * check if Tethering/Vpn already on
     */
    private void preCheckStatus() {
        mConnManager.registerNetworkCallback(VPN_REQUEST, mNetworkCallback);
        NetworkInfo info = mConnManager.getActiveNetworkInfo();
        if (info != null) {
            mIsRoaming = info.isRoaming();
        }
        mIsTetherOn = mConnManager.getTetheredIfaces().length > 1 ? true:false;
    }
  
    /**
     * Utility function for check Wi-Fi or Mobile connection status.
     */
    private void showConnectionPrompt() {
        boolean isWifiEnabled = mWifiManager.isWifiEnabled() ;
        boolean isMobileEnabled = mTelephonyManager.getDataEnabled();

        if (mIsTetherOn || mIsVpnOn || mIsRoaming) {
            Log.i(TAG, "not showConnectionPrompt");
            return;
        }

        Log.i(TAG, "wif:" + isWifiEnabled + " mobile:" + isMobileEnabled);
        final Resources r = mContext.getResources();
        String notice_title = r.getString(R.string.conn_notice_title);
        String notice_detail = r.getString(R.string.conn_notice_detail);
        String notice_warning = r.getString(R.string.conn_charging_warning);

        if (!isWifiEnabled || !isMobileEnabled) {
            clearPrompt();
            String buttonText;
            String wlanConn = r.getString(R.string.connection_wlan);
            String mobileConn = r.getString(R.string.connection_mobile);

            if (!isWifiEnabled && !isMobileEnabled) {
                buttonText = r.getString(R.string.turn_on, wlanConn + " & " + mobileConn);
            } else if (!isWifiEnabled) {
                buttonText = r.getString(R.string.turn_on, wlanConn);
            } else {
                buttonText = r.getString(R.string.turn_on, mobileConn);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(HetCommActivity.this)
            .setTitle(r.getString(R.string.app_name))
            .setMessage(notice_title + notice_detail + notice_warning)
            .setPositiveButton(buttonText, this)
            .setNegativeButton(android.R.string.cancel, this)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    turnOffHetComm();
                }
            })
            .setOnKeyListener(new OnKeyListener() {         
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                        turnOffHetComm();
                        mDialog.dismiss();
                        return true;   
                    }
                    return false;
                }             
            });
            mDialog = builder.create();
            mDialog.show();
        } else if (isWifiEnabled && isMobileEnabled) {
            AlertDialog.Builder builder = new AlertDialog.Builder(HetCommActivity.this)
            .setTitle(r.getString(R.string.app_name))
            .setMessage(notice_warning)
            .setPositiveButton(android.R.string.ok, this)
            .setOnKeyListener(new OnKeyListener() {         
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                        turnOffHetComm();
                        mDialog.dismiss();
                        return true;   
                    }
                    return false;
                }             
            });
            mDialog = builder.create();
            mDialog.show();
        }

    }

    private void turnOffHetComm() {
        updateHetCommSetting(false);
        showHetCommSetting(false);
        runHetCommService(false);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (DialogInterface.BUTTON_POSITIVE == which) {
            boolean isWifiEnabled = mWifiManager.isWifiEnabled() ;
            boolean isMobileEnabled = mTelephonyManager.getDataEnabled();

            Log.i(TAG, "isMobileEnabled:" + isMobileEnabled);
            if (!isMobileEnabled) {
                if (SubscriptionManager.getDefaultDataSubId()
                        == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    try {
                        startActivity(new Intent(
                                    "com.android.settings.sim.SIM_SUB_INFO_SETTINGS"));
                        return;
                    } catch(ActivityNotFoundException ae) {
                        Log.e(TAG, "No activity for sim setting");
                    }
                }
                mTelephonyManager.setDataEnabled(true);
                Log.i(TAG, "Turn on mobile data connection");
            }

            if (!isWifiEnabled) {
                Log.i(TAG, "Start Wi-Fi setting");
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        } else if ( DialogInterface.BUTTON_NEGATIVE == which) {
            turnOffHetComm();
        }
    }

    /*
     * Clear the previous dialog instance.
     */
    private void clearPrompt() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            NetworkCapabilities networkCapabilities =
                mConnManager.getNetworkCapabilities(network);

            Log.d(TAG, "onAvailable " + network.netId + " : " + networkCapabilities);

            if (networkCapabilities == null) {
                Log.e(TAG, "The connection could be disconnected:" + network);
                return;
            }

            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                mIsVpnOn = true;
            }
        };
    };
}
