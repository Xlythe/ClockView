package com.xlythe.sample.clock;

import android.content.Context;
import android.view.View;

import com.xlythe.view.clock.ClockView;
import com.xlythe.widget.clock.ClockWidget;

public class MyClockWidget extends ClockWidget {
    @Override
    public ClockView onCreateClockView(Context context) {
        return (ClockView) View.inflate(context, R.layout.clock_view, null);
    }
}
