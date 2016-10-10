package com.xlythe.sample.clock;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;

import com.xlythe.view.clock.ClockView;
import com.xlythe.widget.clock.ClockWidget;

public class MyClockWidget extends ClockWidget {
    @Override
    public ClockView onCreateClockView(Context context, @Nullable ClockView convertView, int appWidgetId) {
        if (convertView != null) {
            return convertView;
        }
        return (ClockView) View.inflate(context, R.layout.clock_view, null);
    }
}
