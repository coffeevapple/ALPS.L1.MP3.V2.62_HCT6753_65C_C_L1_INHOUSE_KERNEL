ifndef DRVGEN_OUT
DRVGEN_OUT := $(call local-intermediates-dir)
endif

ALL_DRVGEN_FILE :=
ifeq ($(MTK_PLATFORM),MT2601)
ALL_DRVGEN_FILE += \
	cust_kpd.h \
	cust_eint.h
else
ALL_DRVGEN_FILE += \
	inc/cust_kpd.h \
	inc/cust_eint.h
endif

DRVGEN_FILE_LIST := $(addprefix $(DRVGEN_OUT)/,$(ALL_DRVGEN_FILE))
DRVGEN_TOOL := $(LOCAL_PATH)/dct/DrvGen
DWS_FILE := vendor/mediatek/proprietary/custom/$(MTK_BASE_PROJECT)/kernel/dct/$(if $(CUSTOM_KERNEL_DCT),$(CUSTOM_KERNEL_DCT),dct)/codegen.dws
DRVGEN_PREBUILT_PATH := vendor/mediatek/proprietary/custom/$(MTK_BASE_PROJECT)/kernel/dct
DRVGEN_PREBUILT_CHECK := $(filter-out $(wildcard $(addprefix $(DRVGEN_PREBUILT_PATH)/,$(ALL_DRVGEN_FILE))),$(addprefix $(DRVGEN_PREBUILT_PATH)/,$(ALL_DRVGEN_FILE)))

ifneq ($(DRVGEN_PREBUILT_CHECK),)
$(info drvgen: $(DRVGEN_PREBUILT_CHECK))
$(DRVGEN_FILE_LIST): PRIVATE_DWS_FILE := $(DWS_FILE)
$(DRVGEN_FILE_LIST): $(DRVGEN_TOOL) $(DWS_FILE)
	@mkdir -p $(dir $@)
	$(DRVGEN_TOOL) $(PRIVATE_DWS_FILE) $(dir $@) $(dir $@) $(subst .,_,$(patsubst pmic_drv.%,pmic_%,$(patsubst cust_%,%,$(notdir $@))))
else
$(DRVGEN_FILE_LIST): $(DRVGEN_OUT)/% : $(DRVGEN_PREBUILT_PATH)/%
	@mkdir -p $(dir $@)
	cp -f $< $@
endif

