#
# camshottest
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#
LOCAL_SRC_FILES := \
    main.cpp \
    test_singleshot.cpp \
#    test_simager.cpp \

#
# Note: "/bionic" and "/external/stlport/stlport" is for stlport.
LOCAL_C_INCLUDES += \
    $(TOP)/bionic \
    $(TOP)/$(MTK_PATH_SOURCE)/kernel/drivers/video \
    $(TOP)/$(MTK_PATH_PLATFORM)/kernel/core/include/mach \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/m4u \
    $(TOP)/$(MTK_PATH_SOURCE)/kernel/include \
    $(TOP)/$(MTK_PATH_PLATFORM)/external/ldvt/include \
    $(TOP)/external/stlport/stlport \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/inc/acdk \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/acdk/inc/acdk \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/D1 \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1 \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/core/drv/imgsensor \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/core/drv/res_mgr \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/core/imageio/inc \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/D1/core/imageio/pipe/inc \
    $(TOP)/$(MTK_PATH_COMMON)/kernel/imgsensor/inc \
    $(TOP)/mediatek/hardware \
    $(TOP)/mediatek/hardware/include \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/include/mtkcam \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D1/mtkcam \
    $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D1 \

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/include

# vector
LOCAL_SHARED_LIBRARIES := \
    libstlport \
    libcutils \
    libimageio \
    libcamdrv \
    libcam.iopipe \
    libcam_utils \
    libcam.halsensor \
    libcam.metadata \
    libm4u \
    libutils \
    libstlport \
    libcam.camshot \

LOCAL_SHARED_LIBRARIES += libhardware
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libbinder
LOCAL_SHARED_LIBRARIES += libdl

LOCAL_SHARED_LIBRARIES += libcamera_client libcamera_client_mtk


#
LOCAL_STATIC_LIBRARIES := \

#
LOCAL_WHOLE_STATIC_LIBRARIES := \

#
LOCAL_MODULE := acdk_camshottest

#
LOCAL_MODULE_TAGS := eng

#
LOCAL_PRELINK_MODULE := false

#

#
# Start of common part ------------------------------------
sinclude $(TOP)/$(MTK_PATH_PLATFORM)/hardware/mtkcam/mtkcam.mk

#-----------------------------------------------------------
LOCAL_CFLAGS += $(MTKCAM_CFLAGS)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(MTKCAM_C_INCLUDES)

#-----------------------------------------------------------
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/include/D1
LOCAL_C_INCLUDES += $(TOP)/system/media/camera/include

# End of common part ---------------------------------------
#
include $(BUILD_EXECUTABLE)


#
#include $(call all-makefiles-under,$(LOCAL_PATH))
