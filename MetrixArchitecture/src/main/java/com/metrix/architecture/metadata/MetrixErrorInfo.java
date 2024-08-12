package com.metrix.architecture.metadata;

import com.metrix.architecture.utilities.AndroidResourceHelper;

import java.util.Hashtable;

/**
 * Constaints meta data about an error that was returned for a transaction which
 * will be used as part of client-side error handling. It's not likely that
 * you'll need to do anything with this class.
 * 
 * @since 5.4
 */
public class MetrixErrorInfo {

	/**
	 * The name of the table for the transaction with an error.
	 */
	public String tableName;

	/**
	 * A friendly description of the transaction.
	 */
	public String transactionDescription;

	/**
	 * The primary key of the mm_message_in row containing the error message.
	 */
	public String messageId;

	/**
	 * The error message describing the error that was found processing this
	 * transaction.
	 */
	public String errorMessage;

	/**
	 * A hashtable containing the names and values of the primary key of the row
	 * in error.
	 */
	public Hashtable<String, String> primaryKeys;

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table for the transaction with an error.
	 * @param transactionDescription
	 *            A friendly description of the transaction.
	 * @param messageId
	 *            The primary key of the mm_message_in row containing the error
	 *            message.
	 * @param errorMessage
	 *            The error message describing the error that was found
	 *            processing this transaction.
	 * @param primaryKeys
	 *            A hashtable containing the names and values of the primary key
	 *            of the row in error.
	 */
	public MetrixErrorInfo(String tableName, String transactionDescription, String messageId, String errorMessage, Hashtable<String, String> primaryKeys) {
		this.tableName = tableName;
		this.transactionDescription = transactionDescription;
		this.messageId = messageId;
		this.errorMessage = errorMessage;
		this.primaryKeys = primaryKeys;
	}

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();

		value.append(AndroidResourceHelper.getMessage("MessageId1Args", this.messageId));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Description1Args", this.transactionDescription));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Error1Args", this.errorMessage));

		return value.toString();
	}
}
