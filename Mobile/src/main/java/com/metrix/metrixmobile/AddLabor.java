package com.metrix.metrixmobile;

// a designer based screen
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.policies.NonPartUsagePolicy;
import com.metrix.metrixmobile.system.MetrixActivity;

public class AddLabor extends MetrixActivity implements View.OnClickListener {
	private FloatingActionButton mSaveButton, mNextButton;
	private String mPlanedWorkDate;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_labor);

		mPlanedWorkDate = getIntent().getStringExtra("PlannedWorkDate");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		this.helpText = AndroidResourceHelper.getMessage("ScreenDescriptionDebriefLabor");
	}

//	protected void displayPreviousCount() {
//		int count = MetrixDatabaseManager.getCount("non_part_usage", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id") + " and line_code in (select line_code from line_code where npu_code='TIME')");
//
//		try {
//			MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), "View Previous (" + count + ")");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		mNextButton = (FloatingActionButton) findViewById(R.id.next);
		//mWorkDate = (EditText) findViewById(R.id.non_part_usage__work_dt);

		mSaveButton.setOnClickListener(this);
		mSaveButton.setImageResource(R.drawable.ic_save_white_24dp);
		MetrixControlAssistant.setButtonVisibility(mNextButton, View.GONE);

		if (this.mActivityDef != null && this.mActivityDef.TransactionType == MetrixTransactionTypes.UPDATE) {
			this.displaySaveButtonOnAddNextBar();
		}
	}

	public void applyLineCodeSettings() {
		NonPartUsagePolicy.applyLineCodeSettings(mFormDef, this, mLayout);
	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		if (this.mActivityDef == null || this.mActivityDef.TransactionType == MetrixTransactionTypes.INSERT) {
			try {
				if(MetrixStringHelper.isNullOrEmpty(mPlanedWorkDate))
					MetrixControlAssistant.setValue(mFormDef, mLayout, "non_part_usage", "work_dt", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_FORMAT));
				else
					MetrixControlAssistant.setValue(mFormDef, mLayout, "non_part_usage", "work_dt", mPlanedWorkDate);

				applyLineCodeSettings();
				MetrixControlAssistant.setValue(mFormDef, mLayout, "non_part_usage", "transaction_currency", User.getUser().getCurrencyToUse());
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef nonPartUsageDef = null;
		if (this.mActivityDef != null) {
			nonPartUsageDef = new MetrixTableDef("non_part_usage", this.mActivityDef.TransactionType);
			if (this.mActivityDef.Keys != null) {
				nonPartUsageDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
			}
		} else {
			nonPartUsageDef = new MetrixTableDef("non_part_usage", MetrixTransactionTypes.INSERT);
		}

		this.mFormDef = new MetrixFormDef(nonPartUsageDef);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.save:
				MetrixTransaction transactionInfo = new MetrixTransaction();//.getTransaction("task", "task_id");

				String quantity = MetrixControlAssistant.getValue(mFormDef, mLayout, "non_part_usage", "transaction_amount");

				if (!MetrixStringHelper.isNullOrEmpty(quantity))
					calculateSimplePricing();

				String errors = NonPartUsagePolicy.validate(mFormDef, this, mLayout);
				if (MetrixStringHelper.isNullOrEmpty(errors)) {
					MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, true, AndroidResourceHelper.getMessage("Labor"));
//				if(MetrixStringHelper.isNullOrEmpty(previousActivity))
//					nextActivity = DebriefNonPartUsageList.class;
//				else
//					nextActivity = DayOverviewActivity.class;
//
//				Calendar dayCalendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, mPlanedWorkDate);
//				DateParam dParam = new DateParam(dayCalendar.get(Calendar.YEAR), dayCalendar.get(Calendar.MONTH), dayCalendar.get(Calendar.DATE));
//
//				Intent intent = MetrixActivityHelper.createActivityIntent(this, nextActivity);
//				intent.putExtra(DateParam.DATE_PARAM, dParam);
//				MetrixActivityHelper.startNewActivityAndFinish(this, intent);

					finish();

				} else {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, errors);
				}

				break;
			case R.id.update:
				if (anyOnStartValuesChanged()) {
					transactionInfo = new MetrixTransaction();

					calculateSimplePricing();

					errors = NonPartUsagePolicy.validate(mFormDef, this, mLayout);
					if (MetrixStringHelper.isNullOrEmpty(errors)) {
						MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("Labor"));
//					if(MetrixStringHelper.isNullOrEmpty(previousActivity))
//						nextActivity = DebriefNonPartUsageList.class;
//					else
//						nextActivity = DayOverviewActivity.class;
//					Intent intent = MetrixActivityHelper.createActivityIntent(this, nextActivity);
//					intent.putExtra("CalendarDay", workDt);
//					intent.putExtra("LineCode", lineCode);
//					MetrixActivityHelper.startNewActivity(this, intent);
						finish();
					} else {
						MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, errors);
					}
				} else {
					finish();
				}
				break;
			default:
				super.onClick(v);
		}
	}

	private void calculateSimplePricing() {
		try {
			MetrixControlAssistant.setValue(mFormDef, mLayout, "non_part_usage", "quantity", MetrixControlAssistant.getValue(mFormDef, mLayout, "non_part_usage", "transaction_amount"));

			String simplePricingEnabled = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_MOBILE_SIMPLE_LABOR_PRICING'");
			if (simplePricingEnabled.compareToIgnoreCase("y") == 0) {
				MetrixControlAssistant.setValue(mFormDef, mLayout, "non_part_usage", "bill_price", User.getUser().laborRate);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "non_part_usage", "unadj_list_price", User.getUser().laborRate);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "non_part_usage", "billing_currency", User.getUser().getCurrencyToUse());
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

//	/*
//	 * (non-Javadoc)
//	 *
//	 * @see
//	 * android.view.View.OnFocusChangeListener#onFocusChange(android.view.View,
//	 * boolean)
//	 */
//	@Override
//	public void onFocusChange(View view, boolean hasFocus) {
//		if (hasFocus) {
//			if(view.getId() == R.id.non_part_usage__work_dt)
//				mWorkDate.requestFocus();
//
//		}
//	}
}

