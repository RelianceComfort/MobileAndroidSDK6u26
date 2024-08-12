package com.metrix.architecture.utilities;


/**
 * The DataField class is used on an overload of the MetrixUpdateManager's
 * update methods to allow you to perform a DB transaction with a corresponding
 * synchronization to the back-end with non-databound data.
 * 
 * @since 5.4
 * 
 * <pre>
 * MetrixSqlData timeClockEvent = new MetrixSqlData("time_clock_event", MetrixTransactionTypes.INSERT);
 * timeClockEvent.dataFields.add(new DataField("event_sequence", MetrixDatabaseManager.generatePrimaryKey("time_clock_event")));
 * timeClockEvent.dataFields.add(new DataField("clock_id", clockId));
 * timeClockEvent.dataFields.add(new DataField("event_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS)));
 * timeClockEvent.dataFields.add(new DataField("foreign_key_num1", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
 * timeClockEvent.dataFields.add(new DataField("person_id", User.getUser().personId));
 * eventsToInsert.add(timeClockEvent);
 *
 * MetrixTransaction transactionInfo = new MetrixTransaction();
 * if (MetrixUpdateManager.update(eventsToInsert, true, transactionInfo, "Time Clock Event", activity)) {
 *	...
 * </pre>
 */
public class DataField {
	public String name;
	public String value;
	public String type;
	public String tableName;
	
	public DataField(){
	}

	public DataField(String name, String value){
		this.name = name;
		this.value = value;
	}
	
	public DataField(String name, int value){
		this.name = name;
		this.value = Integer.toString(value);
	}
	
	public DataField(String name, double value){
		this.name = name;
		this.value = Double.toString(value);
	}

	public DataField(String name, String value, String type, String tableName){
		this.name = name;
		this.value = value;
		this.type = type;
		this.tableName = tableName;
	}
}
