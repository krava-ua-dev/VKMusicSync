package com.krava.vkmedia.presentation.view.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.krava.vkmedia.R;
import com.krava.vkmedia.presentation.ui.Dp;
import com.krava.vkmedia.presentation.ui.service.AudioPlayerService;
import com.krava.vkmedia.presentation.ui.widget.CircularSeekBar;
import com.krava.vkmedia.presentation.ui.widget.PlayerPopupWindow;
import com.labo.kaji.relativepopupwindow.RelativePopupWindow;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.model.VKApiAudio;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created by krava2008 on 25.06.16.
 */

public class ActivityAudioPlayer extends AppCompatActivity {
    PlayerPopupWindow popup;
    private CircularSeekBar seekBar;
    private CircleImageView coverView;
    private ImageView playAction;
    boolean seekbarInTracking = false;
    boolean needSeek = false;
    private Bitmap coverPlaceholder;
    private TextView songName;
    private TextView songArtist;
    private TextView duration;
    private TextView currentDuration;
    private ImageView repeatView;
    private ImageView shuffleiew;
    private ImageView addSong;
    private boolean repeat = false;


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            switch (intent.getAction()){
                case AudioPlayerService.ACTION_BUFFERING_UPDATE:
                    if(intent.hasExtra("percent")) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                            seekBar.setSecondaryProgress(intent.getIntExtra("percent", 0));
                        });
                    }
                    break;
                case AudioPlayerService.ACTION_PLAYING_NEW_TRACK:
                    if(intent.hasExtra("song")){
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                            VKApiAudio song = intent.getParcelableExtra("song");
                            songName.setText(song.title);
                            songArtist.setText(song.artist);
                            currentDuration.setText("0:00");
                            String time = "";
                            int hours = song.duration / 3600;
                            int minutes = (song.duration % 3600)/60;
                            int seconds = song.duration % 60;
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
                            duration.setText(time);
                            invalidateAddSong();
                            coverView.setImageBitmap(coverPlaceholder);
                        });
                    }
                    break;
                case AudioPlayerService.ACTION_PLAYING_PROGRESS:
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        if(!seekbarInTracking && intent.hasExtra("percent") && intent.hasExtra("position")) {
                            seekBar.setProgress(intent.getIntExtra("percent", 0));
                            String time = "";
                            int duration1 = intent.getIntExtra("position", 0);
                            int hours = duration1 / 3600;
                            int minutes = (duration1 % 3600)/60;
                            int seconds = duration1 % 60;
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
                            currentDuration.setText(time);
                        }else{
                            Log.e("ACTION_PLAYING_PROGRESS", "no need update progress");
                        }
                    });
                    break;
                case AudioPlayerService.ACTION_SERVICE_STOPPING:
                    stopRotateCover();
                    finish();
                    return;
                case AudioPlayerService.ACTION_PLAYING_COMPLETE:
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        playAction.setImageResource(R.drawable.ic_attach_audio_play);
                        stopRotateCover();
                    });
                    break;
                case AudioPlayerService.ACTION_PAUSE:
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        playAction.setImageResource(R.drawable.ic_attach_audio_play);
                        stopRotateCover();
                    });
                    break;
                case AudioPlayerService.ACTION_PLAY:
                    playAction.setImageResource(R.drawable.ic_attach_audio_pause);
                    new Handler(Looper.getMainLooper()).post(rotateCover);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("ActivityAudioPlayer", "onCreate: " + System.currentTimeMillis());
        setContentView(R.layout.activity_player);


        seekBar = (CircularSeekBar) findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                Log.e("SeekBar", "onProgressChanged fromUser: " + fromUser);
                if(needSeek) {
                    Log.e("Seekbar", "need seek to: " + progress);
                    needSeek = false;
                    Intent playPauseIntent = new Intent(ActivityAudioPlayer.this, AudioPlayerService.class);
                    playPauseIntent.putExtra("audio_service_action", AudioPlayerService.ACTION_SEEK_TO);
                    playPauseIntent.putExtra("value", progress);
                    startService(playPauseIntent);
                }
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                Log.e("Seekbar", "onStopTrackingTouch");
                needSeek = true;
                new Handler(Looper.getMainLooper()).postDelayed(() -> seekbarInTracking = false, 200);
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
                Log.e("Seekbar", "onStartTrackingTouch");
                seekbarInTracking = true;
            }
        });
        coverView = (CircleImageView)findViewById(R.id.album_image);
        decodeCoverPlaceHolder();
        coverView.setImageBitmap(coverPlaceholder);
        playAction = (ImageView)findViewById(R.id.play_action);
        playAction.setOnClickListener(view -> {
            Intent playPauseIntent = new Intent(ActivityAudioPlayer.this, AudioPlayerService.class);
            playPauseIntent.putExtra("audio_service_action", AudioPlayerService.ACTION_PLAY_PAUSE);
            startService(playPauseIntent);
        });
        popup = new PlayerPopupWindow(ActivityAudioPlayer.this);
        songName = (TextView)findViewById(R.id.title);
        songName.setText(AudioPlayerService.sharedInstance.currentSong.title);
        songArtist = (TextView) findViewById(R.id.artist);
        songArtist.setText(AudioPlayerService.sharedInstance.currentSong.artist);
        songName.setSelected(true);
        duration = (TextView)findViewById(R.id.duration);
        currentDuration = (TextView)findViewById(R.id.current_duration);
        setDate();

        if(AudioPlayerService.sharedInstance != null && AudioPlayerService.sharedInstance.isPlaying()){
            playAction.setImageResource(R.drawable.ic_attach_audio_pause);
            new Handler(Looper.getMainLooper()).post(rotateCover);
        }else{
            playAction.setImageResource(R.drawable.ic_attach_audio_play);
        }
//        getSongCoverAsync.execute(AudioPlayerService.sharedInstance.currentSong.url);
        findViewById(R.id.back).setOnClickListener(view -> finish());
        this.repeatView = (ImageView)findViewById(R.id.repeat);
        this.repeatView.setOnClickListener(view -> {
            Intent action = new Intent(ActivityAudioPlayer.this, AudioPlayerService.class);
            action.putExtra("audio_service_action", AudioPlayerService.ACTION_TOGGLE_REPEAT);
            startService(action);
            repeat = !repeat;
            invalidateRepeat();
        });
        this.repeat = AudioPlayerService.sharedInstance.repeat;
        invalidateRepeat();
        findViewById(R.id.shufle).setOnClickListener(view -> {

        });
        findViewById(R.id.next_track).setOnClickListener(v -> {
            Intent nextTrack = new Intent(ActivityAudioPlayer.this, AudioPlayerService.class);
            nextTrack.putExtra("audio_service_action", AudioPlayerService.ACTION_NEXT_TRACK);
            startService(nextTrack);
        });
        findViewById(R.id.prev_track).setOnClickListener(v -> {
            Intent prevTrack = new Intent(ActivityAudioPlayer.this, AudioPlayerService.class);
            prevTrack.putExtra("audio_service_action", AudioPlayerService.ACTION_PREV_TRACK);
            startService(prevTrack);
        });
        addSong = (ImageView)findViewById(R.id.add_song);
        addSong.setOnClickListener(v -> {
            if(AudioPlayerService.sharedInstance.currentSong.owner_id == Integer.valueOf(VKAccessToken.currentToken().userId)){
                Toast.makeText(ActivityAudioPlayer.this, "This song already added to your list.", Toast.LENGTH_SHORT).show();
            }else{
                addSong.setImageDrawable(VectorDrawableCompat.create(getResources(), R.drawable.check_primary, null));
            }
        });
        invalidateAddSong();

        findViewById(R.id.menu_popup).setOnClickListener(v -> {
            if(popup == null) {
                popup = new PlayerPopupWindow(ActivityAudioPlayer.this);
            }
            popup.setOnDismissListener(() -> popup = null);
            popup.showOnAnchor(v, RelativePopupWindow.VerticalPosition.ABOVE, RelativePopupWindow.HorizontalPosition.CENTER);
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioPlayerService.ACTION_BUFFERING_UPDATE);
        filter.addAction(AudioPlayerService.ACTION_PLAYING_PROGRESS);
        filter.addAction(AudioPlayerService.ACTION_PLAY);
        filter.addAction(AudioPlayerService.ACTION_SERVICE_STOPPING);
        filter.addAction(AudioPlayerService.ACTION_PLAYING_COMPLETE);
        filter.addAction(AudioPlayerService.ACTION_PLAYING_NEW_TRACK);
        filter.addAction(AudioPlayerService.ACTION_PAUSE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("ActivityAudioPlayer", "onResume: " + System.currentTimeMillis());
    }

    private void invalidateAddSong(){
        if(AudioPlayerService.sharedInstance.currentSong.owner_id == Integer.valueOf(VKAccessToken.currentToken().userId)){
            addSong.setImageDrawable(VectorDrawableCompat.create(getResources(), R.drawable.check_primary, null));
        }else{
            addSong.setImageDrawable(VectorDrawableCompat.create(getResources(), R.drawable.plus, null));
        }
    }

    private void invalidateRepeat(){
        Drawable repeatIcon = VectorDrawableCompat.create(getResources(),
                repeat ? R.drawable.repeat_active : R.drawable.repeat_normal,
                null);
        this.repeatView.setImageDrawable(repeatIcon);
    }

    private void setDate(){
        String time = "";
        int d = AudioPlayerService.sharedInstance.currentSong.duration;

        int hours = d / 3600;
        int minutes = (d % 3600)/60;
        int seconds = d % 60;
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
        duration.setText(time);
        currentDuration.setText("0:00");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
//        if(getSongCoverAsync.getStatus() != AsyncTask.Status.FINISHED){
//            getSongCoverAsync.cancel(false);
//        }
    }

    private AsyncTask<String, Void, Bitmap> getSongCoverAsync = new AsyncTask<String, Void, Bitmap>() {
        @Override
        protected Bitmap doInBackground(String... strings) {
            FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
            Bitmap cover;
            try {
                mmr.setDataSource(AudioPlayerService.sharedInstance.currentSong.url, new HashMap<String, String>());
                byte[] data = mmr.getEmbeddedPicture();
                cover = BitmapFactory.decodeByteArray(data, 0, data.length);;
            }catch (Exception e){
                Log.e("GetCover", "error: " + e.toString());
                cover = null;
            }finally {
                mmr.release();
            }
            return cover;
        }

        @Override
        protected void onPostExecute(Bitmap cover) {
            super.onPostExecute(cover);

            if(cover != null){
                coverView.setImageBitmap(cover);
            }else{
                coverView.setImageBitmap(coverPlaceholder);
            }
        }
    };

    private RotateAnimation animation;
    private Runnable rotateCover = new Runnable() {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            coverView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    coverView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    coverView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    animation = new RotateAnimation(0.0f, 359.0f, coverView.getPivotX(), coverView.getPivotY());
                    animation.setDuration(1500);
                    animation.setInterpolator(new LinearInterpolator());
                    animation.setRepeatCount(Animation.INFINITE);
                    animation.setRepeatMode(Animation.INFINITE);
                    animation.setFillAfter(true);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            coverView.setLayerType(View.LAYER_TYPE_NONE, null);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    coverView.setAnimation(animation);
                }
            });
        }
    };

    private void stopRotateCover(){
        Animation rotate = coverView.getAnimation();
        if(rotate != null && rotate instanceof RotateAnimation){
            rotate.cancel();
        }
    }

    private void decodeCoverPlaceHolder(){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = Dp.toPx(78);
        options.outHeight = Dp.toPx(78);
        coverPlaceholder = BitmapFactory.decodeResource(getResources(), R.drawable.default_artwork_400x400, options);
    }
}
