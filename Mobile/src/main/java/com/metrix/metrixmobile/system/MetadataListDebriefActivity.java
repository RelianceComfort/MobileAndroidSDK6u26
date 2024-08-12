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
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
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
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
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

public class MetadataListDebriefActivity extends DebriefListViewActivity implements View.OnClickListener, MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private MetadataRecyclerViewAdapter mAdapter;
	private int mSelectedPosition;
	private HashMap<String, String> currentScreenProperties;
	private String primaryTable, whereClauseScript, linkedScreenId;
	private boolean allowDelete;
	private boolean allowModify;
	private FloatingActionButton mNextButton, mAddButton, mSaveButton;
	private boolean screenExistsInCurrentWorkflow, gotHereFromLinkedScreen;
	private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

	@SuppressLint("DefaultLocale")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		screenExistsInCurrentWorkflow = MetrixWorkflowManager.isScreenExistsInCurrentWorkflow(this, codeLessScreenId);
		gotHereFromLinkedScreen = (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"));

		if (shouldRunTabletSpecificUIMode) {
			//Even though we asked for the Tablet UI optimization, and the app is running in a tablet with landscape mode,
			//still the current screen is not in workflow, we won't apply the Tablet UI.
			if(screenExistsInCurrentWorkflow)
				setContentView(R.layout.tb_land_yycsmd_list_debrief_activity);
			else
				setContentView(R.layout.yycsmd_list_debrief_activity);
			//Conditionally show the Linked screen panel(Ex: STANDARD screen)
			setVisibilityOfLinkedScreenPanel();
		}
		else{
			setContentView(R.layout.yycsmd_list_debrief_activity);
		}

		recyclerView = findViewById(R.id.recyclerView);
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
				if (currentScreenProperties.containsKey("allow_delete")) {
					String strAllowDelete = currentScreenProperties.get("allow_delete");
					if (MetrixStringHelper.valueIsEqual(strAllowDelete, "Y"))
						allowDelete = true;
				}

				if (currentScreenProperties.containsKey("allow_modify")) {
					String strAllowModify = currentScreenProperties.get("allow_modify");
					if (MetrixStringHelper.valueIsEqual(strAllowModify, "Y"))
						allowModify = true;
				}
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	public void onStart() {
		if (shouldRunTabletSpecificUIMode)
			mLayout = (ViewGroup) findViewById(R.id.table_layout_data);
		else
			mLayout = (ViewGroup) findViewById(R.id.table_layout);

		super.onStart();
		setupActionBar();
		populateList();
	}

	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mAddButton = (FloatingActionButton) findViewById(R.id.save);
		mSaveButton = (FloatingActionButton) findViewById(R.id.update);
		mNextButton = (FloatingActionButton) findViewById(R.id.next);

		if (mAddButton != null)
			mAddButton.setOnClickListener(this);
		if (mSaveButton != null)
			mSaveButton.setOnClickListener(this);
		if (mNextButton != null)
			mNextButton.setOnClickListener(this);

		hideAllButtons();
		if (screenExistsInCurrentWorkflow && !gotHereFromLinkedScreen && !showListScreenLinkedScreenInTabletUIMode) {
			MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
			mFABList.add(mNextButton);
		}

		if (showListScreenLinkedScreenInTabletUIMode) {
			MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
			mFABList.add(mSaveButton);
		}

		if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId) && !gotHereFromLinkedScreen && !showListScreenLinkedScreenInTabletUIMode) {
			MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
			mFABList.add(mAddButton);
		}

		mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

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
	}

	private void populateList() {
		try {
			ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
			String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(mCurrentActivity), clientScriptDef);
			String query = MetrixListScreenManager.generateListQuery(primaryTable, result, codeLessScreenId);

			String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
			if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
				query = query + " limit " + maxRows;
			}

			MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, codeLessScreenId);
					table.add(row);
					cursor.moveToNext();
				}
				cursor.close();

				table = MetrixListScreenManager.performScriptListPopulation(this, codeLessScreenId, codeLessScreenName, table);
			}

			//overloaded constructor for cater code-less screens
			if (mAdapter == null) {
				mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
						R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, codeLessScreenId,primaryTable.toLowerCase() + ".metrix_row_id", this);
				recyclerView.addItemDecoration(mBottomOffset);
				recyclerView.setAdapter(mAdapter);
			} else {
				mAdapter.updateData(table);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	@Override
	public void onClick(View v) {
		if (scriptEventConsumesClick(this, v))
			return;

		Intent intent = null;
		switch (v.getId()) {
			case R.id.save:
				if (screenExistsInCurrentWorkflow) {
					if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
						int intLinkedScreenId = Integer.valueOf(linkedScreenId);
						String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
						if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
							if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD")) {
								intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class);
								intent.putExtra("ScreenID", intLinkedScreenId);
								intent.putExtra("NavigatedFromLinkedScreen", true);
								MetrixActivityHelper.startNewActivity(this, intent);
							}
							else
								MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
						}
					}
				}
				break;

			case R.id.next:
				MetrixWorkflowManager.advanceWorkflow(this);
				break;

			case R.id.update:
				if (shouldRunTabletSpecificUIMode) {
					if (anyOnStartValuesChanged()) {
						MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
						MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, String.format("MDLA-%s", codeLessScreenName));
						if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
							return;
						//we should show the same/current page
						intent = MetrixActivityHelper.createActivityIntent(this, MetadataListDebriefActivity.class);
						intent.putExtra("ScreenID", codeLessScreenId);
						MetrixActivityHelper.startNewActivityAndFinish(this, intent);
					}
				}
				else{
					if (anyOnStartValuesChanged()) {
						MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
						MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, String.format("MDLA-%s", codeLessScreenName));
						if(saveResult == MetrixSaveResult.ERROR)
							return;
						//we should show the same/current page
						intent = MetrixActivityHelper.createActivityIntent(this, MetadataListDebriefActivity.class);
						intent.putExtra("ScreenID", codeLessScreenId);
						MetrixActivityHelper.startNewActivityAndFinish(this, intent);
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
							// this is the (tablet) Save case, where we are not advancing workflow, nor have specified that we are finishing or going to a screen
							// we should show the same/current page
							Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, MetadataListDebriefActivity.class);
							intent.putExtra("ScreenID", codeLessScreenId);
							MetrixActivityHelper.startNewActivityAndFinish(currentActivity, intent);
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
		return MetrixWorkflowManager.isScreenExistsInCurrentWorkflow(this, codeLessScreenId) ? true : false;
	}

	@SuppressLint("DefaultLocale")
	protected void defineForm() {
		String standardScreenPrimaryTable = null;
		HashMap<String, String> standardScreenProperties = null;

		if (showListScreenLinkedScreenInTabletUIMode) {
			//This should be the standard screen
			if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
				int intStandardScreenScreenId = Integer.valueOf(linkedScreenId);
				standardScreenProperties = MetrixScreenManager.getScreenProperties(intStandardScreenScreenId);
				if(standardScreenProperties == null)return;
				if(!standardScreenProperties.containsKey("primary_table")) return;

				standardScreenPrimaryTable = standardScreenProperties.get("primary_table");
				if(MetrixStringHelper.isNullOrEmpty(standardScreenPrimaryTable)) return;

				standardScreenPrimaryTable = standardScreenPrimaryTable.toLowerCase();

				MetrixTableDef metrixTableDef = null;
				if (this.mActivityDef != null) {
					metrixTableDef = new MetrixTableDef(standardScreenPrimaryTable, this.mActivityDef.TransactionType);
					if (this.mActivityDef.Keys != null) {
						metrixTableDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys
								.get("metrix_row_id")), double.class));
					}
				} else {
					metrixTableDef = new MetrixTableDef(standardScreenPrimaryTable, MetrixTransactionTypes.INSERT);
				}

				this.mFormDef = new MetrixFormDef(metrixTableDef);
			}
		}
	}

	private void setVisibilityOfLinkedScreenPanel() {
		LinearLayout panelTwo = (LinearLayout) findViewById(R.id.panel_two);
		if (panelTwo != null) {
			if(!showListScreenLinkedScreenInTabletUIMode)
				panelTwo.setVisibility(View.GONE);
			else
				panelTwo.setVisibility(View.VISIBLE);
		}

		LinearLayout panelThree = (LinearLayout) findViewById(R.id.panel_three);
		if (panelThree != null) {
			LinearLayout.LayoutParams panelThreeParams = (LinearLayout.LayoutParams) panelThree.getLayoutParams();
			if (!showListScreenLinkedScreenInTabletUIMode) {
				panelThreeParams.weight = 0.8f;
				panelThree.setLayoutParams(panelThreeParams);
			} else {
				panelThreeParams.weight = 0.3f;
				panelThree.setLayoutParams(panelThreeParams);
			}
		}
	}
	//End Tablet UI Optimization

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if (taskIsComplete() || scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		mSelectedPosition = position;
		final OnClickListener modifyListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				if (selectedItem == null) return;

				if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
					int intLinkedScreenId = Integer.valueOf(linkedScreenId);
					String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
					if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
						if (screenType.toLowerCase().contains("standard")) {
							String metrixRowIdKeyName = String.format("%s.%s", primaryTable, "metrix_row_id");
							Intent intent = MetrixActivityHelper.createActivityIntent(MetadataListDebriefActivity.this, MetadataDebriefActivity.class, MetrixTransactionTypes.UPDATE,
									"metrix_row_id", selectedItem.get(metrixRowIdKeyName));
							intent.putExtra("ScreenID", intLinkedScreenId);
							intent.putExtra("NavigatedFromLinkedScreen", true);
							MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
						}
						else
							MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
					}
				}
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		final OnClickListener deleteListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				if (selectedItem == null) return;

				String metrixRowIdKeyName = String.format("%s.%s", primaryTable, "metrix_row_id");
				String metrixRowIdValue = selectedItem.get(metrixRowIdKeyName);

				Hashtable<String, String> keys = new Hashtable<String, String>();
				MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(primaryTable);
				for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
					String columnName = key.getKey();
					String columnNameWithTable = String.format("%s.%s", primaryTable, columnName);
					if (selectedItem.containsKey(columnNameWithTable)) {
						String currentKeyValue = selectedItem.get(columnNameWithTable);
						keys.put(columnName, currentKeyValue);
					}
				}

				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				MetrixUpdateManager.delete(MetadataListDebriefActivity.this, primaryTable.toLowerCase(), metrixRowIdValue, keys, String.format("MLDA-%s", codeLessScreenName), transactionInfo);
				mAdapter.getListData().remove(mSelectedPosition);
				mAdapter.notifyItemRemoved(mSelectedPosition);

				refreshDebriefNavigationList();
				reloadFreshActivity();
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		if (allowModify && allowDelete)
			MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), modifyListener, deleteListener, this);
		else if (allowModify && !allowDelete)
			MetrixDialogAssistant.showModifyDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), modifyListener, null, this);
		else if (!allowModify && allowDelete)
			MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), deleteListener, null, this);
		else
			MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSListItmModDelNotAllowed"));
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}
