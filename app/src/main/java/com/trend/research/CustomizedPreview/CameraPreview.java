package com.trend.research.CustomizedPreview;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by vito_li on 2017/2/28.
 */
public class CameraPreview extends GLSurfaceView implements Camera.PreviewCallback,GLSurfaceView.Renderer {

    private Camera mCamera;

    private int previewWidth,previewHeight,bufferSize,previewSize;
    public byte gBuffer[];
    private final Object mRenderLocker=new Object();
    private boolean mRenderDirty =false;
    public ByteBuffer  y;
    public ByteBuffer  uv;
    boolean surfaceCreated = false;
    long start;

    final float TEXTURE_NO_ROTATION[] = { 0.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, };
    final float TEXTURE_ROTATED_90[] = { 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, };
    final float TEXTURE_ROTATED_180[] = { 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f, };
    final float TEXTURE_ROTATED_270[] = { 0.0f, 0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, };

    float textureCoordinate[] = TEXTURE_NO_ROTATION;

    float vertices[] = new float[] { -1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f };

    /*float vertices[] = {
            -1.0f,  -1.0f,
            1.0f,  -1.0f,
            -1.0f,   1.0f,
            1.0f,   1.0f,
    };*/
    float coordVertices[] = {
            0.0f,   1.0f,   // Top left
            1.0f,   1.0f,   // Top right
            0.0f,   0.0f,   // Bottom left
            1.0f,   0.0f,   // Bottom right
    };

    private final float[] mVerticesData = { -1.f, 1.f, 0.0f, // Position 0
            0.0f, 0.0f, // TexCoord 0
            -1.f, -1.f, 0.0f, // Position 1
            0.0f, 1.0f, // TexCoord 1
            1.f, -1.f, 0.0f, // Position 2
            1.0f, 1.0f, // TexCoord 2
            1.f, 1.f, 0.0f, // Position 3
            1.0f, 0.0f // TexCoord 3
    };

    private final short[] mIndicesData = { 0, 1, 2, 0, 2, 3 };

    FloatBuffer mVerticesBuffer;
    FloatBuffer mTextureCoordinateBuffer;

    private int mPositionHandler;
    private int mTextureCoordinateHandler;
    private int mGLUniformTexture;
    private int mGLUniformUTexture;

    private int mProgramHandler;

    private int mTextureId;
    private int mUTextureId;
    private int mVTextureId;

    private boolean capture = false;
    SurfaceTexture gSurfaceTexture;
    Context mContext;
    private FloatBuffer mVertices;
    private FloatBuffer mIndices;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

    }
    public void stopCamera()
    {
        mCamera.stopPreview();
        mCamera.release();
    }
    //相机参数的初始化设置
    public void startCamera()
    {
        // calls to this canvas

        gSurfaceTexture= new SurfaceTexture(10);
        mCamera = Camera.open();

        Camera.Parameters parameters=mCamera.getParameters();

        List<Camera.Size> preSize = parameters.getSupportedPreviewSizes();
        previewWidth = preSize.get(0).width;
        previewHeight = preSize.get(0).height;
        parameters.setPreviewSize(previewWidth, previewHeight);

        previewSize = previewWidth * previewHeight;

        y = ByteBuffer.allocateDirect(previewSize);
        uv =ByteBuffer.allocateDirect(previewSize/2);

        bufferSize  = previewSize * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
        gBuffer = new byte[bufferSize];

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);//1连续对焦

        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewTexture(gSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.addCallbackBuffer(gBuffer);
        mCamera.setPreviewCallbackWithBuffer(this);
        mCamera.startPreview();
    }
    public native int nativeCameraDataHandler(ByteBuffer luma, ByteBuffer Chroma, int width, int height);
    static {
        System.loadLibrary("main-jni");
    }
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //long t1 = System.currentTimeMillis();
        camera.addCallbackBuffer(gBuffer);

        if(capture) {
            Camera.Parameters parameters = camera.getParameters();
            int format = parameters.getPreviewFormat();
            //YUV formats require more conversion
            if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {
                int w = parameters.getPreviewSize().width;
                int h = parameters.getPreviewSize().height;
                // Get the YuV image
                YuvImage yuv_image = new YuvImage(data, format, w, h, null);
                // Convert YuV to Jpeg
                Rect rect = new Rect(0, 0, w, h);
                ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
                yuv_image.compressToJpeg(rect, 100, output_stream);
                byte[] byt = output_stream.toByteArray();
                FileOutputStream outStream = null;
                try {
                    // Write to SD Card
                    File file = new File("/mnt/sdcard/data/Image"+".jpg");
                    //Uri uriSavedImage = Uri.fromFile(file);
                    outStream = new FileOutputStream(file);
                    outStream.write(byt);
                    outStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                }
            }
            capture = false;
        }

        synchronized (mRenderLocker) {
            if(surfaceCreated) {
                mRenderDirty =true;
                decodeYUV2(data);
                int result = nativeCameraDataHandler(y,uv,previewWidth,previewHeight);
                if(result == 1) {
                    requestRender();
                }
            }
        }
        /*long t2 = System.currentTimeMillis();
        System.out.println("the time is: " + (t2 - t1));*/
    }
    public void decodeYUV2(byte[] data){
        //提取Y分量
        y.put(data, 0, previewSize);
        y.position(0);
        uv.put(data, previewSize, previewSize/2);
        uv.position(0);
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition
     * .khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)
     */
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        mVerticesBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mVertices = mVerticesBuffer.put(vertices);

        mTextureCoordinateBuffer = ByteBuffer
                .allocateDirect(textureCoordinate.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mIndices = mTextureCoordinateBuffer.put(textureCoordinate);


        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        int textures[] = new int[2];
        GLES20.glGenTextures(2, textures, 0);

        mTextureId = textures[0];

        mUTextureId = textures[1];


        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (vertexShaderHandle != 0) {
            // Load the shader
            GLES20.glShaderSource(vertexShaderHandle,
                    loadRawString(mContext, R.raw.filter_vs));
            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);
            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS,
                    compileStatus, 0);
            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.d("OpenGLES",
                        "Compilation:"
                                + GLES20.glGetShaderInfoLog(vertexShaderHandle));
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }

        if (vertexShaderHandle == 0) {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader shader.
        int fragmentShaderHandle = GLES20
                .glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (fragmentShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle,
                    loadRawString(mContext, R.raw.filter_fs));
            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);
            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle,
                    GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.d("OpenGLES",
                        "Compilation:"
                                + GLES20.glGetShaderInfoLog(fragmentShaderHandle));
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }

        if (fragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "position");
            // GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
            GLES20.glBindAttribLocation(programHandle, 1,
                    "inputTextureCoordinate");
            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);
            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS,
                    linkStatus, 0);
            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        mPositionHandler = GLES20
                .glGetAttribLocation(programHandle, "position");
        mTextureCoordinateHandler = GLES20.glGetAttribLocation(programHandle,
                "inputTextureCoordinate");
        mGLUniformTexture = GLES20.glGetUniformLocation(programHandle,
                "inputImageTexture");
        mGLUniformUTexture = GLES20.glGetUniformLocation(programHandle,
                "uvTexture");

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(programHandle);
        mProgramHandler = programHandle;

        surfaceCreated =true;
        Log.d("OpenGLES", "Init success.");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.
     * khronos.opengles.GL10)
     */
    public void onDrawFrame(GL10 gl) {
        synchronized (mRenderLocker) {

            if (surfaceCreated && mRenderDirty) {
                long start = System.currentTimeMillis();

                GLES20.glUseProgram(mProgramHandler);
                // Clears the screen and depth buffer.
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
                // Draw our scene.

                mVertices.position(0);
                mIndices.position(0);

                GLES20.glVertexAttribPointer(mPositionHandler, 3, GLES20.GL_FLOAT,
                        false, 0, mVerticesBuffer);
                GLES20.glEnableVertexAttribArray(mPositionHandler);
                GLES20.glVertexAttribPointer(mTextureCoordinateHandler, 2,
                        GLES20.GL_FLOAT, false, 0, mTextureCoordinateBuffer);
                GLES20.glEnableVertexAttribArray(mTextureCoordinateHandler);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
                GLES20.glUniform1i(mGLUniformTexture, 0);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                        previewWidth, previewHeight, 0, GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE, y);

                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mUTextureId);
                GLES20.glUniform1i(mGLUniformUTexture, 1);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                        previewWidth / 2, previewHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA,
                        GLES20.GL_UNSIGNED_BYTE, uv);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);


                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                GLES20.glFinish();
                GLES20.glDisableVertexAttribArray(mPositionHandler);
                GLES20.glDisableVertexAttribArray(mTextureCoordinateHandler);

                long end = System.currentTimeMillis();

                if(start != 0)
                {
                    //System.out.println("the time is: " + (end - start));
                }

                start = end;
                mRenderDirty = false;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition
     * .khronos.opengles.GL10, int, int)
     */
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public  String loadRawString(Context context, int rawId) {
        InputStream is = context.getResources().openRawResource(rawId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[1024];
            int len = -1;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toString();
    }
}

