package com.insa.randon.controller;



import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.insa.randon.R;
import com.insa.randon.model.GoogleMap;
import com.insa.randon.model.Hike;
import com.insa.randon.model.Map;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

public class MapActivity extends BaseActivity {	
	private static final int MIN_TIME_INTERVAL_MS = 1000;
	private static final int MIN_DISTANCE_INTERVAL_M = 3;
	private static final int DELAY = 60000;
	private static final String DISTANCE_UNIT_DISTANCE = " km";
	private static final String DISTANCE_UNIT_DIFF = " m";
	private static final String HOUR_UNIT = " h";
	private static final String MINUTE_UNIT = " min";
	private static final String SPEED_UNIT = " km/h";
	private static final float CONVERT_SPEED_UNIT_TO_KMH = 3600;
	private static final int REQUEST_CODE_FINISH_HIKE = 1;
	public static final String EXTRA_HIKE = "hike";
	public static final String EXTRA_MODE = "mode";
	public static final int CREATION_MODE = 0;
	public static final int FOLLOWING_MODE = 1;
	
	private Map map;
	private int mode;
	private LocationManager locManager;
	private Hike newHike;
	private Hike existingHike;
	private TextView distanceTextView;
	private TextView speedTextView;
	private TextView durationTextView;
	private TextView positiveAltitudeTextView;
	private TextView negativeAltitudeTextView;
	private ImageView slidingIcon;
	private FollowHikeLocationListener locListener;
	private ViewStub mapContainer;
	private AlertDialog alert = null;

	private Context context;
	private long startTime = 0;

	//timer
	private Handler timerHandler = new Handler();
	private Runnable timerRunnable = new Runnable() {
		float speed=0;

		@Override
		public void run() {
			long millis = System.currentTimeMillis() - startTime;
			int seconds = (int) (millis / 1000);
			int minutes = seconds / 60;
			int hours = minutes / 60;
			seconds = seconds % 60;
			minutes = minutes % 60;

			String timeStr = String.format("%d"+HOUR_UNIT+" %d"+MINUTE_UNIT, hours, minutes);
			durationTextView.setText(timeStr);
			newHike.setDuration(timeStr);

			//compute average speed
			if (millis == 0){
				speed = 0;
			} else {
				speed = newHike.getDistance()*1000/millis*CONVERT_SPEED_UNIT_TO_KMH;
			}			
			newHike.setAverageSpeed(speed);
			speedTextView.setText(String.format("%.2f", speed) + SPEED_UNIT);

			timerHandler.postDelayed(this, DELAY);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		context=this;
		
		//Get mode
		Intent intent = getIntent();
        mode = intent.getIntExtra(EXTRA_MODE, CREATION_MODE);

		map = new GoogleMap();      
		mapContainer = (ViewStub) findViewById(R.id.map_activity_container);
		mapContainer.setLayoutResource(map.getLayoutId());    
		mapContainer.inflate();
		map.setUpMap(this);

		distanceTextView = (TextView) findViewById(R.id.distance_textView);
		speedTextView = (TextView) findViewById(R.id.speed_textView);
		durationTextView = (TextView) findViewById(R.id.duration_textView);
		positiveAltitudeTextView = (TextView) findViewById(R.id.positive_altitude_textView);
		negativeAltitudeTextView = (TextView) findViewById(R.id.negative_altitude_textView);
		positiveAltitudeTextView.setText("0"+DISTANCE_UNIT_DIFF);
		negativeAltitudeTextView.setText("0"+DISTANCE_UNIT_DIFF);
		distanceTextView.setText("0"+DISTANCE_UNIT_DISTANCE);

		//setting PanelSlideListener listener
		SlidingUpPanelLayout sPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_map);
		sPanelLayout.expandPanel();
		slidingIcon = (ImageView) findViewById(R.id.sliding_icon);
		sPanelLayout.setPanelSlideListener(new SlidingUpPanelListener(slidingIcon));


		locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locListener = new FollowHikeLocationListener(); 

		//Start timer
		startTime = System.currentTimeMillis();
		timerHandler.postDelayed(timerRunnable, 0);

		newHike = new Hike();
		if(mode==CREATION_MODE){
			//Create a new hike
			map.initializeNewHike();
		}else if(mode==FOLLOWING_MODE){
			existingHike = (Hike) intent.getParcelableExtra(EXTRA_HIKE);
			map.showRoute(existingHike.getCoordinates());
			
			//Listen for the layout to be complete
			distanceTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
	            @Override
	            public void onGlobalLayout() {
					map.setBounds(existingHike.getBoungToDisplay());
	             }
	        });
		}

		//check if GPS is enabled
		PackageManager pm = getPackageManager();
		boolean hasGps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
		if(!hasGps){
			locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_INTERVAL_MS, MIN_DISTANCE_INTERVAL_M, locListener);
		} else if (hasGps && !locManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
			showAlertMessageNoGps();
		} else {
			locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_INTERVAL_MS, MIN_DISTANCE_INTERVAL_M, locListener);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (alert != null){
			alert.dismiss();
		}
		if (locManager != null){
			locManager.removeUpdates(locListener);
		}    
	}

	@Override
	protected void onPause(){
		super.onPause();
		timerHandler.removeCallbacks(timerRunnable);

	}

	@Override
	protected void onResume(){
		super.onResume();

		//resume timer
		timerHandler.postDelayed(timerRunnable, 0);
	}

	@Override
	public void onBackPressed() {
		if (newHike.getCoordinates().size() <= 1){
			super.onBackPressed();
		} else {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setMessage(R.string.exit_confirmation)
			.setNeutralButton(R.string.dialog_cancel, null)
			.setPositiveButton(R.string.dialog_save, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(context, FinishHikeActivity.class);
					intent.putExtra(EXTRA_HIKE, newHike);
					startActivityForResult(intent, REQUEST_CODE_FINISH_HIKE);
				}
			})
			.setNegativeButton(R.string.dialog_quit, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();					
				}
			});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
	}

	private void showAlertMessageNoGps() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.gps_disabled)
		.setCancelable(false)
		.setPositiveButton(R.string.positive_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				finish();
			}
		})
		.setNegativeButton(R.string.negative_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_DISTANCE_INTERVAL_M, MIN_DISTANCE_INTERVAL_M, locListener);    
			}
		});
		alert = builder.create();
		alert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Location lastLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		map.centerOnLocation(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_finnish_hike:
			timerHandler.removeCallbacks(timerRunnable); //Stop timer

			if (newHike.getCoordinates().size() == 0 && mode==CREATION_MODE){
				finish();
			} else {
				Intent intent = new Intent(context, FinishHikeActivity.class);
				if(mode==CREATION_MODE){
					intent.putExtra(EXTRA_HIKE, newHike);
					startActivityForResult(intent, REQUEST_CODE_FINISH_HIKE);
		     	} else if(mode==FOLLOWING_MODE){
		     		existingHike.setDuration(newHike.getDuration());
					intent.putExtra(EXTRA_HIKE, existingHike);
					startActivity(intent);
				}
				
			}
			
		break;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			finish();
		}

	}

	//------------------ LOCATION LISTENER ------------------------------------
	public class FollowHikeLocationListener implements LocationListener{
		double currentAltitude=0;
		double previousAltitude=Double.NEGATIVE_INFINITY;
		double positiveHeightDifference=0;
		double negativeHeightDifference=0;

		@Override
		public void onLocationChanged(Location location)
		{    
			//if the instantaneous speed is null, we do not change the distance done
			if (location.getSpeed() > 0){
				//TODO : test if we are in creation mode
				//if we follow a hike that we downloaded maybe we can create a new hike like but we don't call followingHike
				map.followingHike(new LatLng(location.getLatitude(),location.getLongitude()));

				newHike.extendHike(new LatLng(location.getLatitude(),location.getLongitude()));
				distanceTextView.setText(String.format("%.2f", newHike.getDistance()) + DISTANCE_UNIT_DISTANCE);

				//Compute the positive difference of height
				currentAltitude = location.getAltitude();
				if(currentAltitude>previousAltitude && previousAltitude!=Double.NEGATIVE_INFINITY)
				{
					positiveHeightDifference+=currentAltitude-previousAltitude;
					positiveAltitudeTextView.setText(String.format("%.2f", positiveHeightDifference)+DISTANCE_UNIT_DIFF);
					newHike.setPositiveDiffHeight((float) positiveHeightDifference);
				} else if(currentAltitude<previousAltitude && previousAltitude!=Double.NEGATIVE_INFINITY){
					negativeHeightDifference+=currentAltitude-previousAltitude;
					negativeAltitudeTextView.setText(String.format("%.2f", negativeHeightDifference)+DISTANCE_UNIT_DIFF);
					newHike.setNegativeDiffHeight((float) negativeHeightDifference);
				}
				previousAltitude=currentAltitude;
			}
		}

		@Override
		public void onProviderDisabled(String provider)
		{

		}

		@Override
		public void onProviderEnabled(String provider)
		{

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{

		}                
	}

	//--------------- SlidingUpPlanel Listener
	private class SlidingUpPanelListener implements PanelSlideListener{
		private ImageView imageView;

		public SlidingUpPanelListener(ImageView imageView){
			this.imageView = imageView;
		}

		@Override
		public void onPanelSlide(View panel, float slideOffset) {
		}

		@Override
		public void onPanelCollapsed(View panel) {
			imageView.setImageResource(R.drawable.ic_action_collapse);
		}

		@Override
		public void onPanelExpanded(View panel) {
			imageView.setImageResource(R.drawable.ic_action_expand);
		}

		@Override
		public void onPanelAnchored(View panel) {

		}

		@Override
		public void onPanelHidden(View panel) {

		}

	}
}
