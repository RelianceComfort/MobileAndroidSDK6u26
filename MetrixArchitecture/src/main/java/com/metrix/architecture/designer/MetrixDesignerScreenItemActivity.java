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

public class MetrixDesignerScreenItemActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private ListView mListView;
	private AlertDialog mDeleteScreenItemDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private Button mAddScreenItem;
	private TextView mTitle;
	private String mScreenName;
	private static String mNoDescString;
	private ScreenItemListAdapter mScreenItemAdapter;
	private MetrixDesignerResourceData mScreenItemResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mScreenItemResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerScreenItemActivityResourceData");

		setContentView(mScreenItemResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mScreenItemResourceData.ListViewResourceID);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mScreenItemResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mTitle = (TextView) findViewById(mScreenItemResourceData.getExtraResourceID("R.id.zzmd_screen_item_title"));
		String fullTitle = AndroidResourceHelper.getMessage("Items1Args", mScreenName);
		mTitle.setText(fullTitle);

		mAddScreenItem = (Button) findViewById(mScreenItemResourceData.getExtraResourceID("R.id.add_screen_item"));
		setAddScreenItemEnabling();
		mAddScreenItem.setOnClickListener(this);

		mNoDescString = AndroidResourceHelper.getMessage("NoDescriptionFound");

		TextView mScrInfo = (TextView) findViewById(mScreenItemResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_screen_item"));

		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesScnItm");
		AndroidResourceHelper.setResourceValues(mAddScreenItem, "AddScreenItem");

		populateList();

		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mScreenItemAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;

					boolean isBaseline = Boolean.valueOf(selectedItem.get("mm_screen_item.is_baseline"));
					if (isBaseline || !mAllowChanges) {
						return false;
					} else {
						setSelectedItemToDelete(selectedItem);
						mDeleteScreenItemDialog = new AlertDialog.Builder(MetrixDesignerScreenItemActivity.this).create();
						mDeleteScreenItemDialog.setMessage(AndroidResourceHelper.getMessage("ScnItmDelConfirm"));
						mDeleteScreenItemDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteScreenItemListener);
						mDeleteScreenItemDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteScreenItemListener);
						mDeleteScreenItemDialog.show();
						return true;
					}
				}
			});
		}
	}

	private void setAddScreenItemEnabling() {
		// only enable if changes allowed and this screen has item names available
		if (mAllowChanges) {
			String itemNameWhereClause = String.format("code_name = 'MM_SCREEN_ITEM_ITEM_NAME' and code_value not in (select distinct item_name from mm_screen_item where screen_id = %s)", MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
			int availableNames = MetrixDatabaseManager.getCount("metrix_code_table", itemNameWhereClause);
			if (availableNames > 0) {
				mAddScreenItem.setEnabled(true);
				return;
			}
		}

		mAddScreenItem.setEnabled(false);
	}

	private void populateList() {
		String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
		String currentScreenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");

		// now, with tracking data in place, do the actual list selection
		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_screen_item.metrix_row_id, mm_screen_item.item_id, mm_screen_item.item_name, mm_screen_item.description, mm_screen_item.created_revision_id, mm_screen_item.modified_revision_id");
		query.append(" from mm_screen_item where mm_screen_item.screen_id = " + currentScreenID);
		query.append(" order by mm_screen_item.item_name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_screen_item.metrix_row_id", cursor.getString(0));
				row.put("mm_screen_item.item_id", cursor.getString(1));
				row.put("mm_screen_item.item_name", cursor.getString(2));

				String rawText = cursor.getString(3);
				if (MetrixStringHelper.isNullOrEmpty(rawText)) {
					rawText = mNoDescString;
				} else if (rawText.length() > 40) {
					rawText = rawText.substring(0, 39) + "...";
				}
				row.put("mm_screen_item.description", rawText);

				String rawCreatedRevisionID = cursor.getString(4);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
					isBaseline = false;
					if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
				}
				row.put("mm_screen_item.is_baseline", String.valueOf(isBaseline));
				row.put("mm_screen_item.added_this_revision", String.valueOf(addedThisRevision));

				String rawModifiedRevisionID = cursor.getString(5);
				boolean editedPrevRevision = false;
				boolean editedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawModifiedRevisionID)) {
					if (MetrixStringHelper.valueIsEqual(rawModifiedRevisionID, currentRevisionID))
						editedThisRevision = true;
					else
						editedPrevRevision = true;
				}
				row.put("mm_screen_item.edited_prev_revision", String.valueOf(editedPrevRevision));
				row.put("mm_screen_item.edited_this_revision", String.valueOf(editedThisRevision));

				table.add(row);
				cursor.moveToNext();
			}

			mScreenItemAdapter = new ScreenItemListAdapter(this, table, mScreenItemResourceData.ListViewItemResourceID, mScreenItemResourceData.ExtraResourceIDs);
			mListView.setAdapter(mScreenItemAdapter);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mScreenItemResourceData.getExtraResourceID("R.id.add_screen_item")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenItemAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mScreenItemAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String itemId = selectedItem.get("mm_screen_item.item_id");
		String itemName = selectedItem.get("mm_screen_item.item_name");
		MetrixCurrentKeysHelper.setKeyValue("mm_screen_item", "item_id", itemId);
		MetrixCurrentKeysHelper.setKeyValue("mm_screen_item", "item_name", itemName);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenItemPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener deleteScreenItemListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (deleteSelectedScreenItem()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerScreenItemActivity.this, MetrixDesignerScreenItemActivity.class);
						intent.putExtra("headingText", mHeadingText);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerScreenItemActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private boolean deleteSelectedScreenItem() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_screen_item.metrix_row_id");
			String currItemID = mSelectedItemToDelete.get("mm_screen_item.item_id");

			// generate a deletion transaction for mm_screen_item ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_screen_item", MetrixUpdateMessageTransactionType.Delete, "item_id", currItemID);

			MetrixDatabaseManager.begintransaction();

			// delete all records in DB (all 3 tables), without doing a message transaction
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_screen_item_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_screen_item_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_screen_item", String.format("item_id = %s", currItemID));

			if (success)
				success = message.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_screen_item with item_id = " + currItemID);

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

	public static class ScreenItemListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mNewEditedIcon, mOldEditedIcon, mBlankIcon;

		public ScreenItemListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);

			mNewAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_blue16x16"));
			mOldAddedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.circle_gray16x16"));
			mNewEditedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.pencil_purple16x16"));
			mOldEditedIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.pencil_gray16x16"));
			mBlankIcon = BitmapFactory.decodeResource(MetrixPublicCache.instance.getApplicationContext().getResources(), mListViewItemElementResourceIDs.get("R.drawable.transparent"));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen_item__item_name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen_item__metrix_row_id"));
				holder.mItemID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen_item__item_id"));
				holder.mDescription = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen_item__description"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen_item__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));
				holder.mEditedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_edited_icon"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mName.setText(dataRow.get("mm_screen_item.item_name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_screen_item.metrix_row_id"));
			holder.mItemID.setText(dataRow.get("mm_screen_item.item_id"));

			String currentDescText = dataRow.get("mm_screen_item.description");
			holder.mDescription.setText(currentDescText);
			if (MetrixStringHelper.valueIsEqual(currentDescText, mNoDescString)) {
				holder.mDescription.setVisibility(View.GONE);
			} else {
				holder.mDescription.setVisibility(View.VISIBLE);
			}

			boolean isBaseline = Boolean.valueOf(dataRow.get("mm_screen_item.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

			boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_screen_item.added_this_revision"));
			boolean isEditedThisRev = Boolean.valueOf(dataRow.get("mm_screen_item.edited_this_revision"));
			boolean isEditedPrevRev = Boolean.valueOf(dataRow.get("mm_screen_item.edited_prev_revision"));

			// determine which added state icon to use
			if (isBaseline) {
				holder.mAddedIcon.setImageBitmap(mBlankIcon);
			} else if (isAddedThisRev) {
				holder.mAddedIcon.setImageBitmap(mNewAddedIcon);
			} else {
				holder.mAddedIcon.setImageBitmap(mOldAddedIcon);
			}

			// determine which edited state icon to use
			if (isEditedThisRev) {
				holder.mEditedIcon.setImageBitmap(mNewEditedIcon);
			} else if (isEditedPrevRev) {
				holder.mEditedIcon.setImageBitmap(mOldEditedIcon);
			} else {
				holder.mEditedIcon.setImageBitmap(mBlankIcon);
			}

			return vi;
		}

		static class ViewHolder {
			TextView mName;
			TextView mMetrixRowID;
			TextView mItemID;
			TextView mDescription;
			TextView mIsBaseline;
			ImageView mAddedIcon;
			ImageView mEditedIcon;
		}
	}
}

