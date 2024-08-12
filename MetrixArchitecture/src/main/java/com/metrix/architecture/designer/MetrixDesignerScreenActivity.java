package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

public class MetrixDesignerScreenActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private ListView mListView;
	private AlertDialog mDeleteScreenDialog;
	private HashMap<String, String> mSelectedItemToDelete;
	private Button mAddScreen;
	private static String mNoDescString;
	private ScreenListAdapter mScreenAdapter;
	private MetrixDesignerResourceData mScreenResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mScreenResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerScreenActivityResourceData");

		setContentView(mScreenResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mScreenResourceData.ListViewResourceID);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mScreenResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mAddScreen = (Button) findViewById(mScreenResourceData.getExtraResourceID("R.id.add_screen"));
		mAddScreen.setEnabled(mAllowChanges);
		mAddScreen.setOnClickListener(this);

		mNoDescString = AndroidResourceHelper.getMessage("NoDescriptionFound");

		TextView mScr = (TextView) findViewById(mScreenResourceData.getExtraResourceID("R.id.screens"));
		TextView mScrInfo = (TextView) findViewById(mScreenResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_screen"));

		AndroidResourceHelper.setResourceValues(mScr, "Screens");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesScn");
		AndroidResourceHelper.setResourceValues(mAddScreen, "AddScreen");

		populateList();

		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Object item = mScreenAdapter.getItem(position);
					@SuppressWarnings("unchecked")
					HashMap<String, String> selectedItem = (HashMap<String, String>) item;

					boolean isBaseline = Boolean.valueOf(selectedItem.get("mm_screen.is_baseline"));
					if (isBaseline || !mAllowChanges) {
						return false;
					} else {
						setSelectedItemToDelete(selectedItem);
						if(!validateBeforeScreenDelete())
							return true;
						else
						{
							String deleteDialogMsg = "";
							if (selectedScreenIsTabParent()) {
								deleteDialogMsg = generateTabParentDeleteMsg();
							} else
								deleteDialogMsg = AndroidResourceHelper.getMessage("ScreenDeleteConfirm");

							mDeleteScreenDialog = new AlertDialog.Builder(MetrixDesignerScreenActivity.this).create();
							mDeleteScreenDialog.setMessage(deleteDialogMsg);
							mDeleteScreenDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteScreenListener);
							mDeleteScreenDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteScreenListener);
							mDeleteScreenDialog.show();
							return true;
						}
					}
				}
			});
		}

		if (this.getIntent().getExtras().containsKey("targetDesignerActivity") && !mProcessedTargetIntent) {
			String screenId = (String) this.getIntent().getExtras().get("targetDesignerScreenID");
			String screenName = (String) this.getIntent().getExtras().get("targetDesignerScreenName");
			MetrixCurrentKeysHelper.setKeyValue("mm_screen", "screen_id", screenId);
			MetrixCurrentKeysHelper.setKeyValue("mm_screen", "screen_name", screenName);

			// all available options going to screen prop
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenPropActivity.class);
			intent.putExtra("headingText", mHeadingText);

			String targetDesignerActivity = (String) this.getIntent().getExtras().get("targetDesignerActivity");
			if (!MetrixStringHelper.valueIsEqual(targetDesignerActivity, "MetrixDesignerScreenPropActivity")) {
				intent.putExtra("targetDesignerActivity", targetDesignerActivity);
			}

			mProcessedTargetIntent = true;

			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	private boolean validateBeforeScreenDelete() {
		String currScreenID = mSelectedItemToDelete.get("mm_screen.screen_id");
		int currentScreenId = Integer.valueOf(currScreenID);

		if(isLinkedScreen(currentScreenId)){
			StringBuilder stringBuilder = new StringBuilder();
			List<String> linkedScreenBoundScreens = linkedScreenBoundScreens(currentScreenId);
			if(linkedScreenBoundScreens != null && linkedScreenBoundScreens.size() > 0)
			{
				if(linkedScreenBoundScreens.size() == 1)
					stringBuilder.append(linkedScreenBoundScreens.get(0));
				else{
					for(int x = 0; x < linkedScreenBoundScreens.size(); x++)
					{
						stringBuilder.append(linkedScreenBoundScreens.get(x) + ", ");
						if(x == linkedScreenBoundScreens.size() - 1)
							stringBuilder.append(linkedScreenBoundScreens.get(x));
					}
				}
			}
			Toast.makeText(this, AndroidResourceHelper.getMessage("YYCSLinkedScreenExists", stringBuilder.toString()), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	private void populateList() {
		String currentDesignSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
		String currentRevisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
		String firstRevisionID = MetrixDatabaseManager.getFieldStringValue(true, "mm_revision", "revision_id", String.format("design_set_id = %1$s and revision_id < %2$s", currentDesignSetID, currentRevisionID), null, null, null, "revision_id asc", "1");

		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_screen.metrix_row_id, mm_screen.screen_id, mm_screen.screen_name, mm_screen.description,");
		query.append(" mm_screen.created_revision_id, mm_screen.modified_revision_id from mm_screen where");
		query.append(" mm_screen.revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
		query.append(" and mm_screen.design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		query.append(" order by mm_screen.screen_name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_screen.metrix_row_id", cursor.getString(0));
				String screenID = cursor.getString(1);
				row.put("mm_screen.screen_id", screenID);
				row.put("mm_screen.screen_name", cursor.getString(2));

				String rawText = cursor.getString(3);
				if (MetrixStringHelper.isNullOrEmpty(rawText)) {
					rawText = mNoDescString;
				} else if (rawText.length() > 40) {
					rawText = rawText.substring(0, 39) + "...";
				}
				row.put("mm_screen.description", rawText);

				String rawCreatedRevisionID = cursor.getString(4);
				boolean isBaseline = true;
				boolean addedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawCreatedRevisionID)) {
					isBaseline = false;
					if (MetrixStringHelper.valueIsEqual(rawCreatedRevisionID, currentRevisionID))
						addedThisRevision = true;
				}
				row.put("mm_screen.is_baseline", String.valueOf(isBaseline));
				row.put("mm_screen.added_this_revision", String.valueOf(addedThisRevision));


				ArrayList<Hashtable<String, String>> fieldRevisionResult = MetrixDatabaseManager.getFieldStringValuesList(String.format("select MAX(mm_field.created_revision_id), MAX(mm_field.modified_revision_id) from mm_field where screen_id = %s", screenID));
				String maxFieldCreatedRevisionID = "";
				String maxFieldModifiedRevisionID = "";
				if (fieldRevisionResult != null && fieldRevisionResult.size() > 0) {
					maxFieldCreatedRevisionID = (String) fieldRevisionResult.get(0).values().toArray()[0];
					maxFieldModifiedRevisionID = (String) fieldRevisionResult.get(0).values().toArray()[1];
				}

				ArrayList<Hashtable<String, String>> screenItemRevisionResult = MetrixDatabaseManager.getFieldStringValuesList(String.format("select MAX(mm_screen_item.created_revision_id), MAX(mm_screen_item.modified_revision_id) from mm_screen_item where screen_id = %s", screenID));
				String maxScreenItemCreatedRevisionID = "";
				String maxScreenItemModifiedRevisionID = "";
				if (screenItemRevisionResult != null && screenItemRevisionResult.size() > 0) {
					maxScreenItemCreatedRevisionID = (String) screenItemRevisionResult.get(0).values().toArray()[0];
					maxScreenItemModifiedRevisionID = (String) screenItemRevisionResult.get(0).values().toArray()[1];
				}

				ArrayList<Hashtable<String, String>> filterSortItemRevisionResult = MetrixDatabaseManager.getFieldStringValuesList(String.format("select MAX(mm_filter_sort_item.created_revision_id), MAX(mm_filter_sort_item.modified_revision_id) from mm_filter_sort_item where screen_id = %s", screenID));
				String maxFilterSortItemCreatedRevisionID = "";
				String maxFilterSortItemModifiedRevisionID = "";
				if (filterSortItemRevisionResult != null && filterSortItemRevisionResult.size() > 0) {
					maxFilterSortItemCreatedRevisionID = (String) filterSortItemRevisionResult.get(0).values().toArray()[0];
					maxFilterSortItemModifiedRevisionID = (String) filterSortItemRevisionResult.get(0).values().toArray()[1];
				}

				String rawModifiedRevisionID = cursor.getString(5);
				boolean editedPrevRevision = false;
				boolean editedThisRevision = false;
				if (!MetrixStringHelper.isNullOrEmpty(rawModifiedRevisionID)) {
					if (MetrixStringHelper.valueIsEqual(rawModifiedRevisionID, currentRevisionID))
						editedThisRevision = true;
					else
						editedPrevRevision = true;
				}

				if (!editedThisRevision &&
						((!MetrixStringHelper.isNullOrEmpty(maxFieldCreatedRevisionID) && MetrixStringHelper.valueIsEqual(maxFieldCreatedRevisionID, currentRevisionID))
								||
								(!MetrixStringHelper.isNullOrEmpty(maxFieldModifiedRevisionID) && MetrixStringHelper.valueIsEqual(maxFieldModifiedRevisionID, currentRevisionID)))) {
					editedThisRevision = true;
				}

				if (!editedThisRevision &&
						((!MetrixStringHelper.isNullOrEmpty(maxScreenItemCreatedRevisionID) && MetrixStringHelper.valueIsEqual(maxScreenItemCreatedRevisionID, currentRevisionID))
								||
								(!MetrixStringHelper.isNullOrEmpty(maxScreenItemModifiedRevisionID) && MetrixStringHelper.valueIsEqual(maxScreenItemModifiedRevisionID, currentRevisionID)))) {
					editedThisRevision = true;
				}

				if (!editedThisRevision &&
						((!MetrixStringHelper.isNullOrEmpty(maxFilterSortItemCreatedRevisionID) && MetrixStringHelper.valueIsEqual(maxFilterSortItemCreatedRevisionID, currentRevisionID))
								||
								(!MetrixStringHelper.isNullOrEmpty(maxFilterSortItemModifiedRevisionID) && MetrixStringHelper.valueIsEqual(maxFilterSortItemModifiedRevisionID, currentRevisionID)))) {
					editedThisRevision = true;
				}

				if (!editedThisRevision && !editedPrevRevision &&
						((!MetrixStringHelper.isNullOrEmpty(maxFieldCreatedRevisionID) && !MetrixStringHelper.isNullOrEmpty(firstRevisionID) && !MetrixStringHelper.valueIsEqual(maxFieldCreatedRevisionID, firstRevisionID))
								||
								(!MetrixStringHelper.isNullOrEmpty(maxFieldModifiedRevisionID)))) {
					editedPrevRevision = true;
				}

				if (!editedThisRevision && !editedPrevRevision &&
						((!MetrixStringHelper.isNullOrEmpty(maxScreenItemCreatedRevisionID) && !MetrixStringHelper.isNullOrEmpty(firstRevisionID) && !MetrixStringHelper.valueIsEqual(maxScreenItemCreatedRevisionID, firstRevisionID))
								||
								(!MetrixStringHelper.isNullOrEmpty(maxScreenItemModifiedRevisionID)))) {
					editedPrevRevision = true;
				}

				if (!editedThisRevision && !editedPrevRevision &&
						((!MetrixStringHelper.isNullOrEmpty(maxFilterSortItemCreatedRevisionID) && !MetrixStringHelper.isNullOrEmpty(firstRevisionID) && !MetrixStringHelper.valueIsEqual(maxFilterSortItemCreatedRevisionID, firstRevisionID))
								||
								(!MetrixStringHelper.isNullOrEmpty(maxFilterSortItemModifiedRevisionID)))) {
					editedPrevRevision = true;
				}

				row.put("mm_screen.edited_prev_revision", String.valueOf(editedPrevRevision));
				row.put("mm_screen.edited_this_revision", String.valueOf(editedThisRevision));

				table.add(row);
				cursor.moveToNext();
			}

			mScreenAdapter = new ScreenListAdapter(this, table, mScreenResourceData.ListViewItemResourceID, mScreenResourceData.ExtraResourceIDs);
			mListView.setAdapter(mScreenAdapter);
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
		if (viewId == mScreenResourceData.getExtraResourceID("R.id.add_screen")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenAddActivity.class);
			intent.putExtra("headingText", mHeadingText);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mScreenAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String screenId = selectedItem.get("mm_screen.screen_id");
		String screenName = selectedItem.get("mm_screen.screen_name");
		MetrixCurrentKeysHelper.setKeyValue("mm_screen", "screen_id", screenId);
		MetrixCurrentKeysHelper.setKeyValue("mm_screen", "screen_name", screenName);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenPropActivity.class);
		intent.putExtra("headingText", mHeadingText);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener deleteScreenListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					if (deleteSelectedScreenAndChildren()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerScreenActivity.this, MetrixDesignerScreenActivity.class);
						intent.putExtra("headingText", mHeadingText);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerScreenActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	@SuppressLint("ShowToast")
	private boolean deleteSelectedScreenAndChildren() {
		boolean returnValue = true;
		try {
			String currMetrixRowID = mSelectedItemToDelete.get("mm_screen.metrix_row_id");
			String currScreenID = mSelectedItemToDelete.get("mm_screen.screen_id");
			int currentScreenId = Integer.valueOf(currScreenID);

			if (isLinkedScreen(currentScreenId)) {
				List<String> linkedScreenBoundScreens = linkedScreenBoundScreens(currentScreenId);
				if (linkedScreenBoundScreens != null && linkedScreenBoundScreens.size() > 0) {
					StringBuilder stringBuilder = new StringBuilder();
					if (linkedScreenBoundScreens.size() == 1)
						stringBuilder.append(linkedScreenBoundScreens.get(0));
					else {
						for (int x = 0; x < linkedScreenBoundScreens.size(); x++) {
							stringBuilder.append(linkedScreenBoundScreens.get(x) + ", ");
							if (x == linkedScreenBoundScreens.size() - 1) {
								stringBuilder.append(linkedScreenBoundScreens.get(x));
							}
						}
					}
					Toast.makeText(this, AndroidResourceHelper.getMessage("YYCSLinkedScreenExists", stringBuilder.toString()), Toast.LENGTH_LONG).show();
					return false;
				}
			}

			boolean success = false;
			MetrixUpdateMessage baseMessage = new MetrixUpdateMessage("mm_screen", MetrixUpdateMessageTransactionType.Delete, "screen_id", currScreenID);
			if (selectedScreenIsTabParent()) {
				// first attempt to do DB deletion of all tab children
				List<TabChildData> childTabList = getChildTabData(currScreenID);
				MetrixDatabaseManager.begintransaction();
				for (TabChildData tcd : childTabList) {
					success = deleteScreenAndChildrenFromDB(tcd.MetrixRowID, tcd.ScreenID);
					if (!success) { break; }
				}

				// if this succeeds, attempt DB deletion of tab parent
				if (success)
					success = deleteScreenAndChildrenFromDB(currMetrixRowID, currScreenID);

				// now submit messages for deletion of tab children
				if (success) {
					for (TabChildData tcd : childTabList) {
						MetrixUpdateMessage tabChildMsg = new MetrixUpdateMessage("mm_screen", MetrixUpdateMessageTransactionType.Delete, "screen_id", tcd.ScreenID);
						success = tabChildMsg.save();
						if (!success) { break; }
					}
				}
			} else {
				MetrixDatabaseManager.begintransaction();
				success = deleteScreenAndChildrenFromDB(currMetrixRowID, currScreenID);
			}

			if (success)
				success = baseMessage.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_screen with screen_id = " + currScreenID);

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

	private boolean deleteScreenAndChildrenFromDB(String currMetrixRowID, String currScreenID) {
		boolean success = true;
		// delete all child records of all kinds in DB, without doing a message transaction
		// MENU ITEM TABLES
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_menu_item_log where metrix_row_id in (select metrix_row_id from mm_menu_item where screen_id = %s))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_menu_item_log", String.format("metrix_row_id in (select metrix_row_id from mm_menu_item where screen_id = %s)", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_menu_item", String.format("screen_id = %s", currScreenID));

		// HOME ITEM TABLES
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_home_item_log where metrix_row_id in (select metrix_row_id from mm_home_item where screen_id = %s))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_home_item_log", String.format("metrix_row_id in (select metrix_row_id from mm_home_item where screen_id = %s)", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_home_item", String.format("screen_id = %s", currScreenID));

		// WORKFLOW SCREEN TABLES
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_workflow_screen_log where metrix_row_id in (select metrix_row_id from mm_workflow_screen where screen_id = %s))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_workflow_screen_log", String.format("metrix_row_id in (select metrix_row_id from mm_workflow_screen where screen_id = %s)", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_workflow_screen", String.format("screen_id = %s", currScreenID));

		// FIELD TABLES (including field lookup tables)
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_column_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s)))))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_column", String.format("lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s)))", currScreenID));

		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_table_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s)))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_table", String.format("lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))", currScreenID));

		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_filter_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_filter where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_filter where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s)))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_filter", String.format("lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))", currScreenID));

		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_orderby_log where metrix_row_id in (select metrix_row_id from mm_field_lkup_orderby where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup_orderby where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s)))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_orderby", String.format("lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))", currScreenID));

		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_lkup_log where metrix_row_id in (select metrix_row_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s)))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup_log", String.format("metrix_row_id in (select metrix_row_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id = %s))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_lkup", String.format("field_id in (select field_id from mm_field where screen_id = %s)", currScreenID));

		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_field_log where metrix_row_id in (select metrix_row_id from mm_field where screen_id = %s))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field_log", String.format("metrix_row_id in (select metrix_row_id from mm_field where screen_id = %s)", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_field", String.format("screen_id = %s", currScreenID));

		// SCREEN TABLES
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_filter_sort_item_log where metrix_row_id in (select metrix_row_id from mm_filter_sort_item where screen_id = %s))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_filter_sort_item_log", String.format("metrix_row_id in (select metrix_row_id from mm_filter_sort_item where screen_id = %s)", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_filter_sort_item", String.format("screen_id = %s", currScreenID));

		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_screen_item_log where metrix_row_id in (select metrix_row_id from mm_screen_item where screen_id = %s))", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_screen_item_log", String.format("metrix_row_id in (select metrix_row_id from mm_screen_item where screen_id = %s)", currScreenID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_screen_item", String.format("screen_id = %s", currScreenID));

		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_screen_log where metrix_row_id = %s)", currMetrixRowID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_screen_log", String.format("metrix_row_id = %s", currMetrixRowID));
		if (success)
			success = MetrixDatabaseManager.deleteRow("mm_screen", String.format("screen_id = %s", currScreenID));

		return success;
	}

	private void setSelectedItemToDelete(HashMap<String, String> item) {
		mSelectedItemToDelete = item;
	}

	/**
	 * Check whether the screen is bound with another screen.
	 * @param screenId
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	protected boolean isLinkedScreen(int screenId) {
		boolean status = false;
		String screenType = MetrixDatabaseManager.getFieldStringValue(String.format("select screen_type from mm_screen where screen_id = %d", screenId));
		String query = "";
		MetrixCursor cursor = null;

		if (!MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD") && !MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_LIST")) {
			query = String.format("SELECT COUNT(distinct screen_name) FROM mm_screen where linked_screen_id = %d and mm_screen.revision_id = %s and mm_screen.design_id = %s",
					screenId, MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"),
					MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		} else if (MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD")) {
			query = String.format("SELECT COUNT(distinct screen_name) FROM mm_screen where mm_screen.revision_id = %s and mm_screen.design_id = %s" +
					" and screen_id in (select screen_id from mm_field where card_screen_id = %d)",
					MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"),
					MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"), screenId);
		}

		if (!MetrixStringHelper.isNullOrEmpty(query)) {
			try {
				cursor = MetrixDatabaseManager.rawQueryMC(query, null);
				if (cursor != null && cursor.moveToFirst()) {
					while (cursor.isAfterLast() == false) {
						int count = cursor.getInt(0);
						status = (count > 0) ? true : false;
						break;
					}
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}

		return status;
	}

	@SuppressLint("DefaultLocale")
	protected List<String> linkedScreenBoundScreens(int currentLinkedScreenId) {
		List<String> screens = new ArrayList<String>();
		String screenType = MetrixDatabaseManager.getFieldStringValue(String.format("select screen_type from mm_screen where screen_id = %s", currentLinkedScreenId));
		String query = "";
		MetrixCursor cursor = null;

		if (!MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD") && !MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_LIST")) {
			query = String.format("SELECT distinct screen_name FROM mm_screen where linked_screen_id = %d and mm_screen.revision_id = %s and mm_screen.design_id = %s",
					currentLinkedScreenId, MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"),
					MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
		} else if (MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD")) {
			query = String.format("SELECT distinct screen_name FROM mm_screen where mm_screen.revision_id = %s and mm_screen.design_id = %s" +
							" and screen_id in (select screen_id from mm_field where card_screen_id = %d)",
					MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"),
					MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"), currentLinkedScreenId);
		}

		if (!MetrixStringHelper.isNullOrEmpty(query)) {
			try {
				cursor = MetrixDatabaseManager.rawQueryMC(query, null);
				if (cursor != null && cursor.moveToFirst()) {
					while (cursor.isAfterLast() == false) {
						screens.add(cursor.getString(0));
						cursor.moveToNext();
					}
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}

		return screens;
	}

	private boolean selectedScreenIsTabParent() {
		boolean isTabParent = false;
		try {
			String currScreenID = mSelectedItemToDelete.get("mm_screen.screen_id");
			int tabChildCount = MetrixDatabaseManager.getCount("mm_screen", String.format("tab_parent_id = %s", currScreenID));
			isTabParent = (tabChildCount > 0);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
			isTabParent = false;
		}
		return isTabParent;
	}

	private String generateTabParentDeleteMsg() {
		String tabParentDeleteMsg = "";
		StringBuilder setOfTabChildNames = new StringBuilder();
		MetrixCursor cursor = null;
		try {
			String currScreenID = mSelectedItemToDelete.get("mm_screen.screen_id");
			cursor = MetrixDatabaseManager.rawQueryMC(String.format("select screen_name from mm_screen where tab_parent_id = %s order by screen_name asc", currScreenID), null);
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					setOfTabChildNames.append(cursor.getString(0));
					if (!cursor.isLast())
						setOfTabChildNames.append(", ");
					cursor.moveToNext();
				}
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}

			tabParentDeleteMsg = AndroidResourceHelper.getMessage("ScreenDeleteTabParentConfirm", setOfTabChildNames.toString());
		}
		return tabParentDeleteMsg;
	}

	private List<TabChildData> getChildTabData(String currScreenID) {
		List<TabChildData> tcdList = new ArrayList<TabChildData>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(String.format("select metrix_row_id, screen_id from mm_screen where tab_parent_id = %s", currScreenID), null);
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					TabChildData thisTCD = new TabChildData(cursor.getString(0), cursor.getString(1));
					tcdList.add(thisTCD);
					cursor.moveToNext();
				}
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return tcdList;
	}

	private static class TabChildData {
		public String MetrixRowID;
		public String ScreenID;

		public TabChildData(String metrixRowID, String screenID) {
			MetrixRowID = metrixRowID;
			ScreenID = screenID;
		}
	}

	public static class ScreenListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private Bitmap mNewAddedIcon, mOldAddedIcon, mNewEditedIcon, mOldEditedIcon, mBlankIcon;

		public ScreenListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
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
				holder.mName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__screen_name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__metrix_row_id"));
				holder.mScreenID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__screen_id"));
				holder.mDescription = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__description"));
				holder.mIsBaseline = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__is_baseline"));
				holder.mAddedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_added_icon"));
				holder.mEditedIcon = (ImageView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.is_edited_icon"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mName.setText(dataRow.get("mm_screen.screen_name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_screen.metrix_row_id"));
			holder.mScreenID.setText(dataRow.get("mm_screen.screen_id"));

			String currentDescText = dataRow.get("mm_screen.description");
			holder.mDescription.setText(currentDescText);
			if (MetrixStringHelper.valueIsEqual(currentDescText, mNoDescString)) {
				holder.mDescription.setVisibility(View.GONE);
			} else {
				holder.mDescription.setVisibility(View.VISIBLE);
			}

			boolean isBaseline = Boolean.valueOf(dataRow.get("mm_screen.is_baseline"));
			holder.mIsBaseline.setText(String.valueOf(isBaseline));

			boolean isAddedThisRev = Boolean.valueOf(dataRow.get("mm_screen.added_this_revision"));
			boolean isEditedThisRev = Boolean.valueOf(dataRow.get("mm_screen.edited_this_revision"));
			boolean isEditedPrevRev = Boolean.valueOf(dataRow.get("mm_screen.edited_prev_revision"));

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
			TextView mScreenID;
			TextView mDescription;
			TextView mIsBaseline;
			ImageView mAddedIcon;
			ImageView mEditedIcon;
		}
	}
}