package com.metrix.architecture.metadata;

/**
 * @author elin
 * A holder class for storing column definition data
 */
public class TableColumnDef {
	public String column_name = "";
	private String column_type = "";
	private Boolean column_notnull = false;
	private Boolean column_primary_key = false;

	public TableColumnDef(String columnName, String dataType,
			String isPrimaryKey, String isNotNull) {
		this.column_name = columnName;
		this.setColumn_type(dataType);
		this.setColumn_primary_key(isPrimaryKey);
		this.setColumn_notnull(isNotNull);
	}

	public void setColumn_type(String column_type) {
		if (column_type.toLowerCase().contains("integer")
				|| column_type.toLowerCase().contains("numeric"))
			this.column_type = "numeric";
		else
			this.column_type = column_type;
	}

	public String getColumn_type() {
		return column_type;
	}

	public void setColumn_notnull(String column_notnull) {
		if (column_notnull.compareTo("0") == 0)
			this.column_notnull = false;
		else
			this.column_notnull = true;
	}

	public Boolean getColumn_notnull() {
		return column_notnull;
	}

	public void setColumn_primary_key(String column_primary_key) {
		if (column_primary_key.compareTo("0") == 0)
			this.column_primary_key = false;
		else
			this.column_primary_key = true;
	}

	public Boolean getColumn_primary_key() {
		return column_primary_key;
	}
}
