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
#define LOG_TAG "mmsdk/EffectHalClient"


#include <cutils/log.h>
#include <utils/Errors.h>


//#include "EffectHalClient.h"
#include <mmsdk/IEffectHal.h>

#include <mtkcam/common.h>
#include <mtkcam/utils/imagebuf/IGraphicImageBufferHeap.h>

#include <gui/Surface.h>

//use property_get
#include <cutils/properties.h>

//#include <device3/Camera3Device.h>


/******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[%s] " fmt, __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)

// #define FUNCTION_LOG_START          MY_LOGD_IF(1<=mLogLevel, "+");
// #define FUNCTION_LOG_END            MY_LOGD_IF(1<=mLogLevel, "-");
#define FUNCTION_LOG_START          MY_LOGD_IF(1<=1, "+");
#define FUNCTION_LOG_END            MY_LOGD_IF(1<=1, "-");
/******************************************************************************
 *
 ******************************************************************************/



/******************************************************************************
 *
 ******************************************************************************/
#include <cutils/xlog.h>
// #define MY_LOGV(fmt, arg...)       XLOGV(fmt"\r\n", ##arg)
// #define MY_LOGD(fmt, arg...)       XLOGD(fmt"\r\n", ##arg)
// #define MY_LOGI(fmt, arg...)       XLOGI(fmt"\r\n", ##arg)
// #define MY_LOGW(fmt, arg...)       XLOGW(fmt"\r\n", ##arg)
// #define MY_LOGE(fmt, arg...)       XLOGE(fmt" (%s){#%d:%s}""\r\n", ##arg, __FUNCTION__, __LINE__, __FILE__)


// #define FUNCTION_LOG_START      ALOGD("[%s] - E.", __FUNCTION__)
// #define FUNCTION_LOG_END        ALOGD("[%s] - X.", __FUNCTION__)

using namespace std;
using namespace NSCam;
using namespace android;

//-----------------------------------------------------------------------------
//public: // ctor, dtor
//-----------------------------------------------------------------------------

const char EffectHalClient::KEY_PICTURE_SIZE[] = "picture-size";
const char EffectHalClient::KEY_PICTURE_FORMAT[] = "picture-format";


void
BufferListener::
onFrameAvailable(const BufferItem& item)
{
    MY_LOGD("BufferListener::[%s]: index=%d", __FUNCTION__, mIdx);
    sp<EffectHalClient> listener;
    listener = mFrameAvailableListener.promote();
    listener->onBufferFrameAvailable(mIdx);

    //listener->availableBufferIdx = mIdx;
}


EffectHalClient::
EffectHalClient(IEffectHal* effect)
{
    mpEffect = effect;
    mMaxBufferQueueSize = 5;
    mSurfaceMap.clear();
    mInputComsumer.clear();
    mListener.clear();
    mBufferMap.clear();
    mInputSyncMode.clear();
    mInputFrameInfo.clear();
    
    mOutputBuffers.clear();

    availableBufferIdx = 0;
    char cLogLevel[PROPERTY_VALUE_MAX];
    ::property_get("debug.camera.log", cLogLevel, "0");
    mLogLevel = ::atoi(cLogLevel);
    if ( 0 == mLogLevel ) {
        ::property_get("debug.camera.log.effecthal", cLogLevel, "1");
        mLogLevel = ::atoi(cLogLevel);
    }

}


EffectHalClient::
~EffectHalClient()
{
    FUNCTION_LOG_START;
    mEffectListener = NULL;
    mpEffect = NULL;
    FUNCTION_LOG_END;
}



//-----------------------------------------------------------------------------
//public: // may change state
//-----------------------------------------------------------------------------
status_t
EffectHalClient::
init() 
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->init();

FUNCTION_END:    
    FUNCTION_LOG_END;
    return ret;
}


status_t
EffectHalClient::
uninit() 
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->uninit();

FUNCTION_END:
    FUNCTION_LOG_END;
    return ret;
}


status_t
EffectHalClient::
configure() 
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->configure();

FUNCTION_END:    
    FUNCTION_LOG_END;
    return ret;
}


status_t
EffectHalClient::
unconfigure() 
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->unconfigure();

FUNCTION_END:    
    FUNCTION_LOG_END;
    return ret;
}


uint64_t
EffectHalClient::
start() 
{
    FUNCTION_LOG_START;
    //status_t ret = OK;
    uint64_t uid;

    uid = mpEffect->start();

    //test, get output buffer
    mOutputBuffers.clear();
    for (size_t idx = 0; idx < mOutputSurfaces.size(); ++idx) 
    {
        sp<ANativeWindow> anw = mOutputSurfaces[idx];
        getOutputBuffer(idx, mOutputEffectParams[idx]->getInt("picture-number"), 
                        anw, mOutputEffectParams[idx]);
    }

FUNCTION_END:
    FUNCTION_LOG_END;
    //return ret;
    return uid;
}


status_t
EffectHalClient::
abort(EffectParameter const *parameter) 
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->abort();    

FUNCTION_END:    
    FUNCTION_LOG_END;
    return ret;
}


//-----------------------------------------------------------------------------
//public: // would not change state
//-----------------------------------------------------------------------------
status_t                
EffectHalClient::
getNameVersion(EffectHalVersion &nameVersion) const
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->getNameVersion(nameVersion);
    
FUNCTION_END:    
    FUNCTION_LOG_END;
    return ret;
}


status_t
EffectHalClient::
setEffectListener(const wp<IEffectListener> &listener)
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    mEffectListener = listener.promote();

    ret = mpEffect->setEffectListener(this);

FUNCTION_END:
    FUNCTION_LOG_END;
    return ret;
}


status_t
EffectHalClient::
setParameter(String8 &key, String8 &object)
{
    FUNCTION_LOG_START;
    status_t ret = OK;
    MY_LOGD_IF(mLogLevel, "[%s]: key=%s, value:%s", __FUNCTION__, key.string(), object.string());
    ret = mpEffect->setParameter(key, object);

FUNCTION_END:
    FUNCTION_LOG_END;
    return ret;
}

status_t
EffectHalClient::
setParameters(sp<EffectParameter> parameter)
{
    FUNCTION_LOG_START;
    status_t ret = OK;
    MY_LOGD_IF(mLogLevel, "[%s]: height=%s, level:%s", __FUNCTION__, parameter->get("picture-height"), parameter->get("fb-smooth-level"));
    
    ret = mpEffect->setParameters(parameter);

FUNCTION_END:
    FUNCTION_LOG_END;
    return ret;
}

void
EffectHalClient::
setStaticMetadata(sp<BasicParameters> staticMetadata)
{
    FUNCTION_LOG_START;
    status_t ret = OK;
    MY_LOGD_IF(mLogLevel, "[%s] maxJpegsize=%d, maxJpegWidth=%d, maxJpegHeight=%d", 
            __FUNCTION__, staticMetadata->getInt("maxJpegsize"), 
            staticMetadata->getInt("maxJpegWidth"), 
            staticMetadata->getInt("maxJpegHeight"));

    mpStaticMetadata = staticMetadata;
    FUNCTION_LOG_END;
}


status_t           
EffectHalClient::
getCaptureRequirement(EffectParameter *inputParam, Vector<EffectCaptureRequirement> &requirements) const 
{   
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->getCaptureRequirement(inputParam, requirements);

    //set surface-id
    int surfaceId;
    EffectCaptureRequirement*const editedRequirements = requirements.editArray();
    for(int i=0; i<requirements.size(); ++i)
    {
        if (requirements[i].get(KEY_PICTURE_SIZE) && requirements[i].get(KEY_PICTURE_FORMAT) )
        {
            String8 picFormat = String8(requirements[i].get(KEY_PICTURE_SIZE));
            picFormat += requirements[i].get(KEY_PICTURE_FORMAT);
            surfaceId = mSurfaceMap.valueFor(picFormat);
            MY_LOGD_IF(mLogLevel, "[%s]: picFormat=%s, surfaceId=%d", __FUNCTION__, picFormat.string(), surfaceId);
            editedRequirements[i].set("surface-id", surfaceId);
        }
        else
        {
            ALOGE("picture-size or picture-format is null");
        }
    }


FUNCTION_END:
    FUNCTION_LOG_END;
    return ret;
}


//non-blocking
status_t           
EffectHalClient::
prepare() 
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->prepare();

FUNCTION_END:    
    FUNCTION_LOG_END;
    return ret;
}


status_t
EffectHalClient::
release() 
{
    FUNCTION_LOG_START;
    status_t ret = OK;

    ret = mpEffect->release();

FUNCTION_END:
    FUNCTION_LOG_END;
    return ret;
}


status_t
EffectHalClient::
addInputFrame(const sp<IImageBuffer> frame, const sp<EffectParameter> parameter )
{
    FUNCTION_LOG_START;

FUNCTION_END:
    FUNCTION_LOG_END;
    return INVALID_OPERATION;
}


status_t
EffectHalClient::
addOutputFrame(const sp<IImageBuffer> frame, const sp<EffectParameter> parameter )
{
    FUNCTION_LOG_START;

FUNCTION_END:
    FUNCTION_LOG_END;
    return INVALID_OPERATION;
}



void
EffectHalClient::
addBufferQueue(Vector< sp<IGraphicBufferProducer> > &input, int index, int *height, int *width, int format)
{
    //create bufferqueue and bufferItemConsumer
    status_t res;
    sp<IGraphicBufferProducer> producer;
    sp<IGraphicBufferConsumer> consumer;
    sp<BufferItemConsumer> itemConsumer;

    BufferQueue::createBufferQueue(&producer, &consumer);

    //get buffer count 
    int minUndequeuedBuffers = 0;
    res = producer->query(NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS, &minUndequeuedBuffers);
    if (res != OK || minUndequeuedBuffers < 0) 
    {
        ALOGE("%s: Could not query min undequeued buffers (error %d, bufCount %d)",
              __FUNCTION__, res, minUndequeuedBuffers);
        return;
    }
    size_t minBufs = static_cast<size_t>(minUndequeuedBuffers);
    mMaxBufferQueueSize = mMaxBufferQueueSize > minBufs ? mMaxBufferQueueSize : minBufs;

    //itemConsumer = new BufferItemConsumer(consumer, GRALLOC_USAGE_SW_READ_OFTEN | GRALLOC_USAGE_HW_RENDER, mMaxBufferQueueSize);

    itemConsumer = new BufferItemConsumer(consumer, (GRALLOC_USAGE_SW_READ_OFTEN|GRALLOC_USAGE_SW_WRITE_OFTEN)
                            |(GRALLOC_USAGE_HW_CAMERA_READ|GRALLOC_USAGE_HW_CAMERA_WRITE), mMaxBufferQueueSize);
    
    MY_LOGD_IF(mLogLevel, "[%s]: producer(Binder):%p, minBufs=%d, mMaxBufferQueueSize=%d, itemConsumer=%p", 
        __FUNCTION__, producer->asBinder().get(), minBufs, mMaxBufferQueueSize, itemConsumer.get());
    

    //itemConsumer = new BufferItemConsumer(consumer, GRALLOC_USAGE_HW_VIDEO_ENCODER, mMaxBufferQueueSize + 1);

    //add EffectHalClient instance to BufferListener and add BufferListener instance to bufferqueue
    sp<BufferItemConsumer::FrameAvailableListener> listener;
    listener = new BufferListener(index, this);
    itemConsumer->setFrameAvailableListener(listener);
    mListener.push_back(listener);

    //set consumer name
    // char str[32];
    // sprintf(str, "EffectHalClient-Consumer-%d", i);
    // itemConsumer->setName(String8(str));
    itemConsumer->setName(String8::format("EffectHalClient-Consumer-%d", index));

    
    //set buffer size
    res = itemConsumer->setDefaultBufferSize(*width, *height);
    if (res != OK) 
    {
        ALOGE("%s: Could not set buffer dimensions (w = %d, h = %d)", __FUNCTION__, *width, *height);
        return;
    }
    MY_LOGD_IF(mLogLevel, "[%s]: set buffer dimensions (w = %d, h = %d)", __FUNCTION__, *width, *height);
    
    //set buffer format
    res = itemConsumer->setDefaultBufferFormat(format);
    if (res != OK) 
    {
        ALOGE("%s: Could not set buffer format:(0x%x)", __FUNCTION__, format);
        return;
    }
    MY_LOGD_IF(mLogLevel, "[%s]: set buffer format:(0x%x)", __FUNCTION__, format);

    mInputComsumer.push_back(itemConsumer);
    input.push_back(producer);
    //set default sync mode
    mInputSyncMode.push_back(false);

    //for test
    mProducer = producer;
}


static int parse_pair(const char *str, int *first, int *second, char delim,
                      char **endptr = NULL)
{
    // Find the first integer.
    char *end;
    int w = (int)strtol(str, &end, 10);
    // If a delimeter does not immediately follow, give up.
    if (*end != delim) 
    {
        ALOGE("Cannot find delimeter (%c) in str=%s", delim, str);
        return -1;
    }

    // Find the second integer, immediately after the delimeter.
    int h = (int)strtol(end+1, &end, 10);

    *first = w;
    *second = h;

    if (endptr) {
        *endptr = end;
    }
    return 0;
}

//-----------------------------------------------------------------------------
//API for buffer queue
//-----------------------------------------------------------------------------
status_t
EffectHalClient::
getInputSurfaces(Vector< sp<IGraphicBufferProducer> > &input)
{
    FUNCTION_LOG_START;
    mSurfaceMap.clear();
    mInputComsumer.clear();
    mListener.clear();
    mBufferMap.clear();
    mInputSyncMode.clear();
    mInputFrameInfo.clear();

    EffectParameter *inputParam = NULL;
    Vector<EffectCaptureRequirement> requirements;
    status_t ret = mpEffect->getCaptureRequirement(inputParam, requirements);
    MY_LOGD_IF(mLogLevel, "[%s]: requirements.size()=%d", __FUNCTION__, requirements.size());

    //decided surface numbers according to picture size and format
    for(int i=0; i<requirements.size(); ++i)
    {
        //get format and hight,weight
        if (requirements[i].get(KEY_PICTURE_SIZE) && requirements[i].get(KEY_PICTURE_FORMAT) )
        {
            String8 picFormat = String8(requirements[i].get(KEY_PICTURE_SIZE));
            picFormat += requirements[i].get(KEY_PICTURE_FORMAT);
            MY_LOGD_IF(mLogLevel, "[%s]: picFormat=%s, is exist=%d", __FUNCTION__, picFormat.string(), 
                                                        mSurfaceMap.valueFor(picFormat));

            if (mSurfaceMap.valueFor(picFormat) == 0)
            {
                int width, height;
                int _size = mSurfaceMap.size();
                mSurfaceMap.replaceValueFor(picFormat, _size+1);
                parse_pair(requirements[i].get(KEY_PICTURE_SIZE), &height, &width, 'x');
                addBufferQueue(input, _size, &height, &width, requirements[i].getInt(KEY_PICTURE_FORMAT)); //jpeg
            }
        }
        else
        {
            MY_LOGD_IF(mLogLevel, "[%s]: picture-size or picture-format is null", __FUNCTION__);
        }
    }

    MY_LOGD_IF(mLogLevel, "[%s]: mListener size=%d", __FUNCTION__, mListener.size());
    FUNCTION_LOG_END;
    return OK;
}



status_t
EffectHalClient::
addInputParameter(int index, sp<EffectParameter> &parameter, int64_t timestamp, bool repeat)
//addInputParameter(int index, EffectParameter &parameter, int64_t timestamp, bool repeat)
{
    MY_LOGD_IF(mLogLevel, "[%s]: index=%d, mInputFrameInfo size=%d", __FUNCTION__, index, mInputFrameInfo.size());
    //todo: if repeat is true, cache the parameter

    int res;
    bool sync;
    sp<FrameInfo> frameInfo = new FrameInfo();
    frameInfo->mIndex = index;
    frameInfo->mTimestamp = timestamp;
    frameInfo->mRepeat = repeat;
    frameInfo->mEffectParam = parameter;

    mInputFrameInfo.replaceValueFor(timestamp, frameInfo);
    //mInputFrameInfo.push_back(frameInfo);
    // size_t frameInfoSize = mInputFrameInfo.size();
    // MY_LOGD_IF(mLogLevel, "mInputFrameInfo replace idx=%d, ts=%d, ready=%d, repeat=%d", 
    //             index, mInputFrameInfo[frameInfoSize-1]->mTimestamp, 
    //             mInputFrameInfo[frameInfoSize-1]->isReady, 
    //             mInputFrameInfo[frameInfoSize-1]->mRepeat);


    MY_LOGD_IF(mLogLevel, "[%s]: mInputFrameInfo mInputFrameInfo idx=%d, ts=%" PRId64 " ", __FUNCTION__, 
            mInputFrameInfo.valueFor(timestamp)->mIndex, mInputFrameInfo.valueFor(timestamp)->mTimestamp);

    //check sync mode
    sync = getInputsyncMode(index);
    if (sync)
    {
        if(mBufferMap.indexOfKey(timestamp) < 0)
        {
            MY_LOGD_IF(mLogLevel, "[%s]: ImageBuffer not ready yet. Save buffer info.(surface index=%d)", __FUNCTION__, index); 
        }
        else
        {
            BufferItemConsumer::BufferItem imgBuffer = mBufferMap.valueFor(timestamp);
            MY_LOGD_IF(mLogLevel, "[%s]: matched: index=%d, ts=%" PRId64 "", __FUNCTION__, index, timestamp); 
            //convert to image buffer
            sp<IImageBuffer> imageBuffer;
            convertGraphicBufferToIImageBuffer(imgBuffer.mGraphicBuffer, imageBuffer);

            res = mpEffect->addInputFrame(imageBuffer, parameter);
        }
    }
    else
    {
        //call directly
        sp<IImageBuffer> imageBuffer;
        res = mpEffect->addInputFrame(imageBuffer, parameter);
    }
    return OK;
}


status_t
EffectHalClient::
addOutputParameter(int index, EffectParameter &parameter, int64_t timestamp, bool repeat)
{
    //@todo implement this
    return OK;
}


status_t
EffectHalClient::
setInputsyncMode(int index, bool sync)
{
    //david add
    FUNCTION_LOG_START;
    if (index > mInputSyncMode.size()-1)
    {
        //return outof index;
        return OK;
    }
    MY_LOGD_IF(mLogLevel, "[%s]: index=%d, sync mode=%d", __FUNCTION__, index, int(sync));
    mInputSyncMode.replaceAt(sync, index);
    FUNCTION_LOG_END;
    return OK;
}


bool
EffectHalClient::
getInputsyncMode(int index)
{
    //david add
    MY_LOGD_IF(mLogLevel, "[%s]: index=%d, sync mode=%d", __FUNCTION__, index, int(mInputSyncMode.itemAt(index)));
    return mInputSyncMode.itemAt(index);
}


status_t
EffectHalClient::
setOutputsyncMode(int index, bool sync)
{
    //david add
    FUNCTION_LOG_START;
    if (index < mOutputSyncMode.size()-1)
    {
        //return outof index;
        return OK; 
    }
    mOutputSyncMode.replaceAt(sync, index);
    FUNCTION_LOG_END;
    return OK;
}


bool
EffectHalClient::
getOutputsyncMode(int index)
{
    //david add
    return mOutputSyncMode.itemAt(index);
}

void
EffectHalClient::
onInputFrameAvailable()
{
    //@todo implement this
}


void
EffectHalClient::
onInputSurfacesChanged(EffectResult partialResult)
{
    //@todo implement this
}

status_t
EffectHalClient::
setInputSurfaces(Vector< sp<IGraphicBufferProducer> > &input)
{
    //@todo implement this
    return OK;
}


void
EffectHalClient::
convertGraphicBufferToIImageBuffer(sp<GraphicBuffer> &buf, sp<IImageBuffer> &imageBuffer)
{
#if '1' == MTKCAM_HAVE_IIMAGE_BUFFER
    sp<IGraphicImageBufferHeap> bufferHeap = IGraphicImageBufferHeap::create(LOG_TAG, buf.get());
    //IGraphicImageBufferHeap *bufferHeap = IGraphicImageBufferHeap::create(LOG_TAG, buf.get());
    imageBuffer = bufferHeap->createImageBuffer();
#endif
}



void
EffectHalClient::
getOutputBuffer(int surfaceIdx, int bufferCount, sp<ANativeWindow> anw, sp<EffectParameter> param)
{
    ANativeWindowBuffer* anb;
    sp<IImageBuffer> imageBuffer;
    status_t res;

    for(int i=0; i<bufferCount; i++)
    {
        MY_LOGD_IF(mLogLevel, "[%s]: Dequeue buffer from %p", __FUNCTION__, anw.get());
        /************** for test, ap have to done this. ***************/
        // res = native_window_set_usage(anw.get(),
        // GRALLOC_USAGE_SW_WRITE_OFTEN);
        // MY_LOGD_IF(mLogLevel, "set_usage, result: %d", res);

        // int minUndequeuedBuffers;
        // res = anw.get()->query(anw.get(),
        //         NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS,
        //         &minUndequeuedBuffers);
        // MY_LOGD_IF(mLogLevel, "query, result: %d", res);
        /************** for test, ap have to done this. ***************/


        res = native_window_dequeue_buffer_and_wait(anw.get(), &anb);
        if (res != OK) 
        {
            ALOGE("[%s]:  Unable to dequeue buffer: %s (%d) surfaceId:%d bufferIndex:%d", 
                __FUNCTION__, strerror(-res), res, surfaceIdx, mOutputBuffers.size());
            return;
        }

        //MY_LOGD_IF(mLogLevel, "anb should != NULL, result=%d", int(anb != NULL));
        sp<GraphicBuffer> buf(new GraphicBuffer(anb, false));

        //convert to IImageBuffer
        convertGraphicBufferToIImageBuffer(buf, imageBuffer);
        MY_LOGD_IF(mLogLevel, "ImageBuffer format=(0x%x), dimensions: (w = %d, h = %d)", 
                imageBuffer->getImgFormat(), imageBuffer->getImgSize().w, imageBuffer->getImgSize().h);
        MY_LOGD_IF(mLogLevel, "imageBuffer address=%p, GraphicBuffer address=%p, ANativeWindowBuffer=%p", 
                imageBuffer.get(), buf.get(), anb);


        // int64_t timestamp=1234L;
        // MY_LOGD_IF(mLogLevel, "Set timestamp:%d to %p", timestamp, anw.get());
        // err = native_window_set_buffers_timestamp(anw.get(), timestamp);
        // MY_LOGD_IF(mLogLevel, "Set timestamp result:%d", err);
        

        //cache buffer
        mOutputBuffers.push_back(buf);
        //add surface-id and bufferIndex
        param->set("surfaceId", surfaceIdx);
        param->set("bufferIndex", mOutputBuffers.size()-1);
        imageBuffer->lockBuf( "addOutputFrame", eBUFFER_USAGE_HW_CAMERA_READWRITE | eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_SW_WRITE_OFTEN );


        res = mpEffect->addOutputFrame(imageBuffer, param);
        //imageBuffer->unlockBuf( "addOutputFrame");

        //MY_LOGD_IF(mLogLevel, "Queue buffer to %p", anw.get());
        //err = anw->queueBuffer(anw.get(), buf->getNativeBuffer(), -1);
        //mOutputBuffers.replaceAt(param->getInt("bufferIndex"));

    }
}
    


/**
* init surfaces and parameters
* configure surface
    #.connect to bufferQueue
    #.query width, height and format info. for debug
*/
status_t
EffectHalClient::
setOutputSurfaces(Vector< sp<IGraphicBufferProducer> > &ouput, Vector<sp<EffectParameter> > &effectParams)
{
    FUNCTION_LOG_START;

    // init surfaces and parameters
    mOutputSurfaces.clear();
    mOutputSurfaces.setCapacity(ouput.size());
    mOutputEffectParams.clear();
    mOutputEffectParams.setCapacity(effectParams.size());

    mOutputEffectParams = effectParams;

    //configure
    for (size_t idx = 0; idx < ouput.size(); ++idx) 
    {
        sp<Surface> s = new Surface(ouput[idx]);
        sp<ANativeWindow> anw = s;
        //sp<ANativeWindow> anw(s);
        mOutputSurfaces.push_back(s);
        mOutputEffectParams.push_back(effectParams[idx]);
        status_t res;
        ANativeWindowBuffer* anb;

        MY_LOGD_IF(mLogLevel, "[%s]: idx=%d, producer(Binder):%p", __FUNCTION__, idx, ouput[idx]->asBinder().get());
        
        // connect to bufferQueue
        // res = native_window_api_connect(anw.get(), NATIVE_WINDOW_API_CAMERA);
        // MY_LOGD_IF(mLogLevel, "[%s]: connect=%d, native window is: %p", __FUNCTION__, res, anw.get());
        // if (res != OK) 
        // {
        //     ALOGE("%s: Unable to connect ANativeWindow: %s (%d)", __FUNCTION__, strerror(-res), res);
        //     //for test, do reconnect
        //     res = native_window_api_disconnect(anw.get(), NATIVE_WINDOW_API_CAMERA);
        //     MY_LOGD_IF(mLogLevel, "[%s]: disconnect=%d, native window is: %p", __FUNCTION__, res, anw.get());
        //     res = native_window_api_connect(anw.get(), NATIVE_WINDOW_API_CAMERA);
        //     MY_LOGD_IF(mLogLevel, "[%s]: connect retry, result=%d, native window is: %p", __FUNCTION__, res, anw.get());
        // }

        // query width, height format and usage info. for debug
        int width, height, format;
        int32_t consumerUsage;
        if ((res = anw->query(anw.get(), NATIVE_WINDOW_WIDTH, &width)) != OK) {
            ALOGE("[%s]: Failed to query Surface width", __FUNCTION__);
            return res;
        }
        if ((res = anw->query(anw.get(), NATIVE_WINDOW_HEIGHT, &height)) != OK) {
            ALOGE("[%s]: Failed to query Surface height", __FUNCTION__);
            return res;
        }
        if ((res = anw->query(anw.get(), NATIVE_WINDOW_FORMAT, &format)) != OK) {
            ALOGE("[%s]: Failed to query Surface format", __FUNCTION__);
            return res;
        }
        if ((res = anw->query(anw.get(), NATIVE_WINDOW_CONSUMER_USAGE_BITS, &consumerUsage)) != OK) {
            ALOGE("%s: Failed to query consumer usage", __FUNCTION__);
            return res;
        }
        MY_LOGD_IF(mLogLevel, "[%s]: (w = %d, h = %d), format:(0x%x), consumerUsage=%d", 
                    __FUNCTION__, width, height, format, consumerUsage);

        

        // for test, check camera device allow and disallow usage.
        /*int32_t disallowedFlags = GraphicBuffer::USAGE_HW_VIDEO_ENCODER |
                                  GRALLOC_USAGE_RENDERSCRIPT;
        int32_t allowedFlags = GraphicBuffer::USAGE_SW_READ_MASK |
                               GraphicBuffer::USAGE_HW_TEXTURE |
                               GraphicBuffer::USAGE_HW_COMPOSER;
        bool flexibleConsumer = (consumerUsage & disallowedFlags) == 0 &&
                (consumerUsage & allowedFlags) != 0;
        ALOGD("[%s]: consumerUsage=%08x, disallowedUsage:%08x, allowedUsage:%08x, isvalid=%d)", 
            __FUNCTION__, consumerUsage, disallowedFlags, allowedFlags, flexibleConsumer);*/

        // Configure consumer-side ANativeWindow interface
        //for test, 0x60033 copy from Camera3OutputStream.cpp
        //GRALLOC_USAGE_HW_CAMERA_MASK        = 0x00060000
        //GRALLOC_USAGE_SW_WRITE_OFTEN        = 0x00000030
        //GRALLOC_USAGE_SW_READ_OFTEN         = 0x00000003
        int32_t effectUsage = 0x60033;
        res = native_window_set_usage(anw.get(), effectUsage);
        MY_LOGD_IF(mLogLevel, "[%s]: native_window_set_usage :%08x", __FUNCTION__, effectUsage);
        if (res != OK) {
            ALOGE("[%s]: Unable to configure usage %08x", __FUNCTION__, effectUsage);
        }

        res = native_window_set_scaling_mode(anw.get(),
                NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW);
        if (res != OK) {
            ALOGE("[%s]: Unable to configure stream scaling: %s (%d)",
                    __FUNCTION__, strerror(-res), res);
        }

        ssize_t jpegBufferSize = 0;
        if (format == HAL_PIXEL_FORMAT_BLOB) 
        {
            // Calculate final jpeg buffer size for the given resolution.
            ssize_t maxJpegBufferSize = mpStaticMetadata->getInt("maxJpegsize");
            if (maxJpegBufferSize == 0)
            {
                float scaleFactor = ((float) (width * height)) /
                (mpStaticMetadata->getInt("maxJpegWidth") * mpStaticMetadata->getInt("maxJpegHeight"));

                jpegBufferSize = scaleFactor * (maxJpegBufferSize - kMinJpegBufferSize) +
                    kMinJpegBufferSize;

                MY_LOGD_IF(mLogLevel, "[%s]: jpegBufferSize=%d, kMinJpegBufferSize=%d, scaleFactor=%f",
                __FUNCTION__, jpegBufferSize, kMinJpegBufferSize, scaleFactor);

                if (jpegBufferSize > maxJpegBufferSize) 
                {
                    jpegBufferSize = maxJpegBufferSize;
                }
            }
            else
            {
                jpegBufferSize = width*height*1.2;
                if (jpegBufferSize > 5898240)
                {
                    jpegBufferSize = 5898240;
                }    
            }

            res = native_window_set_buffers_dimensions(anw.get(), jpegBufferSize, 1);
            MY_LOGD_IF(mLogLevel, "[%s]: native_window_set_buffers_dimensions: (w = %d, h = %d), realH:%d, realW:%d",
                __FUNCTION__, width, height, jpegBufferSize, 1);
        }
        else
        {
            // For buffers of known size
            res = native_window_set_buffers_dimensions(anw.get(), width, height);
            MY_LOGD_IF(mLogLevel, "[%s]: set non jpeg dimension: (w = %d, h = %d), realH:%d, realW:%d",
                __FUNCTION__, width, height, height, width);
        }


        // //check format and set the buffer width and height
        // //if format is blob(same to jpeg), have to get jpeg buffer size, for test set 1105920 directly.
        // // ssize_t jpegBufferSize = width*height*1.2;
        // // if (jpegBufferSize > 5898240)
        // // {
        // //     jpegBufferSize = 5898240;
        // // }
        // if (format == HAL_PIXEL_FORMAT_BLOB) {
        //     //ssize_t jpegBufferSize = Camera3Device::getJpegBufferSize(width, height); //must call with instance
        //     // if (jpegBufferSize <= 0) {
        //     //     ALOGE("Invalid jpeg buffer size %zd", jpegBufferSize);
        //     //     return BAD_VALUE;
        //     // }
        //     // For buffers with bounded size
        //     res = native_window_set_buffers_dimensions(anw.get(), jpegBufferSize, 1);
        //     MY_LOGD_IF(mLogLevel, "[%s]: native_window_set_buffers_dimensions: (w = %d, h = %d), realH:%d, realW:%d",
        //         __FUNCTION__, width, height, jpegBufferSize, 1);

        // } else {
        //     // For buffers of known size
        //     res = native_window_set_buffers_dimensions(anw.get(), width, height);
        //     MY_LOGD_IF(mLogLevel, "[%s]: set non jpeg dimension: (w = %d, h = %d), realH:%d, realW:%d",
        //         __FUNCTION__, width, height, height, width);
        // }

        if (res != OK) {
            ALOGE("%s: Unable to configure stream buffer dimensions"
                    " %d x %d (maxSize %zu)", __FUNCTION__, height, width, jpegBufferSize);
        }


        //set buffer format, can remove if ap is configed.
        res = native_window_set_buffers_format(anw.get(), format);
        MY_LOGD_IF(mLogLevel, "[%s]: native_window_set_buffers_format :format:(0x%x)", __FUNCTION__, format);
        if (res != OK) {
            ALOGE("%s: Unable to configure stream buffer format %#x", __FUNCTION__, format);
            return res;
        }

        int maxConsumerBuffers;
        res = anw->query(anw.get(), NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS, &maxConsumerBuffers);
        if (res != OK) {
            ALOGE("%s: Unable to query consumer undequeued", __FUNCTION__);
            return res;
        }

        MY_LOGD_IF(mLogLevel, "[%s]: Consumer wants %d buffers, HAL wants %d", __FUNCTION__, maxConsumerBuffers, 5);
        MY_LOGD_IF(mLogLevel, "[%s]: picture-numbers=%d", __FUNCTION__, effectParams[idx]->getInt("picture-number"));

        //ap have to set this
        res = native_window_set_buffer_count(anw.get(), 10);
        if (res != OK)
        {
            ALOGE("%s: Unable to native_window_set_buffer_count: %s (%d)", __FUNCTION__, strerror(-res), res);
            return OK;
        }
    } 
    FUNCTION_LOG_END;
    return OK;
}


status_t
EffectHalClient::
getOutputSurfaces(Vector< sp<IGraphicBufferProducer> > &ouput)
{
    //@todo implement this
    return OK;
}


void
EffectHalClient::
onBufferFrameAvailable(int idx)
{
    
    bool sync;
    status_t res;
    sp<BufferItemConsumer> consumer = mInputComsumer[idx];
    BufferItemConsumer::BufferItem imgBuffer;

    //acquire buffer
    res = consumer->acquireBuffer(&imgBuffer, 0);
    MY_LOGD_IF(mLogLevel, "[%s]: acquireBuffer, result=%d, itemConsumer=%p ", __FUNCTION__, res, consumer.get());
    if (res != OK) 
    {
        if (res == BufferItemConsumer::NO_BUFFER_AVAILABLE) 
        {
            MY_LOGE("%s: NO_BUFFER_AVAILABLE: %s (%d)", __FUNCTION__, strerror(-res), res);
        }
        return;
    }

    //save buffer according to timestamp
    mBufferMap.replaceValueFor(imgBuffer.mTimestamp, imgBuffer);
    MY_LOGD_IF(mLogLevel, "handle=%p, dimensions: (w = %d, h = %d), Stride=%d, usage=%d, PixelFormat=%d, timestamp=%" PRId64 "", 
            imgBuffer.mGraphicBuffer->handle, imgBuffer.mGraphicBuffer->getWidth(), imgBuffer.mGraphicBuffer->getHeight(), 
            imgBuffer.mGraphicBuffer->getStride(), imgBuffer.mGraphicBuffer->getUsage(), 
            imgBuffer.mGraphicBuffer->getPixelFormat(), imgBuffer.mTimestamp );

    //convert to image buffer
    sp<IImageBuffer> imageBuffer;
    convertGraphicBufferToIImageBuffer(imgBuffer.mGraphicBuffer, imageBuffer);
    MY_LOGD_IF(mLogLevel, "[%s]: imageBuffer dimensions: (w = %d, h = %d), format:(0x%x), plane(%zu)", __FUNCTION__, 
            imageBuffer->getImgSize().w, imageBuffer->getImgSize().h, 
            imageBuffer->getImgFormat(), imageBuffer->getPlaneCount()); 
    MY_LOGD_IF(mLogLevel, "[%s]: eImgFmt_JPEG=(0x%x), eImgFmt_BLOB=(0x%x), eImgFmt_RGBA8888=(0x%x), eImgFmt_YV12=(0x%x)", 
            __FUNCTION__, eImgFmt_JPEG, eImgFmt_BLOB, eImgFmt_RGBA8888, eImgFmt_YV12);

    //check sync mode
    sync = getInputsyncMode(idx);
    MY_LOGD_IF(mLogLevel, "[%s]: index=%d, mBufferMap size=%d, sync mode=%d", __FUNCTION__, idx, mBufferMap.size(), int(sync)); 
    

    imageBuffer->lockBuf("addInputFrame", eBUFFER_USAGE_HW_CAMERA_READWRITE | eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_SW_WRITE_OFTEN );
    
    // MBOOL ret = imageBuffer->saveToFile("/sdcard/result.yuv");
    // MY_LOGD_IF(mLogLevel, "[%s] save iamge end ret=%d ", __FUNCTION__, ret);
    
    if (sync)
    {
        if(mInputFrameInfo.indexOfKey(imgBuffer.mTimestamp) < 0)
        {
            MY_LOGD_IF(mLogLevel, "[%s]:Frame parmaeter not ready yet. Save buffer info.(surface index=%d)", __FUNCTION__, idx); 
        }
        else
        {
            sp<FrameInfo> frameInfo = mInputFrameInfo.valueFor(imgBuffer.mTimestamp);
            MY_LOGD_IF(mLogLevel, "[%s]: matched: index=%d, ts=%" PRId64 "", __FUNCTION__, frameInfo->mIndex, 
                                                                    frameInfo->mTimestamp); 
            sp<EffectParameter> parameter = frameInfo->mEffectParam;
            if (parameter == 0)
            {
                parameter = new EffectParameter();
            }
            parameter->setInt64("timestamp", imgBuffer.mTimestamp);
            parameter->set("surfaceId", idx);
            res = mpEffect->addInputFrame(imageBuffer, frameInfo->mEffectParam);
        }
    }
    else
    {
        //call directly
        sp<EffectParameter> parameter= new EffectParameter();
        parameter->setInt64("timestamp", imgBuffer.mTimestamp);
        parameter->set("surfaceId", idx);
        res = mpEffect->addInputFrame(imageBuffer, parameter);
    }
    //imageBuffer->unlockBuf("addInputFrame");
    return;
}


void
EffectHalClient::
acquireBufferManual()
{
    MY_LOGD_IF(mLogLevel, "[%s]: index=%d", __FUNCTION__, availableBufferIdx);
    onBufferFrameAvailable(availableBufferIdx);
    availableBufferIdx = 0;
}


//reference from cpuComsumer_test
void
EffectHalClient::configureANW(const sp<ANativeWindow>& anw,
        const CpuConsumerTestParams& params,
        int maxBufferSlack) {
    status_t err;
    err = native_window_set_buffers_geometry(anw.get(),
            params.width, params.height, params.format);
    MY_LOGD_IF(mLogLevel, "set_buffers_geometry, result: %d", err);

    err = native_window_set_usage(anw.get(),
            GRALLOC_USAGE_SW_WRITE_OFTEN);
    MY_LOGD_IF(mLogLevel, "set_usage, result: %d", err);

    int minUndequeuedBuffers;
    err = anw.get()->query(anw.get(),
            NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS,
            &minUndequeuedBuffers);
    MY_LOGD_IF(mLogLevel, "query, result: %d", err);

    MY_LOGD_IF(mLogLevel, "Setting buffer count to %d",
            maxBufferSlack + 1 + minUndequeuedBuffers);
    err = native_window_set_buffer_count(anw.get(),
            maxBufferSlack + 1 + minUndequeuedBuffers);
    MY_LOGD_IF(mLogLevel, "set_buffer_count, result: %d", err);
}


//reference from cpuComsumer_test
void
EffectHalClient::produceOneFrame(const sp<ANativeWindow>& anw,
        const CpuConsumerTestParams& params,
        int64_t timestamp, uint32_t *stride) {
    status_t err;
    ANativeWindowBuffer* anb;
    MY_LOGD_IF(mLogLevel, "Dequeue buffer from %p", anw.get());
    err = native_window_dequeue_buffer_and_wait(anw.get(), &anb);

    MY_LOGD_IF(mLogLevel, "anb should != NULL, result=%d", int(anb != NULL));

    sp<GraphicBuffer> buf(new GraphicBuffer(anb, false));

    *stride = buf->getStride();
    uint8_t* img = NULL;

    MY_LOGD_IF(mLogLevel, "Lock buffer from %p for write", anw.get());
    err = buf->lock(GRALLOC_USAGE_SW_WRITE_OFTEN, (void**)(&img));

    //do something

    MY_LOGD_IF(mLogLevel, "Unlock buffer from %p", anw.get());
    err = buf->unlock();


    MY_LOGD_IF(mLogLevel, "Set timestamp:%d to %p", timestamp, anw.get());
    err = native_window_set_buffers_timestamp(anw.get(), timestamp);
     MY_LOGD_IF(mLogLevel, "Set timestamp result:%d", err);
    

    MY_LOGD_IF(mLogLevel, "Queue buffer to %p", anw.get());
    err = anw->queueBuffer(anw.get(), buf->getNativeBuffer(), -1);
}


//for test
void
EffectHalClient::
dequeueAndQueueBuf(int64_t timestamp)
{
    status_t err;
    sp<Surface> s = new Surface(mProducer);
    sp<ANativeWindow> anw(s);

    CpuConsumerTestParams params;
    params.height = 1080;
    params.width = 720;
    params.maxLockedBuffers = 5;
    params.format = HAL_PIXEL_FORMAT_Y8;

    int res;
    res = native_window_api_connect(anw.get(), NATIVE_WINDOW_API_CAMERA);
    MY_LOGD_IF(mLogLevel, "connect=%d, native window is: %p", res, anw.get());

    configureANW(anw, params, 5);
    // Produce

    //const int64_t time = 12345678L;
    uint32_t stride;
    produceOneFrame(anw, params, timestamp, &stride);
}


//-----------------------------------------------------------------------------
//API for IEffectListener
//-----------------------------------------------------------------------------
void
EffectHalClient::
onPrepared(const IEffectHalClient* effectClient, const EffectResult& result) const
{
    FUNCTION_LOG_START;
    mEffectListener->onPrepared(effectClient, result);
    FUNCTION_LOG_END;
}

void
EffectHalClient::
onInputFrameProcessed(const IEffectHalClient* effectClient, const sp<EffectParameter> parameter, EffectResult partialResult) const
{
    FUNCTION_LOG_START;
    mEffectListener->onInputFrameProcessed(this, parameter, partialResult);
    if (parameter == 0)
    {
        MY_LOGE("[%s]: EffectParameter is null", __FUNCTION__);
        return;
    }

    int64_t timestamp = parameter->getInt64("timestamp");
    if(mBufferMap.indexOfKey(timestamp) < 0)
    {
        MY_LOGE("[%s]: imgBuffer not found, timestamp=%" PRId64 " ", __FUNCTION__, timestamp);
        return;
    }
    BufferItemConsumer::BufferItem imgBuffer = mBufferMap.valueFor(timestamp);
    sp<BufferItemConsumer> consumer = mInputComsumer[parameter->getInt("surfaceId")];
    MY_LOGD_IF(mLogLevel, "[%s]: surface-id:%d ", __FUNCTION__, parameter->getInt("surfaceId"));
    consumer->releaseBuffer(imgBuffer);
    
    //res = mConsumer->releaseBuffer(bufferItem, releaseFence);
    //mRecordingConsumer->releaseBuffer(imgBuffer);
    FUNCTION_LOG_END;
}


void
EffectHalClient::
onOutputFrameProcessed(const IEffectHalClient* effectClient, const sp<EffectParameter> parameter, EffectResult partialResult)
{
    FUNCTION_LOG_START;
    status_t res;

    
    if (parameter == 0)
    {
        MY_LOGE("[%s]: EffectParameter is null", __FUNCTION__);
        return;
    }

    //get surface index and buffer index
    int idx = parameter->getInt("surfaceId");
    int bufferIndex = parameter->getInt("bufferIndex");
    MY_LOGD_IF(mLogLevel, "[%s]: surface-id:%d bufferIndex:%d", __FUNCTION__, idx, bufferIndex);

    //set timestamp
    sp<ANativeWindow> anw = mOutputSurfaces[idx];
    int64_t timestamp = 123456L;
    MY_LOGD_IF(mLogLevel, "[%s]: Set timestamp=%" PRId64 " to %p", __FUNCTION__, timestamp, anw.get());
    res = native_window_set_buffers_timestamp(anw.get(), timestamp);
    if (res != OK) 
    {
        ALOGE("[%s]: Error setting timestamp: %s (%d)", __FUNCTION__, strerror(-res), res);
        return;
    }

    //for test, convert to image buffer and dump it.
    MY_LOGD_IF(mLogLevel, "[%s]: Queue buffer to %p, GraphicBuffer address:%p, ANativeWindowBuffer address:%p", 
            __FUNCTION__, anw.get(), mOutputBuffers[bufferIndex].get(), mOutputBuffers[bufferIndex]->getNativeBuffer());

    sp<GraphicBuffer> buf = mOutputBuffers[bufferIndex];
    sp<IImageBuffer> imageBuffer;
    convertGraphicBufferToIImageBuffer(buf, imageBuffer);
    MY_LOGD_IF(mLogLevel, "[%s]: ImageBuffer format=(0x%x), dimensions: (w = %d, h = %d)", __FUNCTION__, 
            imageBuffer->getImgFormat(), imageBuffer->getImgSize().w, imageBuffer->getImgSize().h);
    MY_LOGD_IF(mLogLevel, "imageBuffer address=%p, GraphicBuffer address=%p", imageBuffer.get(), buf.get());
    
    //save debug image
    /*imageBuffer->lockBuf( String8::format("%s-%d-%d", __FUNCTION__, idx, bufferIndex), 
        eBUFFER_USAGE_HW_CAMERA_READWRITE | eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_SW_WRITE_OFTEN );

    MBOOL ret = imageBuffer->saveToFile("/sdcard/outputResult.jpg");
    MY_LOGD_IF(mLogLevel, "[%s] save iamge end ret=%d ", __FUNCTION__, ret);
    imageBuffer->unlockBuf(String8::format("%s-%d-%d", __FUNCTION__, idx, bufferIndex));
    */

    //queue buffer to bufferQueue
    res = anw->queueBuffer(anw.get(), mOutputBuffers[bufferIndex]->getNativeBuffer(), -1);
    mOutputBuffers.replaceAt(bufferIndex);
    if (res != OK)
    {
        MY_LOGE("%s: Unable to queue output frame: %s (%d)", __FUNCTION__, strerror(-res), res);
        return;
    }

    //on event
    mEffectListener->onOutputFrameProcessed(this, parameter, partialResult);
    FUNCTION_LOG_END;
}

void
EffectHalClient::
onCompleted(const IEffectHalClient* effectClient, const EffectResult& partialResult, uint64_t uid) const
{
    FUNCTION_LOG_START;
    mEffectListener->onCompleted(effectClient, partialResult, uid);
    FUNCTION_LOG_END;
}

void
EffectHalClient::
onAborted(const IEffectHalClient* effectClient, const EffectResult& result) const
{
    FUNCTION_LOG_START;
    mEffectListener->onAborted(effectClient, result);
    FUNCTION_LOG_END;
}

void
EffectHalClient::
onFailed(const IEffectHalClient* effectClient, const EffectResult& result) const
{
    FUNCTION_LOG_START;
    mEffectListener->onFailed(effectClient, result);
    FUNCTION_LOG_END;
}
