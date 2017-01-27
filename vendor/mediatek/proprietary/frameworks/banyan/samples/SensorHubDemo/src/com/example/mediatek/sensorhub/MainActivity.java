package com.example.mediatek.sensorhub;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediatek.sensorhub.enabler.FacedownEnabler;
import com.example.mediatek.sensorhub.enabler.InPocketEnabler;
import com.example.mediatek.sensorhub.enabler.PedometerEnabler;
import com.example.mediatek.sensorhub.enabler.PickupEnabler;
import com.example.mediatek.sensorhub.enabler.ShakeEnabler;
import com.example.mediatek.sensorhub.enabler.UserActivityEnabler;
import com.example.mediatek.sensorhub.sensor.SensorHubClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends PreferenceActivity {
    private final static String TAG = "MainActivity";
    private final static boolean DEBUG_LOG = Config.ENABLE_DEBUG_LOG;
    private final static String BUNDLE_HEADERS = "bundle_headers";
    private List<Header> mHeaders;

    private PickupEnabler mPickupEnabler;
    private ShakeEnabler mShakeEnabler;
    private FacedownEnabler mFacedownEnabler;
    private InPocketEnabler mInPocketEnabler;
    private PedometerEnabler mPedometerEnabler;
    private UserActivityEnabler mUserEnabler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(BUNDLE_HEADERS);
            if (parcelables != null) {
                if (DEBUG_LOG) Log.d(TAG, "onCreate: restoreSize=" + parcelables.length);
                mHeaders = new ArrayList<PreferenceActivity.Header>();
                for (int i = 0; i < parcelables.length; i++) {
                    mHeaders.add((Header)parcelables[i]);
                }
            }
        }
        
        super.onCreate(savedInstanceState);
        if (DEBUG_LOG) Log.d(TAG, "onCreate");
        mPickupEnabler = PickupEnabler.getInstance(this);
        mShakeEnabler = ShakeEnabler.getInstance(this);
        mFacedownEnabler = FacedownEnabler.getInstance(this);
        mInPocketEnabler = InPocketEnabler.getInstance(this);
        mPedometerEnabler = PedometerEnabler.getInstance(this);
        mUserEnabler = UserActivityEnabler.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG_LOG) Log.d(TAG, "onResume");
        if (!SensorHubClient.isSensorHubAvailable()) {
            Toast.makeText(this, R.string.sensorhub_not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG_LOG) Log.d(TAG, "onStart");
        mPickupEnabler.onStart();
        mShakeEnabler.onStart();
        mFacedownEnabler.onStart();
        mInPocketEnabler.onStart();
        mPedometerEnabler.onStart();
        mUserEnabler.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG_LOG) Log.d(TAG, "onStop");
        mFacedownEnabler.onStop();
        mInPocketEnabler.onStop();
        mPedometerEnabler.onStop();
        mPickupEnabler.onStop();
        mShakeEnabler.onStop();
        mUserEnabler.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG_LOG) Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (DEBUG_LOG) Log.d(TAG, "onSaveInstanceState");
        if (mHeaders != null) {
            Header[] array = mHeaders.toArray(new Header[0]);
            outState.putParcelableArray(BUNDLE_HEADERS, (Parcelable[])array);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.sensorhubdemo_headers, target);
        mHeaders = target;
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        super.setListAdapter(new HeaderAdapter(this, mHeaders));
    }
    
    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private LayoutInflater mInflater;
        private Context mContext;
        
        private static final int POSITION_PICKUP = 0;
        private static final int POSITION_SHAKE = 1;
        private static final int POSITION_FACE_DOWN = 2;
        private static final int POSITION_IN_POCKET = 3;
        private static final int POSITION_PEDOMETER = 4;
        private static final int POSITION_USER_ACTIVITY = 5;

        private static class HeaderViewHolder {
            TextView title;
            TextView summary;
            Switch switch_;
        }
        
        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            PickupEnabler.unregisterAllSwitches();
            ShakeEnabler.unregisterAllSwitches();
            InPocketEnabler.unregisterAllSwitches();
            FacedownEnabler.unregisterAllSwitches();
            PedometerEnabler.unregisterAllSwitches();
            UserActivityEnabler.unregisterAllSwitches();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder = new HeaderViewHolder();
            Header header = getItem(position);
            View view = mInflater.inflate(R.layout.preference_header_switch_item, parent,
                    false);

            holder.title = (TextView)
                    view.findViewById(R.id.title);
            holder.summary = (TextView)
                    view.findViewById(R.id.summary);
            holder.switch_ = (Switch) view.findViewById(R.id.switchWidget);
            if (!SensorHubClient.isSensorHubAvailable()) {
                holder.switch_.setEnabled(false);
            }
   
            switch (position) {
                case POSITION_PICKUP:
                    PickupEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_SHAKE:
                    ShakeEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_FACE_DOWN:
                    FacedownEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_IN_POCKET:
                    InPocketEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_PEDOMETER:
                    PedometerEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                case POSITION_USER_ACTIVITY:
                    UserActivityEnabler.registerSwitch(mContext, holder.switch_);
                    break;
                default:
                    break;
            }

            CharSequence title = header.getTitle(getContext().getResources());
            holder.title.setText(title);
            CharSequence summary = header.getSummary(getContext().getResources());
            holder.summary.setText(summary);

            return view;
        }
    }
}
