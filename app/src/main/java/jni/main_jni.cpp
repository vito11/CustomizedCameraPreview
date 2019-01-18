//
// Created by vito_li on 2017/3/7.
//

#include <iostream>
#include "main_jni.h"
#include "camera_log.h"


int call_java_method(JNIEnv *env);

JNIEXPORT jint JNICALL Java_com_trend_research_CustomizedPreview_CameraPreview_nativeCameraDataHandler(JNIEnv *env, jobject instance, jbyteArray luma,jbyteArray chroma,jint width, jint height ){
    unsigned char* y= NULL;
    unsigned char* uv= NULL;

    y= (unsigned char*)env->GetDirectBufferAddress(luma);
    uv= (unsigned char*)env->GetDirectBufferAddress(chroma);

    if(y != NULL && uv!=NULL)
    {
       LOGI("get a frame, width:%d, height:%d",width,height);
    }
    //do your customized change to the yuv format image here
    call_java_method(env);

    return 1;

}

int call_java_method(JNIEnv *env)
{

  jmethodID nativeEvent = NULL;
  jclass mainActivity = NULL;

   mainActivity = env->FindClass("com/trend/research/CustomizedPreview/MainActivity");
   if(mainActivity == NULL){
      LOGE("get mainActivity class error");
      return -1;
   }


    nativeEvent = env->GetStaticMethodID(mainActivity, "nativeEvent","(Ljava/lang/String;)V");
    if (nativeEvent == NULL) {
         LOGE("get nativeEvent method error");
         return -2;
    }

    jstring jstrMSG = NULL;
    jstrMSG = env->NewStringUTF("Hi,I'm From C");
    env->CallStaticVoidMethod(mainActivity, nativeEvent,jstrMSG);
    env->DeleteLocalRef(jstrMSG);
    return 1;
}