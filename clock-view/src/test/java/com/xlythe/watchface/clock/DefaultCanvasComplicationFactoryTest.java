package com.xlythe.watchface.clock;

import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.watchface.BoundingArc;
import androidx.wear.watchface.CanvasComplication;
import androidx.wear.watchface.RenderParameters;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.EmptyComplicationData;

import com.xlythe.view.clock.ComplicationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DefaultCanvasComplicationFactoryTest {

    private ComplicationView mComplicationView;
    private DefaultCanvasComplicationFactory mFactory;
    private WatchState mWatchState;
    private CanvasComplication.InvalidateCallback mInvalidateCallback;

    @Before
    public void setUp() {
        mComplicationView = mock(ComplicationView.class);
        mFactory = new DefaultCanvasComplicationFactory(mComplicationView);
        mWatchState = mock(WatchState.class);
        mInvalidateCallback = mock(CanvasComplication.InvalidateCallback.class);
    }

    @Test
    public void testCreateCanvasComplication() {
        CanvasComplication complication = mFactory.create(mWatchState, mInvalidateCallback);
        assertNotNull(complication);
    }

    @Test
    public void testCanvasComplicationDataInteraction() {
        CanvasComplication complication = mFactory.create(mWatchState, mInvalidateCallback);

        ComplicationData mockData = new EmptyComplicationData();
        when(mComplicationView.getComplicationData()).thenReturn(mockData);

        assertEquals(mockData, complication.getData());

        ComplicationData newData = new EmptyComplicationData();
        complication.loadData(newData, true);
        verify(mComplicationView).setComplicationData(newData);
    }

    @Test
    public void testCanvasComplicationRenderMethods() {
        CanvasComplication complication = mFactory.create(mWatchState, mInvalidateCallback);

        Renderer renderer = mock(Renderer.class);
        complication.onRendererCreated(renderer);

        Canvas canvas = mock(Canvas.class);
        Rect rect = new Rect(0, 0, 100, 100);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        RenderParameters renderParameters = mock(RenderParameters.class);

        complication.render(canvas, rect, zonedDateTime, renderParameters, 1);
        complication.drawHighlight(canvas, rect, 1, zonedDateTime, 0xFF0000);
        complication.drawHighlight(canvas, rect, 1, zonedDateTime, 0xFF0000, mock(BoundingArc.class));
    }
}
