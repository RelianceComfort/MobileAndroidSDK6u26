package com.metrix.architecture.metadata;

/**
 * 
 * Contains meta data that describes how the data displayed in a lookup should
 * be filtered. For example, if I only wanted to show a list of high priority
 * customers, I could create a MetrixLookupFilterDef on the priority column.
 * 
 * @since 5.4
 */
public class MetrixLookupFilterDef {

	/**
	 * Identifies the number of open parenthesis characters to put in front of this filter.
	 */
	public int leftParens = 0;

	/**
	 * Identifies the column to be filtered on.
	 */
	public String leftOperand;

	/**
	 * Identifies the type of operator that should be applied (greater than,
	 * less than, equals, not equals, etc.)
	 */
	public String operator;

	/**
	 * Identifies the value to use on the filter.
	 */
	public String rightOperand;

	/**
	 * Identifies whether the right operand should not be surrounded with single quotes.
	 */
	public boolean noQuotes = false;

	/**
	 * Identifies the script to run to obtain the value to use on the filter.
	 */
	public String scriptForRightOperand;

	/**
	 * Identifies the number of close parenthesis characters to put after this filter.
	 */
	public int rightParens = 0;

	/**
	 * If more than one MetrixLookupFilterDef will be used, identifies the if an
	 * 'and' or 'or' should be used.
	 */
	public String logicalOperator;

	/**
	 * A convenience constructor.
	 */
	public MetrixLookupFilterDef() {
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param leftOperand
	 *            The column to filter on.
	 * @param filterOperator
	 *            The operator to use. Typically 'null' or 'not null'.
	 */
	public MetrixLookupFilterDef(String leftOperand, String filterOperator) {
		this.leftOperand = leftOperand;
		this.operator = filterOperator;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param leftOperand
	 *            The column to filter on.
	 * @param filterOperator
	 *            The operator to use.
	 * @param rightOperand
	 *            The value to use on the filter.
	 * 
	 * <pre>
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.part_id"));
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.serial_id",
	 * 	R.id.truck_part_selected_serial));
	 * lookupDef.title = MetrixStringHelper.formatLookupTitle(this, R.string.LookupHelpA, R.string.SerialId);
	 * lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.part_id", "=", partId));
	 * lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.usable", "=", "Y"));
	 * </pre>
	 */
	public MetrixLookupFilterDef(String leftOperand, String filterOperator, String rightOperand) {
		this.leftOperand = leftOperand;
		this.operator = filterOperator;
		this.rightOperand = rightOperand;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param leftOperand
	 *            The column to filter on.
	 * @param filterOperator
	 *            The operator to use.
	 * @param rightOperand
	 *            The value to use on the filter.
	 * @param logicalOperator
	 *            If more than one MetrixLookupFilterDef will be used,
	 *            identifies the if an 'and' or 'or' should be used.
	 */
	public MetrixLookupFilterDef(String leftOperand, String filterOperator, String rightOperand, String logicalOperator) {
		this.leftOperand = leftOperand;
		this.operator = filterOperator;
		this.rightOperand = rightOperand;
		this.logicalOperator = logicalOperator;
	}
}
