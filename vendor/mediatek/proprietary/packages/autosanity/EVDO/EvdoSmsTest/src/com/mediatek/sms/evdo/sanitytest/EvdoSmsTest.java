package com.mediatek.sms.evdo.sanitytest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.view.View;

import com.jayway.android.robotium.solo.Solo;

import org.w3c.dom.Node;

/**
 * M: Sms Test Class for EVDO OP09.
 */
public class EvdoSmsTest extends InstrumentationTestCase {
    private Solo mSolo;

    private static final String TAG = "EvdoSmsTest";

    private static final String CONFIG_FILE = "/sdcard/sanity_configure.xml";

    private static final String SMS_SEND_TABLE_URI = "content://sms/sent";

    private static final String SMS_INBOX_TABLE_URI = "content://sms/inbox";

    private static String sSim1Number = null;

    private static String sSim2Number = null;

    private Context mContext = null;

    private Activity mActivity = null;

    private Instrumentation mInst = null;

    @Override
    public void setUp() throws Exception {
        mSolo = new Solo(getInstrumentation());
        mContext = getInstrumentation().getTargetContext();
        SystemClock.sleep(3000);
        mInst = getInstrumentation();
        XmlParser mParser = new XmlParser(CONFIG_FILE);
        Node node = mParser.getRootNode();
        Node sim1 = mParser.getNodeByName(node, "sim1");
        Node sim2 = mParser.getNodeByName(node, "sim2");
        sSim1Number = mParser.getNodeValue(sim1);
        sSim2Number = mParser.getNodeValue(sim2);

        Log.i(TAG, "sim1: " + sSim1Number + " sim2:" + sSim2Number);
    }

    @Override
    public void tearDown() throws Exception {
        try {
            // Robotium will finish all the activities that have been opened
            mSolo.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        super.tearDown();
    }

    /**
     * M: Test sms1 send.
     * @throws Exception the exception.
     */
    public void testSms1Send() throws Exception {
        final String testName = "test_sms1_send";
        final String message = "[" + testName + "] ";
        int mSendCnt = 0;
        int mReceiveCnt = 0;
        // test start
        Log.v(TAG, message + "test start");
        // Send sms from sim2 to sim1
        Log.i(TAG, "Send message from sim1 to sim2");
        mSendCnt = getCnt(Uri.parse(SMS_SEND_TABLE_URI), sSim2Number);
        mReceiveCnt = getCnt(Uri.parse(SMS_INBOX_TABLE_URI), sSim1Number);
        Log.i(TAG, "mSendCnt: " + mSendCnt + " mReceiveCnt: " + mReceiveCnt);
        sendSMS("this is a test message send to SIM2 ", sSim2Number, 1);
        assertTrue("Send message failed from sim1 to sim2", isSuccessfully(mSendCnt, Uri
                .parse(SMS_SEND_TABLE_URI), sSim2Number));
        assertTrue("Receive message failed from sim1 to sim2", isSuccessfully(mReceiveCnt, Uri
                .parse(SMS_INBOX_TABLE_URI), sSim1Number));
        SystemClock.sleep(2000);
        // goToHome();
        Log.v(TAG, message + "test end");
        assertTrue(testName, true);
    }

    /**
     * M: test method.
     * @throws Exception the exception.
     */
    public void testSms2send() throws Exception {
        final String testName = "test_sms2_send";
        final String message = "[" + testName + "] ";
        int mSendCnt = 0;
        int mReceiveCnt = 0;
        assertTrue(message + " sim1_number not null ", sSim1Number != null);
        assertTrue(message + " sim2_number not null ", sSim2Number != null);
        // test start
        Log.v(TAG, message + "test start");
        SystemClock.sleep(1000);
        // Send sms from sim1 to sim2
        Log.i(TAG, "Send message from sim2 to sim1");
        mSendCnt = getCnt(Uri.parse(SMS_SEND_TABLE_URI), sSim1Number);
        mReceiveCnt = getCnt(Uri.parse(SMS_INBOX_TABLE_URI), sSim2Number);
        Log.i(TAG, "mSendCnt: " + mSendCnt + " mReceiveCnt: " + mReceiveCnt);
        sendSMS("this is a test message send to SIM1", sSim1Number, 2);
        assertTrue("Send message failed from sim2 to sim1", isSuccessfully(mSendCnt, Uri
                .parse(SMS_SEND_TABLE_URI), sSim2Number));
        assertTrue("Receive message failed from sim2 to sim1", isSuccessfully(mReceiveCnt, Uri
                .parse(SMS_INBOX_TABLE_URI), sSim1Number));
        SystemClock.sleep(2000);
        Log.v(TAG, message + "test end");
        assertTrue(testName, true);
    }

    /**
     * M: Send sms.
     * @param message message content.
     * @param simNumber sim number.
     * @param listIndex 1: sim1-->sim2; 2:sim2-->sim1.
     */
    public void sendSMS(String message, String simNumber, int listIndex) {
        sendMMSIntent(simNumber, message);
        SystemClock.sleep(3000);
        // Get current Activity
        mActivity = mSolo.getCurrentActivity();
        SystemClock.sleep(2000);
        // Click send button
        int defaultSubId = SubscriptionManager.getDefaultSmsSubId();
        int slotId = 0;
        if (defaultSubId < 0) {
            slotId = 0;
        } else {
            SubscriptionInfo sir = SubscriptionManager.from(mActivity).getSubscriptionInfo(
                defaultSubId);
            if (sir == null) {
                slotId = 0;
            } else {
                slotId = sir.getSimSlotIndex();
            }
        }

        int sendId = mActivity.getResources().getIdentifier("send_button_sms", "id",
            "com.android.mms");
        if (listIndex == 1) {
            if (slotId == 0) {
                sendId = mActivity.getResources().getIdentifier("ct_send_button_big", "id",
                    "com.android.mms");
            } else {
                sendId = mActivity.getResources().getIdentifier("ct_send_button_small", "id",
                    "com.android.mms");
            }
        } else {
            if (slotId == 0) {
                sendId = mActivity.getResources().getIdentifier("ct_send_button_small", "id",
                    "com.android.mms");
            } else {
                sendId = mActivity.getResources().getIdentifier("ct_send_button_big", "id",
                    "com.android.mms");
            }
        }
        View send = mSolo.getView(sendId);
        Log.i(TAG, "Send button is: " + send);
        mSolo.clickOnView(send);
        SystemClock.sleep(2000);
    }

    /**
     * M: send Sms Intent.
     * @param number sim number.
     * @param text message text.
     */
    public void sendMMSIntent(String number, String text) {
        // Attach text message and file by intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("address", number);
        intent.putExtra("sms_body", text);
        intent.putExtra("compose_mode", false);
        intent.putExtra("exit_on_sent", true);
        mInst.getContext().startActivity(intent);
    }

    /**
     * M: get sent/receive count.
     * @param uri the message uri.
     * @param telNo sim num.
     * @return the counter.
     */
    public int getCnt(Uri uri, String telNo) {
        String telNo2 = "";
        if (telNo.startsWith("+86")) {
            telNo2 = telNo.substring(3);
        } else {
            telNo2 = "+86" + telNo;
        }
        int cnt = -1;
        ContentResolver cr = mContext.getContentResolver();
        Cursor c = cr.query(uri, null, "address = ? or address = ?", new String[] {telNo, telNo2},
            null);
        if (c != null) {
            Log.i(TAG, telNo + " Get cursor successfully");
            cnt = c.getCount();
            Log.i(TAG, telNo + " cnt: " + cnt);
            c.close();
        } else {
            Log.i(TAG, "cursor is null");
        }
        return cnt;
    }

    /**
     * M: Judge the result is sucess or not.
     * @param oldCount the old count.
     * @param uri the message uri.
     * @param telNo the sim number.
     * @return true: success; false: failed.
     */
    public boolean isSuccessfully(int oldCount, Uri uri, String telNo) {
        for (int i = 0; i < 12; i++) {
            SystemClock.sleep(5000);
            if ((oldCount + 1) <= getCnt(uri, telNo)) {
                Log.i(TAG, telNo + " Send/Receive message successfully");
                return true;
            }
        }
        return false;
    }
}
