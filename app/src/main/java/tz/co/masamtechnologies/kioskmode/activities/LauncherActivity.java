package tz.co.masamtechnologies.kioskmode.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;

import tz.co.masamtechnologies.kioskmode.ApplicationContext;

/**
 * Created by Mike on 3/6/2017.
 */

public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        PackageManager pm = getPackageManager();
        Intent intent=pm.getLaunchIntentForPackage(getPackageName());
        startActivity(intent);
    }
}
