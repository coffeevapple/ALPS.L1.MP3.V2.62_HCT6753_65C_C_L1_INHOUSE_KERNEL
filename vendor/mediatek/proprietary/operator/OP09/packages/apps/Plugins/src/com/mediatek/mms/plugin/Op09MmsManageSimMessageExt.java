/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultMmsManageSimMessageExt;
import com.mediatek.xlog.Xlog;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * M: Op09MmsManageSimMessageExt.
 */
@PluginImpl(interfaceName = "com.mediatek.mms.ext.IMmsManageSimMessageExt")
public class Op09MmsManageSimMessageExt extends DefaultMmsManageSimMessageExt {

    private static final String TAG = "OP09/OP09MmsManageSimMessageExt";

    private Context mContext = null;

    /**
     * M: The Constructor.
     * @param context the Context.
     */
    public Op09MmsManageSimMessageExt(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * M: Is whether the activated message or not.
     * @param index the message's id.
     * @return ture: activated message. false: no.
     */
    private boolean isUnactivatedMessage(int index) {
        int temp = (index & (0x01 << 10));
        return (temp == (0x01 << 10));
    }

    @Override
    public boolean isInternationalCard(int subId) {
        SubscriptionInfo sir = MessageUtils.getSimInfoBySubId(mContext, subId);
        if (sir == null || sir.getSimSlotIndex() != 0) {
            Xlog.d(TAG, "[isInternationalCard],failed. Just return false.");
            return false;
        }
        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_CDMA_CARD_TYPE);
        Intent intent = mContext.registerReceiver(null, intentFilter);
        if (intent == null) {
            Xlog.d(TAG, "[isInternationalCard]:failed. intent == null;");
            return false;
        }
        Bundle bundle = intent.getExtras();
        IccCardConstants.CardType cardType = (IccCardConstants.CardType) bundle
                .get(TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);
        boolean isDualSim = cardType == IccCardConstants.CardType.CT_UIM_SIM_CARD;
        Xlog.d(TAG, "[isInternationalCard]:" + isDualSim);
        return isDualSim;
    }

    @Override
    public boolean canBeOperated(Cursor cursor) {
        if (cursor == null) {
            return false;
        }
        try {
            int index = cursor.getInt(cursor.getColumnIndex("index_on_icc"));
            Xlog.d(TAG, "canBeOperated: index:" + index);
            return !isUnactivatedMessage(index);
        } catch (SQLiteException e) {
            Xlog.e(TAG, "error to canBeOperated");
        }
        return true;
    }

    @Override
    public boolean hasIncludeUnoperatedMessage(Iterator<Entry<String, Boolean>> it) {
        // TODO Auto-generated method stub
        if (it == null) {
            return false;
        }
        while (it.hasNext()) {
            Entry<String, Boolean> entry = (Entry<String, Boolean>) it.next();
            if (entry.getValue()) {
                if (isUnactivatedMessage(Integer.parseInt(entry.getKey()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String[] filterUnoperatedMsgs(String[] simMsgIndex) {
        // TODO Auto-generated method stub
        if (simMsgIndex == null || simMsgIndex.length < 1) {
            return simMsgIndex;
        }
        String[] temp = new String[simMsgIndex.length];
        int index = 0;
        for (String msgIndex : simMsgIndex) {
            if (!isUnactivatedMessage(Integer.parseInt(msgIndex))) {
                temp[index] = msgIndex;
                index++;
            }
        }
        return temp;
    }

    @Override
    public Uri getAllContentUriForInternationalCard(int subId) {
        int slotId = MessageUtils.getSimInfoBySubId(mContext, subId).getSimSlotIndex();
        if (slotId == 0) {
            return Uri.parse("content://sms/icc_international");
        } else if (slotId == 1) {
            return Uri.parse("content://sms/icc2_international");
        }
        return null;
    }

}
