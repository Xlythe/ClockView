package com.xlythe.view.clock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.widget.AppCompatButton;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationExperimental;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.EmptyComplicationData;
import androidx.wear.watchface.complications.data.ListComplicationData;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.NoPermissionComplicationData;
import androidx.wear.watchface.complications.data.NotConfiguredComplicationData;
import androidx.wear.watchface.complications.data.PhotoImageComplicationData;
import androidx.wear.watchface.complications.data.ProtoLayoutComplicationData;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.SmallImage;
import androidx.wear.watchface.complications.data.SmallImageComplicationData;

import com.xlythe.watchface.clock.PermissionActivity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OptIn(markerClass = ComplicationExperimental.class)
public class ComplicationView extends AppCompatButton {
  private static final String ACTION_WATCH_FACE_CONTROL = "com.google.android.wearable.action.WATCH_FACE_CONTROL";
  private static final String ACTION_CHOOSE_PROVIDER = "com.google.android.clockwork.home.complications.ACTION_CHOOSE_PROVIDER";
  private static final String EXTRA_COMPLICATION_ID = "android.support.wearable.complications.EXTRA_COMPLICATION_ID";
  private static final String EXTRA_SUPPORTED_TYPES =  "android.support.wearable.complications.EXTRA_SUPPORTED_TYPES";
  private static final String EXTRA_WATCH_FACE_COMPONENT_NAME = "android.support.wearable.complications.EXTRA_WATCH_FACE_COMPONENT_NAME";
  private static final String EXTRA_PENDING_INTENT = "android.support.wearable.complications.EXTRA_PENDING_INTENT";
  private static final String EXTRA_WATCHFACE_INSTANCE_ID = "androidx.wear.watchface.complications.EXTRA_WATCHFACE_INSTANCE_ID";

  private int mComplicationId;
  private ComplicationData mComplicationData;
  private boolean mAmbientModeEnabled = false;

  private ComponentName mWatchFaceComponentName;
  private String mWatchFaceInstanceId;

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
    if (attrs != null) {
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ComplicationView);
      setComplicationId(a.getInteger(R.styleable.ComplicationView_complicationId, 0));
      a.recycle();
    }
    enableWatchfaceControlService();
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

  public List<ComplicationType> getSupportedComplicationTypes() {
    List<ComplicationType> complicationTypes = new ArrayList<>();
    Collections.addAll(complicationTypes, ComplicationType.values());
    return complicationTypes;
  }

  public void setComplicationData(ComplicationData complicationData) {
    mComplicationData = complicationData;

    if (complicationData.getTapAction() != null) {
      setOnClickListener(v -> {
        try {
          complicationData.getTapAction().send();
        } catch (PendingIntent.CanceledException e) {
          Log.e(ClockView.TAG, "Failed to trigger complication OnClick event", e);
        }
      });
    } else {
      setOnClickListener(null);
    }

    switch (complicationData.getType()) {
      case NO_DATA:
        setComplicationData((NoDataComplicationData) complicationData);
        break;
      case EMPTY:
        setComplicationData((EmptyComplicationData) complicationData);
        break;
      case NOT_CONFIGURED:
        setComplicationData((NotConfiguredComplicationData) complicationData);
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
        setComplicationData((ProtoLayoutComplicationData) complicationData);
        break;
      case LIST:
        setComplicationData((ListComplicationData) complicationData);
        break;
      default:
        setComplicationData(
                null,
                null,
                null,
                null);
        break;
    }
  }

  private void setComplicationData(NoDataComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            asCharSequence(complicationData.getContentDescription()),
            null);
    setOnClickListener(v -> launchComplicationChooserActivity());
  }

  private void setComplicationData(EmptyComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            null,
            null);
    setOnClickListener(v -> launchComplicationChooserActivity());
  }

  private void setComplicationData(NotConfiguredComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            null,
            null);
    setOnClickListener(v -> launchComplicationChooserActivity());
  }

  private void setComplicationData(ShortTextComplicationData complicationData) {
    setComplicationData(
            asCharSequence(complicationData.getTitle()),
            asCharSequence(complicationData.getText()),
            asCharSequence(complicationData.getContentDescription()),
            asDrawable(complicationData.getMonochromaticImage()));
  }

  private void setComplicationData(LongTextComplicationData complicationData) {
    setComplicationData(
            asCharSequence(complicationData.getTitle()),
            asCharSequence(complicationData.getText()),
            asCharSequence(complicationData.getContentDescription()),
            asDrawable(complicationData.getMonochromaticImage()));
  }

  private void setComplicationData(RangedValueComplicationData complicationData) {
    setComplicationData(
            asCharSequence(complicationData.getTitle()),
            asCharSequence(complicationData.getText()),
            asCharSequence(complicationData.getContentDescription()),
            asDrawable(complicationData.getMonochromaticImage()));
  }

  private void setComplicationData(MonochromaticImageComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            asCharSequence(complicationData.getContentDescription()),
            asDrawable(complicationData.getMonochromaticImage()));
  }

  private void setComplicationData(SmallImageComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            asCharSequence(complicationData.getContentDescription()),
            asDrawable(complicationData.getSmallImage()));
  }

  private void setComplicationData(PhotoImageComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            asCharSequence(complicationData.getContentDescription()),
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? complicationData.getPhotoImage().loadDrawable(getContext()) : null);
  }

  private void setComplicationData(NoPermissionComplicationData complicationData) {
    setComplicationData(
            asCharSequence(complicationData.getTitle()),
            asCharSequence(complicationData.getText()),
            null,
            asDrawable(complicationData.getMonochromaticImage()));
    setOnClickListener(v -> launchPermissionRequestActivity());
  }

  private void setComplicationData(ProtoLayoutComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            asCharSequence(complicationData.getContentDescription()),
            null);
  }

  private void setComplicationData(ListComplicationData complicationData) {
    setComplicationData(
            null,
            null,
            asCharSequence(complicationData.getContentDescription()),
            null);
  }

  private void setComplicationData(
          @Nullable CharSequence title,
          @Nullable CharSequence text,
          @Nullable CharSequence contentDescription,
          @Nullable Drawable icon) {
    Log.d(ClockView.TAG, "setComplicationData[" + mComplicationData.getType().name() + "] title=" + title + ", text=" + text + ", contentDescription=" + contentDescription + ", icon=" + icon);
    setText(text);
    setContentDescription(contentDescription);
    setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);

    // TODO: Consider using ComplicationDrawable
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

    if (text == ComplicationText.PLACEHOLDER || text == ComplicationText.EMPTY) {
      return null;
    }

    return text.getTextAt(getResources(), Instant.now());
  }

  @Nullable
  private Drawable asDrawable(@Nullable MonochromaticImage image) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return null;
    }

    if (image == null) {
      return null;
    }

    if (image == MonochromaticImage.PLACEHOLDER) {
      return null;
    }

    if (isAmbientModeEnabled()) {
      if (image.getAmbientImage() != null) {
        return image.getAmbientImage().loadDrawable(getContext());
      }
    }

    return image.getImage().loadDrawable(getContext());
  }

  @Nullable
  private Drawable asDrawable(@Nullable SmallImage image) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return null;
    }

    if (image == null) {
      return null;
    }

    if (image == SmallImage.PLACEHOLDER) {
      return null;
    }

    if (isAmbientModeEnabled()) {
      if (image.getAmbientImage() != null) {
        return image.getAmbientImage().loadDrawable(getContext());
      }
    }

    return image.getImage().loadDrawable(getContext());
  }

  public void setWatchFaceComponentName(ComponentName watchFaceComponentName) {
    mWatchFaceComponentName = watchFaceComponentName;
  }

  public void setWatchFaceInstanceId(String watchFaceInstanceId) {
    mWatchFaceInstanceId = watchFaceInstanceId;
  }

  private void enableWatchfaceControlService() {
    PackageManager packageManager = getContext().getPackageManager();

    Intent intent = new Intent(ACTION_WATCH_FACE_CONTROL);
    intent.setPackage(getContext().getPackageName());
    if (packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA).size() != 0) {
      Log.d(ClockView.TAG, "Ignoring call to enable WatchFaceControl. WatchFaceControl is already active.");
      return;
    }

    packageManager.setComponentEnabledSetting(new ComponentName(getContext().getPackageName(), "androidx.wear.watchface.control.WatchFaceControlService"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    Log.d(ClockView.TAG, "Enabled WatchFaceControl. ComplicationChooserActivity can no longer be launched.");
  }

  private void disableWatchfaceControlService() {
    PackageManager packageManager = getContext().getPackageManager();
    packageManager.setComponentEnabledSetting(new ComponentName(getContext().getPackageName(), "androidx.wear.watchface.control.WatchFaceControlService"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

    Intent intent = new Intent(ACTION_WATCH_FACE_CONTROL);
    intent.setPackage(getContext().getPackageName());
    if (packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA).size() != 0) {
      Log.w(ClockView.TAG, "WatchFaceControl is active. ComplicationChooserActivity cannot be launched.");
    } else {
      Log.d(ClockView.TAG, "Disabled WatchFaceControl. ComplicationChooserActivity can now be launched.");
    }
  }

  public void launchPermissionRequestActivity() {
    Intent intent = new Intent(getContext(), PermissionActivity.class);
    if (!(getContext() instanceof Activity)) {
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    getContext().startActivity(intent);
  }

  public void launchComplicationChooserActivity() {
    Intent intent = new Intent(ACTION_CHOOSE_PROVIDER);
    intent.putExtra(EXTRA_WATCH_FACE_COMPONENT_NAME, mWatchFaceComponentName);
    intent.putExtra(EXTRA_COMPLICATION_ID, getComplicationId());

    int[] wireSupportedTypes = new int[getSupportedComplicationTypes().size()];
    int i = 0;
    for (ComplicationType supportedType : getSupportedComplicationTypes()) {
      wireSupportedTypes[i++] = supportedType.toWireComplicationType();
    }
    intent.putExtra(EXTRA_SUPPORTED_TYPES, wireSupportedTypes);
    intent.putExtra(EXTRA_WATCHFACE_INSTANCE_ID, mWatchFaceInstanceId);

    if (getContext() instanceof Activity) {
      Activity activity = (Activity) getContext();
      activity.startActivityForResult(intent, 1000);
      return;
    }

    // Add a placeholder PendingIntent to allow the UID to be checked.
    intent.putExtra(
            EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(getContext(), 0, new Intent(""), Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    // When launching from a service, you must disable the control service first. Otherwise, the activity will immediately close.
    // However, we cannot leave this permanently disabled or the watchface will break.
    /*disableWatchfaceControlService();
    new Handler().postDelayed(this::enableWatchfaceControlService, 5000);*/

    getContext().startActivity(intent);
  }
}
