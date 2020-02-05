package com.example.decoderencoder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Developer on 6/08/2017.
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }
}
