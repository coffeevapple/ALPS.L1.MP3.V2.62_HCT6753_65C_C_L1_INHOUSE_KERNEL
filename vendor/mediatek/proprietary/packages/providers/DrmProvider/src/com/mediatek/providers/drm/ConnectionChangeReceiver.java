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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.mediatek.drm.OmaDrmClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

// when connection is available, sync secure timer
public class ConnectionChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "DRM/ConnectionChangeReceiver";
    private static final String INVALID_DEVICE_ID = "000000000000000";

    @Override
    public void onReceive(
            final Context context, Intent intent) {
        Log.d(TAG, "onReceive : CONNECTIVITY_CHANGE received.");
        if (OmaDrmClient.isOmaDrmEnabled()) {
            OmaDrmClient client = new OmaDrmClient(context);

            // first we check if the secure timer is valid or not
            boolean isValid = OmaDrmHelper.checkClock(client);

            // M: @{
            // ALPS00772785, call release() function, or else cannot find an
            // unique ID in DrmManager.cpp
            client.release();
            client = null;
            // M: @}

            if (isValid) {
                Log.d(TAG, "ConnectionChangeReceiver : Secure timer is already valid");
                return;
            } else {
                if (OmaDrmHelper.isTestIccCard()) {
                    Log.d(TAG,
                            "ConnectionChangeReceiver : It is now test sim state, not sync time.");
                    return;
                }
            }

            // Check if network is available, if not, then do nothing
            if (!OmaDrmHelper.isNetWorkAvailable(context)) {
                Log.w(TAG, "Network is not available, so do not send ntp package");
                return;
            }

            // launch the thread to do SNTP
            if (null != context) {
                Log.d(TAG, "start service from connectionChangeRecevier");
               // ConnectionChangeReceiver.this.launchSNTP(context);
                context.startService(new Intent(context, DrmSyncTimeService.class));
            }
        }
    }

}
