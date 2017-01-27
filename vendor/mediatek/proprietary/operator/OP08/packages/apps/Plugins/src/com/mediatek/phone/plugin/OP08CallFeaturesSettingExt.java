package com.mediatek.phone.plugin;

import android.util.Log;
import android.content.Context;
import android.content.Intent;
import com.mediatek.common.PluginImpl;

import com.mediatek.phone.ext.DefaultCallFeaturesSettingExt;


@PluginImpl(interfaceName="com.mediatek.phone.ext.ICallFeaturesSettingExt")
public class OP08CallFeaturesSettingExt extends DefaultCallFeaturesSettingExt {
    private static final String TAG = "OP08CallFeaturesSettingExt";
    private static final String SETTINGS_CHANGED_OR_SS_COMPLETE = "com.mediatek.op.telephony.SETTINGS_CHANGED_OR_SS_COMPLETE";
    private static final int MESSAGE_SET_SS = 1; 
    @Override
    public void resetImsPdnOverSSComplete(Context context, int msg) {
        Log.d(TAG,"resetImsPdnOverSSComplete");
        if (msg == MESSAGE_SET_SS) {
            Intent intent = new Intent(SETTINGS_CHANGED_OR_SS_COMPLETE);
            context.sendBroadcast(intent);
            Log.d(TAG,"Intent Broadcast "+SETTINGS_CHANGED_OR_SS_COMPLETE);
        }
    }
}
