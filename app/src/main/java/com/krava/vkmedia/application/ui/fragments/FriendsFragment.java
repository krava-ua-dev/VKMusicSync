package com.krava.vkmedia.application.ui.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.krava.vkmedia.R;
import com.krava.vkmedia.application.ui.adapters.FriendsAdapter;
import com.krava.vkmedia.databinding.FragmentSongListBinding;
import com.vk.sdk.api.VKBatchRequest;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by krava2008 on 21.06.16.
 */

public class FriendsFragment extends Fragment {
    private FragmentSongListBinding binding;
    private VKRequest firstRequest;
    private VKBatchRequest getAllUsersSongRequest;
    private FriendsAdapter adapter;
    private boolean isIniting = false;
    private int allCount = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_song_list, container, false);

        binding.list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        adapter = new FriendsAdapter();
        binding.list.setAdapter(adapter);

        getFriendsWithSongs();

        return binding.getRoot();
    }


    private void getCachedFriends(){

    }

    private void getFriendsWithSongs(){
        if(firstRequest != null){
            firstRequest.cancel();
        }
        firstRequest = new VKRequest("execute.getFriendsWithSongsCount", VKParameters.from("offset", 0));
        firstRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                Log.e ("response", response.json.toString());
                allCount = response.json.optJSONObject("response").optInt("all");
                JSONArray array = response.json.optJSONObject("response").optJSONArray("users");
                for(int i = 0; i < array.length(); i++){
                    try {
                        VKApiUser user = new VKApiUser(array.optJSONObject(i));

                        adapter.addItem(user);
                    }catch (JSONException exc){

                    }
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                Log.e("error", error.toString());
            }
        });
    }
}
