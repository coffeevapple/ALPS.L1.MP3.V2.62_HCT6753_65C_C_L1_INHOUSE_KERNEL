/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.sim;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.Log;
import com.mediatek.settings.sim.PhoneServiceStateHandler;
import com.mediatek.settings.sim.RadioPowerManager;
import com.mediatek.settings.sim.RadioPowerPreference;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.telecom.TelecomManagerEx;

import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment
        implements Indexable,
        PhoneServiceStateHandler.Listener {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = true;

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_SIM_ACTIVITIES = "sim_activities";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    private static final String KEY_ACTIVITIES = "activities";
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int PROXY_INDEX = 3;
    private static final int PORT_INDEX = 4;
    private static final int USER_INDEX = 5;
    private static final int SERVER_INDEX = 6;
    private static final int PASSWORD_INDEX = 7;
    private static final int MMSC_INDEX = 8;
    private static final int MCC_INDEX = 9;
    private static final int MNC_INDEX = 10;
    private static final int NUMERIC_INDEX = 11;
    private static final int MMSPROXY_INDEX = 12;
    private static final int MMSPORT_INDEX = 13;
    private static final int AUTH_TYPE_INDEX = 14;
    private static final int TYPE_INDEX = 15;
    private static final int PROTOCOL_INDEX = 16;
    private static final int CARRIER_ENABLED_INDEX = 17;
    private static final int BEARER_INDEX = 18;
    private static final int ROAMING_PROTOCOL_INDEX = 19;
    private static final int MVNO_TYPE_INDEX = 20;
    private static final int MVNO_MATCH_DATA_INDEX = 21;
    private static final int DATA_PICK = 0;
    private static final int CALLS_PICK = 1;
    private static final int SMS_PICK = 2;

    /**
     * By UX design we use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     * mSelectableSubInfos is the list of SubInfos that a user can select for data, calls, and SMS.
     */
    private List<SubscriptionInfo> mAvailableSubInfos = null;
    private List<SubscriptionInfo> mSubInfoList = null;
    private List<SubscriptionInfo> mSelectableSubInfos = null;

    private SubscriptionInfo mCellularData = null;
    private SubscriptionInfo mCalls = null;
    private SubscriptionInfo mSMS = null;

    private PreferenceScreen mSimCards = null;

    private SubscriptionManager mSubscriptionManager;
    private Utils mUtils;
    private ISettingsMiscExt mMiscExt;


    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);

        mSubscriptionManager = SubscriptionManager.from(getActivity());

        if (mSubInfoList == null) {
            mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
            // FIXME: b/18385348, needs to handle null from getActiveSubscriptionInfoList
        }
        if (DBG) log("[onCreate] mSubInfoList=" + mSubInfoList);

        /// M: @{
        init();
        /// @}
        createPreferences();
        updateAllOptions();

        SimBootReceiver.cancelNotification(getActivity());
    }

    private void createPreferences() {
        final TelephonyManager tm =
            (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.sim_settings);

        mSimCards = (PreferenceScreen)findPreference(SIM_CARD_CATEGORY);
        /// M: only for OP09 UIM/SIM changes.
        changeSimActivityTitle();

        final int numSlots = tm.getSimCount();
        mAvailableSubInfos = new ArrayList<SubscriptionInfo>(numSlots);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        for (int i = 0; i < numSlots; ++i) {
            final SubscriptionInfo sir = Utils.findRecordBySlotId(getActivity(), i);
            SimPreference simPreference = new SimPreference(getActivity(), sir, i);
            simPreference.setOrder(i-numSlots);
            /// M: add for Radio on/off feature @{
            bindWithRadioPowerManager(simPreference, sir);
            /// @}
            mSimCards.addPreference(simPreference);
            mAvailableSubInfos.add(sir);
            if (sir != null) {
                mSelectableSubInfos.add(sir);
            }
        }

        updateActivitesCategory();
    }

    private void updateAvailableSubInfos(){
        final TelephonyManager tm =
            (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        final int numSlots = tm.getSimCount();

        mAvailableSubInfos = new ArrayList<SubscriptionInfo>(numSlots);
        for (int i = 0; i < numSlots; ++i) {
            final SubscriptionInfo sir = Utils.findRecordBySlotId(getActivity(), i);
            mAvailableSubInfos.add(sir);
            if (sir != null) {
            }
        }
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        mSubscriptionManager.getAllSubscriptionInfoList();

        final int prefSize = mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference)pref).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
    }

    private void updateSmsValues() {
        final Preference simPref = findPreference(KEY_SMS);
        if (simPref != null) {
            SubscriptionInfo sir = Utils.findRecordBySubId(getActivity(),
                    mSubscriptionManager.getDefaultSmsSubId());
            simPref.setTitle(R.string.sms_messages_title);
            if (DBG) log("[updateSmsValues] mSubInfoList=" + mSubInfoList);

            sir = mExt.setDefaultSubId(getActivity(), sir, 1);
            if (sir != null) {
                simPref.setSummary(sir.getDisplayName());
            } else if (sir == null) {
                updateSmsSummary(simPref);
            }
            simPref.setEnabled(mSelectableSubInfos.size() >= 1); 
        }
    }

    private void updateCellularDataValues() {
        final Preference simPref = findPreference(KEY_CELLULAR_DATA);
        if (simPref != null) {
            SubscriptionInfo sir = Utils.findRecordBySubId(getActivity(),
                    mSubscriptionManager.getDefaultDataSubId());                    
            simPref.setTitle(R.string.cellular_data_title);
            if (DBG) log("[updateCellularDataValues] mSubInfoList=" + mSubInfoList);

            sir = mExt.setDefaultSubId(getActivity(), sir, 2);
            if (sir != null) {
                simPref.setSummary(sir.getDisplayName());
            } else if (sir == null) {
                simPref.setSummary(R.string.sim_selection_required_pref);
            }
            /// M: @{
            simPref.setEnabled(mSelectableSubInfos.size() >= 1 &&
                    (!mIsAirplaneModeOn) &&
                    (!isCapabilitySwitching()));
            /// @}    
        }

    }

    private void updateSimPref() {
        final Preference simPref = findPreference(KEY_CELLULAR_DATA);
        if (simPref != null) {
            /// M: @{
            simPref.setEnabled(mSelectableSubInfos.size() >= 1 &&
                    (!mIsAirplaneModeOn) &&
                    (!isCapabilitySwitching()));
            /// @}    
        }

    }

    private void updateCallValues() {
        final Preference simPref = findPreference(KEY_CALLS);
        if (simPref != null) {
            final TelecomManager telecomManager = TelecomManager.from(getActivity());
            PhoneAccountHandle phoneAccount =
                telecomManager.getUserSelectedOutgoingPhoneAccount();

            phoneAccount = mExt.setDefaultCallValue(phoneAccount);
            Log.d(TAG, "updateCallValues phoneAccount=" + phoneAccount);
            simPref.setTitle(R.string.calls_title);
            simPref.setSummary(phoneAccount == null
                    ? getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                    : (String)telecomManager.getPhoneAccount(phoneAccount).getLabel());
            int accoutSum = telecomManager.getCallCapablePhoneAccounts().size();
            Log.d(TAG, "accountSum: " + accoutSum + "PhoneAccount: " + phoneAccount);
            simPref.setEnabled(accoutSum >= 1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /// M: add for SIM hot swap @{
        mSimHotSwapHandler.registerOnSubscriptionsChangedListener();
        /// @}
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        // FIXME: b/18385348, needs to handle null from getActiveSubscriptionInfoList
        if (DBG) log("[onResme] mSubInfoList=" + mSubInfoList);

        removeItem();

        updateAvailableSubInfos();
        updateAllOptions();
        /// M: Auto open the other card's data connection. when current card is radio off@{
        mExt.registerObserver();
        mExt.dealWithDataConnChanged(null, isResumed());
        /// @}
        // M: for CT to replace the SIM to UIM
        replaceSIMString();
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        final Context context = getActivity();
        Intent intent = new Intent(context, SimDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (preference instanceof SimPreference) {
            ((SimPreference)preference).createEditDialog((SimPreference)preference);
        } else if (findPreference(KEY_CELLULAR_DATA) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_CALLS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.CALLS_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_SMS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.SMS_PICK);
            context.startActivity(intent);
        }

        return true;
    }

    private class SimPreference extends RadioPowerPreference{
        private SubscriptionInfo mSubInfoRecord;
        private int mSlotId;
        private int[] mTintArr;
        Context mContext;
        private String[] mColorStrings;
        private int mTintSelectorPos;
        
        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);
            mContext = context;
            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
            mTintArr = context.getResources().getIntArray(com.android.internal.R.array.sim_colors);
            mColorStrings = context.getResources().getStringArray(R.array.color_picker);
            mTintSelectorPos = 0;
        }

        public void update() {
            final Resources res = getResources();

            setTitle(String.format(res.getString(R.string.sim_editor_title),
                                   (mSlotId + 1)));
            /// M: only for OP09 UIM/SIM changes.
            changeTitle();
            if (mSubInfoRecord != null) {
                if (TextUtils.isEmpty(getPhoneNumber(mSubInfoRecord))) {
                   setSummary(mSubInfoRecord.getDisplayName());
                } else {
                    setSummary(mSubInfoRecord.getDisplayName() + " - " +
                            getPhoneNumber(mSubInfoRecord));
                    setEnabled(true);
                }
                setIcon(new BitmapDrawable(res, (mSubInfoRecord.createIconBitmap(mContext))));
                /// M: add for radio on/off @{
                setRadioEnabled(!mIsAirplaneModeOn
                        && isRadioSwitchComplete(mSubInfoRecord.getSubscriptionId()));
                setRadioOn(TelephonyUtils.isRadioOn(mSubInfoRecord.getSubscriptionId()));
                /// @}
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
        }

        public SubscriptionInfo getSubInfoRecord() {
            return mSubInfoRecord;
        }

        public void createEditDialog(SimPreference simPref) {
            final Resources res = getResources();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final View dialogLayout = getActivity().getLayoutInflater().inflate(
                    R.layout.multi_sim_dialog, null);
            builder.setView(dialogLayout);

            EditText nameText = (EditText) dialogLayout.findViewById(R.id.sim_name);
            nameText.setText(mSubInfoRecord.getDisplayName());
            /// M: only for OP09 UIM/SIM changes.
            changeSimNameTitle(dialogLayout);

            final Spinner tintSpinner = (Spinner) dialogLayout.findViewById(R.id.spinner);
            SelectColorAdapter adapter = new SelectColorAdapter(getContext(),
                     R.layout.settings_color_picker_item, mColorStrings);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tintSpinner.setAdapter(adapter);

            for (int i = 0; i < mTintArr.length; i++) {
                if (mTintArr[i] == mSubInfoRecord.getIconTint()) {
                    tintSpinner.setSelection(i);
                    mTintSelectorPos = i;
                    break;
                }
            }

            tintSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                    int pos, long id){
                    tintSpinner.setSelection(pos);
                    mTintSelectorPos = pos;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            TextView numberView = (TextView)dialogLayout.findViewById(R.id.number);
            final String rawNumber = getPhoneNumber(mSubInfoRecord);
            if (TextUtils.isEmpty(rawNumber)) {
                numberView.setText(res.getString(com.android.internal.R.string.unknownName));
            } else {
                numberView.setText(PhoneNumberUtils.formatNumber(rawNumber));
            }

            final TelephonyManager tm =
                    (TelephonyManager) getActivity().getSystemService(
                            Context.TELEPHONY_SERVICE);
            String simCarrierName = tm.getSimOperatorNameForSubscription(mSubInfoRecord
                    .getSubscriptionId());
            TextView carrierView = (TextView) dialogLayout.findViewById(R.id.carrier);
            carrierView.setText(!TextUtils.isEmpty(simCarrierName) ? simCarrierName :
                    getContext().getString(com.android.internal.R.string.unknownName));


            builder.setTitle(String.format(res.getString(R.string.sim_editor_title),
                                           (mSubInfoRecord.getSimSlotIndex() + 1)));

            /// M: only for OP09 UIM/SIM changes.
            changeEditorTitle(builder);
            builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    final EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);

                    String displayName = nameText.getText().toString();
                    int subId = mSubInfoRecord.getSubscriptionId();
                    mSubInfoRecord.setDisplayName(displayName);
                    mSubscriptionManager.setDisplayName(displayName, subId,
                            SubscriptionManager.NAME_SOURCE_USER_INPUT);
                    //Utils.findRecordBySubId(getActivity(), subId).setDisplayName(displayName);

                    final int tintSelected = tintSpinner.getSelectedItemPosition();
                    int subscriptionId = mSubInfoRecord.getSubscriptionId();
                    int tint = mTintArr[tintSelected];
                    mSubInfoRecord.setIconTint(tint);
                    mSubscriptionManager.setIconTint(tint, subscriptionId);
                    //Utils.findRecordBySubId(getActivity(), subscriptionId).setIconTint(tint);

                    updateAllOptions();
                    update();
                }
            });

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });
            /// M:  @{
            mExt.hideSimEditorView(dialogLayout, mContext);
            /// @}
            builder.create().show();
        }

        private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
            private Context mContext;
            private int mResId;

            public SelectColorAdapter(
                Context context, int resource, String[] arr) {
                super(context, resource, arr);
                mContext = context;
                mResId = resource;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View rowView;
                final ViewHolder holder;
                Resources res = getResources();
                int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
                int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);

                /// M: for ALPS01972022 this is workaround solution. the icon show wrong for
                /// landscape
                //if (convertView == null) {
                    // Cache views for faster scrolling
                    rowView = inflater.inflate(mResId, null);
                    holder = new ViewHolder();
                    ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                    drawable.setIntrinsicHeight(iconSize);
                    drawable.setIntrinsicWidth(iconSize);
                    drawable.getPaint().setStrokeWidth(strokeWidth);
                    holder.label = (TextView) rowView.findViewById(R.id.color_text);
                    holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                    holder.swatch = drawable;
                    rowView.setTag(holder);
                //} else {
                //    rowView = convertView;
                //    holder = (ViewHolder) rowView.getTag();
                //}

                holder.label.setText(getItem(position));
                holder.swatch.getPaint().setColor(mTintArr[position]);
                holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
                holder.icon.setVisibility(View.VISIBLE);
                holder.icon.setImageDrawable(holder.swatch);
                return rowView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View rowView = getView(position, convertView, parent);
                final ViewHolder holder = (ViewHolder) rowView.getTag();

                if (mTintSelectorPos == position) {
                    holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
                } else {
                    holder.swatch.getPaint().setStyle(Paint.Style.STROKE);
                }
                holder.icon.setVisibility(View.VISIBLE);
                return rowView;
            }

            private class ViewHolder {
                TextView label;
                ImageView icon;
                ShapeDrawable swatch;
            }
        }

        /**
         * only for OP09 UIM/SIM changes.
         */
        private void changeTitle() {
            int subId = 0;

            if (mSubInfoRecord != null) {
                subId = mSubInfoRecord.getSubscriptionId();
            } else {
                subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            setTitle(String.format(mMiscExt.customizeSimDisplayString(
                                                getResources().getString(R.string.sim_editor_title),
                                                subId),
                                   (mSlotId + 1)));
        }

        /**
         * only for OP09 UIM/SIM changes.
         *
         * @param dialogLayout the layout of the dialog view.
         */
        private void changeSimNameTitle(View dialogLayout) {
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            if (mSubInfoRecord != null) {
                subId = mSubInfoRecord.getSubscriptionId();
            }

            TextView nameTitle = (TextView) dialogLayout.findViewById(R.id.sim_name_title);
            nameTitle.setText(mMiscExt.customizeSimDisplayString(
                                nameTitle.getText().toString(), subId));
            EditText nameText = (EditText) dialogLayout.findViewById(R.id.sim_name);
            nameText.setHint(mMiscExt.customizeSimDisplayString(
                                getResources().getString(R.string.sim_name_hint), subId));
        }

        /**
         * only for OP09 UIM/SIM changes.
         *
         * @param builder the AlertDialog builder.
         */
        private void changeEditorTitle(AlertDialog.Builder builder) {
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            if (mSubInfoRecord != null) {
                subId = mSubInfoRecord.getSubscriptionId();
            }
            builder.setTitle(String.format(mMiscExt.customizeSimDisplayString(
                                              getResources().getString(R.string.sim_editor_title),
                                              subId),
                                           (mSubInfoRecord.getSimSlotIndex() + 1)));
        }
    }

    // Returns the line1Number. Line1number should always be read from TelephonyManager since it can
    // be overridden for display purposes.
    private String getPhoneNumber(SubscriptionInfo info) {
        final TelephonyManager tm =
            (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1NumberForSubscriber(info.getSubscriptionId());
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    if (Utils.showSimCardTile(context)) {
                        SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.sim_settings;
                        result.add(sir);
                    }

                    return result;
                }
            };
            

    /*
     * M: New method please add below
     ************************************************************************
     */
    private ITelephonyEx mTelephonyEx;
    private SimHotSwapHandler mSimHotSwapHandler;
    private boolean mIsAirplaneModeOn = false;
    private IntentFilter mIntentFilter;
    private ISimManagementExt mExt;
    private static final boolean RADIO_POWER_OFF = false;
    private static final boolean RADIO_POWER_ON = true;
    private static final int MODE_PHONE1_ONLY = 1;
    private PhoneServiceStateHandler mStateHandler;

    // Receiver to handle different actions
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mSubReceiver action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                handleAirplaneModeBroadcast(intent);
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                handleDataConnectionStateChanged(intent);
            } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                updateCellularDataValues();
            } else if (isPhoneAccountAction(action)) {
                updateCallValues();
            } else if (isSimSwitchAction(action)) {
                updateSimPref();
            }
        }
    };

    private boolean isPhoneAccountAction(String action) {
        return action.equals(TelecomManagerEx.ACTION_DEFAULT_ACCOUNT_CHANGED) ||
                action.equals(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED);
    }
    private void handleDataConnectionStateChanged(Intent intent) {
        String apnTypeList = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
        /// M: just process default type data change, avoid unnecessary
        /// change broadcast
        if ((PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList))) {
            /// M: Auto open the other card's data connection.
            // when current card is radio off
            mExt.dealWithDataConnChanged(intent, isResumed());
            /// @}
        }
    }

    /**
     * When airplane mode is on, some parts need to be disabled for prevent some telephony issues
     * when airplane on.
     * Default data is not able to switch as may cause modem switch
     * SIM radio power switch need to disable, also this action need operate modem
     * @param airplaneOn airplane mode state true on, false off
     */
    private void handleAirplaneModeBroadcast(Intent intent) {
        mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.d(TAG, "air plane mode is = " + mIsAirplaneModeOn);
        updateSimSlotValues();
        updateCellularDataValues();
        removeItem();
    }

    private void init() {
        mTelephonyEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        mSimHotSwapHandler = SimHotSwapHandler.newInstance(getActivity());
        mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        Log.d(TAG, "init()... air plane mode is: " + mIsAirplaneModeOn);
        initIntentFilter();
        getActivity().registerReceiver(mSubReceiver, mIntentFilter);
        mExt = UtilsExt.getSimManagmentExtPlugin(getActivity());
        mMiscExt = UtilsExt.getMiscPlugin(getActivity());
        mStateHandler = new PhoneServiceStateHandler(getActivity());
        mStateHandler.addPhoneServiceStateListener(this);
    }

    /// Get whether sim switch still under operating, if no, need to keep data switching dialog
    private boolean isCapabilitySwitching() {
        boolean isSwitching = false;
        try {
            if (mTelephonyEx != null) {
                isSwitching = mTelephonyEx.isCapabilitySwitching();
            } else {
                Log.d(TAG, "mTelephonyEx is null, returen false");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException = " + e);
        }
        Log.d(TAG, "isSwitching = " + isSwitching);
        return isSwitching;
    }

    private boolean isSimSwitchAction(String action) {
        return action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE) ||
               action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
    }

    private void initIntentFilter() {
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        // For PhoneAccount
        mIntentFilter.addAction(TelecomManagerEx.ACTION_DEFAULT_ACCOUNT_CHANGED);
        mIntentFilter.addAction(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED);
        // For SIM Switch
        mIntentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
    }

    private void bindWithRadioPowerManager(SimPreference simPreference, SubscriptionInfo subInfo) {
        int subId = subInfo == null ? SubscriptionManager.INVALID_SUBSCRIPTION_ID : 
                                      subInfo.getSubscriptionId();
        RadioPowerManager radioMgr = new RadioPowerManager(getActivity());
        radioMgr.bindPreference(simPreference, subId);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSimHotSwapHandler.unregisterOnSubscriptionsChangedListener();
        mExt.unregisterObserver();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        getActivity().unregisterReceiver(mSubReceiver);
        mStateHandler.removePhoneServiceSateListener();
        super.onDestroy();
    }

    /**
     * whether radio switch finish about subId.
     * @param subId subId
     * @return true finish
     */
    private boolean isRadioSwitchComplete(int subId) {
        boolean isComplete = true;
        int slotId = SubscriptionManager.getSlotId(subId);
        if (SubscriptionManager.isValidSlotId(slotId)) {
            Bundle bundle = null;
            try {
                if (mTelephonyEx != null) {
                    bundle = mTelephonyEx.getServiceState(subId);
                } else {
                    Log.d(TAG, "mTelephonyEx is null, returen false");
                }
            } catch (RemoteException e) {
                isComplete = false;
                Log.d(TAG, "getServiceState() error, subId: " + subId);
                e.printStackTrace();
            }
            if (bundle != null) {
                ServiceState serviceState = ServiceState.newFromBundle(bundle);
                isComplete = isRadioSwitchComplete(subId, serviceState);
            }
        }
        Log.d(TAG, "isRadioSwitchComplete(" + subId + ")" + ", slotId: " + slotId
                + ", isComplete: " + isComplete);
        return isComplete;
    }

    private boolean isRadioSwitchComplete(final int subId, ServiceState state) {
        int slotId = SubscriptionManager.getSlotId(subId);
        boolean radiosState = getRadioStateForSlotId(slotId);
        Log.d(TAG, "soltId: " + slotId + ", radiosState is : " + radiosState);
        if (radiosState && (state.getState() != ServiceState.STATE_POWER_OFF)) {
            return true;
        } else if (state.getState() == ServiceState.STATE_POWER_OFF) {
            return true;
        }
        return false;
    }

    private boolean getRadioStateForSlotId(final int slotId) {
        if (getActivity() == null) {
            Log.d(TAG, "getRadioStateForSlotId()... activity is null");
            return false;
        }
        int currentSimMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean radiosState = ((currentSimMode & (MODE_PHONE1_ONLY << slotId)) == 0) ?
                RADIO_POWER_OFF : RADIO_POWER_ON;
        Log.d(TAG, "soltId: " + slotId + ", radiosState : " + radiosState);
        return radiosState;
    }

    private void handleRadioPowerSwitchComplete() {
        updateSimSlotValues();
        // M Auto open the other card's data connection. when current card is radio off
        mExt.showChangeDataConnDialog(this, isResumed());
    }

    private void updateSmsSummary(final Preference simPref) {
        int defaultSmsSubId = SubscriptionManager.getDefaultSmsSubId();
        if (defaultSmsSubId == Settings.System.SMS_SIM_SETTING_AUTO) {
            mExt.updateDefaultSIMSummary(simPref, defaultSmsSubId);
        } else {
            simPref.setSummary(R.string.sim_calls_ask_first_prefs_title);
        }
    }

    @Override
    public void onServiceStateChanged(ServiceState state, int subId) {
        Log.d(TAG, "PhoneStateListener:onServiceStateChanged: subId: " + subId
                + ", state: " + state);
        if (isRadioSwitchComplete(subId, state)) {
            handleRadioPowerSwitchComplete();
        }
    }

    private void removeItem() {
        //remove some item when in 4gds wifi-only 
        if(FeatureOption.MTK_PRODUCT_IS_TABLET){
            Preference sim_call_Pref = findPreference(KEY_CALLS);
            Preference sim_sms_Pref = findPreference(KEY_SMS);
            Preference sim_data_Pref = findPreference(KEY_CELLULAR_DATA);
            PreferenceCategory mPreferenceCategoryActivities = (PreferenceCategory) findPreference(KEY_SIM_ACTIVITIES);
            TelephonyManager mTelephonyManager = TelephonyManager.from(getActivity());
            boolean mIsVoiceCapable = mTelephonyManager.isVoiceCapable();
            boolean mIsSmsCapable = mTelephonyManager.isSmsCapable();
            if (!mIsSmsCapable && sim_sms_Pref!=null) {
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!mTelephonyManager.isMultiSimEnabled() && sim_data_Pref!=null && sim_sms_Pref!=null) {
                mPreferenceCategoryActivities.removePreference(sim_data_Pref);
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!mIsVoiceCapable && sim_call_Pref!=null) {
                mPreferenceCategoryActivities.removePreference(sim_call_Pref);
            }
            mExt.updateDefaultSettingsItem(mPreferenceCategoryActivities);
        }
    }

    /**
     * only for OP09 UIM/SIM changes.
     */
    private void changeSimActivityTitle() {
        PreferenceCategory preferenceCategoryActivities =
                (PreferenceCategory) findPreference(KEY_SIM_ACTIVITIES);
        preferenceCategoryActivities.setTitle(
                mMiscExt.customizeSimDisplayString(
                        preferenceCategoryActivities.getTitle().toString(),
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }

    /**
     * M: Replace SIM to SIM/UIM.
     */
    private void replaceSIMString() {
        if (mSimCards != null) {
            mSimCards.setTitle(mMiscExt.customizeSimDisplayString(
                    getString(R.string.sim_settings_title),
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        }
        getActivity().setTitle(
                mMiscExt.customizeSimDisplayString(getString(R.string.sim_settings_title),
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }
}
