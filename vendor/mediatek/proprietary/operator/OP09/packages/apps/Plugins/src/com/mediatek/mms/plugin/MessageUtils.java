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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * M: For OP09; Message Utils.
 */
public class MessageUtils {

    private static final int SINGLE_SIM_CARD = 1;
    private static final int DOUBLE_SIM_CARD = 2;

    private static final int GEMINI_SIM_1 = 0;

    private static final String TIMEZONE_ID_BEIJING = "Asia/Shanghai";

    private static final String TAG = "OP09MessageUtils";

    /**
     * M: get short time format.
     * @param context the Context.
     * @param time the time stamp.
     * @return the format string.
     */
    public static String getShortTimeString(Context context, long time) {
        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL
            | DateUtils.FORMAT_CAP_AMPM;
        formatFlags |= DateUtils.FORMAT_SHOW_TIME;
        return formatDateTime(context, time, formatFlags);
    }

    /**
     * M: formate the date and time.
     * @param context the Context.
     * @param time the time stamp.
     * @param formatFlags the format flag.
     * @return the formated string.
     */
    public static String formatDateTime(Context context, long time, int formatFlags) {
        if (!isInternationalRoamingStatus(context)) {
            Xlog.d(TAG, "formatDateTime Default");
            return DateUtils.formatDateTime(context, time, formatFlags);
        }
        int localNum = Settings.System.getInt(context.getContentResolver(),
            Settings.System.CT_TIME_DISPLAY_MODE, 0);
        if (localNum == 0) {
            Formatter f = new Formatter(new StringBuilder(50), Locale.CHINA);
            String str = DateUtils.formatDateRange(context, f, time, time, formatFlags,
                TIMEZONE_ID_BEIJING).toString();
            Xlog.d(TAG, "FormateDateTime  Time:" + time + "\t formatFlags:" + formatFlags
                    + "\tTimeZone:" + TIMEZONE_ID_BEIJING);
            return str;
        } else {
            Xlog.d(TAG, "FormateDateTime; time display mode: LOCAL");
            return DateUtils.formatDateTime(context, time, formatFlags);
        }
    }

    /**
     * M: check the first sim is whether in internation roaming status or not.
     * @param context the Context.
     * @return true: in international romaing. false : not in.
     */
    public static boolean isInternationalRoamingStatus(Context context) {
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        boolean isRoaming = false;
        int simCount = SubscriptionManager.from(context).getActiveSubscriptionInfoCount();
        /// M: Two SIMs inserted
        if (simCount == DOUBLE_SIM_CARD) {
            // isRoaming = isCdmaRoaming();
            isRoaming = telephonyManagerEx.isNetworkRoaming(GEMINI_SIM_1);
        } else if (simCount == SINGLE_SIM_CARD) {
            // One SIM inserted
            SubscriptionInfo sir = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoList().get(0);
            isRoaming = telephonyManagerEx.isNetworkRoaming(sir.getSimSlotIndex());
        } else { // Error: no SIM inserted
            Xlog.e(TAG, "There is no SIM inserted!");
        }
        Xlog.d(TAG, "isInternationalRoamingStatus:" + isRoaming);
        return isRoaming;
    }

    /**
     * M: check the subId sim is whether in internation roaming status or not.
     * @param context the Context.
     * @param subId the subId.
     * @return true: in international romaing. false : not in.
     */
    public static boolean isInternationalRoamingStatus(Context context, long subId) {
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        boolean isRoaming = false;
        int simCount = SubscriptionManager.from(context).getActiveSubscriptionInfoCount();

        if (simCount <= 0) {
            Xlog.e(TAG, "MessageUtils.isInternationalRoamingStatus(): Wrong subId!");
            return false;
        }
        isRoaming = telephonyManagerEx.isNetworkRoaming(SubscriptionManager.getSlotId((int) subId));
        Xlog.d(TAG, "isInternationalRoamingStatus() isRoaming: " + isRoaming);
        return isRoaming;
    }

    /**
     * M: check the subId sim is whether in internation roaming status or not.
     * @param context the Context.
     * @param subId the sim subid.
     * @return true: in international romaing. false : not in.
     */
    public static boolean isInternationalRoamingStatusBySubId(Context context, long subId) {
        SubscriptionInfo sir = getSimInfoBySubId(context, subId);
        if (sir == null) {
            return false;
        }
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        boolean isRoaming = false;
        isRoaming = telephonyManagerEx.isNetworkRoaming(sir.getSimSlotIndex());
        Xlog.d(TAG, "isInternationalRoamingStatus() isRoaming: " + isRoaming);
        return isRoaming;
    }

    /**
     * M: remove year date from date formated string.
     * @param allFormatStr the formated date string.
     * @return the reformated string.
     */
    private static String removeYearFromFormat(String allFormatStr) {
        if (TextUtils.isEmpty(allFormatStr)) {
            return allFormatStr;
        }
        String finalStr = "";
        int yearIndex = allFormatStr.indexOf("y");
        int monthIndex = allFormatStr.indexOf("M");
        int dayIndex = allFormatStr.indexOf("d");
        if (yearIndex >= 0) {
            if (yearIndex > monthIndex) {
                finalStr = allFormatStr.substring(0, yearIndex).trim();
            } else if (monthIndex > dayIndex) {
                finalStr = allFormatStr.substring(dayIndex, allFormatStr.length()).trim();
            } else {
                finalStr = allFormatStr.substring(monthIndex, allFormatStr.length()).trim();
            }
            if (finalStr.endsWith(",") || finalStr.endsWith("/") || finalStr.endsWith(".")
                || finalStr.endsWith("-")) {
                finalStr = finalStr.substring(0, finalStr.length() - 1);
            }
            return finalStr;
        } else {
            return allFormatStr;
        }
    }

    /**
     * M: format date or time stamp with the system settings.
     * @param context the Context.
     * @param when the date or time stamp.
     * @param fullFormat true: show time. false: not show time.
     * @return the formated sting.
     */
    public static String formatDateOrTimeStampStringWithSystemSetting(Context context, long when,
            boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL
            | DateUtils.FORMAT_CAP_AMPM;
        SimpleDateFormat sdf = (SimpleDateFormat) (DateFormat.getDateFormat(context));
        String allDateFormat = sdf.toPattern();

        if (fullFormat) {
            String timeStr = getShortTimeString(context, when);
            String dateStr = DateFormat.format(allDateFormat, when).toString();
            formatFlags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
            String defaultDateStr = formatDateTime(context, when, formatFlags);
            if (defaultDateStr.indexOf(":") > 5) {
                return dateStr + " " + timeStr;
            }
            return timeStr + " " + dateStr;
        }
        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            formatFlags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
            return DateFormat.format(allDateFormat, when).toString();
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            formatFlags |= DateUtils.FORMAT_SHOW_DATE;
            if ((now.yearDay - then.yearDay) == 1) {
                return context.getString(R.string.str_yesterday);
            } else {
            String dayMonthFormatStr = removeYearFromFormat(allDateFormat);
            return DateFormat.format(dayMonthFormatStr, when).toString();
            }
        } else if (0 <= (now.toMillis(false) - then.toMillis(false))
            && (now.toMillis(false) - then.toMillis(false)) < 60000) {
            return context.getString(R.string.time_now);
        } else {
            // Otherwise, if the message is from today, show the time.
            formatFlags |= DateUtils.FORMAT_SHOW_TIME;
        }
        return formatDateTime(context, when, formatFlags);
    }

    /**
     * M: handle Miessed Pasts for cascaed sms.
     * @param parts the smsMessage parts.
     * @return the message content string.
     */
    public static String handleMissedParts(SmsMessage[] parts) {
        if (parts == null || parts.length <= 0) {
            Xlog.e(TAG, "[fake invalid message array");
            return null;
        }

        int totalPartsNum = parts[0].getUserDataHeader().concatRef.msgCount;

        String[] fakeContents = new String[totalPartsNum];
        for (SmsMessage msg : parts) {
            int seq = msg.getUserDataHeader().concatRef.seqNumber;
            Xlog.d(TAG, "[fake add segment " + seq);
            fakeContents[seq - 1] = msg.getDisplayMessageBody();
        }
        for (int i = 0; i < fakeContents.length; ++i) {
            if (fakeContents[i] == null) {
                Xlog.d(TAG, "[fake replace segment " + (i + 1));
                fakeContents[i] = "(...)";
            }
        }

        StringBuilder body = new StringBuilder();
        for (String s : fakeContents) {
            body.append(s);
        }
        return body.toString();
    }

    /**
     * M: the sim is cdma sim.
     * @param context the Context.
     * @param subId the subid.
     * @return true: is cdma sim; false: not.
     */
    public static boolean isCDMAType(Context context, int subId) {
        String phoneType = TelephonyManagerEx.getDefault().getIccCardType(subId);
        if (phoneType == null) {
            Xlog.d(TAG, "[isCDMAType]: phoneType = null");
            return false;
        }
        Xlog.d(TAG, "[isCDMAType]: phoneType = " + phoneType);
        return phoneType.equalsIgnoreCase("CSIM") || phoneType.equalsIgnoreCase("UIM")
            || phoneType.equalsIgnoreCase("RUIM");
    }

    /**
     * M: get sim info by sub id.
     * @param ctx the context.
     * @param subId the sim's subId.
     * @return the sim Information record.
     */
    public static SubscriptionInfo getSimInfoBySubId(Context ctx, long subId) {
        return SubscriptionManager.from(ctx).getActiveSubscriptionInfo((int) subId);
    }

    /**
     * Get the first SubscriptionInfo with the same slotId.
     * @param ctx the Context.
     * @param slotId the slotId.
     * @return the subInforRecord.
     */
    public static SubscriptionInfo getFirstSimInfoBySlotId(Context ctx, int slotId) {
        return SubscriptionManager.from(ctx).getActiveSubscriptionInfoForSimSlotIndex(slotId);
    }

    /**
    * resize the given bitmap,to make subicon bitmap to fit mms.
    * @param context the context.
    * @param origenBitmap the bitmap get from subscriptionInfo.
    * @return the resized bitmap fit mms icon size.
    */
    public static Bitmap resizeBitmap(Context context, Bitmap origenBitmap) {
        Drawable noSimDrawable = context.getResources().getDrawable(
                R.drawable.sim_indicator_no_sim_mms);
        BitmapDrawable bd = (BitmapDrawable) noSimDrawable;
        Bitmap newBitmap = bd.getBitmap();
        float scaleWidth = ((float) newBitmap.getWidth()) / origenBitmap.getWidth();
        float scaleHeight = ((float) newBitmap.getHeight()) / origenBitmap.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        int resizedW = origenBitmap.getWidth();
        int resizedH = origenBitmap.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(origenBitmap, 0, 0, resizedW, resizedH, matrix, true);
        return bitmap;
    }
}
