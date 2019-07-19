package com.shuidianbind.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.shuidianbind.image.widget.CameraSurfaceView;
import com.shuidianbind.imageaid.CameraHelp;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * OpenCV3.4.1快速集成到Android studio中，10分钟搞定 << https://blog.csdn.net/yu540135101/article/details/82593860
 * Android OpenCv进行图片比对 << https://blog.csdn.net/u012808234/article/details/50594421
 * opencv3.0.0 for android .判断两张图片是否一致 << https://blog.csdn.net/cf125313/article/details/49758765
 */
public class OpenCvSimilarityActivity extends AppCompatActivity {
    private CameraSurfaceView cameraSurfaceView;
    private HandlerThread thread;
    private Handler handler;
    private ThreadPoolExecutor threadPoolExecutor;
    private ComparisonRunable comparisonRunable;
    private Camera.PreviewCallback previewCallback;
    private final Object LOCK = new Object();
    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    break;
                case -1:
                    cameraSurfaceView.setOneShotPreviewCallback(previewCallback);
                    break;
            }
            return true;
        }
    };
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    start(BitmapFactory.decodeResource(getResources(), R.mipmap.image_jiezishu));
                    break;

                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preview();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // 通过OpenCV引擎服务加载并初始化OpenCV类库，所谓OpenCV引擎服务即是
        // OpenCV_2.4.9.2_Manager_2.4_*.apk程序包，存在于OpenCV安装包的apk目录中
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
                    loaderCallback);
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    private void preview() {
        final ImageView imageBalloon = findViewById(R.id.imageBalloon3);
        final View view = findViewById(R.id.window);
        int rotation = CameraHelp.calculateCameraPreviewOrientation(this);
        (cameraSurfaceView = findViewById(R.id.cameraSurface)).setOneShotPreviewCallback(previewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //android摄像头开发,将Camera.onPreviewFrame里面的data转换成bitmap << https://blog.csdn.net/qiguangyaolove/article/details/53130061
//                Camera.Size previewSize = camera.getParameters().getPreviewSize();//获取尺寸,格式转换的时候要用到
//                BitmapFactory.Options newOpts = new BitmapFactory.Options();
//                newOpts.inJustDecodeBounds = true;
//                YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
//                byte[] rawImage = baos.toByteArray();
//                //将rawImage转换成bitmap
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inPreferredConfig = Bitmap.Config.RGB_565;
//                Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
                if (null != comparisonRunable && null != comparisonRunable.origin) {
                    Bitmap bitmap = CameraHelp.getBitMap(data, camera,
                            comparisonRunable.origin.getWidth(), comparisonRunable.origin.getHeight(),false);
                    comparisonRunable.setContrast(bitmap);
                    threadPoolExecutor.execute(comparisonRunable);
                    Glide.with(imageBalloon.getContext()).load(bitmap).into(imageBalloon);
                }
            }
        });
    }
    public void start(Bitmap bitmap) {
        thread = new HandlerThread(getClass().getName());
        thread.start();
        handler = new Handler(thread.getLooper(), callback);
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        comparisonRunable = new ComparisonRunable(this, bitmap);
    }
    public class ComparisonRunable implements Runnable {
        private WeakReference<AppCompatActivity> weakReference;
        private Bitmap origin, contrast;
        private String fingerprint;
        public ComparisonRunable(AppCompatActivity appCompatActivity, Bitmap origin) {
            weakReference = new WeakReference<AppCompatActivity>(appCompatActivity);
            this.origin = origin;
        }
        public void setContrast(Bitmap bitmap) {
            contrast = bitmap;
        }
        @Override
        public void run() {
            Log.d("当前线程：", Thread.currentThread().getName());
            Mat mat1 = new Mat();
            Mat mat2 = new Mat();
            Mat mat11 = new Mat();
            Mat mat22 = new Mat();
            Utils.bitmapToMat(origin, mat1);
            Utils.bitmapToMat(contrast, mat2);
            Imgproc.cvtColor(mat1, mat11, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(mat2, mat22, Imgproc.COLOR_BGR2GRAY);
            double similarity = comPareHist(mat11, mat22);//SimilarPicture.similarity(origin,contrast,8,8);
            synchronized (LOCK) {
                handler.obtainMessage(0.7 < similarity ? 0 : -1, contrast).sendToTarget();
            }
        }
        private Mat                  mMat0;
        private MatOfInt             mChannels[];
        private MatOfInt             mHistSize;
        private int                  mHistSizeNum = 25;
        private MatOfFloat           mRanges;
        private Scalar               mColorsRGB[];
        private Point                mP1;
        private Point                mP2;
        private float                mBuff[];
        public Mat procSrc2GrayJni(Mat srcMat,int type) {
            Mat grayMat = new Mat();
            Imgproc.cvtColor(srcMat, grayMat, type);//转换为灰度图
            // Imgproc.HoughCircles(rgbMat, gray,Imgproc.CV_HOUGH_GRADIENT, 1, 18);
            // //霍夫变换找园
            mChannels = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
            mBuff = new float[mHistSizeNum];
            mHistSize = new MatOfInt(mHistSizeNum);
            mRanges = new MatOfFloat(0f, 256f);
            mMat0  = new Mat();
            mColorsRGB = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255) };
            mP1 = new Point();
            mP2 = new Point();

            Mat rgba = srcMat;
            Size sizeRgba = rgba.size();
            Mat hist = new Mat(); //转换直方图进行绘制
            int thikness = (int) (sizeRgba.width / (mHistSizeNum + 10) / 5);
            if(thikness > 5) thikness = 5;
            int offset = (int) ((sizeRgba.width - (5*mHistSizeNum + 4*10)*thikness)/2);
            // RGB
            for(int c=0; c<3; c++) {
                Imgproc.calcHist(Arrays.asList(rgba), mChannels[c], mMat0, hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = (int)sizeRgba.height-1;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    //Core.line(rgba, mP1, mP2, mColorsRGB[c], thikness);
                }
            }
            return rgba;
        }
        /**
         * 比较来个矩阵的相似度
         * @param srcMat
         * @param desMat
         */
        public double comPareHist(Mat srcMat,Mat desMat){
            srcMat.convertTo(srcMat, CvType.CV_32F);
            desMat.convertTo(desMat, CvType.CV_32F);
            double target = Imgproc.compareHist(srcMat, desMat, Imgproc.CV_COMP_CORREL);
            Log.e(OpenCvSimilarityActivity.class.getName(), "相似度 ：   ==" + target);
            return target;
        }
    }
}
