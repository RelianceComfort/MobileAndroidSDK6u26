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
import com.metrix.architecture.utilities.MetrixPublicCache;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MetrixDesignerFieldLookupOrderbyOrderActivity extends MetrixDesignerActivity {
	private DynamicListView mListView;
	private TextView mEmphasis;
	private Button mSave, mFinish;
	private String mScreenName, mFieldName;
	private LookupOrderbyOrderAdapter mLookupOrderbyOrderAdapter;
	private MetrixDesignerResourceData mLookupOrderbyOrderResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupOrderbyOrderResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupOrderbyOrderActivityResourceData");

		setContentView(mLookupOrderbyOrderResourceData.LayoutResourceID);

		mListView = (DynamicListView) findViewById(mLookupOrderbyOrderResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupOrderbyOrderResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupOrderbyOrderResourceData.getExtraResourceID("R.id.zzmd_field_lookup_orderby_order_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupOrdByOrd", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mListView.setLongClickable(mAllowChanges);

		mSave = (Button) findViewById(mLookupOrderbyOrderResourceData.getExtraResourceID("R.id.save"));
		mSave.setEnabled(mAllowChanges);
		mSave.setOnClickListener(this);

		mFinish = (Button) findViewById(mLookupOrderbyOrderResourceData.getExtraResourceID("R.id.finish"));
		mFinish.setOnClickListener(this);

		TextView mLkupOrderByOrder = (TextView) findViewById(mLookupOrderbyOrderResourceData.getExtraResourceID("R.id.lookup_order_by_order"));

		AndroidResourceHelper.setResourceValues(mLkupOrderByOrder, "LookupOrderByOrder");
		AndroidResourceHelper.setResourceValues(mSave, "Save");
		AndroidResourceHelper.setResourceValues(mFinish, "Finish");

		populateList();
	}

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("SELECT mm_field_lkup_orderby.metrix_row_id, mm_field_lkup_orderby.lkup_orderby_id, mm_field_lkup_orderby.applied_order,");
		query.append(" mm_field_lkup_orderby.table_name, mm_field_lkup_orderby.column_name, mm_field_lkup_orderby.sort_order");
		query.append(" FROM mm_field_lkup_orderby");
		query.append(" WHERE mm_field_lkup_orderby.lkup_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id"));
		query.append(" ORDER BY mm_field_lkup_orderby.applied_order ASC");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_field_lkup_orderby.metrix_row_id", cursor.getString(0));
				row.put("mm_field_lkup_orderby.lkup_orderby_id", cursor.getString(1));
				row.put("mm_field_lkup_orderby.applied_order", cursor.getString(2));

				String tableName = cursor.getString(3);
				String columnName = cursor.getString(4);
				String sortOrder = cursor.getString(5);

				String compositeName = String.format("%1$s.%2$s %3$s", tableName, columnName, sortOrder.toLowerCase());
				row.put("mm_field_lkup_orderby.composite_name", compositeName);
				table.add(row);

				cursor.moveToNext();
			}

			mLookupOrderbyOrderAdapter = new LookupOrderbyOrderAdapter(this, table, mLookupOrderbyOrderResourceData.ListViewItemResourceID, mLookupOrderbyOrderResourceData.ExtraResourceIDs, mAllowChanges);
			mListView.setListWithEnabling(table, mAllowChanges);
			mListView.setAdapter(mLookupOrderbyOrderAdapter);
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
		if (viewId == mLookupOrderbyOrderResourceData.getExtraResourceID("R.id.save")) {
			if (mAllowChanges) {
				processAndSaveChanges();

				Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupOrderbyOrderActivity.class);
				intent.putExtra("headingText", mHeadingText);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		} else if (viewId == mLookupOrderbyOrderResourceData.getExtraResourceID("R.id.finish")) {
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
			ArrayList<MetrixSqlData> lookupOrderbysToUpdate = new ArrayList<MetrixSqlData>();
			int i = 1;
			for (HashMap<String, String> row : mListView.mListData) {
				String oldPosition = row.get("mm_field_lkup_orderby.applied_order");
				String newPosition = String.valueOf(i);

				if (!oldPosition.equals(newPosition)) {
					// then we need to do an update
					String listMetrixRowID = row.get("mm_field_lkup_orderby.metrix_row_id");
					String listLkupOrderbyID = row.get("mm_field_lkup_orderby.lkup_orderby_id");

					MetrixSqlData data = new MetrixSqlData("mm_field_lkup_orderby", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
					data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
					data.dataFields.add(new DataField("lkup_orderby_id", listLkupOrderbyID));
					data.dataFields.add(new DataField("applied_order", newPosition));
					lookupOrderbysToUpdate.add(data);
				}

				i++;
			}

			if (lookupOrderbysToUpdate.size() > 0) {
				MetrixTransaction transactionInfo = new MetrixTransaction();
				MetrixUpdateManager.update(lookupOrderbysToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("LookupOrderBy_Order"), this);
			}
		}
	}

	public static class LookupOrderbyOrderAdapter extends DynamicListAdapter {
		static ViewHolder holder;

		public LookupOrderbyOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
			super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);

				holder = new ViewHolder();
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__metrix_row_id"));
				holder.mLkupOrderbyID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__lkup_orderby_id"));
				holder.mCompositeName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__composite_name"));
				holder.mOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__applied_order"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mMetrixRowID.setText(dataRow.get("mm_field_lkup_orderby.metrix_row_id"));
			holder.mLkupOrderbyID.setText(dataRow.get("mm_field_lkup_orderby.lkup_orderby_id"));
			holder.mCompositeName.setText(dataRow.get("mm_field_lkup_orderby.composite_name"));
			holder.mOrder.setText(dataRow.get("mm_field_lkup_orderby.applied_order"));

			return vi;
		}

		static class ViewHolder {
			TextView mMetrixRowID;
			TextView mLkupOrderbyID;
			TextView mCompositeName;
			TextView mOrder;
		}
	}
}
