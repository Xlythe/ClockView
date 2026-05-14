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
    private float rotation = Float.NaN;

    public ClockHandView(Context context) {
        super(context);
    }

    public ClockHandView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClockHandView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public ClockHandView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
    }

    @Override
    public void setRotation(float rotation) {
        if (this.rotation != rotation) {
            this.rotation = rotation;
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        if (!Float.isNaN(rotation)) {
            canvas.rotate(rotation, getWidth() / 2f, getHeight() / 2f);
        }
        super.onDraw(canvas);
        canvas.restore();
    }
}
