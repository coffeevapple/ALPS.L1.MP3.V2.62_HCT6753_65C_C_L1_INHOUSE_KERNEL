#include <utils/Log.h>
#include <utils/Errors.h>
#include <math.h>
#include "kd_imgsensor.h"
#include <cutils/properties.h>
#include <stdlib.h>
#include <aaa_log.h>

#define LOG_TAG "pd_buf_mgr"


#include <pd_buf_mgr.h>
#include <pd_ov23850mipiraw.h>
#include <pd_s5k2p8mipiraw.h>

namespace NS3A
{


PDBufMgr::PDBufMgr()
{
    memset( &m_PDBlockInfo, 0, sizeof(SET_PD_BLOCK_INFO_T));
}

PDBufMgr::~PDBufMgr()
{}



PDBufMgr*   
PDBufMgr::createInstance(SPDProfile_t &iPdProfile)
{

	PDBufMgr *instance = NULL;
	PDBufMgr *ret      = NULL;


	switch( iPdProfile.i4CurrSensorId)
	{
#if defined(OV23850_MIPI_RAW)	
		case OV23850_SENSOR_ID : 
			instance = PD_OV23850MIPIRAW::getInstance();
		break;
#endif
		
#if defined(S5K2P8_MIPI_RAW)
		case S5K2P8_SENSOR_ID : 
			instance = PD_S5K2P8MIPIRAW::getInstance();
		break;
#endif



		default :
			instance = NULL;
		break;
	}

	if( instance)
		ret = instance->IsSupport(iPdProfile) ? instance : NULL;

	MY_LOG("[PD] [SensorId]0x%04x, [%x]", iPdProfile.i4CurrSensorId, instance);

	return ret;
}

MBOOL PDBufMgr::SetPDBlockInfo( SET_PD_BLOCK_INFO_T &iPDBlockInfo)
{
	memcpy( &m_PDBlockInfo, &iPDBlockInfo, sizeof(SET_PD_BLOCK_INFO_T));

	MY_LOG("[PD] set block information	 %d %d %d %d %d %d %d", 
				m_PDBlockInfo.i4OffsetX, 
				m_PDBlockInfo.i4OffsetY,
				m_PDBlockInfo.i4PitchX, 
				m_PDBlockInfo.i4PitchY,
				m_PDBlockInfo.i4SubBlkW,
				m_PDBlockInfo.i4SubBlkH,
				m_PDBlockInfo.i4PairNum); 

	return MTRUE;
}





};  //  namespace PDMGR

