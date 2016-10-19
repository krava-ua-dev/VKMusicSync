package com.krava.vkmedia.presentation.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.krava.vkmedia.R;
import com.krava.vkmedia.data.audio.AudioAlbum;
import com.vk.sdk.api.model.VKList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krava2008 on 11.09.16.
 */

public class ToolbarSpinnerAdapter extends ArrayAdapter<String> {
    private VKList<AudioAlbum> items;
    private Context context;
    private Spinner spinner;

    public ToolbarSpinnerAdapter(Context context, Spinner spinner, int textViewResourceId) {
        super(context, textViewResourceId);

        this.spinner = spinner;
        this.context = context;
        this.items = new VKList<>();
    }

    public void addItem(AudioAlbum item){
        this.items.add(item);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("ViewHolder") TextView row = (TextView)inflater.inflate(
                R.layout.toolbar_spinner_selected_item, parent, false
        );

        row.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setText(items.get(position).getTitle());
        return row;
    }

    private int measureContentWidth(View view, Drawable background) {

        if (view.getLayoutParams() == null) {
            view.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        view.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED));
        int width = view.getMeasuredWidth();
        if (background != null) {
            Rect mTempRect = new Rect();
            background.getPadding(mTempRect);
            width += mTempRect.left + mTempRect.right;
        }
        return width;
    }


    public TextView getDropDownView(int position, View convertView,
                                    @NonNull ViewGroup parent) {
        TextView row = (TextView)LayoutInflater.from(context).inflate(
                R.layout.toolbar_spinner_list_item, parent, false
        );
        row.setText(items.get(position).getTitle());
        return row;
    }

}
