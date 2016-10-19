package com.krava.vkmedia.presentation.ui.adapters;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.krava.vkmedia.R;
import com.krava.vkmedia.presentation.view.fragments.SongListFragment;
import com.krava.vkmedia.presentation.ui.holders.ProgressHolder;
import com.krava.vkmedia.presentation.ui.holders.SongHolder;
import com.krava.vkmedia.databinding.SongListItemBinding;
import com.krava.vkmedia.domain.DataManager;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKList;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by krava2008 on 21.06.16.
 */

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private final int VIEW_PROGRESS = 1;
    private final int VIEW_SONG = 0;
    private VKList<VKApiAudio> audios;
    private HashMap<Integer, Integer> ids;
    private boolean showProgress = false;
    private SongListFragment.OnSongClickListener songClickListener;
    private int lastVisualizerItem = -1;

    @SuppressLint("UseSparseArrays")
    public SongAdapter(){
        this.ids = new HashMap<>();
        this.audios = new VKList<>();
    }

    public void itemCached(int songId, String cachePath){
        VKApiAudio song = audios.get(lastVisualizerItem);
        if(song.id == songId){
            song.cachePath = cachePath;
            notifyItemChanged(lastVisualizerItem);
        }
    }

    public ArrayList<VKApiAudio> getList(){
        return new ArrayList<>(this.audios);
    }

    public void setSongClickListener(SongListFragment.OnSongClickListener songClickListener) {
        this.songClickListener = songClickListener;
    }

    public void setAudios(VKList<VKApiAudio> audios){
        this.audios.clear();
        this.ids.clear();
        for(VKApiAudio song : audios){
            this.ids.put(song.id, 0);
            if(song.cachePath == null || song.cachePath.equals("")) {
                song.cachePath = DataManager.getInstance().getSongCache(song.id);
            }
            Log.e("ListAdapter", String.format("song: %s - %s\ncache: %s", song.artist, song.title, song.cachePath));
            this.audios.add(song);
        }
        this.showProgress = this.audios.getCount() > this.audios.size();
        this.notifyDataSetChanged();
    }

    public void addAudioList(VKList<VKApiAudio> audios) {
        int addedCount = 0;
        for(VKApiAudio song : audios){
            if(!ids.containsKey(song.id)){
                if(song.cachePath == null || song.cachePath.equals("")) {
                    song.cachePath = DataManager.getInstance().getSongCache(song.id);
                }
                this.ids.put(song.id, 0);
                this.audios.add(song);
                addedCount++;
            }
        }
        this.showProgress = this.audios.getCount() > this.audios.size();
        this.notifyItemRangeInserted(this.audios.size() - addedCount, addedCount);
    }

    public int getCount(){
        return this.audios.getCount();
    }

    public int getSize(){
        return this.audios.size();
    }

    public void invalidateLastVisualizer(){
        if(lastVisualizerItem != -1){
            notifyItemChanged(lastVisualizerItem);
        }
    }

    public void invalidatePlayState(int song_id){
        for(int i = 0; i < getSize(); i++){
            if(audios.get(i).id == song_id){
                notifyItemChanged(i);
                if(lastVisualizerItem != -1 && lastVisualizerItem != song_id){
                    notifyItemChanged(lastVisualizerItem);
                }
                Log.e("SongAdapter", "lastVisualizerItem: " + Integer.toString(lastVisualizerItem));
                lastVisualizerItem = i;
                break;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if(viewType == VIEW_SONG) {
            SongListItemBinding itemBinding = SongListItemBinding.inflate(inflater, parent, false);
            return new SongHolder(itemBinding.getRoot());
        }else{
            View v = inflater.inflate(R.layout.progress_list_item, parent, false);
            v.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ProgressHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if(position < this.audios.size()){
            ((SongHolder)holder).setSong(this.audios.get(position));
            ((SongHolder)holder).setOnSongClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(songClickListener != null){
                        songClickListener.onClick(audios.get(holder.getAdapterPosition()));
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position > this.audios.size()-1 ? VIEW_PROGRESS : VIEW_SONG;
    }

    @Override
    public int getItemCount() {
        return showProgress ? this.audios.size() + 1 : this.audios.size();
    }
}
