LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libalacdec_mtk
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_SRC_FILES_32 := arm/libalacdec_mtk.a
LOCAL_SRC_FILES_64 := libalacdec_mtk.a
LOCAL_MODULE_SUFFIX := .a
LOCAL_MULTILIB := both

include $(BUILD_PREBUILT)