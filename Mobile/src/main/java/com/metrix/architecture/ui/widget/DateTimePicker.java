package com.metrix.architecture.ui.widget;

/**
 *
 * The widget source is from http://code.google.com/p/datetimepicker/
 *
 */

import java.lang.reflect.Method;
import java.util.Calendar;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.RelativeLayout;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import com.metrix.architecture.utilities.LogManager;
import com.metrix.metrixmobile.R;

public class DateTimePicker extends RelativeLayout implements OnDateChangedListener, OnTimeChangedListener {
	private DatePicker mDatePicker;
	private TimePicker mTimePicker;
	private Calendar mCalendar;

	// Constructor start
	public DateTimePicker(Context context) {
		this(context, null);
	}

	public DateTimePicker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DateTimePicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		// Get LayoutInflater instance
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// Inflate myself
		inflater.inflate(R.layout.datetimepicker, this, true);

		// Grab a Calendar instance
		mCalendar = Calendar.getInstance();

		// Init date picker
		mDatePicker = (DatePicker) findViewById(R.id.DatePicker);
		mDatePicker.init(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), this);

		// Init time picker
		mTimePicker = (TimePicker) findViewById(R.id.TimePicker);
		mTimePicker.setOnTimeChangedListener(this);

		//http://developer.android.com/reference/android/widget/DatePicker.html#setCalendarViewShown%28boolean%29
		if(mDatePicker != null)
			hideDefaultCalendarView(mDatePicker);
	}

	// Constructor end

	// Called every time the user changes DatePicker values
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		// Update the internal Calendar instance
		mCalendar.set(year, monthOfYear, dayOfMonth, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE));
	}

	// Called every time the user changes TimePicker values
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		// Update the internal Calendar instance
		mCalendar.set(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
	}

	// Convenience wrapper for internal Calendar instance
	public int get(final int field) {
		return mCalendar.get(field);
	}

	// Reset DatePicker, TimePicker and internal Calendar instance
	public void reset() {
		final Calendar c = Calendar.getInstance();
		updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		updateTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
	}

	// Convenience wrapper for internal Calendar instance
	public long getDateTimeMillis() {
		return mCalendar.getTimeInMillis();
	}

	// Convenience wrapper for internal TimePicker instance
	public void setIs24HourView(boolean is24HourView) {
		mTimePicker.setIs24HourView(is24HourView);
	}

	// Convenience wrapper for internal TimePicker instance
	public boolean is24HourView() {
		return mTimePicker.is24HourView();
	}

	// Convenience wrapper for internal DatePicker instance
	public void updateDate(int year, int monthOfYear, int dayOfMonth) {
		mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
	}

	// Convenience wrapper for internal TimePicker instance
	public void updateTime(int currentHour, int currentMinute) {
		mTimePicker.setCurrentHour(currentHour);
		mTimePicker.setCurrentMinute(currentMinute);
	}

	public void forcePickerStateIntoCalendarIfNeeded() {
		if (Build.VERSION.SDK_INT < 23) {
			// Manually update the internal Calendar instance if OS earlier than Android 6.0
			onDateChanged(mDatePicker, mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
			onTimeChanged(mTimePicker, mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
		}
	}

	/****
	 * Hide default Calendar component in DatePicker
	 * @param datePicker
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void hideDefaultCalendarView(DatePicker datePicker){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			datePicker.setCalendarViewShown(false);
	    }
		else{
			try {
				  Method method = mDatePicker.getClass().getMethod("setCalendarViewShown", boolean.class);
				  method.invoke(mDatePicker, false);
			  }
			  catch (Exception e) {
				  LogManager.getInstance().error(e);
			  }
		}
	}
}