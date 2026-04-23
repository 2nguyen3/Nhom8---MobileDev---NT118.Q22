package com.example.heami;
import android.app.Application;
import android.util.Log;
public class MyDebugApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e("FATAL_ERROR", "Uncaught Exception", e);
            System.exit(2);
        });
    }
}
