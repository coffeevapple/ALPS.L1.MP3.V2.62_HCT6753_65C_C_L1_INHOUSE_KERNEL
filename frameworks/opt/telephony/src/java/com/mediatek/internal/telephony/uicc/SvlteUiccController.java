/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.mediatek.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;

/**
 * Provide SVLTE UICC card/application related flow control.
 */

public class SvlteUiccController extends Handler {

    private static final boolean DBG = true;
    private static final String LOG_TAG = "SvlteUiccController";

    private static final int EVENT_C2K_WP_CARD_TYPE_READY = 1;
    private static final int EVENT_ICC_CHANGED = 2;

    private static final int INVALID_APP_TYPE = -1;
    private static final int OPO9_SOLT_ID = 0;

    private UiccController mUiccController;
    private boolean mIsSwitchedPinApp = false;
    private int mLastAppType = UiccController.APP_FAM_3GPP;

    /**
     * To make sure SvlteUiccController single instance is created.
     *
     * @return SvlteUiccController instance
     */
    public static SvlteUiccController make() {
        return getInstance();
    }

    /**
     * Singleton to get SvlteUiccController instance.
     *
     * @return SvlteUiccController instance
     */
    public static synchronized SvlteUiccController getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private SvlteUiccController() {
        logd("Constructing");
        mUiccController = UiccController.getInstance();
        mUiccController.registerForC2KWPCardTypeReady(this, EVENT_C2K_WP_CARD_TYPE_READY, null);
        mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
    }

    /**
     * SvlteUiccController clear up.
     *
     */
    public void dispose() {
        logd("Disposing");
        //Cleanup ICC references
        mUiccController.unregisterForC2KWPCardTypeReady(this);
        mUiccController.unregisterForIccChanged(this);
        mUiccController = null;
    }

    @Override
    public void handleMessage(Message msg) {
        logd("receive message " + msg.what);
        AsyncResult ar = null;

        switch (msg.what) {
            case EVENT_C2K_WP_CARD_TYPE_READY:
                logd("handleMessage (EVENT_C2K_WP_CARD_TYPE_READY).");
                onCardTypeReady();
                break;
            case EVENT_ICC_CHANGED:
                ar = (AsyncResult) msg.obj;
                int index = 0;
                if (ar != null && ar.result instanceof Integer) {
                    index = ((Integer) ar.result).intValue();
                    logd("handleMessage (EVENT_ICC_CHANGED) , index = " + index);
                } else {
                    logd("handleMessage (EVENT_ICC_CHANGED), come from myself");
                }
                // SVLTE
                if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()
                        && UiccController.INDEX_SVLTE == index) {
                    index = SvlteModeController.getCdmaSocketSlotId();
                }
                int appType = getActivePhoneAppType(index);
                if (INVALID_APP_TYPE != appType) {
                    onIccCardStatusChange(index, appType);
                }
                break;
            default:
                loge("Unhandled message with number: " + msg.what);
                break;
        }
    }

    private void onCardTypeReady() {
        do {
            if (isUsimTestSim()) {
                doIccAppTypeSwitch(OPO9_SOLT_ID, ServiceState.RIL_RADIO_TECHNOLOGY_GSM);
                if (DBG) {
                    logd("OP09 Switch gsm radio technology for usim in slot 0");
                }
                break;
            }

            doRemoteSimAppSwitchCheck();
        } while (false);

    }

    private void onIccCardStatusChange(int slotId, int appType) {
        if (DBG) {
            logd("slotId: " + slotId + ", appType: " + appType
                    + ", mIsSwitchedPinApp: " + mIsSwitchedPinApp
                    + ", mLastAppType: " + mLastAppType);
        }
        if (isPinState(slotId, appType)) {
            if ((!mIsSwitchedPinApp) && isNeedToAdjustPinAppType(slotId, appType)) {
                mLastAppType = appType;
                doIccAppTypeSwitch(slotId, ServiceState.RIL_RADIO_TECHNOLOGY_GSM);
                mIsSwitchedPinApp = true;
                if (DBG) {
                    logd("SVLTE Remote SIM PIN GSM switched.");
                }
            }
        } else {
            if (mIsSwitchedPinApp) {
                doIccAppTypeSwitch(slotId, mLastAppType);
                mIsSwitchedPinApp = false;
                if (DBG) {
                    logd("SVLTE Remote SIM PIN switched back.");
                }
            }
        }
    }

    private void doIccAppTypeSwitch(int phoneId, int radioTech) {
        IccCardProxy iccCard = (IccCardProxy) PhoneFactory.getPhone(phoneId).getIccCard();
        iccCard.setVoiceRadioTech(radioTech);
    }

    private boolean isUsimTestSim() {
        return (CdmaFeatureOptionUtils.isCdmaLteDcSupport()
                && (isOP09())
                && (SvlteUiccUtils.getInstance().isUsimOnly(OPO9_SOLT_ID)));
    }

    private boolean isOP09() {
        return SystemProperties.get("ro.operator.optr", "OM").equals("OP09");
    }

    private boolean isNeedToAdjustPinAppType(int slotId, int appType) {
        return (CdmaFeatureOptionUtils.isCdmaLteDcSupport()
                && (SvlteUiccUtils.getInstance().isUsimWithCsim(slotId))
                && (appType != getRemoteSimPinAppType(slotId, appType)));
    }

    private int getActivePhoneAppType(int phoneId) {
        PhoneProxy phone = (PhoneProxy) PhoneFactory.getPhone(phoneId);
        if (null != phone) {
            return (PhoneConstants.PHONE_TYPE_GSM == phone.getActivePhone().getPhoneType()) ?
                UiccController.APP_FAM_3GPP : UiccController.APP_FAM_3GPP2;
        } else {
            if (DBG) {
                loge("Could not get valid phone instance.");
            }
            return INVALID_APP_TYPE;
        }
    }

    private int getRemoteSimPinAppType(int slotId, int appType) {
        if (SvlteUiccUtils.getInstance().isUsimSim(slotId)) {
            return UiccController.APP_FAM_3GPP;
        } else if (SvlteUiccUtils.getInstance().isRuimCsim(slotId)) {
            return UiccController.APP_FAM_3GPP2;
        } else {
            return appType;
        }
    }

    private boolean isPinState(int slotId, int appType) {
        UiccCard newCard = mUiccController.getUiccCard(slotId);
        UiccCardApplication newApp = null;
        if (newCard != null) {
            newApp = newCard.getApplication(appType);
        }
        return ((null != newApp)
                && (AppState.APPSTATE_PIN == newApp.getState()
                || AppState.APPSTATE_PUK == newApp.getState()));
    }

    /* ALPS02148729, For GSM+CDMA card, need switch SIM APP for SIM change PIN {*/
    private void doRemoteSimAppSwitchCheck() {
        int soltCount = TelephonyManager.getDefault().getPhoneCount();
        for (int slotId = 0; slotId < soltCount; slotId++) {
            if (isNeedSwitchRemoteSimApp(slotId)) {
                if (DBG) {
                    logd("Remote SIM, IccCard switch to 3GPP APP.");
                }
                doIccAppTypeSwitch(slotId, ServiceState.RIL_RADIO_TECHNOLOGY_GSM);
                break;
            }
        }
    }

    private boolean isNeedSwitchRemoteSimApp(int slotId) {
        return (CdmaFeatureOptionUtils.isCdmaLteDcSupport()
                && (isNon3GPP(slotId))
                && (SvlteUiccUtils.getInstance().isUsimWithCsim(slotId)));
    }

    private boolean isNon3GPP(int phoneId) {
        return (UiccController.APP_FAM_3GPP != getActivePhoneAppType(phoneId));
    }
    /* ALPS02148729, For GSM+CDMA card, need switch SIM APP for SIM change PIN }*/

    /**
     * Create SvlteUiccApplicationUpdateStrategy instance.
     *
     * @hide
     */
    private static class SingletonHolder {
        public static final SvlteUiccController INSTANCE =
                new SvlteUiccController();
    }


    /**
     * Log level.
     *
     * @hide
     */
    private void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

}
