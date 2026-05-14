package com.xlythe.view.clock;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ClockViewTest {

    private Context mContext;
    private ClockView mClockView;
    private TextView mTimeView;
    private ImageView mSecondsView;
    private ImageView mMinutesView;
    private ImageView mHoursView;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mClockView = new ClockView(mContext);

        mTimeView = new TextView(mContext);
        mTimeView.setId(R.id.clock_time);
        mClockView.addView(mTimeView);

        mSecondsView = new ImageView(mContext);
        mSecondsView.setId(R.id.clock_seconds);
        mClockView.addView(mSecondsView);

        mMinutesView = new ImageView(mContext);
        mMinutesView.setId(R.id.clock_minutes);
        mClockView.addView(mMinutesView);

        mHoursView = new ImageView(mContext);
        mHoursView.setId(R.id.clock_hours);
        mClockView.addView(mHoursView);

        mClockView.onFinishInflate();
    }

    @Test
    public void testConstructorsAndFinishInflate() {
        ClockView view1 = new ClockView(mContext);
        assertNotNull(view1);

        ClockView view2 = new ClockView(mContext, null);
        assertNotNull(view2);

        ClockView view3 = new ClockView(mContext, null, 0);
        assertNotNull(view3);

        ClockView view4 = new ClockView(mContext, null, 0, 0);
        assertNotNull(view4);

        // Test onFinishInflate without any children added, expecting IllegalStateException
        ClockView emptyView = new ClockView(mContext);
        try {
            emptyView.onFinishInflate();
            fail("Expected IllegalStateException for missing clock children");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Before inflating this View, you must include at least one child"));
        }

        // Test onFinishInflate with only digital child
        ClockView digitalView = new ClockView(mContext);
        TextView timeChild = new TextView(mContext);
        timeChild.setId(R.id.clock_time);
        digitalView.addView(timeChild);
        digitalView.onFinishInflate();
        assertTrue(digitalView.supportsDigital());
        assertFalse(digitalView.supportsAnalog());

        // Test onFinishInflate with only analog children
        ClockView analogView = new ClockView(mContext);
        ImageView hoursChild = new ImageView(mContext);
        hoursChild.setId(R.id.clock_hours);
        analogView.addView(hoursChild);
        analogView.onFinishInflate();
        assertTrue(analogView.supportsAnalog());
        assertFalse(analogView.supportsDigital());
    }

    @Test
    public void testInitWithAttributes() {
        AttributeSet attrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.clockStyle, "1") // digital
                .addAttribute(R.attr.showSeconds, "false")
                .addAttribute(R.attr.showMilliseconds, "true")
                .addAttribute(R.attr.partialRotation, "true")
                .addAttribute(R.attr.lowBitAmbient, "true")
                .addAttribute(R.attr.hasBurnInProtection, "true")
                .addAttribute(R.attr.ambientModeEnabled, "true")
                .build();

        ClockView clockView = new ClockView(mContext, attrs);
        TextView timeChild = new TextView(mContext);
        timeChild.setId(R.id.clock_time);
        clockView.addView(timeChild);
        clockView.onFinishInflate();

        assertTrue(clockView.isDigitalEnabled());
        assertFalse(clockView.isSecondsEnabled()); // ambient mode disables seconds
        assertTrue(clockView.isPartialRotationEnabled());
        assertTrue(clockView.isLowBitAmbient());
        assertTrue(clockView.hasBurnInProtection());
        assertTrue(clockView.isAmbientModeEnabled());
    }

    @Test
    public void testSettersAndGetters() {
        mClockView.setDigitalEnabled(true);
        assertTrue(mClockView.isDigitalEnabled());

        mClockView.setDigitalEnabled(false);
        assertFalse(mClockView.isDigitalEnabled());

        // Test restart on setDigitalEnabled
        mClockView.start();
        assertTrue(mClockView.isStarted());
        mClockView.setDigitalEnabled(true);
        assertTrue(mClockView.isStarted());

        mClockView.setSecondsEnabled(false);
        assertFalse(mClockView.isSecondsEnabled());
        mClockView.setSecondsEnabled(true);
        assertTrue(mClockView.isSecondsEnabled());

        mClockView.setAmbientModeEnabled(false);
        mClockView.setMillisecondsEnabled(true);
        assertTrue(mClockView.isMillisecondsEnabled());

        mClockView.setPartialRotationEnabled(true);
        assertTrue(mClockView.isPartialRotationEnabled());

        // Test ComplicationView children updates
        ComplicationView complicationView = new ComplicationView(mContext);
        mClockView.addView(complicationView);

        mClockView.setLowBitAmbient(true);
        assertTrue(mClockView.isLowBitAmbient());
        assertTrue(complicationView.isLowBitAmbient());

        mClockView.setHasBurnInProtection(true);
        assertTrue(mClockView.hasBurnInProtection());
        assertTrue(complicationView.hasBurnInProtection());

        mClockView.setAmbientModeEnabled(true);
        assertTrue(mClockView.isAmbientModeEnabled());
        assertTrue(complicationView.isAmbientModeEnabled());
    }

    @Test
    public void testTimeSettersAndGetters() {
        ZonedDateTime dateTime = ZonedDateTime.parse("2026-05-14T10:15:30+00:00[UTC]");
        mClockView.setTime(dateTime);
        assertEquals(dateTime.toInstant().toEpochMilli(), mClockView.getTimeMillis());
        assertEquals(10, mClockView.getHour());
        assertEquals(15, mClockView.getMinute());
        assertEquals(30, mClockView.getSecond());

        long millis = 1500000000000L;
        mClockView.setTime(millis);
        assertEquals(millis, mClockView.getTimeMillis());

        mClockView.setTime(8, 30);
        assertEquals(8, mClockView.getHour());
        assertEquals(30, mClockView.getMinute());

        mClockView.setTime(9, 45, 15);
        assertEquals(9, mClockView.getHour());
        assertEquals(45, mClockView.getMinute());
        assertEquals(15, mClockView.getSecond());

        mClockView.resetTime();
        assertTrue(mClockView.getTimeMillis() <= System.currentTimeMillis());
    }

    @Test
    public void testStartStopAndOnLayout() {
        assertFalse(mClockView.isStarted());
        mClockView.start();
        assertTrue(mClockView.isStarted());

        mClockView.stop();
        assertFalse(mClockView.isStarted());

        AtomicBoolean ticked = new AtomicBoolean(false);
        mClockView.setOnTimeTickListener(() -> ticked.set(true));
        mClockView.layout(0, 0, 100, 100);
        assertTrue(ticked.get());
    }

    @Test
    public void testOnTimeTickDigitalAndAnalog() {
        // Test digital formatting
        mClockView.setDigitalEnabled(true);
        mClockView.setSecondsEnabled(true);
        mClockView.setMillisecondsEnabled(false);
        mClockView.setTime(ZonedDateTime.parse("2026-05-14T10:15:30+00:00[UTC]"));
        mClockView.onTimeTick();
        assertEquals(View.VISIBLE, mTimeView.getVisibility());
        assertNotNull(mTimeView.getText());

        // Test analog rotation without partial rotation
        mClockView.setDigitalEnabled(false);
        mClockView.setPartialRotationEnabled(false);
        mClockView.setTime(ZonedDateTime.parse("2026-05-14T06:30:15+00:00[UTC]"));
        mClockView.onTimeTick();
        // 6 hours, 30 mins -> hour degree = 6 * 30 + 30 / 2f = 180 + 15 = 195f
        assertEquals(195f, mHoursView.getRotation(), 0.001f);
        // 30 mins -> min degree = 30 * 6 = 180f
        assertEquals(180f, mMinutesView.getRotation(), 0.001f);
        // 15 secs -> sec degree = 15 * 6 = 90f
        assertEquals(90f, mSecondsView.getRotation(), 0.001f);
        assertEquals(View.VISIBLE, mSecondsView.getVisibility());

        // Test analog rotation WITH partial rotation
        mClockView.setPartialRotationEnabled(true);
        mClockView.setTime(ZonedDateTime.parse("2026-05-14T06:30:15+00:00[UTC]"));
        mClockView.onTimeTick();
        // hour degree = 195f + 15 / 120f = 195.125f
        assertEquals(195.125f, mHoursView.getRotation(), 0.001f);
        // min degree = 180f + 15 / 10f = 181.5f
        assertEquals(181.5f, mMinutesView.getRotation(), 0.001f);
    }

    @Test
    public void testSaveAndRestoreInstanceState() {
        mClockView.setDigitalEnabled(true);
        mClockView.setSecondsEnabled(false);
        mClockView.setMillisecondsEnabled(true);
        mClockView.setAmbientModeEnabled(true);
        mClockView.setPartialRotationEnabled(true);
        mClockView.setLowBitAmbient(true);
        mClockView.setHasBurnInProtection(true);
        ZonedDateTime dateTime = ZonedDateTime.parse("2026-05-14T10:15:30+00:00[UTC]");
        mClockView.setTime(dateTime);

        Parcelable state = mClockView.onSaveInstanceState();
        assertNotNull(state);

        ClockView restoredView = new ClockView(mContext);
        restoredView.onRestoreInstanceState(state);

        assertTrue(restoredView.isDigitalEnabled());
        assertFalse(restoredView.isSecondsEnabled());
        assertTrue(restoredView.isPartialRotationEnabled());
        assertTrue(restoredView.isLowBitAmbient());
        assertTrue(restoredView.hasBurnInProtection());
        assertTrue(restoredView.isAmbientModeEnabled());
        assertEquals(dateTime.toInstant().toEpochMilli(), restoredView.getTimeMillis());
    }

    @Test
    public void testManualInvalidationAndHandlerInterception() {
        AtomicBoolean invalidated = new AtomicBoolean(false);
        mClockView.setOnInvalidateListener(() -> invalidated.set(true));
        assertNotNull(mClockView.getOnInvalidateListener());

        assertNotNull(mClockView.getHandler());

        AtomicBoolean posted = new AtomicBoolean(false);
        mClockView.post(() -> posted.set(true));
        ShadowLooper.runUiThreadTasks();
        assertTrue(posted.get());

        AtomicBoolean postedDelayed = new AtomicBoolean(false);
        mClockView.postDelayed(() -> postedDelayed.set(true), 100);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertTrue(postedDelayed.get());

        mClockView.postInvalidateDelayed(50);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertTrue(invalidated.get());

        invalidated.set(false);
        Drawable mockDrawable = mock(Drawable.class);
        mClockView.invalidateDrawable(mockDrawable);
        assertTrue(invalidated.get());

        invalidated.set(false);
        mClockView.onDescendantInvalidated(mClockView, mClockView);
        assertTrue(invalidated.get());

        mClockView.setBackground(mockDrawable);
        AtomicBoolean scheduled = new AtomicBoolean(false);
        Runnable scheduleRunnable = () -> scheduled.set(true);
        mClockView.scheduleDrawable(mockDrawable, scheduleRunnable, 100);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertTrue(scheduled.get());

        mClockView.unscheduleDrawable(mockDrawable, scheduleRunnable);
        mClockView.unscheduleDrawable(mockDrawable);
    }

    @Test
    public void testTouchEventsWithManualInvalidation() {
        AtomicBoolean touched = new AtomicBoolean(false);
        mClockView.setOnTouchListener((v, event) -> {
            touched.set(true);
            return true;
        });
        mClockView.setOnInvalidateListener(() -> {});
        mClockView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
        assertTrue(touched.get());

        // Remove OnTouchListener to test child view dispatching
        mClockView.setOnTouchListener(null);

        View clickableChild = new View(mContext);
        clickableChild.setClickable(true);
        AtomicBoolean childClicked = new AtomicBoolean(false);
        clickableChild.setOnClickListener(v -> childClicked.set(true));
        mClockView.addView(clickableChild);

        mClockView.layout(0, 0, 200, 200);
        clickableChild.layout(10, 10, 100, 100);

        MotionEvent downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50, 50, 0);
        assertTrue(mClockView.dispatchTouchEvent(downEvent));
        assertTrue(clickableChild.isPressed());

        MotionEvent upEvent = MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, 50, 50, 0);
        assertTrue(mClockView.dispatchTouchEvent(upEvent));
        assertFalse(clickableChild.isPressed());
        assertTrue(childClicked.get());

        // Test Long Click
        View longClickableChild = new View(mContext);
        longClickableChild.setLongClickable(true);
        AtomicBoolean childLongClicked = new AtomicBoolean(false);
        longClickableChild.setOnLongClickListener(v -> {
            childLongClicked.set(true);
            return true;
        });
        mClockView.addView(longClickableChild);
        longClickableChild.layout(110, 110, 190, 190);

        long downTime = System.currentTimeMillis();
        MotionEvent downEvent2 = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 150, 150, 0);
        assertTrue(mClockView.dispatchTouchEvent(downEvent2));
        assertTrue(longClickableChild.isPressed());

        MotionEvent upEvent2 = MotionEvent.obtain(downTime, downTime + 600, MotionEvent.ACTION_UP, 150, 150, 0);
        assertTrue(mClockView.dispatchTouchEvent(upEvent2));
        assertFalse(longClickableChild.isPressed());
        assertTrue(childLongClicked.get());

        // Test Cancel
        long downTime3 = System.currentTimeMillis();
        MotionEvent downEvent3 = MotionEvent.obtain(downTime3, downTime3, MotionEvent.ACTION_DOWN, 150, 150, 0);
        assertTrue(mClockView.dispatchTouchEvent(downEvent3));
        assertTrue(longClickableChild.isPressed());

        MotionEvent cancelEvent = MotionEvent.obtain(downTime3, downTime3 + 100, MotionEvent.ACTION_CANCEL, 150, 150, 0);
        assertTrue(mClockView.dispatchTouchEvent(cancelEvent));
        assertFalse(longClickableChild.isPressed());
    }

    @Test
    public void testWatchfaceEditorContext() {
        Intent intent = new Intent(ClockView.ACTION_WATCH_FACE_EDITOR);
        ComponentActivity activity = Robolectric.buildActivity(ComponentActivity.class, intent).get();

        ClockView clockView = new ClockView(activity);
        TextView timeView = new TextView(activity);
        timeView.setId(R.id.clock_time);
        clockView.addView(timeView);

        ComplicationView complicationView = new ComplicationView(activity);
        clockView.addView(complicationView);

        clockView.onFinishInflate();
        clockView.onDetachedFromWindow();
    }
}
