#define LOG_TAG "flash_cct_func"


#include <utils/threads.h>  // For Mutex::Autolock.
#include <cutils/properties.h>
#include <aaa_types.h>
#include <aaa_error_code.h>
#include <dbg_aaa_param.h>
#include <dbg_isp_param.h>
#include <kd_camera_feature.h>
#include <aaa_log.h>
#include <mtkcam/common/faces.h>
//#include <mtkcam/featureio/aaa_hal_common.h>
//#include <mtkcam/featureio/aaa_hal_if.h>
#include <mtkcam/hal/aaa_hal_base.h>
#include <aaa_hal.h>
#include <camera_custom_nvram.h>
#include <af_param.h>
#include <awb_param.h>
#include <ae_param.h>
#include <af_tuning_custom.h>
#include <mcu_drv.h>
#include <nvbuf_util.h>
#include <mtkcam/drv_common/isp_reg.h>
#include <mtkcam/drv/isp_drv.h>
#include <mtkcam/hal/IHalSensor.h>
#include <mtkcam/hal/sensor_hal.h>
#include <nvram_drv.h>
#include <nvram_drv_mgr.h>
#include <mtkcam/acdk/cct_feature.h>
#include <flash_param.h>
#include <isp_tuning.h>
#include <isp_tuning_mgr.h>
#include <af_feature.h>
#include <mtkcam/algorithm/lib3a/af_algo_if.h>
#include <mtkcam/featureio/flicker_hal_base.h>
#include "af_mgr.h"
#include <mtkcam/common.h>
using namespace NSCam;
#include "nvbuf_util.h"
#include "aaa_common_custom.h"
#include "camera_custom_cam_cal.h"  //seanlin 121022 for test
#include "cam_cal_drv.h" //seanlin 121022 for test


using namespace NS3A;
using namespace NSIspTuning;
using namespace NSCam;


void setRawPregain2(int r, int g, int b)
{
  IspDrv* pIspDrv;
    pIspDrv = IspDrv::createInstance();
  isp_reg_t* pIspReg;
    pIspReg = (isp_reg_t*)pIspDrv->getRegAddr();

    ISP_WRITE_BITS(pIspReg , CAM_AE_RAWPREGAIN2_0, RAWPREGAIN2_R, 0x200);
    ISP_WRITE_BITS(pIspReg , CAM_AE_RAWPREGAIN2_0, RAWPREGAIN2_G, 0x200);
    ISP_WRITE_BITS(pIspReg , CAM_AE_RAWPREGAIN2_1, RAWPREGAIN2_B, 0x200);
}

void aaoInitPara()
{
  IspDrv* pIspDrv;
  pIspDrv = IspDrv::createInstance();
  isp_reg_t* pIspReg;
  pIspReg = (isp_reg_t*)pIspDrv->getRegAddr();
  ISP_WRITE_BITS(pIspReg , CAM_AWB_RAWPREGAIN1_0, RAWPREGAIN1_R, 0x200);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_RAWPREGAIN1_0, RAWPREGAIN1_G, 0x200);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_RAWPREGAIN1_1, RAWPREGAIN1_B, 0x200);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_RAWLIMIT1_0, RAWLIMIT1_R, 0xfff);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_RAWLIMIT1_0, RAWLIMIT1_G, 0xfff);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_RAWLIMIT1_1, RAWLIMIT1_B, 0xfff);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_LOW_THR    , AWB_LOW_THR0, 0);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_LOW_THR    , AWB_LOW_THR1, 0);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_LOW_THR    , AWB_LOW_THR2, 0);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_HI_THR    , AWB_HI_THR0, 255);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_HI_THR    , AWB_HI_THR1, 255);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_HI_THR    , AWB_HI_THR2, 255);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_ERR_THR    , AWB_ERR_THR, 0);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_ROT    , AWB_COS, 0x100);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_ROT    , AWB_SIN, 0x0);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_L0_X    , AWB_L0_X_LOW, 0x3388);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_L0_X    , AWB_L0_X_UP, 0x1388);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_L0_Y    , AWB_L0_Y_LOW, 0x3388);
  ISP_WRITE_BITS(pIspReg , CAM_AWB_L0_Y    , AWB_L0_Y_UP, 0x1388);
}