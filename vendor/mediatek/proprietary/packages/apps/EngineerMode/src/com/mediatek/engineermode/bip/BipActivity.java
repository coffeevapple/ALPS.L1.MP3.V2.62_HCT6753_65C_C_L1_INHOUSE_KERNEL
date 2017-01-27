package com.mediatek.engineermode.bip;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

/**
 * BipActivity.
 */
public class BipActivity extends Activity {
    private static final String TAG = "EM/BIP";
    private Button mDisableButton;
    private Button mEnableButton;
    private TextView mTextView;

    /**
     * Disable sending STATUS during boot.
     */
    private void disableBIP() {
        SystemProperties.set("persist.radio.biptest", "0");
        Elog.v(TAG, "set persist.radio.biptest to 0");
        updateTextView();
        Toast.makeText(BipActivity.this, "Set Success, Please reboot phone",
            Toast.LENGTH_LONG).show();
    }

    /**
     * Enable sending STATUS during boot.
     */
    private void enableBIP() {
        SystemProperties.set("persist.radio.biptest", "1");
        Elog.v(TAG, "set persist.radio.biptest to 1");
        updateTextView();
        Toast.makeText(BipActivity.this, "Set Success, Please reboot phone",
            Toast.LENGTH_LONG).show();
    }

    private OnClickListener mDisableListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            disableBIP();
        }
    };

    private OnClickListener mEnableListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            enableBIP();
        }
    };

    /**
     * findViews.
     */
    protected void findViews() {
        this.mDisableButton = (Button) this.findViewById(R.id.BIP_disable);
        this.mEnableButton = (Button) this.findViewById(R.id.BIP_enable);
    }

    /**
     * setActionListener.
     */
    protected void setActionListener() {
        this.mDisableButton.setOnClickListener(this.mDisableListener);
        this.mEnableButton.setOnClickListener(this.mEnableListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bip);
        this.findViews();
        this.setActionListener();
        mTextView = (TextView) this.findViewById(R.id.BIP_text);
        updateTextView();
    }

    /**
     * updateTextView.
     */
    private void updateTextView() {
        final String off = SystemProperties.get("persist.radio.biptest", "0");
        String txt;
        if (off.equals("0")) {
            txt = getString(R.string.BIP_NotSendStatusWhenBootup);
        } else {
            txt = getString(R.string.BIP_SendStatusWhenBootup);
        }
        mTextView.setText("Current is : " + txt);
    }
}
