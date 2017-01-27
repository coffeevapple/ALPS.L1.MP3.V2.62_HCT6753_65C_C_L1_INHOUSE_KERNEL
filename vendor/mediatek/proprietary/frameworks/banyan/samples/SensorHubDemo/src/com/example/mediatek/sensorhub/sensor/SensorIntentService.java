package com.example.mediatek.sensorhub.sensor;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.PowerManager;
import android.util.Log;

import java.util.List;

import com.mediatek.sensorhub.ActionDataResult;
import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.sensorhub.DataCell;

import com.example.mediatek.sensorhub.Config;
import com.example.mediatek.sensorhub.R;


/**
 * IntentService class that receives and handles SensorHub events.
 */
public class SensorIntentService extends IntentService {
    private static final String TAG = "SensorIntentService";
    private static final boolean DEBUG_LOG = Config.ENABLE_DEBUG_LOG;
    private SoundPool mSounds;
    private int mSoundStreamId;
    private int mSoundId;
    private PowerManager.WakeLock mWakelock;

    public SensorIntentService() {
        super("SensorIntentService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mWakelock && mWakelock.isHeld()) {
            if (DEBUG_LOG) Log.d(TAG, "onDestroy: release wake lock.");
            mWakelock.release();
        }
    }

    /**
     * Handles SensorHub events delivered to it.
     *
     * @param intent Intent instance starts this service. It contains SensorHub event type and
     *               data snapshot of that event.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        acquireWakeLock();

        if (!ActionDataResult.hasResult(intent)) {
            Log.w(TAG, "onHandleIntent: intent has no action data result.");
            return;
        }

        // extracts data snapshot of contexts that constructing the SensorHub condition
        ActionDataResult result = ActionDataResult.extractResult(intent);
        List<DataCell> datalist = result.getData();
        for (DataCell data : datalist) {
            // do not check the previous value of SensorHub context
            if (data.isPrevious()) {
                continue;
            }
            // gets the index of context
            int idx = data.getIndex();
            if (DEBUG_LOG) Log.d(TAG, "onHandleIntent: dataIndex = " + idx);
            switch (idx) {
            case ContextInfo.Pickup.VALUE:
                if (DEBUG_LOG) Log.d(TAG, "onHandleIntent: TYPE_PICKUP");
                playSound();
                break;
            case ContextInfo.Shake.VALUE:
                if (DEBUG_LOG) Log.i(TAG, "onHandleIntent: TYPE_SHAKE");
                playSound();
                break;
            case ContextInfo.Facing.FACE_DOWN:
                if (DEBUG_LOG) Log.d(TAG, "onHandleIntent: TYPE_FACEDOWN");
                playSound();
                break;
            case ContextInfo.Carry.IN_POCKET:
                if (DEBUG_LOG) Log.d(TAG, "onHandleIntent: TYPE_INPOCKET");
                playSound();
                break;
            case ContextInfo.Pedometer.TOTAL_COUNT:
                if (DEBUG_LOG) Log.d(TAG, "onHandleIntent: TYPE_PEDOMETER");
                playSound();
                break;
            case ContextInfo.UserActivity.CURRENT_STATE:
                if (DEBUG_LOG) Log.d(TAG, "onHandleIntent: TYPE_ACTIVITY");
                playSound();
                break;
                }
            }
    }

    private void initSoundPool() {
        mSounds = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundId = mSounds.load(this, R.raw.notify_sound, 0/*priority*/);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Log.e(TAG, "initSoundPool: InterruptedException", ex);
        }
    }

    private void playSound() {
        if (mSounds == null) {
            initSoundPool();
        }
        mSounds.stop(mSoundStreamId);
        mSoundStreamId = mSounds.play(mSoundId, 1/*leftVolume*/, 1/*rightVolume*/, 1/* priortiy */, 0/* loop */, 1.0f/* rate */);

    }

    private void acquireWakeLock() {
        if (null == mWakelock) {
            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            mWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!mWakelock.isHeld()) {
            mWakelock.acquire();
        }
    }
}
