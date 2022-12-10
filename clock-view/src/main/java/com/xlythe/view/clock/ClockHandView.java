package com.xlythe.view.clock;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.xlythe.view.clock.utils.BitmapUtils;

public class ClockHandView extends AppCompatImageView {
    private final Matrix matrix = new Matrix();
    private final Paint paint = new Paint();

    private float rotation = Float.NaN;
    private Bitmap bitmap;

    public ClockHandView(Context context) {
        super(context);
        init();
    }

    public ClockHandView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClockHandView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public ClockHandView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setFilterBitmap(true);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
    }

    @Override
    public void setRotation(float rotation) {
        if (this.rotation != rotation) {
            this.rotation = rotation;
            this.bitmap = null;
            postInvalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            bitmap = null;
        }
    }

    @NonNull
    private Bitmap getBitmap() {
        if (this.bitmap == null) {
            Bitmap bitmap = BitmapUtils.asBitmap(getDrawable());
            bitmap = BitmapUtils.resize(bitmap, getWidth(), getHeight(), true);
            bitmap = BitmapUtils.rotate(bitmap, rotation);
            this.bitmap = bitmap;
            matrix.setTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f);
        }
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(getBitmap(), matrix, paint);
        canvas.restore();
    }
}
