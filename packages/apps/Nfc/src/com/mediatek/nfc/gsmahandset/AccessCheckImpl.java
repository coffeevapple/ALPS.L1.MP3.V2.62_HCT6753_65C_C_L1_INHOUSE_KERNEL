package com.mediatek.nfc.gsmahandset;


import java.util.List;

import java.util.ArrayList;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import android.content.ComponentName;
import android.content.ServiceConnection;


//import android.media.MediaScannerConnection;
import android.net.Uri;



import android.os.IBinder;

//import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemProperties;

import android.util.Log;






import com.mediatek.nfc.addon.MtkNfcAddonSequence;

import org.simalliance.openmobileapi.service.ISmartcardService;
import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.SmartcardError;



/**
 *     AccessCheckImpl of GSAM NFCHandset
 *      main implement of Gemalto test requirement
 */
public class AccessCheckImpl {


    static final String TAG = "AccessCheckImpl";
    static final boolean DBG = true;

    //GSMA 3.0 EvtTransaction intent
    public static final String ACTION_TRANSACTION_DETECTED =
            "android.nfc.ACTION_TRANSACTION_DETECTED";

    //GSMA 3.0 PERMISSION
    public static final String NFC_TRANSACTION_PERMISSION =
            "android.permission.NFC_TRANSACTION";


    //GSMA 6.0 EvtTransaction intent
    public static final String ACTION_TRANSACTION_DETECTED_6_0 = "com.gsma.services.nfc.action.TRANSACTION_EVENT";
    public static final String EXTRA_DATA_6_0 = "com.gsma.services.nfc.extra.DATA";
    public static final String EXTRA_AID_6_0 = "com.gsma.services.nfc.extra.AID";

    //GSMA 6.0 PERMISSION
    public static final String NFC_TRANSACTION_PERMISSION_60 = "com.gsma.services.nfc.permission.TRANSACTION_EVENT";





    final Context mContext;

    private static AccessCheckImpl sStaticInstance = null;

    private ISmartcardService mService = null;

    final Object mLock = new Object();
    // Variables below synchronized on mLock

    //boolean mBound;

    String noActiveSe   = "SE_DEACTIVE";
    String sim1String   = "SIM1";
    String sim2String   = "SIM2";
    String sdString     = "SD";
    String eSeString    = "eSE";

    //mActiveSe should map to SecureElementSelector.USER_SIM1:
    private final String[] mActiveSeMap = {noActiveSe, sim1String, sim2String, sdString, eSeString};

    String mActiveString;

    int mVersion;

    /**
     * This implementation is used to receive callbacks from backend.
     */
    private final ISmartcardServiceCallback mCallback = new ISmartcardServiceCallback.Stub() {
    };

    public AccessCheckImpl(Context context) {
        mContext = context;
        Log.i(TAG, " AccessCheckImpl Construct ");

        initSmartCardService();
    }

    public static void createSingleton(Context context) {
        if (sStaticInstance == null) {
            sStaticInstance = new AccessCheckImpl(context);
        }
    }

    public static AccessCheckImpl getInstance() {
        return sStaticInstance;
    }

    /*
     *   method accessControlCheck()
     *
     *      use method isNFCEventAllowed() of smartCardService to check is access rule exist in the termianl.(UICC)
     *
     *      do AccessControl check with specific AID
     *
     *
     *
     * */
    public List<ResolveInfo> accessControlCheck(byte[] aid, List<ResolveInfo> toCheckInfos) {
        Log.i(TAG, "accessControlCheck  toCheckInfos.size():" + toCheckInfos.size());

        SmartcardError result = new SmartcardError();
        String[] readers = null;
        List<ResolveInfo> acPassResolveInfos = new ArrayList<ResolveInfo>();

        int pkgCount = 0;
        String[] pkgNames = new String[toCheckInfos.size()];
        boolean[] boolRet = null;
        int activeReader = 0;

        if (mService == null) {
            Log.i(TAG, "!!!! mService == null !!!!");
        }

        for (ResolveInfo r : toCheckInfos) {
            Log.d(TAG, "pkgCount:" + pkgCount + " r.activityInfo.packageName: " + r.activityInfo.packageName);
            //new String[toCheckInfos.size()]
            pkgNames[pkgCount] = r.activityInfo.packageName;
            pkgCount++;
        }



        try {
            int count = 0;

            readers = mService.getReaders(result);

            for (String readerString : readers) {
                Log.i(TAG, " count:" + count + "  readerString:" + readerString);
                if (mActiveString.equalsIgnoreCase(noActiveSe) || mActiveString.equalsIgnoreCase(sdString)) {
                    Log.i(TAG, "!!!! mActiveString:" + mActiveString + ", By Pass AccessControl Check !!!!" + readerString);
                    return null;
                }
                else if (mActiveString.startsWith(readerString.substring(0, 2))) {
                    //we suppose Reader is SIM or eSE, so cut 3 char font to copmare
                    //ex:
                    //        readerString:SIM: UICC
                    //        readerString:eSE: SmartMX
                    Log.i(TAG, " mActiveString:" + mActiveString + ", equal to Reader [" + count + "]");
                    activeReader = count;
                }
                count++;
            }

        } catch (RemoteException e) {
            Log.d(TAG, "mService.getReaders  RemoteException:" + e);
            e.printStackTrace();

            return null;
        } catch (Exception exception) {
            Log.d(TAG, "mService.getReaders   Exception:" + exception);
            exception.printStackTrace();
            return null;
        }



        try {
            boolRet = mService.isNFCEventAllowed(readers[activeReader],
                    aid,
                    pkgNames,
                    mCallback,
                    result);
        } catch (RemoteException e) {
                   Log.d(TAG, "mService.isNFCEventAllowed  RemoteException:" + e);
                   e.printStackTrace();
            return null;
        } catch (Exception exception) {
            Log.d(TAG, "mService.isNFCEventAllowed   Exception:" + exception);
            exception.printStackTrace();
            return null;
        }

        if (boolRet == null) {
            Log.i(TAG, " boolRet == null  ");
            return null;
        }

        Log.d(TAG, "==================================:");

        pkgCount = 0;

        for (boolean bRet : boolRet) {
            Log.d(TAG, "boolean bRet:" + bRet);
            if (bRet == true)
                acPassResolveInfos.add(toCheckInfos.get(pkgCount));

            pkgCount++;
        }
        Log.d(TAG, "==================================:");


        pkgCount = 0;
        for (ResolveInfo r : acPassResolveInfos) {
            Log.d(TAG, "acPassResolveInfos count:" + pkgCount + " r.activityInfo.packageName: " + r.activityInfo.packageName);
            pkgCount++;
        }



        return acPassResolveInfos;
    }

    public String getActiveSeName() {
        Log.i(TAG, "getActiveSeName  ");

        int activeSe = MtkNfcAddonSequence.getInstance().getActiveSeValue();

        Log.i(TAG, "SecureElementSelector.getActiveSeValue() :" + activeSe);

        return mActiveString = mActiveSeMap[activeSe];

    }

    /*
     *   method getInterestedPackage() do the following two things.
     *
     *   0.getActiveSeName
     *   1.Query interested package
     *   2.check Permission
     *
     *
     * */
    public List<ResolveInfo> getInterestedPackage(Intent composeIntent) {
        Log.d(TAG, "getInterestedPackage  composeIntent:" + composeIntent);

        //1.Query interested package
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        List<ResolveInfo> perCheckResolveInfos = new ArrayList<ResolveInfo>();

        Intent queryIntent; //= new Intent();
        int count = 0;

        queryIntent = composeIntent; //composeNewIntent(aidString);
        queryIntent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);

        Log.d(TAG, "queryIntent: " + queryIntent);
        //Log.d(TAG, "queryIntent.getScheme(): "+queryIntent.getScheme());

        PackageManager pm = mContext.getPackageManager();

        try {
            resolveInfos = pm.queryBroadcastReceivers(queryIntent, 0);
            Log.d(TAG, "resolveInfos:" + resolveInfos);

        } catch (Exception e) {
            Log.d(TAG, "exception during pm.queryBroadcastReceivers");
            e.printStackTrace();
        } finally {
            //return;
        }


        //2.check Permission
        Log.d(TAG, "resolveInfos.size:" + resolveInfos.size());
        for (ResolveInfo r : resolveInfos) {
            Log.d(TAG, "====  queryBroadcastReceivers result  count:" + count + " ====");

            if (r.filter != null)
                Log.d(TAG, "r.filter:" + r.filter);
            Log.d(TAG, "r.priority:" + r.priority + "  r.activityInfo.packageName: " + r.activityInfo.packageName);
            //Log.d(TAG, "r.activityInfo.applicationInfo.packageName: " + r.activityInfo.applicationInfo.packageName);

            switch(mVersion){
                case NfcGsmaRuntimeOptions.VERSION_6_0:
                    //version 6.0
                    int result=pm.checkPermission(NFC_TRANSACTION_PERMISSION_60, r.activityInfo.packageName);
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "version 6.0, pm.checkPermission  Pass ");
                        perCheckResolveInfos.add(r);
                        // do something
                    } else {
                        Log.i(TAG, "version 6.0, pm.checkPermission Fail, delete  result:"+result);
                        Log.e(TAG, "exception, version 6.0, pm.checkPermission Fail -- com.gsma.service.nfc.permission.TRANSACTION_EVENT  ");
                        //resolveInfos.remove(r);
                        // do something
                    }
                    break;
            
                case NfcGsmaRuntimeOptions.VERSION_UNKNOW:
                case NfcGsmaRuntimeOptions.VERSION_3_0:                
                default:
                    //version 3.0
            if (pm.checkPermission(NFC_TRANSACTION_PERMISSION, r.activityInfo.packageName)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, " pm.checkPermission  Pass ");
                perCheckResolveInfos.add(r);
                // do something
            } else {
                Log.i(TAG, " pm.checkPermission  Fail delete ");
                //resolveInfos.remove(r);
                // do something
            }
                    break;
            }

            count++;

        }

        Log.d(TAG, "per.CheckResolveInfos.size:" + perCheckResolveInfos.size());
        return perCheckResolveInfos;

    }

    /*      priorityDispatch(String aidString,List<ResolveInfo> resolveInfos)
     *
     *        1.get Max ResolveInfo List
     *        2.Check maxList size. If only exist one item,single broadcast
     *        3.Check the install time with same priority
     * */
    public boolean priorityDispatch(Intent composeIntent, List<ResolveInfo> resolveInfos) {
        Log.d(TAG, "priorityDispatch ,resolveInfos.size():" + resolveInfos.size());

        int count = 0;
        int maxPriority = 0;


        if (resolveInfos.size() == 0) {
            Log.d(TAG, "resolveInfos.size() == 0 , return");
            return false;
        }

        //1.get Max ResolveInfo List
        List<ResolveInfo> maxResolveInfoList = new ArrayList<ResolveInfo>();

        for (ResolveInfo r : resolveInfos) {
            Log.d(TAG, "========  toComparePriorityList count:" + count + " ========");
            Log.d(TAG, "r.priority:" + r.priority + " maxPriority:" + maxPriority);
            //Log.d(TAG, "r.filter:"+r.filter);
            Log.d(TAG, "r.activityInfo.packageName: " + r.activityInfo.packageName);

            count++;

            if (r.priority >= maxPriority) {
                if (r.priority > maxPriority) {
                    Log.d(TAG, "Clear Max List");
                    maxResolveInfoList.clear();
                }
                maxPriority = r.priority;
                Log.d(TAG, "add element to the head");

                maxResolveInfoList.add(0, r);
            } else {
                Log.d(TAG, "small element ,do nothing");
                //maxResolveInfoList.clear();//add(r);
            }
        }

        //2.Check maxList size. If only exist one item,single broadcast
        Log.d(TAG, "maxResolveInfoList.size():" + maxResolveInfoList.size());

        if (maxResolveInfoList.size() == 1) {
            String highestPackageName = "";
            highestPackageName = maxResolveInfoList.get(0).activityInfo.packageName;

            Log.d(TAG, "maxResolveInfoList.size() == 1, sendBroadcast to " + highestPackageName);

            uniBroadcastAction(composeIntent, highestPackageName);
            Log.d(TAG, "return true");
            return true;
        }



        //3. Check the install time with same priority
        //compareInstalltimeDispatch(maxPriority,aidString,SortResolveInfoList);
        String earlyInstallPackageName = "";
        long earlyInstallTime = 0;
        PackageInfo pkgInfo = null;

        PackageManager pm = mContext.getPackageManager();
        Log.d(TAG, "======= Get install time =============");

        for (ResolveInfo r : maxResolveInfoList) {

            if (maxPriority != r.priority) {
                Log.d(TAG, "!!!! should exception, maxPriority:" + maxPriority + "  r.priority:" + r.priority);
            }

                try {
                    pkgInfo = pm.getPackageInfo(r.activityInfo.packageName, 0);
                } catch (NameNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    continue;
                }

                if (pkgInfo != null) {
                    Log.d(TAG, "r.priority:" + r.priority + " pkg name:" + r.activityInfo.packageName);
                    Log.d(TAG, " install time:" + pkgInfo.firstInstallTime);
                }

                if (earlyInstallTime == 0) {
                    earlyInstallTime = (pkgInfo != null) ? pkgInfo.firstInstallTime : 0;
                    earlyInstallPackageName = r.activityInfo.packageName;
                } else {
                    if ((pkgInfo != null) && (pkgInfo.firstInstallTime < earlyInstallTime)) {
                        earlyInstallTime = pkgInfo.firstInstallTime;
                        earlyInstallPackageName = r.activityInfo.packageName;
                    }
                }

        }

        if (earlyInstallPackageName.isEmpty()) {
            Log.d(TAG, "!! earlyInstallPackageName isEmpty !! ,return false");
            return false;
        } else {
            Log.d(TAG, "!! find install pkg with earliest !! :" + earlyInstallPackageName);
            uniBroadcastAction(composeIntent, earlyInstallPackageName);
            return true;
        }

    }


    public Intent composeNewIntent(byte[] aid, byte[] data) {

        Intent composeIntent = new Intent();



        String aidString = bytesToString(aid);
        Log.i(TAG, "aidString: " + aidString);

        if (aidString.isEmpty()) {
            Log.i(TAG, " aidString.isEmpty ,return null");
            return null;
        }


        mVersion = NfcGsmaRuntimeOptions.getGsmaVersion();

        Log.i(TAG, " mVersion: 0x" + Integer.toHexString(mVersion));

        switch(mVersion){
            case NfcGsmaRuntimeOptions.VERSION_6_0:
                //version 6.0
                composeIntent.setAction(ACTION_TRANSACTION_DETECTED_6_0);
                composeIntent.putExtra(EXTRA_AID_6_0, aid);
                composeIntent.putExtra(EXTRA_DATA_6_0, data);

                break;
              

            case NfcGsmaRuntimeOptions.VERSION_UNKNOW:
            case NfcGsmaRuntimeOptions.VERSION_3_0:                
            default:
                //version 3.0
                composeIntent.setAction(ACTION_TRANSACTION_DETECTED);
        composeIntent.putExtra(EvtTransactionHandle.EXTRA_AID, aid);
        composeIntent.putExtra(EvtTransactionHandle.EXTRA_DATA, data);
                break;
        }




        //nfc://secure:0/<SE_Name>/<AID>
        String uiccName = getActiveSeName();

        Uri schemeData = Uri.parse("nfc://secure:0/" + uiccName + "/" + aidString);

        composeIntent.setData(schemeData); // set the Uri,://secure:0/SIM1/00010203
        //composeIntent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);

        Log.d(TAG, "composeNewIntent: " + composeIntent);
        //Log.d(TAG, "queryIntent.getScheme(): "+composeIntent.getScheme());

        return composeIntent;
    }


    public void multiBroadcastAction(Intent broadcastIntent, List<ResolveInfo> resolveInfos) {

            Log.d(TAG, "multiBroadcastAction resolveInfos:" + resolveInfos);
            int count = 0;

            //Intent broadcastIntent = broadcastIntent;//composeNewIntent(aidString);
            Log.d(TAG, "broadcastIntent:" + broadcastIntent);

            for (ResolveInfo r : resolveInfos) {
                Log.d(TAG, "===================multiBroadcast  count:" + count + " ==============");
                Log.d(TAG, "r.priority:" + r.priority);
                Log.d(TAG, "r.filter:" + r.filter);
                Log.d(TAG, "r.activityInfo.packageName: " + r.activityInfo.packageName);


                count++;
                broadcastIntent.setPackage(r.activityInfo.packageName);
                Log.d(TAG, "sendBroadcast to " + r.activityInfo.packageName);
                mContext.sendBroadcast(broadcastIntent);
            }

    }

    void uniBroadcastAction(Intent broadcastIntent, String PackageName) {

        Log.d(TAG, "uni sendBroadcast to " + PackageName);
        //Intent broadcastIntent = broadcastIntent;//composeNewIntent(aidString);
        Log.d(TAG, "broadcastIntent:" + broadcastIntent);
        broadcastIntent.setPackage(PackageName);

        mContext.sendBroadcast(broadcastIntent);
    }



    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mLock) {

                Log.d(TAG, " onServiceConnected  name:" + name + " service:" + service);

                mService = ISmartcardService.Stub.asInterface(service);
                //mBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {

                Log.d(TAG, " onServiceDisconnected  name:" + name);
                if (mService != null) {
                    //handling here
                }
                mService = null;
                //mBound = false;
            }
        }
    };

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if(action == null){
                Log.e(TAG, "onReceive() action == null");
                return;
            } 


            if (action.equals("android.intent.action.NFC_BindSmartCardService")) {

                Log.d(TAG, "rec. android.intent.action.NFC_BindSmartCardService    bindSmartCardService");
                final String off = SystemProperties.get("persist.radio.seapi.off", "0");
                Log.d(TAG, "persist.radio.seapi.off:" + off);

                // Re-bind a service
                if (mService == null && off.equals("0")) {
                    bindSmartCardService();
                }

            }

        }
    };

    private void initSmartCardService() {

        Log.d(TAG, " initSmartCardService()");

        IntentFilter filter = new IntentFilter("android.intent.action.NFC_BindSmartCardService");

        mContext.registerReceiver(mReceiver, filter, null, null);

        //bindSmartCardService();
    }

    private void bindSmartCardService() {

        Log.d(TAG, " bindSmartCardService  ISmartcardService.class.getName():" + ISmartcardService.class.getName());

        try {
            //SetClassName & Bind fail

            final PackageManager packageManager = mContext.getPackageManager();
            //final Intent intent = new Intent(mContext, MyService.class);

            Intent queryIntent = new Intent(ISmartcardService.class.getName());
            List resolveInfo = packageManager.queryIntentServices(queryIntent,
                            PackageManager.GET_RESOLVED_FILTER);//PackageManager.MATCH_DEFAULT_ONLY fail

            Log.d(TAG, "query result, resolveInfo.size():" + resolveInfo.size());

            if (resolveInfo.size() > 0) {


                //for(ResolveInfo info : l){
                ResolveInfo explicitResolveInfo = (ResolveInfo) resolveInfo.get((int) 0);
                ServiceInfo servInfo = explicitResolveInfo.serviceInfo;
                ComponentName name = new ComponentName(servInfo.applicationInfo.packageName, servInfo.name);


                Log.d(TAG, " explicit intent  name:" + name);

                Intent intent = new Intent(ISmartcardService.class.getName());
                intent.setComponent(name);

                //startService(i2);
                mContext.bindService(intent, mConnection,
                    Context.BIND_AUTO_CREATE);

            }




        } catch (Exception e) {
            Log.e(TAG, "bindSmartCardService() Exception:" + e);
            e.printStackTrace();
        }


    }

    String bytesToString(byte[] bytes) {
        if (bytes == null)
            return "";
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        String str = sb.toString();
        Log.d(TAG, "bytesToString str:(sb.toString())" + str);
        Log.d(TAG, "bytesToString  str.length():" + str.length());

        //if (str.length() > 0) {
        //    str = str.substring(0, str.length() - 1);
        //}
        return str;
    }

    //GSMA 6.0
    public ISmartcardService getSmartCardService() {
        return mService;
    }
    
}

