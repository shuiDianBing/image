package com.shuidianbind.imageaid;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;

public class BitmapHelp {
    /**
     * @param bytes
     * @param strides The YUV data format as defined in {@link ImageFormat}.
     * @param quality 0-100图片压缩 0表示压缩为小尺寸，100表示压缩最大质量。
     * @param width
     * @param height
     * @return
     */
    public static byte[] convertYuv(byte[] bytes,int strides, int quality,int width,int height){
        YuvImage yuvImage = new YuvImage(bytes,strides,width,height,null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), quality, byteArrayOutputStream);
        byte[] jpegData = byteArrayOutputStream.toByteArray();
        return jpegData;
    }
    public static Bitmap zoom(Bitmap bitmap,float rotateDegrees,float scale){
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(rotateDegrees);
        matrix.postScale(scale,scale); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return resizeBmp;
    }
}
