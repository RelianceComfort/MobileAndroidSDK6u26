package com.metrix.architecture.ui.widget;

import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.MapView;
import com.metrix.metrixmobile.R;

public final class MapWidgetHolder {
    public final MapView mapView;
    public final Button btnGetDirections;
    public final View container;

    public MapWidgetHolder (View containerView) {
        this.mapView = containerView.findViewById(R.id.map_view);
        this.btnGetDirections = containerView.findViewById(R.id.btn_get_directions);
        this.container = containerView;
    }
}
