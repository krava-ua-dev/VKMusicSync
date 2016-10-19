package com.krava.vkmedia.presentation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.vk.sdk.VKSdk;

import java.util.HashMap;

/**
 * Created by krava2008 on 11.06.16.
 */

public class Global {
    public static final String ACTION_ACCOUNT_TYPE = "com.vkmedia.account";
    public static float displayDensity;
    public static final int tag_visibility_anim = -1602;


    public static String getMyId(){
        return VKSdk.getAccessToken().userId;
    }

    public static void init(Context context){
        Global.appContext = context;
        displayDensity = context.getResources().getDisplayMetrics().density;
    }

    public static int scale(float paramFloat)
    {
        return Math.round(displayDensity * paramFloat);
    }

    private static Context appContext;
    private static HashMap<View, ObjectAnimator> visibilityAnims;

    static class ShowAnimationListener extends AnimatorListenerAdapter {
        final View view;
        final int visibility;

        ShowAnimationListener(View view, int i) {
            this.view = view;
            this.visibility = i;
        }

        public void onAnimationStart(Animator anim) {
            this.view.setVisibility(this.visibility);
        }

        public void onAnimationEnd(Animator anim) {
            this.view.setVisibility(this.visibility);
            Global.visibilityAnims.remove(this.view);
        }

        public void onAnimationCancel(Animator anim) {
            this.view.setVisibility(this.visibility);
        }
    }


    static class HideAnimationListener extends AnimatorListenerAdapter {
        boolean canceled;
        final View view;
        final int visibility;

        HideAnimationListener(View view, int i) {
            this.view = view;
            this.visibility = i;
            this.canceled = false;
        }

        public void onAnimationStart(Animator anim) {
        }

        public void onAnimationEnd(Animator anim) {
            this.view.setTag(tag_visibility_anim, null);
            Global.visibilityAnims.remove(this.view);
            if (!this.canceled) {
                this.view.setVisibility(this.visibility);
                this.view.setAlpha(1.0f);
            }
        }

        public void onAnimationCancel(Animator anim) {
            this.canceled = true;
        }
    }

    static {
        visibilityAnims = new HashMap();
    }

    public static void setApplicationContext(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    public static int dp(float dp) {
        if (appContext != null) {
            return Math.round(appContext.getResources().getDisplayMetrics().density * dp);
        }
        throw new IllegalStateException("Application context is not set, call V.setApplicationContext() before using these methods");
    }

    public static void setVisibilityAnimated(View view, int visibility) {
        if (view != null) {
            boolean vis;
            boolean viewVis;
            vis = visibility == View.VISIBLE;

            viewVis = (view.getVisibility() == View.VISIBLE && view.getTag(tag_visibility_anim) == null);

            if (vis != viewVis) {
                if (visibilityAnims.containsKey(view)) {
                    ((ObjectAnimator) visibilityAnims.get(view)).cancel();
                    visibilityAnims.remove(view);
                }
                ObjectAnimator anim;
                if (vis) {
                    float alpha;
                    String str = "alpha";
                    float[] fArr = new float[2];
                    if (view.getAlpha() < 1.0f) {
                        alpha = view.getAlpha();
                    } else {
                        alpha = 0.0f;
                    }
                    fArr[0] = alpha;
                    fArr[1] = 1.0f;
                    anim = ObjectAnimator.ofFloat(view, str, fArr);
                    anim.addListener(new ShowAnimationListener(view, visibility));
                    anim.setDuration(300);
                    visibilityAnims.put(view, anim);
                    anim.start();
                    return;
                }
                anim = ObjectAnimator.ofFloat(view, "alpha", new float[]{0.0f});
                anim.addListener(new HideAnimationListener(view, visibility));
                view.setTag(tag_visibility_anim, Boolean.valueOf(true));
                anim.setDuration(300);
                visibilityAnims.put(view, anim);
                anim.start();
            }
        }
    }

    public static void cancelVisibilityAnimation(View view) {
        if (visibilityAnims.containsKey(view)) {
            ((ObjectAnimator) visibilityAnims.get(view)).cancel();
            view.setAlpha(1.0f);
        }
    }

    public static Point getViewOffset(@Nullable View v1, @Nullable View v2) {
        int[] p1 = new int[]{0, 0};
        int[] p2 = new int[]{0, 0};
        if (!(v1 == null || v2 == null)) {
            v1.getLocationOnScreen(p1);
            v2.getLocationOnScreen(p2);
        }
        return new Point(p1[0] - p2[0], p1[1] - p2[1]);
    }

    public static View findClickableChild(ViewGroup viewGroup, int x, int y) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View c = viewGroup.getChildAt(i);
            if (c.getLeft() < x && c.getRight() > x && c.getTop() < y && c.getBottom() > y) {
                if (c.isClickable()) {
                    return c;
                }
                if (c instanceof ViewGroup) {
                    View r = Global.findClickableChild((ViewGroup) c, x - c.getLeft(), y - c.getTop());
                    if (r != null) {
                        return r;
                    }
                } else {
                    continue;
                }
            }
        }
        return null;
    }
}
