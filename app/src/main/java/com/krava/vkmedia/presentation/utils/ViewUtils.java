package com.krava.vkmedia.presentation.utils;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.HashMap;

/**
 * Created by krava2008 on 15.05.16.
 */
public class ViewUtils {
    private static Handler handler;
    private static HashMap<View, ObjectAnimator> visibilityAnims;

    public static void invalidate(@Nullable View view) {
        if (view != null) {
            view.invalidate();
        }
    }
}
