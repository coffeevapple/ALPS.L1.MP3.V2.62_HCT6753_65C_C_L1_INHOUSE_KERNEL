/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.mediatek.settings.cdma;

import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardTile;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.settings.FeatureOption;

public class CdmaUtils {

    private static final String TAG = "CdmaUtils";

    public static final int CT_4G_SIM = 0;
    public static final int CT_3G_SIM = 1;
    public static final int NOT_CT_SIM = 2;

    private static final String TWO_CDMA_INTENT = "com.mediatek.settings.cdma.TWO_CDMA_POPUP";

    private static final String[]  PROPERTY_RIL_FULL_UICC_TYPE = {
        "gsm.ril.fulluicctype",
        "gsm.ril.fulluicctype.2",
        "gsm.ril.fulluicctype.3",
        "gsm.ril.fulluicctype.4",
    };
    /**
     * Get whether a CT cdam card inserted refer to {@link SvlteUiccUtils}
     * @return
     */
    public static boolean isCdmaCardInsert() {
        int simCount = TelephonyManager.getDefault().getSimCount();
        boolean hasCdmaCards =false;
        Log.d(TAG,"simCount = " + simCount);
        for (int slotId = 0 ; slotId < simCount; slotId ++) {
            if (isCdmaCardType(slotId)) {
                hasCdmaCards = true;
                break;
            }
        }
        return hasCdmaCards;
    }
    
    public static int getSIMCardType(int slotId) {
        SvlteUiccUtils utils = SvlteUiccUtils.getInstance();
        boolean isCtCdmaCard = utils.isRuimCsim(slotId);
        int type = NOT_CT_SIM;
        if (isCtCdmaCard) {
            type = utils.isUsimWithCsim(slotId) ? CT_4G_SIM : CT_3G_SIM;
        }
        Log.d(TAG,"type = " + type);
        return type;
    }
    
    
    private static boolean isCdmaCardType(int slotId) {
        SvlteUiccUtils util = SvlteUiccUtils.getInstance();
        boolean isCdmaCard= util.isRuimCsim(slotId);
        Log.d(TAG,"slotId = " + slotId + " isCdmaCard = " + isCdmaCard);
        return isCdmaCard;
    }

    /**
     * get the c2k Modem Slot.
     * @return 1 means sim1,2 means sim2
     */
    public static int getExternalModemSlot() {
        return CdmaFeatureOptionUtils.getExternalModemSlot();
    }

    public static boolean phoneConstantsIsSimOne(SubscriptionInfo subscriptionInfo) {
        if (subscriptionInfo != null) {
            int slotId = SubscriptionManager.getSlotId(subscriptionInfo.getSubscriptionId());
            Log.i(TAG, "phoneConstantsIsSimOne slotIsSimOne = "
                    + (PhoneConstants.SIM_ID_1 == slotId));
            return PhoneConstants.SIM_ID_1 == slotId;
        }
        return false;
    }

    /**
     * add for c2k. get data usage for CDMA LTE.
     * @param template template.
     * @param subId subId
     */
    public static void fillTemplateForCdmaLte(NetworkTemplate template, int subId) {
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            final TelephonyManagerEx teleEx = TelephonyManagerEx.getDefault();
            final String svlteSubscriberId = teleEx.getSubscriberIdForLteDcPhone(subId);
            if (!(TextUtils.isEmpty(svlteSubscriberId)) && svlteSubscriberId.length() > 0){
                Log.d(TAG, "bf:" + template);
                template.addMatchSubscriberIds(svlteSubscriberId);
                Log.d(TAG, "af:" + template);
            }
        }
    }
    /**
     * Launch the dialog activity if under SVLTE and two new SIM detect.
     * @param context Context
     * @param simDetectNum New SIM number detected
     */
    public static void startCdmaWaringDialog(Context context, int simDetectNum) {
        boolean isC2KP2Support = CdmaFeatureOptionUtils.isC2KWorldPhoneP2Support();
        Log.d(TAG, "isC2KP2Support = " + isC2KP2Support + " simDetectNum = " + simDetectNum);
        if (isC2KP2Support) {
            boolean twoCdmaInsert = true;
            if (simDetectNum > 1) {
                for (int i = 0 ; i < simDetectNum ; i ++) {
                    if (getSIMCardType(i) == NOT_CT_SIM) {
                        twoCdmaInsert = false;
                    }
                }
            } else if (simDetectNum == 1) {
                //In fwk limitation, one 3G, one 4G cards insert, can not detect 3G, so
                //AP work around to handle this case.
                int simCount = TelephonyManager.getDefault().getSimCount();
                for (int i = 0; i < simCount; i ++) {
                    if (getSIMCardType(i) == NOT_CT_SIM) {
                        twoCdmaInsert = false;
                    }
                }
            }
            Log.d(TAG,"twoCdmaInsert = " + twoCdmaInsert);
            if (twoCdmaInsert) {
                Intent intent = new Intent(TWO_CDMA_INTENT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
}
