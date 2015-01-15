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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends Activity {
    public static final String NOTIFICATION_LIGHT = "notification_light_pulse";
    public static final String LOG_TAG = "MTKTOOLS";
    public static final String MY_DOWNLOADS = "content://downloads/my_downloads";
    public static String[] EPO_FILES = {"/EPO.DAT", "/EPO.MD5"};

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
        for (String epo_file : EPO_FILES) {
            String uri = ((EditText) findViewById(R.id.etEPO)).getText().toString().concat(epo_file);
            ContentValues values = new ContentValues();

            Log.d(LOG_TAG, DownloadReceiver.class.getCanonicalName());
            Log.d(LOG_TAG, Uri.fromFile(getExternalCacheDir()).toString().concat(epo_file));

            try {
                File file = new File(getExternalCacheDir().toString().concat(epo_file));
                if (!file.delete()) Log.e(LOG_TAG, "Can not delete " + file.getName());
                values.put("uri", uri);
                values.put("notificationpackage", getPackageName());
                //values.put("notificationclass", DownloadReceiver.class.getCanonicalName());
                values.put("is_public_api", true);
                values.put("allowed_network_types", ~0);
                values.put("visibility", 1);
                values.put("hint", Uri.fromFile(getExternalCacheDir()).toString().concat(epo_file));
                values.put("destination", 4);
                Uri content = getContentResolver().insert(Uri.parse(MY_DOWNLOADS), values);
                Log.d(LOG_TAG, content.toString());
            } catch (RuntimeException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

    }
    public void onButtonClick1(View view) {
        try {
            getContentResolver().delete(Uri.parse(MY_DOWNLOADS),"is_public_api = true",null);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

}
