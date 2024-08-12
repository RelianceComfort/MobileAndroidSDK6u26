package com.metrix.architecture.constants;

/**
 * Contains an class with methods that support interacting with 
 * the MetrixTransactionTypes enumeration. This is used primary
 * by the Sync process internally.
 * 
 * @since 5.4
 */
public class MetrixTransactionTypesConverter {
	/**
	 * Returns the SQL equivalent of a MetrixTransactionType enumeration.
	 * For example, this method will return 'insert' for 
	 * <code>MetrixTransactionType.INSERT</code>.
	 * 
	 * @param type The enumeration value to get the string value of.
	 * @return The string value of the enumeration.
	 */
	public static String toString(MetrixTransactionTypes type) {
		if (type == null) {
			return "";
		}
		
		switch (type) {
		case INSERT:
			return "insert";
		case UPDATE:
			return "update";
		case DELETE:
			return "delete";
		case SELECT:
			return "select";
		case OTHER:
			return "other";
		case CORRECT_ERROR:
			return "correct error";
		case TRUNC:
			return "trunc";
		case INITSTARTED:
			return "initstarted";
		case INITENDED:
			return "initended";
		case INITREQUESTED:
			return "initrequest";
		case NOTIFY:
			return "notify";
		case PCHANGE:
			return "pchange";
		default:
			return "";
		}
	}
	
	/**
	 * Returns the MetrixTransactionType enumeration equivalent of 
	 * a SQL transaction type. For example, this method will return
	 * <code>MetrixTransactionType.INSERT</code> for "insert".
	 * 
	 * @param type The string value of the enumeration.
	 * @return The enumeration value to get the string value of.
	 */
	public static MetrixTransactionTypes toEnum(String type) {
		if (type.compareToIgnoreCase("insert") == 0) {
			return MetrixTransactionTypes.INSERT;
		} else if (type.compareToIgnoreCase("update") == 0) {
			return MetrixTransactionTypes.UPDATE;
		} else if (type.compareToIgnoreCase("delete") == 0) {
			return MetrixTransactionTypes.DELETE;
		} else if (type.compareToIgnoreCase("select") == 0) {
			return MetrixTransactionTypes.SELECT;
		} else if (type.compareToIgnoreCase("other") == 0) {
			return MetrixTransactionTypes.OTHER;
		} else if (type.compareToIgnoreCase("correct error") == 0) {
			return MetrixTransactionTypes.CORRECT_ERROR;
		} else if (type.compareToIgnoreCase("trunc") == 0) {
			return MetrixTransactionTypes.TRUNC;
		} else if (type.compareToIgnoreCase("initstarted") == 0) {
			return MetrixTransactionTypes.INITSTARTED;
		} else if (type.compareToIgnoreCase("initended") == 0) {
			return MetrixTransactionTypes.INITENDED;
		} else if (type.compareToIgnoreCase("initrequest") == 0) {
			return MetrixTransactionTypes.INITREQUESTED;
		} else if (type.compareToIgnoreCase("notify") == 0) {
			return MetrixTransactionTypes.NOTIFY;
		} else if (type.compareToIgnoreCase("pchange") == 0) {
			return MetrixTransactionTypes.PCHANGE;
		}
		return null;
	}
}
