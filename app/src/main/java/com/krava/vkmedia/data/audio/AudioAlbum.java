package com.krava.vkmedia.data.audio;

import android.annotation.SuppressLint;
import android.os.Parcel;

import com.vk.sdk.api.model.Identifiable;
import com.vk.sdk.api.model.VKApiModel;

/**
 * Created by krava2008 on 19.10.16.
 */

@SuppressLint("ParcelCreator")
public class AudioAlbum extends VKApiModel implements Identifiable {
    private int id;
    private String title;

    public AudioAlbum() {

    }

    public AudioAlbum(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
