package com.krava.vkmedia.presentation.ui.adapters;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.krava.vkmedia.R;
import com.krava.vkmedia.presentation.view.fragments.FriendsFragment;
import com.krava.vkmedia.presentation.ui.widget.CircleTransform;
import com.squareup.picasso.Picasso;
import com.vk.sdk.api.model.VKApiUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krava2008 on 02.09.16.
 */

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    private List<VKApiUser> users;
    private FriendsFragment.OnUserClickListener onUserClick;

    public FriendsAdapter(FriendsFragment.OnUserClickListener clickListener){
        this.onUserClick = clickListener;
        this.users = new ArrayList<>();
    }

    public void addItem(VKApiUser user){
        this.users.add(user);
        notifyItemInserted(users.size()-1);
    }

    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FriendViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.friends_list_item, parent, false));
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(FriendViewHolder holder, int position) {
        VKApiUser user = users.get(position);

        if(user.songsCount == 0) {
            holder.root.setEnabled(false);
        }else {
            holder.root.setEnabled(true);
            holder.root.setOnClickListener(v -> onUserClick.onUserClick(user));
        }
        holder.name.setText(user.toString());
        holder.songCount.setText(user.songsCount == 0 ?
                "audios: -- " :
                String.format("audios: %d", user.songsCount));
        Picasso.with(holder.avatar.getContext())
                .load(user.photo_100)
                .placeholder(R.drawable.placeholder_user_48dp)
                .transform(new CircleTransform())
                .into(holder.avatar);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder{
        final TextView name;
        final TextView songCount;
        final ImageView avatar;
        final View root;

        FriendViewHolder(View v) {
            super(v);

            this.root = v;
            this.name = (TextView)v.findViewById(R.id.user_name);
            this.songCount = (TextView)v.findViewById(R.id.user_song_count);
            this.avatar = (ImageView)v.findViewById(R.id.user_avatar);
        }
    }
}
