package com.metrix.architecture.signature;

import android.app.Activity;
import android.content.Intent;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixManagerConstants;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static com.metrix.architecture.superclasses.MetrixBaseActivity.BASE_TAKE_SIGNATURE;

public class SignatureWidgetManager {
    //region #Constants
    private static final List<String> API_SCREEN_NAMES = Arrays.asList("FSMSignatureAdd");
    //endregion

    //region #State variables with get-only public access for use by Attachment Widget activities
    private static WeakReference<Activity> mCurrentActivity;
    private static WeakReference<SignatureField> mSignatureField;
    public static Activity getCurrentActivity() { return mCurrentActivity.get(); }
    public static SignatureField getAttachmentField() { return mSignatureField.get(); }

    private static String parentTable;
    private static String linkTable;
    public static String getParentTable() { return parentTable; }
    public static String getLinkTable() { return linkTable; }

    private static String transactionIdTableName;
    private static String transactionIdColumnName;
    public static String getTransactionIdTableName() { return transactionIdTableName; }
    public static String getTransactionIdColumnName() { return transactionIdColumnName; }

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

    public static String generateFileName() {
        // ([ParentTable]_[PersonID]_[DeviceSequence]_[Timestamp] (no file extension)
        User currentUser = User.getUser();
        String personId = currentUser.personId;
        String deviceSequence = String.valueOf(currentUser.sequence);
        String timestamp = MetrixDateTimeHelper.getCurrentDate("yyyyMMddHHmmss");
        return String.format("%1$s_%2$s_%3$s_%4$s", parentTable, personId, deviceSequence, timestamp);
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

    //region #Methods for accessing widget from Signature Field
    public static void openFromFieldForInsert(WeakReference<Activity> sourceScreen, SignatureField fieldInUse, int requestCodeUsed, String selectedFileName) {
        try {
            clearStateVariables();

            mCurrentActivity = sourceScreen;
            mSignatureField = new WeakReference<>(fieldInUse);

            parentTable = fieldInUse.getFieldTableName();
            transactionIdTableName = fieldInUse.getTransactionIdTableName();
            transactionIdColumnName = fieldInUse.getTransactionIdColumnName();

            // Set normal goingBackFromLookup flag now, so that when we come back to the originating screen,
            // it should act like we're coming back from a lookup.
            MetrixPublicCache.instance.addItem("goingBackFromLookup", true);

            // Also, remember current original values to be recalled when we leave the widget
            HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);
            MetrixPublicCache.instance.addItem("originalValuesForAttachmentField", originalValues);

            // At this point, we already know the request came back with an OK status, so just figure out how to set up FSMSignatureAdd navigation.
            Activity activity = mCurrentActivity.get();
            Intent intent = MetrixActivityHelper.createActivityIntent(activity, FSMSignatureAdd.class);
            if(requestCodeUsed == BASE_TAKE_SIGNATURE) {
                MetrixActivityHelper.startNewActivity(activity, intent);
            }

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void setParentTableFromField(String parentTableName) { parentTable = parentTableName; }
    //endregion

    //region #Private Helper Methods
    private static void clearStateVariables() {
        mCurrentActivity = null;
        mSignatureField = null;

        parentTable = "";
        linkTable = "";
        transactionIdTableName = "";
        transactionIdColumnName = "";

        keyNameValueMap = new LinkedHashMap<>();
    }
    //endregion
}
