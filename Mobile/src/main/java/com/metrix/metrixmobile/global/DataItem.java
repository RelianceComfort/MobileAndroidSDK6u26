package com.metrix.metrixmobile.global;

public class DataItem {
	public String LineCode;
	public String Description;
	public String CreatedDateTime;
	public String TaskId;
	public String metrixRowId;
	public double Duration;
	public int ItemNumber;
	
	public DataItem(String code, String startTime, double jobHour, String rowId) {
		this.LineCode = code;
		this.CreatedDateTime = startTime;
		this.Duration = jobHour;
		this.metrixRowId = rowId;
	}
	
	public DataItem(String code, String startTime, double jobHour, String rowId, String taskId) {
		this.LineCode = code;
		this.CreatedDateTime = startTime;		
		this.Duration = jobHour;
		this.TaskId = taskId;
		this.metrixRowId = rowId;
	}	
	
	public DataItem(String code, String startTime, double jobHour, String rowId, String taskId, String description) {
		this.LineCode = code;
		this.CreatedDateTime = startTime;		
		this.Duration = jobHour;
		this.TaskId = taskId;
		this.Description = description;
		this.metrixRowId = rowId;
	}	
}
