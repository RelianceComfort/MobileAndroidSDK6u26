package com.metrix.architecture.designer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixFieldManager.WholeNumberKeyListener;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixLookupTableDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.metadata.MetrixUpdateMessage;
import com.metrix.architecture.metadata.MetrixUpdateMessage.MetrixUpdateMessageTransactionType;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

@SuppressLint("UseSparseArrays")
public class MetrixDesignerFieldPropActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
    private HashMap<Integer, HashMap<String, String>> mOriginalData;
    private LinearLayout mTable;
    private TextView mEmphasis;
    private Button mSave, mFinish;
    private String mScreenName, mFieldName, mFieldControlType;
    private AlertDialog mDeleteFieldLookupDialog;
    private MetrixDesignerResourceData mFieldPropResourceData;
    private boolean isNonStandardControl = false;
    private boolean isAttachmentField = false;
    private boolean isSignatureField = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFieldPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldPropActivityResourceData");

        setContentView(mFieldPropResourceData.LayoutResourceID);

        mTable = (LinearLayout) findViewById(mFieldPropResourceData.getExtraResourceID("R.id.table_layout"));

        String currentRevisionStatus = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "status");
        mAllowChanges = (MetrixStringHelper.valueIsEqual(currentRevisionStatus, "PENDING"));
        populateScreen();
    }

    @Override
    public void onStart() {
        super.onStart();

        helpText = mFieldPropResourceData.HelpTextString;

        mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
        mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
        mFieldControlType = MetrixCurrentKeysHelper.getKeyValue("mm_field", "control_type");
        mHeadingText = getIntent().getStringExtra("headingText");
        if (mActionBarTitle != null) {
            mActionBarTitle.setText(mHeadingText);
        }

        mEmphasis = (TextView) findViewById(mFieldPropResourceData.getExtraResourceID("R.id.zzmd_field_prop_emphasis"));
        String fullText;
        if (!MetrixStringHelper.isNullOrEmpty(mFieldControlType) && MetrixStringHelper.valueIsEqual(mFieldControlType.toUpperCase(), "BUTTON"))
            fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldProp", AndroidResourceHelper.getMessage("ScnInfoMxDesFldPropBtn"), mFieldName, mScreenName);
        else
            fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldProp", AndroidResourceHelper.getMessage("ScnInfoMxDesFldPropFld"), mFieldName, mScreenName);
        mEmphasis.setText(fullText);

        mSave = (Button) findViewById(mFieldPropResourceData.getExtraResourceID("R.id.save"));
        mSave.setEnabled(mAllowChanges);
        mSave.setOnClickListener(this);
        AndroidResourceHelper.setResourceValues(mSave, "Save");

        mFinish = (Button) findViewById(mFieldPropResourceData.getExtraResourceID("R.id.finish"));
        mFinish.setOnClickListener(this);
        AndroidResourceHelper.setResourceValues(mFinish, "Finish");

        TextView mTitle = (TextView) findViewById(mFieldPropResourceData.getExtraResourceID("R.id.screenTitle"));
        AndroidResourceHelper.setResourceValues(mTitle, "FieldProperties");

        // if we come back to this screen after doing an add field lookup, make sure Lookup buttons show correctly
        View v = mTable.findViewWithTag("FIELD_LOOKUP_CRUD_BUTTON");
        if (v != null) {
            LinearLayout fieldLookupCrudButtonLayout = (LinearLayout) v;
            Button btnAdd = (Button) fieldLookupCrudButtonLayout.findViewWithTag("add_button");
            Button btnModify = (Button) fieldLookupCrudButtonLayout.findViewWithTag("modify_button");
            Button btnDelete = (Button) fieldLookupCrudButtonLayout.findViewWithTag("delete_button");

            String fieldID = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_id");
            if (fieldHasLookup(fieldID)) {
                btnAdd.setVisibility(View.GONE);
                btnModify.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                btnAdd.setVisibility(View.VISIBLE);
                btnModify.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }
        }
    }

    private void populateScreen() {
        String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
        String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = " + screenID);
        String tabParentID = MetrixDatabaseManager.getFieldStringValue("mm_screen", "tab_parent_id", "screen_id = " + screenID);
        boolean isStandardScreen = (MetrixStringHelper.valueIsEqual(screenType, "STANDARD") || (MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD")));

        boolean isTabChild = (!MetrixStringHelper.isNullOrEmpty(tabParentID));
        final String fieldID = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_id");

        boolean fieldIsCustomAdded = false;
        boolean isHyperlink = false;
        String metrixRowID = "";
        String controlType = "";
        String currencyColumnName = "";
        String dataType = "";
        String defaultValue = "";
        String displayStyle = "";
        String forceCase = "";
        String inputType = "";
        String label = "";
        String listDisplayColumn = "";
        String listFilterColumn = "";
        String listFilterValue = "";
        String listJoinType = "";
        String listOrderBy = "";
        String listTableName = "";
        String listValueColumn = "";
        String maxChars = "";
        String readOnly = "";
        String region = "";
        String relativeWidth = "";
        String required = "";
        String validation = "";
        String valueChangedEvent = "";
        String visible = "";
        String createdRevisionID = "";
        String controlEvent = "";
        String searchable = "";
        String allowFile = "";
        String allowPhoto = "";
        String allowVideo = "";
        String cardScreenID = "";
        String transactionTable = "";
        String transactionColumn = "";
        String allowClear = "";
        String signerColumn = "";
        String signMessage = "";

        StringBuilder query = new StringBuilder();
        query.append("SELECT mm_field.metrix_row_id, mm_field.control_type, mm_field.currency_column_name,");
        query.append(" mm_field.data_type, mm_field.default_value, mm_field.display_style,");
        query.append(" mm_field.force_case, mm_field.input_type, mm_field.label,");
        query.append(" mm_field.list_display_column, mm_field.list_filter_column, mm_field.list_filter_value,");
        query.append(" mm_field.list_join_type, mm_field.list_order_by, mm_field.list_table_name, mm_field.list_value_column,");
        query.append(" mm_field.read_only, mm_field.region, mm_field.relative_width, mm_field.required,");
        query.append(" mm_field.validation, mm_field.value_changed_event, mm_field.visible, mm_field.created_revision_id, mm_field.max_chars,");
        query.append(" mm_field.control_event, mm_field.searchable, mm_field.allow_file, mm_field.allow_photo, mm_field.allow_video,");
        query.append(" mm_field.card_screen_id, mm_field.transaction_table, mm_field.transaction_column,");
        query.append(" mm_field.allow_clear, mm_field.signer_column, mm_field.sign_message_id");
        query.append(" FROM mm_field WHERE mm_field.field_id = " + fieldID);

        mOriginalData = new HashMap<Integer, HashMap<String, String>>();
        MetrixCursor cursor = null;
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            while (cursor.isAfterLast() == false) {
                metrixRowID = cursor.getString(0);
                controlType = cursor.getString(1);
                currencyColumnName = cursor.getString(2);
                dataType = cursor.getString(3);
                defaultValue = cursor.getString(4);
                displayStyle = cursor.getString(5);
                forceCase = cursor.getString(6);
                inputType = cursor.getString(7);
                label = cursor.getString(8);
                listDisplayColumn = cursor.getString(9);
                listFilterColumn = cursor.getString(10);
                listFilterValue = cursor.getString(11);
                listJoinType = cursor.getString(12);
                listOrderBy = cursor.getString(13);
                listTableName = cursor.getString(14);
                listValueColumn = cursor.getString(15);
                readOnly = cursor.getString(16);
                region = cursor.getString(17);
                relativeWidth = cursor.getString(18);
                required = cursor.getString(19);
                validation = cursor.getString(20);
                valueChangedEvent = cursor.getString(21);
                visible = cursor.getString(22);
                createdRevisionID = cursor.getString(23);
                maxChars = cursor.getString(24);
                controlEvent = cursor.getString(25);
                searchable = cursor.getString(26);
                allowFile = cursor.getString(27);
                allowPhoto = cursor.getString(28);
                allowVideo = cursor.getString(29);
                cardScreenID = cursor.getString(30);
                transactionTable = cursor.getString(31);
                transactionColumn = cursor.getString(32);
                allowClear = cursor.getString(33);
                signerColumn = cursor.getString(34);
                signMessage = cursor.getString(35);

                if (MetrixStringHelper.isNullOrEmpty(controlType)) controlType = "";
                if (MetrixStringHelper.isNullOrEmpty(currencyColumnName)) currencyColumnName = "";
                if (MetrixStringHelper.isNullOrEmpty(dataType)) dataType = "";
                if (MetrixStringHelper.isNullOrEmpty(defaultValue)) defaultValue = "";
                if (MetrixStringHelper.isNullOrEmpty(displayStyle)) displayStyle = "";
                if (MetrixStringHelper.isNullOrEmpty(forceCase)) forceCase = "";
                if (MetrixStringHelper.isNullOrEmpty(inputType)) inputType = "";
                if (MetrixStringHelper.isNullOrEmpty(label)) label = "";
                if (MetrixStringHelper.isNullOrEmpty(listDisplayColumn)) listDisplayColumn = "";
                if (MetrixStringHelper.isNullOrEmpty(listFilterColumn)) listFilterColumn = "";
                if (MetrixStringHelper.isNullOrEmpty(listFilterValue)) listFilterValue = "";
                if (MetrixStringHelper.isNullOrEmpty(listJoinType)) listJoinType = "";
                if (MetrixStringHelper.isNullOrEmpty(listOrderBy)) listOrderBy = "";
                if (MetrixStringHelper.isNullOrEmpty(listTableName)) listTableName = "";
                if (MetrixStringHelper.isNullOrEmpty(listValueColumn)) listValueColumn = "";
                if (MetrixStringHelper.isNullOrEmpty(readOnly)) readOnly = "";
                if (MetrixStringHelper.isNullOrEmpty(region)) region = "";
                if (MetrixStringHelper.isNullOrEmpty(relativeWidth)) relativeWidth = "";
                if (MetrixStringHelper.isNullOrEmpty(required)) required = "";
                if (MetrixStringHelper.isNullOrEmpty(validation)) validation = "";
                if (MetrixStringHelper.isNullOrEmpty(valueChangedEvent)) valueChangedEvent = "";
                if (MetrixStringHelper.isNullOrEmpty(visible)) visible = "";
                if (MetrixStringHelper.isNullOrEmpty(maxChars)) maxChars = "";
                if (MetrixStringHelper.isNullOrEmpty(controlEvent)) controlEvent = "";
                if (MetrixStringHelper.isNullOrEmpty(searchable)) searchable = "";
                if (MetrixStringHelper.isNullOrEmpty(allowFile)) allowFile = "";
                if (MetrixStringHelper.isNullOrEmpty(allowPhoto)) allowPhoto = "";
                if (MetrixStringHelper.isNullOrEmpty(allowVideo)) allowVideo = "";
                if (MetrixStringHelper.isNullOrEmpty(transactionTable)) transactionTable = "";
                if (MetrixStringHelper.isNullOrEmpty(transactionColumn)) transactionColumn = "";
                if(MetrixStringHelper.isNullOrEmpty(allowClear)) allowClear = "";
                if(MetrixStringHelper.isNullOrEmpty(signerColumn)) signerColumn = "";
                if(MetrixStringHelper.isNullOrEmpty(signMessage)) signMessage = "";

                String separator = MetrixFloatHelper.getDecimalSeparator();
                if (MetrixStringHelper.valueIsEqual(relativeWidth, "1")
                        || MetrixStringHelper.valueIsEqual(relativeWidth, "1,0")
                        || MetrixStringHelper.valueIsEqual(relativeWidth, "1.0"))
                    relativeWidth = String.format("1%s0", separator);
                else
                    relativeWidth = MetrixFloatHelper.convertNumericFromDBToUI(relativeWidth);

                if (!MetrixStringHelper.isNullOrEmpty(createdRevisionID))
                    fieldIsCustomAdded = true;

                isHyperlink = (MetrixStringHelper.valueIsEqual(controlType, "HYPERLINK") || MetrixStringHelper.valueIsEqual(controlType, "LONGHYPERLINK")) ? true : false;
                isNonStandardControl = MetrixControlAssistant.isNonStandardControl(controlType);
                isAttachmentField = MetrixStringHelper.valueIsEqual(controlType, "ATTACHMENT");
                isSignatureField = MetrixStringHelper.valueIsEqual(controlType, "SIGNATURE");

                int thisFieldIDNum = Integer.valueOf(fieldID);

                // populate mOriginalData with a row
                HashMap<String, String> origRow = new HashMap<String, String>();
                origRow.put("mm_field.metrix_row_id", metrixRowID);
                origRow.put("mm_field.field_id", fieldID);
                origRow.put("mm_field.allow_file", allowFile);
                origRow.put("mm_field.allow_photo", allowPhoto);
                origRow.put("mm_field.allow_video", allowVideo);
                origRow.put("mm_field.card_screen_id", cardScreenID);
                origRow.put("mm_field.control_event", controlEvent);
                origRow.put("mm_field.control_type", controlType);
                origRow.put("mm_field.currency_column_name", currencyColumnName);
                origRow.put("mm_field.data_type", dataType);
                origRow.put("mm_field.default_value", defaultValue);
                origRow.put("mm_field.display_style", displayStyle);
                origRow.put("mm_field.force_case", forceCase);
                origRow.put("mm_field.hyperlink", (!isStandardScreen && isHyperlink) ? "Y" : "N");
                origRow.put("mm_field.input_type", inputType);
                origRow.put("mm_field.label", label);
                origRow.put("mm_field.list_display_column", listDisplayColumn);
                origRow.put("mm_field.list_filter_column", listFilterColumn);
                origRow.put("mm_field.list_filter_value", listFilterValue);
                origRow.put("mm_field.list_join_type", listJoinType);
                origRow.put("mm_field.list_order_by", listOrderBy);
                origRow.put("mm_field.list_table_name", listTableName);
                origRow.put("mm_field.list_value_column", listValueColumn);
                origRow.put("mm_field.max_chars", maxChars);
                origRow.put("mm_field.read_only", readOnly);
                origRow.put("mm_field.region", region);
                origRow.put("mm_field.relative_width", relativeWidth);
                origRow.put("mm_field.required", required);
                origRow.put("mm_field.searchable", searchable);
                origRow.put("mm_field.transaction_column", transactionColumn);
                origRow.put("mm_field.transaction_table", transactionTable);
                origRow.put("mm_field.allow_clear", allowClear);
                origRow.put("mm_field.signer_column", signerColumn);
                origRow.put("mm_field.sign_message_id", signMessage);
                origRow.put("mm_field.validation", validation);
                origRow.put("mm_field.value_changed_event", valueChangedEvent);
                origRow.put("mm_field.visible", visible);
                mOriginalData.put(thisFieldIDNum, origRow);

                break;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // generate an ordered map, so that properties appear in translated alphabetical order
        // iterate through map and dynamically render layouts accordingly
        StringBuilder msgQuery = new StringBuilder();
        msgQuery.append("SELECT c.code_value, v.message_text FROM metrix_code_table c");
        msgQuery.append(" JOIN mm_message_def_view v ON v.message_id = c.message_id AND v.message_type = 'CODE'");
        if (isStandardScreen) {
            if (isNonStandardControl)
                msgQuery.append(" WHERE c.code_name = 'MM_FIELD_BUTTON_PROP_NAME' AND c.code_value NOT IN ('CUSTOM_ADDED','ORDER')");
            else if (isAttachmentField)
                msgQuery.append(" WHERE c.code_name = 'MM_FIELD_ATTACHMENT_PROP_NAME'");
            else if(isSignatureField)
                msgQuery.append(" WHERE c.code_name = 'MM_FIELD_SIGNATURE_PROP_NAME'");
            else
                msgQuery.append(" WHERE c.code_name = 'MM_FIELD_PROP_NAME' AND c.code_value NOT IN ('CUSTOM_ADDED','ORDER')");
        } else
            msgQuery.append(" WHERE c.code_name = 'MM_FIELD_LIST_PROP_NAME'");
        msgQuery.append(" ORDER BY v.message_text ASC");

        MetrixCursor msgCursor = null;
        try {
            msgCursor = MetrixDatabaseManager.rawQueryMC(msgQuery.toString(), null);

            if (msgCursor == null || !msgCursor.moveToFirst()) {
                return;
            }

            if (MetrixStringHelper.isNullOrEmpty(mFieldName)) {
                mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
            }
            String tableName = mFieldName.substring(0, mFieldName.lastIndexOf("."));
            String columnName = mFieldName.substring(mFieldName.lastIndexOf(".") + 1);

            while (msgCursor.isAfterLast() == false) {
                String propName = msgCursor.getString(0);
                String propNameString = msgCursor.getString(1);

                // using property_name, use hard-coded map to determine what layout to inflate and populate with data
                // CHECKBOX		ALLOW_FILE, ALLOW_PHOTO, ALLOW_VIDEO, READ_ONLY, REQUIRED, SEARCHABLE (non-CUSTOM tables only), VISIBLE
                // COMBOBOX		CARD_SCREEN_ID, CONTROL_TYPE (custom standard fields only),
                //				CURRENCY_COLUMN_NAME (non-CUSTOM tables only),
                //				DATA_TYPE, DISPLAY_STYLE, FORCE_CASE, INPUT_TYPE, LIST_JOIN_TYPE, RELATIVE_WIDTH
                // LONGTEXT		VALIDATION
                // LABEL		CONTROL_TYPE (baseline standard fields and Attachment Fields only)
                // MESSAGE LOOKUP			LABEL
                // CLIENT SCRIPT LOOKUP		VALUE_CHANGED_EVENT
                // CRUD BUTTON				LOOKUP (if STANDARD screen_type, CONTROL_TYPE is TEXT, AND READ_ONLY not true)
                // TEXTBOX		MAX_CHARS (numeric) ... (default, all others)

                if (MetrixStringHelper.valueIsEqual(tableName, "CUSTOM")
                        && (MetrixStringHelper.valueIsEqual(propName, "CURRENCY_COLUMN_NAME") || MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE"))) {
                    // don't try to render a currency spinner or a searchable checkbox for the CUSTOM table
                    msgCursor.moveToNext();
                    continue;
                }

                // hyperlink property for list fields (skip for Attachment API List screens and baseline TEXT fields)
                if (MetrixStringHelper.valueIsEqual(propName, "HYPERLINK") && (!fieldIsCustomAdded || MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_LIST"))) {
                    if (MetrixStringHelper.valueIsEqual(controlType, "TEXT")) {
                        msgCursor.moveToNext();
                        continue;
                    }
                }

                LinearLayout layout = null;

                // hyperlink property for list fields
                if (MetrixStringHelper.valueIsEqual(propName, "HYPERLINK")) {
                    // CHECKBOX
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
                    CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
                    chkPropValue.setChecked((MetrixStringHelper.valueIsEqual(controlType, "HYPERLINK")) ? true : false);
                    chkPropValue.setEnabled(fieldIsCustomAdded ? true : false);

                    chkPropValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            try {
                                View controlEventView = mTable.findViewWithTag("CONTROL_EVENT");
                                if (controlEventView != null) {
                                    if (isChecked)
                                        controlEventView.setVisibility(View.VISIBLE);
                                    else {
                                        EditText controlEventPropValue = (EditText) controlEventView.findViewWithTag("property_value");
                                        if (controlEventPropValue != null)
                                            controlEventPropValue.setText("");
                                        TextView controlEventDescLabel = (TextView) controlEventView.findViewWithTag("description_label");
                                        if (controlEventDescLabel != null)
                                            controlEventDescLabel.setText("");
                                        TextView controlEventDescText = (TextView) controlEventView.findViewWithTag("description_text");
                                        if (controlEventDescText != null)
                                            controlEventDescText.setText("");

                                        controlEventView.setVisibility(View.GONE);
                                    }
                                }
                            } catch (Exception ex) {
                                LogManager.getInstance().error(ex);
                            }
                        }
                    });
                } else if (MetrixStringHelper.valueIsEqual(propName, "VALIDATION")) {
                    // LONGTEXT
                    layout = MetrixControlAssistant.addLinearLayoutWithInnerLinearLayouts(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_longedittext_line"), mTable);
                    EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
                    etPropValue.setText(validation);
                } else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_FILE") || MetrixStringHelper.valueIsEqual(propName, "ALLOW_PHOTO")
                        || MetrixStringHelper.valueIsEqual(propName, "ALLOW_VIDEO") || MetrixStringHelper.valueIsEqual(propName, "READ_ONLY")
                        || MetrixStringHelper.valueIsEqual(propName, "REQUIRED") || MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE")
                        || MetrixStringHelper.valueIsEqual(propName, "VISIBLE") || MetrixStringHelper.valueIsEqual(propName, "ALLOW_CLEAR")) {
                    // CHECKBOX
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
                    CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
                    if ((MetrixStringHelper.valueIsEqual(propName, "ALLOW_FILE") && allowFile.compareToIgnoreCase("Y") == 0)
                            || (MetrixStringHelper.valueIsEqual(propName, "ALLOW_PHOTO") && allowPhoto.compareToIgnoreCase("Y") == 0)
                            || (MetrixStringHelper.valueIsEqual(propName, "ALLOW_VIDEO") && allowVideo.compareToIgnoreCase("Y") == 0)
                            || (MetrixStringHelper.valueIsEqual(propName, "READ_ONLY") && readOnly.compareToIgnoreCase("Y") == 0)
                            || (MetrixStringHelper.valueIsEqual(propName, "REQUIRED") && required.compareToIgnoreCase("Y") == 0)
                            || (MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE") && searchable.compareToIgnoreCase("Y") == 0)
                            || (MetrixStringHelper.valueIsEqual(propName, "VISIBLE") && visible.compareToIgnoreCase("Y") == 0)
                            || (MetrixStringHelper.valueIsEqual(propName, "ALLOW_CLEAR") && allowClear.compareToIgnoreCase("Y") == 0))
                        chkPropValue.setChecked(true);
                    else
                        chkPropValue.setChecked(false);

                    layout.setTag(propName);
                    if (MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE") && MetrixStringHelper.valueIsEqual(visible, "Y")) {
                        // if this list screen field starts out as Visible, hide the Searchable property
                        layout.setVisibility(View.GONE);
                    } else if (MetrixStringHelper.valueIsEqual(propName, "VISIBLE") && !isStandardScreen && !MetrixStringHelper.valueIsEqual(tableName, "CUSTOM")) {
                        // set up a check change event on Visible relating to Searchable (non-custom list screen fields only)
                        chkPropValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                try {
                                    View v = mTable.findViewWithTag("SEARCHABLE");
                                    if (v != null) {
                                        LinearLayout searchableLayout = (LinearLayout) v;
                                        CheckBox chkSearchable = (CheckBox) v.findViewWithTag("property_value");
                                        if (isChecked) {
                                            // Field is now visible, so hide Searchable and check it by default
                                            chkSearchable.setChecked(true);
                                            searchableLayout.setVisibility(View.GONE);
                                        } else {
                                            // Field is now invisible, so show Searchable and uncheck it by default
                                            chkSearchable.setChecked(false);
                                            searchableLayout.setVisibility(View.VISIBLE);
                                        }
                                    }
                                } catch (Exception ex) {
                                    LogManager.getInstance().error(ex);
                                }
                            }
                        });
                    } else if (MetrixStringHelper.valueIsEqual(propName, "READ_ONLY")) {
                        // if this is a tab child screen or this is a hyperlink field, disable the read-only checkbox ... these fields are not editable
                        if (isTabChild || isHyperlink)
                            chkPropValue.setEnabled(false);

                        if (isHyperlink)
                            chkPropValue.setChecked(true);

                        // if we change read_only, determine whether LOOKUP property should show
                        chkPropValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                try {
                                    View v = mTable.findViewWithTag("FIELD_LOOKUP_CRUD_BUTTON");
                                    if (v != null) {
                                        LinearLayout fieldLookupCrudButtonLayout = (LinearLayout) v;
                                        if (isChecked)
                                            fieldLookupCrudButtonLayout.setVisibility(View.GONE);
                                        else {
                                            // confirm that CONTROL_TYPE is still Text before making visible
                                            boolean shouldMakeVisible = true;
                                            View w = mTable.findViewWithTag("CONTROL_TYPE");
                                            if (w != null) {
                                                Spinner spnControlType = (Spinner) w.findViewWithTag("property_value");
                                                String currValue = MetrixControlAssistant.getValue(spnControlType);
                                                if (!MetrixStringHelper.valueIsEqual(currValue, "TEXT"))
                                                    shouldMakeVisible = false;
                                            } else {
                                                View testView = mTable.findViewWithTag("CONTROL_TYPE_LABEL_TEXT");
                                                if (testView == null)
                                                    shouldMakeVisible = false;
                                            }

                                            if (shouldMakeVisible)
                                                fieldLookupCrudButtonLayout.setVisibility(View.VISIBLE);
                                        }
                                    }
                                } catch (Exception ex) {
                                    LogManager.getInstance().error(ex);
                                }
                            }
                        });
                    }
                } else if ((MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE") && fieldIsCustomAdded && !isNonStandardControl && !isAttachmentField && !isSignatureField)
                        || MetrixStringHelper.valueIsEqual(propName, "CARD_SCREEN_ID")
                        || MetrixStringHelper.valueIsEqual(propName, "CURRENCY_COLUMN_NAME")
                        || MetrixStringHelper.valueIsEqual(propName, "DATA_TYPE")
                        || MetrixStringHelper.valueIsEqual(propName, "DISPLAY_STYLE")
                        || MetrixStringHelper.valueIsEqual(propName, "FORCE_CASE")
                        || MetrixStringHelper.valueIsEqual(propName, "INPUT_TYPE")
                        || MetrixStringHelper.valueIsEqual(propName, "LIST_JOIN_TYPE")
                        || MetrixStringHelper.valueIsEqual(propName, "RELATIVE_WIDTH")) {
                    // COMBOBOX
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
                    Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");

                    if (MetrixStringHelper.valueIsEqual(propName, "RELATIVE_WIDTH")) {
                        ArrayList<SpinnerKeyValuePair> numberSet = new ArrayList<SpinnerKeyValuePair>();
                        numberSet.add(new SpinnerKeyValuePair("", ""));
                        String separator = MetrixFloatHelper.getDecimalSeparator();
                        for (int i = 1; i < 11; i++) {
                            String stringToUse = i < 10 ? String.format("0%1$s%2$s", separator, String.valueOf(i)) : String.format("1%s0", separator);
                            SpinnerKeyValuePair item = new SpinnerKeyValuePair(stringToUse, stringToUse);
                            numberSet.add(item);
                        }
                        MetrixControlAssistant.populateSpinnerFromPair(this, spnPropValue, numberSet);
                    } else if (MetrixStringHelper.valueIsEqual(propName, "CARD_SCREEN_ID")) {
                        StringBuilder screenIdQuery = new StringBuilder();
                        screenIdQuery.append("select mm_screen.screen_name, mm_screen.screen_id from mm_screen");
                        screenIdQuery.append(" where screen_type = 'ATTACHMENT_API_CARD'");
                        screenIdQuery.append(" and revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
                        screenIdQuery.append(" order by mm_screen.screen_name asc");
                        MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, screenIdQuery.toString(), true);
                    } else if (MetrixStringHelper.valueIsEqual(propName, "CURRENCY_COLUMN_NAME")) {
                        ArrayList<String> currencyColumnList = getColumnNames(tableName);
                        currencyColumnList.remove(columnName);
                        ArrayList<SpinnerKeyValuePair> currencyColumnSet = new ArrayList<SpinnerKeyValuePair>();
                        currencyColumnSet.add(new SpinnerKeyValuePair("", ""));
                        for (int i = 0; i < currencyColumnList.size(); i++) {
                            String columnString = currencyColumnList.get(i);
                            SpinnerKeyValuePair item = new SpinnerKeyValuePair(columnString, columnString);
                            currencyColumnSet.add(item);
                        }
                        MetrixControlAssistant.populateSpinnerFromPair(this, spnPropValue, currencyColumnSet);
                    } else {
                        StringBuilder spinnerQuery = new StringBuilder();
                        spinnerQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
                        spinnerQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
                        spinnerQuery.append(String.format("where metrix_code_table.code_name = 'MM_FIELD_%s' ", propName));
                        if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE") && isAttachmentField)
                            spinnerQuery.append(String.format("and metrix_code_table.code_value <> '%1$s' and metrix_code_table.code_value <> '%2$s' ", "ATTACHMENT", "BUTTON"));
                        else if(MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE") && isSignatureField)
                            spinnerQuery.append(String.format("and metrix_code_table.code_value <> '%1$s' and metrix_code_table.code_value <> '%2$s' ", "SIGNATURE", "BUTTON"));
                        spinnerQuery.append("order by mm_message_def_view.message_text asc");
                        MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, spinnerQuery.toString(), true);
                    }

                    try {
                        if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE")) {
                            layout.setTag("CONTROL_TYPE");
                            MetrixControlAssistant.setValue(spnPropValue, controlType);

                            // if we change control_type, determine whether LOOKUP property should show
                            spnPropValue.setOnItemSelectedListener(new OnItemSelectedListener() {
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    try {
                                        SpinnerKeyValuePair kvp = (SpinnerKeyValuePair) parent.getItemAtPosition(position);
                                        String currValue = kvp.spinnerValue;

                                        CheckBox chkReadOnly = null;
                                        View readOnlyView = mTable.findViewWithTag("READ_ONLY");
                                        if (readOnlyView != null)
                                            chkReadOnly = (CheckBox) readOnlyView.findViewWithTag("property_value");

                                        View v = mTable.findViewWithTag("FIELD_LOOKUP_CRUD_BUTTON");
                                        if (v != null) {
                                            LinearLayout fieldLookupCrudButtonLayout = (LinearLayout) v;
                                            if (MetrixStringHelper.valueIsEqual(currValue, "TEXT")) {
                                                // confirm that READ_ONLY is still N before making visible
                                                boolean shouldMakeVisible = true;
                                                if (chkReadOnly != null && chkReadOnly.isChecked())
                                                    shouldMakeVisible = false;

                                                if (shouldMakeVisible)
                                                    fieldLookupCrudButtonLayout.setVisibility(View.VISIBLE);
                                            } else
                                                fieldLookupCrudButtonLayout.setVisibility(View.GONE);
                                        }

                                        View controlEventView = mTable.findViewWithTag("CONTROL_EVENT");
                                        if (MetrixStringHelper.valueIsEqual(currValue, "HYPERLINK") || MetrixStringHelper.valueIsEqual(currValue, "LONGHYPERLINK")) {
                                            if (chkReadOnly != null) {
                                                chkReadOnly.setChecked(true);
                                                chkReadOnly.setEnabled(false);
                                            }

                                            if (controlEventView != null)
                                                controlEventView.setVisibility(View.VISIBLE);
                                        } else {
                                            if (chkReadOnly != null) {
                                                chkReadOnly.setChecked(false);
                                                chkReadOnly.setEnabled(true);
                                            }

                                            if (controlEventView != null) {
                                                EditText controlEventPropValue = (EditText) controlEventView.findViewWithTag("property_value");
                                                if (controlEventPropValue != null)
                                                    controlEventPropValue.setText("");
                                                TextView controlEventDescLabel = (TextView) controlEventView.findViewWithTag("description_label");
                                                if (controlEventDescLabel != null)
                                                    controlEventDescLabel.setText("");
                                                TextView controlEventDescText = (TextView) controlEventView.findViewWithTag("description_text");
                                                if (controlEventDescText != null)
                                                    controlEventDescText.setText("");

                                                controlEventView.setVisibility(View.GONE);
                                            }
                                        }

                                    } catch (Exception ex) {
                                        LogManager.getInstance().error(ex);
                                    }
                                }

                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });
                        } else if (MetrixStringHelper.valueIsEqual(propName, "CARD_SCREEN_ID"))
                            MetrixControlAssistant.setValue(spnPropValue, cardScreenID);
                        else if (MetrixStringHelper.valueIsEqual(propName, "CURRENCY_COLUMN_NAME"))
                            MetrixControlAssistant.setValue(spnPropValue, currencyColumnName);
                        else if (MetrixStringHelper.valueIsEqual(propName, "DATA_TYPE"))
                            MetrixControlAssistant.setValue(spnPropValue, dataType);
                        else if (MetrixStringHelper.valueIsEqual(propName, "DISPLAY_STYLE"))
                            MetrixControlAssistant.setValue(spnPropValue, displayStyle);
                        else if (MetrixStringHelper.valueIsEqual(propName, "FORCE_CASE"))
                            MetrixControlAssistant.setValue(spnPropValue, forceCase);
                        else if (MetrixStringHelper.valueIsEqual(propName, "INPUT_TYPE"))
                            MetrixControlAssistant.setValue(spnPropValue, inputType);
                        else if (MetrixStringHelper.valueIsEqual(propName, "LIST_JOIN_TYPE"))
                            MetrixControlAssistant.setValue(spnPropValue, listJoinType);
                        else if (MetrixStringHelper.valueIsEqual(propName, "RELATIVE_WIDTH"))
                            MetrixControlAssistant.setValue(spnPropValue, relativeWidth);
                    } catch (Exception e) {
                        LogManager.getInstance(this).error(e);
                    }
                } else if (MetrixStringHelper.valueIsEqual(propName, "VALUE_CHANGED_EVENT") || MetrixStringHelper.valueIsEqual(propName, "CONTROL_EVENT")) {
                    // CLIENT SCRIPT LOOKUP
                    String propValue = "";
                    if (MetrixStringHelper.valueIsEqual(propName, "VALUE_CHANGED_EVENT"))
                        propValue = valueChangedEvent;
                    else if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_EVENT"))
                        propValue = controlEvent;

                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
                    if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_EVENT"))
                        layout.setTag("CONTROL_EVENT");
                    EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
                    TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
                    TextView tvDescText = (TextView) layout.findViewWithTag("description_text");
                    etPropValue.setId(MetrixControlAssistant.generateViewId());
                    etPropValue.setText(propValue);
                    etPropValue.setOnFocusChangeListener(this);
                    etPropValue.addTextChangedListener(new ClientScriptTextWatcher(tvDescLabel, tvDescText));

                    tvDescLabel.setVisibility(View.GONE);
                    tvDescText.setVisibility(View.GONE);
                    if (!MetrixStringHelper.isNullOrEmpty(propValue)) {
                        String description = "";
                        String scriptName = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "name", String.format("unique_vs = '%s'", propValue));
                        String scriptVersion = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "version_number", String.format("unique_vs = '%s'", propValue));
                        if (!MetrixStringHelper.isNullOrEmpty(scriptName) && !MetrixStringHelper.isNullOrEmpty(scriptVersion)) {
                            if (MetrixStringHelper.valueIsEqual(scriptVersion, "0"))
                                description = String.format("%1$s (%2$s)", scriptName, AndroidResourceHelper.getMessage("Baseline"));
                            else
                                description = String.format("%1$s (%2$s %3$s)", scriptName, AndroidResourceHelper.getMessage("Version"), scriptVersion);

                            tvDescText.setText(description);
                            tvDescLabel.setVisibility(View.INVISIBLE);
                            tvDescText.setVisibility(View.VISIBLE);
                        }
                    }

                    if (!isNonStandardControl && !isHyperlink) {
                        if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_EVENT")) {
                            View controlEventView = mTable.findViewWithTag("CONTROL_EVENT");
                            if (controlEventView != null)
                                controlEventView.setVisibility(View.GONE);
                        }
                    }

                } else if (MetrixStringHelper.valueIsEqual(propName, "LOOKUP")) {
                    if (isStandardScreen) {
                        // CRUD BUTTON
                        layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_crud_button_line"), mTable);
                        layout.setTag("FIELD_LOOKUP_CRUD_BUTTON");
                        Button btnAdd = (Button) layout.findViewWithTag("add_button");
                        Button btnModify = (Button) layout.findViewWithTag("modify_button");
                        Button btnDelete = (Button) layout.findViewWithTag("delete_button");

                        AndroidResourceHelper.setResourceValues(btnAdd, "Add");
                        AndroidResourceHelper.setResourceValues(btnModify, "Modify");
                        AndroidResourceHelper.setResourceValues(btnDelete, "Delete");

                        btnAdd.setEnabled(mAllowChanges);
                        btnDelete.setEnabled(mAllowChanges);

                        btnAdd.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldPropActivity.this, MetrixDesignerFieldLookupAddActivity.class);
                                intent.putExtra("headingText", mHeadingText);
                                MetrixActivityHelper.startNewActivity(MetrixDesignerFieldPropActivity.this, intent);
                            }
                        });

                        btnModify.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                String lkupID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup", "lkup_id", "field_id = " + fieldID);
                                MetrixCurrentKeysHelper.setKeyValue("mm_field_lkup", "lkup_id", lkupID);
                                Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldPropActivity.this, MetrixDesignerFieldLookupPropActivity.class);
                                intent.putExtra("headingText", mHeadingText);
                                MetrixActivityHelper.startNewActivity(MetrixDesignerFieldPropActivity.this, intent);
                            }
                        });

                        btnDelete.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                mDeleteFieldLookupDialog = new AlertDialog.Builder(MetrixDesignerFieldPropActivity.this).create();
                                mDeleteFieldLookupDialog.setMessage(AndroidResourceHelper.getMessage("FieldLookupDeleteConfirm"));
                                mDeleteFieldLookupDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteFieldLookupListener);
                                mDeleteFieldLookupDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteFieldLookupListener);
                                mDeleteFieldLookupDialog.show();
                            }
                        });

                        if (fieldHasLookup(fieldID)) {
                            btnAdd.setVisibility(View.GONE);
                            btnModify.setVisibility(View.VISIBLE);
                            btnDelete.setVisibility(View.VISIBLE);
                        } else {
                            btnAdd.setVisibility(View.VISIBLE);
                            btnModify.setVisibility(View.GONE);
                            btnDelete.setVisibility(View.GONE);
                        }

                        if (MetrixStringHelper.valueIsEqual(controlType, "TEXT") && !MetrixStringHelper.valueIsEqual(readOnly, "Y"))
                            layout.setVisibility(View.VISIBLE);
                        else
                            layout.setVisibility(View.GONE);
                    }
                } else if (MetrixStringHelper.valueIsEqual(propName, "LABEL")) {
                    // MESSAGE LOOKUP
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
                    String messageType = propName;
                    EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
                    TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
                    TextView tvDescText = (TextView) layout.findViewWithTag("description_text");
                    etPropValue.setId(MetrixControlAssistant.generateViewId());
                    etPropValue.setText(label);
                    etPropValue.setOnFocusChangeListener(this);
                    etPropValue.addTextChangedListener(new MessageTextWatcher(messageType, tvDescLabel, tvDescText));

                    tvDescLabel.setVisibility(View.GONE);
                    tvDescText.setVisibility(View.GONE);
                    if (!MetrixStringHelper.isNullOrEmpty(label)) {
                        String description = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", String.format("message_type = '%1$s' and message_id = '%2$s'", messageType, label));
                        if (!MetrixStringHelper.isNullOrEmpty(description)) {
                            tvDescText.setText(description);
                            tvDescLabel.setVisibility(View.INVISIBLE);
                            tvDescText.setVisibility(View.VISIBLE);
                        }
                    }
                } else if ((MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE") && !fieldIsCustomAdded)
                        || (MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE") && isNonStandardControl)
                        || (MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE") && isAttachmentField)
                        ||  (MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE") && isSignatureField)) {
                    // LABEL (with translated text, if possible)
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
                    layout.setTag("CONTROL_TYPE_LABEL_" + controlType);
                    TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");

                    StringBuilder translationQuery = new StringBuilder();
                    translationQuery.append("select mm_message_def_view.message_text from metrix_code_table ");
                    translationQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
                    translationQuery.append(String.format("where metrix_code_table.code_name = 'MM_FIELD_%1$s' and metrix_code_table.code_value = '%2$s'", propName, controlType));
                    ArrayList<Hashtable<String, String>> result = MetrixDatabaseManager.getFieldStringValuesList(translationQuery.toString());
                    if (result != null && result.size() > 0 && result.get(0).values().size() > 0) {
                        String translatedValue = (String) result.get(0).values().toArray()[0];
                        tvPropValue.setText(translatedValue);
                    } else {
                        tvPropValue.setText(controlType);
                    }
                } else if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT_VALUE")) {
                    //Selecting of predefined default value options
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_imageview_line"), mTable);
                    ImageView imgBtnDefaultValueLookup = (ImageView) layout.findViewWithTag("lookup_button");

                    EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
                    etPropValue.setId(MetrixControlAssistant.generateViewId());
                    etPropValue.setText(defaultValue);
                    TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
                    TextView tvDescText = (TextView) layout.findViewWithTag("description_text");

                    TextView tvPropDataType = (TextView) layout.findViewWithTag("default_value_data_type");
                    tvPropDataType.setId(MetrixControlAssistant.generateViewId());

                    //Search button click event
                    imgBtnDefaultValueLookup.setOnClickListener(this);
                    //Text change event
                    etPropValue.addTextChangedListener(new MessageTextWatcher("MM_DEFAULT_VALUE", tvDescLabel, tvDescText, tvPropDataType));

                    tvDescLabel.setVisibility(View.GONE);
                    tvDescText.setVisibility(View.GONE);
                    if (!MetrixStringHelper.isNullOrEmpty(defaultValue)) {
                        String description = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", String.format("message_type = '%1$s' and message_id = '%2$s'", "MM_DEFAULT_VALUE", defaultValue));
                        if (!MetrixStringHelper.isNullOrEmpty(description)) {
                            tvDescText.setText(description);
                            tvDescLabel.setVisibility(View.INVISIBLE);
                            tvDescText.setVisibility(View.VISIBLE);
                        }
                    }

                    Hashtable<String, String> result = MetrixDatabaseManager.getFieldStringValues("metrix_code_table", new String[]{"comments"}, String.format("code_name = '%1$s' and message_id = '%2$s'", "MM_DEFAULT_VALUE", defaultValue));
                    if (result != null && result.size() > 0) {
                        if (tvPropDataType != null)
                            tvPropDataType.setText(result.get("comments"));
                    } else {
                        if (tvPropDataType != null)
                            tvPropDataType.setText(null);
                    }
                } else if(MetrixStringHelper.valueIsEqual(propName, "SIGN_MESSAGE_ID")) {
                    //Selecting of predefined default value options
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_imageview_line"), mTable);
                    ImageView imgBtnDefaultValueLookup = (ImageView) layout.findViewWithTag("lookup_button");

                    EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
                    etPropValue.setId(MetrixControlAssistant.generateViewId());
                    etPropValue.setText(signMessage);
                    TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
                    TextView tvDescText = (TextView) layout.findViewWithTag("description_text");

                    //Search button click event
                    imgBtnDefaultValueLookup.setOnClickListener(this);
                    //Text change event
                    etPropValue.addTextChangedListener(new MessageTextWatcher("MM_RESOURCE_STRING", tvDescLabel, tvDescText));

                    tvDescLabel.setVisibility(View.GONE);
                    tvDescText.setVisibility(View.GONE);
                    if (!MetrixStringHelper.isNullOrEmpty(signMessage)) {
                        String description = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", String.format("message_type = '%1$s' and message_id = '%2$s'", "MM_RESOURCE_STRING", signMessage));
                        if (!MetrixStringHelper.isNullOrEmpty(description)) {
                            tvDescText.setText(description);
                            tvDescLabel.setVisibility(View.INVISIBLE);
                            tvDescText.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    // TEXTBOX
                    layout = MetrixControlAssistant.addLinearLayout(this, mFieldPropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_line"), mTable);
                    EditText etPropValue = (EditText) layout.findViewWithTag("property_value");

                    if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT_VALUE"))
                        etPropValue.setText(defaultValue);
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_DISPLAY_COLUMN"))
                        etPropValue.setText(listDisplayColumn);
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_FILTER_COLUMN"))
                        etPropValue.setText(listFilterColumn);
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_FILTER_VALUE"))
                        etPropValue.setText(listFilterValue);
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_ORDER_BY"))
                        etPropValue.setText(listOrderBy);
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_TABLE_NAME"))
                        etPropValue.setText(listTableName);
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_VALUE_COLUMN"))
                        etPropValue.setText(listValueColumn);
                    else if (MetrixStringHelper.valueIsEqual(propName, "REGION"))
                        etPropValue.setText(region);
                    else if (MetrixStringHelper.valueIsEqual(propName, "TRANSACTION_COLUMN"))
                        etPropValue.setText(transactionColumn);
                    else if (MetrixStringHelper.valueIsEqual(propName, "TRANSACTION_TABLE"))
                        etPropValue.setText(transactionTable);
                    else if (MetrixStringHelper.valueIsEqual(propName, "SIGNER_COLUMN"))
                        etPropValue.setText(signerColumn);
                    else if (MetrixStringHelper.valueIsEqual(propName, "MAX_CHARS")) {
                        etPropValue.setText(maxChars);
                        MetrixControlAssistant.addInputType(etPropValue.getId(), layout, InputType.TYPE_CLASS_NUMBER);
                        MetrixFieldManager fieldManager = new MetrixFieldManager();
                        WholeNumberKeyListener mWholeNumberKeyListener = fieldManager.new WholeNumberKeyListener();
                        etPropValue.setKeyListener(mWholeNumberKeyListener);
                    }
                }

                // all layouts have these, so set generically
                if (layout != null) {
                    TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
                    TextView tvFieldID = (TextView) layout.findViewWithTag("pv_id");
                    TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
                    TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

                    tvMetrixRowID.setText(metrixRowID);
                    tvFieldID.setText(fieldID);
                    tvPropName.setText(propName);
                    tvPropNameString.setText(propNameString);
                }

                msgCursor.moveToNext();
            }
        } finally {
            if (msgCursor != null) {
                msgCursor.close();
            }
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        Object tagObj = v.getTag();
        if (v instanceof EditText && tagObj != null && tagObj instanceof String) {
            String viewTag = tagObj.toString();
            if (MetrixStringHelper.valueIsEqual(viewTag, "property_value") && hasFocus) {
                LinearLayout rowLayout = (LinearLayout) v.getParent();
                TextView tvPropName = (TextView) rowLayout.findViewWithTag("property_name");
                String propName = tvPropName.getText().toString();

                if (MetrixStringHelper.valueIsEqual(propName, "VALUE_CHANGED_EVENT") || MetrixStringHelper.valueIsEqual(propName, "CONTROL_EVENT"))
                    doClientScriptSelection(v.getId(), rowLayout);
                else
                    doMessageSelection(propName, v.getId(), rowLayout);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        super.onClick(v);

        int viewId = v.getId();
        if (viewId == mFieldPropResourceData.getExtraResourceID("R.id.save")) {
            if (mAllowChanges) {
                if (!processAndSaveChanges())
                    return;

                Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldPropActivity.class);
                intent.putExtra("headingText", mHeadingText);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
            }
        } else if (viewId == mFieldPropResourceData.getExtraResourceID("R.id.finish")) {
            if (mAllowChanges) {
                if (!processAndSaveChanges())
                    return;
            }
            // allow pass through, even if changes aren't allowed (popping this activity off of stack)
            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldActivity.class);
            intent.putExtra("headingText", mHeadingText);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MetrixActivityHelper.startNewActivity(this, intent);
        }

        // Default value selection button click event
        Object viewTag = v.getTag();
        if (viewTag != null) {
            String tag = String.valueOf(viewTag);
            LinearLayout rowLayout = (LinearLayout) v.getParent();

            EditText etPropValue = (EditText) rowLayout.findViewWithTag("property_value");
            TextView tvPropName = (TextView) rowLayout.findViewWithTag("property_name");
            TextView tvPropDataType = (TextView) rowLayout.findViewWithTag("default_value_data_type");

            if (MetrixStringHelper.valueIsEqual(tag, "lookup_button")) {
                String propName = tvPropName.getText().toString();
                if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT_VALUE")) {
                    doMessageSelectionDefaultValue("MM_DEFAULT_VALUE", etPropValue.getId(), rowLayout, tvPropDataType.getId());
                } else if (MetrixStringHelper.valueIsEqual(propName, "SIGN_MESSAGE_ID"))
                    doMessageSelectionDefaultValue("MM_RESOURCE_STRING", etPropValue.getId(), rowLayout, tvPropDataType.getId());
            }
        }
    }

    private boolean fieldHasLookup(String fieldId) {
        int lkupCount = MetrixDatabaseManager.getCount("mm_field_lkup", "field_id = " + fieldId);
        return lkupCount > 0;
    }

    DialogInterface.OnClickListener deleteFieldLookupListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:    // Yes
                    if (deleteSelectedFieldLookupAndChildren()) {
                        Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldPropActivity.this, MetrixDesignerFieldPropActivity.class);
                        intent.putExtra("headingText", mHeadingText);
                        MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerFieldPropActivity.this, intent);
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
                    break;
            }
        }
    };

    private boolean deleteSelectedFieldLookupAndChildren() {
        boolean returnValue = true;
        try {
            boolean success = true;
            String currFieldID = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_id");
            String currLkupID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup", "lkup_id", "field_id = " + currFieldID);
            String currMetrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup", "metrix_row_id", "lkup_id = " + currLkupID);

            // generate a deletion transaction for mm_field_lkup ONLY
            MetrixUpdateMessage message = new MetrixUpdateMessage("mm_field_lkup", MetrixUpdateMessageTransactionType.Delete, "lkup_id", currLkupID);

            MetrixDatabaseManager.begintransaction();

            // delete all child records in DB (all 3 tables), without doing a message transaction
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_column_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %s)))", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %s))", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column", String.format("lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %s)", currLkupID));

            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_table_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_table where lkup_id = %s))", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_table where lkup_id = %s)", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table", String.format("lkup_id = %s", currLkupID));

            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_filter_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_filter where lkup_id = %s))", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_filter where lkup_id = %s)", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter", String.format("lkup_id = %s", currLkupID));

            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_orderby_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_orderby where lkup_id = %s))", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_orderby where lkup_id = %s)", currLkupID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby", String.format("lkup_id = %s", currLkupID));

            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_log where metrix_row_id = %s)", currMetrixRowID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup_log", String.format("metrix_row_id = %s", currMetrixRowID));
            if (success)
                success = MetrixDatabaseManager.deleteRow("mm_field_lkup", String.format("lkup_id = %s", currLkupID));

            if (success)
                success = message.save();
            else
                LogManager.getInstance().info("Failed to delete the mm_field_lkup for field_id = " + currFieldID);

            if (success)
                MetrixDatabaseManager.setTransactionSuccessful();

            returnValue = success;
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
            returnValue = false;
        } finally {
            MetrixDatabaseManager.endTransaction();
        }

        return returnValue;
    }

    /***
     * Populate the lookup for picking the dynamic default value code
     *
     * @param propName
     * @param viewToPopulateId
     * @param parentLayout
     * @param viewToKeepDataType
     */
    private void doMessageSelectionDefaultValue(String propName, int viewToPopulateId, LinearLayout parentLayout, int viewToKeepDataType) {
        MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
        MetrixLookupTableDef metrixCodeTableDef = new MetrixLookupTableDef("metrix_code_table", "mm_message_def_view", "metrix_code_table.message_id", "mm_message_def_view.message_id", "=");
        lookupDef.tableNames.add(metrixCodeTableDef);
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId, false));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("metrix_code_table.comments", viewToKeepDataType, true));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", propName));

        Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
        intent.putExtra("NoOptionsMenu", true);
        MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
        MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
        startActivityForResult(intent, 2727);
    }

    private void doMessageSelection(String propName, int viewToPopulateId, LinearLayout parentLayout) {
        MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", propName));

        Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
        intent.putExtra("NoOptionsMenu", true);
        MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
        MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
        startActivityForResult(intent, 2727);
    }

    private boolean processAndSaveChanges() {
        boolean noErrors = true;
        try {
            HashMap<String, String> dataTypeHashMap = new HashMap<String, String>();

            int fieldPropChangeCount = 0;

            ArrayList<MetrixSqlData> fieldToUpdate = new ArrayList<MetrixSqlData>();

            String fieldID = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_id");
            String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_field", "metrix_row_id", "field_id = " + fieldID);
            String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_field", "created_revision_id", "field_id = " + fieldID);
            MetrixSqlData data = new MetrixSqlData("mm_field", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
            data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
            data.dataFields.add(new DataField("field_id", fieldID));

            for (int i = 0; i < mTable.getChildCount(); i++) {
                LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
                TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
                TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

                String controlType = tvControlType.getText().toString();
                String propName = tvPropName.getText().toString();
                String currentPropValue = "";

                if (MetrixStringHelper.valueIsEqual(controlType, "TEXTBOX") || MetrixStringHelper.valueIsEqual(controlType, "LONGTEXT")
                        || MetrixStringHelper.valueIsEqual(controlType, "LOOKUP")) {
                    EditText etPropValue = (EditText) currLayout.findViewWithTag("property_value");
                    currentPropValue = etPropValue.getText().toString();
                } else if (MetrixStringHelper.valueIsEqual(controlType, "CHECKBOX")) {
                    CheckBox chkPropValue = (CheckBox) currLayout.findViewWithTag("property_value");
                    if (chkPropValue.isChecked())
                        currentPropValue = "Y";
                    else
                        currentPropValue = "N";
                } else if (MetrixStringHelper.valueIsEqual(controlType, "COMBOBOX")) {
                    Spinner spnPropValue = (Spinner) currLayout.findViewWithTag("property_value");
                    currentPropValue = MetrixControlAssistant.getValue(spnPropValue);
                } else if (MetrixStringHelper.valueIsEqual(controlType, "LABEL") || MetrixStringHelper.valueIsEqual(controlType, "CRUD_BUTTON")) {
                    // don't detect changes, and move onto next control
                    continue;
                } else {
                    throw new Exception("MDFieldProp: Unhandled control type.");
                }

                int currFieldIDNum = Integer.valueOf(fieldID);
                HashMap<String, String> origRow = mOriginalData.get(currFieldIDNum);

                String origPropValue = "";
                if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_FILE"))
                    origPropValue = origRow.get("mm_field.allow_file");
                else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_PHOTO"))
                    origPropValue = origRow.get("mm_field.allow_photo");
                else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_VIDEO"))
                    origPropValue = origRow.get("mm_field.allow_video");
                else if(MetrixStringHelper.valueIsEqual(propName, "ALLOW_CLEAR"))
                    origPropValue = origRow.get("mm_field.allow_clear");
                else if(MetrixStringHelper.valueIsEqual(propName, "SIGNER_COLUMN"))
                    origPropValue = origRow.get("mm_field.signer_column");
                else if(MetrixStringHelper.valueIsEqual(propName, "SIGN_MESSAGE_ID"))
                    origPropValue = origRow.get("mm_field.sign_message_id");
                else if (MetrixStringHelper.valueIsEqual(propName, "CARD_SCREEN_ID"))
                    origPropValue = origRow.get("mm_field.card_screen_id");
                else if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE"))
                    origPropValue = origRow.get("mm_field.control_type");
                else if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_EVENT"))
                    origPropValue = origRow.get("mm_field.control_event");
                else if (MetrixStringHelper.valueIsEqual(propName, "CURRENCY_COLUMN_NAME"))
                    origPropValue = origRow.get("mm_field.currency_column_name");
                else if (MetrixStringHelper.valueIsEqual(propName, "DATA_TYPE")) {
                    //keeping the field -> datatype
                    dataTypeHashMap.put("data_type", currentPropValue);
                    origPropValue = origRow.get("mm_field.data_type");
                } else if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT_VALUE")) {
                    TextView textViewDefaultValueDataType = (TextView) currLayout.findViewWithTag("default_value_data_type");
                    if (textViewDefaultValueDataType != null) {
                        String defaultValueDataType = textViewDefaultValueDataType.getText().toString();
                        //keeping the dynamic default value -> datatype
                        dataTypeHashMap.put("default_value_data_type", defaultValueDataType);
                        dataTypeHashMap.put("current_default_value", currentPropValue);
                    }
                    origPropValue = origRow.get("mm_field.default_value");
                } else if (MetrixStringHelper.valueIsEqual(propName, "DISPLAY_STYLE"))
                    origPropValue = origRow.get("mm_field.display_style");
                else if (MetrixStringHelper.valueIsEqual(propName, "FORCE_CASE"))
                    origPropValue = origRow.get("mm_field.force_case");
                else if (MetrixStringHelper.valueIsEqual(propName, "HYPERLINK"))
                    origPropValue = origRow.get("mm_field.hyperlink");
                else if (MetrixStringHelper.valueIsEqual(propName, "INPUT_TYPE"))
                    origPropValue = origRow.get("mm_field.input_type");
                else if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
                    origPropValue = origRow.get("mm_field.label");
                else if (MetrixStringHelper.valueIsEqual(propName, "LIST_DISPLAY_COLUMN"))
                    origPropValue = origRow.get("mm_field.list_display_column");
                else if (MetrixStringHelper.valueIsEqual(propName, "LIST_FILTER_COLUMN"))
                    origPropValue = origRow.get("mm_field.list_filter_column");
                else if (MetrixStringHelper.valueIsEqual(propName, "LIST_FILTER_VALUE"))
                    origPropValue = origRow.get("mm_field.list_filter_value");
                else if (MetrixStringHelper.valueIsEqual(propName, "LIST_JOIN_TYPE"))
                    origPropValue = origRow.get("mm_field.list_join_type");
                else if (MetrixStringHelper.valueIsEqual(propName, "LIST_ORDER_BY"))
                    origPropValue = origRow.get("mm_field.list_order_by");
                else if (MetrixStringHelper.valueIsEqual(propName, "LIST_TABLE_NAME"))
                    origPropValue = origRow.get("mm_field.list_table_name");
                else if (MetrixStringHelper.valueIsEqual(propName, "LIST_VALUE_COLUMN"))
                    origPropValue = origRow.get("mm_field.list_value_column");
                else if (MetrixStringHelper.valueIsEqual(propName, "MAX_CHARS"))
                    origPropValue = origRow.get("mm_field.max_chars");
                else if (MetrixStringHelper.valueIsEqual(propName, "READ_ONLY"))
                    origPropValue = origRow.get("mm_field.read_only");
                else if (MetrixStringHelper.valueIsEqual(propName, "REGION"))
                    origPropValue = origRow.get("mm_field.region");
                else if (MetrixStringHelper.valueIsEqual(propName, "RELATIVE_WIDTH"))
                    origPropValue = origRow.get("mm_field.relative_width");
                else if (MetrixStringHelper.valueIsEqual(propName, "REQUIRED"))
                    origPropValue = origRow.get("mm_field.required");
                else if (MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE"))
                    origPropValue = origRow.get("mm_field.searchable");
                else if (MetrixStringHelper.valueIsEqual(propName, "TRANSACTION_COLUMN"))
                    origPropValue = origRow.get("mm_field.transaction_column");
                else if (MetrixStringHelper.valueIsEqual(propName, "TRANSACTION_TABLE"))
                    origPropValue = origRow.get("mm_field.transaction_table");
                else if (MetrixStringHelper.valueIsEqual(propName, "VALIDATION"))
                    origPropValue = origRow.get("mm_field.validation");
                else if (MetrixStringHelper.valueIsEqual(propName, "VALUE_CHANGED_EVENT"))
                    origPropValue = origRow.get("mm_field.value_changed_event");
                else if (MetrixStringHelper.valueIsEqual(propName, "VISIBLE"))
                    origPropValue = origRow.get("mm_field.visible");
                else if (MetrixStringHelper.valueIsEqual(propName, "SIGN_MESSAGE_ID"))
                    origPropValue = origRow.get("mm_field.sign_message_id");

                if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
                    if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_FILE"))
                        data.dataFields.add(new DataField("allow_file", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_PHOTO"))
                        data.dataFields.add(new DataField("allow_photo", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_VIDEO"))
                        data.dataFields.add(new DataField("allow_video", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "ALLOW_CLEAR"))
                        data.dataFields.add(new DataField("allow_clear", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "SIGNER_COLUMN"))
                        data.dataFields.add(new DataField("signer_column", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "SIGN_MESSAGE_ID"))
                        data.dataFields.add(new DataField("sign_message_id", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "CARD_SCREEN_ID"))
                        data.dataFields.add(new DataField("card_screen_id", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_TYPE")) {
                        // only attempt to change control_type property if this is a non-baseline field and is not an Attachment Field
                        if (!MetrixStringHelper.isNullOrEmpty(createdRevisionID) && !isAttachmentField)
                            data.dataFields.add(new DataField("control_type", currentPropValue));
                    } else if (MetrixStringHelper.valueIsEqual(propName, "CONTROL_EVENT"))
                        data.dataFields.add(new DataField("control_event", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "CURRENCY_COLUMN_NAME"))
                        data.dataFields.add(new DataField("currency_column_name", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "DATA_TYPE"))
                        data.dataFields.add(new DataField("data_type", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT_VALUE"))
                        data.dataFields.add(new DataField("default_value", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "DISPLAY_STYLE"))
                        data.dataFields.add(new DataField("display_style", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "FORCE_CASE"))
                        data.dataFields.add(new DataField("force_case", currentPropValue));
                    // hyperlink property which is available for list screens only
                    else if (MetrixStringHelper.valueIsEqual(propName, "HYPERLINK")) {
                        String reqCtrlType = (MetrixStringHelper.valueIsEqual(currentPropValue, "Y")) ? "HYPERLINK" : "TEXT";
                        data.dataFields.add(new DataField("control_type", reqCtrlType));
                    } else if (MetrixStringHelper.valueIsEqual(propName, "INPUT_TYPE"))
                        data.dataFields.add(new DataField("input_type", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
                        data.dataFields.add(new DataField("label", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_DISPLAY_COLUMN"))
                        data.dataFields.add(new DataField("list_display_column", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_FILTER_COLUMN"))
                        data.dataFields.add(new DataField("list_filter_column", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_FILTER_VALUE"))
                        data.dataFields.add(new DataField("list_filter_value", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_JOIN_TYPE"))
                        data.dataFields.add(new DataField("list_join_type", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_ORDER_BY"))
                        data.dataFields.add(new DataField("list_order_by", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_TABLE_NAME"))
                        data.dataFields.add(new DataField("list_table_name", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LIST_VALUE_COLUMN"))
                        data.dataFields.add(new DataField("list_value_column", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "MAX_CHARS"))
                        data.dataFields.add(new DataField("max_chars", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "READ_ONLY"))
                        data.dataFields.add(new DataField("read_only", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "REGION"))
                        data.dataFields.add(new DataField("region", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "RELATIVE_WIDTH"))
                        data.dataFields.add(new DataField("relative_width", MetrixFloatHelper.convertNumericFromUIToDB(currentPropValue)));
                    else if (MetrixStringHelper.valueIsEqual(propName, "REQUIRED"))
                        data.dataFields.add(new DataField("required", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "SEARCHABLE"))
                        data.dataFields.add(new DataField("searchable", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "TRANSACTION_COLUMN"))
                        data.dataFields.add(new DataField("transaction_column", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "TRANSACTION_TABLE"))
                        data.dataFields.add(new DataField("transaction_table", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "VALIDATION"))
                        data.dataFields.add(new DataField("validation", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "VALUE_CHANGED_EVENT"))
                        data.dataFields.add(new DataField("value_changed_event", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "VISIBLE")) {
                        // if current property is the VISIBLE property, set ORDER appropriately
                        data.dataFields.add(new DataField("visible", currentPropValue));
                        DataField orderData = changeFieldOrderBasedOnNewVisibility(currentPropValue);
                        data.dataFields.add(orderData);
                    }

                    fieldPropChangeCount++;
                }
            }

            noErrors = checkDynamicDefaultValueDataType(noErrors, dataTypeHashMap);
            if (noErrors && fieldPropChangeCount > 0) {
                // upon detecting changes...
                // update modified_revision_id, only if ALL of the following apply:
                // 1) this is not the first revision in the design set
                // 2) this field has not been added in this revision
                String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
                String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
                String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
                // if previousRevisionID exists, then this is not the first revision
                if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
                        && (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
                    // field meets both conditions stated above; update modified_revision_id
                    data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
                }

                fieldToUpdate.add(data);

                MetrixTransaction transactionInfo = new MetrixTransaction();
                MetrixUpdateManager.update(fieldToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("FieldChange"), this);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
            Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
            noErrors = false;
        }
        return noErrors;
    }

    /**
     * Check the current field data-type and dynamic value data type
     *
     * @param noErrors
     * @param dataTypeHashMap
     * @return true if the two data types are identical.
     */
    private boolean checkDynamicDefaultValueDataType(boolean noErrors, HashMap<String, String> dataTypeHashMap) {
        if (dataTypeHashMap != null && dataTypeHashMap.size() > 0) {
            String fieldDataType = dataTypeHashMap.get("data_type");
            //if the field data type is empty, we won't bother about the dynamic default value data type.
            if (!MetrixStringHelper.isNullOrEmpty(fieldDataType)) {
                //if the field data type is STRING, we won't validate the dynamic default value data type.
                if (!MetrixStringHelper.valueIsEqual(fieldDataType, "STRING")) {
                    String defaultValueDataType = dataTypeHashMap.get("default_value_data_type");
                    if (!MetrixStringHelper.isNullOrEmpty(defaultValueDataType)) {
                        boolean validationFailed = false;
                        String currentDefaultValue = dataTypeHashMap.get("current_default_value");
                        if (!MetrixStringHelper.isNullOrEmpty(currentDefaultValue) && MetrixStringHelper.valueIsEqual(currentDefaultValue, "CurrentDateTime")) {
                            // don't display an error if we have CurrentDateTime as the default value
                            // and the field's data_type is DATE/TIME/DATE_TIME (any of these)
                            if (!MetrixStringHelper.valueIsEqual(fieldDataType, "DATE") && !MetrixStringHelper.valueIsEqual(fieldDataType, "TIME") && !MetrixStringHelper.valueIsEqual(fieldDataType, "DATE_TIME"))
                                validationFailed = true;
                        } else if (!MetrixStringHelper.valueIsEqual(fieldDataType, defaultValueDataType))
                            validationFailed = true;

                        if (validationFailed) {
                            Toast.makeText(this, AndroidResourceHelper.getMessage("FieldPropDefaultDtMismatch", fieldDataType, defaultValueDataType), Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private DataField changeFieldOrderBasedOnNewVisibility(String visibleValue) {
        // if value is Y ... set ORDER to MAX(Order) + 1 (if this is the first visible field, ORDER is 1)
        // if value is N ... set ORDER to -1
        String newOrder = "-1";
        if (MetrixStringHelper.valueIsEqual(visibleValue, "Y")) {
            String maxOrder = MetrixDatabaseManager.getFieldStringValue(true, "mm_field", "display_order", String.format("field_id in (select field_id from mm_field where screen_id = %s)", MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id")), null, null, null, "display_order DESC", null);
            int currentMax = Integer.valueOf(maxOrder);
            if (currentMax < 0) currentMax = 0;
            newOrder = String.valueOf(currentMax + 1);
        }

        return new DataField("display_order", newOrder);
    }
}

