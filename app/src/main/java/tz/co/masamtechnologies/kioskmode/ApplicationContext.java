package tz.co.masamtechnologies.kioskmode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.lang.reflect.Method;

import tz.co.masamtechnologies.kioskmode.receiver.OnScreenOffReceiver;
import tz.co.masamtechnologies.kioskmode.service.KioskService;

/**
 * Created by Mike on 2/28/2017.
 */

public class ApplicationContext extends Application {
    private ApplicationContext instance;
    private PowerManager.WakeLock wakeLock;
    private OnScreenOffReceiver onScreenOffReceiver;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public void registerKioskModeScreenOffReceiver() {
        // register screen off receiver
        if (!onLock())
            return;
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        onScreenOffReceiver = new OnScreenOffReceiver();
        registerReceiver(onScreenOffReceiver, filter);
    }

    public PowerManager.WakeLock getWakeLock() {
        if (!onLock())
            return wakeLock;

        if (wakeLock == null) {
            // lazy loading: first call, create wakeLock via PowerManager.
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "wakeup");
        }
        return wakeLock;
    }

    public void startKioskService() { // ... and this method
        if (!onLock())
            return;
        startService(new Intent(this, KioskService.class));
    }

    @SuppressLint("NewApi")
    public static boolean canDrawOverlayViews(Context con) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        try {
            return Settings.canDrawOverlays(con);
        } catch (NoSuchMethodError e) {
            return canDrawOverlaysUsingReflection(con);
        }
    }


    public static boolean canDrawOverlaysUsingReflection(Context context) {

        try {

            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            Class clazz = AppOpsManager.class;
            Method dispatchMethod = clazz.getMethod("checkOp", new Class[]{int.class, int.class, String.class});
            //AppOpsManager.OP_SYSTEM_ALERT_WINDOW = 24
            int mode = (Integer) dispatchMethod.invoke(manager, new Object[]{24, Binder.getCallingUid(), context.getApplicationContext().getPackageName()});

            return AppOpsManager.MODE_ALLOWED == mode;

        } catch (Exception e) {
            return false;
        }

    }

    @SuppressLint("InlinedApi")
    public static void requestOverlayDrawPermission(Activity act, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + act.getPackageName()));
        act.startActivityForResult(intent, requestCode);
    }

    public boolean startLock(Activity activity) {
        if (!ApplicationContext.canDrawOverlayViews(this)) {
            ApplicationContext.requestOverlayDrawPermission(activity, 100);
            return false;
        } else {
            _onLock = true;

            registerKioskModeScreenOffReceiver();
            startKioskService();  // add this
            return true;
        }
    }

    public void stopLock(){
        _onLock = false;
    }
    private boolean _onLock = false;
    public boolean onLock(){
        return  _onLock;
    }

    public void restoreApp() {
        // Restart activity
        PackageManager pm = this.getPackageManager();
        //check if we got the PackageManager
        if (pm != null) {
            Intent i = pm.getLaunchIntentForPackage(this.getPackageName());
            this.startActivity(i);
        }
    }
}
