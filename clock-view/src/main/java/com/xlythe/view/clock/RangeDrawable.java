package com.xlythe.view.clock;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SumPathEffect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class RangeDrawable extends ComplicationDrawable {
    private final static int BACKGROUND_PROGRESS_ALPHA = 76;

    private final float mMin;
    private final float mMax;
    private final float mValue;

    private final Paint mBackgroundProgressPaint = new Paint();
    private final Paint mForegroundProgressPaint = new Paint();

    RangeDrawable(
            @Nullable Drawable icon,
            @Nullable CharSequence text,
            @Nullable CharSequence title,
            float min,
            float max,
            float value) {
        super(icon, text, title);
        mMin = min;
        mMax = max;
        mValue = value;

        mBackgroundProgressPaint.setColor(DEFAULT_COLOR);
        mBackgroundProgressPaint.setAlpha(BACKGROUND_PROGRESS_ALPHA);
        mBackgroundProgressPaint.setStyle(Paint.Style.STROKE);
        mBackgroundProgressPaint.setStrokeWidth(2);
        mBackgroundProgressPaint.setAntiAlias(true);

        mForegroundProgressPaint.setColor(DEFAULT_COLOR);
        mForegroundProgressPaint.setStyle(Paint.Style.STROKE);
        mForegroundProgressPaint.setStrokeWidth(2);
        mForegroundProgressPaint.setAntiAlias(true);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        // Draw the background
        float radius = getHeight() / 2f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect( mBackgroundProgressPaint.getStrokeWidth() / 2,  mBackgroundProgressPaint.getStrokeWidth() / 2, getWidth() - mBackgroundProgressPaint.getStrokeWidth() / 2, getHeight() - mBackgroundProgressPaint.getStrokeWidth() / 2, radius, radius, mBackgroundProgressPaint);
        } else {
            canvas.drawCircle(getWidth() / 2f - mBackgroundProgressPaint.getStrokeWidth(), getHeight() / 2f - mBackgroundProgressPaint.getStrokeWidth(), radius, mBackgroundProgressPaint);
        }

        // Draw the foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(mForegroundProgressPaint.getStrokeWidth() / 2,  mForegroundProgressPaint.getStrokeWidth() / 2, getWidth() - mForegroundProgressPaint.getStrokeWidth() / 2, getHeight() - mForegroundProgressPaint.getStrokeWidth() / 2, radius, radius, mForegroundProgressPaint);
        } else {
            canvas.drawCircle(getWidth() / 2f - mForegroundProgressPaint.getStrokeWidth(), getHeight() / 2f - mForegroundProgressPaint.getStrokeWidth(), radius, mForegroundProgressPaint);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        super.setTintList(tint);

        if (tint == null) {
            tint = ColorStateList.valueOf(DEFAULT_COLOR);
        }

        mForegroundProgressPaint.setColor(tint.getColorForState(getState(), tint.getDefaultColor()));
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);

        RectF rect = new RectF(0, 0, getWidth(), getHeight());
        float inset = mForegroundProgressPaint.getStrokeWidth();
        rect.inset(inset, inset);

        float radius = getHeight() / 2f;
        Path path = new Path();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        float length = new PathMeasure(path, false).getLength();

        PathEffect effect = new DashPathEffect(new float[] { length, length }, length - length * getProgress());

        mForegroundProgressPaint.setPathEffect(effect);
    }

    private float getProgress() {
        if (mValue >= mMax) {
            return 1f;
        }

        return mValue / (mMax - mMin);
    }

    public static class Builder extends ComplicationDrawable.Builder {
        private boolean mShowBackground;
        private CharSequence mTitle;
        private CharSequence mText;
        private Drawable mIcon;
        private float mMin;
        private float mMax;
        private float mValue;

        public Builder showBackground(boolean show) {
            mShowBackground = show;
            return this;
        }

        public Builder title(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder text(CharSequence text) {
            mText = text;
            return this;
        }

        public Builder icon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Builder range(float min, float max) {
            if (max < min) {
                throw new IllegalArgumentException("RangeDrawable does not support a max (" + max + ") smaller than the min (" + min + ")");
            }
            mMin = min;
            mMax = max;
            return this;
        }

        public Builder value(float value) {
            mValue = value;
            return this;
        }

        public RangeDrawable build() {
            RangeDrawable drawable = new RangeDrawable(
                    mIcon,
                    mText,
                    mTitle,
                    mMin,
                    mMax,
                    mValue);
            drawable.mShowBackground = mShowBackground;
            return drawable;
        }
    }
}
