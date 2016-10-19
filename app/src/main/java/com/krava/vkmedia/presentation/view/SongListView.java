package com.krava.vkmedia.presentation.view;

import com.arellomobile.mvp.MvpView;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKList;

/**
 * Created by krava2008 on 19.10.16.
 */

public interface SongListView extends MvpView {

    void initSongList(VKList<VKApiAudio> songs);
    void addSongs(VKList<VKApiAudio> songs);
    void onError();
}
