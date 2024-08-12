package com.metrix.architecture.metadata;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simplifies passing a date parameter between activities and fragments. The
 * date in this case is a compound value of year, month and date without time or
 * time zone information.
 * 
 * Pass this value in a bundle or as an intent extra like follows:
 * 
 * <pre>
 * <code>
 * DateParam dateParam = new DateParam();
 * Intent i = new Intent();
 * i.putExtra(DateParam.DATE_PARAM, dateParam);
 * </code>
 * </pre>
 */
public class DateParam implements Parcelable {
	public static final String DATE_PARAM = "EXTRA_DATE_PARAM";

	private int year;
	private int month;
	private int date;

	/**
	 * @return the year.
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @param year
	 *            the year to set.
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * @return the month as a zero based index. (January = 0)
	 */
	public int getMonth() {
		return month;
	}

	/**
	 * @param month
	 *            the month to set as a zero based index. (January = 0)
	 */
	public void setMonth(int month) {
		this.month = month;
	}

	/**
	 * @return the day of month.
	 */
	public int getDate() {
		return date;
	}

	/**
	 * @param date
	 *            the day of month to set.
	 */
	public void setDate(int date) {
		this.date = date;
	}
	
	/**
	 * Create a new DateParam initialized with the values specified.
	 * 
	 * @param year
	 *            the year to set.
	 * @param month
	 *            the month to set as a zero based index. (January = 0)
	 * @param date
	 *            the day of month to set.
	 */
	public DateParam(int year, int month, int date) {
		this.year = year;
		this.month = month;
		this.date = date;
	}

	/**
	 * Create a new DateParam initialized with the supplied java date.
	 * 
	 * @param date
	 *            the date to set.
	 */
	public DateParam(Date date) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTime(date);
		this.year = cal.get(Calendar.YEAR);
		this.month = cal.get(Calendar.MONTH);
		this.date = cal.get(Calendar.DATE);
	}

	/**
	 * Create a new DateParam initialized with the current system date
	 */
	public DateParam() {
		Calendar cal = Calendar.getInstance();
		this.year = cal.get(Calendar.YEAR);
		this.month = cal.get(Calendar.MONTH);
		this.date = cal.get(Calendar.DATE);
	}

	public Date toJavaDate() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis(0L);
		cal.set(this.year, this.month, this.date);
		return cal.getTime();
	}
	
	private DateParam(Parcel source) {
		year = source.readInt();
		month = source.readInt();
		date = source.readInt();
	}

	/**
	 * @return the date in ISO string format. (2001-01-01)
	 */
	@SuppressLint("DefaultLocale")
	public String getIsoDateString() {
		return String.format("%1$04d-%2$02d-%3$02d", year, month + 1, date);
	}
	
	/**
	 * @return the date in ISO string format. (2001-01-01)
	 */
	@SuppressLint("DefaultLocale")
	public String getIsoDateTimeString() {
		return String.format("%1$04d-%2$02d-%3$02dT00:00:00.000", year, month + 1, date);
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(year);
		dest.writeInt(month);
		dest.writeInt(date);
	}

	/**
	 * <strong>DO NOT USE</strong> this constant in your code. It is provided as
	 * a requirement of the {@link Parcelable} interface to allow inflating the
	 * parcel to an object.
	 */
	public static final Parcelable.Creator<DateParam> CREATOR = new Parcelable.Creator<DateParam>() {

		@Override
		public DateParam createFromParcel(Parcel source) {
			return new DateParam(source);
		}

		@Override
		public DateParam[] newArray(int size) {
			return new DateParam[size];
		}
	};
}
