package com.harsh.shah.threads.clone;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BaseApplication extends Application {
    private static Context mApplicationContext;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public static Context getContext() {
        return mApplicationContext;
    }

    @Override // android.app.Application
    public void onCreate() {
        mApplicationContext = getApplicationContext();
//        this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
//        Thread.setDefaultUncaughtExceptionHandler((thread, th) -> {
//            Process.killProcess(Process.myPid());
//            System.exit(1);
//        });
        super.onCreate();
    }

}
