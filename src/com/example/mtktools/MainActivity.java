package com.example.mtktools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import java.net.URL;

public class MainActivity extends Activity {
    ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // init switch
        Switch sw = (Switch) findViewById(R.id.swNotificationLight);
        if (sw != null) {
            try {
                sw.setChecked(Settings.System.getInt(getContentResolver(), Consts.NOTIFICATION_LIGHT) != 0);
            } catch (Settings.SettingNotFoundException e) {
                sw.setEnabled(false);
            }
        }
        // init progress dialog
        mProgressDialog = new ProgressDialog(MainActivity.this);
        //mProgressDialog.setTitle(getString(R.string.download_msg));
        mProgressDialog.setMessage(getString(R.string.download_msg));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
    }

    public void onSwitchClick(View view) {
        if (((Switch) view).isChecked()) {
            Settings.System.putInt(getContentResolver(), Consts.NOTIFICATION_LIGHT, 1);
            Toast.makeText(view.getContext(), getString(R.string.notification_light_pulse) + " " + getString(R.string.on), Toast.LENGTH_LONG).show();
        } else {
            Settings.System.putInt(getContentResolver(), Consts.NOTIFICATION_LIGHT, 0);
            Toast.makeText(view.getContext(), getString(R.string.notification_light_pulse) + " " + getString(R.string.off), Toast.LENGTH_LONG).show();
        }
    }

    public void onButtonClick(View view) {
        final DownloadTask downloadTask = new DownloadTask(MainActivity.this);

        downloadTask.execute(((EditText) findViewById(R.id.etEPO)).getText().toString());
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });
    }

    public static void sudo(String...strings) {
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            for (String s : strings) {
                outputStream.writeBytes(s + "\n");
                outputStream.flush();
            }
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                Log.e(Consts.LOG_TAG, e.getMessage());
                //Toast.makeText(context , e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            outputStream.close();
        } catch(IOException e){
            Log.e(Consts.LOG_TAG, e.getMessage());
            //Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private static final int BUFFER_SIZE = 4096;
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            for (String epo_file : Consts.EPO_FILES) {
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(sUrl[0] + epo_file);
                    Log.d(Consts.LOG_TAG, url.toString());
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                    }
                    Log.d(Consts.LOG_TAG, connection.getHeaderField("Last-Modified"));
                    int fileLength = connection.getContentLength();
                    input = connection.getInputStream();
                    output = new FileOutputStream(getFilesDir().getPath() + epo_file);
                    byte data[] = new byte[BUFFER_SIZE];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        if (isCancelled()) {
                            input.close();
                            return null;
                        }
                        total += count;
                        if (fileLength > 0) publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                } catch (Exception e) {
                    return e.toString();
                } finally {
                    try {
                        if (output != null) output.close();
                        if (input != null) input.close();
                    } catch (IOException e) {
                        Log.e(Consts.LOG_TAG, e.toString());
                    }
                    if (connection != null) connection.disconnect();
                }
            }
            return null;
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
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null) Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
            try {
                Toast.makeText(context, "EPO updated", Toast.LENGTH_SHORT).show();
                sudo("cp -v " + getFilesDir().getPath() + Consts.EPO_FILES[0] + " /data/misc");
            } catch (Exception e) {
                Log.e(Consts.LOG_TAG, e.getMessage());
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
