package com.shuidianbind.image;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class OpencvActivity extends AppCompatActivity {
    private CameraBridgeViewBase cameraBridgeViewBase;
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
            //loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            cameraBridgeViewBase.enableView();
        } else {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, loaderCallback);
        }
    }
    private void previewOpnCv(){
        cameraBridgeViewBase = findViewById(R.id.javaCameraView);
        cameraBridgeViewBase.setVisibility(CameraBridgeViewBase.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
                Mat gray = cvCameraViewFrame.gray();
                Mat rgba = cvCameraViewFrame.rgba();
                if (false) {// 旋转输入帧
                    Core.rotate(rgba, rgba, Core.ROTATE_90_COUNTERCLOCKWISE);
                    Core.rotate(gray, gray, Core.ROTATE_90_COUNTERCLOCKWISE);
                    Core.flip(rgba, rgba, 1);
                    Core.flip(gray, gray, 1);
                } else {
                    Core.rotate(rgba, rgba, Core.ROTATE_90_CLOCKWISE);
                    Core.rotate(gray, gray, Core.ROTATE_90_CLOCKWISE);
                }
                //Imgproc.resize(rgba,rgba,new Size(cameraBridgeViewBase.getWidth(), cameraBridgeViewBase.getHeight()),0.0D,0.0D,0);
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
                return rgba;
            }
            @Override
            public void onCameraViewStarted(int i, int i1) {}
            @Override
            public void onCameraViewStopped() {}
        });
    }
}
