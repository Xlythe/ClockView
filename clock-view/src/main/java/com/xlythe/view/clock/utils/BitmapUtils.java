package com.xlythe.view.clock.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;

import java.util.Calendar;

public class BitmapUtils {
    public static Bitmap getHourAsBitmap(Context context, int res) {
        final int hour = Calendar.getInstance().get(Calendar.HOUR);
        final Bitmap defaultDial = BitmapFactory.decodeResource(context.getResources(), res);
        final Matrix matrix = new Matrix();
        final float degrees = (hour) * 30;

        // Rotate the image
        matrix.postRotate(degrees);
        Bitmap b = Bitmap.createBitmap(defaultDial, 0, 0, defaultDial.getWidth(), defaultDial.getHeight(), matrix, false);

        // Rotating a square makes it longer, so crop of those corners
        int x = (b.getWidth() - defaultDial.getWidth()) / 2;
        int y = (b.getHeight() - defaultDial.getHeight()) / 2;
        Bitmap croppedBmp;
        if (x > 0 && y > 0)
            croppedBmp = Bitmap.createBitmap(b, x, y, defaultDial.getWidth(), defaultDial.getHeight());
        else croppedBmp = b;
        return croppedBmp;
    }

    public static Bitmap getMinuteAsBitmap(Context context, int res) {
        final int minute = Calendar.getInstance().get(Calendar.MINUTE);
        final Bitmap defaultDial = BitmapFactory.decodeResource(context.getResources(), res);
        final Matrix matrix = new Matrix();
        final float degrees = (minute) * 6;

        // Rotate the image
        matrix.postRotate(degrees);
        Bitmap b = Bitmap.createBitmap(defaultDial, 0, 0, defaultDial.getWidth(), defaultDial.getHeight(), matrix, false);

        // Rotating a square makes it longer, so crop of those corners
        int x = (b.getWidth() - defaultDial.getWidth()) / 2;
        int y = (b.getHeight() - defaultDial.getHeight()) / 2;
        Bitmap croppedBmp;
        if (x > 0 && y > 0)
            croppedBmp = Bitmap.createBitmap(b, x, y, defaultDial.getWidth(), defaultDial.getHeight());
        else croppedBmp = b;
        return croppedBmp;
    }

    public static Bitmap getSecondAsBitmap(Context context, int res) {
        final int second = Calendar.getInstance().get(Calendar.SECOND);
        final Bitmap defaultDial = BitmapFactory.decodeResource(context.getResources(), res);
        final Matrix matrix = new Matrix();
        final float degrees = (second) * 6;

        // Rotate the image
        matrix.postRotate(degrees);
        Bitmap b = Bitmap.createBitmap(defaultDial, 0, 0, defaultDial.getWidth(), defaultDial.getHeight(), matrix, false);

        // Rotating a square makes it longer, so crop of those corners
        int x = (b.getWidth() - defaultDial.getWidth()) / 2;
        int y = (b.getHeight() - defaultDial.getHeight()) / 2;
        Bitmap croppedBmp;
        if (x > 0 && y > 0)
            croppedBmp = Bitmap.createBitmap(b, x, y, defaultDial.getWidth(), defaultDial.getHeight());
        else croppedBmp = b;
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

        // Draw the view
        view.draw(canvas);
    }
}