package tz.co.masamtechnologies.kioskmode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import tz.co.masamtechnologies.kioskmode.activities.KioskActivity;
import tz.co.masamtechnologies.kioskmode.activities.LauncherActivity;
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
    private static boolean canDrawOverlayViews(Context con) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        try {
            return Settings.canDrawOverlays(con);
        } catch (NoSuchMethodError e) {
            return canDrawOverlaysUsingReflection(con);
        }
    }


    private static boolean canDrawOverlaysUsingReflection(Context context) {

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
    private static void requestOverlayDrawPermission(Activity act, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + act.getPackageName()));
        act.startActivityForResult(intent, requestCode);
    }

    public boolean isReady() {
        if (!ApplicationContext.canDrawOverlayViews(this))
            return false;
        /*if (!isMyLauncherDefault())
            return false;*/
        return true;
    }

    int OVERLAY_DRAW_CODE = 100;

    public void prepare(Activity activity) {
        if (!ApplicationContext.canDrawOverlayViews(this)) {
            ApplicationContext.requestOverlayDrawPermission(activity, OVERLAY_DRAW_CODE);
        } else {
            resetPreferredLauncherAndOpenChooser(activity);
        }
        //onActivityResult

    }

    public void onActivityResult(int code, Activity activity) {
        if (OVERLAY_DRAW_CODE == code) {
            resetPreferredLauncherAndOpenChooser(activity);
        }
    }

    public boolean startLock() {
        if (!isReady()) {
            return false;
        } else {
            startHardware();
            _onLock = true;
            registerKioskModeScreenOffReceiver();
            startKioskService();  // add this
            return true;
        }
    }

    public void stopLock(Activity context) {
        _onLock = false;
        resetPreferredLauncherAndOpenChooser(context);
        stopHardware();
    }

    private boolean startHardware() {
        try {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null)
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private boolean stopHardware() {
        try {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null)
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    public boolean force_launcher_change = false;
    private void resetPreferredLauncherAndOpenChooser(Activity context) {
        if (!force_launcher_change){
            return;
        }
        //resetDefault(context);
        context.getPackageManager().clearPackagePreferredActivities(context.getPackageName());
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, LauncherActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_DEFAULT);
        //selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //Intent chooser = Intent.createChooser(selector, "Launcher");
        context.startActivity(selector);

        //packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }
/*    private static void resetDefault(Activity context) {
        PackageManager manager = context.getPackageManager();
        ComponentName component = new ComponentName(context, LauncherActivity.class);
        manager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        manager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }*/

    /**
     * method checks to see if app is currently set as default launcher
     *
     * @return boolean true means currently set as default, otherwise false
     */
    boolean isMyLauncherDefault() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        final String myPackageName = getPackageName();
        List<ComponentName> activities = new ArrayList<ComponentName>();
        final PackageManager packageManager = (PackageManager) getPackageManager();

        // You can use name of your package here as third argument
        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean _onLock = false;

    public boolean onLock() {
        return _onLock;
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
