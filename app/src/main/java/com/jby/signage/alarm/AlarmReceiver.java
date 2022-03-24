package com.jby.signage.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("refresh", true);
        bundle.putString("alarm_id", intent.getStringExtra("alarm_id"));

        Intent i = new Intent("alarmManager");
        i.putExtras(bundle);

        context.sendBroadcast(i);
    }
}