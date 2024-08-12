package com.metrix.architecture.attachment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixManagerConstants;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixParcelable;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_SHOW_FILEDIALOG;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_TAKE_PICTURE;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_TAKE_VIDEO;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.SELECT_FILES;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_ATTACHMENT_ADD;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_ATTACHMENT_EDIT;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.baseMediaUri;

public class AttachmentWidgetManager {
    //region #Constants
    public static final String ATTACHMENT_WIDGET_PHOTO = "ATTACHMENT_WIDGET_PHOTO";
    public static final String ATTACHMENT_WIDGET_VIDEO = "ATTACHMENT_WIDGET_VIDEO";
    public static final String ATTACHMENT_WIDGET_FILE = "ATTACHMENT_WIDGET_FILE";

    private static final List<String> API_SCREEN_NAMES = Arrays.asList("FSMAttachmentAdd", "FSMAttachmentCard", "FSMAttachmentFullScreen", "FSMAttachmentList");
    //endregion

    //region #State variables with get-only public access for use by Attachment Widget activities
    private static AttachmentField.LaunchingAttachmentFieldData launchingAttachmentFieldData;

    private static String parentTable;
    private static String linkTable;
    public static String getParentTable() { return parentTable; }
    public static String getLinkTable() { return linkTable; }

    private static String attachmentListScreenName;
    private static int attachmentListScreenId;
    private static String attachmentCardScreenName;
    private static int attachmentCardScreenId;
    private static String transactionIdTableName;
    private static String transactionIdColumnName;
    public static String getAttachmentListScreenName() { return attachmentListScreenName; }
    public static int getAttachmentListScreenId() { return attachmentListScreenId; }
    public static String getAttachmentCardScreenName() { return attachmentCardScreenName; }
    public static int getAttachmentCardScreenId() { return attachmentCardScreenId; }
    public static String getTransactionIdTableName() { return transactionIdTableName; }
    public static String getTransactionIdColumnName() { return transactionIdColumnName; }
    public static AttachmentField.LaunchingAttachmentFieldData getAttachmentFieldData() { return launchingAttachmentFieldData; }

    // A LinkedHashMap will remember insertion order
    private static LinkedHashMap<String, Object> keyNameValueMap;
    public static LinkedHashMap<String, Object> getKeyNameValueMap() { return keyNameValueMap; }
    //endregion

    //region #Cross-applicable public methods
    public static String attachmentIsValid(String fileName) {
        String maximumSize = MobileApplication.getAppParam("ATTACHMENT_MAX_SIZE");
        if (MetrixStringHelper.isNullOrEmpty(maximumSize)) {
            maximumSize = "10242880";
        }

        try {
            File file = new File(fileName);
            if (file.length() > Long.parseLong(maximumSize)) {
                return AndroidResourceHelper.getMessage("ExceedMaxFileSize");
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        return "";
    }

    public static String convertKeyValueToDBString(String keyName, Object keyValue) throws Exception {
        String objStringValue = keyValue.toString();    // starting point that gives us something for exception reporting

        if (keyValue instanceof String) {
            objStringValue = (String)keyValue;
        } else if (keyValue instanceof Double) {
            Double numParam = (Double)keyValue;
            objStringValue = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(String.valueOf(numParam), Locale.US);
        } else if (keyValue instanceof Date) {
            Date dateParam = (Date)keyValue;
            objStringValue = MetrixDateTimeHelper.convertDateTimeFromDateToDB(dateParam);
        } else
            throw new Exception(String.format("Cannot use value %1$s for key column %2$s, due to a data type match failure.", objStringValue, keyName));

        return objStringValue;
    }

    public static String generateFileName(String extension) {
        // ([ParentTable]_[PersonID]_[DeviceSequence]_[Timestamp] (no file extension)
        User currentUser = User.getUser();
        String personId = currentUser.personId;
        String deviceSequence = String.valueOf(currentUser.sequence);
        String timestamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
        String fileName = String.format("%1$s_%2$s_%3$s_%4$s", parentTable, personId, deviceSequence, timestamp);
        String fullFileName = fileName+"."+extension;
        if(MetrixStringHelper.isNullOrEmpty(extension))
            fullFileName = fileName;
        String fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + fullFileName;

        File attachmentFile = new File(fullPath);
        if (attachmentFile.exists()) {
            boolean fileExist = true;
            int i = 0;
            do {
                fileName = String.format("%1$s_%2$s_%3$s_%4$s_%5$s", parentTable, personId, deviceSequence, timestamp, String.valueOf(i+1));
                if(!MetrixStringHelper.isNullOrEmpty(extension))
                    fullFileName = fileName+"."+extension;
                else
                    fullFileName = fileName;

                fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + fullFileName;
                attachmentFile = new File(fullPath);
                if (attachmentFile.exists()) {
                    i++;
                }
                else {
                    fileExist = false;
                }
            } while(fileExist);
        }
        return fullFileName;
    }

     public static boolean getListAllowModify() {
        boolean allowModify = false;
        try {
            HashMap<String, String> listScreenProperties = MetrixScreenManager.getScreenProperties(AttachmentWidgetManager.getAttachmentListScreenId());
            if (listScreenProperties != null && listScreenProperties.containsKey("allow_modify"))
                allowModify = MetrixStringHelper.valueIsEqual(listScreenProperties.get("allow_modify"), "Y");
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
        return allowModify;
    }

    public static MetrixTransaction getTransactionInfo() {
        MetrixTransaction transactionInfo;

        if (!MetrixStringHelper.isNullOrEmpty(transactionIdTableName) && !MetrixStringHelper.isNullOrEmpty(transactionIdColumnName))
            transactionInfo = MetrixTransaction.getTransaction(transactionIdTableName, transactionIdColumnName);
        else
            transactionInfo = new MetrixTransaction();

        return transactionInfo;
    }
    //endregion

    //region #Methods for accessing widget from Client Script
    public static void openFromScript(WeakReference<Activity> sourceScreen, String listScreenName, String cardScreenName, String transactionTable, String transactionColumn, String workflowName) {
        try {
            clearStateVariables();
            attachmentListScreenName = listScreenName;
            attachmentListScreenId = MetrixScreenManager.getScreenId(attachmentListScreenName);
            attachmentCardScreenName = cardScreenName;
            attachmentCardScreenId = (MetrixStringHelper.isNullOrEmpty(attachmentCardScreenName)) ? -1 : MetrixScreenManager.getScreenId(attachmentCardScreenName);
            transactionIdTableName = transactionTable;
            transactionIdColumnName = transactionColumn;

            setStateVariablesFromCache();

            Intent intent = MetrixActivityHelper.createActivityIntent(sourceScreen.get(), FSMAttachmentList.class);
            if(!MetrixStringHelper.isNullOrEmpty(workflowName)) {
                intent.putExtra("fromWorkflow", new MetrixParcelable<String>(workflowName));
                intent.putExtra("workflowScreenName", new MetrixParcelable<String>(listScreenName));
            }
            MetrixActivityHelper.startNewActivity(sourceScreen.get(), intent);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static String generateWhereClause(String attachmentId) {
        StringBuilder whereClauseBuilder = new StringBuilder();

        try {
            whereClauseBuilder.append("(attachment.internal_type is null or attachment.internal_type != 'SIGNATURE')");

            if (!MetrixStringHelper.isNullOrEmpty(attachmentId)) {
                whereClauseBuilder.append(String.format(" and attachment.attachment_id = %s", attachmentId));
            } else if (!keyNameValueMap.isEmpty()) {
                whereClauseBuilder.append(String.format(" and attachment.attachment_id in (select %1$s.attachment_id from %1$s where ", linkTable));
                generateDynamicKeyPortionOfWhereClause(whereClauseBuilder);
                whereClauseBuilder.append(")");
            }

            // Add a default hard-coded sort order, showing most recently-added attachment at the top
            whereClauseBuilder.append(" order by attachment.created_dttm desc");
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        return whereClauseBuilder.toString();
    }

    public static Hashtable<String, String> generateLinkTablePrimaryKeyList(String attachmentId) {
        Hashtable<String, String> primaryKeySet = new Hashtable<>();

        try {
            primaryKeySet.put("attachment_id", attachmentId);

            String keyName;
            Object keyValue;
            if (!keyNameValueMap.isEmpty()) {
                for (Map.Entry<String, Object> row : keyNameValueMap.entrySet()) {
                    keyName = row.getKey();
                    keyValue = row.getValue();
                    primaryKeySet.put(keyName, convertKeyValueToDBString(keyName, keyValue));
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
            primaryKeySet = null;
        }

        return primaryKeySet;
    }

    public static String getLinkTableMetrixRowId(String attachmentId) {
        String metrixRowId = null;

        try {
            StringBuilder whereClauseBuilder = new StringBuilder();
            whereClauseBuilder.append(String.format(" %1$s.attachment_id = %2$s ", linkTable, attachmentId));
            if (!keyNameValueMap.isEmpty()) {
                whereClauseBuilder.append(" and ");
                generateDynamicKeyPortionOfWhereClause(whereClauseBuilder);
            }

            metrixRowId = MetrixDatabaseManager.getFieldStringValue(linkTable, "metrix_row_id", whereClauseBuilder.toString());
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        return metrixRowId;
    }

    public static boolean isAttachmentAPIScreenName(String screenName) {
        return API_SCREEN_NAMES.contains(screenName);
    }

    private static void setStateVariablesFromCache() {
        // Only used in the Client Script case.  Attachment Field doesn't use the cache.
        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__ParentTable"))
            parentTable = String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__ParentTable"));

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__LinkTable"))
            linkTable = String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__LinkTable"));

        //region #Handle KeyName/Value Pairs
        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName1") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue1"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName1")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue1"));

        // From this point forward, once we detect a missing pair, leave this method.
        // We expect key-value pairs to be placed in cache sequentially.
        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName2") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue2"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName2")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue2"));
        else
            return;

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName3") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue3"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName3")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue3"));
        else
            return;

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName4") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue4"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName4")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue4"));
        else
            return;

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName5") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue5"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName5")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue5"));
        else
            return;

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName6") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue6"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName6")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue6"));
        else
            return;

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName7") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue7"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName7")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue7"));
        else
            return;

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName8") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue8"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName8")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue8"));
        else
            return;

        if (MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyName9") && MetrixPublicCache.instance.containsKey("AttachmentWidget__KeyValue9"))
            keyNameValueMap.put(String.valueOf(MetrixPublicCache.instance.getItem("AttachmentWidget__KeyName9")), MetrixPublicCache.instance.getItem("AttachmentWidget__KeyValue9"));
        else
            return;
        //endregion
    }
    //endregion

    public static void openFromWorkFlow(Activity sourceScreen, String workflowName) {
        String transactionTable = "";
        String transactionId = "";
        String workflowScreenName = "";
        if (workflowName.toLowerCase().contains("debrief")) {
            MetrixPublicCache.instance.addItem("AttachmentWidget__ParentTable", "task");
            MetrixPublicCache.instance.addItem("AttachmentWidget__LinkTable", "task_attachment");
            MetrixPublicCache.instance.addItem("AttachmentWidget__KeyName1", "task_id");
            MetrixPublicCache.instance.addItem("AttachmentWidget__KeyValue1", MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));

            workflowName = MetrixWorkflowManager.DEBRIEF_WORKFLOW;
            workflowScreenName = "DebriefAttachmentList"; // Default Attachment API list screen name for Debrief workflow
            transactionTable = "task";
            transactionId = "task_id";
        } else if (workflowName.toLowerCase().contains("quote")) {
            MetrixPublicCache.instance.addItem("AttachmentWidget__ParentTable", "quote");
            MetrixPublicCache.instance.addItem("AttachmentWidget__LinkTable", "quote_attachment");
            MetrixPublicCache.instance.addItem("AttachmentWidget__KeyName1", "quote_id");
            MetrixPublicCache.instance.addItem("AttachmentWidget__KeyValue1", MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id"));
            MetrixPublicCache.instance.addItem("AttachmentWidget__KeyName2", "quote_version");
            MetrixPublicCache.instance.addItem("AttachmentWidget__KeyValue2", MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version"));

            workflowName = MetrixWorkflowManager.QUOTE_WORKFLOW;
            workflowScreenName = "QuoteAttachmentList"; // Default Attachment API list screen name for Quote workflow
            transactionTable = "quote";
            transactionId = "quote_id";
        }
        WeakReference<Activity> currentActivity = new WeakReference<Activity>(sourceScreen);
        AttachmentWidgetManager.openFromScript(currentActivity, workflowScreenName, "BaseAttachmentCard", transactionTable, transactionId, workflowName);
    }

    //region #Methods for accessing widget from Attachment Field
    public static void openFromFieldForInsert(WeakReference<Activity> sourceScreen, AttachmentField fieldInUse, int requestCodeUsed, String selectedFileName) {
        try {
            clearStateVariables();

            launchingAttachmentFieldData = fieldInUse.getLaunchingAttachmentFieldData();

            attachmentCardScreenId = fieldInUse.getCardScreenId();
            attachmentCardScreenName = (attachmentCardScreenId > 0) ? MetrixScreenManager.getScreenName(attachmentCardScreenId) : "";
            parentTable = fieldInUse.getFieldTableName();
            transactionIdTableName = fieldInUse.getTransactionIdTableName();
            transactionIdColumnName = fieldInUse.getTransactionIdColumnName();

            // Set normal goingBackFromLookup flag now, so that when we come back to the originating screen,
            // it should act like we're coming back from a lookup.
            MetrixPublicCache.instance.addItem("goingBackFromLookup", true);

            // Also, remember current original values to be recalled when we leave the widget
            HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);
            MetrixPublicCache.instance.addItem("originalValuesForAttachmentField", originalValues);

            // At this point, we already know the request came back with an OK status, so just figure out how to set up FSMAttachmentAdd navigation.
            Activity activity = sourceScreen.get();
            Intent intent = MetrixActivityHelper.createActivityIntent(activity, FSMAttachmentAdd.class);
            switch (requestCodeUsed) {
                case BASE_TAKE_PICTURE:
                    intent.putExtra("ImageUri", baseMediaUri);
                    intent.putExtra("FromCamera", true);
                    intent.putExtra("isFromList", false);
                    break;
                case BASE_TAKE_VIDEO:
                    intent.putExtra("VideoUri", baseMediaUri);
                    intent.putExtra("FromVideoCamera", true);
                    intent.putExtra("isFromList", false);
                    break;
                case SELECT_FILES:
                    Uri selectedFileUri = Uri.parse(selectedFileName);
                    intent.putExtra("FileUri", selectedFileUri);
                    intent.putExtra("FromFileUri", true);
                    intent.putExtra("isFromList", false);
                    break;
                case BASE_SHOW_FILEDIALOG:
                    intent.putExtra("FileName", selectedFileName);
                    intent.putExtra("FromFileDialog", true);
                    intent.putExtra("isFromList", false);
                    break;
            }
            activity.startActivityForResult(intent, BASE_ATTACHMENT_ADD);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void openFromFieldForUpdate(WeakReference<Activity> sourceScreen, AttachmentField fieldInUse) {
        try {
            clearStateVariables();

            launchingAttachmentFieldData = fieldInUse.getLaunchingAttachmentFieldData();

            attachmentCardScreenId = fieldInUse.getCardScreenId();
            attachmentCardScreenName = (attachmentCardScreenId > 0) ? MetrixScreenManager.getScreenName(attachmentCardScreenId) : "";
            parentTable = fieldInUse.getFieldTableName();
            transactionIdTableName = fieldInUse.getTransactionIdTableName();
            transactionIdColumnName = fieldInUse.getTransactionIdColumnName();

            // Set normal goingBackFromLookup flag now, so that when we come back to the originating screen,
            // it should act like we're coming back from a lookup.
            MetrixPublicCache.instance.addItem("goingBackFromLookup", true);

            // Also, remember current original values to be recalled when we leave the widget
            HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);
            MetrixPublicCache.instance.addItem("originalValuesForAttachmentField", originalValues);

            Intent intent = MetrixActivityHelper.createActivityIntent(sourceScreen.get(), FSMAttachmentFullScreen.class);
            intent.putExtra("isFromList", false);
            intent.putExtra("attachmentFieldIsEnabled", fieldInUse.isEnabled());
            intent.putExtra("attachmentIdFromField", fieldInUse.mHiddenAttachmentIdTextView.getText().toString());
            intent.putExtra("isOnDemandFromField", fieldInUse.getAttachmentIsOnDemand());
            sourceScreen.get().startActivityForResult(intent, BASE_ATTACHMENT_EDIT);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void setParentTableFromField(String parentTableName) { parentTable = parentTableName; }
    //endregion

    //region #Private Helper Methods
    private static void clearStateVariables() {
        launchingAttachmentFieldData = null;

        parentTable = "";
        linkTable = "";

        attachmentListScreenName = "";
        attachmentListScreenId = -1;
        attachmentCardScreenName = "";
        attachmentCardScreenId = -1;
        transactionIdTableName = "";
        transactionIdColumnName = "";

        keyNameValueMap = new LinkedHashMap<>();
    }

    private static void generateDynamicKeyPortionOfWhereClause(StringBuilder whereClauseBuilder) throws Exception {
        boolean isFirst = true;
        for (Map.Entry<String, Object> row : keyNameValueMap.entrySet()) {
            if (!isFirst)
                whereClauseBuilder.append(" and ");

            String keyName = row.getKey();
            Object keyValue = row.getValue();
            String objStringValue = convertKeyValueToDBString(keyName, keyValue);
            if (keyValue instanceof Double)
                whereClauseBuilder.append(String.format("%1$s.%2$s = %3$s", linkTable, keyName, objStringValue));
            else
                whereClauseBuilder.append(String.format("%1$s.%2$s = '%3$s'", linkTable, keyName, objStringValue));

            isFirst = false;
        }
    }
    //endregion
}
