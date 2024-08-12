package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MetrixDesignerFieldLookupFilterOrderActivity extends MetrixDesignerActivity {
	private DynamicListView mListView;
	private TextView mEmphasis;
	private Button mSave, mFinish;
	private String mScreenName, mFieldName;
	private LookupFilterOrderAdapter mLookupFilterOrderAdapter;
	private MetrixDesignerResourceData mLookupFilterOrderResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupFilterOrderResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupFilterOrderActivityResourceData");

		setContentView(mLookupFilterOrderResourceData.LayoutResourceID);

		mListView = (DynamicListView) findViewById(mLookupFilterOrderResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupFilterOrderResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupFilterOrderResourceData.getExtraResourceID("R.id.zzmd_field_lookup_filter_order_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupFltrOrd", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mListView.setLongClickable(mAllowChanges);

		mSave = (Button) findViewById(mLookupFilterOrderResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mLookupFilterOrderResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mLkupFilter = (TextView) findViewById(mLookupFilterOrderResourceData.getExtraResourceID("R.id.lookup_filter_order"));

		AndroidResourceHelper.setResourceValues(mLkupFilter, "LookupFilterOrder");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup_filter.metrix_row_id, mm_field_lkup_filter.lkup_filter_id, mm_field_lkup_filter.applied_order, mm_field_lkup_filter.table_name, mm_field_lkup_filter.column_name,");
		query.append(" mm_field_lkup_filter.operator, mm_field_lkup_filter.right_operand, mm_field_lkup_filter.logical_operator, mm_field_lkup_filter.left_parens, mm_field_lkup_filter.right_parens, mm_field_lkup_filter.no_quotes");
		query.append(" FROM mm_field_lkup_filter");
		query.append(" WHERE mm_field_lkup_filter.lkup_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id"));
		query.append(" ORDER BY mm_field_lkup_filter.applied_order ASC");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_field_lkup_filter.metrix_row_id", cursor.getString(0));
				row.put("mm_field_lkup_filter.lkup_filter_id", cursor.getString(1));
				row.put("mm_field_lkup_filter.applied_order", cursor.getString(2));

				String tableName = cursor.getString(3);
				String columnName = cursor.getString(4);
				String operator = MetrixFieldLookupManager.symbolMap.get(cursor.getString(5));
				String rightOperand = cursor.getString(6);
				String logicalOperator = cursor.getString(7);
				String leftParens = cursor.getString(8);
				String rightParens = cursor.getString(9);
				String noQuotes = cursor.getString(10);

				String leftParensString = "";
				if (!MetrixStringHelper.isNullOrEmpty(leftParens) && !MetrixStringHelper.valueIsEqual(leftParens, "0")) {
					int leftParensCount = MetrixFloatHelper.convertNumericFromDBToNumber(leftParens).intValue();
					for (int a = 1; a <= leftParensCount; a++) {
						leftParensString = leftParensString + "(";
					}
				}

				String rightParensString = "";
				if (!MetrixStringHelper.isNullOrEmpty(rightParens) && !MetrixStringHelper.valueIsEqual(rightParens, "0")) {
					int rightParensCount = MetrixFloatHelper.convertNumericFromDBToNumber(rightParens).intValue();
					for (int b = 1; b <= rightParensCount; b++) {
						rightParensString = rightParensString + ")";
					}
				}

				String compositeName = String.format("%1$s.%2$s %3$s", tableName, columnName, operator);
				if (!MetrixStringHelper.isNullOrEmpty(leftParensString))
					compositeName = String.format("%1$s%2$s", leftParensString, compositeName);
				if (!MetrixStringHelper.isNullOrEmpty(rightOperand))
					compositeName = String.format("%1$s {%2$s}", compositeName, ((!MetrixStringHelper.valueIsEqual(noQuotes, "Y")) ? String.format("'%s'", rightOperand) : rightOperand));
				if (!MetrixStringHelper.isNullOrEmpty(logicalOperator))
					compositeName = String.format("%1$s %2$s", logicalOperator.toLowerCase(), compositeName);
				if (!MetrixStringHelper.isNullOrEmpty(rightParensString))
					compositeName = String.format("%1$s%2$s", compositeName, rightParensString);
				row.put("mm_field_lkup_filter.composite_name", compositeName);
				table.add(row);

				cursor.moveToNext();
			}

			mLookupFilterOrderAdapter = new LookupFilterOrderAdapter(this, table, mLookupFilterOrderResourceData.ListViewItemResourceID, mLookupFilterOrderResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setListWithEnabling(table, mAllowChanges);
			mListView.setAdapter(mLookupFilterOrderAdapter);
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mLookupFilterOrderResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupFilterOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mLookupFilterOrderResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				processAndSaveChanges();
			}
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupPropActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void processAndSaveChanges() {
		if (mListView.mListData != null && mListView.mListData.size() > 0) {
			ArrayList<MetrixSqlData> lookupFiltersToUpdate = new ArrayList<MetrixSqlData>();
			int i = 1;
			for (HashMap<String, String> row : mListView.mListData) {
				String oldPosition = row.get("mm_field_lkup_filter.applied_order");
				String newPosition = String.valueOf(i);

				if (!oldPosition.equals(newPosition)) {
					// then we need to do an update
					String listMetrixRowID = row.get("mm_field_lkup_filter.metrix_row_id");
					String listLkupFilterID = row.get("mm_field_lkup_filter.lkup_filter_id");

					MetrixSqlData data = new MetrixSqlData("mm_field_lkup_filter", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
					data.dataFields.add(new DataField("lkup_filter_id", listLkupFilterID));
					data.dataFields.add(new DataField("applied_order", newPosition));
					lookupFiltersToUpdate.add(data);
				}

				i++;
			}

			if (lookupFiltersToUpdate.size() > 0) {
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lookupFiltersToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("LookupFilOrder"), this);
			}
		}
	}

	public static class LookupFilterOrderAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public LookupFilterOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);

				holder = new ViewHolder();
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__metrix_row_id"));
				holder.mLkupFilterID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__lkup_filter_id"));
				holder.mCompositeName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__composite_name"));
				holder.mOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__applied_order"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mMetrixRowID.setText(dataRow.get("mm_field_lkup_filter.metrix_row_id"));
			holder.mLkupFilterID.setText(dataRow.get("mm_field_lkup_filter.lkup_filter_id"));
			holder.mCompositeName.setText(dataRow.get("mm_field_lkup_filter.composite_name"));
			holder.mOrder.setText(dataRow.get("mm_field_lkup_filter.applied_order"));

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mLkupFilterID;
			TextView mCompositeName;
			TextView mOrder;
		}
	}
}
