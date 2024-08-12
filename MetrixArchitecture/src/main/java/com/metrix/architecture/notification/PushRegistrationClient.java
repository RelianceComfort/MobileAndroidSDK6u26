package com.metrix.architecture.notification;

import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixTimeSpan;
import com.metrix.architecture.utilities.SettingsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;

import io.reactivex.Single;

public class PushRegistrationClient {
    private String Backend_Endpoint;

    public PushRegistrationClient() {
        Backend_Endpoint = SettingsHelper.getServiceAddress(MobileApplication.getAppContext()) + "/register";
    }

    public static Single<String> getFcmToken(Executor executor) {
        return Single.create((subscriber) -> {
            final Task<InstanceIdResult> task = FirebaseInstanceId.getInstance().getInstanceId();
            task.addOnSuccessListener(executor, (result) -> subscriber.onSuccess(result.getToken()));
            task.addOnFailureListener(executor, subscriber::onError);
        });
    }

    public void register(String handle) {
        try {
            if (FSMNotificationAssistant.pushNotificationIsEnabled()) {
                int deviceSequence = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
                String deviceSequenceString = String.valueOf(deviceSequence);

                String registrationId = SettingsHelper.getStringSetting(MobileApplication.getAppContext(), SettingsHelper.PUSH_REGISTRATION_ID);
                if (MetrixStringHelper.isNullOrEmpty(registrationId)) {
                    // Try to populate from person_mobile table in DB
                    registrationId = MetrixDatabaseManager.getFieldStringValue("person_mobile", "registration_id", String.format("sequence = %s", deviceSequenceString));
                }

                if (MetrixStringHelper.isNullOrEmpty(registrationId))
                    createRegistrationIdOrRequestNewOne(handle);
                else {
                    String expirationDttm = MetrixDatabaseManager.getFieldStringValue("person_mobile", "expiration_dttm", String.format("sequence = %s", deviceSequenceString));
                    if (MetrixStringHelper.isNullOrEmpty(expirationDttm))
                        expirationDttm = "1980-01-01";

                    // Get the time span defined as [Expiration Date] - [Now + 1 month].
                    // If this span is negative, we need to re-register the device, as expiration is less than a month away.
                    Date expireDate = MetrixDateTimeHelper.convertDateTimeFromDBToDate(expirationDttm);
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MONTH, 1);
                    Date nowPlusAMonth = cal.getTime();

                    MetrixTimeSpan expirationGap = MetrixDateTimeHelper.getDateDifference(expireDate, nowPlusAMonth);
                    if (expirationGap.mMilliseconds < 0) {
                        // Re-register entirely
                        createRegistrationIdOrRequestNewOne(handle);
                    } else {
                        // Refresh existing registration with new token
                        upsertRegistration(registrationId, handle);
                    }
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void upsertRegistration(String registrationId, String handle) {
        try {
            JSONObject deviceInfo = generateDeviceInfo(handle);
            HttpURLConnection connection = (HttpURLConnection) new URL(Backend_Endpoint + "/handle/" + registrationId).openConnection();
            connection.setRequestProperty("Authorization", "Basic");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            try (OutputStream os = connection.getOutputStream()) {
                byte[] msg = deviceInfo.toString().getBytes(StandardCharsets.UTF_8);
                os.write(msg, 0, msg.length);
            }
            int statusCode = connection.getResponseCode();
            LogManager.getInstance().debug("upsertRegistration status code: %d", statusCode);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void createRegistrationIdOrRequestNewOne(String handle) {
        String registrationId = "";
        try {
            JSONObject deviceInfo = generateDeviceInfo(handle);
            registrationId = new MetrixRemoteExecutor(MobileApplication.getAppContext()).executePost(Backend_Endpoint + "/handle", "application/json", deviceInfo.toString());
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        if (!MetrixStringHelper.isNullOrEmpty(registrationId))
            SettingsHelper.saveStringSetting(MobileApplication.getAppContext(), SettingsHelper.PUSH_REGISTRATION_ID, registrationId, false);
    }

    private JSONObject generateDeviceInfo(String handle) throws JSONException {
        String personId = SettingsHelper.getActivatedUser(MobileApplication.getAppContext());
        int deviceSequence = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("DeviceSequence", String.valueOf(deviceSequence));
        deviceInfo.put("Handle", handle);
        deviceInfo.put("PersonId", personId);
        deviceInfo.put("Platform", "android");
        return deviceInfo;
    }
}
