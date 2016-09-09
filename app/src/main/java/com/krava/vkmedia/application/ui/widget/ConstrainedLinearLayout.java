package com.krava.vkmedia.application.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by krava2008 on 13.06.16.
 */

public class ConstrainedLinearLayout extends LinearLayout {
    private int maxHeight;
    private int maxWidth;

    public ConstrainedLinearLayout(Context context) {
        super(context);
        this.maxWidth = Integer.MAX_VALUE;
        this.maxHeight = Integer.MAX_VALUE;
    }

    public ConstrainedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.maxWidth = Integer.MAX_VALUE;
        this.maxHeight = Integer.MAX_VALUE;
        applyAttrs(attrs, 0);
    }

    public ConstrainedLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.maxWidth = Integer.MAX_VALUE;
        this.maxHeight = Integer.MAX_VALUE;
        applyAttrs(attrs, defStyle);
    }

    private void applyAttrs(AttributeSet attrs, int defStyle) {
//        if (attrs != null) {
//            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstrainedLinearLayout, defStyle, 0);
//            this.maxWidth = a.getDimensionPixelSize(0, Integer.MAX_VALUE);
//            this.maxHeight = a.getDimensionPixelSize(1, Integer.MAX_VALUE);
//        }
    }

    public void onMeasure(int wms, int hms) {
        if (View.MeasureSpec.getMode(wms) == MeasureSpec.AT_MOST || View.MeasureSpec.getMode(wms) == MeasureSpec.EXACTLY) {
            wms = Math.min(View.MeasureSpec.getSize(wms), this.maxWidth) | View.MeasureSpec.getMode(wms);
        }
        if (View.MeasureSpec.getMode(hms) == MeasureSpec.AT_MOST || View.MeasureSpec.getMode(hms) == MeasureSpec.EXACTLY) {
            hms = Math.min(View.MeasureSpec.getSize(hms), this.maxHeight) | View.MeasureSpec.getMode(hms);
        }
        super.onMeasure(wms, hms);
    }
}
