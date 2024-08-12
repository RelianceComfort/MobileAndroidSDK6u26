package com.metrix.architecture.designer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixUpdateMessage;
import com.metrix.architecture.metadata.MetrixUpdateMessage.MetrixUpdateMessageTransactionType;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("deprecation")
public class MetrixDesignerSkinActivity extends MetrixDesignerActivity implements OnItemClickListener {
	private ListView mListView;
	private AlertDialog mGetAllSkinsDialog, mDeleteSkinDialog;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private HashMap<String, String> mSelectedItemToDelete;
	private Button mRefreshSkins, mAddSkin;
	private static String mNoDescString;
	private SkinListAdapter mSkinAdapter;
	private MetrixDesignerResourceData mSkinResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSkinResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerSkinActivityResourceData");

		setContentView(mSkinResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mSkinResourceData.ListViewResourceID);
		mListView.setOnItemClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mSkinResourceData.HelpTextString;

		mHeadingText = AndroidResourceHelper.getMessage("SkinEditingSuite");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mRefreshSkins = (Button) findViewById(mSkinResourceData.getExtraResourceID("R.id.refresh_skins"));
		mAddSkin = (Button) findViewById(mSkinResourceData.getExtraResourceID("R.id.add_skin"));
		mRefreshSkins.setOnClickListener(this);
		mAddSkin.setOnClickListener(this);

		mNoDescString = AndroidResourceHelper.getMessage("NoDescriptionFound");

		TextView mSkns = (TextView) findViewById(mSkinResourceData.getExtraResourceID("R.id.skins"));
		TextView mScrInfo = (TextView) findViewById(mSkinResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_skin"));

		AndroidResourceHelper.setResourceValues(mSkns, "Skins");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesSkin");
		AndroidResourceHelper.setResourceValues(mRefreshSkins, "Refresh");
		AndroidResourceHelper.setResourceValues(mAddSkin, "AddSkin");

		populateList();

		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
				Object item = mSkinAdapter.getItem(position);
				@SuppressWarnings("unchecked")
				HashMap<String, String> selectedItem = (HashMap<String, String>) item;
				setSelectedItemToDelete(selectedItem);
				mDeleteSkinDialog = new AlertDialog.Builder(MetrixDesignerSkinActivity.this).create();
				mDeleteSkinDialog.setMessage(AndroidResourceHelper.getMessage("SkinDeleteConfirm"));
				mDeleteSkinDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), deleteSkinListener);
				mDeleteSkinDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), deleteSkinListener);
				mDeleteSkinDialog.show();
				return true;
			}
		});
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerSkinActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	@Override
	protected void unbindService() {
		if (mIsBound) {
			try {
				if (service != null) {
					service.removeListener(listener);
					unbindService(mConnection);
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				mIsBound = false;
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);
			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMASK\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerSkinActivity.this, MetrixDesignerSkinActivity.class);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerSkinActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_skin.metrix_row_id, mm_skin.skin_id, mm_skin.name, mm_skin.description,");
		query.append(" mm_skin.primary_color, mm_skin.secondary_color, mm_skin.hyperlink_color,");
		query.append(" mm_skin.first_gradient1, mm_skin.first_gradient2, mm_skin.first_gradient_text,");
		query.append(" mm_skin.second_gradient1, mm_skin.second_gradient2, mm_skin.second_gradient_text");
		query.append(" from mm_skin order by mm_skin.name asc");

		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		MetrixCursor cursor = null;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
				row.put("mm_skin.metrix_row_id", cursor.getString(0));
				String screenID = cursor.getString(1);
				row.put("mm_skin.skin_id", screenID);
				row.put("mm_skin.name", cursor.getString(2));

				String rawText = cursor.getString(3);
				if (MetrixStringHelper.isNullOrEmpty(rawText)) {
					rawText = mNoDescString;
				} else if (rawText.length() > 40) {
					rawText = rawText.substring(0, 39) + "...";
				}
				row.put("mm_skin.description", rawText);

				row.put("mm_skin.primary_color", cursor.getString(4));
				row.put("mm_skin.secondary_color", cursor.getString(5));
				row.put("mm_skin.hyperlink_color", cursor.getString(6));
				row.put("mm_skin.first_gradient1", cursor.getString(7));
				row.put("mm_skin.first_gradient2", cursor.getString(8));
				row.put("mm_skin.first_gradient_text", cursor.getString(9));
				row.put("mm_skin.second_gradient1", cursor.getString(10));
				row.put("mm_skin.second_gradient2", cursor.getString(11));
				row.put("mm_skin.second_gradient_text", cursor.getString(12));

				table.add(row);
				cursor.moveToNext();
			}

			mSkinAdapter = new SkinListAdapter(this, table, mSkinResourceData.ListViewItemResourceID, mSkinResourceData.ExtraResourceIDs);
			mListView.setAdapter(mSkinAdapter);
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
		if (viewId == mSkinResourceData.getExtraResourceID("R.id.refresh_skins")) {
			mGetAllSkinsDialog = new AlertDialog.Builder(MetrixDesignerSkinActivity.this).create();
			mGetAllSkinsDialog.setMessage(AndroidResourceHelper.getMessage("RefreshAllSkinsConfirm"));
			mGetAllSkinsDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), getAllSkinsListener);
			mGetAllSkinsDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), getAllSkinsListener);
			mGetAllSkinsDialog.show();
		} else if (viewId == mSkinResourceData.getExtraResourceID("R.id.add_skin")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerSkinAddActivity.class);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object item = mSkinAdapter.getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> selectedItem = (HashMap<String, String>) item;

		String skinId = selectedItem.get("mm_skin.skin_id");
		String skinName = selectedItem.get("mm_skin.name");
		MetrixCurrentKeysHelper.setKeyValue("mm_skin", "skin_id", skinId);
		MetrixCurrentKeysHelper.setKeyValue("mm_skin", "name", skinName);

		Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerSkinColorsActivity.class);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	DialogInterface.OnClickListener getAllSkinsListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // OK
					// wire up OK button to call perform_get_mobile_all_skins
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startGetAllSkinsListener();
								}
							});
						}
					} else
						startGetAllSkinsListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // Cancel (do nothing)
					break;
			}
		}
	};

	private void startGetAllSkinsListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				clearSkinMetadata();

				if (doAllSkinsGet(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerSkinActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mGetAllSkinsDialog != null) {
								mGetAllSkinsDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mGetAllSkinsDialog != null) {
					mGetAllSkinsDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("GetAllSkinsInProgress"));
			}
		});

		thread.start();
	}

	DialogInterface.OnClickListener deleteSkinListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					String currSkinID = mSelectedItemToDelete.get("mm_skin.skin_id");
					int revisionCount = MetrixDatabaseManager.getCount("mm_revision", String.format("skin_id = %s", currSkinID));
					if (revisionCount > 0) {
						Toast.makeText(MetrixDesignerSkinActivity.this, AndroidResourceHelper.getMessage("SkinDeleteCountError"), Toast.LENGTH_LONG).show();
						break;
					}

					if (deleteSelectedSkin()) {
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerSkinActivity.this, MetrixDesignerSkinActivity.class);
						MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerSkinActivity.this, intent);
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	public static boolean doAllSkinsGet(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		try {
			Hashtable<String, String> params = new Hashtable<String, String>();
			int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

			params.put("device_sequence", String.valueOf(device_id));

			MetrixPerformMessage performGMAS = new MetrixPerformMessage("perform_get_mobile_all_skins", params);
			performGMAS.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}

	private boolean deleteSelectedSkin() {
		boolean returnValue = true;
		try {
			boolean success = true;
			String currMetrixRowID = mSelectedItemToDelete.get("mm_skin.metrix_row_id");
			String currSkinID = mSelectedItemToDelete.get("mm_skin.skin_id");

			// generate a deletion transaction for mm_skin ONLY
			MetrixUpdateMessage message = new MetrixUpdateMessage("mm_skin", MetrixUpdateMessageTransactionType.Delete, "skin_id", currSkinID);

			MetrixDatabaseManager.begintransaction();

			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_message_out", String.format("metrix_log_id in (select metrix_log_id from mm_skin_log where metrix_row_id = %s)", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_skin_log", String.format("metrix_row_id = %s", currMetrixRowID));
			if (success)
				success = MetrixDatabaseManager.deleteRow("mm_skin", String.format("skin_id = %s", currSkinID));

			// if all of the above worked, send the message.
			if (success)
				success = message.save();
			else
				LogManager.getInstance().info("Failed to delete the mm_skin with skin_id = " + currSkinID);

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

	public static class SkinListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private final String letterA;

		public SkinListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);
			letterA = AndroidResourceHelper.getMessage("A");
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__name"));
				holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__metrix_row_id"));
				holder.mSkinID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__skin_id"));
				holder.mDescription = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__description"));
				holder.mPrimaryColor = vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__primary_color"));
				holder.mSecondaryColor = vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__secondary_color"));
				holder.mHyperlinkColor = vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__hyperlink_color"));
				holder.mFirstGradient = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__first_gradient"));
				holder.mSecondGradient = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_skin__second_gradient"));

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mName.setText(dataRow.get("mm_skin.name"));
			holder.mMetrixRowID.setText(dataRow.get("mm_skin.metrix_row_id"));
			holder.mSkinID.setText(dataRow.get("mm_skin.skin_id"));

			String currentDescText = dataRow.get("mm_skin.description");
			holder.mDescription.setText(currentDescText);
			if (MetrixStringHelper.valueIsEqual(currentDescText, mNoDescString)) {
				holder.mDescription.setVisibility(View.GONE);
			} else {
				holder.mDescription.setVisibility(View.VISIBLE);
			}

			String primaryColorString = dataRow.get("mm_skin.primary_color");
			String secondaryColorString = dataRow.get("mm_skin.secondary_color");
			String hyperlinkColorString = dataRow.get("mm_skin.hyperlink_color");
			String firstGradient1String = dataRow.get("mm_skin.first_gradient1");
			String firstGradient2String = dataRow.get("mm_skin.first_gradient2");
			String firstGradientTextString = dataRow.get("mm_skin.first_gradient_text");
			String secondGradient1String = dataRow.get("mm_skin.second_gradient1");
			String secondGradient2String = dataRow.get("mm_skin.second_gradient2");
			String secondGradientTextString = dataRow.get("mm_skin.second_gradient_text");

			primaryColorString = MetrixStringHelper.isNullOrEmpty(primaryColorString) ? "FFFFFF" : primaryColorString;
			secondaryColorString = MetrixStringHelper.isNullOrEmpty(secondaryColorString) ? "FFFFFF" : secondaryColorString;
			hyperlinkColorString = MetrixStringHelper.isNullOrEmpty(hyperlinkColorString) ? "FFFFFF" : hyperlinkColorString;
			firstGradient1String = MetrixStringHelper.isNullOrEmpty(firstGradient1String) ? "FFFFFF" : firstGradient1String;
			firstGradient2String = MetrixStringHelper.isNullOrEmpty(firstGradient2String) ? "FFFFFF" : firstGradient2String;
			firstGradientTextString = MetrixStringHelper.isNullOrEmpty(firstGradientTextString) ? "FFFFFF" : firstGradientTextString;
			secondGradient1String = MetrixStringHelper.isNullOrEmpty(secondGradient1String) ? "FFFFFF" : secondGradient1String;
			secondGradient2String = MetrixStringHelper.isNullOrEmpty(secondGradient2String) ? "FFFFFF" : secondGradient2String;
			secondGradientTextString = MetrixStringHelper.isNullOrEmpty(secondGradientTextString) ? "FFFFFF" : secondGradientTextString;

			int primaryColor = (int)Long.parseLong(primaryColorString, 16);
			int secondaryColor = (int)Long.parseLong(secondaryColorString, 16);
			int hyperlinkColor = (int)Long.parseLong(hyperlinkColorString, 16);
			int firstGradient1 = (int)Long.parseLong(firstGradient1String, 16);
			int firstGradient2 = (int)Long.parseLong(firstGradient2String, 16);
			int firstGradientText = (int)Long.parseLong(firstGradientTextString, 16);
			int secondGradient1 = (int)Long.parseLong(secondGradient1String, 16);
			int secondGradient2 = (int)Long.parseLong(secondGradient2String, 16);
			int secondGradientText = (int)Long.parseLong(secondGradientTextString, 16);

			holder.mPrimaryColor.setBackgroundColor(Color.rgb((primaryColor >> 16) & 0xFF, (primaryColor >> 8) & 0xFF, (primaryColor >> 0) & 0xFF));
			holder.mSecondaryColor.setBackgroundColor(Color.rgb((secondaryColor >> 16) & 0xFF, (secondaryColor >> 8) & 0xFF, (secondaryColor >> 0) & 0xFF));
			holder.mHyperlinkColor.setBackgroundColor(Color.rgb((hyperlinkColor >> 16) & 0xFF, (hyperlinkColor >> 8) & 0xFF, (hyperlinkColor >> 0) & 0xFF));

			int[] colors = new int[2];
			colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
			colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
			GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
			firstGradient.setCornerRadius(0f);
			holder.mFirstGradient.setBackgroundDrawable(firstGradient);
			holder.mFirstGradient.setTextColor(Color.rgb((firstGradientText >> 16) & 0xFF, (firstGradientText >> 8) & 0xFF, (firstGradientText >> 0) & 0xFF));
			holder.mFirstGradient.setText(letterA);

			int[] colors2 = new int[2];
			colors2[0] = Color.rgb((secondGradient1 >> 16) & 0xFF, (secondGradient1 >> 8) & 0xFF, (secondGradient1 >> 0) & 0xFF);
			colors2[1] = Color.rgb((secondGradient2 >> 16) & 0xFF, (secondGradient2 >> 8) & 0xFF, (secondGradient2 >> 0) & 0xFF);
			GradientDrawable secondGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors2);
			secondGradient.setCornerRadius(0f);
			holder.mSecondGradient.setBackgroundDrawable(secondGradient);
			holder.mSecondGradient.setTextColor(Color.rgb((secondGradientText >> 16) & 0xFF, (secondGradientText >> 8) & 0xFF, (secondGradientText >> 0) & 0xFF));
			holder.mSecondGradient.setText(letterA);

			return vi;
		}

		static class ViewHolder {
			TextView mName;
			TextView mMetrixRowID;
			TextView mSkinID;
			TextView mDescription;
			View mPrimaryColor;
			View mSecondaryColor;
			View mHyperlinkColor;
			TextView mFirstGradient;
			TextView mSecondGradient;
		}
	}
}

