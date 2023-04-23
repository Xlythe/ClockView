package com.xlythe.widget.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.R;
import com.xlythe.view.clock.utils.BitmapUtils;
import com.xlythe.view.clock.utils.MathUtils;

import androidx.annotation.Nullable;

public abstract class ClockWidget extends AppWidgetProvider {
    public static final String ACTION_CLOCK_WIDGET_UPDATE = "com.xlythe.widget.clock.CLOCK_WIDGET_UPDATE";
    public static final String EXTRA_APP_WIDGET_ID = "app_widget_id";
    private static final String TAG = ClockWidget.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final String PREFERENCE_PREAMBLE = "settings_";
    private static final String PREFERENCE_WIDGET_SIZE_PREAMBLE = PREFERENCE_PREAMBLE + "widget_size_";

    private ClockView mClockView;

    private static String TAG(int appId) {
        return TAG + "[" + appId + "]";
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        for (int appWidgetID : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetID);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_CLOCK_WIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (intent.hasExtra(EXTRA_APP_WIDGET_ID)) {
                updateAppWidget(context, appWidgetManager, intent.getIntExtra(EXTRA_APP_WIDGET_ID, 0));
            } else {
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
                for (int appWidgetID : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetID);
                }
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    public abstract ClockView onCreateClockView(Context context, @Nullable ClockView convertView, int appWidgetId);

    @Nullable
    public Intent getConfigurationIntent(Context context) {
        return null;
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Create a clock view
        mClockView = onCreateClockView(context, mClockView, appWidgetId);
        if (mClockView == null) {
            Log.w(TAG(appWidgetId), "Ignoring widget. No clock provided.");
            return;
        }

        mClockView.setSecondHandEnabled(false);

        // Set the bounds
        int clockSize = getWidgetSize(context, appWidgetId);
        Rect rect = new Rect(0, 0, clockSize, clockSize);

        // Invalidate the clock (requires being measured first)
        mClockView.onTimeTick();

        // Draw the view onto the widget
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.clock_widget);
        remoteViews.setImageViewBitmap(R.id.content, BitmapUtils.draw(mClockView, rect));

        // Launch a config activity when tapped (if set up)
        Intent configIntent = getConfigurationIntent(context);
        if (configIntent != null) {
            // Set a custom data field so that this intent is treated separately from other PendingIntents to the same receiver
            configIntent.setData(new Uri.Builder().scheme("app").authority(context.getPackageName()).appendPath(Integer.toString(appWidgetId)).build());

            configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            configIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int flags = (Build.VERSION.SDK_INT >= 23) ? PendingIntent.FLAG_IMMUTABLE : 0;
            remoteViews.setOnClickPendingIntent(R.id.content, PendingIntent.getActivity(context, appWidgetId, configIntent, flags));
        }

        if (DEBUG) {
            Log.v(TAG(appWidgetId), "Updating the widget ui");
        }
        try {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        } catch (Exception e) {
            Log.e(TAG(appWidgetId), "Error updating ui!", e);
        }
    }

    private PendingIntent createClockTickIntent(Context context) {
        Intent intent = new Intent(ACTION_CLOCK_WIDGET_UPDATE);
        intent.setPackage(context.getPackageName());
        int flags = (Build.VERSION.SDK_INT >= 23) ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 60 * 1000, createClockTickIntent(context));
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createClockTickIntent(context));
        super.onDisabled(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        setWidgetSize(context, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    protected void setWidgetSize(Context context, int appWidgetId, Bundle bundle) {
        if (Build.VERSION.SDK_INT >= 16) {
            int minWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int maxWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int minHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int maxHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            int min = MathUtils.min(minWidth, maxWidth, minHeight, maxHeight);
            int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, min, context.getResources().getDisplayMetrics());
            getSharedPreferences(context).edit().putInt(PREFERENCE_WIDGET_SIZE_PREAMBLE + appWidgetId, pixels).apply();
        }
    }

    protected int getWidgetSize(Context context, int appWidgetId) {
        return getSharedPreferences(context).getInt(PREFERENCE_WIDGET_SIZE_PREAMBLE + appWidgetId,
                context.getResources().getDimensionPixelSize(R.dimen.default_clock_size));
    }

    protected SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
    }
}
