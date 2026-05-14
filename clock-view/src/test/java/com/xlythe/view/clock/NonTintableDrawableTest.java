package com.xlythe.view.clock;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class NonTintableDrawableTest {

    @Test
    public void testConstructorAndSetTintList() {
        Drawable baseDrawable = new ColorDrawable(Color.RED);
        NonTintableDrawable nonTintableDrawable = new NonTintableDrawable(baseDrawable);
        assertNotNull(nonTintableDrawable);

        // Verify setTintList does nothing and does not throw
        nonTintableDrawable.setTintList(ColorStateList.valueOf(Color.BLUE));
    }
}
