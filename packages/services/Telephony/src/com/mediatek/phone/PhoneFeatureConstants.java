/*
* Copyright (C) 2011-2014 Mediatek.inc.
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
package com.mediatek.phone;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.util.Log;

import com.android.phone.PhoneGlobals;

public class PhoneFeatureConstants {

    public static final class FeatureOption {
        private static final String TAG = "FeatureOption";
        private static final String MTK_DUAL_MIC_SUPPORT = "MTK_DUAL_MIC_SUPPORT";
        private static final String MTK_DUAL_MIC_SUPPORT_on = "MTK_DUAL_MIC_SUPPORT=true";
        private final static String ONE = "1";
        // C2K 5M(CLLWG)
        private final static String C2K_5M = "CLLWG";
        public static boolean isMtkDualMicSupport() {
            String state = null;
            AudioManager audioManager = (AudioManager)
                    PhoneGlobals.getInstance().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                state = audioManager.getParameters(MTK_DUAL_MIC_SUPPORT);
                Log.d(state, "isMtkDualMicSupport(): state: " + state);
                if (state.equalsIgnoreCase(MTK_DUAL_MIC_SUPPORT_on)) {
                    return true;
                }
            }
            return false;
        }

        public static boolean isMtkFemtoCellSupport() {
            boolean isSupport = ONE.equals(
                    SystemProperties.get("ro.mtk_femto_cell_support")) ? true : false;
            Log.d(TAG, "isMtkFemtoCellSupport(): " + isSupport);
            return isSupport;
        }

        public static boolean isMtk3gDongleSupport() {
            boolean isSupport = ONE.equals(
                    SystemProperties.get("ro.mtk_3gdongle_support")) ? true : false;
            Log.d(TAG, "isMtk3gDongleSupport()" + isSupport);
            return isSupport;
        }

        public static boolean isMtkLteSupport() {
            boolean isSupport = ONE.equals(
                    SystemProperties.get("ro.mtk_lte_support")) ? true : false;
            Log.d(TAG, "isMtkLteSupport(): " + isSupport);
            return isSupport;
        }

        public static boolean isMtkC2k5M() {
            boolean isSupport = C2K_5M.equalsIgnoreCase(
                    SystemProperties.get("ro.mtk.c2k.om.mode")) ? true : false;
            Log.d(TAG, "isMtkC2k5M(): " + isSupport);
            return isSupport;
        }
    }
}
