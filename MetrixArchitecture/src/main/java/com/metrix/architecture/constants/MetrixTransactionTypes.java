package com.metrix.architecture.constants;

/**
 * Contains an enumeration of acceptable values for the transactionType attribute
 * on the <code>MetrixTableDef</code> class. This allows you to specify what type
 * of database transaction will be performed by the current layout against the 
 * specified table.
 * 
 * @since 5.4
 */
public enum MetrixTransactionTypes {

	/**
	 * Identifies that a new row will be inserted into the database.
	 */
	INSERT,
	/**
	 * Identifies that an existing row in the database will be selected, bound to
	 * the current view and then later updated by this view.
	 */
	UPDATE,
	/**
	 * Identifies that an existing row in the database will be selected and bound
	 * to the current view.
	 */
	SELECT,
	/**
	 * Identifies that an existing row in the database will be selected, bound to
	 * the current view and then later deleted by this view.
	 */
	DELETE, 
	/**
	 * Identifies that some other custom processing will be done by this view against
	 * the table identified.
	 */
	OTHER,
	/**
	 * Identifies that the row contains a synchronization error and should be 
	 * displayed so that the user can edit and correct it.
	 */
	CORRECT_ERROR, 
	/**
	 * Used internally by the Sync process. No layouts should use this.
	 */
	TRUNC, 
	/**
	 * Used internally by the Sync process. No layouts should use this.
	 */
	INITSTARTED, 
	/**
	 * Used internally by the Sync process. No layouts should use this.
	 */
	INITENDED,
	INITREQUESTED,
	NOTIFY,
	/**
	 * Used internally for password change event.
	 */
	PCHANGE
}
