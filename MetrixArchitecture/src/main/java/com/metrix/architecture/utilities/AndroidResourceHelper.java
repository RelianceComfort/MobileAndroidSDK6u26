package com.metrix.architecture.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hesplk on 11/3/2016.
 */
public class AndroidResourceHelper {

    private static HashMap<String, String> _messageCache = new HashMap<>(256);
    public static String getMessage(String messageId) {
        String resourceValue = "";

        try {
            resourceValue = readMessageFromDb(messageId, null);

            if (MetrixStringHelper.isNullOrEmpty(resourceValue)) {
                final Context context = MobileApplication.getAppContext();
                resourceValue = tryBackupString(context, messageId);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
        return MetrixStringHelper.isNullOrEmpty(resourceValue) ? messageId : resourceValue;
    }

    public static String getMessageFromScript(String messageId, String messageType) {
        String resourceValue = "";

        try {
            resourceValue = readMessageFromDb(messageId, messageType);

            if (MetrixStringHelper.isNullOrEmpty(resourceValue)) {
                final Context context = MobileApplication.getAppContext();
                resourceValue = tryBackupString(context, messageId);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        return MetrixStringHelper.isNullOrEmpty(resourceValue) ? messageId : resourceValue;
    }

    /**
     * This method takes care of retrieval and formatting of messages from database
     * @param messageId Resource message id
     * @param params A varargs parameter to be used in the formatting of the message
     * @return Formatted string if the message is properly formatted. Otherwise, Log and return an
     * error with the message id
     */
    public static String getMessage(String messageId, Object... params) {
        final String resourceString = getMessage(messageId);
        return formatMessage(resourceString, messageId, params);
    }

    public static String formatMessage(String message, String messageId, Object... params) {
        try {
            return String.format(message, params);
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            final String errorResource = getMessage("InvalidMessageFormat1Args");
            return String.format(errorResource, messageId);
        }
    }

    public static String createLookupTitle(String titleId, String itemId) {
        final String item = AndroidResourceHelper.getMessage(itemId);
        return AndroidResourceHelper.getMessage(titleId, item.toLowerCase());
    }

    private static String processString(String resValue) {
        resValue = resValue.replace("\\n", System.getProperty("line.separator"));
        resValue = resValue.replaceAll("'", "''");
        resValue = handleStringFormatters(resValue);
        return resValue;
    }

    private static String processStringForScript(String resValue) {
        resValue = resValue.replace("\\n", System.getProperty("line.separator"));
        resValue = resValue.replaceAll("'", "''");
        return resValue;
    }

    private static String readMessageFromDb(String messageId, String messageType) {

        try {
            if (_messageCache.containsKey(messageId))
            {
                return  _messageCache.get(messageId);
            }

            if (MobileApplication.DatabaseLoaded) {

                if(MetrixStringHelper.isNullOrEmpty(messageType)){
                    messageType = "MM_RESOURCE_STRING";
                }

                String query = String.format("select message_text from mm_message_def_view where message_id = '%s' and message_type = '%s'", messageId, messageType);
                String messageText = MetrixDatabaseManager.getFieldStringValue(query);

                if (!MetrixStringHelper.isNullOrEmpty(messageText))
                {
                    messageText = processString(messageText);
                    _messageCache.put(messageId, messageText);
                    return messageText;
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
        return null;
    }

    public static void clearMessageTranslationCache()
    {
        _messageCache.clear();
    }

    private static String tryBackupString(Context context, String messageId) {
        String resourceValue = "";
        if (context != null) {
            String pkgName = context.getPackageName();
            int strId = context.getResources().getIdentifier(messageId, "string", pkgName);
            if (strId != 0)
                resourceValue = context.getResources().getString(strId);
        }
        return resourceValue;
    }

    private static String handleStringFormatters(String resourceValue) {
        String DIGIT = "d";
        String STRING = "s";
        Matcher matcher = Pattern.compile("(\\{.*?\\})")
                .matcher(resourceValue);
        try {
            while (matcher.find()) {
                String element = matcher.group();
                String innerData = element.replaceAll("[{}]", "");
                //If type is not available, default type will be STRING
                //ex: {0} --> %s
                String type = STRING;
                //ex: {0d}
                if (innerData.endsWith(DIGIT)) {
                    type = DIGIT;
                }

                String javaFormatter;
                final int innerLength = innerData.length();
                if(innerLength == 1 || innerLength == 2) {
                    //ex: {0}/{1s}/{2d} --> %1$s / %2$s / %3$d
                    //messages use zero-indexed formatters (as they should),
                    //but when we specify order in a Java formatter, they are 1-indexed
                    String index = innerData.replace(type, "");
                    index = String.valueOf(Integer.valueOf(index) + 1);
                    javaFormatter = "%" + index + "$" + type;
                    resourceValue = resourceValue.replace(element, javaFormatter);
                } else if (innerLength == 3) {
                    //ex: 00:{02d} --> 00:%02d
                    javaFormatter = "%" + innerData.substring(0, 1) + innerData.substring(1, 2) + type;
                    resourceValue = resourceValue.replace(element, javaFormatter);
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        } finally {
            return resourceValue;
        }
    }

    //This method can only be used for screens
    //Activity will be set
    public static void setResourceValues(Activity activity, List<ResourceValueObject> resourceValues) {
        try {
            if (resourceValues == null || resourceValues.size() == 0) return;
            MetrixPublicCache.instance.addItem("StringResourceActivity", activity);

            StringBuilder selectQuery = new StringBuilder();
            List<String> messageIds = new ArrayList<String>();
            selectQuery.append("select message_id,message_text from mm_message_def_view where (message_id = ");
            for (int r = 0; r < resourceValues.size(); r++) {
                if (r == 0) {
                    selectQuery.append(String.format("'%s'", resourceValues.get(r).messageId));
                } else {
                    selectQuery.append(String.format(" or message_id = '%s'", resourceValues.get(r).messageId));
                }
                messageIds.add(resourceValues.get(r).messageId);
            }
            selectQuery.append(") and message_type = 'MM_RESOURCE_STRING'");
            ArrayList<Hashtable<String, String>> messageList = MetrixDatabaseManager.getFieldStringValuesList(selectQuery.toString());
            if (messageList != null) {
                for (int r = 0; r < resourceValues.size(); r++) {
                    String mId = resourceValues.get(r).messageId;
                    for (int m = 0; m < messageList.size(); m++) {
                        String msgId = messageList.get(m).get("message_id");
                        if (msgId.equals(mId)) {
                            setValue(resourceValues.get(r).resourceId, messageList.get(m).get("message_text"), resourceValues.get(r).isHint);
                            break;
                        }
                    }
                }
            } else {
                //Code will reach here when the DB is closed and will try to get backup strings from strings.xml
                //ex: SyncServiceMonitor
                for (int r = 0; r < resourceValues.size(); r++) {
                    setValuefromMessageId(resourceValues.get(r).resourceId, resourceValues.get(r).messageId, resourceValues.get(r).isHint);
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

    }

    public static void setResourceValues(View view, String messageId, boolean isHint) {
        try {
            setValue(view, messageId, isHint);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void setResourceValues(Activity context, View view, String messageId, boolean isHint) {
        try {
            MetrixPublicCache.instance.addItem("StringResourceActivity", context);
            setValue(view, messageId, isHint);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

    }

    public static void setResourceValues(Activity context, View view, String messageId) {
        try {
            MetrixPublicCache.instance.addItem("StringResourceActivity", context);
            setValue(view, messageId, false);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void setResourceValues(View view, String messageId) {
        try {
            setValue(view, messageId, false);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private static void setValue(View view, String messageId, boolean isHint) {
        if (view == null) {
            throw new IllegalArgumentException("The view parameter is required.");
        }

        String messageText = getMessage(messageId);

        if (view instanceof Button) {
            Button button = (Button) view;
            button.setText(messageText);
            return;
        }

        if (view instanceof MetrixHyperlink) {
            MetrixHyperlink hyperlink = (MetrixHyperlink) view;
            hyperlink.setLinkText(getMessage(messageId));
            return;
        }

        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            if (isHint) {
                editText.setHint(messageText);
            } else {
                editText.setText(messageText);
            }
            return;
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (isHint) {
                textView.setHint(messageText);
            } else {
                textView.setText(messageText);
            }
            return;
        }

        if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            checkBox.setText(messageText);
            return;
        }

        if (view instanceof CheckedTextView) {
            CheckedTextView checkedTextView = (CheckedTextView) view;
            checkedTextView.setText(messageText);
            return;
        }

        if (view instanceof AutoCompleteTextView) {
            AutoCompleteTextView acText = (AutoCompleteTextView) view;
            if (isHint) {
                acText.setHint(messageText);
            } else {
                acText.setText(messageText);
            }
            return;
        }


    }

    private static void setValue(int controlId, String messageText, boolean isHint) {

        Activity resActivity = (Activity) MetrixPublicCache.instance.getItem("StringResourceActivity");
        View view = resActivity.findViewById(controlId);
        messageText = processString(messageText);

        if (view == null) {
            return;
        }

        if (view instanceof Button) {
            Button button = (Button) view;
            button.setText(messageText);
            return;
        }

        if (view instanceof MetrixHyperlink) {
            MetrixHyperlink hyperlink = (MetrixHyperlink) view;
            hyperlink.setLinkText(messageText);
            return;
        }

        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            if (isHint) {
                editText.setHint(messageText);
            } else {
                editText.setText(messageText);
            }
            return;
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (isHint) {
                textView.setHint(messageText);
            } else {
                textView.setText(messageText);
            }
            return;
        }

        if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            checkBox.setText(messageText);
            return;
        }

        if (view instanceof CheckedTextView) {
            CheckedTextView checkedTextView = (CheckedTextView) view;
            checkedTextView.setText(messageText);
            return;
        }

        if (view instanceof AutoCompleteTextView) {
            AutoCompleteTextView acText = (AutoCompleteTextView) view;
            if (isHint) {
                acText.setHint(messageText);
            } else {
                acText.setText(messageText);
            }
            return;
        }
    }

    private static void setValuefromMessageId(int controlId, String messageId, boolean isHint) {

        Activity resActivity = (Activity) MetrixPublicCache.instance.getItem("StringResourceActivity");
        View view = resActivity.findViewById(controlId);
        String messageText = getMessage(messageId);

        if (view == null) {
            return;
        }

        if (view instanceof Button) {
            Button button = (Button) view;
            button.setText(messageText);
            return;
        }

        if (view instanceof MetrixHyperlink) {
            MetrixHyperlink hyperlink = (MetrixHyperlink) view;
            hyperlink.setLinkText(messageText);
            return;
        }

        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            if (isHint) {
                editText.setHint(messageText);
            } else {
                editText.setText(messageText);
            }
            return;
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (isHint) {
                textView.setHint(messageText);
            } else {
                textView.setText(messageText);
            }
            return;
        }

        if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            checkBox.setText(messageText);
            return;
        }

        if (view instanceof CheckedTextView) {
            CheckedTextView checkedTextView = (CheckedTextView) view;
            checkedTextView.setText(messageText);
            return;
        }

        if (view instanceof AutoCompleteTextView) {
            AutoCompleteTextView acText = (AutoCompleteTextView) view;
            if (isHint) {
                acText.setHint(messageText);
            } else {
                acText.setText(messageText);
            }
            return;
        }
    }
}
