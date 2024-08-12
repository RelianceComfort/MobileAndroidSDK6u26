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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseSparseArrays")
public class MetrixDesignerFieldLookupOrderbyPropActivity extends MetrixDesignerActivity {
	private HashMap<Integer, HashMap<String, String>> mOriginalData;
	private LinearLayout mTable;
	private TextView mEmphasis;
	private Button mSave, mFinish;
	private String mScreenName, mFieldName;
	private MetrixDesignerResourceData mLookupOrderbyPropResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupOrderbyPropResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupOrderbyPropActivityResourceData");

		setContentView(mLookupOrderbyPropResourceData.LayoutResourceID);

		mTable = (LinearLayout) findViewById(mLookupOrderbyPropResourceData.getExtraResourceID("R.id.table_layout"));
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupOrderbyPropResourceData.HelpTextString;

		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupOrderbyPropResourceData.getExtraResourceID("R.id.zzmd_field_lookup_orderby_prop_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupOrdByProp", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mSave = (Button) findViewById(mLookupOrderbyPropResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mLookupOrderbyPropResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mLkupOrderByProp = (TextView) findViewById(mLookupOrderbyPropResourceData.getExtraResourceID("R.id.lookup_order_by_properties"));

		AndroidResourceHelper.setResourceValues(mLkupOrderByProp, "LookupOrderByProperties");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");
	}

	private void populateScreen() {
		String lkupOrderbyID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_orderby", "lkup_orderby_id");

		String metrixRowID = "";
		String tableName = "";
		String columnName = "";
		String sortOrder = "";

		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup_orderby.metrix_row_id, mm_field_lkup_orderby.table_name, mm_field_lkup_orderby.column_name, mm_field_lkup_orderby.sort_order");
		query.append(" FROM mm_field_lkup_orderby WHERE mm_field_lkup_orderby.lkup_orderby_id = " + lkupOrderbyID);

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
				columnName = cursor.getString(2);
				sortOrder = cursor.getString(3);

				// populate mOriginalData with a row
				HashMap<String, String> origRow = new HashMap<String, String>();
				origRow.put("mm_field_lkup_orderby.metrix_row_id", metrixRowID);
				origRow.put("mm_field_lkup_orderby.table_name", tableName);
				origRow.put("mm_field_lkup_orderby.column_name", columnName);
				origRow.put("mm_field_lkup_orderby.sort_order", sortOrder);
				mOriginalData.put(Integer.valueOf(lkupOrderbyID), origRow);

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
		msgQuery.append(" WHERE c.code_name = 'MM_FIELD_LKUP_ORDERBY_PROP'");
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
				// COMBOBOX			SORT_ORDER
				// LABEL			TABLE_NAME, COLUMN_NAME
				LinearLayout layout = null;
				if (MetrixStringHelper.valueIsEqual(propName, "SORT_ORDER")) {
					// COMBOBOX
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupOrderbyPropResourceData.getExtraResourceID("R.layout.zzmd_prop_spinner_line"), mTable);
					Spinner spnPropValue = (Spinner) layout.findViewWithTag("property_value");

					StringBuilder spinnerQuery = new StringBuilder();
					spinnerQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
					spinnerQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
					spinnerQuery.append("where metrix_code_table.code_name = 'MM_FIELD_LKUP_ORDERBY_SORT_ORDER' ");
					spinnerQuery.append("order by mm_message_def_view.message_text asc");
					MetrixControlAssistant.populateSpinnerFromQuery(this, spnPropValue, spinnerQuery.toString(), true);

					try {
						MetrixControlAssistant.setValue(spnPropValue, sortOrder);
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
					}
				} else if (MetrixStringHelper.valueIsEqual(propName, "TABLE_NAME") || MetrixStringHelper.valueIsEqual(propName, "COLUMN_NAME")) {
					// LABEL
					layout = MetrixControlAssistant.addLinearLayout(this, mLookupOrderbyPropResourceData.getExtraResourceID("R.layout.zzmd_prop_textview_line"), mTable);
					TextView tvPropValue = (TextView) layout.findViewWithTag("property_value");

					if (MetrixStringHelper.valueIsEqual(propName, "TABLE_NAME"))
						tvPropValue.setText(tableName);
					else if (MetrixStringHelper.valueIsEqual(propName, "COLUMN_NAME"))
						tvPropValue.setText(columnName);
				}

				// all layouts have these, so set generically
				if (layout != null) {
					TextView tvMetrixRowID = (TextView) layout.findViewWithTag("metrix_row_id");
					TextView tvLkupOrderbyID = (TextView) layout.findViewWithTag("pv_id");
					TextView tvPropName = (TextView) layout.findViewWithTag("property_name");
					TextView tvPropNameString = (TextView) layout.findViewWithTag("property_name_string");

					tvMetrixRowID.setText(metrixRowID);
					tvLkupOrderbyID.setText(lkupOrderbyID);
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
		if (viewId == mLookupOrderbyPropResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupOrderbyPropActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mLookupOrderbyPropResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				if(!processAndSaveChanges())
					return;
			}
			// allow pass through, even if changes aren't allowed
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupOrderbyActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private boolean processAndSaveChanges() {
		try {
			int lookupOrderbyPropChangeCount = 0;
			ArrayList<MetrixSqlData> lookupOrderbyToUpdate = new ArrayList<MetrixSqlData>();

			String lkupOrderbyID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_orderby", "lkup_orderby_id");
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_orderby", "metrix_row_id", "lkup_orderby_id = " + lkupOrderbyID);
			String createdRevisionID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup_orderby", "created_revision_id", "lkup_orderby_id = " + lkupOrderbyID);
			MetrixSqlData data = new MetrixSqlData("mm_field_lkup_orderby", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowID);
			data.dataFields.add(new DataField("metrix_row_id", metrixRowID));
			data.dataFields.add(new DataField("lkup_orderby_id", lkupOrderbyID));

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
				} else {
					throw new Exception("MDFieldLookupOrderbyProp: Unhandled control type.");
				}

				int currLookupTableIDNum = Integer.valueOf(lkupOrderbyID);
				HashMap<String, String> origRow = mOriginalData.get(currLookupTableIDNum);

				String origPropValue = "";
				if (MetrixStringHelper.valueIsEqual(propName, "SORT_ORDER"))
					origPropValue = origRow.get("mm_field_lkup_orderby.sort_order");

				if (!MetrixStringHelper.valueIsEqual(origPropValue, currentPropValue)) {
					if (MetrixStringHelper.valueIsEqual(propName, "SORT_ORDER"))
						data.dataFields.add(new DataField("sort_order", currentPropValue));

					lookupOrderbyPropChangeCount++;
				}
			}

			if (lookupOrderbyPropChangeCount > 0) {
				// upon detecting changes...
				// first validate that sort_order is not empty (halt save process if this is not the case)
				// the original values should be non-empty by previous validation, so only validate non-null on proposed changes
				for (DataField df : data.dataFields) {
					if (MetrixStringHelper.valueIsEqual(df.name, "sort_order") && MetrixStringHelper.isNullOrEmpty(df.value)) {
						Toast.makeText(this, AndroidResourceHelper.getMessage("LookupOrderbyPropError"), Toast.LENGTH_LONG).show();
						return false;
					}
				}

				// now, update modified_revision_id, only if ALL of the following apply:
				// 1) this is not the first revision in the design set
				// 2) this field lookup filter has not been added in this revision
				String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
				String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
				String previousRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id desc", "1");
				// if previousRevisionID exists, then this is not the first revision
				if (!MetrixStringHelper.isNullOrEmpty(previousRevisionID)
						&& (MetrixStringHelper.isNullOrEmpty(createdRevisionID) || (!MetrixStringHelper.valueIsEqual(createdRevisionID, currentRevisionID)))) {
					// filter meets both conditions stated above; update modified_revision_id
					data.dataFields.add(new DataField("modified_revision_id", currentRevisionID));
				}

				lookupOrderbyToUpdate.add(data);

				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lookupOrderbyToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("LookupOrderByChange"), this);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			Toast.makeText(this, AndroidResourceHelper.getMessage("SaveFailedExThrown"), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
}
