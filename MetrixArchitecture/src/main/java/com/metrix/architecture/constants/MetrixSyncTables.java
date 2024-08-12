package com.metrix.architecture.constants;

/**
 * Contains constants defining the names of the tables used by the
 * synchronization process. 
 * 
 * @since 5.4
 */
public class MetrixSyncTables {

	/**
	 * The name of the table which stores user preferences.
	 */
	public static final String USER_PREFERENCES = "mm_user_preferences";
	/**
	 * The name of the table which containing incoming messages.
	 */
	public static final String MESSAGE_IN = "mm_message_in";
	/**
	 * The name of the table which contains outgoing messages.
	 */
	public static final String MESSAGE_OUT = "mm_message_out";
	/**
	 * The name of the log table associated to the table containing the 
	 * incoming messages.
	 */
	public static final String MESSAGE_IN_LOG = "mm_message_in_log";
	/**
	 * The name of the log table associated to the table containing the 
	 * outgoing messages.
	 */
	public static final String MESSAGE_OUT_LOG = "mm_message_out_log";
	/**
	 * The name of the log table which contains receipts for messages the client has
	 * received from the service that will be used to acknowledge the receipt.
	 */
	public static final String MESSAGE_RECEIPT = "mm_message_receipt";
	/**
	 * The name of the log table associated to the table containing the receipts 
	 * for messages.
	 */
	public static final String MESSAGE_RECEIPT_LOG = "mm_message_receipt_log";
}
