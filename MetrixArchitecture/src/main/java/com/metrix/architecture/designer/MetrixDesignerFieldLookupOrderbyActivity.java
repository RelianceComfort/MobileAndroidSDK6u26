package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixUpdateMessage;
import com.metrix.architecture.metadata.MetrixUpdateMessage.MetrixUpdateMessageTransactionType;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class MetrixDesignerFieldLookupOrderbyActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private TextView mEmphasis;
	private Button mAddOrderby, mOrderbyOrder;
	private String mScreenName, mFieldName;
	private ListView mListView;
	private AlertDialog mDeleteLookupOrderbyDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private LookupOrderbyListAdapter mLookupOrderbyAdapter;
	private MetrixDesignerResourceData mLookupOrderbyResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupOrderbyResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupOrderbyActivityResourceData");

		setContentView(mLookupOrderbyResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mLookupOrderbyResourceData.ListViewResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupOrderbyResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupOrderbyResourceData.getExtraResourceID("R.id.zzmd_field_lookup_orderby_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupOrdBy", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mAddOrderby = (Button) findViewById(mLookupOrderbyResourceData.getExtraResourceID("R.id.add_orderby"));
		mAddOrderby.setEnabled(mAllowChanges);
		mAddOrderby.setOnClickListener(this);

		mOrderbyOrder = (Button) findViewById(mLookupOrderbyResourceData.getExtraResourceID("R.id.orderby_order"));
		mOrderbyOrder.setOnClickListener(this);

		TextView mLkupOrderBy = (TextView) findViewById(mLookupOrderbyResourceData.getExtraResourceID("R.id.lookup_order_by"));

		AndroidResourceHelper.setResourceValues(mLkupOrderBy, "LookupOrderBy");
		AndroidResourceHelper.setResourceValues(mAddOrderby, "AddOrderBy");
		AndroidResourceHelper.setResourceValues(mOrderbyOrder, "OrderByOrder");

		populateList();

		mListView.setOnItemClickListener(this);
		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mLookupOrderbyAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;
					if (!mAllowChanges) {
						return false;
					} else {
						setSelectedItemToDelete(selectedItem);
						mDeleteLookupOrderbyDialog = new AlertDialog.Builder(MetrixDesignerFieldLookupOrderbyActivity.this).create();
						mDeleteLookupOrderbyDialog.setMessage(AndroidResourceHelper.getMessage("LookupOrderbyDeleteConfirm"));
						mDeleteLookupOrderbyDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteLookupOrderbyListener);
						mDeleteLookupOrderbyDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteLookupOrderbyListener);
						mDeleteLookupOrderbyDialog.show();
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
		query.append("select distinct mm_field_lkup_orderby.metrix_row_id, mm_field_lkup_orderby.lkup_orderby_id, mm_field_lkup_orderby.table_name,");
		query.append(" mm_field_lkup_orderby.column_name, mm_field_lkup_orderby.sort_order, mm_field_lkup_orderby.created_revision_id");
		query.append(" from mm_field_lkup_orderby where mm_field_lkup_orderby.lkup_id = " + currentLkupID);
		query.append(" order by mm_field_lkup_orderby.table_name, mm_field_lkup_orderby.column_name asc");

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
				String tableName = cursor.getString(2);
				String columnName = cursor.getString(3);
				String sortOrder = cursor.getString(4);

				String compositeName = String.format("%1$s.%2$s %3$s", tableName, columnName, sortOrder.toLowerCase());
				row.put("mm_field_lkup_orderby.composite_name", compositeName);

				String rawCreatedRevisionID = cursor.getString(5);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
					isBaseline = false;
					if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
				}
				row.put("mm_field_lkup_orderby.is_baseline", String.valueOf(isBaseline));
				row.put("mm_field_lkup_orderby.added_this_revision", String.valueOf(addedThisRevision));

				table.add(row);
				cursor.moveToNext();
			}

			mLookupOrderbyAdapter = new LookupOrderbyListAdapter(this, table, mLookupOrderbyResourceData.ListViewItemResourceID, mLookupOrderbyResourceData.ExtraResourceIDs);
			mListView.setAdapter(mLookupOrderbyAdapter);
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
		if (viewId == mLookupOrderbyResourceData.getExtraResourceID("R.id.add_orderby")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupOrderbyAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		} else if (viewId == mLookupOrderbyResourceData.getExtraResourceID("R.id.orderby_order")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupOrderbyOrderActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mLookupOrderbyAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String lkupOrderbyID = selectedItem.get("mm_field_lkup_orderby.lkup_orderby_id");
		MetrixCurrentKeysHelper.setKeyValue("mm_field_lkup_orderby", "lkup_orderby_id", lkupOrderbyID);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFieldLookupOrderbyPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener deleteLookupOrderbyListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (deleteSelectedLookupOrderbyAndChildren()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupOrderbyActivity.this, MetrixDesignerFieldLookupOrderbyActivity.class);
						intent.putExtra("headingText", mHeadingText);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerFieldLookupOrderbyActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private boolean deleteSelectedLookupOrderbyAndChildren() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_field_lkup_orderby.metrix_row_id");
			String currLkupOrderbyID = mSelectedItemToDelete.get("mm_field_lkup_orderby.lkup_orderby_id");

			// generate a deletion transaction for mm_field_lkup_orderby ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_field_lkup_orderby", MetrixUpdateMessageTransactionType.Delete, "lkup_orderby_id", currLkupOrderbyID);

			MetrixDatabaseManager.begintransaction();

			// delete all child records in DB (all 3 tables), without doing a message transaction
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_orderby_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby", String.format("lkup_orderby_id = %s", currLkupOrderbyID));

			if (success)
				success = message.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_field_lkup_orderby with lkup_orderby_id = " + currLkupOrderbyID);

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

	public static class LookupOrderbyListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mBlankIcon;

		public LookupOrderbyListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
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
				holder.mCompositeName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__composite_name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__metrix_row_id"));
				holder.mLkupOrderbyID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__lkup_orderby_id"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field_lkup_orderby__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mCompositeName.setText(dataRow.get("mm_field_lkup_orderby.composite_name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_field_lkup_orderby.metrix_row_id"));
			holder.mLkupOrderbyID.setText(dataRow.get("mm_field_lkup_orderby.lkup_orderby_id"));

			boolean isBaseline = Boolean.valueOf(dataRow.get("mm_field_lkup_orderby.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

			boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_field_lkup_orderby.added_this_revision"));

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
			TextView mLkupOrderbyID;
			TextView mIsBaseline;
			ImageView mAddedIcon;
		}
	}
}

