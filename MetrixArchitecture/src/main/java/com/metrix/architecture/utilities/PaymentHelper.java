package com.metrix.architecture.utilities;

import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixFormDef;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by hesplk on 6/9/2016.
 */
public class PaymentHelper{
    public static Map<String, Double> CalculateTotalExpenses() {
        Map<String, Double> totalExpenses = new HashMap<String, Double>();

        // add to totalExpenses any expenses with a quantity_format NOT 1, using (transaction_amount * bill_price)
        String query = "select transaction_amount, transaction_currency, bill_price from non_part_usage "
                + "left join line_code on non_part_usage.line_code = line_code.line_code "
                + "where line_code.npu_code = 'EXP' and quantity_format != '1' and non_part_usage.task_id = "
                + MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    double transactionAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    String transactionCurrency = cursor.getString(1);
                    double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(2), Locale.US));
                    double extendedPrice = (transactionAmount * billPrice);

                    if (totalExpenses.containsKey(transactionCurrency)) {
                        totalExpenses.put(transactionCurrency, totalExpenses.get(transactionCurrency) + extendedPrice);
                    } else {
                        totalExpenses.put(transactionCurrency, extendedPrice);
                    }

                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // add to totalExpenses any expenses with a quantity_format of 1, using transaction_amount
        query = "select transaction_amount, transaction_currency from non_part_usage "
                + "left join line_code on non_part_usage.line_code = line_code.line_code "
                + "where line_code.npu_code = 'EXP' and quantity_format = '1' and non_part_usage.task_id = "
                + MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

        cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    double transactionAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    String transactionCurrency = cursor.getString(1);

                    if (totalExpenses.containsKey(transactionCurrency)) {
                        totalExpenses.put(transactionCurrency, totalExpenses.get(transactionCurrency) + transactionAmount);
                    } else {
                        totalExpenses.put(transactionCurrency, transactionAmount);
                    }

                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalExpenses;
    }

    public static Map<String, Double> CalculateTotalLabor() {
        Map<String, Double> totalLabor = new HashMap<String, Double>();
        String query = "select bill_price, billing_currency, quantity from non_part_usage left join line_code on non_part_usage.line_code = line_code.line_code where line_code.npu_code = 'TIME' and non_part_usage.task_id = "
                + MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int i = 1;
                while (cursor.isAfterLast() == false) {
                    double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    String transactionCurrency = cursor.getString(1);
                    double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(2), Locale.US));

                    if (totalLabor.containsKey(transactionCurrency)) {
                        totalLabor.put(transactionCurrency, totalLabor.get(transactionCurrency) + (billPrice * quantity));
                    } else {
                        totalLabor.put(transactionCurrency, (billPrice * quantity));
                    }
                    cursor.moveToNext();
                    i = i + 1;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalLabor;
    }

    public static Map<String, Double> CalculateTotalParts() {
        Map<String, Double> totalPart = new HashMap<String, Double>();
        String query = "select bill_price, quantity, billing_currency from part_usage where part_usage.task_id = " + MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    String transactionCurrency = cursor.getString(2);
                    double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    int quantity = cursor.getInt(1);

                    if(totalPart.containsKey(transactionCurrency)) {
                        totalPart.put(transactionCurrency, totalPart.get(transactionCurrency) + billPrice*quantity);
                    }
                    else {
                        totalPart.put(transactionCurrency, billPrice*quantity);
                    }

                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalPart;
    }

    public static Map<String, Double> CalculatePayments() {
        Map<String, Double> totalPayment = new HashMap<String, Double>();
        String query = "select payment_amount, payment_currency from payment where payment_status = 'AUTH' and request_id = '" + MetrixDatabaseManager.getFieldStringValue("task", "request_id", "task_id=" + MetrixCurrentKeysHelper.getKeyValue("task", "task_id")) + "'";

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    String transactionCurrency = cursor.getString(1);
                    double paymentAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));

                    if(totalPayment.containsKey(transactionCurrency)) {
                        totalPayment.put(transactionCurrency, totalPayment.get(transactionCurrency) + paymentAmount);
                    }
                    else {
                        totalPayment.put(transactionCurrency, paymentAmount);
                    }
                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalPayment;
    }

    public static Map<String, Double> CalculateTotalQuoteExpenses() {
        Map<String, Double> totalExpenses = new HashMap<String, Double>();

        // add to totalExpenses any expenses with a quantity_format NOT 1, using (transaction_amount * bill_price)
        String query = "select transaction_amount, transaction_currency, bill_price from quote_non_part_usage "
                + "left join line_code on quote_non_part_usage.line_code = line_code.line_code "
                + "where line_code.npu_code = 'EXP' and quantity_format != '1' and quote_non_part_usage.quote_id = "
                + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id")
                + " and quote_non_part_usage.quote_version = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    double transactionAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    String transactionCurrency = cursor.getString(1);
                    double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(2), Locale.US));
                    double extendedPrice = (transactionAmount * billPrice);

                    if (totalExpenses.containsKey(transactionCurrency)) {
                        totalExpenses.put(transactionCurrency, totalExpenses.get(transactionCurrency) + extendedPrice);
                    } else {
                        totalExpenses.put(transactionCurrency, extendedPrice);
                    }

                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // add to totalExpenses any expenses with a quantity_format of 1, using transaction_amount
        query = "select transaction_amount, transaction_currency from quote_non_part_usage "
                + "left join line_code on quote_non_part_usage.line_code = line_code.line_code "
                + "where line_code.npu_code = 'EXP' and quantity_format = '1' and quote_non_part_usage.quote_id = "
                + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id")
                + " and quote_non_part_usage.quote_version = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");

        cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    double transactionAmount = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    String transactionCurrency = cursor.getString(1);

                    if (totalExpenses.containsKey(transactionCurrency)) {
                        totalExpenses.put(transactionCurrency, totalExpenses.get(transactionCurrency) + transactionAmount);
                    } else {
                        totalExpenses.put(transactionCurrency, transactionAmount);
                    }

                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalExpenses;
    }

    public static Map<String, Double> CalculateTotalQuoteLabor() {
        Map<String, Double> totalLabor = new HashMap<String, Double>();
        String query = "select bill_price, billing_currency, quantity from quote_non_part_usage left join line_code on quote_non_part_usage.line_code = line_code.line_code where line_code.npu_code = 'TIME' and quote_non_part_usage.quote_id = "
                + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id")
                + " and quote_non_part_usage.quote_version = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int i = 1;
                while (cursor.isAfterLast() == false) {
                    double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    String transactionCurrency = cursor.getString(1);
                    double quantity = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(2), Locale.US));

                    if (totalLabor.containsKey(transactionCurrency)) {
                        totalLabor.put(transactionCurrency, totalLabor.get(transactionCurrency) + (billPrice * quantity));
                    } else {
                        totalLabor.put(transactionCurrency, (billPrice * quantity));
                    }
                    cursor.moveToNext();
                    i = i + 1;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalLabor;
    }

    public static Map<String, Double> CalculateTotalQuoteParts() {
        Map<String, Double> totalPart = new HashMap<String, Double>();
        String query = "select bill_price, quantity, billing_currency from quote_part_usage where quote_part_usage.quote_id = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_id")
                + " and quote_part_usage.quote_version = " + MetrixCurrentKeysHelper.getKeyValue("quote", "quote_version");;

        MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    String transactionCurrency = cursor.getString(2);
                    double billPrice = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(0), Locale.US));
                    int quantity = cursor.getInt(1);

                    if(totalPart.containsKey(transactionCurrency)) {
                        totalPart.put(transactionCurrency, totalPart.get(transactionCurrency) + billPrice*quantity);
                    }
                    else {
                        totalPart.put(transactionCurrency, billPrice*quantity);
                    }

                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return totalPart;
    }

    public static String getCurrency(Map<String, Double> money) {
        String currency = "";

        if(money == null || money.size()<1)
            return "";
        else {
            for (Map.Entry<String, Double> entry : money.entrySet()) {
                currency = entry.getKey();
                break;
            }
        }

        return currency;
    }

    public static boolean isSameCurrency(String a, String b, String c, String d) {
        ArrayList<String> currencies = new ArrayList<String>();

        currencies.add(a);
        currencies.add(b);
        currencies.add(c);
        currencies.add(d);

        return isSameCurrency(currencies);
    }

    public static boolean isSameCurrency(String a, String b, String c) {
        ArrayList<String> currencies = new ArrayList<String>();

        currencies.add(a);
        currencies.add(b);
        currencies.add(c);

        return isSameCurrency(currencies);
    }

    public static boolean isSameCurrency(String a, String b) {
        ArrayList<String> currencies = new ArrayList<String>();

        currencies.add(a);
        currencies.add(b);

        return isSameCurrency(currencies);
    }

    public static boolean isSameCurrency(ArrayList<String> currencies)
    {
        if(currencies == null || currencies.isEmpty())
            return true;

        String firstCurrency = "";
        for(String currency : currencies) {
            if(currency ==  null || currency.isEmpty())
                continue;

            if(firstCurrency.isEmpty()) {
                firstCurrency = currency;
                continue;
            }

            if(!firstCurrency.equalsIgnoreCase(currency))
                return false;
        }
        return true;
    }
}
