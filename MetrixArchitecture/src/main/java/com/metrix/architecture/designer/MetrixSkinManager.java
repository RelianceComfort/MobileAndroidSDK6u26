package com.metrix.architecture.designer;

import java.util.HashMap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;

import androidx.appcompat.app.ActionBar;
import android.view.View;
import android.widget.TextView;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * This class contains methods which can be used to apply the meta data defined in the
 * mobile designer related to skin properties.
 * 
 * @since 5.6.2
 */
@SuppressWarnings("deprecation")
public class MetrixSkinManager extends MetrixDesignerManager {

	//region #Constants
	public static final String DEFAULT_PRIMARY_COLOR = "#360065";
	public static final String DEFAULT_SECONDARY_COLOR = "#8427E2";
	public static final String DEFAULT_HYPERLINK_COLOR = "#6495ED";
	//endregion

	private static HashMap<String, String> skinItems = new HashMap<String, String>();
	private static boolean hasItems = true;

	/***
	 * Gets a Map of all of the items to be included in the list of skin-based metadata.
	 * 
	 * @return the skin items.
	 *  
	 * @since 5.6.2
	 */
	public static HashMap<String, String> getSkinItems() {
		if (skinItems == null || skinItems.size() == 0) {
			if (hasItems && MetrixSkinManager.cacheSkinItems()) {
				return skinItems;
			} else {
				return null;
			}
		} else {
			return skinItems;
		}
	}
	
	public static void clearSkinItemsCache() {
		skinItems.clear();
		hasItems = true;
	}
	
	public static boolean cacheSkinItems() {
		skinItems.clear();
		
		String query = "SELECT use_mm_skin.primary_color, use_mm_skin.secondary_color, use_mm_skin.hyperlink_color,"
				+ " use_mm_skin.first_gradient1, use_mm_skin.first_gradient2, use_mm_skin.first_gradient_text,"
				+ " use_mm_skin.second_gradient1, use_mm_skin.second_gradient2, use_mm_skin.second_gradient_text,"
				+ " use_mm_skin.icon_small_image_id, use_mm_skin.icon_large_image_id, use_mm_skin.login_image_id_portrait, use_mm_skin.login_image_id_landscape from use_mm_skin";

		MetrixCursor cursor = null;
		
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
			if (cursor == null || !cursor.moveToFirst()) {
				hasItems = false;
				return false;
			}

			while (cursor.isAfterLast() == false) {
				skinItems.put("primary_color", cursor.getString(0));
				skinItems.put("secondary_color", cursor.getString(1));
				skinItems.put("hyperlink_color", cursor.getString(2));
				skinItems.put("first_gradient1", cursor.getString(3));
				skinItems.put("first_gradient2", cursor.getString(4));
				skinItems.put("first_gradient_text", cursor.getString(5));
				skinItems.put("second_gradient1", cursor.getString(6));
				skinItems.put("second_gradient2", cursor.getString(7));
				skinItems.put("second_gradient_text", cursor.getString(8));
				skinItems.put("icon_small_image_id", cursor.getString(9));
				skinItems.put("icon_large_image_id", cursor.getString(10));
				skinItems.put("login_image_id_portrait", cursor.getString(11));
				skinItems.put("login_image_id_landscape", cursor.getString(12));
				hasItems = true;
				break;
			}
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		return true;		
	}

	/***
	 * Gets a string for the primary color defined by the assigned skin.
	 * 
	 * @return the primary color as a hex string in usable format ("#000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getPrimaryColor() {
		String primaryColor = getColorForColumn("primary_color", true);
		if (MetrixStringHelper.isNullOrEmpty(primaryColor))
			return DEFAULT_PRIMARY_COLOR;
		return primaryColor;
	}
	
	/***
	 * Gets a string for the secondary color defined by the assigned skin.
	 * 
	 * @return the secondary color as a hex string in usable format ("#000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getSecondaryColor() {
		String secondaryColor = getColorForColumn("secondary_color", true);
		if (MetrixStringHelper.isNullOrEmpty(secondaryColor))
			return DEFAULT_SECONDARY_COLOR;
		return secondaryColor;
	}

	/***
	 * Gets a string for the hyperlink color defined by the assigned skin.
	 * 
	 * @return the hyperlink color as a hex string in usable format ("#000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getHyperlinkColor() {
		String hyperLinkColor = getColorForColumn("hyperlink_color", true);
		if (MetrixStringHelper.isNullOrEmpty(hyperLinkColor))
		    return DEFAULT_HYPERLINK_COLOR;
		return hyperLinkColor;
	}
	
	/***
	 * Gets a string for the top first gradient color defined by the assigned skin.
	 * 
	 * @return the top first gradient color as a hex string in usable format ("000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getTopFirstGradientColor() {
		return getColorForColumn("first_gradient1", false);			
	}
	
	/***
	 * Gets a string for the bottom first gradient color defined by the assigned skin.
	 * 
	 * @return the bottom first gradient color as a hex string in usable format ("000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getBottomFirstGradientColor() {
		return getColorForColumn("first_gradient2", false);			
	}
	
	/***
	 * Gets a string for the first gradient text color defined by the assigned skin.
	 * 
	 * @return the first gradient text color as a hex string in usable format ("#000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getFirstGradientTextColor() {
		return getColorForColumn("first_gradient_text", true);
	}
	
	/***
	 * Gets a string for the top second gradient color defined by the assigned skin.
	 * 
	 * @return the top second gradient color as a hex string in usable format ("000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getTopSecondGradientColor() {
		return getColorForColumn("second_gradient1", false);			
	}
	
	/***
	 * Gets a string for the bottom second gradient color defined by the assigned skin.
	 * 
	 * @return the bottom second gradient color as a hex string in usable format ("000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getBottomSecondGradientColor() {
		return getColorForColumn("second_gradient2", false);
	}

	/***
	 * Gets a string for the second gradient text color defined by the assigned skin.
	 * 
	 * @return the second gradient text color as a hex string in usable format ("#000000").
	 *  
	 * @since 5.6.2
	 */	
	public static String getSecondGradientTextColor() {		
		return getColorForColumn("second_gradient_text", true);
	}
	
	/***
	 * Gets a string identifying the small icon image, as defined by the assigned skin.
	 * 
	 * @return the image ID of the small icon ("1234").
	 *  
	 * @since 5.6.2
	 */	
	public static String getSmallIconImageID() {
		return getStandardStringForColumn("icon_small_image_id");
	}
	
	/***
	 * Gets a string identifying the large icon image, as defined by the assigned skin.
	 * 
	 * @return the image ID of the large icon ("1234").
	 *  
	 * @since 5.6.2
	 */	
	public static String getLargeIconImageID() {
		return getStandardStringForColumn("icon_large_image_id");
	}
	
	/***
	 * Converts a color to a lighter version of itself.
	 * 
	 * @return the color string in usable format ("[#]000000"), depending on usesHashTag flag.
	 *  
	 * @since 5.6.2
	 */	
	public static String generateLighterVersionOfColor(String colorString, float ratio, boolean usesHashTag) {
		if (MetrixStringHelper.isNullOrEmpty(colorString))
			return (usesHashTag) ? "#FFFFFF" : "FFFFFF";
		
		if (usesHashTag) {
			colorString = colorString.replace("#", "");
		}
		String fullString = lightenColor(colorString, ratio);
		
		return (usesHashTag) ? "#" + fullString : fullString;
	}
	
	/***
	 * Get the color filter to change the image color based on the color parameter 
	 * 
	 * @return the color filter with the color parameter specified.
	 *  
	 * @since 5.6.3
	 */	
	public static ColorFilter getColorFilter(String color){
		int iColor = Color.parseColor(color);

        int red = (iColor & 0xFF0000) / 0xFFFF;
        int green = (iColor & 0xFF00) / 0xFF;
        int blue = iColor & 0xFF;

        float[] matrix = { 0, 0, 0, 0, red
                         , 0, 0, 0, 0, green
                         , 0, 0, 0, 0, blue
                         , 0, 0, 0, 1, 0 };

        ColorFilter colorFilter = new ColorMatrixColorFilter(matrix);

        return colorFilter;
	}
	
	private static String getColorForColumn(String columnName, boolean tackOnHashTag) {
		String colorString = "";
		HashMap<String, String> mapCopy = getSkinItems();
		if (mapCopy != null) {
			colorString = mapCopy.get(columnName);	
		}
		
		if (!MetrixStringHelper.isNullOrEmpty(colorString) && tackOnHashTag)
			colorString = "#" + colorString;
		
		return colorString;		
	}
	
	private static String getStandardStringForColumn(String columnName) {
		String imageIDString = "";
		HashMap<String, String> mapCopy = getSkinItems();
		if (mapCopy != null) {
			imageIDString = mapCopy.get(columnName);	
		}
		
		return imageIDString;			
	}
	
	private static String lightenColor(String colorString, float ratio) {
		int colorInt = (int)Long.parseLong(colorString, 16);
		int red = (colorInt >> 16) & 0xFF;
		int green = (colorInt >> 8) & 0xFF;
		int blue = (colorInt >> 0) & 0xFF;	
		int redDiff = 255 - red;
		int greenDiff = 255 - green;
		int blueDiff = 255 - blue;
		
		ratio = (ratio > 1 || ratio <= 0) ? 0.875f : ratio;
		
		int newRed = (int) (red + (redDiff * ratio));
		int newGreen = (int) (green + (greenDiff * ratio));
		int newBlue = (int) (blue + (blueDiff * ratio));
		
		String newRedString = String.format("%02X", newRed);
		String newGreenString = String.format("%02X", newGreen);
		String newBlueString = String.format("%02X", newBlue);
		
		return newRedString + newGreenString + newBlueString;
	}
	
	/***
	 * Sets the background of a view, using the first gradient skin metadata.
	 *  
	 * @since 5.6.2
	 */	
	public static void setFirstGradientBackground(View v, float cornerRadius) {
		setFirstGradientBackground(v, cornerRadius, false);
	}
	
	/***
	 * Sets the background of a view, using the first gradient skin metadata.
	 *  
	 * @since 5.6.3
	 */	
	public static void setFirstGradientBackground(View v, float cornerRadius, boolean forceBaseline) {
		if (v != null) {
			String firstGradient1String = "";
			String firstGradient2String = "";
			
			HashMap<String, String> mapCopy = getSkinItems();
			if (mapCopy != null && !forceBaseline) {
				firstGradient1String = mapCopy.get("first_gradient1");
				firstGradient2String = mapCopy.get("first_gradient2");
			} else {
				firstGradient1String = "360065";
				firstGradient2String = firstGradient1String;
			}
		
			if (!MetrixStringHelper.isNullOrEmpty(firstGradient1String) || !MetrixStringHelper.isNullOrEmpty(firstGradient2String)) {
				firstGradient1String = MetrixStringHelper.isNullOrEmpty(firstGradient1String) ? "FFFFFF" : firstGradient1String;
				firstGradient2String = MetrixStringHelper.isNullOrEmpty(firstGradient2String) ? "FFFFFF" : firstGradient2String;
				int firstGradient1 = (int)Long.parseLong(firstGradient1String, 16);
				int firstGradient2 = (int)Long.parseLong(firstGradient2String, 16);
				
				int[] colors = new int[2];
				colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
				colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
				GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				firstGradient.setCornerRadius((cornerRadius > 0) ? cornerRadius : 0f);
				v.setBackgroundDrawable(firstGradient);
			}		
		}
	}

	/***
	 * Sets the background and text color of a textview, using the first gradient skin metadata.
	 *  
	 * @since 5.6.2
	 */	
	public static void setFirstGradientColorsForTextView(TextView tv, float cornerRadius) {
		if (tv != null) {
			String firstGradient1String = "";
			String firstGradient2String = "";
			String firstGradientTextString = "";
			
			HashMap<String, String> mapCopy = getSkinItems();
			if (mapCopy != null) {
				firstGradient1String = mapCopy.get("first_gradient1");
				firstGradient2String = mapCopy.get("first_gradient2");
				firstGradientTextString = mapCopy.get("first_gradient_text");
			}
		
			if (!MetrixStringHelper.isNullOrEmpty(firstGradient1String) || !MetrixStringHelper.isNullOrEmpty(firstGradient2String)) {
				firstGradient1String = MetrixStringHelper.isNullOrEmpty(firstGradient1String) ? "FFFFFF" : firstGradient1String;
				firstGradient2String = MetrixStringHelper.isNullOrEmpty(firstGradient2String) ? "FFFFFF" : firstGradient2String;
				int firstGradient1 = (int)Long.parseLong(firstGradient1String, 16);
				int firstGradient2 = (int)Long.parseLong(firstGradient2String, 16);
				
				int[] colors = new int[2];
				colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
				colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
				GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				firstGradient.setCornerRadius((cornerRadius > 0) ? cornerRadius : 0f);
				tv.setBackgroundDrawable(firstGradient);
			}
			
			if (!MetrixStringHelper.isNullOrEmpty(firstGradientTextString)) {
				firstGradientTextString = "#" + firstGradientTextString;
				tv.setTextColor(Color.parseColor(firstGradientTextString));
			}
		}
	}
	
	/***
	 * Sets the background of a view, using the second gradient skin metadata.
	 *  
	 * @since 5.6.2
	 */	
	public static void setSecondGradientBackground(View v, float cornerRadius, int strokeSize) {
		if (v != null) {
			String secondGradient1String = "";
			String secondGradient2String = "";
			
			HashMap<String, String> mapCopy = getSkinItems();
			if (mapCopy != null) {
				secondGradient1String = mapCopy.get("second_gradient1");
				secondGradient2String = mapCopy.get("second_gradient2");
			}
		
			if (!MetrixStringHelper.isNullOrEmpty(secondGradient1String) || !MetrixStringHelper.isNullOrEmpty(secondGradient2String)) {
				secondGradient1String = MetrixStringHelper.isNullOrEmpty(secondGradient1String) ? "FFFFFF" : secondGradient1String;
				secondGradient2String = MetrixStringHelper.isNullOrEmpty(secondGradient2String) ? "FFFFFF" : secondGradient2String;
				int secondGradient1 = (int)Long.parseLong(secondGradient1String, 16);
				int secondGradient2 = (int)Long.parseLong(secondGradient2String, 16);
				
				int[] colors = new int[2];
				colors[0] = Color.rgb((secondGradient1 >> 16) & 0xFF, (secondGradient1 >> 8) & 0xFF, (secondGradient1 >> 0) & 0xFF);
				colors[1] = Color.rgb((secondGradient2 >> 16) & 0xFF, (secondGradient2 >> 8) & 0xFF, (secondGradient2 >> 0) & 0xFF);
				GradientDrawable secondGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				secondGradient.setCornerRadius((cornerRadius > 0) ? cornerRadius : 0f);
				if (strokeSize > 0) {
					secondGradient.setStroke(strokeSize, Color.BLACK);
				}				
				v.setBackgroundDrawable(secondGradient);
			}		
		}
	}
	
	/***
	 * Sets the background of a android.support.ActionBar, using the first gradient skin metadata.
	 *  
	 * @since 5.6.2
	 */	
	public static void setFirstGradientBackground(ActionBar supportActionBar, float cornerRadius) {
		setFirstGradientBackground(supportActionBar, cornerRadius, false);
	}
	
	/***
	 * Sets the background of a android.support.ActionBar, using the first gradient skin metadata.
	 *  
	 * @since 5.6.3
	 */	
	public static void setFirstGradientBackground(ActionBar supportActionBar, float cornerRadius, boolean forceBaseline) {
		if (supportActionBar != null) {
			String firstGradient1String = "";
			String firstGradient2String = "";
			
			HashMap<String, String> mapCopy = getSkinItems();
			if (mapCopy != null && !forceBaseline) {
				firstGradient1String = mapCopy.get("first_gradient1");
				firstGradient2String = mapCopy.get("first_gradient2");
			} else {
				firstGradient1String = "360065";
				firstGradient2String = firstGradient1String;
			}
		
			if (!MetrixStringHelper.isNullOrEmpty(firstGradient1String) || !MetrixStringHelper.isNullOrEmpty(firstGradient2String)) {
				firstGradient1String = MetrixStringHelper.isNullOrEmpty(firstGradient1String) ? "FFFFFF" : firstGradient1String;
				firstGradient2String = MetrixStringHelper.isNullOrEmpty(firstGradient2String) ? "FFFFFF" : firstGradient2String;
				int firstGradient1 = (int)Long.parseLong(firstGradient1String, 16);
				int firstGradient2 = (int)Long.parseLong(firstGradient2String, 16);
				
				int[] colors = new int[2];
				colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
				colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
				GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				firstGradient.setCornerRadius((cornerRadius > 0) ? cornerRadius : 0f);
				supportActionBar.setBackgroundDrawable(firstGradient);
			}		
		}
	}
	
	/***
	 * Sets the background of a android.app.ActionBar, using the first gradient skin metadata.
	 *  
	 * @since 5.6.2
	 */	
	public static void setFirstGradientBackground(android.app.ActionBar actionBar, float cornerRadius) {
		setFirstGradientBackground(actionBar, cornerRadius, false);
	}
	
	public static void setFirstGradientBackground(android.app.ActionBar actionBar, float cornerRadius, boolean forceBaseline) {
		if (actionBar != null) {
			String firstGradient1String = "";
			String firstGradient2String = "";
			
			HashMap<String, String> mapCopy = getSkinItems();
			if (mapCopy != null && !forceBaseline) {
				firstGradient1String = mapCopy.get("first_gradient1");
				firstGradient2String = mapCopy.get("first_gradient2");
			} else {
				firstGradient1String = "360065";
				firstGradient2String = firstGradient1String;
			}
		
			if (!MetrixStringHelper.isNullOrEmpty(firstGradient1String) || !MetrixStringHelper.isNullOrEmpty(firstGradient2String)) {
				firstGradient1String = MetrixStringHelper.isNullOrEmpty(firstGradient1String) ? "FFFFFF" : firstGradient1String;
				firstGradient2String = MetrixStringHelper.isNullOrEmpty(firstGradient2String) ? "FFFFFF" : firstGradient2String;
				int firstGradient1 = (int)Long.parseLong(firstGradient1String, 16);
				int firstGradient2 = (int)Long.parseLong(firstGradient2String, 16);
				
				int[] colors = new int[2];
				colors[0] = Color.rgb((firstGradient1 >> 16) & 0xFF, (firstGradient1 >> 8) & 0xFF, (firstGradient1 >> 0) & 0xFF);
				colors[1] = Color.rgb((firstGradient2 >> 16) & 0xFF, (firstGradient2 >> 8) & 0xFF, (firstGradient2 >> 0) & 0xFF);
				GradientDrawable firstGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				firstGradient.setCornerRadius((cornerRadius > 0) ? cornerRadius : 0f);
				actionBar.setBackgroundDrawable(firstGradient);
			}		
		}
	}

	/***
	 * Gets a string identifying the portrait login image, as defined by the assigned skin.
	 *
	 * @return the image ID of the login image ("1234").
	 *
	 * @since 5.7
	 */
	public static String getLoginImageImageIDPortrait() {
		return getStandardStringForColumn("login_image_id_portrait");
	}

	/***
	 * Gets a string identifying the landscape login image, as defined by the assigned skin.
	 *
	 * @return the image ID of the login image ("1234").
	 *
	 * @since 5.7
	 */
	public static String getLoginImageImageIDLandscape() {
		return getStandardStringForColumn("login_image_id_landscape");
	}
}
