package com.mediatek.datatransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.mediatek.datatransfer.utils.SDCardUtils;

public class BootActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SDCardUtils.getExternalStoragePath(this) == null) {
            Toast.makeText(this, R.string.nosdcard_notice, Toast.LENGTH_SHORT).show();
            BootActivity.this.finish();
        }
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
        this.finish();
    }
}
