package com.krava.vkmedia.presentation.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by krava2008 on 13.06.16.
 */

public class XLinearLayout extends LinearLayout {
    private OnKeyboardStateChangeListener listener;
    private int prevh;
    private int prevw;

    public interface OnKeyboardStateChangeListener {
        void onKeyboardStateChanged(boolean z);
    }

    public XLinearLayout(Context context) {
        super(context);
        this.prevw = -1;
        this.prevh = -1;
        this.listener = null;
    }

    public XLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.prevw = -1;
        this.prevh = -1;
        this.listener = null;
    }

    public void onMeasure(int wm, int hm) {
        if (isInEditMode()) {
            super.onMeasure(wm, hm);
            return;
        }
        int w = View.MeasureSpec.getSize(wm);
        int h = View.MeasureSpec.getSize(hm);
        if (!(this.prevh == -1 || this.prevw == -1 || this.prevw != w)) {
            Rect rect = new Rect();
            ((Activity) getContext()).getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            int diff = (((Activity) getContext()).getWindowManager().getDefaultDisplay().getHeight() - rect.top) - h;
            if (this.listener != null) {
                this.listener.onKeyboardStateChanged(diff >= prevh);
            }
        }
        this.prevw = w;
        this.prevh = h;
        super.onMeasure(wm, hm);
    }

    public void setOnKeyboardStateListener(OnKeyboardStateChangeListener listener) {
        this.listener = listener;
    }
}