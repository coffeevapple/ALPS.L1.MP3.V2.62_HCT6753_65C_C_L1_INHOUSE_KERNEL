package com.mediatek.settings.cdma;

import android.telephony.SubscriptionManager;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.uicc.UiccController;
import com.android.phone.PhoneGlobals;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.IMobileNetworkSettingsExt;
import com.mediatek.telephony.TelephonyManagerEx;

public class TelephonyUtilsEx {

    private static final String TAG = "TelephonyUtilsEx";

    public static final int CT_SIM = 5;
    public static final int CT4G_SIM = 4;
    public static final int CT3G_SIM = 3;
    public static final int GSM_SIM = 2;
    public static final int ERROR_SIM = -1;
    private static final int MODE_PHONE1_ONLY = 1;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {
        "gsm.ril.fulluicctype",
        "gsm.ril.fulluicctype.2",
        "gsm.ril.fulluicctype.3",
        "gsm.ril.fulluicctype.4",
    };
    /**
     * @param subId sub id
     * @return true if is cdma phone
     */
    public static boolean isCDMAPhone(Phone phone) {
        int phoneType = phone.getPhoneType();
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            Log.d(TAG, "isCDMAPhone = true");

            return true;
        }

        CardType cardType = UiccController.getInstance().getCardType();
        boolean isCTCard = cardType.isOPO9Card();
        int phoneSlotId = SubscriptionManager.getSlotId(phone.getSubId());
        int c2kSlotId = SvlteModeController.getActiveSvlteModeSlotId();

        Log.d(TAG, "isCDMAPhone cardType = " + cardType.toString()
                + " isCTCard = " + isCTCard
                + " phone slotId = " + phoneSlotId
                + " c2k slot = " + c2kSlotId);

        if (isCTCard && phoneSlotId == c2kSlotId) {
            boolean isRoaming = isSvlteRoaming(phone);
            Log.d(TAG, "isRoaming: " + isRoaming);

            return !isRoaming;
        } else {
            return false;
        }
    }

    public static boolean isLTE(Phone phone) {
        CardType cardType = UiccController.getInstance().getCardType();
        Log.i(TAG, "isLTE cardType = " + cardType.toString());
        boolean isCTCard = cardType.isOPO9Card();
        Log.i(TAG, "isLTE isCTCard = " + isCTCard);

        boolean isLTE = cardType.is4GCard();
        Log.i(TAG, "isLTE isCTCard = " + isCTCard);
        int slotId = SubscriptionManager.getSlotId(phone.getSubId());
        Log.i(TAG, "isLTE slotId = " + slotId);

        return isCTCard && isLTE && SvlteModeController.getActiveSvlteModeSlotId() == slotId;
    }

    /**
     * get getFullIccCardTypeExt type .
     * @param
     * @return sim string type
     */
    public static String getFullIccCardTypeExt(int slotId) {
        String cardType = (slotId >= 0 && slotId < PROPERTY_RIL_FULL_UICC_TYPE.length)
                ? SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotId]) : "";

        Log.d(TAG, "getFullIccCardTypeExt cardType = " + cardType + " slotId = " + slotId);
        return cardType;
    }

    /**
     * get sim type .
     * @param
     * @return sim type
     */
    public static int getSimType(int slotId) {
        String fullUiccType = getFullIccCardTypeExt(slotId);

        if (fullUiccType != null) {
            if (fullUiccType.contains("CSIM") || fullUiccType.contains("RUIM")) {

                if (fullUiccType.contains("CSIM") || fullUiccType.contains("USIM")) {
                    return CT4G_SIM;
                } else if (fullUiccType.contains("SIM")) {
                    return CT3G_SIM;
                }
                return CT_SIM;
            } else if (fullUiccType.contains("SIM") || fullUiccType.contains("USIM")) {
                Log.d(TAG, "getSimType is GSM sim");
                return GSM_SIM;
            } else {
                Log.d(TAG, "getSimType not GSM, CT34G");
                return ERROR_SIM;
            }
        }
        Log.d(TAG, "getSimType fullUiccType null");
        return ERROR_SIM;
    }

   public static boolean isCTCardType(int slotId){
       boolean isCTCardType = (getSimType(slotId) > GSM_SIM);
       Log.i(TAG, "isCTCardType = " + isCTCardType);
       return isCTCardType;
   }

   public static boolean isCTLTECardType(int slotId){
       boolean isCTLTECardType = (getSimType(slotId) == CT4G_SIM);
       Log.i(TAG, "isCTLTECardType = " + isCTLTECardType);
       return isCTLTECardType;
   }

   public static boolean isCDMACardType(int slotId){
       boolean isCDMACardType = (getSimType(slotId) == CT3G_SIM);
       Log.i(TAG, "isCDMACardType = " + isCDMACardType);
       return isCDMACardType;
   }


   /**
     * Check if it is support tdd data only check.
     * @return true if it is support tdd data only check.
     */
    public static boolean isSupportTddDataOnlyCheck() {
        //boolean isCT4gCard = (IccCardConstants.CardType.CT_4G_UICC_CARD
        //    == UiccController.getInstance().getCardType());
        boolean isCT4gCard = isCTLTECardType(SvlteModeController.getActiveSvlteModeSlotId());
        boolean isCdmaLteDcSupport = CdmaFeatureOptionUtils.isCdmaLteDcSupport();
        boolean checkResult = false;

        if (isCT4gCard && isCdmaLteDcSupport) {
           checkResult = true;
        }
        Log.d(TAG, "isCT4gCard : " + isCT4gCard
            + ", isCdmaLteDcSupport : " + isCdmaLteDcSupport
            + ", isSupportTddDataOnlyCheck return " + checkResult);
        return checkResult;
    }

    /**
     * Get MobileNetworkSettingsExt.
     * @return IMobileNetworkSettingsExt
     */
    public static IMobileNetworkSettingsExt getMobileNetworkSettingsExt() {
        return ExtensionManager.getMobileNetworkSettingsExt();
    }

    /**
     * Check Radio State by target slot.
     * @param slotId for check
     * @return true if radio is on
    */
    public static boolean getRadioStateForSlotId(final int slotId) {
        int currentSimMode = Settings.System.getInt(PhoneGlobals.getInstance().getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean radiosState = ((currentSimMode & (MODE_PHONE1_ONLY << slotId)) == 0) ?
                false : true;
        Log.d(TAG, "soltId: " + slotId + ", radiosState : " + radiosState);
        return radiosState;
    }

    /**
     * Check is airplane mode on.
     * @return true if airplane mode on
    */
    public static boolean isAirPlaneMode() {
        boolean isAirPlaneMode = Settings.System.getInt(
                PhoneGlobals.getInstance().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1;
        Log.d(TAG, "isAirPlaneMode = " + isAirPlaneMode);
        return isAirPlaneMode;
    }

    /**
     * Check lte data only mode.
     * @param context for getContentResolver
     * @return true if it is LteDataOnly mode
    */
    public static boolean is4GDataOnly(Context context) {
        if (context == null) {
            return false;
        }
        int networkMode = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.LTE_ON_CDMA_RAT_MODE, TelephonyManagerEx.SVLTE_RAT_MODE_4G);
        boolean is4GOnly = (networkMode == TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY);
        Log.d(TAG, "is4GDataOnly : " + is4GOnly);
        return is4GOnly;
    }

    /**
     * Check is svlte slot inserted.
     * @return true if svlte slot inserted
     */
    public static boolean isSvlteSlotInserted() {
        boolean isSvlteSlotInserted = false;
        int slot = SvlteModeController.getActiveSvlteModeSlotId();
        if (slot != -1) {
            TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
            if (telephonyManagerEx != null) {
                isSvlteSlotInserted = telephonyManagerEx.hasIccCard(slot);
            }
        }
        Log.d(TAG, "isSvlteSlotInserted = " + isSvlteSlotInserted + "slot = " + slot);
        return isSvlteSlotInserted;
    }

    /**
     * Check is svlte slot Radio On.
     * @return true if svlte slot Radio on
     */
    public static boolean isSvlteSlotRadioOn() {
        int slot = SvlteModeController.getActiveSvlteModeSlotId();
        boolean result = slot != -1 ? getRadioStateForSlotId(slot) : false;
        Log.d(TAG, "isSvlteSlotRadioOn = " + result);
        return result;
    }

    /**
     * Check is Svlte Roaming.
     * @return true if Svlte Roaming
     */
    private static boolean isSvlteRoaming(Phone phone) {
        boolean roaming = false;
        LteDcPhoneProxy lteDcPhoneProxy = (LteDcPhoneProxy) phone;
        SvlteRatController sLteRatController = lteDcPhoneProxy.getSvlteRatController();
        RoamingMode mRoamingMode = null;
        if (sLteRatController != null) {
            mRoamingMode = sLteRatController.getRoamingMode();
            Log.d(TAG, "isSvlteRoaming get roaming state: " + mRoamingMode);
            if ((RoamingMode.ROAMING_MODE_HOME != mRoamingMode) &&
                (RoamingMode.ROAMING_MODE_UNKNOWN != mRoamingMode)) {
                roaming = true;
            }
        }
        Log.d(TAG, "isSvlteRoaming? " + roaming);
        return roaming;
    }

    /**
     * Check is ct Roaming.
     * @return true if ct Roaming
     */
    public static boolean isCTRoaming(Phone phone) {
        CardType cardType = UiccController.getInstance().getCardType();
        boolean isCTCard = cardType.isOPO9Card();
        Log.d(TAG, "isCTRoaming cardType = " + cardType.toString() + " isCTCard = " + isCTCard);
        int slotId = SubscriptionManager.getSlotId(phone.getSubId());
        Log.d(TAG, "isCDMAPhone phone slotId = " + slotId +
                "c2k slot  = " + SvlteModeController.getActiveSvlteModeSlotId());
        if (isCTCard && SvlteModeController.getActiveSvlteModeSlotId() == slotId) {
            return isSvlteRoaming(phone);
        }
        return false;
    }
}
