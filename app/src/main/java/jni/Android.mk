LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := main-jni
LOCAL_SRC_FILES := main_jni.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)