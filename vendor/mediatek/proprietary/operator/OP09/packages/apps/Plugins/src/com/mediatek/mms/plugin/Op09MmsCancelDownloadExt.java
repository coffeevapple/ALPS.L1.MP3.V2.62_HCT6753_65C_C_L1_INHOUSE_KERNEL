/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.plugin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.telephony.SmsManager;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultMmsCancelDownloadExt;
import com.mediatek.mms.ext.IMmsCancelDownloadHost;
import com.mediatek.xlog.Xlog;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

import java.util.HashMap;

/**
 * M: Plugin implemention for OP09 mms cancel download.
 */
@PluginImpl(interfaceName = "com.mediatek.mms.ext.IMmsCancelDownloadExt")
public class Op09MmsCancelDownloadExt extends DefaultMmsCancelDownloadExt {
    private static final String TAG = "Mms/Op09MmsCancelDownloadExt";
    private static final String STATUS_EXT = "st_ext";

    private Context mContext;
    private HashMap<String, Uri> mClientMap;
    private boolean mEnableCancelToast;
    private boolean mWaitingCnxn;
    private DefaultHttpRequestRetryHandler mHttpRetryHandler;

    /**
     * M: Constructor.
     * @param context the Context.
     */
    public Op09MmsCancelDownloadExt(Context context) {
        super(context);
        mContext = context;
        mClientMap = new HashMap<String, Uri>();
        mEnableCancelToast = false;
        mWaitingCnxn = false;
    }

    @Override
    public void addHttpClient(String url, Uri client) {
        Xlog.d(TAG, "setHttpClient(): url = " + url);

        mClientMap.put(url, client);
    }

    @Override
    public void cancelDownload(final Uri uri) {
        Xlog.d(TAG, "MmsCancelDownloadExt: cancelDownload()");
        if (uri == null) {
            Xlog.d(TAG, "cancelDownload(): uri is null!");
            return;
        }

        // Update the download status
        markStateExt(uri, STATE_CANCELLING);

        Thread thread = new Thread(new Runnable() {
            String mContentUrl = null;

            @Override
            public void run() {
                mContentUrl = getContentLocation(uri);

                if (!mClientMap.containsKey(mContentUrl)) {
                    setCancelDownloadState(uri, true);
                } else {
                    abortMmsHttp(mContentUrl, uri);
                }
            }
        });

        thread.start();
    }

    @Override
    public void removeHttpClient(String url) {
        Xlog.d(TAG, "removeHttpClient(): url = " + url);

        mClientMap.remove(url);
    }

    @Override
    public void setCancelToastEnabled(boolean isEnable) {
        Xlog.d(TAG, "setCancelEnabled(): mEnableCancelToast = " + isEnable);
        mEnableCancelToast = isEnable;
    }

    @Override
    public boolean getCancelToastEnabled() {
        Xlog.d(TAG, "getCancelEnabled(): mEnableCancelToast = " + mEnableCancelToast);
        return mEnableCancelToast;
    }

    @Override
    public void markStateExt(Uri uri, int state) {
        Xlog.d(TAG, "markStateExt: state = " + state + " uri = " + uri);

        // Use the STATUS field to store the state of downloading process
        ContentValues values = new ContentValues(1);
        values.put(STATUS_EXT, state);
        SqliteWrapper.update(mContext, mContext.getContentResolver(),
                    uri, values, null, null);
    }

    @Override
    public int getStateExt(Uri uri) {
        Xlog.d(TAG, "getStateExt: uri = " + uri);
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                            uri, new String[] {STATUS_EXT}, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return STATE_UNKNOWN;
    }

    @Override
    public int getStateExt(String url) {
        Xlog.d(TAG, "getStateExt: url = " + url);

        String where = Mms.CONTENT_LOCATION + " = ?";
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                Mms.CONTENT_URI, new String[] {STATUS_EXT}, where, new String[] {url}, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return STATE_UNKNOWN;
    }

    @Override
    public void setWaitingDataCnxn(boolean isWaiting) {
        Xlog.d(TAG, "setWaitingDataCnxn(): mWaitingCnxn = " + isWaiting);
        mWaitingCnxn = isWaiting;
    }

    @Override
    public boolean getWaitingDataCnxn() {
        Xlog.d(TAG, "getWaitingDataCnxn(): mWaitingCnxn = " + mWaitingCnxn);
        return mWaitingCnxn;
    }

    @Override
    public void saveDefaultHttpRetryHandler(DefaultHttpRequestRetryHandler retryHandler) {
        Xlog.d(TAG, "saveDefaultHttpRetryHandler(): retryHandler = " + retryHandler);
        mHttpRetryHandler = retryHandler;
    }

    /**
     * M: set mms cancel dowanload state.
     * @param uri the mms uri.
     * @param isCancelling true: is in cancelling; false: not.
     */
    private void setCancelDownloadState(Uri uri, boolean isCancelling) {
        Xlog.d(TAG, "setCancelDownloadState()...");
        IMmsCancelDownloadHost mcdh = this.getHost();
        if (mcdh != null) {
            mcdh.setCancelDownloadState(uri, isCancelling);
        }
    }

    /**
     * M: get contentLocation for mms.
     * @param uri the mms uri.
     * @return the mms contentLocaion.
     */
    private String getContentLocation(final Uri uri) {
        String contentUrl = null;

        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
            uri, new String[]{Mms.CONTENT_LOCATION}, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    contentUrl = cursor.getString(0);
                    Xlog.d(TAG, "getContentLocation(): contentUrl = " + contentUrl);
                }
            } finally {
                cursor.close();
            }
        }

        return contentUrl;
    }

    /**
     * M: abort mms http connection.
     * @param contentUrl the mms contentUrl.
     * @param uri the mms uri.
     */
    private void abortMmsHttp(String contentUrl, Uri uri) {
        Xlog.d(TAG, "[abortMmsHttp], contentUrl:" + contentUrl + " uri:" + uri);
        mClientMap.remove(contentUrl);
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), uri,
            new String[] {Mms.SUBSCRIPTION_ID}, null, null, null);
        try {
            if (cursor != null) {
                if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                    int subId = cursor.getInt(0);
                    SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(subId);
                    uri = uri.buildUpon().appendQueryParameter("cancel", "1").build();
                    manager.downloadMultimediaMessage(mContext, contentUrl, uri, null, null);
                }
            }
        } catch (SQLiteException e) {
            Xlog.e(TAG, "[abortMmsHttp] failed, as " + e.getMessage());
        }

    }

}
