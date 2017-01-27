package com.mediatek.dialer.plugin;

import java.util.List;
import java.util.HashMap;


import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.mediatek.common.PluginImpl;
import com.mediatek.dialer.ext.DefaultCallLogExtension;
import com.mediatek.dialer.ext.ICallLogExtension.ICallLogAction;
import com.mediatek.op01.plugin.R;

@PluginImpl(interfaceName="com.mediatek.dialer.ext.ICallLogExtension")
public class Op01CallLogExtension extends DefaultCallLogExtension {
    private static final String TAG = "Op01CallLogExtension";

    public static final int CALL_TYPE_ALL = -1;
    private boolean mAutoRejectedFilterMode = false;
    private OnMenuItemClickListener mAutoRejectMenuClickListener ;
    private int mAutoRejectMenuId = -1;
    public static String AUTO_REJECTION_KEY = "AUTO_REJECTION";
	private int mPosition = 0;
    //map the host activity instance and the mAutoRejectedFilterMode
    private HashMap<Activity, Boolean> mRefMap = new HashMap<Activity, Boolean>();

    /**
     * for op01
     * @param context the current context
     * @param pagerAdapter the view pager adapter used in activity
     * @param tabs the ViewPagerTabs used in activity
     */
    public void restoreFragments(Context context,
            FragmentPagerAdapter pagerAdapter, HorizontalScrollView tabs) {
        if(mRefMap.containsKey(context)) {
            mAutoRejectedFilterMode = mRefMap.get(context);
        }
        
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
            Log.d(TAG, "restoreFragments(), notifyDataSetChanged");
        }
        Log.d(TAG, "restoreFragments() mAutoRejectedFilterMode : " + mAutoRejectedFilterMode);
        if (mAutoRejectedFilterMode) {
            try {
                final Context cont = context.createPackageContext("com.mediatek.op01.plugin",
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                tabs.removeAllViews();
                Resources resources = cont.getResources();
                int backgroundColor = resources.getColor(R.color.actionbar_background_color);
                tabs.setBackgroundColor(backgroundColor);
                TextView textView = new TextView(cont);
                textView.setBackgroundColor(backgroundColor);
                final String autoRejectTitle = cont.getString(R.string.call_log_auto_rejected_label);
                textView.setText(autoRejectTitle);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(Color.WHITE);
                tabs.addView(textView, new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT));
            } catch (NameNotFoundException e) {
                Log.d(TAG, "no com.mediatek.op01.plugin packages");
            }
        }
    }

    /**
     * for op01
     * @param activity the current activity
     * @param outState save state
     */
    @Override
    public void onSaveInstanceState(Activity activity, Bundle outState) {
        Log.d(TAG, "onSaveInstanceState mAutoRejectedFilterMode = " + mAutoRejectedFilterMode);
        if (mRefMap.containsKey(activity)) {
            mRefMap.put(activity,mAutoRejectedFilterMode);
        }
    }

    /**
     * for op01
     * @param context
     * @param fragment
     */
    @Override
    public void onCreateForCallLogFragment(Context context, ListFragment fragment) {
       //mAutoRejectedFilterMode = false;
       //Log.d(TAG, "onCreateForCallLogFragment set mAutoRejectedFilterMode false");
    }

    /**
     * for op01
     * called when host create menu, to add plug-in own menu here
     * @param activity the current activity
     * @param menu
     * @param tabs the ViewPagerTabs used in activity
     * @param callLogAction callback plug-in need if things need to be done by host
     */
    @Override
    public void createCallLogMenu(Activity activity, Menu menu,
            HorizontalScrollView viewPagerTabs, ICallLogAction callLogAction) {
        Log.d(TAG, "createCallLogMenu");
        final Activity fCallLogActivity = activity;
        final ICallLogAction fCallLogAction = callLogAction;
        final HorizontalScrollView fViewPagerTabs = viewPagerTabs;
        try {
            final Context cont = fCallLogActivity.getApplicationContext().createPackageContext(
                    "com.mediatek.op01.plugin",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            int index = menu.size();
            MenuItem autoRejectMenu = menu.add(Menu.NONE, index, index, cont.getText(
                    R.string.call_log_auto_rejected_label));
            mAutoRejectMenuId = autoRejectMenu.getItemId();
            autoRejectMenu.setOnMenuItemClickListener(mAutoRejectMenuClickListener =
                new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Log.d(TAG, "Auto reject onMenuItemClick");
                    if (fCallLogAction != null) {
                        mAutoRejectedFilterMode = true;
                        Log.d(TAG, "OnMenuItemClickListener() mRefMap.put mAutoRejectedFilterMode" + mAutoRejectedFilterMode);
                        mRefMap.put(fCallLogActivity, mAutoRejectedFilterMode);
                    }
                    fCallLogAction.updateCallLogScreen();
                    fViewPagerTabs.removeAllViews();
                    Resources resources = cont.getResources();
                    int backgroundColor = resources.getColor(R.color.actionbar_background_color);
                    fViewPagerTabs.setBackgroundColor(backgroundColor);
                    TextView textView = new TextView(cont);
                    textView.setBackgroundColor(backgroundColor);
                    final String autoRejectTitle = cont.getString(
                            R.string.call_log_auto_rejected_label);
                    textView.setText(autoRejectTitle);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(Color.WHITE);
                    fViewPagerTabs.addView(textView, new LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT));
                    return true;
                }
            });
        } catch (NameNotFoundException e) {
            Log.d(TAG, "no com.mediatek.op01.plugin packages");
        }
    }

    /**
     * for op01
     * @param activity
     * @param menu
     */
    @Override
    public void prepareCallLogMenu(Activity activity, Menu menu) {
        Log.d(TAG, "prepareCallLogMenu");
        if (activity == null) {
            mAutoRejectedFilterMode = false;
        }
        Log.d(TAG, "isAutoRejectedFilterMode: " + mAutoRejectedFilterMode);
        if (mAutoRejectMenuId >= 0) {
            menu.findItem(mAutoRejectMenuId).setVisible(!mAutoRejectedFilterMode);
        }
    }

    /**
     * for op01
     * @param activity the current activity
     * @param pagerAdapter the view pager adapter used in activity
     * @param item
     * @return true if do not need further operation in host
     */
    public boolean onHomeButtonClick(Activity activity, FragmentPagerAdapter pagerAdapter, MenuItem item) {
        Log.d(TAG, "onHomeButtonClick");
        if (activity != null) {

            if (mRefMap.containsKey(activity)) {
                mAutoRejectedFilterMode = mRefMap.get(activity);
                Log.d(TAG, "onHomeButtonClick() mRefMap.put mAutoRejectedFilterMode" + mAutoRejectedFilterMode);
            }
            if (mAutoRejectedFilterMode && (item.getItemId() == android.R.id.home)) {
                mAutoRejectedFilterMode = false;
                mRefMap.remove(activity);
                if (pagerAdapter != null) {
                    pagerAdapter.notifyDataSetChanged();
                    Log.d(TAG, "onHomeButtonClick(), notifyDataSetChanged");
                }

                final Intent intent = new Intent();
                intent.setClassName("com.android.dialer",
                        "com.android.dialer.calllog.CallLogActivity");
                activity.startActivity(intent);
                activity.finish();
                return true;
            }
        } else {
            mAutoRejectedFilterMode = false;
            mRefMap.put(activity, mAutoRejectedFilterMode);
        }
        return false;
    }

    /**
     * for op01
     * called when host press back key
     * @param activity the current activity
     * @param pagerAdapter the view pager adapter used in activity
     * @param callLogAction call back function
     */
    @Override
    public void onBackPressed(Activity activity, FragmentPagerAdapter pagerAdapter, ICallLogAction callLogAction) {
        Log.d(TAG, "onBackPressed mRefMap = " + mRefMap + " activity = " + activity);
        if (activity != null) {
            if (mRefMap.containsKey(activity)) {
                mAutoRejectedFilterMode = mRefMap.get(activity);
            }
            
            if (mAutoRejectedFilterMode) {
                mAutoRejectedFilterMode = false;
                mRefMap.remove(activity);
                Log.d(TAG, "onBackPressed mRefMap.remove(activity) successful");
                if (pagerAdapter != null) {
                    pagerAdapter.notifyDataSetChanged();
                    Log.d(TAG, "onBackPressed(), notifyDataSetChanged");
                }

                final Intent intent = new Intent();
                intent.setClassName("com.android.dialer",
                        "com.android.dialer.calllog.CallLogActivity");
                activity.startActivity(intent);
                activity.finish();
            } else {
                callLogAction.processBackPressed();
            }
        } else {
            mAutoRejectedFilterMode = false;
            callLogAction.processBackPressed();
            mRefMap.put(activity, mAutoRejectedFilterMode);
        }
    }

    /**
     * for op01
     * @param typeFiler current query type
     * @param builder the query selection Stringbuilder
     * @param selectionArgs the query selection args, modify to change query selection
     */
    @Override
    public void appendQuerySelection(int typeFiler, StringBuilder builder,
            List<String> selectionArgs) {
        Log.d(TAG, "appendQuerySelection mAutoRejectedFilterMode = " +
            mAutoRejectedFilterMode);

        String strbuilder = null;
        if (CALL_TYPE_ALL == typeFiler && !mAutoRejectedFilterMode) {
            if (builder.length() > 0) {
                builder.append(" AND ");
            }
            strbuilder = Calls.TYPE + "!=" + Calls.AUTO_REJECT_TYPE;
            builder.append(strbuilder);
        }
        if (mAutoRejectedFilterMode) {
            Log.d(TAG, "selectionArgs1: " + selectionArgs);
            if (typeFiler > CALL_TYPE_ALL) {
                if (!selectionArgs.isEmpty()) {
                    selectionArgs.set(0, Integer.toString(Calls.AUTO_REJECT_TYPE));
                }
            } else if (typeFiler == CALL_TYPE_ALL) {
                selectionArgs.add(0, Integer.toString(Calls.AUTO_REJECT_TYPE));
            }
            Log.d(TAG, "selectionArgs2: " + selectionArgs);
            if (builder.length() > 0 && (builder.indexOf("(type = ?)") == -1)) {
                String strbuild = "(type = ?) AND ";
                builder.insert(0, strbuild);
            } else if (builder.length() == 0) {
                builder.append("(type = ?)");
            }
        }

        Log.d(TAG, "builder: " + builder);
    }

    /**
     * for op01
     * called when updating tab count
     * @param activity the current activity
     * @param count
     * @return tab count
     */
    @Override
    public int getTabCount(Activity activity, int count) {
        Log.d(TAG, "getTabCount, mRefMap: " + mRefMap);
        if (!mRefMap.isEmpty() && mRefMap.containsKey(activity)) {
            mAutoRejectedFilterMode = (Boolean)mRefMap.get(activity);
        }
        if (mAutoRejectedFilterMode) {
            count = 1;
        }
        Log.d(TAG, "getTabCount, count: " + count);
        return count;
    }

    /**
     * for op01
     */
    @Override
    public void onDestroyForCallLogFragment() {
        //mAutoRejectedFilterMode = false;
        //Log.d(TAG, "onDestroyForCallLogFragment: " + mAutoRejectedFilterMode);
    }

	/**
     * for op01
     * plug-in set position
     * @param position to set
     */
    @Override
	public void setPosition(int position) {		
		Log.d(TAG, "setPosition()-->position " + position);
		mPosition = position;
	}

	/**
     * for op01
     * plug-in get current position
     * @param position
     * @return get the position
     */
    @Override
	public int getPosition(int position) {
		if(mAutoRejectedFilterMode) {
			return mPosition;
		}
		return position;
	}

    /**
     * for op01
     * plug-in modify clear the member objects of plug-in
     * @param activity the current activity
     */
    @Override
    public void clearObjects(Activity activity) {
        if (!mRefMap.isEmpty() && mRefMap.containsKey(activity)) {
            mRefMap.remove(activity);
        }
        mAutoRejectedFilterMode = false;
        Log.d(TAG, "clearObjects()-->mAutoRejectedFilterMode " + mAutoRejectedFilterMode);
    }

    /**
     * for op01
     * plug-in init the reject mode in the host
     * @param activity the current activity
     */
    @Override
    public void initRejectMode(Activity activity) {
        mAutoRejectedFilterMode = false;
        mRefMap.put(activity, mAutoRejectedFilterMode);
    }

}
