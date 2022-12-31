package com.xlythe.sample.clock;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.watchface.editor.EditorSession;

import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.ComplicationView;
import com.xlythe.watchface.clock.utils.KotlinUtils.Continuation;

import static com.xlythe.watchface.clock.utils.KotlinUtils.continuation;

public class ConfigurationActivity extends AppCompatActivity {
  private static final String COMPONENT_NAME_KEY = "COMPONENT_NAME_KEY";
  private static final String INSTANCE_ID_KEY = "INSTANCE_ID_KEY";

  private ClockView mClockView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.clock_view);

    mClockView = findViewById(R.id.clockView);
    mClockView.setWatchFaceComponentName(getIntent().getParcelableExtra(COMPONENT_NAME_KEY));
    mClockView.setWatchFaceInstanceId(getIntent().getStringExtra(INSTANCE_ID_KEY));

    // TODO
    // How do I load the current complications?
    // How do I listen to changes to complications?

    EditorSession.createOnWatchEditorSession(this, new Continuation<EditorSession>() {
      @Override
      public void onUpdate(EditorSession editorSession) {
        editorSession.getWatchFaceId();
        for (ComplicationView view : mClockView.getComplicationViews()) {
          view.setOnClickListener(v -> editorSession.openComplicationDataSourceChooser(view.getComplicationId(), continuation()));
        }
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
    super.onStop();
    mClockView.stop();
  }
}
