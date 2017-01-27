package com.mediatek.engineermode.seapi;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
//import android.provider.Settings;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.Elog;

public class seapiActivity extends Activity {
    /** Called when the activity is first created. */

    private static final String TAG = "EM/SEAPI";

    private Button mDisableButton;
    private Button mEnableButton;
    private TextView mTextView;

    private void disable_SEAPI() {
        SystemProperties.set("persist.radio.seapi.off", "1");
        //Settings.Global.putInt(getContentResolver(), Settings.Global.SEAPI_NOT_START, 1);
        
        Elog.v(TAG, "set persist.radio.seapi.off to 1");
        updateTextView();
        Toast.makeText(seapiActivity.this, "Set Success, Please reboot phone", Toast.LENGTH_LONG).show();
    }

    private void enable_SEAPI() {
        SystemProperties.set("persist.radio.seapi.off", "0");
        //Settings.Global.putInt(getContentResolver(), Settings.Global.SEAPI_NOT_START, 1);
        
        Elog.v(TAG, "set persist.radio.seapi.off to 0");
        updateTextView();
        Toast.makeText(seapiActivity.this, "Set Success, Please reboot phone", Toast.LENGTH_LONG).show();
    }

    private OnClickListener mDisableListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            disable_SEAPI();
        }
    };

    private OnClickListener mEnableListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            enable_SEAPI();
        }
    };

    protected void findViews() {
        this.mDisableButton = (Button) this.findViewById(R.id.SEAPI_disable);
        this.mEnableButton = (Button) this.findViewById(R.id.SEAPI_enable);
    }

    protected void setActionListener() {
        this.mDisableButton.setOnClickListener(this.mDisableListener);
        this.mEnableButton.setOnClickListener(this.mEnableListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seapi);
        this.findViews();
        this.setActionListener();
        mTextView = (TextView) this.findViewById(R.id.SEAPI_text);
        updateTextView();
    }

    private void updateTextView() {
        final String off = SystemProperties.get("persist.radio.seapi.off", "0");
        String txt;
        if (off.equals("0")) {
            txt = getString(R.string.SEAPI_StartWhenBootup);
        } else {
            txt = getString(R.string.SEAPI_NotStartWhenBootup);
        }
        mTextView.setText("Current is : " + txt);
    }

}
