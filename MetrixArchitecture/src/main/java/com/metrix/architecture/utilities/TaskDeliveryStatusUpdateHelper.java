package com.metrix.architecture.utilities;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.MmMessageIn;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by RaWiLK on 3/17/2016.
 */
public class TaskDeliveryStatusUpdateHelper {
    /***
     * This method is used to indicate/send a message to the FSM back-end when a task reaches the Mobile Client by updating
     * last_received_person_id, last_received_device_sequence, and last_received_dttm.
     * It is meant to be executed AFTER all relevant DB processing for the MmMessageIn parameter has occurred.
     * Further, we have the following additional conditions:
     * 1) RECEIVED_TASK_EVENT app param is populated with a valid event_type
     * 2) A task event does NOT exist with event_type that matches the RECEIVED_TASK_EVENT app param
     * 3) task.person_id is populated with the user's person_id.
     * @param clientMessageIn
     * @param originalOwnerIn
     */
    public static void generateNewTaskReceivedMessage(MmMessageIn clientMessageIn, String originalOwnerIn) {
        try {
            // If this is an empty message or it's not a Task HSR, SKIP
            if (MetrixStringHelper.isNullOrEmpty(clientMessageIn.message) || !clientMessageIn.message.contains("{\"task_hierarchy_select_result\":"))
                return;

            if (MetrixPublicCache.instance.getItem("INIT_STARTED") != null && !(Boolean)MetrixPublicCache.instance.getItem("INIT_STARTED")) {
                JSONObject JsonMessage = new JSONObject(clientMessageIn.message);
                JSONObject jsonTaskHierarchy = JsonMessage.getJSONObject("task_hierarchy_select_result");
                JSONObject jsonTask = jsonTaskHierarchy.getJSONObject("task");
                String currTaskId = jsonTask.getString("task_id");
                String currTaskPersonId = null;
                if (jsonTask.has("person_id"))
                    currTaskPersonId = jsonTask.getString("person_id");
                String currentDevicePersonId = User.getUser().personId;

                // If we can't find person_id in the message, SKIP
                if (MetrixStringHelper.isNullOrEmpty(currTaskPersonId))
                    return;

                // If the current person_id on the task does not match the Mobile user, SKIP
                if (!MetrixStringHelper.valueIsEqual(currentDevicePersonId, currTaskPersonId))
                    return;

                // If app param for the Received Task Event is null/empty, SKIP
                String appParamEventType = MobileApplication.getAppParam("RECEIVED_TASK_EVENT");
                if (MetrixStringHelper.isNullOrEmpty(appParamEventType))
                    return;

                // If this same app param has an invalid value (should exist in the event_type code table), SKIP
                if (MetrixDatabaseManager.getCount("event_type", String.format("event_type = '%s'", appParamEventType)) <= 0)
                    return;

                // If this task event already exists, SKIP
                String eventSequence = MetrixDatabaseManager.getFieldStringValue("task_event", "event_sequence", String.format("task_id = %1$s and event_type = '%2$s'", currTaskId, appParamEventType));
                if (!MetrixStringHelper.isNullOrEmpty(eventSequence))
                    return;

                UpdateTaskRecord(currTaskId);
            }
        } catch (Exception e) {
            LogManager.getInstance().error("MessageHandler -> GenerateNewTaskReceivedMessage : Exception");
            LogManager.getInstance().error(e);
        }
    }

    /**
     * This method is used to retrieve the original owner of an existing task.
     * It is meant to be executed BEFORE all relevant DB processing for the MmMessageIn parameter has occurred.
     * @param clientMessageIn
     */
    public static String getOriginalOwnerOfTaskReceivedMessage(MmMessageIn clientMessageIn) {
        String originalPersonId = null;

        try {
            if (MetrixStringHelper.isNullOrEmpty(clientMessageIn.message) || !clientMessageIn.message.contains("{\"task_hierarchy_select_result\":"))
                return null;

            if (MetrixPublicCache.instance.getItem("INIT_STARTED") != null && !(Boolean)MetrixPublicCache.instance.getItem("INIT_STARTED")
                    && clientMessageIn.transaction_type == MetrixTransactionTypes.UPDATE) {
                JSONObject JsonMessage = new JSONObject(clientMessageIn.message);
                JSONObject jsonTaskHierarchy = JsonMessage.getJSONObject("task_hierarchy_select_result");
                JSONObject jsonTask = jsonTaskHierarchy.getJSONObject("task");
                String currTaskId = jsonTask.getString("task_id");

                if (!MetrixStringHelper.isNullOrEmpty(currTaskId))
                    originalPersonId = MetrixDatabaseManager.getFieldStringValue("task", "person_id", String.format("task_id = %s", currTaskId));
            }
        } catch (Exception e) {
            LogManager.getInstance().error("TaskDeliveryStatusUpdateHelper -> GetOriginalOwnerOfTaskReceivedMessage : Exception");
            LogManager.getInstance().error(e);
        }

        return originalPersonId;
    }

    /***
     * This method is used to update task record with relevant information for last received.
     * @param currTaskId
     */
    public static void UpdateTaskRecord(String currTaskId) {
        String personId = "";
        String strDeviceId = "";
        String strCurrentDttm = "";

        try {
            if (MetrixStringHelper.isNullOrEmpty(currTaskId))
                return;

            String rowId = MetrixDatabaseManager.getFieldStringValue("task", "metrix_row_id", String.format("task_id = %s", currTaskId));

            MetrixSqlData taskData = new MetrixSqlData("task", MetrixTransactionTypes.UPDATE);
            taskData.dataFields.add(new DataField("metrix_row_id", rowId));
            taskData.dataFields.add(new DataField("task_id", currTaskId));

            personId = User.getUser().personId;
            taskData.dataFields.add(new DataField("last_received_person_id", personId));

            int deviceId = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
            strDeviceId = String.valueOf(deviceId);
            taskData.dataFields.add(new DataField("last_received_device_sequence", strDeviceId));

            strCurrentDttm = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, MetrixDateTimeHelper.ISO8601.Yes, true);
            taskData.dataFields.add(new DataField("last_received_dttm", strCurrentDttm));

            taskData.filter = String.format("task_id = %s", currTaskId);

            ArrayList<MetrixSqlData> taskTransaction = new ArrayList<MetrixSqlData>();
            taskTransaction.add(taskData);
            MetrixTransaction transactionInfo = new MetrixTransaction();
            if (MetrixUpdateManager.update(taskTransaction, true, transactionInfo, String.format("New Task(%s) Received", currTaskId), null))
                LogManager.getInstance().info(String.format("Task(%s) received... updating successful...[last_received_person_id(%s) last_received_device_sequence(%s) last_received_dttm(%s)]", currTaskId, personId, strDeviceId, strCurrentDttm));
            else
                LogManager.getInstance().info(String.format("Task(%s) received... updating fail...[last_received_person_id(%s) last_received_device_sequence(%s) last_received_dttm(%s)]", currTaskId, personId, strDeviceId, strCurrentDttm));
        } catch (Exception e) {
            LogManager.getInstance().error(String.format("Task(%s) received... exception occurred while updating...[last_received_person_id(%s) last_received_device_sequence(%s) last_received_dttm(%s)]", currTaskId, personId, strDeviceId, strCurrentDttm));
            LogManager.getInstance().error(e);
        }
    }
}
