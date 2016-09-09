package com.krava.vkmedia.application.ui.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.krava.vkmedia.R;
import com.krava.vkmedia.application.media.StreamProxy;
import com.krava.vkmedia.application.ui.activity.ActivityAudioPlayer;
import com.krava.vkmedia.domain.DataManager;
import com.vk.sdk.api.model.VKApiAudio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by krava2008 on 25.06.16.
 */

public class AudioPlayerService extends Service implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,  MediaPlayer.OnCompletionListener {
    public static final int ACTION_NEXT_TRACK = 5;
    public static final int ACTION_PAUSE_IF_PLAYING = 7;
    public static final int ACTION_PLAY_IF_PAUSED = 8;
    public static final int ACTION_PLAY_PAUSE = 13;
    public static final int ACTION_SEEK_TO = 14;
    public static final int ACTION_PREV_TRACK = 6;
    public static final int ACTION_NEW_TRACK = 12;
    public static final String ACTION_SERVICE_STOPPING = "com.krava.vkmedia.SERVICE_STOPPING";
    public static final String ACTION_BUFFERING_UPDATE = "com.krava.vkmedia.BUFFERING_UPDATE";
    public static final String ACTION_PLAYING_PROGRESS = "com.krava.vkmedia.PLAYING_PROGRESS";
    public static final String ACTION_PLAYING_COMPLETE = "com.krava.vkmedia.PLAYING_COMPLETE";
    public static final String ACTION_PLAYING_NEW_TRACK = "com.krava.vkmedia.PLAYING_NEW_TRACK";
    public static final String ACTION_SONG_CACHED = "com.krava.vkmedia.SONG_CACHED";
    public static final String ACTION_PLAY = "com.krava.vkmedia.PLAY";
    public static final String ACTION_PAUSE = "com.krava.vkmedia.PAUSE";
    public static final int ACTION_SHOW_PLAYER = 4;
    public static final int ACTION_STOP_SERVICE = 11;
    public static final int ACTION_TOGGLE_REPEAT = 9;
    public static final int ACTION_TOGGLE_SHUFFLE = 10;
    public static final String ACTION_UPDATE_AUDIO_LISTS = "com.krava.vkmedia.UPDATE_AUDIO_LISTS";
    public static final String ACTION_UPDATE_PLAYING = "com.krava.vkmedia.PLAYER_PLAYING";
    public static final String B_ACTION_PLAYER_CONTROL = "com.krava.vkmedia.PLAYER_CONTROL";
    private static final int ID_NOTIFICATION = 303;
    private static final long MIN_SPACE_TO_CACHE = 52428800;
    private static final long PLAYER_RELEASE_DELAY = 60000;
    private static final long SERVICE_STOP_DELAY = 1800000;
    public static final int STATE_INITING = 3;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_STOPPED = 0;
    public static AudioPlayerService sharedInstance;

    private boolean cacheCurrent;
    private ArrayList<VKApiAudio> currentPlaylist;
    private NotificationManager nm;
    private Notification notification;
    private static PendingIntent notificationContentIntent;
    private boolean error;
    private int currentListOwner = 0;
    private boolean haveAudioFocus;
    private boolean headsetPlugState;
    private boolean pausedByCall;
    private boolean pausedBySystem;
    private Timer playerStopTimer;
    private int playlistPosition;
    public boolean random = false;
    public boolean repeat = false;
    private boolean useCustomNotification;
    private ArrayList<VKApiAudio> randomPlaylist;
    private BroadcastReceiver receiver;
    private boolean startAfterCall;
    private MediaPlayer player;
    private String playUrl;
    public VKApiAudio currentSong;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean isPreparing = false;
    private StreamProxy proxy;
    private boolean isBuffered = false;
    private int lastBufferPercent = 0;
    private OnFileCachedListener cachedListener = new OnFileCachedListener() {
        @Override
        public void onComplete(File file) {
            Log.e("OnFileCachedListener", "onComplete: " + file.toString());
            cacheCurrent = true;
            currentSong.cachePath = file.getAbsolutePath();
            DataManager.getInstance().updateSong(currentSong);

            Intent intent = new Intent(ACTION_SONG_CACHED);
            intent.putExtra("song_id", currentSong.id);
            intent.putExtra("cache_path", currentSong.cachePath);
            sendBroadcast(intent);
        }

        @Override
        public void onError() {
            Log.e("OnFileCachedListener", "onError");
        }
    };

    int lastStartId = 0;

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("AUDIO_SERVICE", "onStartCommand");

        if(intent != null) {
            switch (intent.getIntExtra("audio_service_action", -1)) {
                case STATE_PLAYING:
                    Log.e("AUDIO_SERVICE", "STATE_PLAYING");
                    this.currentPlaylist = null;
                    this.currentPlaylist = new ArrayList();
                    this.currentPlaylist.add((VKApiAudio) intent.getParcelableExtra("file"));
                    break;
                case STATE_PAUSED:
                    Log.e("AUDIO_SERVICE", "STATE_POUSED");
                    if (intent.hasExtra("list_al")) {
                        ArrayList<VKApiAudio> files = intent.getParcelableArrayListExtra("list_al");
                        if (intent.getBooleanExtra("force_random", false)) {
                            random = true;
                        }
                        break;
                    }
                    throw new UnsupportedOperationException("list_al is required");
                case ACTION_NEW_TRACK:
                    Log.e("AUDIO_SERVICE", "ACTION_NEW_TRACK");
                    if(intent.hasExtra("song_list")){
                        currentPlaylist = intent.getParcelableArrayListExtra("song_list");
                        currentListOwner = intent.getIntExtra("list_owner", -1);
                    }
                    if (intent.hasExtra("song")) {
                        VKApiAudio song = intent.getParcelableExtra("song");
                        if(currentPlaylist == null || currentPlaylist.size() == 0){
                            playlistPosition = 0;
                            currentPlaylist = new ArrayList<>();
                            currentPlaylist.add(song);
                        }else {
                            calculateSongPosition(song);
                        }
                        newTrack(song);
                    }
                    break;
                case ACTION_SHOW_PLAYER:
                    Log.e("AUDIO_SERVICE", "ACTION_SHOW_PLAYER");
//                showPlayer(z, intent.hasExtra("from_notify"));
                    break;
                case ACTION_NEXT_TRACK:
                    Log.e("AUDIO_SERVICE", "ACTION_NEXT_TRACK");
                    nextTrack();
                    break;
                case ACTION_PREV_TRACK:
                    Log.e("AUDIO_SERVICE", "ACTION_PREV_TRACK");
                    prevTrack();
                    break;
                case ACTION_PLAY_PAUSE:
                    Log.e("AUDIO_SERVICE", "ACTION_PLAY_PAUSE");
                    playPause();
                    break;
                case ACTION_TOGGLE_REPEAT:
                    Log.e("AUDIO_SERVICE", "STATE_TOOGLE_REPEAT");
                    repeat = !repeat;
                    break;
                case ACTION_TOGGLE_SHUFFLE:
                    random = !random;
                    //calculate random list
                    break;
                case ACTION_SEEK_TO:
                    seekTo(intent.getIntExtra("value", 0));
                    break;
                case ACTION_STOP_SERVICE:
                    Log.e("AUDIO_SERVICE", "ACTION_STOP_SERVICE");
                    if(player != null){
                        player.release();
                        player = null;
                    }
                    if(proxy != null){
                        proxy.stop();
                    }
                    currentSong = null;
                    stopForeground(true);
                    nm.cancel(ID_NOTIFICATION);
                    sendBroadcast(new Intent(ACTION_SERVICE_STOPPING));
                    stopSelf(lastStartId);
                    stopSelf();
                    return super.onStartCommand(intent, flags, startId);
                default:
                    Log.e("AUDIO_SERVICE", "deff: " + intent.getIntExtra("audio_service_action", -1));
                    break;
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(B_ACTION_PLAYER_CONTROL);
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.media.ACTION_SCO_AUDIO_STATE_UPDATED");
        filter.addAction("android.media.AUDIO_BECOMING_NOISY");
        registerReceiver(this.receiver, filter);

        lastStartId = startId;

        return START_STICKY;
    }

    private void calculateSongPosition(VKApiAudio song){
        List<VKApiAudio> temp = random ? randomPlaylist : currentPlaylist;
        for(int i = 0; i < temp.size(); i++){
            if(song.id == temp.get(i).id){
                playlistPosition = i;
                break;
            }
        }
    }

    private void playPause(){
        Intent intent;
        if(player.isPlaying()){
            player.pause();
            intent = new Intent(ACTION_PAUSE);
        }else{
            player.start();
            intent = new Intent(ACTION_PLAY);
        }
        intent.putExtra("song_id", currentSong.id);
        sendBroadcast(intent);
    }

    private void seekTo(int i){
        this.player.seekTo(player.getDuration() * i / 100);
    }

    private void newTrack(final VKApiAudio song){
        synchronized (this) {
            Log.e("AUDIO_SERVICE", "current position: " + playlistPosition);
            if(currentSong != null && currentSong.id == song.id){
                Log.e("AUDIO_SERVICE", String.format("newId: %d , oldId: %d", song.id, currentSong.id));
                playPause();
            }else {
                this.currentSong = song;

                Intent event = new Intent(ACTION_PLAYING_NEW_TRACK);
                event.putExtra("song", currentSong);
                sendBroadcast(event);

                if (player != null && (player.isLooping() || player.isPlaying())) {
                    player.release();
                    player = null;
                    Log.e("AUDIO_SERVICE", "release player");
                }
                initPlayer();

                final boolean needStream;
                song.cachePath = DataManager.getInstance().getSongCache(song.id);
                if(song.cachePath != null && !song.cachePath.equals("")){
                    Log.e("newTrack", "has chache");
                    needStream = false;
                    playUrl = song.cachePath;
                    if(proxy != null ) {
                        proxy.stop();
                        proxy = null;
                    }
                }else{
                    needStream = true;
                    if(proxy == null || !proxy.isRunning) {
                        proxy = new StreamProxy();
                        proxy.init(cachedListener);
                        proxy.start();
                    }
                }
                Log.e("AUDIO_SERVICE", "need_stream: " + needStream);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        if(needStream) {
                            playUrl = String.format("http://127.0.0.1:%d/%s",
                                    proxy.getPort(), song.url);
                        }
                        try {
                            player.setDataSource(playUrl);
                            player.prepareAsync();
                            isPreparing = true;
                        } catch (Exception ignored) {
                            ignored.printStackTrace();
                        }
                        updateNotification();
                        Intent intent = new Intent(ACTION_PLAY);
                        intent.putExtra("song_id", currentSong.id);
                        sendBroadcast(intent);
                    }
                },500);
            }
        }
    }

    private void initPlayer(){
        this.player = new MediaPlayer();
        this.player.setOnBufferingUpdateListener(this);
        this.player.setOnCompletionListener(this);
        this.player.setOnErrorListener(this);
        this.player.setOnPreparedListener(this);
    }

    private void nextTrack(){
        if(random){
            if(playlistPosition + 1 == randomPlaylist.size()){
                playlistPosition = -1;
            }
            newTrack(randomPlaylist.get(++playlistPosition));
        }else{
            if(playlistPosition + 1 == currentPlaylist.size()){
                playlistPosition = -1;
            }
            newTrack(currentPlaylist.get(++playlistPosition));
        }
    }

    private void prevTrack(){
        if(random){
            if(playlistPosition - 1 < 0){
                playlistPosition = randomPlaylist.size();
            }
            newTrack(randomPlaylist.get(--playlistPosition));
        }else{
            if(playlistPosition - 1 < 0){
                playlistPosition = currentPlaylist.size();
            }
            newTrack(currentPlaylist.get(--playlistPosition));
        }
    }

    private void updateNotification() {
        Runnable r = new Runnable() {

            public void run() {
                useCustomNotification = true;
                if (AudioPlayerService.this.useCustomNotification) {
                    Intent playpause = new Intent(AudioPlayerService.this, AudioPlayerService.class);
                    playpause.setAction("PlayPauseN");
                    playpause.putExtra("audio_service_action", AudioPlayerService.STATE_INITING);
                    playpause.putExtra("from_notify", true);
                    PendingIntent pendingPlaypause = PendingIntent.getService(AudioPlayerService.this, 0, playpause, PendingIntent.FLAG_CANCEL_CURRENT);
                    Intent next = new Intent(AudioPlayerService.this, AudioPlayerService.class);
                    next.setAction("NextN");
                    next.putExtra("audio_service_action", AudioPlayerService.ACTION_NEXT_TRACK);
                    PendingIntent pendingNext = PendingIntent.getService(AudioPlayerService.this, 0, next, PendingIntent.FLAG_CANCEL_CURRENT);
                    Intent prev = new Intent(AudioPlayerService.this, AudioPlayerService.class);
                    prev.setAction("PrevN");
                    prev.putExtra("audio_service_action", AudioPlayerService.ACTION_PREV_TRACK);
                    PendingIntent pendingPrev = PendingIntent.getService(AudioPlayerService.this, 0, prev, PendingIntent.FLAG_CANCEL_CURRENT);
                    Intent stop = new Intent(AudioPlayerService.this, AudioPlayerService.class);
                    stop.setAction("StopN");
                    stop.putExtra("audio_service_action", AudioPlayerService.ACTION_STOP_SERVICE);
                    PendingIntent pendingStop = PendingIntent.getService(AudioPlayerService.this, 0, stop, PendingIntent.FLAG_CANCEL_CURRENT);
                    Intent playerActivity = new Intent(AudioPlayerService.this, ActivityAudioPlayer.class);
                    PendingIntent pendingPlayer = PendingIntent.getActivity(AudioPlayerService.this, 0, playerActivity, PendingIntent.FLAG_UPDATE_CURRENT);

                    RemoteViews views = new RemoteViews(AudioPlayerService.this.getPackageName(), R.layout.player_notification_small);
                    views.setTextViewText(R.id.song_name, AudioPlayerService.this.currentSong.title);
                    views.setTextViewText(R.id.song_artist, AudioPlayerService.this.currentSong.artist);
//                    views.setImageViewResource(R.id.cover, R.drawable.aplayer_cover_placeholder);
                    views.setImageViewResource(R.id.playpause, AudioPlayerService.this.isPlaying() ? R.drawable.ic_attach_audio_pause : R.drawable.ic_attach_audio_play);
                    views.setOnClickPendingIntent(R.id.playpause, pendingPlaypause);
                    views.setOnClickPendingIntent(R.id.next, pendingNext);
                    views.setOnClickPendingIntent(R.id.stop, pendingStop);
                    views.setOnClickPendingIntent(R.id.prev, pendingPrev);
                    views.setOnClickPendingIntent(R.id.notification_root, pendingPlayer);
                    int bgRes = 0;
                    if (Build.VERSION.SDK_INT < 21) {
                        bgRes = Resources.getSystem().getIdentifier("notification_bg", "drawable", "android");
                        Resources.getSystem().getIdentifier("notification_template_icon_bg", "drawable", "android");
                    } else {
                        AudioPlayerService.this.notification.deleteIntent = pendingStop;
                        if (AudioPlayerService.this.isPlaying()) {
                            notification.flags |= 34;
                        } else {
                            notification.flags &= 35;
                        }
                    }
//                    if (AudioPlayerService.this.isFlymeOrMIUI()) {
                        views.setInt(R.id.notification_root, "setBackgroundColor", -1);
//                    } else if (bgRes != 0) {
//                        views.setInt(R.id.notification_root, "setBackgroundResource", bgRes);
//                    }
                    AudioPlayerService.this.notification.contentView = views;
                    if (Build.VERSION.SDK_INT >= 16) {
                        RemoteViews xviews = new RemoteViews(AudioPlayerService.this.getPackageName(), R.layout.notification_player);
                        xviews.setTextViewText(R.id.song_name, AudioPlayerService.this.currentSong.title);
                        xviews.setTextViewText(R.id.song_artist, AudioPlayerService.this.currentSong.artist);
//                        xviews.setImageViewResource(R.id.cover, R.drawable.aplayer_cover_placeholder);
                        xviews.setImageViewResource(R.id.playpause, AudioPlayerService.this.isPlaying() ? R.drawable.ic_attach_audio_pause : R.drawable.ic_attach_audio_play);
                        xviews.setOnClickPendingIntent(R.id.playpause, pendingPlaypause);
                        xviews.setOnClickPendingIntent(R.id.next, pendingNext);
                        xviews.setOnClickPendingIntent(R.id.prev, pendingPrev);
                        xviews.setOnClickPendingIntent(R.id.stop, pendingStop);
                        xviews.setOnClickPendingIntent(R.id.notification_root, pendingPlayer);
                        xviews.setInt(R.id.notification_root, "setBackgroundColor", -1);
//                        if (AudioPlayerService.this.isFlymeOrMIUI()) {
//
//                        } else if (bgRes != 0) {
//                            xviews.setInt(R.id.notification_root, "setBackgroundResource", bgRes);
//                        }
                        AudioPlayerService.this.notification.bigContentView = xviews;
                    }
                }
                if (isPlaying()) {
                    AudioPlayerService.this.startForeground(AudioPlayerService.ID_NOTIFICATION, notification);
                } else {
                    AudioPlayerService.this.stopForeground(false);
                    AudioPlayerService.this.nm.notify(AudioPlayerService.ID_NOTIFICATION, notification);
                }
//                AudioPlayerService.this.acquireWakeLock();
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(r).start();
        } else {
            r.run();
        }
    }

    private boolean isFlymeOrMIUI() {
        if (!new File("/system/framework/flyme-framework.jar").exists()) {
            try {
                if ((getPackageManager().getPackageInfo("com.miui.core", 0).applicationInfo.flags & STATE_PLAYING) != STATE_PLAYING || isNotificationColorLight()) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (isNotificationColorLight()) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isNotificationColorLight() {
        if (Build.VERSION.SDK_INT < 21) {
            return true;
        }
//        int[] iArr = new int[STATE_PLAYING];
//        iArr[0] = 16842904;
//        TypedArray ta = obtainStyledAttributes(16974339, iArr);
//        int color = ta.getColor(0, 0);
//        ta.recycle();
//        if ((((color & 255) + ((color >> ACTION_PLAY_IF_PAUSED) & 255)) + ((color >> 16) & 255)) / STATE_INITING <= R.styleable.Theme_switchPreferenceStyle) {
//            return false;
//        }
        return false;
    }

    public boolean isPlaying(){
        return player != null && (player.isPlaying() || isPreparing);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("AUDIO_SERVICE", "OnCreate");
        sharedInstance = this;
        this.nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        this.notification = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_notify_play).setAutoCancel(false).build();

        Intent intent = new Intent(this, getClass());
        intent.putExtra("action", ACTION_SHOW_PLAYER);
        intent.putExtra("from_notify", true);
        PendingIntent pintent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationContentIntent = pintent;
        this.notification.contentIntent = pintent;
        if (Build.VERSION.SDK_INT >= 16) {
            this.notification.priority = Notification.PRIORITY_MAX;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            this.notification.visibility = 1;
        }
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "vkmedia.audio.player.get_info":
                        Intent info = new Intent("vkmedia.audio.player.get_info");
                        String state = "null";
                        if(player != null){
                            state = player.isPlaying() ? "plaing" : "stoped";
                        }
                        info.putExtra("state", state);
                        if(currentSong != null) info.putExtra("song", currentSong);

                        break;
                    case "android.intent.action.HEADSET_PLUG":

                        break;
                    default:
                        break;
                }
            }
        };
//        if (proxy != null) {
//            proxy.stop();
//            proxy = null;
//        }
        if (proxy == null) {
            proxy = new StreamProxy();
            proxy.init(cachedListener);
            proxy.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(receiver);
        }catch (IllegalArgumentException ignored){}
        if(proxy != null){
            proxy.stop();
            proxy = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if(!isBuffered && lastBufferPercent < percent) {
            if(percent == 100){
                isBuffered = true;
                lastBufferPercent = 0;
            }else{
                lastBufferPercent = percent;
            }
            Intent intent = new Intent(ACTION_BUFFERING_UPDATE);
            intent.putExtra("percent", percent);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        sendBroadcast(new Intent(ACTION_PLAYING_COMPLETE));
        if(repeat){
            newTrack(currentSong);
        }else{
            nextTrack();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPreparing = false;
        mp.start();
        sendBroadcast(new Intent(ACTION_UPDATE_PLAYING));
        this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                monitorHandler.sendMessage(monitorHandler.obtainMessage());
            }
        }, 400, 400, TimeUnit.MILLISECONDS);
    }

    final Handler monitorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if(player != null && player.isPlaying()){
                Intent intent = new Intent(ACTION_PLAYING_PROGRESS);
                int currentPosition = player.getCurrentPosition()/1000;
                intent.putExtra("percent", currentPosition != 0 ? (int)(((float)currentPosition / (float)currentSong.duration) * 100) : 0);
                intent.putExtra("position", currentPosition);
                intent.putExtra("all_time", currentSong.duration);
                sendBroadcast(intent);
            }
        }
    };

    public interface OnFileCachedListener{
        void onComplete(File file);
        void onError();
    }
}
