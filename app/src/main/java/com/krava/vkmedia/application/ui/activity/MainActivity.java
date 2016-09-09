package com.krava.vkmedia.application.ui.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.krava.vkmedia.R;
import com.krava.vkmedia.application.VKApplication;
import com.krava.vkmedia.application.ui.fragments.FriendsFragment;
import com.krava.vkmedia.application.ui.fragments.SongListFragment;
import com.krava.vkmedia.application.ui.service.AudioPlayerService;
import com.krava.vkmedia.application.ui.widget.CircleTransform;
import com.krava.vkmedia.databinding.ActivityMainBinding;
import com.krava.vkmedia.domain.DataManager;
import com.squareup.picasso.Picasso;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SongListFragment songsFragment;
    private ActionBarDrawerToggle drawerToggle;
    private VKApiUser myself;
    private ArrayList translatedViews;
    public boolean isPlayerShowing = false;
    private static final int PLAYER_DX;
    public View playerBar;
    static {
        PLAYER_DX = VKApplication.context.getResources().getDimensionPixelSize(R.dimen.mini_player);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case AudioPlayerService.ACTION_BUFFERING_UPDATE:
                    if(intent.hasExtra("percent")) {
                        ((RoundCornerProgressBar)findViewById(R.id.mini_player_progress)).setSecondaryProgress(intent.getIntExtra("percent", 0));
                    }
                    Log.e("Receiver", "buff: " + Integer.toString(intent.getIntExtra("percent", 0)));
                    break;
                case AudioPlayerService.ACTION_PLAYING_PROGRESS:
                    if(intent.hasExtra("percent") && intent.hasExtra("position")) {
                        ((RoundCornerProgressBar)findViewById(R.id.mini_player_progress)).setProgress(intent.getIntExtra("percent", 0));
                        Log.e("Receiver", "progress: " + Integer.toString(intent.getIntExtra("percent", 0)));
                        String time = "";
                        int duration = intent.getIntExtra("all_time", 0) - intent.getIntExtra("position", 0);

                        int hours = duration / 3600;
                        int minutes = (duration % 3600)/60;
                        int seconds = duration % 60;

                        if(hours > 0){
                            time += Integer.toString(hours) + ":";
                        }
                        if(minutes < 10 && hours > 0) {
                            time += "0";
                        }
                        time += Integer.toString(minutes) + ":";
                        if(seconds < 10){
                            time += "0";
                        }
                        time += Integer.toString(seconds);
                        ((TextView)findViewById(R.id.mp_duration)).setText(time);
                    }
                    break;
                case AudioPlayerService.ACTION_SERVICE_STOPPING:
                    if(isPlayerShowing){
                        isPlayerShowing = false;
                        showPlayerAnim();
                    }
                    return;
                case AudioPlayerService.ACTION_PAUSE:
                    ((ImageView)findViewById(R.id.song_photo)).setImageResource(R.drawable.ic_attach_audio_play);
                    break;
                case AudioPlayerService.ACTION_PLAY:
                    ((ImageView)findViewById(R.id.song_photo)).setImageResource(R.drawable.ic_attach_audio_pause);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(binding.toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
        this.songsFragment = (SongListFragment)getSupportFragmentManager().findFragmentByTag("my_list");
        if(songsFragment == null) {
            this.songsFragment = new SongListFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_wrapper, songsFragment, "my_list")
                    .commit();
        }
        this.binding.downloadsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DownloadsActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.song_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent playPauseIntent = new Intent(MainActivity.this, AudioPlayerService.class);
                playPauseIntent.putExtra("audio_service_action", AudioPlayerService.ACTION_PLAY_PAUSE);
                startService(playPauseIntent);
            }
        });
        drawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.app_name, R.string.app_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        findViewById(R.id.mini_player).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent player = new Intent(MainActivity.this, ActivityAudioPlayer.class);
                startActivity(player);
                overridePendingTransition(R.anim.player_slide_in, R.anim.player_slide_out);
            }
        });
        this.translatedViews = getTranslatedView();
        myself = DataManager.getInstance().getMyself();
        if(myself.id == -1){
            VKApi.users().get(VKParameters.from("fields", "photo_100")).executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);

                    myself = ((VKList<VKApiUser>)response.parsedModel).get(0);
                    DataManager.getInstance().saveMyself(myself);
                    initDrawerHeader();
                }
            });
        }else{
            initDrawerHeader();
        }
        initDrawerClicks();
    }

    private void initDrawerClicks(){
        findViewById(R.id.my_list).setOnClickListener(onDrawerItemClick);
        findViewById(R.id.cached).setOnClickListener(onDrawerItemClick);
        findViewById(R.id.friends).setOnClickListener(onDrawerItemClick);
        findViewById(R.id.settings).setOnClickListener(onDrawerItemClick);
        findViewById(R.id.feedback).setOnClickListener(onDrawerItemClick);
        findViewById(R.id.logout).setOnClickListener(onDrawerItemClick);
    }

    private View.OnClickListener onDrawerItemClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.my_list:
                    if(getSupportFragmentManager().findFragmentByTag("my_list") == null) {
                        SongListFragment mySongsFragment = new SongListFragment();
                        getSupportFragmentManager().beginTransaction()
                                                   .replace(R.id.fragment_wrapper, mySongsFragment, "my_list")
                                                   .commit();
                    }
                    closeDrawer();
                    break;
                case R.id.logout:
                    VKSdk.logout();
                    VKApiUser fake = new VKApiUser();
                    fake.id = -1;
                    DataManager.getInstance().saveMyself(fake);
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    stopService(new Intent(MainActivity.this, AudioPlayerService.class));
                    finish();
                    return;
                case R.id.cached:
                    if(getSupportFragmentManager().findFragmentByTag("cached_list") == null) {
                        SongListFragment cachedFragment = new SongListFragment();
                        Bundle extras = new Bundle();
                        extras.putBoolean("show_cached", true);
                        cachedFragment.setArguments(extras);

                        getSupportFragmentManager().beginTransaction()
                                                   .replace(R.id.fragment_wrapper, cachedFragment, "cached_list")
                                                   .commit();
                    }
                    closeDrawer();
                    break;
                case R.id.friends:
                    if(getSupportFragmentManager().findFragmentByTag("friends_list") == null) {

                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_wrapper, new FriendsFragment(), "friends_list")
                                .commit();
                    }
                    closeDrawer();
                    break;
                case R.id.settings:

                    closeDrawer();
                    break;
                default:
                    break;
            }
        }
    };

    private void closeDrawer(){
        binding.drawerLayout.closeDrawers();
    }

    private void initDrawerHeader(){
        ((TextView)findViewById(R.id.user_name)).setText(myself.toString());
        ((TextView)findViewById(R.id.user_link)).setText(String.format("http://vk.com/id%d", myself.id));
        ImageView avatar = (ImageView)findViewById(R.id.drawer_avatar);
        Picasso.with(avatar.getContext())
                .load(myself.photo_100)
                .placeholder(VectorDrawableCompat.create(getResources(), R.drawable.account_white, null))
                .transform(new CircleTransform())
                .into(avatar);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    public ArrayList getTranslatedView(){
        ArrayList views = new ArrayList();
        views.add(this.binding.downloadsFab);
        views.add(findViewById(R.id.mini_player));
        views.add(findViewById(R.id.player_shadow));

        return views;
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioPlayerService.ACTION_BUFFERING_UPDATE);
        filter.addAction(AudioPlayerService.ACTION_PLAYING_PROGRESS);
        filter.addAction(AudioPlayerService.ACTION_PLAY);
        filter.addAction(AudioPlayerService.ACTION_SERVICE_STOPPING);
        filter.addAction(AudioPlayerService.ACTION_PAUSE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public void showPlayerAnim(){
        if (this.translatedViews == null) {
            return;
        }
        this.playerBar = (View)translatedViews.get(1);
        ArrayList animators = new ArrayList(this.translatedViews.size());
        for (int i = 0; i < this.translatedViews.size(); i++) {
            Object obj = this.translatedViews.get(i);
            String str = "translationY";
            float[] fArr = new float[2];
            fArr[0] = ((View) this.translatedViews.get(i)).getTranslationY();
            fArr[1] = isPlayerShowing ? (float) PLAYER_DX : 0.0f;
            animators.add(ObjectAnimator.ofFloat(obj, str, fArr));
        }
        if(!animators.isEmpty()){
            AnimatorSet animatorSet = new AnimatorSet().setDuration(200);
            animatorSet.playTogether(animators);
            animatorSet.start();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
