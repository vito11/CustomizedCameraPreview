//
// Created by vito_li on 2017/3/7.
//

#ifndef _MAIN_JNI_H
#define _MAIN_JNI_H


#include <string.h>
#include <jni.h>


 #ifdef __cplusplus
 extern "C" {
 #endif

JNIEXPORT jint JNICALL
Java_com_trend_research_CustomizedPreview_CameraPreview_nativeCameraDataHandler(JNIEnv *env, jobject instance, jbyteArray luma,jbyteArray chroma,jint width, jint height );


 #ifdef __cplusplus
 }
#endif

#endif //_MAIN_JNI_H
