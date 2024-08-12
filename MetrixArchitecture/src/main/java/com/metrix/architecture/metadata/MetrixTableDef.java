package com.metrix.architecture.metadata;

import java.util.ArrayList;
import java.util.List;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.utilities.AndroidResourceHelper;

/**
 * Defines the meta data for a table that will be bound to the current layout.
 * 
 * @since 5.4
 */
public class MetrixTableDef {

	/**
	 * The name of the table to be bound.
	 */
	public String tableName;

	/**
	 * The type of transaction that will be performed against this table.
	 * (insert, update, delete, select)
	 */
	public MetrixTransactionTypes transactionType;

	/**
	 * Relationships between this table and parent tables also bound to the
	 * layout.
	 */
	public List<MetrixRelationDef> relations;

	/**
	 * Sort orders for selected data.
	 */
	public List<MetrixOrderByDef> orderBys;

	/**
	 * A list of the columns from this table which will be bound.
	 */
	public List<MetrixColumnDef> columns;

	/**
	 * A list of constraints that should be applied when selecting bound
	 * data from the database. In the case of an update or delete, this
	 * typically is used to select a row by it's primary key.
	 */
	public List<MetrixConstraintDef> constraints;

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table to be bound.
	 * @param transactionType
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 */
	public MetrixTableDef(String tableName, MetrixTransactionTypes transactionType) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.columns = new ArrayList<MetrixColumnDef>();
		this.relations = new ArrayList<MetrixRelationDef>();
		this.constraints = new ArrayList<MetrixConstraintDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table to be bound.
	 * @param transactionType
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 * @param relations
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 */
	public MetrixTableDef(String tableName, MetrixTransactionTypes transactionType, List<MetrixRelationDef> relations) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.columns = new ArrayList<MetrixColumnDef>();
		this.relations = relations;
		this.constraints = new ArrayList<MetrixConstraintDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table to be bound.
	 * @param transactionType
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 * @param relations
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 * @param constraints
	 *            A list of constraints that should be applied when selecting
	 *            this bound data from the database. In the case of an update or
	 *            delete, this typically is used to select a row by it's primary
	 *            key.
	 */
	public MetrixTableDef(String tableName, MetrixTransactionTypes transactionType, List<MetrixRelationDef> relations, List<MetrixConstraintDef> constraints) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.columns = new ArrayList<MetrixColumnDef>();
		this.relations = relations;
		this.constraints = constraints;
		this.orderBys = new ArrayList<MetrixOrderByDef>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table to be bound.
	 * @param transactionType
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 * @param relation
	 *            Relationship between this table and parent tables also bound
	 *            to the layout.
	 */
	public MetrixTableDef(String tableName, MetrixTransactionTypes transactionType, MetrixRelationDef relation) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.columns = new ArrayList<MetrixColumnDef>();
		this.relations = new ArrayList<MetrixRelationDef>();
		this.relations.add(relation);
		this.constraints = new ArrayList<MetrixConstraintDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table to be bound.
	 * @param transactionType
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 * @param constraint
	 *            A constraint that should be applied when selecting this bound
	 *            data from the database. In the case of an update or delete,
	 *            this typically is used to select a row by it's primary key.
	 */
	public MetrixTableDef(String tableName, MetrixTransactionTypes transactionType, MetrixConstraintDef constraint) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.columns = new ArrayList<MetrixColumnDef>();
		this.relations = new ArrayList<MetrixRelationDef>();
		this.constraints = new ArrayList<MetrixConstraintDef>();
		this.constraints.add(constraint);
		this.orderBys = new ArrayList<MetrixOrderByDef>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table to be bound.
	 * @param transactionType
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 * @param relation
	 *            Relationship between this table and parent tables also bound
	 *            to the layout.
	 * @param constraints
	 *            A list of constraints that should be applied when selecting
	 *            this bound data from the database. In the case of an update or
	 *            delete, this typically is used to select a row by it's primary
	 *            key.
	 */
	public MetrixTableDef(String tableName, MetrixTransactionTypes transactionType, MetrixRelationDef relation, List<MetrixConstraintDef> constraints) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.columns = new ArrayList<MetrixColumnDef>();
		this.relations = new ArrayList<MetrixRelationDef>();
		this.relations.add(relation);
		this.constraints = constraints;
		this.orderBys = new ArrayList<MetrixOrderByDef>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table to be bound.
	 * @param transactionType
	 *            Relationships between this table and parent tables also bound
	 *            to the layout.
	 * @param relation
	 *            Relationship between this table and parent tables also bound
	 *            to the layout.
	 * @param constraints
	 *            A list of constraints that should be applied when selecting
	 *            this bound data from the database. In the case of an update or
	 *            delete, this typically is used to select a row by it's primary
	 *            key.
	 * @param orderBys
	 *            Sort orders for selected data.
	 */
	public MetrixTableDef(String tableName, MetrixTransactionTypes transactionType, MetrixRelationDef relation, List<MetrixConstraintDef> constraints, List<MetrixOrderByDef> orderBys) {
		this.tableName = tableName;
		this.transactionType = transactionType;
		this.columns = new ArrayList<MetrixColumnDef>();
		this.relations = new ArrayList<MetrixRelationDef>();
		this.relations.add(relation);
		this.constraints = constraints;
		this.orderBys = orderBys;
	}

	/**
	 * Determines if this MetrixTableDef has any MetrixRelationDefs assigned to
	 * it.
	 * 
	 * @return TRUE if there are MetrixRelationDefs, FALSE otherwise.
	 */
	public boolean hasRelations() {
		if (this.relations != null && this.relations.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determines if this MetrixTableDef has any MetrixConstraintDef assigned to
	 * it.
	 * 
	 * @return TRUE if there are MetrixConstraintDefs, FALSE otherwise.
	 */
	public boolean hasConstraints() {
		if (this.constraints != null && this.constraints.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determines if this MetrixTableDef has any MetrixOrderByDefs associated to
	 * it.
	 * 
	 * @return TRUE if there are MetrixOrderByDefs, FALSE otherwise.
	 */
	public boolean hasOrderBys() {
		if (this.orderBys != null && this.orderBys.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();

		value.append(AndroidResourceHelper.getMessage("TableName1Args", this.tableName));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("TransactionType1Args", this.transactionType));

		return value.toString();
	}
}
