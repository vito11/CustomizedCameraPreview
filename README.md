# CustomizedCameraPreview
This is an Android Customized camera preview app support image processing in native level. 

This project use opengl 2.0 to render every image to the screen, and you can do any changes to the images in C++/C 

# How to try
Just import the whole project into android studio and build an apk to install.

If you want to make any changes to preview, check the main_jni.cpp file

```C++
JNIEXPORT jint JNICALL Java_com_trend_research_CustomizedPreview_CameraPreview_nativeCameraDataHandler(JNIEnv *env, jobject instance, jbyteArray luma,jbyteArray chroma,jint width, jint height ){
    ...
    
    if(y != NULL && uv!=NULL)
    {
       LOGI("get a frame, width:%d, height:%d",width,height);
    }
    //do your customized changes to the yuv format image here
    call_java_method(env);

    ...

}
```
If you have any question about this project, feel free to contact me.

Email: limm.hq@gmail.com
