package com.xlythe.sample.clock;

import android.content.Context;
import android.view.View;

import com.xlythe.view.clock.ClockView;
import com.xlythe.watchface.clock.WatchfaceService;

public class MyWatchfaceService extends WatchfaceService {
    @Override
    public ClockView onCreateClockView(Context context) {
        ClockView clockView = (ClockView) View.inflate(context, R.layout.clock_view, null);
        clockView.setOnClickListener(v -> {
            clockView.setDigitalEnabled(!clockView.isDigitalEnabled());
        });
        return clockView;
    }
}
