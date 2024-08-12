package com.metrix.architecture.designer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class MetrixDesignerFilterSortPropActivity extends MetrixDesignerActivity implements View.OnFocusChangeListener {
    private HashMap<Integer, HashMap<String, String>> mOriginalData;
    private LinearLayout mTable;
    private TextView mTitle;
    private Button mSave, mFinish;
    private String mItemName, mSelectedItemType;
    private MetrixDesignerResourceData mFSPResourceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFSPResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFilterSortPropActivityResourceData");

        setContentView(mFSPResourceData.LayoutResourceID);

        mTable = (LinearLayout) findViewById(mFSPResourceData.getExtraResourceID("R.id.table_layout"));
        mItemName = MetrixCurrentKeysHelper.getKeyValue("mm_filter_sort_item", "item_name");
        populateScreen();
    }

    @Override
    public void onStart() {
        super.onStart();

        helpText = mFSPResourceData.HelpTextString;

        mHeadingText = getIntent().getStringExtra("headingText");
        if (mActionBarTitle != null)
            mActionBarTitle.setText(mHeadingText);

        mTitle = (TextView) findViewById(mFSPResourceData.getExtraResourceID("R.id.zzmd_filter_sort_prop_label"));
        String fullTitle = AndroidResourceHelper.getMessage("Properties1Args", mItemName);
        mTitle.setText(fullTitle);

        mSave = (Button) findViewById(mFSPResourceData.getExtraResourceID("R.id.save"));
        mSave.setEnabled(mAllowChanges);
        mSave.setOnClickListener(this);

        mFinish = (Button) findViewById(mFSPResourceData.getExtraResourceID("R.id.finish"));
        mFinish.setOnClickListener(this);

        TextView mTip = (TextView) findViewById(mFSPResourceData.getExtraResourceID("R.id.zzmd_filter_sort_prop_tip"));
        AndroidResourceHelper.setResourceValues(mTip, "FilterSortPropTip");

        AndroidResourceHelper.setResourceValues(mSave, "Save");
        AndroidResourceHelper.setResourceValues(mFinish, "Finish");
    }

    private void populateScreen() {
        String itemID = MetrixCurrentKeysHelper.getKeyValue("mm_filter_sort_item", "item_id");

        String metrixRowID = "";
        String content = "";
        String isDefault = "";
        String fullFilter = "";
        String itemType = "";
        String label = "";

        String query = "select mm_filter_sort_item.metrix_row_id, mm_filter_sort_item.content, mm_filter_sort_item.is_default, "
                + " mm_filter_sort_item.full_filter, mm_filter_sort_item.item_type, mm_filter_sort_item.label"
                + " from mm_filter_sort_item where item_id = " + itemID;

        mOriginalData = new HashMap<Integer, HashMap<String, String>>();
        MetrixCursor cursor = null;
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            while (cursor.isAfterLast() == false) {
                metrixRowID = cursor.getString(0);
                content = cursor.getString(1);
                isDefault = cursor.getString(2);
                fullFilter = cursor.getString(3);
                itemType = cursor.getString(4);
                label = cursor.getString(5);

                if (MetrixStringHelper.isNullOrEmpty(content)) content = "";
                if (MetrixStringHelper.isNullOrEmpty(isDefault)) isDefault = "";
                if (MetrixStringHelper.isNullOrEmpty(fullFilter)) fullFilter = "";
                if (MetrixStringHelper.isNullOrEmpty(itemType)) itemType = "";
                if (MetrixStringHelper.isNullOrEmpty(label)) label = "";

                int thisItemIDNum = Integer.valueOf(itemID);

                // populate mOriginalData with a row
                HashMap<String, String> origRow = new HashMap<String, String>();
                origRow.put("mm_filter_sort_item.metrix_row_id", metrixRowID);
                origRow.put("mm_filter_sort_item.content", content);
                origRow.put("mm_filter_sort_item.is_default", isDefault);
                origRow.put("mm_filter_sort_item.full_filter", fullFilter);
                origRow.put("mm_filter_sort_item.item_type", itemType);
                origRow.put("mm_filter_sort_item.label", label);
                mOriginalData.put(thisItemIDNum, origRow);

                mSelectedItemType = itemType;

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
        msgQuery.append(" WHERE c.code_name = 'MM_FILTER_SORT_ITEM_PROP'");
        msgQuery.append(" ORDER BY v.message_text ASC");

        MetrixCursor msgCursor = null;
        try {
            msgCursor = MetrixDatabaseManager.rawQueryMC(msgQuery.toString(), null);

            if (msgCursor == null || !msgCursor.moveToFirst()) {
                return;
            }

            while (msgCursor.isAfterLast() == false) {
                String propName = msgCursor.getString(0);
                String propNameString = msgCursor.getString(1);

                // using property_name, use hard-coded map to determine what layout to inflate and populate with data
                // MESSAGE LOOKUP	LABEL
                // TEXTBOX/LOOKUP	CONTENT
                // CHECKBOX			DEFAULT, FULL_FILTER
                // TEXTVIEW         ITEM_TYPE
                // TEXTBOX			(default, all others)
                LinearLayout layout;
                if (MetrixStringHelper.valueIsEqual(propName, "LABEL")) {
                    // MESSAGE LOOKUP
                    layout = MetrixControlAssistant.addLinearLayout(this, mFSPResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
                    String messageType = "MM_FILTER_SORT";
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
                } else if (MetrixStringHelper.valueIsEqual(propName, "CONTENT")) {
                    // TEXTBOX/LOOKUP (can type in a value OR do a client script lookup)
                    layout = MetrixControlAssistant.addLinearLayout(this, mFSPResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_imageview_line"), mTable);
                    layout.setTag("CONTENT");
                    ImageView imgLookupButton = (ImageView)layout.findViewWithTag("lookup_button");

                    EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
                    etPropValue.setId(MetrixControlAssistant.generateViewId());
                    etPropValue.setText(content);
                    TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
                    TextView tvDescText = (TextView) layout.findViewWithTag("description_text");

                    TextView tvPropDataType = (TextView) layout.findViewWithTag("default_value_data_type");
                    tvPropDataType.setId(MetrixControlAssistant.generateViewId());

                    //Search button click event
                    imgLookupButton.setOnClickListener(this);
                    //Text change event
                    etPropValue.addTextChangedListener(new ClientScriptOrLiteralTextWatcher(tvDescLabel, tvDescText));

                    tvDescLabel.setVisibility(View.GONE);
                    tvDescText.setVisibility(View.GONE);
                    if (!MetrixStringHelper.isNullOrEmpty(content)) {
                        String description = "";
                        String scriptName = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "name", String.format("unique_vs = '%s'", content));
                        String scriptVersion = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "version_number", String.format("unique_vs = '%s'", content));
                        if (!MetrixStringHelper.isNullOrEmpty(scriptName) && !MetrixStringHelper.isNullOrEmpty(scriptVersion)) {
                            if (MetrixStringHelper.valueIsEqual(scriptVersion, "0"))
                                description = String.format("%1$s (%2$s)", scriptName, AndroidResourceHelper.getMessage("Baseline"));
                            else
                                description = String.format("%1$s (%2$s %3$s)", scriptName, AndroidResourceHelper.getMessage("Version"), scriptVersion);
                        } else
                            description = AndroidResourceHelper.getMessage("LiteralValue");

                        tvDescText.setText(description);
                        tvDescLabel.setVisibility(View.INVISIBLE);
                        tvDescText.setVisibility(View.VISIBLE);
                    }
                } else if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT")) {
                    // CHECKBOX
                    layout = MetrixControlAssistant.addLinearLayout(this, mFSPResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
                    CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");

                    if (isDefault.compareToIgnoreCase("Y") == 0)
                        chkPropValue.setChecked(true);
                    else
                        chkPropValue.setChecked(false);
                } else if (MetrixStringHelper.valueIsEqual(propName, "FULL_FILTER")) {
                    // CHECKBOX - FILTER item_type only - skip otherwise
                    if (!MetrixStringHelper.valueIsEqual(itemType, "FILTER")) {
                        msgCursor.moveToNext();
                        continue;
                    }

                    layout = MetrixControlAssistant.addLinearLayout(this, mFSPResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
                    CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");

                    if (fullFilter.compareToIgnoreCase("Y") == 0)
                        chkPropValue.setChecked(true);
                    else
                        chkPropValue.setChecked(false);
                } else if (MetrixStringHelper.valueIsEqual(propName, "ITEM_TYPE")) {
                    // TEXTVIEW
                    layout = MetrixControlAssistant.addLinearLayout(this, mFSPResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
                    TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
                    String itemTypeDesc = MetrixDatabaseManager.getFieldStringValue(String.format("select message_text from mm_message_def_view where message_type = 'CODE' and message_id in (select message_id from metrix_code_table where code_name = 'MM_FILTER_SORT_ITEM_TYPE' and code_value = '%1$s')", itemType));
                    tvPropValue.setText(itemTypeDesc);
                } else {
                    // TEXTBOX
                    layout = MetrixControlAssistant.addLinearLayout(this, mFSPResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_line"), mTable);
                }

                // all layouts have these, so set generically
                TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
                TextView tvItemID = (TextView) layout.findViewWithTag("pv_id");
                TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
                TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

                tvMetrixRowID.setText(metrixRowID);
                tvItemID.setText(itemID);
                tvPropName.setText(propName);
                tvPropNameString.setText(propNameString);

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
                doMessageSelection(v.getId(), rowLayout);
            }
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int viewId = v.getId();
        if (viewId == mFSPResourceData.getExtraResourceID("R.id.save")) {
            if (mAllowChanges) {
                processAndSaveChanges();

                Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFilterSortPropActivity.class);
                intent.putExtra("headingText", mHeadingText);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
            }
        } else if (viewId == mFSPResourceData.getExtraResourceID("R.id.finish")) {
            if (mAllowChanges) {
                processAndSaveChanges();
            }
            // allow pass through, even if changes aren't allowed
            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFilterSortEnablingActivity.class);
            intent.putExtra("headingText", mHeadingText);
            intent.putExtra("filterSortCodeValue", mSelectedItemType);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MetrixActivityHelper.startNewActivity(this, intent);
        } else {
            Object viewTag = v.getTag();
            if (viewTag != null) {
                String tag = String.valueOf(viewTag);
                if (MetrixStringHelper.valueIsEqual(tag, "lookup_button")) {
                    LinearLayout rowLayout = (LinearLayout) v.getParent();
                    EditText etPropValue = (EditText) rowLayout.findViewWithTag("property_value");
                    doClientScriptSelection(etPropValue.getId(), rowLayout);
                }
            }
        }
    }

    private void doMessageSelection(int viewToPopulateId, LinearLayout parentLayout) {
        MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_FILTER_SORT"));

        Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
        intent.putExtra("NoOptionsMenu", true);
        MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
        MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
        startActivityForResult(intent, 2727);
    }

    private void processAndSaveChanges() {
        try {
            int itemPropChangeCount = 0;
            boolean defaultChangedToY = false;

            ArrayList<MetrixSqlData> itemToUpdate = new ArrayList<MetrixSqlData>();

            String itemID = MetrixCurrentKeysHelper.getKeyValue("mm_filter_sort_item", "item_id");
            String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_filter_sort_item", "metrix_row_id", "item_id = " + itemID);
            String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_filter_sort_item", "created_revision_id", "item_id = " + itemID);
            MetrixSqlData data = new MetrixSqlData("mm_filter_sort_item", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
            data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
            data.dataFields.add(new DataField("item_id", itemID));

            for (int i = 0; i < mTable.getChildCount(); i++) {
                LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
                TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
                TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

                String controlType = tvControlType.getText().toString();
                String propName = tvPropName.getText().toString();
                String currentPropValue = "";

                if (MetrixStringHelper.valueIsEqual(controlType, "TEXTBOX")	|| MetrixStringHelper.valueIsEqual(controlType, "LOOKUP")) {
                    EditText etPropValue = (EditText) currLayout.findViewWithTag("property_value");
                    currentPropValue = etPropValue.getText().toString();
                } else if (MetrixStringHelper.valueIsEqual(controlType, "CHECKBOX")) {
                    CheckBox chkPropValue = (CheckBox) currLayout.findViewWithTag("property_value");
                    if (chkPropValue.isChecked())
                        currentPropValue = "Y";
                    else
                        currentPropValue = "N";
                } else if (MetrixStringHelper.valueIsEqual(controlType, "LABEL")) {
                    // We skip label properties (ITEM_TYPE), because the value doesn't change
                    continue;
                } else {
                    throw new Exception("MDFilterSortProp: Unhandled control type.");
                }

                int currItemIDNum = Integer.valueOf(itemID);
                HashMap<String, String> origRow = mOriginalData.get(currItemIDNum);

                String origPropValue = "";
                if (MetrixStringHelper.valueIsEqual(propName, "CONTENT"))
                    origPropValue = origRow.get("mm_filter_sort_item.content");
                else if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT"))
                    origPropValue = origRow.get("mm_filter_sort_item.is_default");
                else if (MetrixStringHelper.valueIsEqual(propName, "FULL_FILTER"))
                    origPropValue = origRow.get("mm_filter_sort_item.full_filter");
                else if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
                    origPropValue = origRow.get("mm_filter_sort_item.label");

                if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
                    if (MetrixStringHelper.valueIsEqual(propName, "CONTENT"))
                        data.dataFields.add(new DataField("content", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "DEFAULT")) {
                        data.dataFields.add(new DataField("is_default", currentPropValue));
                        defaultChangedToY = MetrixStringHelper.valueIsEqual(currentPropValue, "Y");
                    } else if (MetrixStringHelper.valueIsEqual(propName, "FULL_FILTER"))
                        data.dataFields.add(new DataField("full_filter", currentPropValue));
                    else if (MetrixStringHelper.valueIsEqual(propName, "LABEL"))
                        data.dataFields.add(new DataField("label", currentPropValue));

                    itemPropChangeCount++;
                }
            }

            if (itemPropChangeCount > 0) {
                // upon detecting changes...
                // update modified_revision_id, only if ALL of the following apply:
                // 1) this is not the first revision in the design set
                // 2) this item has not been added in this revision
                String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
                String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
                String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
                // if previousRevisionID exists, then this is not the first revision
                if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
                        && (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
                    // field meets both conditions stated above; update modified_revision_id
                    data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
                }

                itemToUpdate.add(data);

                if (defaultChangedToY)
                    turnOffOtherFilterSortDefaults(MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"), MetrixDatabaseManager.getFieldStringValue("mm_filter_sort_item", "item_type", "item_id = " + itemID));

                MetrixTransaction transactionInfo = new MetrixTransaction();
                MetrixUpdateManager.update(itemToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("FilterSortUpdate"), this);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
            Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
        }
    }
}