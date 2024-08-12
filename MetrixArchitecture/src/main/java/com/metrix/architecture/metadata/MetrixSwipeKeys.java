package com.metrix.architecture.metadata;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * 
 * The MetrixSwipeKeys class is designed to be a container for a table's rows
 * that are used on a swipe-enabled screen. Each instance represents a single
 * database row with all of it's primary key column names and values.
 * 
 * @since 5.6
 */
public class MetrixSwipeKeys implements Parcelable {
	private List<String> mColumnNames = new ArrayList<String>();
	private List<String> mColumnValues = new ArrayList<String>();

	/**
	 * The MetrixSwipeKeys class default constructor.
	 */
	public MetrixSwipeKeys() {
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName The name of a column of the primary key.
	 * @param columnValue The value for the primary key column.
	 */
	public MetrixSwipeKeys(String columnName, String columnValue) {
		if (MetrixStringHelper.isNullOrEmpty(columnName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
		}

		if (MetrixStringHelper.isNullOrEmpty(columnValue)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnValueParamIsReq"));
		}

		this.addKey(columnName, columnValue);
	}
	
	private MetrixSwipeKeys(Parcel parcel) {
		parcel.readStringList(this.mColumnNames);
		parcel.readStringList(this.mColumnValues);
	}
	
	/**
	 * The addKey method allow you to add a key column name and value to this
	 * instance.
	 * 
	 * @param columnName
	 *            The name of the primary key column.
	 * @param columnValue
	 *            The value of the primary key column.
	 */
	public void addKey(String columnName, String columnValue) {
		if (MetrixStringHelper.isNullOrEmpty(columnName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
		}

		if (MetrixStringHelper.isNullOrEmpty(columnValue)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnValueParamIsReq"));
		}

		this.mColumnNames.add(columnName);
		this.mColumnValues.add(columnValue);
	}

	/**
	 * The getUniqueId method creates a unique hash value of all of the column
	 * names and values for this row which can be used to compare the row
	 * against others.
	 * 
	 * @return A unique hash value for this row.
	 */
	public String getUniqueId() {
		StringBuilder uniqueId = new StringBuilder();
		if (this.mColumnNames != null && this.mColumnNames.size() > 0) {
			for (int i = 0; i < this.mColumnNames.size(); i++) {
				uniqueId.append(this.mColumnNames.get(i));
				uniqueId.append("=");
				uniqueId.append(this.mColumnValues.get(i));
				uniqueId.append(";");
			}
		}
		return uniqueId.toString();
	}

	/**
	 * The getColumnNames method returns the names of all of the primary key
	 * columns for this row.
	 * 
	 * @return An ArrayList<String> of column names.
	 */
	public ArrayList<String> getColumnNames() {
		return (ArrayList<String>)this.mColumnNames;
	}

	/**
	 * The getColumnValue method returns the value for the column identified by
	 * the received column name.
	 * 
	 * @param columnName
	 *            The name of the column whose value this method should return.
	 * @return The column's value.
	 */
	public String getColumnValue(String columnName) {
		if (MetrixStringHelper.isNullOrEmpty(columnName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
		}

		if (this.mColumnNames.contains(columnName)) {
			return this.mColumnValues.get(this.mColumnNames.indexOf(columnName));
		} else {
			throw new IndexOutOfBoundsException(AndroidResourceHelper.getMessage("TheColumnnameDoesNotExist"));
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		arg0.writeStringList(this.mColumnNames);
		arg0.writeStringList(this.mColumnValues);
	}

	public static final Parcelable.Creator<MetrixSwipeKeys> CREATOR = new Parcelable.Creator<MetrixSwipeKeys>() {

		@Override
		public MetrixSwipeKeys createFromParcel(Parcel source) {
			return new MetrixSwipeKeys(source);
		}

		@Override
		public MetrixSwipeKeys[] newArray(int size) {
			return new MetrixSwipeKeys[size];
		}
	};
}
