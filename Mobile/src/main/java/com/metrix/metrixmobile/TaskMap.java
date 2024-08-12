package com.metrix.metrixmobile;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
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
import com.metrix.architecture.constants.MetrixDistanceUnitOfMeasure;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.FilterSortItem;
import com.metrix.architecture.designer.MetrixFilterSortManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixMapApiHelper;
import com.metrix.metrixmobile.system.Help;
import com.metrix.metrixmobile.system.MetrixMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TaskMap extends MetrixMap {
	private final String taskTextContainer = AndroidResourceHelper.getMessage("TaskMapItem5Args");
	private Location taskCurrentLocation;
	double taskCurrentLat;
	double taskCurrentLong;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String filterItemLabel = getIntent().getStringExtra("Filter");
		setupActionBar(filterItemLabel);
	}

	@Override
	public void onStart() {
		super.onStart();
		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());
		this.helpText = AndroidResourceHelper.getMessage("ScreenDescriptionTaskMap");
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.up:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MetrixActivityHelper.startNewActivity(this, intent);
				break;
			case R.id.action_bar_help:
				intent = MetrixActivityHelper.createActivityIntent(this, Help.class);

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
		try {
			int jobListScreenID = MetrixScreenManager.getScreenId("JobList");
			String filterItemLabel = getIntent().getStringExtra("Filter");
			String distanceLimit = getIntent().getStringExtra("DistanceLimit");
			String placeLimit = getIntent().getStringExtra("TaskLimit");

			StringBuilder query = new StringBuilder();
			query.append("select task.task_id, task.description, task.priority, task.address_id, address.geocode_lat, address.geocode_long,");
			query.append(" address.address, address.city, address.state_prov, place.name, place.place_id, contact.email_address, contact.phone, request.origin");
			query.append(" from task left outer join address on task.address_id = address.address_id left outer join place on task.place_id_cust = place.place_id");
			query.append(" left outer join contact on task.contact_sequence = contact.contact_sequence left outer join request on task.request_id = request.request_id");
			query.append(" where address.geocode_lat is not null and address.geocode_long is not null");

			String restOfWhereClause = String.format(" and (task.person_id = '%1$s' or exists (select task_id from task_resource where task_resource.task_id = task.task_id and task_resource.person_id = '%1$s' and task_resource.assigned_resource = 'Y')) and task.task_status in (select task_status from task_status where status = 'OP' and task_status <> '%2$s')", User.getUser().personId, MobileApplication.getAppParam("REJECTED_TASK_STATUS"));
			String constraintFilter = MetrixDatabaseManager.getAppParam("ADDITIONAL_JOB_LIST_CONSTRAINTS");
			if (!MetrixStringHelper.isNullOrEmpty(constraintFilter)){
				restOfWhereClause = restOfWhereClause + " and ("+constraintFilter+")";
			}

			if (MetrixStringHelper.valueIsEqual(filterItemLabel, AndroidResourceHelper.getMessage("OtherJobs"))) {
				// For "Other Jobs", we passed a literal filter from DebriefOverview into JobList - continue using it here
				String literalFilter = getIntent().getStringExtra("LiteralFilter");
				restOfWhereClause = restOfWhereClause + " and " + literalFilter;
			} else {
				// This should be a selected FilterSortItem from JobList
				// We always keep the address GPS not null, even if full filter ... but respect the filter metadata otherwise
				FilterSortItem filterItem = MetrixFilterSortManager.getFilterSortItemByLabel(filterItemLabel, MetrixFilterSortManager.getFilterItems(jobListScreenID));
				if (filterItem != null && !MetrixStringHelper.isNullOrEmpty(filterItem.Content)) {
					String actualContent = MetrixFilterSortManager.resolveFilterSortContent(this, filterItem.Content);
					if (filterItem.FullFilter)
						restOfWhereClause = " and " + actualContent;
					else
						restOfWhereClause = restOfWhereClause + " and " + actualContent;
				}
			}
			query.append(restOfWhereClause);

			taskCurrentLocation = MetrixLocationAssistant.getCurrentLocation(this);
			if (taskCurrentLocation != null) {
				taskCurrentLat = taskCurrentLocation.getLatitude();
				taskCurrentLong = taskCurrentLocation.getLongitude();
			}

			if (MetrixStringHelper.valueIsEqual(filterItemLabel, MetrixFilterSortManager.getFilterLabelByItemName("Team Tasks", jobListScreenID))) {
				if(taskCurrentLocation != null)
					showTeamTask(query.toString(), distanceLimit, placeLimit);
				else
					MetrixUIHelper.showSnackbar(this, AndroidResourceHelper.getMessage("GPSProblem"));
			} else
				showNonTeamTasks(query.toString());
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	@Override
	protected void setupPopup(Marker marker) {
		popDialog = new Dialog(TaskMap.this);
		popDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		popDialog.setTitle(AndroidResourceHelper.getMessage("SiteInfoTitle"));
		popDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
		popDialog.setContentView(R.layout.map_popup);
		ViewGroup vg = (ViewGroup) popDialog.findViewById(R.id.balloon_inner_layout);

		streetView = (ImageView) popDialog.findViewById(R.id.street_view);

		loadSingleView executeDownload = new loadSingleView();

		final LatLng lPosition = marker.getPosition();
		double lat = lPosition.latitude;
		double lng = lPosition.longitude;

		@SuppressWarnings("unused")
		String mapKey = MetrixApplicationAssistant.getMetaStringValue(this, "com.google.android.maps.v2.API_KEY");
		String urlString = "http://maps.googleapis.com/maps/api/streetview?size=400x100&location=" + lat + "," + lng + "&sensor=false";
		executeDownload.execute(urlString);

		String generalInfo = marker.getTitle();
		String address = marker.getSnippet();

		String[] infoItem = generalInfo.split("\r\n"); // we know it should include task_id, task description, customer name

		TextView headerTitle = (TextView) popDialog.findViewById(R.id.headerTitle);
		MetrixSkinManager.setFirstGradientBackground(headerTitle, 0);
		String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
		if(infoItem.length > 2 && MetrixStringHelper.isNullOrEmpty(infoItem[2]) == false)
			headerTitle.setText(infoItem[2]);
		if (!MetrixStringHelper.isNullOrEmpty(firstGradientText)) {
			headerTitle.setTextColor(Color.parseColor(firstGradientText));
		}

		TextView title = (TextView) popDialog.findViewById(R.id.balloon_item_title);
		if (infoItem.length > 0 && MetrixStringHelper.isNullOrEmpty(infoItem[0]) == false)
			title.setText(infoItem[0]);
		else
			title.setVisibility(View.GONE);

		MetrixHyperlink mh1 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item1);
		if (infoItem.length > 1 && MetrixStringHelper.isNullOrEmpty(infoItem[1]) == false)
			mh1.setText(infoItem[1]);
		else
			mh1.setVisibility(View.GONE);

		MetrixHyperlink mh2 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item2);

		if (infoItem.length > 3 && MetrixStringHelper.isNullOrEmpty(infoItem[3]) == false) {
			//tv2.setAutoLinkMask(Linkify.PHONE_NUMBERS);
			final String phone = infoItem[3];
			mh2.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
						String uri = "tel:" + phone;
						Intent intent = new Intent(Intent.ACTION_DIAL);
						intent.setData(Uri.parse(uri));
						TaskMap.this.startActivity(intent);
					}
					else
						MetrixUIHelper.showSnackbar(TaskMap.this, AndroidResourceHelper.getMessage("NoTelephonyServiceAvailable"));
				}
			});
			mh2.setLinkText(phone);
		} else {
			mh2.setVisibility(View.GONE);
		}

		MetrixHyperlink mh3 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item3);

		if (infoItem.length > 4 && MetrixStringHelper.isNullOrEmpty(infoItem[4]) == false) {
			final String emailAddress = infoItem[4];
			mh3.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
					emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailAddress});
					emailIntent.setType("plain/text");
					MetrixActivityHelper.startNewActivity(TaskMap.this, Intent.createChooser(emailIntent, AndroidResourceHelper.getMessage("SendEmail")));
				}
			});

			mh3.setLinkText(emailAddress);
		} else {
			mh3.setVisibility(View.GONE);
		}

		MetrixHyperlink mh4 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item4);
		mh4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Location currentLocation = MetrixLocationAssistant.getCurrentLocation(TaskMap.this);

				if (currentLocation != null) {
					double geoLat = currentLocation.getLatitude();
					double geoLong = currentLocation.getLongitude();

					Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW,
							Uri.parse("http://maps.google.com/maps?f=q&hl=en&geocode=&saddr=" + geoLat + "," + geoLong + "&daddr=" + lPosition.latitude + "," + lPosition.longitude));
					TaskMap.this.startActivity(mapIntent);
				}
			}
		});
		mh4.setLinkText(address);

		MetrixHyperlink mh5 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item5);
		mh5.setVisibility(View.GONE);

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

	/***
	 * Displaying of default tasks(which are assigned to a person) on a map
	 * @param query
	 */
	private void showNonTeamTasks(String query) {
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
		HashMap<String, ArrayList<String>> tasksInfo = new HashMap<String, ArrayList<String>>();

		try {
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			double minLat = Double.MAX_VALUE;
			double maxLat = Double.MIN_VALUE;
			double minLon = Double.MAX_VALUE;
			double maxLon = Double.MIN_VALUE;

			int k = 0;
			builder = new LatLngBounds.Builder();

			while (cursor.isAfterLast() == false) {
				if (cursor.getString(4) != null && cursor.getString(5) != null) {
					StringBuilder address = new StringBuilder();
					address.append(cursor.getString(6));
					address.append(", ");
					address.append(cursor.getString(7));
					address.append(", ");
					address.append(cursor.getString(8));

					String taskDescription = cursor.getString(1).length() > 30 ? cursor.getString(1).substring(0, 30) + "..." : cursor.getString(1);
					String customerName = cursor.getString(9);

					String email = cursor.getString(11) == null ? "" : cursor.getString(11);
					String phone = cursor.getString(12) == null ? "" : cursor.getString(12);

					String key = cursor.getString(4) + "-" + cursor.getString(5);

					double dblGeoLat;
					double dblGeoLong;

					if (MetrixFloatHelper.getServerDecimalSeparator().compareTo(".") != 0) {
						dblGeoLat = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(4), Locale.US));
						dblGeoLong = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(5), Locale.US));
					} else {
						dblGeoLat = cursor.getDouble(4);
						dblGeoLong = cursor.getDouble(5);
					}

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

					ArrayList<String> listInfo = new ArrayList<String>();
					String taskInfo = AndroidResourceHelper.formatMessage(taskTextContainer,
							"TaskMapItem5Args", cursor.getString(0), taskDescription, customerName, phone, email);

					if (tasksInfo.containsKey(key)) {
						listInfo = tasksInfo.get(key);
						listInfo.add(taskInfo);
						tasksInfo.put(key, listInfo);
					} else {
						listInfo.add(taskInfo);
						tasksInfo.put(key, listInfo);
					}

					StringBuilder taskItem = new StringBuilder();

					if (listInfo.size() > 0) {
						int i = 0;
						for (String task : listInfo) {
							if (i == listInfo.size() - 1)
								taskItem.append(task);
							else
								taskItem.append(task + "\r\n");
							i++;
						}
					}

					if ((cursor.getString(2) != null) && (cursor.getString(2).compareToIgnoreCase("00") == 0 || cursor.getString(2).compareToIgnoreCase("01") == 0)) {
						builder.include(MetrixMapApiHelper.addMarker(map, dblGeoLat, dblGeoLong, taskItem.toString(), address.toString(), R.drawable.map_pin_red_32_20, false).getPosition());
					} else {
						builder.include(MetrixMapApiHelper.addMarker(map, dblGeoLat, dblGeoLong, taskItem.toString(), address.toString(), R.drawable.map_pin_blue_32_20, false).getPosition());
					}

					k++;
				}
				cursor.moveToNext();
			}
			// show current user location for non team tasks
			if(!MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild") && taskCurrentLocation != null){
				builder.include(MetrixMapApiHelper.addMarker(map, taskCurrentLat, taskCurrentLong, AndroidResourceHelper.getMessage("MyLocation"), "", R.drawable.location, false).getPosition());
				k++;
			}

			if (k != 0) {
				center = CameraUpdateFactory.newLatLng(new LatLng((maxLat + minLat) / 2, (maxLon + minLon) / 2));
			} else {
				Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);
				if (currentLocation != null)
					center = CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
				else
					center = CameraUpdateFactory.newLatLng(new LatLng((maxLat + minLat) / 2, (maxLon + minLon) / 2));
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
		} catch (Exception ex) {
			LogManager.getInstance(this).error(ex);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/***
	 * Displaying of team/pooled tasks on a map
	 * @param query
	 * @param distanceLmt
	 * @param placeLmt
	 */
	private void showTeamTask(String query, String distanceLmt, String placeLmt) {
		List<HashMap<String, Object>> teamTaskList = getTeamTasks(query, distanceLmt);

		try {
			HashMap<String, ArrayList<String>> teamTasksInfo = new HashMap<String, ArrayList<String>>();

			int displayCount = -1;
			if (!MetrixStringHelper.isNullOrEmpty(placeLmt)) {
				int placeLimit = Integer.parseInt(placeLmt);
				displayCount = placeLimit < teamTaskList.size() ? placeLimit : teamTaskList.size();
			}

			int minLat = Integer.MAX_VALUE;
			int maxLat = Integer.MIN_VALUE;
			int minLon = Integer.MAX_VALUE;
			int maxLon = Integer.MIN_VALUE;

			int k = 0;
			builder = new LatLngBounds.Builder();

			if(teamTaskList != null && teamTaskList.size() > 0) {
				for (HashMap<String, Object> teamTaskDetail : teamTaskList) {
					if (k > displayCount)
						break;

					String address = (String) teamTaskDetail.get("address");
					String taskDescription = (String) teamTaskDetail.get("taskDescription");
					String customerName = (String) teamTaskDetail.get("customerName");
					String email = (String) teamTaskDetail.get("email");
					String phone = (String) teamTaskDetail.get("phone");
					String key = (String) teamTaskDetail.get("key");
					double dblGeoLat = (Double) teamTaskDetail.get("dblGeoLat");
					double dblGeoLong = (Double) teamTaskDetail.get("dblGeoLong");
					String taskId = (String) teamTaskDetail.get("taskId");
					String taskPriority = (String) teamTaskDetail.get("taskPriority");

					ArrayList<String> listInfo = new ArrayList<String>();
					String taskInfo = AndroidResourceHelper.formatMessage(taskTextContainer,
							"TaskMapItem5Args", taskId, taskDescription, customerName, phone, email);

					if (teamTasksInfo.containsKey(key)) {
						listInfo = teamTasksInfo.get(key);
						listInfo.add(taskInfo);
						teamTasksInfo.put(key, listInfo);
					} else {
						listInfo.add(taskInfo);
						teamTasksInfo.put(key, listInfo);
					}

					StringBuilder taskItem = new StringBuilder();

					if (listInfo.size() > 0) {
						int i = 0;
						for (String task : listInfo) {
							if (i == listInfo.size() - 1)
								taskItem.append(task);
							else
								taskItem.append(task + "\r\n");
							i++;
						}
					}

					maxLat = Math.max((int) (dblGeoLat * 1E6), maxLat);
					minLat = Math.min((int) (dblGeoLat * 1E6), minLat);
					maxLon = Math.max((int) (dblGeoLong * 1E6), maxLon);
					minLon = Math.min((int) (dblGeoLong * 1E6), minLon);

					if ((taskPriority != null) && (taskPriority.compareToIgnoreCase("00") == 0 || taskPriority.compareToIgnoreCase("01") == 0)) {
						builder.include(MetrixMapApiHelper.addMarker(map, dblGeoLat, dblGeoLong, taskItem.toString(), address.toString(), R.drawable.map_pin_red_32_20, false).getPosition());
					} else {
						builder.include(MetrixMapApiHelper.addMarker(map, dblGeoLat, dblGeoLong, taskItem.toString(), address.toString(), R.drawable.map_pin_blue_32_20, false).getPosition());
					}

					k++;
				}
			}
			else
				MetrixUIHelper.showSnackbar(this, AndroidResourceHelper.getMessage("PTaskNotFound"));

			if(!MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild")){
				builder.include(MetrixMapApiHelper.addMarker(map, taskCurrentLat, taskCurrentLong, AndroidResourceHelper.getMessage("MyLocation"), "", R.drawable.location, false).getPosition());
				k++;
			}

			if(k!=0) {
				center=CameraUpdateFactory.newLatLng(new LatLng((maxLat+minLat)/2,	(maxLon+minLon)/2));
			}
			else
			{
				center=CameraUpdateFactory.newLatLng(new LatLng(taskCurrentLat, taskCurrentLong));
			}

			int padding = 128; // offset from edges of the map in pixels
			if(k<=1){	// 0 or 1 marker included
				boundZoom = CameraUpdateFactory.zoomTo(10);
			}
			else {
				LatLngBounds bounds = builder.build();
				boundZoom = CameraUpdateFactory.newLatLngBounds(bounds, padding);
			}

		} catch (Exception ex) {
			LogManager.getInstance(this).error(ex);
		}
	}

	private List<HashMap<String, Object>> getTeamTasks(String query, String distanceLmt) {
		MetrixCursor cursor = null;
		List<HashMap<String, Object>> teamTaskList = new ArrayList<HashMap<String, Object>>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			double distanceLimit = -1;
			if (!MetrixStringHelper.isNullOrEmpty(distanceLmt))
				distanceLimit = Double.parseDouble(distanceLmt);

			while (cursor.isAfterLast() == false) {
				if (cursor.getString(4) != null && cursor.getString(5) != null) {
					StringBuilder address = new StringBuilder();
					address.append(cursor.getString(6));
					address.append(", ");
					address.append(cursor.getString(7));
					address.append(", ");
					address.append(cursor.getString(8));

					String taskDescription = cursor.getString(1).length() > 30 ? cursor.getString(1).substring(0, 30) + "..." : cursor.getString(1);
					String customerName = cursor.getString(9);

					String email = cursor.getString(11) == null ? "" : cursor.getString(11);
					String phone = cursor.getString(12) == null ? "" : cursor.getString(12);

					String taskId = cursor.getString(0);
					String key = cursor.getString(4) + "-" + cursor.getString(5) + taskId;
					String taskPriority = cursor.getString(2);

					double dblGeoLat;
					double dblGeoLong;
					if (MetrixFloatHelper.getServerDecimalSeparator().compareTo(".") != 0) {
						dblGeoLat = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(4), Locale.US));
						dblGeoLong = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(5), Locale.US));
					} else {
						dblGeoLat = cursor.getDouble(4);
						dblGeoLong = cursor.getDouble(5);
					}

					MetrixDistanceUnitOfMeasure distanceUOMEnum = MetrixDistanceUnitOfMeasure.MILES;
					String strGdu = MetrixDatabaseManager.getAppParam("GEOCODE_DISTANCE_UNIT");
					if (!MetrixStringHelper.isNullOrEmpty(strGdu))
					{
						if (MetrixStringHelper.valueIsEqual(strGdu.toUpperCase(), "K"))
							distanceUOMEnum = MetrixDistanceUnitOfMeasure.KILOMETERS;
					}

					double distanceRetured = MetrixLocationAssistant.calculateDistanceBetweenLocations(taskCurrentLat, taskCurrentLong, dblGeoLat, dblGeoLong, distanceUOMEnum);
					if (distanceRetured <= distanceLimit) {
						HashMap<String, Object> teamTaskDetail = new HashMap<String, Object>();
						teamTaskDetail.put("address", address.toString());
						teamTaskDetail.put("taskDescription", taskDescription);
						teamTaskDetail.put("customerName", customerName);
						teamTaskDetail.put("email", email);
						teamTaskDetail.put("phone", phone);
						teamTaskDetail.put("key", key);
						teamTaskDetail.put("dblGeoLat", dblGeoLat);
						teamTaskDetail.put("dblGeoLong", dblGeoLong);
						teamTaskDetail.put("taskId", taskId);
						teamTaskDetail.put("taskPriority", taskPriority);

						teamTaskList.add(teamTaskDetail);
					}
				}
				cursor.moveToNext();
			}

		} catch (SQLException ex) {
			LogManager.getInstance(this).error(ex);
		} catch (Exception ex) {
			LogManager.getInstance(this).error(ex);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return teamTaskList;
	}
}

