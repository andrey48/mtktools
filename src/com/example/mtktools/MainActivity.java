package com.example.mtktools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import java.io.*;
import java.lang.Process;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

public class MainActivity extends Activity {
    static final String NOTIFICATION_LIGHT = "notification_light_pulse";
    static final String LOG_TAG = "MTKTOOLS";

    String[] epoFiles;
    String miscPath, cachePath;
    ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // init resources
        cachePath = getCacheDir().getPath();
        epoFiles = getResources().getStringArray(R.array.epo_files);
        miscPath = getString(R.string.misc_path);
        // init switch
        Switch sw = (Switch) findViewById(R.id.swNotificationLight);
        if (sw != null) {
            try {
                sw.setChecked(Settings.System.getInt(getContentResolver(), NOTIFICATION_LIGHT) != 0);
            } catch (Settings.SettingNotFoundException e) {
                sw.setEnabled(false);
            }
        }
        // init progress dialog
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setTitle(getString(R.string.download_msg));
        mProgressDialog.setMessage(epoFiles[0]);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
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
        switch (view.getId()) {
            case R.id.btnUpdateEPO:
                if (!isInternetAvailable()) {
                    Toast.makeText(view.getContext(), getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();
                    return;
                }
                final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
                downloadTask.execute(((EditText) findViewById(R.id.etEPO)).getText().toString());
                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true);
                    }
                });
                break;
            case R.id.btnDelMtkGps:
                try {
                    String res = sudo("rm " + getString(R.string.misc_path) + "/mtkgps.dat");
                    if ( res == null) Toast.makeText(view.getContext(), getString(R.string.file_deleted), Toast.LENGTH_LONG).show();
                    else Toast.makeText(view.getContext(), res, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                    Toast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public static String sudo(String...strings) {
        String line, res = null;
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(su.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(su.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(su.getErrorStream()));
            for (String s : strings) {
                out.writeBytes(s + "\n");
                out.flush();
            }
            out.writeBytes("exit\n");
            out.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, e.toString());
            }
            out.close();
            line = null;
            while ((line = in.readLine()) != null) {
                Log.d(LOG_TAG, "OUT: " + line);
            }
            in.close();
            line = null;
            while ((line = err.readLine()) != null) {
                res = line;
                Log.d(LOG_TAG, "ERR: " + line);
            }
            err.close();
            return res;
        } catch(IOException e){
            Log.e(LOG_TAG, e.toString());
            return e.toString();

        }
    }

    public boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return (activeNetwork != null) && activeNetwork.isConnected();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
            return false;
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {
        private static final int BUFFER_SIZE = 4096;
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... sUrl) {
            for (String epo_file : epoFiles) {
                final String msg = epo_file;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.setMessage(msg);
                    }
                });
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(sUrl[0] + "/" + epo_file);
                    Log.d(LOG_TAG, url.toString());
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        final String err_msg = "HTTP/" + connection.getResponseCode() + " " + connection.getResponseMessage();
                        Log.e(LOG_TAG, err_msg);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, err_msg, Toast.LENGTH_LONG).show();
                            }
                        });
                        return false;
                    }
                    int fileLength = connection.getContentLength();
                    input = connection.getInputStream();
                    output = new FileOutputStream(cachePath + "/" + epo_file);
                    byte data[] = new byte[BUFFER_SIZE];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        if (isCancelled()) {
                            input.close();
                            return false;
                        }
                        total += count;
                        if (fileLength > 0) publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                    final String err_msg = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, err_msg, Toast.LENGTH_LONG).show();
                        }
                    });
                    return false;
                } finally {
                    try {
                        if (output != null) output.close();
                        if (input != null) input.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                    if (connection != null) connection.disconnect();
                }
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result) {
                Toast.makeText(context, getString(R.string.download_complete), Toast.LENGTH_LONG).show();
                try {
                    for (String epo_file : epoFiles) {
                        String[] cmd_list = {
                                "cp -v " + cachePath + "/" + epo_file + " " + miscPath,
                                "chown gps:nvram " + miscPath + "/" + epo_file,
                                "chmod 664 " + miscPath + "/" + epo_file
                        };
                        String res = sudo(cmd_list);
                        if (res == null) Toast.makeText(context, getString(R.string.epo_updated), Toast.LENGTH_LONG).show();
                        else Toast.makeText(context, res, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, getString(R.string.download_error), Toast.LENGTH_LONG).show();
            }
        }
    }
}
