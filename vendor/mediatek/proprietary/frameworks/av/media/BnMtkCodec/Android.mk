LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    BnMtkCodec.cpp \
    CAPEWrapper.cpp

LOCAL_C_INCLUDES:= \
	$(TOP)/frameworks/native/include \
        $(TOP)/$(MTK_ROOT)/external/apedec \
        $(TOP)/$(MTK_ROOT)/external/apedec/arm_32 \
        $(TOP)/$(MTK_ROOT)/external/apedec/arm_64 \
        $(TOP)/$(MTK_ROOT)/external/apedec/arm_32/inc \
        $(TOP)/$(MTK_ROOT)/external/apedec/arm_64/inc \
        $(TOP)/$(MTK_ROOT)/frameworks/av/media/libstagefright/include/omx_core


LOCAL_SHARED_LIBRARIES :=       \
        libbinder               \
        libutils                \
        libcutils               \
        libdl                   \
        libui



LOCAL_STATIC_LIBRARIES :=	\
	libapedec_mtk

  
LOCAL_PRELINK_MODULE:= false
LOCAL_MODULE := libBnMtkCodec
LOCAL_MULTILIB := both

include $(BUILD_SHARED_LIBRARY)
