package org.rephial.xyangband;

import android.app.Activity;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

public class GxUtils {
    public static Point getWinSize(Activity ctx)
    {
        Point aux = new Point();
        WindowManager wm2 = ctx.getWindowManager();
        wm2.getDefaultDisplay().getSize(aux);
        return aux;
    }

    public static float getDpWidth(Activity ctx) {
        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return dpWidth;
    }

    public static float toDips(Activity ctx, float dps)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps,
                ctx.getResources().getDisplayMetrics());
    }
}
