package com.metrix.metrixmobile;

import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.DebriefListViewActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class DebriefProductRemove extends DebriefListViewActivity implements MetrixRecyclerViewListener, TextWatcher {
	private RecyclerView recyclerView;
	private MetadataRecyclerViewAdapter mAdapter;
	private int mSelectedPosition;
	private FloatingActionButton mNextButton;
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
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_debrief_product_to_remove_list);
		else
			setContentView(R.layout.debrief_product_to_remove_list);
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
		setListeners();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();
		FloatingActionButton mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		MetrixControlAssistant.setButtonVisibility(mSaveButton, View.GONE);
		populateList();		
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		mNextButton.setOnClickListener(this);
		mFABList.add(mNextButton);

		mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

		fabRunnable = this::showFABs;

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				MetrixActivityHelper.hideKeyboard(DebriefProductRemove.this);
			}

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

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		String requestId = MetrixDatabaseManager.getFieldStringValue("task", "request_id", String.format("task_id = %s", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		StringBuilder whereClause = new StringBuilder();
		whereClause.append(String.format("request_unit.request_id = '%s'", requestId));
		whereClause.append(" and request_unit.part_id is not null");
		whereClause.append(" and not exists (select disposition_id from part_disp where request_unit_id = request_unit.request_unit_id)");
		whereClause.append(" order by request_unit.request_unit_id");
		String query = MetrixListScreenManager.generateListQuery(this, "request_unit", whereClause.toString());
		
		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");		
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query = query + " limit " + maxRows;
		}

		MetrixCursor cursor = null;
		int rowCount = 0;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
			if (cursor != null && cursor.moveToFirst()) {
				rowCount = cursor.getCount();
				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
					table.add(row);
					cursor.moveToNext();
				}
			}

			table = MetrixListScreenManager.performScriptListPopulation(this, table);
		} finally {
			MetrixUIHelper.displayListRowCount(this, (TextView) findViewById(R.id.row_count), rowCount);
			if (cursor != null) {
				cursor.close();
			}
		}

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "request_unit.metrix_row_id", this);
			recyclerView.addItemDecoration(mBottomOffset);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	public void onClick(View v) 
	{
		MetrixWorkflowManager.advanceWorkflow(this);
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		this.populateList();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
	
	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired()
	{
		return true;
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		final String placeName = listItemData.get("place.name");
		mSelectedPosition = position;

		final OnClickListener modifyListener = (dialog, which) -> {};

		final OnClickListener deleteListener = (dialog, which) -> {
			try {
				HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
				String[] columnsToGet = new String[] { "place_id", "location" };
				Hashtable<String, String> personPlaces = MetrixDatabaseManager.getFieldStringValues("person_place", columnsToGet, "place_relationship='FOR_STOCK'");

				MetrixSqlData partDispData = new MetrixSqlData("part_disp", MetrixTransactionTypes.INSERT);

				String dispositionCode = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='DEFAULT_DISP_CODE_PRODUCT_REMOVAL'");
				columnsToGet = new String[] { "return_to_inv", "return_to_whse", "create_rma", "in_transit", "return_reason", "ship_via", "print_shipper", "usable" };
				Hashtable<String, String> dispositionCodeSettings = MetrixDatabaseManager.getFieldStringValues("disp_code", columnsToGet, "dispositon_code = '" + dispositionCode + "'");

				String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
				String dispositionId = String.valueOf(MetrixDatabaseManager.generatePrimaryKey("part_disp"));
				partDispData.dataFields.add(new DataField("disposition_id", dispositionId));
				partDispData.dataFields.add(new DataField("task_id", taskId));
				partDispData.dataFields.add(new DataField("request_unit_id", selectedItem.get("request_unit.request_unit_id")));
				partDispData.dataFields.add(new DataField("part_id", selectedItem.get("request_unit.part_id")));
				partDispData.dataFields.add(new DataField("serial_id", selectedItem.get("request_unit.serial_id")));
				partDispData.dataFields.add(new DataField("quantity", "1"));
				partDispData.dataFields.add(new DataField("rcv_place_id", personPlaces.get("place_id")));
				partDispData.dataFields.add(new DataField("rcv_location", personPlaces.get("location")));
				partDispData.dataFields.add(new DataField("dispositon_code", dispositionCode));
				partDispData.dataFields.add(new DataField("return_to_inv", dispositionCodeSettings.get("return_to_inv")));
				partDispData.dataFields.add(new DataField("return_to_whse", dispositionCodeSettings.get("return_to_whse")));
				partDispData.dataFields.add(new DataField("create_rma", dispositionCodeSettings.get("create_rma")));
				partDispData.dataFields.add(new DataField("in_transit", dispositionCodeSettings.get("in_transit")));
				partDispData.dataFields.add(new DataField("return_reason", dispositionCodeSettings.get("return_reason")));
				partDispData.dataFields.add(new DataField("ship_via", dispositionCodeSettings.get("ship_via")));
				partDispData.dataFields.add(new DataField("print_shipper", dispositionCodeSettings.get("print_shipper")));

				ArrayList<MetrixSqlData> partDispTrans = new ArrayList<MetrixSqlData>();
				partDispTrans.add(partDispData);

				MetrixTransaction transactionInfo = new MetrixTransaction();

				boolean successful = MetrixUpdateManager.update(partDispTrans, true, transactionInfo, AndroidResourceHelper.getMessage("PartDisp"), DebriefProductRemove.this);

				if (!successful) {
					MetrixUIHelper.showSnackbar(DebriefProductRemove.this, AndroidResourceHelper.getMessage("ErrorUploadPartDisposition"));
					return;
				} else {
					Intent intent = MetrixActivityHelper.createActivityIntent(DebriefProductRemove.this, DebriefProductRemove.class);
					MetrixActivityHelper.startNewActivityAndFinish(DebriefProductRemove.this, intent);
				}

			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		};

		MetrixDialogAssistant.showAlertDialog(AndroidResourceHelper.getMessage("Confirm"), AndroidResourceHelper.getMessage("RemoveProductConfirm1Arg", placeName), AndroidResourceHelper.getMessage("YesButton"), deleteListener, AndroidResourceHelper.getMessage("NoButton"), modifyListener, this);

	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
	//End Tablet UI Optimization
}
