package com.metrix.architecture.utilities;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.provider.Settings;

import com.metrix.architecture.database.MobileApplication;

public class MetrixDateTimeHelper {
	public static final String DATE_TIME_FORMAT = "date_time_format";
	public static final String DATE_FORMAT = "date_format";
	public static final String TIME_FORMAT = "time_format";
	public static final String DATE_TIME_FORMAT_WITH_SECONDS = "date_time_format_with_seconds";
	public static final String DATE_TIME_SEPARATOR = " ";

	public enum ISO8601 {
		Yes, No
	}
	
	public static final int YearPart = 1;
	public static final int MonthPart = 2;
	public static final int DayPart = 4;
	public static final int HourPart = 8;
	public static final int MinutePart = 16;
	public static final int SecondPart = 32;

	public static String adjustDate(String value, String formatType, int type, int duration) {
		Calendar calendar = getDate(formatType, value);
        calendar.add(type, duration);

        return MetrixDateTimeHelper.formatDate(formatType, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
	}

	public static String getCurrentDate(String formatType) {
		try {
			return getDateTimeFormat(formatType).format(new Date(System.currentTimeMillis()));
		} catch (Exception ex) {
			if(ex.getMessage()!=null)
				LogManager.getInstance().error(ex);
		}

		return "";
	}

	public static String getCurrentDate(String formatType, boolean dbVersion) {
		try {
			DateFormat dateFormatter = getDateTimeFormat(formatType);

			if (dbVersion)
				return convertDateTimeFromUIToDB(dateFormatter.format(new Date(System.currentTimeMillis())), formatType);
			else
				return dateFormatter.format(new Date(System.currentTimeMillis()));
		} catch (Exception ex) {
			if(ex.getMessage()!=null)
			LogManager.getInstance().error(ex);
		}

		return "";
	}

	public static String getCurrentDate(String formatType, ISO8601 enabled, boolean dbVersion) {
		try {
			DateFormat dateFormatter = getDateTimeFormat(formatType);

			if (dbVersion)
				return convertDateTimeFromUIToDB(dateFormatter.format(new Date(System.currentTimeMillis())), formatType);
			else
				return dateFormatter.format(new Date(System.currentTimeMillis()));
		} catch (Exception ex) {
			if(ex.getMessage()!=null)
			LogManager.getInstance().error(ex);
		}

		return "";
	}

	public static String getNextDay(String theUIDay, boolean dbVersion) {
		try {
			Date theDate = convertDateTimeFromUIToDate(theUIDay);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(theDate);

			calendar.add(Calendar.DATE, 1);

			if (dbVersion)
				return sdf.format(calendar.getTime());
			else {
				DateFormat dateFormatter = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT);
				dateFormatter.format(calendar.getTime());
			}
		} catch (Exception ex) {
			if(ex.getMessage()!=null)
				LogManager.getInstance().error(ex);
		}

		return "";
	}


	/**
	 * This function is used to get the date value (Year/Month/Day) string using
	 * device locale format
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return
	 */
	public static String formatDate(int year, int month, int day) {
		GregorianCalendar calendar = new GregorianCalendar(year, month, day);
		Date date = calendar.getTime();
		return getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT).format(date);
	}
	
	/**
	 * This function is used to get the time value string using
	 * device locale format
	 * 
	 * @param hourOfDay
	 * @param minute
	 * @return
	 */
	public static String formatTime(int hourOfDay, int minute) {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		calendar.set(Calendar.MINUTE, minute);
		Date date = calendar.getTime();
		return getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT).format(date);
	}

	/**
	 * Get the DatetimeFormatter using the device locale for different date/time
	 * type
	 * 
	 * @param formatType
	 * @return
	 */
	public static DateFormat getDateTimeFormat(String formatType) {
		Locale currentLocale = Locale.getDefault();
		DateFormat dateFormatter = null;
		String localeName = currentLocale.toString();
		Context ctx = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);
		
		DateFormat dtFormat = android.text.format.DateFormat.getDateFormat(ctx);
//		final String dateFormat = Settings.System.getString(ctx.getContentResolver(), Settings.System.DATE_FORMAT);	
		final String dateFormat = ((SimpleDateFormat)dtFormat).toPattern();
		String timeFormat = getTimeFormatString(ctx, formatType);

		if (localeName.compareToIgnoreCase("en_gb") == 0 || localeName.compareToIgnoreCase("en_nz") == 0 || localeName.compareToIgnoreCase("en_au") == 0) {
			if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("dd/MM/yyyy"+DATE_TIME_SEPARATOR+timeFormat);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);				
			} else if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
				else
					dateFormatter = new SimpleDateFormat(dateFormat);								
			} else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat(timeFormat);
			} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("dd/MM/yyyy"+DATE_TIME_SEPARATOR+timeFormat);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);				
			} else {
				dateFormatter = new SimpleDateFormat(formatType, currentLocale);
			}
		} else if (localeName.compareToIgnoreCase("en_za") == 0) {
			if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("yyyy/MM/dd"+DATE_TIME_SEPARATOR+timeFormat);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);				
			} else if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("yyyy/MM/dd");
				else
					dateFormatter = new SimpleDateFormat(dateFormat);									
			} else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat(timeFormat);
			} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("yyyy/MM/dd"+DATE_TIME_SEPARATOR+timeFormat);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);													
			} else {
				dateFormatter = new SimpleDateFormat(formatType, currentLocale);
			}
		} else {
			if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))					
					dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, currentLocale);
				else 
					dateFormatter  = new SimpleDateFormat(dateFormat);
			}
			else {			
				if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
					if(MetrixStringHelper.isNullOrEmpty(dateFormat))
						dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, currentLocale);
					else {
						DateFormat dFormatter = new SimpleDateFormat(dateFormat);					
						dateFormatter = new SimpleDateFormat(((SimpleDateFormat)dFormatter).toPattern()+DATE_TIME_SEPARATOR+timeFormat);
					}
					
				}  else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
					dateFormatter = new SimpleDateFormat(timeFormat);
				} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
					if(MetrixStringHelper.isNullOrEmpty(dateFormat))
						dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, currentLocale);
					else {
						DateFormat dFormatter = new SimpleDateFormat(dateFormat);
						dateFormatter = new SimpleDateFormat(((SimpleDateFormat)dFormatter).toPattern()+DATE_TIME_SEPARATOR+timeFormat);
					}					
					
				} else {					
					dateFormatter = new SimpleDateFormat(formatType, currentLocale);
				}
				
				SimpleDateFormat simpleFormat = (SimpleDateFormat) dateFormatter;
				String formatString = simpleFormat.toPattern();
				
//				if(!formatString.contains(" a"))				
//					formatString = formatString.replace("H", "h") + " a";
//				else
//					formatString = formatString.replace("H", "h");
				
				simpleFormat.applyLocalizedPattern(formatString);
			}
		}
		return dateFormatter;
	}

	/**
	 * Get the DatetimeFormatter using the device locale for different date/time
	 * type if the dateformat is iso8601 then return DateFormat with iso 8601
	 * 
	 * @param formatType
	 * @return
	 */
	public static DateFormat getDateTimeFormat(String formatType, ISO8601 isoEnabled) {
		Locale currentLocale = Locale.getDefault();
		DateFormat dateFormatter = null;

		if (isoEnabled == ISO8601.Yes) {
			if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			} else if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
			} else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat("HH:mm:ss");
			} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			} else {
				dateFormatter = new SimpleDateFormat(formatType, currentLocale);
			}
		} else {
			return getDateTimeFormat(formatType);
		}

		return dateFormatter;
	}

	/**
	 * Get the datetime string based on the different date/time type
	 * 
	 * @param formatType
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 * @return
	 */
	public static String formatDate(String formatType, int year, int month, int day, int hour, int minute, int second) {
		DateFormat dateFormatter = null;
		Locale currentLocale = Locale.getDefault();
		Calendar cal = GregorianCalendar.getInstance();

		try {
			if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
				dateFormatter = getDateTimeFormat(formatType);
				cal.set(year, month, day, hour, minute, second);
				// Date date = new Date(year, month, day, hour, minute, second);
				Date date = cal.getTime();
				return dateFormatter.format(date);
			} else if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				dateFormatter = getDateTimeFormat(formatType);
				cal.set(year, month, day);
				// Date date = new Date(year, month, day);
				Date date = cal.getTime();
				return dateFormatter.format(date);
			} else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
				cal.set(year, month, day, hour, minute, second);
				// Date date = new Date(year, month, day, hour, minute, second);
				Date date = cal.getTime();
				dateFormatter = getDateTimeFormat(formatType);
				return dateFormatter.format(date);
			} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
				dateFormatter = getDateTimeFormat(formatType);
				cal.set(year, month, day, hour, minute, second);
				// Date date = new Date(year, month, day, hour, minute, second);
				Date date = cal.getTime();
				return dateFormatter.format(date);
			} else {
				cal.set(year, month, day, hour, minute, second);
				// Date date = new Date(year, month, day, hour, minute, second);
				Date date = cal.getTime();
				dateFormatter = new SimpleDateFormat(formatType, currentLocale);
				return dateFormatter.format(date);
			}
		} catch (Exception ex) {
			return "";
		}
	}

	/**
	 * Get the calendar object based on device locale and date string
	 * 
	 * @param formatType
	 * @param value
	 * @return
	 */
	public static Calendar getDate(String formatType, String value) {
		DateFormat dateFormatter = getDateTimeFormat(formatType);
		Calendar calendar = Calendar.getInstance(Locale.getDefault());

		try {
			calendar.setTime(dateFormatter.parse(value));
			return calendar;
		} catch (ParseException e) {
			calendar = Calendar.getInstance();
			LogManager.getInstance().error(e);
			return calendar;
		}
	}

	/**
	 * Get the calendar object based on device locale and date string
	 * 
	 * @param formatType
	 * @param value
	 * @return
	 */
	public static Calendar getDate(String formatType, String value, ISO8601 isoEnabled) {
		DateFormat dateFormatter = getDateTimeFormat(formatType, isoEnabled);
		Calendar calendar = Calendar.getInstance(Locale.getDefault());

		try {
			calendar.setTime(dateFormatter.parse(value));
			return calendar;
		} catch (ParseException e) {
			calendar = Calendar.getInstance();
			LogManager.getInstance().error(e);
			return calendar;
		}
	}

	public static String getRelativeDate(String formatType, int numberOfDays) {
		GregorianCalendar calendar = new GregorianCalendar();
		Locale currentLocale = Locale.getDefault();
		Context ctx = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);
		DateFormat dtFormat = android.text.format.DateFormat.getDateFormat(ctx);
//		final String dateFormat = Settings.System.getString(ctx.getContentResolver(), Settings.System.DATE_FORMAT);	
		final String dateFormat = ((SimpleDateFormat)dtFormat).toPattern();
		String timeFormat = getTimeFormatString(ctx, formatType);		
		DateFormat dateFormatter = null;

		if (currentLocale.toString().contains("en_US")) {
			if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("MM/dd/yyyy"+DATE_TIME_SEPARATOR+timeFormat, currentLocale);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);					
			} else if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("MM/dd/yyyy", currentLocale);
				else
					dateFormatter = new SimpleDateFormat(dateFormat);					
			} else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat(timeFormat, currentLocale);
			} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("MM/dd/yyyy"+DATE_TIME_SEPARATOR+timeFormat, currentLocale);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);									
			} else {
				dateFormatter = new SimpleDateFormat(formatType, currentLocale);
			}
		} else {
			dateFormatter = getDateTimeFormat(formatType);
		}

		calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
		Date theDay = calendar.getTime();
		String formattedDate = "";
		formattedDate = dateFormatter.format(theDay);

		return formattedDate;
	}

	public static String getRelativeDate(String formatType, int numberOfDays, boolean dbVersion, ISO8601 isoEnabled) {
		GregorianCalendar calendar = new GregorianCalendar();
		Locale currentLocale = Locale.getDefault();
		Context ctx = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);
		DateFormat dtFormat = android.text.format.DateFormat.getDateFormat(ctx);
//		final String dateFormat = Settings.System.getString(ctx.getContentResolver(), Settings.System.DATE_FORMAT);	
		final String dateFormat = ((SimpleDateFormat)dtFormat).toPattern();
		String timeFormat = getTimeFormatString(ctx, formatType);		
		
		DateFormat dateFormatter = null;

		if (dbVersion == false && isoEnabled == ISO8601.No) {
			return getRelativeDate(formatType, numberOfDays);
		} else if (dbVersion == false && isoEnabled == ISO8601.Yes) {
			if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", currentLocale);
			} else if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				dateFormatter = new SimpleDateFormat("yyyy-MM-dd", currentLocale);
			} else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat("HH:mm:ss", currentLocale);
			} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
				dateFormatter = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss", currentLocale);
			} else {
				dateFormatter = new SimpleDateFormat(formatType, currentLocale);
			}
		} else if (dbVersion == true && isoEnabled == ISO8601.Yes) {
			return convertDateTimeFromUIToDB(getRelativeDate(formatType, numberOfDays), formatType);
		}

		if (currentLocale.toString().contains("en_US")) {
			if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("MM/dd/yyyy"+DATE_TIME_SEPARATOR+timeFormat, currentLocale);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);					
			} else if (formatType == MetrixDateTimeHelper.DATE_FORMAT) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("MM/dd/yyyy", currentLocale);
				else
					dateFormatter = new SimpleDateFormat(dateFormat);					
			} else if (formatType == MetrixDateTimeHelper.TIME_FORMAT) {
				dateFormatter = new SimpleDateFormat(timeFormat, currentLocale);
			} else if (formatType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS) {
				if(MetrixStringHelper.isNullOrEmpty(dateFormat))
					dateFormatter = new SimpleDateFormat("MM/dd/yyyy"+DATE_TIME_SEPARATOR+timeFormat, currentLocale);
				else
					dateFormatter = new SimpleDateFormat(dateFormat+DATE_TIME_SEPARATOR+timeFormat);									
			} else {
				dateFormatter = new SimpleDateFormat(formatType, currentLocale);
			}
		} else {
			dateFormatter = getDateTimeFormat(formatType);
		}

		calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
		Date theDay = calendar.getTime();
		String formattedDate = "";
		formattedDate = dateFormatter.format(theDay);

		return formattedDate;
	}

	/**
	 * Convert the datetime string from the database format with server locale
	 * to local UI locale. The Local locale will use Date/Time short pattern.
	 * 
	 * @param value
	 * @param context
	 * @return
	 */
	public static String convertDateTimeFromDBToUI(String value) {
		// since we already had another two parameter overload (already shipped
		// in a previous release)
		// and I wanted to keep this as simple as possible for programmers using
		// this method, I pass
		// in a NumberFormat instance knowing it'll be ignored.
		return MetrixDateTimeHelper.convertDateTimeFromDBToUI(value, NumberFormat.getCurrencyInstance());
	}

	/**
	 * Convert the datetime string from the database format with server locale
	 * to local UI locale. The Local locale will use Date/Time short pattern.
	 * 
	 * @param value
	 * @param context
	 * @param formatter
	 * @return
	 */
	public static String convertDateTimeFromDBToUI(String value, Format formatter) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		String localValue = value;
		Date dateValue = null;
		DateFormat serverDateFormat = null;

		String dateTimeType = getDateTimeType(value, "T");
		serverDateFormat = getDateTimeFormat(dateTimeType, ISO8601.Yes);
		
		if(Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
				&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0))
			serverDateFormat.setTimeZone(TimeZone.getTimeZone(User.getUser().serverTimeZoneId));

		try {
			dateValue = serverDateFormat.parse(value);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return "";
		}

		if (formatter == null || (!(formatter instanceof DateFormat))) {
			DateFormat clientDateFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT);
			DateFormat clientTimeFormat = getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT); //new SimpleDateFormat("hh:mm aa", Locale.getDefault());
		
			if(Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
					&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0)) {
				clientDateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
				clientTimeFormat.setTimeZone(Calendar.getInstance().getTimeZone());
			}

			if (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) == 0)
				localValue = clientDateFormat.format(dateValue);
			else if (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) == 0)
				localValue = clientTimeFormat.format(dateValue);
			else
				localValue = clientDateFormat.format(dateValue) + DATE_TIME_SEPARATOR + clientTimeFormat.format(dateValue);
		} else {
			localValue = formatter.format(dateValue);
		}

		return localValue;
	}

	public static String convertDateTimeFromDBToUIDateOnly(String value) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		String localValue = value;
		Date dateValue = null;
		DateFormat serverDateFormat = null;
		String dateTimeType = getDateTimeType(value, "T");
		serverDateFormat = getDateTimeFormat(dateTimeType, ISO8601.Yes);

		try {
			dateValue = serverDateFormat.parse(value);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return "";
		}

		DateFormat clientDateFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT);
		localValue = clientDateFormat.format(dateValue);
		return localValue;
	}
	
	public static String convertDateTimeFromDBToUITimeOnly(String value) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		String localValue = value;
		Date dateValue = null;
		DateFormat serverDateFormat = null;
		String dateTimeType = getDateTimeType(value, "T");
		serverDateFormat = getDateTimeFormat(dateTimeType, ISO8601.Yes);

		try {
			dateValue = serverDateFormat.parse(value);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return "";
		}

		DateFormat clientTimeFormat = getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT);
		localValue = clientTimeFormat.format(dateValue);
		return localValue;
	}
	
	/**
	 * Convert the datetime string from the database format with server locale
	 * to local customized datetime format. This function also automatically handles
	 * TimeZone conversion.  
	 * 
	 * @param value
	 * @param context
	 * @param customizedDateFormat
	 * @return
	 */
	public static String convertDateTimeFromDBToCustomizedUI(String value, String customizedDateFormat) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		String localValue = value;
		Date dateValue = null;
		DateFormat serverDateFormat = null;

		String dateTimeType = getDateTimeType(value, "T");
		serverDateFormat = getDateTimeFormat(dateTimeType, ISO8601.Yes);
		
		if(Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
				&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0))
			serverDateFormat.setTimeZone(TimeZone.getTimeZone(User.getUser().serverTimeZoneId));

		try {
			dateValue = serverDateFormat.parse(value);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return "";
		}

		if (customizedDateFormat == null) {
			DateFormat clientDateFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT);
			DateFormat clientTimeFormat = getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT); //new SimpleDateFormat("hh:mm aa", Locale.getDefault());			
			
			if(Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
					&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0)) {
				clientDateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
				clientTimeFormat.setTimeZone(Calendar.getInstance().getTimeZone());
			}

			if (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) == 0)
				localValue = clientDateFormat.format(dateValue);
			else if (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) == 0)
				localValue = clientTimeFormat.format(dateValue);
			else
				localValue = clientDateFormat.format(dateValue) + " " + clientTimeFormat.format(dateValue);
		} else {
			DateFormat clientFormat = new SimpleDateFormat(customizedDateFormat);
			
			try {
				if(Global.enableTimeZone)
					clientFormat.setTimeZone(Calendar.getInstance().getTimeZone());
				
				localValue = clientFormat.format(dateValue);
			}
			catch(Exception ex){
				localValue = "";
			}
		}

		return localValue;
	}

	/**
	 * Convert the datetime string from the database format with server locale
	 * to local UI locale. The Local locale will use Date/Time short pattern. It
	 * assumes the datetime format in database comply with ISO8601
	 * 
	 * @param value
	 * @param outputDateTimeFormat
	 *            - it will specify the output datetime type
	 * @return
	 */
	public static String convertDateTimeFromDBToUI(String value, String outputDateTimeFormat) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		String localeValue = value;
		Date dateValue = null;
		
		String dateTimeType = getDateTimeType(value, "T");
//		DateFormat sDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, ISO8601.Yes);
		DateFormat sDTFormat = getDateTimeFormat(dateTimeType, ISO8601.Yes);
		
		if(Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
				&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0))
			sDTFormat.setTimeZone(TimeZone.getTimeZone(User.getUser().serverTimeZoneId));

		try {
			dateValue = sDTFormat.parse(value);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return "";
		}
		Locale currentLocale = Locale.getDefault(); // local locale
		DateFormat cDFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT);
		DateFormat cTFormat = getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT);
		
		if(Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
				&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0)) {
			Calendar cal = Calendar.getInstance(currentLocale);
			TimeZone cTZ = cal.getTimeZone();

			cDFormat.setTimeZone(cTZ);
			cTFormat.setTimeZone(cTZ);
		}

		if (outputDateTimeFormat.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) == 0)
			localeValue = cDFormat.format(dateValue);
		else if (outputDateTimeFormat.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) == 0)
			localeValue = cTFormat.format(dateValue);
		else
			localeValue = cDFormat.format(dateValue) + " " + cTFormat.format(dateValue);

		return localeValue;
	}

	public static String convertDateTimeFromUIToDB(String value) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		String dateTimeType = getDateTimeType(value, " ");

		Date inDate = null;
		DateFormat cDTFormat = null;

		if (dateTimeType == MetrixDateTimeHelper.DATE_TIME_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.DATE_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.TIME_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else
			cDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT);

		if(Global.enableTimeZone) {
			Calendar cal = Calendar.getInstance();
			TimeZone cTZ = cal.getTimeZone();	
			cDTFormat.setTimeZone(cTZ);
		}
		
		try {
			inDate = cDTFormat.parse(value);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return "";
		}

		DateFormat sDTFormat = null;

		sDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, ISO8601.Yes);
		if (Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
				&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0))
			sDTFormat.setTimeZone(TimeZone.getTimeZone(User.getUser().serverTimeZoneId));
		
		String st = sDTFormat.format(inDate);
		return st;
	}

	public static String convertDateTimeFromUIToDB(String value, String outputFormatType) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		String dateTimeType = getDateTimeType(value, " ");

		Date inDate = null;
		DateFormat cDTFormat = null;

		if (dateTimeType == MetrixDateTimeHelper.DATE_TIME_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.DATE_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.TIME_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else
			cDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT);

		if(Global.enableTimeZone) {
			Calendar cal = Calendar.getInstance();
			TimeZone cTZ = cal.getTimeZone();

			cDTFormat.setTimeZone(cTZ);
		}

		try {
			inDate = cDTFormat.parse(value);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return "";
		}

		DateFormat sDTFormat = null;
		String st = "";

		if (outputFormatType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) == 0) {
			sDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT, ISO8601.Yes);
		} else if (outputFormatType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) == 0) {
			sDTFormat = getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT, ISO8601.Yes);
		} else {
			sDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, ISO8601.Yes);
			if(Global.enableTimeZone)
				sDTFormat.setTimeZone(TimeZone.getTimeZone(User.getUser().serverTimeZoneId));
		}
		
		st = sDTFormat.format(inDate);
		return st;
	}

	public static String convertDateTimeFromDateToDB(Date dateValue) {
		DateFormat sDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, ISO8601.Yes);
		if (Global.enableTimeZone)
			sDTFormat.setTimeZone(TimeZone.getTimeZone(User.getUser().serverTimeZoneId));
		
		return sDTFormat.format(dateValue);
	}
	
	/**
	 * Convert the datetime string from the database format with server locale
	 * to a Date object.
	 * 
	 * @param value
	 * @return
	 */
	public static Date convertDateTimeFromDBToDate(String value) {
		return convertDateTimeFromDBToDate(value, true);
	}
	
	/**
	 * Convert the datetime string from the database format with server locale
	 * to a Date object.
	 * 
	 * @param value
	 * @return
	 */
	public static Date convertDateTimeFromDBToDate(String value, boolean parseFailIsCritical) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return null;

		Date dateValue = null;
		DateFormat serverDateFormat = null;

		String dateTimeType = getDateTimeType(value, "T");
		serverDateFormat = getDateTimeFormat(dateTimeType, ISO8601.Yes);
		serverDateFormat.setLenient(false);

		if (Global.enableTimeZone && (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT) != 0)
				&& (dateTimeType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT) != 0))
			serverDateFormat.setTimeZone(TimeZone.getTimeZone(User.getUser().serverTimeZoneId));

		try {
			dateValue = serverDateFormat.parse(value);
		} catch (Exception ex) {
			if (parseFailIsCritical)
				LogManager.getInstance().error(ex);
			else
				LogManager.getInstance().info(ex.getMessage());
			return null;
		}
		
		return dateValue;
	}
	
	public static String convertDateTimeFromDateToUI(Date dateValue) {
		if (dateValue == null)
			return "";

		DateFormat cDFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT);		
		DateFormat cTFormat = getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT);		
		
		if(Global.enableTimeZone)
		{
			Calendar cal = Calendar.getInstance();
			TimeZone cTZ = cal.getTimeZone();

			cDFormat.setTimeZone(cTZ);
			cTFormat.setTimeZone(cTZ);
		}

		String localeValue = cDFormat.format(dateValue) + " " + cTFormat.format(dateValue);
		return localeValue;
	}
	
	public static String convertDateTimeFromDateToUIDateOnly(Date dateValue) {
		if (dateValue == null)
			return "";

		DateFormat cDFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_FORMAT);					
		if (Global.enableTimeZone) {
			Calendar cal = Calendar.getInstance();
			TimeZone cTZ = cal.getTimeZone();
			cDFormat.setTimeZone(cTZ);
		}

		String localeValue = cDFormat.format(dateValue);
		return localeValue;
	}
	
	public static String convertDateTimeFromDateToUITimeOnly(Date dateValue) {
		if (dateValue == null)
			return "";
		
		DateFormat cTFormat = getDateTimeFormat(MetrixDateTimeHelper.TIME_FORMAT);			
		if (Global.enableTimeZone) {
			Calendar cal = Calendar.getInstance();
			TimeZone cTZ = cal.getTimeZone();
			cTFormat.setTimeZone(cTZ);
		}

		String localeValue = cTFormat.format(dateValue);
		return localeValue;
	}

	public static Date convertDateTimeFromUIToDate(String value) {
		return convertDateTimeFromUIToDate(value, true);
	}
	
	public static Date convertDateTimeFromUIToDate(String value, boolean parseFailIsCritical) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return null;

		String dateTimeType = getDateTimeType(value, " ");

		Date inDate = null;
		DateFormat cDTFormat = null;

		if (dateTimeType == MetrixDateTimeHelper.DATE_TIME_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.DATE_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else if (dateTimeType == MetrixDateTimeHelper.TIME_FORMAT)
			cDTFormat = getDateTimeFormat(dateTimeType);
		else
			cDTFormat = getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT);

		if(Global.enableTimeZone) {			
			Calendar cal = Calendar.getInstance();
			TimeZone cTZ = cal.getTimeZone();

			cDTFormat.setTimeZone(cTZ);
		}
		
		try {
			inDate = cDTFormat.parse(value);
		} catch (Exception ex) {
			if (parseFailIsCritical)
				LogManager.getInstance().error(ex);
			else
				LogManager.getInstance().info(ex.getMessage());
			return null;
		}

		return inDate;
	}
	
	/**
	 * @param inputDate
	 * @param formatType
	 * @param removedDateTimeParts - Bitwise flags to indicate what parts need to be removed
	 * Parameter removedDateTimeParts Example: MetrixDateTimeHelper.YearPart|MetrixDateTimeHelper.SecondPart
	 * @return the dateTime string which had removed certain parts such as no year or no seconds
	 * 
	 * Example: 
	 * Date today = new Date(System.currentTimeMillis()));
	 * getDateTimeStringByRemoveSpecifedDateTimeParts(today, MetrixDateTimeHelper.DATE_TIME_FORMAT, MetrixDateTimeHelper.YearPart|MetrixDateTimeHelper.SecondPart);
	 * 
	 * The above example will remove both of the yyyy and ss from a date object and return a datetime string.
	 * @since 5.6
	 */
	public static String getDateTimeStringByRemoveSpecifiedDateTimeParts(Date inputDate, String formatType, int removedDateTimeParts) {
		DateFormat dtFormat = getDateTimeFormat(formatType);
		String dtFormatString = ((SimpleDateFormat)dtFormat).toLocalizedPattern();
		String dateSeparator = getDateSeparator();
		String timeSeparator = getTimeSeparator();

		if((removedDateTimeParts & YearPart) == YearPart)
		{
			dtFormatString = getRemovedFormat(dtFormatString, "yyyy", dateSeparator);
		}
			
		if((removedDateTimeParts & MonthPart) == MonthPart)
		{
			dtFormatString = getRemovedFormat(dtFormatString, "MM", dateSeparator);
		}
		
		if((removedDateTimeParts & DayPart) == DayPart)
		{
			dtFormatString = getRemovedFormat(dtFormatString, "dd", dateSeparator);
		}
		
		if((removedDateTimeParts & HourPart) == HourPart)
		{
			dtFormatString = getRemovedFormat(dtFormatString, "hh", timeSeparator);
			dtFormatString = getRemovedFormat(dtFormatString, "HH", timeSeparator);
		}
		
		if((removedDateTimeParts & MinutePart) == MinutePart)
		{
			dtFormatString = getRemovedFormat(dtFormatString, "mm", timeSeparator);
		}
		
		if((removedDateTimeParts & SecondPart) == SecondPart)
		{
			dtFormatString = getRemovedFormat(dtFormatString, "ss", timeSeparator);
		}
				
		SimpleDateFormat newDTFormat = new SimpleDateFormat(dtFormatString, Locale.getDefault());
		String removedValue = newDTFormat.format(inputDate);		
		
		return removedValue;				
	}
	
	private static String getRemovedFormat(String originalDateTimeFormat, String removedPart, String splitter) {
		String generatedFormat="";
		
		if(originalDateTimeFormat.contains(splitter+removedPart)){
			generatedFormat = originalDateTimeFormat.replace(splitter+removedPart, "");
		}
		else if(originalDateTimeFormat.contains(removedPart+splitter)) {
			generatedFormat = originalDateTimeFormat.replace(removedPart+splitter, "");
		}
		else {
			generatedFormat = originalDateTimeFormat.replace(removedPart, "");
		}
		
		return generatedFormat.trim();
	}

//	private static String getLanguage(String locale) {
//		if (MetrixStringHelper.isNullOrEmpty(locale) || locale.contains("-") == false)
//			return "";
//
//		String language = locale.split("-")[0];
//		return language;
//	}
//
//	/**
//	 * Get the country code from locale string
//	 * 
//	 * @param locale
//	 * @return
//	 */
//	private static String getCountry(String locale) {
//		if (MetrixStringHelper.isNullOrEmpty(locale) || locale.contains("-") == false)
//			return "";
//
//		String country = locale.split("-")[1];
//		return country;
//	}

	/**
	 * The function is to determine the DateTime type of the string
	 * 
	 * @param value
	 * @param delimiterBetweenDateAndTime
	 * @return
	 */
	private static String getDateTimeType(String value, String delimiterBetweenDateAndTime) {
		if (MetrixStringHelper.isNullOrEmpty(value))
			return "";

		if (delimiterBetweenDateAndTime.compareTo(" ") == 0) {
			// region Handling UI Value (space delimiter)
			String[] dtPieces = value.split(delimiterBetweenDateAndTime);
			if (dtPieces == null)
				return MetrixDateTimeHelper.DATE_FORMAT;
			else if (dtPieces.length == 1) {
				// It's possible in a 24-hour format to have no space, thus one piece.
				// A time string here will either contain a colon or exactly one period.
				String testString = dtPieces[0];
				if (testString.contains(":") || (testString.contains(".") && testString.indexOf(".") == testString.lastIndexOf(".")))
					return MetrixDateTimeHelper.TIME_FORMAT;
				else
					return MetrixDateTimeHelper.DATE_FORMAT;
			} else if (dtPieces.length >= 3) {
				String delimiter = ":";
				
				if (dtPieces[1].contains("."))
					delimiter = "\\.";
					
				String[] dtp = dtPieces[1].split(delimiter);
				if (dtp != null) {
					if (dtp.length == 3)
						return MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS;
				}

				return MetrixDateTimeHelper.DATE_TIME_FORMAT;
			} else if (dtPieces.length == 2) {
				// A time string here will either contain a colon or exactly one period (followed by am/pm).
				String testString = dtPieces[0];
				if (testString.contains(":") || (testString.contains(".") && testString.indexOf(".") == testString.lastIndexOf(".")))
					return MetrixDateTimeHelper.TIME_FORMAT;
				else {
					String delimiter = ":";
					
					if (dtPieces[1].contains("."))
						delimiter = "\\.";
					
					String[] dtp = dtPieces[1].split(delimiter);
					if (dtp != null) {
						if (dtp.length == 3)
							return MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS;
					}

					return MetrixDateTimeHelper.DATE_TIME_FORMAT;
				}
			}
			// endregion
		} else {
			// region Handling DB Value (T delimiter for ISO-8601)
			String[] dtPieces = value.split(delimiterBetweenDateAndTime);
			if (dtPieces == null)
				return MetrixDateTimeHelper.DATE_FORMAT;
			else if (dtPieces.length == 1)
				return MetrixDateTimeHelper.DATE_FORMAT;
			else if (dtPieces.length > 1) {
				String delimiter = ":";
				
				if(dtPieces[1].contains(".")&&!dtPieces[1].contains(":"))
					delimiter = "\\.";
				
				String[] dtp = dtPieces[1].split(delimiter);
				if (dtp != null) {
					if (dtp.length == 3)
						return MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS;
				}

				return MetrixDateTimeHelper.DATE_TIME_FORMAT;
			}
			// endregion
		}

		return "";
	}

	public static int dateComparisonCount(String dateTimeToBeCompared, ArrayList<String> dateList, String dateTimeFormat, Global.ComparisonOperator comparison) {
		int count = 0;
		Calendar cDate = MetrixDateTimeHelper.getDate(dateTimeFormat, dateTimeToBeCompared);

		for (String sDate : dateList) {
			Calendar dDate = MetrixDateTimeHelper.getDate(dateTimeFormat, sDate);

			if (comparison == Global.ComparisonOperator.Greater) {
				if (dDate.after(cDate))
					count++;
			} else if (comparison == Global.ComparisonOperator.GreaterEqual) {
				if (dDate.after(cDate) || dDate.equals(cDate))
					count++;
			} else if (comparison == Global.ComparisonOperator.Less) {
				if (dDate.before(cDate))
					count++;
			} else if (comparison == Global.ComparisonOperator.LessEqual) {
				if (dDate.before(cDate) || dDate.equals(cDate))
					count++;
			} else if (comparison == Global.ComparisonOperator.Equal) {
				if (dDate.equals(cDate))
					count++;
			}
		}

		return count;
	}
	
	/**
	 * Gets the time separator for the current default locale.
	 * @return
	 * @since 5.6
	 */
	public static String getTimeSeparator() {
		DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());		
		String timeFormat = ((SimpleDateFormat)timeFormatter).toLocalizedPattern();
		String timeSeparator = ":";
		
		if(!timeFormat.contains(":")){
			timeSeparator = ".";
		}
		
		return timeSeparator;
	}
	
	/**
	 * Get the date separator for the current default locale.
	 * @return The date separator.
	 * @since 5.6
	 */
	public static String getDateSeparator(){
		String datePattern = getDateFormatFromSetting(); 
		
		if(MetrixStringHelper.isNullOrEmpty(datePattern)){
			DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());		
			datePattern = ((SimpleDateFormat)dateFormatter).toLocalizedPattern();			
		}
		
		Matcher matcher = Pattern.compile("[^\\w]").matcher(datePattern); 
		return matcher.find() ? matcher.group(0) : "/";
	}
	
	private static String getDateFormatFromSetting() {
		Context ctx = (Context)MetrixPublicCache.instance.getItem(Global.MobileApplication);
		final String dateFormat = Settings.System.getString(ctx.getContentResolver(), Settings.System.DATE_FORMAT);	
		
		return dateFormat;
	}
	
	/**
	 * Get time format based on datetime format type. Format will be decided by 12_24 hours definition too.
	 * The separator between hours and minutes is obtained by locale information.    
	 * @param context The application context.
	 * @param formatType The format type.
	 * @return The resulting format string.
	 * @since 5.6
	 */
	public static String getTimeFormatString(Context context, String formatType){				
		String timeSeparator = getTimeSeparator();
		
		return getTimeFormatString(context, formatType, timeSeparator);
	}
	
	/**
	 * Get time format based on datetime format type and splitter between hours and minutes. Format will be decided by 12_24 hours definition too.  
	 * @param context The application context.
	 * @param formatType The format type.
	 * @param timeSeparator The time separator to apply.
	 * @return String  The resulting format string
	 * @since 5.6
	 */
	public static String getTimeFormatString(Context context, String formatType, String timeSeparator){
		String timeFormat = "";
		boolean time_status = android.text.format.DateFormat.is24HourFormat(MobileApplication.getAppContext());

		if(!time_status){
			if(formatType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_TIME_FORMAT)==0				
			||formatType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT)==0){
				timeFormat = "hh"+timeSeparator+"mm a";
			}
			else if (formatType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS)==0){
				timeFormat = "hh"+timeSeparator+"mm"+timeSeparator+"ss a";						
			}
			else if(formatType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT)==0) {
				timeFormat = "";
			}
			else {
				DateFormat tFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);				
				timeFormat = ((SimpleDateFormat)tFormatter).toLocalizedPattern();				
			}
		}
		else {
			if(formatType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_TIME_FORMAT)==0				
				||formatType.compareToIgnoreCase(MetrixDateTimeHelper.TIME_FORMAT)==0){
				timeFormat = "HH"+timeSeparator+"mm";
			}
			else if (formatType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS)==0){
				timeFormat = "HH"+timeSeparator+"mm"+timeSeparator+"ss";						
			}
			else if(formatType.compareToIgnoreCase(MetrixDateTimeHelper.DATE_FORMAT)==0) {
				timeFormat = "";
			}
			else {
				DateFormat tFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);				
				timeFormat = ((SimpleDateFormat)tFormatter).toLocalizedPattern();
			}
		}
		
		return timeFormat;
	}
	
	/**
	 * This methods returns a Calendar instance with only the date portion of the
	 * value set. The hour, minute, second and millisecond portions will all be
	 * set to 0. 
	 * @param date The date to adjust.
	 * @return A Calendar instance.
	 * @since 5.6
	 */
	public static Calendar getDatePart(Date date){
	    Calendar cal = Calendar.getInstance();       // get calendar instance
	    cal.setTime(date);      
	    cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
	    cal.set(Calendar.MINUTE, 0);                 // set minute in hour
	    cal.set(Calendar.SECOND, 0);                 // set second in minute
	    cal.set(Calendar.MILLISECOND, 0);            // set millisecond in second

	    return cal;                                  // return the date part
	}
	
	/**
	 * This method also assumes endDate >= startDate
	 * @param startDate The starting date.
	 * @param endDate The ending date.
	 * @return The number of days between the starting date and the ending date.
	 * @since 5.6
	**/
	public static long daysBetween(Date startDate, Date endDate) {
	  Calendar sDate = getDatePart(startDate);
	  Calendar eDate = getDatePart(endDate);

	  long daysBetween = 0;
	  while (sDate.before(eDate)) {
	      sDate.add(Calendar.DAY_OF_MONTH, 1);
	      daysBetween++;
	  }
	  return daysBetween;
	}
	
	/**
	 * This method gets the MetrixTimeSpan difference (startDate - endDate).
	 * @param startDate The starting date.
	 * @param endDate The ending date.
	 * @return A MetrixTimeSpan representing the (+/-) milliseconds between the starting date and the ending date.
	 * @since 5.6.3
	 */
	public static MetrixTimeSpan getDateDifference(Date startDate, Date endDate) {
		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		startCal.setTime(startDate);
		endCal.setTime(endDate);
		
		long startMS = startCal.getTimeInMillis();
		long endMS = endCal.getTimeInMillis();
		
		return new MetrixTimeSpan(startMS - endMS);
	}
	
	/**
	 * Adjusts startDate by timeSpan in the direction indicated by isAddition.
	 * @param startDate The starting date.
	 * @param timeSpan The MetrixTimeSpan by which the date should be adjusted.
	 * @param isAddition TRUE if adding, FALSE if subtracting.
	 * @return A Date that has been suitably adjusted.
	 */
	public static Date adjustDate(Date startDate, MetrixTimeSpan timeSpan, boolean isAddition) {
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(startDate);
		
		long adjustMS = timeSpan.mMilliseconds;
		if (!isAddition)
			adjustMS = adjustMS * -1;
		
		startCal.setTimeInMillis(startCal.getTimeInMillis() + adjustMS);
		return startCal.getTime();
	}

	public static boolean use24HourTimeFormat() {
		return android.text.format.DateFormat.is24HourFormat(MobileApplication.getAppContext());
	}
}
