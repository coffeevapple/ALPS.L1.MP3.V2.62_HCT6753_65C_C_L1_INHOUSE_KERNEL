package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import java.util.ArrayList;
import java.util.List;

/* Dummy implmentation , do nothing */
public class DefaultWifiSettingsExt implements IWifiSettingsExt {
    private static final String TAG = "DefaultWifiSettingsExt";

    public void registerPriorityObserver(ContentResolver contentResolver) {
    }
    public void unregisterPriorityObserver(ContentResolver contentResolver) {
    }
    public void setLastConnectedConfig(WifiConfiguration config) {
    }
    public void setLastPriority(int priority) {
    }
    public void updatePriority() {
    }
    public  void updateContextMenu(ContextMenu menu, int menuId, DetailedState state) {

    }

    public void setCategory(PreferenceCategory trustPref, PreferenceCategory configedPref,
            PreferenceCategory newPref) {
    }
    public void emptyCategory(PreferenceScreen screen) {
        screen.removeAll();
    }
    public void emptyScreen(PreferenceScreen screen) {
        screen.removeAll();
    }
    public boolean isTustAP(String ssid, int security) {
        return false;
    }
    public void refreshCategory(PreferenceScreen screen) {
    }
    public int getAccessPointsCount(PreferenceScreen screen) {
        return screen.getPreferenceCount();
    }
    public void adjustPriority() {
    }
    public void recordPriority(int selectPriority) {
    }
    public void setNewPriority(WifiConfiguration config) {
    }
    public void updatePriorityAfterSubmit(WifiConfiguration config) {
    }
    public void disconnect(int networkId) {
    }
    public void updatePriorityAfterConnect(int networkId) {
    }
    public void addPreference(PreferenceScreen screen, Preference preference, boolean isConfiged) {
        if (screen != null) {
            screen.addPreference(preference);
        }
    }

    public Preference getPreference(PreferenceScreen preferenceScreen, int index) {
        return preferenceScreen.getPreference(index);
    }

    public void addCategories(PreferenceScreen screen) {

    }

    public List<PreferenceGroup> getPreferenceCategory(PreferenceScreen screen) {
        List<PreferenceGroup> preferenceCategoryList = new ArrayList<PreferenceGroup>();
        preferenceCategoryList.add(screen);
        return preferenceCategoryList;
    }
}
