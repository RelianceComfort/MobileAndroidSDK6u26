package com.metrix.architecture.utilities;

import com.metrix.architecture.assistants.MetrixControlAssistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;

public class MetrixMapsHelper {
	/**
	 * Launches Google Maps on the device using the Intent.ACTION_VIEW. Will get
	 * the value of the address and display name from the views identified by
	 * their associated id from the received layout. It will then display the
	 * map with a pin pointing at the address.
	 * 
	 * @param activity
	 *            the activity to be setup.
	 * @param layout
	 *            the activities related layout.
	 * @param addressId
	 *            the R.id of the view containing the address value.
	 * @param displayNameId
	 *            the R.id of the view containing the name to display on the
	 *            pin.
	 */
	public static void displayMapWithPin(Activity activity, ViewGroup layout, int addressId, int displayNameId) {
		String address = MetrixControlAssistant.getValue(addressId, layout);
		String displayName = MetrixControlAssistant.getValue(displayNameId, layout);

		MetrixMapsHelper.displayMapWithPin(activity, address, "", "", displayName);
	}

	/**
	 * Launches Google Maps on the device using the Intent.ACTION_VIEW. Will get
	 * the value of the address, latitude, longitude and display name from the
	 * views identified by their associated id from the received layout. It will
	 * then display the map with a pin pointing at either the latitude/longitude
	 * pair (if set) or from the address.
	 * 
	 * @param activity
	 *            the activity to be setup.
	 * @param layout
	 *            the activities related layout.
	 * @param addressId
	 *            the R.id of the view containing the address value.
	 * @param latitudeId
	 *            the R.id of the view containing the latitude value.
	 * @param longitudeId
	 *            the R.id of the view containing the longitude value.
	 * @param displayNameId
	 *            the R.id of the view containing the name to display on the
	 *            pin.
	 */
	public static void displayMapWithPin(Activity activity, ViewGroup layout, int addressId, int latitudeId, int longitudeId, int displayNameId) {
		String address = MetrixControlAssistant.getValue(addressId, layout);
		String latitude = MetrixControlAssistant.getValue(latitudeId, layout);
		String longitude = MetrixControlAssistant.getValue(longitudeId, layout);
		String displayName = MetrixControlAssistant.getValue(displayNameId, layout);

		MetrixMapsHelper.displayMapWithPin(activity, address, latitude, longitude, displayName);
	}

	/**
	 * Launches Google Maps on the device using the Intent.ACTION_VIEW. It will
	 * then display the map with a pin pointing at either the latitude/longitude
	 * pair (if set) or from the address.
	 * 
	 * @param activity
	 *            the activity to be setup.
	 * @param layout
	 *            the activities related layout.
	 * @param address
	 *            the street address.
	 * @param latitude
	 *            the latitude (optional).
	 * @param longitude
	 *            the longitude (optional).
	 * @param displayName
	 *            the name to display on the pin.
	 */
	public static void displayMapWithPin(Activity activity, String address, String latitude, String longitude, String displayName) {
		if ((!MetrixStringHelper.isNullOrEmpty(latitude)) && (!MetrixStringHelper.isNullOrEmpty(longitude))) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(" + displayName + ")"));
			activity.startActivity(intent);
		} else if (!MetrixStringHelper.isNullOrEmpty(address)) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0" + "?q=" + address + "(" + displayName + ")"));
			activity.startActivity(intent);
		}
	}
}
