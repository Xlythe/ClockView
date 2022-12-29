package com.xlythe.view.clock.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.xlythe.view.clock.ClockView;

import java.lang.reflect.Method;
import java.util.Calendar;

public class BitmapUtils {
    public static Bitmap getHourAsBitmap(Context context, int res) {
        final int hour = Calendar.getInstance().get(Calendar.HOUR);
        final Bitmap defaultDial = BitmapFactory.decodeResource(context.getResources(), res);
        final float degrees = (hour) * 30;
        return rotate(defaultDial, degrees);
    }

    public static Bitmap getMinuteAsBitmap(Context context, int res) {
        final int minute = Calendar.getInstance().get(Calendar.MINUTE);
        final Bitmap defaultDial = BitmapFactory.decodeResource(context.getResources(), res);
        final float degrees = (minute) * 6;
        return rotate(defaultDial, degrees);
    }

    public static Bitmap getSecondAsBitmap(Context context, int res) {
        final int second = Calendar.getInstance().get(Calendar.SECOND);
        final Bitmap defaultDial = BitmapFactory.decodeResource(context.getResources(), res);
        final float degrees = (second) * 6;
        return rotate(defaultDial, degrees);
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        if (degrees == 0f) {
            return bitmap;
        }

        final Matrix matrix = new Matrix();

        // Rotate the image
        matrix.postRotate(degrees, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
        Bitmap b = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

        // Rotating a square makes it longer, so crop off those corners
        int x = (b.getWidth() - bitmap.getWidth()) / 2;
        int y = (b.getHeight() - bitmap.getHeight()) / 2;
        Bitmap croppedBmp;
        if (x > 0 && y > 0) {
            croppedBmp = Bitmap.createBitmap(b, x, y, bitmap.getWidth(), bitmap.getHeight());
        }
        else {
            croppedBmp = b;
        }
        return croppedBmp;
    }

    public static Bitmap flatten(Bitmap... bitmaps) {
        Bitmap container = Bitmap.createBitmap(bitmaps[0].getWidth(), bitmaps[0].getHeight(), bitmaps[0].getConfig());
        Canvas canvas = new Canvas(container);
        Rect rect = new Rect(0, 0, bitmaps[0].getWidth(), bitmaps[0].getHeight());
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, rect, null);
            }
        }
        return container;
    }

    public static Bitmap asBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                return bitmap;
            }
        }

        int width = Math.max(1, drawable.getIntrinsicWidth());
        int height = Math.max(1, drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap resize(Bitmap bitmap, int width, int height, boolean keepAspectRatio) {
        if (!keepAspectRatio) {
            return Bitmap.createScaledBitmap(bitmap, width, height, false);
        }

        float aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        if (height * aspectRatio < width) {
            width = Math.round(height * aspectRatio);
        } else {
            height = Math.round(width / aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    public static void measure(View view, Rect bounds) {
        // Update the view dimensions
        view.measure(View.MeasureSpec.makeMeasureSpec(bounds.width(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(bounds.height(), View.MeasureSpec.EXACTLY));
        view.layout(0, 0, bounds.width(), bounds.height());
    }

    public static Bitmap draw(View view, Rect bounds) {
        return draw(view, bounds, false);
    }

    public static Bitmap draw(View view, Rect bounds, boolean forceResize) {
        Bitmap container = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(container);

        draw(view, canvas, bounds, forceResize);

        return container;
    }

    public static void draw(View view, Canvas canvas, Rect bounds) {
        draw(view, canvas, bounds, false);
    }

    public static void draw(View view, Canvas canvas, Rect bounds, boolean forceResize) {
        // Update the view dimensions
        if (forceResize || view.getWidth() != bounds.width() || view.getHeight() != bounds.height()
                || view.isLayoutRequested()) {
            measure(view, bounds);
        }

        // Prepare the view for drawing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setForceSoftware(view.getForeground());
        }
        setForceSoftware(view.getBackground());

        // Draw the view
        view.draw(canvas);
    }

    /**
     * RippleDrawables, by default, can only be drawn when attached to a window.
     * However, by calling the hidden method RippleDrawable#setForceSoftware, it's possible
     * to draw them anyways.
     */
    private static void setForceSoftware(@Nullable Drawable drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        if (drawable == null) {
            return;
        }

        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {
                setForceSoftware(layerDrawable.getDrawable(i));
            }
        }

        if (drawable instanceof StateListDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StateListDrawable stateListDrawable = (StateListDrawable) drawable;
            for (int i = 0; i < stateListDrawable.getStateCount(); i++) {
                setForceSoftware(stateListDrawable.getStateDrawable(i));
            }
        }

        Drawable current = drawable.getCurrent();
        if (current != drawable) {
            setForceSoftware(current);
        }

        if (!(drawable instanceof RippleDrawable)) {
            return;
        }

        try {
            @SuppressLint("PrivateApi") Method method = RippleDrawable.class.getDeclaredMethod("setForceSoftware", Boolean.TYPE);
            method.invoke(drawable, true);
        } catch (Exception e) {
            Log.e(ClockView.TAG, "Failed to call RippleDrawable#setForceSoftware", e);
        }
    }
}