package com.krava.vkmedia.presentation.ui.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by krava2008 on 30.06.16.
 */

public class DownloadsService extends Service {
    private ThreadPoolExecutor poolExecutor;

    @Override
    public void onCreate() {
        super.onCreate();

        this.poolExecutor = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                1,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    private void removeDownload(String taskID){
        BlockingQueue<Runnable> queue = this.poolExecutor.getQueue();
        for(Runnable runnable : queue){
            if(((VKDownloadRunnable)runnable).taskId.equals(taskID)){
                queue.remove(runnable);
            }
        }
    }

    private class VKDownloadRunnable implements Runnable {
        String taskId = "";
        private String url;
        private InputStream in;
        private OutputStream out;

        VKDownloadRunnable(String type, int id, String url){
            this.taskId = String.format("$s_%d", type, id);
            this.url = url;
        }

        @Override
        public void run() {
            
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
