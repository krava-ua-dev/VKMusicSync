package com.krava.vkmedia.presentation.presenter;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.krava.vkmedia.domain.DataManager;
import com.krava.vkmedia.presentation.view.MainView;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

/**
 * Created by krava2008 on 19.10.16.
 */

@InjectViewState
public class MainPresenter extends MvpPresenter<MainView> {
    private VKRequest request;

    public MainPresenter() {

    }

    public void loadMyself() {
        VKApiUser myself = DataManager.getInstance().getMyself();
        if(myself.id == -1) {
            if(request != null){
                request.cancel();
            }
            request = VKApi.users().get(VKParameters.from("fields", "photo_100"));
            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);

                    VKApiUser user = ((VKList<VKApiUser>)response.parsedModel).get(0);
                    DataManager.getInstance().saveMyself(user);

                    getViewState().onUserLoaded(user);
                }
            });
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if(request != null){
            request.cancel();
        }
    }
}
