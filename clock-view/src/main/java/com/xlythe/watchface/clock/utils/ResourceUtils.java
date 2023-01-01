package com.xlythe.watchface.clock.utils;

import android.content.Context;
import android.util.TypedValue;

public class ResourceUtils {
    public static float inDisplayIndependentPixels(Context context, int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static float inScaleIndependentPixels(Context context, int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, dp, context.getResources().getDisplayMetrics());
    }
}
