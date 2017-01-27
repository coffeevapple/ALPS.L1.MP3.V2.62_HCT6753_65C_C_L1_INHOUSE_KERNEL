package com.mediatek.wifi.sanitytest;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.app.Activity;
import android.widget.Button;
import android.widget.Switch;

import com.android.settings.Settings;
import com.jayway.android.robotium.solo.Solo;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WifiTest extends
        ActivityInstrumentationTestCase2<Settings.WifiSettingsActivity> {

    private static final String TAG = "WifiTest";

    private static final String AP_CONFIGURATION_PATH = "/data/wifi_accounts.xml";
    private static final String DEFAULT_SSID = "mtklab";
    private static final String DEFAULT_PASSWORD = "1234567890";

    private static final int TIME_OUT = 1000;
    private static final int MAX_WAIT_COUNT = 120;
    private static final int MAX_TRY_COUNT = 10;
    private static final String WIFI_SSID = "ssid";
    private static final String WIFI_PASSWORD = "password";

    private int mWifiState = WifiManager.WIFI_STATE_UNKNOWN;
    private WifiManager mWifiManager;
    private String mSSId = DEFAULT_SSID;
    private String mPassword = DEFAULT_PASSWORD;

    private boolean mIsSuccess = false;
    private boolean mIsForgetSuccess = false;
    private boolean mIsConnectSuccess = false;

    private Activity mActivity;
    private Context mContext;
    private Instrumentation mInstrumentation;
    private Solo mSolo;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                mWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                debugLog("WIFI state changed to " + mWifiState);
            }
        }
    };

    public WifiTest() {
        super("com.android.settings", Settings.WifiSettingsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mContext = mInstrumentation.getTargetContext();
        mSolo = new Solo(mInstrumentation, mActivity);

        IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, filter);

        mWifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
    }

    public void test01_setWiFiOn() throws InterruptedException {
        debugLog("test01_setWiFiOn start.");
        boolean isWifiOn = getWifiMode();
        if (isWifiOn) {
            debugLog("Disable WIFI");
            mWifiManager.setWifiEnabled(false);
        } else {
            debugLog("Enable WIFI");
            mWifiManager.setWifiEnabled(true);
        }

        int tryCount = 0;
        do {
            mSolo.sleep(TIME_OUT);
            tryCount++;
            if (isWifiOn) {
                mIsSuccess = mWifiState == WifiManager.WIFI_STATE_DISABLED;
            } else {
                mIsSuccess = mWifiState == WifiManager.WIFI_STATE_ENABLED;
            }
            debugLog("tryCount : " + tryCount + " mWifiState : " + mWifiState);
        } while (!mIsSuccess && (tryCount < MAX_TRY_COUNT));

        assertTrue("Enable WIFI, the final result is success ?  ", mIsSuccess);
        debugLog("test01_setWiFiOn end.");
    }

    public void test02_connectAp() throws InterruptedException {
        debugLog("test02_connectAp start.");
        ///M: Parse AP configuration.
        parseAPConfiguration();

        ///M: Request WIFI State ON.
        if (!requestWiFiOn()) {
            debugLog("Request WIFI on fail, please check WIFI state. ");
            return ;
        } else {
            debugLog("WIFI is on, continue");
        }
 
        ///M: Forget saved AP.
        if (!forgetSavedAP()) {
            debugLog("Forget saved AP fail, can not continue to connect the same AP. ");
            return ;
        } else {
            debugLog("Forget AP success, continue");
        }

        ///M: Connect test AP.
        if (!connectAP()) {
            debugLog("Connect commands did not send out, do not to check the connect state");
            return;
        } else {
            debugLog("Connect sned out success, continue");
        }

        ///M: Check connect state.
        checkConnectedState();
        debugLog("test02_connectAp end.");
    }

    public void test03_setWiFiOff() throws InterruptedException {
        debugLog("test03_setWiFiOff start.");
        boolean isWifiOn = getWifiMode();
        if (isWifiOn) {
            debugLog("Disable WIFI");
            mWifiManager.setWifiEnabled(false);
            int tryCount = 0;
            do {
                mSolo.sleep(TIME_OUT);
                tryCount++;
                mIsSuccess = mWifiState == WifiManager.WIFI_STATE_DISABLED;
                debugLog("tryCount : " + tryCount + " mWifiState : " + mWifiState);
            } while (!mIsSuccess && (tryCount < MAX_TRY_COUNT));

            assertTrue("Disable WIFI, the final result is success ?  ", mIsSuccess);
        } else {
            debugLog("WIFI is already off, just exit");
        }
        debugLog("test03_setWiFiOff end.");
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            mActivity.finish();
        }
        mContext.unregisterReceiver(mReceiver);
        super.tearDown();
    }

    private boolean forgetSavedAP() {
        boolean isForgeted = false;
        int savedNetworkId = getSavedNetworkId(mSSId);
        if (savedNetworkId != -1) {
            mWifiManager.forget(savedNetworkId, new WifiManager.ActionListener() {
                public void onSuccess() {
                   mIsForgetSuccess = true;
                   debugLog("Forget saved AP Success");
                }
                public void onFailure(int reason) {
                    mIsForgetSuccess = false;
                    debugLog("Forget saved AP fail, reason : " + reason);
                }
           });

           int tryCount = 0;
           do {
               mSolo.sleep(TIME_OUT);
               tryCount++;
               Log.d(TAG, "tryCount : " + tryCount + " mIsForgetSuccess : " + mIsForgetSuccess);
           } while (!mIsForgetSuccess && (tryCount < MAX_TRY_COUNT)); 
           isForgeted = mIsForgetSuccess;
           debugLog("Forget saved AP : " + mSSId + " Success : " + isForgeted);
        } else {
           isForgeted = true;
           debugLog("SSID : " + mSSId + " no need to forget.");
        }
        return isForgeted;
    }

    private boolean getWifiConnectedState(final String ssid) {
        boolean isConnected = false;
        final String SSID = "\"" + ssid + "\"";

        WifiInfo connecdInfo = mWifiManager.getConnectionInfo();
        if (connecdInfo != null && SSID.equals(connecdInfo.getSSID())) {
            debugLog("getSSID() : " + connecdInfo.getSSID());
            isConnected = true;
        }
        return isConnected;
    }

    private int getSavedNetworkId(String ssid) {
        int savedId = -1;
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if ((config.SSID).equals("\"" + ssid + "\"")) {
                    savedId = config.networkId;
                    break;
                }
            }
        }
        return savedId;
    }

    private boolean getWifiMode() {
        boolean isEnable = mWifiManager.isWifiEnabled();
        Log.d(TAG, "getWifiMode() isEnable : " + isEnable);
        return isEnable;
    }

    private void parseAPConfiguration() {
        DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dombuilder = domfac.newDocumentBuilder();
            InputStream is = new FileInputStream(AP_CONFIGURATION_PATH);
            Document doc = (Document) dombuilder.parse(is);
            Element root = (Element) doc.getDocumentElement();
            NodeList accessPoints = ((Node) root).getChildNodes();
            if (accessPoints != null) {
                debugLog("accessPoints.getLength() = " + accessPoints.getLength());
                for (int i = 0; i < accessPoints.getLength(); i++) {
                    Node accessPoint = accessPoints.item(i);
                    debugLog("i : " + i + " accessPoint : " + accessPoint);
                    if (accessPoint.getNodeType() == Node.ELEMENT_NODE) {
                        for (Node node = accessPoint.getFirstChild(); node != null; node = node.getNextSibling()) {
                            debugLog("node : " + node);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                if (node.getNodeName().equals(WIFI_SSID)) {
                                    mSSId = node.getFirstChild().getNodeValue();
                                } else if (node.getNodeName().equals(WIFI_PASSWORD)) {
                                    mPassword = node.getFirstChild().getNodeValue();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean requestWiFiOn() {
        debugLog("requestWiFiOn()");
        boolean isStatusReady = false;
        boolean isWifiOn = getWifiMode();
        if (!isWifiOn) {
            debugLog("Enable WIFI");
            mWifiManager.setWifiEnabled(true);

            int tryCount = 0;
            do {
                mSolo.sleep(TIME_OUT);
                tryCount++;
                mIsSuccess = mWifiState == WifiManager.WIFI_STATE_ENABLED;
                debugLog("tryCount : " + tryCount + " mWifiState : " + mWifiState);
            } while (!mIsSuccess && (tryCount < MAX_TRY_COUNT));
            isStatusReady = mIsSuccess;
        } else {
            debugLog("WIFI  is already on");
            isStatusReady = true;
        }
        return isStatusReady;
    }   

    private void scrollToTop() {
        while (mSolo.scrollUp()) {
            mSolo.sleep(TIME_OUT);
            mSolo.scrollUp();
        }
    }

    private boolean connectAP() {
        ///M: Scroll to AP top.
        scrollToTop();

        ///M: Chose AP to connect.
        boolean isAPSearched = mSolo.searchText(mSSId);
        boolean isSendOut = false;
        if (isAPSearched) {
            mSolo.clickOnText(mSSId);
            mSolo.sleep(TIME_OUT * 2);
            debugLog("Click AP name : " + mSSId);
            if (isRightView()) {
                mSolo.enterText(0, mPassword);
                mSolo.sleep(TIME_OUT * 2);
                Button connButton = mSolo.getButton(3);
                if (connButton != null) {
                    mSolo.clickOnView(connButton);
                    mSolo.sleep(TIME_OUT * 2);
                    isSendOut = true;
                    debugLog("Confirm connect AP : " + mSSId);
                }
            }
        }
        return isSendOut;
    }
    
    private void checkConnectedState() {
        boolean isConnected = false;
        int tryCount = 0;
        do {
            mSolo.sleep(TIME_OUT);
            tryCount++;
            isConnected = getWifiConnectedState(mSSId);
            debugLog("tryCount : " + tryCount + " isConnected : " + isConnected);
        } while (!isConnected && tryCount < MAX_WAIT_COUNT);
        assertEquals(isConnected, true);
    }

    private boolean isRightView() {
        boolean isFound = false;
        int tryCount = 0;
        do {
            if (mSolo.searchButton(mActivity.getString(android.R.string.cancel)) && mSolo.searchText(mSSId)) {
                Log.d(TAG, "isRightView = true");
                return true;
            } else if (mSolo.searchButton(mActivity.getString(android.R.string.cancel))) {
                mSolo.clickOnButton(mActivity.getString(android.R.string.cancel));
                mSolo.sleep(TIME_OUT);

                ///Retry to click
                scrollToTop();
                if (mSolo.searchText(mSSId)) {
                    mSolo.clickOnText(mSSId);
                    mSolo.sleep(TIME_OUT);
                }
            }
            tryCount++;
            debugLog("tryCount : " + tryCount + " isFound : " + isFound);
        } while (!isFound && tryCount < MAX_TRY_COUNT);

        return isFound;
    }

    private void debugLog(String msg) {
        Log.d(TAG, msg);
    }
}
