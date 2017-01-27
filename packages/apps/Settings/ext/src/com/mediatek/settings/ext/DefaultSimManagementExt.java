package com.mediatek.settings.ext;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import com.mediatek.widget.AccountViewAdapter.AccountElements;
//import com.mediatek.telephony.SimInfoManager;
//import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class DefaultSimManagementExt implements ISimManagementExt {
    private static final String TAG = "DefaultSimManagementExt";

    /**
     * update the preference screen of sim management
     * @param parent parent preference
     */
    public void updateSimManagementPref(PreferenceGroup parent) {
        Xlog.d(TAG, "updateSimManagementPref()");
        PreferenceScreen pref3GService = null;
        PreferenceScreen prefWapPush = null;
        PreferenceScreen prefStatus = null;
        if (pref3GService != null) {
            Xlog.d(TAG, "updateSimManagementPref()---remove pref3GService");
            parent.removePreference(pref3GService);
        }
        if (prefStatus != null) {
            Xlog.d(TAG, "updateSimManagementPref()---remove prefStatus");
            parent.removePreference(prefStatus);
        }
    }

    public void updateSimEditorPref(PreferenceFragment pref) {
        return;
    }
    public void dealWithDataConnChanged(Intent intent, boolean isResumed) {
        return;
    }

    @Override
    public void updateDefaultSIMSummary(Preference pref, int subId) {
    }

    public boolean isNeedsetAutoItem() {
        return false;
    }
    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
        Xlog.d(TAG, "showChangeDataConnDialog");

        return;
    }

    public void setToClosedSimSlot(int simSlot) {
        return;
    }

    public void customizeSimColorEditPreference(PreferenceFragment pref, String key) {
    }

    @Override
    public void customizeVoiceChoiceArray(List<AccountElements> voiceList, boolean voipAvailable) {

    }

    @Override
    public void customizeSmsChoiceArray(List<AccountElements> smsList) {

    }

    @Override
    public void customizeSmsChoiceValueArray(List<Long> smsValueList) {

    }
    
    @Override
    public void customizeSmsChoiceValue(List<Object> smsValueList) {

    }

    @Override
    public void updateDefaultSettingsItem(PreferenceGroup prefGroup) {
    }

    @Override
    public boolean enableSwitchForSimInfoPref() {
        return true;
    }

    @Override
    public void updateSimNumberMaxLength(EditTextPreference editTextPreference, int slotId) {
    }

    @Override
    public void hideSimEditorView(View view, Context context) {
    }

    @Override
    public Drawable getSmsAutoItemIcon(Context context) {
        return null;
    }

    @Override
    public int getDefaultSmsSubIdForAuto() {
        return 0;
    }

    @Override
    public void initAutoItemForSms(ArrayList<String> list,
            ArrayList<SubscriptionInfo> smsSubInfoList) {
    }

    @Override
    public void registerObserver() {
    }

    @Override
    public void unregisterObserver() {
    }

    @Override
    public boolean switchDefaultDataSub(Context context, int subId) {
        return false;
    }
    
    @Override
    public void customizeListArray(List<String> strings){

    }
    
    @Override
    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> subscriptionInfo){
    	
    }

	@Override
	public int customizeValue(int value) {
		return value;
	}
    
	@Override
	public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo sir, int type) {
	    return sir;
	}
    
    @Override
    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccount){
        return phoneAccount;
    }

    @Override
    public void setToClosedSimSlotSwitch(int simSlot, Context context) {
        return;
    }	
    
	@Override
    public void setDataState(int subid) {
		return;
	}

    @Override
    public void setDataStateEnable(int subid){
        return;
    }
}
