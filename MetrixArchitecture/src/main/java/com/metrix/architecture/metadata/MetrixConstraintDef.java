package com.metrix.architecture.metadata;

import java.lang.reflect.Type;
import java.util.List;

import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * Contains the meta data describing a constraint that should be applied
 * against the data to be bound. This is used primarily for identifying the
 * primary key of a row to be bound for an update but can also be used to
 * filter data that is presented in a ListView (for example, only show
 * rows that have a status of 'OPEN').
 * 
 * @since 5.4
 */
public class MetrixConstraintDef {

	/**
	 * The column to apply the constraint against.
	 */
	public String column;
	
	/**
	 * The operand to use for the value identified for the constraint. For
	 * examples, equals, greater than, less then, etc.
	 */
	public MetrixConstraintOperands operand = MetrixConstraintOperands.EQUALS;
	
	/**
	 * The value to use on the constraint.
	 */
	public String value;
	
	/**
	 * The datatype of the constraint's value.
	 */
	public Type dataType;
	
	/**
	 * If you are using more than one constraint, this contains an 'and' or 'or'
	 * to apply between each of the columns.
	 */
	public String logicalOperator;
	
	/**
	 * Allows you to pass in more than one constraint if you're constraining
	 * on multiple columns.
	 */
	public List<MetrixConstraintDef> group;
	
	public String control;
	public String controlConstraint;

	/**
	 * A convenience constructor.
	 */
	public MetrixConstraintDef() {
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName The name of the column the constraint is against.
	 * @param operand A MetrixConstraintOperand defining equals, not equals, greater than, etc.
	 * @param value The value to be used on the constraint.
	 * @param dataType The datatype of the value.
	 * 
	 * <pre>
	 * nonPartUsageDef.constraints.add(new MetrixConstraintDef("npu_id", MetrixConstraintOperands.EQUALS, 
	 * 	String.valueOf(this.mActivityDef.Keys.get("npu_id")), double.class));
	 * </pre>
	 */
	public MetrixConstraintDef(String columnName, MetrixConstraintOperands operand, String value,
			Type dataType) {
		this.column = columnName;
		this.operand = operand;
		this.value = value;
		this.dataType = dataType;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param control
	 * @param constraint
	 */
	public MetrixConstraintDef(String control, String constraint) {
		this.control = control;
		this.controlConstraint = constraint;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param group If you are constraining data on more than one column, you can
	 * pass in multiple MetrixConstraintDef instances.
	 */
	public MetrixConstraintDef(List<MetrixConstraintDef> group) {
		this.group = group;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName The name of the column the constraint is against.
	 * @param operand A MetrixConstraintOperand defining equals, not equals, greater than, etc.
	 * @param value The value to be used on the constraint.
	 * @param dataType The datatype of the value.
	 * @param logicalOperator 'And' or 'Or' used when more than one column is used in the constraint.
	 * 
	 * <pre>
	 * stockBinDef.constraints.add(new MetrixConstraintDef("place_id", MetrixConstraintOperands.EQUALS, 
	 * 	MetrixCurrentKeysHelper.getKeyValue("stock_bin", "place_id"), String.class));
	 * stockBinDef.constraints.add(new MetrixConstraintDef("location", MetrixConstraintOperands.EQUALS, 
	 * 	MetrixCurrentKeysHelper.getKeyValue("stock_bin", "location"), String.class, " and "));
	 * stockBinDef.constraints.add(new MetrixConstraintDef("part_id", MetrixConstraintOperands.EQUALS, 
	 * 	MetrixCurrentKeysHelper.getKeyValue("stock_bin", "part_id"), String.class, " and "));
	 * stockBinDef.constraints.add(new MetrixConstraintDef("usable", MetrixConstraintOperands.EQUALS, 
	 * 	MetrixCurrentKeysHelper.getKeyValue("stock_bin", "usable"), String.class, " and "));
	 * </pre>
	 */
	public MetrixConstraintDef(String columnName, MetrixConstraintOperands operand, String value,
			Type dataType, String logicalOperator) {
		this.column = columnName;
		this.operand = operand;
		this.value = value;
		this.dataType = dataType;
		this.logicalOperator = logicalOperator;
	}
	
	@Override 
	public String toString() {
        StringBuilder value = new StringBuilder();

        value.append(AndroidResourceHelper.getMessage("Column1Args", this.column));
        value.append(", ");
        value.append(AndroidResourceHelper.getMessage("Operand1Args", this.operand));
        value.append(", ");
        value.append(AndroidResourceHelper.getMessage("Value1Args", this.value));

        if (!MetrixStringHelper.isNullOrEmpty(this.control))
        {
            value.append(", ");
            value.append(AndroidResourceHelper.getMessage("Control1Args", this.control));
            value.append(", ");
            value.append(AndroidResourceHelper.getMessage("ControlConstraint1Args", this.controlConstraint));
        }

        return value.toString();
	}
}
