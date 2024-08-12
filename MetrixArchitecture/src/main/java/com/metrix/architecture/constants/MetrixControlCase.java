package com.metrix.architecture.constants;

/**
 * Contains an enumeration of acceptable values for the forceCase attribute
 * on the <code>MetrixColumnDef</code> class. This allows you to specify that
 * a value entered for this column should automatically be upper or lowercased.
 * 
 * @since 5.4
 */
public enum MetrixControlCase {
	/**
	 * The value should be uppercased by the Architecture before it's saved.
	 */
	UPPER,
	/**
	 * THe value should be lowercased by the Architecture before it's saved.
	 */
	LOWER, 
	/**
	 * No change should be applied to the value before it's saved.
	 */
	NONE
}
