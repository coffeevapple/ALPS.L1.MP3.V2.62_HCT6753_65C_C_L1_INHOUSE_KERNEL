package com.mediatek.engineermode.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.wifi.EmPerformanceWrapper;
/**
 * a broadcast Receiver of EM boot.
 * @author: mtk81238
 */
public class EmBootupReceiver extends BroadcastReceiver {

    private static final String TAG = "EM/BootupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            onBootupCompleted(context, intent);
        }
    }

    private void onBootupCompleted(Context context, Intent intent) {
        Elog.d(TAG, "Start onBootupCompleted");
        if (FeatureSupport.isSupported(FeatureSupport.FK_C2K_SUPPORT)) {
            onBootupUsbTethering(context);
        }
        if (ChipSupport.isFeatureSupported(ChipSupport.MTK_WLAN_SUPPORT)) {
            if (EmPerformanceWrapper.isPerfSettingEnabled(context)) {
                EmBootStartService.requestStartService(context, new WifiSpeedUpBootHandler());
            }
        }
    }

    private void onBootupUsbTethering(Context context) {
        if (UsbTetheringBootHandler.isSupportBootUsbTethering()) {
            EmBootStartService.requestStartService(context, new UsbTetheringBootHandler());
        }
    }
}
