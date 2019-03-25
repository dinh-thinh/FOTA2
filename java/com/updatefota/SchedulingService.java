package com.updatefota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

public class SchedulingService extends JobIntentService {
    String TAG = "UpdateFOTA";
    boolean DEBUG = true;
    public static final int JOB_ID = 1;
    static boolean data;
    static String mDataHash;
    static String mDataVersion;
    static String mDataLink;
    static String mDataName;
    static int responseCode;
    static int responseCode1;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private BroadcastReceiver receiver2;
    static String UPDATEREADY = "com.catalia.mabu.application.MabuApplication.updateReady";
    static String NOUPDATE = "com.catalia.mabu.application.MabuApplication.noUpdate";
    static final int UPDATE_SERVICE_MSG = 1;


    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SchedulingService.class, JOB_ID, work);
    }

    private class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_SERVICE_MSG:
                    Log.d(TAG, "Response UPDATE_SERVICE_FINISH..... : ");
                    Intent myIntent = new Intent(getApplicationContext(), ShowNote.class);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(myIntent);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate..... : ");
        HandlerThread thread = new HandlerThread("IntentService");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
         receiver2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(NOUPDATE)) {
                    Log.d(TAG, "BroadcastReceiver NoReady .............: " + NOUPDATE);
                    mServiceHandler.removeMessages(UPDATE_SERVICE_MSG);
                }
            }
        };
    }


    @Override
    protected void onHandleWork(Intent intent) {

        String cookie = null;
        String serial = null;

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        cookie = setLogin();
        getDataServer(cookie);
        registerReceiver(receiver2, new IntentFilter(NOUPDATE));
        Intent i = new Intent(NOUPDATE);
        sendBroadcast(i);
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "BroadcastReceiver onDestroy .............: ");
        //unregisterReceiver(receiver2);
    }

    @Override
    public boolean onStopCurrentWork() {
        unregisterReceiver(receiver2);
        Log.d(TAG, "BroadcastReceiver onStopCurrentWork .............: ");
        return super.onStopCurrentWork();
    }

    public int verCode(String PackageName) {
        int verCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(PackageName, 0);
            verCode = pInfo.versionCode;
            String version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verCode;
    }


    public String getSerialNumber() {
        String SerialNumber = "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SerialNumber = Build.getSerial();
            } else {
                SerialNumber = Build.SERIAL;
            }
            Log.d(TAG, "getSerialNumber  : " + SerialNumber);
        } catch (SecurityException e) {
            Log.e(TAG, " sirial " + e);
        }
        return SerialNumber;
    }

    public String buildVersion() {
        String Build_version = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            Build_version = (String) get.invoke(c, "ro.build.version.incremental");
        } catch (Exception ignored) {
        }
        return Build_version;
    }

    public String setLogin() {
        String cookie = null;
        try {
            URL url = new URL("https://api.staging.cataliahealth.com/fota/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            JSONObject cred = new JSONObject();
            cred.put("username", "wr_agent");
            cred.put("password", "faVddNNbimSXq0JKd7C8w9LRw6AG0zN9nyrYLF0yvuaxfDoCDW3ZVWm3uebXJm7R");

            OutputStream os = conn.getOutputStream();
            os.write(cred.toString().getBytes("UTF-8"));
            os.close();
            responseCode = conn.getResponseCode();
            if (DEBUG) {
                Log.d(TAG, "Sending 'GET' request to URL : " + url);
                Log.d(TAG, "Response Code : " + responseCode);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            StringBuilder sb = new StringBuilder();
            while ((output = br.readLine()) != null) {
                sb.append(output + "\n");
            }
            String headerName = null;
            for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
                if (headerName.equals("Set-Cookie")) {
                    cookie = conn.getHeaderField(i);
                }
            }

            conn.disconnect();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return cookie;
    }

    public void getDataServer(String cookie) {
        try {
            int verApk1 = verCode("com.catalia.mabu");
            int verApk2 = verCode("org.opencv.engine");
            int verApk3 = verCode("com.catalia.mabu.voice");
            int verApk4 = verCode("com.catalia.mabu.softkeyboard");
            String verBuild = buildVersion();
            String serial = getSerialNumber();
            //String url = "https://api.staging.cataliahealth.com/fota/apk_updates?getSerialNumber=" + "1VI03UD5M2" + "&have_versions=com.catalia.mabu:" + verApk1 + ",org.opencv.engine:" + verApk2 + ",com.catalia.mabu.voice:" + verApk3 + ",com.catalia.mabu.softkeyboard:" + verApk4 + ",ro.build.version.incremental:" + verBuild;
            //String url = "https://api.staging.cataliahealth.com/fota/apk_updates?serial=1VI03UD5M2&have_versions=com.catalia.mabu:3933,org.opencv.engine:3452,com.catalia.mabu.voice:1,com.catalia.mabu.softkeyboard:39,ro.build.version.incremental:171217";
            String url = "https://api.staging.cataliahealth.com/fota/apk_updates?serial=1VI03UD5M2&have_versions=com.catalia.mabu:3850,org.opencv.engine:3452,com.catalia.mabu.voice:1,com.catalia.mabu.softkeyboard:39,ro.build.version.incremental:181217";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Cookie", cookie);
            responseCode1 = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            if (DEBUG) {
                Log.d(TAG, "Sending 'GET' request to URL : " + url);
                Log.d(TAG, "Response Code : " + responseCode1);
            }
            try {
                JSONObject response1 = new JSONObject(response.toString());
                JSONArray array = response1.getJSONArray("data");

                if (array != null & array.length() > 0) { //check data server empty or not
                    Intent updateready = new Intent(UPDATEREADY);
                    sendBroadcast(updateready);
                    mServiceHandler.sendEmptyMessageDelayed(UPDATE_SERVICE_MSG, 30000);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject data = array.getJSONObject(i);
                        mDataHash = data.getString("apk_hash");
                        mDataLink = data.getString("link");
                        mDataVersion = data.getString("version");
                        mDataName = data.getString("name");
                        if (DEBUG) {
                            Log.d(TAG, "Response from server = " + response1);
                        }
                    }
                }
                else if( array.length() < 1){
                    mServiceHandler.removeMessages(UPDATE_SERVICE_MSG);
                    Log.d(TAG, "Data from server empty. " + array);
                }
                con.disconnect();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {

        }
    }

}