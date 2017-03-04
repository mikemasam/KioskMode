package tz.co.masamtechnologies.kioskmode.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import tz.co.masamtechnologies.kioskmode.ApplicationContext;

/**
 * Created by Mike on 2/28/2017.
 */

public class KioskService extends Service {
    private static final long INTERVAL = TimeUnit.SECONDS.toMillis(1); // periodic interval to check in seconds -> 2 seconds
    private static final String TAG = KioskService.class.getSimpleName();
    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";

    private Thread t = null;
    private Context ctx = null;
    private boolean running = false;

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping service 'KioskService'");
        running =false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!onLock()) {
            Log.i(TAG, "Stopping service on lock 'KioskService'");
            return Service.START_NOT_STICKY;
        }
        Log.i(TAG, "Starting service 'KioskService'");
        running = true;
        ctx = this;

        // start a thread that periodically checks if your app is in the foreground
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    handleKioskMode();
                    try {
                        Log.i(TAG, "Thread running: 'KioskService'");
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Thread interrupted: 'KioskService'");
                    }
                }while(running);
                try {
                    t.stop();
                    t.destroy();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
                stopSelf();
            }
        });

        t.start();
        return Service.START_NOT_STICKY;
    }

    private void handleKioskMode() {
        if (!onLock())
            return;
        // is Kiosk Mode active?
        if(onLock()) {
            Log.i(TAG, "Active: 'KioskService'");
            // is App in background?
            if(isInBackground(ctx)) {
                Log.i(TAG, "In the background: 'KioskService'");
                ((ApplicationContext)getApplicationContext()).restoreApp(); // restore!
            }else
            {
                Log.i(TAG, "Not in the background: 'KioskService'");
            }
        }else
            Log.i(TAG, "Not active: 'KioskService'");
    }

/*    private boolean isInBackground() {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        return (!ctx.getApplicationContext().getPackageName().equals(componentInfo.getPackageName()));
    }*/

    private boolean _killing = false;
    private boolean isInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        if (_killing)
            return isInBackground;

        try{
            _killing = true;
            List<ApplicationInfo> packages;
            PackageManager pm;
            pm = getPackageManager();
            //get a list of installed apps.
            packages = pm.getInstalledApplications(0);

            ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            String myPackage = getApplicationContext().getPackageName();
            for (ApplicationInfo packageInfo : packages) {
                try {
                    if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) continue;

                    if (packageInfo.packageName.equals(myPackage)) continue;
                    mActivityManager.killBackgroundProcesses(packageInfo.packageName);
                }catch (Exception _ex){

                }
            }
            _killing = false;
        }catch(Exception ex){
            _killing = false;
        }

        return isInBackground;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public boolean onLock(){
        boolean v = false;
        try{
            v = ((ApplicationContext)getApplicationContext()).onLock();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        running = v;
        return  v;
    }
}
