package com.example.mediatek.sensorhub.sensor;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.compatibility.sensorhub.SensorHubSupport;
import com.mediatek.sensorhub.SensorHubManager;
import com.mediatek.sensorhub.Action;
import com.mediatek.sensorhub.Condition;
import com.mediatek.sensorhub.ContextInfo;
import com.example.mediatek.sensorhub.Config;

/**
 * Utility class that provides simple SensorHub usage such as adding and removing requests by types.
 */
public class SensorHubClient {
    private static final String TAG = "SensorHubClient";
    private static final boolean DEBUG_LOG = Config.ENABLE_DEBUG_LOG;

    private static final int PICKUP_VALUE = 1;
    private static final int SHAKE_VALUE = 1;
    private static final int FACE_DOWN_VALUE = 1;
    private static final int IN_POCKET_VALUE = 1;
    private static final int PEDOMETER_STEP_COUNT = 10;
    private static final int ACTIVITY_CONFIDENCE = 60;

    private Context mContext;
    private SensorHubManager mSensorHubManager;

    public SensorHubClient(Context context) {
        mContext = context;
        if (mContext != null) {
            mSensorHubManager = (SensorHubManager)mContext.getSystemService(
                    SensorHubManager.SENSORHUB_SERVICE);
        }
    }

    /**
     * Checks out whether the SensorHub feature is supported or not on this platform.
     *
     * @return True if the SensorHub is supported; otherwise, false.
     */
    public static boolean isSensorHubAvailable() {
        return SensorHubSupport.isSensorHubFeatureAvailable();
    }

    /**
     * Adds a specific type of request.
     *
     * @param type The type of request to be added.
     * @return The request ID of
     */
    public int addRequest(int type) {
        if (DEBUG_LOG) Log.d(TAG, "addRequest: type=" + type);
        if (mSensorHubManager == null) {
            Log.w(TAG, "addRequest: Null SensorHubManager!");
            return -1;
        }
        if (mSensorHubManager == null || !mSensorHubManager.getContextList().contains(type)) {
            Log.w(TAG, "addRequest: SensorHubManager does not contain this type!");
            return -1;
        }
        
        Condition.Builder builder = new Condition.Builder();
        Condition condition = null;
        switch (type) {
        case ContextInfo.Type.PICK_UP:
            // creates a condition representing pickup == 1
            condition = builder.createCondition(ContextInfo.Pickup.VALUE, Condition.OP_EQUALS, PICKUP_VALUE);
            break;
        case ContextInfo.Type.SHAKE:
            // creates a condition representing shake == 1
            condition = builder.createCondition(ContextInfo.Shake.VALUE, Condition.OP_EQUALS, SHAKE_VALUE);
            break;
        case ContextInfo.Type.FACING:
            // creates a condition representing face_down == 1
            condition = builder.createCondition(ContextInfo.Facing.FACE_DOWN, Condition.OP_EQUALS, FACE_DOWN_VALUE);
            break;
        case ContextInfo.Type.CARRY:
            // creates a condition representing in_pocket == 1
            condition = builder.createCondition(ContextInfo.Carry.IN_POCKET, Condition.OP_EQUALS, IN_POCKET_VALUE);
            break;
        case ContextInfo.Type.PEDOMETER:
            // creates a condition representing step_count >= 10
            condition = builder.createCondition(
                    ContextInfo.Pedometer.TOTAL_COUNT,
                    Condition.OP_GREATER_THAN_OR_EQUALS,
                    PEDOMETER_STEP_COUNT);
            break;
        case ContextInfo.Type.USER_ACTIVITY:
            // creates a condition representing (user_activity == on_foot && confidence >= 60%)
            Condition c1 = builder.createCondition(
                    ContextInfo.UserActivity.CURRENT_STATE,
                    Condition.OP_EQUALS,
                    ContextInfo.UserActivity.State.ON_FOOT);
            Condition c2 = builder.createCondition(
                    ContextInfo.UserActivity.CONFIDENCE,
                    Condition.OP_GREATER_THAN_OR_EQUALS,
                    ACTIVITY_CONFIDENCE);
            condition = builder.combineWithAnd(c1, c2);
            break;
        default:
            break;
        }
        // sets up the PendingIntent instance that starts the SensorIntentService
        Intent intent = new Intent(mContext, SensorIntentService.class);
        PendingIntent callbackIntent = PendingIntent.getService(
                mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // creates the action to be performed when its related condition is met based on
        // PendingIntent instance callbackIntent
        Action action = new Action(callbackIntent, true/*repeatable*/, false/*onConditionChanged*/);
        // adds the request
        int rid = mSensorHubManager.requestAction(condition, action);
        if (DEBUG_LOG) Log.d(TAG, "addRequest: rid=" + rid);
        return rid;
    }

    /**
     * Cancels the request specified by the request ID.
     *
     * @param rid Request ID returned by SensorHubManager.requestAction function.
     */
    public void cancelRequest(int rid) {
        if (mSensorHubManager == null || !mSensorHubManager.cancelAction(rid)) {
            Log.w(TAG, "cancelRequest failed: " + rid);
        }
    }
}
