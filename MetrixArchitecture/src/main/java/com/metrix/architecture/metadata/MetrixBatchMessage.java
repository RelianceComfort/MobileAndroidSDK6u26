package com.metrix.architecture.metadata;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.User;

/**
 * Can be used to batch together other generated messages into a single
 * <code>JSON</code> document that will be synced with the back-end system.
 * 
 * @since 5.5
 */
public class MetrixBatchMessage implements IMetrixMessage {

	/**
	 * An enumeration used by the <code>sequentialType</code> attribute on
	 * <code>MetrixBatchMessage</code>.
	 */
	public enum MetrixBatchSequentialType {
		/**
		 * All messages should be processed as a single database transaction.
		 */
		Dependent,
		/**
		 * Each message should be processed as a discrete database transaction.
		 */
		Independent
	};

	/**
	 * Defines whether or not the messages should be processed in a single
	 * database transaction or multiple transactions.
	 */
	public MetrixBatchSequentialType sequentialType;
	/**
	 * Contains each of the messages to be included in the batch.
	 */
	public ArrayList<IMetrixMessage> messages;

	/**
	 * Instantiates a new instance of <code>MetrixBatchMessage<code>.
	 * 
	 * @param sequentialType
	 *            Defines if the messages should be processed in a single
	 *            database transaction or multiple transactions.
	 */
	public MetrixBatchMessage(MetrixBatchSequentialType sequentialType) {
		this.messages = new ArrayList<IMetrixMessage>();
		this.sequentialType = sequentialType;
	}

	/**
	 * Instantiates a new instance of <code>MetrixBatchMessage<code>.
	 * 
	 * @param sequentialType
	 *            Defines if the messages should be processed in a single
	 *            database transaction or multiple transactions.
	 * @param message
	 *            A generated message to include in the batch.
	 */
	public MetrixBatchMessage(MetrixBatchSequentialType sequentialType, IMetrixMessage message) {
		this.messages = new ArrayList<IMetrixMessage>();
		this.sequentialType = sequentialType;
		this.messages.add(message);
	}

	/**
	 * Instantiates a new instance of <code>MetrixBatchMessage<code>.
	 * 
	 * @param sequentialType
	 *            Defines if the messages should be processed in a single
	 *            database transaction or multiple transactions.
	 * @param messages
	 *            A series of messages to include in the batch.
	 */
	public MetrixBatchMessage(MetrixBatchSequentialType sequentialType, ArrayList<IMetrixMessage> messages) {
		this.sequentialType = sequentialType;
		this.messages = messages;
	}

	/**
	 * Generates a JSON document to be synced based on the data found in the
	 * attributes.
	 */
	@Override
	public String format() {
		JsonObject document = new JsonObject();
		JsonObject sequential = new JsonObject();
		JsonParser parser = new JsonParser();
		JsonObject formattedMessage = new JsonObject();
		JsonArray formattedMessages = new JsonArray();

		for (IMetrixMessage message : messages) {
			formattedMessage = parser.parse(message.format()).getAsJsonObject();
			formattedMessages.add(formattedMessage);
		}

		if (this.sequentialType == MetrixBatchSequentialType.Dependent) {
			sequential.add("sequential_dependent", formattedMessages);
		} else {
			sequential.add("sequential_independent", formattedMessages);
		}

		document.add("perform_batch", sequential);

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

		fields.add(new DataField("transaction_type", "update"));
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

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();
		value.append(AndroidResourceHelper.getMessage("Type1Args", sequentialType));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Format1Args", this.format()));

		return value.toString();
	}
}
