package com.krava.vkmedia.data.audio;

import android.content.Context;
import android.util.Log;

import com.krava.vkmedia.presentation.VKApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Vector;

/**
 * Created by krava2008 on 30.06.16.
 */

public class AudioCache {
    public static final String ACTION_ALBUM_ART_AVAILABLE = "com.vkmedia.android.ALBUM_ART_AVAILABLE";
    public static final String ACTION_FILE_ADDED = "com.vkmedia.android.AUDIO_FILE_ADDED";
    public static final String ACTION_FILE_DELETED = "com.vkmedia.android.AUDIO_FILE_DELETED";
    private static final int COPY_BUFFER_SIZE = 10240;
    public static final int ID3_MAX_SIZE = 1024000;
    private static final int PROXY_PORT = 48329;
    public static Vector<String> cacheReqs;
    public static Vector<String> cachedIDs;
    private static Context context;
    private static boolean deleteCurrent;
    private static Thread dlPartsThread;
    private static boolean filledIDs;
    private static final long[] retryIntervals;

    static {
        cachedIDs = new Vector();
        cacheReqs = new Vector();
        filledIDs = false;
        retryIntervals = new long[]{1000, 2000, 5000, 10000, 15000};
        deleteCurrent = false;
        context = VKApplication.context;
    }

    public static class Proxy {
        private ServerSocket ss;

        class MainProxyRunnable implements Runnable {
            MainProxyRunnable() { }

            public void run() {
                try {
                    Proxy.this.ss = new ServerSocket();
                    Proxy.this.ss.bind(new InetSocketAddress("127.0.0.1", AudioCache.PROXY_PORT));
                    while (true) {
                        ProxyRunner proxyRunner = new ProxyRunner(Proxy.this.ss.accept());
                        Log.i("vk", "accepted");
                    }
                } catch (Throwable x) {
                    Log.w("vk", x);
                }
            }
        }

        public Proxy(Context _context) {
            AudioCache.context = _context;
        }

        public void start() { new Thread(new MainProxyRunnable()).start(); }

        public void stop() {
            try {
                this.ss.close();
            } catch (Exception ignored) { }
        }
    }

    private static class ProxyRunner implements Runnable {
        InputStream in;
        OutputStream out;
        private int retries;
        Socket socket;

        ProxyRunner(Socket s) {
            this.socket = s;
            try {
                this.in = s.getInputStream();
                this.out = s.getOutputStream();
            } catch (Exception e) {
            }
            Thread thread = new Thread(this);
            thread.setPriority(1);
            thread.start();
        }

        @Override
        public void run() {
            try{
                byte[] buf = new byte[AudioCache.COPY_BUFFER_SIZE];
                while (true) {
                    int read = in.read(buf);
                    if (read <= 0) {
                        break;
                    }

                    //r.startOffset += read;
                }
            }catch (IOException ioExc){

            }
        }
    }

    public static int getFileSize(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.connect();
            int len = conn.getContentLength();
            conn.disconnect();
            return len;
        } catch (Throwable x) {
            Log.w("vk", x);
            return -1;
        }
    }

}
