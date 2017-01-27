package com.mediatek.settings.plugin;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceFragment;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.Utils;
import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultSimManagementExt;
import com.mediatek.widget.AccountViewAdapter.AccountElements;
import com.mediatek.xlog.Xlog;

import java.util.List;

/**
 * For settings SIM management feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.ISimManagementExt")
public class OP09SimManagementExtImp extends DefaultSimManagementExt {
    private static final String TAG = "OP09SimManagementExt";

    private Context mContext;
    private int mToCloseSlot = -1;
    private TelephonyManager mTelephonyManager;
    private ITelephony mITelephony;

    /**
     * update the preference screen of sim management.
     * @param context The context
     */
    public OP09SimManagementExtImp(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void setToClosedSimSlotSwitch(int simSlot, Context context) {
        Xlog.d(TAG, "setToClosedSimSlot = " + simSlot);
		
	    SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
		TelephonyManager mTelephonyManager;
		int subId = 0;
		int subId_close;
		int subId_t;
		int simCount;
		Boolean result;

        subId_close = subscriptionManager.getSubIdUsingPhoneId(simSlot);
        //subId_close = SubscriptionManager.getSubIdUsingSlotId(simSlot);
        mTelephonyManager = TelephonyManager.from(context);
		boolean enable_before = mTelephonyManager.getDataEnabled();
		subId = subscriptionManager.getDefaultDataSubId();
		Log.d(TAG, "setToClosedSimSlot: subId = " + subId + "subId_close=" + subId_close);		
	    if (subId_close != subId) {
		    return;
	    }
		simCount = mTelephonyManager.getSimCount();
		Log.d(TAG, "setToClosedSimSlot: simCount = " + simCount);

        for (int i = 0; i < simCount; i++) {
            final SubscriptionInfo sir = Utils.findRecordBySlotId(context, i);
            if (sir != null) {
                subId_t = sir.getSubscriptionId();
                Log.d(TAG, "setToClosedSimSlot: sir subId_t = " + subId_t);
                if (subId_t != subId) {
                     subscriptionManager.setDefaultDataSubId(subId_t);
                     if (enable_before) {
                         mTelephonyManager.setDataEnabled(subId_t, true);
                     } else {
                         mTelephonyManager.setDataEnabled(subId_t, false);
                     }
                }
					
            }
        }
		         
    }	

//    @Override
//    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
//    	SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
//        if (mToCloseSlot >= 0 && subscriptionManager.getActiveSubscriptionInfoList().size() > 1) {
//        	
//            if (isMobileDataOn()) {
//                long preferredDataSub = getPreferedDataSub();
//                if (preferredDataSub > -1 &&
//                        SubscriptionManager.getDefaultDataSubId() != preferredDataSub) {
//                    SubscriptionManager.setDefaultDataSubId(preferredDataSub);
//                    Log.d(TAG, "SIM Setting radio changed, switch data sub to " + preferredDataSub
//                            + ", slotId=" + SubscriptionManager.getSlotId(preferredDataSub));
//                }
//            }
//        }
//    }
//
//    @Override
//    public void setToClosedSimSlot(int simSlot) {
//        Xlog.d(TAG, "setToClosedSimSlot = " + simSlot);
//        mToCloseSlot = simSlot;
//    }
//
//    private boolean isMobileDataOn() {
//        boolean dataEnabled = getTelephonyManager().getDataEnabled();
//        Log.d(TAG, "isMobileDataOn dataEnabled=" + dataEnabled);
//        return dataEnabled;
//    }
//
//    private long getPreferedDataSub() {
//        long preferredDataSub = -1;
//        List<SubInfoRecord> subInfoRecordList = SubscriptionManager.getActiveSubInfoList();
//        for (SubInfoRecord subInfoRecord : subInfoRecordList) {
//            if (subInfoRecord.slotId != mToCloseSlot && isTargetSubRadioOn(subInfoRecord.subId)) {
//                preferredDataSub = subInfoRecord.subId;
//                break;
//            }
//        }
//        return preferredDataSub;
//    }
//    private TelephonyManager getTelephonyManager() {
//        if (mTelephonyManager == null) {
//            mTelephonyManager =
//                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
//        }
//        return mTelephonyManager;
//    }
//
//    private boolean isTargetSubRadioOn(long subId) {
//        boolean isRadioOn = false;
//        try {
//            isRadioOn = getITelephony().isRadioOnForSubscriber(subId);
//        } catch (RemoteException e) {
//            isRadioOn = false;
//            Log.e(TAG, "ITelephony exception");
//        }
//        Log.d(TAG, "isTargetSubRadioOn subId=" + subId + ", isRadioOn=" + isRadioOn);
//        return isRadioOn;
//    }
//
//    private ITelephony getITelephony() {
//        if (mITelephony == null) {
//            mITelephony = ITelephony.Stub.asInterface(
//                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
//        }
//        return mITelephony;
//    }

//    @Override
//    public void customizeSmsChoiceArray(List<AccountElements> smsList) {
//        //Remove always ask item, always item only exist when data size is greater than 1
//        if (smsList != null && smsList.size() > 1) {
//            smsList.remove(0);
//        }
//    }
//
//    @Override
//    public void customizeSmsChoiceValue(List<Object> smsValueList) {
//        if (smsValueList != null && smsValueList.size() > 1) {
//            smsValueList.remove(0);
//        }
//    }
    
    
    @Override
    public void customizeListArray(List<String> strings){
        Log.i(TAG, "op09 customizeListArray");

        if (strings != null && strings.size() > 1) {
        	strings.remove(0);
            Log.i(TAG, "op09 customizeListArray dothings");
 
        }
    }
    
    @Override
    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> subscriptionInfo){
        if (subscriptionInfo != null && subscriptionInfo.size() > 1) {
        	subscriptionInfo.remove(0);
        }
    }

	@Override
	public int customizeValue(int value) {
        Log.i(TAG, "op09 customizeValue");

		return value + 1;
	}

	@Override
    public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo sir, int type) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        SubscriptionInfo sir_tmp = sir;
        if (sir == null) {
            List<SubscriptionInfo> subList = subscriptionManager.getActiveSubscriptionInfoList();
            int subCount;
            if (subList == null) {
                subCount = 0;
            } else {
                subCount = subList.size();
            } 
            int subId = SubscriptionManager.getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1);
            if (subCount == 1) {
                subId = subList.get(0).getSubscriptionId();
                if (type == 2) {
                    subscriptionManager.setDefaultDataSubId(subId);
                    TelephonyManager.getDefault().setDataEnabled(subId, true);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 2, data sub set to " + subId);
                } else if (type == 1){
                    //subscriptionManager.setDefaultSmsSubId(subId);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 1, sms sub set to " + subId);
                }
                sir_tmp = Utils.findRecordBySubId(context, subId);
            } else if (subCount >= 2) {
                if (type == 2 && SubscriptionManager.getDefaultDataSubId() ==
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    subscriptionManager.setDefaultDataSubId(subId);
                    TelephonyManager.getDefault().setDataEnabled(subId, true);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 2, data sub set to " + subId);
                } else if (type == 1 && SubscriptionManager.getDefaultSmsSubId() ==
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    subscriptionManager.setDefaultSmsSubId(subId);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 1, sms sub set to " + subId);
                }
                sir_tmp = Utils.findRecordBySubId(context, subId);
            }
        }
        return sir_tmp;
    }

    @Override
    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccount){
        final TelecomManager telecomManager = TelecomManager.from(mContext);
        PhoneAccountHandle result = phoneAccount;
		Log.d(TAG, "setDefaultCallValue phoneAccount=" + phoneAccount);
        if (phoneAccount == null){
            List<PhoneAccountHandle> PhoneAccountlist = telecomManager.getCallCapablePhoneAccounts();
            int accoutSum = PhoneAccountlist.size();

            Log.d(TAG, "setDefaultCallValue accoutSum=" + accoutSum);
            if (accoutSum > 0) {
                 result = PhoneAccountlist.get(0);
            }		    
        }
        Log.d(TAG, "setDefaultCallValue result=" + result);
        return result;
    }
}
