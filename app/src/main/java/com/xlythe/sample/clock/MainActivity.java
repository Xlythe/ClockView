package com.xlythe.sample.clock;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xlythe.view.clock.ClockView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ClockView clockView = (ClockView) findViewById(R.id.clockView);
        clockView.start();

        clockView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clockView.setDigitalEnabled(!clockView.isDigitalEnabled());
            }
        });
    }
}