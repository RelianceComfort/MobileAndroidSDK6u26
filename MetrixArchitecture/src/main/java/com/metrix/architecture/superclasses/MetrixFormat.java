package com.metrix.architecture.superclasses;

import java.text.Format;
import android.content.Context;

/**
 * This class can be used to build subclasses of the Format class
 * that will be used by the application to format data-bound values
 * for layouts or lookups.
 * 
 * @since 5.6
 */
public abstract class MetrixFormat extends Format {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1476797298805312359L;

	public abstract String format(String value, Context context);
}