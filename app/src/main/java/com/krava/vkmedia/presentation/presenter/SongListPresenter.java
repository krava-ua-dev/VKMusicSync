package com.krava.vkmedia.presentation.presenter;


import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.krava.vkmedia.data.audio.AudioAlbum;
import com.krava.vkmedia.domain.DataManager;
import com.krava.vkmedia.presentation.view.SongListView;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKList;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by krava2008 on 19.10.16.
 */

@InjectViewState
public class SongListPresenter extends MvpPresenter<SongListView> {
    private final int TYPE_MY_LIST = 0;
    private final int TYPE_CACHED = 1;
    private final int TYPE_RECOMMENDATION = 2;
    private final int TYPE_POPULAR = 3;

    private VKRequest request;
    private VKRequest requestAlbums;
    private int type = 0;
    private int ownerId = -1;
    private boolean isHistoryLoading = false;

    public SongListPresenter() {

    }

    public void setType(int type) {
        this.type = type;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isTypeMyList() { return type == TYPE_MY_LIST; }

    public boolean isTypeCached() { return type == TYPE_CACHED; }

    public void getSongList(int offset) {
        switch (type){
            case TYPE_MY_LIST:
                loadUserSongs(offset, ownerId);
                break;
            case TYPE_CACHED:
                loadCachedSongs();
                break;
            case TYPE_POPULAR:
                loadPopularSongs(offset);
                break;
            case TYPE_RECOMMENDATION:
                loadRecommendationSongs(offset);
                break;
            default:
                break;
        }
    }

    public String getToolbarTitle(){
        switch (type){
            case TYPE_CACHED:
                return "Cached";
            case TYPE_POPULAR:
                return "Popular";
            case TYPE_RECOMMENDATION:
                return "Recommendation";
            default:
                return "";
        }
    }

    public void loadAlbums(){
        if(requestAlbums != null){
            request.cancel();
        }
        requestAlbums = VKApi.audio().getAlbums();
        requestAlbums.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                if(response.json.optJSONObject("response").has("items")) {
                    VKList<AudioAlbum> albums = new VKList<AudioAlbum>();
                    JSONArray albumsJson = response.json.optJSONObject("response").optJSONArray("items");
                    for(int i = 0; i < albumsJson.length(); i++) {
                        JSONObject albumJson = albumsJson.optJSONObject(i);
                        albums.add(new AudioAlbum(albumJson.optInt("id"), albumJson.optString("title")));
                    }

                    if(albums.size() > 0){
                        getViewState().albumsLoaded(albums);
                    }
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);

                if(error.errorCode != VKError.VK_CANCELED) {
                    getViewState().onError();
                }
            }
        });
    }

    private void loadUserSongs(int offset, int ownerId) {
        if(isHistoryLoading) return;

        if(request != null){
            request.cancel();
        }
        if(offset != 0) {
            isHistoryLoading = true;
        }
        this.request = VKApi.audio().get(VKParameters.from("owner_id", ownerId, "count", 30, "offset", offset));
        this.request.attempts = 3;
        this.request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                super.onComplete(response);
                isHistoryLoading = false;

                if(offset == 0) {
                    getViewState().initSongList((VKList<VKApiAudio>)response.parsedModel);
                }else{
                    getViewState().addSongs((VKList<VKApiAudio>)response.parsedModel);
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                isHistoryLoading = false;

                if(error.errorCode != VKError.VK_CANCELED) {
                    getViewState().onError();
                }
            }
        });
    }

    private void loadCachedSongs() {
        getViewState().initSongList(DataManager.getInstance().getCachedSongs());
    }

    private void loadPopularSongs(int offset) {
        if(isHistoryLoading) return;

        if(request != null) {
            request.cancel();
        }
        request = VKApi.audio().getPopular(VKParameters.from("offset", offset));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                isHistoryLoading = false;

                if(offset == 0) {
                    getViewState().initSongList((VKList<VKApiAudio>)response.parsedModel);
                }else{
                    getViewState().addSongs((VKList<VKApiAudio>)response.parsedModel);
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                isHistoryLoading = false;

                if(error.errorCode != VKError.VK_CANCELED) {
                    getViewState().onError();
                }
            }
        });
    }

    private void loadRecommendationSongs(int offset) {
        if(isHistoryLoading) return;

        if(request != null) {
            request.cancel();
        }
        request = VKApi.audio().getRecommendations(VKParameters.from("user_id", ownerId, "count", 30, "offset", offset));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                isHistoryLoading = false;

                if(offset == 0) {
                    getViewState().initSongList((VKList<VKApiAudio>)response.parsedModel);
                }else{
                    getViewState().addSongs((VKList<VKApiAudio>)response.parsedModel);
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                isHistoryLoading = false;

                if(error.errorCode != VKError.VK_CANCELED) {
                    getViewState().onError();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(request != null) {
            request.cancel();
        }
        if(requestAlbums != null) {
            requestAlbums.cancel();
        }
    }
}
