package com.xlythe.sample.clock;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xlythe.view.clock.ClockView;

public class MainActivity extends AppCompatActivity {
    private ClockView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClockView = (ClockView) findViewById(R.id.clockView);

        mClockView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClockView.setDigitalEnabled(!mClockView.isDigitalEnabled());
            }
        });
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
