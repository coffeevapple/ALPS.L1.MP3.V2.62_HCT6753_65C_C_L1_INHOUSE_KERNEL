LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

incallui_dir := ../InCallUI
contacts_common_dir := ../ContactsCommon
phone_common_dir := ../PhoneCommon
contacts_ext_dir := ../ContactsCommon/ext
# M: add mtk-ex
chips_dir := ../../../frameworks/ex/chips

src_dirs := src \
    $(incallui_dir)/src \
    $(contacts_common_dir)/src \
    $(phone_common_dir)/src \
    $(contacts_ext_dir)/src

res_dirs := res \
    res_ext \
    $(incallui_dir)/res \
    $(incallui_dir)/res_ext \
    $(contacts_common_dir)/res \
    $(contacts_common_dir)/res_ext \
    $(phone_common_dir)/res \
    $(chips_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.incallui \
    --extra-packages com.android.contacts.common \
    --extra-packages com.android.phone.common
# M: add mtk-ex
LOCAL_AAPT_FLAGS += --extra-packages com.android.mtkex.chips

# M: add ims-common for WFC feature
LOCAL_JAVA_LIBRARIES := telephony-common mediatek-framework voip-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES := \
    com.android.services.telephony.common \
    com.android.vcard \
    android-common \
    guava \
    android-support-v13 \
    android-support-v4 \
    android-ex-variablespeed \
    libphonenumber \
    libgeocoding \
    com.mediatek.dialer.ext \
    com.mediatek.incallui.ext
# M: add mtk-ex
LOCAL_STATIC_JAVA_LIBRARIES += android-common-chips

LOCAL_REQUIRED_MODULES := libvariablespeed

LOCAL_PACKAGE_NAME := Dialer
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags $(incallui_dir)/proguard.flags

# Uncomment the following line to build against the current SDK
# This is required for building an unbundled app.
# LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
