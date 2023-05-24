package com.xlythe.view.clock;

import static com.xlythe.watchface.clock.utils.KotlinUtils.addObserver;
import static com.xlythe.watchface.clock.utils.KotlinUtils.continuation;
import static com.xlythe.watchface.clock.utils.KotlinUtils.removeObserver;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Observer;
import androidx.wear.watchface.complications.ComplicationDataSourceInfo;
import androidx.wear.watchface.complications.data.EmptyComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.editor.EditorSession;

import com.xlythe.watchface.clock.utils.KotlinUtils;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * An adjustable clock view
 */
public class ClockView extends FrameLayout {
    public static final String TAG = ClockView.class.getSimpleName();
    public static final boolean DEBUG = false;
    static final String ACTION_WATCH_FACE_EDITOR = "androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR";

    private static final long ONE_MILLISECOND = 1;
    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * 1000;

    private static final String BUNDLE_SUPER = "super";
    private static final String EXTRA_DIGITAL_ENABLED = "digital_enabled";
    private static final String EXTRA_SECONDS_ENABLED = "seconds_enabled";
    private static final String EXTRA_MILLISECONDS_ENABLED = "milliseconds_enabled";
    private static final String EXTRA_AMBIENT_MODE_ENABLED = "ambient_mode_enabled";
    private static final String EXTRA_PARTIAL_ROTATION_ENABLED = "partial_rotation_enabled";
    private static final String EXTRA_LOW_BIT_AMBIENT = "low_bit_ambient";
    private static final String EXTRA_BURN_IN_PROTECTION = "burn_in_protection";

    // Debug logic
    private long mInvalidationCycle = -1;
    private int mInvalidationCount = 0;
    private static final int MAX_INVALIDATIONS_PER_SECOND = 1001;

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
    private boolean mMillisecondsEnabled = false;
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
    private long mTimeMillis = -1;

    private final Runnable mTicker = new Runnable() {
        @Override
        public void run() {
            onTimeTick();
            mHandler.postDelayed(this, isSecondsEnabled() ? isMillisecondsEnabled() ? ONE_MILLISECOND : ONE_SECOND : ONE_MINUTE);
        }
    };

    private boolean isStarted = false;

    // For overriding onTouch
    @Nullable private OnTouchListener mOnTouchListener;
    private final int mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
    private long mInitialMotionEventMillis;
    private View mTouchFocusView;

    private final List<Observer<Map<Integer, ComplicationDataSourceInfo>>> mComplicationDataObservers = new ArrayList<>();

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
            mSecondsEnabled = a.getBoolean(R.styleable.ClockView_showSeconds, mSecondsEnabled);
            mMillisecondsEnabled = a.getBoolean(R.styleable.ClockView_showMilliseconds, mMillisecondsEnabled);
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
    public void onDescendantInvalidated(View child, View target) {
        if (!isManualInvalidationEnabled()) {
            super.onDescendantInvalidated(child, target);
            return;
        }

        invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (mOnInvalidateListener != null) {
            mOnInvalidateListener.onInvalidate();
        }

        if (DEBUG) {
            if (getSecond() != mInvalidationCycle) {
                mInvalidationCycle = getSecond();
                mInvalidationCount = 0;
            } else {
                mInvalidationCount++;

                if (mInvalidationCount > MAX_INVALIDATIONS_PER_SECOND) {
                    throw new RuntimeException("Expected fewer than " + MAX_INVALIDATIONS_PER_SECOND + " invalidations per second but exceeded that number");
                }
            }
        }
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        super.setOnTouchListener(l);

        // There's no way to get the OnTouchListener unless we intercept it when its set.
        // So we'll do just that. This is used in #onTouchEvent.
        mOnTouchListener = l;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isManualInvalidationEnabled()) {
            return super.dispatchTouchEvent(event);
        }

        // By default, dispatching a touch event tries to identify which
        // (child) view should get the event before falling back to this
        // view. However, touch events are broken for all our children
        // views unless they're attached to a window (and they're not
        // attached to a window when used in a watchface), so we want to
        // immediately intercept it.
        return onTouchEvent(event);
    }

    /**
     * We override this to provide touch events to our children views.
     * This is optional for ClockView itself, because we properly intercept
     * #getHandler, #post and #postDelayed and that's enough to get touch
     * working, but we can't do the same for any of our children views.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isManualInvalidationEnabled()) {
            return super.onTouchEvent(event);
        }

        // If this view has a OnTouchListener, let the listener handle everything.
        if (mOnTouchListener != null && mOnTouchListener.onTouch(this, event)) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // In a down event, identify which child has a click/longClick
                // listener registered and press it.
                mInitialMotionEventMillis = event.getEventTime();
                mTouchFocusView = getTouchFocusView(this, event);
                if (mTouchFocusView != null) {
                    event = getLocalizedMotionEvent(event);
                }

                if (mTouchFocusView != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mTouchFocusView.drawableHotspotChanged(event.getX(), event.getY());
                    }
                    mTouchFocusView.setPressed(true);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                // In an up event, decide if the button was held down long
                // enough for a long click event or not.
                if (mTouchFocusView != null) {
                    mTouchFocusView.setPressed(false);

                    boolean touchHandled = false;
                    if (event.getEventTime() - mInitialMotionEventMillis > mLongPressTimeout && mTouchFocusView.isLongClickable()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            event = getLocalizedMotionEvent(event);
                            touchHandled = mTouchFocusView.performLongClick(event.getX(), event.getY());
                        } else {
                            touchHandled = mTouchFocusView.performLongClick();
                        }
                    }
                    if (!touchHandled && mTouchFocusView.isClickable()) {
                        mTouchFocusView.performClick();
                    }
                    mTouchFocusView = null;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                // In a cancel event, clean up modifications to the view.
                if (mTouchFocusView != null) {
                    mTouchFocusView.setPressed(false);
                    mTouchFocusView = null;
                    return true;
                }
                return false;
        }

        return false;
    }

    /**
     * Loops through the view and all its children until it finds one
     * with either an OnClickListener or OnLongClickListener set that
     * overlaps with the MotionEvent.
     */
    @Nullable
    private View getTouchFocusView(View view, MotionEvent event) {
        if (view.isClickable() || view.isLongClickable()) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            // Loop backwards so that views on top get focus first
            for (int i = viewGroup.getChildCount() - 1; i >= 0; i--) {
                View child = viewGroup.getChildAt(i);

                Rect rect = new Rect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                if (rect.contains((int) event.getX(), (int) event.getY())) {
                    MotionEvent localizedEvent = MotionEvent.obtain(event);
                    localizedEvent.offsetLocation(-child.getLeft(), -child.getTop());
                    View possibleTouchFocusView = getTouchFocusView(child, localizedEvent);
                    if (possibleTouchFocusView != null) {
                        return possibleTouchFocusView;
                    }
                }
            }
        }

        return null;
    }

    private MotionEvent getLocalizedMotionEvent(MotionEvent event) {
        return getLocalizedMotionEvent(this, event);
    }

    @Nullable
    private MotionEvent getLocalizedMotionEvent(ViewGroup viewGroup, MotionEvent event) {
        if (mTouchFocusView == null) {
            return event;
        }

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child == mTouchFocusView) {
                MotionEvent localizedEvent = MotionEvent.obtain(event);
                localizedEvent.offsetLocation(-child.getLeft(), -child.getTop());
                return localizedEvent;
            }

            if (child instanceof ViewGroup) {
                MotionEvent tempEvent = MotionEvent.obtain(event);
                tempEvent.offsetLocation(-child.getLeft(), -child.getTop());
                MotionEvent localizedEvent = getLocalizedMotionEvent((ViewGroup) child, tempEvent);
                tempEvent.recycle();
                if (localizedEvent != null) {
                    return localizedEvent;
                }
            }
        }

        return null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public void setTime(ZonedDateTime dateTime) {
        mDateTime = dateTime;
        mTimeMillis = -1;

        for (ComplicationView view : getComplicationViews()) {
            view.setTime(mDateTime);
        }
    }

    public void setTime(long timeInMillis) {
        mTimeMillis = timeInMillis;
        mDateTime = null;

        for (ComplicationView view : getComplicationViews()) {
            view.setTime(mTimeMillis);
        }
    }

    public void setTime(int hour, int minute) {
        setHour(hour);
        setMinute(minute);
    }

    public void setTime(int hour, int minute, int seconds) {
        setHour(hour);
        setMinute(minute);
        setSecond(seconds);
    }

    public long getTimeMillis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mDateTime != null) {
            return mDateTime.toInstant().toEpochMilli();
        } else {
            return mTimeMillis >= 0 ? mTimeMillis : System.currentTimeMillis();
        }
    }

    public void resetTime() {
        mTimeMillis = -1;
        mDateTime = null;
    }

    public void setHour(int hour) {
        Calendar calendar = Calendar.getInstance();
        if (mTimeMillis >= 0) {
            calendar.setTimeInMillis(mTimeMillis);
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        setTime(calendar.getTimeInMillis());
    }

    public int getHour() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mDateTime != null) {
            return mDateTime.getHour();
        } else {
            long timeInMillis = mTimeMillis >= 0 ? mTimeMillis : System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeInMillis);
            return calendar.get(Calendar.HOUR);
        }
    }

    public void setMinute(int minute) {
        Calendar calendar = Calendar.getInstance();
        if (mTimeMillis >= 0) {
            calendar.setTimeInMillis(mTimeMillis);
        }
        calendar.set(Calendar.MINUTE, minute);
        setTime(calendar.getTimeInMillis());
    }

    public int getMinute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mDateTime != null) {
            return mDateTime.getMinute();
        } else {
            long timeInMillis = mTimeMillis >= 0 ? mTimeMillis : System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeInMillis);
            return calendar.get(Calendar.MINUTE);
        }
    }

    public void setSecond(int second) {
        Calendar calendar = Calendar.getInstance();
        if (mTimeMillis >= 0) {
            calendar.setTimeInMillis(mTimeMillis);
        }
        calendar.set(Calendar.SECOND, second);
        setTime(calendar.getTimeInMillis());
    }

    public int getSecond() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mDateTime != null) {
            return mDateTime.getSecond();
        } else {
            long timeInMillis = mTimeMillis >= 0 ? mTimeMillis : System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeInMillis);
            return calendar.get(Calendar.SECOND);
        }
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void registerEditableComplicationObserver(EditorSession editorSession, ComplicationView view) {
        Observer<Map<Integer, ComplicationDataSourceInfo>> observer = idsToDataSourceInfo -> {
            ComplicationDataSourceInfo complicationDataSourceInfo = idsToDataSourceInfo.get(view.getComplicationId());
            if (complicationDataSourceInfo == null) {
                view.setComplicationData(new EmptyComplicationData());
                return;
            }

            view.setComplicationData(complicationDataSourceInfo.getFallbackPreviewData());
        };
        mComplicationDataObservers.add(observer);
        addObserver(editorSession.getComplicationsDataSourceInfo(), observer);
    }

    private void unregisterAllEditableComplicationObservers() {
        for (Observer<Map<Integer, ComplicationDataSourceInfo>> observer : mComplicationDataObservers) {
            removeObserver(observer);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterAllEditableComplicationObservers();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInWatchfaceEditor()) {
                try {
                    EditorSession.createOnWatchEditorSession((ComponentActivity) getContext(), new KotlinUtils.Continuation<EditorSession>() {
                        @Override
                        public void onUpdate(EditorSession editorSession) {
                            editorSession.setCommitChangesOnClose(true);
                            for (ComplicationView view : getComplicationViews()) {
                                view.setOnClickListener(v -> editorSession.openComplicationDataSourceChooser(view.getComplicationId(), continuation()));
                                registerEditableComplicationObserver(editorSession, view);
                            }
                        }
                    });
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Failed to load WearOS EditorSession. If you are using ComplicationView, please create ClockView in onCreate()", e);
                    for (ComplicationView view : getComplicationViews()) {
                        view.setComplicationData(new NoDataComplicationData());
                    }
                }
            } else {
                for (ComplicationView view : getComplicationViews()) {
                    view.setComplicationData(new NoDataComplicationData());
                }
            }
        }


        for (ComplicationView complicationView : getComplicationViews()) {
            complicationView.setLowBitAmbient(isLowBitAmbient());
            complicationView.setHasBurnInProtection(hasBurnInProtection());
            complicationView.setAmbientModeEnabled(isAmbientModeEnabled());
        }

        super.onFinishInflate();
    }

    private boolean isInWatchfaceEditor() {
        Context context = getContext();
        if (!(context instanceof ComponentActivity)) {
            return false;
        }

        Intent intent = ((ComponentActivity) context).getIntent();
        return ACTION_WATCH_FACE_EDITOR.equals(intent.getAction());
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

    public boolean isSecondsEnabled() {
        return !isAmbientModeEnabled() && mSecondsEnabled;
    }

    public void setSecondsEnabled(boolean enabled) {
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

    public boolean isMillisecondsEnabled() {
        return !isAmbientModeEnabled() && isDigitalEnabled() && isSecondsEnabled() && mMillisecondsEnabled;
    }

    public void setMillisecondsEnabled(boolean enabled) {
        if (mMillisecondsEnabled == enabled) {
            return;
        }

        mMillisecondsEnabled = enabled;
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
        for (ComplicationView complicationView : getComplicationViews()) {
            complicationView.setLowBitAmbient(lowBitAmbient);
        }
    }

    public boolean hasBurnInProtection() {
        return mBurnInProtection;
    }

    public void setHasBurnInProtection(boolean burnInProtection) {
        mBurnInProtection = burnInProtection;
        for (ComplicationView complicationView : getComplicationViews()) {
            complicationView.setHasBurnInProtection(burnInProtection);
        }
    }

    protected String getDateFormat() {
        StringBuilder format = new StringBuilder();
        format.append(DateFormat.is24HourFormat(getContext()) ? "HH" : "hh");
        format.append(":");
        format.append("mm");
        if (isSecondsEnabled()) {
            format.append(":");
            format.append("ss");
        }
        if (isSecondsEnabled() && isMillisecondsEnabled()) {
            format.append(".");
            format.append("SSS");
        }
        return format.toString();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_SUPER, super.onSaveInstanceState());
        bundle.putBoolean(EXTRA_DIGITAL_ENABLED, mDigitalEnabled);
        bundle.putBoolean(EXTRA_SECONDS_ENABLED, mSecondsEnabled);
        bundle.putBoolean(EXTRA_MILLISECONDS_ENABLED, mMillisecondsEnabled);
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
        mMillisecondsEnabled = bundle.getBoolean(EXTRA_MILLISECONDS_ENABLED, mMillisecondsEnabled);
        mAmbientModeEnabled = bundle.getBoolean(EXTRA_AMBIENT_MODE_ENABLED, mAmbientModeEnabled);
        mPartialRotationEnabled = bundle.getBoolean(EXTRA_PARTIAL_ROTATION_ENABLED, mPartialRotationEnabled);
        mLowBitAmbient = bundle.getBoolean(EXTRA_LOW_BIT_AMBIENT, mLowBitAmbient);
        mBurnInProtection = bundle.getBoolean(EXTRA_BURN_IN_PROTECTION, mBurnInProtection);
    }

    public void onTimeTick() {
        final int hour = getHour();
        final int minute = getMinute();
        final int second = getSecond();

        if (mTimeView != null) {
            final String formattedDate;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mDateTime != null) {
                formattedDate = mDateTime.format(DateTimeFormatter.ofPattern(getDateFormat()));
            } else {
                SimpleDateFormat formatter = new SimpleDateFormat(getDateFormat(), Locale.getDefault());
                formattedDate = formatter.format(new Date(getTimeMillis()));
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
            if (isSecondsEnabled() && !isDigitalEnabled()) {
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
        for (ComplicationView complicationView : getComplicationViews()) {
            complicationView.setAmbientModeEnabled(enabled);
        }
        onTimeTick();
        if (isStarted()) {
            stop();
            start();
        }
    }

    public Collection<ComplicationView> getComplicationViews() {
        return getComplicationViews(this);
    }

    private Collection<ComplicationView> getComplicationViews(ViewGroup root) {
        Set<ComplicationView> complicationViews = new HashSet<>();
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof ComplicationView) {
                complicationViews.add((ComplicationView) child);
            } else if (child instanceof ViewGroup) {
                complicationViews.addAll(getComplicationViews((ViewGroup) child));
            }
        }
        return complicationViews;
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
