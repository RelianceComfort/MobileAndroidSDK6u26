package com.metrix.metrixmobile;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixHyperlink;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.global.MetrixMapApiHelper;
import com.metrix.metrixmobile.system.Help;
import com.metrix.metrixmobile.system.MetrixMap;

import java.util.Locale;

public class TeamMemberMap extends MetrixMap {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar(AndroidResourceHelper.getMessage("TeamMemberMapTitle"));
	}

	@Override
	public void onStart() {
		super.onStart();
		LogManager.getInstance(this).info("{0} onStart()", this.getLocalClassName());

		this.helpText = AndroidResourceHelper.getMessage("ScreenDescriptionTeamMemberMap");
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.up:
				Intent intent = MetrixActivityHelper.createActivityIntent(this, TeamList.class);
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
		MetrixCursor cursor = null;
		try {
			StringBuilder query = new StringBuilder();
			query.append("select person.first_name, person.last_name, team.description, person.email_address, person.job_title, person.phone, person.mobile_phone, person.person_id");
			query.append(" , person.geocode_lat, person.geocode_long, person.modified_dttm");
			query.append(" from team_member left join person on team_member.person_id = person.person_id left join team on team_member.team_id = team.team_id");
			query.append(" where team_member.team_id in (select distinct(team_id) from team_member where person_id = '");
			query.append(User.getUser().personId);
			query.append("')");

			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			double minLat = Double.MAX_VALUE;
			double maxLat = Double.MIN_VALUE;
			double minLon = Double.MAX_VALUE;
			double maxLon = Double.MIN_VALUE;

			int k = 0;
			builder = new LatLngBounds.Builder();
			String personInfoContainer = AndroidResourceHelper.getMessage("PersonInfo4Args");

			while (cursor.isAfterLast() == false) {
				if (cursor.getString(8) != null && cursor.getString(9) != null) {
					String personInfo = AndroidResourceHelper.formatMessage(personInfoContainer, "PersonInfo4Args", MetrixStringHelper.getString(cursor.getString(4)), MetrixStringHelper.getString(cursor.getString(6)), MetrixStringHelper.getString(cursor.getString(3)), MetrixDateTimeHelper.convertDateTimeFromDBToUI(MetrixStringHelper.getString(cursor.getString(10))));

					double dblGeoLat;
					double dblGeoLong;

					if (MetrixFloatHelper.getServerDecimalSeparator().compareTo(".")!=0) {
						dblGeoLat = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(8), Locale.US));
						dblGeoLong = Double.valueOf(MetrixFloatHelper.convertNumericFromDBToForcedLocale(cursor.getString(9), Locale.US));
					} else {
						dblGeoLat = cursor.getDouble(8);
						dblGeoLong = cursor.getDouble(9);
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

					StringBuilder personItem = new StringBuilder();
					personItem.append(MetrixStringHelper.getString(cursor.getString(0)) + " " + MetrixStringHelper.getString(cursor.getString(1)) + "\r\n");

					if ((cursor.getString(7) != null) && (cursor.getString(7).compareToIgnoreCase(User.getUser().personId) == 0)) {
						builder.include(MetrixMapApiHelper.addMarker(map, dblGeoLat, dblGeoLong, personItem.toString(), personInfo, R.drawable.map_pin_red_32_20, false).getPosition());
					} else {
						builder.include(MetrixMapApiHelper.addMarker(map, dblGeoLat, dblGeoLong, personItem.toString(), personInfo, R.drawable.map_pin_blue_32_20, false).getPosition());
					}
				}

				cursor.moveToNext();
			}

			LatLngBounds bounds = builder.build();

			center = CameraUpdateFactory.newLatLng(new LatLng((maxLat+minLat)/2, (maxLon+minLon)/2));
			int padding = 128; // offset from edges of the map in pixels
			if (k == 1) {
				boundZoom = CameraUpdateFactory.zoomTo(10);
			} else {
				boundZoom = CameraUpdateFactory.newLatLngBounds(bounds, padding);
			}
		} catch (SQLException ex) {
			LogManager.getInstance(this).error(ex);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	@Override
	protected void setupPopup(Marker marker) {
		popDialog = new Dialog(TeamMemberMap.this);
		popDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		popDialog.setTitle(AndroidResourceHelper.getMessage("SiteInfo"));
		popDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
		popDialog.setContentView(R.layout.map_popup);
		ViewGroup vg = (ViewGroup) popDialog.findViewById(R.id.balloon_inner_layout);

		streetView = (ImageView) popDialog.findViewById(R.id.street_view);

		loadSingleView executeDownload = new loadSingleView();

		final LatLng lPosition = marker.getPosition();
		double lat = lPosition.latitude;
		double lng = lPosition.longitude;

//	      String mapKey = MetrixApplicationAssistant.getMetaStringValue(this, "com.google.android.maps.v2.API_KEY");
		String urlString = "http://maps.googleapis.com/maps/api/streetview?size=400x100&location=" + lat + "," + lng + "&sensor=false";
		executeDownload.execute(urlString);

		String titleInfo = marker.getTitle();
		String generalInfo = marker.getSnippet();

		String[] infoItem = generalInfo.split("\r\n"); // we know it should include first name and last name

		TextView headerTitle = (TextView) popDialog.findViewById(R.id.headerTitle);
		MetrixSkinManager.setFirstGradientBackground(headerTitle, 0);
		String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
		headerTitle.setText(titleInfo);
		if (!MetrixStringHelper.isNullOrEmpty(firstGradientText)) {
			headerTitle.setTextColor(Color.parseColor(firstGradientText));
		}

		TextView title = (TextView) popDialog.findViewById(R.id.balloon_item_title);
		title.setVisibility(View.GONE);

		MetrixHyperlink mh1 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item1);
		mh1.setText(infoItem[0]);

		MetrixHyperlink mh2 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item2);

		if (infoItem.length > 3 && MetrixStringHelper.isNullOrEmpty(infoItem[3]) == false) {
			//tv2.setAutoLinkMask(Linkify.PHONE_NUMBERS);
			final String phone = infoItem[1];
			mh2.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
						String uri = "tel:" + phone;
						Intent intent = new Intent(Intent.ACTION_DIAL);
						intent.setData(Uri.parse(uri));
						TeamMemberMap.this.startActivity(intent);
					}
					else
						MetrixUIHelper.showSnackbar(TeamMemberMap.this, AndroidResourceHelper.getMessage("NoTelephonyServiceAvailable"));
				}
			});
			mh2.setLinkText(phone);
		} else {
			mh2.setVisibility(View.GONE);
		}

		MetrixHyperlink mh3 = (MetrixHyperlink)popDialog.findViewById(R.id.balloon_item3);

		if (infoItem.length > 2 && MetrixStringHelper.isNullOrEmpty(infoItem[2]) == false) {
			final String emailAddress = infoItem[2];
			mh3.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
					emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailAddress});
					emailIntent.setType("plain/text");
					MetrixActivityHelper.startNewActivity(TeamMemberMap.this, Intent.createChooser(emailIntent, AndroidResourceHelper.getMessage("SendEmail")));
				}
			});

			mh3.setLinkText(emailAddress);
		} else {
			mh3.setVisibility(View.GONE);
		}

		MetrixHyperlink mh4 = (MetrixHyperlink) popDialog.findViewById(R.id.balloon_item4);
		//MetrixControlAssistant.giveTextViewHyperlinkApperance(R.id.balloon_item4, vg);
		if (infoItem.length > 3 && MetrixStringHelper.isNullOrEmpty(infoItem[3]) == false) {
			final String time = infoItem[3];
			mh4.setText(time);
		} else {
			mh4.setVisibility(View.GONE);
		}


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
}

