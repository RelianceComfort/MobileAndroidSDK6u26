package com.metrix.architecture.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;

public class MapWidgetReadyCallback implements OnMapReadyCallback {
    private final MapWidgetHolder holder;
    private final int screenId;
    private final Activity activity;

    public MapWidgetReadyCallback(int screenId, MapWidgetHolder holder, Activity activity) {
        this.screenId = screenId;
        this.holder = holder;
        this.activity = activity;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        googleMap.setOnMapClickListener((latLng) -> {}); // Disabling default click behaviour on the map
        this.holder.container.setVisibility(View.GONE);

        final ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || connectivityManager.getActiveNetworkInfo() == null || !connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting())
            return; // Hide the map and don't process Map Script, if there is no internet connection

        final ClientScriptDef mapScript = MetrixScreenManager.getMapScriptDef(screenId);
        if (mapScript != null) {
            final String strCoordinates = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<>(activity), mapScript);
            if (!MetrixStringHelper.isNullOrEmpty(strCoordinates)) {
                String[] coords = strCoordinates.split(",");
                if (coords.length > 2) {
                    // This could be from a locale that uses comma as a decimal separator.
                    int occurrence = 2;
                    for (int i = 0; i < strCoordinates.length(); i++) {
                        if (strCoordinates.charAt(i) == ',') {
                            occurrence--;
                            if (occurrence == 0) {
                                coords = new String[]{strCoordinates.substring(0, i), strCoordinates.substring(i + 1)};
                                break;
                            }
                        }
                    }
                }
                if (coords.length == 2) {
                    final String strLatitude = coords[0].trim();
                    final String strLongitude = coords[1].trim();
                    try {
                        final NumberFormat nf = NumberFormat.getInstance();
                        final double latitude = nf.parse(strLatitude).doubleValue();
                        final double longitude = nf.parse(strLongitude).doubleValue();
                        final LatLng coordinates = new LatLng(latitude, longitude);
                        googleMap.addMarker(new MarkerOptions().position(coordinates));
                        // Adjust map's camera view so that it shows the added marker
                        final CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(coordinates, 15);
                        googleMap.moveCamera(cu);
                        ((View) holder.btnGetDirections.getParent()).setTag(latitude + "," + longitude);
                        holder.btnGetDirections.setVisibility(View.VISIBLE);
                        holder.container.setVisibility(View.VISIBLE);
                    } catch (Exception ex) {
                        LogManager.getInstance().error(ex);
                    }
                }
            }
        }
    }
}