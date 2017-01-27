package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.provider.Settings;
import com.android.internal.telephony.ISms;
import android.os.ServiceManager;

/* temp close for build , as com.mediatek.telephone can not ready

import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
*/
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.os.RemoteException;

import com.mediatek.xlog.Xlog;
import java.util.List;


public class Op01MmsUtils {
    private static final String TAG = "Op01MmsUtils";
    private static final String TEXT_SIZE = "message_font_size";
    private static final float DEFAULT_TEXT_SIZE = 18;
    private static final float MIN_TEXT_SIZE = 10;
    private static final float MAX_TEXT_SIZE = 32;
    private static final String MMS_APP_PACKAGE = "com.android.mms";


    public static float getTextSize(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        float size = sp.getFloat(TEXT_SIZE, DEFAULT_TEXT_SIZE);
        Xlog.v(TAG, "getTextSize = " + size);
        if (size < MIN_TEXT_SIZE) {
            size = MIN_TEXT_SIZE;
        } else if (size > MAX_TEXT_SIZE) {
            size = MAX_TEXT_SIZE;
        }
        return size;
    }

    public static void setTextSize(Context context, float size) {
        float textSize;

        Xlog.v(TAG, "setTextSize = " + size);

        if (size < MIN_TEXT_SIZE) {
            textSize = MIN_TEXT_SIZE;
        } else if (size > MAX_TEXT_SIZE) {
            textSize = MAX_TEXT_SIZE;
        } else {
            textSize = size;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(TEXT_SIZE, textSize);
        editor.commit();
    }

    public static boolean isSmsEnabled(Context context) {

        String defaultSmsApplication = Telephony.Sms.getDefaultSmsPackage(context);
        if (defaultSmsApplication != null && defaultSmsApplication.equals(MMS_APP_PACKAGE)) {
            return true;
        }
        return false;
    }

    public static boolean isSimInserted(Context context) {

        /* temp close for build , as com.mediatek.telephone can not ready
        List<SimInfoRecord> listSimInfo = SimInfoManager.getInsertedSimInfoList(context);
        if (listSimInfo == null || listSimInfo.isEmpty()) {
            return false;
        } else {
            return true;
        }*/
        List<SubscriptionInfo> listSimInfo = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (listSimInfo == null || listSimInfo.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isAirplaneOn(Context context) {
        boolean airplaneOn = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
        if (airplaneOn) {
            Xlog.d(TAG, "airplane is On");
            return true;
        }
        return false;
    }
    
    public static boolean isSmsReady(Context context) {
        Xlog.d(TAG, "isSmsReady");
        ISms smsManager = ISms.Stub.asInterface(ServiceManager.getService("isms"));
        if (smsManager == null) {
            Xlog.d(TAG, "smsManager is null");
            return false;
        }
        
        boolean smsReady = false;
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();

        for (SubscriptionInfo subInfoRecord : subInfoList) {
            try {
                Xlog.d(TAG, "subId=" + subInfoRecord.getSubscriptionId());
                smsReady = smsManager.isSmsReadyForSubscriber(subInfoRecord.getSubscriptionId());
                if (smsReady) {
                    break;
                }
            } catch (RemoteException e) {
                Xlog.d(TAG, "isSmsReady failed to get sms state for sub "
                        + subInfoRecord.getSubscriptionId());
                smsReady = false;
            }
        }

        Xlog.d(TAG, "smsReady" + smsReady);
        return smsReady;
    }
}
