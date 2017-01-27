package com.mediatek.multiwindow.service;

import com.mediatek.multiwindow.service.MultiWindowApplication;
import com.mediatek.common.multiwindow.IMultiWindowManager;
import com.mediatek.common.multiwindow.IMWAmsCallback;
import com.mediatek.common.multiwindow.IMWPhoneWindowCallback;
import com.mediatek.common.multiwindow.IMWWmsCallback;
import com.mediatek.common.multiwindow.IMWSystemUiCallback;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.IProcessObserver;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.List;
import android.os.Handler;
import android.os.HandlerThread;
import com.mediatek.sef.service.FeatureServiceBase;

public class MultiWindowServices extends FeatureServiceBase {

    private static final String TAG = "MultiWindowServices";
    private static boolean DBG_MWS = false;
    private static final int MAX_RUNNING_TASK = ActivityManager
            .getMaxRecentTasksStatic();
    private ActivityManager mActivityManager;
    private static Context mContext;

    // Callbacks
    private IMWAmsCallback mAMSCb;
    private IMWWmsCallback mWMSCb;
    private IMWSystemUiCallback mSystemUiCb;

    private SparseArray<StackInfo> mStackInfos = new SparseArray<StackInfo>();
    private SparseArray<TaskInfo> mTaskInfos = new SparseArray<TaskInfo>();

    BlackNameListManager mBlackNameListManager = new BlackNameListManager();
    
    final public static String ACTION_DISABLE_PKG_UPDATED = "action_multiwindow_disable_pkg_updated";
    
    private static final int MSG_WRITE_TO_XML = 1;
    private static final int MSG_READ_FROM_XML = 2;
    // Remove package from black list when it was removed or updated.
    private static final int MSG_PACKAGE_REMOVED = 3;
    
    
    static final int WRITE_TO_XML_DELAY = 10 * 1000;  // 10 seconds

    private static boolean PACKAGE_MONITOR_ENABLED = SystemProperties.getBoolean(
            "debug.mw.pkg_monitor_enabled", false);

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String pkg = intent.getData().getSchemeSpecificPart();
            mHandler.sendMessage(mHandler.obtainMessage(MSG_PACKAGE_REMOVED, pkg));
        }
    };

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WRITE_TO_XML: {
                    Slog.v(TAG, "MSG_WRITE_TO_XML");
                    synchronized (mBlackNameListManager) {
                        mBlackNameListManager.writeToXmlLocked();
                    }
                    break;
                }
                case MSG_READ_FROM_XML: {
                    Slog.v(TAG, "MSG_READ_FROM_XML");
                    synchronized (mBlackNameListManager) {
                        mBlackNameListManager.readFromXmlLocked();
                    }
                    break;
                }
                case MSG_PACKAGE_REMOVED: {
                    Slog.v(TAG, "MSG_PACKAGE_REMOVED");
                    String pkg = (String) msg.obj;
                    synchronized (mBlackNameListManager) {
                        mBlackNameListManager.removePkgLocked(pkg);
                        scheduleWriteToXmlLocked();
                    }
                    break;
                }
            }
        }
    };

    public MultiWindowServices() throws IOException {
        Slog.v(TAG, "MultiWindowServices init");
        mContext = MultiWindowApplication.getContext();
        mActivityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        List<RecentTaskInfo> mRecentTask = mActivityManager.getRecentTasks(
                MAX_RUNNING_TASK, ActivityManager.RECENT_WITH_EXCLUDED);
        for (int index = 0; index < mRecentTask.size(); index++) {
            RecentTaskInfo receTaskInfo = mRecentTask.get(index);
            if (receTaskInfo.id != -1) {
                addTaskInfo(receTaskInfo.id);
            }
        }

        IBinder binder = new IMultiWindowManager.Stub() {

            /**
             * By this cb, we can access AMS directly.
             */
            @Override
            public void setAMSCallback(IMWAmsCallback cb) {
                mAMSCb = cb;
            }

            /**
             * By this cb, we can access WMS directly.
             */
            @Override
            public void setWMSCallback(IMWWmsCallback cb) {
                mWMSCb = cb;
            }

            /**
             * By this cb, we can access SystemUI directly.
             */
            @Override
            public void setSystemUiCallback(IMWSystemUiCallback cb) {
                mSystemUiCb = cb;
            }

            /**
             * Moves an activity, and all of the other activities within the same task, to
             * the front of the history stack. The activity's order within the task is
             * unchanged.
             * 
             * @param token A reference to the activity we wish to move.
             */
            @Override
            public void moveActivityTaskToFront(IBinder token) {
                if (mAMSCb == null) {
                    Slog.e(TAG, "moveActivityTaskToFront, mAMSCb is null!!!");
                    return;
                }
                try {
                    /// M: [ALPS01891295] If Task is being removed, do not move it to
                    /// front. @{
                    int taskId = ActivityManagerNative.getDefault()
                            .getTaskForActivity(token, false);
                    synchronized (mTaskInfos) {
                        TaskInfo taskInfo = mTaskInfos.get(taskId);
                        if (taskInfo != null && taskInfo.mPendingRemove) {
                            return;
                        }
                    }
                    /// @}
                    if (DBG_MWS)
                        Slog.w(TAG, "moveActivityTaskToFront token = " + token);
                    mAMSCb.moveActivityTaskToFront(token);
                } catch (RemoteException e) {
                }
            }

            /**
             * Completely remove the activity's task. TODO Wish to remove all the tasks of
             * the Activity's stack.
             * 
             * @param token A reference to the activity we wish to close.
             */
            @Override
            public void closeWindow(IBinder token) {
                if (DBG_MWS)
                    Slog.v(TAG, "closeWindow token = " + token);
                try {
                    int taskId = ActivityManagerNative.getDefault()
                            .getTaskForActivity(token, false);
                    /// M: [ALPS01891295] If Task is being removed, do not move it to
                    /// front.@{
                    synchronized (mTaskInfos) {
                        TaskInfo taskInfo = mTaskInfos.get(taskId);
                        if (taskInfo != null) {
                            taskInfo.mPendingRemove = true;
                        }
                    }
                    ///@}
                    /// M: [ALPS01885359] Music control panel still alive in notification
                    /// list
                    mActivityManager.removeTask(taskId/*,  0 ActivityManager.REMOVE_TASK_KILL_PROCESS*/);
                    if (DBG_MWS)
                        Slog.w(TAG, "closeWindow remove task! taskInfo.id = "
                                + taskId);
                } catch (RemoteException e) {
                    Slog.e(TAG, "closeWindow RemoteException:" + e);
                }
            }

            /**
             * Restore or Max the activity's stack.
             * 
             * @param token The Binder token referencing the Activity we want to
             *            restore/max.
             * @param toMax If true, move floating task to normal stack. Otherwise, move
             *            normal task to a new floating stack
             */
            @Override
            public void restoreWindow(IBinder token, boolean toMax) {
                if (mAMSCb == null) {
                    Slog.e(TAG, "restoreWindow, mAMSCb is null!!!");
                    return;
                }
                try {
                    mAMSCb.restoreStack(token, toMax);
                } catch (RemoteException e) {
                }
            }

            /**
             * Stick Stack, this is a toggle function let this stack can always keep in
             * top.
             * 
             * @param token A reference to the activity we want to stick.
             * @param isSticky If true, always keep the stack in top.
             */
            @Override
            public void stickWindow(IBinder token, boolean isSticky) {
                if (DBG_MWS)
                    Slog.w(TAG, "stickWindow token = " + token
                            + ", isSticky = " + isSticky);
                if (mAMSCb == null) {
                    Slog.e(TAG, "stickWindow, mAMSCb is null!!!");
                    return;
                }
                try {
                    int stackId = mAMSCb.findStackIdByToken(token);
                    synchronized (mStackInfos) {
                        StackInfo stackInfo = mStackInfos.get(stackId);
                        if (stackInfo == null) {
                            Slog.e(TAG, "stickWindow no found stack id= "
                                    + stackId);
                            return;
                        }
                        stackInfo.mSticky = isSticky;
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "stickWindow RemoteException:" + e);
                }
            }

            /**
             * Check the stack is floating or not.
             */
            @Override
            public boolean isFloatingStack(int stackId) {
                synchronized (mStackInfos) {
                    StackInfo stackInfo = mStackInfos.get(stackId);
                    if (stackInfo != null) {
                        return stackInfo.mFloating;
                    }
                }
                return false;
            }

            /**
             * Set the stack floating, when create a new floating stack.
             * 
             * @param stackId The unique identifier of the stack.
             */
            @Override
            public void setFloatingStack(int stackId) {
                if (DBG_MWS)
                    Slog.w(TAG, "setFloatingStack stackId = " + stackId);
                synchronized (mStackInfos) {
                    StackInfo stackInfo = mStackInfos.get(stackId);
                    if (stackInfo == null) {
                        stackInfo = new StackInfo(stackId);
                        mStackInfos.put(stackId, stackInfo);
                    }
                    stackInfo.mFloating = true;
                }
            }

            /**
             * Check the stack is sticky or not.
             */
            @Override
            public boolean isStickStack(int stackId) {
                synchronized (mStackInfos) {
                    StackInfo stackInfo = mStackInfos.get(stackId);
                    if (stackInfo != null) {
                        return stackInfo.mSticky;
                    }
                }
                return false;
            }

            /**
             * Check whether the task is in Mini/Max status.
             */
            @Override
            public boolean isInMiniMax(int taskId) {
                synchronized (mTaskInfos) {
                    TaskInfo taskInfo = mTaskInfos.get(taskId);
                    if (taskInfo != null) {
                        return taskInfo.mInMiniMax;
                    }
                }
                return false;
            }

            @Override
            public void moveFloatingWindow(int disX, int disY) {
                if (mWMSCb == null) {
                    Slog.e(TAG, "moveFloatingWindow, mWMSCb is null!!!");
                    return;
                }
                try {
                    mWMSCb.moveFloatingWindow(disX, disY);
                } catch (RemoteException e) {
                    // Empty
                }
            }

            @Override
            public void resizeFloatingWindow(int direction, int deltaX,
                    int deltaY) {
                if (mWMSCb == null) {
                    Slog.e(TAG, "resizeFloatingWindow, mWMSCb is null!!!");
                    return;
                }
                try {
                    mWMSCb.resizeFloatingWindow(direction, deltaX, deltaY);
                } catch (RemoteException e) {
                    // Empty
                }
            }

            @Override
            public void enableFocusedFrame(boolean enable) {
                if (mWMSCb == null) {
                    Slog.e(TAG, "enableFocusedFrame, mWMSCb is null!!!");
                    return;
                }
                try {
                    mWMSCb.enableFocusedFrame(enable);
                } catch (RemoteException e) {
                    // Empty
                }
            }

            /**
             * Record the mini/max status for the task, and notify WMS.
             */
            @Override
            public void miniMaxTask(int taskId) {
                if (mWMSCb == null) {
                    Slog.e(TAG, "miniMaxTask, mWMSCb is null!!!");
                    return;
                }
                try {
                    if (DBG_MWS)
                        Slog.v(TAG, "miniMaxTask taskId = " + taskId);
                    synchronized (mTaskInfos) {
                        TaskInfo taskInfo = mTaskInfos.get(taskId);
                        if (taskInfo != null) {
                            taskInfo.mInMiniMax = true;
                        }
                    }
                    mWMSCb.miniMaxTask(taskId);
                } catch (RemoteException e) {
                    // Empty
                }
            }

            /**
             * Called by WindowManagerService to contorl restore button on systemUI
             * module.
             */
            @Override
            public void showRestoreButton(boolean flag) {
                if (mSystemUiCb == null) {
                    Slog.e(TAG, "showRestoreButton, mSystemUiCb is null!!!");
                    return;
                }
                try {
                    mSystemUiCb.showRestoreButton(flag);
                } catch (RemoteException e) {
                }
            }

            /**
             * Some APP can not change the config, so we keep they in the list, and not
             * change the config
             */
            @Override
            public boolean matchConfigNotChangeList(String packageName) {
                if (DBG_MWS)
                    Slog.v(TAG, "matchConfigNotChangeList packageName = "
                            + packageName);
                return mBlackNameListManager.matchConfigNotChangeList(packageName);
            }
            
            @Override
            public boolean matchConfigChangeList(String packageName) {
                if (DBG_MWS)
                    Slog.v(TAG, "matchConfigChangeList packageName = "
                            + packageName);
                return mBlackNameListManager.matchConfigChangeList(packageName);
            }

            /**
             * Check if the package can be floating mode by querying the black list.
             */
            @Override
            public boolean matchDisableFloatPkgList(String packageName) {
                if (DBG_MWS)
                    Slog.v(TAG, "matchDisableFloatPkgList packageName = "
                            + packageName);
                return mBlackNameListManager.matchDisableFloatPkgList(packageName);
            }

            /**
             * Check if the component can be floating mode by querying the black list.
             */
            @Override
            public boolean matchDisableFloatActivityList(String ActivityName) {
                if (DBG_MWS)
                    Slog.v(TAG, "matchDisableFloatActivityList ActivityName = "
                            + ActivityName);
                return mBlackNameListManager.matchDisableFloatActivityList(ActivityName);
            }

            /**
             * Check if the window can be floating mode by querying the black list.
             */
            @Override
            public boolean matchDisableFloatWinList(String winName) {
                if (DBG_MWS)
                    Slog.v(TAG, "matchDisableFloatWinList winName = " + winName);
                if (matchDisableFloatActivityList(winName))
                    return true;
                return mBlackNameListManager.matchDisableFloatWinList(winName);

            }

            /**
             * Return a list of the package name that are not allowed to be floating mode.
             */
            @Override
            public List<String> getDisableFloatPkgList() {
                if (DBG_MWS)
                    Slog.v(TAG, "getDisableFloatPkgList");
                return mBlackNameListManager.getDisableFloatPkgList();
            }

            /**
             * Return a list of the Component name that are not allowed to be floating mode.
             */
            @Override
            public List<String> getDisableFloatComponentList() {
                if (DBG_MWS)
                    Slog.v(TAG, "getDisableFloatComponentList");
                return mBlackNameListManager.getDisableFloatComponentList();
            }

            /**
             * Check if need to restart apps when doing Restore and Max by querying the
             * black list. Called by AMS.
             */
            @Override
            public boolean matchMinimaxRestartList(String packageName) {
                if (DBG_MWS)
                    Slog.v(TAG, "matchMinimaxRestartList packageName = "
                            + packageName);
                return mBlackNameListManager.matchMinimaxRestartList(packageName);
            }

            /**
             * Check the activity is sticy or not.
             * 
             * @param token A referencing to the Activity that we want to check.
             */
            @Override
            public boolean isSticky(IBinder token) {
                if (DBG_MWS)
                    Slog.v(TAG, "isSticky token = " + token);
                if (mAMSCb == null) {
                    Slog.e(TAG, "isSticky, mAMSCb is null!!!");
                    return false;
                }
                try {
                    int stackId = mAMSCb.findStackIdByToken(token);
                    synchronized (mTaskInfos) {
                        StackInfo stackInfo = mStackInfos.get(stackId);
                        if (stackInfo != null)
                            return stackInfo.mSticky;
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "isSticky RemoteException:" + e);
                }
                return false;
            }

            /**
             * Called by ActivityThread to Tell the MultiWindowService we have created.
             * MultiWindowService need to do operations for the activity at this point.
             * 
             * @param token A referencing to the Activity that has created.
             */
            @Override
            public void activityCreated(IBinder token) {
                if (DBG_MWS)
                    Slog.v(TAG, "activityCreated token = " + token);
                try {
                    int taskId = ActivityManagerNative.getDefault()
                            .getTaskForActivity(token, false);
                    resetMiniMax(taskId);
                } catch (RemoteException e) {
                    Slog.e(TAG, "activityCreated RemoteException:" + e);
                }
            }

            /**
             * Called by ActivityManagerService when task has been added.
             * MultiWindowService need to maintain the all running task infos, so, we
             * should synchronize task infos for it.
             * 
             * @param taskId Unique ID of the added task.
             */
            @Override
            public void taskAdded(int taskId) {
                if (DBG_MWS)
                    Slog.v(TAG, "taskAdded taskId = " + taskId);
                addTaskInfo(taskId);

            }

            /**
             * Called by ActivityManagerService when task has been removed.
             * MultiWindowService need to maintain the all running task infos, so, we
             * should synchronize task infos for it.
             * 
             * @param taskId Unique ID of the removed task.
             */
            @Override
            public void taskRemoved(int taskId) {
                if (DBG_MWS)
                    Slog.v(TAG, "taskRemoved taskId = " + taskId);
                removeTaskInfo(taskId);
            }

            @Override
            public void addDisableFloatPkg(String packageName) {
                synchronized (mBlackNameListManager) {
                    mBlackNameListManager.disableFloatList
                            .addPkgLocked(packageName);
                    sendDisablePackageBroadcast(packageName, true/*add*/);
                    scheduleWriteToXmlLocked();
                }
            }

            @Override
            public void addConfigNotChangePkg(String packageName) {
                synchronized (mBlackNameListManager) {
                    mBlackNameListManager.configNotChangeList
                            .addPkgLocked(packageName);
                    scheduleWriteToXmlLocked();
                }
            }

            @Override
            public void addMiniMaxRestartPkg(String packageName) {
                synchronized (mBlackNameListManager) {
                    mBlackNameListManager.restartList.addPkgLocked(packageName);
                    scheduleWriteToXmlLocked();
                }
            }

            @Override
            public int appErrorHandling(String packageName,
                    boolean inMaxOrRestore, boolean defaultChangeConfig) {
                boolean configNotChange = false;
                boolean needKill = false;
                boolean needDisable = false;

                int res = 0;
                synchronized (mBlackNameListManager) {
                    if (inMaxOrRestore) {
                        if (!mBlackNameListManager.matchConfigNotChangeList(packageName)
                                && defaultChangeConfig) {
                            configNotChange = true;
                        } else if (!mBlackNameListManager.matchMinimaxRestartList(packageName)) {
                            needKill = true;
                        } else if (!mBlackNameListManager.matchDisableFloatPkgList(packageName)) {
                            needDisable = true;
                        }
                    } else {
                        if (!mBlackNameListManager.matchConfigNotChangeList(packageName)
                                && defaultChangeConfig) {
                            configNotChange = true;
                        } else if (!mBlackNameListManager.matchDisableFloatPkgList(packageName)) {
                            needDisable = true;
                        }
                    }
                    if (configNotChange && defaultChangeConfig) {
                        mBlackNameListManager.configNotChangeList
                                .addPkgLocked(packageName);
                        mBlackNameListManager.restartList
                                .removePkgLocked(packageName);
                        mBlackNameListManager.disableFloatList
                                .removePkgLocked(packageName);
                        res = 1;
                    } else if (needKill) {
                        mBlackNameListManager.restartList
                                .addPkgLocked(packageName);
                        mBlackNameListManager.configNotChangeList
                                .removePkgLocked(packageName);
                        mBlackNameListManager.disableFloatList
                                .removePkgLocked(packageName);
                        res = 2;
                    } else if (needDisable) {
                        mBlackNameListManager.disableFloatList
                                .addPkgLocked(packageName);
                        mBlackNameListManager.configNotChangeList
                                .removePkgLocked(packageName);
                        mBlackNameListManager.restartList
                                .removePkgLocked(packageName);
                        res = 3;
                        sendDisablePackageBroadcast(packageName, true/*add*/);
                    }
                    scheduleWriteToXmlLocked();
                }
                return res;
            }

            @Override
            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (mContext
                        .checkCallingOrSelfPermission("android.permission.DUMP") != PackageManager.PERMISSION_GRANTED) {
                    pw.println("Permission Denial: can't dump MultiWindow from from pid="
                            + Binder.getCallingPid()
                            + ", uid="
                            + Binder.getCallingUid());
                    return;
                }

                pw.println("MULTI WINDOW MANAGER (dumpsys multiwindow_service_v1)");
                // Print the current date and time
                String currentDateTimeString = DateFormat.getDateTimeInstance()
                        .format(new Date());
                pw.println("Dump time : " + currentDateTimeString);

                boolean dumpAll = false;

                int opti = 0;
                while (opti < args.length) {
                    String opt = args[opti];
                    if (opt == null || opt.length() <= 0 || opt.charAt(0) != '-') {
                        break;
                    }
                    mArgs = args;
                    mNextArg = 1;

                    opti++;
                    if ("-h".equals(opt)) {
                        showUsage(pw);
                        return;
                    } else if ("-sync".equals(opt)) {
                        synchronized (mBlackNameListManager) {
                            scheduleReadFromXmlLocked();
                            mBlackNameListManager.dumpAllList(fd, pw);
                        }
                        return;
                    } else if ("-monitor".equals(opt)) {
                        enablePackageMonitor(fd, pw);
                        return;
                    } else if ("-log".equals(opt)) {
                        runDebug(fd, pw);
                        return;
                    } else {
                        pw.println("Unknown argument: " + opt + "; use -h for help");
                    }
                }

                pw.println("mAMSCb:" + mAMSCb);
                pw.println("mWMSCb:" + mWMSCb);
                pw.println("mSystemUiCb:" + mSystemUiCb);
                pw.println();
                
                synchronized (mTaskInfos) {
                    dumpTaskInfos(fd, pw, "    ");
                }
                pw.println();
                
                synchronized (mStackInfos) {
                    dumpStackInfos(fd, pw, "    ");
                }
                pw.println();

                pw.println("Dump Black List: ");
                synchronized (mBlackNameListManager) {
                    mBlackNameListManager.dumpAllList(fd, pw);
                }
                pw.println();
            }
        };

        // input current API interface and previous version for varifying
        // compatibility
        registerService("multiwindow_service_v1", binder, null,
                IMultiWindowManager.Stub.class);

        synchronized (mBlackNameListManager) {
            mBlackNameListManager.readFromXmlLocked();
        }

        // listen for package/component changes
        if (PACKAGE_MONITOR_ENABLED){
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addDataScheme("package");
            mContext.registerReceiver(mBroadcastReceiver, filter);
        }
    }

    void scheduleWriteToXmlLocked() {
        if (!mHandler.hasMessages(MSG_WRITE_TO_XML)) {
            mHandler.sendEmptyMessageDelayed(MSG_WRITE_TO_XML, WRITE_TO_XML_DELAY);
        }
    }
    
    void scheduleReadFromXmlLocked() {
        if (!mHandler.hasMessages(MSG_READ_FROM_XML)) {
            mHandler.sendEmptyMessage(MSG_READ_FROM_XML);
        }
    }
    
    void sendDisablePackageBroadcast(String packageName, boolean isAdding){
        Intent intent = new Intent(ACTION_DISABLE_PKG_UPDATED);
        Bundle bundle = new Bundle();
        bundle.putString("packageName", packageName);
        bundle.putBoolean("isAdding", isAdding);
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
    }

    void removeTaskInfo(int taskId) {
        synchronized (mTaskInfos) {
            mTaskInfos.remove(taskId);
        }
        Slog.w(TAG, "removeTaskInfo taskId=" + taskId);
    }

    void addTaskInfo(int taskId) {
        synchronized (mTaskInfos) {
            if (mTaskInfos.get(taskId) == null) {
                TaskInfo taskInfo = new TaskInfo(taskId);
                mTaskInfos.put(taskId, taskInfo);
                Slog.w(TAG, "addTaskInfo taskId=" + taskId);
            }
        }
    }

    void resetMiniMax(int taskId) {
        if (DBG_MWS)
            Slog.v(TAG, "resetMiniMax taskId = " + taskId);
        synchronized (mTaskInfos) {
            TaskInfo taskInfo = mTaskInfos.get(taskId);
            if (taskInfo != null) {
                taskInfo.mInMiniMax = false;
            }
        }
    }

    private String[] mArgs;
    private int mNextArg;

    private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }
    
    private static void showUsage(PrintWriter pw) {
        pw.println("Multi Window Service dump options:");
        pw.println("  sync: sync black list immediately");
        pw.println("  monitor [on/off]: monitor the package removed/updated, and remove from black listxxx.");
        pw.println("  log [on/off]");
    }
    
    private void runDebug(FileDescriptor fd, PrintWriter pw) {
        String type = nextArg();
        if (type == null) {
            System.err.println("Error: didn't specify type of data to list");
            showUsage(pw);
            return;
        }
        if ("on".equals(type)) {
            DBG_MWS = true;
            pw.println("Debug Log:on");
        } else if ("off".equals(type)) {
            DBG_MWS = false;
            pw.println("Debug Log:off");
        } else {
            pw.println("Error argument: " + type + "; use -h for help");
        }
    }

    private void enablePackageMonitor(FileDescriptor fd, PrintWriter pw) {
        String type = nextArg();
        if (type == null) {
            System.err.println("Error: didn't specify type of data to list");
            showUsage(pw);
            return;
        }
        if ("on".equals(type)) {
            PACKAGE_MONITOR_ENABLED = true;
            pw.println("PackageMonitor:on");
        } else if ("off".equals(type)) {
            PACKAGE_MONITOR_ENABLED = false;
            pw.println("PackageMonitor:off");
        } else {
            pw.println("Error argument: " + type + "; use -h for help");
        }
    }

    private void dumpTaskInfos(FileDescriptor fd, PrintWriter pw, String prefix) {
        // Dump task info
        int NT = mTaskInfos.size();
        for (int taskNdx = 0; taskNdx < NT; taskNdx++) {
            TaskInfo task = mTaskInfos.valueAt(taskNdx);
            pw.print(prefix);
            pw.print("TaskInfo #");
            pw.println(task.mTaskId);
            pw.print(prefix);
            pw.print(prefix);
            pw.print(" mInMiniMax=");
            pw.print(task.mInMiniMax);
            pw.println();
            dumpActivityInfos(fd, pw, task.mActivityInfos, "        ");
        }
    }

    private void dumpActivityInfos(FileDescriptor fd, PrintWriter pw,
            ArrayList<ActivityInfo> list, String prefix) {
        // Dump activity info
        int size = list.size();
        for (int activityNdx = 0; activityNdx < size; activityNdx++) {
            ActivityInfo activity = list.get(activityNdx);
            pw.print(prefix);
            pw.print("ActivityInfo #");
            pw.print(" token:");
            pw.print(activity.mToken);
            pw.println();
            pw.print(prefix);
            pw.print("    mRestoring:");
            pw.print(activity.mRestoring);
            pw.print(" mFloating:");
            pw.print(activity.mFloating);
            pw.print(" mFullScreen:");
            pw.print(activity.mFullScreen);
            pw.print(" mTranslucent:");
            pw.print(activity.mTranslucent);
            pw.println();
        }
    }

    private void dumpStackInfos(FileDescriptor fd, PrintWriter pw, String prefix) {

        // Dump stack infos
        int size = mStackInfos.size();
        for (int stackNdx = 0; stackNdx < size; stackNdx++) {
            StackInfo stack = mStackInfos.valueAt(stackNdx);
            pw.print(prefix);
            pw.print("StackInfo #");
            pw.print(stack.mStackId);
            pw.println();
            pw.print(prefix);
            pw.print("    mFloating:");
            pw.print(stack.mFloating);
            pw.print(" mSticky:");
            pw.print(stack.mSticky);
            pw.print(" mNeedKeepFocusActivity:");
            pw.print(stack.mNeedKeepFocusActivity);
            pw.println();
        }
    }

    // For TaskInfo
    public class TaskInfo {
        int mTaskId;
        boolean mPendingRemove;
        final ArrayList<ActivityInfo> mActivityInfos = new ArrayList<ActivityInfo>();
        boolean mInMiniMax;

        public TaskInfo(int taskId) {
            mTaskId = taskId;
            mInMiniMax = false;
            mPendingRemove = false;
        }

        ArrayList<ActivityInfo> getActivityInfos() {
            return mActivityInfos;
        }

    }

    // For ActivityInfo
    public class ActivityInfo {
        IBinder mToken;
        boolean mRestoring;
        boolean mFloating;
        boolean mFullScreen;
        boolean mTranslucent;

        public ActivityInfo(IBinder token) {
            mToken = token;
        }

    }

    // For StackInfo
    public class StackInfo {
        int mStackId;
        boolean mFloating;
        boolean mSticky;
        boolean mNeedKeepFocusActivity;

        public StackInfo(int StackId) {
            mStackId = StackId;
            mFloating = false;
            mSticky = false;
            mNeedKeepFocusActivity = false;
        }
    }

}

