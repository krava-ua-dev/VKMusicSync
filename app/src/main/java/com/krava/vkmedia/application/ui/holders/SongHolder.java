package com.krava.vkmedia.application.ui.holders;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.krava.vkmedia.R;
import com.krava.vkmedia.application.Global;
import com.krava.vkmedia.application.VKApplication;
import com.krava.vkmedia.application.ui.drawable.AudioVisualizerDrawable;
import com.krava.vkmedia.application.ui.service.AudioPlayerService;
import com.krava.vkmedia.databinding.SongListItemBinding;
import com.vk.sdk.api.model.VKApiAudio;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by krava2008 on 21.06.16.
 */

public class SongHolder extends RecyclerView.ViewHolder {
    private SongListItemBinding binding;
    private boolean isRunningVisualizer = false;

    public SongHolder(View v) {
        super(v);

        this.binding = DataBindingUtil.bind(v);
    }

    public void setSong(VKApiAudio song) {
        boolean pt;
        this.binding.setSong(song);

        String time = "";
        int hours = song.duration / 3600;
        int minutes = (song.duration % 3600) / 60;
        int seconds = song.duration % 60;

        if (hours > 0) {
            time += Integer.toString(hours) + ":";
        }
        if (minutes < 10 && hours > 0) {
            time += "0";
        }
        time += Integer.toString(minutes) + ":";
        if (seconds < 10) {
            time += "0";
        }
        time += Integer.toString(seconds);
        binding.duration.setText(time);

        boolean isCurrent;
        isCurrent = AudioPlayerService.sharedInstance != null
                && AudioPlayerService.sharedInstance.currentSong != null
                && AudioPlayerService.sharedInstance.currentSong.id == song.id;
        binding.actionIcon.setVisibility(isCurrent ? VISIBLE : GONE);

        pt = isCurrent && AudioPlayerService.sharedInstance.isPlaying();
        if(pt){
            if(!isRunningVisualizer) {
                binding.actionIcon.resume(true);
                isRunningVisualizer = true;
            }
        }else {
            isRunningVisualizer = false;
            binding.actionIcon.stop(true);
        }
        Drawable hasCacheDrawable = VectorDrawableCompat.create(
                VKApplication.context.getResources(),
                hasCache(song) ? R.drawable.cloud_download_primary : R.drawable.cloud_download_gray,
                null);
        binding.download.setImageDrawable(hasCacheDrawable);
    }

    private boolean hasCache(VKApiAudio song){
        return  song.cachePath != null && !song.cachePath.equals("");
    }

    public void setOnSongClickListener(View.OnClickListener onClickListener){
        this.binding.getRoot().setOnClickListener(onClickListener);
    }
}
