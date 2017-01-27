#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>

#include <pd_imx230mipiraw.h>
#include <aaa_log.h>
#include <cutils/properties.h>
#include <stdlib.h>


#define LOG_TAG "pd_buf_mg_imx230mipiraw"


#define dSize 16*12

#define cSize 8*6

namespace NS3A
{

PDBufMgrOpen* 
PD_IMX230MIPIRAW::getInstance()
{
    static PD_IMX230MIPIRAW singleton;
    return &singleton;

}


PD_IMX230MIPIRAW::PD_IMX230MIPIRAW()
{
	m_phase_difference = new MUINT16 [dSize];
	m_confidence_level = new MUINT16 [dSize];
	m_calibration_data = new MUINT16 [cSize];


	m_XKnotNum1 = 8;
	m_YKnotNum1 = 6;
	
	m_XKnotNum2 = 8;
	m_YKnotNum2 = 6;
	
	m_PointNumForThrLine = 5;

	m_CurrMode = 0; //default : all pixel mode.

	// Malloc
	m_SonyPdLibInputData.p_SlopeData				= (signed long *)malloc( sizeof(signed long) * m_XKnotNum1 * m_YKnotNum1 );
	m_SonyPdLibInputData.p_OffsetData 				= (signed long *)malloc( sizeof(signed long) * m_XKnotNum1 * m_YKnotNum1 );
	m_SonyPdLibInputData.p_XAddressKnotSlopeOffset	= (unsigned short *)malloc( sizeof(unsigned short) * m_XKnotNum1 );
	m_SonyPdLibInputData.p_YAddressKnotSlopeOffset	= (unsigned short *)malloc( sizeof(unsigned short) * m_YKnotNum1 );
	m_SonyPdLibInputData.p_DefocusOKNGThrLine 		= (DefocusOKNGThrLine_t *)malloc( sizeof(DefocusOKNGThrLine_t) * m_XKnotNum2 * m_YKnotNum2 );
		
	for( unsigned short i = 0; i < m_XKnotNum2*m_YKnotNum2; i++ )
	{
		m_SonyPdLibInputData.p_DefocusOKNGThrLine[i].p_AnalogGain = (unsigned long *)malloc( sizeof(unsigned long) * m_PointNumForThrLine );
		m_SonyPdLibInputData.p_DefocusOKNGThrLine[i].p_Confidence = (unsigned long *)malloc( sizeof(unsigned long) * m_PointNumForThrLine ); 
	}
		
	m_SonyPdLibInputData.p_XAddressKnotDefocusOKNG	= (unsigned short *)malloc( sizeof(unsigned short) * m_XKnotNum2 );
	m_SonyPdLibInputData.p_YAddressKnotDefocusOKNG	= (unsigned short *)malloc( sizeof(unsigned short) * m_YKnotNum2 );


	MY_LOG("[PD Mgr] +IMX230+\n");

}

PD_IMX230MIPIRAW::~PD_IMX230MIPIRAW()
{
	if( m_phase_difference)
		delete m_phase_difference;

	if( m_confidence_level)
		delete m_confidence_level;

	if( m_calibration_data)
		delete m_calibration_data;

	m_phase_difference = NULL;
	m_confidence_level = NULL;
	m_calibration_data = NULL;


	// Finalize
	// Free
	free ( m_SonyPdLibInputData.p_YAddressKnotDefocusOKNG );
	free ( m_SonyPdLibInputData.p_XAddressKnotDefocusOKNG );
			
	for( unsigned short i = 0; i < m_XKnotNum2*m_YKnotNum2; i++ )
	{
		free ( m_SonyPdLibInputData.p_DefocusOKNGThrLine[i].p_Confidence );
		free ( m_SonyPdLibInputData.p_DefocusOKNGThrLine[i].p_AnalogGain );
	}
			
	free ( m_SonyPdLibInputData.p_DefocusOKNGThrLine );
	free ( m_SonyPdLibInputData.p_YAddressKnotSlopeOffset );
	free ( m_SonyPdLibInputData.p_XAddressKnotSlopeOffset );
	free ( m_SonyPdLibInputData.p_OffsetData );
	free ( m_SonyPdLibInputData.p_SlopeData  );

}

MBOOL PD_IMX230MIPIRAW::IsSupport( SPDProfile_t &iPdProfile)
{
	MBOOL ret = MFALSE;

	//binning ( V:1/2, H:1/2) mode and all-pixel mode.
	if( ( iPdProfile.uImgXsz==2672 && iPdProfile.uImgYsz==2008) || ( iPdProfile.uImgXsz==5344 && iPdProfile.uImgYsz==4016) )
	{
		ret = MTRUE;
		m_CurrMode = iPdProfile.u4IsZSD ? 0 : 2;
	}
	else
	{
		MY_LOG("[Sony]Mode is not Supported (%d, %d)\n", iPdProfile.uImgXsz, iPdProfile.uImgYsz);
	}

	return ret;

}

MINT32 PD_IMX230MIPIRAW::GetPDCalSz()
{
	return 96;
}


MBOOL PD_IMX230MIPIRAW::ExtractPDCL()
{
	static MINT32 frameCnt = 0;

	MUINT8 flexibleEn = m_databuf[0];
	MUINT8 modesel = m_databuf[1]>>6;

	
	MUINT8 offset = 5;
	MUINT8 *ptr = &m_databuf[offset];
		
	for( int i=0; i<dSize; i++)
	{
		m_confidence_level[i] = 0xff  & (ptr[i*5]);  
		m_phase_difference[i] = 0x3ff & (ptr[i*5+1]<<2 | ptr[i*5+2]>>6);

	}



	char value[200] = {'\0'};
	property_get("vc.dump.enable", value, "0");
	MBOOL bEnable = atoi(value);
	if (bEnable)
	{
		char fileName[64];
		sprintf(fileName, "/sdcard/vc/%d_pd.raw", frameCnt);
		FILE *fp = fopen(fileName, "w");
		if (NULL == fp)
		{
			return MFALSE;
		}	 
		fwrite(reinterpret_cast<void *>(m_phase_difference), 1, 16*12*2, fp);
		fclose(fp); 	   
		
		sprintf(fileName, "/sdcard/vc/%d_cl.raw", frameCnt);
		fp = fopen(fileName, "w");
		if (NULL == fp)
		{
			return MFALSE;
		}	 
		fwrite(reinterpret_cast<void *>(m_confidence_level), 1, 16*12*2, fp);
		fclose(fp); 
		frameCnt++;
	}
	else
	{
		frameCnt = 0; 
	}

	return MTRUE;

	
}


MBOOL PD_IMX230MIPIRAW::ExtractCaliData()
{
	static MINT32 frameCnt = 0;


	for( int i=0; i<cSize; i++)
	{
		m_calibration_data[i] = 0xffff & ( m_calidatabuf[i*2]<<8 | m_calidatabuf[i*2+1]);

	}

	// Slope and offset (defocus vs phase difference)
	// Set slope
	MY_LOG("[Sony] *Set slope\n");
	for( unsigned short i = 0; i < m_XKnotNum1*m_YKnotNum1; i++ )
	{
		m_SonyPdLibInputData.p_SlopeData[i] = m_calibration_data[i]; 
		MY_LOG("[Sony] SonyPdLibInputData.p_SlopeData[%d]	:%d\n", i,(int)m_SonyPdLibInputData.p_SlopeData[i]);
	}
				
	// Set offset
	MY_LOG("[Sony] *Set offset)\n");
	for( unsigned short i = 0; i < m_XKnotNum1*m_YKnotNum1; i++ )
	{
		m_SonyPdLibInputData.p_OffsetData[i] = 0;
		MY_LOG("[Sony] SonyPdLibInputData.p_OffsetData[%d]   :%d\n", i,(int)m_SonyPdLibInputData.p_OffsetData[i]);
	}
				



	char value[200] = {'\0'};
	property_get("vc.dump.enable", value, "0");
	MBOOL bEnable = atoi(value);


	if (bEnable)
	{
		char fileName[64];
		sprintf(fileName, "/sdcard/vc/%d_cali.raw", frameCnt++);
		FILE *fp = fopen(fileName, "w");
		if (NULL == fp)
		{
			return MFALSE;
		}	 
		fwrite(reinterpret_cast<void *>(m_calibration_data), 1, 8*6*2, fp);
		fclose(fp); 	   
	}
	else
	{
		frameCnt = 0; 
	}

	return MTRUE;
	

}



MVOID PD_IMX230MIPIRAW::GetVersionOfPdafLibrary( SPDLibVersion_t &oPdLibVersion)
{
	// Input
	SonyPdLibVersion_t SonyPdLibVersion;
	
	// Execute
	SonyPdLibGetVersion(&SonyPdLibVersion);

	oPdLibVersion.MajorVersion = SonyPdLibVersion.MajorVersion;
	oPdLibVersion.MinorVersion = SonyPdLibVersion.MinorVersion;
	
	// Output
	MY_LOG("[Sony] Unit Test SonyPdLibGetVersion()\n");
	MY_LOG("[Sony] \n");
	MY_LOG("[Sony] Result\n");
	MY_LOG("[Sony] -----------------------------------------\n");
	MY_LOG("[Sony] (*pfa_SonyPdLibVersion).MajorVersion = %d\n",(int)SonyPdLibVersion.MajorVersion);
	MY_LOG("[Sony] (*pfa_SonyPdLibVersion).MinorVersion = %d\n",(int)SonyPdLibVersion.MinorVersion);
	
}



MBOOL PD_IMX230MIPIRAW::GetDefocus( SPDROIInput_T &iPDInputData, SPDROIResult_T &oPdOutputData)
{
	MY_LOG("[Sony] \n");
	
	signed long    Ret = D_SONY_PD_LIB_E_NG;
	unsigned short i   = 0;
	unsigned long  k   = 0;
	
	unsigned short XSize = iPDInputData.XSizeOfImage;	
	unsigned short YSize = iPDInputData.YSizeOfImage;

	
	// Input
	// Phase difference data and confidence level
	MUINT16 szX = iPDInputData.XSizeOfImage/8;
	MUINT16 szY = iPDInputData.YSizeOfImage/6;
	MUINT8 Xidx = (MUINT8)(( (iPDInputData.ROI.i4XStart+iPDInputData.ROI.i4XEnd)/2 + szX/2 )/szX);  
	MUINT8 Yidx = (MUINT8)(( (iPDInputData.ROI.i4YStart+iPDInputData.ROI.i4YEnd)/2 + szY/2 )/szY);  
	m_SonyPdLibInputData.PhaseDifference = (signed long) (m_phase_difference[Yidx*16+Xidx]>=512 ? m_phase_difference[Yidx*16+Xidx]-1024 : m_phase_difference[Yidx*16+Xidx]);
	m_SonyPdLibInputData.ConfidenceLevel = (signed long) (m_confidence_level[Yidx*16+Xidx]>=512 ? m_confidence_level[Yidx*16+Xidx]-1024 : m_confidence_level[Yidx*16+Xidx]);
			
	MY_LOG("[Sony] **Phase difference data and confidence level sz=(%d,%d), idx=(%d,%d)\n", iPDInputData.XSizeOfImage, iPDInputData.YSizeOfImage, Xidx, Yidx);
			
	MY_LOG("[Sony] SonyPdLibInputData.PhaseDifference       :%d\n", (int)m_SonyPdLibInputData.PhaseDifference);
	MY_LOG("[Sony] SonyPdLibInputData.ConfidenceLevel       :%d\n", (int)m_SonyPdLibInputData.ConfidenceLevel);		
	// PDAF window
	// Address is required to be converted into all-pixel mode address 
	// before scaling, cropping, mirroring and flipping
			
	// PDAF window information must be 
	// in synchronization with phase difference data and confidence level
			
	// Set size
	m_SonyPdLibInputData.XSizeOfImage = XSize;
	m_SonyPdLibInputData.YSizeOfImage = YSize;
	MY_LOG("[Sony] SonyPdLibInputData.XSizeOfImage          :%d(%d)\n", (int)m_SonyPdLibInputData.XSizeOfImage, iPDInputData.XSizeOfImage);
	MY_LOG("[Sony] SonyPdLibInputData.YSizeOfImage          :%d(%d)\n", (int)m_SonyPdLibInputData.YSizeOfImage, iPDInputData.YSizeOfImage);
			
	// Set PDAF window
	m_SonyPdLibInputData.XAddressOfWindowStart	= iPDInputData.ROI.i4XStart;
	m_SonyPdLibInputData.YAddressOfWindowStart	= iPDInputData.ROI.i4YStart;
	m_SonyPdLibInputData.XAddressOfWindowEnd	= iPDInputData.ROI.i4XEnd;
	m_SonyPdLibInputData.YAddressOfWindowEnd	= iPDInputData.ROI.i4YEnd;
	MY_LOG("[Sony] SonyPdLibInputData.XAddressOfWindowStart :%d\n", (int)m_SonyPdLibInputData.XAddressOfWindowStart);
	MY_LOG("[Sony] SonyPdLibInputData.YAddressOfWindowStart :%d\n", (int)m_SonyPdLibInputData.YAddressOfWindowStart);
	MY_LOG("[Sony] SonyPdLibInputData.XAddressOfWindowEnd   :%d\n", (int)m_SonyPdLibInputData.XAddressOfWindowEnd  );
	MY_LOG("[Sony] SonyPdLibInputData.YAddressOfWindowEnd   :%d\n", (int)m_SonyPdLibInputData.YAddressOfWindowEnd  );
			
			
	// Set the number of knots
	m_SonyPdLibInputData.XKnotNumSlopeOffset = m_XKnotNum1;
	m_SonyPdLibInputData.YKnotNumSlopeOffset = m_YKnotNum1;
			
			
	// Set x address of konts
	for( i = 0; i < m_XKnotNum1; i++ )
	{
		m_SonyPdLibInputData.p_XAddressKnotSlopeOffset[i] = 0 + i * XSize / (m_XKnotNum1-1);	// Value is as an example
	}
			
	// Set y address of konts
	for( i = 0; i < m_YKnotNum1; i++ )
	{
		m_SonyPdLibInputData.p_YAddressKnotSlopeOffset[i] = 0 + i * YSize / (m_YKnotNum1-1);	// Value is as an example
	}

	// Set adjustment coefficient of slope according to image sensor mode
	// Set phase detection pixel density aaccording to image sensor mode
	switch( m_CurrMode)
	{
		case 2 : // bining mode	
			m_SonyPdLibInputData.DensityOfPhasePix = D_SONY_PD_LIB_DENSITY_SENS_MODE2;
			m_SonyPdLibInputData.AdjCoeffSlope     = D_SONY_PD_LIB_SLOPE_ADJ_COEFF_SENS_MODE2;
			break;
		default : // all pixel mode
			m_SonyPdLibInputData.DensityOfPhasePix = D_SONY_PD_LIB_DENSITY_SENS_MODE0;	
			m_SonyPdLibInputData.AdjCoeffSlope	   = D_SONY_PD_LIB_SLOPE_ADJ_COEFF_SENS_MODE0;
			break;

	}
	MY_LOG("[Sony] SonyPdLibInputData.AdjCoeffSlope   :%d\n", (int)m_SonyPdLibInputData.AdjCoeffSlope);
	MY_LOG("[Sony] SonyPdLibInputData.DensityOfPhasePix:%d\n",(int)m_SonyPdLibInputData.DensityOfPhasePix);
		
	// Defocus OK/NG : not using
	// Set image sensor analog gain
	// which must be in synchronization with phase difference data and confidence level
	m_SonyPdLibInputData.ImagerAnalogGain = 10;			// Value is as an example //@Tim
			
	// Set the number of knots
	m_SonyPdLibInputData.XKnotNumDefocusOKNG = m_XKnotNum2;	// Value is as an example
	m_SonyPdLibInputData.YKnotNumDefocusOKNG = m_YKnotNum2;	// Value is as an example
			
	// Set the threshold line
	for( i = 0; i < m_XKnotNum2*m_YKnotNum2; i++ )
	{
		m_SonyPdLibInputData.p_DefocusOKNGThrLine[i].PointNum = m_PointNumForThrLine;
				
		for( k = 0; k < m_PointNumForThrLine; k++ )
		{
			m_SonyPdLibInputData.p_DefocusOKNGThrLine[i].p_AnalogGain[k] = 10 * k;	// Value is as an example
			m_SonyPdLibInputData.p_DefocusOKNGThrLine[i].p_Confidence[k] =  2 * k;	// Value is as an example
		}
	}
			
	// Set x address of konts
	for( i = 0; i < m_XKnotNum2; i++ )
	{
		m_SonyPdLibInputData.p_XAddressKnotDefocusOKNG[i] = 0 + i * XSize / (m_XKnotNum2-1);	// Value is as an example
	}
			
	// Set y address of konts
	for( i = 0; i < m_YKnotNum2; i++ )
	{
		m_SonyPdLibInputData.p_YAddressKnotDefocusOKNG[i] = 0 + i * YSize / (m_YKnotNum2-1);	// Value is as an example
	}
		
	
	// Execute
	// Get defocus data
	Ret = SonyPdLibGetDefocus(&m_SonyPdLibInputData, &m_SonyPdLibOutputData);
	
	// Output
	if( Ret == D_SONY_PD_LIB_E_OK )
	{


		//(1) normalize to fit API spec : output target position.
		oPdOutputData.Defocus                = iPDInputData.curLensPos + ((-1)*m_SonyPdLibOutputData.Defocus/16384);
		//(2) normalize to fit API spec : confidence level is in ragne 0~100.
		oPdOutputData.DefocusConfidence      = (int)(m_SonyPdLibInputData.ConfidenceLevel*100/255);
		//(1) normalize to fit API spec : confidence level is in ragne 0~100.
		oPdOutputData.DefocusConfidenceLevel = (int)(m_SonyPdLibInputData.ConfidenceLevel*100/255);
		//(1) normalize to fit API spec : pixel base.
		oPdOutputData.PhaseDifference        = (-1)*m_SonyPdLibInputData.PhaseDifference*1000/16;
		
		MY_LOG("[Sony] Output:Reurn Value == D_SONY_PD_LIB_E_OK\n");
		MY_LOG("[Sony] ---------------------------------\n");
		// Defocus data
		MY_LOG("[Sony] CurrLensPos           :%d\n", (int)iPDInputData.curLensPos);
		MY_LOG("[Sony] Defocus               :%d\n", (int)m_SonyPdLibOutputData.Defocus);
		MY_LOG("[Sony] DefocusConfidence     :%d\n", (int)m_SonyPdLibOutputData.DefocusConfidence);
		MY_LOG("[Sony] DefocusConfidenceLevel:%d\n", (int)m_SonyPdLibOutputData.DefocusConfidenceLevel);
		MY_LOG("[Sony] PhaseDifference       :%d\n", (int)m_SonyPdLibOutputData.PhaseDifference);
		MY_LOG("[Sony][Output] target DAC %d, confidence %d, pd %d\n", 
			oPdOutputData.Defocus, 
			oPdOutputData.DefocusConfidenceLevel, 
			oPdOutputData.PhaseDifference);	
	}
	else
	{
		oPdOutputData.Defocus                = 0;
		oPdOutputData.DefocusConfidence      = 0;
		oPdOutputData.DefocusConfidenceLevel = 0;
		oPdOutputData.PhaseDifference        = 0;
		MY_LOG("[Sony] Output:Reurn Value == D_SONY_PD_LIB_E_NG\n");
	}

		

	MY_LOG("[Sony] ---------------------------------\n");
	
	return true;		
}

};  //  namespace NS3A

