package com.xlythe.watchface.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.watchface.ComplicationSlot;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.DrawMode;
import androidx.wear.watchface.RenderParameters;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.TapEvent;
import androidx.wear.watchface.TapType;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicies;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.EmptyComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.style.CurrentUserStyleRepository;
import androidx.wear.watchface.style.UserStyle;

import com.xlythe.view.clock.ClockView;
import com.xlythe.view.clock.ComplicationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.flow.StateFlow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class WatchfaceServiceTest {

    private Context mContext;
    private TestWatchfaceService mService;
    private CurrentUserStyleRepository mCurrentUserStyleRepository;
    private StateFlow<UserStyle> mUserStyleStateFlow;
    private FlowCollector<UserStyle> mUserStyleCollector;
    private UserStyle mUserStyle;
    private WatchState mWatchState;
    private StateFlow<Boolean> mAmbientStateFlow;
    private FlowCollector<Boolean> mAmbientCollector;
    private ComplicationSlot mComplicationSlot;
    private StateFlow<ComplicationData> mComplicationDataStateFlow;
    private FlowCollector<ComplicationData> mComplicationDataCollector;
    private ComplicationData mComplicationData;
    private ComplicationSlotsManager mComplicationSlotsManager;
    private SurfaceHolder mSurfaceHolder;
    private Continuation<WatchFace> mContinuation;

    public static class TestWatchfaceService extends WatchfaceService {
        ClockView clockView;
        ComplicationView complicationView;
        boolean userStyleChanged = false;

        @Override
        public ClockView onCreateClockView(Context context) {
            if (clockView == null) {
                clockView = new ClockView(context);
                complicationView = new ComplicationView(context);
                complicationView.setComplicationId(123);
                clockView.addView(complicationView);
            }
            return clockView;
        }

        @Override
        protected void onUserStyleChanged(UserStyle userStyle) {
            super.onUserStyleChanged(userStyle);
            userStyleChanged = true;
        }
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mService = Robolectric.buildService(TestWatchfaceService.class).create().get();

        mCurrentUserStyleRepository = mock(CurrentUserStyleRepository.class);
        mUserStyle = mock(UserStyle.class);
        mUserStyleStateFlow = mock(StateFlow.class);
        when(mCurrentUserStyleRepository.getUserStyle()).thenReturn(mUserStyleStateFlow);
        when(mUserStyleStateFlow.getValue()).thenReturn(mUserStyle);
        doAnswer(invocation -> {
            mUserStyleCollector = invocation.getArgument(0);
            return null;
        }).when(mUserStyleStateFlow).collect(any(), (Continuation) null);

        mWatchState = mock(WatchState.class);
        mAmbientStateFlow = mock(StateFlow.class);
        when(mWatchState.isAmbient()).thenReturn(mAmbientStateFlow);
        when(mAmbientStateFlow.getValue()).thenReturn(false);
        when(mWatchState.hasBurnInProtection()).thenReturn(true);
        when(mWatchState.hasLowBitAmbient()).thenReturn(true);
        doAnswer(invocation -> {
            mAmbientCollector = invocation.getArgument(0);
            return null;
        }).when(mAmbientStateFlow).collect(any(), (Continuation) null);

        mComplicationSlot = mock(ComplicationSlot.class);
        mComplicationData = new EmptyComplicationData();
        mComplicationDataStateFlow = mock(StateFlow.class);
        when(mComplicationSlot.getComplicationData()).thenReturn(mComplicationDataStateFlow);
        when(mComplicationDataStateFlow.getValue()).thenReturn(mComplicationData);
        doAnswer(invocation -> {
            mComplicationDataCollector = invocation.getArgument(0);
            return null;
        }).when(mComplicationDataStateFlow).collect(any(), (Continuation) null);

        mComplicationSlotsManager = mock(ComplicationSlotsManager.class);
        Map<Integer, ComplicationSlot> slotsMap = new HashMap<>();
        slotsMap.put(123, mComplicationSlot);
        when(mComplicationSlotsManager.getComplicationSlots()).thenReturn(slotsMap);

        mSurfaceHolder = mock(SurfaceHolder.class);
        when(mSurfaceHolder.getSurfaceFrame()).thenReturn(new Rect(0, 0, 400, 400));

        mContinuation = mock(Continuation.class);
    }

    private ComplicationText createComplicationText(String text) {
        if (text == null) return null;
        ComplicationText compText = mock(ComplicationText.class);
        when(compText.getTextAt(any(), any())).thenReturn(text);
        when(compText.isPlaceholder()).thenReturn(false);
        when(compText.isAlwaysEmpty()).thenReturn(text.isEmpty());
        when(compText.getNextChangeTime(any())).thenReturn(Instant.MAX);
        when(compText.toWireComplicationText()).thenReturn(mock(android.support.wearable.complications.ComplicationText.class));
        return compText;
    }

    private WatchFace.TapListener getTapListener(WatchFace watchFace) {
        try {
            Field field = watchFace.getClass().getDeclaredField("tapListener");
            field.setAccessible(true);
            return (WatchFace.TapListener) field.get(watchFace);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get tapListener", ex);
        }
    }

    private void setRenderParameters(Object renderer, RenderParameters renderParameters) {
        try {
            Class<?> clazz = renderer.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField("renderParameters");
                    field.setAccessible(true);
                    field.set(renderer, renderParameters);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set renderParameters", e);
        }
    }

    @Test
    public void testCreateClockViewAndComplicationSlotsManager() {
        ComplicationSlotsManager manager = mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        assertNotNull(manager);
        assertNotNull(mService.clockView);
        assertTrue(mService.clockView.isStarted());
    }

    @Test
    public void testUserStyleObserver() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        assertNotNull(mUserStyleCollector);

        UserStyle newUserStyle = mock(UserStyle.class);
        try {
            mUserStyleCollector.emit(newUserStyle, mock(Continuation.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertTrue(mService.userStyleChanged);
    }

    @Test
    public void testAmbientModeObserver() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);
        assertNotNull(mAmbientCollector);

        try {
            mAmbientCollector.emit(true, mock(Continuation.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertTrue(mService.clockView.isAmbientModeEnabled());
    }

    @Test
    public void testComplicationDataObserver() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);
        assertNotNull(mComplicationDataCollector);

        ShortTextComplicationData newData = new ShortTextComplicationData.Builder(createComplicationText("Text"), createComplicationText("Desc"))
                .setTitle(createComplicationText("Title"))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        try {
            mComplicationDataCollector.emit(newData, mock(Continuation.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertEquals(newData, mService.complicationView.getComplicationData());
    }

    @Test
    public void testCreateWatchFaceAndTapListener() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        WatchFace watchFace = mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);
        assertNotNull(watchFace);

        WatchFace.TapListener tapListener = getTapListener(watchFace);
        assertNotNull(tapListener);

        AtomicBoolean touchReceived = new AtomicBoolean(false);
        mService.clockView.setOnTouchListener((v, event) -> {
            touchReceived.set(true);
            return true;
        });

        TapEvent tapEvent = mock(TapEvent.class);
        when(tapEvent.getXPos()).thenReturn(100);
        when(tapEvent.getYPos()).thenReturn(100);
        when(tapEvent.getTapTime()).thenReturn(Instant.now());

        // Test TapType.DOWN
        tapListener.onTapEvent(TapType.DOWN, tapEvent, mComplicationSlot);
        assertTrue(touchReceived.get());

        // Test TapType.UP
        touchReceived.set(false);
        tapListener.onTapEvent(TapType.UP, tapEvent, mComplicationSlot);
        assertTrue(touchReceived.get());

        // Test TapType.CANCEL
        touchReceived.set(false);
        tapListener.onTapEvent(TapType.CANCEL, tapEvent, mComplicationSlot);
        assertTrue(touchReceived.get());

        // Test unknown tap type
        touchReceived.set(false);
        tapListener.onTapEvent(999, tapEvent, mComplicationSlot);
        assertFalse(touchReceived.get());
    }

    @Test
    public void testOnDestroy() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);

        assertTrue(mService.clockView.isStarted());
        mService.onDestroy();
        assertFalse(mService.clockView.isStarted());
    }

    @Test
    public void testInvalidate() {
        // Call invalidate before renderer is created
        mService.invalidate();

        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);

        // Call invalidate after renderer is created
        mService.invalidate();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWatchfaceRendererRender() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        WatchFace watchFace = mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);

        Renderer.CanvasRenderer2<Renderer.SharedAssets> renderer = (Renderer.CanvasRenderer2<Renderer.SharedAssets>) watchFace.getRenderer();
        assertNotNull(renderer);

        Renderer.SharedAssets sharedAssets = mock(Renderer.SharedAssets.class);
        assertNotNull(sharedAssets);

        Canvas canvas = mock(Canvas.class);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        // Test square bounds
        renderer.render(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);

        // Test width < height bounds
        renderer.render(canvas, new Rect(0, 0, 300, 400), zonedDateTime, sharedAssets);

        // Test width > height bounds
        renderer.render(canvas, new Rect(0, 0, 400, 300), zonedDateTime, sharedAssets);

        // Test drawable callbacks interception
        Drawable bgDrawable = new ColorDrawable(Color.RED);
        Drawable fgDrawable = new ColorDrawable(Color.BLUE);
        mService.complicationView.setBackground(bgDrawable);
        mService.complicationView.setForeground(fgDrawable);

        renderer.render(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);

        assertNotNull(bgDrawable.getCallback());
        assertNotNull(fgDrawable.getCallback());

        // Test Drawable.Callback methods
        bgDrawable.invalidateSelf();
        bgDrawable.scheduleSelf(mock(Runnable.class), 100);
        bgDrawable.unscheduleSelf(mock(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWatchfaceRendererDrawModes() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        WatchFace watchFace = mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);

        Renderer.CanvasRenderer2<Renderer.SharedAssets> renderer = (Renderer.CanvasRenderer2<Renderer.SharedAssets>) watchFace.getRenderer();
        Renderer.SharedAssets sharedAssets = mock(Renderer.SharedAssets.class);
        Canvas canvas = mock(Canvas.class);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        RenderParameters renderParams = mock(RenderParameters.class);

        // Test INTERACTIVE
        when(renderParams.getDrawMode()).thenReturn(DrawMode.INTERACTIVE);
        setRenderParameters(renderer, renderParams);
        renderer.render(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);
        assertFalse(mService.clockView.isAmbientModeEnabled());

        // Test LOW_BATTERY_INTERACTIVE
        when(renderParams.getDrawMode()).thenReturn(DrawMode.LOW_BATTERY_INTERACTIVE);
        setRenderParameters(renderer, renderParams);
        renderer.render(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);
        assertFalse(mService.clockView.isAmbientModeEnabled());

        // Test AMBIENT
        when(renderParams.getDrawMode()).thenReturn(DrawMode.AMBIENT);
        setRenderParameters(renderer, renderParams);
        renderer.render(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);
        assertTrue(mService.clockView.isAmbientModeEnabled());

        // Test MUTE
        when(renderParams.getDrawMode()).thenReturn(DrawMode.MUTE);
        setRenderParameters(renderer, renderParams);
        renderer.render(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);
        assertTrue(mService.clockView.isAmbientModeEnabled());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWatchfaceRendererHighlightLayer() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);
        WatchFace watchFace = mService.createWatchFace(mSurfaceHolder, mWatchState, mComplicationSlotsManager, mCurrentUserStyleRepository, mContinuation);

        Renderer.CanvasRenderer2<Renderer.SharedAssets> renderer = (Renderer.CanvasRenderer2<Renderer.SharedAssets>) watchFace.getRenderer();
        Renderer.SharedAssets sharedAssets = mock(Renderer.SharedAssets.class);
        Canvas canvas = mock(Canvas.class);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        RenderParameters renderParams = mock(RenderParameters.class);

        // Test null highlight layer
        when(renderParams.getHighlightLayer()).thenReturn(null);
        setRenderParameters(renderer, renderParams);
        renderer.renderHighlightLayer(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);

        // Test non-null highlight layer
        RenderParameters.HighlightLayer highlightLayer = mock(RenderParameters.HighlightLayer.class);
        when(highlightLayer.getBackgroundTint()).thenReturn(Color.GREEN);
        when(renderParams.getHighlightLayer()).thenReturn(highlightLayer);
        setRenderParameters(renderer, renderParams);

        renderer.renderHighlightLayer(canvas, new Rect(0, 0, 400, 400), zonedDateTime, sharedAssets);
        verify(canvas).drawColor(Color.GREEN);
    }

    @Test
    public void testWatchfaceRendererNullComplicationSlot() {
        mService.createComplicationSlotsManager(mCurrentUserStyleRepository);

        // Pass a ComplicationSlotsManager that returns an empty map
        ComplicationSlotsManager emptySlotsManager = mock(ComplicationSlotsManager.class);
        when(emptySlotsManager.getComplicationSlots()).thenReturn(new HashMap<>());

        mService.createWatchFace(mSurfaceHolder, mWatchState, emptySlotsManager, mCurrentUserStyleRepository, mContinuation);

        assertTrue(mService.complicationView.getComplicationData() instanceof NoDataComplicationData);
    }
}
