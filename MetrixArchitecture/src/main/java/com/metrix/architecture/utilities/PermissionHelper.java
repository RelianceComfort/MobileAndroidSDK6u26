package com.metrix.architecture.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MobileApplication;

import androidx.core.app.ActivityCompat;

public class PermissionHelper {

    public static final int CORE_PERMISSION_REQUEST = 123;
    public static final int MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 321;

    public static boolean checkPublicFilePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 30) {
            return true;
        } else {
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void requestPublicFilePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 30) {
            DialogInterface.OnClickListener listener = (dialog, which) -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
                } catch (ActivityNotFoundException ex) {
                    // This is extremely rare
                    Toast.makeText(activity.getApplicationContext(), AndroidResourceHelper
                            .getMessage("EnablePermManuallyAndRetry"), Toast.LENGTH_LONG).show();
                    activity.finish();
                }
            };

            DialogInterface.OnClickListener listener2 = (dialog, which) -> activity.finish();

            MetrixDialogAssistant.showAlertDialog(
                    AndroidResourceHelper.getMessage("PermissionRequired"),
                    AndroidResourceHelper.getMessage("StorageAndPhonePermExpl"),
                    AndroidResourceHelper.getMessage("Yes"),
                    listener,
                    AndroidResourceHelper.getMessage("No"),
                    listener2,
                    activity
            );
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
        }
    }

    public static boolean checkPhoneStatePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(MobileApplication.getAppContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(MobileApplication.getAppContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
