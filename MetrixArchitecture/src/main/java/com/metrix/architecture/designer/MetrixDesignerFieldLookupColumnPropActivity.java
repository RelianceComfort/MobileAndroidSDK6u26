package com.metrix.architecture.designer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

@SuppressLint("UseSparseArrays")
public class MetrixDesignerFieldLookupColumnPropActivity extends MetrixDesignerActivity {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mEmphasis;
	private Button mSave, mFinish;
	private String mScreenName, mFieldName, mTableName;
	private MetrixDesignerResourceData mLookupColumnPropResourceData;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    	
    	
    	mLookupColumnPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupColumnPropActivityResourceData");
    	
        setContentView(mLookupColumnPropResourceData.LayoutResourceID);
        
    	mTable = (LinearLayout) findViewById(mLookupColumnPropResourceData.getExtraResourceID("R.id.table_layout"));
    	populateScreen();
    }
	
	@Override
	public void onStart() {
		super.onStart();
		
		helpText = mLookupColumnPropResourceData.HelpTextString;
			
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		mTableName = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "table_name");
		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}
		
		mEmphasis = (TextView) findViewById(mLookupColumnPropResourceData.getExtraResourceID("R.id.zzmd_field_lookup_column_prop_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupColProp", mTableName, mFieldName, mScreenName);
		mEmphasis.setText(fullText);
		
		mSave = (Button) findViewById(mLookupColumnPropResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);
		
		mFinish = (Button) findViewById(mLookupColumnPropResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mLkupProp = (TextView) findViewById(mLookupColumnPropResourceData.getExtraResourceID("R.id.lookup_column_properties"));

		AndroidResourceHelper.setResourceValues(mLkupProp, "LookupColumnProperties");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");
	}
	
	private void populateScreen() {
		String lkupColumnID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_column", "lkup_column_id");
			
		String metrixRowID = "";
		String columnName = "";
		String linkedFieldID = "";
		String alwaysHide = "";
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup_column.metrix_row_id, mm_field_lkup_column.column_name, mm_field_lkup_column.linked_field_id, mm_field_lkup_column.always_hide");
		query.append(" FROM mm_field_lkup_column WHERE mm_field_lkup_column.lkup_column_id = " + lkupColumnID);
		
		mOriginalData = new HashMap<Integer, HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
	
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}
			
			while (cursor.isAfterLast() == false) {
				metrixRowID = cursor.getString(0);
				columnName = cursor.getString(1);
				linkedFieldID = cursor.getString(2);
				alwaysHide = cursor.getString(3);
				
				if (MetrixStringHelper.isNullOrEmpty(linkedFieldID)) linkedFieldID = "";
				if (MetrixStringHelper.isNullOrEmpty(alwaysHide)) alwaysHide = "";
							
				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_field_lkup_column.metrix_row_id", metrixRowID);
				origRow.put("mm_field_lkup_column.column_name", columnName);
				origRow.put("mm_field_lkup_column.linked_field_id", linkedFieldID);
				origRow.put("mm_field_lkup_column.always_hide", alwaysHide);
				mOriginalData.put(Integer.valueOf(lkupColumnID), origRow);
				
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
		msgQuery.append(" WHERE c.code_name = 'MM_FIELD_LKUP_COLUMN_PROP'");
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
				// COMBOBOX		LINKED_FIELD_ID
				// LABEL		COLUMN_NAME
				// CHECKBOX		ALWAYS_HIDE			
				LinearLayout layout = null;
				if (MetrixStringHelper.valueIsEqual(propName, "ALWAYS_HIDE")) {
					// CHECKBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupColumnPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
					CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
					if ((MetrixStringHelper.valueIsEqual(propName, "ALWAYS_HIDE") && alwaysHide.compareToIgnoreCase("Y") == 0))
						chkPropValue.setChecked(true);
					else
						chkPropValue.setChecked(false);			
				} else if (MetrixStringHelper.valueIsEqual(propName, "LINKED_FIELD_ID")) {
					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupColumnPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");
					
					// populate Linked Field spinner with all fields on this screen, except those that already serve as linked fields somewhere on the current lookup (across all tables)
					String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
					String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");				
					String linkedFieldSQL = "";
					if (MetrixStringHelper.isNullOrEmpty(linkedFieldID)) {
						//we should omit control types such as buttons as they're not allowed to treat as linked fields
						linkedFieldSQL = String.format("select field_id, table_name, column_name from mm_field where screen_id = %1$s and field_id not in (select distinct linked_field_id from mm_field_lkup_column where linked_field_id is not null and lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %2$s)) and control_type not in ('%3$s') order by table_name, column_name asc", screenID, lkupID, "BUTTON");
					} else {
						//we should omit control types such as buttons as they're not allowed to treat as linked fields
						linkedFieldSQL = String.format("select field_id, table_name, column_name from mm_field where field_id = %1$s or (screen_id = %2$s and field_id not in (select distinct linked_field_id from mm_field_lkup_column where linked_field_id is not null and lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %3$s))) and control_type not in ('%4$s') order by table_name, column_name asc", linkedFieldID, screenID, lkupID, "BUTTON");
					}
					
					ArrayList<Hashtable<String, String>> screenFieldArray = MetrixDatabaseManager.getFieldStringValuesList(linkedFieldSQL);
					ArrayList<SpinnerKeyValuePair> linkedFieldSet = new ArrayList<SpinnerKeyValuePair>();
					linkedFieldSet.add(new SpinnerKeyValuePair("", ""));
					if (screenFieldArray != null && screenFieldArray.size() > 0) {
						for (Hashtable<String, String> fieldData : screenFieldArray) {
							String fieldID = fieldData.get("field_id");		
							String fieldName = String.format("%1$s.%2$s", fieldData.get("table_name"), fieldData.get("column_name"));
							SpinnerKeyValuePair item = new SpinnerKeyValuePair(fieldName, fieldID);
							linkedFieldSet.add(item);
						}
					}		
					MetrixControlAssistant.populateSpinnerFromPair(this, spnPropValue, linkedFieldSet);
					
					try {
						MetrixControlAssistant.setValue(spnPropValue, linkedFieldID);
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "COLUMN_NAME")) {
					// LABEL
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupColumnPropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
					tvPropValue.setText(columnName);
				}
							
				// all layouts have these, so set generically
				if (layout != null) {
					TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
					TextView tvLkupColumnID = (TextView) layout.findViewWithTag("pv_id");
					TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
					TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");
					
					tvMetrixRowID.setText(metrixRowID);
					tvLkupColumnID.setText(lkupColumnID);
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
	public void onClick(View v) {
		super.onClick(v);
		
		int viewId = v.getId();
		if (viewId == mLookupColumnPropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
				
				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupColumnPropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mLookupColumnPropResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}			
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupColumnActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private boolean processAndSaveChanges() {
		try {
			int lookupColumnPropChangeCount = 0;		
			ArrayList<MetrixSqlData> lookupColumnToUpdate = new ArrayList<MetrixSqlData>();
			
			String lkupColumnID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_column", "lkup_column_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_column", "metrix_row_id", "lkup_column_id = " + lkupColumnID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_column", "created_revision_id", "lkup_column_id = " + lkupColumnID);
			MetrixSqlData data = new MetrixSqlData("mm_field_lkup_column", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("lkup_column_id", lkupColumnID));
			
			for (int i = 0; i < mTable.getChildCount(); i++) {
				LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
				TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
				TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");
							
				String controlType = tvControlType.getText().toString();	
				String propName = tvPropName.getText().toString();
				String currentPropValue = "";
				
				if (MetrixStringHelper.valueIsEqual(controlType, "COMBOBOX")) {
					Spinner spnPropValue = (Spinner) currLayout.findViewWithTag("property_value");
					currentPropValue = MetrixControlAssistant.getValue(spnPropValue);
				} else if (MetrixStringHelper.valueIsEqual(controlType, "LABEL")) {
					continue;	// don't consider labels for saving changes
				} else if (MetrixStringHelper.valueIsEqual(controlType, "CHECKBOX")) {
					CheckBox chkPropValue = (CheckBox) currLayout.findViewWithTag("property_value");
					if (chkPropValue.isChecked())
						currentPropValue = "Y";
					else
						currentPropValue = "N";
				} else {
					throw new Exception("MDFieldLookupTableProp: Unhandled control type.");
				}
				
				int currLookupColumnIDNum = Integer.valueOf(lkupColumnID);
				HashMap<String, String> origRow = mOriginalData.get(currLookupColumnIDNum);
				
				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "LINKED_FIELD_ID"))
					origPropValue = origRow.get("mm_field_lkup_column.linked_field_id");
				else if (MetrixStringHelper.valueIsEqual(propName, "ALWAYS_HIDE"))
					origPropValue = origRow.get("mm_field_lkup_column.always_hide");
						
				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {	
					if (MetrixStringHelper.valueIsEqual(propName, "LINKED_FIELD_ID"))
						data.dataFields.add(new DataField("linked_field_id", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "ALWAYS_HIDE")) {
						// if current property is the ALWAYS_HIDE property, set APPLIED_ORDER appropriately
						data.dataFields.add(new DataField("always_hide", currentPropValue));
						DataField orderData = changeColumnAppliedOrderBasedOnNewVisibility(currentPropValue);
						data.dataFields.add(orderData);
					}

					lookupColumnPropChangeCount++;
				}	
			}
			
			if (lookupColumnPropChangeCount > 0) {
				// upon detecting changes...
				// update modified_revision_id, only if ALL of the following apply:
				// 1) this is not the first revision in the design set
				// 2) this field lookup column has not been added in this revision
				String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
				String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
				String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");				
				// if previousRevisionID exists, then this is not the first revision
				if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID) 
					&& (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
					// column meets both conditions stated above; update modified_revision_id
					data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
				}
							
				lookupColumnToUpdate.add(data);
				
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lookupColumnToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("LookUpColumnChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private DataField changeColumnAppliedOrderBasedOnNewVisibility(String alwaysHideValue) {		
		// if value is N ... set ORDER to MAX(Order) + 1 (across all tables for this lookup ... if this is the first visible column, ORDER is 1)
		// if value is Y ... set ORDER to -1
		String newOrder = "-1";
		if (!MetrixStringHelper.valueIsEqual(alwaysHideValue, "Y")) {
			String maxOrder = MetrixDatabaseManager.getFieldStringValue(true, "mm_field_lkup_column", "applied_order", String.format("lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %s)", MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id")), null, null, null, "applied_order DESC", null);
			int currentMax = Integer.valueOf(maxOrder);
			if (currentMax < 0) currentMax = 0;
			newOrder = String.valueOf(currentMax + 1);
		}
		
		return new DataField("applied_order", newOrder);
	}
}