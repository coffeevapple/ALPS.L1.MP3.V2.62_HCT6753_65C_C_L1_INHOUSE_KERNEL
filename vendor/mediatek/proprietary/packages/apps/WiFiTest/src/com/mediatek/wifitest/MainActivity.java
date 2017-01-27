package com.mediatek.wifitest;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.RttManager;
import android.net.wifi.RttManager.RttResult;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;





/**
 * wifi Test
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "wifiTest";

    public static final String LLS_RESULTS_AVAILABLE_ACTION =
            "android.net.wifi.LLS_RESULTS";
    public static final String EXTRA_LLS_INFO = "extra_lls_info";

    private EditText[] mChannelSetEdit;
    private EditText[] mChannelSetEdit2;
    int support_gscan_set = 0;
    private EditText mSupportSetEdit;

    private TextView mScanList;
    private TextView mOnChangingList;
    private TextView mOnQuiescenceList;


    private EditText mWifiChangeEdit;
    private EditText mWifiChangeEdit2;

    private EditText mhotlistEdit;
    private TextView mHotlistResultList;
    private TextView mLlsResultList;

    private TextView mWifiCapability;

        
    WifiManager mWifiManager;
    WifiScanner mWifiScanner;
    RttManager mRttManager;

//extend to 8 set
    WifiScanner.ScanListener[] scanlistener;
    WifiScanner.WifiChangeListener wifichangelistener;
    WifiScanner.BssidListener bssidListener ;

    RttManager.RttListener rttListener;

   private IntentFilter mllsFilter;

    Toast toast;




    public static class ScanResultViewInfo {
        ScanResult[] list;
        TextView tv;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start_gscan_btn).setOnClickListener(this);
        findViewById(R.id.stop_gscan_btn).setOnClickListener(this);
        findViewById(R.id.get_gscan_result_btn).setOnClickListener(this);

        findViewById(R.id.start_track_btn).setOnClickListener(this);
        findViewById(R.id.stop_track_btn).setOnClickListener(this);

        findViewById(R.id.set_wifichange_btn).setOnClickListener(this);

        findViewById(R.id.start_hotlist_btn).setOnClickListener(this);
        findViewById(R.id.stop_hotlist_btn).setOnClickListener(this);
        findViewById(R.id.start_ranging_btn).setOnClickListener(this);
        findViewById(R.id.stop_ranging_btn).setOnClickListener(this);

        findViewById(R.id.wificap_btn).setOnClickListener(this);

        mSupportSetEdit = (EditText) findViewById(R.id.supportset_value);

        mChannelSetEdit = new EditText[8];
        mChannelSetEdit2 = new EditText[8];

        for (int i = 0; i < 8 ; i++) {
            mChannelSetEdit[i] = (EditText) new EditText(this);
            mChannelSetEdit2[i] = (EditText) new EditText(this);
        }

        mChannelSetEdit[0] = (EditText) findViewById(R.id.set1_value1);
        mChannelSetEdit2[0] = (EditText) findViewById(R.id.set1_value2);
        mChannelSetEdit[1] = (EditText) findViewById(R.id.set2_value1);
        mChannelSetEdit2[1] = (EditText) findViewById(R.id.set2_value2);
        mChannelSetEdit[2] = (EditText) findViewById(R.id.set3_value1);
        mChannelSetEdit2[2] = (EditText) findViewById(R.id.set3_value2);
        mChannelSetEdit[3] = (EditText) findViewById(R.id.set4_value1);
        mChannelSetEdit2[3] = (EditText) findViewById(R.id.set4_value2);
        mChannelSetEdit[4] = (EditText) findViewById(R.id.set5_value1);
        mChannelSetEdit2[4] = (EditText) findViewById(R.id.set5_value2);
        mChannelSetEdit[5] = (EditText) findViewById(R.id.set6_value1);
        mChannelSetEdit2[5] = (EditText) findViewById(R.id.set6_value2);
        mChannelSetEdit[6] = (EditText) findViewById(R.id.set7_value1);
        mChannelSetEdit2[6] = (EditText) findViewById(R.id.set7_value2);
        mChannelSetEdit[7] = (EditText) findViewById(R.id.set8_value1);
        mChannelSetEdit2[7] = (EditText) findViewById(R.id.set8_value2);

        scanlistener = new WifiScanner.ScanListener[8];
        for (int i = 0; i < 8; i++) {
            scanlistener[i] = null;
        }

        mScanList = (TextView) findViewById(R.id.scan_list);
        mOnChangingList = (TextView) findViewById(R.id.on_change_list);
        mOnQuiescenceList = (TextView) findViewById(R.id.on_mquiescence_list);

        mLlsResultList = (TextView) findViewById(R.id.lls_list);
        mWifiChangeEdit = (EditText) findViewById(R.id.wifi_change_value);
        mWifiChangeEdit2 = (EditText) findViewById(R.id.wifi_change_value2);

        mhotlistEdit = (EditText) findViewById(R.id.hotlist_value);
        mHotlistResultList = (TextView) findViewById(R.id.hotlist_list);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        mWifiScanner = (WifiScanner) getSystemService(WIFI_SCANNING_SERVICE);
        mRttManager = (RttManager) getSystemService(WIFI_RTT_SERVICE);

        findViewById(R.id.start_gscan_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_gscan_btn).setVisibility(View.GONE);

        findViewById(R.id.start_track_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_track_btn).setVisibility(View.GONE);

        findViewById(R.id.start_hotlist_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_hotlist_btn).setVisibility(View.GONE);
        findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);

        mllsFilter = new IntentFilter(LLS_RESULTS_AVAILABLE_ACTION);

        mWifiCapability = (TextView) findViewById(R.id.wifi_capability);

        findViewById(R.id.wificap_btn).setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        //M: batched scan
        registerReceiver(mLlsReceiver, mllsFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        //M: batched scan
        unregisterReceiver(mLlsReceiver);
    }

    private Boolean stopGScan(int setNum) {

        if (scanlistener[setNum] != null) {
            mWifiScanner.stopBackgroundScan(scanlistener[setNum]);
            scanlistener[setNum] = null;
            mScanList.setText("");
        }
        return true;
    }
    private WifiScanner.ScanSettings getGscanSetting(int setNum) {

        Log.d(TAG, "start_gscan_btn first line= " + mChannelSetEdit[setNum].getText().toString());
        Log.d(TAG, "start_gscan_btn 2nd line= " + mChannelSetEdit2[setNum].getText().toString());
        //1. get scanSettings -- support 5 channel settings
        WifiScanner.ScanSettings ssettings = new WifiScanner.ScanSettings();
        List<String> band_period_rEvents_num =
            new ArrayList(Arrays.asList(mChannelSetEdit[setNum].getText().toString().split(",")));
        if (band_period_rEvents_num.size() != 4) {
            Log.d(TAG, "innput wrong size()="
                + band_period_rEvents_num.size() +
                " band_period_rEvents_num=" + band_period_rEvents_num);
            for (int a = 0; a < band_period_rEvents_num.size(); a++) {
                Log.d(TAG, a + "= " + band_period_rEvents_num.get(a));
            }
            showToast("band_period_rEvents_num input wrong");
            return null;
        }
        ssettings.band = Integer.parseInt(band_period_rEvents_num.get(0));
        ssettings.periodInMs = Integer.parseInt(band_period_rEvents_num.get(1));
        ssettings.reportEvents = Integer.parseInt(band_period_rEvents_num.get(2));
        ssettings.numBssidsPerScan = Integer.parseInt(band_period_rEvents_num.get(3));

        Collection<String> input2 =
            new ArrayList(Arrays.asList(mChannelSetEdit2[setNum].getText().toString().split(";")));
        int i = 0;
        boolean parsingDone = true;
        if (input2.size() > 0) {
            ssettings.channels = new WifiScanner.ChannelSpec[input2.size()];
            for (String channel : input2) {
                Log.d(TAG, "channel " + i + ":" + channel);
                String modifiedString = channel.replaceAll("\\(", "");
                modifiedString = modifiedString.replaceAll("\\)", "");
                modifiedString = modifiedString.replaceAll(";", "");
                modifiedString = modifiedString.replaceAll(" ", "");
                String[] tokens = modifiedString.split(",");
                Log.d(TAG, "channels.length:" + tokens.length);
                if (tokens.length != 3) {
                    showToast("channels input wrong");
                    return null;
                }
                WifiScanner.ChannelSpec ch =
                    new WifiScanner.ChannelSpec(Integer.parseInt(tokens[0], 10));
                ch.passive = (Integer.parseInt(tokens[1], 10) == 1) ? true : false;
                ch.dwellTimeMS = Integer.parseInt(tokens[2], 10);
                ssettings.channels[i] = ch;
                Log.d(TAG, "ssettings.channelSet = " + ssettings.channels);
                i++;
            }
        } else {
            ssettings.channels = null;
        }
        if (!isValidScanSettings(ssettings)) {
            return null;
        }
        return ssettings;

    }

    @Override
    public void onClick(View v) {
	    try{
        // 1. Start G-Scan
        if (v == findViewById(R.id.start_gscan_btn)) {

            //1. get support set
            support_gscan_set =  (int) Integer.parseInt(mSupportSetEdit.getText().toString(), 10);
            if (support_gscan_set < 0 || support_gscan_set > 8) {
                showToast("support set is wrong " + support_gscan_set);
                return;
            }
            Log.d(TAG, "support_gscan_set " + support_gscan_set);
            for (int i = 0; i < support_gscan_set ; i++) {
                //1. get scan settings
                WifiScanner.ScanSettings ssettings = getGscanSetting(i);
                if (ssettings == null) {
                    Log.d(TAG, "getGscanSetting fail on " + i);
                    for (int k = 0; k < i - 1; k++) {
                        stopGScan(k);
                    }
                    showToast(" set " + (i + 1) + " settings is wrong ");
                    return;
                }
                //2. get scanlistener
                if (scanlistener[i] != null) {
                    //stop previous first.
                    mWifiScanner.stopBackgroundScan(scanlistener[i]);
                    scanlistener[i] = null;
                    mScanList.setText("");
                }
                scanlistener[i] = new WifiScanner.ScanListener() {
                    public void onPeriodChanged(int periodInMs) {
                        Log.d(TAG, "ScanListener onPeriodChanged");
                    }
                    public void onResults(ScanResult[] results) {
                        Log.d(TAG, "ScanListener onResults QQ");
                        setScanListToView(results, mScanList);
//                        setScanList(results,mScanList);
                    }
                    public void onFullResult(ScanResult fullScanResult) {
                        Log.d(TAG, "ScanListener onFullResult");
                    }
                    public void onSuccess() {
                        Log.d(TAG, "ScanListener onSuccess");
                    }
                    public void onFailure(int reason, String description) {
                        Log.d(TAG, "ScanListener onFailure reason=" +
                            reason + " description=" + description);
                    }
                } ;
                if (ssettings == null || scanlistener == null || mWifiScanner == null) {
                    Log.d(TAG, "ssettings==null || scanlistener==null || mWifiScanner==null");
                    return;
                }
                Log.d(TAG, "(call mWifiScanner.startBackgroundScan");
                mWifiScanner.startBackgroundScan(ssettings, scanlistener[i]);
            }
            findViewById(R.id.start_gscan_btn).setVisibility(View.GONE);
            findViewById(R.id.stop_gscan_btn).setVisibility(View.VISIBLE);
        // 2. Stop G-Scan
        } else if (v == findViewById(R.id.stop_gscan_btn)) {
            Log.d(TAG, "stop_gscan_btn");
            for (int i = 0; i < support_gscan_set; i++) {
                Log.d(TAG, "stopGScan" + i);
                stopGScan(i);
            }
            findViewById(R.id.start_gscan_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_gscan_btn).setVisibility(View.GONE);
        // 3. Get G Scan Result
        } else if (v == findViewById(R.id.get_gscan_result_btn)) {
            Log.d(TAG, "get_gscan_result_btn");
            ScanResult[] list = mWifiScanner.getScanResults();
            if (list != null) {
                Log.e(TAG, "setScanList A");
                setScanList(list, mScanList);
            } else {
                setScanListString("getScanResults null", mScanList);
            }
        ///4. Start tracking wifi change
        } else if (v == findViewById(R.id.start_track_btn)) {
            Log.d(TAG, "start_track_btn");
            if (wifichangelistener != null) {
                mWifiScanner.stopTrackingWifiChange(wifichangelistener);
                wifichangelistener = null;
                Log.e(TAG, "setScanList B");
                setScanList(null, mOnChangingList);
                Log.e(TAG, "setScanList C");
                setScanList(null, mOnQuiescenceList);
            }
            wifichangelistener = new WifiScanner.WifiChangeListener() {
                public void onChanging(ScanResult[] results) {
                    Log.d(TAG, "WifiChangeListener onChanging");
                    setScanListToView(results, mOnChangingList);
                    //setScanList(results,mOnChangingList);
                }
                public void onQuiescence(ScanResult[] results) {
                    Log.d(TAG, "WifiChangeListener onQuiescence");
                    setScanListToView(results, mOnQuiescenceList);
                }
                public void onSuccess() {
                    Log.d(TAG, "WifiChangeListener onSuccess");
                }
                public void onFailure(int reason, String description) {
                    Log.d(TAG, "WifiChangeListener onFailure reason=" +
                        reason + " description=" + description);
                }
            } ;
            mWifiScanner.startTrackingWifiChange(wifichangelistener);
            findViewById(R.id.start_track_btn).setVisibility(View.GONE);
            findViewById(R.id.stop_track_btn).setVisibility(View.VISIBLE);
         //5. Stop Tracking wifi change
        } else if (v == findViewById(R.id.stop_track_btn)) {
            Log.d(TAG, "stop_track_btn");
            if (wifichangelistener != null) {
                mWifiScanner.stopTrackingWifiChange(wifichangelistener);
                wifichangelistener = null;
                setScanList(null, mOnChangingList);
                setScanList(null, mOnQuiescenceList);
            }
            findViewById(R.id.start_track_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_track_btn).setVisibility(View.GONE);
        ///6. config wifi change
        } else if (v == findViewById(R.id.set_wifichange_btn)) {
            Log.d(TAG, "set_wifichange_btn");
            List<String> para1 =
                new ArrayList(Arrays.asList(mWifiChangeEdit.getText().toString().split(",")));
            List<String> para2 =
                new ArrayList(Arrays.asList(mWifiChangeEdit2.getText().toString().split(";")));
            if (para1.size() != 5 || para2 == null) {
                Log.d(TAG, "input wrong: intput1.size=" + para1.size() + " input2=" + para2);
                showToast("input wrong");
                return;
            }
            WifiScanner.BssidInfo[] bssidInfos = new WifiScanner.BssidInfo[para2.size()];
            for (int i = 0; i < para2.size(); i++) {
                List<String> bssidinfolist =
                    new ArrayList(Arrays.asList(para2.get(i).toString().split(",")));
                WifiScanner.BssidInfo  bssidonfo = new WifiScanner.BssidInfo();
                bssidonfo.bssid = bssidinfolist.get(0);
                bssidonfo.low = Integer.parseInt(bssidinfolist.get(1), 10);
                bssidonfo.high = Integer.parseInt(bssidinfolist.get(2), 10);
                bssidonfo.frequencyHint = Integer.parseInt(bssidinfolist.get(3), 10);
                bssidInfos[i] = bssidonfo;
            }
            mWifiScanner.configureWifiChange(
                Integer.parseInt(para1.get(0), 10),
                Integer.parseInt(para1.get(1), 10),
                Integer.parseInt(para1.get(2), 10),
                Integer.parseInt(para1.get(3), 10),
                Integer.parseInt(para1.get(4), 10),
                bssidInfos
            );
        //7. start tracking hotlist
        } else if (v == findViewById(R.id.start_hotlist_btn)) {
            Log.d(TAG, "start_hotlist_btn");
            if (bssidListener != null) {
                mWifiScanner.stopTrackingBssids(bssidListener);
                bssidListener = null;
                setScanList(null, mHotlistResultList);
            }
            List<String> hotlistString =
                new ArrayList(Arrays.asList(mhotlistEdit.getText().toString().split(";")));
            if (hotlistString == null) {
                Log.d(TAG, "hotlistString null");
                return;
            }
            WifiScanner.BssidInfo[] hotlistInfo = new WifiScanner.BssidInfo[hotlistString.size()];
            for (int i = 0; i < hotlistString.size(); i++) {
                List<String> bssidinfolist =
                    new ArrayList(Arrays.asList(hotlistString.get(i).toString().split(",")));
                WifiScanner.BssidInfo  hotitem = new WifiScanner.BssidInfo();
                hotitem.bssid = bssidinfolist.get(0);
                hotitem.low = Integer.parseInt(bssidinfolist.get(1), 10);
                hotitem.high = Integer.parseInt(bssidinfolist.get(2), 10);
                hotitem.frequencyHint = Integer.parseInt(bssidinfolist.get(3), 10);
                hotlistInfo[i] = hotitem;
            }
            bssidListener = new WifiScanner.BssidListener() {
                public void onFound(ScanResult[] results) {
                    Log.d(TAG, "BssidListener onFound");
                    setScanListToView(results, mHotlistResultList);
                }
                public void onSuccess() {
                    Log.d(TAG, "BssidListener onSuccess");
                }
                public void onFailure(int reason, String description) {
                    Log.d(TAG, "BssidListener onFailure reason=" +
                        reason + " description=" + description);

                }
            } ;
            mWifiScanner.startTrackingBssids(hotlistInfo, 0, bssidListener);

            findViewById(R.id.start_hotlist_btn).setVisibility(View.GONE);
            findViewById(R.id.stop_hotlist_btn).setVisibility(View.VISIBLE);
        // 8. Stop Tracking hotlist
        } else if (v == findViewById(R.id.stop_hotlist_btn)) {
            Log.d(TAG, "stop_hotlist_btn");
            if (bssidListener != null) {
                Log.d(TAG, " kill bssidListenerList[0]");
                mWifiScanner.stopTrackingBssids(bssidListener);
                bssidListener = null;
                setScanList(null, mHotlistResultList);
            }
            findViewById(R.id.start_hotlist_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_hotlist_btn).setVisibility(View.GONE);
        //9. start RTT
        } else if (v == findViewById(R.id.start_ranging_btn)) {
            Log.d(TAG, "start_ranging_btn");
            if (rttListener != null) {
                mRttManager.stopRanging(rttListener);
                rttListener = null;
            }
            RttManager.RttParams[] rttparalist = new RttManager.RttParams[1];
            RttManager.RttParams rttpara = new RttManager.RttParams();

            rttpara.deviceType = RttManager.RTT_PEER_TYPE_AP;
            rttpara.requestType = RttManager.RTT_TYPE_UNSPECIFIED;
            rttpara.bssid = "11:22:33:44:55:66";
            rttpara.frequency = 2412;
            rttpara.channelWidth = RttManager.RTT_CHANNEL_WIDTH_UNSPECIFIED;
            rttpara.num_samples = 3;
            rttpara.num_retries = 3;

            rttparalist[0] = rttpara;

            rttListener = new RttManager.RttListener() {
                public void onSuccess(RttResult[] results) {
                     Log.d(TAG, "RttListener onSuccess");
                }
                public void onFailure(int reason, String description) {
                    Log.d(TAG, "RttListener onFailure reason=" +
                        reason + " description=" + description);
                }
                public void onAborted() {
                    Log.d(TAG, "RttListener onAborted");
                }
            } ;
            if (mRttManager != null) {
                mRttManager.startRanging(rttparalist, rttListener);
            } else {
                Log.d(TAG, "mRttManager==null");
            }
            findViewById(R.id.start_ranging_btn).setVisibility(View.GONE);
            findViewById(R.id.stop_ranging_btn).setVisibility(View.VISIBLE);
        //10. stop RTT
        } else if (v == findViewById(R.id.stop_ranging_btn)) {
            Log.d(TAG, "stop_ranging_btn");
            if (rttListener != null) {
                mRttManager.stopRanging(rttListener);
                rttListener = null;
            }
            findViewById(R.id.start_ranging_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.stop_ranging_btn).setVisibility(View.GONE);
        } else if (v == findViewById(R.id.wificap_btn)) {
            Log.d(TAG, "wificap_btn");
            mWifiCapability.setText(String.valueOf(getWifiCapability()));
        }
        } catch (Exception e) {
               
        }
    }

    private final BroadcastReceiver mLlsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LLS_RESULTS_AVAILABLE_ACTION)) {
                Log.e(TAG, "Received BATCHED_SCAN_RESULTS_AVAILABLE_ACTION");
                StringBuffer llsList = new StringBuffer();
                WifiLinkLayerStats stats =
                    (WifiLinkLayerStats) intent.getParcelableExtra(EXTRA_LLS_INFO);
                llsList.append(stats);
                mLlsResultList.setText(llsList);
            } else {
                Log.e(TAG, "Received an unknown Wifi Intent");
            }
        }
    };

    private boolean isValidScanSettings(WifiScanner.ScanSettings s) {
        if (s.band <= WifiScanner.WIFI_BAND_UNSPECIFIED ||
            s.band > WifiScanner.WIFI_BAND_BOTH_WITH_DFS) {
            Log.e(TAG, "isValidScanSettings: band invalid set to WIFI_BAND_UNSPECIFIED");
            s.band = WifiScanner.WIFI_BAND_UNSPECIFIED;
            showToast("band invalid. set to WIFI_BAND_UNSPECIFIED");
        }
        if (s.channels == null) {
            if (s.band == WifiScanner.WIFI_BAND_UNSPECIFIED) {
                showToast("Failure. s.channels = null and no band");
                return false;
            }
        }
        if (s.periodInMs < WifiScanner.MIN_SCAN_PERIOD_MS  ||
            s.periodInMs > WifiScanner.MAX_SCAN_PERIOD_MS) {
            showToast("Failure. periodInMs = invalid");
            return false;
        }
        if (s.reportEvents < WifiScanner.REPORT_EVENT_AFTER_BUFFER_FULL
            || s.reportEvents > WifiScanner.REPORT_EVENT_FULL_SCAN_RESULT) {
            showToast("Failure. reportEvents = invalid");
            return false;
        }
        if (s.numBssidsPerScan <= 0) {
            showToast("Failure. numBssidsPerScan = invalid");
            return false;
        }
        return true;
    }
    private void showToast(String s) {
        toast = Toast.makeText(this, s, Toast.LENGTH_LONG);
        toast.show();
    }

    private void setScanListString(String s, TextView tv) {
        tv.setText(s);
        return;
    }
    private void setScanList(ScanResult[] list, TextView tv) {

        StringBuffer scanList = new StringBuffer();
        if (list != null) {
            for (int i = list.length - 1; i >= 0; i--) {
                final ScanResult scanResult = list[i];

                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }
                scanList.append(list[i]);
            }
            tv.setText(scanList);
        } else {
            tv.setText("");
            return;
        }
    }

    private void setScanListToView(ScanResult[] list, TextView tv) {

        Message msg = handler.obtainMessage();
        ScanResultViewInfo sv = new ScanResultViewInfo();
        sv.list = list;
        sv.tv = tv;
        msg.obj = sv;
        handler.sendMessage(msg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            ScanResultViewInfo sv = (ScanResultViewInfo) msg.obj;
            setScanList(sv.list, sv.tv);
        }
    };


    private String getWifiCapability() {
        StringBuilder sb = new StringBuilder();
        sb.append("Support for 5 GHz Band: " + mWifiManager.is5GHzBandSupported() + "\n");
        sb.append("Wifi-Direct Support: " + mWifiManager.isP2pSupported() + "\n");
        sb.append("GAS/ANQP Support: "
                + mWifiManager.isPasspointSupported() + "\n");
        sb.append("Soft AP Support: "
                + mWifiManager.isPortableHotspotSupported() + "\n");
        sb.append("WifiScanner APIs Support: "
                + mWifiManager.isWifiScannerSupported() + "\n");
        sb.append("Neighbor Awareness Networking Support: "
                + mWifiManager.isNanSupported() + "\n");
        sb.append("Device-to-device RTT Support: "
                + mWifiManager.isDeviceToDeviceRttSupported() + "\n");
        sb.append("Device-to-AP RTT Support: "
                + mWifiManager.isDeviceToApRttSupported() + "\n");
        sb.append("Preferred network offload Support: "
                + mWifiManager.isPreferredNetworkOffloadSupported() + "\n");
        sb.append("Tunnel directed link setup Support: "
                + mWifiManager.isTdlsSupported() + "\n");
        sb.append("Enhanced power reporting: "
                    + mWifiManager.isEnhancedPowerReportingSupported() + "\n");
        return sb.toString();
    }
}
