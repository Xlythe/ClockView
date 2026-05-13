package com.xlythe.sample.clock;

import android.os.Bundle;
import android.widget.CheckBox;

import com.xlythe.view.clock.ClockView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ClockView mClockView;
    private CheckBox mCheckDigital;
    private CheckBox mCheckSeconds;
    private CheckBox mCheckMilliseconds;
    private CheckBox mCheckPartialRotation;
    private CheckBox mCheckAmbient;
    private CheckBox mCheckLowBit;
    private CheckBox mCheckBurnIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClockView = findViewById(R.id.clockView);
        mCheckDigital = findViewById(R.id.check_digital);
        mCheckSeconds = findViewById(R.id.check_seconds);
        mCheckMilliseconds = findViewById(R.id.check_milliseconds);
        mCheckPartialRotation = findViewById(R.id.check_partial_rotation);
        mCheckAmbient = findViewById(R.id.check_ambient);
        mCheckLowBit = findViewById(R.id.check_low_bit);
        mCheckBurnIn = findViewById(R.id.check_burn_in);

        mCheckDigital.setChecked(mClockView.isDigitalEnabled());
        mCheckSeconds.setChecked(mClockView.isSecondsEnabled());
        mCheckMilliseconds.setChecked(mClockView.isMillisecondsEnabled());
        mCheckPartialRotation.setChecked(mClockView.isPartialRotationEnabled());
        mCheckAmbient.setChecked(mClockView.isAmbientModeEnabled());
        mCheckLowBit.setChecked(mClockView.isLowBitAmbient());
        mCheckBurnIn.setChecked(mClockView.hasBurnInProtection());

        mCheckDigital.setOnCheckedChangeListener((buttonView, isChecked) -> mClockView.setDigitalEnabled(isChecked));
        mCheckSeconds.setOnCheckedChangeListener((buttonView, isChecked) -> mClockView.setSecondsEnabled(isChecked));
        mCheckMilliseconds.setOnCheckedChangeListener((buttonView, isChecked) -> mClockView.setMillisecondsEnabled(isChecked));
        mCheckPartialRotation.setOnCheckedChangeListener((buttonView, isChecked) -> mClockView.setPartialRotationEnabled(isChecked));
        mCheckAmbient.setOnCheckedChangeListener((buttonView, isChecked) -> mClockView.setAmbientModeEnabled(isChecked));
        mCheckLowBit.setOnCheckedChangeListener((buttonView, isChecked) -> mClockView.setLowBitAmbient(isChecked));
        mCheckBurnIn.setOnCheckedChangeListener((buttonView, isChecked) -> mClockView.setHasBurnInProtection(isChecked));

        mClockView.setOnClickListener(v -> {
            mClockView.setDigitalEnabled(!mClockView.isDigitalEnabled());
            mCheckDigital.setChecked(mClockView.isDigitalEnabled());
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
