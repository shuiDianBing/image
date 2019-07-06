package com.shuidianbind.imageaid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import static android.content.ContentValues.TAG;

/**
 * Android 屏幕各尺寸的获取 << https://www.jianshu.com/p/a1ab688d7ef8
 * Android 获取屏幕宽度和高度的几种方法 << https://www.jianshu.com/p/1a931d943fb4
 */
public class Screen {
    /**
     * 获取屏幕尺寸
     * @param activity
     * @return
     */
    @Deprecated
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public static int[] getScreenSize(Activity activity){
        Display display = activity.getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        Log.d(TAG, "width = " + width + ",height = " + height);
        return new int[]{width,height};
    }

    /**
     * 获取屏幕规模
     * @param activity
     * @return
     */
    public static int[] getScreenScale(Activity activity){
        Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        int x = point.x;
        int y = point.y;
        return new int[]{x,y};
    }

    /**
     *
     * @param windowManager
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int[] getScreenScale(WindowManager windowManager){
        Point outSize = new Point();
        windowManager.getDefaultDisplay().getRealSize(outSize);
        int x = outSize.x;
        int y = outSize.y;
        Log.w(TAG, "x = " + x + ",y = " + y);
        //x = 1440,y = 2960
        return new int[]{x,y};
    }

    /**
     *
     * @param windowManager
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int[] getScreenStandard(WindowManager windowManager){
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);
        int widthPixel = outMetrics.widthPixels;
        int heightPixel = outMetrics.heightPixels;
        Log.w(TAG, "widthPixel = " + widthPixel + ",heightPixel = " + heightPixel);
        //widthPixel = 1440,heightPixel = 2960
        return new int[]{widthPixel,heightPixel};
    }

    /**
     * 获取屏幕规格
     * @param activity
     * @return
     */
    public static int[] getScreenStandard(Activity activity){
        Rect outSize = new Rect();
        activity.getWindowManager().getDefaultDisplay().getRectSize(outSize);
        int left = outSize.left;
        int top = outSize.top;
        int right = outSize.right;
        int bottom = outSize.bottom;
        Log.d(TAG, "left = " + left + ",top = " + top + ",right = " + right + ",bottom = " + bottom);
        //left = 0,top = 0,right = 1440,bottom = 2768
        return new int[]{right,bottom};
    }
    /**
     * 获取屏幕的宽高
     *
     * @param context
     * @return
     */
    public static int[] getScreenWH(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return new int[]{dm.widthPixels,dm.heightPixels};
    }

    /**
     * 去除虚拟键底部导航高度
     * @param context
     * @return
     */
    public static int getRealHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screenHeight = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics dm = new DisplayMetrics();
            display.getRealMetrics(dm);
            screenHeight = dm.heightPixels;

            //或者也可以使用getRealSize方法
//            Point size = new Point();
//            display.getRealSize(size);
//            screenHeight = size.y;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            try {
                screenHeight = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception e) {
                DisplayMetrics dm = new DisplayMetrics();
                display.getMetrics(dm);
                screenHeight = dm.heightPixels;
            }
        return screenHeight;
    }

    /**
     * 虚拟按键高度
     * @param context
     * @return
     */
    public static int getNavigationBarHeight(Context context) {
        int navigationBarHeight = -1;
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height","dimen", "android");
        if (resourceId > 0)
            navigationBarHeight = resources.getDimensionPixelSize(resourceId);
        return navigationBarHeight;
    }

    /**
     * 状态栏高度
     * @return
     */
    public int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            result = context.getResources().getDimensionPixelSize(resourceId);
        return result;
    }

    /**
     *  通过反射获取状态栏高度
     */
    public void getStatusBarHeightByReflect(Context context) {
        int statusBarHeight2 = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusBarHeight2 = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "状态栏高度-反射方式：" + statusBarHeight2);
    }
    /**
     * 应用区的顶端位置即状态栏的高度
     * *注意*该方法不能在初始化的时候用
     * */
    public void getStatusBarHeightByTop(Activity activity) {
        Rect rectangle = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
        Log.e(TAG, "状态栏高度-应用区顶部:" + rectangle.top);
    }
    /**
     * 应用区域高度
     * 不能在 onCreate 方法中使用。
     * 因为这种方法依赖于WMS（窗口管理服务的回调）。正是因为窗口回调机制，所以在Activity初始化时执行此方法得到的高度是0。
     * 这个方法推荐在回调方法onWindowFocusChanged()中执行，才能得到预期结果。
     */
    public void getAppViewHeight(Activity activity){
        //屏幕
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        //应用区域
        Rect outRect1 = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect1);
        int statusBar = dm.heightPixels - outRect1.height();  //状态栏高度=屏幕高度-应用区域高度
        Log.e(TAG, "应用区高度:" + statusBar);
    }

    /**
     *界面创建后获取
     * @param activity
     * @return
     */
    public static int getContentViewHeight(Activity activity) {
        Rect rectangle= new Rect();
        activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getDrawingRect(rectangle);
        return rectangle.height();
    }

    /**
     * 标题栏高度 = 应用区高度 - view 显示高度
     * @param activity
     */
    public static void getTitleBarHeight(Activity activity) {
        Rect outRect1 = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect1);

        int viewTop = activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();   //要用这种方法
        int titleBarH = viewTop - outRect1.top;

        Log.e(TAG, "标题栏高度-计算:" + titleBarH);
    }

}
