package com.example.mtktools;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String NOTIFICATION_LIGHT = "notification_light_pulse";
    public static final String LOG_TAG = "MTKTOOLS";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TabHost tabs = (TabHost)findViewById(R.id.tabHost);
        tabs.setup();
        TabHost.TabSpec spec = tabs.newTabSpec("tag1");
        spec.setContent(R.id.linearLayout);
        spec.setIndicator(getString(R.string.tab1_title));
        tabs.addTab(spec);
        spec = tabs.newTabSpec("tag2");
        spec.setContent(R.id.linearLayout2);
        spec.setIndicator(getString(R.string.tab2_title));
        tabs.addTab(spec);
        tabs.setCurrentTab(0);

        Switch sw = (Switch) findViewById(R.id.swNotificationLight);
        if (sw != null) {
            try {
                sw.setChecked(Settings.System.getInt(getContentResolver(), NOTIFICATION_LIGHT) != 0);
            } catch (Settings.SettingNotFoundException e) {
                sw.setEnabled(false);
            }
        }
    }
    public void onSwitchClick(View view) {
        if (((Switch) view).isChecked()) {
            Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT, 1);
            Toast.makeText(view.getContext(), getString(R.string.notification_light_pulse) + " " + getString(R.string.on), Toast.LENGTH_LONG).show();
        } else {
            Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT, 0);
            Toast.makeText(view.getContext(), getString(R.string.notification_light_pulse) + " " + getString(R.string.off), Toast.LENGTH_LONG).show();
        }

    }
    public void onButtonClick(View view) {
        ContentValues values = new ContentValues();
        String uri = ((EditText) findViewById(R.id.etEPO)).getText().toString().concat("/EPO.DAT");
        values.put("uri", uri);
        values.put("notificationpackage", getPackageName());
        //values.put("notificationclass", DownloadReceiver.class.getCanonicalName());
        Log.d(LOG_TAG, DownloadReceiver.class.getCanonicalName());
        values.put("is_public_api", true);
        values.put("allowed_network_types", ~0);
        values.put("visibility", 1);
        values.put("hint", "file://" + Environment.getExternalStorageDirectory().toString() + "/Download/EPO.DAT");
        values.put("destination", 4);
        try {
            Uri content = getContentResolver().insert(Uri.parse("content://downloads/my_downloads"), values);
            Log.d(LOG_TAG, content.toString());
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

}
