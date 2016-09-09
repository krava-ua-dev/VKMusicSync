package com.krava.vkmedia.data.model;

import com.vk.sdk.api.model.VKApiAudio;

import io.realm.Realm;

/**
 * Created by krava2008 on 28.08.16.
 */

public class AudioEntityDataMapper {

    public static AudioEntity transform(VKApiAudio audio){
        AudioEntity entity = new AudioEntity();
        entity.setAccess_key(audio.access_key);
        entity.setAlbum_id(audio.album_id);
        entity.setArtist(audio.artist);
        entity.setCachePath(audio.cachePath);
        entity.setDuration(audio.duration);
        entity.setGenre(audio.genre);
        entity.setId(audio.id);
        entity.setLyrics_id(audio.lyrics_id);
        entity.setOwner_id(audio.owner_id);
        entity.setTitle(audio.title);
        entity.setUrl(audio.url);

        return Realm.getDefaultInstance().copyToRealmOrUpdate(entity);
    }

    public static VKApiAudio transform(AudioEntity entity){
        VKApiAudio audio = new VKApiAudio();
        audio.cachePath = entity.getCachePath();
        audio.access_key = entity.getAccess_key();
        audio.album_id = entity.getAlbum_id();
        audio.artist = entity.getArtist();
        audio.duration = entity.getDuration();
        audio.genre = entity.getGenre();
        audio.id = entity.getId();
        audio.lyrics_id = entity.getLyrics_id();
        audio.owner_id = entity.getOwner_id();
        audio.title = entity.getTitle();
        audio.url = entity.getUrl();

        return audio;
    }
}
