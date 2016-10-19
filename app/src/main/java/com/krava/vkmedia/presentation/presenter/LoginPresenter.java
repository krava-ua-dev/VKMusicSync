package com.krava.vkmedia.presentation.presenter;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.krava.vkmedia.presentation.view.LoginView;

/**
 * Created by krava2008 on 19.10.16.
 */

@InjectViewState
public class LoginPresenter extends MvpPresenter<LoginView> {

    public LoginPresenter() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
