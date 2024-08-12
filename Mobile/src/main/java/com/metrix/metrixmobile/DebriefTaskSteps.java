package com.metrix.metrixmobile;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
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
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.ui.widget.MetrixQuickLinksBar;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.DebriefListViewActivity;
import com.metrix.metrixmobile.system.MetadataDebriefActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DebriefTaskSteps extends DebriefListViewActivity implements View.OnClickListener, MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private FloatingActionButton mNextButton, mUpdateButton;
	private EditText mCompletedAsOf;
	private CheckBox mCompleted;
	private MetadataRecyclerViewAdapter mAdapter;
	private BottomOffsetDecoration mBottomOffset;
	private List<FloatingActionButton> mFABList;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (shouldRunTabletSpecificUIMode) {
			setContentView(R.layout.tb_land_debrief_task_steps_list);
			//Conditionally show the Linked screen panel(Ex: STANDARD screen)
			setVisibilityOfLinkedScreenPanel();
		}
		else {
			if(showListScreenLinkedScreenInTabletUIMode)
				setContentView(R.layout.yycsmd_debrief_activity);
			else
				setContentView(R.layout.debrief_task_steps_list);
		}
		recyclerView = findViewById(R.id.task_steps_recyclerview);
		if (recyclerView != null)
			MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
		setCustomListeners();
	}

	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.action_bar_error, "ViewSyncErrors"));
		super.onStart();

		if (shouldRunTabletSpecificUIMode)
			mLayout = (ViewGroup) findViewById(R.id.table_layout_data);
		else
			mLayout = (ViewGroup) findViewById(R.id.table_layout);

        FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.save);
        if (saveButton != null)
            MetrixControlAssistant.setButtonVisibility(saveButton,View.GONE);

		setupActionBar();
		populateList();
	}
	
	protected void setCustomListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		if(mNextButton != null){
			mNextButton.setOnClickListener(this);
			MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
			mFABList.add(mNextButton);
		}

		mUpdateButton = (FloatingActionButton) findViewById(R.id.update);
		if(mUpdateButton != null) {
			mUpdateButton.setOnClickListener(this);
			mFABList.add(mUpdateButton);
		}

		mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

		fabRunnable = this::showFABs;

		if (recyclerView != null) {
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
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (shouldRunTabletSpecificUIMode && showListScreenLinkedScreenInTabletUIMode) {
			ViewGroup secondPanel = (ViewGroup) findViewById(R.id.panel_two);
			if (secondPanel != null) {
				CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator_layout);
				FloatingActionButton hiddenSaveButton = (FloatingActionButton) coordinatorLayout.findViewById(R.id.save);
				mUpdateButton = (FloatingActionButton) coordinatorLayout.findViewById(R.id.update);
			
				mCompleted = (CheckBox)MetrixControlAssistant.getControl(mFormDef, mLayout, "task_steps", "completed");
				MetrixControlAssistant.setButtonVisibility(hiddenSaveButton, View.GONE);
				MetrixControlAssistant.setButtonVisibility(mUpdateButton, View.VISIBLE);
			
				mCompletedAsOf = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "task_steps", "completed_as_of");
	
				mUpdateButton.setOnClickListener(this);
				mCompletedAsOf.setOnClickListener(this);
				mCompletedAsOf.setOnFocusChangeListener(this);
				mCompleted.setOnClickListener(this);
			}

			//Hide list's next button and quick links bar, when the time of an update is happening
			hideListButtonAndBar();
		}

		Button viewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);
		if(viewPreviousEntriesButton != null)
			MetrixControlAssistant.setButtonVisibility(viewPreviousEntriesButton, View.GONE);
	}

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		if(recyclerView != null) {
			String whereClause = String.format("task_steps.task_unit_id is null and task_steps.task_id = %s order by task_steps.sequence", MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
			String query = MetrixListScreenManager.generateListQuery(this, "task_steps", whereClause);

			MetrixCursor cursor = null;
			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
			try {
				cursor = MetrixDatabaseManager.rawQueryMC(query, null);

				if (cursor == null || !cursor.moveToFirst()) {
					return;
				}

				final String requiredText = AndroidResourceHelper.getMessage("Required");
				final String optionalText = AndroidResourceHelper.getMessage("Optional");

				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
					String required = row.get("task_steps.required");
					if (required.compareToIgnoreCase("Y") == 0) {
						row.put("custom.required_text", requiredText);
					} else {
						row.put("custom.required_text", optionalText);
					}

					table.add(row);
					cursor.moveToNext();
				}

				table = MetrixListScreenManager.performScriptListPopulation(this, table);
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}

			if (mAdapter == null) {
				mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_checkbox,
						R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, R.id.list_checkbox,
						"task_steps.completed", 0, R.id.sliver,  null, "task_steps.metrix_row_id", this);
				recyclerView.setAdapter(mAdapter);
			} else {
				mAdapter.updateData(table);
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
		if (scriptEventConsumesClick(this, v))
			return;
		
		//mCompleted checkbox action
		if (mCompleted != null) {
			if (v.getId() == mCompleted.getId()) {
				String value = MetrixControlAssistant.getValue(mFormDef, mLayout, "task_steps", "completed");
				try {
					if (value != null && value.compareToIgnoreCase("Y") == 0) {
	                    MetrixControlAssistant.setValue(mFormDef, mLayout, "task_steps", "completed_as_of", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT));
					} else {
						MetrixControlAssistant.setValue(mFormDef, mLayout, "task_steps", "completed_as_of", "");
					}
				} catch (Exception e) {
					LogManager.getInstance(this).error(e);
				}
				return;
			}
		}
		
		switch (v.getId()) {
			case R.id.next:
				updateTaskSteps(true);
				MetrixWorkflowManager.advanceWorkflow(this);
				break;
			case R.id.update:
				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
				MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, DebriefTaskSteps.class, true, AndroidResourceHelper.getMessage("TaskSteps"));
				break;
			default:
				super.onClick(v);
		}
	}

	private void updateTaskSteps(boolean refreshScreen) {
		for (HashMap<String, String> item : mAdapter.getListData()) {
			if (item != null) {
				String checkState = item.get("checkboxState");
				String origCompletedValue = item.get("task_steps.completed");
					
				if (!MetrixStringHelper.isNullOrEmpty(checkState) && !MetrixStringHelper.valueIsEqual(checkState, origCompletedValue)) {
					String metrixRowId = item.get("task_steps.metrix_row_id");
					String stepId = item.get("task_steps.task_step_id");			
					String completedAsOf = "";
					if (MetrixStringHelper.valueIsEqual(checkState, "Y")) {
						completedAsOf = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, ISO8601.Yes, true);
					}
					
					ArrayList<MetrixSqlData> taskStepsToUpdate = new ArrayList<MetrixSqlData>();
					MetrixSqlData data = new MetrixSqlData("task_steps", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowId);
					data.dataFields.add(new DataField("metrix_row_id", metrixRowId));
					data.dataFields.add(new DataField("task_step_id", stepId));
					data.dataFields.add(new DataField("completed", checkState));
					data.dataFields.add(new DataField("completed_as_of", completedAsOf));
					taskStepsToUpdate.add(data);

					MetrixTransaction transactionInfo = new MetrixTransaction();
					MetrixUpdateManager.update(taskStepsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("Steps"), this);
				}
			}
		}

		if (refreshScreen) {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefTaskSteps.class);
			MetrixActivityHelper.startNewActivityAndFinish(this, intent);
		}
	}
	
	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired() {
		return true;
	}
	
	protected void defineForm() {
		if(showListScreenLinkedScreenInTabletUIMode){
			MetrixTableDef taskStepsDef = null;
			taskStepsDef = new MetrixTableDef("task_steps", MetrixTransactionTypes.UPDATE);
			taskStepsDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("task_steps", "metrix_row_id"), double.class));

			this.mFormDef = new MetrixFormDef(taskStepsDef);

			if(showListScreenLinkedScreenInTabletUIMode)
				this.standardActivityFullNameInTabletUIMode = "DebriefTaskStep";
		}
	}
	
	protected void renderTabletSpecificLayoutControls(){
		super.renderTabletSpecificLayoutControls();
		populateList();
		this.standardActivityFullNameInTabletUIMode = "DebriefTaskStep";
	}
	
	private void setVisibilityOfLinkedScreenPanel() {
		LinearLayout panelTwo = (LinearLayout) findViewById(R.id.panel_two);
		if(panelTwo != null){
			if(!showListScreenLinkedScreenInTabletUIMode)
				panelTwo.setVisibility(View.GONE);
			else
				panelTwo.setVisibility(View.VISIBLE);
		}

		LinearLayout panelThree = (LinearLayout) findViewById(R.id.panel_three);
		if(panelThree != null){
			LinearLayout.LayoutParams panelThreeParams = (LinearLayout.LayoutParams) panelThree.getLayoutParams();
			if(!showListScreenLinkedScreenInTabletUIMode){
				panelThreeParams.weight = 0.8f;
				panelThree.setLayoutParams(panelThreeParams);
			}
			else{
				panelThreeParams.weight = 0.3f;
				panelThree.setLayoutParams(panelThreeParams);
			}
		}
	}

	private void hideListButtonAndBar() {
		String metrixRowId = MetrixCurrentKeysHelper.getKeyValue("task_steps", "metrix_row_id");
		if(!MetrixStringHelper.isNullOrEmpty(metrixRowId)){
			if(mNextButton != null) MetrixControlAssistant.setButtonVisibility(mNextButton, View.GONE);

			MetrixQuickLinksBar mqlb = (MetrixQuickLinksBar) findViewById(R.id.quick_links_bar);
			if (mqlb != null)
				mqlb.setVisibility(View.GONE);
		}
	}
	//End Tablet UI Optimization

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(taskIsComplete() || scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		MetrixCurrentKeysHelper.setKeyValue("task_steps", "metrix_row_id", listItemData.get("task_steps.metrix_row_id"));
		MetrixCurrentKeysHelper.setKeyValue("task_steps", "task_step_id", listItemData.get("task_steps.task_step_id"));

		this.updateTaskSteps(false);

		if(shouldRunTabletSpecificUIMode){
			Intent intent = MetrixActivityHelper.createActivityIntent(this, DebriefTaskSteps.class);
			intent.putExtra("ShowListScreenLinkedScreenInTabletUIMode", true);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
		else {
			Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class, MetrixTransactionTypes.UPDATE,
					"metrix_row_id", listItemData.get("task_steps.metrix_row_id"));
			intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefTaskStep"));
			intent.putExtra("NavigatedFromLinkedScreen", true);
			MetrixActivityHelper.startNewActivity(this, intent);
		}
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}
