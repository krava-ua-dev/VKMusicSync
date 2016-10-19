package com.krava.vkmedia.presentation.view;

import com.arellomobile.mvp.MvpView;
import com.vk.sdk.api.model.VKApiUser;

/**
 * Created by krava2008 on 19.10.16.
 */

public interface MainView extends MvpView {

    void onUserLoaded(VKApiUser user);
}
