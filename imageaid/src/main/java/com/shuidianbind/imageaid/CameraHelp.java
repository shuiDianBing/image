package com.shuidianbind.imageaid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraHelp {
    // 相机默认宽高，相机的宽度和高度跟屏幕坐标不一样，手机屏幕的宽度和高度是反过来的。
    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;
    public static final int DESIRED_PREVIEW_FPS = 30;

    private static int mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private static Camera mCamera;
    private static int mCameraPreviewFps;
    private static int mOrientation = 0;
    private static boolean open;

    /**
     * 打开相机，默认打开前置相机
     * @param expectFps
     */
    public static void openFrontalCamera(int expectFps) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized!");
        }
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        int numCameras = Camera.getNumberOfCameras();
//        for (int i = 0; i < numCameras; i++) {
//            Camera.getCameraInfo(i, info);
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                mCamera = Camera.open(i);
//                mCameraID = info.facing;
//                break;
//            }
//        }
        // 如果没有前置摄像头，则打开默认的后置摄像头
        if (mCamera == null) {
            mCamera = Camera.open();
            mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
            open = true;
        }
        // 没有摄像头时，抛出异常
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parameters = mCamera.getParameters();
        mCameraPreviewFps = chooseFixedPreviewFps(parameters, expectFps * 1000);
        parameters.setRecordingHint(true);
        //parameters.setZoom(parameters.getMaxZoom());
        mCamera.setParameters(parameters);
        setPreviewSize(mCamera, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setPictureSize(mCamera, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        mCamera.setDisplayOrientation(mOrientation);
        //mCamera.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    }

    /**
     * 根据ID打开相机
     * @param cameraID
     * @param expectFps
     */
    public static void openCamera(int cameraID, int expectFps) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized!");
        }
        mCamera = Camera.open(cameraID);
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        mCameraID = cameraID;
        Camera.Parameters parameters = mCamera.getParameters();
        mCameraPreviewFps = chooseFixedPreviewFps(parameters, expectFps * 1000);
        parameters.setRecordingHint(true);
        //parameters.setZoom(parameters.getMaxZoom());
        mCamera.setParameters(parameters);
        setPreviewSize(mCamera, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setPictureSize(mCamera, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        mCamera.setDisplayOrientation(mOrientation);
    }

    /**
     * 开始预览
     * @param holder
     */
    public static void startPreviewDisplay(SurfaceHolder holder) {
        if (mCamera == null) {
            throw new IllegalStateException("Camera must be set when start preview");
        }
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                        Camera.Parameters parameters = camera.getParameters();
                        if (!Build.MODEL.equals("KORIDY H30"))
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
                        else
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        camera.setParameters(parameters);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换相机
     * @param cameraID
     */
    public static void switchCamera(int cameraID, SurfaceHolder holder) {
        if (mCameraID == cameraID) {
            return;
        }
        mCameraID = cameraID;
        // 释放原来的相机
        releaseCamera();
        // 打开相机
        openCamera(cameraID, DESIRED_PREVIEW_FPS);
        // 打开预览
        startPreviewDisplay(holder);
    }

    /**
     * 释放相机
     */
    public static void releaseCamera() {
        open = false;
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 开始预览
     */
    public static void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    /**
     * 停止预览
     */
    public static void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
    }

    /**
     * 拍照
     */
    public static void takePicture(Camera.ShutterCallback shutterCallback,
                                   Camera.PictureCallback rawCallback,
                                   Camera.PictureCallback pictureCallback) {
        if (mCamera != null) {
            mCamera.takePicture(shutterCallback, rawCallback, pictureCallback);
        }
    }

    /**
     * 设置预览大小
     * @param camera
     * @param expectWidth
     * @param expectHeight
     */
    public static void setPreviewSize(Camera camera, int expectWidth, int expectHeight) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = calculatePerfectSize(parameters.getSupportedPreviewSizes(),
                expectWidth, expectHeight);
        parameters.setPreviewSize(size.width, size.height);
        camera.setParameters(parameters);
    }

    /**
     * 获取预览大小
     * @return
     */
    public static Camera.Size getPreviewSize() {
        if (mCamera != null) {
            return mCamera.getParameters().getPreviewSize();
        }
        return null;
    }

    /**
     * 设置拍摄的照片大小
     * @param camera
     * @param expectWidth
     * @param expectHeight
     */
    public static void setPictureSize(Camera camera, int expectWidth, int expectHeight) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = calculatePerfectSize(parameters.getSupportedPictureSizes(),
                expectWidth, expectHeight);
        parameters.setPictureSize(size.width, size.height);
        camera.setParameters(parameters);
    }

    /**
     * 获取照片大小
     * @return
     */
    public static Camera.Size getPictureSize() {
        if (mCamera != null) {
            return mCamera.getParameters().getPictureSize();
        }
        return null;
    }

    /**
     * 计算最完美的Size
     * @param sizes
     * @param expectWidth
     * @param expectHeight
     * @return
     */
    public static Camera.Size calculatePerfectSize(List<Camera.Size> sizes, int expectWidth, int expectHeight) {
        sortList(sizes); // 根据宽度进行排序
        Camera.Size result = sizes.get(0);
        boolean widthOrHeight = false; // 判断存在宽或高相等的Size
        // 辗转计算宽高最接近的值
        for (Camera.Size size: sizes) {
            // 如果宽高相等，则直接返回
            if (size.width == expectWidth && size.height == expectHeight) {
                result = size;
                break;
            }
            // 仅仅是宽度相等，计算高度最接近的size
            if (size.width == expectWidth) {
                widthOrHeight = true;
                if (Math.abs(result.height - expectHeight)
                        > Math.abs(size.height - expectHeight)) {
                    result = size;
                }
            }
            // 高度相等，则计算宽度最接近的Size
            else if (size.height == expectHeight) {
                widthOrHeight = true;
                if (Math.abs(result.width - expectWidth)
                        > Math.abs(size.width - expectWidth)) {
                    result = size;
                }
            }
            // 如果之前的查找不存在宽或高相等的情况，则计算宽度和高度都最接近的期望值的Size
            else if (!widthOrHeight) {
                if (Math.abs(result.width - expectWidth)
                        > Math.abs(size.width - expectWidth)
                        && Math.abs(result.height - expectHeight)
                        > Math.abs(size.height - expectHeight)) {
                    result = size;
                }
            }
        }
        return result;
    }

    /**
     * 排序
     * @param list
     */
    private static void sortList(List<Camera.Size> list) {
        Collections.sort(list, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size pre, Camera.Size after) {
                if (pre.width > after.width) {
                    return 1;
                } else if (pre.width < after.width) {
                    return -1;
                }
                return 0;
            }
        });
    }

    /**
     * 选择合适的FPS
     * @param parameters
     * @param expectedThoudandFps 期望的FPS
     * @return
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parameters, int expectedThoudandFps) {
        List<int[]> supportedFps = parameters.getSupportedPreviewFpsRange();
        for (int[] entry : supportedFps) {
            if (entry[0] == entry[1] && entry[0] == expectedThoudandFps) {
                parameters.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }
        int[] temp = new int[2];
        int guess;
        parameters.getPreviewFpsRange(temp);
        if (temp[0] == temp[1]) {
            guess = temp[0];
        } else {
            guess = temp[1] / 2;
        }
        return guess;
    }

    /**
     * 设置预览角度，setDisplayOrientation本身只能改变预览的角度
     * previewFrameCallback以及拍摄出来的照片是不会发生改变的，拍摄出来的照片角度依旧不正常的
     * 拍摄的照片需要自行处理
     * 这里Nexus5X的相机简直没法吐槽，后置摄像头倒置了，切换摄像头之后就出现问题了。
     * @param activity
     */
    public static int calculateCameraPreviewOrientation(Activity activity) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        mOrientation = result;
        return result;
    }
    /**
     * 安卓摄像头 data 转bitmap << https://blog.csdn.net/lsw8569013/article/details/78882156
     * @param bytes 图像流数据
     * @param camera 相机
     * @param widthScale 裁剪宽度比例
     * @param aspectRatio 宽高比例
     * @param mIsFrontalCamera
     * @return
     */
    public static Bitmap getBitMap(byte[] bytes, Camera camera,float widthScale,float aspectRatio, boolean mIsFrontalCamera) {
        int cameraWidth = camera.getParameters().getPreviewSize().width;
        int cameraHeight = camera.getParameters().getPreviewSize().height;
        YuvImage yuvImage = new YuvImage(bytes,camera.getParameters().getPreviewFormat(),cameraWidth,cameraHeight,null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, cameraWidth, cameraHeight), 80, byteArrayOutputStream);
        byte[] jpegData = byteArrayOutputStream.toByteArray();
        // 获取照相后的bitmap
        Bitmap tmpBitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.postScale(1,1);
        if(mIsFrontalCamera)
            matrix.setRotate(-90);
        else
            matrix.setRotate(90);
        tmpBitmap = Bitmap.createBitmap(tmpBitmap,0,0, cameraWidth,cameraHeight,matrix,true);
        float width = tmpBitmap.getWidth()* widthScale;
        float height = width * aspectRatio;
        float startX = (tmpBitmap.getWidth()- width)/2;
        float startY = (tmpBitmap.getHeight()- height)/2;
        tmpBitmap = Bitmap.createBitmap(tmpBitmap,(int)startX,(int)startY, (int)width,(int)height);
        tmpBitmap = tmpBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int w = tmpBitmap.getWidth();
        int h = tmpBitmap.getHeight();
        int hight = tmpBitmap.getHeight()> tmpBitmap.getWidth() ? tmpBitmap.getHeight(): tmpBitmap.getWidth();
        //float scale = hight / 800.0f;
        //if (scale > 1)
        //    tmpBitmap = Bitmap.createScaledBitmap(tmpBitmap,(int)(tmpBitmap.getWidth()/ scale),(int)(tmpBitmap.getHeight()/ scale), false);
        return tmpBitmap;
    }

    /**
     *
     * @param activity
     * @param data
     * @param camera
     * @param width
     * @param height
     * @return
     */
    public static Bitmap adjustment(Activity activity,byte[] data,Camera camera,int width,int height){
        final int[] xy = Screen.getScreenScale(activity);
        height *= width/(float)xy[0];
        Bitmap bitmap = getBitMap(data, camera, width/(float)xy[0], height/(float)width,false);
        return bitmap;
    }
    /**
     * 获取当前的Camera ID
     * @return
     */
    public static int getCameraID() {
        return mCameraID;
    }

    /**
     * 获取当前预览的角度
     * @return
     */
    public static int getPreviewOrientation() {
        return mOrientation;
    }

    /**
     * 获取FPS（千秒值）
     * @return
     */
    public static int getCameraPreviewThousandFps() {
        return mCameraPreviewFps;
    }

    public static void setOneShotPreviewCallback(Camera.PreviewCallback previewCallback) {
        if(null != mCamera && open)
            mCamera.setOneShotPreviewCallback(previewCallback);
    }
}
