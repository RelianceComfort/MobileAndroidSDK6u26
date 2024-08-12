package com.metrix.architecture.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

//import org.apache.commons.lang3.StringEscapeUtils;

public class MetrixStringHelper {
	@SuppressWarnings("unchecked")
	public static String getArchitectureString(String key) {
		String value = AndroidResourceHelper.getMessage(key);
		return value;
	}
	
	public static boolean isNullOrEmpty(String value) {
		if (value == null || value.length() == 0) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isInteger(String value) {
		return value.matches("^(\\+|-)?[0-9]+$"); // this includes the negative value
	}

	public static boolean isDouble(String value) {
		//return value.matches("^(\\+|-)?[0-9.]+$"); // this includes the negative value
		return value.matches("^(-?)(0|([1-9][0-9]*))(\\.[0-9]+)?$") || value.matches("^(-?)(0|([1-9][0-9]*))(\\,[0-9]+)?$");
	}

	public static boolean isDoubleWithoutLeadingZero(String value) {
		if (value.length() > 1 && (value.startsWith(".") || value.startsWith(",")) && isInteger(value.substring(1)))
			return true;

		return false;
	}

	public static boolean isZeroPrefixNonNumber(String value) {
		if (value.length() >= 2) {
			if (value.startsWith("0") && !(value.startsWith("0.") || value.startsWith("0,")))
				return true;
		}
		return false;
	}

	public static boolean isValidPhoneNumber(String value) {
	    if (TextUtils.isEmpty(value)) {
	        return false;
	    } else {
	        return Patterns.PHONE.matcher(value).matches();
	    }
	}

	public static String removeFirstAndLast(String string) {
		return string.substring(1, string.length() - 1);
	}

	public static boolean isNegativeValue(String value) {
		return value.matches("^-[0-9]+$");
	}

	/**
	 * Test if the pattern is matched by the input string
	 * 
	 * @param expression
	 * @param input
	 * @return
	 */
	public static boolean doRegularExpressionMatch(String expression, String input) {
		try {

			Pattern p = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
			// Create a matcher with an input string
			Matcher m = p.matcher(input);
			boolean result = m.find();

			// with the replacements
			return result;
		} catch (PatternSyntaxException ex) {
			LogManager.getInstance().error("doRegularExpression", "There is an error in regular expression:\n" + ex.getDescription());
			return false;
		}
	}

	/**
	 * Find the match group for the searched pattern
	 * 
	 * @param expression
	 * @param input
	 * @return
	 */
	public static String getRegularExpressionMatch(String expression, String input) {
		try {

			Pattern p = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
			// Create a matcher with an input string
			Matcher m = p.matcher(input);

			boolean result = m.find();

			if (result)
				return m.group();
			else
				return "";
		} catch (PatternSyntaxException ex) {
			LogManager.getInstance().error("doRegularExpression", "There is an error in regular expression:\n" + ex.getDescription());
			return "";
		}
	}

	/**
	 * This method returns the Base64 encoded version of the received string value.
	 * @param inputString The string to convert.
	 * @return The Base64 encoded version of the string.
	 * @since 5.6
	 */
	public static String encodeBase64String(String inputString) {
		byte[] bytes = inputString.getBytes();
		return Base64.encodeToString(bytes, Base64.NO_WRAP);
		//return Base64.encodeToString(bytes, 0);
	}	
	
	/**
	 * This method returns the Base64 encoded version of the received byte array.
	 * @param byteout The byte array to convert.
	 * @return The Base64 encoded version of the string.
	 * @since 5.6
	 */
	public static String encodeBase64(byte[] byteout) {
		return Base64.encodeToString(byteout, 0);
	}
	
	/**
	 * This method returns the Base64 encoded version of the received string value.
	 * @param inputString The string to convert.
	 * @return The Base64 encoded version of the string.
	 * @since 5.6
	 */
	public static String encodeBase64ToUrlSafe(String inputString){
		byte[] bytes = inputString.getBytes();
		
		return encodeBase64ToStringForUrl(bytes);
	}
	
	/**
	 * This method returns the Base64 encoded version of the received byte array.
	 * @param byteout The byte array to convert.
	 * @return The Base64 encoded version of the string.
	 * @since 5.6
	 */
	public static String encodeBase64ToStringForUrl(byte[] byteout) {
		String intermediateCode = Base64.encodeToString(byteout, 0);
		
		return intermediateCode.replace("+", "-").replace("/", "_").replace("=", "~").replace("\n", ""); 
	}	

	/**
	 * This method decoded an encoded Base64 string into a byte array.
	 * @param encoded64 The encoded value.
	 * @return The byte array.
	 * @since 5.6
	 */
	public static byte[] decodeBase64(String encoded64) {
		return Base64.decode(encoded64, 0);
	}
	
	public static String decodeBase64ToString(String encoded64) {
		byte[] bytes = Base64.decode(encoded64, 0);
		return new String(bytes);
	}	

	public static String encodeBase64FromImageFile(String fileName) {
		Bitmap bm = BitmapFactory.decodeFile(fileName);// ("/path/to/image.jpg");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);

		return encodeBase64(baos.toByteArray());
	}

	public static String encodeBase64FromStream(ByteArrayOutputStream baos) {
		byte[] byteArray = baos.toByteArray();
		return encodeBase64(byteArray);
	}
	
	public static String filterJsonMessage(String originalMessage){
		return originalMessage.replace("u+0027", "'").replace("u+005c", "\\\\").
				replace("u+0022", "\\\"").replace("&gt;",">").
				replace("&lt;","<").replace("&amp;","&");
	}

	public final static byte[] load(String fileName) {
		try {
			FileInputStream fin = new FileInputStream(fileName);
			return load(fin);
		} catch (Exception e) {
			return new byte[0];
		}
	}

	public final static byte[] load(File file) {
		try {
			FileInputStream fin = new FileInputStream(file);
			return load(fin);
		} catch (Exception e) {
			return new byte[0];
		}
	}

	public final static byte[] load(FileInputStream fin) {
		byte readBuf[] = new byte[1024 * 1024];

		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();

			int readCnt = fin.read(readBuf);
			while (0 < readCnt) {
				bout.write(readBuf, 0, readCnt);
				readCnt = fin.read(readBuf);
			}

			fin.close();

			return bout.toByteArray();
		} catch (Exception e) {
			return new byte[0];
		}
	}
	
	public static String getValueFromSqlResults(ArrayList<Hashtable<String, String>> dataRows, String columnName, int rowIndex){
		String value = "";
		
		if(dataRows!=null && dataRows.size()>0){
			Hashtable <String, String> row = dataRows.get(rowIndex);
			
			value = row.get(columnName);
		}
		
		return value;
	}
	
	public static boolean valueIsEqual(String value1, String value2){
		boolean equalValue = true;
		
		try {
			if(isNullOrEmpty(value1) && isNullOrEmpty(value2))
				return true;
			else if(isNullOrEmpty(value1) && !isNullOrEmpty(value2))
				return false;
			else if(isNullOrEmpty(value2) && !isNullOrEmpty(value1))
				return false;
			else if(!isNullOrEmpty(value1) && !isNullOrEmpty(value2)){
				if(value1.equals(value2))
					return true;
				else
					return false;
			}
		} catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
		return equalValue;
	}
	
	/**
	 * This method takes a string value and returns it if the value
	 * is not null. If the value is null, this method returns an 
	 * empty string.
	 * @param input The string to evaluate.
	 * @return Either the string value or an empty string.
	 * @since 5.6
	 */
	public static String getString(String input) {
		String output = "";
		
		if(!isNullOrEmpty(input))
			return input;
		
		return output;
	}

	/**
	 * This method takes a string value and returns it if the value
	 * is not null. If the value is null, this method returns an
	 * empty string.
	 * @param input The string to evaluate.
	 * @return Either the string value or an empty string.
	 * @since 5.6
	 */
	public static String getString(Object input) {
		String output = "";

		if(input == null)
			return output;

		output = input.toString();

		return output;
	}
	
	public static String escapeDataString(String input) {
		String uriResult = "";
		try {
//			URI tempURL = new URI(input);
//			uriResult = tempURL.toURL().toString();
			uriResult = java.net.URLEncoder.encode(input, "UTF-8").replace("+", "%20");
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
		return uriResult;
	}
}
