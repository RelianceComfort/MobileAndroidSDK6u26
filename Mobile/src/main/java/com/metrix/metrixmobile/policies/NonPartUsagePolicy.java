package com.metrix.metrixmobile.policies;

import java.util.Hashtable;

import android.app.Activity;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class NonPartUsagePolicy {
	public static final String QUANTITY_FORMAT = "quantity_format";
	public static final String ALLOW_O_SELL_PRICE = "allow_0_sell_price";
	public static final String USER_INPUT_COST = "user_input_cost";
	public static final String ALLOW_NEGATIVE = "allow_negative";

	public static String validateQuantityFormat(String lineCode, String quantity, Activity activity) {
		Hashtable<String, String> lineCodeRow = MetrixDatabaseManager.getFieldStringValues("line_code", new String[] { QUANTITY_FORMAT }, "line_code = '" + lineCode + "'");

		if (lineCodeRow == null || lineCodeRow.size() == 0 || (!lineCodeRow.containsKey(QUANTITY_FORMAT))) {
			return "";
		}

		String quantityFormat = lineCodeRow.get(QUANTITY_FORMAT);
		Number quantityNumber = MetrixFloatHelper.convertNumericFromUIToNumber(quantity);

		float quantityAsFloat = quantityNumber.floatValue(); // Float.parseFloat(quantity);

		if (quantityFormat.compareToIgnoreCase("1") == 0) {
			if (quantity.compareToIgnoreCase("1") != 0) {
				return AndroidResourceHelper.getMessage("LineCodeQuantityMustBeOne");
			}
		} else if (quantityFormat.compareToIgnoreCase("H") == 0) {
			float roundedValue = MetrixFloatHelper.round(quantityAsFloat, 2);
			if (quantityAsFloat != roundedValue) {
				return AndroidResourceHelper.getMessage("QuantityFormatError", 2);
			}
		} else if (quantityFormat.compareToIgnoreCase("I") == 0) {
			float roundedValue = MetrixFloatHelper.round(quantityAsFloat, 0);
			if (quantityAsFloat != roundedValue) {
				return AndroidResourceHelper.getMessage("QuantityFormatError", 0);
			}
		} else if (quantityFormat.compareToIgnoreCase("T") == 0) {
			float roundedValue = MetrixFloatHelper.round(quantityAsFloat, 1);
			if (quantityAsFloat != roundedValue) {
				return AndroidResourceHelper.getMessage("QuantityFormatError", 1);
			}
		}

		return "";
	}

	public static String validateLineCodeValues(String lineCode, String billPrice, String billCost, String quantity, Activity activity) {
		Hashtable<String, String> lineCodeRow = MetrixDatabaseManager.getFieldStringValues("line_code", new String[] { ALLOW_O_SELL_PRICE, USER_INPUT_COST, ALLOW_NEGATIVE }, "line_code = '" + lineCode + "'");

		if (lineCodeRow == null || lineCodeRow.size() == 0) {
			return "";
		}

		if (!MetrixStringHelper.isNullOrEmpty(lineCodeRow.get(ALLOW_O_SELL_PRICE)) && lineCodeRow.get(ALLOW_O_SELL_PRICE).compareToIgnoreCase("N") == 0) {
			Number billPriceNumber = MetrixFloatHelper.convertNumericFromUIToNumber(billPrice);
			if (MetrixStringHelper.isNullOrEmpty(billPrice) || billPriceNumber.floatValue() == 0) {
				return AndroidResourceHelper.getMessage("PartUsageZeroPriceNotAllowed", lineCode);
			}
		}

		if (!MetrixStringHelper.isNullOrEmpty(lineCodeRow.get(ALLOW_NEGATIVE)) && lineCodeRow.get(ALLOW_NEGATIVE).compareToIgnoreCase("N") == 0) {
			Number quantityNumber = MetrixFloatHelper.convertNumericFromUIToNumber(quantity);
			if (MetrixStringHelper.isNullOrEmpty(quantity) || quantityNumber.floatValue() < 0) {
				return AndroidResourceHelper.getMessage("NonPartUsageNegQty1Arg", lineCode);
			}
		}
		return "";
	}

	public static String validate(MetrixFormDef formDef, Activity activity, ViewGroup layout) {
		String lineCode = MetrixControlAssistant.getValue(formDef, layout, "non_part_usage", "line_code");
		String billCost = MetrixControlAssistant.getValue(formDef, layout, "non_part_usage", "bill_cost");
		String quantity = MetrixControlAssistant.getValue(formDef, layout, "non_part_usage", "quantity");
		String billPrice = MetrixControlAssistant.getValue(formDef, layout, "non_part_usage", "bill_price");

		if (MetrixStringHelper.isNullOrEmpty(quantity)) {
			return AndroidResourceHelper.getMessage("ReqFieldsNotPopAmount");

		}

		String lineCodeErrors = NonPartUsagePolicy.validateLineCodeValues(lineCode, billPrice, billCost, quantity, activity);
		if (!MetrixStringHelper.isNullOrEmpty(lineCodeErrors)) {
			return lineCodeErrors;
		}

		String quantityErrors = NonPartUsagePolicy.validateQuantityFormat(lineCode, quantity, activity);
		if (!MetrixStringHelper.isNullOrEmpty(quantityErrors)) {
			return quantityErrors;
		}

		return "";
	}

	public static void applyLineCodeSettings(MetrixFormDef formDef, Activity activity, ViewGroup layout) {
		EditText transactionQuantity = (EditText) MetrixControlAssistant.getControl(formDef, layout, "non_part_usage", "quantity");
		transactionQuantity.setEnabled(true);

		String lineCode = MetrixControlAssistant.getValue(formDef, layout, "non_part_usage", "line_code");
		String allowNegative = MetrixDatabaseManager.getFieldStringValue("line_code", ALLOW_NEGATIVE, "line_code = '" + lineCode + "'");
		String quantityFormat = MetrixDatabaseManager.getFieldStringValue("line_code", QUANTITY_FORMAT, "line_code = '" + lineCode + "'");

		if (allowNegative.compareToIgnoreCase("Y") == 0) {
			transactionQuantity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		} else {
			transactionQuantity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		}

		if (quantityFormat.compareToIgnoreCase("1") == 0) {
			try {
				MetrixControlAssistant.setValue(transactionQuantity, "1");
				transactionQuantity.setEnabled(false);
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
		} else if (quantityFormat.compareToIgnoreCase("I") == 0) {
			transactionQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
		}
	}

	public static void populateFilteredExpenseLineCodes(MetrixFormDef formDef, Activity activity, ViewGroup layout, boolean filterQuantityTypeOne) {
		String query = "";
		if (filterQuantityTypeOne) {
			query = "select description, line_code from line_code where npu_code = 'EXP' and quantity_format = '1' order by sequence asc";
		} else {
			query = "select description, line_code from line_code where npu_code = 'EXP' and quantity_format != '1' order by sequence asc";
		}

		Spinner lineCode = (Spinner)MetrixControlAssistant.getControl(formDef, layout, "non_part_usage", "line_code");
		if (lineCode != null) {
			MetrixControlAssistant.populateSpinnerFromQuery(activity, lineCode, query);
		}
	}
}
