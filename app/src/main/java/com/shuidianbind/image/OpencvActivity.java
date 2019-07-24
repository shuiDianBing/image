package com.shuidianbind.image;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class OpencvActivity extends AppCompatActivity {
    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat grayscaleImage,rgbaImage,gMatlin,Matlin;
    private float absoluteFaceSize;
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    //executeCompare();
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opencv);
        previewOpnCv();
        if (OpenCVLoader.initDebug()) {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            //cameraBridgeViewBase.enableView();
        } else {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, loaderCallback);
        }
    }
    private void previewOpnCv(){
        cameraBridgeViewBase = findViewById(R.id.javaCameraView);
        cameraBridgeViewBase.setVisibility(CameraBridgeViewBase.VISIBLE);
        final boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
               /* Mat gray = cvCameraViewFrame.gray();
                Mat rgba = cvCameraViewFrame.rgba();
                if (landscape) {
                    Core.rotate(rgba, rgba, Core.ROTATE_90_COUNTERCLOCKWISE);
                    Core.rotate(gray, gray, Core.ROTATE_90_COUNTERCLOCKWISE);
                    Core.flip(rgba, rgba, 1);
                    Core.flip(gray, gray, 1);
                } else {
                    Core.rotate(rgba, rgba, Core.ROTATE_90_CLOCKWISE);
                    Core.rotate(gray, gray, Core.ROTATE_90_CLOCKWISE);
                }*/
                //Imgproc.resize(rgba,rgba,new Size(cameraBridgeViewBase.getWidth(), cameraBridgeViewBase.getHeight()),0.0D,0.0D,0);// 旋转输入帧（部分手机黑屏无预览）
//                if(null != comparisonRunable && null!= comparisonRunable.origin) {
//                    // 要把Mat对象转换成Bitmap对象，需要创建一个宽高相同的Bitmap对象昨晚参数
//                    Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.RGB_565);
//                    Utils.matToBitmap(rgba, bitmap);// Mat >>> Bitmap
//                    bitmap = Bitmap.createBitmap(bitmap,0,0,comparisonRunable.origin.getWidth(),comparisonRunable.origin.getHeight());
//                    comparisonRunable.setContrast(bitmap);
//                    threadPoolExecutor.execute(comparisonRunable);
//                    // 每隔0.5秒对比一次
//                    handler.sendEmptyMessageDelayed(-1, 500);
//                }
                // 将每一帧的图像展示在界面上,
                //return rgba;
                Mat mRgba = cvCameraViewFrame.rgba();
                Mat mRgbaT = mRgba.t();
                Core.flip(mRgba.t(), mRgbaT, 1);
                Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
                return mRgbaT;
            }
            @Override
            public void onCameraViewStarted(int width, int height) {
                rgbaImage = new Mat(width, height, CvType.CV_8UC4);
                grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
                Matlin = new Mat(width, height, CvType.CV_8UC4);
                gMatlin = new Mat(width, height, CvType.CV_8UC4);
                absoluteFaceSize = height * 0.2f;
            }
            @Override
            public void onCameraViewStopped() {}
        });
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Mat priview(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame){
        grayscaleImage = cvCameraViewFrame.gray();
        rgbaImage = cvCameraViewFrame.rgba();
        int rotation = cameraBridgeViewBase.getDisplay().getRotation();

        //使前置的图像也是正的
       /* if (camera_scene == CAMERA_FRONT) {
            Core.flip(rgbaImage, rgbaImage, 1);
            Core.flip(grayscaleImage, grayscaleImage, 1);
        }

        //MatOfRect faces = new MatOfRect();

        if (rotation == Surface.ROTATION_0) {
            MatOfRect faces = new MatOfRect();
            Core.rotate(grayscaleImage, gMatlin, Core.ROTATE_90_CLOCKWISE);
            Core.rotate(rgbaImage, Matlin, Core.ROTATE_90_CLOCKWISE);
            if (cascadeClassifier != null) {
                cascadeClassifier.detectMultiScale(gMatlin, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            Rect[] faceArray = faces.toArray();
            for (int i = 0; i < faceArray.length; i++)
                Imgproc.rectangle(Matlin, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 2);
            Core.rotate(Matlin, rgbaImage, Core.ROTATE_90_COUNTERCLOCKWISE);

        } else {
            MatOfRect faces = new MatOfRect();
            if (cascadeClassifier != null) {
                cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            Rect[] faceArray = faces.toArray();
            for (int i = 0; i < faceArray.length; i++)
                Imgproc.rectangle(rgbaImage, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 2);
        }*/
        return rgbaImage;
    }
}
