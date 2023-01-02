package com.xlythe.view.clock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RangeDrawable extends ComplicationDrawable {
    private final static int BACKGROUND_PROGRESS_ALPHA = 76;

    private final float mMin;
    private final float mMax;
    private final float mValue;
    @Nullable private final int[] mColors;
    private final boolean mSmoothColors;

    private final Paint mBackgroundProgressPaint = new Paint();
    private final Paint mForegroundProgressPaint = new Paint();
    private Path mRoundedRectPath;
    private Paint[] mMultiColoredForegroundPaints = new Paint[0];

    // To work around a bug in SweepGradient. See #createSweepGradient for details.
    private static final Set<Integer> sReservedSweepGradientColors = new HashSet<>();
    private int mSweepGradientHash;

    RangeDrawable(
            Context context,
            @Nullable Drawable icon,
            @Nullable CharSequence text,
            @Nullable CharSequence title,
            float min,
            float max,
            float value,
            @Nullable int[] colors,
            boolean smoothColors) {
        super(context, icon, text, title);
        mMin = min;
        mMax = max;
        mValue = value;

        mColors = colors;
        mSmoothColors = smoothColors;

        mBackgroundProgressPaint.setColor(DEFAULT_COLOR);
        mBackgroundProgressPaint.setAlpha(BACKGROUND_PROGRESS_ALPHA);
        mBackgroundProgressPaint.setStyle(Paint.Style.STROKE);
        mBackgroundProgressPaint.setStrokeWidth(getLineStrokeWidth());
        mBackgroundProgressPaint.setAntiAlias(true);

        mForegroundProgressPaint.setColor(DEFAULT_COLOR);
        mForegroundProgressPaint.setStyle(Paint.Style.STROKE);
        mForegroundProgressPaint.setStrokeWidth(getLineStrokeWidth());
        mForegroundProgressPaint.setAntiAlias(true);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        canvas.save();
        canvas.translate(mForegroundProgressPaint.getStrokeWidth(), mForegroundProgressPaint.getStrokeWidth());

        // Draw the background
        canvas.drawPath(mRoundedRectPath, mBackgroundProgressPaint);

        // Draw the foreground
        if (mMultiColoredForegroundPaints.length > 0) {
            for (Paint paint : mMultiColoredForegroundPaints) {
                canvas.drawPath(mRoundedRectPath, paint);
            }
        } else {
            canvas.drawPath(mRoundedRectPath, mForegroundProgressPaint);
        }

        canvas.restore();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        super.setTintList(tint);

        if (tint == null) {
            tint = ColorStateList.valueOf(DEFAULT_COLOR);
        }

        if (mColors == null) {
            mForegroundProgressPaint.setColor(tint.getColorForState(getState(), tint.getDefaultColor()));
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);

        // We need to inset by the width of our stroke, or we'll get cropped. We'll shrink our
        // width/height by the stroke on both sides, and then we'll shift the canvas when we later draw.
        float width = getWidth() - 2 * mForegroundProgressPaint.getStrokeWidth();
        float height = getHeight() - 2 * mForegroundProgressPaint.getStrokeWidth();
        float radius = height / 2f;

        // Construct a rounded rect. We do this manually (with lines and arcs) because we want to
        // control where the starting position is (center-top, or -90d).
        mRoundedRectPath = new Path();
        mRoundedRectPath.setLastPoint(width / 2f, 0);
        mRoundedRectPath.lineTo(width - radius, 0);
        mRoundedRectPath.arcTo(new RectF(width - 2 * radius, 0, width, height), 270, 180);
        mRoundedRectPath.lineTo(radius, height);
        mRoundedRectPath.arcTo(new RectF(0, 0, 2 * radius, height), 90, 180);
        mRoundedRectPath.close();

        // Measure the length of the path and then create a dash and a space that are each equal in length to the path.
        // We'll then rotate this effect as the progress increments.
        float length = new PathMeasure(mRoundedRectPath, false).getLength();
        PathEffect effect = new DashPathEffect(new float[] { length, length }, length - length * getProgress());

        // PathMeasure doesn't perfectly measure the length of the radius, sometimes leaving the path
        // unclosed as you approach 100%. To avoid that, we don't set the effect at 100% to ensure the path is closed.
        if (getProgress() < 1f) {
            mForegroundProgressPaint.setPathEffect(effect);
        }

        // If the user specified what colors to use, we'll apply those here.
        if (mColors != null) {
            if (mSmoothColors) {
                // Create a gradient that sweeps through the colors
                SweepGradient gradient = createSweepGradient(width / 2f, height / 2f, mColors);

                // The gradient starts at 0d, but our progress starts at -90d so we'll rotate backwards to match it.
                Matrix gradientMatrix = new Matrix();
                gradientMatrix.preRotate(-90, width / 2f, height / 2f);
                gradient.setLocalMatrix(gradientMatrix);

                mForegroundProgressPaint.setShader(gradient);
            } else {
                // To mimic the illusion of shifting colors in fixed steps, we'll draw multiple times.
                // The first pass will be the furthest color, followed by drawing on top of that with a smaller
                // percentage, and again and again. To do that, we first break the percentages down into steps.
                float stepSize = 1f / mColors.length;

                // Next, we'll calculate the step that we're on now.
                int stepsToDraw = (int) Math.ceil(getProgress() / stepSize);
                mMultiColoredForegroundPaints = new Paint[stepsToDraw];

                // Now we'll loop over those steps and create a paint for each one.
                for (int i = 0; i < mMultiColoredForegroundPaints.length; i++) {
                    float progress = Math.min((i + 1) * stepSize, getProgress());
                    mMultiColoredForegroundPaints[i] = new Paint(mForegroundProgressPaint);
                    mMultiColoredForegroundPaints[i].setColor(mColors[i]);
                    if (progress < 1f) {
                        mMultiColoredForegroundPaints[i].setPathEffect(new DashPathEffect(new float[]{length, length}, length - length * progress));
                    }
                }

                // And finally we'll flip the array so that they're drawn in order of the largest
                // percentage first.
                reverse(mMultiColoredForegroundPaints);
            }
        }
    }

    // Note: This is to work around a bug seen on the emulator. Not sure if it repros in production too.
    // SweepGradient can corrupt itself if multiple instances of it are used at the same time, using the same params.
    // To work around that, we'll keep track of what colors we're using and subtly modify them to create unique params.
    private SweepGradient createSweepGradient(float cx, float cy, @ColorInt int[] colors) {
        synchronized (RangeDrawable.this) {
            sReservedSweepGradientColors.remove(mSweepGradientHash);

            // We don't need to perfectly compare the colors, since the modification is subtle if we're wrong.
            // So we'll just hash our colors and compare with the other hashes.
            int hash = Arrays.hashCode(colors);
            while (sReservedSweepGradientColors.contains(hash)) {
                randomizeColors(colors);
                hash = Arrays.hashCode(colors);
            }
            sReservedSweepGradientColors.add(hash);
            mSweepGradientHash = hash;
            return new SweepGradient(cx, cy, colors, null);
        }
    }

    private static void randomizeColors(int[] colors) {
        Random random = new Random();
        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            int alpha = Color.alpha(color);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);

            // Create an offset of -1 to 1
            red += random.nextInt(3) - 1;
            green += random.nextInt(3) - 1;
            blue += random.nextInt(3) - 1;

            red = clamp(red, 0, 255);
            green = clamp(green, 0, 255);
            blue = clamp(blue, 0, 255);

            // Add the offset
            colors[i] = Color.argb(alpha, red, green, blue);
        }
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(Math.min(val, max), min);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        synchronized (RangeDrawable.this) {
            sReservedSweepGradientColors.remove(mSweepGradientHash);
        }
    }

    private static <T> void reverse(T[] arr) {
        for (int i = 0; i < arr.length / 2; i++) {
            T temp = arr[i];
            arr[i] = arr[arr.length - i - 1];
            arr[arr.length - i - 1] = temp;
        }
    }

    /** Returns the current progress from 0f to 1f */
    private float getProgress() {
        if (mValue >= mMax) {
            return 1f;
        }

        return mValue / (mMax - mMin);
    }

    public static class Builder {
        private final Context mContext;
        private boolean mShowBackground;
        private CharSequence mTitle;
        private CharSequence mText;
        private Drawable mIcon;
        private float mMin;
        private float mMax;
        private float mValue;
        private int[] mColors;
        private boolean mSmoothColors;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder showBackground(boolean show) {
            mShowBackground = show;
            return this;
        }

        public Builder style(Style style) {
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

        public Builder colors(@ColorInt int[] colors, boolean smooth) {
            if (colors.length > 0) {
                mColors = colors;
                mSmoothColors = smooth;
            } else {
                mColors = null;
                mSmoothColors = true;
            }
            return this;
        }

        public RangeDrawable build() {
            RangeDrawable drawable = new RangeDrawable(
                    mContext,
                    mIcon,
                    mText,
                    mTitle,
                    mMin,
                    mMax,
                    mValue,
                    mColors,
                    mSmoothColors);
            drawable.mShowBackground = mShowBackground;
            return drawable;
        }
    }
}
