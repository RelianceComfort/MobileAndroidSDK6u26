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
import com.metrix.architecture.designer.MetadataHeadingsRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAutoCompleteHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.MetrixActivity;

public class CustomerList extends MetrixActivity implements View.OnClickListener, MetrixRecyclerViewListener, TextWatcher {
	private RecyclerView recyclerView;
	private MetadataHeadingsRecyclerViewAdapter mAdapter;
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
		setContentView(R.layout.customer_list);
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mHandler = new Handler();
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
		resourceStrings.add(new ResourceValueObject(R.id.row_count, "RowCount"));
		super.onStart();

		populateList();
		setListeners();

		MetrixAutoCompleteHelper.populateAutoCompleteTextView(CustomerList.class.getName(), mSearchCriteria, this);
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
				MetrixActivityHelper.hideKeyboard(CustomerList.this);
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
		String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
		String whereClause = " where place.whos_place = 'CUST' order by place.name asc";
		String query = MetrixListScreenManager.generateListQuery(this, "place", whereClause, searchCriteria);

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
		
					String address = row.get("address.address");
					if (MetrixStringHelper.isNullOrEmpty(address)) {
						row.put("custom.full_address", "");
					} else {
						String city = row.get("address.city");
						row.put("custom.full_address", address + ", " + city);					
					}
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
					R.id.list_item_seperator__header, R.id.sliver, "place.metrix_row_id", this);
			recyclerView.setAdapter(mAdapter);
		} else {
			mAdapter.updateData(table);
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		mHandler.removeCallbacksAndMessages(null);
		mHandler.postDelayed(() -> {
			if (!CustomerList.this.isDestroyed())
				this.populateList();
		}, 500);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		MetrixAutoCompleteHelper.saveAutoCompleteFilter(CustomerList.class.getName(), mSearchCriteria);
		if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

		String placeId = listItemData.get("place.place_id");
		MetrixCurrentKeysHelper.setKeyValue("place", "place_id", placeId);
		Intent intent = MetrixActivityHelper.createActivityIntent(this, CustomerTabs.class);
		MetrixActivityHelper.startNewActivity(this, intent);
	}

	@Override
	public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

	}
}
