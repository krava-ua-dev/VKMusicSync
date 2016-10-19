package com.krava.vkmedia.presentation;

import android.app.Application;
import android.content.Context;

import com.vk.sdk.VKSdk;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by krava2008 on 21.06.16.
 */

public class VKApplication extends Application {
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        RealmConfiguration config = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(config);
        Global.setApplicationContext(context);
        VKSdk.initialize(getApplicationContext());
    }
}
