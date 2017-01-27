/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaApnSetting;
import com.mediatek.settings.deviceinfo.UnLockSubDialog;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.ext.IRcseOnlyApnExtension;
import com.mediatek.settings.ext.IRcseOnlyApnExtension.OnRcseOnlyApnStateChangedListener;
import com.mediatek.settings.sim.MsimRadioValueObserver;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;

public class ApnSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, MsimRadioValueObserver.Listener  {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static boolean mRestoreDefaultApnMode;

    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private SubscriptionInfo mSubscriptionInfo;

    private UserManager mUm;

    private String mSelectedKey;

    private IntentFilter mMobileStateFilter;

    private boolean mUnavailable;

    /// M: add for SIM hot swap @{
    private SimHotSwapHandler mSimHotSwapHandler;
    /// @}
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED.equals(action)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    } else {
                        showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
                String apnType  = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                Log.d(TAG, "Receiver,send MMS status, get type = " + apnType);
                if (PhoneConstants.APN_TYPE_MMS.equals(apnType)) {
                    getPreferenceScreen().setEnabled(
                            mExt.getScreenEnableState(mSubscriptionInfo.getSubscriptionId(),
                                    getActivity()));
                }
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                //M: Update the screen since airplane mode on need to disable the whole screen include menu {
                getPreferenceScreen().setEnabled(
                        mExt.getScreenEnableState(mSubscriptionInfo.getSubscriptionId(),
                                getActivity()));
                getActivity().invalidateOptionsMenu();
                //@}
            }
        }
    };

    private OnRcseOnlyApnStateChangedListener mListener = new OnRcseOnlyApnStateChangedListener() {
        @Override
        public void onRcseOnlyApnStateChanged(boolean isEnabled) {
            Log.d(TAG, "onRcseOnlyApnStateChanged()-current state is " + isEnabled);
            if (mSubscriptionInfo.getSubscriptionId() !=
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                fillList();
            }
        }
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        final int subId = activity.getIntent().getIntExtra("sub_id",
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        mExt = UtilsExt.getApnSettingsPlugin(getActivity());
        mExt.initTetherField(this);
        mRcseExt = UtilsExt.getRcseApnPlugin(getActivity());
        mRcseExt.addRcseOnlyApnStateChanged(mListener);

        mMobileStateFilter = mExt.getIntentFilter();
        if (!mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            setHasOptionsMenu(true);
        }

        mSubscriptionInfo = Utils.findRecordBySubId(activity, subId);
        /// M: @{
        if (mSubscriptionInfo == null) {
            Log.d(TAG, "onCreate()... Invalid subId: " + subId);
            getActivity().finish();
        }
        mRadioValueObserver = new MsimRadioValueObserver(getActivity());
        /// @}
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView empty = (TextView) getView().findViewById(android.R.id.empty);
        if (empty != null) {
            empty.setText(R.string.apn_settings_not_available);
            getListView().setEmptyView(empty);
        }

        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }

        addPreferencesFromResource(R.xml.apn_settings);

        getListView().setItemsCanFocus(true);

        mSimHotSwapHandler = SimHotSwapHandler.newInstance(getActivity());
        mSimHotSwapHandler.registerOnSubscriptionsChangedListener();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }
        UnLockSubDialog.showDialog(getActivity(), mSubscriptionInfo.getSubscriptionId());
        getActivity().registerReceiver(mExt.getBroadcastReceiver(mMobileStateReceiver),
                mMobileStateFilter);
        /// M: @{
        mRadioValueObserver.registerMsimObserver(this);
        /// @}
        if (!mRestoreDefaultApnMode) {
            fillList();
            // M: In case dialog not dismiss as activity is in background, so when resume back, need to 
            // remove the dialog {
            removeDialog(DIALOG_RESTORE_DEFAULTAPN);
            //@}
        }

        mExt.updateTetherState(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(mExt.getBroadcastReceiver(mMobileStateReceiver));
        mRadioValueObserver.ungisterMsimObserver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSimHotSwapHandler.unregisterOnSubscriptionsChangedListener();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
        mRcseExt.removeRcseOnlyApnStateChanged(mListener);
    }

    private void fillList() {
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String mccmnc = mSubscriptionInfo == null ? ""
            : tm.getSimOperator(mSubscriptionInfo.getSubscriptionId());
        Log.d(TAG, "mccmnc = " + mccmnc);
        String where = mExt.getFillListQuery(mccmnc, mSubscriptionInfo.getSubscriptionId());
        where = CdmaApnSetting.customizeQuerySelection(
                mExt, mccmnc, mSubscriptionInfo.getSubscriptionId(), where);
        where += " AND NOT (type='ia' AND (apn=\'\' OR apn IS NULL))";
        /// M: for non-volte project,do not show ims apn @{
        if (!FeatureOption.MTK_VOLTE_SUPPORT) {
            where += " AND NOT type='ims'";
        }
        /// @}
        Log.d(TAG, "fillList where: " + where);

        Cursor cursor = getContentResolver().query(
                Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "name", "apn", "type", "sourcetype"}, where, null, null);
        cursor = mExt.customizeQueryResult(
                getActivity(), cursor, Telephony.Carriers.CONTENT_URI, mccmnc);

        if (cursor != null) {
            Log.d(TAG, "fillList cursor count: " + cursor.getCount());
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();

            ArrayList<Preference> mmsApnList = new ArrayList<Preference>();

            mSelectedKey = getSelectedApnKey();
            Log.d(TAG, "fillList getSelectedApnKey: " + mSelectedKey);
            // M: define tmp select key
            String selectedKey = null;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                int sourcetype = cursor.getInt(SOURCE_TYPE_INDEX);

                if (mExt.isSkipApn(type, mRcseExt)) {
                    cursor.moveToNext();
                    continue;
                }

                name = mExt.updateAPNName(name, sourcetype);		
                ApnPreference pref = new ApnPreference(getActivity());

                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);

                pref.setApnEditable(mExt.isAllowEditPresetApn(type, apn, mccmnc, sourcetype));
                pref.setSubId(mSubscriptionInfo.getSubscriptionId());

                /// M: All tether apn will be selectable for otthers , mms will not be selectable.
                boolean selectable = mExt.isSelectable(type);
                pref.setSelectable(selectable);
                Log.d(TAG,"mSelectedKey = " + mSelectedKey + " key = " + key + " name = " + name);
                if (selectable) {
                    if (selectedKey == null) {
                        pref.setChecked();
                        selectedKey = key;
                    }
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                        selectedKey = mSelectedKey;
                    }
                    apnList.addPreference(pref);
                } else {
                    mmsApnList.add(pref);
                    CdmaApnSetting.customizeUnselectablePreferences(
                            mmsApnList, mSubscriptionInfo.getSubscriptionId());
                    mExt.customizeUnselectableApn(
                            mmsApnList, mSubscriptionInfo.getSubscriptionId());
                }
                cursor.moveToNext();
            }
            cursor.close();

            if (selectedKey != null && selectedKey != mSelectedKey) {
                setSelectedApnKey(selectedKey);
            }

            for (Preference preference : mmsApnList) {
                apnList.addPreference(preference);
            }
            getPreferenceScreen().setEnabled(mExt.getScreenEnableState(
                    mSubscriptionInfo.getSubscriptionId(), getActivity()));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mUnavailable) {
            menu.add(0, MENU_NEW, 0,
                    getResources().getString(R.string.menu_new))
                    .setIcon(android.R.drawable.ic_menu_add)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(0, MENU_RESTORE, 0,
                    getResources().getString(R.string.menu_restore))
                    .setIcon(android.R.drawable.ic_menu_upload);
        }
        mExt.updateMenu(menu, MENU_NEW, MENU_RESTORE,
                TelephonyManager.getDefault().getSimOperator(
                mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                        : SubscriptionManager.INVALID_SUBSCRIPTION_ID));

        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        int size = menu.size();
        boolean isOn = TelephonyUtils.isAirplaneModeOn(getActivity()); 
        Log.d(TAG,"onPrepareOptionsMenu isOn = " + isOn);
        // When airplane mode on need to disable options menu
        for (int i = 0;i< size;i++) {
            menu.getItem(i).setEnabled(!isOn);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;

        case MENU_RESTORE:
            restoreDefaultApn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        intent.putExtra("sub_id", subId);
        /// M: add for custom the intent @{
        mExt.addApnTypeExtra(intent);
        /// @}
        startActivity(intent);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        /// M: SVLTE maintain 2 phone(CDMAPhone and LteDcPhone), need to set/get preferred apn
        /// using correct sub id, LteDcPhone uses special sub id. @{
        if (FeatureOption.MTK_SVLTE_SUPPORT) {
            Cursor cursor = getContentResolver().query(
                    Telephony.Carriers.CONTENT_URI,
                    new String[] { "_id", "sourcetype" },
                    "_id = " + key,
                    null,
                    null);

            Log.d(TAG, "setSelectedApnKey cursor.getCount() - " + cursor.getCount());
            if (cursor.getCount() > 0) {
                int c2kSlot = SvlteModeController.getActiveSvlteModeSlotId();
                Log.d(TAG, "setSelectedApnKey c2kSlot" + c2kSlot);
                Log.d(TAG, "setSelectedApnKey mSubscriptionInfo getSubscriptionId" + mSubscriptionInfo.getSubscriptionId());
                int currentSlot = SubscriptionManager.getSlotId(mSubscriptionInfo.getSubscriptionId());
                Log.d(TAG, "setSelectedApnKey currentSlot" + currentSlot);
                cursor.moveToFirst();
                int sourceType = cursor.getInt(1);
                // If user-created apn set as preferred apn,
                // we will set it to both LTE and C2K Phone.
                if (sourceType == 1 && c2kSlot == currentSlot) {
                    resolver.update(mExt.getRestoreCarrierUri(
                        mSubscriptionInfo.getSubscriptionId()), values, null, null);
                    Log.d(TAG, "setSelectedApnKey sourceType == 1 && c2kSlot == currentSlot");
                    resolver.update(mExt.getRestoreCarrierUri(SvlteUtils.getLteDcSubId(c2kSlot)),
                            values, null, null);
                } else {
                    int current_sub = -1;
                    ContentValues values_t = new ContentValues();
                    values_t.put(APN_ID, -1);

                    Log.d(TAG, "setSelectedApnKey not sourceType == 1 && c2kSlot == currentSlot");
                    Log.d(TAG, "setSelectedApnKey CdmaApnSetting.getPreferredSubId subid=" + CdmaApnSetting.getPreferredSubId(getActivity(),
                              mSubscriptionInfo.getSubscriptionId()));

                    current_sub = CdmaApnSetting.getPreferredSubId(getActivity(),
                        mSubscriptionInfo.getSubscriptionId());
                    Log.d(TAG, "setSelectedApnKey current_sub =" + 
                        CdmaApnSetting.getPreferredSubId(getActivity(),
                        mSubscriptionInfo.getSubscriptionId()));

                    resolver.update(mExt.getRestoreCarrierUri(current_sub), values, null, null);
                    
                    if (current_sub == SvlteUtils.getLteDcSubId(c2kSlot)) {
                        resolver.update(mExt.getRestoreCarrierUri(
                            mSubscriptionInfo.getSubscriptionId()), values_t, null, null);
                    } else {
                        resolver.update(
                            mExt.getRestoreCarrierUri(SvlteUtils.getLteDcSubId(c2kSlot)),
                            values_t, null, null);
                    }
                }
            }
            cursor.close();
        } else {
            resolver.update(mExt.getRestoreCarrierUri(
                CdmaApnSetting.getPreferredSubId(getActivity(),
                mSubscriptionInfo.getSubscriptionId())), values, null, null);
        }
        /// @}
    }

    private String getSelectedApnKey() {
        String key = null;

        /// M: SVLTE maintain 2 phone(CDMAPhone and LteDcPhone), need to set/get preferred apn
        /// using correct sub id, LteDcPhone uses special sub id. @{
        Cursor cursor = getContentResolver().query(
                mExt.getRestoreCarrierUri(
                        CdmaApnSetting.getPreferredSubId(getActivity(),
                                mSubscriptionInfo.getSubscriptionId())), new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        /// @}
        Log.d(TAG, "getSelectedApnKey cursor.getCount " + cursor.getCount());
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        Log.d(TAG,"getSelectedApnKey key = " + key);
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    Activity activity = getActivity();
                    if (activity == null) {
                        mRestoreDefaultApnMode = false;
                        return;
                    }
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    removeDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        activity,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    //M: ALPS01760966 In case the dialog intend to dismiss in background and i will cause JE, so
    // the dialog will not be removed in background, but to remove on resume if it is showing { 
    @Override
    protected void removeDialog(int dialogId) {
        if (this.isResumed() && isDialogShowing(dialogId)) {
            super.removeDialog(dialogId);
        }
    }
    //@}
    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(getUri(DEFAULTAPN_URI), null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    private Uri getUri(Uri uri) {
        return Uri.withAppendedPath(uri, "/subId/" + mSubscriptionInfo.getSubscriptionId());
    }

    /// M: add for mediatek Plugin @{
    private static final int SOURCE_TYPE_INDEX = 4;
    private IApnSettingsExt mExt;
    private IRcseOnlyApnExtension mRcseExt;
    /// @}

    private MsimRadioValueObserver mRadioValueObserver;

    @Override
    public void onChange(int msimModevalue, boolean selfChange) {
        getPreferenceScreen().setEnabled(
                mExt.getScreenEnableState(mSubscriptionInfo.getSubscriptionId(),
                        getActivity()));
        getActivity().invalidateOptionsMenu();
    }

}
