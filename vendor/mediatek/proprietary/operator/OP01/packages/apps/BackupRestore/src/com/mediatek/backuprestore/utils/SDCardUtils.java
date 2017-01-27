package com.mediatek.backuprestore.utils;

import android.content.Context;
import android.os.Looper;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.Environment;
import android.util.Log;

import com.mediatek.backuprestore.R;
import com.mediatek.backuprestore.utils.Constants.LogTag;
import com.mediatek.backuprestore.utils.Constants.ModulePath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SDCardUtils {

    public static final int MINIMUM_SIZE = 512;
    private static String usbPath = Environment.DIRECTORY_USBOTG;

    public static String getStoragePath(String storagePath) {
        if (storagePath == null) {
            return null;
        }
        String storageMountPath = storagePath + File.separator + ModulePath.FOLDER_BACKUP;
        Log.d(LogTag.LOG_TAG, "getStoragePath: path is " + storagePath);
        if(checkStoragePath(storageMountPath) != null ){
            return storageMountPath;
        } 
        return null;
    }
    
    public static String getStoragePath() {
        String sdcardMountPath = getSdCardMountPath();
        if (sdcardMountPath == null) {
            Log.d(LogTag.LOG_TAG, "getStoragePath getSdCardMountPath() == null");
            return null;
        }
        String storagePath = sdcardMountPath + File.separator;
        Log.d(LogTag.LOG_TAG, "getStoragePath: path is " + storagePath);
        if (storagePath == null) {
            return null;
        }
        if(checkStoragePath(storagePath) != null ){
            return storagePath;
        } else {
            String phonePath = getPhoneStoragePath();
            if (sdcardMountPath == null) {
                Log.d(LogTag.LOG_TAG, "getStoragePath getSdCardMountPath() == null");
                return null;
            }
            String phoneStoragePath = phonePath + File.separator + "backup";
            if(checkStoragePath(phoneStoragePath) != null ){
                return phoneStoragePath;
            } else {
                Log.d(LogTag.LOG_TAG, "the phone is no storage");
                return null;
            }
            
        }
    }

    private static String checkStoragePath(String storagePath) {
        File file = new File(storagePath);
        if (file != null) {
            if (file.exists() && file.isDirectory()) {
                File temp = new File(storagePath + File.separator + ".BackupRestoretemp");
                boolean ret;
                if (temp.exists()) {
                    ret = temp.delete();
                } else {
                    try {
                        ret = temp.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LogTag.LOG_TAG, "getStoragePath: " + e.getMessage());
                        ret = false;
                    } finally {
                        temp.delete();
                    }
                }
                if (ret) {
                    return storagePath;
                } else {
                    return null;
                }

            } else if (file.mkdir()) {
                return storagePath;
            }
        } 
        return null;
    }
    
    public static boolean isSupprotSDcard() {
        StorageManager storageManager = null;
        String mPath = null;
        try {
            storageManager = new StorageManager(null, Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }

        StorageVolume[] volumes = storageManager.getVolumeList();
        if (volumes != null) {
            for (StorageVolume volume : volumes) {
                mPath = volume.getPath();
                if (mPath != null && volume.isRemovable() && !mPath.matches(usbPath)) {
                    return true;
                } 
            }
        }
        return false;
    }

    public static String getPhoneStoragePath() {
        StorageManager storageManager = null;
        String mPhonePath = null;
        try {
            storageManager = new StorageManager(null, Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }

        StorageVolume[] volumes = storageManager.getVolumeList();
        if (volumes != null) {
            for (StorageVolume volume : volumes) {
                if (!volume.isRemovable()) {
                    mPhonePath = volume.getPath();
                    return mPhonePath;
                }
            }
        }
        return mPhonePath;
    }
        
    
    public static String getSdCardMountPath() {
        StorageManager storageManager = null;

        try {
            storageManager = new StorageManager(null, Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }

        StorageVolume[] volumes = storageManager.getVolumeList();
        if (volumes != null) {
            for (StorageVolume volume : volumes) {
                if (volume.isRemovable()) {
                    String path = volume.getPath();
                    if (path != null && !path.matches(usbPath)) {
                        return path;
                    }
                }
            }
        }
        Log.v(LogTag.LOG_TAG, "getSdCardMountPath() = null");
        return null;
    }
    
    public static List<String> getPathList() {
        StorageManager storageManager = null;
        List<String> pathList = new ArrayList<String>();
        try {
            storageManager = new StorageManager(null, Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }

        StorageVolume[] volumes = storageManager.getVolumeList();
        if (volumes != null) {
            for (StorageVolume volume : volumes) {
                String path = volume.getPath();
                if (path != null && !path.matches(usbPath)) {
                    pathList.add(path);
                }
            }
        }
        return pathList;
    }

    public static String getPersonalDataBackupPath(String path) {
        if (path != null) {
            return path + File.separator + ModulePath.FOLDER_DATA;
        }
        return path;
    }

    public static String getAppsBackupPath(String path) {
        if (path != null) {
            return path + File.separator + ModulePath.FOLDER_APP;
        }
        return path;
    }

    public static boolean isSdCardAvailable() {
        return (getSDCardDataPath() != null);
    }

    public static boolean isSdcardFull() {
        String path = getSdCardMountPath();
        if (path == null) {
             return false;
        }
        Log.v(LogTag.LOG_TAG, "path =  " + path);

        long count = 0;
        try {
            android.os.StatFs stat = new android.os.StatFs(path);
            count = stat.getBlockCount();
        } catch (IllegalArgumentException e) {
            Log.v(LogTag.LOG_TAG, "android.os.StatFs exception.");
            e.printStackTrace();
            return false;
        }

        long size = getAvailableSize(path);
        if (size <= 512 && count > 0) {
            Log.v(LogTag.LOG_TAG, "isSdcardFull = true.");
            return true;
        } else {
            Log.v(LogTag.LOG_TAG, "isSdcardFull = false");
            return false;
        }
    }

    public static long getAvailableSize(String file) {
        android.os.StatFs stat = new android.os.StatFs(file);
        long count = stat.getAvailableBlocks();
        long size = stat.getBlockSize();
        long totalSize = count * size;
        Log.v(LogTag.LOG_TAG, "file remain size = " + totalSize);
        return totalSize;
    }

    public static String getSDStatueMessage(Context context) {
        String message = context.getString(R.string.nosdcard_notice);
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED) ||
            status.equals(Environment.MEDIA_UNMOUNTED)) {
            message = context.getString(R.string.sdcard_busy_message);
        }
        return message;
    }
    
    
    public static String getSDCardDataPath() {
        return SDCardUtils.getStoragePath(SDCardUtils.getSdCardMountPath());
    }
    
    public static String getPhoneDataPath() {
        return SDCardUtils.getStoragePath(SDCardUtils.getPhoneStoragePath());
    }

    public static boolean checkedPath(String path) {
        File checkedFile = new File(path); 
        if (checkedFile != null) {
            if (checkedFile.exists() && checkedFile.isDirectory()) {
                File temp = new File(path + File.separator + ".BackupRestoretemp");
                boolean ret;
                if (temp.exists()) {
                    ret = temp.delete();
                } else {
                    try {
                        ret = temp.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LogTag.LOG_TAG, "checkedPath: " + e.getMessage());
                        ret = false;
                    } finally {
                        temp.delete();
                    }
                }
                Log.e(LogTag.LOG_TAG, "checkedPath: ret = " + ret);
                if (ret) {
                    return true;
                } else {
                    return false;
                }
            }
            
            if(checkedFile.exists() && checkedFile.isFile()) {
                Log.e(LogTag.LOG_TAG, "checkedPath: the path is a File and it is exists! ");
                return true;
            }
        }
        return false;
    }
}
