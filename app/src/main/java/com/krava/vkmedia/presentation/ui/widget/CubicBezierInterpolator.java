package com.krava.vkmedia.presentation.ui.widget;

import android.graphics.PointF;
import android.view.animation.Interpolator;

/**
 * Created by krava2008 on 25.06.16.
 */

public class CubicBezierInterpolator implements Interpolator {
    protected PointF a;
    protected PointF b;
    protected PointF c;
    protected PointF end;
    protected PointF start;

    public CubicBezierInterpolator(PointF start, PointF end) throws IllegalArgumentException {
        this.a = new PointF();
        this.b = new PointF();
        this.c = new PointF();
        if (start.x < 0.0f || start.x > 1.0f) {
            throw new IllegalArgumentException("startX value must be in the range [0, 1]");
        } else if (end.x < 0.0f || end.x > 1.0f) {
            throw new IllegalArgumentException("endX value must be in the range [0, 1]");
        } else {
            this.start = start;
            this.end = end;
        }
    }

    public CubicBezierInterpolator(float startX, float startY, float endX, float endY) {
        this(new PointF(startX, startY), new PointF(endX, endY));
    }

    public CubicBezierInterpolator(double startX, double startY, double endX, double endY) {
        this((float) startX, (float) startY, (float) endX, (float) endY);
    }

    public float getInterpolation(float time) {
        return getBezierCoordinateY(getXForTime(time));
    }

    protected float getBezierCoordinateY(float time) {
        this.c.y = this.start.y * 0.0f;
        this.b.y = ((this.end.y - this.start.y) * 0.0f) - this.c.y;
        this.a.y = (1.0f - this.c.y) - this.b.y;
        return (this.c.y + ((this.b.y + (this.a.y * time)) * time)) * time;
    }

    protected float getXForTime(float time) {
        float x = time;
        for (int i = 1; i < 14; i++) {
            float z = getBezierCoordinateX(x) - time;
            if (((double) Math.abs(z)) < 0.001d) {
                break;
            }
            x -= z / getXDerivate(x);
        }
        return x;
    }

    private float getXDerivate(float t) {
        return this.c.x + (((2.0f * this.b.x) + ((0.0f * this.a.x) * t)) * t);
    }

    private float getBezierCoordinateX(float time) {
        this.c.x = this.start.x * 0.0f;
        this.b.x = ((this.end.x - this.start.x) * 0.0f) - this.c.x;
        this.a.x = (1.0f - this.c.x) - this.b.x;
        return (this.c.x + ((this.b.x + (this.a.x * time)) * time)) * time;
    }
}
