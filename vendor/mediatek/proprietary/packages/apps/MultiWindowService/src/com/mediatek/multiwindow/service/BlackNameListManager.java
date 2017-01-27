package com.mediatek.multiwindow.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import com.android.internal.util.FastXmlSerializer;

import android.util.ArraySet;
import android.util.Slog;
import android.os.FileUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import static android.os.Process.PACKAGE_INFO_GID;
import static android.os.Process.SYSTEM_UID;
/**
test xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <MinimaxRestartList>
        <PackageName>com.android.calendar</PackageName>
        <WindowName>com.android.contacts</WindowName>
    </MinimaxRestartList>
    <ConfigNotChangeList>
        <PackageName>com.android.calculator2</PackageName>
        <WindowName>com.android.calculator2</WindowName>
    </ConfigNotChangeList>
    <DisableFloatList>
        <PackageName>com.android.settings</PackageName>
        <WindowName>com.android.settings</WindowName>
    </DisableFloatList>
</root>

**/

class BlackNameListManager {

    static final String TAG_MiniMaxRestartList = "MinimaxRestartList";
    static final String TAG_ConfigNotChangeList = "ConfigNotChangeList";
    static final String TAG_DisableFloatList = "DisableFloatList";
    static final String TAG_ConfigChangeList = "ConfigChangeList";

    static final String TAG_PackageName = "PackageName";
    static final String TAG_ActivityName = "ActivityName";
    static final String TAG_WindowName = "WindowName";

    DisableFloatList disableFloatList = new DisableFloatList();
    MiniMaxRestartList restartList = new MiniMaxRestartList();
    ConfigNotChangeList configNotChangeList = new ConfigNotChangeList();
    ConfigChangeList configChangeList = new ConfigChangeList();
    
    private static final String BLACKLIST_INIT_FILEPATH = "system/etc/blacklist.xml";
    private static final String BLACKLIST_FILEPATH = "data/system/blacklist.xml";
    
    void readFromXmlLocked(){
        try {
            File file = new File(BLACKLIST_FILEPATH); 
            if (!file.exists())
                file = new File(BLACKLIST_INIT_FILEPATH); 
                
            String xml = read(file);
            ByteArrayInputStream bin = new ByteArrayInputStream(xml.getBytes());
            InputStreamReader in = new InputStreamReader(bin);
        
            XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullFactory.newPullParser();

            boolean toDisableList = false;
            boolean toRestartList = false;
            boolean toConfigNotChangeList = false;
            boolean toConfigChangeList = false;
            
            configNotChangeList.clear();
            configChangeList.clear();
            restartList.clear();
            disableFloatList.clear();
            
            parser.setInput(in);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = parser.getName();
                switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (nodeName.equals(TAG_MiniMaxRestartList)) {
                        toRestartList = true;
                    } else if (nodeName.equals(TAG_ConfigNotChangeList)) {
                        toConfigNotChangeList = true;
                    } else if (nodeName.equals(TAG_DisableFloatList)) {
                        toDisableList = true;
                    } else if (nodeName.equals(TAG_ConfigChangeList)) {
                        toConfigChangeList = true;
                        
                    } else if (nodeName.equals(TAG_PackageName)) {
                        if (toRestartList) {
                            restartList.packageNameList.add(parser.nextText());
                        } else if (toConfigNotChangeList) {
                            configNotChangeList.packageNameList.add(parser.nextText());
                        } else if (toConfigChangeList) {
                            configChangeList.packageNameList.add(parser.nextText());
                        } else if (toDisableList) {
                            disableFloatList.packageNameList.add(parser.nextText());
                        }

                    } else if (nodeName.equals(TAG_ActivityName)) {
                        if (toRestartList) {
                            restartList.activityNameList.add(parser.nextText());
                        } else if (toDisableList) {
                            disableFloatList.activityNameList.add(parser.nextText());
                        }

                    } else if (nodeName.equals(TAG_WindowName)) {
                        if (toRestartList) {
                            restartList.windowNameList.add(parser.nextText());
                        } else if (toDisableList) {
                            disableFloatList.windowNameList.add(parser.nextText());
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (nodeName.equals(TAG_MiniMaxRestartList)) {
                        toRestartList = false;
                    } else if (nodeName.equals(TAG_ConfigNotChangeList)) {
                        toConfigNotChangeList = false;
                    } else if (nodeName.equals(TAG_DisableFloatList)) {
                        toDisableList = false;
                    } else if (nodeName.equals(TAG_ConfigChangeList)) {
                        toConfigChangeList = false;
                    }
                default:
                    break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String read(File src) {
        StringBuffer res = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(src));
            while ((line = reader.readLine()) != null) {
                res.append(line + "\n");
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res.toString();
    }

    void writeToXmlLocked() {
        try {
            FileOutputStream fstr = new FileOutputStream(BLACKLIST_FILEPATH);
            BufferedOutputStream str = new BufferedOutputStream(fstr);
            FileUtils.setPermissions(BLACKLIST_FILEPATH, 0660, SYSTEM_UID, SYSTEM_UID);
            // XmlSerializer serializer = XmlUtils.serializerInstance();
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(str, "utf-8");
            serializer.startDocument(null, true);
            serializer.startTag(null, "root");

            configNotChangeList.writeToXml(serializer);
            configChangeList.writeToXml(serializer);
            restartList.writeToXml(serializer);
            disableFloatList.writeToXml(serializer);

            serializer.endTag(null, "root");
            serializer.endDocument();

            str.flush();
            FileUtils.sync(fstr);
            str.close();
        } catch (Exception e) {
            Slog.e("XmlParser", "IOException " + e.toString());
        }
    }

    void removePkgLocked(String packageName) {
        disableFloatList.removePkgLocked(packageName);
        configChangeList.removePkgLocked(packageName);
        configNotChangeList.removePkgLocked(packageName);
        restartList.removePkgLocked(packageName);
    }

    boolean matchConfigNotChangeList(String packageName) {
        return configNotChangeList.matchPkgList(packageName);
    }
    
    boolean matchDisableFloatPkgList(String packageName) {
        return disableFloatList.matchPkgList(packageName);
    }
    
    boolean matchDisableFloatActivityList(String activityName) {
        return disableFloatList.matchActivityList(activityName);
    }
    
    boolean matchDisableFloatWinList(String packageName) {
        return disableFloatList.matchWinList(packageName);
    }
    
    List<String> getDisableFloatPkgList() {
        return disableFloatList.getDisablePkgList();
    }

    List<String> getDisableFloatComponentList() {
        return disableFloatList.getDisableActivityList();
    }
    
    boolean matchMinimaxRestartList(String packageName) {
        return restartList.matchPkgList(packageName);
    }

    boolean matchConfigChangeList(String packageName) {
        return configChangeList.matchPkgList(packageName);
    }
    
    void dumpAllList(FileDescriptor fd, PrintWriter pw) {
        String prefix = "    ";
        // BlackNameList
        pw.println();
        pw.println(TAG_DisableFloatList + ":");
        pw.print(prefix);
        pw.println(TAG_PackageName + ":");
        for (String s : disableFloatList.packageNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }

        pw.print(prefix);
        pw.println(TAG_ActivityName + ":");
        for (String s : disableFloatList.activityNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }

        pw.print(prefix);
        pw.println(TAG_WindowName + ":");
        for (String s : disableFloatList.windowNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }

        // RestartList
        pw.println();
        pw.println(TAG_MiniMaxRestartList + ":");
        pw.print(prefix);
        pw.println(TAG_PackageName + ":");
        for (String s : restartList.packageNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }

        pw.print(prefix);
        pw.println(TAG_ActivityName + ":");
        for (String s : restartList.activityNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }

        pw.print(prefix);
        pw.println(TAG_WindowName + ":");
        for (String s : restartList.windowNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }

        // ConfigNotChangeList
        pw.println();
        pw.println(TAG_ConfigNotChangeList + ":");
        pw.print(prefix);
        pw.println(TAG_PackageName + ":");
        for (String s : configNotChangeList.packageNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }

        // ConfigChangeList
        pw.println();
        pw.println(TAG_ConfigChangeList + ":");
        pw.print(prefix);
        pw.println(TAG_PackageName + ":");
        for (String s : configChangeList.packageNameList) {
            pw.print(prefix);
            pw.print(prefix);
            pw.println(s);
        }
    }

    class DisableFloatList {
        ArraySet<String> packageNameList = new ArraySet<String>();
        ArraySet<String> activityNameList = new ArraySet<String>();
        ArraySet<String> windowNameList = new ArraySet<String>();

        void writeToXml(XmlSerializer serializer) throws java.io.IOException {

            serializer.startTag(null, TAG_DisableFloatList);
            for (String s : packageNameList) {
                serializer.startTag(null, TAG_PackageName);
                serializer.text(s);
                serializer.endTag(null, TAG_PackageName);
            }

            for (String s : activityNameList) {
                serializer.startTag(null, TAG_ActivityName);
                serializer.text(s);
                serializer.endTag(null, TAG_ActivityName);
            }

            for (String s : windowNameList) {
                serializer.startTag(null, TAG_WindowName);
                serializer.text(s);
                serializer.endTag(null, TAG_WindowName);
            }

            serializer.endTag(null, TAG_DisableFloatList);
        }

        boolean matchPkgList(String name) {
            if (packageNameList.contains(name))
                return true;
            return false;
        }

        boolean matchWinList(String name) {
            if (windowNameList.contains(name))
                return true;
            return false;
        }

        boolean matchActivityList(String name) {
            // M: Adding the package name as prefix for class name,
            // if the name is short component name.
            // Ex, complements the short component name 
            // "com.mediatek.miravision.ui/.MiraVisionActivity"  as 
            // "com.mediatek.miravision.ui/com.mediatek.miravision.ui.MiraVisionActivity"
            String sArray[] = name.split("/");
            String fullAcName = name;
            
            if (sArray[1].startsWith(".")) {
                sArray[1] = sArray[0]+sArray[1];
                fullAcName = sArray[0]+ "/" + sArray[1];
            }
            
            if (activityNameList.contains(fullAcName))
                return true;
            return false;
        }

        boolean match(String name) {
            if (matchPkgList(name))
                return true;
            if (matchWinList(name))
                return true;
            if (matchActivityList(name))
                return true;
            return false;
        }

        List<String> getDisablePkgList() {
            List<String> list = new ArrayList<String>();
            for (String s : packageNameList)
                list.add(s);
            return list;
        }

        List<String> getDisableActivityList() {
            List<String> list = new ArrayList<String>();
            for (String s : activityNameList)
                list.add(s);
            return list;
        }
        
        void addPkgLocked(String packageName) {
            packageNameList.add(packageName);
        }

        void removePkgLocked(String packageName) {
            packageNameList.remove(packageName);
        }

        void clear() {
            packageNameList.clear();
            activityNameList.clear();
            windowNameList.clear();
        }

    }

    class MiniMaxRestartList {
        ArraySet<String> packageNameList = new ArraySet<String>();
        ArraySet<String> activityNameList = new ArraySet<String>();
        ArraySet<String> windowNameList = new ArraySet<String>();

        void writeToXml(XmlSerializer serializer) throws java.io.IOException {
            serializer.startTag(null, TAG_MiniMaxRestartList);
            for (String s : packageNameList) {
                serializer.startTag(null, TAG_PackageName);
                serializer.text(s);
                serializer.endTag(null, TAG_PackageName);
            }

            for (String s : activityNameList) {
                serializer.startTag(null, TAG_ActivityName);
                serializer.text(s);
                serializer.endTag(null, TAG_ActivityName);
            }

            for (String s : windowNameList) {
                serializer.startTag(null, TAG_WindowName);
                serializer.text(s);
                serializer.endTag(null, TAG_WindowName);
            }

            serializer.endTag(null, TAG_MiniMaxRestartList);
        }

        boolean matchPkgList(String name) {
            if (packageNameList.contains(name))
                return true;
            return false;
        }

        boolean matchActivityList(String name) {
            if (activityNameList.contains(name))
                return true;
            return false;
        }

        boolean matchWinList(String name) {
            if (windowNameList.contains(name))
                return true;
            return false;
        }

        boolean match(String name) {
            if (matchPkgList(name))
                return true;
            if (matchWinList(name))
                return true;
            if (matchActivityList(name))
                return true;
            return false;
        }

        ArraySet<String> getPkgList() {
            return packageNameList;
        }

        void addPkgLocked(String packageName) {
            packageNameList.add(packageName);
        }

        void removePkgLocked(String packageName) {
            packageNameList.remove(packageName);
        }

        void clear() {
            packageNameList.clear();
            activityNameList.clear();
            windowNameList.clear();
        }
    }

    class ConfigNotChangeList {
        ArraySet<String> packageNameList = new ArraySet<String>();

        void writeToXml(XmlSerializer serializer) throws java.io.IOException {
            serializer.startTag(null, TAG_ConfigNotChangeList);
            for (String s : packageNameList) {
                serializer.startTag(null, TAG_PackageName);
                serializer.text(s);
                serializer.endTag(null, TAG_PackageName);
            }
            serializer.endTag(null, TAG_ConfigNotChangeList);
        }

        boolean matchPkgList(String name) {
            if (packageNameList.contains(name))
                return true;
            return false;
        }

        boolean match(String name) {
            if (matchPkgList(name))
                return true;
            return false;
        }

        void addPkgLocked(String packageName) {
            packageNameList.add(packageName);
        }
        void removePkgLocked(String packageName) {
            packageNameList.remove(packageName);
        }
        
        void clear() {
            packageNameList.clear();
        }
    }

    

    class ConfigChangeList {
        ArraySet<String> packageNameList = new ArraySet<String>();

        void writeToXml(XmlSerializer serializer) throws java.io.IOException {
            serializer.startTag(null, TAG_ConfigChangeList);
            for (String s : packageNameList) {
                serializer.startTag(null, TAG_PackageName);
                serializer.text(s);
                serializer.endTag(null, TAG_PackageName);
            }
            serializer.endTag(null, TAG_ConfigChangeList);
        }

        boolean matchPkgList(String name) {
            if (packageNameList.contains(name))
                return true;
            return false;
        }

        boolean match(String name) {
            if (matchPkgList(name))
                return true;
            return false;
        }

        void addPkgLocked(String packageName) {
            packageNameList.add(packageName);
        }
        void removePkgLocked(String packageName) {
            packageNameList.remove(packageName);
        }
        
        void clear() {
            packageNameList.clear();
        }
    }
}

