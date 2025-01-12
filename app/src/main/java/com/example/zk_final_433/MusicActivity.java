package com.example.zk_final_433;

import android.app.Application;
import android.content.Intent;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.Lifecycle;

public class MusicActivity extends Application implements LifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {
        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {
        Intent musicIntent = new Intent(this, MusicService.class);
        stopService(musicIntent);
    }
}
