package tz.co.masamtechnologies.kioskmode.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import tz.co.masamtechnologies.kioskmode.activities.KioskActivity;

/**
 * Created by Mike on 2/28/2017.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        // Restart activity
        PackageManager pm = ctx.getPackageManager();
        //check if we got the PackageManager
        if (pm != null) {
            Intent i = pm.getLaunchIntentForPackage(ctx.getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        }
    }
}
