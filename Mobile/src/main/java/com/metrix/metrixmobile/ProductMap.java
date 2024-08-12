package com.metrix.metrixmobile;

import android.app.Dialog;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.system.Help;
import com.metrix.metrixmobile.system.MetrixMap;

public class ProductMap extends MetrixMap implements OnMarkerClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar(AndroidResourceHelper.getMessage("ProductMapTitle"));
	}

	@Override
	public void onStart(){
		super.onStart();
		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());

		this.helpText = AndroidResourceHelper.getMessage("ScreenDescriptionProductMap");
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public boolean onMarkerClick(Marker arg0) {
		if(arg0.getSnippet() == null){
			map.moveCamera(CameraUpdateFactory.zoomIn());
			return true;
		}

		setupPopup(arg0);

		return true;
	}

	private Marker addMarker(GoogleMap map, double lat, double lon, String title, String snippet, int iconId, boolean showInfoWindow) {
		Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
				.icon(BitmapDescriptorFactory.fromResource(iconId))
				.title(title)
				.snippet(snippet));

		if (showInfoWindow==false)
			marker.hideInfoWindow();

		return marker;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.action_bar_help:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, Help.class);
				String message = this.helpText;
				intent.putExtra("help_text", message);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			default:
				break;
		}
	}

	@Override
	protected void setupMarkers() {
		MetrixCursor cursor = null;
		try {
			String productID = getIntent().getStringExtra("product_id");

			StringBuilder query = new StringBuilder();
			query.append("select product.geocode_lat, product.geocode_long, model.internal_descriptn, product.serial_id");
			query.append(" from product left outer join model on product.model_id = model.model_id ");
			query.append(" where product.product_id = '" + productID + "'");

			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			builder = new LatLngBounds.Builder();

			double minLat = Double.MAX_VALUE;
			double maxLat = Double.MIN_VALUE;
			double minLon = Double.MAX_VALUE;
			double maxLon = Double.MIN_VALUE;
			double dblGeoLat = 0;
			double dblGeoLong = 0;
			String key = "";
			String itemInfo = "";

			int k = 0;
			while (cursor.isAfterLast() == false) {
				if (cursor.getString(0) != null && cursor.getString(1) != null) {
					key = AndroidResourceHelper.getMessage("Product1Arg", productID);

					if (MetrixFloatHelper.getServerDecimalSeparator().compareTo(".") != 0) {
						dblGeoLat = MetrixFloatHelper.convertNumericFromDBToNumber(cursor.getString(0)).doubleValue();
						dblGeoLong = MetrixFloatHelper.convertNumericFromDBToNumber(cursor.getString(1)).doubleValue();
					} else {
						dblGeoLat = cursor.getDouble(0);
						dblGeoLong = cursor.getDouble(1);
					}

					String modelDesc = cursor.getString(2);
					String serialID = cursor.getString(3);
					if (k == 0) {
						maxLat = dblGeoLat;
						minLat = dblGeoLat;
						maxLon = dblGeoLong;
						minLon = dblGeoLong;
					} else {
						maxLat = Math.max(dblGeoLat, maxLat);
						minLat = Math.min(dblGeoLat, minLat);
						maxLon = Math.max(dblGeoLong, maxLon);
						minLon = Math.min(dblGeoLong, minLon);
					}

					itemInfo = modelDesc + "\r\n" + serialID + "\r\n";
					builder.include(addMarker(map, dblGeoLat, dblGeoLong, key, itemInfo, R.drawable.map_pin_red_32_20, false).getPosition());
					k++;
				}
				cursor.moveToNext();
			}

			// now, add a marker for the current location, if possible
			Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);
			if (currentLocation != null) {
				key = AndroidResourceHelper.getMessage("CurrentLocation");
				dblGeoLat = currentLocation.getLatitude();
				dblGeoLong = currentLocation.getLongitude();

				if (k == 0) {
					maxLat = dblGeoLat;
					minLat = dblGeoLat;
					maxLon = dblGeoLong;
					minLon = dblGeoLong;
				} else {
					maxLat = Math.max(dblGeoLat, maxLat);
					minLat = Math.min(dblGeoLat, minLat);
					maxLon = Math.max(dblGeoLong, maxLon);
					minLon = Math.min(dblGeoLong, minLon);
				}

				itemInfo = "";
				builder.include(addMarker(map, dblGeoLat, dblGeoLong, key, itemInfo, R.drawable.map_pin_blue_32_20, false).getPosition());
				k++;
			}

			if (k != 0) {
				center = CameraUpdateFactory.newLatLng(new LatLng((maxLat+minLat)/2,	(maxLon+minLon)/2));
			} else {
				if (currentLocation!=null)
					center = CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
				else
					center = CameraUpdateFactory.newLatLng(new LatLng((maxLat+minLat)/2,	(maxLon+minLon)/2));
			}

			int padding = 128; // offset from edges of the map in pixels
			if (k <= 1) {    // 0 or 1 marker included
				boundZoom = CameraUpdateFactory.zoomTo(10);
			} else {
				LatLngBounds bounds = builder.build();
				boundZoom = CameraUpdateFactory.newLatLngBounds(bounds, padding);
			}
		} catch (SQLException ex) {
			LogManager.getInstance(this).error(ex);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	@Override
	protected void setupPopup(Marker marker) {
		popDialog = new Dialog(ProductMap.this);
		popDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		String titleString = marker.getTitle();
		popDialog.setTitle(titleString);
		popDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
		popDialog.setContentView(R.layout.map_popup);

		streetView = (ImageView)popDialog.findViewById(R.id.street_view);
		streetView.setVisibility(View.GONE);

		final LatLng lPosition = marker.getPosition();
		double lat = lPosition.latitude;
		double lng = lPosition.longitude;

		String info = marker.getSnippet();
		String[] infoItems = info.split("\r\n");

		TextView headerTitle = (TextView) popDialog.findViewById(R.id.headerTitle);
		MetrixSkinManager.setFirstGradientBackground(headerTitle, 0);
		String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
		headerTitle.setText(titleString);
		if (!MetrixStringHelper.isNullOrEmpty(firstGradientText)) {
			headerTitle.setTextColor(Color.parseColor(firstGradientText));
		}

		TextView title = (TextView)popDialog.findViewById(R.id.balloon_item_title);
		if (infoItems.length > 0 && !MetrixStringHelper.isNullOrEmpty(infoItems[0])) {
			title.setText(infoItems[0]);
		} else {
			title.setVisibility(View.GONE);
		}

		MetrixHyperlink mh1 = (MetrixHyperlink)popDialog.findViewById(R.id.balloon_item1);
		if (infoItems.length > 1 && !MetrixStringHelper.isNullOrEmpty(infoItems[1])) {
			mh1.setText(infoItems[1]);
		} else {
			mh1.setVisibility(View.GONE);
		}

		MetrixHyperlink mh2 = (MetrixHyperlink)popDialog.findViewById(R.id.balloon_item2);
		mh2.setVisibility(View.GONE);

		MetrixHyperlink mh3 = (MetrixHyperlink)popDialog.findViewById(R.id.balloon_item3);
		mh3.setVisibility(View.GONE);

		MetrixHyperlink mh4 = (MetrixHyperlink)popDialog.findViewById(R.id.balloon_item4);
		mh4.setVisibility(View.GONE);

		MetrixHyperlink mh5 = (MetrixHyperlink)popDialog.findViewById(R.id.balloon_item5);
		mh5.setVisibility(View.GONE);

		center = CameraUpdateFactory.newLatLng(new LatLng(lat, lng));
		map.animateCamera(boundZoom);
		map.moveCamera(center);

		Window window = popDialog.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();

		wlp.verticalMargin = (float).5;
		wlp.gravity = Gravity.BOTTOM;
		window.setAttributes(wlp);
		popDialog.show();

	}
}

