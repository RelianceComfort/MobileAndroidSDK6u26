package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.MetrixActivity;

public class CommitmentList extends MetrixActivity implements TextWatcher, MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private MetadataRecyclerViewAdapter mAdapter;
	private AutoCompleteTextView mSearchCriteria;
	private Handler mHandler;
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.commitment_list);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
        mHandler = new Handler();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
		resourceStrings.add(new ResourceValueObject(R.id.row_count, "RowCount"));
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		populateList();
		setListeners();

		MetrixAutoCompleteHelper.populateAutoCompleteTextView(CommitmentList.class.getName(), mSearchCriteria, this);
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mSearchCriteria = (AutoCompleteTextView) findViewById(R.id.search_criteria);
		mSearchCriteria.addTextChangedListener(this);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				MetrixActivityHelper.hideKeyboard(CommitmentList.this);
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
			}
		});
	}

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		StringBuilder whereClause = new StringBuilder();
		String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
		Bundle bundle = getIntent().getExtras();
		
		whereClause.append(" where time_commit.actual_dttm is null");
		whereClause.append(" and task.person_id = '");
		whereClause.append(User.getUser().personId);
		whereClause.append("'");
		if (bundle != null) {
			if (!MetrixStringHelper.isNullOrEmpty(bundle.getString("task_id"))) {
				whereClause.append(" and task.task_id = " + bundle.getString("task_id"));
			}
			
			if (!MetrixStringHelper.isNullOrEmpty(bundle.getString("filter"))) {
				whereClause.append(" and commit_dttm < '" + MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true) + "'");
			}
		}

		whereClause.append(" order by time_commit.commit_dttm");

		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			whereClause.append(" limit " + maxRows);
		}

		String query = MetrixListScreenManager.generateListQuery(this, "time_commit", whereClause.toString(), searchCriteria);
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

				table = MetrixListScreenManager.performScriptListPopulation(this, table);
			}
	
		} finally {
			MetrixUIHelper.displayListRowCount(this, (TextView) findViewById(R.id.row_count), rowCount);
			if (cursor != null) {
				cursor.close();
			}
		}

		if (mAdapter == null) {
			mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "time_commit.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		mHandler.removeCallbacksAndMessages(null);
		mHandler.postDelayed(() -> {
			if (!CommitmentList.this.isDestroyed())
				this.populateList();
		}, 500);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		MetrixAutoCompleteHelper.saveAutoCompleteFilter(CommitmentList.class.getName(), mSearchCriteria);
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		Intent intent = MetrixActivityHelper.createActivityIntent(this, Commitment.class);
		MetrixCurrentKeysHelper.setKeyValue("time_commit", "metrix_row_id", listItemData.get("time_commit.metrix_row_id"));
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}
