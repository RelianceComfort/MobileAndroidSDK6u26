package com.metrix.architecture.metadata;

import java.util.ArrayList;
import java.util.Hashtable;

import android.app.Activity;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;

/**
 * Can be used to easily generate a custom update_tableName message to be synced
 * with the back-end application. This isn't normally required as the
 * Metrix.Architecture will automatically generate messages for data bound
 * transactions.
 * 
 * @since 5.5
 */
public class MetrixUpdateMessage implements IMetrixMessage {

	/**
	 * Enumeration providing acceptable values for the transactionType attribute
	 * on MetrixUpdateMessage.
	 */
	public enum MetrixUpdateMessageTransactionType {
		/**
		 * Represents a transaction where data should be inserted into the
		 * database.
		 */
		Insert,
		/**
		 * Represents a transaction where existing database data should be
		 * updated.
		 */
		Update,
		/**
		 * Represents a transaction where existing database data should be
		 * deleted.
		 */
		Delete,
		/**
		 * Represents a transaction where data should be updated if it already
		 * exists or inserted if it does not.
		 */
		InsertUpdate
	};

	/**
	 * The name of the table that the update should be performed against.
	 */
	public String tableName;
	/**
	 * The type of transaction to be performed (Insert, Update, Delete).
	 */
	public MetrixUpdateMessageTransactionType transactionType;
	/**
	 * The current values of the data. In an update this will be the values that
	 * the column's were updated to have.
	 */
	public Hashtable<String, String> values;
	/**
	 * The original values of the data before the transaction. This is only
	 * important for update and delete transactions.
	 */
	public Hashtable<String, String> originalValues;

	/**
	 * A convenience constructor that allows you to pass in the name of the
	 * table and the transaction type.
	 * 
	 * @param tableName
	 *            The name of the table the transaction is performed against.
	 * @param transactionType
	 *            The type of transaction to perform.
	 */
	public MetrixUpdateMessage(String tableName, MetrixUpdateMessageTransactionType transactionType) {
		this.values = new Hashtable<String, String>();
		this.originalValues = new Hashtable<String, String>();
		this.tableName = tableName;
		this.transactionType = transactionType;
	}

	/**
	 * A convenience constructor that allows you to pass in the name of the
	 * table , the transaction type, and a single column and it's value.
	 * 
	 * @param tableName
	 *            The name of the table the transaction is performed against.
	 * @param transactionType
	 *            The type of transaction to perform.
	 * @param columnName
	 *            The column to be updated.
	 * @param columnValue
	 *            The column's new value.
	 */
	public MetrixUpdateMessage(String tableName, MetrixUpdateMessageTransactionType transactionType, String columnName, String columnValue) {
		this.values = new Hashtable<String, String>();
		this.originalValues = new Hashtable<String, String>();
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.values.put(columnName, columnValue);
	}

	/**
	 * A convenience constructor that allows you to pass in the name of the
	 * table , the transaction type, a single column, it's value and it's
	 * original value.
	 * 
	 * @param tableName
	 *            The name of the table the transaction is performed against.
	 * @param transactionType
	 *            The type of transaction to perform.
	 * @param columnName
	 *            The column to be updated.
	 * @param columnValue
	 *            The column's new value.
	 * @param columnOriginalValue
	 *            The column's pre-update value.
	 * 
	 *            <pre>
	 * MetrixUpdateMessage message = new MetrixUpdateMessage(&quot;escalation&quot;, MetrixUpdateMessageTransactionType.Delete, &quot;esc_id&quot;, selectedItem.get(&quot;escalation.esc_id&quot;));
	 * MetrixDatabaseManager.deleteRow(&quot;escalation&quot;, &quot;esc_id=&quot; + selectedItem.get(&quot;escalation.esc_id&quot;));
	 * message.save(DebriefEscalationList.this, DebriefEscalationList.class);
	 * </pre>
	 */
	public MetrixUpdateMessage(String tableName, MetrixUpdateMessageTransactionType transactionType, String columnName, String columnValue, String columnOriginalValue) {
		this.values = new Hashtable<String, String>();
		this.originalValues = new Hashtable<String, String>();
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.values.put(columnName, columnValue);
		this.originalValues.put(columnName, columnOriginalValue);
	}

	/**
	 * A convenience constructor that allows you to pass in the name of the
	 * table , the transaction type, and a hash table containing a list of
	 * columns and their current values.
	 * 
	 * @param tableName
	 *            The name of the table the transaction is performed against.
	 * @param transactionType
	 *            The type of transaction to perform.
	 * @param values
	 *            A hash table containing a list of columns and their values.
	 */
	public MetrixUpdateMessage(String tableName, MetrixUpdateMessageTransactionType transactionType, Hashtable<String, String> values) {
		this.values = values;
		this.originalValues = new Hashtable<String, String>();
		this.tableName = tableName;
		this.transactionType = transactionType;
	}

	/**
	 * A convenience constructor that allows you to pass in the name of the
	 * table , the transaction type, and a hash table containing a list of
	 * columns, their current values and their original values.
	 * 
	 * @param tableName
	 *            The name of the table the transaction is performed against.
	 * @param transactionType
	 *            The type of transaction to perform.
	 * @param values
	 *            A hash table containing a list of columns and their values.
	 * @param originalValues
	 *            A hash table containing a list of columns and their
	 *            pre-transaction values.
	 */
	public MetrixUpdateMessage(String tableName, MetrixUpdateMessageTransactionType transactionType, Hashtable<String, String> values, Hashtable<String, String> originalValues) {
		this.values = values;
		this.originalValues = originalValues;
		this.tableName = tableName;
		this.transactionType = transactionType;
	}

	/**
	 * Generates a JSON document to be synced based on the data found in the
	 * attributes.
	 */
	@Override
	public String format() {
		JsonObject document = new JsonObject();
		JsonObject table = new JsonObject();
		JsonObject nameValue = new JsonObject();

		nameValue.addProperty(this.getMessageTransactionType(), "");
		table.add(this.tableName, nameValue);

		for (String key : this.values.keySet()) {
			String value = this.values.get(key);
			nameValue.addProperty(key, value);
			table.add(this.tableName, nameValue);
		}

		if (this.transactionType == MetrixUpdateMessageTransactionType.Update && this.originalValues.size() > 0) {
			JsonObject original = new JsonObject();

			for (String key : this.originalValues.keySet()) {
				String value = this.originalValues.get(key);
				nameValue.addProperty(key, value);
				original.add("original_" + this.tableName, nameValue);
			}

			nameValue.addProperty("original_" + this.tableName, original.toString());
			table.add(this.tableName, nameValue);
		}

		document.add("update_" + this.tableName, table);
		return document.toString();
	}

	/**
	 * Inserts the JSON document generated by the format method into the sync
	 * tables.
	 */
	@Override
	public Boolean save() {
		ArrayList<DataField> fields = new ArrayList<DataField>();
		fields.add(new DataField("person_id", User.getUser().personId));
		fields.add(new DataField("table_name", this.tableName));
		fields.add(new DataField("transaction_type", this.getMessageTransactionType()));
		fields.add(new DataField("message", this.format()));
		fields.add(new DataField("transaction_id", MetrixDatabaseManager.generateTransactionId("mm_message_out")));
		fields.add(new DataField("status", "READY"));
		fields.add(new DataField("created_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
		fields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
		long result = MetrixDatabaseManager.insertRow("mm_message_out", fields);

		if (result < 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Inserts the JSON document generated by the format method into the sync
	 * tables.
	 * 
	 * <pre>
	 * MetrixUpdateMessage message = new MetrixUpdateMessage(&quot;escalation&quot;, MetrixUpdateMessageTransactionType.Delete, &quot;esc_id&quot;, selectedItem.get(&quot;escalation.esc_id&quot;));
	 * MetrixDatabaseManager.deleteRow(&quot;escalation&quot;, &quot;esc_id=&quot; + selectedItem.get(&quot;escalation.esc_id&quot;));
	 * message.save(DebriefEscalationList.this, DebriefEscalationList.class);
	 * </pre>
	 */
	public void save(Activity activity, Class<?> preconditionActivityClass) {
		if (this.save()) {
			MetrixActivityHelper.startNewActivityAndFinish(activity, preconditionActivityClass);
		} else {
			MetrixUIHelper.showSnackbar(activity, AndroidResourceHelper.getMessage("TransactionFailed"));
		}
	}

	private String getMessageTransactionType() {
		if (this.transactionType == MetrixUpdateMessageTransactionType.Insert) {
			return "insert";
		} else if (this.transactionType == MetrixUpdateMessageTransactionType.Update) {
			return "update";
		} else if (this.transactionType == MetrixUpdateMessageTransactionType.Delete) {
			return "delete";
		} else if (this.transactionType == MetrixUpdateMessageTransactionType.InsertUpdate) {
			return "insert_update";
		}

		return "";
	}

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();
		value.append(AndroidResourceHelper.getMessage("Table1Args", tableName));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("TransactionType1Args", transactionType));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Format1Args", this.format()));

		return value.toString();
	}
}