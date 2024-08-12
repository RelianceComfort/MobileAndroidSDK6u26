package com.metrix.architecture.utilities;

import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;

public class MetrixFloatHelper {
	public static Float round(Float value, int numberOfDecimals) {
		float power = (float) Math.pow(10, numberOfDecimals);
		value = value * power;
		float temporaryValue = Math.round(value);
		return (float) temporaryValue / power;
	}

	public static Double round(Double value, int numberOfDecimals) {
		Double power = Math.pow(10, numberOfDecimals);
		value = value * power;
		long temporaryValue = Math.round(value);
		return (temporaryValue / power);
	}
	
	public static String currentLocaleNumericValue(Object inNumber){
		Locale localLocale = Locale.getDefault();
		String st = "";
		
		try {
			NumberFormat sNumericFormat= NumberFormat.getNumberInstance(localLocale);	
			((DecimalFormat)sNumericFormat).setNegativePrefix("-");
			sNumericFormat.setGroupingUsed(false);
			sNumericFormat.setMaximumFractionDigits(8);
			st=sNumericFormat.format(inNumber);
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
		
		return st;
	}
	
	public static String convertNumericFromForcedLocaleToDB(String inValue, Locale localLocale) {
		if(MetrixStringHelper.isNullOrEmpty(inValue))
			return "";	
		
		String language = "en"; // default value is en
		String country = "US"; // default value is US
			
		if(!MetrixStringHelper.isNullOrEmpty(User.getUser().serverLocaleCode) && User.getUser().serverLocaleCode.contains("-"))
		{	
			language = User.getUser().serverLocaleCode.split("-")[0];		
			country = User.getUser().serverLocaleCode.split("-")[1];
		}				
		
		Number inNumber = null;
		java.text.NumberFormat cNumericFormat = NumberFormat.getNumberInstance(localLocale);	
		((DecimalFormat)cNumericFormat).setNegativePrefix("-");
		cNumericFormat.setGroupingUsed(false);
		cNumericFormat.setMaximumFractionDigits(8);
		
		try {
			inNumber  = cNumericFormat.parse(inValue);
		}
		catch(ParseException ex){
			LogManager.getInstance().error(ex);
			return "";
		}
				
		Locale serverLocale = new Locale(language, country);		
		NumberFormat sNumericFormat= NumberFormat.getNumberInstance(serverLocale);	
		((DecimalFormat)sNumericFormat).setNegativePrefix("-");
		sNumericFormat.setGroupingUsed(false);
		sNumericFormat.setMaximumFractionDigits(8);
		return sNumericFormat.format(inNumber);		
	}
	
	public static String convertNumericFromForcedLocaleToUI(String inValue, Locale sourceLocale) {
		if(MetrixStringHelper.isNullOrEmpty(inValue))
			return "";	

		Number inNumber = null;
		NumberFormat sourceNumericFormat = NumberFormat.getNumberInstance(sourceLocale);
		((DecimalFormat)sourceNumericFormat).setNegativePrefix("-");
		sourceNumericFormat.setGroupingUsed(false);
		sourceNumericFormat.setMaximumFractionDigits(8);
		
		try {
			inNumber = sourceNumericFormat.parse(inValue);
		}
		catch(ParseException ex){
			LogManager.getInstance().error(ex);
			return "";
		}				
		
		Locale localLocale = Locale.getDefault();	
		NumberFormat clientNumericFormat = NumberFormat.getNumberInstance(localLocale);
		((DecimalFormat)clientNumericFormat).setNegativePrefix("-");
		clientNumericFormat.setGroupingUsed(false);
		clientNumericFormat.setMaximumFractionDigits(8);
		return clientNumericFormat.format(inNumber);
	}
	
	public static String convertNumericFromDBToForcedLocale(String value, Locale localLocale) {
		if(MetrixStringHelper.isNullOrEmpty(value))
			return "";	
		
		String language = "en"; 
		String country = "US";
			
		if(!MetrixStringHelper.isNullOrEmpty(User.getUser().serverLocaleCode) && User.getUser().serverLocaleCode.contains("-"))
		{	
			language = User.getUser().serverLocaleCode.split("-")[0];		
			country = User.getUser().serverLocaleCode.split("-")[1];
		}				
		
		Number inNumber = null;
		Locale serverLocale = new Locale(language, country);
		NumberFormat serverNumericFormat = NumberFormat.getNumberInstance(serverLocale);
		((DecimalFormat)serverNumericFormat).setNegativePrefix("-");
		serverNumericFormat.setGroupingUsed(false);
		serverNumericFormat.setMaximumFractionDigits(8);
		
		try {
			inNumber = serverNumericFormat.parse(value);
		}
		catch(ParseException ex){
			LogManager.getInstance().error(ex);
			return "";
		}				
		
		NumberFormat clientNumericFormat = NumberFormat.getNumberInstance(localLocale);
		((DecimalFormat)clientNumericFormat).setNegativePrefix("-");
		clientNumericFormat.setGroupingUsed(false);
		clientNumericFormat.setMaximumFractionDigits(8);
		return clientNumericFormat.format(inNumber);		
	}
	
	public static String convertNumericFromUIToDB(String inValue){
		if(MetrixStringHelper.isNullOrEmpty(inValue))
			return "";	
		
		String language = "en"; // default value is en
		String country = "US"; // default value is US
		Locale localLocale = Locale.getDefault();
			
		if(!MetrixStringHelper.isNullOrEmpty(User.getUser().serverLocaleCode) && User.getUser().serverLocaleCode.contains("-"))
		{	
			language = User.getUser().serverLocaleCode.split("-")[0];		
			country = User.getUser().serverLocaleCode.split("-")[1];
		}				
		
		Number inNumber = null;
		java.text.NumberFormat cNumericFormat = NumberFormat.getNumberInstance(localLocale);	
		((DecimalFormat)cNumericFormat).setNegativePrefix("-");
		cNumericFormat.setGroupingUsed(false);
		cNumericFormat.setMaximumFractionDigits(8);
		
		try {
			inNumber  = cNumericFormat.parse(inValue);
		}
		catch(ParseException ex){
			LogManager.getInstance().error(ex);
			return "";
		}
				
		Locale serverLocale = new Locale(language, country);
		
		NumberFormat sNumericFormat= NumberFormat.getNumberInstance(serverLocale);	
		((DecimalFormat)sNumericFormat).setNegativePrefix("-");
		sNumericFormat.setGroupingUsed(false);
		sNumericFormat.setMaximumFractionDigits(8);
		String st=sNumericFormat.format(inNumber);
	
		return st;							
	}		
	
	public static String convertNumericFromDBToUI(String value){
		return MetrixFloatHelper.convertNumericFromDBToUI(value, null);
	}	
	
	public static String convertNumericFromDBToUI(String value, Format formatter) {
		if(MetrixStringHelper.isNullOrEmpty(value))
			return "";	
		
		String language = "en"; 
		String country = "US"; 
		Locale localLocale = Locale.getDefault();
			
		if(!MetrixStringHelper.isNullOrEmpty(User.getUser().serverLocaleCode) && User.getUser().serverLocaleCode.contains("-"))
		{	
			language = User.getUser().serverLocaleCode.split("-")[0];		
			country = User.getUser().serverLocaleCode.split("-")[1];
		}				
		
		Number inNumber = null;
		Locale serverLocale = new Locale(language, country);
		NumberFormat serverNumericFormat = NumberFormat.getNumberInstance(serverLocale);
		((DecimalFormat)serverNumericFormat).setNegativePrefix("-");
		serverNumericFormat.setGroupingUsed(false);
		serverNumericFormat.setMaximumFractionDigits(8);
		
		try {
			inNumber = serverNumericFormat.parse(value);
		}
		catch(ParseException ex){
			LogManager.getInstance().error(ex);
			return "";
		}				
		
		if (formatter == null) {
			NumberFormat clientNumericFormat = NumberFormat.getNumberInstance(localLocale);
			((DecimalFormat)clientNumericFormat).setNegativePrefix("-");
			clientNumericFormat.setGroupingUsed(false);
			clientNumericFormat.setMaximumFractionDigits(8);
			return clientNumericFormat.format(inNumber);
		} else {
			return formatter.format(inNumber);
		}
	}
	
	public static Number convertNumericFromUIToNumber(String inValue){
		if(MetrixStringHelper.isNullOrEmpty(inValue))
			return null;	
		
		String language = "en"; // default value is en
		String country = "US"; // default value is US
		Locale localLocale = Locale.getDefault();				
		
		Number inNumber = null;
		java.text.NumberFormat cNumericFormat = NumberFormat.getNumberInstance(localLocale);	
		((DecimalFormat)cNumericFormat).setNegativePrefix("-");
		cNumericFormat.setGroupingUsed(false);
		cNumericFormat.setMaximumFractionDigits(8);
		int timeTried = 1;
		
		try {
			inNumber  = cNumericFormat.parse(inValue);
		}
		catch(ParseException ex){
			if(timeTried == 1){
				timeTried ++;
				
				Locale locale = new Locale(language, country);
				
				cNumericFormat = NumberFormat.getNumberInstance(locale);	
				((DecimalFormat)cNumericFormat).setNegativePrefix("-");
				cNumericFormat.setGroupingUsed(false);
				cNumericFormat.setMaximumFractionDigits(8);
				
				try {
					inNumber = cNumericFormat.parse(inValue);
				}
				catch(Exception e){
					LogManager.getInstance().error(ex);
					return null;
				}				
			}
			else {
				LogManager.getInstance().error(ex);
				return null;
			}
		}				
		
		return inNumber;							
	}	
	
	/**
	 * @param value
	 * @return
	 */
	public static Number convertNumericFromDBToNumber(String dbValue) {
		if(MetrixStringHelper.isNullOrEmpty(dbValue))
			return null;	
		
		String language = "en"; 
		String country = "US"; 
			
		if(!MetrixStringHelper.isNullOrEmpty(User.getUser().serverLocaleCode) && User.getUser().serverLocaleCode.contains("-"))
		{	
			language = User.getUser().serverLocaleCode.split("-")[0];		
			country = User.getUser().serverLocaleCode.split("-")[1];
		}				
		
		Number inNumber = null;
		Locale serverLocale = new Locale(language, country);
		NumberFormat serverNumericFormat = NumberFormat.getNumberInstance(serverLocale);
		((DecimalFormat)serverNumericFormat).setNegativePrefix("-");
		serverNumericFormat.setGroupingUsed(false);
		serverNumericFormat.setMaximumFractionDigits(8);
		
		try {
			inNumber = serverNumericFormat.parse(dbValue);
		}
		catch(ParseException ex){
			LogManager.getInstance().error(ex);
			return null;
		}				
		
		return inNumber;
	}	
	
	public static String getDecimalSeparator() {
		Locale localLocale = Locale.getDefault();	
		
		NumberFormat numericFormat = NumberFormat.getNumberInstance(localLocale);
		DecimalFormat format= (DecimalFormat)numericFormat; 
		DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
		char sep=symbols.getDecimalSeparator();
		
		return Character.toString(sep);
	}
	
	public static String getServerDecimalSeparator() {
		String language = "en"; 
		String country = "US"; 
			
		if(!MetrixStringHelper.isNullOrEmpty(User.getUser().serverLocaleCode) && User.getUser().serverLocaleCode.contains("-"))
		{	
			language = User.getUser().serverLocaleCode.split("-")[0];		
			country = User.getUser().serverLocaleCode.split("-")[1];
		}				
		
		Locale serverLocale = new Locale(language, country);	
		
		NumberFormat numericFormat = NumberFormat.getNumberInstance(serverLocale);
		DecimalFormat format= (DecimalFormat)numericFormat; 
		DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
		char sep=symbols.getDecimalSeparator();
		
		return Character.toString(sep);
	}
	
	public static DecimalFormat getCurrencyFormatter() {
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
		
		if(Locale.getDefault().getLanguage().compareToIgnoreCase("sv")==0) {
			String country = Locale.getDefault().getCountry();
			DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("sv", country));
			symbols.setDecimalSeparator(',');
		    symbols.setGroupingSeparator(' ');			
		    symbols.setMonetaryDecimalSeparator(',');
		    formatter.setDecimalFormatSymbols(symbols);
		}
	    
	    return formatter;
	}
}