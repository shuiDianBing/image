package com.shuidianbind.image;

import android.Manifest;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.shuidianbind.image.widget.CameraSurfaceView;
import com.shuidianbind.imageaid.CameraHelp;
import com.shuidianbind.imageaid.SimilarPicture;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ActivityCaptureVideo extends AppCompatActivity {
    private static final String TAG = ActivityCaptureVideo.class.getSimpleName();
    private CameraSurfaceView cameraSurfaceView;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private LinearLayout putLayout;
    private ImageView imageSelect;
    private Sensor sensor;
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private LocationManager locationManager;
    private LocationListener onLocationChange;
    private float currentAngle = 0,excursionAngle= 0;
    private HandlerThread thread;
    private Handler handler;
    private ThreadPoolExecutor threadPoolExecutor;
    private ComparisonRunable comparisonRunable;
    private Camera.PreviewCallback previewCallback;
    private final Object LOCK = new Object();
    private float aspectRatio;
    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    Toast.makeText(ActivityCaptureVideo.this,"相似的为："+message.obj,Toast.LENGTH_SHORT).show();
                    break;
                case -1:
                    //cameraSurfaceView.setOneShotPreviewCallback(previewCallback);
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
                    executeCompare();
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    break;
            }
        }
    };
    public static void startActivityForResult(Activity activity,String originUrl) {
        Intent intent = new Intent(activity, ActivityCaptureVideo.class);
        intent.putExtra(ActivityCaptureVideo.class.getSimpleName(), originUrl);
        activity.startActivityForResult(intent, ActivityCaptureVideo.RESULT_CANCELED);
    }
    public static void startActivityForResult(Fragment fragment,String originUrl) {
        Intent intent = new Intent(fragment.getContext(), ActivityCaptureVideo.class);
        intent.putExtra(ActivityCaptureVideo.class.getSimpleName(), originUrl);
        fragment.startActivityForResult(intent, ActivityCaptureVideo.RESULT_CANCELED);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture_video);
        findViewById(R.id.imageBalloon0).startAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon1).startAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon2).startAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon3).setAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon4).setAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon5).setAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        findViewById(R.id.imageBalloon6).setAnimation(AnimationUtils.loadAnimation(this, R.anim.rising));
        //Bitmap bitmapOriginal = BitmapFactory.decodeResource(getResources(), R.mipmap.loading_1);
        //Bitmap grayOriginal = SimilarPicture.convertGreyImg(ThumbnailUtils.extractThumbnail(bitmapOriginal, 8, 8));
        //Bitmap bitmapContrast = BitmapFactory.decodeResource(getResources(), R.mipmap.loading_4);
        //Bitmap grayContrast = SimilarPicture.convertGreyImg(ThumbnailUtils.extractThumbnail(bitmapContrast, 8, 8));
        //float similarity = SimilarPicture.similarity(SimilarPicture.binaryString2hexString(SimilarPicture.getBinary(grayOriginal,SimilarPicture.getAvg(grayOriginal))),
        //        SimilarPicture.binaryString2hexString(SimilarPicture.getBinary(grayContrast,SimilarPicture.getAvg(grayContrast))));
        //Glide.with(this).load(grayOriginal).into((ImageView)findViewById(R.id.imageBalloon3));
        //Glide.with(this).load(grayContrast).into((ImageView)findViewById(R.id.imageBalloon4));
        imageSelect = findViewById(R.id.imageSelect);
        final Criteria criteria = initLocation();
        Glide.with(this).asBitmap().load(getIntent().getStringExtra(getClass().getSimpleName()))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        aspectRatio = resource.getHeight() / (float) resource.getWidth();
                        imageSelect.setVisibility(View.VISIBLE);
                        imageSelect.setImageBitmap(resource);
                        comparisonRunable.setOrigin(resource);
                    }
                });
        initPutLayout(8);
        executeCompare();
        //preview();
        previewOpnCv();
        if (OpenCVLoader.initDebug()) {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, loaderCallback);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(onLocationChange);
        sensorManager.unregisterListener(sensorEventListener);
        if (null != cameraBridgeViewBase)
            cameraBridgeViewBase.disableView();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000/*4秒，为测试方便*/, 0/*1公里*/,
                    onLocationChange/*位置监听器*/);
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI);//rate suitable for the user interface
        if (OpenCVLoader.initDebug()) {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, loaderCallback);
        }
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        for(int index = 0;index < putLayout.getChildCount();index++)
            startAnim(putLayout.getChildAt(index));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPoolExecutor.shutdownNow();
    }
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        for(int index = 0;index < putLayout.getChildCount();index++)
//            startAnim(putLayout.getChildAt(index));
//    }
    private Criteria initLocation(){
        final TextView textDirection = findViewById(R.id.textDirection);
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        List<String> list = locationManager.getProviders(true);
        if (list != null) {
            for (String x : list) {
                Log.e("gzq", "name:" + x);
            }
        }
        LocationProvider lpGps = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        LocationProvider lpNet = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
        LocationProvider lpPsv = locationManager.getProvider(LocationManager.PASSIVE_PROVIDER);
        Criteria criteria = new Criteria();
        // Criteria是一组筛选条件
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        //设置定位精准度
        criteria.setAltitudeRequired(false);
        //是否要求海拔
        criteria.setBearingRequired(true);
        //是否要求方向
        criteria.setCostAllowed(true);
        //是否要求收费
        criteria.setSpeedRequired(true);
        //是否要求速度
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        //设置电池耗电要求
        criteria.setBearingAccuracy(Criteria.ACCURACY_HIGH);
        //设置方向精确度
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
        //设置速度精确度
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        //设置水平方向精确度
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        //设置垂直方向精确度
        //返回满足条件的当前设备可用的provider，第二个参数为false时返回当前设备所有provider中最符合条件的那个provider，但是不一定可用
        String provider = locationManager.getBestProvider(criteria, true);
        @SuppressLint("MissingPermission") final Location endLocation = new Location(provider);//locationManager.getLastKnownLocation(provider);
        endLocation.setLatitude(106.568434);
        endLocation.setLongitude(29.544372);
        final Location statLocation = new Location(provider);
        statLocation.setLatitude(106.568434);
        statLocation.setLongitude(29.544372);
        excursionAngle = statLocation.bearingTo(endLocation);
        //RotateAnimation rotateAnimation = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        //rotateAnimation.setFillAfter(true);//停留在动画结束
        //rotateAnimation.setDuration(1000);
        //imageDirection.startAnimation(rotateAnimation);
        onLocationChange = new LocationListener(){
            @Override
            public void onLocationChanged(Location location) {
                float currentAngle = location.bearingTo(endLocation);
                RotateAnimation rotateAnimation = new RotateAnimation(ActivityCaptureVideo.this.currentAngle, currentAngle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(100);
                float distance = location.distanceTo(endLocation);
                if(1000<= distance)
                    textDirection.setText(new DecimalFormat("0.00").format(distance/1000)+"km");
                else
                    textDirection.setText(new DecimalFormat("0.00").format(distance)+"m");
                textDirection.startAnimation(rotateAnimation);
                ActivityCaptureVideo.this.currentAngle = currentAngle;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG,"onStatusChanged.status = "+status);
            }
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,0, onLocationChange);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ORIENTATION){
                    float degree = -event.values[0];
                    RotateAnimation rotateAnimation = new RotateAnimation(currentAngle, currentAngle=(degree + excursionAngle), Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotateAnimation.setFillAfter(true);
                    rotateAnimation.setDuration(4000);
                    statLocation.setLatitude(106.568434);
                    statLocation.setLongitude(29.544372);
                    float distance = statLocation.distanceTo(endLocation);
                    if(1000<= distance)
                        textDirection.setText(new DecimalFormat("0.00").format(distance/1000)+"km");
                    else
                        textDirection.setText(new DecimalFormat("0.00").format(distance)+"m");
                    textDirection.setAnimation(rotateAnimation);
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI);//rate suitable for the user interface
        return criteria;
    }
    private float excursionAngle(Criteria criteria,double latitude,double longitude){
        //返回满足条件的当前设备可用的provider，第二个参数为false时返回当前设备所有provider中最符合条件的那个provider，但是不一定可用
        String provider = locationManager.getBestProvider(criteria, true);
        @SuppressLint("MissingPermission") Location endLocation = new Location(provider);
        endLocation.setLatitude(latitude);
        endLocation.setLongitude(longitude);
        Location statLocation = new Location(provider);
        statLocation.setLatitude(106.568434);
        statLocation.setLongitude(29.544372);
        return statLocation.bearingTo(endLocation);
    }
    /**
     * 旋转图片，使图片保持正确的方向。
     */
    public static Bitmap rota(float degrees,Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (null != bitmap)
            bitmap.recycle();
        return bmp;
    }
    private void initPutLayout(int count){
        int[] balloonImage = new int[]{R.mipmap.balloon_blue,R.mipmap.balloon_light_blue,R.mipmap.balloon_green,
                R.mipmap.balloon_pink,R.mipmap.balloon_soft_red,R.mipmap.balloon_red,R.mipmap.balloon_yellow};
        putLayout = findViewById(R.id.linear);
        //putLayout.setTag(putBalloons.get(0));
        Random random = new Random();
        for(int index = 0;index < count;index++) {
            ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(this).inflate(R.layout.view_balloon,putLayout,false);
            layout.setId(index);
            layout.setOnClickListener(this::onClick);
            ((ImageView)layout.findViewById(R.id.imageBackgound)).setImageResource(balloonImage[random.nextInt(balloonImage.length)]);
            //if(0== index)
            //    layout.setBackgroundColor(ContextCompat.getColor(this,android.R.color.holo_red_dark));
            Glide.with(this).load("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1564126557377&di=7b533387099f09d34aba833b2d273ae1&imgtype=0&src=http%3A%2F%2Fhbimg.huabanimg.com%2F1501ba2377b7b7ccf791fc13ac48f3a35c5eaa5e278a1-NOdwbF_fw658")
                    .into((ImageView) layout.findViewById(R.id.imageView));
            putLayout.addView(layout);
        }
    }
    private void preview() {
        //ImageView imageBalloon = findViewById(R.id.imageBalloon3);
        int rotation = CameraHelp.calculateCameraPreviewOrientation(this);
        (cameraSurfaceView = findViewById(R.id.cameraSurface)).setOneShotPreviewCallback(previewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
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
                if(null != comparisonRunable && null!= comparisonRunable.origin) {
                    Bitmap bitmap = CameraHelp.getBitMap(data, camera,comparisonRunable.origin.getWidth()
                            ,comparisonRunable.origin.getHeight(), false);
                    threadPoolExecutor.execute(comparisonRunable);
                    comparisonRunable.setContrast(bitmap);
                }else
                    handler.obtainMessage(-1).sendToTarget();
                //Glide.with(imageBalloon.getContext()).load(bitmap).into(imageBalloon);
            }
        });
    }
    private void previewOpnCv(){
        //final int[] xy = Screen.getScreenScale(this);
        cameraBridgeViewBase = findViewById(R.id.javaCameraView);
        cameraBridgeViewBase.setVisibility(CameraBridgeViewBase.VISIBLE);
        //final ImageView imageView = findViewById(R.id.imageOpencvPreview);
        final View window = findViewById(R.id.window);
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
                if(null != comparisonRunable && null!= comparisonRunable.origin) {
                    // 要把Mat对象转换成Bitmap对象，需要创建一个宽高相同的Bitmap对象昨晚参数
                    Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(rgba, bitmap);// Mat >>> Bitmap
                    float height = window.getHeight()* aspectRatio;
                    bitmap = Bitmap.createBitmap(bitmap,(int)window.getX(),(int)(window.getY()* bitmap.getHeight()/cameraBridgeViewBase.getHeight()),window.getWidth(),(int)height);
                    comparisonRunable.setContrast(bitmap = zoomImg(bitmap,comparisonRunable.origin.getWidth(),comparisonRunable.origin.getHeight()));
                    threadPoolExecutor.execute(comparisonRunable);
                    // 每隔0.5秒对比一次
                    handler.sendEmptyMessageDelayed(-1, 500);
                    //displayFrame(imageView,bitmap);
                }
                // 将每一帧的图像展示在界面上,
                return rgba;
            }
            @Override
            public void onCameraViewStarted(int i, int i1) {}
            @Override
            public void onCameraViewStopped() {}
        });
    }
    public static Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }
    private void displayFrame(final ImageView imageView,final Bitmap bitmap){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }
    private void executeCompare() {
        thread = new HandlerThread(TAG);
        thread.start();
        handler = new Handler(thread.getLooper(), callback);
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        comparisonRunable = new ComparisonRunable(this);
    }
    /**
     * 气球漂浮动画
     * @param view
     */
    private void startAnim(final View view){
        Display display = getWindowManager().getDefaultDisplay();
        final int width = display.getWidth();
        final int height = display.getHeight();
        int viewWidth = view.getMeasuredWidth();
        int viewHeight = view.getMeasuredHeight();
        //int absoluteXy[] = new int[2];
        //view.getLocationInWindow(absoluteXy); //获取在当前窗口内的绝对坐标
        //view.getLocationOnScreen(absoluteXy);//获取在整个屏幕内的绝对坐标
        PointF startPoint = new PointF(), endPoint = new PointF(), controllPointone = new PointF(), controllPointTwo = new PointF();
        startPoint.x = view.getX();//(width - viewWidth) / 2;
        startPoint.y = height;// - viewHeight;
        endPoint.x = view.getX();//new Random().nextInt(width - viewWidth);//(width - viewWidth) / 2;
        endPoint.y = (height - viewHeight) / 64;//-viewHeight;
        Random random = new Random(System.currentTimeMillis());
        controllPointone.x = random.nextInt(width / 2);
        controllPointone.y = random.nextInt(height / 2) + height / 2;

        controllPointTwo.x = random.nextInt(width / 2) + width / 2;
        controllPointTwo.y = random.nextInt(height / 2);
        BezierTypeEvaluator bezierTypeEvaluator = new BezierTypeEvaluator(startPoint,controllPointone, controllPointTwo);
        ValueAnimator valueAnimator = ValueAnimator.ofObject(bezierTypeEvaluator, startPoint, endPoint);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PointF pointF = (PointF) animation.getAnimatedValue();
                view.setX(pointF.x);
                view.setY(pointF.y);
            }
        });
        valueAnimator.setDuration(4000);
        valueAnimator.start();
    }
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageBack:
                finish();
                break;
            case R.id.imageBalloon0:
            case R.id.imageBalloon1:
            case R.id.imageBalloon2:
            case R.id.imageBalloon3:
            case R.id.imageBalloon4:
            case R.id.imageBalloon5:
            case R.id.imageBalloon6:break;
            case R.id.imageOpencvPreview:
                break;
            default:
                for(int index = 0;index < putLayout.getChildCount();index++) {
                    ViewGroup viewGroup = (ViewGroup) putLayout.getChildAt(index);
                    for (int point = 0; point < 8; point++)
                        viewGroup.getChildAt(point).setVisibility(View.GONE);
                }
                for(int index = 0;index < 8;index++)
                    ((ViewGroup)view).getChildAt(index).setVisibility(View.VISIBLE);
                putLayout.setTag(view.getTag());
                //TranslateAnimation translateAnimation1 = new TranslateAnimation(view.getX(),0,view.getY(),1000);
//        使用java代码的方式创建TranslateAnimation，传入六个参数，fromXType、fromXValue、toXType、toXValue和fromYType、fromYValue、toYType、toYValue，使用如下构造方法。
//        参数说明：
//        fromXType：动画开始前的X坐标类型。取值范围为ABSOLUTE（绝对位置）、RELATIVE_TO_SELF（以自身宽或高为参考）、RELATIVE_TO_PARENT（以父控件宽或高为参考）。
//        fromXValue：动画开始前的X坐标值。当对应的Type为ABSOLUTE时，表示绝对位置；否则表示相对位置，1.0表示100%。
//        toXType：动画结束后的X坐标类型。
//        toXValue：动画结束后的X坐标值。
//        fromYType：动画开始前的Y坐标类型。
//        fromYValue：动画开始前的Y坐标值。
//        toYType：动画结束后的Y坐标类型。
//        toYValue：动画结束后的Y坐标值。
                //translateAnimation1.setDuration(1000);
                //translateAnimation1.setInterpolator(new DecelerateInterpolator());
                //view.startAnimation(translateAnimation1);
                break;
        }
    }
    private void loadBitmap(){
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap bitmap = null;
                try {
                    bitmap = Glide.with(putLayout.getContext())
                            .asBitmap()
                            .load("http://img.redocn.com/sheji/20141219/zhongguofengdaodeliyizhanbanzhijing_3744115.jpg")
                            .into(1000, 633).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
            }
        }.execute();
    }
    private static void goback(AppCompatActivity appCompatActivity,double similarity) {
        if (null != appCompatActivity) {
            Intent intent = new Intent();
            intent.putExtra("similarity", similarity);
            appCompatActivity.setResult(Activity.RESULT_OK, intent);
            appCompatActivity.finish();
        }
    }
    public static float resultSimilarity(Intent intent) {
        return intent.getFloatExtra("similarity", 0);
    }
    public class ComparisonRunable implements Runnable {
        private WeakReference<AppCompatActivity> weakReference;
        private Bitmap origin, contrast;
        private String fingerprint;
        public ComparisonRunable(AppCompatActivity appCompatActivity) {
            weakReference = new WeakReference<AppCompatActivity>(appCompatActivity);
        }
        public void setOrigin(Bitmap origin){
            this.origin = origin;
        }
        public void setContrast(Bitmap bitmap) {
            contrast = bitmap;
        }
        @Override
        public void run() {
            Log.d("当前线程：", Thread.currentThread().getName());
            if(null != origin && null != contrast) {
                /*Mat mat1 = new Mat();
                Mat mat2 = new Mat();
                Mat mat11 = new Mat();
                Mat mat22 = new Mat();
                Utils.bitmapToMat(origin, mat1);
                Utils.bitmapToMat(contrast, mat2);
                Imgproc.cvtColor(mat1, mat11, Imgproc.COLOR_BGR2GRAY);
                Imgproc.cvtColor(mat2, mat22, Imgproc.COLOR_BGR2GRAY);
                double similarity = comPareHist(mat11, mat22);*/
                double similarity = /*comPareHist(origin,contrast);*/SimilarPicture.similarity(origin,contrast,8,8);
                Log.e(ActivityCaptureVideo.class.getName(), "相似度 ：   ==" + similarity);
                synchronized (LOCK) {
                    handler.obtainMessage(0.64 < similarity && similarity <1 ? 0 : -1, similarity).sendToTarget();//(200 < similarity && Double.NaN != similarity? 0 : -1, similarity).sendToTarget();
                }
//                Mat mat1 = new Mat();
//                Mat mat2 = new Mat();
//                Utils.bitmapToMat(origin, mat1);
//                Utils.bitmapToMat(contrast, mat2);
//                boolean similarity = compareByHist(mat1,mat2);
//                synchronized (LOCK) {
//                    handler.obtainMessage(similarity ? 0 : -1, similarity).sendToTarget();
//                }
            }else
                handler.obtainMessage(-1).sendToTarget();
        }
        /**
         * 比较来个矩阵的相似度
         * @param srcMat
         * @param desMat
         */
//        public double comPareHist(Mat srcMat, Mat desMat){
//            srcMat.convertTo(srcMat, CvType.CV_32F);
//            desMat.convertTo(desMat, CvType.CV_32F);
//            double target = Imgproc.compareHist(srcMat, desMat, Imgproc.CV_COMP_CORREL);
//            Log.e(ActivityCaptureVideo.class.getName(), "相似度 ：   ==" + target);
//            return target;
//        }
        /**
         * Compare that two images is similar useing histogram
         *
         * @param img    the first image mat
         * @param orgImg the seconed image mat
         * @return true : two image are similar , false is difference
         * @author navyLiu
         */
        private boolean compareByHist(Mat img, Mat orgImg) {
            Mat hsvImg1 = new Mat();
            Mat hsvImg2 = new Mat();
            // 图像转换到HSV空间进行比较
            Imgproc.cvtColor(img, hsvImg1, Imgproc.COLOR_BGR2HSV);
            Imgproc.cvtColor(orgImg, hsvImg2, Imgproc.COLOR_BGR2HSV);
//        // 设定直方图需要的相关参数
//        int h_bins = 50;
//        int s_bins =60;
//        int v_bins=60;
//        int histSize[] = {h_bins,s_bins,v_bins};
//        // hue varies from 0 to 179, saturation from 0 to 255
//        float h_ranges[] = {0,180}; // 色调H用角度度量，取值范围为0°～360°，从红色开始按逆时针方向计算，红色为0°，绿色为120°,蓝色为240°。它们的补色是：黄色为60°，青色为180°,品红为300°；
//        float s_ranges[] = {0,180};
//        float v_ranges[] = {0,180};
//        float ranges[][] = {h_ranges,s_ranges,v_ranges};
//
//        int channels[] = {0 , 1};//比较H和S通道直方图，由于函数只能最多比较2维直方图，所以需要进行选择。
//        //int channels[] = { 1, 2};//比较S和V通道直方图
//        //int channels[] = {0};//只比较第一个H通道的直方图选择
            // 新建多维度Mat类型变量
            List<Mat> listImage1 = new ArrayList<>();
            List<Mat> listImage2 = new ArrayList<>();

            listImage1.add(hsvImg1);
            listImage2.add(hsvImg2);

            MatOfFloat ranges = new MatOfFloat(0, 255);
            MatOfInt histSize = new MatOfInt(50);
            MatOfInt channels = new MatOfInt(0);

            // histograms
            Mat histImg1 = new Mat();
            Mat histImg2 = new Mat();

            // calculate the histogram ofr the HSV images
            Imgproc.calcHist(listImage1, channels, new Mat(), histImg1, histSize, ranges);
            Imgproc.calcHist(listImage2, channels, new Mat(), histImg2, histSize, ranges);

            Core.normalize(histImg1, histImg1, 0, 1, Core.NORM_MINMAX, -1, new Mat());
            Core.normalize(histImg2, histImg2, 0, 1, Core.NORM_MINMAX, -1, new Mat());

            // Apply the histogram comparison methods
            // 0 - correlateion:the heiger the metric, the more accurate the match ">0.9"
            // 1 - chi-square: the lower the metric, the more accurate the match " <0.1"
            // 2 - intersection : the higher the metric, the more accurate the match ">1.5"
            // 3 - bhattacharyya: the lower the metric, the more accurate the match "<0.3"
            double result0, result1, result2, result3;
            result0 = Imgproc.compareHist(histImg1, histImg2, Imgproc.CV_COMP_CORREL);
            result1 = Imgproc.compareHist(histImg1, histImg2, Imgproc.CV_COMP_CHISQR);
            result2 = Imgproc.compareHist(histImg1, histImg2, Imgproc.CV_COMP_INTERSECT);
            result3 = Imgproc.compareHist(histImg1, histImg2, Imgproc.CV_COMP_BHATTACHARYYA);

            //LogUtils.d("result0:" + result0 + ",result1" + result1 + ",result2" + result2 + ",result3" + result3);

            // if the count that it is satisfied with the condition is over 3, two images is same
            int count = 0;
            if (result0 > 0.9) count++;
            if (result1 < 0.1) count++;
            if (result2 > 1.5) count++;
            if (result3 < 0.3) count++;
            Log.d("ComparisonRunable","compareByHist:count="+count);
           // ToastUtils.showShort("count is" + count);
            return count > 1;
        }

        /**
         * 比较来个矩阵的相似度
         *
         * @param origin
         * @param contrast
         * @return
         */
        public double comPareHist(Mat origin, Mat contrast) {
            Mat srcMat = new Mat();
            Mat desMat = new Mat();
            Imgproc.cvtColor(origin, srcMat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(contrast, desMat, Imgproc.COLOR_BGR2GRAY);
            srcMat.convertTo(srcMat, CvType.CV_32F);
            desMat.convertTo(desMat, CvType.CV_32F);
            double target = Imgproc.compareHist(srcMat, desMat, Imgproc.CV_COMP_CORREL);
            return target;
        }
        /**
         * 比较来个矩阵的相似度
         *
         * @param origin
         * @param contrast
         * @return
         */
        public double comPareHist(Bitmap origin, Bitmap contrast) {
            Mat mat1 = new Mat();
            Mat mat2 = new Mat();
            Utils.bitmapToMat(origin, mat1);
            Utils.bitmapToMat(contrast, mat2);
            return imgMatching2(mat1,mat2);//comPareHist(mat1, mat2);
        }
        // 特征点匹配，值越大匹配度越高
        public float imgMatching2(Mat origin,Mat contrast) {
            //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            Mat gray_base = new Mat();
            Mat gray_test = new Mat();
            // 转换为灰度
            Imgproc.cvtColor(origin, gray_base, Imgproc.COLOR_RGB2GRAY);
            Imgproc.cvtColor(contrast, gray_test, Imgproc.COLOR_RGB2GRAY);
            // 初始化ORB检测描述子
            FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.ORB);//特别提示下这里opencv暂时不支持SIFT、SURF检测方法，这个好像是opencv(windows) java版的一个bug,本人在这里被坑了好久。
            DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            // 关键点及特征描述矩阵声明
            MatOfKeyPoint keyPoint1 = new MatOfKeyPoint(), keyPoint2 = new MatOfKeyPoint();
            Mat descriptorMat1 = new Mat(), descriptorMat2 = new Mat();
            // 计算ORB特征关键点
            featureDetector.detect(gray_base, keyPoint1);
            featureDetector.detect(gray_test, keyPoint2);
            // 计算ORB特征描述矩阵
            descriptorExtractor.compute(gray_base, keyPoint1, descriptorMat1);
            descriptorExtractor.compute(gray_test, keyPoint2, descriptorMat2);
            float result = 0;
            // 特征点匹配
            Log.d(ActivityCaptureVideo.class.getName(),"keyPoint1.size= "+ keyPoint1.size()+"keyPoint2.size ="+ keyPoint2.size());
            if (!keyPoint1.size().empty() && !keyPoint2.size().empty()) {
                // FlannBasedMatcher matcher = new FlannBasedMatcher();
                DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);
                MatOfDMatch matches = new MatOfDMatch();
                matcher.match(descriptorMat1, descriptorMat2, matches);
                // 最优匹配判断
                double minDist = 100;
                DMatch[] dMatchs = matches.toArray();
                int num = 0;
                for (int i = 0; i < dMatchs.length; i++) {
                    if (dMatchs[i].distance <= 2 * minDist) {
                        result += dMatchs[i].distance * dMatchs[i].distance;
                        num++;
                    }
                }
                // 匹配度计算
                result /= num;
            }
            System.out.println(result);
            return result;
        }
    }
    public static class BezierTypeEvaluator implements TypeEvaluator<PointF> {
        private PointF startPoint,mControllPoint1, mControllPoint2;

        public BezierTypeEvaluator(PointF startPoint,PointF mControllPointOne, PointF mControllPointTwo) {
            this.startPoint = startPoint;
            mControllPoint1 = mControllPointOne;
            mControllPoint2 = mControllPointTwo;
        }

        @Override
        public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
            PointF pointCur = new PointF();
            //三次贝塞尔曲线
            pointCur.x = startPoint.x * (1 - fraction) * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint1.x * fraction * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint2.x * (1 - fraction) * fraction * fraction + endValue.x * fraction * fraction * fraction;// 实时计算最新的点X坐标
            pointCur.y = startPoint.y * (1 - fraction) * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint1.y * fraction * (1 - fraction) * (1 - fraction) + 3
                    * mControllPoint2.y * (1 - fraction) * fraction * fraction + endValue.y * fraction * fraction * fraction;// 实时计算最新的点Y坐标
            return pointCur;
        }
    }
}
