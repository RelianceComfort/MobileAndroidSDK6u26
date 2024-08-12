package com.metrix.metrixmobile.policies;

import java.util.Hashtable;
import java.util.Locale;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;

public class MeterReadingsPolicy {
	private static final String MINIMUM_READING = "minimum_reading";
	private static final String MAXIMUM_READING = "maximum_reading";
	private static final String COUNT_METHOD = "count_method";
	private static final String MAX_PERCENT_CHANGE = "max_percent_change";
	private static final String MAX_QUANTITY_CHANGE = "max_quantity_change";
	private static final String ROLLOVER_VALUE = "rollover_value";
	private static final String ALLOW_ROLLOVER = "allow_rollover";

	public static boolean displayMessageForOutstandingReadings(String taskId, Activity activity) {
		String message = MeterReadingsPolicy.generateErrorForOutstandingReadings(taskId);
		if (!MetrixStringHelper.isNullOrEmpty(message)) {
			MetrixUIHelper.showSnackbar(activity, message);
			return true;
		} else {
			return false;
		}
	}

	public static boolean metersCanBeReadForThisTask(String taskId) {
		StringBuilder sql = new StringBuilder();
		sql.append("select product_meter.meter_name from product_meter, task ");
		sql.append("where not exists(");
		sql.append("select * from meter_readings where product_meter.product_id = meter_readings.product_id and ");
		sql.append("product_meter.meter_name = meter_readings.meter_name and meter_readings.task_id = ");
		sql.append(taskId);
		sql.append(") and product_meter.product_id = task.product_id and task.task_id = ");
		sql.append(taskId);

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(sql.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return false;
			}
			else {
				return true;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static String generateErrorForOutstandingReadings(String taskId) {
		StringBuilder sql = new StringBuilder();
		sql.append("select product_meter.meter_name from product_meter, task ");
		sql.append("where not exists(");
		sql.append("select * from meter_readings where product_meter.product_id = meter_readings.product_id and ");
		sql.append("product_meter.meter_name = meter_readings.meter_name and meter_readings.task_id = ");
		sql.append(taskId);
		sql.append(") and product_meter.product_id = task.product_id and task.task_id = ");
		sql.append(taskId);
		sql.append(" and product_meter.readings_required = 'Y'");

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(sql.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return "";
			}

			StringBuilder metersList = new StringBuilder();
			boolean addedMeter = false;
			while (cursor.isAfterLast() == false) {
				if (addedMeter) {
					metersList.append(", ");
				}
				metersList.append(cursor.getString(0));
				addedMeter = true;
				cursor.moveToNext();
			}

			return AndroidResourceHelper.getMessage("MetersMustBeRead1Arg", metersList.toString());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static String validate(Activity activity, MetrixFormDef formDef, ViewGroup layout) {
		String productId = MetrixControlAssistant.getValue(formDef, layout, "meter_readings", "product_id");
		String meterName = MetrixControlAssistant.getValue(formDef, layout, "meter_readings", "meter_name");
		String meterReading = MetrixControlAssistant.getValue(formDef, layout, "meter_readings", "meter_value");

		if (MetrixStringHelper.isNullOrEmpty(meterName) || MetrixStringHelper.isNullOrEmpty(meterReading)) {
			String notPopFieldNames = "";
			String nameString = AndroidResourceHelper.getMessage("Name");
			String valueString = AndroidResourceHelper.getMessage("Value");
			if (MetrixStringHelper.isNullOrEmpty(meterName) && MetrixStringHelper.isNullOrEmpty(meterReading))
				notPopFieldNames = String.format("%1$s, %2$s", nameString, valueString);
			else if (MetrixStringHelper.isNullOrEmpty(meterName))
				notPopFieldNames = nameString;
			else if (MetrixStringHelper.isNullOrEmpty(meterReading))
				notPopFieldNames = valueString;

			return AndroidResourceHelper.getMessage("RequiredFieldsNotPop1Arg", notPopFieldNames);
		}

		Hashtable<String, String> productMeter = MetrixDatabaseManager.getFieldStringValues("product_meter", new String[] { MINIMUM_READING, MAXIMUM_READING, COUNT_METHOD, MAX_PERCENT_CHANGE, MAX_QUANTITY_CHANGE, ROLLOVER_VALUE, ALLOW_ROLLOVER },
				"product_id = " + productId + " and meter_name = '" + meterName + "'");

		if (productMeter == null) {
			return "";
		}

		String maximumReading = productMeter.get(MAXIMUM_READING);
		String minimumReading = productMeter.get(MINIMUM_READING);
		String countMethod = productMeter.get(COUNT_METHOD);
		String maxPercentChange = productMeter.get(MAX_PERCENT_CHANGE);
		String maxQuantityChange = productMeter.get(MAX_QUANTITY_CHANGE);
		String allowRollover = productMeter.get(ALLOW_ROLLOVER);
		String rolloverValue = productMeter.get(ROLLOVER_VALUE);
		Number currentReading = MetrixStringHelper.isNullOrEmpty(meterReading)?0:MetrixFloatHelper.convertNumericFromUIToNumber(meterReading);

		if (!MetrixStringHelper.isNullOrEmpty(minimumReading) && currentReading.doubleValue()< MetrixFloatHelper.convertNumericFromDBToNumber(minimumReading).doubleValue()) {
			return AndroidResourceHelper.getMessage("MeterReadingBelowMinimum");
		}

		if (!MetrixStringHelper.isNullOrEmpty(maximumReading) && currentReading.doubleValue() > MetrixFloatHelper.convertNumericFromDBToNumber(maximumReading).doubleValue()) {
			return AndroidResourceHelper.getMessage("MeterReadingAboveMaximum");
		}

		String value = MetrixDatabaseManager.getFieldStringValue(true, "meter_readings", "meter_value", "product_id = " + productId + " and meter_name = '" + meterName + "'", null, null, null, "reading_dttm desc", "1");
		Number previousReading;

		if (MetrixStringHelper.isNullOrEmpty(value)) {
			previousReading = 0;
		} else {
			previousReading = MetrixFloatHelper.convertNumericFromDBToNumber(value);
		}
		NetChangeResult netChangeResult = calculateNetChange(productId, meterName, currentReading.doubleValue(), previousReading.doubleValue(), allowRollover, rolloverValue);

		if (!MetrixStringHelper.isNullOrEmpty(countMethod)) {
			if (countMethod.compareToIgnoreCase("RESET_ZERO") == 0) {
				netChangeResult.netChange = currentReading.doubleValue();
			} else {
				if ((countMethod.compareToIgnoreCase("INCREASING") == 0 && netChangeResult.netChange < 0) || (countMethod.compareToIgnoreCase("DECREASING") == 0 && netChangeResult.netChange > 0)
						|| (countMethod.compareToIgnoreCase("DECREASING_ABSOLUTE") == 0 && netChangeResult.netChange < 0) || (countMethod.compareToIgnoreCase("DIRECTIONAL_ABSOLUTE") == 0 && netChangeResult.netChange < 0)) {
					netChangeResult.netChange = netChangeResult.netChange * -1;
				}
			}

			if (previousReading.doubleValue() != 0) {
				if ((!netChangeResult.meterRolled) && (previousReading.doubleValue() > currentReading.doubleValue()) && (countMethod.compareToIgnoreCase("INCREASING") == 0)) {
					return AndroidResourceHelper.getMessage("PrevReadCantBeGreaterThanCurr", countMethod);
				}
			}

			if (!MetrixStringHelper.isNullOrEmpty(maxQuantityChange) && MetrixFloatHelper.convertNumericFromDBToNumber(maxQuantityChange).doubleValue() < netChangeResult.netChange) {
				return AndroidResourceHelper.getMessage("NetChangeExceedsMaxQtyChange");
			}

			if (!MetrixStringHelper.isNullOrEmpty(maxPercentChange) && netChangeResult.netChange > (previousReading.doubleValue() * (MetrixFloatHelper.convertNumericFromDBToNumber(maxPercentChange).doubleValue() / 100))) {
				return AndroidResourceHelper.getMessage("NetChangeExceedsMaxPctChange");
			}
		}

		return "";
	}

	private static NetChangeResult calculateNetChange(String productId, String meterName, double currentReading, double previousReading, String allowRollover, String rolloverValue) {
		double netChange;
		double rolloverValueNum = 0;
		boolean meterRolled = false;

		if (!MetrixStringHelper.isNullOrEmpty(rolloverValue)) {
			rolloverValue = MetrixFloatHelper.convertNumericFromDBToForcedLocale(rolloverValue, Locale.US);
			rolloverValueNum = Double.parseDouble(rolloverValue);
		}

		if ((allowRollover.compareToIgnoreCase("Y") == 0) && (!MetrixStringHelper.isNullOrEmpty(rolloverValue) && (previousReading < rolloverValueNum && currentReading < previousReading))) {
			meterRolled = true;
			netChange = (rolloverValueNum - previousReading) + currentReading + 1;
		} else {
			netChange = currentReading - previousReading;
		}

		return new NetChangeResult(meterRolled, netChange);
	}
}

