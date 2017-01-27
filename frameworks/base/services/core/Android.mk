LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := services.core

LOCAL_SRC_FILES += \
    $(call all-java-files-under,java) \
    java/com/android/server/EventLogTags.logtags \
    java/com/android/server/am/EventLogTags.logtags

LOCAL_JAVA_LIBRARIES := android.policy telephony-common mediatek-framework
LOCAL_STATIC_JAVA_LIBRARIES := anrmanager \
                               services.ipo
LOCAL_STATIC_JAVA_LIBRARIES += com_mediatek_amplus

LOCAL_STATIC_JAVA_LIBRARIES += arch_helper

include $(BUILD_STATIC_JAVA_LIBRARY)
