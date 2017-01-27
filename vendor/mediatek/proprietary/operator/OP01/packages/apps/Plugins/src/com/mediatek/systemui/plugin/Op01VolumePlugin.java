package com.mediatek.systemui.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.systemui.ext.DefaultVolumePlugin;

/**
 * M: OP01 implementation of Plug-in definition of Volume Ext.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IVolumePlugin")
public class Op01VolumePlugin extends DefaultVolumePlugin {

    /**
     * Constructs a new Op01VolumePlugin instance with Context.
     *
     * @param context A Context object.
     */
    public Op01VolumePlugin(Context context) {
        super(context);
    }

    @Override
    public String customizeZenModeNoInterruptionsTitle(String orgText) {
        return getBaseContext().getString(R.string.zen_no_interruptions_with_warning);
    }
}
