package com.mediatek.contacts.plugin;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.Intents.Insert;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.method.DialerKeyListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.EditText;

import com.mediatek.op01.plugin.R;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.contacts.ext.DefaultOp01Extension;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.contacts.ext.IOp01Extension")
public class OP01Extension extends DefaultOp01Extension {
    private static final String TAG = "OP01Extension";
    private Context mContext;
    private static SubscriptionManager mManager;
    private static Context mContextHost;
    private static final int MENU_SIM_STORAGE = 9999;

    public OP01Extension(Context context) {
        mContext = context;
        mManager = SubscriptionManager.from(context);
    }

    @Override
    public void addOptionsMenu(Context context, Menu menu) {
        Log.i(TAG, "addOptionsMenu");
        mContextHost = context;
        MenuItem item = menu.findItem(MENU_SIM_STORAGE);
        List<SubscriptionInfo> simInfos = mManager.getActiveSubscriptionInfoList();
        if (item == null && simInfos != null && simInfos.size() > 0) {
            String string = mContext.getResources().getString(R.string.look_simstorage);
            menu.add(0, MENU_SIM_STORAGE, 0, string).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ShowSimCardStorageInfoTask.showSimCardStorageInfo(mContext);
                        return true;
                    }
            });
        }
    }

    public static class ShowSimCardStorageInfoTask extends AsyncTask<Void, Void, Void> {
        private static ShowSimCardStorageInfoTask sInstance = null;
        private boolean mIsCancelled = false;
        private boolean mIsException = false;
        private String mDlgContent = null;
        private Context mContext = null;

        public static void showSimCardStorageInfo(Context context) {
            Log.i(TAG, "[ShowSimCardStorageInfoTask]_beg");
            if (sInstance != null) {
                sInstance.cancel();
                sInstance = null;
            }
            sInstance = new ShowSimCardStorageInfoTask(context);
            sInstance.execute();
            Log.i(TAG, "[ShowSimCardStorageInfoTask]_end");
        }

        public ShowSimCardStorageInfoTask(Context context) {
            mContext = context;
            Log.i(TAG, "[ShowSimCardStorageInfoTask] onCreate()");
        }

        @Override
        protected Void doInBackground(Void... args) {
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: doInBackground_beg");
            List<SubscriptionInfo> simInfos = 
                    getSortedInsertedSimInfoList(mManager.getActiveSubscriptionInfoList());
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: simInfos.size = " + simInfos.size());
            if (!mIsCancelled && (simInfos != null) && simInfos.size() > 0) {
                StringBuilder build = new StringBuilder();
                int simId = 0;
                for (SubscriptionInfo simInfo : simInfos) {
                    if (simId > 0) {
                        build.append("\n\n");
                    }
                    simId++;
                    int[] storageInfos = null;
                    build.append(simInfo.getDisplayName());
                    build.append(":\n");
                    try {
                        ITelephonyEx phoneEx = ITelephonyEx.Stub.asInterface(ServiceManager
                              .checkService("phoneEx"));
                        if (!mIsCancelled && phoneEx != null) {
                            storageInfos = phoneEx.getAdnStorageInfo(simInfo.getSubscriptionId());
                            if (storageInfos == null) {
                                mIsException = true;
                                Log.i(TAG, " storageInfos is null");
                                return null;
                            }
                            Log.i(TAG, "[ShowSimCardStorageInfoTask] infos: "
                                    + storageInfos.toString());
                        } else {
                            Log.i(TAG, "[ShowSimCardStorageInfoTask]: phone = null");
                            mIsException = true;
                            return null;
                        }
                    } catch (RemoteException ex) {
                        Log.i(TAG, "[ShowSimCardStorageInfoTask]_exception: " + ex);
                        mIsException = true;
                        return null;
                    }
                    build.append(mContext.getResources().getString(R.string.dlg_simstorage_content,
                            storageInfos[1], storageInfos[0]));
                    if (mIsCancelled) {
                        return null;
                    }
                }
                mDlgContent = build.toString();
            }
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: doInBackground_end");
            return null;
        }

        public void cancel() {
            super.cancel(true);
            mIsCancelled = true;
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: mIsCancelled = true");
        }

        @Override
        protected void onPostExecute(Void v) {
            if (mContextHost instanceof Activity) {
                Log.i(TAG, "[onPostExecute]: activity find");
                Activity activity = (Activity) mContextHost;
                if (activity.isFinishing()) {
                    Log.i(TAG, "[onPostExecute]: activity finish");
                    mIsCancelled = false;
                    mIsException = false;
                    sInstance = null;
                    return;
                }
            }

            Drawable icon = mContext.getResources().getDrawable(R.drawable.ic_menu_look_simstorage_holo_light);
            String string = mContext.getResources().getString(R.string.look_simstorage);
            sInstance = null;
            if (!mIsCancelled && !mIsException) {
                new AlertDialog.Builder(mContextHost).setIcon(icon).setTitle(string).setMessage(mDlgContent).setPositiveButton(
                       android.R.string.ok, null).setCancelable(true).create().show();
            }
            mIsCancelled = false;
            mIsException = false;
        }

        public List<SubscriptionInfo> getSortedInsertedSimInfoList(List<SubscriptionInfo> ls) {
            Collections.sort(ls, new Comparator<SubscriptionInfo>() {
                @Override
                public int compare(SubscriptionInfo arg0, SubscriptionInfo arg1) {
                    return (arg0.getSimSlotIndex() - arg1.getSimSlotIndex());
                }
            });
            return ls;
        }
    }

    @Override
    public int getMultiChoiceLimitCount(int defaultCount) {
        Log.i(TAG, "[getMultiChoiceLimitCount]");
        return 5000;
    }

    @Override
    public String formatNumber(String number, Bundle bundle) {
        String result = number;
        if (bundle != null) {
            final CharSequence data = bundle.getCharSequence(Insert.PHONE);
            if (data != null && TextUtils.isGraphic(data)) {
                String phone = data.toString();
                Log.i(TAG, "[formatNumber] orignal: " + phone);
                if (phone != null && !TextUtils.isEmpty(phone)) {
                    phone = phone.replaceAll(" ", "");
                    Log.i(TAG, "[formatNumber]" + phone);
                    bundle.putString(Insert.PHONE, phone);
                }
            }
            return result;
        }
        if (result != null && !TextUtils.isEmpty(result)) {
            result = result.replaceAll(" ", "");          
        }
        Log.i(TAG, "[formatNumber]" + result);
        return result;
    }

    @Override
    public void setViewKeyListener(EditText fieldView) {
        Log.i(TAG, "[setViewKeyListener] fieldView : " + fieldView);
        if (fieldView != null) {
            fieldView.setKeyListener(SIMKeyListener.getInstance());
        } else {
            Log.e(TAG, "[setViewKeyListener]fieldView is null");
        }
    }

    public static class SIMKeyListener extends DialerKeyListener {
        private static SIMKeyListener sKeyListener;
        /**
         * The characters that are used.
         *
         * @see KeyEvent#getMatch
         * @see #getAcceptedChars
         */
        public static final char[] CHARACTERS = new char[] { '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '+', '*', '#', 'P', 'W', 'p', 'w', ',', ';'};

        @Override
        protected char[] getAcceptedChars() {
            return CHARACTERS;
        }

        public static SIMKeyListener getInstance() {
            if (sKeyListener == null) {
                sKeyListener = new SIMKeyListener();
            }
            return sKeyListener;
        }

    }
}
