package com.sanron.hwrushhelper;

import android.app.Application;
import android.content.Context;

/**
 * @author chenrong
 * @date 2019/11/23
 */
public class App extends Application {

    private static App mApp;

    public static Context getInstance() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
    }
}
