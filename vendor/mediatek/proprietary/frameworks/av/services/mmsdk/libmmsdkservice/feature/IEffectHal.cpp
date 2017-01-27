/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

//#include <cutils/xlog.h>
#define LOG_TAG "mmsdk/IEffectHal"

#include <cutils/log.h>
#include <utils/Errors.h>
#include <gui/IGraphicBufferProducer.h>
#include <mmsdk/IEffectHal.h>


/******************************************************************************
 *
 ******************************************************************************/
#define FUNCTION_LOG_START      ALOGD("[%s] - E.", __FUNCTION__)
#define FUNCTION_LOG_END        ALOGD("[%s] - X.", __FUNCTION__)


using namespace NSCam;
using namespace android;

enum {
//public: // may change state
    INIT = IBinder::FIRST_CALL_TRANSACTION,
    UNINIT,
    CONFIGURE,
    UNCONFIGURE,
    START,
    ABORT,
//public: // would not change state
    GET_NAME_VERSION,
    SET_EFFECT_LISTENER,
    SET_PARAMETER,
    GET_CAPTURE_REQUIREMENT,
    PREPARE,
    RELEASE,
    ADD_INPUT_FRAME,
    ADD_OUTPUT_FRAME,
};


class BpEffectHal : public BpInterface<IEffectHal>
{
public:
    BpEffectHal(const sp<IBinder>& impl)
        : BpInterface<IEffectHal>(impl)
    {
    }

public: // may change state
    virtual status_t   init()
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(INIT, data, &reply);
        
        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            _result = reply.readInt32();
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        
        FUNCTION_LOG_END;
        return _result;
    }

    
    virtual status_t   uninit()
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(UNINIT, data, &reply);

        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            _result = reply.readInt32();
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        FUNCTION_LOG_END;
        return _result;
    }

    
    virtual status_t   configure()
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(CONFIGURE, data, &reply);

        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            _result = reply.readInt32();
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        FUNCTION_LOG_END;
        return _result;
    }

    
    virtual status_t   unconfigure()
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(UNCONFIGURE, data, &reply);

        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            _result = reply.readInt32();
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        FUNCTION_LOG_END;
        return _result;
    }


    virtual uint64_t   start()
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(START, data, &reply);

        uint64_t uid = 0;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            uid = reply.readInt64();
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        FUNCTION_LOG_END;
        return uid;
    }


    virtual status_t   abort(EffectParameter const *parameter)
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        if(parameter != NULL) {
            data.writeInt32(1);
            data.write(*parameter);
        } else {
            data.writeInt32(0);
        }
        remote()->transact(ABORT, data, &reply);
        
        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if(!exceptionCode) {
            _result = reply.readInt32();
        }
        FUNCTION_LOG_END;
        return _result;
    }
    
public: // would not change state
    virtual status_t   getNameVersion(EffectHalVersion &nameVersion) const
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(GET_NAME_VERSION, data, &reply);
        
        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            _result = reply.readInt32();
            if (reply.readInt32() != 0) {
                reply.read(nameVersion);
            }
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        FUNCTION_LOG_END;
        return _result;
    }
    

    virtual status_t   setEffectListener(const wp<IEffectListener>& listener)
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        // data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        // data.writeStrongBinder((listener!=NULL)?listener->asBinder():NULL);
        // remote()->transact(SET_EFFECT_LISTENER, data, &reply);
        
        status_t _result = UNKNOWN_ERROR;
        // int32_t exceptionCode = reply.readExceptionCode();
        // if (!exceptionCode) {
        //     _result = reply.readInt32();
        // } else {
        //     // An exception was thrown back; fall through to return failure
        //     ALOGE("caught exception %d\n", exceptionCode);
        // }

        FUNCTION_LOG_END;
        return _result;
    }


    virtual status_t   setParameter(String8 &key, String8 &object)
    {
        FUNCTION_LOG_START;
        status_t status = OK;
        //@todo implement this
        FUNCTION_LOG_END;
        return status;
    }


    virtual status_t   setParameter(const sp<IEffectListener>& listener)
    {
        FUNCTION_LOG_START;
        status_t status = OK;
        //@todo implement this
        FUNCTION_LOG_END;
        return status;
    }

    virtual status_t   setParameters(const sp<EffectParameter> parameter)
    {
        FUNCTION_LOG_START;
        status_t status = OK;
        FUNCTION_LOG_END;
        return status;
    }


    virtual status_t   getCaptureRequirement(EffectParameter *inputParam, Vector<EffectCaptureRequirement> &requirements) const
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(GET_CAPTURE_REQUIREMENT, data, &reply);
        
        status_t _result = UNKNOWN_ERROR;
        // int32_t exceptionCode = reply.readExceptionCode();
        // if (!exceptionCode) {
        //     _result = reply.readInt32();
        //     if (reply.readInt32() != 0) {
        //         reply.read(requirement);
        //     }
        // } else {
        //     // An exception was thrown back; fall through to return failure
        //     ALOGE("caught exception %d\n", exceptionCode);
        // }

        FUNCTION_LOG_END;
        return _result;
    }
    

    //non-blocking
    virtual status_t   prepare()
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(PREPARE, data, &reply);
        
        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            _result = reply.readInt32();
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        FUNCTION_LOG_END;
        return _result;
    }
    

    virtual status_t   release()
    {
        FUNCTION_LOG_START;
        Parcel data, reply;
        data.writeInterfaceToken(IEffectHal::getInterfaceDescriptor());
        remote()->transact(RELEASE, data, &reply);
        
        status_t _result = UNKNOWN_ERROR;
        int32_t exceptionCode = reply.readExceptionCode();
        if (!exceptionCode) {
            _result = reply.readInt32();
        } else {
            // An exception was thrown back; fall through to return failure
            ALOGE("caught exception %d\n", exceptionCode);
        }
        FUNCTION_LOG_END;
        return _result;
    }
    

    //non-blocking
    virtual status_t   addInputFrame(const android::sp<IImageBuffer> frame, const android::sp<EffectParameter> parameter=NULL)
    {
        //@todo implement this
        return OK;
    }
    

    //non-blocking
    virtual status_t   addOutputFrame(const android::sp<IImageBuffer> frame, const android::sp<EffectParameter> parameter=NULL)
    {
        //@todo implement this
        return OK;
    }
};


IMPLEMENT_META_INTERFACE(EffectHal, "com.mediatek.mmsdk.IEffectHal");


status_t BnEffectHal::onTransact(
        uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    FUNCTION_LOG_START;
    ALOGD("[%s] - code=%d", __FUNCTION__, code);
    switch(code) {
//public: // may change state
        case INIT: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            status_t _result = init();            
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

        case UNINIT: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            status_t _result = uninit();            
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

        case CONFIGURE: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            status_t _result = configure();            
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

        case UNCONFIGURE: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            status_t _result = unconfigure();            
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

        case START: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            uint64_t uid = start();            
            reply->writeNoException();
            reply->writeInt64(uid);
            return OK;
        } break;

        case ABORT: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            EffectParameter parameter;
            if(data.readInt32() != 0) {
                data.read(parameter);
            }
            status_t _result = abort(&parameter);
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

//public: // would not change state
        case GET_NAME_VERSION: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            EffectHalVersion version;
            status_t _result = getNameVersion(version);
            reply->writeNoException();
            if(_result == OK) {
                reply->writeInt32(1);
                reply->write(version);
            } else {
                reply->writeInt32(0);
                ALOGD("GET_NAME_VERSION _result=%d", _result);
            }
            return OK;
        } break;

        case SET_EFFECT_LISTENER: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            wp<IEffectListener> listener = interface_cast<IEffectListener>(data.readStrongBinder());
            status_t _result = setEffectListener(listener);
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

        case SET_PARAMETER: {
            //@todo implement this
            return OK;
        } break;

        case GET_CAPTURE_REQUIREMENT: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            // EffectParameter inputParam;
            // Vector<EffectCaptureRequirement> requirements;

            // int size = data.readInt32();
            // if(size != 0) {
            //     ALOGD("GET_CAPTURE_REQUIREMENT, size=%d, dataPosition=%d", size, data.dataPosition());
            //     const String8 param = String8(data.readString16());
            //     ALOGD("GET_CAPTURE_REQUIREMENT, param=%s", param.string());
            //     inputParam.unflatten(param);
            // }

            // status_t result = getCaptureRequirement(&inputParam, requirements);


            // reply->writeNoException();
            // reply->writeInt32(result);
            // reply->writeInt32(requirements.size());
            // ALOGD("requirements size=%d", requirements.size());
            // for (int i=0; i<requirements.size(); ++i)
            // {

            //     if (requirements[i] != 0) 
            //     {
            //         ALOGD("GET_CAPTURE_REQUIREMENT requirement set!! index=%d", i);
            //         reply->writeInt32(1);
            //         reply->write(requirements[i]);
            //     } 
            //     else 
            //     {
            //         ALOGD("requirement=null");
            //         reply->writeInt32(0);
            //     }
            // }
            return OK;
        } break;

        case PREPARE: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            status_t _result = prepare();
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

        case RELEASE: {
            CHECK_INTERFACE(IEffectHal, data, reply);
            status_t _result = release();
            reply->writeNoException();
            reply->writeInt32(_result);
            return OK;
        } break;

        case ADD_INPUT_FRAME: {
            //@todo implement this
            reply->writeNoException();
            return OK;
        } break;

        case ADD_OUTPUT_FRAME: {
            //@todo implement this
            reply->writeNoException();
            return OK;
        } break;
    }
    FUNCTION_LOG_END;
    return BBinder::onTransact(code, data, reply, flags);
}

