package com.mediatek.phone.plugin;

import com.mediatek.common.PluginImpl;
import com.mediatek.phone.ext.DefaultPhoneMiscExt;

/**
 * CT OP09 Phone misc feature.
 */
@PluginImpl(interfaceName = "com.mediatek.phone.ext.IPhoneMiscExt")
public class OP09PhoneMiscExt extends DefaultPhoneMiscExt {
  @Override
    public boolean publishBinderDirectly() {
        return true;
    }

    /**
     * Whether need to remove "Ask First" item from call with selection list.
     *
     * @return true if need to remove it.
     */
    @Override
    public boolean needRemoveAskFirstFromSelectionList() {
        return true;
    }
}
