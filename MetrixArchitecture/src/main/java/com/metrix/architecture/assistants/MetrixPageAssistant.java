package com.metrix.architecture.assistants;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;

/**
 * Contains helper methods to make it easy to interact with views on a layout.
 * 
 * @since 5.4
 */
public class MetrixPageAssistant {

	/**
	 * Gets all of the controls on the received layout and returns them as a
	 * list.
	 * 
	 * @param layout
	 *            the layout to get the controls from.
	 * @return a list containing all of the controls.
	 */
	public static List<View> GetLayoutControls(ViewGroup layout) {
		List<View> controls = new ArrayList<View>();

		for (int i = 0; i < layout.getChildCount(); i++)
			controls.add(layout.getChildAt(i));

		return controls;
	}
}
