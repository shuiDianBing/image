package com.shuidianbind.image;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.shuidianbind.image.opencvAuxiliary.ColorBlobDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.CascadeClassifier;

import java.util.List;

/**
 * https://docs.opencv.org/java/2.4.7/org/opencv/android/CameraBridgeViewBase.html
 */
public class OpencvActivity extends AppCompatActivity implements View.OnTouchListener{
    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat mRgba,mRgbaF,mRgbaT;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage,rgbaImage,gMatlin,Matlin;
    private float absoluteFaceSize;
    private boolean mIsColorSelected = false;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_opencv);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setBackgroundColor(getResources().getColor(R.color.lotine_background));
        if(Build.VERSION.SDK_INT>=23){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
        //if (Build.VERSION.SDK_INT >= 21)
       //     getWindow().setStatusBarColor(getResources().getColor(R.color.lotine_background));
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
                Mat gray = cvCameraViewFrame.gray();
                Mat rgba = cvCameraViewFrame.rgba();
                if (landscape) {
                    Core.rotate(rgba, rgba, Core.ROTATE_90_COUNTERCLOCKWISE);
                    Core.rotate(gray, gray, Core.ROTATE_90_COUNTERCLOCKWISE);
                    Core.flip(rgba, rgba, 1);
                    Core.flip(gray, gray, 1);
                } else {
                    Core.rotate(rgba, rgba, Core.ROTATE_90_CLOCKWISE);
                    Core.rotate(gray, gray, Core.ROTATE_90_CLOCKWISE);
                }
                //Imgproc.resize(rgba,rgba,new Size(cameraBridgeViewBase.getWidth(), cameraBridgeViewBase.getHeight()),0.0D,0.0D,0);// 旋转输入帧,部分手机黑屏无预览(宽高和预览取得宽高不一样造成)
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
                /*Mat mRgba = cvCameraViewFrame.rgba();
                Mat mRgbaT = mRgba.t();
                Core.flip(mRgba.t(), mRgbaT, 1);
                Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
                return mRgbaT;*/
                /*mRgba = cvCameraViewFrame.rgba();
                Object localObject;

                Core.transpose(mRgba, mRgbaT);
                Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0.0D, 0.0D, 0);
                Core.flip(mRgbaF, mRgba, 1);

                if (mIsColorSelected) {
                    mDetector.process(mRgba);
                    List<MatOfPoint> contours = mDetector.getContours();
                    Log.v(OpencvActivity.class.getSimpleName(), "Contours count: " + contours.size());
                    Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

                    localObject = new Message();
                    Bundle localBundle = new Bundle();

                    if (contours.size() != 0) {
                        Log.e("coutonrs.size!=0", "!=1");
                        Moments localMoments = Imgproc.moments((Mat) contours.get(0), false);
                        int i = (int) (localMoments.get_m10() / localMoments.get_m00());
                        int j = (int) (localMoments.get_m01() / localMoments.get_m00());
                        int k = (int) ((MatOfPoint) contours.get(0)).size().area();
                        localBundle.putInt("x", j);
                        localBundle.putInt("y", i);
                        localBundle.putInt("size", k);
                        Log.e("x:", i + "y:" + j + "size:" + k);
                        ((Message) localObject).what = 0;
                        ((Message) localObject).setData(localBundle);
                        //Mat colorLabel = mRgba.submat(0, 38, 0, 800); //间距为0，写38行，800列
                        //colorLabel.setTo(mBlobColorRgba);

                        //Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
                        // mSpectrum.copyTo(spectrumLabel);
                    }
                }
                    return mRgba;*/
                //return cutVerticalScreen(cvCameraViewFrame);
            }
            @Override
            public void onCameraViewStarted(int width, int height) {
                /*mRgba = new Mat(height, width, CvType.CV_8UC4);
                mDetector = new ColorBlobDetector();
                mSpectrum = new Mat();
                mBlobColorRgba = new Scalar(255);
                mBlobColorHsv = new Scalar(255);
                SPECTRUM_SIZE = new Size(200, 64);
                CONTOUR_COLOR = new Scalar(255,0,0,255);
                mRgbaF = new Mat(height, width, CvType.CV_8UC4);
                mRgbaT = new Mat(height, width, CvType.CV_8UC4);*/

                /*rgbaImage = new Mat(width, height, CvType.CV_8UC4);
                grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
                Matlin = new Mat(width, height, CvType.CV_8UC4);
                gMatlin = new Mat(width, height, CvType.CV_8UC4);

                absoluteFaceSize = height * 0.2f;*/
            }
            @Override
            public void onCameraViewStopped() {
                //mRgba.release();
            }
        });
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Mat cutVerticalScreen(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame){
        grayscaleImage = cvCameraViewFrame.gray();
        rgbaImage = cvCameraViewFrame.rgba();
        int rotation = cameraBridgeViewBase.getDisplay().getRotation();

        //使前置的图像也是正的
//                if (camera_scene == CAMERA_FRONT) {
//                    Core.flip(rgbaImage, rgbaImage, 1);
//                    Core.flip(grayscaleImage, grayscaleImage, 1);
//                }

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
        }

        return rgbaImage;
    }
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        // int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        //int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)(event.getX()/cameraBridgeViewBase.getWidth()*cols);
        int y = (int)(event.getY()/cameraBridgeViewBase.getHeight()*rows);

        Log.i(getClass().getSimpleName(), "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setBackgroundColor(Color.rgb((int) this.mBlobColorRgba.val[0], (int) this.mBlobColorRgba.val[1], (int) this.mBlobColorRgba.val[2]));
        if (Build.VERSION.SDK_INT >= 21)
            getWindow().setStatusBarColor(Color.rgb((int)this.mBlobColorRgba.val[0], (int)this.mBlobColorRgba.val[1], (int) this.mBlobColorRgba.val[2]));


        Log.e(getClass().getSimpleName(), "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();
        return false; // don't need subsequent touch events
    }
    @Override
    public void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()){
            Log.d(getClass().getSimpleName(), "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback);
        }else {
            Log.d(getClass().getSimpleName(), "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    public void onDestroy(){
        super.onDestroy();
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }
}
