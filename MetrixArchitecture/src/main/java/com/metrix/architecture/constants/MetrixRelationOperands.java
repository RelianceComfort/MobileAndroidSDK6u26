package com.metrix.architecture.constants;

/**
 * Contains an enumeration of acceptable values for the join attribute
 * on the <code>MetrixRelationDef</code> class. This allows you to specify that
 * the two tables should be joined with an equi join or a left outer join.
 * 
 * @since 5.4
 */
public enum MetrixRelationOperands {

	/**
	 * The join applied to the two tables identified by this MetrixRelationDef should be an equi join.
	 */
	EQUALS, 
	/**
	 * The join applied to the two tables identified by this MetrixRelationDef should be an left outer join.
	 */
	LEFT_OUTER;

}