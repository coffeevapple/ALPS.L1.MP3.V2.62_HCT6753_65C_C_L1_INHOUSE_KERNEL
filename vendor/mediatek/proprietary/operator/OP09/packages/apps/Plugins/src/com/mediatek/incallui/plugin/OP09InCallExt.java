package com.mediatek.incallui.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultInCallExt;
import com.mediatek.incallui.ext.IInCallExt;
import com.mediatek.op09.plugin.R;

/**
 * callcard extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.incallui.ext.IInCallExt")
public class OP09InCallExt extends DefaultInCallExt {
    private static final String TAG = "OP09InCallExt";
    private Context mContext;

    /**
     * Incall plugin code.
     * @param context the context.
     */
    public OP09InCallExt(Context context) {
        mContext = context;
    }

    /**
     * replace string.
     * @param defaultString defualt string
     * @param hint string value
     * @return string value
     */
    @Override
    public String replaceString(String defaultString, String hint) {
        if (!TextUtils.isEmpty(hint) &&
                hint.equals(IInCallExt.HINT_ERROR_MSG_SIM_ERROR)) {
            Resources res = mContext.getResources();
            String tag = res.getString(R.string.callFailed_simError_ct);
            Log.i(TAG, "OP09InCallExt [replaceString] return tag : " + tag);
            return tag;
        }
        Log.i(TAG, "OP09InCallExt [replaceString] return defualt");
        return defaultString;
    }

    /**
     * replace object.
     * @param defaultObject the defualt value
     * @param hint string hint value
     * @return matched object value
     */
    @Override
    public Object replaceValue(Object defaultObject, String hint) {
        if (!TextUtils.isEmpty(hint) &&
                hint.equals(IInCallExt.HINT_BOOLEAN_SHOW_ACCOUNT_DIALOG)) {
            Log.i(TAG, "OP09InCallExt [replaceValue] return false");
            return false;
        } else {
            return defaultObject;
        }
    }
}

