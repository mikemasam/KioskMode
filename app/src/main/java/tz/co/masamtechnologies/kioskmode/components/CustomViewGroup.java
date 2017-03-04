package tz.co.masamtechnologies.kioskmode.components;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;

/**
 * Created by Mike on 2/28/2017.
 */

public class CustomViewGroup extends ViewGroup {
    public CustomViewGroup(Context context) {
        super(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Intercepted touch!
        return true;
    }
}
