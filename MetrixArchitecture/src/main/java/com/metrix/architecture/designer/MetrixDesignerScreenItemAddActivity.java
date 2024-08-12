package com.metrix.architecture.designer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixPerformMessage;
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

import java.util.Hashtable;

public class MetrixDesignerScreenItemAddActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
	private Button mSaveButton;
	private static Spinner mItemName, mEventType;
	private static EditText mEvent;
	private static EditText mDesc;
	private static TextView mItemType;
	private TextView mEmphasis, mEventDescLabel, mEventDesc;
	private AlertDialog mAddScreenItemDialog;
	private String mScreenName;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mScreenItemAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mScreenItemAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerScreenItemAddActivityResourceData");

		setContentView(mScreenItemAddResourceData.LayoutResourceID);
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mScreenItemAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.zzmd_screen_item_add_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesScnItmAdd", mScreenName);
		mEmphasis.setText(fullText);

		mSaveButton = (Button) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);

		TextView mAddScr = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.add_screen_item"));
		TextView mItmLbl = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.item_type_label"));
		TextView mItmNamLbl = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.item_name_label"));
		TextView mEvntTyp = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.event_type_label"));
		TextView mEvntLbl = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.event_label"));
		TextView mDescLbl = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.description_label"));


		AndroidResourceHelper.setResourceValues(mAddScr, "AddScreenItem");
		AndroidResourceHelper.setResourceValues(mItmLbl, "ItemType");
		AndroidResourceHelper.setResourceValues(mItmNamLbl, "ItemName");
		AndroidResourceHelper.setResourceValues(mEvntTyp, "EventType");
		AndroidResourceHelper.setResourceValues(mEvntLbl, "Event");
		AndroidResourceHelper.setResourceValues(mDescLbl, "Description");
		AndroidResourceHelper.setResourceValues(mSaveButton, "Save");
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerScreenItemAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMSI\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerScreenItemAddActivity.this, MetrixDesignerScreenItemActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
						intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
						MetrixActivityHelper.startNewActivity(MetrixDesignerScreenItemAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		try {
			mItemType = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.item_type"));
			mItemName = (Spinner) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.item_name"));
			mEventType = (Spinner) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.event_type"));
			mEvent = (EditText) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.event"));
			mDesc = (EditText) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.description"));

			mEventDescLabel = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.event_description_label"));
			mEventDesc = (TextView) findViewById(mScreenItemAddResourceData.getExtraResourceID("R.id.event_description"));

			// only one code_value at this time, so display as read-only
			String buttonItemTypeText = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", "message_type = 'CODE' and message_id in (select message_id from metrix_code_table where code_name = 'MM_SCREEN_ITEM_ITEM_TYPE' and code_value = 'BUTTON')");
			mItemType.setText(buttonItemTypeText);

			StringBuilder itemNameQuery = new StringBuilder();
			itemNameQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
			itemNameQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
			itemNameQuery.append("where metrix_code_table.code_name = 'MM_SCREEN_ITEM_ITEM_NAME' ");
			itemNameQuery.append(String.format("and metrix_code_table.code_value not in (select distinct item_name from mm_screen_item where screen_id = %s) ", MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id")));
			itemNameQuery.append("order by mm_message_def_view.message_text asc");
			MetrixControlAssistant.populateSpinnerFromQuery(this, mItemName, itemNameQuery.toString(), true);

			StringBuilder eventTypeQuery = new StringBuilder();
			eventTypeQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
			eventTypeQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
			eventTypeQuery.append("where metrix_code_table.code_name = 'MM_SCREEN_ITEM_EVENT_TYPE' ");
			eventTypeQuery.append("order by mm_message_def_view.message_text asc");
			MetrixControlAssistant.populateSpinnerFromQuery(this, mEventType, eventTypeQuery.toString(), true);

			String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = "
					+ MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
			if (MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD")) {
				// Force event type to be VALIDATION and disable.
				MetrixControlAssistant.setValue(mEventType, "VALIDATION");
				mEventType.setEnabled(false);
			}

			mEvent.setOnFocusChangeListener(this);
			mEvent.addTextChangedListener(new ClientScriptTextWatcher(mEventDescLabel, mEventDesc));
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (hasFocus) {
			LinearLayout rowLayout = (LinearLayout) v.getParent();
			int viewId = v.getId();
			if (viewId == mScreenItemAddResourceData.getExtraResourceID("R.id.event"))
				doClientScriptSelection(viewId, rowLayout);
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mScreenItemAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// validate that Item Name is filled in
				String itemName = MetrixControlAssistant.getValue(mItemName);
				if (MetrixStringHelper.isNullOrEmpty(itemName)) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddScreenItemItemNameError"), Toast.LENGTH_LONG).show();
					return;
				}

				// validate that both or neither Event / Event Type are filled in
				String eventType = MetrixControlAssistant.getValue(mEventType);
				String event = MetrixControlAssistant.getValue(mEvent);
				if ((!MetrixStringHelper.isNullOrEmpty(eventType) && MetrixStringHelper.isNullOrEmpty(event))
						|| (MetrixStringHelper.isNullOrEmpty(eventType) && !MetrixStringHelper.isNullOrEmpty(event))) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("ScnItmEvntErr"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddScreenItemDialog = new AlertDialog.Builder(this).create();
				mAddScreenItemDialog.setMessage(AndroidResourceHelper.getMessage("AddScreenItemConfirm"));
				mAddScreenItemDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addScreenItemListener);
				mAddScreenItemDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addScreenItemListener);
				mAddScreenItemDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	DialogInterface.OnClickListener addScreenItemListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_field
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddScreenItemListener();
								}
							});
						}
					} else
						startAddScreenItemListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddScreenItemListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doScreenItemAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerScreenItemAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddScreenItemDialog != null) {
								mAddScreenItemDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddScreenItemDialog != null) {
					mAddScreenItemDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddScreenItemInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doScreenItemAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

		try {
			String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
			String itemName = MetrixControlAssistant.getValue(mItemName);
			// String itemType = MetrixControlAssistant.getValue(mItemType);
			String eventType = MetrixControlAssistant.getValue(mEventType);
			String event = MetrixControlAssistant.getValue(mEvent);
			String description = MetrixControlAssistant.getValue(mDesc);

			params.put("screen_id", screenID);
			params.put("item_name", itemName);
			params.put("item_type", "BUTTON");
			params.put("device_sequence", String.valueOf(device_id));
			if (!MetrixStringHelper.isNullOrEmpty(eventType)) {
				params.put("event_type", eventType);
			}
			if (!MetrixStringHelper.isNullOrEmpty(event)) {
				params.put("event", event);
			}
			if (!MetrixStringHelper.isNullOrEmpty(description)) {
				params.put("description", description);
			}
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

			MetrixPerformMessage performGMSI = new MetrixPerformMessage("perform_generate_mobile_screen_item", params);
			performGMSI.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}
}

