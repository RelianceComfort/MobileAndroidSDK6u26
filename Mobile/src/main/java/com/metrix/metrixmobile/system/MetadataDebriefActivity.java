package com.metrix.metrixmobile.system;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixActivityDef;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixKeys;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDatabases;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class MetadataDebriefActivity extends DebriefActivity implements View.OnClickListener, View.OnFocusChangeListener, MetrixRecyclerViewListener {

	private HashMap<String, String> currentScreenProperties;
	private String primaryTable, linkedScreenId, whereClauseScript;
	private FloatingActionButton mAddButton, mSaveButton, mNextButton, mCorrectErrorButton;
	private Button mViewPreviousEntriesButton;
	private boolean screenExistsInCurrentWorkflow, isStandardUpdateOnly, gotHereFromLinkedScreen;

	private RecyclerView recyclerView;
	private BottomOffsetDecoration mBottomOffset;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;
	private MetadataRecyclerViewAdapter mAdapter;
	private int mSelectedPosition;
	private String listScreenPrimaryTable;
	private String listScreenWhereClauseScript;
	private boolean listScreenAllowModify;
	private boolean listScreenAllowDelete;
	private float mOffset;

	@SuppressLint("DefaultLocale")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		screenExistsInCurrentWorkflow = MetrixWorkflowManager.isScreenExistsInCurrentWorkflow(this, codeLessScreenId);
		isStandardUpdateOnly = MetrixScreenManager.isStandardUpdateOnly(codeLessScreenId);
		gotHereFromLinkedScreen = (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"));

		if (shouldRunTabletSpecificUIMode) {
			if (screenExistsInCurrentWorkflow)
				setContentView(R.layout.tb_land_yycsmd_debrief_activity);
			else
				setContentView(R.layout.yycsmd_debrief_activity);
		} else
			setContentView(R.layout.yycsmd_debrief_activity);

		recyclerView = findViewById(R.id.recyclerView);
		if (recyclerView != null)
			MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

		try {
			currentScreenProperties = MetrixScreenManager.getScreenProperties(codeLessScreenId);

			if (currentScreenProperties != null) {
				if (currentScreenProperties.containsKey("primary_table")) {
					primaryTable = currentScreenProperties.get("primary_table");
					if (MetrixStringHelper.isNullOrEmpty(primaryTable)) {
						throw new Exception("Primary table is required for code-less screen");
					}
					//This is needed (Ex: NON_PART_USAGE)
					primaryTable = primaryTable.toLowerCase();
				}
				if (currentScreenProperties.containsKey("linked_screen_id"))
					linkedScreenId = currentScreenProperties.get("linked_screen_id");
				if (currentScreenProperties.containsKey("where_clause_script"))
					whereClauseScript = currentScreenProperties.get("where_clause_script");
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}

		if (isStandardUpdateOnly) {
			ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
			if (MetrixStringHelper.isNullOrEmpty(primaryTable) || MetrixStringHelper.isNullOrEmpty(whereClauseScript) || clientScriptDef == null) {
				LogManager.getInstance().error(String.format("Standard update-only screen (ID %d) is missing metadata", codeLessScreenId));
				return;
			}

			String whereClause = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(this), clientScriptDef);
			String metrixRowID = MetrixDatabaseManager.getFieldStringValue(primaryTable, "metrix_row_id", whereClause);
			mActivityDef = new MetrixActivityDef(MetrixTransactionTypes.UPDATE, "metrix_row_id", metrixRowID);
		}
	}

	public void onStart() {
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
	}

	@SuppressLint("DefaultLocale")
	protected void defineForm() {
		MetrixTableDef metrixTableDef = null;
		if (this.mActivityDef != null) {
			metrixTableDef = new MetrixTableDef(primaryTable, this.mActivityDef.TransactionType);
			if (this.mActivityDef.Keys != null) {
				metrixTableDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
			}
		} else {
			metrixTableDef = new MetrixTableDef(primaryTable, MetrixTransactionTypes.INSERT);
		}

		this.mFormDef = new MetrixFormDef(metrixTableDef);
	}

	protected void displayPreviousCount() {
		if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
			try {
				if (!MetrixStringHelper.isNullOrEmpty(whereClauseScript)) {
					ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
					if (clientScriptDef != null) {
						String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(mCurrentActivity), clientScriptDef);
						int count = MetrixDatabaseManager.getCount(primaryTable, result);
						MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List1Arg", String.valueOf(count)));
					} else
						MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List"));
				} else
					MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List"));
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mCoordinatorLayout = findViewById(R.id.add_next_bar);
		mAddButton = (FloatingActionButton) findViewById(R.id.save);
		mSaveButton = (FloatingActionButton) findViewById(R.id.update);
		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		mCorrectErrorButton = (FloatingActionButton) findViewById(R.id.correct_error);
		mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);

		if (mAddButton != null) {
			mAddButton.setOnClickListener(this);
		}
		if (mSaveButton != null) {
			mSaveButton.setOnClickListener(this);
		}
		if (mNextButton != null) {
			mNextButton.setOnClickListener(this);
		}
		if (mCorrectErrorButton != null) {
			mCorrectErrorButton.setOnClickListener(this);
		}
		if (mViewPreviousEntriesButton != null) {
			mViewPreviousEntriesButton.setOnClickListener(this);
			AndroidResourceHelper.setResourceValues(mViewPreviousEntriesButton, "List1Arg");
		}

		hideAllButtons();
		mOffset = 0;
		if (!mHandlingErrors) {
			if (isStandardUpdateOnly) {
				if (screenExistsInCurrentWorkflow) {
					MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
					mFABList.add(mNextButton);
				}
				MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
				mFABList.add(mSaveButton);
			} else {
				if (gotHereFromLinkedScreen) {
						MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
						mFABList.add(mSaveButton);
				} else {
					if (screenExistsInCurrentWorkflow) {
						MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
						mFABList.add(mNextButton);
					}
					MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
					mFABList.add(mAddButton);

					if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
						MetrixControlAssistant.setButtonVisibility(mViewPreviousEntriesButton, View.VISIBLE);
						// The amount subtracted it equal to the button height plus the top and bottom margins of the button
						mOffset -= getResources().getDimension((R.dimen.button_height)) + (2*getResources().getDimension((R.dimen.md_margin)));
					}
				}
			}
		}

		mOffset += generateOffsetForFABs(mFABList);

		if (!shouldRunTabletSpecificUIMode || !screenExistsInCurrentWorkflow) {
			fabRunnable = this::showFABs;

			NestedScrollView scrollView = findViewById(R.id.scroll_view);
			mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(),(int)mOffset);
			scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
				if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
					fabHandler.removeCallbacks(fabRunnable);
					if(mFABsToShow != null)
						mFABsToShow.clear();
					else
						mFABsToShow = new ArrayList<>();

					hideFABs(mFABList);
					fabHandler.postDelayed(fabRunnable, fabDelay);
				}
			});
		} else if (shouldRunTabletSpecificUIMode && screenExistsInCurrentWorkflow) {
			fabRunnable = this::showFABs;

			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					if (dy > 0 || dy < 0) {
						fabHandler.removeCallbacks(fabRunnable);
						hideFABs(mFABList);
						fabHandler.postDelayed(fabRunnable, fabDelay);
					}
				}
			});

			if(mBottomOffset != null) {
				// Removing the BottomOffset Item Decoration so they don't stack
				recyclerView.removeItemDecoration(mBottomOffset);
			}
			mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));
			recyclerView.addItemDecoration(mBottomOffset);
		}
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onClick(View v) {
		//hide soft input keyboard before do a actions
		if(getCurrentFocus()!=null) {
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}

		if (scriptEventConsumesClick(this, v))
			return;

		Intent intent;
		switch (v.getId()) {
			case R.id.save:
				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, String.format("MDA-%s", codeLessScreenName));
				if (saveResult == MetrixSaveResult.ERROR)
					return;

				intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
				intent.putExtra("ScreenID", codeLessScreenId);
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
				break;

			case R.id.next:
				if (anyOnStartValuesChanged() && (!taskIsComplete())) {
					transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
					saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, String.format("MDA-%s", codeLessScreenName), true);
					if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
						return;
				} else
					MetrixWorkflowManager.advanceWorkflow(this);
				break;

			case R.id.update:
				if (anyOnStartValuesChanged()) {
					transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
					saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, String.format("MDA-%s", codeLessScreenName));
					if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
						return;

					if (isStandardUpdateOnly) {
						//we should show the same/current page
						intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
						intent.putExtra("ScreenID", codeLessScreenId);
						MetrixActivityHelper.startNewActivityAndFinish(this, intent);
					} else
						finish();
				} else {
					if (!isStandardUpdateOnly)
						finish();
				}
				break;

			case R.id.correct_error:
				transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, String.format("MDA-%s", codeLessScreenName));
				if (saveResult == MetrixSaveResult.ERROR)
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("Error"));
				break;

			case R.id.view_previous_entries:
				if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
					int intLinkedScreenId = Integer.valueOf(linkedScreenId);
					String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
					if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
						if (screenType.toLowerCase().contains("list")) {
							intent = MetrixActivityHelper.createActivityIntent(this, MetadataListDebriefActivity.class);
							intent.putExtra("ScreenID", intLinkedScreenId);
							intent.putExtra("NavigatedFromLinkedScreen", true);
							MetrixActivityHelper.startNewActivity(this, intent);
						} else
							MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
					}
				}
				break;

			default:
				super.onClick(v);
		}
	}

	private void hideAllButtons() {
		MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
		MetrixControlAssistant.setButtonVisibility(mSaveButton, View.GONE);
		MetrixControlAssistant.setButtonVisibility(mNextButton, View.GONE);
		MetrixControlAssistant.setButtonVisibility(mViewPreviousEntriesButton, View.GONE);
	}

	@Override
	public void showIgnoreErrorDialog(String message, Class<?> nextActivity, boolean finishCurrentActivity, boolean advanceWorkflow) {
		if (!message.endsWith(".")) {
			message = message + ".";
		}
		message = message + " " + AndroidResourceHelper.getMessage("ContinueWithoutSaving");

		final Class<?> finalNextActivity = nextActivity;
		final boolean finalFinishCurrentActivity = finishCurrentActivity;
		final Activity currentActivity = this;
		final boolean finalAdvanceWorkflow = advanceWorkflow;

		new AlertDialog.Builder(this).setCancelable(false).setMessage(message).setPositiveButton(AndroidResourceHelper.getMessage("Yes"),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (finalNextActivity != null) {
							if (finalFinishCurrentActivity) {
								Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, finalNextActivity);
								MetrixActivityHelper.startNewActivityAndFinish(currentActivity, intent);
							} else {
								Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, finalNextActivity);
								MetrixActivityHelper.startNewActivity(currentActivity, intent);
							}
						} else if (finalAdvanceWorkflow || finalFinishCurrentActivity) {
							// next activity and advance workflow should be mutually exclusive
							// finishCurrentActivity should still fire, regardless of whether we have a next activity specified
							if (finalAdvanceWorkflow)
								MetrixWorkflowManager.advanceWorkflow(currentActivity);

							if (finalFinishCurrentActivity)
								currentActivity.finish();
						} else {
							// this is the Save case, where we are not advancing workflow, nor have specified that we are finishing or going to a screen
							if (isStandardUpdateOnly) {
								//we should show the same/current page
								Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, MetadataDebriefActivity.class);
								intent.putExtra("ScreenID", codeLessScreenId);
								MetrixActivityHelper.startNewActivityAndFinish(currentActivity, intent);
							} else
								currentActivity.finish();
						}
					}
				}).setNegativeButton(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		}).create().show();
	}

	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired() {
		return true;
	}

	protected void renderTabletSpecificLayoutControls(){
		try {
			super.renderTabletSpecificLayoutControls();
			populateList();
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	@SuppressLint("DefaultLocale")
	private void populateList() throws Exception {
		if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
			int intLinkedScreenId = Integer.valueOf(linkedScreenId);
			String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
			if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
				if(screenType.toLowerCase().contains("list")) {
					HashMap<String, String> listScreenProperties = MetrixScreenManager.getScreenProperties(intLinkedScreenId);
					if (listScreenProperties != null) {
						if (listScreenProperties.containsKey("primary_table")) {
							listScreenPrimaryTable = listScreenProperties.get("primary_table");
							if (MetrixStringHelper.isNullOrEmpty(primaryTable)) {
								throw new Exception("Primary table is required for code-less screen");
							}
							//This is needed (Ex: NON_PART_USAGE)
							listScreenPrimaryTable = listScreenPrimaryTable.toLowerCase();
						}

						if (listScreenProperties.containsKey("where_clause_script"))
							listScreenWhereClauseScript = listScreenProperties.get("where_clause_script");

						if (listScreenProperties.containsKey("allow_delete")) {
							String strAllowDelete = listScreenProperties.get("allow_delete");
							if (MetrixStringHelper.valueIsEqual(strAllowDelete, "Y"))
								listScreenAllowDelete = true;
						}

						if (listScreenProperties.containsKey("allow_modify")){
							String strAllowModify = listScreenProperties.get("allow_modify");
							if (MetrixStringHelper.valueIsEqual(strAllowModify, "Y"))
								listScreenAllowModify = true;
						}

						if (!MetrixStringHelper.isNullOrEmpty(listScreenWhereClauseScript)) {
							try {
								ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(listScreenWhereClauseScript);
								if(clientScriptDef != null){
									String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(mCurrentActivity), clientScriptDef);
									String query = MetrixListScreenManager.generateListQuery(listScreenPrimaryTable, result, intLinkedScreenId);
									MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);

									if (cursor == null || !cursor.moveToFirst()) {
										return;
									}

									List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
									while (cursor.isAfterLast() == false) {
										HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, intLinkedScreenId);
										table.add(row);
										cursor.moveToNext();
									}
									cursor.close();

									table = MetrixListScreenManager.performScriptListPopulation(this, intLinkedScreenId, codeLessScreenName, table);

									//overloaded constructor for cater code-less screens
									if (mAdapter == null) {
										mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
												R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, intLinkedScreenId, listScreenPrimaryTable + ".metrix_row_id", this);
										recyclerView.setAdapter(mAdapter);
									} else {
										mAdapter.updateData(table);
									}
								}
							} catch (Exception e) {
								LogManager.getInstance(this).error(e);
							}
						}
					}
				}
				else
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
			}
		}
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if (taskIsComplete() || scriptEventConsumesListTap(this, view, linkedScreenIdInTabletUIMode)) return;

		mSelectedPosition = position;
		final OnClickListener modifyListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				if (selectedItem == null) return;

				String metrixRowIdKeyName = String.format("%s.%s", listScreenPrimaryTable, "metrix_row_id");
				Intent intent = MetrixActivityHelper.createActivityIntent(MetadataDebriefActivity.this, MetadataDebriefActivity.class, MetrixTransactionTypes.UPDATE, "metrix_row_id", selectedItem.get(metrixRowIdKeyName));
				intent.putExtra("ScreenID", codeLessScreenId);
				intent.putExtra("NavigatedFromLinkedScreen", true);
				MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);

			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		final OnClickListener deleteListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				if (selectedItem == null) return;

				String metrixRowIdKeyName = String.format("%s.%s", listScreenPrimaryTable, "metrix_row_id");
				String metrixRowIdValue = selectedItem.get(metrixRowIdKeyName);

				Hashtable<String, String> keys = new Hashtable<String, String>();
				MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(primaryTable);
				for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
					String columnName = key.getKey();
					String columnNameWithTable = String.format("%s.%s", listScreenPrimaryTable, columnName);
					if (selectedItem.containsKey(columnNameWithTable)) {
						String currentKeyValue = selectedItem.get(columnNameWithTable);
						keys.put(columnName, currentKeyValue);
					}
				}

				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				MetrixUpdateManager.delete(MetadataDebriefActivity.this, listScreenPrimaryTable.toLowerCase(), metrixRowIdValue, keys, String.format("MLDA-%s", codeLessScreenName), transactionInfo);
				mAdapter.getListData().remove(mSelectedPosition);
				mAdapter.notifyItemRemoved(mSelectedPosition);

				refreshDebriefNavigationList();
				reloadFreshActivity();

			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		if(listScreenAllowModify && listScreenAllowDelete)
			MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), modifyListener, deleteListener, this);
		else if(listScreenAllowModify && !listScreenAllowDelete)
			MetrixDialogAssistant.showModifyDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), modifyListener, null, this);
		else if(!listScreenAllowModify && listScreenAllowDelete)
			MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), deleteListener, null, this);
		else
			MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSListItmModDelNotAllowed"));
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
	//End Tablet UI Optimization
}

