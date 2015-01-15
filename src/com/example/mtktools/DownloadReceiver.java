package com.example.mtktools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, intent.getAction() + " * " + intent.getDataString(), Toast.LENGTH_LONG).show();
        Log.d(MainActivity.LOG_TAG, intent.getAction() + " * " + intent.getDataString());
    }
}
