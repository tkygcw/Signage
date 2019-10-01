package com.jby.signage.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.jby.signage.SettingActivity;

import java.util.Objects;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            //open activity automatically
            Intent i = new Intent(context, SettingActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}