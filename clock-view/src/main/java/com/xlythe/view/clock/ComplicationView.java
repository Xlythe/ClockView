package com.xlythe.view.clock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewParent;

import androidx.activity.ComponentActivity;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationExperimental;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.NoPermissionComplicationData;
import androidx.wear.watchface.complications.data.PhotoImageComplicationData;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.SmallImage;
import androidx.wear.watchface.complications.data.SmallImageComplicationData;

import com.xlythe.watchface.clock.PermissionActivity;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OptIn(markerClass = ComplicationExperimental.class)
public class ComplicationView extends AppCompatImageView {
  private int mComplicationId;
  private ComplicationData mComplicationData = new NoDataComplicationData();
  private boolean mAmbientModeEnabled = false;
  private ComplicationDrawable.Style mComplicationDrawableStyle = ComplicationDrawable.Style.DOT;
  private PlaceholderDrawable mPlaceholderDrawable;

  @Nullable private OnClickListener mOnClickListener;

  private final Handler mHandler = new Handler(Looper.getMainLooper());

  @Nullable
  private ZonedDateTime mDateTime;
  private long mTimeMillis = -1;

  public ComplicationView(@NonNull Context context) {
    super(context);
    init(context, /*attrs=*/ null);
  }

  public ComplicationView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public ComplicationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  public ComplicationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, @Nullable AttributeSet attrs) {
    boolean hasForeground = false;
    if (attrs != null) {
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ComplicationView);
      setComplicationId(a.getInteger(R.styleable.ComplicationView_complicationId, 0));
      setComplicationDrawableStyle(ComplicationDrawable.Style.values()[a.getInteger(R.styleable.ComplicationView_complicationStyle, mComplicationDrawableStyle.ordinal())]);
      a.recycle();

      a = context.obtainStyledAttributes(attrs, new int[] { android.R.attr.foreground });
      hasForeground = a.hasValue(0);
      a.recycle();
    }

    mPlaceholderDrawable = new PlaceholderDrawable(getContext());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasForeground) {
      setForeground(new RippleDrawable(
              getResources().getColorStateList(R.color.complication_pressed, getContext().getTheme()),
              null,
              mPlaceholderDrawable));
    }
  }

  public boolean isAmbientModeEnabled() {
    return mAmbientModeEnabled;
  }

  public void setAmbientModeEnabled(boolean enabled) {
    if (enabled == mAmbientModeEnabled) {
      return;
    }

    mAmbientModeEnabled = enabled;

    // Some components change based off ambient mode. This will invalidate them.
    setComplicationData(mComplicationData);
  }

  public int getComplicationId() {
    return mComplicationId;
  }

  public void setComplicationId(int complicationId) {
    mComplicationId = complicationId;
  }

  public ComplicationDrawable.Style getComplicationDrawableStyle() {
    return mComplicationDrawableStyle;
  }

  public void setComplicationDrawableStyle(ComplicationDrawable.Style style) {
    mComplicationDrawableStyle = style;
    setComplicationData(mComplicationData);
  }

  public List<ComplicationType> getSupportedComplicationTypes() {
    List<ComplicationType> complicationTypes = new ArrayList<>();
    Collections.addAll(complicationTypes, ComplicationType.values());
    complicationTypes.remove(ComplicationType.LIST);
    complicationTypes.remove(ComplicationType.PROTO_LAYOUT);
    complicationTypes.remove(ComplicationType.PHOTO_IMAGE);
    return complicationTypes;
  }

  @Override
  public void setOnClickListener(@Nullable OnClickListener l) {
    super.setOnClickListener(l);
    mOnClickListener = l;
  }

  @Override
  public boolean performClick() {
    if (mOnClickListener != null) {
      mOnClickListener.onClick(this);
      return true;
    }

    if (mComplicationData instanceof NoPermissionComplicationData) {
      launchPermissionRequestActivity();
      return true;
    }

    if (mComplicationData.getTapAction() != null) {
      try {
        mComplicationData.getTapAction().send();
        return true;
      } catch (PendingIntent.CanceledException e) {
        Log.e(ClockView.TAG, "Failed to trigger complication OnClick event", e);
      }
    }

    return super.performClick();
  }

  @Override
  public boolean isClickable() {
    if (mComplicationData != null && mComplicationData.getTapAction() != null) {
      return true;
    }

    if (mComplicationData instanceof NoPermissionComplicationData) {
      return true;
    }

    return super.isClickable();
  }

  private boolean isInWatchfaceEditor() {
    Context context = getContext();
    if (!(context instanceof ComponentActivity)) {
      return false;
    }

    Intent intent = ((ComponentActivity) context).getIntent();
    return ClockView.ACTION_WATCH_FACE_EDITOR.equals(intent.getAction());
  }

  @CallSuper
  public void setComplicationData(ComplicationData complicationData) {
    mComplicationData = complicationData;

    switch (complicationData.getType()) {
      case NO_DATA:
        setComplicationData((NoDataComplicationData) complicationData);
        break;
      case SHORT_TEXT:
        setComplicationData((ShortTextComplicationData) complicationData);
        break;
      case LONG_TEXT:
        setComplicationData((LongTextComplicationData) complicationData);
        break;
      case RANGED_VALUE:
        setComplicationData((RangedValueComplicationData) complicationData);
        break;
      case MONOCHROMATIC_IMAGE:
        setComplicationData((MonochromaticImageComplicationData) complicationData);
        break;
      case SMALL_IMAGE:
        setComplicationData((SmallImageComplicationData) complicationData);
        break;
      case PHOTO_IMAGE:
        setComplicationData((PhotoImageComplicationData) complicationData);
        break;
      case NO_PERMISSION:
        setComplicationData((NoPermissionComplicationData) complicationData);
        break;
      case PROTO_LAYOUT:
      case LIST:
      case EMPTY:
      case NOT_CONFIGURED:
      default:
        setContentDescription(null);
        setImageDrawable(isInWatchfaceEditor() ? mPlaceholderDrawable : null);
        break;
    }
  }

  private void setComplicationData(NoDataComplicationData complicationData) {
    setContentDescription(asCharSequence(complicationData.getContentDescription()));
    setImageDrawable(isInWatchfaceEditor() ? mPlaceholderDrawable : null);

    scheduleNextUpdate(complicationData.getContentDescription());
  }

  private void setComplicationData(ShortTextComplicationData complicationData) {
    setContentDescription(asCharSequence(complicationData.getContentDescription()));
    setImageDrawable(new ComplicationDrawable.Builder(getContext())
            .title(asCharSequence(complicationData.getTitle()))
            .text(asCharSequence(complicationData.getText()))
            .icon(asDrawable(complicationData.getMonochromaticImage()))
            .style(isAmbientModeEnabled() ? ComplicationDrawable.Style.EMPTY : mComplicationDrawableStyle)
            .build());

    scheduleNextUpdate(complicationData.getContentDescription());
    scheduleNextUpdate(complicationData.getTitle());
    scheduleNextUpdate(complicationData.getText());
  }

  private void setComplicationData(LongTextComplicationData complicationData) {
    setContentDescription(asCharSequence(complicationData.getContentDescription()));
    Drawable smallIcon = asDrawable(complicationData.getSmallImage());
    if (smallIcon != null) {
      setImageDrawable(smallIcon);
    } else {
      setImageDrawable(new ComplicationDrawable.Builder(getContext())
              .title(asCharSequence(complicationData.getTitle()))
              .text(asCharSequence(complicationData.getText()))
              .icon(asDrawable(complicationData.getMonochromaticImage()))
              .style(isAmbientModeEnabled() ? ComplicationDrawable.Style.EMPTY : mComplicationDrawableStyle)
              .build());
    }

    scheduleNextUpdate(complicationData.getContentDescription());
    scheduleNextUpdate(complicationData.getTitle());
    scheduleNextUpdate(complicationData.getText());
  }

  private void setComplicationData(RangedValueComplicationData complicationData) {
    setContentDescription(asCharSequence(complicationData.getContentDescription()));
    setImageDrawable(new RangeDrawable.Builder(getContext())
            .title(asCharSequence(complicationData.getTitle()))
            .text(asCharSequence(complicationData.getText()))
            .icon(asDrawable(complicationData.getMonochromaticImage()))
            .range(complicationData.getMin(), complicationData.getMax())
            .value(complicationData.getValue())
            .showBackground(!isAmbientModeEnabled())
            .build());

    scheduleNextUpdate(complicationData.getContentDescription());
    scheduleNextUpdate(complicationData.getTitle());
    scheduleNextUpdate(complicationData.getText());
  }

  private void setComplicationData(MonochromaticImageComplicationData complicationData) {
    setContentDescription(asCharSequence(complicationData.getContentDescription()));
    setImageDrawable(asDrawable(complicationData.getMonochromaticImage()));

    scheduleNextUpdate(complicationData.getContentDescription());
  }

  private void setComplicationData(SmallImageComplicationData complicationData) {
    setContentDescription(asCharSequence(complicationData.getContentDescription()));
    setImageDrawable(asDrawable(complicationData.getSmallImage()));

    scheduleNextUpdate(complicationData.getContentDescription());
  }

  private void setComplicationData(PhotoImageComplicationData complicationData) {
    setContentDescription(asCharSequence(complicationData.getContentDescription()));
    setImageDrawable(asDrawable(complicationData.getPhotoImage()));

    scheduleNextUpdate(complicationData.getContentDescription());
  }

  private void setComplicationData(NoPermissionComplicationData complicationData) {
    setContentDescription(null);
    setImageDrawable(new ComplicationDrawable.Builder(getContext())
            .title(asCharSequence(complicationData.getTitle()))
            .text(asCharSequence(complicationData.getText()))
            .icon(asDrawable(complicationData.getMonochromaticImage()))
            .style(isAmbientModeEnabled() ? ComplicationDrawable.Style.EMPTY : mComplicationDrawableStyle)
            .build());

    scheduleNextUpdate(complicationData.getTitle());
    scheduleNextUpdate(complicationData.getText());
  }

  public ComplicationData getComplicationData() {
    return mComplicationData;
  }

  @Nullable
  private CharSequence asCharSequence(@Nullable ComplicationText text) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return null;
    }

    if (text == null) {
      return null;
    }

    if (text.isPlaceholder() || text.isAlwaysEmpty()) {
      return null;
    }

    return text.getTextAt(getResources(), getInstant());
  }

  private void scheduleNextUpdate(@Nullable ComplicationText text) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }

    if (text == null) {
      return;
    }

    if (text.isPlaceholder() || text.isAlwaysEmpty()) {
      return;
    }

    long timeUntilNextUpdate = Duration.between(getInstant(), text.getNextChangeTime(getInstant())).getSeconds() * 1000;
    if (timeUntilNextUpdate < 0) {
      return;
    }

    mHandler.postDelayed(() -> setComplicationData(mComplicationData), timeUntilNextUpdate);
  }

  @Nullable
  private Drawable asDrawable(@Nullable MonochromaticImage image) {
    if (image == null) {
      return null;
    }

    if (image.isPlaceholder()) {
      return null;
    }

    if (isAmbientModeEnabled()) {
      if (image.getAmbientImage() != null) {
        Drawable drawable = asDrawable(image.getAmbientImage());
        if (drawable != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          drawable.setTint(Color.WHITE);
        }
      }
    }

    return asDrawable(image.getImage());
  }

  @Nullable
  private Drawable asDrawable(@Nullable SmallImage image) {
    if (image == null) {
      return null;
    }

    if (image.isPlaceholder()) {
      return null;
    }

    if (isAmbientModeEnabled()) {
      if (image.getAmbientImage() != null) {
        return asDrawable(image.getAmbientImage());
      }
    }

    return asDrawable(image.getImage());
  }

  @Nullable
  private Drawable asDrawable(@Nullable Icon icon) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return null;
    }

    if (icon == null) {
      return null;
    }

    return icon.loadDrawable(getContext());
  }

  public void launchPermissionRequestActivity() {
    Intent intent = new Intent(getContext(), PermissionActivity.class);
    if (!(getContext() instanceof Activity)) {
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    getContext().startActivity(intent);
  }

  @RequiresApi(26)
  public void setTime(ZonedDateTime dateTime) {
    mDateTime = dateTime;
    mTimeMillis = -1;
  }

  public void setTime(long timeInMillis) {
    mTimeMillis = timeInMillis;
    mDateTime = null;
  }

  public long getTimeMillis() {
    if (Build.VERSION.SDK_INT >= 26 && mDateTime != null) {
      return mDateTime.toInstant().toEpochMilli();
    } else {
      return mTimeMillis >= 0 ? mTimeMillis : System.currentTimeMillis();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private Instant getInstant() {
    return Instant.ofEpochMilli(getTimeMillis());
  }

  public void resetTime() {
    mTimeMillis = -1;
    mDateTime = null;
  }

  @Override
  public void invalidate() {
    super.invalidate();

    ViewParent parent = getParent();
    if (parent == null) {
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      parent.onDescendantInvalidated(this, this);
    }
  }
}
