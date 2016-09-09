package com.krava.vkmedia.application.ui;

import android.content.Context;
import com.krava.vkmedia.application.VKApplication;

public class Dp {
    private static float scale;

    static {
        scale = -1.0f;
    }

    public static int toPx(float dp) {
        if (scale >= 0.0f) {
            return (int) ((scale * dp) + 0.5f);
        }
        Context context = VKApplication.context;
        if (context == null) {
            return 0;
        }
        scale = context.getResources().getDisplayMetrics().density;
        if (scale < 0.0f) {
            return 0;
        }
        return toPx(dp);
    }

    public static int getDeviceWidth(){
        return VKApplication.context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDeviceHeight(){
        return VKApplication.context.getResources().getDisplayMetrics().heightPixels;
    }
}
