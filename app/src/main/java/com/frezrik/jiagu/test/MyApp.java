package com.frezrik.jiagu.test;

import android.content.Context;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

public class MyApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("JIAGU_TEST", "onCreate[Application] ==> " + getApplicationContext().getClass().getName());
    }

    @Override
    public void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        Log.w("JIAGU_TEST", "attachBaseContext[Application]");

        //MultiDex.install(this);
    }

}
