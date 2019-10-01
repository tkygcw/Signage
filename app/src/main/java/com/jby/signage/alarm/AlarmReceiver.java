package com.jby.signage.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("haha","alarm fired!");
        Bundle bundle = new Bundle();
        bundle.putBoolean("refresh", true);

        Intent i = new Intent("alarmManager");
        i.putExtras(bundle);

        context.sendBroadcast(i);
    }

}