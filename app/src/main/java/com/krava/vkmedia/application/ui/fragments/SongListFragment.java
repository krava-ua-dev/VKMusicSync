package com.krava.vkmedia.application.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.krava.vkmedia.R;
import com.krava.vkmedia.application.VKApplication;
import com.krava.vkmedia.application.ui.activity.MainActivity;
import com.krava.vkmedia.application.ui.adapters.SongAdapter;
import com.krava.vkmedia.application.ui.service.AudioPlayerService;
import com.krava.vkmedia.databinding.FragmentSongListBinding;
import com.krava.vkmedia.domain.DataManager;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKList;

/**
 * Created by krava2008 on 21.06.16.
 */

public class SongListFragment extends Fragment {
    private FragmentSongListBinding binding;
    private VKRequest songRequest;
    private boolean isLoadingHistory = false;
    private VKRequest loadHistoryRequest;
    private SongAdapter adapter;


    private boolean initingList = false;
    private boolean showCached;
    private int ownerId = 0;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int songID;
            switch (intent.getAction()){
                case AudioPlayerService.ACTION_PAUSE:
                    songID = intent.getIntExtra("song_id", 0);
                    adapter.invalidatePlayState(songID);
                    break;
                case AudioPlayerService.ACTION_PLAY:
                    songID = intent.getIntExtra("song_id", 0);
                    adapter.invalidatePlayState(songID);
                    break;
                case AudioPlayerService.ACTION_SONG_CACHED:
                    songID = intent.getIntExtra("song_id", 0);
                    String cache = intent.getStringExtra("cache_path");
                    adapter.itemCached(songID, cache);
                    break;
                case AudioPlayerService.ACTION_SERVICE_STOPPING:
                    adapter.invalidateLastVisualizer();
                    break;
                case AudioPlayerService.ACTION_UPDATE_PLAYING:
                    songID = intent.getIntExtra("song_id", 0);
                    adapter.invalidatePlayState(songID);
                    break;
                default:
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_song_list, container, false);

        Bundle bundle = getArguments();
        if(bundle != null){
            showCached = bundle.getBoolean("show_cached", false);
            ownerId = bundle.getInt("owner_id", Integer.valueOf(VKAccessToken.currentToken().userId));
        }
        this.binding.list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        this.adapter = new SongAdapter();
        this.adapter.setSongClickListener(onSongClickListener);
        this.binding.list.setAdapter(this.adapter);
        this.binding.swipe2refresh.setColorSchemeResources(R.color.colorPrimary);
        this.binding.swipe2refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(showCached){
                    getCachedSongs();
                }else {
                    getSongList();
                }
            }
        });
        this.binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if(!showCached) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) binding.list.getLayoutManager();
                    int lastItem = layoutManager.findLastVisibleItemPosition();
                    if (initingList && lastItem + 10 >= adapter.getSize() && !isLoadingHistory) {
                        loadHistory();
                    }
                }
            }
        });
        if (showCached){
            getCachedSongs();
        }else {
            getSongList();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioPlayerService.ACTION_UPDATE_PLAYING);
        filter.addAction(AudioPlayerService.ACTION_PLAY);
        filter.addAction(AudioPlayerService.ACTION_PAUSE);
        filter.addAction(AudioPlayerService.ACTION_SERVICE_STOPPING);
        filter.addAction(AudioPlayerService.ACTION_SONG_CACHED);

        getActivity().registerReceiver(receiver, filter);

        return binding.getRoot();
    }

    private void getSongList(){
        if(songRequest != null){
            songRequest.cancel();
        }
        if(loadHistoryRequest != null && isLoadingHistory){
            loadHistoryRequest.cancel();
        }
        this.songRequest = VKApi.audio().get(VKParameters.from("count", 30));
        this.songRequest.attempts = 3;
        this.songRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                super.onComplete(response);
                Log.e ("response", response.json.toString());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        adapter.setAudios((VKList<VKApiAudio>)response.parsedModel);
                        initingList = true;
                    }
                });

                binding.swipe2refresh.setRefreshing(false);
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                Log.e("error", error.toString());
                binding.swipe2refresh.setRefreshing(false);
            }
        });
    }

    private void getCachedSongs(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                adapter.setAudios(DataManager.getInstance().getCachedSongs());
                initingList = true;
                binding.swipe2refresh.setRefreshing(false);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }

    private void loadHistory(){
        if(!isLoadingHistory){
            isLoadingHistory = true;
            int count = this.adapter.getCount();
            int size = this.adapter.getSize();
            int newCount = 30;
            if(count - size < 30 && count != size){
                newCount = count - size;
            }
            this.loadHistoryRequest = VKApi.audio().get(VKParameters.from("offset", size, "count", newCount));
            this.loadHistoryRequest.attempts = 3;
            this.loadHistoryRequest.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                    isLoadingHistory = false;
                    adapter.addAudioList((VKList<VKApiAudio>) response.parsedModel);
                }

                @Override
                public void onError(VKError error) {
                    super.onError(error);
                    isLoadingHistory = false;
                    Toast.makeText(getActivity(), R.string.error_message_unknown, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private OnSongClickListener onSongClickListener = new OnSongClickListener() {
        @Override
        public void onClick(final VKApiAudio song) {
            if(!((MainActivity)getActivity()).isPlayerShowing){
                ((MainActivity)getActivity()).isPlayerShowing = true;
                ((MainActivity)getActivity()).showPlayerAnim();
            }
            View playerBar = ((MainActivity)getActivity()).playerBar;
            ((TextView)playerBar.findViewById(R.id.song_label)).setText(song.artist + " - " + song.title);
            ((RoundCornerProgressBar)playerBar.findViewById(R.id.mini_player_progress)).setSecondaryProgress(0);
            ((RoundCornerProgressBar)playerBar.findViewById(R.id.mini_player_progress)).setProgress(0);
            ((ImageButton)playerBar.findViewById(R.id.song_photo)).setImageResource(R.drawable.ic_attach_audio_pause);

            Intent playInService = new Intent(VKApplication.context, AudioPlayerService.class);
            playInService.putParcelableArrayListExtra("song_list", adapter.getList());
            playInService.putExtra("song", song);
            playInService.putExtra("list_owner", Integer.valueOf(VKAccessToken.currentToken().userId));
            playInService.putExtra("audio_service_action", AudioPlayerService.ACTION_NEW_TRACK);
            VKApplication.context.startService(playInService);
        }
    };




    public interface OnSongClickListener {
        void onClick(VKApiAudio song);
    }
}
