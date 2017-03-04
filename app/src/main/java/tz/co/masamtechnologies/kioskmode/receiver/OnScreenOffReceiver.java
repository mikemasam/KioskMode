package tz.co.masamtechnologies.kioskmode.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import tz.co.masamtechnologies.kioskmode.ApplicationContext;

/**
 * Created by Mike on 2/28/2017.
 */

public class OnScreenOffReceiver extends BroadcastReceiver {
    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
            ApplicationContext ctx = (ApplicationContext) context.getApplicationContext();
            // is Kiosk Mode active?
            if(onLock(ctx)) {
                wakeUpDevice(ctx);
            }
        }
    }

    private void wakeUpDevice(ApplicationContext context) {
        PowerManager.WakeLock wakeLock = context.getWakeLock(); // get WakeLock reference via ApplicationContext
        if (wakeLock.isHeld()) {
            wakeLock.release(); // release old wake lock
        }

        // create a new wake lock...
        wakeLock.acquire();

        // ... and release again
        wakeLock.release();
    }

/*    private boolean isKioskModeActive(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KIOSK_MODE, false);
    }*/

    public boolean onLock(Context context){
        boolean v = false;
        try{
            v = ((ApplicationContext)context.getApplicationContext()).onLock();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return  v;
    }
}
