#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>

#include "pd_buf_mgr_open.h"

#include <sony/SonyIMX230PdafLibrary.h>


#ifndef _PD_IMX230MIPIRAW_H_
#define _PD_IMX230MIPIRAW_H_

namespace NS3A
{

class PD_IMX230MIPIRAW : protected PDBufMgrOpen
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Ctor/Dtor.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:    ////    Disallowed.

protected :
	
    MBOOL IsSupport( SPDProfile_t &iPdProfile); 
	MBOOL ExtractPDCL(); 
	MBOOL ExtractCaliData(); 


	unsigned short m_XKnotNum1;
	unsigned short m_YKnotNum1;
	
	unsigned short m_XKnotNum2;
	unsigned short m_YKnotNum2;

	unsigned short m_PointNumForThrLine;


	SonyPdLibInputData_t  m_SonyPdLibInputData;
	SonyPdLibOutputData_t m_SonyPdLibOutputData;

	unsigned short m_CurrMode;


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Operations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    PD_IMX230MIPIRAW();
    ~PD_IMX230MIPIRAW();

	static PDBufMgrOpen* getInstance();

	MINT32 GetPDCalSz();

	MVOID GetVersionOfPdafLibrary( SPDLibVersion_t &oPdLibVersion);

    MBOOL GetDefocus( SPDROIInput_T &iPDInputData, SPDROIResult_T &oPdOutputData); 

};

};  //  namespace PDMGR
#endif // _PD_IMX230MIPIRAW_H_

