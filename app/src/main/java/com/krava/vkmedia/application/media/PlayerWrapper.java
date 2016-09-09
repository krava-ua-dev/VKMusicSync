package com.krava.vkmedia.application.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.TextureView;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;

/**
 * Created by krava2008 on 25.06.16.
 */

public class PlayerWrapper implements Callback {
    public static final int ERROR_CANT_DECODE = 1;
    public static final int ERROR_CODEC_NOT_FOUND = 2;
    public static final int ERROR_FILE_NOT_FOUND = 3;
    public static final int ERROR_NETWORK = -1;
    public static final int ERROR_NOT_PROCESSED = 4;
    public static final int ERROR_UNSUPPORTED_OS = 0;

    private Context context;
    private String dataSource;
    private SurfaceHolder holder;
    private MediaPlayer hwPlayer;
    private boolean hwSurfaceSet;
    private PlayerStateListener listener;
    private int prevBufPercent;
    private int seekTo;
    private boolean surfaceReady;
    private SurfaceTexture surfaceTexture;
    private TextureView textureView;
    private boolean useHWPlayer;


    class TextureListener implements TextureView.SurfaceTextureListener {
        TextureListener() {
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            PlayerWrapper.this.surfaceTexture = surface;
            PlayerWrapper.this.hwPlayer.setSurface(new Surface(surface));
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (PlayerWrapper.this.hwPlayer != null) {
                PlayerWrapper.this.hwPlayer.setSurface(null);
            }
            return true;
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    class AudioOnPreparedListener implements OnPreparedListener {
        AudioOnPreparedListener() {
        }

        public void onPrepared(MediaPlayer mp) {
            PlayerWrapper.this.hwPlayerPrepared();
        }
    }

    /* renamed from: com.vkcoffeelite.android.media.PlayerWrapper.3 */
    class AudioOnBufferingUpdateListener implements OnBufferingUpdateListener {
        AudioOnBufferingUpdateListener() {
        }

        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent - PlayerWrapper.this.prevBufPercent <= 80) {
                if (PlayerWrapper.this.listener != null) {
                    PlayerWrapper.this.listener.onUpdateBuffered(percent);
                }
                PlayerWrapper.this.prevBufPercent = percent;
            }
        }
    }

    class AudioOnCompleteListener implements OnCompletionListener {
        AudioOnCompleteListener() {
        }

        public void onCompletion(MediaPlayer mp) {
            if (PlayerWrapper.this.listener != null) {
                PlayerWrapper.this.listener.onPlaybackCompleted();
            }
        }
    }

    class AudioOnErrorListener implements OnErrorListener {
        AudioOnErrorListener() {
        }

        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e("vk", "VPLAYER ERROR " + what + "; " + extra);
            if (what == PlayerWrapper.ERROR_CANT_DECODE && PlayerWrapper.this.listener != null) {
                PlayerWrapper.this.listener.onError(PlayerWrapper.ERROR_CANT_DECODE);
            }
            return true;
        }
    }

    /* renamed from: com.vkcoffeelite.android.media.PlayerWrapper.6 */
    class AudioOnInfoListener implements OnInfoListener {
        AudioOnInfoListener() {
        }

        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == 701 && PlayerWrapper.this.listener != null) {
                PlayerWrapper.this.listener.onEndOfBuffer();
            }
            if (what == 702 && PlayerWrapper.this.listener != null) {
                PlayerWrapper.this.listener.onPlaybackResumed();
            }
            return true;
        }
    }

    class UpdatePlaybackPosition implements Runnable {
        UpdatePlaybackPosition() {
        }

        public void run() {
            while (PlayerWrapper.this.hwPlayer != null) {
                try {
                    if (PlayerWrapper.this.listener != null) {
                        PlayerWrapper.this.listener.onUpdatePlaybackPosition(PlayerWrapper.this.hwPlayer.getCurrentPosition() / 1000);
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
    }

    public interface PlayerStateListener {
        void onEndOfBuffer();

        void onError(int i);

        void onPlaybackCompleted();

        void onPlaybackResumed();

        void onPlayerReady(int i, int i2);

        void onUpdateBuffered(int i);

        void onUpdatePlaybackPosition(int i);
    }

    public PlayerWrapper(Context _context, SurfaceHolder _holder) {
        this.useHWPlayer = false;
        this.surfaceReady = false;
        this.hwSurfaceSet = false;
        this.prevBufPercent = 0;
        this.seekTo = 0;
        this.context = _context;
        this.holder = _holder;
        this.holder.addCallback(this);
    }

    public PlayerWrapper(Context _context, TextureView tv) {
        this.useHWPlayer = false;
        this.surfaceReady = false;
        this.hwSurfaceSet = false;
        this.prevBufPercent = 0;
        this.seekTo = 0;
        this.context = _context;
        this.textureView = tv;
        tv.setSurfaceTextureListener(new TextureListener());
    }

    public void init(String ds) {
        this.useHWPlayer = true;
        initHWPlayer(ds);
        this.dataSource = ds;
    }

    private void initHWPlayer(String ds) {
        try {
            this.hwPlayer = new MediaPlayer();
            this.hwPlayer.setDataSource(this.context, Uri.parse(ds));
            this.hwPlayer.setOnPreparedListener(new AudioOnPreparedListener());
            this.hwPlayer.setOnBufferingUpdateListener(new AudioOnBufferingUpdateListener());
            this.hwPlayer.setOnCompletionListener(new AudioOnCompleteListener());
            this.hwPlayer.setOnErrorListener(new AudioOnErrorListener());
            this.hwPlayer.setOnInfoListener(new AudioOnInfoListener());
            if (this.textureView != null && this.textureView.isAvailable() && this.surfaceTexture == null) {
                this.hwPlayer.setSurface(new Surface(this.textureView.getSurfaceTexture()));
            }
            this.hwPlayer.prepareAsync();
        } catch (Exception e) {
        }
    }

    public boolean isHardwareAccelerated() {
        return this.useHWPlayer;
    }

    public String getDataSource() {
        return this.dataSource;
    }

    private void hwPlayerPrepared() {
        if (this.listener != null) {
            this.listener.onPlayerReady(this.hwPlayer.getVideoWidth(), this.hwPlayer.getVideoHeight());
        }
        new Thread(new UpdatePlaybackPosition()).start();
        if (this.seekTo > 0) {
            this.hwPlayer.seekTo(this.seekTo);
            this.seekTo = 0;
        }
    }

    public void play() {
        if (this.useHWPlayer) {
            this.hwPlayer.start();
            if (this.surfaceReady) {
                this.hwPlayer.setDisplay(this.holder);
            }
        }
    }

    public void pause() {
        if (this.useHWPlayer) {
            this.hwPlayer.pause();
        }
    }

    public void seek(int sec) {
        if (this.useHWPlayer) {
            this.hwPlayer.seekTo(sec * 1000);
        } else {
//            this.nativePlayer.seek(sec);
        }
    }

    public int getPosition() {
        if (this.useHWPlayer) {
            return this.hwPlayer.getCurrentPosition() / 1000;
        }
        return 0;
//        return this.nativePlayer.getPosition();
    }

    public void stopAndRelease() {
        if (this.useHWPlayer && this.hwPlayer != null) {
            try {
                this.hwPlayer.stop();
            } catch (Exception e) {
            }
            try {
                this.hwPlayer.release();
            } catch (Exception e2) {
            }
            this.hwPlayer = null;
        }
    }

    public void setListener(PlayerStateListener l) {
        this.listener = l;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("vk", "==== SURFACE CHANGED!!");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        this.surfaceReady = true;
        Log.d("vk", "==== SURFACE CREATED!!");
        this.holder = holder;
        if (this.hwPlayer != null) {
            boolean p = false;
            try {
                p = this.hwPlayer.isPlaying();
            } catch (Exception e) {
            }
            if (p) {
                this.seekTo = this.hwPlayer.getCurrentPosition();
                this.hwPlayer.reset();
                initHWPlayer(this.dataSource);
                return;
            }
            this.hwPlayer.setDisplay(holder);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("vk", "==== SURFACE DESTROYED!!");
        this.surfaceReady = false;
        this.holder = null;
        if (this.hwPlayer != null) {
            this.hwPlayer.setDisplay(null);
        }
    }
}
