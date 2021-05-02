package com.reddragon.customcachesample;

import android.app.Application;

import timber.log.Timber;

public class CustomCacheApp extends Application {
    private static CustomCacheApp app;
    public static CustomCacheApp getInstance() {
        return app;
    }
    public void onCreate() {
        super.onCreate();
        app = this;
        Timber.plant(new SampleDebugTree());

    }
}
