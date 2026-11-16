package com.example.notepad;

import android.app.Application;

import com.example.notepad.util.Prefs;

public class NodePadApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.applyTheme(this);
    }
}
