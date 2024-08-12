package com.metrix.architecture.ui.widget;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TimePicker;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * a helper class to display UI controls, which includes Progress dialog,
 * datetimepicker etc.
 * 
 * @author elin
 * 
 */
@SuppressLint("InflateParams")
public class MobileUIHelper {
	private static final Map<String, String> timeSpanStrings = new HashMap<String, String>();

	/**
	 * This function display DateTimePicker with the current date time value
	 * 
	 * @param activity
	 * @param controlId
	 */
	public static void showDateTimeDialog(final Activity activity, final int controlId) {
		try {
			// Inflate the root layout
			final LinearLayout mDateTimeDialogView = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.date_time_dialog, null);
			boolean isFullScreenDialog = false;

			// Create the dialog (full screen if the screen is sufficiently small)
            final Dialog mDateTimeDialog;
            if (shouldShowDateTimePickerFullScreen()) {
				mDateTimeDialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
				mDateTimeDialogView.setBackgroundColor(Color.WHITE);
				isFullScreenDialog = true;
			} else
				mDateTimeDialog = new Dialog(activity);

            if (isFullScreenDialog && activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Adjust the padding of the buttons container in landscape mode to conserve vertical real estate
                final View controlButtons = mDateTimeDialogView.findViewById(R.id.ControlButtons);
                controlButtons.setPadding(controlButtons.getPaddingStart(), 0, controlButtons.getPaddingEnd(), 0);
            }

			// Grab widget instance
			final DateTimePicker mDateTimePicker = (DateTimePicker) mDateTimeDialogView.findViewById(R.id.DateTimePicker);
			mDateTimePicker.setIs24HourView(MetrixDateTimeHelper.use24HourTimeFormat());

			TextView control = (TextView) activity.findViewById(controlId);
			if (control != null) {
				if (!MetrixStringHelper.isNullOrEmpty(control.getText().toString())) {
					Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, control.getText().toString());

					mDateTimePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
					mDateTimePicker.updateTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
				}
			}

			// Start out by setting DatePicker to be visible
			final DatePicker dPicker = (DatePicker) mDateTimeDialogView.findViewById(R.id.DatePicker);
			final TimePicker tPicker = (TimePicker) mDateTimeDialogView.findViewById(R.id.TimePicker);
			dPicker.setVisibility(View.VISIBLE);
			tPicker.setVisibility(View.GONE);

			final Button btnTogglePicker = ((Button) mDateTimeDialogView.findViewById(R.id.TogglePicker));
			AndroidResourceHelper.setResourceValues(btnTogglePicker, "Time");
			btnTogglePicker.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					final ObjectAnimator anim1, anim2;
					if (dPicker.getVisibility() == View.VISIBLE) {
						// Slide left
						anim1 = ObjectAnimator.ofFloat(dPicker, "translationX", -mDateTimePicker.getWidth());
						anim1.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								dPicker.setVisibility(View.GONE);
							}
						});
						anim2 = ObjectAnimator.ofFloat(tPicker, "translationX", mDateTimePicker.getWidth(), 0);
						anim2.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationStart(Animator animation) {
								tPicker.setVisibility(View.VISIBLE);
							}
						});
						AndroidResourceHelper.setResourceValues(btnTogglePicker, "Date");
					} else {
						// Slide right
						anim1 = ObjectAnimator.ofFloat(tPicker, "translationX", mDateTimePicker.getWidth());
						anim1.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								tPicker.setVisibility(View.GONE);
							}
						});
						anim2 = ObjectAnimator.ofFloat(dPicker, "translationX", -mDateTimePicker.getWidth(), 0);
						anim2.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationStart(Animator animation) {
								dPicker.setVisibility(View.VISIBLE);
							}
						});
						AndroidResourceHelper.setResourceValues(btnTogglePicker, "Time");
					}

					final AnimatorSet set = new AnimatorSet();
					set.setInterpolator(new AccelerateDecelerateInterpolator());
					set.setDuration(300);
					set.playTogether(anim1, anim2);
					set.start();
				}
			});

			// Update demo TextViews when the "OK" button is clicked
			Button btnSetDateTime = ((Button) mDateTimeDialogView.findViewById(R.id.SetDateTime));
			AndroidResourceHelper.setResourceValues(btnSetDateTime, "OK");
			btnSetDateTime.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					mDateTimePicker.clearFocus();
					mDateTimePicker.forcePickerStateIntoCalendarIfNeeded();

					final TextView dateTimeView = activity.findViewById(controlId);

					@SuppressWarnings("deprecation")
					Date setDate = new Date(mDateTimePicker.get(Calendar.YEAR) - 1900, mDateTimePicker.get(Calendar.MONTH), mDateTimePicker
							.get(Calendar.DAY_OF_MONTH), mDateTimePicker.get(Calendar.HOUR_OF_DAY), mDateTimePicker.get(Calendar.MINUTE), 0);

					if (mDateTimePicker.is24HourView()) {
						String dateString = android.text.format.DateFormat.getDateFormat(activity).format(setDate);
						dateTimeView.setText(dateString + " " + mDateTimePicker.get(Calendar.HOUR_OF_DAY) + MetrixDateTimeHelper.getTimeSeparator()
								+ String.format("%02d", mDateTimePicker.get(Calendar.MINUTE)));
					} else {
						String dateOut = MetrixDateTimeHelper.convertDateTimeFromDateToUI(setDate);
						dateTimeView.setText(dateOut);
					}
					mDateTimeDialog.dismiss();
				}
			});

			// Cancel the dialog when the "Cancel" button is clicked
			final Button btnCancel = (Button) mDateTimeDialogView.findViewById(R.id.CancelDialog);
			AndroidResourceHelper.setResourceValues(btnCancel, "Cancel");
			btnCancel.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					mDateTimeDialog.cancel();
				}
			});

			// Reset Date and Time pickers when the "Reset" button is clicked
			final Button btnReset = (Button) mDateTimeDialogView.findViewById(R.id.ResetDateTime);
			AndroidResourceHelper.setResourceValues(btnReset, "Reset");
			btnReset.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					mDateTimePicker.reset();
				}
			});

			// No title on the dialog window
			mDateTimeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			// Set the dialog content view
			mDateTimeDialog.setContentView(mDateTimeDialogView);
			// Display the dialog
			mDateTimeDialog.show();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}
	
	/****
	 * Added by rawilk, for altering tab appearance due to recent target API level modification(14)
	 * @param tabHost
	 */
	public static void alteringTabWidget(TabHost tabHost) {
		
		try {
			
			if (tabHost == null)return;

			TabWidget tabWidget = tabHost.getTabWidget();
			int tabCount = tabWidget.getChildCount();

			for (int x = 0; x < tabCount; x++) {

				View tabView = tabWidget.getChildAt(x);
				LayoutParams tabViewParams = new LayoutParams(0, 100, 1.0F);
				tabView.setPadding(1, 0, 1, 0);
				tabView.setLayoutParams(tabViewParams);

				View titleView = tabView.findViewById(android.R.id.title);
				if (titleView != null) {
					if (titleView instanceof TextView) {

						TextView tabTextView = (TextView) titleView;
						LayoutParams tabTextViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
						
						//http://developer.android.com/reference/android/widget/TextView.html#setAllCaps%28boolean%29
						Method method = tabTextView.getClass().getMethod("setAllCaps", boolean.class);
						method.invoke(tabTextView, false);
						tabTextView.setLayoutParams(tabTextViewParams);
					}
				}
			}
			
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	/**
	 * Generates an "ago" string that specified how long it has been since the date string passed in
	 * @param createdDate
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	public static String jodaPeriod(String createdDate) {
		Date tastTextCreatedDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(MetrixDateTimeHelper.convertDateTimeFromDBToUI(createdDate));
		DateTime start = new DateTime(tastTextCreatedDate);

		Calendar calendar = Calendar.getInstance(Locale.getDefault());
		DateTime end = new DateTime(calendar.getTime());

		// period of 1 year and 7 days
		Period period = new Period(start, end);

		int noOfYears = period.getYears();
		if (noOfYears > 0) {
			if (noOfYears == 1)
				return getTimeString("YearAgo");
			else
				return MetrixDateTimeHelper.convertDateTimeFromDBToUI(createdDate);
		}

		int noOfMonths = period.getMonths();
		if (noOfMonths > 0) {
			if (noOfMonths == 1)
				return getTimeString("MonthAgo");
			else
				return getFormattedText("MonthsAgo1Arg", String.valueOf(noOfMonths));
		}

		int noOfWeeks = period.getWeeks();
		if (noOfWeeks > 0) {
			if (noOfWeeks == 1)
				return getTimeString("WeekAgo");
			else
				return getFormattedText("WeeksAgo1Arg", String.valueOf(noOfWeeks));
		}

		int noOfDays = period.getDays();
		if (noOfDays > 0) {
			if (noOfDays == 1)
				return getTimeString("DayAgo");
			else
				return getFormattedText("DaysAgo1Arg", String.valueOf(noOfDays));
		}

		int noOfHours = period.getHours();
		if (noOfHours > 0) {
			if (noOfHours == 1)
				return getTimeString("HourAgo");
			else
				return getFormattedText("HoursAgo1Arg", String.valueOf(noOfHours));
		}

		int noOfMinutes = period.getMinutes();
		if (noOfMinutes > 0) {
			if (noOfMinutes == 1)
				return getTimeString("MinuteAgo");
			else
				return getFormattedText("MinutesAgo1Arg", String.valueOf(noOfMinutes));
		}

		int noOfSeconds = period.getSeconds();
		if (noOfSeconds > 0) {
			if (noOfSeconds == 1)
				return getTimeString("SecondAgo");
			else
				return getFormattedText("SecondsAgo1Arg", String.valueOf(noOfSeconds));
		}

		return null;
	}

	/**
	 * Lazy load required message from {@link AndroidResourceHelper} to improve list scrolling performance.
	 * @param key message key
	 * @return String value for the received key
	 */
	private static String getTimeString(String key) {
		if(!timeSpanStrings.containsKey(key))
			timeSpanStrings.put(key, AndroidResourceHelper.getMessage(key));

		return timeSpanStrings.get(key);
	}

	private static String getFormattedText(String messageId, String param) {
		final String message = getTimeString(messageId);
		return AndroidResourceHelper.formatMessage(message, messageId, param);
	}

    private static boolean shouldShowDateTimePickerFullScreen() {
        int screenSize = MobileApplication.getAppContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL || screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL || screenSize == Configuration.SCREENLAYOUT_SIZE_UNDEFINED)
            return true;
        else
            return false;
    }
}
