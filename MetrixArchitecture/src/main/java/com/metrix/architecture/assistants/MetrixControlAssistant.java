package com.metrix.architecture.assistants;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.metrix.architecture.R;
import com.metrix.architecture.attachment.AttachmentField;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixFieldManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.signature.SignatureField;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDate;
import com.metrix.architecture.utilities.MetrixDateTime;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixTime;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

import java.lang.ref.WeakReference;
import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains helper methods to make it easy to interact with views on a layout.
 * getControl allows you to request an instance of a view based on it's R.id or
 * MetrixColumnDef instance. getValue allows you to get the value of a view,
 * regardless of the view's type (i.e. <code>Spinner</code>,
 * <code>EditText</code>). populateSpinnerFromList allows you to fill the
 * contents of a spinner with an array you pass in and setValue allows you to
 * set the value of a view regardless of the view's type.
 * 
 * @since 5.4
 */
public class MetrixControlAssistant {

	private static final AtomicInteger uniqueViewId = new AtomicInteger(1);

	/**
	 * This method, and the associated static property are copies of a method
	 * and property that were added to Android API 17 to generate unique ids
	 * for dynamically added views on layouts.
	 *
	 * @return a unique view id.
	 * 
	 * @since 5.6.1
	 */
	public static int generateViewId() {
	    for (;;) {
	        final int result = uniqueViewId.get();
	        int newValue = result + 1;
	        if (newValue > 0x00FFFFFF) newValue = 1; 
	        if (uniqueViewId.compareAndSet(result, newValue)) {
	            return result;
	        }
	    }
	}

	/**
	 * Add a LinearLayout (and its context) identified by the received id to the received
	 * parent layout.
	 * 
	 * @param activity the current activity.
	 * @param id the id of the LinearLayout to add.
	 * @param parentLayout the parent layout to add the LinearLayout to.
	 * @return the LinearLayout that was added.
	 * 
	 * @since 5.6.1
	 */
	public static LinearLayout addLinearLayout(Activity activity, Integer id, ViewGroup parentLayout) {
		LinearLayout linearLayout = (LinearLayout)activity.getLayoutInflater().inflate(id, null);
		linearLayout.setId(MetrixControlAssistant.generateViewId());
		
		for (int i=0; i < linearLayout.getChildCount(); i++) {
			linearLayout.getChildAt(i).setId(MetrixControlAssistant.generateViewId());
		}
		
		parentLayout.addView(linearLayout);
		return linearLayout;
	}

	/**
	 * Add a LinearLayout (and its context) identified by the received id to the received
	 * parent layout. If the received tableName is not null and there is a child LinearLayout
	 * with the same value in it's contentDescription attribute, the LinearLayout should get
	 * added there instead.
	 * 
	 * @param activity the current activity.
	 * @param id the id of the LinearLayout to add.
	 * @param parentLayout the parent layout to add the LinearLayout to.
	 * @param region the name of the contentDescription layout to look for.
	 * @return the LinearLayout that was added.
	 * 
	 * @since 5.6.1
	 */
	public static LinearLayout addLinearLayout(Activity activity, Integer id, ViewGroup parentLayout, String region) {
		LinearLayout linearLayout = (LinearLayout)activity.getLayoutInflater().inflate(id, null);
		linearLayout.setId(MetrixControlAssistant.generateViewId());

		for (int i=0; i < linearLayout.getChildCount(); i++) {
			if (linearLayout.getChildAt(i) instanceof LinearLayout && !(linearLayout.getChildAt(i) instanceof AttachmentField) && !(linearLayout.getChildAt(i) instanceof SignatureField)) {
				LinearLayout childLinearLayout = (LinearLayout)linearLayout.getChildAt(i);
				for (int j=0; j < childLinearLayout.getChildCount(); j++) {
					childLinearLayout.getChildAt(j).setId(MetrixControlAssistant.generateViewId());
				}
			} else {
				// Allow AttachmentField and SignatureField to get its own View ID (even if it is a LinearLayout subclass)
				linearLayout.getChildAt(i).setId(MetrixControlAssistant.generateViewId());
			}
		}
		
		if (MetrixStringHelper.isNullOrEmpty(region)) {
			parentLayout.addView(linearLayout);
		} else {
			boolean foundContent = false;
			
			if (parentLayout.getChildCount() > 0) {
				for (int i=0; i < parentLayout.getChildCount(); i++) {
					View view = parentLayout.getChildAt(i);
					if (view.getContentDescription() != null && view.getContentDescription().toString().compareToIgnoreCase(region) == 0) {
						LinearLayout childLinearLayout = (LinearLayout)view;
						childLinearLayout.addView(linearLayout);
						foundContent = true;
						break;
					}
				}
			}
			
			if (!foundContent) {
				parentLayout.addView(linearLayout);
			}
		}
		return linearLayout;
	}
	
	/**
	 * Add a LinearLayout (and its context) identified by the received id to the received
	 * parent layout.  Ensures children views two levels down have id's.
	 * 
	 * @param activity the current activity.
	 * @param id the id of the LinearLayout to add.
	 * @param parentLayout the parent layout to add the LinearLayout to.
	 * @return the LinearLayout that was added.
	 * 
	 * @since 5.6.1
	 */
	public static LinearLayout addLinearLayoutWithInnerLinearLayouts(Activity activity, Integer id, ViewGroup parentLayout) {
		LinearLayout linearLayout = (LinearLayout)activity.getLayoutInflater().inflate(id, null);
		linearLayout.setId(MetrixControlAssistant.generateViewId());
		
		for (int i = 0; i < linearLayout.getChildCount(); i++) {
			View v = linearLayout.getChildAt(i);
			v.setId(MetrixControlAssistant.generateViewId());
			
			if (v instanceof LinearLayout) {
				LinearLayout childLinearLayout = (LinearLayout) v;
				for (int j = 0; j < childLinearLayout.getChildCount(); j++) {
					childLinearLayout.getChildAt(j).setId(MetrixControlAssistant.generateViewId());
				}
			}
		}
		
		parentLayout.addView(linearLayout);
		return linearLayout;
	}

	/**
	 * Add a LinearLayout (and its context) identified by the received id to the received
	 * parent layout - but do not set IDs on the child elements (assuming that IDs have already been assigned in the layout).
	 *
	 * @param activity the current activity.
	 * @param id the id of the LinearLayout to add.
	 * @param parentLayout the parent layout to add the LinearLayout to.
	 * @return the LinearLayout that was added.
	 *
	 * @since 5.7.0
	 */
	public static LinearLayout addLinearLayoutWithoutChildIDs(Activity activity, Integer id, ViewGroup parentLayout) {
		LinearLayout linearLayout = (LinearLayout)activity.getLayoutInflater().inflate(id, null);
		linearLayout.setId(MetrixControlAssistant.generateViewId());
		parentLayout.addView(linearLayout);
		return linearLayout;
	}

	/**
	 * Sets an inputType on a view found on the received layout based
	 * on the received view id.
	 *
	 * @param layout the layout containing the view.
	 * 
	 * @since 5.6.1
	 */
	public static void addInputType(Integer id, ViewGroup layout, Integer inputType) {
		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		View view = MetrixControlAssistant.getControl(id, layout);
		
		if (view != null) {
			if (view instanceof EditText) {
				EditText editText = (EditText) view;
				editText.setInputType(inputType);
			}			
		} else {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}
	}
	
	/**
	 * Sets an inputType on a view found on the received layout based
	 * on the received MetrixColumnDef.
	 * 
	 * @param columnDef the MetrixColumnDef associated to the view.
	 * @param layout the layout containing the view.
	 * 
	 * @since 5.6.1
	 */
	public static void addInputType(MetrixColumnDef columnDef, ViewGroup layout, Integer inputType) {
		if (columnDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumndefParameterIsRequired"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		MetrixControlAssistant.addInputType(columnDef.id, layout, inputType);
	}
	
	/**
	 * Gets a control based on it's id from the received layout.
	 * 
	 * @param id
	 *            the id of the control.
	 * @param layout
	 *            the layout the control exists on.
	 * @return the control.
	 * 
	 *         <pre>
	 * EditText myEditText = (EditText) MetrixControlAssistant.getControl(R.id.my_edit_text, mLayout);
	 * </pre>
	 */
	public static View getControl(Integer id, ViewGroup layout) {
		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		return layout.findViewById(id);
	}

	/**
	 * Gets a control based on it's meta data from the received layout.
	 * 
	 * @param columnDef
	 *            the meta data for the control.
	 * @param layout
	 *            the layout the controls exists on.
	 * @return the control.
	 * 
	 *         <pre>
	 * View view = MetrixControlAssistant.getControl(columnDef, layout);
	 * </pre>
	 */
	public static View getControl(MetrixColumnDef columnDef, ViewGroup layout) {
		if (columnDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumndefParameterIsRequired"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		return layout.findViewById(columnDef.id);
	}

	/**
	 * Gets a control based on the name of the table and column bound to it.
	 * 
	 * @param formDef The activity's form definition.
	 * @param layout The activity's layout.
	 * @param tableName The name of the table the control is bound to.
	 * @param columnName The name of the column the control is bound to.
	 * @return An instance of View representing the control. If the control was not found, NULL will be returned.
	 * 
	 * @since 5.6.1
	 */
	public static View getControl(MetrixFormDef formDef, ViewGroup layout, String tableName, String columnName) {
		if (formDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
		}
		
		if (tableName == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
		}

		if (columnName == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
		}
		
		for (MetrixTableDef tableDef : formDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
						return MetrixControlAssistant.getControl(columnDef.id, layout);
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the unique identifier of the field metadata based on the name of the table and column bound to it.
	 * 
	 * @param formDef The activity's form definition.
	 * @param tableName The name of the table the control is bound to.
	 * @param columnName The name of the column the control is bound to.
	 * 
	 * @since 5.6.3
	 */
	public static Integer getFieldId(MetrixFormDef formDef, String tableName, String columnName) {
		if (formDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
		}

		if (tableName == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
		}

		if (columnName == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
		}

		for (MetrixTableDef tableDef : formDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
						return columnDef.fieldId;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the value of a control based on the table column the control is bound to.
	 * 
	 * @param formDef The activity's form definition.
	 * @param layout The activity's layout.
	 * @param tableName The name of the table the control is bound to.
	 * @param columnName The name of the column the control is bound to.
	 * @return The control's value.
	 * 
	 * @since 5.6.1
	 */
	public static String getValue(MetrixFormDef formDef, ViewGroup layout, String tableName, String columnName) {
        if (formDef == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
        }

        if (tableName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
        }

        if (columnName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
        }

		for (MetrixTableDef tableDef : formDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
						return MetrixControlAssistant.getValue(columnDef.id, layout);
					}
				}
				break;
			}
		}
		
		return "";
	}
	
	/**
	 * Gets the value from a control based on it's id from the received layout.
	 * 
	 * @param id
	 *            the id of the control.
	 * @param layout
	 *            the layout the control exists on.
	 * @return the value of the control.
	 * 
	 *         <pre>
	 * final String personId = MetrixControlAssistant.getValue(R.id.personId, mLayout);
	 * </pre>
	 */
	public static String getValue(Integer id, ViewGroup layout) {
		try {
			View view = MetrixControlAssistant.getControl(id, layout);
			return MetrixControlAssistant.getValue(view);
		} catch (Exception ex) {
			return "";
		}
	}

	/**
	 * Gets the value from a control based on it's meta data from the received
	 * layout.
	 * 
	 * @param columnDef
	 *            the meta data for the control.
	 * @param layout
	 *            the layout the control exists on.
	 * @return the value of the control.
	 * 
	 *         <pre>
	 * String value = MetrixControlAssistant.getValue(columnDef, layout);
	 * </pre>
	 */
	public static String getValue(MetrixColumnDef columnDef, ViewGroup layout) {
		if (columnDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumndefParameterIsRequired"));
		}
		
		try {
			View view = MetrixControlAssistant.getControl(columnDef, layout);
			String viewValue = "";

			if (columnDef.maximumCharacters <= 0)
				viewValue = MetrixControlAssistant.getValue(view);
			else
				viewValue = MetrixControlAssistant.getTag(view);

			String returnValue = "";

			if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
				returnValue = MetrixFloatHelper.convertNumericFromUIToDB(viewValue);
			} else if (columnDef.dataType == String.class) {
				returnValue = viewValue;
			} else if (columnDef.dataType == Date.class || columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixTime.class || columnDef.dataType == MetrixDateTime.class) {
				returnValue = MetrixDateTimeHelper.convertDateTimeFromUIToDB(viewValue);
			}

			return returnValue;
		} catch (Exception ex) {
			return "";
		}
	}

	/**
	 * Get the value from a control.
	 * 
	 * @param view
	 *            the control to extract the value from.
	 * @return the value
	 * @throws Exception
	 * 
	 *             <pre>
	 * Spinner statusSpinner = (Spinner) MetrixControlAssistant.getControl(columnDef, layout);
	 * String value = MetrixControlAssistant.getValue(statusSpinner);
	 * </pre>
	 */
	public static String getValue(View view) throws Exception {
		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		if (view instanceof AttachmentField) {
			TextView textView = ((AttachmentField) view).mHiddenAttachmentIdTextView;
			return textView.getText().toString();
		}

		if (view instanceof SignatureField) {
			TextView textView = ((SignatureField) view).mHiddenAttachmentIdTextView;
			return textView.getText().toString();
		}

		if (view instanceof CheckBox) {
			CheckBox checkbox = (CheckBox) view;
			if (checkbox.isChecked()) {
				return "Y";
			} else {
				return "N";
			}
		}

		if (view instanceof CheckedTextView) {
			CheckedTextView checkedTextView = (CheckedTextView) view;
			if (checkedTextView.isChecked()) {
				return "Y";
			} else {
				return "N";
			}
		}

		if (view instanceof Button) {
			Button button = (Button) view;
			return button.getText().toString();
		}

		if (view instanceof MetrixHyperlink) {
			MetrixHyperlink hyperlink = (MetrixHyperlink) view;
			return hyperlink.getLinkText();
		}

		if (view instanceof TextView) {
			TextView textView = (TextView) view;
			return textView.getText().toString();
		}

		if (view instanceof EditText) {
			EditText editText = (EditText) view;
			return editText.getText().toString();
		}

		if (view instanceof Spinner) {
			Spinner spinner = (Spinner) view;
			Object selectedItem = spinner.getSelectedItem();
			if (selectedItem != null) {
				if (selectedItem instanceof SpinnerKeyValuePair) {
					SpinnerKeyValuePair pair = (SpinnerKeyValuePair) selectedItem;
					return pair.spinnerValue;
				} else if (selectedItem instanceof String) {
					String value = (String) selectedItem;
					return value;
				}
			}
			return "";
		}

		throw new Exception(AndroidResourceHelper.getMessage("TheRecvWidgetTypeIs1Args", view.getClass().getName()));
	}

	/**
	 * Attempt to get the items for the Spinner parameter.
	 * @param v The view to inspect.  This should be a Spinner.
	 * @return Either an ArrayList of Strings or SpinnerKeyValuePairs for the view
     */
	public static ArrayList<Object> getSpinnerItems(View v) {
		ArrayList<Object> itemsList = null;
		if (v instanceof Spinner) {
			Spinner spn = (Spinner) v;
			SpinnerAdapter spnAdapter = spn.getAdapter();
			if (spnAdapter != null) {
				int spnCount = spnAdapter.getCount();
				if (spnCount > 0) {
					itemsList = new ArrayList<Object>();
					for (int i = 0; i < spnCount; i++) {
						itemsList.add(spnAdapter.getItem(i));
					}
				}
			}
		}
		return itemsList;
	}

	/**
	 * Get the String value of the Tag object from a control.
	 * 
	 * @param view
	 *            the control to extract the value from.
	 * @return the value
	 * @throws Exception
	 * 
	 *             <pre>
	 * EditText descriptionText = (EditText) MetrixControlAssistant.getControl(columnDef, layout);
	 * String value = MetrixControlAssistant.getValue(descriptionText);
	 * </pre>
	 */
	public static String getTag(View view) throws Exception {
		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		try {
			if (view instanceof Button) {
				Button button = (Button) view;
				return button.getTag().toString();
			}

			if (view instanceof EditText) {
				EditText editText = (EditText) view;
				return editText.getTag().toString();
			}

			if (view instanceof TextView) {
				TextView textView = (TextView) view;
				return textView.getTag().toString();
			}
		} catch (Exception ex) {
			return "";
		}

		throw new Exception(AndroidResourceHelper.getMessage("TheRecvWidgetTypeIs1Args", view.getClass().getName()));
	}

	/**
	 * Sets the value of a control based on the table column the control is bound to.
	 * 
	 * @param formDef The activity's form definition.
	 * @param layout The activity's layout.
	 * @param tableName The name of the table the control is bound to.
	 * @param columnName The name of the column the control is bound to.
	 * @param value The value to set.
	 * 
	 * @since 5.6.1
	 */
	public static void setValue(MetrixFormDef formDef, ViewGroup layout, String tableName, String columnName, String value) {
        if (formDef == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
        }

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

        if (tableName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
        }

        if (columnName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
        }

		for (MetrixTableDef tableDef : formDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
						try {
							MetrixControlAssistant.setValue(columnDef.id, layout, value);
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
					}
				}
				break;
			}
		}		
	}

	/**
	 * Sets the value for a control based on it's id.
	 * 
	 * @param id
	 *            the id of the control to set.
	 * @param layout
	 *            the layout on which the control exists.
	 * @param value
	 *            the value to assign the control.
	 * @throws Exception
	 * 
	 *             <pre>
	 * MetrixControlAssistant.setValue(R.id.escalation__esc_status, mLayout, &quot;OPEN&quot;);
	 * </pre>
	 */
	public static void setValue(Integer id, ViewGroup layout, String value) throws Exception {
		MetrixControlAssistant.setValue(id, layout, value, null);
	}

	/**
	 * Sets the value for a control based on it's id.
	 * 
	 * @param id
	 *            the id of the control to set.
	 * @param layout
	 *            the layout on which the control exists.
	 * @param value
	 *            the value to assign the control.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @throws Exception
	 */
	public static void setValue(Integer id, ViewGroup layout, String value, Format formatter) throws Exception {
		View view = MetrixControlAssistant.getControl(id, layout);
		MetrixControlAssistant.setValue(view, value, formatter);
	}

	/**
	 * Sets the value for a control based on it's meta data.
	 * 
	 * @param columnDef
	 *            the meta data for the control.
	 * @param layout
	 *            the layout on which the control exists.
	 * @param value
	 *            the value to assign the control.
	 * @throws Exception
	 * 
	 *             <pre>
	 * MetrixControlAssistant.setValue(escalationStatusColumnDef, mLayout, &quot;OPEN&quot;);
	 * </pre>
	 */
	public static void setValue(MetrixColumnDef columnDef, ViewGroup layout, String value) throws Exception {
		if (columnDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumndefParameterIsRequired"));
		}

		View view = MetrixControlAssistant.getControl(columnDef, layout);

		String viewValue = "";

		if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
			viewValue = MetrixFloatHelper.convertNumericFromDBToUI(value, columnDef.formatter);
		} else if (columnDef.dataType == String.class) {
			viewValue = value;
		} else if (columnDef.dataType == Date.class || columnDef.dataType == MetrixDateTime.class) {
			viewValue = MetrixDateTimeHelper.convertDateTimeFromDBToUI(value, columnDef.formatter);
		} else if (columnDef.dataType == MetrixDate.class) {
			viewValue = MetrixDateTimeHelper.convertDateTimeFromDBToUIDateOnly(value);
		} else if (columnDef.dataType == MetrixTime.class) {
			viewValue = MetrixDateTimeHelper.convertDateTimeFromDBToUITimeOnly(value);
		}

		MetrixControlAssistant.setValue(view, viewValue);
	}

	/**
	 * Sets the value of the control.
	 * 
	 * @param view
	 *            the control to set the value of.
	 * @param value
	 *            the value.
	 * @throws Exception
	 * 
	 *             <pre>
	 * EditText transactionAmount = (EditText) activity.findViewById(com.metrix.metrixmobile.R.id.non_part_usage__transaction_amount);
	 * MetrixControlAssistant.setValue(transactionAmount, &quot;1&quot;);
	 * </pre>
	 */
	public static void setValue(View view, String value) throws Exception {
		MetrixControlAssistant.setValue(view, value, null);
	}

	/**
	 * Sets the value of the control.
	 * 
	 * @param view
	 *            the control to set the value of.
	 * @param value
	 *            the value.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void setValue(View view, String value, Format formatter) throws Exception {
		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		if (view instanceof AttachmentField) {
			AttachmentField attachField = ((AttachmentField) view);
			TextView textView = attachField.mHiddenAttachmentIdTextView;
			String finalizedValue = (formatter != null) ? formatter.format(value) : value;
			textView.setText(finalizedValue);
			attachField.updateFieldUI();
			return;
		}

		if (view instanceof SignatureField) {
			SignatureField signatureField = ((SignatureField) view);
			TextView textView = signatureField.mHiddenAttachmentIdTextView;
			String finalizedValue = (formatter != null) ? formatter.format(value) : value;
			textView.setText(finalizedValue);
			signatureField.updateFieldUI();
			return;
		}

		if (view instanceof CheckBox) {
			CheckBox checkbox = (CheckBox) view;
			if (value != null && value.compareToIgnoreCase("Y") == 0) {
				checkbox.setChecked(true);
			} else {
				checkbox.setChecked(false);
			}
			return;
		}

		if (view instanceof CheckedTextView) {
			CheckedTextView checkedTextView = (CheckedTextView) view;
			if (value != null && value.compareToIgnoreCase("Y") == 0) {
				checkedTextView.setChecked(true);
			} else {
				checkedTextView.setChecked(false);
			}
			return;
		}

		if (view instanceof Button) {
			Button button = (Button) view;
			button.setText(value);
			return;
		}

		if (view instanceof MetrixHyperlink) {
			MetrixHyperlink hyperlink = (MetrixHyperlink) view;
			if (formatter != null) {
				hyperlink.setLinkText(formatter.format(value));
			} else {
				hyperlink.setLinkText(value);
			}
			return;
		}

		if (view instanceof EditText) {
			EditText editText = (EditText) view;
			if (formatter != null) {
				editText.setText(formatter.format(value));
			} else {
				editText.setText(value);
			}
			return;
		}

		if (view instanceof TextView) {
			TextView textView = (TextView) view;
			if (formatter != null) {
				textView.setText(formatter.format(value));
			} else {
				textView.setText(value);
			}
			return;
		}

		if (view instanceof Spinner) {
			Spinner spinner = (Spinner) view;
			Object trialItem = spinner.getItemAtPosition(0);
			
			if (trialItem instanceof SpinnerKeyValuePair) {				
				ArrayAdapter<SpinnerKeyValuePair> adapter = (ArrayAdapter<SpinnerKeyValuePair>) spinner.getAdapter();
				if (adapter != null && value != null) {
					if(!spinner.isEnabled()){														
						ArrayList<SpinnerKeyValuePair> items = new ArrayList<SpinnerKeyValuePair>();
						for (int i = 0; i < adapter.getCount(); i++) {
							SpinnerKeyValuePair item = adapter.getItem(i);
							items.add(item);
						}
						
						if (MetrixFieldManager.spinnerReadOnlyStyle != 0)
							spinner.setAdapter(new ArrayAdapter<SpinnerKeyValuePair>(spinner.getContext(), MetrixFieldManager.spinnerReadOnlyStyle, items));
					}
					
					for (int i = 0; i < adapter.getCount(); i++) {
						if (value.compareTo(adapter.getItem(i).spinnerValue) == 0) {
							// spinner.setAdapter(adapter);
							spinner.setSelection(i, true);
							return;
						}
					}
				}
			} else if (trialItem instanceof String) {
				ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
				if (adapter != null && value != null) {
					if(!spinner.isEnabled()){														
						ArrayList<String> items = new ArrayList<String>();
						for (int i = 0; i < adapter.getCount(); i++) {
							String item = adapter.getItem(i);
							items.add(item);
						}
						
						if (MetrixFieldManager.spinnerReadOnlyStyle != 0)
							spinner.setAdapter(new ArrayAdapter<String>(spinner.getContext(), MetrixFieldManager.spinnerReadOnlyStyle, items));
					}
					
					for (int i = 0; i < adapter.getCount(); i++) {
						if (value.compareTo(adapter.getItem(i)) == 0) {
							spinner.setSelection(i, true);
							return;
						}
					}
				}
			}
									
			return;
		}

		throw new Exception(AndroidResourceHelper.getMessage("TheRecvWidgetTypeIs1Args", view.getClass().getName()));
	}

	/**
	 * Sets the value object to the tag of the control.
	 * 
	 * @param columnDef
	 *            the meta data for the control.
	 * @param layout
	 *            the layout on which the control exists.
	 * @param value
	 *            the value.
	 * @throws Exception
	 */
	public static void setTag(MetrixColumnDef columnDef, ViewGroup layout, String value) throws Exception {
		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		View view = MetrixControlAssistant.getControl(columnDef, layout);

		if (view instanceof EditText) {
			EditText editText = (EditText) view;
			editText.setTag(value);

			return;
		}

		if (view instanceof TextView) {
			TextView textView = (TextView) view;
			textView.setTag(value);

			return;
		}

		throw new Exception(AndroidResourceHelper.getMessage("TheRecvWidgetTypeIs1Args", view.getClass().getName()));
	}

	/**
	 * This method will (re)populate a spinner using the list of objects passed in.
	 * @param activity  the activity the spinner belongs to.
	 * @param view  the spinner.
	 * @param list  the list of values (Strings or SpinnerKeyValuePairs) to populate the spinner with.
     */
	public static void populateSpinnerFromGenericList(Activity activity, View view, ArrayList<Object> list) {
		if (list != null) {
			Object firstItem = list.get(0);
			if (firstItem instanceof SpinnerKeyValuePair) {
				ArrayList<SpinnerKeyValuePair> skvpList = new ArrayList<SpinnerKeyValuePair>();
				for (Object item : list) {
					skvpList.add((SpinnerKeyValuePair)item);
				}
				MetrixControlAssistant.populateSpinnerFromPair(activity, view, skvpList);
			} else if (firstItem instanceof String) {
				ArrayList<String> strList = new ArrayList<String>();
				for (Object item : list) {
					strList.add((String)item);
				}
				MetrixControlAssistant.populateSpinnerFromList(activity, view, strList);
			}
		}
	}

	/**
	 * Populates the contents of a spinner with the items in the received
	 * ArrayList.
	 * 
	 * @param activity
	 *            the activity that the spinner belongs to.
	 * @param id
	 *            the Android id of the spinner.
	 * @param layout
	 *            the layout that the spinner is part of.
	 * @param list
	 *            the list of values to populate the spinner with.
	 * 
	 *            <pre>
	 * ArrayList&lt;String&gt; months = new ArrayList&lt;String&gt;();
	 * for (int i = 1; i &lt; 13; i++) {
	 * 	String month = &quot;&quot;;
	 * 	if (i &lt; 10) {
	 * 		month = &quot;0&quot; + String.valueOf(i);
	 * 	} else {
	 * 		month = String.valueOf(i);
	 * 	}
	 * 	months.add(month);
	 * }
	 * 
	 * MetrixControlAssistant.populateSpinnerFromList(this, R.id.payment__expire_month, mLayout, months);
	 * </pre>
	 */
	public static void populateSpinnerFromList(Activity activity, Integer id, ViewGroup layout, ArrayList<String> list) {
		View view = MetrixControlAssistant.getControl(id, layout);
		MetrixControlAssistant.populateSpinnerFromList(activity, view, list);
	}

	/**
	 * Populates the contents of a spinner with the items in the received
	 * ArrayList.
	 * 
	 * @param activity
	 *            the activity the spinner belongs to.
	 * @param view
	 *            the spinner.
	 * @param list
	 *            the list of values to populate the spinner with.
	 * 
	 *            <pre>
	 * ArrayList&lt;String&gt; months = new ArrayList&lt;String&gt;();
	 * for (int i = 1; i &lt; 13; i++) {
	 * 	String month = &quot;&quot;;
	 * 	if (i &lt; 10) {
	 * 		month = &quot;0&quot; + String.valueOf(i);
	 * 	} else {
	 * 		month = String.valueOf(i);
	 * 	}
	 * 	months.add(month);
	 * }
	 * 
	 * MetrixControlAssistant.populateSpinnerFromList(this, expireMonthSpinner, months);
	 * </pre>
	 */
	public static void populateSpinnerFromList(Activity activity, View view, ArrayList<String> list) {
		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		Spinner spinner = (Spinner) view;
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, R.layout.spinner_item, list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	/**
	 * Populates the contents of a spinner with the items in the received
	 * ArrayList of SpinnerKeyValuePair.
	 * 
	 * @param activity
	 *            the activity the spinner belongs to.
	 * @param id
	 *            the Android id of the spinner.
	 * @param layout
	 *            the layout that the spinner is part of.
	 * @param list
	 *            the list of values to populate the spinner with.
	 */
	public static void populateSpinnerFromPair(Activity activity, Integer id, ViewGroup layout, ArrayList<SpinnerKeyValuePair> list) {
		View view = MetrixControlAssistant.getControl(id, layout);
		MetrixControlAssistant.populateSpinnerFromPair(activity, view, list);
	}

	/**
	 * Populates the contents of a spinner with the items in the received
	 * ArrayList of SpinnerKeyValuePair.
	 * 
	 * @param activity
	 *            the activity the spinner belongs to.
	 * @param view
	 *            the spinner.
	 * @param list
	 *            the list of values to populate the spinner with.
	 */
	public static void populateSpinnerFromPair(Activity activity, View view, ArrayList<SpinnerKeyValuePair> list) {
		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		Spinner spinner = (Spinner) view;
		ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(activity, R.layout.spinner_item, list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	/**
	 * Populates the contents of a spinner with the results of a database query.
	 * 
	 * @param activity
	 *            the activity the spinner belongs to.
	 * @param view
	 *            the spinner.
	 * @param query
	 *            the query to select the values to populate the spinner with.
	 * 
	 *            <pre>
	 * MetrixControlAssistant.populateSpinnerFromQuery(activity, view, query.toString());
	 * </pre>
	 */
	public static void populateSpinnerFromQuery(Activity activity, View view, String query) {
		MetrixControlAssistant.populateSpinnerFromQuery(activity, view, query, true);
	}

	/**
	 * Populates the contents of a spinner with the results of a database query.
	 * 
	 * @param activity
	 *            the activity the spinner belongs to.
	 * @param view
	 *            the spinner.
	 * @param query
	 *            the query to select the values to populate the spinner with.
	 * @param displayNull
	 *            display an empty option in the spinner.
	 * 
	 *            <pre>
	 * MetrixControlAssistant.populateSpinnerFromQuery(activity, view, query.toString(), true);
	 * </pre>
	 */
	public static void populateSpinnerFromQuery(Activity activity, View view, String query, boolean displayNull) {
		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		MetrixCursor cursor = null;
		SpinnerKeyValuePair items[] = null;

		try {
			String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params","param_value","param_name='MAX_ROWS'");
			if (!MetrixStringHelper.isNullOrEmpty(maxRows)){
				query = query + " limit " + maxRows;
			}
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);

			if (cursor == null || !cursor.moveToFirst()) {
				ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(activity, R.layout.spinner_item,
						new SpinnerKeyValuePair[0]);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				Spinner spinner = (Spinner) view;
				spinner.setAdapter(adapter);
				return;
			}

			int i = 0;
			if (displayNull) {
				items = new SpinnerKeyValuePair[cursor.getCount() + 1];
				items[0] = new SpinnerKeyValuePair("", "");
				i = 1;
			} else {
				items = new SpinnerKeyValuePair[cursor.getCount()];
			}

			while (cursor.isAfterLast() == false) {
				if (cursor.getColumnCount() == 1) {
					items[i] = new SpinnerKeyValuePair(cursor.getString(0), cursor.getString(0));
				} else {
					items[i] = new SpinnerKeyValuePair(cursor.getString(0), cursor.getString(1));
				}
				cursor.moveToNext();
				i = i + 1;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(activity, R.layout.spinner_item, items);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = (Spinner) view;
		spinner.setAdapter(adapter);
	}

	/**
	 * Clears all of the items in a spinner.
	 * 
	 * @param activity
	 *            the activity the spinner belongs to.
	 * @param view
	 *            the spinner.
	 * 
	 *            <pre>
	 * MetrixControlAssistant.clearSpinner(this, mProblem);
	 * </pre>
	 */
	public static void clearSpinner(Activity activity, View view) {
		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		Spinner spinner = (Spinner) view;
		if (spinner == null || spinner.getCount() == 0) {
			return;
		}

		SpinnerKeyValuePair items[] = new SpinnerKeyValuePair[0];

		ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(activity, R.layout.spinner_item, items);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	/**
	 * Converts a view to a read-only display (i.e. a TextView) with the same
	 * id. This method will first remove the view referenced by the id received
	 * and then it will add a TextView and assign the TextView the received id.
	 * This method should be invoked BEFORE invoking setupForm so that the
	 * read-only views will be bound correctly to the data.
	 * 
	 * @param activity
	 *            The activity the view belongs to.
	 * @param id
	 *            The id of the control to convert to read-only.
	 * @param layout
	 *            The ViewGroup containing the view.
	 * @param style
	 *            The style to apply to the read-only view that is added.
	 * 
	 *            <pre>
	 * MetrixControlAssistant.convertToReadOnlyDisplay(this, R.id.task__task_status, mLayout, R.style.TextViewBase_ReadOnlyValue_Table_TwoColumn);
	 * </pre>
	 */
	public static void convertToReadOnlyDisplay(Activity activity, int id, ViewGroup layout, int style) {
		View view = MetrixControlAssistant.getControl(id, layout);
		MetrixControlAssistant.convertToReadOnlyDisplay(activity, view, style);
	}

	/**
	 * Converts a view to a read-only display (i.e. a TextView) with the same
	 * id. This method will first remove the view referenced by the id received
	 * and then it will add a TextView and assign the TextView the received id.
	 * This method should be invoked BEFORE invoking setupForm so that the
	 * read-only views will be bound correctly to the data.
	 * 
	 * @param activity
	 *            The activity the view belongs to.
	 * @param view
	 *            The view to convert to read-only.
	 * @param style
	 *            The style to apply to the read-only view that is added.
	 * 
	 *            <pre>
	 * MetrixControlAssistant.convertToReadOnlyDisplay(this, R.id.task__task_status, mLayout, R.style.TextViewBase_ReadOnlyValue_Table_TwoColumn);
	 * </pre>
	 */
	public static void convertToReadOnlyDisplay(Activity activity, View view, int style) {
		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (view == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheViewParameterIsRequired"));
		}

		int id = view.getId();

		LinearLayout linearLayout = (LinearLayout) view.getParent();
		linearLayout.removeView(view);

		TextView readOnlyView = new TextView(activity);
		readOnlyView.setId(id);
		readOnlyView.setTextAppearance(activity, style);
		linearLayout.addView(readOnlyView, new LinearLayout.LayoutParams((LayoutParams.WRAP_CONTENT), (LayoutParams.WRAP_CONTENT)));
	}

	/***
	 * Compares the current value of a field to it's original value when the
	 * screen was first opened.
	 * 
	 * @param id
	 *            The id of the view to evaluate.
	 * @param formDef
	 *            The activity's MetrixFormDef.
	 * @param layout
	 *            The layout associated to the activity.
	 * @return TRUE if the value changed, FALSE otherwise.
	 */
	public static boolean valueChanged(int id, MetrixFormDef formDef, ViewGroup layout) {
		if (formDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
		}

		String columnName = "";
		String tableName = "";
		for (MetrixTableDef table : formDef.tables) {
			for (MetrixColumnDef column : table.columns) {
				if (column.id == id) {
					tableName = table.tableName;
					columnName = column.columnName;
					break;
				}
			}
			if (!MetrixStringHelper.isNullOrEmpty(columnName)) {
				break;
			}
		}

		String currentValue;
		String originalValue;

		if (!MetrixStringHelper.isNullOrEmpty(columnName)) {
			currentValue = getValue(id, layout);
			originalValue = MetrixFormManager.getOriginalValue(tableName + "." + columnName);

			if (originalValue == null) {
				originalValue = "";
			}
			
			if (currentValue.compareToIgnoreCase(originalValue) == 0) {
				return false;
			} else {
				return true;
			}
		}

		return false;
	}

	/***
	 * Sets the initial control state of a spinner and sets the listener which
	 * should be invoked when a users selects a new value.
	 * 
	 * @param spinner
	 *            The spinner to register the listener for.
	 * @param listener
	 *            The listener (method) which should be invoked when the user
	 *            changes the value.
	 */
	public static void setOnItemSelectedListener(Spinner spinner, OnItemSelectedListener listener) {
		if (spinner == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheSpinnerParamIsReq"));
		}

		if (listener == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheListenerParamIsReq"));
		}

		spinner.setTag(true);
		spinner.setOnItemSelectedListener(listener);
	}

	/***
	 * Indicates whether or not is spinner is ready and whether the invoking
	 * activity should react to it's OnItemSelectedListener invocation.
	 * 
	 * @param spinner
	 *            The spinner to evaluate.
	 * @return TRUE if the spinner is initialized, FALSE otherwise.
	 */
	public static boolean spinnerIsReady(Spinner spinner) {
		if (spinner == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheSpinnerParamIsReq"));
		}
		
		if ((Boolean) spinner.getTag()) {
			return true;
		} else {
			spinner.setTag(false);
			return false;
		}
	}

	/***
	 * Sets the visibility state of a view's parent. This can be used to hide or 
	 * display a view and it's associated TextView label by setting the visibility
	 * state on the parent LinearLayout.
	 * 
	 * @param id The R.id of the view.
	 * @param layout The current layout.
	 * @param state The view state to apply (View.VISIBLE, View.INVISIBLE, or View.GONE).
	 * 
	 * @since 5.6
	 */
	public static void setParentVisibility(int id, ViewGroup layout, int state) {
		if (state != View.VISIBLE && state != View.INVISIBLE && state != View.GONE) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheStateSpecIsInvalid"));
		}
		View view = MetrixControlAssistant.getControl(id, layout);
		View parent = (View) view.getParent();
		parent.setVisibility(state);
	}

    ;/***
     * Sets the visibility state of label and control of a specific column of a table without hiding the entire parent.
     * This is being introduced to hide controls in landscape mode without hiding the parent.
     * @param layout
     * @param state State to be set on the label & control
     * @param formDef
     * @param tableName Table name of the controls
     * @param columnName Column name of the controls
     *
     * @since 5.7
     */
	public static void setParentVisibility(ViewGroup layout, int state, MetrixFormDef formDef, String tableName, String columnName) {
		if (state != View.VISIBLE && state != View.INVISIBLE && state != View.GONE) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheStateSpecIsInvalid"));
		}
		int labelId = 0, mainControlId = 0;
        boolean foundControl = false;
        for (MetrixTableDef tableDef : formDef.tables) {
            if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
                for (MetrixColumnDef columnDef : tableDef.columns) {
                    if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
                        mainControlId = columnDef.id;
                        labelId = columnDef.labelId;
                        foundControl = true;
                        break;
                    }
                }
            }
            if(foundControl) break;
        }

 		View control = MetrixControlAssistant.getControl(mainControlId, layout);
		View label = MetrixControlAssistant.getControl(labelId, layout);
        if(control != null && label != null) {
            control.setVisibility(state);
            label.setVisibility(state);
        }
	}
	
	/***
	 * Sets the visibility state of a view.
	 * 
	 * @param id The R.id of the view.
	 * @param layout The current layout.
	 * @param state The view state to apply (View.VISIBLE, View.INVISIBLE, or View.GONE).
	 * 
	 * @since 5.6
	 */
	public static void setVisibility(int id, ViewGroup layout, int state) {
		if (state != View.VISIBLE && state != View.INVISIBLE && state != View.GONE) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheStateSpecIsInvalid"));
		}
		View view = MetrixControlAssistant.getControl(id, layout);
		view.setVisibility(state);
	}
	
	/**
	 * Sets the visibility of a control based on the table column the control is bound to.
	 * 
	 * @param formDef The activity's form definition.
	 * @param layout The activity's layout.
	 * @param tableName The name of the table the control is bound to.
	 * @param columnName The name of the column the control is bound to.
	 * @param state The visibility state to use.
	 * 
	 * @since 5.6.3
	 */
	public static void setVisibility(MetrixFormDef formDef, ViewGroup layout, String tableName, String columnName, int state) {
        if (formDef == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
        }

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

        if (tableName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
        }

        if (columnName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
        }
		
		if (state != View.VISIBLE && state != View.INVISIBLE && state != View.GONE) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheStateSpecIsInvalid"));
		}
		
		View view = MetrixControlAssistant.getControl(formDef, layout, tableName, columnName);
		view.setVisibility(state);
	}

	/**
	 * Wrapper class to handle Button and FloatingActionButton visibility to help avoid changes needed in Mobile
	 * @param btn
	 * @param visibility
	 */
	public static void setButtonVisibility(View btn, int visibility) {
		if (btn instanceof Button)
			setButtonVisibility((Button)btn, visibility);
		else if (btn instanceof FloatingActionButton)
			setFABVisibility((FloatingActionButton)btn, visibility);
	}

	/**
	 * Sets the visibility of a button, with a built-in null check.
	 * @param btn The button to change.
	 * @param visibility The visibility state (e.g., View.VISIBLE or View.GONE)
	 */
	private static void setButtonVisibility(Button btn, int visibility) {
		btn.setVisibility(visibility);
	}

	/**
	 * Sets the visibility of a button, with a built-in null check.
	 * @param btn The button to change.
	 * @param visibility The visibility state (e.g., View.VISIBLE or View.GONE)
	 */
	private static void setFABVisibility(FloatingActionButton btn, int visibility) {
		if (visibility == View.VISIBLE) {
			btn.show();
		}
		if (visibility == View.GONE) {
			btn.hide();
		}
	}

	/**
	 * Sets the enabled state of a control based on the table column the control is bound to.
	 * 
	 * @param formDef The activity's form definition.
	 * @param layout The activity's layout.
	 * @param tableName The name of the table the control is bound to.
	 * @param columnName The name of the column the control is bound to.
	 * @param state The enabled state to use.
	 * 
	 * @since 5.6.3
	 */
	public static void setEnabled(MetrixFormDef formDef, ViewGroup layout, String tableName, String columnName, boolean state) {
        if (formDef == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
        }

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

        if (tableName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
        }

        if (columnName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
        }
		
		View view = MetrixControlAssistant.getControl(formDef, layout, tableName, columnName);
		view.setEnabled(state);
	}
	
	/**
	 * Sets the required state of a control based on the table column the control is bound to.
	 * 
	 * @param formDef The activity's form definition.
	 * @param layout The activity's layout.
	 * @param tableName The name of the table the control is bound to.
	 * @param columnName The name of the column the control is bound to.
	 * @param state The required state to use.
	 * 
	 * @since 5.6.3
	 */
	public static void setRequired(MetrixFormDef formDef, ViewGroup layout, String tableName, String columnName, boolean state) {
        if (formDef == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheFormdefParameterIsRequired"));
        }

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

        if (tableName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
        }

        if (columnName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
        }
		
		MetrixColumnDef columnDef = formDef.getColumnDef(tableName, columnName);
		if (columnDef != null) {
			columnDef.required = state;
		}
		
		View view = MetrixControlAssistant.getControl(formDef, layout, tableName, columnName);
		if (view instanceof EditText) {
			EditText editText = (EditText) view;
			if (state)
				editText.setHint(AndroidResourceHelper.getMessage("Required"));
			else
				editText.setHint("");
		}
	}
	
	/***
	 * Sets the screen focus to the identified view. This'll
	 * set the focusable and focuableInTouchMode attributes
	 * of the view so that it works for views that can't 
	 * normally get focus in touch mode like TextView.
	 * 
	 * @param id The view to get focus.
	 * @param layout The current layout.
	 * 
	 * @since 5.6
	 */
	public static void setFocus(int id, ViewGroup layout) {
		View view = MetrixControlAssistant.getControl(id, layout);
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
	}

	@SuppressWarnings("unchecked")
	public static boolean anyOnStartValuesChanged(MetrixFormDef formDef, ViewGroup layout) {
		if (formDef != null && formDef.tables.size() > 0) {	
			Hashtable<Integer, String> onStartValues = (Hashtable<Integer, String>) MetrixPublicCache.instance.getItem("MetrixOnStartValues");
			Enumeration<Integer> enumerator = onStartValues.keys();
			while (enumerator.hasMoreElements()) {
				Object key = enumerator.nextElement();
				if (onStartValues.get(key).compareTo(MetrixControlAssistant.getValue((Integer) key, layout)) != 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * Check for non-standard control types
	 * @param controlType
	 * @return true or false based on the control types
     */
	public static boolean isNonStandardControl(String controlType) {
		boolean status = false;
		if (!MetrixStringHelper.isNullOrEmpty(controlType)) {
			if (MetrixStringHelper.valueIsEqual(controlType.toUpperCase(), "BUTTON"))
				status = true;
		}

		return status;
	}

	/***
	 * Bind click events for metadata driven buttons, hyperlinks on standard screens.
	 *
	 * @param activity the given activity
	 * @param view the given view
	 * @param controlEvent the given script based event
	 * @since 5.7
     */
	public static void setUpControlEvent(final Activity activity, View view, final String controlEvent) {
		if (view != null && !MetrixStringHelper.isNullOrEmpty(controlEvent)) {
			if (view instanceof Button) {
				Button button = (Button) view;
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(controlEvent));
					}
				});
			}
			else if (view instanceof MetrixHyperlink) {
				MetrixHyperlink hyperlink = (MetrixHyperlink) view;
				hyperlink.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if(((MetrixHyperlink)v).getLinkText().length() != 0)
							MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(controlEvent));
					}
				});
			}
		}
	}

	/***
	 * Bind click events for metadata driven buttons, hyperlinks on list screens.
	 *
	 * @param activity the given activity
	 * @param view the given view
	 * @param controlEvent the given script based event
	 * @param parent
	 *
	 * @since 5.7
	 */
	public static void setUpControlEvent(final Activity activity, View view, final String controlEvent, final View parent) {
		if (view != null && !MetrixStringHelper.isNullOrEmpty(controlEvent)) {
			if (view instanceof Button) {
				Button button = (Button) view;
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(controlEvent));
					}
				});
			}
			else if (view instanceof MetrixHyperlink) {
				MetrixHyperlink hyperlink = (MetrixHyperlink) view;
				hyperlink.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MetrixPublicCache.instance.addItem("CurrentListRowContainer", parent);
						MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), MetrixClientScriptManager.getScriptDefForScriptID(controlEvent));
						MetrixPublicCache.instance.removeItem("CurrentListRowContainer");
					}
				});
			}
		}
	}

	/***
	 * Gets the control of a specific list row.
	 *
	 * @param container the given list row container
	 * @param tableName name of the table
	 * @param columnName name of the column
     * @return identified control
	 *
	 * @since 5.7
	 */
	public static View GetCurrentListRowControl(View container, String tableName, String columnName) {
		if (container == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheContainerParamIsReq"));
		}

        if (tableName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
        }

        if (columnName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
        }

		String identifier = String.format("%1s__%2s", tableName, columnName);
		return container.findViewWithTag(identifier);
	}

	/***
	 * Gets the value of a control of a specific list row.
	 *
	 * @param container the given list row container
	 * @param tableName name of the table
	 * @param columnName name of the column
	 * @return identified control's value
	 *
	 * @since 5.7
	 */
	public static String GetCurrentListRowControlValue(View container, String tableName, String columnName) throws Exception {
		if (container == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheContainerParamIsReq"));
		}

        if (tableName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
        }

        if (columnName == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheColumnNameParamIsReq"));
        }

		String identifier = String.format("%1s__%2s", tableName, columnName);
		View view = container.findViewWithTag(identifier);
		if(view != null){
			if(view instanceof MetrixHyperlink){
				MetrixHyperlink metrixHyperlink = (MetrixHyperlink) view;
				return metrixHyperlink.getLinkText();
			}
			else if(view instanceof TextView){
				TextView textView = (TextView) view;
				return textView.getText().toString();
			}
		}

		throw new Exception(AndroidResourceHelper.getMessage("TheRecvWidgetTypeIs1Args", view.getClass().getName()));
	}

	//region #Hyperlink related methods

	/**
	 * Converts the text in a TextView to appear as if it were a hyperlink.
	 *
	 * @param id
	 *            The id of the TextView.
	 * @param layout
	 *            The ViewGroup containing the TextView.
	 *
	 *            <pre>
	 * MetrixControlAssistant.giveTextViewHyperlinkApperance(R.id.place__name, mLayout);
	 * </pre>
	 */
	public static void giveTextViewHyperlinkApperance(Integer id, ViewGroup layout) {
		giveTextViewHyperlinkApperance(id, layout, "#4169e1", android.R.color.transparent, true, false);
	}

	/**
	 * Converts the text in a TextView to appear as if it were a hyperlink.
	 *
	 * @param id The id of the TextView.
	 * @param layout The ViewGroup containing the TextView.
	 * @param textColor A HTML color code indicating the text color.
	 * @param backgroundColor A resource id indicating the background color.
	 * @param underlineAllText TRUE if the entire text should be underlined, FALSE otherwise.
	 */
	public static void giveTextViewHyperlinkApperance(Integer id, ViewGroup layout, String textColor, int backgroundColor, boolean underlineAllText) {
		giveTextViewHyperlinkApperance(id, layout, textColor, backgroundColor, underlineAllText, true);
	}

	/**
	 * Converts the text in a TextView to appear as if it were a hyperlink.
	 *
	 * @param id The id of the TextView.
	 * @param layout The ViewGroup containing the TextView.
	 * @param textColor A HTML color code indicating the text color.
	 * @param backgroundColor A resource id indicating the background color.
	 * @param underlineAllText TRUE if the entire text should be underlined, FALSE otherwise.
	 * @param overrideMetadataColor TRUE if metadata for hyperlink color should be ignored.
	 */
	public static void giveTextViewHyperlinkApperance(Integer id, ViewGroup layout, String textColor, int backgroundColor, boolean underlineAllText, boolean overrideMetadataColor) {
		final TextView view = (TextView) MetrixControlAssistant.getControl(id, layout);
		if(MetrixStringHelper.isNullOrEmpty(view.getText().toString())) return;

		final int textBackgroundColor = backgroundColor;

		if (!overrideMetadataColor) {
			String hyperlinkColor = MetrixSkinManager.getHyperlinkColor();
			textColor = MetrixStringHelper.isNullOrEmpty(hyperlinkColor) ? textColor : hyperlinkColor;
		}

		if (view != null) {
			view.setTextColor(Color.parseColor(textColor));
			SpannableString content = new SpannableString(view.getText());
			if (underlineAllText) {
				content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
			}
			view.setText(content);

			if (view instanceof EditText) {
				view.setEnabled(true);
				view.setLinksClickable(true);
				view.setLongClickable(false);
				view.setFocusable(false);
				view.setFocusableInTouchMode(false);
				view.setMovementMethod(LinkMovementMethod.getInstance());
			}

			view.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
						view.setBackgroundColor(0xffced9ed);
					}
					if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE
							|| event.getAction() == MotionEvent.ACTION_CANCEL) {
						view.setBackgroundColor(view.getContext().getResources().getColor(textBackgroundColor));
					}
					return false;
				}
			});
		}
	}

	/**
	 * Converts the text in a TextView to appear as if it were a hyperlink.
	 *
	 * @param id
	 *            The id of the TextView.
	 * @param layout
	 *            The ViewGroup containing the TextView.
	 * @param linkType
	 * 			  The hyperlink type to be rendered
	 * <pre>
	 * MetrixControlAssistant.giveTextViewHyperlinkApperance(R.id.place__name, mLayout);
	 * </pre>
	 */
	public static void giveTextViewHyperlinkApperance(Integer id, ViewGroup layout, Global.HyperlinkType linkType) {
		if (linkType == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLinktypeParamIsReq"));
		}

		final TextView view = (TextView) MetrixControlAssistant.getControl(id, layout);
		if(MetrixStringHelper.isNullOrEmpty(view.getText().toString())) return;

		String hyperlinkColor = MetrixSkinManager.getHyperlinkColor();
		hyperlinkColor = MetrixStringHelper.isNullOrEmpty(hyperlinkColor) ? "#4169e1" : hyperlinkColor;
		view.setTextColor(Color.parseColor(hyperlinkColor));
		SpannableString content = new SpannableString(view.getText());
		content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
		view.setText(content);

		if (view instanceof EditText) {
			view.setEnabled(true);
			view.setLinksClickable(true);
			view.setLongClickable(false);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
			view.setMovementMethod(LinkMovementMethod.getInstance());
		}

		if (linkType == Global.HyperlinkType.map){
			final int mapId = id;
			final ViewGroup vGroup = layout;
			view.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					String fullAddress = MetrixControlAssistant.getValue(mapId, vGroup);
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + fullAddress));
					view.getContext().startActivity(intent);
				}
			});
		} else {
			view.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
						view.setBackgroundColor(0xffced9ed);
					}
					if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE
							|| event.getAction() == MotionEvent.ACTION_CANCEL) {
						view.setBackgroundColor(view.getContext().getResources().getColor(android.R.color.white));
					}
					return false;
				}
			});
		}
	}

	/**
	 * Apply a generic auto-link to the control passed in (i.e., will match any possible auto-link in Android).
	 * This feature is only available for TextView controls.
	 *
	 * @param view The TextView on which auto-link will be set.
	 * @param layout The activity's layout.
	 *
	 * @since 5.6.1
	 */
	public static void setGenericAutoLinkOnTextView(TextView view, ViewGroup layout) {
		view.setAutoLinkMask(0);
		view.setEnabled(true);
		String email = view.getText().toString();

		view.setFocusable(false);
		view.setFocusableInTouchMode(false);

		Linkify.addLinks(view, Linkify.ALL);
		String hyperlinkColor = MetrixSkinManager.getHyperlinkColor();
		if (!MetrixStringHelper.isNullOrEmpty(hyperlinkColor)) {
			view.setLinkTextColor(Color.parseColor(hyperlinkColor));
		}
	}

	/**
	 * If the telephony feature is available on this device, the control
	 * received will have the auto-link for phone numbers applied to it.
	 * This feature is only available for TextView controls.
	 *
	 * @param activity The current activity.
	 * @param id The control to potentially set the auto-link on.
	 * @param layout The activity's layout.
	 * @throws Exception Throw if anything other than a TextView is received.
	 *
	 * @since 5.6 Patch 2
	 */
	public static void setPhoneAutoLinkIfTelephonyAvailable(Activity activity, int id, ViewGroup layout) throws Exception {
		View view = MetrixControlAssistant.getControl(id, layout);
		MetrixControlAssistant.setPhoneAutoLinkIfTelephonyAvailable(activity, view);
	}

	/**
	 * If the telephony feature is available on this device, the control
	 * received will have the auto-link for phone numbers applied to it.
	 * This feature is only available for TextView controls.
	 *
	 * @param activity The current activity.
	 * @param view The control to potentially set the auto-link on.
	 * @throws Exception Throw if anything other than a TextView is received.
	 */
	public static void setPhoneAutoLinkIfTelephonyAvailable(Activity activity, View view) throws Exception {
		if (activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
			if (view instanceof TextView) {
				TextView textView = (TextView) view;
				textView.setEnabled(true);

				view.setFocusable(false);
				view.setFocusableInTouchMode(false);

				Linkify.addLinks(textView, Linkify.EMAIL_ADDRESSES);
				String hyperlinkColor = MetrixSkinManager.getHyperlinkColor();
				if (!MetrixStringHelper.isNullOrEmpty(hyperlinkColor)) {
					textView.setLinkTextColor(Color.parseColor(hyperlinkColor));
				}
				//textView.setAutoLinkMask(Linkify.PHONE_NUMBERS);
				return;
			}
			else if(view instanceof EditText) {
				view.setEnabled(true);

				view.setFocusable(false);
				view.setFocusableInTouchMode(false);

				//Linkify.addLinks((TextView)view, Linkify.ALL);
				String hyperlinkColor = MetrixSkinManager.getHyperlinkColor();
				if (!MetrixStringHelper.isNullOrEmpty(hyperlinkColor)) {
					((TextView)view).setLinkTextColor(Color.parseColor(hyperlinkColor));
				}
			}
			if (view != null) {
				throw new Exception(AndroidResourceHelper.getMessage("TheRecvWidgetTypeIs1Args", view.getClass().getName()));
			} else {
				return;
			}
		}
	}

	//endregion
}
