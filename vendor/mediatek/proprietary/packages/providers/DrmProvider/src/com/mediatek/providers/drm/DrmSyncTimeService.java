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

package com.mediatek.providers.drm;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mediatek.drm.OmaDrmClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * using service to sync time, avoid low memory kill.
 */
public class DrmSyncTimeService extends Service {
    private static final String TAG = "DRM/DrmSyncTimeService";
    private DrmSyncTimeServiceHandler mDrmSyncTimeHandler = null;
    private static final String INVALID_DEVICE_ID = "000000000000000";
    private static final int MSGID_SYNC_TIME = 1;
    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mContext = getApplicationContext();
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Builder builder = new Notification.Builder(mContext);
        Notification notification = builder.getNotification();
        // use 0 does not show notification
        startForeground(0, notification);

        HandlerThread handlerThread = new HandlerThread("DrmSyncTimeServiceThread");
        handlerThread.start();
        mDrmSyncTimeHandler = new DrmSyncTimeServiceHandler(handlerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopForeground(true);
    }

    /*public void onStart(Intent intent, int startId) {
        Log.d(TAG, "onStart");
        syncTimeToServerAsync() ;
        //stopSelf();
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand intent: " + intent + " startId: " + startId);
        int iRet = super.onStartCommand(intent, flags, startId);
        syncTimeToServerAsync() ;
        Log.d(TAG, "onStartCommand: " + iRet);
        return START_STICKY;
    }

    /**
     * method to send sync time command.
     */
    public synchronized void syncTimeToServerAsync() {
        Log.d(TAG, "syncTimeToServerAsync");
        OmaDrmClient client = new OmaDrmClient(mContext);
        boolean isValid = OmaDrmHelper.checkClock(client);
        client.release();
        if (isValid) {
            Log.d(TAG, "syncTimeToServerAsync : Secure timer is already valid, stop service");
            stopSelf();
            return;
        } else {
            if (OmaDrmHelper.isTestIccCard()) {
                Log.d(TAG, "syncTimeToServerAsync : It is now test sim state, not sync time.");
                return;
            }
        }

        // Check if network is available, if not, then do nothing.
        if (!OmaDrmHelper.isNetWorkAvailable(mContext)) {
            Log.w(TAG,
               "syncTimeToServerAsync: Network is not available, so do not send ntp package");
            return;
        }

        final int bundleSize = 1;
        mDrmSyncTimeHandler.removeMessages(MSGID_SYNC_TIME);
        mDrmSyncTimeHandler.sendEmptyMessage(MSGID_SYNC_TIME);
    }

    /**
     * method to call sync time.
     */
    private void launchSNTP(
            Context context) {
        Log.d(TAG, "SNTP : the thread is not running.");
        OmaDrmClient client = new OmaDrmClient(context);

        // firstly we validate the device id
        String id = OmaDrmHelper.loadDeviceId(client);
        Log.d(TAG, "SNTP : load device id: " + id);

        // get an empty device id: the device id was not saved yet
        if (id == null || id.isEmpty() || id.equals(INVALID_DEVICE_ID)) {
            Log.d(TAG, "SNTP : The device id is empty, try obtain it");
            id = BootCompletedReceiver.deviceId(context);
            Log.d(TAG, "SNTP : Obtained device id: " + id);

            // anyway, we need to save the device id (may be invalid value)
            // so that the secure timer can be saved
            int res = OmaDrmHelper.saveDeviceId(client, id);
        }

        // we already have a device id: no matter if it's empty or not
        if (id.equals(INVALID_DEVICE_ID)) {
            Log.w(TAG, "SNTP : The device id is an invalid value, but we continue processing.");
        }

        // launch SNTP
        if (null != context) {
            Log.d(TAG, "SNTP : launch the thread.");
            launchSimpleThread(context, client);
        }

        // after connect to server and sync time, check sync result of drm clock
        boolean isValid = OmaDrmHelper.checkClock(client);
        // M: @{
        // ALPS00772785, call release() function, or else cannot find an
        // unique ID in DrmManager.cpp
        client.release();
        Log.w(TAG, "SNTP : sync time done. Drm Clock valid state = " + isValid);

        if (isValid) {
            Log.d(TAG, "SNTP : sync time done. stop service");
            mDrmSyncTimeHandler.removeCallbacksAndMessages(null);
            mDrmSyncTimeHandler.getLooper().quit();
            stopSelf();
        }
    }
    /**
     * method to sync time.
     */
    private void launchSimpleThread(
            final Context context, final OmaDrmClient client) {
        // check if the network is usable
        final ConnectivityManager conManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == conManager) {
            Log.e(TAG, "SNTP : invalid connectivity manager.");
            return;
        }

        NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
        if (null == networkInfo) {
            Log.e(TAG, "SNTP : invalid active network info.");
            return;
        }
        if (!networkInfo.isAvailable()) {
            Log.e(TAG, "SNTP : unavailable active network.");
            return;
        }

        final int type = networkInfo.getType();
        Log.d(TAG, "SNTP : active network type: " + type);

        int startIndex = 0;
        while (startIndex < sHostList.length) {
            if(OmaDrmHelper.isTestIccCard()) {
                Log.w(TAG, "SNTP: synchronization terminated, since it's a test case");
                break;
            }
            int result = checkRouteToHost(conManager, type, startIndex);
            if (-1 != result) {
                int oft = Ntp.sync(sHostList[result]);
                if (oft != Ntp.INVALID_OFFSET) {
                    Log.d(TAG, "SNTP: synchronization result, utc time offset: " + oft);
                    result = OmaDrmHelper.updateClock(client, oft);

                    // check secure time is synchronized with phone and
                    // server
                    int sentSec = (int) (Ntp.getSentTime() / 1000);
                    String secTime = OmaDrmHelper.getSecureTimeInSeconds(client);
                    if ("invalid".equals(secTime) || "invalid-need-synchronization".equals(secTime)
                            || "".equals(secTime)) {
                        Log.d(TAG, "clock is invalid, continue to sync with server.");
                        startIndex++;
                        continue;
                    } else {
                        int iSecTime = Integer.valueOf(secTime);
                        Log.d(TAG, "send sync time in seconds is:" + sentSec);
                        if (Math.abs(sentSec + oft - iSecTime) > 60) {
                            Log.d(TAG, "time&clock invalid, continue to sync with server.");
                            OmaDrmHelper.updateClock(client, 0x7fffffff);
                            continue;
                        }
                    }
                    break;
                } else {
                    startIndex = result + 1;
                    continue;
                }
            } else {
                startIndex++;
                continue;
            }
        }
    }

    // modify these SNTP host servers, for different countries.
    private static String[] sHostList = new String[]{
        "hshh.org",
        "t1.hshh.org",
        "t2.hshh.org",
        "t3.hshh.org",
        "clock.via.net",
        "pool.ntp.org",
        "asia.pool.ntp.org",
        "europe.pool.ntp.org",
        "north-america.pool.ntp.org",
        "oceania.pool.ntp.org",
        "south-america.pool.ntp.org"};

    /**
     * method to check host is ok or not.
     */
    private int checkRouteToHost(
            ConnectivityManager conManager, int type, int startIndex) {
        Log.v(TAG, "==== check if there's available route to SNTP servers ====");

        int result = -1;
        if (conManager != null) {
            int size = sHostList.length;
            for (int i = startIndex; i < size; i++) {
                int address = 0;
                try {
                    Log.d(TAG, "get host address by name: [" + sHostList[i] + "].");
                    InetAddress addr = InetAddress.getByName(sHostList[i]);
                    address = ipToInt(addr.getHostAddress());
                } catch (UnknownHostException e) {
                    Log.e(TAG, "caught UnknownHostException");
                    continue;
                }

                Log.d(TAG, "request route for host: [" + sHostList[i] + "].");
                if (conManager.requestRouteToHost(type, address)) {
                    Log.d(TAG, "request route for host success.");
                    result = i;
                    break;
                }
                Log.d(TAG, "request route for host failed.");
            }
        }
        return result;
    }

    /**
     * method to translate address.
     */
    private int ipToInt(
            String ipAddress) {
        if (ipAddress == null) {
            return -1;
        }

        String[] addrArray = ipAddress.split("\\.");
        int size = addrArray.length;
        if (size != 4) {
            return -1;
        }

        int[] addrBytes = new int[size];
        try {
            for (int i = 0; i < size; i++) {
                addrBytes[i] = Integer.parseInt(addrArray[i]);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }

        Log.v(TAG, "ipToInt: a[0] = " + addrBytes[0] + ", a[1] = " + addrBytes[1] + ", a[2] = "
                + addrBytes[2] + ", a[3] = " + addrBytes[3]);
        return ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8) | (addrBytes[0] & 0xff);
    }

    /**
     * use handler thread to avoid multi thread.
     */
    class DrmSyncTimeServiceHandler extends Handler {
        public DrmSyncTimeServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

            // synctime
            case MSGID_SYNC_TIME:
                launchSNTP(mContext);
                break;

            default:
                break;
            }
        }
    }
}

