package com.mediatek.browser.plugin;

import android.content.Context;
import android.os.Build;

import com.mediatek.browser.ext.DefaultBrowserSettingExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.custom.CustomProperties;
import com.mediatek.op09.plugin.R;
import com.mediatek.storage.StorageManagerEx;
import com.mediatek.xlog.Xlog;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserSettingExt")
public class Op09BrowserSettingExt extends DefaultBrowserSettingExt {

    private static final String TAG = "Op09BrowserSettingExt";

    private static final String DEFAULT_DOWNLOAD_DIRECTORY_OP09 = "/storage/sdcard0/Download";
    private static final String DEFAULT_DOWNLOAD_FOLDER_OP09 = "/Download";

    private Context mContext;

    public Op09BrowserSettingExt(Context context) {
        super();
        mContext = context;
    }

    public String getCustomerHomepage() {
        Xlog.i(TAG, "Enter: " + "getCustomerHomepage" + " --OP09 implement");
        return mContext.getResources().getString(R.string.homepage_base_site_navigation);
    }

    public String getDefaultDownloadFolder() {
        Xlog.i(TAG, "Enter: " + "getDefaultDownloadFolder()" + " --OP09 implement");
        String defaultDownloadPath = DEFAULT_DOWNLOAD_DIRECTORY_OP09;
        String defaultStorage = StorageManagerEx.getDefaultPath();
        if (null != defaultStorage) {
            defaultDownloadPath = defaultStorage + DEFAULT_DOWNLOAD_FOLDER_OP09;
        }
        Xlog.v(TAG, "device default storage is: " + defaultStorage +
                " defaultPath is: " + defaultDownloadPath);
        return defaultDownloadPath;
    }

    /**
     * Customize the user agent string.
     * @param defaultUA the default user agent string
     * @return the customized user agent string
     */
    public String getOperatorUA(String defaultUA) {
        Xlog.i(TAG, "Enter: " + "getOperatorUA, default UA: " + defaultUA + " --OP09 implement");
        String op09UA = defaultUA;
        String manufacturer = CustomProperties.getString(CustomProperties.MODULE_BROWSER,
                                CustomProperties.MANUFACTURER);
        if (defaultUA != null && defaultUA.length() > 0
            && manufacturer != null && manufacturer.length() > 0) {
            String newModel = manufacturer + "-" + Build.MODEL;
            if (!defaultUA.contains(newModel)) {
                op09UA = defaultUA.replace(Build.MODEL, newModel);
            }
        }
        Xlog.i(TAG, "Exit: " + "getOperatorUA, OP09UA: " + op09UA);
        return op09UA;
    }

}
