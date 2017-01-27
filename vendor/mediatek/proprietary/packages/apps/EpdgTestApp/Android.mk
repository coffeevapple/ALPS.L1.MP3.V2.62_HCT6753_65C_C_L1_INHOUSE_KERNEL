#ifeq ($(MTK_EPDG_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := EpdgTestApp
LOCAL_CERTIFICATE := platform

LOCAL_JNI_SHARED_LIBRARIES := libpingipv4 
LOCAL_REQUIRED_MODULES := libpingipv4

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += wifi-service

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
# include $(call all-makefiles-under,$(LOCAL_PATH))

# Include subdirectory makefiles
# ============================================================
include $(call all-makefiles-under,$(LOCAL_PATH)/src/jni)
#endif