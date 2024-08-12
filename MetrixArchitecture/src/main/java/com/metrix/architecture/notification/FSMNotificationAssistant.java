package com.metrix.architecture.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.FSMNotificationContent;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class FSMNotificationAssistant {
    public static final String CHANNEL_NAME = "LOCAL_NOTIF";
    public static final String HUB_NOTIFICATION_CHANNEL_ID = "fsm-nh-channel-id";

    public static final int BGS_INIT_NOTIFICATION_ID = -10;
    public static final int BGS_PCHANGE_NOTIFICATION_ID = -20;

    public static void generateBGSyncPendingInitNotification() {
        generateNotification(null, AndroidResourceHelper.getMessage("BGSyncPendingInit"), null, null, BGS_INIT_NOTIFICATION_ID);
    }

    public static void generateBGSyncPendingPChangeNotification() {
        generateNotification(null, AndroidResourceHelper.getMessage("BGSyncPendingPChange"), null, null, BGS_PCHANGE_NOTIFICATION_ID);
    }

    public static void generateNotification(String title, String content, String clientScript, String[] dataPoints) {
        generateNotification(title, content, clientScript, dataPoints, null);
    }

    public static void populateDataPointCache(FSMNotificationContent nc) {
        try {
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint1", nc.getDataPoint1());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint2", nc.getDataPoint2());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint3", nc.getDataPoint3());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint4", nc.getDataPoint4());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint5", nc.getDataPoint5());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint6", nc.getDataPoint6());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint7", nc.getDataPoint7());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint8", nc.getDataPoint8());
            MetrixPublicCache.instance.addItem("LocalNotif__DataPoint9", nc.getDataPoint9());
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void clearDataPointCache() {
        try {
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint1");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint2");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint3");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint4");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint5");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint6");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint7");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint8");
            MetrixPublicCache.instance.removeItem("LocalNotif__DataPoint9");
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static boolean pushNotificationIsEnabled() {
        try {
            String pushMsgEnabled = MetrixDatabaseManager.getAppParam("PUSH_MESSAGING_ENABLED");
            String pushHubPath = MetrixDatabaseManager.getAppParam("PUSH_NOTIFICATION_HUB_PATH");
            String pushHubUrl = MetrixDatabaseManager.getAppParam("PUSH_NOTIFICATION_HUB_URL");
            String pushBroadcastTag = MetrixDatabaseManager.getAppParam("PUSH_NOTIFICATION_BROADCAST_TAG");

            if (MetrixStringHelper.valueIsEqual(pushMsgEnabled, "Y") && !MetrixStringHelper.isNullOrEmpty(pushHubPath)
                    && !MetrixStringHelper.isNullOrEmpty(pushHubUrl) && !MetrixStringHelper.isNullOrEmpty(pushBroadcastTag)) {
                return true;
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
        return false;
    }

    public static void recordUserNotificationPermission() {
        try {
            if (pushNotificationIsEnabled()) {
                Context appContext = MobileApplication.getAppContext();
                String currentUser = SettingsHelper.getActivatedUser(appContext);
                int currentSequence = SettingsHelper.getDeviceSequence(appContext);
                String currentSequenceString = String.valueOf(currentSequence);

                String pnEnabledDBValue = MetrixDatabaseManager.getFieldStringValue("person_mobile", "pn_enabled", String.format("person_id = '%1$s' and sequence = %2$s", currentUser, currentSequenceString));

                boolean userAllowsNotifications = NotificationManagerCompat.from(appContext).areNotificationsEnabled();
                String pnEnabledStateValue = userAllowsNotifications ? "Y" : "N";

                if (!MetrixStringHelper.valueIsEqual(pnEnabledStateValue, pnEnabledDBValue)) {
                    String rowId = MetrixDatabaseManager.getFieldStringValue("person_mobile", "metrix_row_id", String.format("person_id = '%1$s' and sequence = %2$s", currentUser, currentSequenceString));
                    MetrixSqlData personData = new MetrixSqlData("person_mobile", MetrixTransactionTypes.UPDATE);
                    personData.dataFields.add(new DataField("metrix_row_id", rowId));
                    personData.dataFields.add(new DataField("pn_enabled", pnEnabledStateValue));
                    personData.dataFields.add(new DataField("person_id", currentUser));
                    personData.dataFields.add(new DataField("sequence", currentSequenceString));
                    personData.filter = String.format("metrix_row_id = %s", rowId);

                    ArrayList<MetrixSqlData> personTransaction = new ArrayList<MetrixSqlData>();
                    personTransaction.add(personData);
                    MetrixTransaction transactionInfo = new MetrixTransaction();
                    MetrixUpdateManager.update(personTransaction, true, transactionInfo, AndroidResourceHelper.getMessage("PersonMobile"), null);
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private static void generateNotification(String title, String content, String clientScript, String[] dataPoints, Integer notificationIdIn) {
        try {
            Context appContext = MobileApplication.getAppContext();
            content = content.replace("chr(10)", "\n");

            // notificationId is a unique int for each notification that you must define - or it's provide explicitly by the caller
            int notificationId = (notificationIdIn == null) ? generateNotificationId() : notificationIdIn;

            NotificationCompat.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                builder = new NotificationCompat.Builder(appContext, CHANNEL_NAME);
            else
                builder = new NotificationCompat.Builder(appContext);

            Class<?> splashClass = Class.forName("com.metrix.metrixmobile.system.Splash");
            Intent notifIntent = new Intent(appContext, splashClass);
            notifIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notifIntent.putExtra("NOTIFICATION_ID", notificationId);
            if (!MetrixStringHelper.isNullOrEmpty(clientScript)) {
                notifIntent.putExtra("CLIENT_SCRIPT", clientScript);
                notifIntent.putExtra("DATA_POINT1", dataPoints[0]);
                notifIntent.putExtra("DATA_POINT2", dataPoints[1]);
                notifIntent.putExtra("DATA_POINT3", dataPoints[2]);
                notifIntent.putExtra("DATA_POINT4", dataPoints[3]);
                notifIntent.putExtra("DATA_POINT5", dataPoints[4]);
                notifIntent.putExtra("DATA_POINT6", dataPoints[5]);
                notifIntent.putExtra("DATA_POINT7", dataPoints[6]);
                notifIntent.putExtra("DATA_POINT8", dataPoints[7]);
                notifIntent.putExtra("DATA_POINT9", dataPoints[8]);
            }
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT < 31) {
                pendingIntent = PendingIntent.getActivity(appContext, notificationId, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent.getActivity(appContext, notificationId, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }

            int iconResID = 0;
            HashMap<String, Integer> resData = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("MetrixLocalNotificationResourceData");
            if (resData != null) {
                iconResID = resData.get("R.drawable.ifs_transparent_logo");
            } else {
                // try to grab from app settings instead
                iconResID = SettingsHelper.getIntegerSetting(appContext, "R.drawable.ifs_transparent_logo");
            }

            if (iconResID != 0)
                builder.setSmallIcon(iconResID);

            builder.setContentTitle(title)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    // Set the intent that will fire when the user taps the notification
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
            notificationManager.notify(notificationId, builder.build());
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private static int generateNotificationId() {
        return Math.abs((int) SystemClock.uptimeMillis());
    }
}
