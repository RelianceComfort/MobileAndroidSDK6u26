package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

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
import com.metrix.architecture.utilities.MetrixPublicCache;

import org.w3c.dom.Text;

public class MetrixDesignerFieldLookupColumnOrderActivity extends MetrixDesignerActivity {
	private DynamicListView mListView;
	private TextView mEmphasis;
	private Button mSave, mFinish;
	private String mScreenName, mFieldName;
	private LookupColumnOrderAdapter mLookupColumnOrderAdapter;
	private MetrixDesignerResourceData mLookupColumnOrderResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupColumnOrderResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupColumnOrderActivityResourceData");

		setContentView(mLookupColumnOrderResourceData.LayoutResourceID);

		mListView = (DynamicListView) findViewById(mLookupColumnOrderResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupColumnOrderResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupColumnOrderResourceData.getExtraResourceID("R.id.zzmd_field_lookup_column_order_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupColOrd", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mListView.setLongClickable(mAllowChanges);

		mSave = (Button) findViewById(mLookupColumnOrderResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mLookupColumnOrderResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mLkupClmnOrder = (TextView) findViewById(mLookupColumnOrderResourceData.getExtraResourceID("R.id.lookup_column_order"));

		AndroidResourceHelper.setResourceValues(mLkupClmnOrder, "LookupColumnOrder");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup_column.metrix_row_id, mm_field_lkup_column.lkup_column_id, mm_field_lkup_column.applied_order, mm_field_lkup_table.table_name, mm_field_lkup_column.column_name");
		query.append(" FROM mm_field_lkup_table JOIN mm_field_lkup_column on mm_field_lkup_table.lkup_table_id = mm_field_lkup_column.lkup_table_id");
		query.append(" WHERE mm_field_lkup_table.lkup_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id"));
		query.append(" AND mm_field_lkup_column.applied_order > 0");
		query.append(" ORDER BY mm_field_lkup_column.applied_order ASC");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_field_lkup_column.metrix_row_id", cursor.getString(0));
				row.put("mm_field_lkup_column.lkup_column_id", cursor.getString(1));
				row.put("mm_field_lkup_column.applied_order", cursor.getString(2));

				String tableName = cursor.getString(3);
				String colName = cursor.getString(4);
				row.put("mm_field_lkup_column.composite_name", tableName + "." + colName);
				table.add(row);

				cursor.moveToNext();
			}

			mLookupColumnOrderAdapter = new LookupColumnOrderAdapter(this, table, mLookupColumnOrderResourceData.ListViewItemResourceID, mLookupColumnOrderResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setListWithEnabling(table, mAllowChanges);
			mListView.setAdapter(mLookupColumnOrderAdapter);
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
		if (viewId == mLookupColumnOrderResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupColumnOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mLookupColumnOrderResourceData.getExtraResourceID("R.id.finish")) {
			if (mAllowChanges) {
				processAndSaveChanges();
			}
			// allow pass through, even if changes aren't allowed (rolling back to Field Lookup Table List)
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupTableActivity.class);
			intent.putExtra("headingText", mHeadingText);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private void processAndSaveChanges() {
		if (mListView.mListData != null && mListView.mListData.size() > 0) {
			ArrayList<MetrixSqlData> lookupColumnsToUpdate = new ArrayList<MetrixSqlData>();
			int i = 1;
			for (HashMap<String, String> row : mListView.mListData) {
				String oldPosition = row.get("mm_field_lkup_column.applied_order");
				String newPosition = String.valueOf(i);

				if (!oldPosition.equals(newPosition)) {
					// then we need to do an update
					String listMetrixRowID = row.get("mm_field_lkup_column.metrix_row_id");
					String listLkupColumnID = row.get("mm_field_lkup_column.lkup_column_id");

					MetrixSqlData data = new MetrixSqlData("mm_field_lkup_column", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
					data.dataFields.add(new DataField("lkup_column_id", listLkupColumnID));
					data.dataFields.add(new DataField("applied_order", newPosition));
					lookupColumnsToUpdate.add(data);
				}

				i++;
			}

			if (lookupColumnsToUpdate.size() > 0) {
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lookupColumnsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("LookupColOrder"), this);
			}
		}
	}

	public static class LookupColumnOrderAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public LookupColumnOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);

				holder = new ViewHolder();
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__metrix_row_id"));
				holder.mLkupColumnID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__lkup_column_id"));
				holder.mCompositeName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__composite_name"));
				holder.mOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_column__applied_order"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mMetrixRowID.setText(dataRow.get("mm_field_lkup_column.metrix_row_id"));
			holder.mLkupColumnID.setText(dataRow.get("mm_field_lkup_column.lkup_column_id"));
			holder.mCompositeName.setText(dataRow.get("mm_field_lkup_column.composite_name"));
			holder.mOrder.setText(dataRow.get("mm_field_lkup_column.applied_order"));

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mLkupColumnID;
			TextView mCompositeName;
			TextView mOrder;
		}
	}
}

