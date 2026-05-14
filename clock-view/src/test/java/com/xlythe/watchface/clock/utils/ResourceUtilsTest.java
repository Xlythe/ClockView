package com.xlythe.watchface.clock.utils;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ResourceUtilsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testInDisplayIndependentPixels() {
        float px = ResourceUtils.inDisplayIndependentPixels(mContext, 10);
        assertTrue(px > 0);
    }

    @Test
    public void testInScaleIndependentPixels() {
        float sp = ResourceUtils.inScaleIndependentPixels(mContext, 10);
        assertTrue(sp > 0);
    }
}
