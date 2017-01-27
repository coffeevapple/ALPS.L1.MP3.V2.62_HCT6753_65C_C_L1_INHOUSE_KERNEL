LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#enable ipv6
ENABLE_IPV6 := true

LOCAL_SRC_FILES:= \
	main.c \
	magic.c \
	fsm.c \
	lcp.c \
	ipcp.c \
	upap.c \
	chap-new.c \
	ccp.c \
	ecp.c \
	auth.c \
	options.c \
	sys-linux.c \
	chap_ms.c \
	demand.c \
	utils.c \
	tty.c \
	eap.c \
	chap-md5.c \
	pppcrypt.c \
	openssl-hash.c \
	pppox.c \
	viaRoute.c
ifeq ($(ENABLE_IPV6),true)
LOCAL_SRC_FILES:= $(LOCAL_SRC_FILES) \
	ipv6cp.c \
	eui64.c
endif

LOCAL_SHARED_LIBRARIES := \
	libcutils libcrypto

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS := -DANDROID_CHANGES -DCHAPMS=1 -DMPPE=1 -Iexternal/openssl/include

ifeq ($(ENABLE_IPV6),true)
LOCAL_CFLAGS += -DINET6=1
endif

LOCAL_CFLAGS += -D__VIA_PPPD_DOWN__
LOCAL_CFLAGS += -D__VIA_RENEGOTIATION_FLAG__
LOCAL_CFLAGS += -D__VIA_SET_AUTHORITY__
LOCAL_CFLAGS += -D__VIA_CONSERVE_ELECTRICITY__

## Note: Suffix will be temp if compile the module by mm in the directory
ifeq ($(strip $(REPO_VERSION)),)
LOCAL_CFLAGS += -DVIA_SUFFIX_VERSION=\"temp\"
else
LOCAL_CFLAGS += -DVIA_SUFFIX_VERSION=$(REPO_VERSION)
endif

LOCAL_MODULE:= pppd_via

include $(BUILD_EXECUTABLE)
