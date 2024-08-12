package com.metrix.architecture.managers;

import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixOrderByDef;
import com.metrix.architecture.metadata.MetrixRelationDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.view.ViewGroup;

/**
 * This class contains methods which manage the construction of
 * database queries.
 * 
 * @since 5.4
 */
class MetrixQueryManager {

	/**
	 * Builds a sql query based on the meta data and layout received.
	 * 
	 * @param layout
	 *            the layout for which this query is being built.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @return the generated sql query.
	 */
	public static String buildQuery(ViewGroup layout, MetrixFormDef metrixFormDef) {
		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}
		
		StringBuilder query = new StringBuilder();
		query.append("select ");

		boolean addComma = false;
		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase("custom") == 0) {
				continue;
			}
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (addComma) {
					query.append(", ");
				}
				query.append(tableDef.tableName);
				query.append(".");
				query.append(columnDef.columnName);
				query.append(" ");
				query.append(tableDef.tableName);
				query.append("__");
				query.append(columnDef.columnName);
				addComma = true;
			}
		}

		query.append(" from ");
		addComma = false;
		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase("custom") == 0) {
				continue;
			}
			if (tableDef.hasRelations()) {
				if (tableDef.relations.get(0).join == MetrixRelationOperands.EQUALS) {
					if (addComma) {
						query.append(", ");
					}
					query.append(tableDef.tableName);
				} else {
					query.append(" left join ");
					query.append(tableDef.tableName);
					query.append(" on ");
					int index = 0;
					for (MetrixRelationDef relationDef : tableDef.relations) {
						if (index > 0) {
							query.append(" and ");
						}
						query.append(relationDef.parentTable);
						query.append(".");
						query.append(relationDef.parentColumn);
						query.append("=");
						query.append(tableDef.tableName);
						query.append(".");
						query.append(relationDef.childColumn);
						index = index + 1;
					}
				}
			} else {
				if (addComma) {
					query.append(", ");
				}
				query.append(tableDef.tableName);
			}
			addComma = true;
		}

		if (metrixFormDef.containsConstraints()
				|| metrixFormDef.containsRelations()) {
			query.append(" where ");

			boolean clauseAdded = false;

			if (metrixFormDef.containsConstraints()) {
				for (MetrixTableDef tableDef : metrixFormDef.tables) {
					if (tableDef.tableName.compareToIgnoreCase("custom") == 0) {
						continue;
					}
					for (MetrixConstraintDef constraintDef : tableDef.constraints) {
						if (MetrixStringHelper
								.isNullOrEmpty(constraintDef.control)) {
							if (!MetrixStringHelper
									.isNullOrEmpty(constraintDef.logicalOperator)) {
								query.append(" ");
								query.append(constraintDef.logicalOperator);
								query.append(" ");
							}
							else {
								if (clauseAdded) {
									query.append(" and "); // default in case they pass in multiple constraints but no logical operators
								}
							}

							if (constraintDef.group != null) {
								if (constraintDef.group.size() > 0) {
									// TODO: finish me
								}
							} else {
								query.append(tableDef.tableName);
								query.append(".");
								query.append(constraintDef.column);

								if (constraintDef.dataType == String.class) {
									addStringConstraint(query, constraintDef);
								} else if (constraintDef.dataType == int.class) {
									addIntegerConstraint(query, constraintDef);
								} else if (constraintDef.dataType == double.class) {
									addDoubleConstraint(query, constraintDef);
								}
								// TODO: add support for other data types (data
								// time)
								clauseAdded = true;
							}
						}
					}
				}
			}

			if (metrixFormDef.containsRelations()) {
				for (MetrixTableDef tableDef : metrixFormDef.tables) {
					if (tableDef.tableName.compareToIgnoreCase("custom") == 0) {
						continue;
					}
					for (MetrixRelationDef relationDef : tableDef.relations) {
						if (relationDef.join == MetrixRelationOperands.EQUALS) {
							if (clauseAdded) {
								query.append(" and ");
							}
							query.append(relationDef.parentColumn);
							query.append("=");
							query.append(relationDef.childColumn);
						}

						clauseAdded = true;
					}
				}
			}
		}

		if (metrixFormDef.tables.get(0).hasOrderBys()) {
			query.append(" order by ");
			addComma = false;
			for (MetrixOrderByDef orderByDef : metrixFormDef.tables.get(0).orderBys) {
				if (addComma) {
					query.append(", ");
				}
				query.append(metrixFormDef.tables.get(0).tableName);
				query.append(".");
				query.append(orderByDef.columnName);
				query.append(" ");
				query.append(orderByDef.sortOrder);
			}
		}

		return query.toString();
	}

	/**
	 * Adds the constraint syntax for a field based upon the meta data for
	 * string data.
	 * 
	 * @param query
	 *            the sql query being generated.
	 * @param constraintDef
	 *            the meta data defining the constraint to be added.
	 */
	private static void addStringConstraint(StringBuilder query,
			MetrixConstraintDef constraintDef) {
		if (constraintDef.operand == MetrixConstraintOperands.EQUALS) {
			query.append(" = ");
			query.append("'");
			query.append(constraintDef.value);
			query.append("'");
		} else if (constraintDef.operand == MetrixConstraintOperands.NOT_EQUALS) {
			query.append(" != ");
			query.append("'");
			query.append(constraintDef.value);
			query.append("'");
		} else if (constraintDef.operand == MetrixConstraintOperands.LIKE) {
			query.append(" LIKE ");
			query.append("'%");
			query.append(constraintDef.value);
			query.append("%'");
		} else {
			query.append(constraintDef.operand);
			query.append("'");
			query.append(constraintDef.value);
			query.append("'");
		}
	}

	/**
	 * Adds the constraint syntax for a field based upon the meta data for
	 * integer data.
	 * 
	 * @param query
	 *            the sql query being generated.
	 * @param constraintDef
	 *            the meta data defining the constraint to be added.
	 */
	private static void addIntegerConstraint(StringBuilder query,
			MetrixConstraintDef constraintDef) {
		if (constraintDef.operand == MetrixConstraintOperands.LESS_THAN) {
			query.append(" < ");
			query.append(constraintDef.value);
		} else if (constraintDef.operand == MetrixConstraintOperands.GREATER_THAN) {
			query.append(" > ");
			query.append(constraintDef.value);
		} else if (constraintDef.operand == MetrixConstraintOperands.EQUALS) {
			query.append(" = ");
			query.append(constraintDef.value);
		} else if (constraintDef.operand == MetrixConstraintOperands.NOT_EQUALS) {
			query.append(" != ");
			query.append(constraintDef.value);
		}
	}

	/**
	 * Adds the constraint syntax for a field based upon the meta data for
	 * double data.
	 * 
	 * @param query
	 *            the sql query being generated.
	 * @param constraintDef
	 *            the meta data defining the constraint to be added.
	 */
	private static void addDoubleConstraint(StringBuilder query,
			MetrixConstraintDef constraintDef) {
		if (constraintDef.operand == MetrixConstraintOperands.LESS_THAN) {
			query.append(" < ");
			query.append(constraintDef.value);
		} else if (constraintDef.operand == MetrixConstraintOperands.GREATER_THAN) {
			query.append(" > ");
			query.append(constraintDef.value);
		} else if (constraintDef.operand == MetrixConstraintOperands.EQUALS) {
			query.append(" = ");
			query.append(constraintDef.value);
		} else if (constraintDef.operand == MetrixConstraintOperands.NOT_EQUALS) {
			query.append(" != ");
			query.append(constraintDef.value);
		}
	}
}
