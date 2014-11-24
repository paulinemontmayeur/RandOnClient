package com.insa.randon.model;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.insa.randon.R;

public class GoogleMap extends Map {
	public static final int LINE_WIDTH = 3;
	com.google.android.gms.maps.GoogleMap googleMap = null;

	@Override
	public void setUpMap(Activity activity) {
		MapFragment fm = (MapFragment) activity.getFragmentManager().findFragmentById(R.id.map);
        googleMap = fm.getMap();
        googleMap.setMyLocationEnabled(true);		
	}

	@Override
	public void showRoute(List<LatLng> route) {
		if (googleMap != null){

			int size = route.size();
			if(size>=2)
			{
				route.add(route.get(0)); //Close the loop
				googleMap.addPolyline(new PolylineOptions()
			     .addAll(route)
			     .width(LINE_WIDTH)
			     .color(Color.GREEN));		
			}	
		}
		
	}

	@Override
	public int getLayoutId() {
		return R.layout.googlemap;
	}

	
}