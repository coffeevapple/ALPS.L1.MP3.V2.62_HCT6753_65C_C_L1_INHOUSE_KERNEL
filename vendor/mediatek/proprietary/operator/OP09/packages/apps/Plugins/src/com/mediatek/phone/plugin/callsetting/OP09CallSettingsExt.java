package com.mediatek.phone.plugin.callsetting;

import android.content.Context;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.phone.ext.DefaultCallFeaturesSettingExt;
import com.mediatek.phone.plugin.Utils;

/**
 * CT sim card call setting.
 */
@PluginImpl(interfaceName = "com.mediatek.phone.ext.ICallFeaturesSettingExt")
public class OP09CallSettingsExt extends DefaultCallFeaturesSettingExt {

    private static final String TAG = "OP09CallSettingsExt";
    private static final String CDMA_CALL_OPTION_CLASS_NAME =
        "com.mediatek.phone.plugin.CdmaAdditionalCallOptions";

    private static final String BUTTON_CFU_KEY = "button_cfu_key";
    private static final String BUTTON_CFB_KEY = "button_cfb_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";
    private static final String BUTTON_CFC_KEY = "button_cfc_key";
    private static final String NATIVE_MCC_SIM1 = "460"; // 46003
    private static final String NATIVE_MCC_SIM2 = "455"; // 45502

    private Context mContext;
    private int mSlotId;

    /**
     *
     * @param context get current context.
     */
    public OP09CallSettingsExt(Context context) {
        Log.i(TAG, "OP09CallSettingsExt: " + context.getPackageName());
        mContext = context;
    }

    @Override
    public void initCdmaCallForwardOptionsActivity(PreferenceActivity activity, int subId) {
        Log.d(TAG, "OP09CallSettingsExt initCdmaCallForwardOptionsActivity, subId: " + subId);
        mSlotId = SubscriptionManager.getSlotId(subId);
        if (PhoneConstants.SIM_ID_1 == mSlotId) {
            PreferenceScreen prefScreen = activity.getPreferenceScreen();
            Preference buttonCFU = prefScreen.findPreference(BUTTON_CFU_KEY);
            Preference buttonCFB = prefScreen.findPreference(BUTTON_CFB_KEY);
            Preference buttonCFNRy = prefScreen.findPreference(BUTTON_CFNRY_KEY);
            Preference buttonCFNRc = prefScreen.findPreference(BUTTON_CFNRC_KEY);
            Preference buttonCFC = prefScreen.findPreference(BUTTON_CFC_KEY);

            if (buttonCFU != null) {
                buttonCFU.setTitle(mContext.getString(R.string.ct_labelCFU));
            }
            if (buttonCFB != null) {
                buttonCFB.setTitle(mContext.getString(R.string.ct_labelCFB));
            }
            if (buttonCFNRy != null) {
                buttonCFNRy.setTitle(mContext.getString(R.string.ct_labelCFNRy));
            }
            if (buttonCFNRc != null) {
                buttonCFNRc.setTitle(mContext.getString(R.string.ct_labelCFNRc));
            }
            if (buttonCFC != null) {
                buttonCFC.setTitle(mContext.getString(R.string.ct_labelCFC));
            }
            removeCFNRc(prefScreen, buttonCFNRc);
        }
    }

    /***
     * For CT feature.
     * when sim1 registe GSM network in IR ,
     * remove ct_labelCFNRc or ct_labelCFNRy.
     * @param prefSet parent prefSet
     * @param preference preference will be remove
     */
    public void removeCFNRc(PreferenceScreen prefSet, Preference preference) {

        boolean isSimInsert = Utils.isSIMInserted(PhoneConstants.SIM_ID_1);
        boolean isSimStateReady = Utils.isSimStateReady(mContext, PhoneConstants.SIM_ID_1);
        boolean isRadioOn = Utils.isTargetSimRadioOn(PhoneConstants.SIM_ID_1);
        boolean isAirPlaneMode = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1);
        Log.i(TAG, "isSimInsert = " + isSimInsert +
                " isSimStateReady = " + isSimStateReady +
                " isRadioOn = " + isRadioOn +
                " isAirPlaneMode = " + isAirPlaneMode +
                " mSlotId = " + mSlotId);

        if (isSimInsert && isSimStateReady &&
                isRadioOn && !isAirPlaneMode &&
                mSlotId == PhoneConstants.SIM_ID_1) {
            Phone phone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
            Log.i(TAG, "1 means gsm, 2 means cama, phone.getPhoneType() = " + phone.getPhoneType());
            if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM && isCdmaRoaming()) {
                prefSet.removePreference(preference);
            }
        }
    }
    /// @}

    private boolean isCdmaRoaming() {
        boolean res = false;

        Log.i(TAG, "in isCdmaRoaming");
        String numeric = SystemProperties
            .get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
        Log.i(TAG, "isCdmaRoaming numeric :" + numeric);
        if (numeric != null && !numeric.equals("-1") && numeric.length() >= 3) {
            String mcc = numeric.substring(0, 3);
            Log.i(TAG, "mcc=" + mcc);
            if (NATIVE_MCC_SIM1.equals(mcc) || NATIVE_MCC_SIM2.equals(mcc)) {
                res = false;
            } else {
                res = true;
            }
        }
        return res;
    }
}
