package com.metrix.architecture.metadata;

import java.util.ArrayList;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.utilities.DataField;

/** 
 * Contains meta data which defines a manual, non layout bound, 
 * database transaction which can be used in overloads of the 
 * MetrixUpdateManager update and performTransactions methods.
 * 
 * @since 5.4
 */
public class MetrixSqlData {
	/**
	 * The name of the table the transaction is against.
	 */
	public String tableName = "";
	
	/**
	 * A list of DataField instances which define the column names and
	 * values for the updated data.
	 */
	public ArrayList<DataField> dataFields = new ArrayList<DataField>();
	
	/**
	 * The type of database transaction to be performed.
	 */
	public MetrixTransactionTypes transactionType;
	
	/**
	 * The filter (optional) to be applied. Used for updates and deletes.
	 */
	public String filter = null;
	
	/**
	 * The unique metrix row id of the row to be updated or deleted
	 */
	public double rowId;

	/**
	 * A convenience constructor.
	 */
	public MetrixSqlData() {

	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName The name of the table in the transaction.
	 * @param transactionType The type of transaction to perform.
	 */
	public MetrixSqlData(String tableName, MetrixTransactionTypes transactionType) {
		this.tableName = tableName;
		this.transactionType = transactionType;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName The name of the table in the transaction.
	 * @param transactionType The type of transaction to perform.
	 * @param filter The filter to be applied for updates or deletes.
	 */
	public MetrixSqlData(String tableName, MetrixTransactionTypes transactionType, String filter) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.filter = filter;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName The name of the table in the transaction.
	 * @param dataFields A collection of dataFields describing the columns and their values.
	 * @param transactionType The type of transaction to perform.
	 * @param filter The filter to be applied for updates or deletes.
	 */
	public MetrixSqlData(String tableName, ArrayList<DataField> dataFields, MetrixTransactionTypes transactionType, String filter) {
		this.tableName = tableName;
		this.dataFields = dataFields;
		this.transactionType = transactionType;
		this.filter = filter;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName The name of the table in the transaction.
	 * @param dataFields A collection of dataFields describing the columns and their values.
	 * @param transactionType The type of transaction to perform.
	 * @param filter The filter to be applied for updates or deletes.
	 * @param rowId The unique metrix row id associated to the row to be updated.
	 * 
	 * <pre>
	 * ArrayList<MetrixSqlData> partNeedsToUpdate = new ArrayList<MetrixSqlData>();
	 * String rowId = MetrixDatabaseManager.getFieldStringValue(
	 *			"part_need", "metrix_row_id", "request_id='"
	 *					+ requestId + "' and sequence=" + partNeedSequence);
 	 *
	 * MetrixSqlData data = new MetrixSqlData("part_need",
	 *			MetrixTransactionTypes.UPDATE, "metrix_row_id=" + rowId);
	 * data.dataFields.add(new DataField("metrix_row_id", rowId));
	 * data.dataFields.add(new DataField("request_id", requestId));
	 * data.dataFields.add(new DataField("sequence", partNeedSequence));
	 * data.dataFields.add(new DataField("parts_used", "Y"));
	 * partNeedsToUpdate.add(data);
 	 *
	 * MetrixTransaction transactionInfo = new MetrixTransaction();
	 * boolean successful = MetrixUpdateManager.update(partNeedsToUpdate,
	 *			true, transactionInfo, "Part Needs", this);
	 * </pre>
	 */
	public MetrixSqlData(String tableName, ArrayList<DataField> dataFields, MetrixTransactionTypes transactionType, String filter, double rowId) {
		this.tableName = tableName;
		this.dataFields = dataFields;
		this.transactionType = transactionType;
		this.filter = filter;
		this.rowId = rowId;
	}
}
