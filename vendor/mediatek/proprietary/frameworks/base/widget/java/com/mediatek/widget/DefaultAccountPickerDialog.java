/*
 * Copyright (C) 2011-2014 MediaTek Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;
import com.mediatek.internal.R;

import java.util.ArrayList;
import java.util.List;

public class DefaultAccountPickerDialog extends DialogFragment {
    private final static String TAG = "DefaultAccountPickerDialog";

    private final static int NO_ITEM_SELECT = -1;
    private static int sSelection = NO_ITEM_SELECT;
    private static int sOldSelection = NO_ITEM_SELECT;
    private static DefaultAccountPickerAdapter sAdapter;

    private DefaultAccountPickerDialog(Context context) {
        sAdapter = new DefaultAccountPickerAdapter(context);
    }

    public DefaultAccountPickerDialog() {}

    /**
     * Build a DefaultAccountPickerDialog instance.
     * @param context the context to show the dialog
     * @return instance of DefaultAccountPickerDialog
     */
    public static DefaultAccountPickerDialog build(Context context) {
        DefaultAccountPickerDialog dialogFragment = new DefaultAccountPickerDialog(context);
        sSelection = NO_ITEM_SELECT;
        return dialogFragment;
    }

    /**
     * Set data to the Dialog, data is a list of {@link #AccountInfo}
     * @param data items to display
     * @return the dialog itself
     */
    public DefaultAccountPickerDialog setData(List<AccountInfo> data) {
        sAdapter.setItemData(data);
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.other_accounts);

        builder.setSingleChoiceItems(sAdapter, sSelection, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sOldSelection = sAdapter.getActivePosition();
                sSelection = which;
                sAdapter.setActiveStatus(sSelection);
                Log.d(TAG, "onClick position: " + sSelection);

                sAdapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sAdapter.setActiveStatus(sOldSelection);
                sAdapter.notifyDataSetChanged();

                Log.d(TAG, " old select position: " + sOldSelection);

                dialog.dismiss();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (sSelection == NO_ITEM_SELECT) {
                    Log.d(TAG, "--- No item is selected ---");
                    return;
                }

                Intent intent = sAdapter.getItem(sSelection).getIntent();
                if (intent != null && getActivity() != null) {
                    Log.d(TAG, "sent broadcast: " + sSelection);

                    getActivity().sendBroadcast(intent);
                }
            }
        });
        return builder.create();
    }
}
