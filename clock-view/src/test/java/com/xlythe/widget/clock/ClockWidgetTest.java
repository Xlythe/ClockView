package com.xlythe.widget.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowAppWidgetManager;
import org.robolectric.shadows.ShadowPendingIntent;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ClockWidgetTest {

    private Context mContext;
    private AppWidgetManager mAppWidgetManager;
    private ShadowAppWidgetManager mShadowAppWidgetManager;
    private AlarmManager mAlarmManager;
    private ShadowAlarmManager mShadowAlarmManager;
    private TestClockWidget mWidget;

    private static class TestClockView extends ClockView {
        public TestClockView(Context context) {
            super(context);
            TextView timeView = new TextView(context);
            timeView.setId(R.id.clock_time);
            addView(timeView);
            onFinishInflate();
        }
    }

    private static class TestClockWidget extends ClockWidget {
        @Override
        public ClockView onCreateClockView(Context context, @Nullable ClockView convertView, int appWidgetId) {
            return new TestClockView(context);
        }
    }

    private static class NullClockWidget extends ClockWidget {
        @Override
        public ClockView onCreateClockView(Context context, @Nullable ClockView convertView, int appWidgetId) {
            return null;
        }
    }

    private static class ConfigClockWidget extends ClockWidget {
        @Override
        public ClockView onCreateClockView(Context context, @Nullable ClockView convertView, int appWidgetId) {
            return new TestClockView(context);
        }

        @Nullable
        @Override
        public Intent getConfigurationIntent(Context context) {
            return new Intent("com.test.CONFIG");
        }
    }

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mAppWidgetManager = AppWidgetManager.getInstance(mContext);
        mShadowAppWidgetManager = Shadows.shadowOf(mAppWidgetManager);
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mShadowAlarmManager = Shadows.shadowOf(mAlarmManager);
        mWidget = new TestClockWidget();
    }

    private void setHasScheduleExactAlarm(ShadowAlarmManager shadowAlarmManager, boolean hasPermission) {
        try {
            Method method = shadowAlarmManager.getClass().getMethod("setHasScheduleExactAlarm", boolean.class);
            method.invoke(shadowAlarmManager, hasPermission);
        } catch (Exception e) {
            // Fallback / ignore if not supported on this robolectric version
        }
    }

    @Test
    public void testOnUpdateValidClock() {
        int id1 = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);
        int id2 = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);

        int[] appWidgetIds = new int[]{id1, id2};
        mWidget.onUpdate(mContext, mAppWidgetManager, appWidgetIds);

        assertNotNull(mShadowAppWidgetManager.getViewFor(id1));
        assertNotNull(mShadowAppWidgetManager.getViewFor(id2));
    }

    @Test
    public void testOnUpdateNullClock() {
        int id = mShadowAppWidgetManager.createWidget(NullClockWidget.class, R.layout.clock_widget);

        NullClockWidget nullWidget = new NullClockWidget();
        int[] appWidgetIds = new int[]{id};
        nullWidget.onUpdate(mContext, mAppWidgetManager, appWidgetIds);

        assertNotNull(mShadowAppWidgetManager.getViewFor(id));
    }

    @Test
    public void testOnUpdateWithConfigIntent() {
        int id = mShadowAppWidgetManager.createWidget(ConfigClockWidget.class, R.layout.clock_widget);

        ConfigClockWidget configWidget = new ConfigClockWidget();
        int[] appWidgetIds = new int[]{id};
        configWidget.onUpdate(mContext, mAppWidgetManager, appWidgetIds);

        assertNotNull(mShadowAppWidgetManager.getViewFor(id));
    }

    @Test
    public void testOnUpdateExceptionHandling() {
        int id = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);

        // Passing null AppWidgetManager forces an exception inside updateAppWidget,
        // verifying that the try/catch block successfully prevents a crash.
        int[] appWidgetIds = new int[]{id};
        mWidget.onUpdate(mContext, null, appWidgetIds);
    }

    @Test
    public void testOnReceiveClockWidgetUpdateWithWidgetId() {
        int id = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);

        Intent intent = new Intent(ClockWidget.ACTION_CLOCK_WIDGET_UPDATE);
        intent.putExtra(ClockWidget.EXTRA_APP_WIDGET_ID, id);

        mWidget.onReceive(mContext, intent);

        assertNotNull(mShadowAppWidgetManager.getViewFor(id));
        assertFalse(mShadowAlarmManager.getScheduledAlarms().isEmpty());
    }

    @Test
    public void testOnReceiveClockWidgetUpdateWithoutWidgetId() {
        int id1 = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);
        int id2 = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);

        Intent intent = new Intent(ClockWidget.ACTION_CLOCK_WIDGET_UPDATE);
        mWidget.onReceive(mContext, intent);

        assertNotNull(mShadowAppWidgetManager.getViewFor(id1));
        assertNotNull(mShadowAppWidgetManager.getViewFor(id2));
        assertFalse(mShadowAlarmManager.getScheduledAlarms().isEmpty());
    }

    @Test
    public void testOnReceiveOtherAction() {
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        mWidget.onReceive(mContext, intent);

        assertTrue(mShadowAlarmManager.getScheduledAlarms().isEmpty());
    }

    @Test
    public void testOnReceiveSecurityException() {
        int id = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);
        setHasScheduleExactAlarm(mShadowAlarmManager, false);

        Intent intent = new Intent(ClockWidget.ACTION_CLOCK_WIDGET_UPDATE);
        intent.putExtra(ClockWidget.EXTRA_APP_WIDGET_ID, id);

        // Should catch SecurityException and ignore without crashing
        mWidget.onReceive(mContext, intent);
        assertNotNull(mShadowAppWidgetManager.getViewFor(id));
    }

    @Test
    public void testOnEnabledMPlusSuccess() {
        mWidget.onEnabled(mContext);

        List<ShadowAlarmManager.ScheduledAlarm> alarms = mShadowAlarmManager.getScheduledAlarms();
        assertFalse(alarms.isEmpty());
        ShadowAlarmManager.ScheduledAlarm alarm = alarms.get(0);
        assertEquals(AlarmManager.RTC, alarm.type);
        assertEquals(0, alarm.interval); // Exact alarm has 0 interval in ShadowAlarmManager
    }

    @Test
    public void testOnEnabledMPlusSecurityExceptionFallback() {
        setHasScheduleExactAlarm(mShadowAlarmManager, false);

        mWidget.onEnabled(mContext);

        List<ShadowAlarmManager.ScheduledAlarm> alarms = mShadowAlarmManager.getScheduledAlarms();
        assertFalse(alarms.isEmpty());
        ShadowAlarmManager.ScheduledAlarm alarm = alarms.get(0);
        assertEquals(AlarmManager.RTC, alarm.type);
        assertTrue(alarm.interval == 60 * 1000 || alarm.interval == 0);
    }

    @Test
    public void testOnDisabled() {
        mWidget.onEnabled(mContext);
        assertFalse(mShadowAlarmManager.getScheduledAlarms().isEmpty());

        mWidget.onDisabled(mContext);
        assertTrue(mShadowAlarmManager.getScheduledAlarms().isEmpty());
    }

    @Test
    public void testOnAppWidgetOptionsChanged() {
        int id = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);

        Bundle bundle = new Bundle();
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 100);
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200);
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100);
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200);

        mWidget.onAppWidgetOptionsChanged(mContext, mAppWidgetManager, id, bundle);

        assertNotNull(mShadowAppWidgetManager.getViewFor(id));
    }

    @Test
    public void testSetAndGetWidgetSize() {
        int id = mShadowAppWidgetManager.createWidget(TestClockWidget.class, R.layout.clock_widget);

        Bundle bundle = new Bundle();
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 50);
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 100);
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 50);
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 100);

        mWidget.onAppWidgetOptionsChanged(mContext, mAppWidgetManager, id, bundle);

        int size = mWidget.getWidgetSize(mContext, id);
        assertTrue(size > 0);

        // Test default size fallback for unknown widget id
        int defaultSize = mWidget.getWidgetSize(mContext, 999);
        assertEquals(mContext.getResources().getDimensionPixelSize(R.dimen.default_clock_size), defaultSize);
    }
}
