package com.mediatek.settings.fuelgauge;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.mediatek.settings.ext.IBatteryExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;

public class PowerUsageExts {

    private static final String TAG = "PowerUsageSummary";

    private static final String KEY_BACKGROUND_POWER_SAVING = "background_power_saving";

    private Context mContext;
    private PreferenceGroup mAppListGroup;
    private SwitchPreference mBgPowerSavingPrf;

    // Power saving mode feature plug in
    private IBatteryExt mBatteryExt;

    public PowerUsageExts(Context context, PreferenceGroup appListGroup) {
        mContext = context;
        mAppListGroup = appListGroup;
        // Battery plugin initialization
        mBatteryExt = UtilsExt.getBatteryExtPlugin(context);

    }

    // init power usage extends items
    public void initPowerUsageExtItems() {
        // Power saving mode for op09
        mBatteryExt.loadPreference(mContext, mAppListGroup);

        // background power saving
        if (FeatureOption.MTK_BG_POWER_SAVING_SUPPORT
                && FeatureOption.MTK_BG_POWER_SAVING_UI_SUPPORT) {
            mBgPowerSavingPrf = new SwitchPreference(mContext);
            mBgPowerSavingPrf.setKey(KEY_BACKGROUND_POWER_SAVING);
            mBgPowerSavingPrf.setTitle(R.string.bg_power_saving_title);
            mBgPowerSavingPrf.setOrder(-4);
            mBgPowerSavingPrf.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.BG_POWER_SAVING_ENABLE, 1) != 0);
            mAppListGroup.addPreference(mBgPowerSavingPrf);
        }
    }

    // on click
    public boolean onPowerUsageExtItemsClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (KEY_BACKGROUND_POWER_SAVING.equals(preference.getKey())) {
            if (preference instanceof SwitchPreference) {
                SwitchPreference pref = (SwitchPreference) preference;
                int bgState = pref.isChecked() ? 1 : 0;
                Log.d(TAG, "background power saving state: " + bgState);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.BG_POWER_SAVING_ENABLE, bgState);
                if (mBgPowerSavingPrf != null) {
                    mBgPowerSavingPrf.setChecked(pref.isChecked());
                }
            }
            // If user click on PowerSaving preference just return here
            return true;
        } else if (mBatteryExt.onPreferenceTreeClick(preferenceScreen, preference)) {
            return true;
        }
        return false;
    }
}
