package com.xlythe.view.clock;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ViewParent;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.watchface.complications.data.ColorRamp;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicies;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.EmptyComplicationData;
import androidx.wear.watchface.complications.data.GoalProgressComplicationData;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.NoPermissionComplicationData;
import androidx.wear.watchface.complications.data.NotConfiguredComplicationData;
import androidx.wear.watchface.complications.data.PhotoImageComplicationData;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.SmallImage;
import androidx.wear.watchface.complications.data.SmallImageComplicationData;
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData;

import com.xlythe.watchface.clock.PermissionActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ComplicationViewTest {

    private Context mContext;
    private ComplicationView mComplicationView;
    private Icon mSampleIcon;
    private Icon mSampleAmbientIcon;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mComplicationView = new ComplicationView(mContext);
        Bitmap bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        mSampleIcon = Icon.createWithBitmap(bitmap);
        mSampleAmbientIcon = Icon.createWithBitmap(bitmap);
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

    private MonochromaticImage createMonochromaticImage(Icon icon, Icon ambientIcon, boolean isPlaceholder) {
        if (icon == null && ambientIcon == null) return null;
        MonochromaticImage monoImage = mock(MonochromaticImage.class);
        when(monoImage.getImage()).thenReturn(icon);
        when(monoImage.getAmbientImage()).thenReturn(ambientIcon);
        when(monoImage.isPlaceholder()).thenReturn(isPlaceholder);
        return monoImage;
    }

    private SmallImage createSmallImage(Icon icon, Icon ambientIcon, boolean isPlaceholder) {
        if (icon == null && ambientIcon == null) return null;
        SmallImage smallImage = mock(SmallImage.class);
        when(smallImage.getImage()).thenReturn(icon);
        when(smallImage.getAmbientImage()).thenReturn(ambientIcon);
        when(smallImage.isPlaceholder()).thenReturn(isPlaceholder);
        return smallImage;
    }

    private ColorRamp createColorRamp(int[] colors, boolean interpolated) {
        ColorRamp colorRamp = mock(ColorRamp.class);
        when(colorRamp.getColors()).thenReturn(colors);
        when(colorRamp.isInterpolated()).thenReturn(interpolated);
        return colorRamp;
    }

    private NoDataComplicationData createNoData(String contentDesc) {
        ComplicationData source = new ShortTextComplicationData.Builder(createComplicationText("Text"), createComplicationText(contentDesc))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
        return new NoDataComplicationData(source);
    }

    private ShortTextComplicationData createShortTextData(String title, String text, String contentDesc, MonochromaticImage monoImage) {
        return new ShortTextComplicationData.Builder(createComplicationText(text), createComplicationText(contentDesc))
                .setTitle(createComplicationText(title))
                .setMonochromaticImage(monoImage)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private LongTextComplicationData createLongTextData(String title, String text, String contentDesc, MonochromaticImage monoImage, SmallImage smallImage) {
        return new LongTextComplicationData.Builder(createComplicationText(text), createComplicationText(contentDesc))
                .setTitle(createComplicationText(title))
                .setMonochromaticImage(monoImage)
                .setSmallImage(smallImage)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private RangedValueComplicationData createRangedValueData(String title, String text, String contentDesc, MonochromaticImage monoImage, float min, float max, float value, ColorRamp colorRamp) {
        return new RangedValueComplicationData.Builder(value, min, max, createComplicationText(contentDesc))
                .setTitle(createComplicationText(title))
                .setText(createComplicationText(text))
                .setMonochromaticImage(monoImage)
                .setColorRamp(colorRamp)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private GoalProgressComplicationData createGoalProgressData(String title, String text, String contentDesc, MonochromaticImage monoImage, SmallImage smallImage, float targetValue, float value, ColorRamp colorRamp) {
        return new GoalProgressComplicationData.Builder(value, targetValue, createComplicationText(contentDesc))
                .setTitle(createComplicationText(title))
                .setText(createComplicationText(text))
                .setMonochromaticImage(monoImage)
                .setSmallImage(smallImage)
                .setColorRamp(colorRamp)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private WeightedElementsComplicationData createWeightedElementsData(String title, String text, String contentDesc, MonochromaticImage monoImage, SmallImage smallImage, int bgColor, List<WeightedElementsComplicationData.Element> elements) {
        return new WeightedElementsComplicationData.Builder(elements, createComplicationText(contentDesc))
                .setTitle(createComplicationText(title))
                .setText(createComplicationText(text))
                .setMonochromaticImage(monoImage)
                .setSmallImage(smallImage)
                .setElementBackgroundColor(bgColor)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private WeightedElementsComplicationData.Element createElement(float weight, int color) {
        return new WeightedElementsComplicationData.Element(weight, color);
    }

    private MonochromaticImageComplicationData createMonochromaticImageData(String contentDesc, MonochromaticImage monoImage) {
        return new MonochromaticImageComplicationData.Builder(monoImage, createComplicationText(contentDesc))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private SmallImageComplicationData createSmallImageData(String contentDesc, SmallImage smallImage) {
        return new SmallImageComplicationData.Builder(smallImage, createComplicationText(contentDesc))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private PhotoImageComplicationData createPhotoImageData(String contentDesc, Icon photoImage) {
        return new PhotoImageComplicationData.Builder(photoImage, createComplicationText(contentDesc))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private NoPermissionComplicationData createNoPermissionData(String title, String text, MonochromaticImage monoImage) {
        return new NoPermissionComplicationData.Builder()
                .setTitle(createComplicationText(title))
                .setText(createComplicationText(text))
                .setMonochromaticImage(monoImage)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();
    }

    private ComplicationData createEmptyData() {
        return new EmptyComplicationData();
    }

    private ComplicationData createNotConfiguredData() {
        return new NotConfiguredComplicationData();
    }

    @Test
    public void testConstructors() {
        ComplicationView view1 = new ComplicationView(mContext);
        assertNotNull(view1);

        ComplicationView view2 = new ComplicationView(mContext, null);
        assertNotNull(view2);

        ComplicationView view3 = new ComplicationView(mContext, null, 0);
        assertNotNull(view3);

        ComplicationView view4 = new ComplicationView(mContext, null, 0, 0);
        assertNotNull(view4);
    }

    @Test
    public void testConstructorsWithAttributeSet() {
        AttributeSet attrs = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.complicationId, "42")
                .addAttribute(R.attr.complicationDrawableStyle, "1") // LINE
                .addAttribute(R.attr.complicationStyle, "1") // BACKGROUND
                .build();

        ComplicationView view = new ComplicationView(mContext, attrs);
        assertEquals(42, view.getComplicationId());
        assertEquals(ComplicationDrawable.Style.LINE, view.getComplicationDrawableStyle());
        assertEquals(ComplicationView.Style.BACKGROUND, view.getComplicationStyle());
    }

    @Test
    public void testStateGettersAndSetters() {
        mComplicationView.setComplicationId(100);
        assertEquals(100, mComplicationView.getComplicationId());

        mComplicationView.setComplicationDrawableStyle(ComplicationDrawable.Style.LINE);
        assertEquals(ComplicationDrawable.Style.LINE, mComplicationView.getComplicationDrawableStyle());

        mComplicationView.setComplicationStyle(ComplicationView.Style.BACKGROUND);
        assertEquals(ComplicationView.Style.BACKGROUND, mComplicationView.getComplicationStyle());

        assertFalse(mComplicationView.isLowBitAmbient());
        mComplicationView.setLowBitAmbient(true);
        assertTrue(mComplicationView.isLowBitAmbient());
        mComplicationView.setLowBitAmbient(true); // Test redundant call

        assertFalse(mComplicationView.hasBurnInProtection());
        mComplicationView.setHasBurnInProtection(true);
        assertTrue(mComplicationView.hasBurnInProtection());
        mComplicationView.setHasBurnInProtection(true); // Test redundant call

        assertFalse(mComplicationView.isAmbientModeEnabled());
        mComplicationView.setAmbientModeEnabled(true);
        assertTrue(mComplicationView.isAmbientModeEnabled());
        mComplicationView.setAmbientModeEnabled(true); // Test redundant call
    }

    @Test
    public void testSaveAndRestoreInstanceState() {
        mComplicationView.setComplicationId(999);
        mComplicationView.setComplicationDrawableStyle(ComplicationDrawable.Style.LINE);
        mComplicationView.setComplicationStyle(ComplicationView.Style.BACKGROUND);
        mComplicationView.setAmbientModeEnabled(true);
        mComplicationView.setLowBitAmbient(true);
        mComplicationView.setHasBurnInProtection(true);

        Parcelable state = mComplicationView.onSaveInstanceState();
        assertNotNull(state);

        ComplicationView restoredView = new ComplicationView(mContext);
        restoredView.onRestoreInstanceState(state);

        assertEquals(999, restoredView.getComplicationId());
        assertEquals(ComplicationDrawable.Style.LINE, restoredView.getComplicationDrawableStyle());
        assertEquals(ComplicationView.Style.BACKGROUND, restoredView.getComplicationStyle());
        assertTrue(restoredView.isAmbientModeEnabled());
        assertTrue(restoredView.isLowBitAmbient());
        assertTrue(restoredView.hasBurnInProtection());
    }

    @Test
    public void testGetSupportedComplicationTypes() {
        mComplicationView.setComplicationStyle(ComplicationView.Style.CHIP);
        List<ComplicationType> chipTypes = mComplicationView.getSupportedComplicationTypes();
        assertEquals(11, chipTypes.size());
        assertTrue(chipTypes.contains(ComplicationType.SHORT_TEXT));
        assertTrue(chipTypes.contains(ComplicationType.LONG_TEXT));
        assertTrue(chipTypes.contains(ComplicationType.RANGED_VALUE));

        mComplicationView.setComplicationStyle(ComplicationView.Style.BACKGROUND);
        List<ComplicationType> bgTypes = mComplicationView.getSupportedComplicationTypes();
        assertEquals(5, bgTypes.size());
        assertTrue(bgTypes.contains(ComplicationType.PHOTO_IMAGE));
        assertFalse(bgTypes.contains(ComplicationType.SHORT_TEXT));
    }

    @Test
    public void testPerformClickWithOnClickListener() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        mComplicationView.setOnClickListener(v -> clicked.set(true));
        assertTrue(mComplicationView.performClick());
        assertTrue(clicked.get());
    }

    @Test
    public void testPerformClickNoPermissionComplicationData() {
        NoPermissionComplicationData data = createNoPermissionData("Title", "Text", null);
        mComplicationView.setComplicationData(data);
        assertTrue(mComplicationView.isClickable());
        assertTrue(mComplicationView.performClick());

        Intent nextIntent = Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext()).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(PermissionActivity.class.getName(), nextIntent.getComponent().getClassName());
    }

    @Test
    public void testPerformClickWithTapAction() throws PendingIntent.CanceledException {
        PendingIntent mockPendingIntent = mock(PendingIntent.class);
        ShortTextComplicationData data = new ShortTextComplicationData.Builder(createComplicationText("Text"), createComplicationText("Desc"))
                .setTitle(createComplicationText("Title"))
                .setTapAction(mockPendingIntent)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data);
        assertTrue(mComplicationView.isClickable());
        assertTrue(mComplicationView.performClick());
        verify(mockPendingIntent).send();
    }

    @Test
    public void testPerformClickTapActionThrowsCanceledException() throws PendingIntent.CanceledException {
        PendingIntent mockPendingIntent = mock(PendingIntent.class);
        doThrow(new PendingIntent.CanceledException()).when(mockPendingIntent).send();

        ShortTextComplicationData data = new ShortTextComplicationData.Builder(createComplicationText("Text"), createComplicationText("Desc"))
                .setTitle(createComplicationText("Title"))
                .setTapAction(mockPendingIntent)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data);
        assertFalse(mComplicationView.performClick());
        verify(mockPendingIntent).send();
    }

    @Test
    public void testSetTimeAndGetTimeMillis() {
        ZonedDateTime dateTime = ZonedDateTime.parse("2026-05-14T10:15:30+00:00[UTC]");
        mComplicationView.setTime(dateTime);
        assertEquals(dateTime.toInstant().toEpochMilli(), mComplicationView.getTimeMillis());

        mComplicationView.setTime(1500000000000L);
        assertEquals(1500000000000L, mComplicationView.getTimeMillis());

        mComplicationView.resetTime();
        assertTrue(mComplicationView.getTimeMillis() <= System.currentTimeMillis());
    }

    @Test
    public void testSetForegroundDisablesDynamicForeground() {
        Drawable customForeground = new ColorDrawable(Color.RED);
        mComplicationView.setForeground(customForeground);
        assertEquals(customForeground, mComplicationView.getForeground());

        ShortTextComplicationData data = createShortTextData("Title", "Text", "Desc", null);
        mComplicationView.setComplicationData(data);
        assertEquals(customForeground, mComplicationView.getForeground());
    }

    @Test
    public void testInvalidatePropagatesToParent() {
        ViewParent mockParent = mock(ViewParent.class);
        ComplicationView spyView = spy(mComplicationView);
        when(spyView.getParent()).thenReturn(mockParent);

        spyView.invalidate();
        verify(mockParent).onDescendantInvalidated(spyView, spyView);
    }

    @Test
    public void testSetComplicationDataNoData() {
        NoDataComplicationData data = createNoData("No Data Desc");
        mComplicationView.setComplicationData(data);
        assertEquals("No Data Desc", mComplicationView.getContentDescription());
        assertNull(mComplicationView.getDrawable());
    }

    @Test
    public void testSetComplicationDataShortText() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        ShortTextComplicationData data = createShortTextData("Short Title", "Short Text", "Short Desc", monoImage);
        mComplicationView.setComplicationData(data);
        assertEquals("Short Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof ComplicationDrawable);
    }

    @Test
    public void testSetComplicationDataLongTextWithSmallImage() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        SmallImage smallImage = createSmallImage(mSampleIcon, mSampleAmbientIcon, false);
        LongTextComplicationData data = createLongTextData("Long Title", "Long Text", "Long Desc", monoImage, smallImage);

        mComplicationView.setComplicationData(data);
        assertEquals("Long Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof NonTintableDrawable);
    }

    @Test
    public void testSetComplicationDataLongTextWithoutSmallImage() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        LongTextComplicationData data = createLongTextData("Long Title", "Long Text", "Long Desc", monoImage, null);

        mComplicationView.setComplicationData(data);
        assertEquals("Long Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof ComplicationDrawable);
    }

    @Test
    public void testSetComplicationDataLongTextAmbientMode() {
        mComplicationView.setAmbientModeEnabled(true);
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        SmallImage smallImage = createSmallImage(mSampleIcon, mSampleAmbientIcon, false);
        LongTextComplicationData data = createLongTextData("Long Title", "Long Text", "Long Desc", monoImage, smallImage);

        mComplicationView.setComplicationData(data);
        assertEquals("Long Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof ComplicationDrawable);
    }

    @Test
    public void testSetComplicationDataRangedValue() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        ColorRamp colorRamp = createColorRamp(new int[]{Color.RED, Color.BLUE}, true);
        RangedValueComplicationData data = createRangedValueData("Range Title", "Range Text", "Range Desc", monoImage, 0f, 100f, 50f, colorRamp);

        mComplicationView.setComplicationData(data);
        assertEquals("Range Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof RangeDrawable);
    }

    @Test
    public void testSetComplicationDataRangedValueAmbientMode() {
        mComplicationView.setAmbientModeEnabled(true);
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        ColorRamp colorRamp = createColorRamp(new int[]{Color.RED, Color.BLUE}, true);
        RangedValueComplicationData data = createRangedValueData("Range Title", "Range Text", "Range Desc", monoImage, 0f, 100f, 50f, colorRamp);

        mComplicationView.setComplicationData(data);
        assertEquals("Range Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof RangeDrawable);
    }

    @Test
    public void testSetComplicationDataGoalProgress() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        SmallImage smallImage = createSmallImage(mSampleIcon, mSampleAmbientIcon, false);
        ColorRamp colorRamp = createColorRamp(new int[]{Color.GREEN, Color.YELLOW}, false);
        GoalProgressComplicationData data = createGoalProgressData("Goal Title", "Goal Text", "Goal Desc", monoImage, smallImage, 1000f, 500f, colorRamp);

        mComplicationView.setComplicationData(data);
        assertEquals("Goal Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof RangeDrawable);
    }

    @Test
    public void testSetComplicationDataGoalProgressAmbientMode() {
        mComplicationView.setAmbientModeEnabled(true);
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        SmallImage smallImage = createSmallImage(mSampleIcon, mSampleAmbientIcon, false);
        ColorRamp colorRamp = createColorRamp(new int[]{Color.GREEN, Color.YELLOW}, false);
        GoalProgressComplicationData data = createGoalProgressData("Goal Title", "Goal Text", "Goal Desc", monoImage, smallImage, 1000f, 500f, colorRamp);

        mComplicationView.setComplicationData(data);
        assertEquals("Goal Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof RangeDrawable);
    }

    @Test
    public void testSetComplicationDataWeightedElements() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        SmallImage smallImage = createSmallImage(mSampleIcon, mSampleAmbientIcon, false);
        List<WeightedElementsComplicationData.Element> elements = Arrays.asList(
                createElement(10f, Color.RED),
                createElement(20f, Color.BLUE)
        );
        WeightedElementsComplicationData data = createWeightedElementsData("Weight Title", "Weight Text", "Weight Desc", monoImage, smallImage, Color.WHITE, elements);

        mComplicationView.setComplicationData(data);
        assertEquals("Weight Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof ChartDrawable);
    }

    @Test
    public void testSetComplicationDataWeightedElementsAmbientMode() {
        mComplicationView.setAmbientModeEnabled(true);
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        SmallImage smallImage = createSmallImage(mSampleIcon, mSampleAmbientIcon, false);
        List<WeightedElementsComplicationData.Element> elements = Arrays.asList(
                createElement(10f, Color.RED),
                createElement(20f, Color.BLUE)
        );
        WeightedElementsComplicationData data = createWeightedElementsData("Weight Title", "Weight Text", "Weight Desc", monoImage, smallImage, Color.WHITE, elements);

        mComplicationView.setComplicationData(data);
        assertEquals("Weight Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof ChartDrawable);
    }

    @Test
    public void testSetComplicationDataMonochromaticImage() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        MonochromaticImageComplicationData data = createMonochromaticImageData("Mono Desc", monoImage);

        mComplicationView.setComplicationData(data);
        assertEquals("Mono Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
    }

    @Test
    public void testSetComplicationDataSmallImage() {
        SmallImage smallImage = createSmallImage(mSampleIcon, mSampleAmbientIcon, false);
        SmallImageComplicationData data = createSmallImageData("Small Desc", smallImage);

        mComplicationView.setComplicationData(data);
        assertEquals("Small Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof NonTintableDrawable);
    }

    @Test
    public void testSetComplicationDataPhotoImage() {
        PhotoImageComplicationData data = createPhotoImageData("Photo Desc", mSampleIcon);

        mComplicationView.setComplicationData(data);
        assertEquals("Photo Desc", mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof NonTintableDrawable);
    }

    @Test
    public void testSetComplicationDataNoPermission() {
        MonochromaticImage monoImage = createMonochromaticImage(mSampleIcon, mSampleAmbientIcon, false);
        NoPermissionComplicationData data = createNoPermissionData("Perm Title", "Perm Text", monoImage);

        mComplicationView.setComplicationData(data);
        assertNull(mComplicationView.getContentDescription());
        assertNotNull(mComplicationView.getDrawable());
        assertTrue(mComplicationView.getDrawable() instanceof ComplicationDrawable);
    }

    @Test
    public void testSetComplicationDataEmptyAndNotConfigured() {
        mComplicationView.setComplicationData(createEmptyData());
        assertNull(mComplicationView.getContentDescription());
        assertNull(mComplicationView.getDrawable());

        mComplicationView.setComplicationData(createNotConfiguredData());
        assertNull(mComplicationView.getContentDescription());
        assertNull(mComplicationView.getDrawable());
    }

    @Test
    public void testSetComplicationDataDoNotShowWhenDeviceLocked() {
        mComplicationView.setAmbientModeEnabled(true);
        ShortTextComplicationData data = new ShortTextComplicationData.Builder(createComplicationText("Text"), createComplicationText("Locked Desc"))
                .setTitle(createComplicationText("Title"))
                .setDisplayPolicy(ComplicationDisplayPolicies.DO_NOT_SHOW_WHEN_DEVICE_LOCKED)
                .build();

        mComplicationView.setComplicationData(data);
        assertNull(mComplicationView.getContentDescription());
    }

    @Test
    public void testSetComplicationDataPlaceholderAndAlwaysEmptyText() {
        ComplicationText placeholderText = mock(ComplicationText.class);
        when(placeholderText.isPlaceholder()).thenReturn(true);
        when(placeholderText.isAlwaysEmpty()).thenReturn(false);
        when(placeholderText.getNextChangeTime(any())).thenReturn(Instant.MAX);
        when(placeholderText.toWireComplicationText()).thenReturn(mock(android.support.wearable.complications.ComplicationText.class));

        ComplicationText alwaysEmptyText = mock(ComplicationText.class);
        when(alwaysEmptyText.isPlaceholder()).thenReturn(false);
        when(alwaysEmptyText.isAlwaysEmpty()).thenReturn(true);
        when(alwaysEmptyText.getNextChangeTime(any())).thenReturn(Instant.MAX);
        when(alwaysEmptyText.toWireComplicationText()).thenReturn(mock(android.support.wearable.complications.ComplicationText.class));

        ShortTextComplicationData data1 = new ShortTextComplicationData.Builder(createComplicationText("Text"), createComplicationText("Desc"))
                .setTitle(placeholderText)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data1);
        assertNotNull(mComplicationView.getDrawable());

        ShortTextComplicationData data2 = new ShortTextComplicationData.Builder(createComplicationText("Text"), createComplicationText("Desc"))
                .setTitle(alwaysEmptyText)
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data2);
        assertNotNull(mComplicationView.getDrawable());
    }

    @Test
    public void testSetComplicationDataSameInstanceIgnored() {
        ShortTextComplicationData data = createShortTextData("Title", "Text", "Desc", null);
        mComplicationView.setComplicationData(data);
        assertEquals(data, mComplicationView.getComplicationData());

        // Set same instance again
        mComplicationView.setComplicationData(data);
        assertEquals(data, mComplicationView.getComplicationData());
    }

    @Test
    public void testScheduleNextUpdate() {
        ComplicationText compText = mock(ComplicationText.class);
        when(compText.getTextAt(any(), any())).thenReturn("Text");
        when(compText.isPlaceholder()).thenReturn(false);
        when(compText.isAlwaysEmpty()).thenReturn(false);
        when(compText.getNextChangeTime(any())).thenReturn(Instant.now().plusSeconds(5));
        when(compText.toWireComplicationText()).thenReturn(mock(android.support.wearable.complications.ComplicationText.class));

        ShortTextComplicationData data = new ShortTextComplicationData.Builder(compText, createComplicationText("Desc"))
                .setTitle(createComplicationText("Title"))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(data, mComplicationView.getComplicationData());
    }

    @Test
    public void testScheduleNextUpdateAmbientModeAndEdgeCases() {
        mComplicationView.setAmbientModeEnabled(true);
        ComplicationText compText1 = mock(ComplicationText.class);
        when(compText1.getTextAt(any(), any())).thenReturn("Text");
        when(compText1.isPlaceholder()).thenReturn(false);
        when(compText1.isAlwaysEmpty()).thenReturn(false);
        when(compText1.getNextChangeTime(any())).thenReturn(Instant.now().plusMillis(500)); // < 1000ms
        when(compText1.toWireComplicationText()).thenReturn(mock(android.support.wearable.complications.ComplicationText.class));

        ShortTextComplicationData data1 = new ShortTextComplicationData.Builder(compText1, createComplicationText("Desc"))
                .setTitle(createComplicationText("Title"))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data1);

        ComplicationText compText2 = mock(ComplicationText.class);
        when(compText2.getTextAt(any(), any())).thenReturn("Text");
        when(compText2.isPlaceholder()).thenReturn(false);
        when(compText2.isAlwaysEmpty()).thenReturn(false);
        when(compText2.getNextChangeTime(any())).thenReturn(Instant.now().minusSeconds(5)); // < 0
        when(compText2.toWireComplicationText()).thenReturn(mock(android.support.wearable.complications.ComplicationText.class));

        ShortTextComplicationData data2 = new ShortTextComplicationData.Builder(compText2, createComplicationText("Desc"))
                .setTitle(createComplicationText("Title"))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data2);

        ComplicationText compText3 = mock(ComplicationText.class);
        when(compText3.getTextAt(any(), any())).thenReturn("Text");
        when(compText3.isPlaceholder()).thenReturn(false);
        when(compText3.isAlwaysEmpty()).thenReturn(false);
        when(compText3.getNextChangeTime(any())).thenReturn(Instant.MAX);
        when(compText3.toWireComplicationText()).thenReturn(mock(android.support.wearable.complications.ComplicationText.class));

        ShortTextComplicationData data3 = new ShortTextComplicationData.Builder(compText3, createComplicationText("Desc"))
                .setTitle(createComplicationText("Title"))
                .setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY)
                .build();

        mComplicationView.setComplicationData(data3);
    }

    @Test
    public void testWatchfaceEditorContext() {
        Intent intent = new Intent(ClockView.ACTION_WATCH_FACE_EDITOR);
        ComponentActivity activity = Robolectric.buildActivity(ComponentActivity.class, intent).get();

        ComplicationView editorView = new ComplicationView(activity);
        editorView.setComplicationData(createEmptyData());
        assertNotNull(editorView.getDrawable()); // PlaceholderDrawable is set
    }
}
