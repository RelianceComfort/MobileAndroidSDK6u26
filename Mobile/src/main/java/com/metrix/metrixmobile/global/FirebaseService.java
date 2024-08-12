package com.metrix.metrixmobile.global;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.notification.FSMNotificationAssistant;
import com.metrix.architecture.services.SyncWorker;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import java.util.Map;

public class FirebaseService extends FirebaseMessagingService {
    private String TAG = "FirebaseService";

    private static final String WORK_TAG = "FSM_BG_SYNC";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            // ...
            // TODO(developer): Handle FCM messages here.
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Log.d(TAG, "From: " + remoteMessage.getFrom());

            String nhMessage = "";
            // Check if message contains a notification payload.
            if (remoteMessage.getNotification() != null) {
                Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
                nhMessage = remoteMessage.getNotification().getBody();
            } else {
                Map<String, String> payloadData = remoteMessage.getData();
                if (payloadData != null && !payloadData.isEmpty()) {
                    nhMessage = payloadData.values().iterator().next();
                }
            }

            // For both On-Demand Push and Background Sync, we don't want to act unless the app actually is in the background.
            if (MobileApplication.appIsInBackground(MobileApplication.getAppContext())) {
                if (!MetrixStringHelper.isNullOrEmpty(nhMessage)) {
                    // Process an on-demand push notification
                    sendNotification(nhMessage);
                } else {
                    // Process a silent notification for Background Sync
                    // Create a Constraints object that defines when the bg sync should run. Ex: must be connected to the internet, etc.
                    Constraints constraints = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();

                    OneTimeWorkRequest bgSyncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                            .setConstraints(constraints)
                            .build();

                    Log.e("FSM_Sync", "Scheduling bg sync work");
                    WorkManager.getInstance(this).enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.KEEP, bgSyncRequest);
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void sendNotification(String msg) {
        try {
            Context appContext = MobileApplication.getAppContext();
            Class<?> splashClass = Class.forName("com.metrix.metrixmobile.system.Splash");
            Intent notifIntent = new Intent(appContext, splashClass);
            notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notifIntent.putExtra("NOTIFICATION_ID", 0);

            mNotificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            PendingIntent contentIntent;
            if (Build.VERSION.SDK_INT < 31) {
                contentIntent = PendingIntent.getActivity(appContext, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                contentIntent = PendingIntent.getActivity(appContext, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Bitmap iconToUse = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.ifs_logo_large);

            NotificationCompat.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                builder = new NotificationCompat.Builder(appContext, FSMNotificationAssistant.HUB_NOTIFICATION_CHANNEL_ID);
            else
                builder = new NotificationCompat.Builder(appContext);

            builder.setContentText(msg)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(contentIntent)
                    .setSound(defaultSoundUri)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setLargeIcon(iconToUse)
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                    .setAutoCancel(true);

            builder.setContentIntent(contentIntent);
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }
}