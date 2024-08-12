package com.metrix.metrixmobile.system;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.JobList;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MetrixTimeClockAssistant;

import java.util.Hashtable;
import java.util.Locale;

/**
 * Created by RaWiLK on 3/21/2016.
 */
public class MetrixPooledTaskAssignmentManager {

    private static MetrixPooledTaskAssignmentManager mMetrixPooledTaskAssignmentManager = null;

    public static synchronized MetrixPooledTaskAssignmentManager instance() {
        if (mMetrixPooledTaskAssignmentManager == null)
            mMetrixPooledTaskAssignmentManager = new MetrixPooledTaskAssignmentManager();
        return mMetrixPooledTaskAssignmentManager;
    }

    public void doTeamTaskAccept(Activity activity, String taskId, String taskStatus, String taskStatusComment, boolean updateTimer) {
        UpdateAsyncTask updateAsyncTask = new UpdateAsyncTask(activity, taskId, taskStatus, taskStatusComment, updateTimer);
        updateAsyncTask.execute();
    }

    public boolean isPooledTask(String taskId) {
        String currTaskPersonId = MetrixDatabaseManager.getFieldStringValue("task", "person_id", String.format("task_id = '%s'", taskId));
        return (MetrixStringHelper.isNullOrEmpty(currTaskPersonId)) ? true : false;
    }

    public boolean currentUserIsTheOwnerOfTheTask(String taskId) {
        String currTaskPersonId = MetrixDatabaseManager.getFieldStringValue("task", "person_id", String.format("task_id = '%s'", taskId));
        return (MetrixStringHelper.valueIsEqual(currTaskPersonId, User.getUser().personId)) ? true : false;
    }

    public boolean currentTaskExists(String taskId) {
        long count = MetrixDatabaseManager.getCount("task", String.format("task_id = '%s'", taskId));
        return (count > 0) ? true : false;
    }

    public void dismissCurrentScreen(Activity activity, String message) {
        MetrixUIHelper.showSnackbar(activity, message);
        Handler handler = new Handler();
        TimeoutCallback timeoutCallback = new TimeoutCallback(activity);
        handler.postDelayed(timeoutCallback, 5000);
    }

    private class TimeoutCallback implements Runnable {
        Activity currAtivity;

        public TimeoutCallback(Activity activity) {
            currAtivity = activity;
        }

        public void run() {
            MetrixActivityHelper.startNewActivityAndFinish(currAtivity, JobList.class);
        }
    }

    private class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {

        private String mLatitude;
        private String mLongitude;
        private String mComment;
        private boolean networkStatus = true;
        private Activity mActivity;
        private String mTaskId;
        private String mTaskStatus;
        private boolean mUpdateTimer;

        public UpdateAsyncTask(Activity activty, String taskId, String taskStatus, String comment, boolean updateTimer) {
            mActivity = activty;
            mTaskId = taskId;
            mTaskStatus = taskStatus;
            mComment = comment;
            mUpdateTimer = updateTimer;
        }

        @Override
        protected void onPreExecute() {
            try {
                MobileApplication.stopSync(mActivity);
                MobileApplication.startSync(mActivity, 5);
            } catch (Exception ex) {
                LogManager.getInstance().error(ex);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
                String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));
                if (MetrixActivity.ping(baseUrl, remote) == false) {
                    networkStatus = false;
                }
            } catch (Exception ex) {
                networkStatus = false;
                LogManager.getInstance().error(ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                if (networkStatus) {

                    String updateGPS = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
                    if (!MetrixStringHelper.isNullOrEmpty(updateGPS) && updateGPS.compareToIgnoreCase("Y") == 0 && MetrixRoleHelper.isGPSFunctionEnabled("GPS_TASK")) {
                        Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mActivity);
                        if (currentLocation != null) {
                            mLatitude = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(Double.toString(currentLocation.getLatitude()), Locale.US);
                            mLongitude = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(Double.toString(currentLocation.getLongitude()), Locale.US);
                        }
                    }

                    // start waiting dialog on-screen
                    MetrixUIHelper mUIHelper = ((MetrixActivity) mActivity).getMetrixUIHelper();
                    mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("PTaskAssignmentInProgress"));

                    if (mActivity instanceof DebriefActivity)
                        ((DebriefActivity) mActivity).setPooledTaskAssignmentStarted(true);

                    Hashtable<String, String> params = new Hashtable<String, String>();
                    int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

                    params.put("task_id", mTaskId);
                    params.put("person_id", User.getUser().personId);
                    params.put("task_status", mTaskStatus);
                    if (!MetrixStringHelper.isNullOrEmpty(mComment))
                        params.put("task_status_comment", mComment);
                    params.put("status_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, MetrixDateTimeHelper.ISO8601.Yes, true));

                    if (!MetrixStringHelper.isNullOrEmpty(mLatitude))
                        params.put("geocode_lat", mLatitude);
                    if (!MetrixStringHelper.isNullOrEmpty(mLongitude))
                        params.put("geocode_long", mLongitude);

                    params.put("device_sequence", String.valueOf(device_id));
                    if (mUpdateTimer)
                        MetrixTimeClockAssistant.updateAutomatedTimeClock(mActivity, mTaskStatus);

                    MetrixPerformMessage performPTA = new MetrixPerformMessage("perform_pooled_task_assignment", params);
                    performPTA.save();
                } else {
                    MobileApplication.stopSync(mActivity);
                    MobileApplication.startSync(mActivity);
                    MetrixUIHelper.showSnackbar(mActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"));
                }
            } catch (Exception ex) {
                MobileApplication.stopSync(mActivity);
                MobileApplication.startSync(mActivity);
                LogManager.getInstance().error(ex);
            }
        }
    }
}

