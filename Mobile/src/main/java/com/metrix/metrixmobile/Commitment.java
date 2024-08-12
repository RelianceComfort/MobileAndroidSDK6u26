package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.List;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.system.MetrixActivity;

import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class Commitment extends MetrixActivity implements View.OnClickListener, View.OnFocusChangeListener {
	private FloatingActionButton mSaveButton;
	private EditText mActualDttm;
	private List<FloatingActionButton> mFABList;
	private List<FloatingActionButton> mFABsToShow;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.commitment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
	}

	protected void displayPreviousCount() {
		int count = MetrixDatabaseManager.getCount("time_commit", "request_id='" + MetrixCurrentKeysHelper.getKeyValue("task", "request_id") + "'");

		try {
			MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List1Arg", String.valueOf(count)));
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		if (mFABList == null)
			mFABList = new ArrayList<FloatingActionButton>();
		else
			mFABList.clear();

		mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		mActualDttm = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "time_commit", "actual_dttm");
		mSaveButton.setOnClickListener(this);

		mFABList.add(mSaveButton);

		fabRunnable = this::showFABs;

		NestedScrollView scrollView = findViewById(R.id.scroll_view);
		mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
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
	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		try {
			mActualDttm = (EditText) MetrixControlAssistant.getControl(mFormDef, mLayout, "time_commit", "actual_dttm");
			if (MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(mActualDttm))) {
				MetrixControlAssistant.setValue(mActualDttm, MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT));
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		ArrayList<MetrixTableDef> tableDefs = new ArrayList<MetrixTableDef>();

		MetrixTableDef timeCommitDef = new MetrixTableDef("time_commit", MetrixTransactionTypes.UPDATE);
		timeCommitDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, MetrixCurrentKeysHelper.getKeyValue("time_commit", "metrix_row_id"), double.class));

		MetrixRelationDef globalCodeTableRelationDef = new MetrixRelationDef("time_commit", "response_type", "code_value", MetrixRelationOperands.LEFT_OUTER);
		MetrixTableDef globalCodeTableDef = new MetrixTableDef("global_code_table", MetrixTransactionTypes.SELECT, globalCodeTableRelationDef);
		globalCodeTableDef.constraints.add(new MetrixConstraintDef("code_name", MetrixConstraintOperands.EQUALS, "RESPONSE_TYPE", String.class));

		tableDefs.add(timeCommitDef);
		tableDefs.add(globalCodeTableDef);

		this.mFormDef = new MetrixFormDef(tableDefs);
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
		
		switch (v.getId()) {
		case R.id.save:
			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");

			if (MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, true, AndroidResourceHelper.getMessage("Commitment")) != MetrixSaveResult.ERROR) {
				this.finish();
			}
			
			break;
		default:
			super.onClick(v);
		}
	}
}