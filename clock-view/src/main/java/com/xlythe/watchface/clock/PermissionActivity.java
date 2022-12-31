package com.xlythe.watchface.clock;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xlythe.watchface.clock.utils.PermissionUtils;

public class PermissionActivity extends Activity {
  private static final String COMPLICATIONS_PERMISSION =
          "com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA";

  private static final String COMPLICATIONS_PERMISSION_PRIVILEGED =
          "com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA_PRIVILEGED";

  private static final int PERMISSION_REQUEST_CODE = 1000;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (hasPermissions() || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      finish();
    } else {
      requestPermissions(new String[] { COMPLICATIONS_PERMISSION }, PERMISSION_REQUEST_CODE);
    }
  }

  private boolean hasPermissions() {
    return PermissionUtils.hasPermissions(this, COMPLICATIONS_PERMISSION)
            || PermissionUtils.hasPermissions(this, COMPLICATIONS_PERMISSION_PRIVILEGED);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    finish();
  }
}
