package com.updatefota;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BackgroundService extends Service {

    BootReciver mReceiver = new BootReciver();
    String TAG = "UpdateFOTA";
    boolean DEBUG = true;
    public BackgroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        String CHANNEL_ID = "my_channel_01";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.updatefota.R.drawable.ic_launcher_background)
                .setContentTitle("")
                .setContentText("").build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            Log.d(TAG, "Start service in background");
        }
        String SOME_ACTION = "com.catalia.mabu.application.WatchdogThread.pushUpdate";
        IntentFilter intentFilter = new IntentFilter(SOME_ACTION);
        registerReceiver(mReceiver, intentFilter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Check onDestroy service in background");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}