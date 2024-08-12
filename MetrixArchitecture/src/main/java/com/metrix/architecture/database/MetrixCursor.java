package com.metrix.architecture.database;

import android.database.Cursor;
import android.database.CursorWrapper;

public class MetrixCursor extends CursorWrapper {
	private Cursor mCursor;
	
	public MetrixCursor(Cursor cursor) {
		super(cursor);
		mCursor = cursor;		
	}
	
	public String getString(int columnIndex) {
		String dbString = "";
		if (MetrixDatabaseManager.isDecimalColumn(mCursor, columnIndex)) {
			dbString = String.valueOf(mCursor.getDouble(columnIndex));
		} else {
			dbString = mCursor.getString(columnIndex);
		}	
		return dbString;
	}
}