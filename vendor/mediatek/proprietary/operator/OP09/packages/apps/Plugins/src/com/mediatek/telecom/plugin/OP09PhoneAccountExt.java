package com.mediatek.telecom.plugin;

import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.telecom.ext.DefaultPhoneAccountExt;

import java.util.List;
import java.util.Objects;

/**
 * PhoneAccount extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.telecom.ext.IPhoneAccountExt")
public class OP09PhoneAccountExt extends DefaultPhoneAccountExt {

    private static final String TAG = "OP09PhoneAccountExt";

    private PhoneAccountHandle mAccountHandle;

    /**
     * Need to set Phone account removed flag for plug-in.
     */
    public void onPhoneAccountRemoved (PhoneAccountHandle accountHandle) {
        log("onPhoneAccountRemoved " + accountHandle);
        mAccountHandle = accountHandle;
    }

    /**
     * Whether or not to set User selected Phone account as outgoing by plug-in
     * Rule.
     * 
     * @param accountHandle
     *            default outgoing phone account.
     * @param accountHandleList
     *            phone account list available on device.
     * @return a account that plug in select.
     */
    public PhoneAccountHandle getExPhoneAccountAsOutgoing(PhoneAccountHandle accountHandle,
            List<PhoneAccountHandle> accountHandleList) {
        log("getExPhoneAccountAsOutgoing accountHandle " + accountHandle);
        if (null == accountHandle &&
                accountHandleList.size() >= 1) {
            log("getExPhoneAccountAsOutgoing list" + accountHandleList.get(0));
            return accountHandleList.get(0);
        }
        return accountHandle;
    }

    /**
     * should reset user selected phone account as outgoing phone account if
     * necessary.
     * 
     * @param accountHandle
     *            user selected phone account.
     * @param defaultAccountHandle
     *            user selected phone account.
     * @param accountHandleList
     *            a list of all Phone accounts.
     * @return true if need to reset outgoing phone account.
     */
    public boolean shouldResetUserSelectedOutgoingPhoneAccount(PhoneAccountHandle accountHandle,
            PhoneAccountHandle defaultAccountHandle, List<PhoneAccountHandle> accountHandleList) {
        log("mAccountHandle " + mAccountHandle + " accountHandleList " + accountHandleList.size());
        int accountsSize = accountHandleList.size();
        if (accountsSize == 3) {
            return true;
        } else if (null != mAccountHandle &&
                (accountsSize == 2) &&
                    (Objects.equals(accountHandle, defaultAccountHandle))) {
            mAccountHandle = null;
            return true;
        }
        return false;
    }

    /**
     * To set out going call account by account list size.
     * 
     * @param accountHandleList
     *            capable account list
     * @return true if need to set.
     */
    public boolean shouldSetDefaultOutgoingAccountAsPhoneAccount(List<PhoneAccountHandle> accountHandleList) {
        if (null != accountHandleList && accountHandleList.size() == 2) {
            return false;
        }
        return true;
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

