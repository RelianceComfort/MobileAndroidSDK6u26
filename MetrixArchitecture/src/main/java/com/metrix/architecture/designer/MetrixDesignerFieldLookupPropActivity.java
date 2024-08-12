package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
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
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseSparseArrays")
public class MetrixDesignerFieldLookupPropActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mEmphasis;
	private Button mValidate, mTables, mFilters, mOrderBys, mSave;
	private String mScreenName, mFieldName;
	private static MetrixDesignerResourceData mFieldLookupPropResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFieldLookupPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupPropActivityResourceData");

		setContentView(mFieldLookupPropResourceData.LayoutResourceID);

		mTable = (LinearLayout) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.table_layout"));
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mFieldLookupPropResourceData.HelpTextString;

		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.zzmd_field_lookup_prop_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupProp", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mValidate = (Button) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.validate"));
		mTables = (Button) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.view_tables"));
		mFilters = (Button) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.view_filters"));
		mOrderBys = (Button) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.view_orderby"));
		mValidate.setOnClickListener(this);
		mTables.setOnClickListener(this);
		mFilters.setOnClickListener(this);
		mOrderBys.setOnClickListener(this);

        AndroidResourceHelper.setResourceValues(mValidate, "Validate");
        AndroidResourceHelper.setResourceValues(mTables, "Tables");
        AndroidResourceHelper.setResourceValues(mFilters, "Filters");
        AndroidResourceHelper.setResourceValues(mOrderBys, "OrderBy");

		mSave = (Button) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);
        AndroidResourceHelper.setResourceValues(mSave, "Save");

        TextView mTitle = (TextView) findViewById(mFieldLookupPropResourceData.getExtraResourceID("R.id.screen_title"));
        AndroidResourceHelper.setResourceValues(mTitle, "FieldLookupProperties");
    }

	private void populateScreen() {
		String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");

		String metrixRowID = "";
		String title = "";
		String performInitialSearch = "";

		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup.metrix_row_id, mm_field_lkup.title, mm_field_lkup.perform_initial_search");
		query.append(" FROM mm_field_lkup WHERE mm_field_lkup.lkup_id = " + lkupID);

		mOriginalData = new HashMap<Integer, HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				metrixRowID = cursor.getString(0);
				title = cursor.getString(1);
				performInitialSearch = cursor.getString(2);

				if (MetrixStringHelper.isNullOrEmpty(performInitialSearch)) performInitialSearch = "";

				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_field_lkup.metrix_row_id", metrixRowID);
				origRow.put("mm_field_lkup.title", title);
				origRow.put("mm_field_lkup.perform_initial_search", performInitialSearch);
				mOriginalData.put(Integer.valueOf(lkupID), origRow);

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
		msgQuery.append(" WHERE c.code_name = 'MM_FIELD_LKUP_PROP'");
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
				// CHECKBOX				PERFORM_INITIAL_SEARCH
				// MESSAGE LOOKUP		TITLE

				LinearLayout layout = null;
				if (MetrixStringHelper.valueIsEqual(propName, "PERFORM_INITIAL_SEARCH")) {
					// CHECKBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mFieldLookupPropResourceData.getExtraResourceID("R.layout.zzmd_prop_checkbox_line"), mTable);
					CheckBox chkPropValue = (CheckBox) layout.findViewWithTag("property_value");
					if ((MetrixStringHelper.valueIsEqual(propName, "PERFORM_INITIAL_SEARCH") && performInitialSearch.compareToIgnoreCase("Y") == 0))
						chkPropValue.setChecked(true);
					else
						chkPropValue.setChecked(false);
				} else if (MetrixStringHelper.valueIsEqual(propName, "TITLE")) {
					// MESSAGE LOOKUP
					layout = MetrixControlAssistant.addLinearLayout(this, mFieldLookupPropResourceData.getExtraResourceID("R.layout.zzmd_prop_lookup_line"), mTable);
					String messageType = "MM_LOOKUP_TITLE";
					EditText etPropValue = (EditText) layout.findViewWithTag("property_value");
					TextView tvDescLabel = (TextView) layout.findViewWithTag("description_label");
					TextView tvDescText = (TextView) layout.findViewWithTag("description_text");
					etPropValue.setId(MetrixControlAssistant.generateViewId());
					etPropValue.setText(title);
					etPropValue.setOnFocusChangeListener(this);
					etPropValue.addTextChangedListener(new MessageTextWatcher(messageType, tvDescLabel, tvDescText));

					tvDescLabel.setVisibility(View.GONE);
					tvDescText.setVisibility(View.GONE);
					if (!MetrixStringHelper.isNullOrEmpty(title)) {
						String description = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", String.format("message_type = '%1$s' and message_id = '%2$s'", messageType, title));
						if (!MetrixStringHelper.isNullOrEmpty(description)) {
							tvDescText.setText(description);
							tvDescLabel.setVisibility(View.INVISIBLE);
							tvDescText.setVisibility(View.VISIBLE);
						}
					}
				}

				// all layouts have these, so set generically
				if (layout != null) {
					TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
					TextView tvLkupID = (TextView) layout.findViewWithTag("pv_id");
					TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
					TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

					tvMetrixRowID.setText(metrixRowID);
					tvLkupID.setText(lkupID);
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
		if (hasFocus) {
			LinearLayout rowLayout = (LinearLayout) v.getParent();
			doMessageSelection("MM_LOOKUP_TITLE", v.getId(), rowLayout);
		}
	}

	private void doMessageSelection(String messageType, int viewToPopulateId, LinearLayout parentLayout) {
		MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
		lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
		lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
		lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", messageType));

		Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
		intent.putExtra("NoOptionsMenu", true);
		MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
		MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
		startActivityForResult(intent, 2727);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mFieldLookupPropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupPropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mFieldLookupPropResourceData.getExtraResourceID("R.id.validate")) {
			Integer lkupID = Integer.valueOf(MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id"));
			validateLookup(lkupID);
		} else if (viewId == mFieldLookupPropResourceData.getExtraResourceID("R.id.view_tables")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupTableActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (viewId == mFieldLookupPropResourceData.getExtraResourceID("R.id.view_filters")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupFilterActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (viewId == mFieldLookupPropResourceData.getExtraResourceID("R.id.view_orderby")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupOrderbyActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private boolean processAndSaveChanges() {
		try {
			int fieldLookupPropChangeCount = 0;
			ArrayList<MetrixSqlData> fieldLookupToUpdate = new ArrayList<MetrixSqlData>();

			String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup", "metrix_row_id", "lkup_id = " + lkupID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup", "created_revision_id", "lkup_id = " + lkupID);
			MetrixSqlData data = new MetrixSqlData("mm_field_lkup", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("lkup_id", lkupID));

			for (int i = 0; i < mTable.getChildCount(); i++) {
				LinearLayout currLayout = (LinearLayout) mTable.getChildAt(i);
				TextView tvControlType = (TextView) currLayout.findViewWithTag("control_type");
				TextView tvPropName = (TextView) currLayout.findViewWithTag("property_name");

				String controlType = tvControlType.getText().toString();
				String propName = tvPropName.getText().toString();
				String currentPropValue = "";

				if (MetrixStringHelper.valueIsEqual(controlType, "LOOKUP")) {
					EditText etPropValue = (EditText) currLayout.findViewWithTag("property_value");
					currentPropValue = etPropValue.getText().toString();
				} else if (MetrixStringHelper.valueIsEqual(controlType, "CHECKBOX")) {
					CheckBox chkPropValue = (CheckBox) currLayout.findViewWithTag("property_value");
					if (chkPropValue.isChecked())
						currentPropValue = "Y";
					else
						currentPropValue = "N";
				} else {
					throw new Exception("MDFieldLookupProp: Unhandled control type.");
				}

				int currFieldLookupIDNum = Integer.valueOf(lkupID);
				HashMap<String, String> origRow = mOriginalData.get(currFieldLookupIDNum);

				if (MetrixStringHelper.valueIsEqual(propName, "TITLE") && MetrixStringHelper.isNullOrEmpty(currentPropValue)) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("FieldLookupPropTitleError"), Toast.LENGTH_LONG).show();
					return false;
				}

				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "PERFORM_INITIAL_SEARCH"))
					origPropValue = origRow.get("mm_field_lkup.perform_initial_search");
				else if (MetrixStringHelper.valueIsEqual(propName, "TITLE"))
					origPropValue = origRow.get("mm_field_lkup.title");

				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
					if (MetrixStringHelper.valueIsEqual(propName, "PERFORM_INITIAL_SEARCH"))
						data.dataFields.add(new DataField("perform_initial_search", currentPropValue));
					else if (MetrixStringHelper.valueIsEqual(propName, "TITLE"))
						data.dataFields.add(new DataField("title", currentPropValue));

					fieldLookupPropChangeCount++;
				}
			}

			if (fieldLookupPropChangeCount > 0) {
				// upon detecting changes...
				// update modified_revision_id, only if ALL of the following apply:
				// 1) this is not the first revision in the design set
				// 2) this field lookup has not been added in this revision
				String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
				String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
				String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
				// if previousRevisionID exists, then this is not the first revision
				if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
						&& (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
					// field lookup meets both conditions stated above; update modified_revision_id
					data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
				}

				fieldLookupToUpdate.add(data);

				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(fieldLookupToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("FieldLookupChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	/**
	 * Toasts whether the lookup passes basic validation tests.
	 * The lookup must generate a query that can execute successfully (not counting scripts).
	 * The lookup must have at least one table and column.
	 * The lookup must have at least one column on every table.
	 * The lookup must have at least one mm_field_lkup_column with a linked_field_id.
	 *
	 * @param lkupId The unique identifier of the lookup.
	 *
	 * @since 5.6.3
	 */
	private static void validateLookup(Integer lkupId) {
		boolean atLeastOneTableOneColumn = false;
		boolean everyTableHasColumn = false;
		boolean lkupLinkedFieldFound = false;
		String query = "";
		try {
			MetrixLookupDef lookupDef = MetrixFieldLookupManager.generateLookupFromID(lkupId, "", true, false);
			atLeastOneTableOneColumn = lookupHasAtLeastOneTableOneColumn(lkupId);
			if (!atLeastOneTableOneColumn)
				throw new Exception();
			everyTableHasColumn = eachLookupTableHasColumn(lkupId);
			if (!everyTableHasColumn)
				throw new Exception();
			lkupLinkedFieldFound = lookupHasLinkedField(lookupDef);
			if (!lkupLinkedFieldFound)
				throw new Exception();
			query = generateValidationQueryForLookup(lookupDef);
			LogManager.getInstance().info(String.format("ValidateLookup Query: %s", query));

			MetrixCursor cursor = null;
			try {
				cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			} catch (Exception ex) {
				throw ex;
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}

			Toast.makeText(MobileApplication.getAppContext(), AndroidResourceHelper.getMessage("FieldLookupPropValidQuery", query), Toast.LENGTH_LONG).show();
		} catch (Exception ex) {
			String toastString = "";
			if (!atLeastOneTableOneColumn)
				toastString = AndroidResourceHelper.getMessage("FieldLookupPropOneTableOneCol");
			else if (!everyTableHasColumn)
				toastString = AndroidResourceHelper.getMessage("FieldLookupPropOneColPerTable");
			else if (!lkupLinkedFieldFound)
				toastString = AndroidResourceHelper.getMessage("FieldLookupPropOneColLinkField");
			else
				toastString = AndroidResourceHelper.getMessage("FieldLookupPropInvalidQuery", query, ex.getMessage());
			Toast.makeText(MobileApplication.getAppContext(), toastString, Toast.LENGTH_LONG).show();
			LogManager.getInstance().error(toastString);
			LogManager.getInstance().error(ex);
		}
	}

	private static boolean lookupHasAtLeastOneTableOneColumn(Integer lkupId) {
		int tableCount = MetrixDatabaseManager.getCount("mm_field_lkup_table", "lkup_id = " + lkupId);
		int columnCount = MetrixDatabaseManager.getCount("mm_field_lkup_column", String.format("lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %s)", lkupId));
		return (tableCount > 0 && columnCount > 0);
	}

	private static boolean eachLookupTableHasColumn(Integer lkupId) {
		boolean allTablesHaveColumns = true;
		MetrixCursor cursor = null;
		try {
			String query = "select lkup_table_id, table_name from mm_field_lkup_table where lkup_id = " + lkupId;
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					String lkupTableID = cursor.getString(0);
					String tableName = cursor.getString(1).toLowerCase();
					int columnCount = MetrixDatabaseManager.getCount("mm_field_lkup_column", "lkup_table_id = " + lkupTableID);
					if (columnCount < 1) {
						LogManager.getInstance().info(String.format("eachLookupTableHasColumn: %s has no columns defined.", tableName));
						allTablesHaveColumns = false;
					}
					cursor.moveToNext();
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		} finally {
			if (cursor != null && (!cursor.isClosed()))
				cursor.close();
		}

		return allTablesHaveColumns;
	}

	private static boolean lookupHasLinkedField(MetrixLookupDef lookupDef) {
		if (lookupDef == null || lookupDef.columnNames == null || lookupDef.columnNames.size() == 0)
			return false;

		for (MetrixLookupColumnDef columnDef : lookupDef.columnNames) {
			if (columnDef.linkedFieldId > 0)
				return true;
		}

		return false;
	}

	private static String generateValidationQueryForLookup(MetrixLookupDef lookupDef) {
		if (lookupDef == null)
			return "";

		StringBuilder sql = new StringBuilder();
		sql.append("select ");
		for (int i = 0; i < lookupDef.columnNames.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append(lookupDef.columnNames.get(i).columnName);
		}

		sql.append(" from ");
		for (int i = 0; i < lookupDef.tableNames.size(); i++) {
			if (MetrixStringHelper.isNullOrEmpty(lookupDef.tableNames.get(i).parentTableName)) {
				if (i > 0) {
					sql.append(", ");
				}
				sql.append(lookupDef.tableNames.get(i).tableName);
			} else {
				for (int j = 0; j < lookupDef.tableNames.get(i).parentKeyColumns.size(); j++) {
					sql.append(" left join ");
					sql.append(lookupDef.tableNames.get(i).tableName);
					sql.append(" on ");
					sql.append(lookupDef.tableNames.get(i).parentKeyColumns.get(j));
					sql.append("=");
					sql.append(lookupDef.tableNames.get(i).childKeyColumns.get(j));
					sql.append(" ");
				}
			}
		}

		if (lookupDef.filters != null && lookupDef.filters.size() > 0) {
			sql.append(" where ");
			for (int i = 0; i < lookupDef.filters.size(); i++) {
				MetrixLookupFilterDef thisFilter = lookupDef.filters.get(i);
				if (i > 0) {
					if (MetrixStringHelper.isNullOrEmpty(thisFilter.logicalOperator)) {
						sql.append(" and ");
					} else {
						sql.append(" " + thisFilter.logicalOperator + " ");
					}
				}

				if (thisFilter.leftParens > 0) {
					for (int a = 1; a <= thisFilter.leftParens; a++) {
						sql.append("(");
					}
				}

				sql.append(thisFilter.leftOperand);
				sql.append(" " + thisFilter.operator + " ");

				if (!MetrixStringHelper.isNullOrEmpty(thisFilter.rightOperand)) {
					String thisRightOperand = thisFilter.rightOperand;
					ClientScriptDef candidateScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(thisRightOperand);
					if (candidateScriptDef != null)
						thisRightOperand = "123";	// replace script with temporary test value

					if (thisFilter.noQuotes)
						sql.append(thisRightOperand);
					else
						sql.append(String.format("'%s'", thisRightOperand));
				}

				if (thisFilter.rightParens > 0) {
					for (int b = 1; b <= thisFilter.rightParens; b++) {
						sql.append(")");
					}
				}
			}
		}

		if (lookupDef.orderBys.size() == 0) {
			sql.append(" order by " + lookupDef.columnNames.get(0).columnName + " collate nocase");
		} else if (lookupDef.orderBys.size() == 1) {
			sql.append(" order by " + lookupDef.orderBys.get(0).columnName + " collate nocase");
			if (!MetrixStringHelper.isNullOrEmpty(lookupDef.orderBys.get(0).sortOrder)) {
				sql.append(" " + lookupDef.orderBys.get(0).sortOrder);
			}
		} else {
			sql.append(" order by " + lookupDef.orderBys.get(0).columnName + " collate nocase");
			if (!MetrixStringHelper.isNullOrEmpty(lookupDef.orderBys.get(0).sortOrder)) {
				sql.append(" " + lookupDef.orderBys.get(0).sortOrder);
			}

			for (int i = 1; i < lookupDef.orderBys.size(); i++) {
				sql.append(", " + lookupDef.orderBys.get(i).columnName);
				if (!MetrixStringHelper.isNullOrEmpty(lookupDef.orderBys.get(i).sortOrder)) {
					sql.append(" " + lookupDef.orderBys.get(i).sortOrder);
				}
			}
		}

		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			sql.append(" limit " + maxRows);
		}

		return sql.toString();
	}

}
