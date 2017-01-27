#define LOG_TAG "MtkCam/CpuCtrl"
//
#include <dlfcn.h>
#include <semaphore.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <fcntl.h>
#include <sys/mman.h>
//
#include <utils/Errors.h>
#include <utils/threads.h>
#include <utils/List.h>
//
#include <cutils/atomic.h>
#include <cutils/log.h>
#include <cutils/xlog.h>
#include <cutils/properties.h>
//
using namespace android;
//
#include <mtkcam/Log.h>
#include <mtkcam/common.h>
#include <mtkcam/utils/CpuCtrl.h>
//-----------------------------------------------------------------------------
#define LOG_MSG(fmt, arg...)        CAM_LOGD("(%d)[%s] "fmt, ::gettid(), __FUNCTION__, ##arg)
#define LOG_WRN(fmt, arg...)        CAM_LOGW("(%d)[%s] "fmt, ::gettid(), __FUNCTION__, ##arg)
#define LOG_ERR(fmt, arg...)        CAM_LOGE("(%d)[%s] "fmt, ::gettid(), __FUNCTION__, ##arg)
//
#define LOG_MSG_IF(cond, arg...)    if (cond) { LOG_MSG(arg); }
#define LOG_WRN_IF(cond, arg...)    if (cond) { LOG_WRN(arg); }
#define LOG_ERR_IF(cond, arg...)    if (cond) { LOG_ERR(arg); }
//
#define FUNCTION_NAME               LOG_MSG("")
#define FUNCTION_IN                 LOG_MSG("+")
#define FUNCTION_OUT                LOG_MSG("-")
//-----------------------------------------------------------------------------
#define PERF_LIB_FULLNAME       "/system/lib/libperfservicenative.so"
#define STR_FUNC_REG            "PerfServiceNative_userRegBigLittle"
#define STR_FUNC_ENABLETIMEOUT  "PerfServiceNative_userEnableTimeout"
#define STR_FUNC_DISABLE        "PerfServiceNative_userDisable"
#define STR_FUNC_UNREG          "PerfServiceNative_userUnreg"
typedef int (*funcPerfRegBL)(int, int, int, int);
typedef void (*funcEnableTimeout)(int, int);
typedef void (*funcDisable)(int);
typedef int (*funcPerfUnreg)(int);
//-----------------------------------------------------------------------------
class CpuCtrlImp : public CpuCtrl
{
    public:
        CpuCtrlImp();
        ~CpuCtrlImp();
        //
        virtual void    destroyInstance(void);
        virtual MBOOL   init(void);
        virtual MBOOL   uninit(void);
        virtual MBOOL   enable(CPU_CTRL_INFO &cpuInfo);
        virtual MBOOL   disable(void);
        //
    private:
        void*               mPerfLib;
        funcEnableTimeout   mPerfEnable;
        funcDisable         mPerfDisable;
        funcPerfRegBL       mPerfReg;
        //
        mutable Mutex       mLock;
        volatile MINT32     mUser;
        MBOOL               mEnable;
        MINT32              mPerfHandle;
        CPU_CTRL_INFO       mCpuCtrlInfo;
};
//-----------------------------------------------------------------------------
CpuCtrlImp::
CpuCtrlImp()
{
    FUNCTION_NAME;
    mPerfLib = NULL;
    mPerfEnable = NULL;
    mPerfDisable = NULL;
    mPerfReg = NULL;
    mUser = 0;
    mEnable = MFALSE;
    mPerfHandle = -1;
}
//----------------------------------------------------------------------------
CpuCtrlImp::
~CpuCtrlImp()
{
    FUNCTION_NAME;
}
//-----------------------------------------------------------------------------
CpuCtrl*
CpuCtrl::
createInstance(void)
{
    FUNCTION_NAME;
    CpuCtrlImp* pInstance = new CpuCtrlImp();
    return pInstance;
}
//----------------------------------------------------------------------------
MVOID
CpuCtrlImp::
destroyInstance(void)
{
    FUNCTION_IN;
    //
    delete this;
    //
    FUNCTION_OUT;
}
//----------------------------------------------------------------------------
MBOOL
CpuCtrlImp::
enable(CPU_CTRL_INFO &cpuInfo)
{
    CAM_TRACE_NAME("CpuCtrl::enable");
    FUNCTION_IN;
    //
    MBOOL ret = MTRUE;
    //
    {
        Mutex::Autolock templock(mLock);
        //
        if(mUser <= 0)
        {
            LOG_WRN("No user");
            ret = MFALSE;
            FUNCTION_OUT;
            return ret;
        }
    }
    //
    if(mEnable)
    {
        disable();
    }
    //
    Mutex::Autolock lock(mLock);
    //
    LOG_MSG("big(%d/%d),little(%d/%d),timeout(%d)",
            cpuInfo.bigCore,
            cpuInfo.bigFreq,
            cpuInfo.littleCore,
            cpuInfo.littleFreq,
            cpuInfo.timeout);
    //
    mCpuCtrlInfo = cpuInfo;
    //
    mPerfHandle = mPerfReg(
                    mCpuCtrlInfo.bigCore,
                    mCpuCtrlInfo.bigFreq,
                    mCpuCtrlInfo.littleCore,
                    mCpuCtrlInfo.littleFreq);
    mPerfEnable(
        mPerfHandle,
        mCpuCtrlInfo.timeout);
    mEnable = MTRUE;
    //
    EXIT:
    FUNCTION_OUT;
    return ret;
}
//----------------------------------------------------------------------------
MBOOL
CpuCtrlImp::
disable()
{
    CAM_TRACE_NAME("CpuCtrl::disable");
    FUNCTION_IN;
    //
    MBOOL ret = MTRUE;
    //
    Mutex::Autolock lock(mLock);
    //
    if(mUser <= 0)
    {
        LOG_WRN("No user");
        ret = MFALSE;
        goto EXIT;
    }
    //
    if(mEnable)
    {
        LOG_MSG("big(%d/%d),little(%d/%d),timeout(%d)",
                mCpuCtrlInfo.bigCore,
                mCpuCtrlInfo.bigFreq,
                mCpuCtrlInfo.littleCore,
                mCpuCtrlInfo.littleFreq,
                mCpuCtrlInfo.timeout);
        //
        mPerfDisable(mPerfHandle);
        mEnable = MFALSE;
    }
    else
    {
        LOG_MSG("Not thing to disable");
    }
    //
    EXIT:
    FUNCTION_OUT;
    return ret;
}
//----------------------------------------------------------------------------
MBOOL
CpuCtrlImp::
init(void)
{
    CAM_TRACE_NAME("CpuCtrl::init");
    //
    MBOOL ret = MTRUE;
    MUINT32 i;
    //
    Mutex::Autolock lock(mLock);
    //
    if(mUser == 0)
    {
        LOG_MSG("First user(%d)",mUser);
    }
    else
    {
        LOG_MSG("More user(%d)",mUser);
        android_atomic_inc(&mUser);
        goto EXIT;
    }
    //
    mPerfLib = NULL;
    mPerfEnable = NULL;
    mPerfDisable = NULL;
    mEnable = MFALSE;
    mPerfHandle = -1;
    //
    if(!mPerfLib)
    {
        mPerfLib = ::dlopen(PERF_LIB_FULLNAME, RTLD_NOW);
        if(! mPerfLib)
        {
            char const *err_str = ::dlerror();
            LOG_ERR("dlopen: %s error=%s", PERF_LIB_FULLNAME, (err_str ? err_str : "unknown"));
            ret = MFALSE;
            goto EXIT;
        }
    }
    //
    mPerfEnable = reinterpret_cast<funcEnableTimeout>(dlsym(mPerfLib, STR_FUNC_ENABLETIMEOUT));
    if(mPerfEnable == NULL)
    {
        LOG_WRN("cannot get %s", STR_FUNC_ENABLETIMEOUT);
    }
    //
    mPerfDisable = reinterpret_cast<funcDisable>(dlsym(mPerfLib, STR_FUNC_DISABLE));
    if(mPerfDisable == NULL)
    {
        LOG_WRN("cannot get %s", STR_FUNC_DISABLE);
    }
    //
    mPerfReg = reinterpret_cast<funcPerfRegBL>(dlsym(mPerfLib, STR_FUNC_REG));
    if(mPerfReg == NULL)
    {
        LOG_WRN("cannot get %s", STR_FUNC_REG);
        ret = MFALSE;
        goto EXIT;
    }
    //
    android_atomic_inc(&mUser);
    //
    EXIT:
    FUNCTION_OUT;
    return ret;
}
//----------------------------------------------------------------------------
MBOOL
CpuCtrlImp::
uninit(void)
{
    CAM_TRACE_NAME("CpuCtrl::uninit");
    //
    MBOOL ret = MTRUE;
    MUINT32 i;
    status_t status;
    funcPerfUnreg pUnreg = NULL;
    //
    {
        Mutex::Autolock templock(mLock);
        //
        if(mUser <= 0)
        {
            LOG_WRN("No user(%d)",mUser);
            FUNCTION_OUT;
            return ret;
        }
    }
    //
    if(mEnable)
    {
        disable();
    }
    //
    Mutex::Autolock lock(mLock);
    //
    android_atomic_dec(&mUser);
    //
    if(mUser == 0)
    {
        LOG_MSG("Last user(%d)",mUser);
    }
    else
    {
        LOG_MSG("More user(%d)",mUser);
        goto EXIT;
    }
    //
    pUnreg = reinterpret_cast<funcPerfUnreg>(dlsym(mPerfLib, STR_FUNC_UNREG));
    if(pUnreg != NULL)
    {
        pUnreg(mPerfHandle);
        mPerfHandle = -1;
    }
    else
    {
        LOG_WRN("cannot get %s", STR_FUNC_UNREG);
    }
    //
    if(!mPerfLib)
    {
        LOG_WRN("no lib");
        ret = MFALSE;
        goto EXIT;
    }
    //
    mPerfEnable = NULL;
    mPerfDisable = NULL;
    //
    ::dlclose(mPerfLib);
    mPerfLib = NULL;
    //
    EXIT:
    FUNCTION_OUT;
    return ret;
}
//----------------------------------------------------------------------------


