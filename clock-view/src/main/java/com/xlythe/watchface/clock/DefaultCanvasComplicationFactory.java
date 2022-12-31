package com.xlythe.watchface.clock;

import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.watchface.BoundingArc;
import androidx.wear.watchface.CanvasComplication;
import androidx.wear.watchface.CanvasComplicationFactory;
import androidx.wear.watchface.RenderParameters;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.complications.data.ComplicationData;

import com.xlythe.view.clock.ComplicationView;

import java.time.ZonedDateTime;

class DefaultCanvasComplicationFactory implements CanvasComplicationFactory {
  private final ComplicationView mComplicationView;

  DefaultCanvasComplicationFactory(ComplicationView complicationView) {
    this.mComplicationView = complicationView;
  }

  @NonNull
  @Override
  public CanvasComplication create(@NonNull WatchState watchState, @NonNull CanvasComplication.InvalidateCallback invalidateCallback) {
    return new CanvasComplication() {
      @Override
      public void onRendererCreated(@NonNull Renderer renderer) {

      }

      @Override
      public void render(@NonNull Canvas canvas, @NonNull Rect rect, @NonNull ZonedDateTime zonedDateTime, @NonNull RenderParameters renderParameters, int id) {

      }

      @Override
      public void drawHighlight(@NonNull Canvas canvas, @NonNull Rect rect, int boundsType, @NonNull ZonedDateTime zonedDateTime, int color) {

      }

      @Override
      public void drawHighlight(@NonNull Canvas canvas, @NonNull Rect rect, int boundsType, @NonNull ZonedDateTime zonedDateTime, int color, @Nullable BoundingArc boundingArc) {

      }

      @NonNull
      @Override
      public ComplicationData getData() {
        return mComplicationView.getComplicationData();
      }

      @Override
      public void loadData(@NonNull ComplicationData complicationData, boolean loadDrawablesAsynchronous) {
        mComplicationView.setComplicationData(complicationData);
      }
    };
  }
}
