package com.krava.vkmedia.presentation.ui.drawable;

import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.krava.vkmedia.R;
import com.krava.vkmedia.presentation.Global;
import com.krava.vkmedia.presentation.VKApplication;

import java.util.Random;

import static android.R.attr.state_selected;

/**
 * Created by krava2008 on 26.06.16.
 */

public class AudioVisualizerDrawable extends Drawable {
    private static final long UPDATE_INTVL = 150;
    private AccelerateInterpolator accelerate;
    private DecelerateInterpolator decelerate;
    private TimeInterpolator[] interpolators;
    private boolean isAnimating;
    private float[] nextVals;
    private Paint paint;
    private float[] prevVals;
    private Random random;
    private long startTime;

    public AudioVisualizerDrawable() {
        this.prevVals = new float[]{0.0f, 0.0f, 0.0f};
        this.nextVals = new float[]{0.0f, 0.0f, 0.0f};
        this.interpolators = new TimeInterpolator[]{null, null, null};
        this.decelerate = new DecelerateInterpolator();
        this.accelerate = new AccelerateInterpolator();
        this.startTime = 0;
        this.random = new Random();
        this.paint = new Paint();
        this.paint.setColor(ContextCompat.getColor(VKApplication.context, R.color.colorPrimary));
    }

    public void draw(Canvas canvas) {
        int i;
        int h = getBounds().height();
        canvas.translate((float) getBounds().left, (float) getBounds().top);
        if (System.currentTimeMillis() - this.startTime > UPDATE_INTVL) {
            this.startTime = System.currentTimeMillis();
            for (i = 0; i < 3; i++) {
                this.prevVals[i] = this.nextVals[i];
                if (this.isAnimating) {
                    this.nextVals[i] = (float) (Math.cbrt((double) this.random.nextInt(1000000)) / 100.0d);
                } else {
                    this.nextVals[i] = 0.0f;
                }
                TimeInterpolator[] timeInterpolatorArr = this.interpolators;
                if (this.nextVals[i] > this.prevVals[i]) {
                    timeInterpolatorArr[i] = this.accelerate;
                } else {
                    timeInterpolatorArr[i] = this.decelerate;
                }
            }
        }
        float t = ((float) (System.currentTimeMillis() - this.startTime)) / 150.0f;
        float s = 0.0f;
        for (i = 0; i < 3; i++) {
            s += this.prevVals[i];
            Canvas canvas2 = canvas;
            canvas2.drawRect((float) (Global.dp(6.0f) * i), (float) (h - Math.max((int) Global.displayDensity, Math.round((this.prevVals[i] + ((this.nextVals[i] - this.prevVals[i]) * this.interpolators[i].getInterpolation(t))) * ((float) h)))), (float) ((Global.dp(6.0f) * i) + Global.dp(4.0f)), (float) h, this.paint);
        }
        canvas.translate((float) (-getBounds().left), (float) (-getBounds().top));
        if (s != 0.0f || this.isAnimating) {
            invalidateSelf();
        }
    }

    public AudioVisualizerDrawable setColor(int color) {
        this.paint.setColor(color);
        return this;
    }

    public void setAlpha(int alpha) {
    }

    public void setColorFilter(ColorFilter colorFilter) {
    }

    public int getOpacity() {
        return 0;
    }

    public boolean isStateful() {
        return true;
    }

    protected boolean onStateChange(int[] state) {
        this.isAnimating = false;
        for (int s : state) {
            boolean i;
            boolean z = this.isAnimating;
            i = s == state_selected;

            this.isAnimating = i | z;
        }
        return true;
    }
}
