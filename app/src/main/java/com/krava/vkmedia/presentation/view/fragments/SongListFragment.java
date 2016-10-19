package com.krava.vkmedia.presentation.view.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.arellomobile.mvp.MvpAppCompatFragment;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.krava.vkmedia.R;
import com.krava.vkmedia.data.audio.AudioAlbum;
import com.krava.vkmedia.presentation.VKApplication;
import com.krava.vkmedia.presentation.presenter.SongListPresenter;
import com.krava.vkmedia.presentation.view.SongListView;
import com.krava.vkmedia.presentation.view.activity.MainActivity;
import com.krava.vkmedia.presentation.ui.adapters.SongAdapter;
import com.krava.vkmedia.presentation.ui.adapters.ToolbarSpinnerAdapter;
import com.krava.vkmedia.presentation.ui.service.AudioPlayerService;
import com.krava.vkmedia.databinding.FragmentSongListBinding;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKList;

import static android.view.View.GONE;

/**
 * Created by krava2008 on 21.06.16.
 */

public class SongListFragment extends MvpAppCompatFragment implements SongListView {
    @InjectPresenter
    SongListPresenter presenter;

    private FragmentSongListBinding binding;
    private boolean isLoadingHistory = false;
    private SongAdapter adapter;
    private ToolbarSpinnerAdapter albumAdapter;
    private AppCompatSpinner spinner;
    private boolean initingList = false;
    private int ownerId = Integer.valueOf(VKAccessToken.currentToken().userId);

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
        this.binding = FragmentSongListBinding.inflate(inflater);

        Bundle bundle = getArguments();
        if(bundle != null){
            presenter.setType(bundle.getInt("list_type", 0));
            ownerId = bundle.getInt("owner_id", Integer.valueOf(VKAccessToken.currentToken().userId));
            presenter.setOwnerId(ownerId);
        }
        this.spinner = (AppCompatSpinner)getActivity().findViewById(R.id.album_spinner);
        if(ownerId == Integer.valueOf(VKAccessToken.currentToken().userId)){
            initAlbums();
        }
        this.binding.list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        this.adapter = new SongAdapter();
        this.adapter.setSongClickListener(onSongClickListener);
        this.binding.list.setAdapter(this.adapter);
        this.binding.swipe2refresh.setColorSchemeResources(R.color.colorPrimary);
        this.binding.swipe2refresh.setOnRefreshListener(() -> presenter.getSongList(0));
        this.binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if(!presenter.isTypeCached()) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) binding.list.getLayoutManager();
                    int lastItem = layoutManager.findLastVisibleItemPosition();
                    if (initingList && lastItem + 10 >= adapter.getSize() && !isLoadingHistory) {
                        presenter.getSongList(adapter.getItemCount());
                    }
                }
            }
        });
        if(!presenter.isTypeMyList()) {
            ((MainActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
            ((MainActivity)getActivity()).getSupportActionBar().setTitle(presenter.getToolbarTitle());
            spinner.setVisibility(GONE);
        }else {
            if(ownerId == Integer.valueOf(VKAccessToken.currentToken().userId)) {
                ((MainActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
                presenter.loadAlbums();
            }else {
                ((MainActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
                spinner.setVisibility(GONE);
                if(bundle != null) {
                    ((MainActivity)getActivity()).getSupportActionBar().setTitle(bundle.getString("owner_name", ""));
                }
            }
        }
        presenter.getSongList(0);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioPlayerService.ACTION_UPDATE_PLAYING);
        filter.addAction(AudioPlayerService.ACTION_PLAY);
        filter.addAction(AudioPlayerService.ACTION_PAUSE);
        filter.addAction(AudioPlayerService.ACTION_SERVICE_STOPPING);
        filter.addAction(AudioPlayerService.ACTION_SONG_CACHED);

        getActivity().registerReceiver(receiver, filter);

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }

    private void initAlbums(){
        spinner.setVisibility(View.VISIBLE);
        albumAdapter = new ToolbarSpinnerAdapter(getActivity(), spinner, android.R.layout.simple_spinner_item);
        spinner.setAdapter(albumAdapter);
        albumAdapter.addItem(new AudioAlbum(-1, getString(R.string.my_audios)));

        spinner.setSelection(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        spinner.setVisibility(GONE);
        ((MainActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    private OnSongClickListener onSongClickListener = new OnSongClickListener() {
        @Override
        public void onClick(final VKApiAudio song) {
            if(!((MainActivity)getActivity()).isPlayerShowing){
                ((MainActivity)getActivity()).isPlayerShowing = true;
                ((MainActivity)getActivity()).showPlayerAnim();
            }
            View playerBar = ((MainActivity)getActivity()).playerBar;
            ((TextView)playerBar.findViewById(R.id.mp_song_label)).setText(song.artist + " - " + song.title);
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

    @Override
    public void initSongList(VKList<VKApiAudio> songs) {
        adapter.setAudios(songs);
        initingList = true;
        if(binding.swipe2refresh.isRefreshing()) {
            binding.swipe2refresh.setRefreshing(false);
        }
    }

    @Override
    public void addSongs(VKList<VKApiAudio> songs) {
        adapter.addAudioList(songs);
    }

    @Override
    public void albumsLoaded(VKList<AudioAlbum> albums) {
        for(AudioAlbum album : albums) {
            albumAdapter.addItem(album);
        }
    }

    @Override
    public void onError() {
        Toast.makeText(getActivity(), R.string.error_message_unknown, Toast.LENGTH_SHORT).show();
    }


    public interface OnSongClickListener {
        void onClick(VKApiAudio song);
    }
}
