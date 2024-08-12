package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixUpdateMessage;
import com.metrix.architecture.metadata.MetrixUpdateMessage.MetrixUpdateMessageTransactionType;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class MetrixDesignerFieldLookupFilterActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private TextView mEmphasis;
	private Button mAddFilter, mFilterOrder;
	private String mScreenName, mFieldName;
	private ListView mListView;
	private AlertDialog mDeleteLookupFilterDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private LookupFilterListAdapter mLookupFilterAdapter;
	private MetrixDesignerResourceData mLookupFilterResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupFilterResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupFilterActivityResourceData");

		setContentView(mLookupFilterResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mLookupFilterResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupFilterResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupFilterResourceData.getExtraResourceID("R.id.zzmd_field_lookup_filter_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupFltr", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mAddFilter = (Button) findViewById(mLookupFilterResourceData.getExtraResourceID("R.id.add_filter"));
		mAddFilter.setEnabled(mAllowChanges);
		mAddFilter.setOnClickListener(this);

		mFilterOrder = (Button) findViewById(mLookupFilterResourceData.getExtraResourceID("R.id.filter_order"));
		mFilterOrder.setOnClickListener(this);

		TextView mLkupFilters = (TextView) findViewById(mLookupFilterResourceData.getExtraResourceID("R.id.lookup_filters"));

		AndroidResourceHelper.setResourceValues(mLkupFilters, "LookupFilters");
		AndroidResourceHelper.setResourceValues(mAddFilter, "AddFilter");
		AndroidResourceHelper.setResourceValues(mFilterOrder, "FilterOrder");

		populateList();

		mListView.setOnItemClickListener(this);
		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mLookupFilterAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;
					if (!mAllowChanges) {
						return false;
					} else {
						setSelectedItemToDelete(selectedItem);
						mDeleteLookupFilterDialog = new AlertDialog.Builder(MetrixDesignerFieldLookupFilterActivity.this).create();
						mDeleteLookupFilterDialog.setMessage(AndroidResourceHelper.getMessage("LookupFilterDeleteConfirm"));
						mDeleteLookupFilterDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteLookupFilterListener);
						mDeleteLookupFilterDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteLookupFilterListener);
						mDeleteLookupFilterDialog.show();
						return true;
					}
				}
			});
		}
	}

	private void populateList() {
		String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
		String currentLkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");

		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_field_lkup_filter.metrix_row_id, mm_field_lkup_filter.lkup_filter_id, mm_field_lkup_filter.table_name,");
		query.append(" mm_field_lkup_filter.column_name, mm_field_lkup_filter.operator, mm_field_lkup_filter.right_operand,");
		query.append(" mm_field_lkup_filter.logical_operator, mm_field_lkup_filter.left_parens,");
		query.append(" mm_field_lkup_filter.right_parens, mm_field_lkup_filter.no_quotes, mm_field_lkup_filter.created_revision_id");
		query.append(" from mm_field_lkup_filter where mm_field_lkup_filter.lkup_id = " + currentLkupID);
		query.append(" order by mm_field_lkup_filter.table_name, mm_field_lkup_filter.column_name asc");

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
				String tableName = cursor.getString(2);
				String columnName = cursor.getString(3);
				String operator = MetrixFieldLookupManager.symbolMap.get(cursor.getString(4));
				String rightOperand = cursor.getString(5);
				String logicalOperator = cursor.getString(6);
				String leftParens = cursor.getString(7);
				String rightParens = cursor.getString(8);
				String noQuotes = cursor.getString(9);

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

				String rawCreatedRevisionID = cursor.getString(10);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
					isBaseline = false;
					if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
				}
				row.put("mm_field_lkup_filter.is_baseline", String.valueOf(isBaseline));
				row.put("mm_field_lkup_filter.added_this_revision", String.valueOf(addedThisRevision));

				table.add(row);
				cursor.moveToNext();
			}

			mLookupFilterAdapter = new LookupFilterListAdapter(this, table, mLookupFilterResourceData.ListViewItemResourceID, mLookupFilterResourceData.ExtraResourceIDs);
			mListView.setAdapter(mLookupFilterAdapter);
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
		if (viewId == mLookupFilterResourceData.getExtraResourceID("R.id.add_filter")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupFilterAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (viewId == mLookupFilterResourceData.getExtraResourceID("R.id.filter_order")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupFilterOrderActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mLookupFilterAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String lkupFilterID = selectedItem.get("mm_field_lkup_filter.lkup_filter_id");
		MetrixCurrentKeysHelper.setKeyValue("mm_field_lkup_filter", "lkup_filter_id", lkupFilterID);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupFilterPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener deleteLookupFilterListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (deleteSelectedLookupFilterAndChildren()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupFilterActivity.this, MetrixDesignerFieldLookupFilterActivity.class);
						intent.putExtra("headingText", mHeadingText);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerFieldLookupFilterActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private boolean deleteSelectedLookupFilterAndChildren() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_field_lkup_filter.metrix_row_id");
			String currLkupFilterID = mSelectedItemToDelete.get("mm_field_lkup_filter.lkup_filter_id");

			// generate a deletion transaction for mm_field_lkup_filter ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_field_lkup_filter", MetrixUpdateMessageTransactionType.Delete, "lkup_filter_id", currLkupFilterID);

			MetrixDatabaseManager.begintransaction();

			// delete all child records in DB (all 3 tables), without doing a message transaction
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_filter_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter", String.format("lkup_filter_id = %s", currLkupFilterID));

			if (success)
				success = message.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_field_lkup_filter with lkup_filter_id = " + currLkupFilterID);

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

	private void setSelectedItemToDelete(HashMap<String, String> item) {
		mSelectedItemToDelete = item;
	}

	public static class LookupFilterListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mBlankIcon;

		public LookupFilterListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);

			mNewAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_blue16x16"));
			mOldAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_gray16x16"));
			mBlankIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.transparent"));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mCompositeName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__composite_name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__metrix_row_id"));
				holder.mLkupFilterID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__lkup_filter_id"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_filter__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mCompositeName.setText(dataRow.get("mm_field_lkup_filter.composite_name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_field_lkup_filter.metrix_row_id"));
			holder.mLkupFilterID.setText(dataRow.get("mm_field_lkup_filter.lkup_filter_id"));

			boolean isBaseline = Boolean.valueOf(dataRow.get("mm_field_lkup_filter.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

			boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_field_lkup_filter.added_this_revision"));

			// determine which added state icon to use
			if (isBaseline) {
				holder.mAddedIcon.setImageBitmap(mBlankIcon);
			} else if (isAddedThisRev) {
				holder.mAddedIcon.setImageBitmap(mNewAddedIcon);
			} else {
				holder.mAddedIcon.setImageBitmap(mOldAddedIcon);
			}

			return vi;
		}

		static class ViewHolder {
			TextView mCompositeName;
			TextView mMetrixRowID;
			TextView mLkupFilterID;
			TextView mIsBaseline;
			ImageView mAddedIcon;
		}
	}
}

