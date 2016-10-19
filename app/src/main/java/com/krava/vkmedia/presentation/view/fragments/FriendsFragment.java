package com.krava.vkmedia.presentation.view.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.krava.vkmedia.R;
import com.krava.vkmedia.presentation.view.activity.MainActivity;
import com.krava.vkmedia.presentation.ui.adapters.FriendsAdapter;
import com.krava.vkmedia.databinding.FragmentSongListBinding;
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
    private FriendsAdapter adapter;
    private int allCount = 0;
    private boolean isLoadingHistory = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_song_list, container, false);

        ((MainActivity)getActivity()).getSupportActionBar().setTitle("Friends");

        binding.list.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        adapter = new FriendsAdapter(user -> ((MainActivity)getActivity()).showSongListFragment(user));
        binding.list.setAdapter(adapter);
        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) binding.list.getLayoutManager();
                int lastItem = layoutManager.findLastVisibleItemPosition();
                if (lastItem + 10 >= adapter.getItemCount() && adapter.getItemCount() <= allCount && !isLoadingHistory) {
                    getFriendsWithSongs();
                }
            }
        });
        getFriendsWithSongs();

        return binding.getRoot();
    }

    private void getFriendsWithSongs(){
        if(isLoadingHistory) return;

        if(firstRequest != null){
            firstRequest.cancel();
        }
        isLoadingHistory = true;
        firstRequest = new VKRequest("execute.getFriendsWithSongsCount", VKParameters.from("offset", adapter.getItemCount()));
        firstRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                isLoadingHistory = false;
                Log.e ("response", response.json.toString());
                allCount = response.json.optJSONObject("response").optInt("all");
                JSONArray array = response.json.optJSONObject("response").optJSONArray("users");
                for(int i = 0; i < array.length(); i++){
                    try {
                        VKApiUser user = new VKApiUser(array.optJSONObject(i));

                        adapter.addItem(user);
                    }catch (JSONException ignored){}
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                isLoadingHistory = false;
                Log.e("error", error.toString());
            }
        });
    }

    public interface OnUserClickListener {
        void onUserClick(VKApiUser user);
    }
}
