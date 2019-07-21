package com.shuidianbind.image;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.shuidianbind.image.widget.CameraSurfaceView;
import com.shuidianbind.imageaid.CameraHelp;
import com.shuidianbind.imageaid.SimilarPicture;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private CameraSurfaceView cameraSurfaceView;
    private PointF mStartPoint = new PointF(), mEndPoint = new PointF(), mControllPointOne = new PointF(), mControllPointTwo = new PointF();
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bitmap bitmapOriginal = BitmapFactory.decodeResource(getResources(), R.mipmap.image_jiezishu);
        //Bitmap grayOriginal = SimilarPicture.convertGreyImg(ThumbnailUtils.extractThumbnail(bitmapOriginal, 8, 8));
        //Bitmap bitmapContrast = BitmapFactory.decodeResource(getResources(), R.mipmap.loading_4);
        //Bitmap grayContrast = SimilarPicture.convertGreyImg(ThumbnailUtils.extractThumbnail(bitmapContrast, 8, 8));
        //float similarity = SimilarPicture.similarity(SimilarPicture.binaryString2hexString(SimilarPicture.getBinary(grayOriginal,SimilarPicture.getAvg(grayOriginal))),
        //        SimilarPicture.binaryString2hexString(SimilarPicture.getBinary(grayContrast,SimilarPicture.getAvg(grayContrast))));
        //Glide.with(this).load(grayOriginal).into((ImageView)findViewById(R.id.imageBalloon3));
        //Glide.with(this).load(grayContrast).into((ImageView)findViewById(R.id.imageBalloon4));
        preview();
        start(bitmapOriginal);
//        new AsyncTask<Void, Void, Bitmap>() {
//            @Override
//            protected Bitmap doInBackground(Void... params) {
//                Bitmap bitmap = null;
//                try {
//                    bitmap = Glide.with(MainActivity.this)
//                            .asBitmap()
//                            .load("http://img.redocn.com/sheji/20141219/zhongguofengdaodeliyizhanbanzhijing_3744115.jpg")
//                            .into(1000, 633).get();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return bitmap;
//            }
//
//            @Override
//            protected void onPostExecute(Bitmap bitmap) {
//                start(bitmap);
//            }
//        }.execute();

        startActivity(new Intent(this, OpencvActivity.class));
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
                Bitmap bitmap = CameraHelp.adjustment(MainActivity.this,data,camera,view.getWidth(),view.getHeight());
                comparisonRunable.setContrast(bitmap);
                if (null != comparisonRunable)
                    threadPoolExecutor.execute(comparisonRunable);
                Glide.with(imageBalloon.getContext()).load(bitmap).into(imageBalloon);
            }
        });
    }

    public void start(Bitmap bitmap) {
        thread = new HandlerThread(TAG);
        thread.start();
        handler = new Handler(thread.getLooper(), callback);
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        comparisonRunable = new ComparisonRunable(this, bitmap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraHelp.releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPoolExecutor.shutdownNow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        findViewById(R.id.imageBalloon0).startAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon1).startAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon2).startAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        final ImageView imageBalloon = findViewById(R.id.imageBalloon3);
        //imageBalloon.setAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        //findViewById(R.id.imageBalloon4).setAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        int viewWidth = imageBalloon.getMeasuredWidth();
        int viewHeight = imageBalloon.getMeasuredHeight();
        mStartPoint.x = (width - viewWidth) / 2;
        mStartPoint.y = height;// - viewHeight;
        mEndPoint.x = (width - viewWidth) / 2;
        mEndPoint.y = 32;

        Random random = new Random();
        mControllPointOne.x = random.nextInt(width / 2);
        mControllPointOne.y = random.nextInt(height / 2) + height / 2;

        mControllPointTwo.x = random.nextInt(width / 2) + width / 2;
        mControllPointTwo.y = random.nextInt(height / 2);
        BezierTypeEvaluator bezierTypeEvaluator = new BezierTypeEvaluator(mControllPointOne, mControllPointTwo);
        ValueAnimator valueAnimator = ValueAnimator.ofObject(bezierTypeEvaluator, mStartPoint, mEndPoint);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PointF pointF = (PointF) animation.getAnimatedValue();
                imageBalloon.setX(pointF.x);
                imageBalloon.setY(pointF.y);
            }
        });
        valueAnimator.setDuration(4000);
        //valueAnimator.start();
    }

    private static void goback(AppCompatActivity appCompatActivity, float similarity) {
        if (null != appCompatActivity) {
            Intent intent = new Intent();
            intent.putExtra(TAG, similarity);
            appCompatActivity.setResult(Activity.RESULT_OK, intent);
            appCompatActivity.finish();
        }
    }

    public static float result(Intent intent) {
        return intent.getFloatExtra(TAG, 0);
    }

    public class ComparisonRunable implements Runnable {
        private WeakReference<Activity> weakReference;
        private Bitmap origin, contrast;
        private String fingerprint;

        public ComparisonRunable(Activity activity, Bitmap origin) {
            weakReference = new WeakReference<Activity>(activity);
            this.origin = origin;
        }

        public void setContrast(Bitmap bitmap) {
            contrast = bitmap;
        }

        @Override
        public void run() {
            Log.d("当前线程：", Thread.currentThread().getName());
            float similarity = SimilarPicture.similarity(origin,contrast,8,8);
            synchronized (LOCK) {
                handler.obtainMessage(0.7 < similarity ? 0 : -1, contrast).sendToTarget();
                //if (0.8 < similarity)
                    //goback(weakReference.get(), similarity);
            }
        }
    }

    public class BezierTypeEvaluator implements TypeEvaluator<PointF> {
        private PointF mControllPoint1, mControllPoint2;

        public BezierTypeEvaluator(PointF mControllPointOne, PointF mControllPointTwo) {
            mControllPoint1 = mControllPointOne;
            mControllPoint2 = mControllPointTwo;
        }

        @Override
        public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
            PointF pointCur = new PointF();
            //三次贝塞尔曲线
            pointCur.x = mStartPoint.x * (1 - fraction) * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint1.x * fraction * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint2.x * (1 - fraction) * fraction * fraction + endValue.x * fraction * fraction * fraction;// 实时计算最新的点X坐标
            pointCur.y = mStartPoint.y * (1 - fraction) * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint1.y * fraction * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint2.y * (1 - fraction) * fraction * fraction + endValue.y * fraction * fraction * fraction;// 实时计算最新的点Y坐标
            return pointCur;
        }
    }
}