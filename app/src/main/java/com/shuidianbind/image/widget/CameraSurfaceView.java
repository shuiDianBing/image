package com.shuidianbind.image.widget;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.shuidianbind.imageaid.CameraHelp;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mSurfaceHolder;
    private Camera.PreviewCallback previewCallback;
    public CameraSurfaceView(Context context) {
        super(context);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CameraHelp.openFrontalCamera(CameraHelp.DESIRED_PREVIEW_FPS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        CameraHelp.startPreviewDisplay(holder);
        CameraHelp.setOneShotPreviewCallback(previewCallback);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraHelp.releaseCamera();
    }

    public void setOneShotPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
        CameraHelp.setOneShotPreviewCallback(previewCallback);
    }
//    @SuppressWarnings("deprecation")
//    private class AutoFocusCallBack implements Camera.AutoFocusCallback {
//
//        @Override
//        public void onAutoFocus(boolean success, Camera camera) {
//            Log.e("mandy", "onAutoFocus==" + success);
//            autoFocusHandler.sendEmptyMessageDelayed(AUTO_FOCUS, AUTO_FOCUS_INTERVAL);
//        }
//    }
}
