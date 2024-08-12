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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixCursor;
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
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.Hashtable;

@SuppressWarnings("deprecation")
public class MetrixDesignerSkinAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private static EditText mName, mDesc;
	private static Spinner mSourceSkin;
	private static LinearLayout mColorPreview;
	private static View mPrimaryColor, mSecondaryColor, mHyperlinkColor;
	private static TextView mFirstGradient, mSecondGradient;
	private AlertDialog mAddSkinDialog;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mSkinAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSkinAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerSkinAddActivityResourceData");

		setContentView(mSkinAddResourceData.LayoutResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mSkinAddResourceData.HelpTextString;

		mHeadingText = AndroidResourceHelper.getMessage("SkinEditingSuite");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mSaveButton = (Button) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);

		TextView mAddSkn = (TextView) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.add_skin"));
		TextView mScrInfo = (TextView) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_skin_add"));
		TextView mNamelbl = (TextView) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.name_lbl"));
		TextView mSrcSkn = (TextView) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.source_skin_lbl"));
		TextView mDescLbl = (TextView) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.description_lbl"));

		AndroidResourceHelper.setResourceValues(mAddSkn, "AddSkin");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesSkinAdd");
		AndroidResourceHelper.setResourceValues(mNamelbl, "Name");
		AndroidResourceHelper.setResourceValues(mSrcSkn, "SourceSkin");
		AndroidResourceHelper.setResourceValues(mDescLbl, "Description");
		AndroidResourceHelper.setResourceValues(mSaveButton, "Save");

		populateScreen();
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerSkinAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMSK\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerSkinAddActivity.this, MetrixDesignerSkinActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						MetrixActivityHelper.startNewActivity(MetrixDesignerSkinAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mName = (EditText) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.name"));
		mSourceSkin = (Spinner) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.source_skin"));
		mColorPreview = (LinearLayout) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.color_preview"));
		mPrimaryColor = findViewById(mSkinAddResourceData.getExtraResourceID("R.id.mm_skin__primary_color"));
		mSecondaryColor = findViewById(mSkinAddResourceData.getExtraResourceID("R.id.mm_skin__secondary_color"));
		mHyperlinkColor = findViewById(mSkinAddResourceData.getExtraResourceID("R.id.mm_skin__hyperlink_color"));
		mFirstGradient = (TextView) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.mm_skin__first_gradient"));
		mSecondGradient = (TextView) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.mm_skin__second_gradient"));
		mDesc = (EditText) findViewById(mSkinAddResourceData.getExtraResourceID("R.id.description"));

		// add required hint to Name
		mName.setHint(AndroidResourceHelper.getMessage("Required"));

		final String letterA = AndroidResourceHelper.getMessage("A");
		mFirstGradient.setText(letterA);
		mSecondGradient.setText(letterA);

		// start off by hiding color_preview layout
		mColorPreview.setVisibility(View.GONE);

		// populate source skin spinner with all skins found in mm_skin
		MetrixControlAssistant.populateSpinnerFromQuery(this, mSourceSkin, "select distinct name, skin_id from mm_skin order by name asc", true);
		mSourceSkin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setColorPreviewBasedOnSourceSkinSelection();
			}

			public void onNothingSelected(AdapterView<?> parent) {
				setColorPreviewBasedOnSourceSkinSelection();
			}
		});
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mSkinAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// make sure Name is populated AND that a skin with that name doesn't already exist
				boolean generateValidationMsg = false;
				String name = MetrixControlAssistant.getValue(mName);
				if (MetrixStringHelper.isNullOrEmpty(name)) {
					generateValidationMsg = true;
				} else {
					int instancesOfName = MetrixDatabaseManager.getCount("mm_skin", String.format("name = '%s'", name));
					if (instancesOfName > 0) {
						generateValidationMsg = true;
					}
				}

				if (generateValidationMsg) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddSkinNameError"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddSkinDialog = new AlertDialog.Builder(this).create();
				mAddSkinDialog.setMessage(AndroidResourceHelper.getMessage("AddSkinConfirm"));
				mAddSkinDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addSkinListener);
				mAddSkinDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addSkinListener);
				mAddSkinDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	private void setColorPreviewBasedOnSourceSkinSelection() {
		try {
			SpinnerKeyValuePair pair = (SpinnerKeyValuePair) mSourceSkin.getSelectedItem();
			String sourceSkinID = pair.spinnerValue;
			if (!MetrixStringHelper.isNullOrEmpty(sourceSkinID)) {
				setColorViewsForSkinID(sourceSkinID);
				mColorPreview.setVisibility(View.VISIBLE);
			} else {
				mColorPreview.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void setColorViewsForSkinID(String sourceSkinID) {
		StringBuilder query = new StringBuilder();
		query.append("select distinct mm_skin.primary_color, mm_skin.secondary_color, mm_skin.hyperlink_color,");
		query.append(" mm_skin.first_gradient1, mm_skin.first_gradient2, mm_skin.first_gradient_text,");
		query.append(" mm_skin.second_gradient1, mm_skin.second_gradient2, mm_skin.second_gradient_text");
		query.append(String.format(" from mm_skin where skin_id = %s", sourceSkinID));

		MetrixCursor cursor = null;
		String primaryColorString = "";
		String secondaryColorString = "";
		String hyperlinkColorString = "";
		String firstGradient1String = "";
		String firstGradient2String = "";
		String firstGradientTextString = "";
		String secondGradient1String = "";
		String secondGradient2String = "";
		String secondGradientTextString = "";

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			while (cursor.isAfterLast() == false) {
				primaryColorString = cursor.getString(0);
				secondaryColorString = cursor.getString(1);
				hyperlinkColorString = cursor.getString(2);
				firstGradient1String = cursor.getString(3);
				firstGradient2String = cursor.getString(4);
				firstGradientTextString = cursor.getString(5);
				secondGradient1String = cursor.getString(6);
				secondGradient2String = cursor.getString(7);
				secondGradientTextString = cursor.getString(8);
				break;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

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

		mPrimaryColor.setBackgroundColor(Color.rgb((primaryColor >> 16) & 0xFF, (primaryColor >> 8) & 0xFF, (primaryColor >> 0) & 0xFF));
		mSecondaryColor.setBackgroundColor(Color.rgb((secondaryColor >> 16) & 0xFF, (secondaryColor >> 8) & 0xFF, (secondaryColor >> 0) & 0xFF));
		mHyperlinkColor.setBackgroundColor(Color.rgb((hyperlinkColor >> 16) & 0xFF, (hyperlinkColor >> 8) & 0xFF, (hyperlinkColor >> 0) & 0xFF));

		int[] colors = new int[2];
		colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
		colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
		GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
		firstGradient.setCornerRadius(0f);
		mFirstGradient.setBackgroundDrawable(firstGradient);
		mFirstGradient.setTextColor(Color.rgb((firstGradientText >> 16) & 0xFF, (firstGradientText >> 8) & 0xFF, (firstGradientText >> 0) & 0xFF));

		int[] colors2 = new int[2];
		colors2[0] = Color.rgb((secondGradient1 >> 16) & 0xFF, (secondGradient1 >> 8) & 0xFF, (secondGradient1 >> 0) & 0xFF);
		colors2[1] = Color.rgb((secondGradient2 >> 16) & 0xFF, (secondGradient2 >> 8) & 0xFF, (secondGradient2 >> 0) & 0xFF);
		GradientDrawable secondGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors2);
		secondGradient.setCornerRadius(0f);
		mSecondGradient.setBackgroundDrawable(secondGradient);
		mSecondGradient.setTextColor(Color.rgb((secondGradientText >> 16) & 0xFF, (secondGradientText >> 8) & 0xFF, (secondGradientText >> 0) & 0xFF));

	}

	DialogInterface.OnClickListener addSkinListener = new DialogInterface.OnClickListener() {
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
									startAddSkinListener();
								}
							});
						}
					} else
						startAddSkinListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddSkinListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doSkinAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerSkinAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddSkinDialog != null) {
								mAddSkinDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddSkinDialog != null) {
					mAddSkinDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddSkinInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doSkinAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

		try {
			String name = mName.getText().toString();
			String sourceSkinID = "";
			SpinnerKeyValuePair pair = (SpinnerKeyValuePair) mSourceSkin.getSelectedItem();
			if (pair != null) {
				sourceSkinID = pair.spinnerValue;
			}
			String description = mDesc.getText().toString();

			params.put("name", name);
			params.put("device_sequence", String.valueOf(device_id));
			if (!MetrixStringHelper.isNullOrEmpty(sourceSkinID)) {
				params.put("source_skin_id", sourceSkinID);
			}
			if (!MetrixStringHelper.isNullOrEmpty(description)) {
				params.put("description", description);
			}

			MetrixPerformMessage performGMSK = new MetrixPerformMessage("perform_generate_mobile_skin", params);
			performGMSK.save();
		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}
}

