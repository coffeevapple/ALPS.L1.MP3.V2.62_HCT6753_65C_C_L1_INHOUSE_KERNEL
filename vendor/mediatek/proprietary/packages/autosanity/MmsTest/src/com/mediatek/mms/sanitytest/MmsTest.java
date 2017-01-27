package com.mediatek.mms.sanitytest;

import com.jayway.android.robotium.solo.Solo;

import android.test.InstrumentationTestCase;
import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.content.ContentResolver;
import android.os.SystemClock;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import com.android.internal.telephony.PhoneConstants;
import android.os.SystemProperties;
import java.util.ArrayList;
import java.util.List;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
public class MmsTest extends InstrumentationTestCase {

    private Solo solo;
    private static final String TAG = "MMSTest";
    private static final String CONFIG_FILE = "/sdcard/com.mtk.telephony.mms.regression.xml";
    private static final String MMS_SEND_TABLE_URI = "content://mms/sent";
    private static final String MMS_INBOX_TABLE_URI = "content://mms/inbox";
    private Context mContext = null;
    private Instrumentation mInst = null;
    private Activity mActivity = null;
    private static boolean hasSetDefaultMms = false;
    private static boolean IS_GEMINI = true;

    private int test_count = 0;
    private String sim1_number = null;
    private String sim2_number = null;
    private String text_message = null;
    private String subject_text = null;
    private String attach_file = null;
    private final String TEST_NAME = "test_mms_send";
    private final String MESSAGE = "[" + TEST_NAME + "] ";
    private boolean mMobileDataEnabledBefore = false;
    /*
     * For gemini: if it's strict, it should send and receive two messages successfully.
     * Else it's success if send and receive one message.
     */
    private boolean mIsStrict = true;
    // how much time it need: 10 minutes / 2 second
    private int mTimes = 300;

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation());
        mContext = getInstrumentation().getTargetContext();
        mInst = getInstrumentation();
    }

    @Override
    public void tearDown() throws Exception {

        try {
            // Robotium will finish all the activities that have been opened
            solo.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        super.tearDown();
    }

    public void test_mms_send() throws Exception {
        // parameter index
        final int TEST_COUNT = 0;
        final int SIM1_NUMBER = 1;
        final int SIM2_NUMBER = 2;
        final int TEXT_MESSAGE = 3;
        final int SUBJECT_TEXT = 4;
        final int ATTACH_FILE = 5;
        final int STRICT_NODE = 6;

        final String PARAMETERS[] = { "test_count", "sim1_number",
                "sim2_number", "text_message", "subject_text", "attach_file", "strict" };

        // create HashMap to handle parameter string
        Map<String, String> params = new HashMap<String, String>();
        getConfigParameters(PARAMETERS, params, TEST_NAME);

        // check param
        assertTrue(MESSAGE + "test_count is null", params
                .get(PARAMETERS[TEST_COUNT]) != null);
        test_count = Integer.parseInt(params.get(PARAMETERS[TEST_COUNT]));
        assertTrue(MESSAGE + " test_count < 0 ", test_count >= 0);

        assertTrue(MESSAGE + "sim1_number is null", params
                .get(PARAMETERS[SIM1_NUMBER]) != null);
        sim1_number = params.get(PARAMETERS[SIM1_NUMBER]);
        assertTrue(MESSAGE + " sim1_number not null ", sim1_number != null);

        //check the test is gemini or not.
        List<SubscriptionInfo> list = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
        assertTrue(MESSAGE + " sub list not null ", list != null);
        assertTrue(MESSAGE + " sub list size > 0 ", list.size() > 0);

        if ((params.get(PARAMETERS[SIM2_NUMBER]) == null) || (list.size() == 1)) {
            IS_GEMINI = false;
        } else {
            IS_GEMINI = true;
            sim2_number = params.get(PARAMETERS[SIM2_NUMBER]);
        }

        
        if (params.get(PARAMETERS[STRICT_NODE]) != null) {
            mIsStrict = params.get(PARAMETERS[STRICT_NODE]).equals("yes") ? true : false;
            Log.d(TAG, "mIsStrict: " + mIsStrict);
        }

        assertTrue(MESSAGE + "text_message is null", params
                .get(PARAMETERS[TEXT_MESSAGE]) != null);
        text_message = params.get(PARAMETERS[TEXT_MESSAGE]);
        assertTrue(MESSAGE + " text_message not null ", text_message != null);

        assertTrue(MESSAGE + "subject_text is null", params
                .get(PARAMETERS[SUBJECT_TEXT]) != null);
        subject_text = params.get(PARAMETERS[SUBJECT_TEXT]);
        assertTrue(MESSAGE + " subject_text not null ", subject_text != null);

        assertTrue(MESSAGE + "attach_file is null", params
                .get(PARAMETERS[ATTACH_FILE]) != null);
        attach_file = params.get(PARAMETERS[ATTACH_FILE]);
        assertTrue(MESSAGE + " attach_file not null ", attach_file != null);

        if (IS_GEMINI) {
            mms_send_gemini();
        } else {
            mms_send_single_card();
        }
    }

    private void mms_send_single_card()  throws Exception {
        int failCount = 0;
        int sendCnt = 0;
        int receiveCnt = 0;
        // test start
        Log.v(TAG, MESSAGE + "test start: mms_send_single_card");

        for (int i = 0; i < test_count; i++) {
            // Send mms to self
            Log.i(TAG, "Send mms to self");
            sendCnt = getCnt(Uri.parse(MMS_SEND_TABLE_URI));
            receiveCnt = getCnt(Uri.parse(MMS_INBOX_TABLE_URI));
            Log.i(TAG, "Send Before, SendCount: " + sendCnt + ", receiveCnt: " + receiveCnt);
            List<SubscriptionInfo> list = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
            assertTrue(MESSAGE + "mms_send_single_card: sub 1 is not null ", list.get(0) != null);
            sendMMS(sim1_number, subject_text, text_message, attach_file, list.get(0).getSubscriptionId());

            if (!isSuccessfully(sendCnt, Uri.parse(MMS_SEND_TABLE_URI))) {
                Log.i(TAG, "Can not send message to self");
                failCount++;
            }
            if (!isSuccessfully(receiveCnt, Uri.parse(MMS_INBOX_TABLE_URI))) {
                Log.i(TAG, "Can not receive message from self");
                failCount++;
            }

            assertTrue("Send and Receive message failed", failCount == 0);
        }

        Log.v(TAG, MESSAGE + "test end");
        assertTrue(TEST_NAME, true);
    }

    /*
     * Gemini: sim1 send to sim2, sim2 send to sim1
     */
    public void mms_send_gemini() throws Exception {
        int failCount = 0;
        int sendCnt1 = 0;
        int receiveCnt1 = 0;
        int sendCnt2 = 0;
        int receiveCnt2 = 0;
        // test start
        // try {
        Log.v(TAG, MESSAGE + "test start: mms_send_gemini");

        for (int i = 0; i < test_count; i++) {
            // Send mms from sim1 to sim2
            Log.i(TAG, "Send mms from sim1 to sim2");
            sendCnt1 = getCnt(Uri.parse(MMS_SEND_TABLE_URI));
            receiveCnt1 = getCnt(Uri.parse(MMS_INBOX_TABLE_URI));
            Log.i(TAG, "Send Before, SendCount1: " + sendCnt1 + ", receiveCnt1: " + receiveCnt1);
            List<SubscriptionInfo> list = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
            assertTrue(MESSAGE + "mms_send_gemini: sub 1 is not null ", list.get(0) != null);
            assertTrue(MESSAGE + "mms_send_gemini: sub 2 is not null ", list.get(1) != null);
            sendMMS(sim2_number, subject_text, text_message, attach_file, list.get(0).getSubscriptionId());

            SystemClock.sleep(5000);

            Log.i(TAG, "Send mms from sim2 to sim1");
            sendCnt2 = getCnt(Uri.parse(MMS_SEND_TABLE_URI));
            receiveCnt2 = getCnt(Uri.parse(MMS_INBOX_TABLE_URI));
            Log.i(TAG, "Send Before, SendCount2: " + sendCnt2 + ", receiveCnt2: " + receiveCnt2);
            assertTrue(MESSAGE + "mms_send_gemini: sub 2 is not null ", list.get(1) != null);
            sendMMS(sim1_number, subject_text, text_message, attach_file, list.get(1).getSubscriptionId());

            // check how many has sent successful
            if (!isSuccessfully(sendCnt1, Uri.parse(MMS_SEND_TABLE_URI))) {
                Log.i(TAG, "Send mms failed");
                failCount++;
            }
            if (failCount == 0 && !isSuccessfully(receiveCnt1, Uri.parse(MMS_INBOX_TABLE_URI))) {
                Log.i(TAG, "Receive mms failed");
                failCount++;
            }

            assertTrue("Send and Receive mms failed", failCount == 0);

        }

        Log.v(TAG, MESSAGE + "test end");
        assertTrue(TEST_NAME, true);
    }

    public void sendMMSIntent(String number, String subject, String text,
            String attach, int subId) {
        Log.v(TAG, "sendMMSIntent: sub id = " + subId);
        // Attach text message and file by intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setClassName("com.android.mms",
                "com.android.mms.ui.ComposeMessageActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("address", number);
        intent.putExtra("subject", subject);
        intent.putExtra("sms_body", text);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(attach)));
        intent.setType("image/jpg");
        intent.putExtra("compose_mode", false);
        intent.putExtra("exit_on_sent", true);
        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
        mInst.getContext().startActivity(intent);

    }

    // Send mms
    public void sendMMS(String number, String subject, String text, String attach, int subId) {

        // Start MMS Activity
        sendMMSIntent(number, subject, text, attach, subId);
        SystemClock.sleep(5000);
        mInst.waitForIdleSync();
        // Get current Activity
        mActivity = solo.getCurrentActivity();
        SystemClock.sleep(2000);
        mInst.waitForIdleSync();

        // Click sim button
        int Send_id = mActivity.getResources().getIdentifier("send_button_mms",
                "id", "com.android.mms");
        View Send = solo.getView(Send_id);
        solo.clickOnView(Send);
        SystemClock.sleep(5000);
        mInst.waitForIdleSync();
        // Click sim to send
        Log.v(TAG, "sendMMS: IS_GEMINI = " + IS_GEMINI);
        mActivity.finish();
        mInst.waitForIdleSync();
    }

    // get sent/receive count
    public int getCnt(Uri uri) {

        int cnt = -1;
        ContentResolver cr = mContext.getContentResolver();
        Cursor c = cr.query(uri, null, null, null, null);
        if (c != null) {
            cnt = c.getCount();
            Log.i(TAG, "Get cursor successfully: " + cnt);
            c.close();
        } else {
            Log.i(TAG, "cursor is null");
        }
        return cnt;
    }

    public boolean isSuccessfully(int count_old, Uri uri) {

        while (mTimes > 0) {
            mTimes--;
            SystemClock.sleep(2000);
            if (mIsStrict && IS_GEMINI) {
                if ((count_old + 2) <= getCnt(uri)) {
                    Log.i(TAG, "Send/Receive two MMS successfully");
                    return true;
                }
            } else {
                if ((count_old + 1) <= getCnt(uri)) {
                    Log.i(TAG, "Send/Receive one MMS successfully");
                    return true;
                }
            }
        }
        return false;
    }

    private void getConfigParameters(String[] PARAMETERS,
            Map<String, String> params, String testName) {
        for (int i = 0; i < PARAMETERS.length; i++) {
            params.put(PARAMETERS[i], null);
        }
        Log.v(TAG, "[" + testName + "] " + "params.size() = " + params.size());

        File f = new File(CONFIG_FILE);
        assertTrue(testName + " test file not exists: " + CONFIG_FILE, f
                .exists());

        // test suite
        XmlParser parser = new XmlParser(CONFIG_FILE);
        Node testSuiteNode = parser.getRootNode();
        String testSuiteName = parser.getAttrValue(testSuiteNode, "name");
        assertTrue(testName + "get testsuite name is null",
                testSuiteName != null);
        Log.v(TAG, "[" + testName + "] " + "testSuiteName = " + testSuiteName);

        // test case
        Node testCaseNode = null;
        String testCaseName = null;
        NodeList nodeList_testCase = parser.getNodeList(testSuiteNode,
                "TestCase");
        assertTrue(testName + "nodeList is null", nodeList_testCase != null);
        for (int i = 0; i < nodeList_testCase.getLength(); i++) {
            testCaseNode = nodeList_testCase.item(i);
            String temp_testCaseName = parser
                    .getAttrValue(testCaseNode, "name");
            Log.v(TAG, "[" + testName + "] " + "testCaseName = "
                    + temp_testCaseName);
            if (0 == temp_testCaseName.compareTo(TAG)) {
                testCaseName = temp_testCaseName;
                break;
            }
        }
        assertTrue(testName + "testCaseName is null", testCaseName != null);

        // test
        NodeList nodeList = parser.getNodeList(testCaseNode, "Test");
        assertTrue(testName + "nodeList is null", nodeList != null);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node testNode = nodeList.item(i);
            String temp_testName = parser.getAttrValue(testNode, "name");
            Log.v(TAG, "[" + testName + "] " + "testName = " + temp_testName);
            if (0 == temp_testName.compareTo(testName)) {
                for (int j = 0; j < params.size(); j++) {
                    String attrValue = parser.getAttrValue(parser
                            .getNodeByName(testNode, PARAMETERS[j]), "value");
                    params.put(PARAMETERS[j], attrValue);
                    Log.v(TAG, "[" + testName + "] " + "param = "
                            + PARAMETERS[j] + "  value = "
                            + params.get(PARAMETERS[j]));
                }
                break;
            }
        }
    }
}
