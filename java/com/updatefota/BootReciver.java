package com.updatefota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * This receiver is set to be disabled (android:enabled="false") in the
 * application's manifest file. When the user sets the alarm, the receiver is enabled.
 * When the user cancels the alarm, the receiver is disabled, so that rebooting the
 * device will not trigger this receiver.
 */
// BEGIN_INCLUDE(autostart)
public class BootReciver extends BroadcastReceiver {
    private final AlarmReceiver alarm = new AlarmReceiver();
    String pushUpdate = "com.catalia.mabu.application.WatchdogThread.pushUpdate";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            alarm.setAlarm(context);
            Intent startServiceIntent = new Intent(context, BackgroundService.class);
            context.startForegroundService(startServiceIntent);
        } else if (intent.getAction().equals(pushUpdate)) {
            SchedulingService.enqueueWork(context, new Intent());
            //setAlarm(context);
        }
    }
}
//END_INCLUDE(autostart)