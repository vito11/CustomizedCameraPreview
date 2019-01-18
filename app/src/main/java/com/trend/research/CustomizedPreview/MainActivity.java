package com.trend.research.CustomizedPreview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity{
    private CameraPreview mPreview;
    private static final int MY_PERMISSIONS_REQUEST_CALL_CAMERA = 1;//请求码，自己定义

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPreview = (CameraPreview)findViewById(R.id.camera_preview);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //如果没有授权，则请求授权
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CALL_CAMERA);
        } else {
            mPreview.startCamera();
        }

        if (!checkCameraHardware(this)) {
            Toast.makeText(MainActivity.this, "相机不支持", Toast.LENGTH_SHORT)
                    .show();
        } else {

        }
    }
    protected void onDestroy()
    {
        super.onDestroy();
        mPreview.stopCamera();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //判断请求码
        if (requestCode == MY_PERMISSIONS_REQUEST_CALL_CAMERA) {
            //grantResults授权结果
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPreview.startCamera();
                //成功，开启摄像头
            } else {
                //授权失败
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 判断相机是否支持
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    public static void nativeEvent(String s) {
        Log.d("camera",  "get msg from c++ level:" +s);
    }
}
