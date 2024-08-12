package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
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
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseSparseArrays")
public class MetrixDesignerFieldLookupTablePropActivity extends MetrixDesignerActivity {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mEmphasis;
	private Button mSave, mViewColumns;
	private String mScreenName, mFieldName;
	private MetrixDesignerResourceData mLookupTablePropResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupTablePropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupTablePropActivityResourceData");

		setContentView(mLookupTablePropResourceData.LayoutResourceID);

		mTable = (LinearLayout) findViewById(mLookupTablePropResourceData.getExtraResourceID("R.id.table_layout"));
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupTablePropResourceData.HelpTextString;

		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupTablePropResourceData.getExtraResourceID("R.id.zzmd_field_lookup_table_prop_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupTblProp", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mSave = (Button) findViewById(mLookupTablePropResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mViewColumns = (Button) findViewById(mLookupTablePropResourceData.getExtraResourceID("R.id.view_columns"));
		mViewColumns.setOnClickListener(this);

		TextView mLkupTblProp = (TextView) findViewById(mLookupTablePropResourceData.getExtraResourceID("R.id.lookup_table_properties"));

		AndroidResourceHelper.setResourceValues(mLkupTblProp, "LookupTableProperties");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mViewColumns, "ViewColumns");
	}

	private void populateScreen() {
		String lkupTableID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "lkup_table_id");

		String metrixRowID = "";
		String tableName = "";
		String parentTableName = "";
		String parentKeyColumns = "";
		String childKeyColumns = "";

		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup_table.metrix_row_id, mm_field_lkup_table.table_name, mm_field_lkup_table.parent_table_name,");
		query.append(" mm_field_lkup_table.parent_key_columns, mm_field_lkup_table.child_key_columns");
		query.append(" FROM mm_field_lkup_table WHERE mm_field_lkup_table.lkup_table_id = " + lkupTableID);

		mOriginalData = new HashMap<Integer, HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				metrixRowID = cursor.getString(0);
				tableName = cursor.getString(1);
				parentTableName = cursor.getString(2);
				parentKeyColumns = cursor.getString(3);
				childKeyColumns = cursor.getString(4);

				if (MetrixStringHelper.isNullOrEmpty(parentTableName)) parentTableName = "";
				if (MetrixStringHelper.isNullOrEmpty(parentKeyColumns)) parentKeyColumns = "";
				if (MetrixStringHelper.isNullOrEmpty(childKeyColumns)) childKeyColumns = "";

				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_field_lkup_table.metrix_row_id", metrixRowID);
				origRow.put("mm_field_lkup_table.table_name", tableName);
				origRow.put("mm_field_lkup_table.parent_table_name", parentTableName);
				origRow.put("mm_field_lkup_table.parent_key_columns", parentKeyColumns);
				origRow.put("mm_field_lkup_table.child_key_columns", childKeyColumns);
				mOriginalData.put(Integer.valueOf(lkupTableID), origRow);

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
		msgQuery.append(" WHERE c.code_name = 'MM_FIELD_LKUP_TABLE_PROP'");
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
				// COMBOBOX		PARENT_TABLE_NAME
				// LABEL		TABLE_NAME
				// TEXTBOX		PARENT_KEY_COLUMNS, CHILD_KEY_COLUMNS
				LinearLayout layout = null;
				if (MetrixStringHelper.valueIsEqual(propName, "PARENT_TABLE_NAME")) {
					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupTablePropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");

					// populate parent table spinner with any tables that already exist on the current lookup
					String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
					String parentTableQuery = "select distinct table_name from mm_field_lkup_table where lkup_id = " + lkupID;
					MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, parentTableQuery, true);

					try {
						MetrixControlAssistant.setValue(spnPropValue, parentTableName);
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "TABLE_NAME")) {
					// LABEL
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupTablePropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");
					tvPropValue.setText(tableName);
				} else {
					// TEXTBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupTablePropResourceData.getExtraResourceID("R.layout.zzmd_prop_edittext_line"), mTable);
					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");

					if (MetrixStringHelper.valueIsEqual(propName, "PARENT_KEY_COLUMNS"))
						etPropValue.setText(parentKeyColumns);
					else if (MetrixStringHelper.valueIsEqual(propName, "CHILD_KEY_COLUMNS"))
						etPropValue.setText(childKeyColumns);
				}

				// all layouts have these, so set generically
				if (layout != null) {
					TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
					TextView tvLkupTableID = (TextView) layout.findViewWithTag("pv_id");
					TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
					TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

					tvMetrixRowID.setText(metrixRowID);
					tvLkupTableID.setText(lkupTableID);
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
		if (viewId == mLookupTablePropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupTablePropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mLookupTablePropResourceData.getExtraResourceID("R.id.view_columns")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupColumnActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private boolean processAndSaveChanges() {
		try {
			int lookupTablePropChangeCount = 0;
			ArrayList<MetrixSqlData> lookupTableToUpdate = new ArrayList<MetrixSqlData>();

			String lkupTableID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "lkup_table_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_table", "metrix_row_id", "lkup_table_id = " + lkupTableID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_table", "created_revision_id", "lkup_table_id = " + lkupTableID);
			MetrixSqlData data = new MetrixSqlData("mm_field_lkup_table", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("lkup_table_id", lkupTableID));

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
				} else if (MetrixStringHelper.valueIsEqual(controlType, "TEXTBOX")) {
					EditText etPropValue = (EditText) currLayout.findViewWithTag("property_value");
					currentPropValue = etPropValue.getText().toString();
				} else {
					throw new Exception("MDFieldLookupTableProp: Unhandled control type.");
				}

				int currLookupTableIDNum = Integer.valueOf(lkupTableID);
				HashMap<String, String> origRow = mOriginalData.get(currLookupTableIDNum);

				if (MetrixStringHelper.valueIsEqual(propName, "PARENT_TABLE_NAME")) {
					String tableName = origRow.get("mm_field_lkup_table.table_name");
					if (MetrixStringHelper.valueIsEqual(currentPropValue, tableName)) {
						Toast.makeText(this, AndroidResourceHelper.getMessage("LookupTablePropParentTableError"), Toast.LENGTH_LONG).show();
						return false;
					}
				}

				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "PARENT_TABLE_NAME"))
					origPropValue = origRow.get("mm_field_lkup_table.parent_table_name");
				else if (MetrixStringHelper.valueIsEqual(propName, "PARENT_KEY_COLUMNS"))
					origPropValue = origRow.get("mm_field_lkup_table.parent_key_columns");
				else if (MetrixStringHelper.valueIsEqual(propName, "CHILD_KEY_COLUMNS"))
					origPropValue = origRow.get("mm_field_lkup_table.child_key_columns");

				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
					if (MetrixStringHelper.valueIsEqual(propName, "PARENT_TABLE_NAME"))
						data.dataFields.add(new DataField("parent_table_name", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "PARENT_KEY_COLUMNS"))
						data.dataFields.add(new DataField("parent_key_columns", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "CHILD_KEY_COLUMNS"))
						data.dataFields.add(new DataField("child_key_columns", currentPropValue));

					lookupTablePropChangeCount++;
				}
			}

			if (lookupTablePropChangeCount > 0) {
				// upon detecting changes...
				// update modified_revision_id, only if ALL of the following apply:
				// 1) this is not the first revision in the design set
				// 2) this field lookup table has not been added in this revision
				String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
				String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
				String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
				// if previousRevisionID exists, then this is not the first revision
				if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
						&& (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
					// table meets both conditions stated above; update modified_revision_id
					data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
				}

				lookupTableToUpdate.add(data);

				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lookupTableToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("LookupTableChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
}

