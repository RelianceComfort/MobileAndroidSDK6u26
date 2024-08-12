
package com.metrix.metrixmobile.system;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.metrix.architecture.assistants.MetrixBarcodeAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.FSMNotificationContent;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.PermissionHelper;
import com.metrix.metrixmobile.R;

import java.util.ArrayList;
import java.util.List;

public class Splash extends AppCompatActivity {

    /** Duration of wait **/
    private static final int SPLASH_DISPLAY_LENGTH = 1000;
    private Activity mContext;
    private boolean shouldShowPermissionDialog = false;
    private boolean shouldGoToSettingsForPermissions = false;
    private boolean phonePermissionGranted = false;
    private boolean storagePermissionGranted = false;
    private boolean notificationPermissionGranted = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (saveNotificationContent() && MobileApplication.SplashComplete)
            finish();

        setContentView(R.layout.splashscreen);
        mContext = this;

        verifyGooglePlayServices();
        seedApplicationIDValues();
        requestCorePermissions();
        MobileApplication.SplashComplete = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!checkCorePermissions() && shouldShowPermissionDialog) {
            shouldShowPermissionDialog = false;

            DialogInterface.OnClickListener listener = (dialog, which) -> {
                if (shouldGoToSettingsForPermissions) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, PermissionHelper.CORE_PERMISSION_REQUEST);
                    } catch (ActivityNotFoundException ex) {
                        // This is extremely rare
                        Toast.makeText(getApplicationContext(), AndroidResourceHelper
                                .getMessage("EnablePermManuallyAndRetry"), Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    requestCorePermissions();
                }
            };

            DialogInterface.OnClickListener listener2 = (dialog, which) -> finish();

            MetrixDialogAssistant.showAlertDialog(
                    AndroidResourceHelper.getMessage("PermissionRequired"),
                    AndroidResourceHelper.getMessage("StorageAndPhonePermExpl"),
                    AndroidResourceHelper.getMessage("Yes"),
                    listener,
                    AndroidResourceHelper.getMessage("No"),
                    listener2,
                    this
            );

        } else if (checkCorePermissions()) {
            launchApp();
        }
    }

    private void launchApp() {
        // Clearing the public folder created just in case the application was force cancelled
        // before deleting the public file.
        cleanUpPublicFiles();
        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
            	Intent login = MetrixActivityHelper.createActivityIntent(mContext, Login.class);
    			login.putExtra("SPLASH", true);
            	MetrixActivityHelper.startNewActivity(mContext, login);
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    private void requestCorePermissions() {
        if (!checkCorePermissions()) {
            List<String> permissionsToRequest = new ArrayList<>();

            if (!phonePermissionGranted) {
                permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE);
            }

            if (!storagePermissionGranted) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= 33) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }

            if (!shouldGoToSettingsForPermissions || (!phonePermissionGranted && !storagePermissionGranted)) {
                String[] permissionsToRequestArray = permissionsToRequest.toArray(new String[0]);
                ActivityCompat.requestPermissions(this, permissionsToRequestArray, PermissionHelper.CORE_PERMISSION_REQUEST);
            }
        }
    }

    private boolean checkCorePermissions() {
        phonePermissionGranted = PermissionHelper.checkPhoneStatePermission();
        storagePermissionGranted = PermissionHelper.checkPublicFilePermission(Splash.this);
        notificationPermissionGranted = PermissionHelper.checkNotificationPermission();

        return (phonePermissionGranted && storagePermissionGranted && notificationPermissionGranted);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PermissionHelper.CORE_PERMISSION_REQUEST) {
            if (checkCorePermissions()) {
                launchApp();
            } else {
                // Showing dialog at this Activity lifecycle can lead to app crash as the view is not guaranteed to
                // be visible to the user. So we set shouldShowPermissionDialog to true and handle it inside
                // onResume activity life cycle
                shouldShowPermissionDialog = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.CORE_PERMISSION_REQUEST) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                launchApp();
            } else if (permissions.length == 1 && permissions[0].equals(Manifest.permission.POST_NOTIFICATIONS)) {
                launchApp(); // Ignore the results of the notification permission request
            } else {
                // Showing dialog at this Activity lifecycle can lead to app crash as the view is not guaranteed to
                // be visible to the user. So we set shouldShowPermissionDialog to true and handle it inside
                // onResume activity life cycle
                shouldShowPermissionDialog = true;

                // Need to check the shouldShowRequestPermissionRationale method here because there
                // is a false negative when launching the application. Before even interacting with
                // the permissions, this check returns as false even though the "Don't ask again"
                // option wouldn't have been brought up to the user.
                if ((!phonePermissionGranted && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE))
                        || (!storagePermissionGranted && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                    shouldGoToSettingsForPermissions = true;
                } else {
                    shouldGoToSettingsForPermissions = false;
                }
            }
        }
    }

    private void verifyGooglePlayServices() {
        // Google Play Services is required for using Firebase push messaging APIs
        try {
            GoogleApiAvailability gaaHelper = GoogleApiAvailability.getInstance();
            int statusCode = gaaHelper.isGooglePlayServicesAvailable(mContext);
            if (statusCode != ConnectionResult.SUCCESS) {
                gaaHelper.makeGooglePlayServicesAvailable(mContext);
            }
            MetrixBarcodeAssistant.makeGoogleCodeScannerAvailable(mContext);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void seedApplicationIDValues() {
        while (MetrixControlAssistant.generateViewId() < 10) {
            // Just burn through the first handful of IDs generated, as they can cause trouble with layout processing
        }
    }

    private boolean saveNotificationContent() {
        FSMNotificationContent nc = null;
        try {
            // Check if navigated here by Notification
            Intent receivedIntent = getIntent();
            int notificationId = receivedIntent.getIntExtra("NOTIFICATION_ID", -1);
            if (notificationId != -1) {
                nc = new FSMNotificationContent();

                String clientScript = receivedIntent.getStringExtra("CLIENT_SCRIPT");
                nc.setClientScript((MetrixStringHelper.isNullOrEmpty(clientScript)) ? "" : clientScript);

                String dataPoint1 = receivedIntent.getStringExtra("DATA_POINT1");
                String dataPoint2 = receivedIntent.getStringExtra("DATA_POINT2");
                String dataPoint3 = receivedIntent.getStringExtra("DATA_POINT3");
                String dataPoint4 = receivedIntent.getStringExtra("DATA_POINT4");
                String dataPoint5 = receivedIntent.getStringExtra("DATA_POINT5");
                String dataPoint6 = receivedIntent.getStringExtra("DATA_POINT6");
                String dataPoint7 = receivedIntent.getStringExtra("DATA_POINT7");
                String dataPoint8 = receivedIntent.getStringExtra("DATA_POINT8");
                String dataPoint9 = receivedIntent.getStringExtra("DATA_POINT9");

                nc.setDataPoint1((MetrixStringHelper.isNullOrEmpty(dataPoint1)) ? "" : dataPoint1);
                nc.setDataPoint2((MetrixStringHelper.isNullOrEmpty(dataPoint2)) ? "" : dataPoint2);
                nc.setDataPoint3((MetrixStringHelper.isNullOrEmpty(dataPoint3)) ? "" : dataPoint3);
                nc.setDataPoint4((MetrixStringHelper.isNullOrEmpty(dataPoint4)) ? "" : dataPoint4);
                nc.setDataPoint5((MetrixStringHelper.isNullOrEmpty(dataPoint5)) ? "" : dataPoint5);
                nc.setDataPoint6((MetrixStringHelper.isNullOrEmpty(dataPoint6)) ? "" : dataPoint6);
                nc.setDataPoint7((MetrixStringHelper.isNullOrEmpty(dataPoint7)) ? "" : dataPoint7);
                nc.setDataPoint8((MetrixStringHelper.isNullOrEmpty(dataPoint8)) ? "" : dataPoint8);
                nc.setDataPoint9((MetrixStringHelper.isNullOrEmpty(dataPoint9)) ? "" : dataPoint9);
            }

            MobileApplication.NotificationContent = nc;
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        return (nc != null);
    }

    private void cleanUpPublicFiles() {
        if (PermissionHelper.checkPublicFilePermission(this)) {
            MetrixFileHelper.deletePublicFolder();
        }
    }
}
