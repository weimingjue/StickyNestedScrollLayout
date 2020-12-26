package com.wang.example;

import android.app.Application;

public class MyApplication extends Application {
    private static MyApplication mApp;

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
    }

    public static MyApplication getContext() {
        return mApp;
    }
}
