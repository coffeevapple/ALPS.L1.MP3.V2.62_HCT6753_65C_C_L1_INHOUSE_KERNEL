package com.mediatek.mms.evdo.sanitytest;

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
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * M: Mms Test Class for EVDO OP09.
 */
public class EvdoMmsTest extends InstrumentationTestCase {
    private Solo mSolo;

    private static final String TAG = "MMSTest";

    private static final String CONFIG_FILE = "/sdcard/com.mtk.telephony.mms.regression.xml";

    private static final String MMS_SEND_TABLE_URI = "content://mms/sent";

    private static final String MMS_INBOX_TABLE_URI = "content://mms/inbox";

    private Context mContext = null;

    private Instrumentation mInst = null;

    private Activity mActivity = null;

    @Override
    public void setUp() throws Exception {
        mSolo = new Solo(getInstrumentation());
        mContext = getInstrumentation().getTargetContext();
        mInst = getInstrumentation();
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
     * M: test mms method.
     * @throws Exception exception.
     */
    public void testMmsSend() throws Exception {
        final String testName = "test_mms_send";
        final String message = "[" + testName + "] ";
        int mSendCnt = 0;
        int mReceiveCnt = 0;
        int failCount = 0;
        // parameter index
        final int testCountIndex = 0;
        final int sim1NumberIndex = 1;
        final int sim2NumberIndex = 2;
        final int testMessageIndex = 3;
        final int subJectTextIndex = 4;
        final int attachFileIndex = 5;
        final String parameters[] = {"test_count", "sim1_number", "sim2_number", "text_message",
            "subject_text", "attach_file"};
        // create HashMap to handle parameter string
        Map<String, String> params = new HashMap<String, String>();
        getConfigParameters(parameters, params, testName);
        int testCount = Integer.parseInt(params.get(parameters[testCountIndex]));
        assertTrue(message + "sim1_number is null",
            params.get(parameters[sim1NumberIndex]) != null);
        String sim1Number = params.get(parameters[sim1NumberIndex]);
        assertTrue(message + " sim2Number not null ", sim1Number != null);
        assertTrue(message + "textMessage is null",
            params.get(parameters[sim2NumberIndex]) != null);
        String sim2Number = params.get(parameters[sim2NumberIndex]);
        assertTrue(message + " sim2_number not null ", sim2Number != null);
        assertTrue(message + "text_message is null",
            params.get(parameters[testMessageIndex]) != null);
        String textMessage = params.get(parameters[testMessageIndex]);
        assertTrue(message + " text_message not null ", textMessage != null);
        assertTrue(message + "subject_text is null",
            params.get(parameters[subJectTextIndex]) != null);
        String subjectText = params.get(parameters[subJectTextIndex]);
        assertTrue(message + " subject_text not null ", subjectText != null);
        assertTrue(message + "attach_file is null",
            params.get(parameters[attachFileIndex]) != null);
        String attachFile = params.get(parameters[attachFileIndex]);
        assertTrue(message + " attach_file not null ", attachFile != null);
        // test start
        Log.v(TAG, message + "test start");
        for (int i = 0; i < testCount; i++) {
            // Send mms from sim1 to sim2
            Log.i(TAG, "Send mms from sim1 to sim2");
            mSendCnt = getCnt(Uri.parse(MMS_SEND_TABLE_URI));
            mReceiveCnt = getCnt(Uri.parse(MMS_INBOX_TABLE_URI));
            sendMMS(sim2Number, subjectText, textMessage, attachFile, 1);
            if (!isSuccessfully(mSendCnt, Uri.parse(MMS_SEND_TABLE_URI))) {
                Log.i(TAG, "Can not send message from sim1 to sim2");
                failCount++;
            }
            if (!isSuccessfully(mReceiveCnt, Uri.parse(MMS_INBOX_TABLE_URI))) {
                Log.i(TAG, "Can not receive message from sim1 to sim2");
                failCount++;
            }
            SystemClock.sleep(5000);
            Log.i(TAG, "Send mms from sim2 to sim1");
            mSendCnt = getCnt(Uri.parse(MMS_SEND_TABLE_URI));
            mReceiveCnt = getCnt(Uri.parse(MMS_INBOX_TABLE_URI));
            sendMMS(sim1Number, subjectText, textMessage, attachFile, 2);
            if (!isSuccessfully(mSendCnt, Uri.parse(MMS_SEND_TABLE_URI))) {
                Log.i(TAG, "Can not send message from sim2 to sim1");
                failCount++;
            }
            if (!isSuccessfully(mReceiveCnt, Uri.parse(MMS_INBOX_TABLE_URI))) {
                Log.i(TAG, "Can not receive message from sim2 to sim1");
                failCount++;
            }
            assertTrue("Send and Receive message failed", failCount == 0);
        }
        Log.v(TAG, message + "test end");
        assertTrue(testName, true);
    }

    /**
     * M: Send Mms Intent.
     * @param number sim number.
     * @param subject mms's subject.
     * @param text message text.
     * @param attach attachment file.
     */
    public void sendMMSIntent(String number, String subject, String text, String attach) {
        // Attach text message and file by intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("address", number);
        intent.putExtra("subject", subject);
        intent.putExtra("sms_body", text);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(attach)));
        intent.setType("image/jpg");
        intent.putExtra("compose_mode", false);
        intent.putExtra("exit_on_sent", true);
        mInst.getContext().startActivity(intent);
    }

    /**
     * M: Send mms.
     * @param number sim Number.
     * @param subject the subject of mms.
     * @param text the message text.
     * @param attach the attach file.
     * @param listIndex 1: 1->2; 2: 2->1.
     */
    public void sendMMS(String number, String subject, String text, String attach, int listIndex) {
        // Start MMS Activity
        sendMMSIntent(number, subject, text, attach);
        SystemClock.sleep(3000);
        // Get current Activity
        mActivity = mSolo.getCurrentActivity();
        SystemClock.sleep(2000);
        // Click send button
        int defaultSubId =  SubscriptionManager.getDefaultSmsSubId();
        int slotId = 0;
        if (defaultSubId < 0) {
            slotId = 0;
        } else {
            SubscriptionInfo subInfo = SubscriptionManager.from(mActivity).getSubscriptionInfo(
                defaultSubId);
            if (subInfo == null) {
                slotId = 0;
            } else {
                slotId = subInfo.getSimSlotIndex();
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
        mSolo.clickOnView(send);
        SystemClock.sleep(2000);
        SystemClock.sleep(5000);
        mActivity.finish();
    }

    /**
     * M: get sent/receive count.
     * @param uri the message uri.
     * @return the count of the uri's content.
     */
    public int getCnt(Uri uri) {
        int cnt = -1;
        ContentResolver cr = mContext.getContentResolver();
        Cursor c = cr.query(uri, null, null, null, null);
        if (c != null) {
            Log.i(TAG, "Get cursor successfully");
            cnt = c.getCount();
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
     * @return true: success; false: failed.
     */
    public boolean isSuccessfully(int oldCount, Uri uri) {
        for (int i = 0; i < 150; i++) {
            SystemClock.sleep(2000);
            if ((oldCount + 1) <= getCnt(uri)) {
                Log.i(TAG, "Send/Receive MMS successfully");
                return true;
            }
        }
        return false;
    }

    /**
     * M: get config parameters for test.
     * @param parameters the tag parameters.
     * @param params out parameters.
     * @param testName just a tag for log.
     */
    private void getConfigParameters(String[] parameters, Map<String, String> params,
            String testName) {
        for (int i = 0; i < parameters.length; i++) {
            params.put(parameters[i], null);
        }
        Log.v(TAG, "[" + testName + "] " + "params.size() = " + params.size());
        File f = new File(CONFIG_FILE);
        assertTrue(testName + " test file not exists: " + CONFIG_FILE, f.exists());
        // test suite
        XmlParser parser = new XmlParser(CONFIG_FILE);
        Node testSuiteNode = parser.getRootNode();
        String testSuiteName = parser.getAttrValue(testSuiteNode, "name");
        Log.v(TAG, "[" + testName + "] " + "testSuiteName = " + testSuiteName);
        // test case
        Node testCaseNode = null;
        String testCaseName = null;
        NodeList nodeListTestCase = parser.getNodeList(testSuiteNode, "TestCase");
        for (int i = 0; i < nodeListTestCase.getLength(); i++) {
            testCaseNode = nodeListTestCase.item(i);
            String tempTestCaseName = parser.getAttrValue(testCaseNode, "name");
            Log.v(TAG, "[" + testName + "] " + "testCaseName = " + tempTestCaseName);
            if (0 == tempTestCaseName.compareTo(TAG)) {
                testCaseName = tempTestCaseName;
                break;
            }
        }
        // test
        NodeList nodeList = parser.getNodeList(testCaseNode, "Test");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node testNode = nodeList.item(i);
            String tempTestName = parser.getAttrValue(testNode, "name");
            Log.v(TAG, "[" + testName + "] " + "testName = " + tempTestName);
            if (0 == tempTestName.compareTo(testName)) {
                for (int j = 0; j < params.size(); j++) {
                    String attrValue = parser.getAttrValue(parser.getNodeByName(testNode,
                        parameters[j]), "value");
                    params.put(parameters[j], attrValue);
                    Log.v(TAG, "[" + testName + "] " + "param = " + parameters[j] + "  value = "
                        + params.get(parameters[j]));
                }
                break;
            }
        }
    }
}
