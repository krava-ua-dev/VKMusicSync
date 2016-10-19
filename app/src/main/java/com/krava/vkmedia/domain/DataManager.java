package com.krava.vkmedia.domain;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.krava.vkmedia.presentation.VKApplication;
import com.krava.vkmedia.data.model.AudioEntity;
import com.krava.vkmedia.data.model.AudioEntityDataMapper;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by krava2008 on 21.06.16.
 */

public class DataManager {
    private static volatile DataManager instance;

    public static DataManager getInstance() {
        DataManager localInstance = instance;
        if (localInstance == null) {
            synchronized (DataManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new DataManager();
                }
            }
        }
        return localInstance;
    }

    public void updateSong(final VKApiAudio song){
        Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if(!existCachedFile(realm, song.id)) {
                    AudioEntityDataMapper.transform(song);
                }
            }
        });
    }

    public String getSongCache(int songId){
        AudioEntity entity = Realm.getDefaultInstance().where(AudioEntity.class)
                .equalTo("id", songId)
                .findFirst();
        if(entity != null){
            return entity.getCachePath();
        }
        return "";
    }

    public VKList<VKApiAudio> getCachedSongs(){
        RealmResults<AudioEntity> results = Realm.getDefaultInstance().where(AudioEntity.class)
                                               .isNotNull("cachePath")
                                               .isNotEmpty("cachePath")
                                               .findAll();
        VKList<VKApiAudio> audios = new VKList<>();
        for(AudioEntity entity : results){
            audios.add(AudioEntityDataMapper.transform(entity));
        }
        return audios;
    }

    public void saveMyself(VKApiUser user){
        SharedPreferences.Editor editor = VKApplication.context.getSharedPreferences("vk_music", Context.MODE_PRIVATE)
                                                               .edit();
        editor.putString("user_name", user.toString());
        editor.putInt("user_id", user.id);
        editor.putString("user_avatar", user.photo_100);

        editor.apply();
    }

    public VKApiUser getMyself(){
        VKApiUser user = new VKApiUser();
        SharedPreferences prefs = VKApplication.context.getSharedPreferences("vk_music", Context.MODE_PRIVATE);
        user.first_name = prefs.getString("user_name", "Name Surname").split(" ")[0];
        user.last_name = prefs.getString("user_name", "Name Surname").split(" ")[1];
        user.id = prefs.getInt("user_id", -1);
        user.photo_100 = prefs.getString("user_avatar", "");

        return user;
    }

    public boolean existCachedFile(int songId){
        AudioEntity entity = Realm.getDefaultInstance().where(AudioEntity.class)
                .equalTo("id", songId)
                .findFirst();
        return entity != null && TextUtils.isEmpty(entity.getCachePath());
    }

    private boolean existCachedFile(Realm realm, int songId){
        AudioEntity entity = realm.where(AudioEntity.class)
                                  .equalTo("id", songId)
                                  .findFirst();
        return entity != null && TextUtils.isEmpty(entity.getCachePath());
    }

    public void deleteSong(final VKApiAudio song){
        Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                AudioEntity entity = realm.where(AudioEntity.class)
                        .equalTo("id", song.id)
                        .findFirst();
                if(entity != null){
                    entity.deleteFromRealm();
                }
            }
        });
    }
}
