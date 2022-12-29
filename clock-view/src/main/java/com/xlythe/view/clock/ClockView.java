package com.xlythe.view.clock;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * An adjustable clock view
 */
public class ClockView extends FrameLayout {
    public static final String TAG = ClockView.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * 1000;

    private static final String BUNDLE_SUPER = "super";
    private static final String EXTRA_DIGITAL_ENABLED = "digital_enabled";
    private static final String EXTRA_SECONDS_ENABLED = "seconds_enabled";
    private static final String EXTRA_AMBIENT_MODE_ENABLED = "ambient_mode_enabled";
    private static final String EXTRA_PARTIAL_ROTATION_ENABLED = "partial_rotation_enabled";
    private static final String EXTRA_LOW_BIT_AMBIENT = "low_bit_ambient";
    private static final String EXTRA_BURN_IN_PROTECTION = "burn_in_protection";

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private TextView mTimeView;
    @Nullable
    private ImageView mSeconds;
    @Nullable
    private ImageView mMinutes;
    @Nullable
    private ImageView mHours;

    private boolean mDigitalEnabled = false;
    private boolean mSecondsEnabled = true;
    private boolean mAmbientModeEnabled = false;
    private boolean mPartialRotationEnabled = false;
    private boolean mLowBitAmbient = false;
    private boolean mBurnInProtection = false;

    @Nullable
    private OnTimeTickListener mOnTimeTickListener;

    @Nullable
    private OnInvalidateListener mOnInvalidateListener;

    @Nullable
    private ZonedDateTime mDateTime;
    private long mFakeTime = -1;

    private final Runnable mTicker = new Runnable() {
        @Override
        public void run() {
            onTimeTick();
            mHandler.postDelayed(this, isSecondHandEnabled() ? ONE_SECOND : ONE_MINUTE);
        }
    };

    private boolean isStarted = false;

    public ClockView(Context context) {
        super(context);
        init(context, /*attrs=*/ null);
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ClockView(Context context, AttributeSet attrs, int typeDef) {
        super(context, attrs, typeDef);
        init(context, attrs);
    }

    @TargetApi(21)
    public ClockView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ClockView);
            mDigitalEnabled = a.getInteger(R.styleable.ClockView_clockStyle, mDigitalEnabled ? 1 : 0) == 1;
            mSecondsEnabled = a.getBoolean(R.styleable.ClockView_showSecondHand, mSecondsEnabled);
            mPartialRotationEnabled = a.getBoolean(R.styleable.ClockView_partialRotation, mPartialRotationEnabled);
            mLowBitAmbient = a.getBoolean(R.styleable.ClockView_lowBitAmbient, mLowBitAmbient);
            mBurnInProtection = a.getBoolean(R.styleable.ClockView_hasBurnInProtection, mBurnInProtection);
            mAmbientModeEnabled = a.getBoolean(R.styleable.ClockView_ambientModeEnabled, mAmbientModeEnabled);
            a.recycle();
        }
        setClipChildren(false);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            onTimeTick();
        }
    }

    /**
     * Several events (such as invalidation) only trigger if
     * a view is attached to a window. To support watchfaces,
     * we'll intercept these events and propagate them to the
     * watchface service.
     */
    private boolean isManualInvalidationEnabled() {
        return mOnInvalidateListener != null;
    }

    /**
     * Overriding this allows injected touch events to trigger
     * #performClick on a view that's not attached to an activity
     * (eg. a watchface)
     */
    @Override
    public Handler getHandler() {
        if (!isManualInvalidationEnabled()) {
            return super.getHandler();
        }

        return mHandler;
    }

    /**
     * Overriding this allows injected touch events to trigger
     * #performClick on a view that's not attached to an activity
     * (eg. a watchface)
     */
    @Override
    public boolean post(Runnable action) {
        if (!isManualInvalidationEnabled()) {
            return super.post(action);
        }

        return getHandler().post(action);
    }

    @Override
    public boolean postDelayed(Runnable action, long delayMillis) {
        if (!isManualInvalidationEnabled()) {
            return super.postDelayed(action, delayMillis);
        }

        return getHandler().postDelayed(action, delayMillis);
    }

    @Override
    public void postInvalidateDelayed(long delayMilliseconds) {
        if (!isManualInvalidationEnabled()) {
            super.postInvalidateDelayed(delayMilliseconds);
            return;
        }

        postDelayed(this::invalidate, delayMilliseconds);
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (!isManualInvalidationEnabled()) {
            super.invalidateDrawable(drawable);
            return;
        }

        invalidate();
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        if (!isManualInvalidationEnabled()) {
            super.scheduleDrawable(who, what, when);
            return;
        }

        if (verifyDrawable(who)) {
            getHandler().postAtTime(what, who, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who) {
        if (!isManualInvalidationEnabled()) {
            super.unscheduleDrawable(who);
            return;
        }

        getHandler().removeCallbacksAndMessages(who);
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        if (!isManualInvalidationEnabled()) {
            super.unscheduleDrawable(who, what);
            return;
        }

        if (verifyDrawable(who)) {
            getHandler().removeCallbacks(what, who);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (mOnInvalidateListener != null) {
            mOnInvalidateListener.onInvalidate();
        }
    }

    @RequiresApi(26)
    public void setTime(ZonedDateTime dateTime) {
        mDateTime = dateTime;
    }

    public long getFakeTime() {
        return mFakeTime;
    }

    public void setFakeTime(long timeInMillis) {
        mFakeTime = timeInMillis;
    }

    public void setFakeTime(int hour, int minute) {
        setFakeHour(hour);
        setFakeMinute(minute);
    }

    public void setFakeTime(int hour, int minute, int seconds) {
        setFakeHour(hour);
        setFakeMinute(minute);
        setFakeSecond(seconds);
    }

    public void setFakeHour(int hour) {
        Calendar calendar = Calendar.getInstance();
        if (mFakeTime >= 0) {
            calendar.setTimeInMillis(mFakeTime);
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        setFakeTime(calendar.getTimeInMillis());
    }

    public void setFakeMinute(int minute) {
        Calendar calendar = Calendar.getInstance();
        if (mFakeTime >= 0) {
            calendar.setTimeInMillis(mFakeTime);
        }
        calendar.set(Calendar.MINUTE, minute);
        setFakeTime(calendar.getTimeInMillis());
    }

    public void setFakeSecond(int second) {
        Calendar calendar = Calendar.getInstance();
        if (mFakeTime >= 0) {
            calendar.setTimeInMillis(mFakeTime);
        }
        calendar.set(Calendar.SECOND, second);
        setFakeTime(calendar.getTimeInMillis());
    }

    /**
     * Updates the ui every couple of seconds
     */
    public void start() {
        mHandler.removeCallbacks(mTicker);
        mHandler.post(mTicker);
        isStarted = true;
    }

    private boolean isStarted() {
        return isStarted;
    }

    public void stop() {
        mHandler.removeCallbacks(mTicker);
        isStarted = false;
    }

    @Override
    protected void onFinishInflate() {
        mTimeView = findViewById(R.id.clock_time);
        mSeconds = findViewById(R.id.clock_seconds);
        mMinutes = findViewById(R.id.clock_minutes);
        mHours = findViewById(R.id.clock_hours);

        if (!supportsDigital() && !supportsAnalog()) {
            throw new IllegalStateException("Before inflating this View, you must include at least one child with the id @id/clock_time [TextView]" +
                    "or @id/clock_seconds [ImageView], @id/clock_minutes [ImageView], @id/clock_hours [ImageView]");
        }

        mDigitalEnabled = mDigitalEnabled || !supportsAnalog();

        super.onFinishInflate();
    }

    public boolean supportsDigital() {
        return mTimeView != null;
    }

    public boolean supportsAnalog() {
        return mSeconds != null || mMinutes != null || mHours != null;
    }

    public boolean isDigitalEnabled() {
        return mDigitalEnabled;
    }

    public void setDigitalEnabled(boolean digitalEnabled) {
        if (mDigitalEnabled == digitalEnabled) {
            return;
        }

        mDigitalEnabled = digitalEnabled;
        onTimeTick();
        if (isStarted()) {
            stop();
            start();
        }
    }

    public boolean isSecondHandEnabled() {
        return !isDigitalEnabled() && !isAmbientModeEnabled() && mSecondsEnabled;
    }

    public void setSecondHandEnabled(boolean enabled) {
        if (mSecondsEnabled == enabled) {
            return;
        }

        mSecondsEnabled = enabled;
        onTimeTick();
        if (isStarted()) {
            stop();
            start();
        }
    }

    public boolean isPartialRotationEnabled() {
        return mPartialRotationEnabled;
    }

    /**
     * Partial rotation for hours and minutes (as opposed to ticks)
     * If enabled, seconds will affect rotation
     */
    public void setPartialRotationEnabled(boolean enabled) {
        mPartialRotationEnabled = enabled;
    }

    public boolean isLowBitAmbient() {
        return mLowBitAmbient;
    }

    public void setLowBitAmbient(boolean lowBitAmbient) {
        mLowBitAmbient = lowBitAmbient;
    }

    public boolean hasBurnInProtection() {
        return mBurnInProtection;
    }

    public void setHasBurnInProtection(boolean burnInProtection) {
        mBurnInProtection = burnInProtection;
    }

    protected String getDateFormat() {
        return DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "hh:mm";
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_SUPER, super.onSaveInstanceState());
        bundle.putBoolean(EXTRA_DIGITAL_ENABLED, mDigitalEnabled);
        bundle.putBoolean(EXTRA_SECONDS_ENABLED, mSecondsEnabled);
        bundle.putBoolean(EXTRA_AMBIENT_MODE_ENABLED, mAmbientModeEnabled);
        bundle.putBoolean(EXTRA_PARTIAL_ROTATION_ENABLED, mPartialRotationEnabled);
        bundle.putBoolean(EXTRA_LOW_BIT_AMBIENT, mLowBitAmbient);
        bundle.putBoolean(EXTRA_BURN_IN_PROTECTION, mBurnInProtection);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        super.onRestoreInstanceState(bundle.getParcelable(BUNDLE_SUPER));
        mDigitalEnabled = bundle.getBoolean(EXTRA_DIGITAL_ENABLED, mDigitalEnabled);
        mSecondsEnabled = bundle.getBoolean(EXTRA_SECONDS_ENABLED, mSecondsEnabled);
        mAmbientModeEnabled = bundle.getBoolean(EXTRA_AMBIENT_MODE_ENABLED, mAmbientModeEnabled);
        mPartialRotationEnabled = bundle.getBoolean(EXTRA_PARTIAL_ROTATION_ENABLED, mPartialRotationEnabled);
        mLowBitAmbient = bundle.getBoolean(EXTRA_LOW_BIT_AMBIENT, mLowBitAmbient);
        mBurnInProtection = bundle.getBoolean(EXTRA_BURN_IN_PROTECTION, mBurnInProtection);
    }

    public void onTimeTick() {
        final int hour;
        final int minute;
        final int second;
        if (Build.VERSION.SDK_INT >= 26 && mDateTime != null) {
            hour = mDateTime.getHour();
            minute = mDateTime.getMinute();
            second = mDateTime.getSecond();
        } else {
            long timeInMillis = mFakeTime >= 0 ? mFakeTime : System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeInMillis);
            hour = calendar.get(Calendar.HOUR);
            minute = calendar.get(Calendar.MINUTE);
            second = calendar.get(Calendar.SECOND);
        }

        if (mTimeView != null) {
            final String formattedDate;
            if (Build.VERSION.SDK_INT >= 26 && mDateTime != null) {
                formattedDate = mDateTime.format(DateTimeFormatter.ofPattern(getDateFormat()));
            } else {
                long timeInMillis = mFakeTime >= 0 ? mFakeTime : System.currentTimeMillis();
                formattedDate = DateFormat.format(getDateFormat(), timeInMillis).toString();
            }
            mTimeView.setText(formattedDate);
            mTimeView.setVisibility(isDigitalEnabled() ? View.VISIBLE : View.GONE);
        }

        float degrees = hour * 30 + minute / 2f;
        if (mPartialRotationEnabled) {
            degrees += second / 120f;
        }
        if (mHours != null) {
            mHours.setRotation(degrees);
            mHours.setVisibility(isDigitalEnabled() ? View.GONE : View.VISIBLE);
        }
        degrees = minute * 6;
        if (mPartialRotationEnabled) {
            degrees += second / 10f;
        }
        if (mMinutes != null) {
            mMinutes.setRotation(degrees);
            mMinutes.setVisibility(isDigitalEnabled() ? View.GONE : View.VISIBLE);
        }
        if (mSeconds != null) {
            if (isSecondHandEnabled() && !isDigitalEnabled()) {
                degrees = second * 6;
                mSeconds.setRotation(degrees);
                mSeconds.setVisibility(View.VISIBLE);
            } else {
                mSeconds.setVisibility(View.INVISIBLE);
            }
        }

        if (mOnTimeTickListener != null) {
            mOnTimeTickListener.onTimeTick();
        }
    }

    public boolean isAmbientModeEnabled() {
        return mAmbientModeEnabled;
    }

    public void setAmbientModeEnabled(boolean enabled) {
        if (mAmbientModeEnabled == enabled) {
            return;
        }

        mAmbientModeEnabled = enabled;
        onTimeTick();
        if (isStarted()) {
            stop();
            start();
        }
    }

    public OnTimeTickListener getOnTimeTickListener() {
        return mOnTimeTickListener;
    }

    public void setOnTimeTickListener(OnTimeTickListener l) {
        mOnTimeTickListener = l;
    }

    public interface OnTimeTickListener {
        void onTimeTick();
    }

    public OnInvalidateListener getOnInvalidateListener() {
        return mOnInvalidateListener;
    }

    public void setOnInvalidateListener(OnInvalidateListener l) {
        mOnInvalidateListener = l;
    }


    public interface OnInvalidateListener {
        void onInvalidate();
    }
}
