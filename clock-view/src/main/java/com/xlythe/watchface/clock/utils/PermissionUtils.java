package com.xlythe.watchface.clock.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

/** Permission utilities. */
public class PermissionUtils {

  private PermissionUtils() {}

  /** @return True if the app was granted all the permissions. False otherwise. */
  public static boolean hasPermissions(Context context, String... permissions) {
    for (String permission : permissions) {
      if (ContextCompat.checkSelfPermission(context, permission)
              != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }
}
