package com.metrix.metrixmobile.global;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * A helper class to add marker on the map with the location and location detail information 
 * @author edlius
 *
 */
public class MetrixMapApiHelper {
	public static Marker addMarker(GoogleMap map, double lat, double lon,
			String title, String snippet, int iconId) {
		// add marker to the map, it will show infoWindow as default
		return addMarker(map, lat, lon, title, snippet, iconId, true);
	}

	public static Marker addMarker(GoogleMap map, double lat, double lon,
			String title, String snippet, int iconId, boolean showInfoWindow) {
		Marker marker = map.addMarker(new MarkerOptions()
				.position(new LatLng(lat, lon))
				.icon(BitmapDescriptorFactory.fromResource(iconId))
				.title(title).snippet(snippet));

		if (showInfoWindow == false)
			marker.hideInfoWindow();

		return marker;
	}

	public static void animateCameraTo(final double lat, final double lng,
			final GoogleMap map) {
		CameraPosition camPosition = map.getCameraPosition();
		if (!((Math.floor(camPosition.target.latitude * 100) / 100) == (Math
				.floor(lat * 100) / 100) && (Math
				.floor(camPosition.target.longitude * 100) / 100) == (Math
				.floor(lng * 100) / 100))) {
			map.getUiSettings().setScrollGesturesEnabled(false);
			map.animateCamera(
					CameraUpdateFactory.newLatLng(new LatLng(lat, lng)),
					new CancelableCallback() {

						@Override
						public void onFinish() {
							map.getUiSettings().setScrollGesturesEnabled(true);

						}

						@Override
						public void onCancel() {
							map.getUiSettings().setAllGesturesEnabled(true);
						}
					});
		}
	}
}
