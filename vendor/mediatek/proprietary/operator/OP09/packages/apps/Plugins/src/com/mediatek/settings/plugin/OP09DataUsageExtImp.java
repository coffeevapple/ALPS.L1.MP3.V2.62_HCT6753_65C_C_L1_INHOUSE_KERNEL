package com.mediatek.settings.plugin;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;

import android.preference.PreferenceFragment;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.Utils;
import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultDataUsageSummaryExt;
import com.mediatek.settings.ext.IDataUsage;
import com.mediatek.widget.AccountViewAdapter.AccountElements;
//import com.android.widget.Switch;

import com.mediatek.xlog.Xlog;

import java.util.List;
import java.util.Map;

/**
 * For settings SIM management feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IDataUsageSummaryExt")
public class OP09DataUsageExtImp extends DefaultDataUsageSummaryExt {
    private static final String TAG = "OP09DataUsageExt";

    private Context mContext;
    private int mToCloseSlot = -1;
    private TelephonyManager mTelephonyManager;
    private ITelephony mITelephony;
	private IDataUsage datausage_entry;

	private Map<String, Boolean> mMobileDataEnabled;
    /**
     * update the preference screen of sim management.
     * @param context The context
     */
    public OP09DataUsageExtImp(Context context) {
        super(context);
        mContext = context;
    }

    private ContentObserver mDataConnectionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange selfChange=" + selfChange);
            //if (!selfChange) {
                TelephonyManager mTelephonyManager = TelephonyManager.getDefault();
        		List<SubscriptionInfo> si =  SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
                if (si != null && si.size() > 0) {
       		       for (int i = 0; i < si.size(); i++) {
       		         SubscriptionInfo subInfo = si.get(i);
                       int subId = subInfo.getSubscriptionId();
                       Log.i(TAG, "onChanged, updateMap key subId = " + subId + " value:" + mTelephonyManager.getDataEnabled(subId));
                       mMobileDataEnabled.put(String.valueOf(subId), mTelephonyManager.getDataEnabled(subId));
       		       }
               }
                datausage_entry.updatePolicy(false);
            //}
        }
    };
	

    @Override
    public void resume(Context context, IDataUsage datausage, Map<String, Boolean> mobileDataEnabled) {
    	Log.i(TAG, "OP09DataUsageExtImp resume go");
        int subId = 0;
        mMobileDataEnabled = mobileDataEnabled;
		List<SubscriptionInfo> si =  SubscriptionManager.from(mContext)
			  .getActiveSubscriptionInfoList();

        if (si != null && si.size() > 0) {
		     for (int i = 0; i < si.size(); i++) {
		         SubscriptionInfo subInfo = si.get(i);
                 subId = subInfo.getSubscriptionId();
                 mContext.getContentResolver().registerContentObserver(
                     Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId),
                     true, mDataConnectionObserver);				 
		     }
        }

		datausage_entry = datausage;
//		datausage_entry.updatePolicy(false);

    }

    @Override
    public void pause(Context context) {
        context.getContentResolver().unregisterContentObserver(mDataConnectionObserver);
    }

}
