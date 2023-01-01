package com.xlythe.view.clock;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class NonTintableDrawable extends StateListDrawable {

    public NonTintableDrawable(Drawable drawable) {
        super();
        addState(new int[0], drawable);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        // Ignored
    }
}
