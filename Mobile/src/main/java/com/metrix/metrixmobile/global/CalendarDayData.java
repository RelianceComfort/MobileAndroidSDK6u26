package com.metrix.metrixmobile.global;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.ui.widget.MobileUIHelper;
import com.metrix.architecture.ui.widget.TimeReportingCalendarAdapter;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;

public class CalendarDayData {
	public double JobHours;
	public double ExpectedJobHours;
	public double ExceptionHours;
	public ArrayList<DataItem> WorkItemList = new ArrayList<DataItem>();
	public String PersonId;
	public String DateValue;
	public String SubTitle = "subTitle";
	public String Title = "Title";
	public String ReportCode = "ReportCode";
	public TimeReportingCalendarAdapter.CalendarDate CalDate;

	public CalendarDayData(String day){
		this.DateValue = day;
		this.PersonId = User.getUser().personId;
	}
		
	public CalendarDayData(String day, String personId){
		this.DateValue = day;
		this.PersonId = personId;
	}
	
	public CalendarDayData(TimeReportingCalendarAdapter.CalendarDate dayDate){
		this.CalDate = dayDate;
		this.PersonId = User.getUser().personId;
		
		int year = dayDate.yearValue;
		int month = dayDate.monthValue;
		int day = dayDate.dateValue;		
		this.DateValue = MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, year, month, day, 0, 0, 0);		
	}
	
	public CalendarDayData(String day, String personId, String subTitle, String title, String reportCode){
		this.DateValue = day;
		this.PersonId = personId;
		this.SubTitle = subTitle;
		this.Title = title;
		this.ReportCode = reportCode;
	}
	
	public void createCalendarItem() {
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("select npu_id, quantity, task_id, non_part_usage.line_code, line_code.description, non_part_usage.created_dttm, ");
		queryBuilder.append(" non_part_usage.metrix_row_id, non_part_usage.work_dt from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code ");
		queryBuilder.append(" left outer join person on non_part_usage.person_id = person.person_id where line_code.npu_code = 'TIME' and non_part_usage.person_id = '" + this.PersonId + "' ");
		/*
		queryBuilder.append("select npu_id, quantity, task_id, non_part_usage.line_code, line_code.description, non_part_usage.created_dttm, non_part_usage.metrix_row_id from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code "); // where line_code.npu_code = 'TIME' and non_part_usage.person_id = " +this.PersonId);		
		queryBuilder.append(" left outer join person on non_part_usage.created_by = person.person_id ");
		queryBuilder.append(" where line_code.npu_code = 'TIME' ");*/
		
		String calendarDayStart = "";
		String calendarDayEnd = "";
		
		if(!MetrixStringHelper.isNullOrEmpty(this.DateValue)){
			Calendar theDay = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, this.DateValue);
			int year = theDay.get(Calendar.YEAR);
			int month = theDay.get(Calendar.MONTH); // Note: zero based!
			int day = theDay.get(Calendar.DAY_OF_MONTH);
			String dayTimeStart = MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, year, month, day, 0, 0, 0);

			theDay.add(Calendar.DATE, 1);
			int nextDayYear = theDay.get(Calendar.YEAR);
			int nextDayMonth = theDay.get(Calendar.MONTH); // Note: zero based!
			int nextDayDay = theDay.get(Calendar.DAY_OF_MONTH);
			String dayTimeEnd =  MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, nextDayYear, nextDayMonth, nextDayDay, 0, 0, 0);

			//String dayStart = MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_FORMAT, year, month, day, 0, 0, 0);
			String dayStart = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayTimeStart, MetrixDateTimeHelper.DATE_FORMAT);
			String dayEnd = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayTimeEnd, MetrixDateTimeHelper.DATE_FORMAT);
			calendarDayStart = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayTimeStart, MetrixDateTimeHelper.DATE_TIME_FORMAT);;
			calendarDayEnd = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayTimeEnd, MetrixDateTimeHelper.DATE_TIME_FORMAT);;
		
			String dayFilter = " and non_part_usage.work_dt >= '" + dayStart
				+ "' and non_part_usage.work_dt < '" + dayEnd+ "'";
			queryBuilder.append(dayFilter);
		}	
		queryBuilder.append(" order by npu_id");
		
		String query = queryBuilder.toString(); // "select quantity from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code where line_code.npu_code = 'TIME' and non_part_usage.person_id = "+this.PersonId;
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(1), Locale.US));
					String taskId = cursor.getString(2);
					String lineCode = cursor.getString(3);
					String description = cursor.getString(4);
					String joda = MobileUIHelper.jodaPeriod(cursor.getString(5));
					String metrixRowId = cursor.getString(6);
										
					DataItem laborItem = new DataItem(lineCode, joda, quantity, metrixRowId, taskId, description);
					this.WorkItemList.add(laborItem); 
					
					cursor.moveToNext();
				}								
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		createCalendarExceptionList(calendarDayStart, calendarDayEnd);
//		ArrayList<Hashtable<String, String>> lineCodeList = MetrixDatabaseManager.getFieldStringValuesList("select distinct line_code.line_code, line_code.description from line_code where line_code.npu_code='TIME'");
//		
//		if(lineCodeList != null) {
//			for(Hashtable<String, String> row : lineCodeList) {
//				String lineCode = row.get("line_code");
//				String description = row.get("description");
//				int itemNumber = this.calculateJobHours(lineCode);
//				DataItem laborItem = new DataItem(lineCode, DateValue, this.JobHours, itemNumber, description);
//				if(this.JobHours!=0)
//					this.WorkItemList.add(laborItem); 					
//			}
//		}		
	}
	
	private void createCalendarExceptionList(String theDayStart, String theDayEnd) {
		String query = String.format("select distinct exception_id, exception_type.description, person_cal_except.start_dttm, person_cal_except.end_dttm, person_cal_except.created_dttm, person_cal_except.metrix_row_id"+
				" from person_cal_except left join exception_type on person_cal_except.exception_type = exception_type.exception_type"+
				" where person_cal_except.person_id = '%1$s' and ((person_cal_except.start_dttm >='%2$s' and person_cal_except.start_dttm <= '%3$s') or (person_cal_except.start_dttm <='%2$s' and person_cal_except.end_dttm >= '%3$s') or (person_cal_except.start_dttm <='%2$s' and person_cal_except.end_dttm < '%3$s' and person_cal_except.end_dttm > '%2$s')) order by person_cal_except.start_dttm", User.getUser().personId, theDayStart, theDayEnd);
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);

		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}
		
		try {
			Date startDate = MetrixDateTimeHelper.convertDateTimeFromDBToDate(theDayStart, false);
			Date endDate = MetrixDateTimeHelper.convertDateTimeFromDBToDate(theDayEnd, false);
			
			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {					
					//String taskId = cursor.getString(0);
					Date startDttm = MetrixDateTimeHelper.convertDateTimeFromDBToDate(cursor.getString(2), false);					
					Date endDttm = MetrixDateTimeHelper.convertDateTimeFromDBToDate(cursor.getString(3), false);
					
					if(startDttm.getTime() < startDate.getTime())
						startDttm = startDate;
					if(endDttm.getTime()> endDate.getTime())
						endDttm = endDate;
					
					long diff = endDttm.getTime() - startDttm.getTime();
					float hourDiff = (float)diff/1000/60/60;

					if(diff == 0) {
						cursor.moveToNext();
						continue;
					}

					String lineCode = cursor.getString(1);
					String description = cursor.getString(1);
					String joda = MobileUIHelper.jodaPeriod(cursor.getString(4));
					String metrixRowId = cursor.getString(5);
										
					// use Customized string on task_id field with "CalendarException" to indicate it is Calendar Exception items. 
					DataItem laborItem = new DataItem(lineCode, joda, hourDiff, metrixRowId, "CalendarException", description);
					this.WorkItemList.add(laborItem); 
					
					cursor.moveToNext();
				}								
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	public double getExpectedWorkHour(){
		String dayStart = MetrixDateTimeHelper.convertDateTimeFromUIToDB(this.DateValue, MetrixDateTimeHelper.DATE_FORMAT);	
		String workHour = MetrixDatabaseManager.getFieldStringValue("select max_work_hours from work_cal_time_person_view where work_dt='"+dayStart+"'");
		
		if(!MetrixStringHelper.isNullOrEmpty(workHour)) {
			Number workHourNumber = MetrixFloatHelper.convertNumericFromDBToNumber(workHour);
			this.ExpectedJobHours = workHourNumber.doubleValue();
		}
		else {
			this.ExpectedJobHours = 0;
		}
		return this.ExpectedJobHours;
	}
	
	public boolean hasExceptionHour(){
		boolean hasException = false;
		Calendar theDay = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, this.DateValue);
		int year = theDay.get(Calendar.YEAR);
		int month = theDay.get(Calendar.MONTH); // Note: zero based!
		int day = theDay.get(Calendar.DAY_OF_MONTH);		
		
		String dayStart = MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, year, month, day, 0, 0, 1);

		theDay.add(Calendar.DATE, 1);
		int nextDayYear = theDay.get(Calendar.YEAR);
		int nextDayMonth = theDay.get(Calendar.MONTH); // Note: zero based!
		int nextDayDay = theDay.get(Calendar.DAY_OF_MONTH);
		String dayTimeEnd =  MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, nextDayYear, nextDayMonth, nextDayDay, 0, 0, 0);

		dayStart = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayStart, MetrixDateTimeHelper.DATE_TIME_FORMAT);
		String dayEnd = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayTimeEnd, MetrixDateTimeHelper.DATE_TIME_FORMAT);
		
		String workHour = MetrixDatabaseManager.getFieldStringValue("select count(*) from person_cal_except where person_cal_except.start_dttm <= '" + dayEnd
				+ "' and person_cal_except.end_dttm > '" + dayStart+ "'");
		
		if(!MetrixStringHelper.isNullOrEmpty(workHour)&&!workHour.equals("0")) {
			hasException = true; 
		}
		
		return hasException;
	}
	
	public int calculateJobHours(String lineCode){
		double total = 0.0;
		int itemNumber = 0;
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("select quantity from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code "); // where line_code.npu_code = 'TIME' and non_part_usage.person_id = " +this.PersonId);
		queryBuilder.append(" left outer join person on non_part_usage.created_by = person.person_id ");
		queryBuilder.append(" where line_code.npu_code = 'TIME' ");
		
		if(!MetrixStringHelper.isNullOrEmpty(lineCode)) {
			queryBuilder.append(" and non_part_usage.line_code = '"+lineCode+"'");
		}
		
		if(!MetrixStringHelper.isNullOrEmpty(this.DateValue)){
			Calendar theDay = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, this.DateValue);
			int year = theDay.get(Calendar.YEAR);
			int month = theDay.get(Calendar.MONTH); // Note: zero based!
			int day = theDay.get(Calendar.DAY_OF_MONTH);		
			
			String dayStart = MetrixDateTimeHelper.formatDate(MetrixDateTimeHelper.DATE_FORMAT, year, month, day, 0, 0, 0);
			dayStart = MetrixDateTimeHelper.convertDateTimeFromUIToDB(dayStart, MetrixDateTimeHelper.DATE_FORMAT);
			String dayEnd = dayStart+"T23:59:59";
			String dayFilter = " and non_part_usage.work_dt >= '" + dayStart
				+ "' and non_part_usage.work_dt <= '" + dayEnd+ "'";
			queryBuilder.append(dayFilter);
		}	
		
		String query = queryBuilder.toString(); // "select quantity from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code where line_code.npu_code = 'TIME' and non_part_usage.person_id = "+this.PersonId;
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				int i = 1;
				while (cursor.isAfterLast() == false) {
					double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
					total = total + quantity;

					cursor.moveToNext();
					i = i + 1;
				}
				
				itemNumber = i-1;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		this.JobHours = total;
		return itemNumber;
	}
}
