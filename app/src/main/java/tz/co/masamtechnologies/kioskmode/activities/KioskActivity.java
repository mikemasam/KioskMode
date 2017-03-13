package tz.co.masamtechnologies.kioskmode.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tz.co.masamtechnologies.kioskmode.ApplicationContext;
import tz.co.masamtechnologies.kioskmode.components.CustomViewGroup;

/**
 * Created by Mike on 2/28/2017.
 */

public class KioskActivity extends AppCompatActivity {
    protected KioskActivity() {
        super();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!onLock())
            return;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        preventStatusBarExpansion(this);
/*
        ((ApplicationContext)getApplicationContext()).registerKioskModeScreenOffReceiver();
        ((ApplicationContext)getApplicationContext()).startKioskService();  // add this*/
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!onLock())
            return;
        if(!hasFocus) {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }

    /*private final List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP));

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!onLock())
            return super.dispatchKeyEvent(event);
        if (blockedKeys.contains(event.getKeyCode())) {
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }*/


    public static void preventStatusBarExpansion(Context context) {
        try {
            WindowManager manager = ((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

            WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
            localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            localLayoutParams.gravity = Gravity.TOP;
            localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;

            int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            int result = 0;
            if (resId > 0) {
                result = context.getResources().getDimensionPixelSize(resId);
            } else {
                // Use Fallback size:
                result = 60; // 60px Fallback
            }

            localLayoutParams.height = result;
            localLayoutParams.format = PixelFormat.TRANSPARENT;

            CustomViewGroup view = new CustomViewGroup(context);
            manager.addView(view, localLayoutParams);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    public boolean onLock(){
        boolean v = false;
        try{
            v = ((ApplicationContext)getApplicationContext()).onLock();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return  v;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (((ApplicationContext)getApplicationContext()).onLock())
            ((ApplicationContext)getApplicationContext()).restoreApp(); // restore!
    }
}
