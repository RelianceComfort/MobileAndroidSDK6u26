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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataHeadingsRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.MetrixActivity;

public class TeamList extends MetrixActivity implements TextWatcher, MetrixRecyclerViewListener {
	private RecyclerView recyclerView;
	private MetadataHeadingsRecyclerViewAdapter mAdapter;
	private AutoCompleteTextView mSearchCriteria;
	private static int recyclerViewPosition = -1;
	private Handler mHandler;
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.team_list);
        mHandler = new Handler();
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
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

		// setListeners();
		populateList();
		setListeners();
		
		MetrixAutoCompleteHelper.populateAutoCompleteTextView(TeamList.class.getName(), mSearchCriteria, this);
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mSearchCriteria = (AutoCompleteTextView) findViewById(R.id.search_criteria);
		mSearchCriteria.addTextChangedListener(this);
		recyclerView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				MetrixActivityHelper.hideKeyboard(TeamList.this);
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
			}
		});
		initMetrixActionView(getMetrixActionBar().getCustomView());
	}
		
	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("team_member.team_id in (select distinct(team_id) from team_member where person_id = '");
		whereClause.append(User.getUser().personId);
		whereClause.append("') and team_member.person_id != '");
		whereClause.append(User.getUser().personId);
		whereClause.append("'");
		
		String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
		String query = MetrixListScreenManager.generateListQuery(this, "team_member", whereClause.toString(), searchCriteria);

		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query = query + " limit " + maxRows;
		}

		MetrixCursor cursor = null;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		int rowCount = 0;

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor != null && cursor.moveToFirst()) {
				
				rowCount = cursor.getCount();
				
				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);

					String firstName = row.get("person.first_name");
					String lastName = row.get("person.last_name");
					String fullName = "";
					if (!MetrixStringHelper.isNullOrEmpty(firstName) || !MetrixStringHelper.isNullOrEmpty(lastName)) {
						if (MetrixStringHelper.isNullOrEmpty(firstName)) {
							fullName = String.format("%s", lastName);
						} else {
							fullName = String.format("%1$s %2$s", firstName, lastName);
						}
					}
					row.put("custom.full_name", fullName);

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
			mAdapter = new MetadataHeadingsRecyclerViewAdapter(this, table, R.layout.list_item_basic,
					R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold,
					MetrixListScreenManager.getFirstDisplayedFieldName(this), R.layout.list_item_seperator,
					R.id.list_item_seperator__header, R.id.sliver, "team_member.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		mHandler.removeCallbacksAndMessages(null);
		mHandler.postDelayed(() -> {
			if (!TeamList.this.isDestroyed())
				this.populateList();
		}, 500);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public boolean OnCreateMetrixActionView(View view, Integer... position) {
		if(position.length > 0)
			recyclerViewPosition = position[0];
		
		MetrixActionView metrixActionView = getMetrixActionView();
		Menu menu = metrixActionView.getMenu();
		TeamListMetrixActionView.onCreateMetrixActionView(menu);
		return super.OnCreateMetrixActionView(view);
	}
	
	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case TeamListMetrixActionView.TEAM:
			Intent intent = MetrixActivityHelper.createActivityIntent(this, TeamMemberMap.class);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		}
		return super.onMetrixActionViewItemClick(menuItem);
	}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if(scriptEventConsumesListTap(TeamList.this, view, MetrixScreenManager.getScreenId(TeamList.this))) return;

		recyclerViewPosition = position;

		String personId = listItemData.get("person.person_id");
		Intent intent = MetrixActivityHelper.createActivityIntent(TeamList.this, TeamMemberAction.class);
		intent.putExtra("PERSON_ID", personId);
		MetrixActivityHelper.startNewActivity(TeamList.this, intent);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		if (onCreateMetrixActionViewListner != null)
			onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, position);
	}
}
