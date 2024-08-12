package com.metrix.architecture.constants;

/**
 * Contains an enumeration of acceptable values for the operand attribute
 * on the <code>MetrixConstraintDef</code> class.
 * 
 * @since 5.4
 */
public enum MetrixConstraintOperands {
	/**
	 * The value of the column specified must equal the value provided.
	 */
	EQUALS,
	/** 
	 * The value of the column specified must be different from the value provided.
	 */
	NOT_EQUALS, 
	/**
	 * The value of the column specified must be less than the value provided. Used for numeric values. 
	 */
	LESS_THAN,
	/**
	 * The value of the column specified must be greater than the value provided. Used for numeric values.
	 */
	GREATER_THAN, 
	/**
	 * The value of the column specified must contain the value provided.
	 */
	LIKE
}
