package com.example.mtktools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(MainActivity.LOG_TAG, intent.getAction() + " * " + intent.getDataString());
    }
}
