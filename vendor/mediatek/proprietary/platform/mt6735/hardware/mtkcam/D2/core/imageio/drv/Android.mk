#
# libimageio_plat_drv
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#
LOCAL_SRC_FILES := \
    cam/cam_path.cpp \
    cam/cam_path_pass1.cpp \
    cam/cam_path_pass2.cpp \
    cam/isp_function.cpp \
    cdp/cdp_drv.cpp \
    mdp/mdp_mgr.cpp

# camutils: For CameraProfile APIs.
#
LOCAL_C_INCLUDES := $(TOP)/bionic \
    $(TOP)/external/stlport/stlport \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/ \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/inc/drv \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D2/core/imageio/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D2/core/imageio_common/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D2/core/imageio/drv/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/kernel/core/include/mach \
    $(TOP)/$(MTK_PATH_PLATFORM)/external/ldvt/include \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/m4u \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/inc/common/camutils \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/dpframework/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D2/mtkcam/drv \
    $(TOP)/bionic/libc/kernel/common/linux/mt6582 \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D2/mtkcam/imageio \


#
LOCAL_STATIC_LIBRARIES := \

#
LOCAL_WHOLE_STATIC_LIBRARIES := \


LOCAL_SHARED_LIBRARIES := \
    libdpframework \
    libstlport \
    libcutils \
    libcamdrv \
    libm4u \

#new from Jonas requests 130510
LOCAL_SHARED_LIBRARIES += liblog
LOCAL_CFLAGS += -DUSING_D2

ifeq ($(BUILD_MTK_LDVT),yes)
    LOCAL_CFLAGS += -DUSING_MTK_LDVT
endif

#
LOCAL_MODULE := libimageio_plat_drv
LOCAL_MODULE_TAGS := optional
#LOCAL_MULTILIB := 32
#
#include $(BUILD_STATIC_LIBRARY)

#
# Start of common part ------------------------------------
-include $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/mtkcam.mk

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D2

# End of common part ---------------------------------------
#
include $(BUILD_SHARED_LIBRARY)


#
#include $(call all-makefiles-under, $(LOCAL_PATH))
