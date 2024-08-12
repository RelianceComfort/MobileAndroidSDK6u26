package com.metrix.metrixmobile;

import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
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
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.Date;

public class CalendarException extends MetrixActivity implements View.OnClickListener {
	private FloatingActionButton mSaveButton, mNextButton;
	private CheckBox mAllDay;
	private String previousActivity;
	private String mStartCalendarDate;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar_exception);

		previousActivity = getIntent().getStringExtra("PreviousActivity");
		mStartCalendarDate = getIntent().getStringExtra("CalendarDay");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		if(!MetrixStringHelper.isNullOrEmpty(previousActivity) && previousActivity.contains("DayOverview"))
			nextActivity = DayOverviewActivity.class;
		else
			nextActivity = CalendarExceptionList.class;

		this.helpText = (AndroidResourceHelper.getMessage("ScrnDescCalExcept"));
	}


	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		mSaveButton = (FloatingActionButton) findViewById(R.id.save);
		mNextButton = (FloatingActionButton) findViewById(R.id.next);
//		mStartDateTime = (EditText) findViewById(R.id.person_cal_except__start_dttm);
//		mStartDateTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.calendar, 0);
//
//		mEndDateTime = (EditText) findViewById(R.id.person_cal_except__end_dttm);
//		mEndDateTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.calendar, 0);
//
//		mExceptionType = (Spinner) findViewById(R.id.person_cal_except__exception_type);
		//mAvailable = (CheckBox) MetrixControlAssistant.getControl(mFormDef, mLayout, "person_cal_except", "available");
		mAllDay = (CheckBox) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "all_day");

		mSaveButton.setOnClickListener(this);
		mSaveButton.setImageResource(R.drawable.ic_save_white_24dp);
		MetrixControlAssistant.setButtonVisibility(mNextButton, View.GONE);

		mAllDay.setOnClickListener(this);

//		MetrixControlAssistant.setOnItemSelectedListener(mExceptionType, new AdapterView.OnItemSelectedListener() {
//			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//				if (MetrixControlAssistant.spinnerIsReady(mExceptionType)) {
////					applyLineCodeSettings();
//				}
//			}
//
//			public void onNothingSelected(AdapterView<?> parent) {
//			}
//		});

		if (this.mActivityDef != null && this.mActivityDef.TransactionType == MetrixTransactionTypes.UPDATE) {
			this.displaySaveButtonOnAddNextBar();
		}
	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		if (this.mActivityDef == null || this.mActivityDef.TransactionType == MetrixTransactionTypes.INSERT) {
			try {
				Date date = MetrixDateTimeHelper.convertDateTimeFromUIToDate(mStartCalendarDate);
				String startDateTime = MetrixDateTimeHelper.convertDateTimeFromDateToUI(date);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "person_cal_except", "start_dttm", startDateTime);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "person_cal_except", "end_dttm", startDateTime);
				MetrixControlAssistant.setValue(mFormDef, mLayout, "person_cal_except", "person_id", SettingsHelper.getActivatedUser(this));
				MetrixControlAssistant.setValue(mFormDef, mLayout, "person_cal_except", "available", "N");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
		MetrixTableDef personCalExceptDef = null;
		if (this.mActivityDef != null) {
			personCalExceptDef = new MetrixTableDef("person_cal_except", this.mActivityDef.TransactionType);
			if (this.mActivityDef.Keys != null) {
				personCalExceptDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
			}
		} else {
			personCalExceptDef = new MetrixTableDef("person_cal_except", MetrixTransactionTypes.INSERT);
		}

//		personCalExceptDef.columns.add(new MetrixColumnDef(R.id.person_cal_except__exception_id, "exception_id", true, String.class, true, "EXCEPTION ID"));
//		personCalExceptDef.columns.add(new MetrixColumnDef(R.id.person_cal_except__person_id, "person_id", false, String.class, "Person ID"));
//		personCalExceptDef.columns.add(new MetrixColumnDef(R.id.person_cal_except__exception_type, "exception_type", true, String.class, MetrixControlCase.UPPER, "", "exception_type", "exception_type", "description", "", "", (
//				AndroidResourceHelper.getMessage("Type"))));
//		personCalExceptDef.columns.add(new MetrixColumnDef(R.id.person_cal_except__available, "available", false, String.class, "Available"));
//		personCalExceptDef.columns.add(new MetrixColumnDef(R.id.person_cal_except__start_dttm, "start_dttm", false, MetrixDateTime.class, "Start Datetime"));
//		personCalExceptDef.columns.add(new MetrixColumnDef(R.id.person_cal_except__end_dttm, "end_dttm", false, MetrixDateTime.class, "End Datetime"));

		this.mFormDef = new MetrixFormDef(personCalExceptDef);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == mAllDay.getId()) {
			boolean isChecked = ((CheckBox) MetrixControlAssistant.getControl(mFormDef, mLayout, "custom", "all_day")).isChecked();
			try {
				if (isChecked) {
						Date date = MetrixDateTimeHelper.convertDateTimeFromUIToDate(mStartCalendarDate);
						String startDateTime = MetrixDateTimeHelper.convertDateTimeFromDateToUI(date);
						date.setTime(date.getTime() + (24 * 60 * 60 * 1000) - 1000);
						String endDateTime = MetrixDateTimeHelper.convertDateTimeFromDateToUI(date);

						MetrixControlAssistant.setValue(mFormDef, mLayout, "person_cal_except", "start_dttm", startDateTime);
						MetrixControlAssistant.setValue(mFormDef, mLayout, "person_cal_except", "end_dttm", endDateTime);
				}
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
			return;
		}

		switch (v.getId()) {
			case R.id.save:
				MetrixTransaction transactionInfo = new MetrixTransaction();
				Boolean isFieldsAreValid = MetrixUpdateManager.fieldsAreValid(this, mLayout, mFormDef,
						false, null, false, false);

				if (isFieldsAreValid){
					MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef,
							transactionInfo, false, null, true,
							AndroidResourceHelper.getMessage("CalendarException"));

					if (saveResult == MetrixSaveResult.SUCCESSFUL)
						finish();
				}
				break;

			case R.id.update:
				if (anyOnStartValuesChanged()) {
					transactionInfo = new MetrixTransaction();
					MetrixSaveResult Result = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("CalendarException"));

					if (Result == MetrixSaveResult.SUCCESSFUL)
						finish();
					else {
						MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("FailedToUpdateTheRecord"));
					}
				} else {
					finish();
				}
				break;
			default:
				super.onClick(v);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * android.view.View.OnFocusChangeListener#onFocusChange(android.view.View,
	 * boolean)
	 */
//	@Override
//	public void onFocusChange(View v, boolean hasFocus) {
//		if (hasFocus) {
//			MobileUIHelper.showDateTimeDialog(this, v.getId());
//		}
//	}
//
//	private OnDateSetListener mStartDateSetListener = new DatePickerDialog.OnDateSetListener() {
//		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//			try {
//				MetrixControlAssistant.setValue(R.id.person_cal_except__start_dttm, mLayout, MetrixDateTimeHelper.formatDate(year, monthOfYear, dayOfMonth));
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	};
//
//	private DatePickerDialog.OnDateSetListener mEndDateSetListener = new DatePickerDialog.OnDateSetListener() {
//		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//			try {
//				MetrixControlAssistant.setValue(R.id.person_cal_except__end_dttm, mLayout, MetrixDateTimeHelper.formatDate(year, monthOfYear, dayOfMonth));
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	};
//
//	@Override
//	protected Dialog onCreateDialog(int id) {
//		switch (id) {
//		case 0:
//			String startDate = MetrixControlAssistant.getValue(R.id.person_cal_except__start_dttm, mLayout);
//			Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, startDate);
//
//			return new DatePickerDialog(this, mStartDateSetListener, calendar.get(calendar.YEAR), calendar.get(calendar.MONTH), calendar.get(calendar.DAY_OF_MONTH));
//		case 1:
//			String endDate = MetrixControlAssistant.getValue(R.id.person_cal_except__start_dttm, mLayout);
//			calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, endDate);
//
//			return new DatePickerDialog(this, mEndDateSetListener, calendar.get(calendar.YEAR), calendar.get(calendar.MONTH), calendar.get(calendar.DAY_OF_MONTH));
//		}
//		return null;
//	}
//
//	@Override
//	protected void onPrepareDialog(int id, Dialog dialog) {
//		switch (id) {
//		case 0:
//			String startDate = MetrixControlAssistant.getValue(R.id.person_cal_except__start_dttm, mLayout);
//			Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, startDate);
//
//			((DatePickerDialog) dialog).updateDate(calendar.get(calendar.YEAR), calendar.get(calendar.MONTH), calendar.get(calendar.DAY_OF_MONTH));
//			break;
//		case 1:
//			String endDate = MetrixControlAssistant.getValue(R.id.person_cal_except__start_dttm, mLayout);
//			calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, endDate);
//
//			((DatePickerDialog) dialog).updateDate(calendar.get(calendar.YEAR), calendar.get(calendar.MONTH), calendar.get(calendar.DAY_OF_MONTH));
//			break;
//		}
//	}
}

