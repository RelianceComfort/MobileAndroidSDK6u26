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
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class MetrixDesignerFieldAddActivity extends MetrixDesignerActivity {
	private Button mAttachmentFieldButton, mSaveButton;
	private static CheckBox mIsCustomField, mIsSelectAllFields, mIsButton;
	private static Spinner mTable, mAddAttachmentDialogColumn, mAddControlType;
	private static ListView mListView;
	private static EditText mCustomColumn, mDesc, mDialogDescAddField, mAddAttachmentDialogDesc;
	private static String mAddAttachmentDialogColumnValue, mAddAttachmentDialogDescValue;
	private TextView mEmphasis, mTableLabel, mCustomColumnLabel, mDescLabel, mIsSelectAllFieldsLabel, mIsButtonLabel;
	private AlertDialog mAddFieldDialog, mAddFieldDescDialog, mAddAttachmentFieldDialog;
	private String mScreenName;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mFieldAddResourceData;
	private FieldListAdapter mFieldAdapter;
	private ScrollView mScrollView;
	private View mListViewArea;

	private boolean mIsStandardScreen;
	private boolean mStatusAllFieldsSelected;
	private HashMap<String, String> mSelectedItem;

	private static HashMap<String, HashMap<String, String>> mCachedTableFieldsDescMap;
	private static String mSelectedTableName;

	private static int prevSelectedIndex;
	private static String controlType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = "
				+ MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
		mIsStandardScreen = !MetrixStringHelper.isNullOrEmpty(screenType) && (MetrixStringHelper.valueIsEqual(screenType, "STANDARD"));

		mFieldAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldAddActivityResourceData");

		setContentView(mFieldAddResourceData.LayoutResourceID);

		mListView = (ListView) findViewById(mFieldAddResourceData.ListViewResourceID);

		mCachedTableFieldsDescMap = new HashMap<String, HashMap<String, String>>();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mFieldAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.zzmd_field_add_emphasis"));
        String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldAdd", mScreenName);
        mEmphasis.setText(fullText);

		mAttachmentFieldButton = (Button) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.attachment_field"));
		mAttachmentFieldButton.setOnClickListener(this);

		mSaveButton = (Button) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);

		AndroidResourceHelper.setResourceValues(mAttachmentFieldButton, "Attachment");
        AndroidResourceHelper.setResourceValues(mSaveButton, "Save");

		populateScreen();

		if (mAllowChanges) {
			mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

					CheckBox theChkBox = (CheckBox) view.findViewById(mFieldAddResourceData.ExtraResourceIDs.get("R.id.checkboxState"));
					if (theChkBox != null && !theChkBox.isChecked()) return false;

					Object item = mFieldAdapter.getItem(position);
					mSelectedItem = (HashMap<String, String>) item;

					// if we get through validation, inflate (zzmd_revision_add_dialog) as an AlertDialog
					AlertDialog.Builder addFieldDescBuilder = new AlertDialog.Builder(mCurrentActivity);
					LayoutInflater inflater = mCurrentActivity.getLayoutInflater();
					View inflatedView = inflater.inflate(mFieldAddResourceData.getExtraResourceID("R.layout.zzmd_field_add_desc_dialog"), null);
					addFieldDescBuilder.setView(inflatedView);

					TextView mDialogDescAddFieldLabel = (TextView) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.field_name_label"));
                    mDialogDescAddFieldLabel.setText(String.format("%s (%s)", AndroidResourceHelper.getMessage("Description"), mSelectedItem.get("mm_field.field_name")));

                    mDialogDescAddField = (EditText) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.field_add_description"));
                    mAddFieldDescDialog = addFieldDescBuilder.create();

					String fieldDesc = getFieldDescription();
					if (!MetrixStringHelper.isNullOrEmpty(fieldDesc)) {
						mDialogDescAddField.setText(fieldDesc);
                        mAddFieldDescDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Update"), addFieldDescListener);
                    } else
                        mAddFieldDescDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("OK"), addFieldDescListener);
                    mAddFieldDescDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("Cancel"), addFieldDescListener);
                    mAddFieldDescDialog.show();
                    return true;
				}
			});
		}
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerFieldAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMF\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldAddActivity.this, MetrixDesignerFieldActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
                        intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
                        MetrixActivityHelper.startNewActivity(MetrixDesignerFieldAddActivity.this, intent);
                    } else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mIsCustomField = (CheckBox) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.is_custom_field"));
		mTable = (Spinner) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.table_name"));
		mCustomColumn = (EditText) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.custom_column_name"));
		mDesc = (EditText) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.description"));
		mDescLabel = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.description_label"));
		mTableLabel = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.table_name_label"));
		mCustomColumnLabel = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.custom_column_name_label"));
		mIsButtonLabel = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.is_button_label"));
		mIsButton = (CheckBox) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.is_button"));

		TextView mAdd = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.add_field"));
		TextView mCustomField = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.custom_field"));

		AndroidResourceHelper.setResourceValues(mAdd, "AddField");
		AndroidResourceHelper.setResourceValues(mCustomField, "CustomField");
		AndroidResourceHelper.setResourceValues(mIsButtonLabel, "Button");
		AndroidResourceHelper.setResourceValues(mDescLabel, "Description");
		AndroidResourceHelper.setResourceValues(mTableLabel, "Table");
		AndroidResourceHelper.setResourceValues(mCustomColumnLabel, "Name");

		// add Required hint to Custom Column
		mCustomColumn.setHint(AndroidResourceHelper.getMessage("Required"));
		mCustomColumn.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

		// start off by hiding custom field controls and setting isCustomField to UNCHECKED
		mIsCustomField.setChecked(false);
		mCustomColumn.setVisibility(View.GONE);
		mCustomColumnLabel.setVisibility(View.GONE);

		mIsCustomField.setOnClickListener(new View.OnClickListener() {
											  @Override
											  public void onClick(View v) {
												  if (mIsCustomField.isChecked()) {
													  if (hasSingleTableOrColumnSelected()) {
														  AlertDialog mConfirmationDialog = new AlertDialog.Builder(mCurrentActivity).create();
														  mConfirmationDialog.setMessage(AndroidResourceHelper.getMessage("UnsavedTableOrFieldData"));
														  mConfirmationDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), unSavedCustomFieldChkDataListener);
														  mConfirmationDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), unSavedCustomFieldChkDataListener);
														  mConfirmationDialog.show();
														  return;
													  }

													  showHideControls();

												  } else {
													  mTable.setVisibility(View.VISIBLE);
													  mTableLabel.setVisibility(View.VISIBLE);

													  mCustomColumn.setVisibility(View.GONE);
													  mCustomColumnLabel.setVisibility(View.GONE);

													  mDesc.setVisibility(View.GONE);
													  mDescLabel.setVisibility(View.GONE);

													  mIsButtonLabel.setVisibility(View.GONE);
													  mIsButton.setVisibility(View.GONE);
													  mIsButton.setChecked(false);

													  mScrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1f));
												  }

												  reset();
											  }
										  }
		);

		mScrollView = (ScrollView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.scrollview"));
		mListViewArea = findViewById(mFieldAddResourceData.getExtraResourceID("R.id.listview_area"));

		mIsSelectAllFieldsLabel = (TextView) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.select_all_fields_label"));
		AndroidResourceHelper.setResourceValues(mIsSelectAllFieldsLabel, "SelectAllFields");
		mIsSelectAllFields = (CheckBox) findViewById(mFieldAddResourceData.getExtraResourceID("R.id.is_select_all_fields"));
		mIsSelectAllFields.setOnClickListener(new View.OnClickListener() {
			public void onClick(View checkboxView) {
				mStatusAllFieldsSelected = (((CheckBox) checkboxView).isChecked());
				if (!mStatusAllFieldsSelected)
					if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName))
						mCachedTableFieldsDescMap.remove(mSelectedTableName);

				setColumnListBasedOnSelectedTable();
			}
		});

		if (!mIsCustomField.isChecked()) {
			mScrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1f));
			mCustomColumn.setVisibility(View.GONE);
			mCustomColumnLabel.setVisibility(View.GONE);

			mDesc.setVisibility(View.GONE);
			mDescLabel.setVisibility(View.GONE);

			mListViewArea.setVisibility(View.VISIBLE);

			mIsSelectAllFieldsLabel.setVisibility(View.GONE);
			mIsSelectAllFields.setVisibility(View.GONE);
			mAttachmentFieldButton.setVisibility(View.GONE);
		}

		populateTableList();

		mTable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (hasSingleTableOrColumnSelected()) {
					if(prevSelectedIndex != mTable.getSelectedItemPosition()) {
						AlertDialog mConfirmationDialog = new AlertDialog.Builder(mCurrentActivity).create();
                        mConfirmationDialog.setMessage(AndroidResourceHelper.getMessage("UnsavedTableOrFieldData"));
                        mConfirmationDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), unSavedTableChangeDataListener);
                        mConfirmationDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), unSavedTableChangeDataListener);
                        mConfirmationDialog.show();
                        return;
					}
				}
				else {
					if (mCachedTableFieldsDescMap != null) mCachedTableFieldsDescMap.clear();
					setColumnListBasedOnSelectedTable();
				}

				prevSelectedIndex = mTable.getSelectedItemPosition();
			}

			public void onNothingSelected(AdapterView<?> parent) {
				setColumnListBasedOnSelectedTable();
			}
		});
	}

	DialogInterface.OnClickListener unSavedCustomFieldChkDataListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:	// Yes
					showHideControls();
					reset();
					break;

				case DialogInterface.BUTTON_NEGATIVE:	// No (do nothing)
					mIsCustomField.setChecked(false);
					break;
			}
		}
	};

	DialogInterface.OnClickListener unSavedTableChangeDataListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:	// Yes
					if(mCachedTableFieldsDescMap != null) mCachedTableFieldsDescMap.clear();
					setColumnListBasedOnSelectedTable();

					break;

				case DialogInterface.BUTTON_NEGATIVE:	// No (do nothing)
					try {
						MetrixControlAssistant.setValue(mTable, mSelectedTableName);
					}
					catch (Exception e)
					{
						LogManager.getInstance(mCurrentActivity).error(e);
					}
					break;
			}
		}
	};

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mFieldAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				mAddFieldDialog = new AlertDialog.Builder(this).create();

				// if IS custom, make sure that the COLUMN name entered (is not-null) AND (does not already exist in MM_FIELD for table CUSTOM, screen SCREEN_ID)
				// if NOT custom, make sure that both TABLE and COLUMN have non-null spinner selections
				if (mIsCustomField.isChecked()) {
					String customColName = mCustomColumn.getText().toString();
					if (MetrixStringHelper.isNullOrEmpty(customColName)) {
                        Toast.makeText(this, AndroidResourceHelper.getMessage("AddFieldCustomColumnEmptyError"), Toast.LENGTH_LONG).show();
                        return;
					} else {
						int instancesOfColName = MetrixDatabaseManager.getCount("mm_field", String.format("table_name = 'CUSTOM' and screen_id = %1$s and column_name = '%2$s'", MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"), customColName));
						if (instancesOfColName > 0) {
                            Toast.makeText(this, AndroidResourceHelper.getMessage("AddFieldCustomColumnError"), Toast.LENGTH_LONG).show();
                            return;
						}
					}

                    mAddFieldDialog.setMessage(AndroidResourceHelper.getMessage("AddFieldConfirm"));

				} else {
					if (!hasSingleTableOrColumnSelected()) {
                        Toast.makeText(this, AndroidResourceHelper.getMessage("AddFieldTableColumnError"), Toast.LENGTH_LONG).show();
                        return;
					}

                    mAddFieldDialog.setMessage(AndroidResourceHelper.getMessage("AddFieldsConfirm"));
                }

                mAddFieldDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addFieldListener);
                mAddFieldDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addFieldListener);
                mAddFieldDialog.show();
            } catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		} else if (viewId == mFieldAddResourceData.getExtraResourceID("R.id.attachment_field")) {
			try {
				mSelectedTableName = MetrixControlAssistant.getValue(mTable);
				if (MetrixStringHelper.isNullOrEmpty(mSelectedTableName))
					return;

				AlertDialog.Builder addAttachmentFieldDescBuilder = new AlertDialog.Builder(mCurrentActivity);
				LayoutInflater inflater = mCurrentActivity.getLayoutInflater();
				View inflatedView = inflater.inflate(mFieldAddResourceData.getExtraResourceID("R.layout.zzmd_field_add_attachment_dialog"), null);
				addAttachmentFieldDescBuilder.setView(inflatedView);

				TextView mAddAttachmentDialogTitle = (TextView) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.field_add_attachment_dialog_title"));
				mAddAttachmentDialogTitle.setText(AndroidResourceHelper.getMessage("AddAttachmentSignatureFieldHeading1Arg", mSelectedTableName));

				TextView mAddAttachmentColumnLabel = (TextView) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.faad_column_name_label"));
				mAddAttachmentColumnLabel.setText(AndroidResourceHelper.getMessage("Column"));
				TextView mAddAttachmentDescLabel = (TextView) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.faad_description_label"));
				mAddAttachmentDescLabel.setText(AndroidResourceHelper.getMessage("Description"));

				TextView mAddControlTypeLabel = (TextView) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.faad_control_type_label"));
				mAddControlTypeLabel.setText(AndroidResourceHelper.getMessage("ControlType"));
				mAddControlType = (Spinner) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.faad_control_type"));
				ArrayList<String> controlTypeList = new ArrayList<>();
				controlTypeList.add("Signature");
				controlTypeList.add("Attachment");
				MetrixControlAssistant.populateSpinnerFromList(this, mAddControlType, controlTypeList);

				mAddAttachmentDialogColumn = (Spinner) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.faad_column_name"));
				ArrayList<String> columnList = getColumnNames(mSelectedTableName);
				columnList.add(0, "");
				ArrayList<Hashtable<String, String>> usedColumns = MetrixDatabaseManager.getFieldStringValuesList(String.format("select distinct column_name from mm_field where screen_id = %1$s and table_name = '%2$s'", MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"), mSelectedTableName));
				if (usedColumns != null && usedColumns.size() > 0) {
					for (int i = 0; i < usedColumns.size(); i++) {
						Hashtable<String, String> usedColumnItem = usedColumns.get(i);
						columnList.remove(usedColumnItem.get("column_name"));
					}
				}
				MetrixControlAssistant.populateSpinnerFromList(this, mAddAttachmentDialogColumn, columnList);

				mAddAttachmentDialogDesc = (EditText) inflatedView.findViewById(mFieldAddResourceData.getExtraResourceID("R.id.faad_description"));

				addAttachmentFieldDescBuilder.setPositiveButton(AndroidResourceHelper.getMessage("OK"), null);
				addAttachmentFieldDescBuilder.setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), null);
				mAddAttachmentFieldDialog = addAttachmentFieldDescBuilder.create();
				mAddAttachmentFieldDialog.setOnShowListener(new DialogInterface.OnShowListener() {
					// Override the show listener to allow validation to occur and leave the dialog up
					@Override
					public void onShow(DialogInterface dialogInterface) {
						Button okButton = ((AlertDialog) mAddAttachmentFieldDialog).getButton(AlertDialog.BUTTON_POSITIVE);
						okButton.setOnClickListener((View.OnClickListener) new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								try {
									controlType = MetrixControlAssistant.getValue(mAddControlType);
									if(MetrixStringHelper.isNullOrEmpty(controlType)) {
										Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("AddAttachmentFieldColumnError"), Toast.LENGTH_LONG).show();
										return;
									}
									mAddAttachmentDialogColumnValue = MetrixControlAssistant.getValue(mAddAttachmentDialogColumn);
									if (MetrixStringHelper.isNullOrEmpty(mAddAttachmentDialogColumnValue)) {
										Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("AddAttachmentFieldColumnError"), Toast.LENGTH_LONG).show();
										return;
									}
									mAddAttachmentDialogDescValue = MetrixControlAssistant.getValue(mAddAttachmentDialogDesc);

									// wire up OK button to call perform_generate_mobile_field
									if (SettingsHelper.getSyncPause(mCurrentActivity)) {
										SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
										if (syncPauseAlertDialog != null) {
											syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
												@Override
												public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
													startAddAttachmentFieldListener();
												}
											});
										}
									}
									else
										startAddAttachmentFieldListener();

									mAddAttachmentFieldDialog.dismiss();
								} catch (Exception e) {
									LogManager.getInstance().error(e);
								}
							}
						});

						Button cancelButton = ((AlertDialog) mAddAttachmentFieldDialog).getButton(AlertDialog.BUTTON_NEGATIVE);
						cancelButton.setOnClickListener((View.OnClickListener) new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								mAddAttachmentFieldDialog.dismiss();
							}
						});
					}
				});
				mAddAttachmentFieldDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	private boolean hasSingleTableOrColumnSelected() {
		Set<String> keys = mCachedTableFieldsDescMap.keySet();
		if(keys == null || (keys != null && keys.size() == 0))
			return false;
		else {
			HashMap<String, String> theFieldsMap = mCachedTableFieldsDescMap.get(keys.iterator().next());
			if (theFieldsMap == null || (theFieldsMap != null && theFieldsMap.size() == 0))
				return false;
			else
				return true;
		}
	}

	DialogInterface.OnClickListener addFieldListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:	// Yes
					// wire up Yes button to call perform_generate_mobile_field
					if(SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if(syncPauseAlertDialog != null)
						{
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddFieldListener();
								}
							});
						}
					}
					else
						startAddFieldListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:	// No (do nothing)
					break;
			}
		}
	};

	private void startAddFieldListener() {
		Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doFieldAddition() == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    MetrixDesignerFieldAddActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();
                            if (mAddFieldDialog != null) {
                                mAddFieldDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mAddFieldDialog != null) {
                    mAddFieldDialog.dismiss();
                }

                // start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddFieldInProgress"));
            }
        });

		thread.start();
	}

	private void startAddAttachmentFieldListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if(!MetrixStringHelper.isNullOrEmpty(controlType)) {
					if(controlType.equals("Attachment")) {
				if (doAttachmentFieldAddition() == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerFieldAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();
						}
					});
					return;
				}
					} else if(controlType.equals("Signature")) {
						if (doSignatureFieldAddition() == false) {
							MobileApplication.stopSync(mCurrentActivity);
							MobileApplication.startSync(mCurrentActivity);
							MetrixDesignerFieldAddActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();
								}
							});
							return;
						}
					}
				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddFieldInProgress"));
			}
			}
		});

		thread.start();
	}

	public static boolean doFieldAddition() {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

		try {
			String tableName = "CUSTOM";
			String columnName;
			String description;
			MetrixPerformMessage performGMF;

			boolean isCustom = mIsCustomField.isChecked();
			boolean isCustomButton = mIsButton.isChecked();

			String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
			String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = " + screenID);

			if (isCustom) {
				columnName = mCustomColumn.getText().toString();
				description = mDesc.getText().toString();

				params.put("screen_id", screenID);
				params.put("screen_type", screenType);
				params.put("table_name", tableName);
				params.put("column_name", columnName);
				if (!MetrixStringHelper.isNullOrEmpty(description)) {
					params.put("description", description);
				}
				if(isCustomButton) {
					params.put("control_type", "BUTTON"); // hard-coded
				}
				params.put("device_sequence", String.valueOf(device_id));
				params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

				performGMF = new MetrixPerformMessage("perform_generate_mobile_field", params);

			}
			else{
				JSONObject jsonTablesFieldsDescsObj = new JSONObject(mCachedTableFieldsDescMap);
				if(jsonTablesFieldsDescsObj == null)
					throw new Exception("jsonTablesFieldsDescsObj cannot be null.");

				String strJsonTablesFieldsDescsObj = jsonTablesFieldsDescsObj.toString();
				if(MetrixStringHelper.isNullOrEmpty(strJsonTablesFieldsDescsObj))
					throw new Exception("strJsonTablesFieldsDescsObj cannot be empty.");

				params.put("screen_id", screenID);
				params.put("tables_fields_descriptions", strJsonTablesFieldsDescsObj);
				params.put("device_sequence", String.valueOf(device_id));
				params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
				params.put("screen_type", screenType);

				performGMF = new MetrixPerformMessage("perform_generate_mobile_fields", params);
			}

			performGMF.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}

	public static boolean doAttachmentFieldAddition() {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

		try {
			String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
			String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = " + screenID);

			params.put("screen_id", screenID);
			params.put("screen_type", screenType);
			params.put("table_name", mSelectedTableName);
			params.put("column_name", mAddAttachmentDialogColumnValue);
			if (!MetrixStringHelper.isNullOrEmpty(mAddAttachmentDialogDescValue)) {
				params.put("description", mAddAttachmentDialogDescValue);
			}
			params.put("control_type", "ATTACHMENT"); // hard-coded
			params.put("device_sequence", String.valueOf(device_id));
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

			MetrixPerformMessage performGMF = new MetrixPerformMessage("perform_generate_mobile_field", params);
			performGMF.save();
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			return false;
		}

		return true;
	}

	public static boolean doSignatureFieldAddition() {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

		try {
			String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
			String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = " + screenID);

			params.put("screen_id", screenID);
			params.put("screen_type", screenType);
			params.put("table_name", mSelectedTableName);
			params.put("column_name", mAddAttachmentDialogColumnValue);
			if (!MetrixStringHelper.isNullOrEmpty(mAddAttachmentDialogDescValue)) {
				params.put("description", mAddAttachmentDialogDescValue);
			}
			params.put("control_type", "SIGNATURE"); // hard-coded
			params.put("device_sequence", String.valueOf(device_id));
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

			MetrixPerformMessage performGMF = new MetrixPerformMessage("perform_generate_mobile_field", params);
			performGMF.save();
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			return false;
		}

		return true;
	}

	private void populateTableList() {
		String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
		String screenType = MetrixDatabaseManager.getFieldStringValue("mm_screen", "screen_type", "screen_id = " + screenID);
		String existingFieldTableQuery = String.format("select distinct table_name from mm_field where table_name <> 'CUSTOM' and screen_id = %s order by table_name asc", screenID);

		if (!screenType.toLowerCase().contains("list")) {
			// Just populate table spinner with all tables from this screen's fields
			MetrixControlAssistant.populateSpinnerFromQuery(this, mTable, existingFieldTableQuery, true);
		} else {
			// If this is any kind of List screen, still get all tables from this screen's fields
			// but also include list_table_name from fields that don't have list_display_column defined
			// so long as they are real tables in the SQLite DB and aren't already table names on existing fields
			String qualifiedListTableQuery = String.format("select distinct list_table_name from mm_field where screen_id = %s and list_table_name is not null and (list_display_column is null or list_display_column = '') order by list_table_name asc", screenID);
			ArrayList<String> fullTableList = new ArrayList<String>();

			// Handle field table names
			ArrayList<Hashtable<String, String>> tableResult = MetrixDatabaseManager.getFieldStringValuesList(existingFieldTableQuery);
			if (tableResult != null && tableResult.size() > 0) {
				for (Hashtable<String, String> row : tableResult) {
					fullTableList.add(row.get("table_name"));
				}
			}

			// Tack on any list_table_name values that qualify (force uppercase to match)
			ArrayList<Hashtable<String, String>> listTableResult = MetrixDatabaseManager.getFieldStringValuesList(qualifiedListTableQuery);
			if (listTableResult != null && listTableResult.size() > 0) {
				for (Hashtable<String, String> listTableRow : listTableResult) {
					String listTable = listTableRow.get("list_table_name").toUpperCase();
					if (!MetrixStringHelper.valueIsEqual(listTable, "CUSTOM") && !fullTableList.contains(listTable) && MetrixDesignerManager.clientDBContainsTable(listTable.toLowerCase()))
						fullTableList.add(listTable);
				}
			}

			// Ensure alphabetical order after inserting into this list from multiple queries
			Collections.sort(fullTableList);
			fullTableList.add(0, "");	// manually add empty item to top of the list

			MetrixControlAssistant.populateSpinnerFromList(this, mTable, fullTableList);
		}
	}

	private void populateColumnList(ArrayList<String> columnList) {
		ArrayList<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

		if (columnList != null && columnList.size() > 0) {
			for (int i = 0; i < columnList.size(); i++) {
				HashMap<String, String> row = new HashMap<String, String>();

				String fieldName = columnList.get(i);
				row.put("mm_field.field_name", fieldName);

				if (mStatusAllFieldsSelected)
					row.put("checkboxState", "Y");
				else {
					if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName)) {
						HashMap<String, String> theFieldsMap = mCachedTableFieldsDescMap.get(mSelectedTableName);
						if (theFieldsMap != null && theFieldsMap.containsKey(fieldName))
							row.put("checkboxState", "Y");
					}
				}

				String checkboxState = row.get("checkboxState");
				HashMap<String, String> theFieldsMap;

				if (!MetrixStringHelper.isNullOrEmpty(checkboxState) && checkboxState.compareToIgnoreCase("Y") == 0) {

					if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName)) {
						theFieldsMap = mCachedTableFieldsDescMap.get(mSelectedTableName);
						if (theFieldsMap != null && !theFieldsMap.containsKey(fieldName))
							theFieldsMap.put(fieldName, "");
					}
					else{
						HashMap<String, String> newFieldMap = new HashMap<String, String>();
						newFieldMap.put(fieldName, "");
						mCachedTableFieldsDescMap.put(mSelectedTableName, newFieldMap);
					}
				}

				if((!MetrixStringHelper.isNullOrEmpty(checkboxState) && checkboxState.compareToIgnoreCase("N") == 0)
						|| MetrixStringHelper.isNullOrEmpty(checkboxState)) {
					if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName)) {
						theFieldsMap = mCachedTableFieldsDescMap.get(mSelectedTableName);
						if (theFieldsMap != null && theFieldsMap.containsKey(fieldName))
                            theFieldsMap.remove(fieldName);
                    }
				}

				table.add(row);
			}

			mIsSelectAllFieldsLabel.setVisibility(View.VISIBLE);
			mIsSelectAllFields.setVisibility(View.VISIBLE);
			if (mIsStandardScreen)	// only show this button if we have a STANDARD screen in context
				mAttachmentFieldButton.setVisibility(View.VISIBLE);
			mListViewArea.setVisibility(View.VISIBLE);
		}
		else{
			mIsSelectAllFieldsLabel.setVisibility(View.GONE);
			mIsSelectAllFields.setVisibility(View.GONE);
			mAttachmentFieldButton.setVisibility(View.GONE);
			mListViewArea.setVisibility(View.GONE);
		}

		mIsSelectAllFields.setChecked(false);
		mStatusAllFieldsSelected = false;

		mFieldAdapter = new FieldListAdapter(this, table, mFieldAddResourceData.ListViewItemResourceID, mFieldAddResourceData.ExtraResourceIDs);
		mListView.setAdapter(mFieldAdapter);

	}

	public static class FieldListAdapter extends DynamicListAdapter {
		static ViewHolder holder;
		private final String checkBoxText;

		public FieldListAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs) {
			super(context, table, listViewItemResourceID, lviElemResIDs);
			this.checkBoxText = AndroidResourceHelper.getMessage("Cb");
			checkSelectAllChkBox();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {

				vi = mInflater.inflate(mListViewItemResourceID, parent, false);
				holder = new ViewHolder();
				holder.mFieldName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_field__field_name"));
				holder.mBox = (CheckBox) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.checkboxState"));
				holder.mBox.setEnabled(mAllowChanges);
				holder.mBox.setText(checkBoxText);

				if (mAllowChanges) {
					holder.mBox.setOnClickListener(new View.OnClickListener() {
						public void onClick(View checkboxView) {
							int getPosition = (Integer) checkboxView.getTag();
							HashMap<String, String> dataRow = mListData.get(getPosition);
							int idMapPosition = mIdMap.get(dataRow);

							String checkState = "";
							if (((CheckBox) checkboxView).isChecked())
								checkState = "Y";
							else
								checkState = "N";

							String fieldName = dataRow.get("mm_field.field_name");

							if (!MetrixStringHelper.isNullOrEmpty(checkState) && checkState.compareToIgnoreCase("Y") == 0) {

								if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName)) {
									HashMap<String, String> theFieldsMap = mCachedTableFieldsDescMap.get(mSelectedTableName);
									if (theFieldsMap != null && !theFieldsMap.containsKey(fieldName))
										theFieldsMap.put(fieldName, "");
								}
								else{
									HashMap<String, String> newFieldMap = new HashMap<String, String>();
									newFieldMap.put(fieldName, "");
									mCachedTableFieldsDescMap.put(mSelectedTableName, newFieldMap);
								}
							}

							if((!MetrixStringHelper.isNullOrEmpty(checkState) && checkState.compareToIgnoreCase("N") == 0)
									|| MetrixStringHelper.isNullOrEmpty(checkState)) {
								if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName)) {
									HashMap<String, String> theFieldsMap = mCachedTableFieldsDescMap.get(mSelectedTableName);
									if (theFieldsMap != null && theFieldsMap.containsKey(fieldName))
                                        theFieldsMap.remove(fieldName);
                                }
							}

							dataRow.put("checkboxState", checkState);
							mIdMap.put(dataRow, idMapPosition);

							checkSelectAllChkBox();
						}
					});
				}

				vi.setTag(holder);

			}else{
				holder = (ViewHolder) vi.getTag();
			}

			holder.mBox.setTag(position);

			HashMap<String, String> dataRow = mListData.get(position);
			holder.mFieldName.setText(dataRow.get("mm_field.field_name"));

			String chkState = dataRow.get("checkboxState");
			if (!MetrixStringHelper.isNullOrEmpty(chkState) && chkState.compareToIgnoreCase("Y") == 0)
				holder.mBox.setChecked(true);
			else
				holder.mBox.setChecked(false);

			return vi;
		}

		private void checkSelectAllChkBox() {
			int count = 0;
			if (mListData.size() == 0) return;

			for(int i = 0; i < mListData.size(); i++){
				HashMap<String, String> row = mListData.get(i);
				String state = row.get("checkboxState");
				if (!MetrixStringHelper.isNullOrEmpty(state) && state.compareToIgnoreCase("Y") == 0){
					count++;
				}
			}

			mIsSelectAllFields.setChecked((mListData.size() == count));
		}

		static class ViewHolder {
			TextView mFieldName;
			CheckBox mBox;
		}
	}

	private void setColumnListBasedOnSelectedTable() {
		try {
			mSelectedTableName = MetrixControlAssistant.getValue(mTable);

			if (!MetrixStringHelper.isNullOrEmpty(mSelectedTableName)) {
				ArrayList<String> columnList = getColumnNames(mSelectedTableName);

				// Remove any column names already used by existing fields on this screen
				ArrayList<Hashtable<String, String>> usedColumns = MetrixDatabaseManager.getFieldStringValuesList(String.format("select distinct column_name from mm_field where screen_id = %1$s and table_name = '%2$s'",
						MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"), mSelectedTableName));
				if (usedColumns != null && usedColumns.size() > 0) {
					for (int i = 0; i < usedColumns.size(); i++) {
						Hashtable<String, String> usedColumnItem = usedColumns.get(i);
						columnList.remove(usedColumnItem.get("column_name"));
					}
				}

				populateColumnList(columnList);
			} else
				populateColumnList(new ArrayList<String>());

		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	DialogInterface.OnClickListener addFieldDescListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:	// OK
					String desc = (mDialogDescAddField != null) ? mDialogDescAddField.getText().toString() : "";

					if (mSelectedItem != null) {
						String fieldName = mSelectedItem.get("mm_field.field_name");

						if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName)) {
							HashMap<String, String> theFieldsMap = mCachedTableFieldsDescMap.get(mSelectedTableName);
							if (theFieldsMap != null && theFieldsMap.containsKey(fieldName))
								theFieldsMap.put(fieldName, desc);
						}
					}
					break;

				case DialogInterface.BUTTON_NEGATIVE:	// Cancel (do nothing)
					break;
			}
		}
	};

	private String getFieldDescription() {
		if (mSelectedItem != null) {
			String fieldName = mSelectedItem.get("mm_field.field_name");

			if (mCachedTableFieldsDescMap != null && mCachedTableFieldsDescMap.containsKey(mSelectedTableName)) {
				HashMap<String, String> theFieldsMap = mCachedTableFieldsDescMap.get(mSelectedTableName);
				if (theFieldsMap != null && theFieldsMap.containsKey(fieldName))
					return theFieldsMap.get(fieldName);
			}
		}
		return null;
	}

	private void showHideControls() {
		mTable.setVisibility(View.GONE);
		mTableLabel.setVisibility(View.GONE);

		mCustomColumn.setVisibility(View.VISIBLE);
		mCustomColumnLabel.setVisibility(View.VISIBLE);

		mDesc.setVisibility(View.VISIBLE);
		mDescLabel.setVisibility(View.VISIBLE);

		mScrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		mIsSelectAllFields.setVisibility(View.GONE);
		mIsSelectAllFieldsLabel.setVisibility(View.GONE);
		mAttachmentFieldButton.setVisibility(View.GONE);

		if (mIsStandardScreen) {
			mIsButtonLabel.setVisibility(View.VISIBLE);
			mIsButton.setVisibility(View.VISIBLE);
		}
	}

	private void reset() {
		if (mCachedTableFieldsDescMap != null) mCachedTableFieldsDescMap.clear();
		prevSelectedIndex = 0;
		mTable.setSelection(0);
		mSelectedTableName = "";
		mStatusAllFieldsSelected = false;
		mDesc.setText("");
		mCustomColumn.setText("");
		mListViewArea.setVisibility(View.GONE);
	}
}

