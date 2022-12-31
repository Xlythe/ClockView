package com.xlythe.sample.clock;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xlythe.view.clock.ClockView;

public class ConfigurationActivity extends AppCompatActivity {
  private ClockView mClockView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.clock_view);

    mClockView = findViewById(R.id.clockView);
  }

  @Override
  protected void onStart() {
    super.onStart();
    mClockView.start();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mClockView.stop();
  }
}
