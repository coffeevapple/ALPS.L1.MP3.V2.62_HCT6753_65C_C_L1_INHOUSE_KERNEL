
#ifeq ($(strip $(MTK_AUTO_TEST)),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := tests
#LOCAL_CERTIFICATE := platform
# We only want this apk build for tests.
#LOCAL_MODULE_TAGS := ReceiveAndReplyMMS_GEMINI

LOCAL_JAVA_LIBRARIES := android.test.runner 
LOCAL_STATIC_JAVA_LIBRARIES := librobotium4
LOCAL_JAVA_LIBRARIES += mediatek-framework

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)


LOCAL_PACKAGE_NAME := EvdoSmsTest

LOCAL_INSTRUMENTATION_FOR := Mms

include $(BUILD_PACKAGE)

#endif
