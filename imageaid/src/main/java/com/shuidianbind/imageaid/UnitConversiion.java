package com.shuidianbind.imageaid;

import android.content.Context;

public final class UnitConversiion {
    /**
     * px转dp
     * dp = pxValue / density + 0.5f;
     * @param context
     * @param px
     * @return
     */
    public static final float pxToDp(Context context,int px){
        final float density = context.getResources().getDisplayMetrics().density;
        return px / density +0.5f;
    }
    /**
     * dp转px
     * px = dpValue * density + 0.5f;
     * @param context
     * @param dp
     * @return
     */
    public static final float dpToPx(Context context,int dp){
        final float density = context.getResources().getDisplayMetrics().density;
        return dp * density + 0.5f;
    }

    /**
     * px转sp
     * @param context
     * @param px
     * @return
     */
    public static final float pxToSp(Context context,int px){
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return px /fontScale + 0.5f;
    }

    /**
     * sp转px
     * @param context
     * @param sp
     * @return
     */
    public static final float spToPx(Context context,int sp){
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return sp * fontScale + 0.5f;
    }
}
