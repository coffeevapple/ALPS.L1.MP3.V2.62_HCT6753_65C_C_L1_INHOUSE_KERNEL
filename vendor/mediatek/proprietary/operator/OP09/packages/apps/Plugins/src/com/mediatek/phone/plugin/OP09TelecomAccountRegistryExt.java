package com.mediatek.phone.plugin;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.phone.ext.DefaultTelecomAccountRegistryExt;

/**
 * TelecomAccountRegistry extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.phone.ext.ITelecomAccountRegistryExt")
public class OP09TelecomAccountRegistryExt extends DefaultTelecomAccountRegistryExt {

    private static final String TAG = "OP09TelecomAccountRegistryExt";

    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    /**
     * Called when need to registry phone.
     *
     * @param subId indicator regitry phone.
     */
    public void setPhoneAccountSubId(int subId) {
        log("setPhoneAccountSubId" + subId);
        mSubId = subId;
    }

    /**
     * Called when need to registry phone account icon.
     *
     * @param resId need update.
     * @return res ID when index is valide.
     */
    public int getPhoneAccountIcon(int resId) {
        log("getPhoneAccountIcon mSubId : " + mSubId);
        if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
            /*SubscriptionInfo record = SubscriptionManager.getSubscriptionInfo(mSubId);
            if (record != null && (record.simIconRes.length == 4)) {
                if (record.simIconRes[3] > 0) {
                    log("record.simIconRes[3]" + record.simIconRes[3]);
                    return record.simIconRes[3];
                }
            }*/
        }
        return resId;
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}

