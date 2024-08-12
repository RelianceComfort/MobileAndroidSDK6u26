package com.metrix.metrixmobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixIntentService.LocalBinder;
import com.metrix.architecture.services.MetrixServiceManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixMapApiHelper;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.system.Help;
import com.metrix.metrixmobile.system.MetrixMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * Display searched parts on the map
 */
public class StockSearchMap extends MetrixMap {
	private static boolean mResponseProcessed = false;
	private boolean mRenderResult = false;
	private boolean mTimeClockRun = false;
	private String mResponseMessage = "";
	private Activity mCurrentActivity = null;
	private Intent mPreviousIntent = null;

	// Service Binding related objects
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	protected boolean mIsBound = false;
	protected IPostMonitor service = null;
	protected boolean mInitializationStarted = false;

	private LocalBinder mLocalBinder;
	private MetrixIntentService mSyncService;
	private static SyncTask mSyncTask;

	public static final String DEMO_PARTS_LOCATION = "{\"perform_get_closest_places_by_part_id_result\":{\"place\":[{\"whos_place\":\"TRUK\",\"place_id\":\"TRUCK05\",\"phone\":\"432-332-4321\",\"geocode_lat\":\"42.9936650000\",\"geocode_long\":\"-88.1274060000\",\"name\":\"TECH05 Inventory\"},{\"whos_place\":\"REPR\",\"place_id\":\"MX CORP\",\"phone\":\"414-555-8560\",\"geocode_lat\":\"42.9144770000\",\"geocode_long\":\"-88.2172170000\",\"name\":\"Metrix Warehouse\",\"email_address\":\"support@metrix.com\"},{\"whos_place\":\"WHSE\",\"place_id\":\"MX MIDWEST\",\"phone\":\"(414)349-3409\",\"geocode_lat\":\"43.0901830000\",\"geocode_long\":\"-87.9834400000\",\"name\":\"Metrix Midwest\"},{\"whos_place\":\"REPR\",\"place_id\":\"MX REPAIR\",\"phone\":\"770-732-2800\",\"geocode_lat\":\"43.2161240000\",\"geocode_long\":\"-88.0929760000\",\"name\":\"Metrix Repair Depot\"}]}}";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mShouldSetupMapOnCreate = false;

		super.onCreate(savedInstanceState);
		setupActionBar(AndroidResourceHelper.getMessage("StockSearchMapTitle"));

		mCurrentActivity = this;

		mUIHelper = new MetrixUIHelper(mCurrentActivity);

		if (MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild")) {
			showMapObject(DEMO_PARTS_LOCATION);
			return;
		}

		// Setup to get INIT notification from Service
		MetrixServiceManager.setup(this);
		bindService();
	}

	public void onStart() {
		super.onStart();

		LogManager.getInstance(this).info("{0} onStart()", mCurrentActivity.getLocalClassName());

		this.mRenderResult = true;
		this.helpText = AndroidResourceHelper.getMessage("ScreenDescriptionSearchPart");

		TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
		if (actionBarTitle != null) {
			User user = User.getUser();
			if (user != null) {
				actionBarTitle.setText(AndroidResourceHelper.getMessage("UserGreeting2Args", user.firstName, user.lastName));
			}
		}

		ImageView actionBarHelp = (ImageView) findViewById(R.id.action_bar_help);
		if (actionBarHelp != null) {
			if (mHandlingErrors) {
				actionBarHelp.setImageDrawable(getResources().getDrawable(R.drawable.transparent_ep));
			} else {
				actionBarHelp.setImageDrawable(getResources().getDrawable(R.drawable.header_help_32x32));
			}
			actionBarHelp.setOnClickListener(this);
		}

		String priorActivityName = getIntent().getStringExtra("PriorActivity");

		try {
			mPreviousIntent = new Intent(this, Class.forName(priorActivityName));
		} catch (Exception ex) {
			LogManager.getInstance(this).error(ex);
		}

		String timeoutParamValue = MetrixDatabaseManager.getCustomOrBaselineAppParam("FIND_PART_TIMEOUT");
		int searchTime = 30000;
		if(!MetrixStringHelper.isNullOrEmpty(timeoutParamValue)){
			try {
				int timeoutInt = Integer.parseInt(timeoutParamValue);
				if(timeoutInt > 0){
					searchTime = timeoutInt * 1000;
				}
			} catch(NumberFormatException ex) {}
		}

		if(MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild")){
			mResponseProcessed = true;
			searchTime = 5000;
		}
		else
			mResponseProcessed = false;

		String filter = getIntent().getStringExtra("Filter");
		if (!MetrixStringHelper.isNullOrEmpty(filter) && mTimeClockRun == false) {
			mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("Searching"));
			Handler handler = new Handler();
			handler.postDelayed(Timer_Tick, searchTime);
			mTimeClockRun = true;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		this.unbindService();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		this.bindService();
	}

	@Override
	protected void onDestroy() {
		unbindService();
		super.onDestroy();
	}

	private void searchEnded() {
		mUIHelper.dismissLoadingDialog();
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			if (!mResponseProcessed) {
				// This method runs in the same thread as the UI.
				searchEnded();
				MetrixUIHelper.showErrorDialogOnGuiThread(mCurrentActivity, mPreviousIntent, AndroidResourceHelper.getMessage("SearchTimedOut"));
			}
			else {
				searchEnded();
			}
		}
	};

	private void showMapObject(String message) {
		mResponseMessage = message;
		setupMap();
	}

	private ArrayList<HashMap<String, String>> processMessage(String message) {
		String metrixPerformResponse = "perform_get_closest_places_by_part_id_result";
		String matchResult = "";
		ArrayList<HashMap<String, String>> tableValues = new ArrayList<HashMap<String, String>>();

		try {
			if (MetrixStringHelper.doRegularExpressionMatch(metrixPerformResponse, message)) {

				String metrixResponse = MetrixStringHelper.getRegularExpressionMatch(metrixPerformResponse, message);

				if (MetrixStringHelper.isNullOrEmpty(metrixResponse) == false) {
					matchResult = metrixResponse;
				}

				JSONObject jResult = new JSONObject(message);
				JSONObject jSelect = jResult.getJSONObject(matchResult);

				JSONArray jTables = jSelect.names();
				//Mo results

				for (int m = 0; m < jTables.length(); m++) {
					String tableName = jTables.getString(m);
					JSONArray jArrays = jSelect.optJSONArray(tableName);

					if (jArrays != null) {
						String jName = jArrays.getString(0);
						if (jName.compareToIgnoreCase("error") == 0) {
							return null;
						} else {
							for (int n = 0; n < jArrays.length(); n++) {
								JSONObject jArrayField = jArrays.optJSONObject(n);
								HashMap<String, String> columns = new HashMap<String, String>();

								if (jArrayField != null) {
									@SuppressWarnings("unchecked")
									Iterator<String> it = jArrayField.keys();
									while (it.hasNext()) {
										String keyName = (String) it.next();
										String keyValue = (String) jArrayField.getString(keyName);
										columns.put(keyName, keyValue);
									}
								}

								tableValues.add(columns);
							}
						}
					}
					else {
						JSONObject jTable = jSelect.optJSONObject(tableName);

						HashMap<String, String> columns = new HashMap<String, String>();

						columns = new HashMap<String, String>();

						if (jTable != null) {
							@SuppressWarnings("unchecked")
							Iterator<String> it = jTable.keys();
							while (it.hasNext()) {
								String keyName = (String) it.next();
								String keyValue = (String) jTable.getString(keyName);
								columns.put(keyName, keyValue);
							}
						}

						tableValues.add(columns);
					}
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return tableValues;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.action_bar_help:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, Help.class);

				String message = this.helpText;
				if (this.mHandlingErrors) {
					message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ErrorMess1Arg", MobileGlobal.mErrorInfo.errorMessage);
				}
				intent.putExtra("help_text", message);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
		}
	}

	protected void bindService() {
		bindService(new Intent(this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	protected void unbindService() {
		if (mIsBound) {
			try {
				if (service != null) {
					service.removeListener(listener);
					unbindService(mConnection);
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				mIsBound = false;
			}
		}
	}

	protected ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);

				mLocalBinder = (MetrixIntentService.LocalBinder) binder;
				mSyncService = mLocalBinder.getService();

				if (mSyncService != null && mSyncService.getSyncManager() != null) {
					// runOnUiThread would not work well, because it will block  the UI.
					mSyncTask = new SyncTask();
					mSyncTask.execute(mSyncService.getSyncManager());
				}

			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					String sync_message = MetrixStringHelper.filterJsonMessage(message);
					if (activityType == ActivityType.Download && mResponseProcessed == false) {
						if (sync_message.contains("perform_get_closest")) {
							if (mRenderResult) {
								showMapObject(sync_message);
							}
							mResponseProcessed = true;
							mUIHelper.dismissLoadingDialog();
						}
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	@Override
	protected void setupMarkers() {
		try {
			ArrayList<HashMap<String, String>> placeList = processMessage(mResponseMessage);
			if (placeList == null || placeList.size() <= 0) {
				if (mPreviousIntent != null)
					MetrixUIHelper.showErrorDialogOnGuiThread(mCurrentActivity, mPreviousIntent, AndroidResourceHelper.getMessage("PartNotFoundInRadius"));

				return;
			}

			int minLat = Integer.MAX_VALUE;
			int maxLat = Integer.MIN_VALUE;
			int minLon = Integer.MAX_VALUE;
			int maxLon = Integer.MIN_VALUE;

			builder = new LatLngBounds.Builder();
			int k = 0;
			for (HashMap<String, String> place : placeList) {
				if (place.get("geocode_lat") != null && place.get("geocode_long") != null) {
					String placeId = MetrixStringHelper.isNullOrEmpty(place.get("place_id")) ? "" : place.get("place_id");
					String placeName = MetrixStringHelper.isNullOrEmpty(place.get("name")) ? "" : place.get("name");
					String phone = MetrixStringHelper.isNullOrEmpty(place.get("phone")) ? "" : place.get("phone");
					String email = MetrixStringHelper.isNullOrEmpty(place.get("email_address")) ? "" : place.get("email_address");
					String qtyAvailable = MetrixStringHelper.isNullOrEmpty(place.get("qty_available")) ? "0" : place.get("qty_available");
					String placeInfo = placeId+" ("+ qtyAvailable +")\r\n"+placeName+"\r\n"+phone + "\r\n" + email + "\r\n";

					double dblGeoLat = Double.parseDouble(MetrixFloatHelper.convertNumericFromDBToForcedLocale(place.get("geocode_lat"), Locale.US));
					double dblGeoLong = Double.parseDouble(MetrixFloatHelper.convertNumericFromDBToForcedLocale(place.get("geocode_long"), Locale.US));

					maxLat = Math.max((int)(dblGeoLat * 1E6), maxLat);
					minLat = Math.min((int)(dblGeoLat * 1E6), minLat);
					maxLon = Math.max((int)(dblGeoLong * 1E6), maxLon);
					minLon = Math.min((int)(dblGeoLong * 1E6), minLon);

					builder.include(MetrixMapApiHelper.addMarker(map, dblGeoLat, dblGeoLong, placeInfo, "", R.drawable.map_pin_red_32_20, false).getPosition());
					k++;
				}
			}

			Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);

			if (!MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild")) {
				if (currentLocation != null) {
					double geoLat = currentLocation.getLatitude();
					double geoLong = currentLocation.getLongitude();

					builder.include(MetrixMapApiHelper.addMarker(map, geoLat, geoLong, AndroidResourceHelper.getMessage("MyLocation"), "", R.drawable.location, false).getPosition());
					k++;
				}
			}

			if (k != 0) {
				center=CameraUpdateFactory.newLatLng(new LatLng((maxLat+minLat)/2,	(maxLon+minLon)/2));
			} else {
				currentLocation = MetrixLocationAssistant.getCurrentLocation(this);
				if (currentLocation != null)
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

			map.moveCamera(center);
			map.animateCamera(boundZoom);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	@Override
	protected void setupPopup(Marker marker) {
		popDialog = new Dialog(StockSearchMap.this);
		popDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		try {
			popDialog.setTitle(AndroidResourceHelper.getMessage("SiteInfoTitle"));
			popDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
			popDialog.setContentView(R.layout.map_popup);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ViewGroup vg = (ViewGroup) popDialog.findViewById(R.id.balloon_inner_layout);

		streetView = (ImageView) popDialog.findViewById(R.id.street_view);
		String generalInfo = marker.getTitle();
		String address = AndroidResourceHelper.getMessage("DrivingDirections");

		String[] infoItem = generalInfo.split("\r\n"); // we know it should include place, phone number and email address
		loadSingleView executeDownload = new loadSingleView();

		final LatLng lPosition = marker.getPosition();
		double lat = lPosition.latitude;
		double lng = lPosition.longitude;

		String mapKey = MetrixApplicationAssistant.getMetaStringValue(this, "com.google.android.maps.v2.API_KEY");
		String urlString = "http://maps.googleapis.com/maps/api/streetview?size=400x100&location=" + lat + "," + lng + "&sensor=false&key=" + mapKey;
		executeDownload.execute(urlString);

		if (infoItem[0].compareToIgnoreCase(AndroidResourceHelper.getMessage("MyLocation")) == 0) {
			TextView headerTitle = (TextView) popDialog.findViewById(R.id.headerTitle);
			MetrixSkinManager.setFirstGradientBackground(headerTitle, 0);
			String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
			headerTitle.setText(AndroidResourceHelper.getMessage("MyLocation"));
			if (!MetrixStringHelper.isNullOrEmpty(firstGradientText)) {
				headerTitle.setTextColor(Color.parseColor(firstGradientText));
			}

			TextView title = (TextView) popDialog.findViewById(R.id.balloon_item_title);
			title.setVisibility(View.GONE);

			MetrixHyperlink mh1 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item1);
			mh1.setVisibility(View.GONE);

			MetrixHyperlink mh2 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item2);
			mh2.setVisibility(View.GONE);

			MetrixHyperlink mh3 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item3);
			mh3.setVisibility(View.GONE);

			MetrixHyperlink mh4 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item4);
			mh4.setVisibility(View.GONE);

			MetrixHyperlink mh5 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item5);
			mh5.setVisibility(View.GONE);
		} else {
			// render the marker which is not my current location
			TextView headerTitle = (TextView) popDialog.findViewById(R.id.headerTitle);
			MetrixSkinManager.setFirstGradientBackground(headerTitle, 0);
			String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
			headerTitle.setText(infoItem[0]);
			if (!MetrixStringHelper.isNullOrEmpty(firstGradientText)) {
				headerTitle.setTextColor(Color.parseColor(firstGradientText));
			}

			TextView title = (TextView) popDialog.findViewById(R.id.balloon_item_title);
			title.setText(infoItem[1]);

			MetrixHyperlink mh1 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item1);

			if (infoItem.length > 2 && MetrixStringHelper.isNullOrEmpty(infoItem[2]) == false) {
				//tv2.setAutoLinkMask(Linkify.PHONE_NUMBERS);
				final String phone = infoItem[2];
				mh1.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
							String uri = "tel:" + phone;
							Intent intent = new Intent(Intent.ACTION_DIAL);
							intent.setData(Uri.parse(uri));
							StockSearchMap.this.startActivity(intent);
						}
						else
							MetrixUIHelper.showSnackbar(StockSearchMap.this, AndroidResourceHelper.getMessage("NoTelephonyServiceAvailable"));
					}
				});
				mh1.setLinkText(phone);
			} else {
				mh1.setVisibility(View.GONE);
			}

			MetrixHyperlink mh2 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item2);

			if (infoItem.length > 3 && MetrixStringHelper.isNullOrEmpty(infoItem[3]) == false) {
				final String emailAddress = infoItem[3];
				mh2.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
						emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailAddress});
						emailIntent.setType("plain/text");
						MetrixActivityHelper.startNewActivity(StockSearchMap.this, Intent.createChooser(emailIntent, AndroidResourceHelper.getMessage("SendEmail")));
					}
				});

				mh2.setLinkText(emailAddress);
			} else {
				mh2.setVisibility(View.GONE);
			}

			MetrixHyperlink mh3 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item3);
			//tv4.setAutoLinkMask(Linkify.MAP_ADDRESSES);
			mh3.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Location currentLocation = MetrixLocationAssistant.getCurrentLocation(StockSearchMap.this);

					if (currentLocation != null) {
						double geoLat = currentLocation.getLatitude();
						double geoLong = currentLocation.getLongitude();

						Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW,
								Uri.parse("http://maps.google.com/maps?f=q&hl=en&geocode=&saddr=" + geoLat + "," + geoLong + "&daddr=" + lPosition.latitude + "," + lPosition.longitude));
						StockSearchMap.this.startActivity(mapIntent);
					}
				}
			});
			mh3.setLinkText(address);

			MetrixHyperlink mh4 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item4);
			mh4.setVisibility(View.GONE);

			MetrixHyperlink mh5 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item5);
			mh5.setVisibility(View.GONE);
		}

		center = CameraUpdateFactory.newLatLng(new LatLng(lat, lng));
		map.animateCamera(boundZoom);
		map.moveCamera(center);

		Window window = popDialog.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();

		wlp.verticalMargin = (float) .5;
		wlp.gravity = Gravity.BOTTOM;
		//wlp.flags &=~WindowManager.LayoutParams.FLAG_DIM_BEHIND; // this will set popup to the bottom
		window.setAttributes(wlp);
		popDialog.show();
	}
}

