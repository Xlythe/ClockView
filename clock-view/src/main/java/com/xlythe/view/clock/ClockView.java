package com.xlythe.view.clock;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

/**
 * An adjustable clock view
 */
public class ClockView extends FrameLayout {
    protected static final String TAG = ClockView.class.getSimpleName();
    private static final boolean DEBUG = false;

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

    private long mFakeTime = -1;

    private final Runnable mTicker = new Runnable() {
        @Override
        public void run() {
            onTimeTick();
            mHandler.postDelayed(this, 1000);
        }
    };

    public ClockView(Context context) {
        super(context);
    }

    public ClockView(Context context, AttributeSet set) {
        super(context, set);
    }

    public ClockView(Context context, AttributeSet set, int typeDef) {
        super(context, set, typeDef);
    }

    @TargetApi(21)
    public ClockView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            onTimeTick();
        }
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

    /**
     * Updates the ui every couple of seconds
     */
    public void start() {
        mHandler.removeCallbacks(mTicker);
        mHandler.post(mTicker);
    }

    public void stop() {
        mHandler.removeCallbacks(mTicker);
    }

    @Override
    protected void onFinishInflate() {
        mTimeView = (TextView) findViewById(R.id.clock_time);
        mSeconds = (ImageView) findViewById(R.id.clock_seconds);
        mMinutes = (ImageView) findViewById(R.id.clock_minutes);
        mHours = (ImageView) findViewById(R.id.clock_hours);

        if (!supportsDigital() && !supportsAnalog()) {
            throw new IllegalStateException("Before inflating this View, you must include at least one child with the id @id/clock_time [TextView]" +
                    "or @id/clock_seconds [ImageView], @id/clock_minutes [ImageView], @id/clock_hours [ImageView]");
        }

        mDigitalEnabled = !supportsAnalog();
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
        mDigitalEnabled = digitalEnabled;
        onTimeTick();
    }

    public boolean isSecondHandEnabled() {
        return !isDigitalEnabled() && !isAmbientModeEnabled() && mSecondsEnabled;
    }

    public void setSecondHandEnabled(boolean enabled) {
        mSecondsEnabled = enabled;
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

    public boolean isBurnInProtection() {
        return mBurnInProtection;
    }

    public void setBurnInProtection(boolean burnInProtection) {
        mBurnInProtection = burnInProtection;
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
        mBurnInProtection = bundle.getBoolean(EXTRA_LOW_BIT_AMBIENT, mBurnInProtection);
    }

    public void onTimeTick() {
        long timeInMillis = mFakeTime >= 0 ? mFakeTime : System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);

        if (mTimeView != null) {
            mTimeView.setText(DateFormat.format("hh:mm", timeInMillis));
            mTimeView.setVisibility(isDigitalEnabled() ? View.VISIBLE : View.GONE);
        }

        final int hour = calendar.get(Calendar.HOUR);
        final int second = calendar.get(Calendar.SECOND);
        final int minute = calendar.get(Calendar.MINUTE);
        float degrees = hour * 30 + minute / 2;
        if (mPartialRotationEnabled) {
            degrees += second / 120;
        }
        if (mHours != null) {
            mHours.setRotation(degrees);
            mHours.setVisibility(isDigitalEnabled() ? View.GONE : View.VISIBLE);
        }
        degrees = minute * 6;
        if (mPartialRotationEnabled) {
            degrees += second / 10;
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
        mAmbientModeEnabled = enabled;
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
}
