package com.metrix.architecture.constants;

/**
 * Contains an enumeration of acceptable values that can be returned by the 
 * <code>MetrixUpdateManager.update</code> method.
 * 
 * @since 5.4
 */
public enum MetrixSaveResult {

	/**
	 * The save was successfully completed.
	 */
	SUCCESSFUL, 
	/**
	 * The save failed because an error was encountered.
	 */
	ERROR,
	/**
	 * The save failed because of an error but this should not prevent 
	 * subsequent processing from continuing.
	 */
	ERROR_WITH_CONTINUE
}