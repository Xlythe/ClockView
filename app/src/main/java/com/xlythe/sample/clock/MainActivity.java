package com.xlythe.sample.clock;

import android.os.Bundle;

import com.xlythe.view.clock.ClockView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ClockView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClockView = findViewById(R.id.clockView);

        mClockView.setOnClickListener(v -> mClockView.setDigitalEnabled(!mClockView.isDigitalEnabled()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClockView.start();
    }

    @Override
    protected void onStop() {
        mClockView.stop();
        super.onStop();
    }
}
