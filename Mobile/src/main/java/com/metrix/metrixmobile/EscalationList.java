package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.system.MetrixActivity;

public class EscalationList extends MetrixActivity implements View.OnClickListener, SimpleRecyclerViewAdapter.OnItemClickListener, SimpleRecyclerViewAdapter.OnItemLongClickListener, TextWatcher {
	private RecyclerView recyclerView;
	private SimpleRecyclerViewAdapter mSimpleAdapter;
	private String[] mFrom;
	private int[] mTo;
	private EditText mSearchCriteria;
	private Handler mHandler;
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.escalation_list);
        mHandler = new Handler();
        recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}
	
	public void onStart() {

		resourceStrings.add(new ResourceValueObject(R.id.search_criteria, "Search", true));
		resourceStrings.add(new ResourceValueObject(R.id.row_count, "RowCount"));
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		populateList();
		setListeners();
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mSearchCriteria = (EditText) findViewById(R.id.search_criteria);
		mSearchCriteria.addTextChangedListener(this);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				MetrixActivityHelper.hideKeyboard(EscalationList.this);
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
		StringBuilder query = new StringBuilder();
		query.append("select escalation.esc_id, escalation.due_dttm, g1.description, es.description, g2.description, escalation.foreign_key_num1, person.first_name, person.last_name, person.phone, person.email_address");
		query.append(" from escalation left outer join person on escalation.created_by = person.person_id");
		query.append(" left outer join global_code_table g1 on (escalation.esc_reason = g1.code_value and g1.code_name='ESC_REASON')");
		query.append(" left outer join global_code_table g2 on (escalation.priority = g2.code_value and g2.code_name='PRIORITY')");
		query.append(" left outer join esc_status es on escalation.esc_status = es.esc_status");
		
		String searchCriteria = MetrixControlAssistant.getValue(R.id.search_criteria, mLayout);
		if (!MetrixStringHelper.isNullOrEmpty(searchCriteria)) {
			query.append(" where (escalation.due_dttm like '%" + searchCriteria + "%' or escalation.esc_reason like '%" + searchCriteria + "%' or escalation.esc_status like '%" + searchCriteria + "%' or escalation.priority like '%" + searchCriteria
					+ "%' or escalation.foreign_key_num1 like '%" + searchCriteria + "%' or person.first_name like '%" + searchCriteria + "%' or person.last_name like '%" + searchCriteria + "%' or person.phone like '%" + searchCriteria
					+ "%' or person.email_address like '%" + searchCriteria + "%') and (escalation.esc_status is null or escalation.esc_status != 'CLOSED' and escalation.person_id = '");
			query.append(User.getUser().personId);
			query.append("' and escalation.table_name='TASK')");
		} else {
			query.append(" where (escalation.esc_status is null or  escalation.esc_status != 'CLOSED') and escalation.person_id = '");
			query.append(User.getUser().personId);
			query.append("' and escalation.table_name='TASK'");
		}

		query.append(" order by escalation.due_dttm");

		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");

		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query.append(" limit " + maxRows);
		}

		mTo = new int[] { R.id.escalation__esc_id, R.id.escalation__due_dttm, R.id.escalation__esc_reason, R.id.escalation__esc_status, R.id.escalation__priority, R.id.escalation__foreign_key_num1, R.id.person__created_user_name, R.id.person__phone, R.id.person__email_address };
		mFrom = new String[] { "escalation.esc_id", "escalation.due_dttm", "g1.description", "es.description", "g2.description", "escalation.foreign_key_num1", "person.created_user_name", "person.phone", "person.email_address" };

		MetrixCursor cursor = null;
		int rowCount = 0;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

		try {

			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
			
			if (cursor != null && cursor.moveToFirst()) {	
				
				rowCount = cursor.getCount();	
				
				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row = new HashMap<String, String>();
		
					row.put(mFrom[0], cursor.getString(0)); 
					row.put(mFrom[1], MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(1))); 
					row.put(mFrom[2], cursor.getString(2)); 
					row.put(mFrom[3], cursor.getString(3)); 
					row.put(mFrom[4], cursor.getString(4)); 
					row.put(mFrom[5], cursor.getString(5)); 
					row.put(mFrom[6], cursor.getString(6) + " " + cursor.getString(7)); 
					row.put(mFrom[7], cursor.getString(8)); 
					row.put(mFrom[8], cursor.getString(9)); 
		
					table.add(row);
					cursor.moveToNext();
				}
			}

		} finally {
			MetrixUIHelper.displayListRowCount(this, (TextView) findViewById(R.id.row_count), rowCount);
			if (cursor != null) {
				cursor.close();
			}
		}
		
		// fill in the grid_item layout
		if (mSimpleAdapter == null) {
			mSimpleAdapter = new SimpleRecyclerViewAdapter(table, R.layout.escalation_list_item, mFrom, mTo, new int[]{}, "escalation.esc_id");
			mSimpleAdapter.setClickListener(this);
			mSimpleAdapter.setLongClickListener(this);
			recyclerView.setAdapter(mSimpleAdapter);
		} else {
			mSimpleAdapter.updateData(table);
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		mHandler.removeCallbacksAndMessages(null);
		mHandler.postDelayed(() -> {
			if (!EscalationList.this.isDestroyed())
				this.populateList();
		}, 500);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		return super.onMetrixActionViewItemClick(menuItem);
	}

	@Override
	public void onSimpleRvItemClick(int position, Object item, View view) {
		scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this));
	}

	@Override
	public void onSimpleRvItemLongClick(int position, Object item, View view) {
		if(onCreateMetrixActionViewListner != null)
			onCreateMetrixActionViewListner.OnCreateMetrixActionView(recyclerView, position);
	}
}
