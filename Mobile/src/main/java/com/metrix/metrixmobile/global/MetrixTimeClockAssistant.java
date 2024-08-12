package com.metrix.metrixmobile.global;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.architecture.utilities.MetrixDateTimeHelper.ISO8601;
import com.metrix.metrixmobile.R;

public class MetrixTimeClockAssistant {
	private static final String TIME_CLOCK_STARTED = "START";
	private static final String TIME_CLOCK_PAUSED = "PAUSE";
	private static final String TIME_CLOCK_STOPPED = "STOP";
	private static final String TIME_CLOCK_RESUMED = "RESUME";

	public static void updateAutomatedTimeClock(Activity activity, String taskStatus) {
		int count = MetrixDatabaseManager.getCount("time_clock", "table_name = 'TASK' and active = 'Y' and column_name = 'TASK_STATUS'");

		if (count > 0) {
			ArrayList<Hashtable<String, String>> timeClocks = MetrixDatabaseManager.getFieldStringValuesList("time_clock", new String[] { "clock_id" }, "table_name = 'TASK' and active = 'Y' and column_name = 'TASK_STATUS'");
			for (Hashtable<String, String> timeClock : timeClocks) {
				String clockId = timeClock.get("clock_id");
				String action = MetrixDatabaseManager.getFieldStringValue("time_clock_rule", "action", "clock_id = " + clockId + " and column_value = '" + taskStatus + "'");

				if (!MetrixStringHelper.isNullOrEmpty(action)) {
					MetrixTimeClockEvents event = MetrixTimeClockEvents.start;

					if (action.compareToIgnoreCase(MetrixTimeClockAssistant.TIME_CLOCK_PAUSED) == 0) {
						event = MetrixTimeClockEvents.pause;
					} else if (action.compareToIgnoreCase(MetrixTimeClockAssistant.TIME_CLOCK_STOPPED) == 0) {
						event = MetrixTimeClockEvents.stop;
					} else if (action.compareToIgnoreCase(MetrixTimeClockAssistant.TIME_CLOCK_RESUMED) == 0) {
						event = MetrixTimeClockEvents.resume;
					}

					insertTimeClockEvent(activity, clockId, event);
				}
			}
		}
	}

	private static void insertTimeClockEvent(Activity activity, String clockId, MetrixTimeClockEvents event) {
		try {
			ArrayList<MetrixSqlData> eventsToInsert = new ArrayList<MetrixSqlData>();

			MetrixSqlData timeClockEvent = new MetrixSqlData("time_clock_event", MetrixTransactionTypes.INSERT);
			timeClockEvent.dataFields.add(new DataField("event_sequence", MetrixDatabaseManager.generatePrimaryKey("time_clock_event")));
			timeClockEvent.dataFields.add(new DataField("clock_id", clockId));
			timeClockEvent.dataFields.add(new DataField("event_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
			timeClockEvent.dataFields.add(new DataField("foreign_key_num1", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
			timeClockEvent.dataFields.add(new DataField("person_id", User.getUser().personId));

			String action = "";
			if (event == MetrixTimeClockEvents.start) {
				action = TIME_CLOCK_STARTED;
			} else if (event == MetrixTimeClockEvents.pause) {
				action = TIME_CLOCK_PAUSED;
			} else if (event == MetrixTimeClockEvents.resume) {
				action = TIME_CLOCK_RESUMED;
			} else if (event == MetrixTimeClockEvents.stop) {
				action = TIME_CLOCK_STOPPED;
			}

			timeClockEvent.dataFields.add(new DataField("time_clock_action", action));
			eventsToInsert.add(timeClockEvent);

			MetrixTransaction transactionInfo = new MetrixTransaction();
			MetrixUpdateManager.update(eventsToInsert, true, transactionInfo, AndroidResourceHelper.getMessage("TimeClockEvent"), activity);
			
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	public static void updateTimeClockStatus(Activity activity, String clockId, MetrixTimeClockEvents event, LinearLayout timeClock, boolean insertEvent) {
		ImageView timeClockStart = (ImageView) MetrixControlAssistant.getControl(R.id.time_clock_start_button, timeClock);
		ImageView timeClockPause = (ImageView) MetrixControlAssistant.getControl(R.id.time_clock_pause_button, timeClock);
		ImageView timeClockStop = (ImageView) MetrixControlAssistant.getControl(R.id.time_clock_stop_button, timeClock);

		if (event == MetrixTimeClockEvents.start) {
			timeClock.setBackgroundResource(R.drawable.time_clock_running_gradient_background);
			timeClockStart.setVisibility(View.GONE);
			timeClockPause.setVisibility(View.VISIBLE);
			timeClockStop.setImageResource(R.drawable.button_blue_stop);
		} else if (event == MetrixTimeClockEvents.pause) {
			timeClock.setBackgroundResource(R.drawable.time_clock_paused_gradient_background);
			timeClockStart.setVisibility(View.VISIBLE);
			timeClockPause.setVisibility(View.GONE);
			timeClockStop.setImageResource(R.drawable.button_blue_stop);
		} else if (event == MetrixTimeClockEvents.resume) {
			timeClock.setBackgroundResource(R.drawable.time_clock_running_gradient_background);
			timeClockStart.setVisibility(View.GONE);
			timeClockPause.setVisibility(View.VISIBLE);
			timeClockStop.setImageResource(R.drawable.button_blue_stop);
		} else if (event == MetrixTimeClockEvents.stop) {
			timeClock.setBackgroundResource(R.drawable.time_clock_gradient_background);
			timeClockStart.setVisibility(View.VISIBLE);
			timeClockPause.setVisibility(View.GONE);
			timeClockStart.setImageResource(R.drawable.button_blue_play);
			timeClockStop.setImageResource(R.drawable.button_grey_stop);
		}

		if (insertEvent) {
			MetrixTimeClockAssistant.insertTimeClockEvent(activity, clockId, event);
		}
	}

	public static MetrixTimeClockEvents getTimeClockStatus(Activity activity, String clockId, String taskId) {
		ArrayList<Hashtable<String, String>> events = MetrixDatabaseManager.getFieldStringValuesList("select metrix_row_id, event_sequence, event_dttm, time_clock_action from time_clock_event where clock_id = " + clockId + " and foreign_key_num1 = "
				+ taskId + " order by metrix_row_id");

		if (events != null && events.size() > 0) {
			String action = String.valueOf(events.get(events.size() - 1).get("time_clock_action"));
			if (action.compareToIgnoreCase(TIME_CLOCK_STARTED) == 0) {
				return MetrixTimeClockEvents.start;
			} else if (action.compareToIgnoreCase(TIME_CLOCK_PAUSED) == 0) {
				return MetrixTimeClockEvents.pause;
			} else if (action.compareToIgnoreCase(TIME_CLOCK_RESUMED) == 0) {
				return MetrixTimeClockEvents.resume;
			} else {
				return MetrixTimeClockEvents.stop;
			}
		} else {
			return MetrixTimeClockEvents.stop;
		}
	}

	public static long getCurrentElapsedTime(Activity activity, String clockId, String taskId) {
		long totalTime = 0;
		String action = "";
		Calendar lastActivityDttm = null;
		Calendar lastStartDttm = null;

		ArrayList<Hashtable<String, String>> events = MetrixDatabaseManager.getFieldStringValuesList("select metrix_row_id, event_sequence, event_dttm, time_clock_action from time_clock_event where clock_id = " + clockId + " and foreign_key_num1 = "
				+ taskId + " order by metrix_row_id");

		if (events != null && events.size() > 0) {
			double lastStartSequence = MetrixTimeClockAssistant.getMetrixRowIdOfLastStart(events);

			for (Hashtable<String, String> event : events) {
				if (Double.valueOf(event.get("metrix_row_id")) >= lastStartSequence) {
					action = event.get("time_clock_action");
					lastActivityDttm = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, event.get("event_dttm"), ISO8601.Yes);

					if ((action.compareToIgnoreCase(TIME_CLOCK_STARTED) == 0) || (action.compareToIgnoreCase(TIME_CLOCK_RESUMED) == 0)) {
						lastStartDttm = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, event.get("event_dttm"), ISO8601.Yes);
					} else if ((action.compareToIgnoreCase(TIME_CLOCK_PAUSED) == 0) || (action.compareToIgnoreCase(TIME_CLOCK_STOPPED) == 0)) {
						totalTime = totalTime + (lastActivityDttm.getTimeInMillis() - lastStartDttm.getTimeInMillis());
					}

					if (action.compareToIgnoreCase(TIME_CLOCK_STOPPED) == 0) {
						return totalTime;
					}
				}
			}

			if (action.compareToIgnoreCase(TIME_CLOCK_STARTED) == 0) {
				Calendar now = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS));
				totalTime = totalTime + (now.getTimeInMillis() - lastActivityDttm.getTimeInMillis());
			}
		}

		return totalTime;
	}

	private static double getMetrixRowIdOfLastStart(ArrayList<Hashtable<String, String>> events) {
		double sequence = 0;

		for (Hashtable<String, String> event : events) {
			if (String.valueOf(event.get("time_clock_action")).compareToIgnoreCase(TIME_CLOCK_STARTED) == 0) {
				sequence = Double.valueOf(event.get("metrix_row_id"));
			}
		}

		return sequence;
	}
}
