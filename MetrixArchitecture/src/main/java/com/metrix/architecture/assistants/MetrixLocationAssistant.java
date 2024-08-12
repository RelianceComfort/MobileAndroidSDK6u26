package com.metrix.architecture.assistants;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import com.metrix.architecture.constants.MetrixDistanceUnitOfMeasure;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.Locale;

/**
 * Contains helper methods to make it easy to interact with the GPS radio on 
 * the device. getCurrentLocation returns an instance of the <code>Location</code>
 * class on which are the current latitude, longitude and other GPS settings.
 * startLocationManager will open the radio while stopLocationManager will close
 * it. Remember there is a significant battery usage cost to keeping the GPS active,
 * so if you don't need it running, be sure to shut it down (by default, the radio
 * will be off when you start up the application).
 * 
 * @see android.location.Location
 * @since 5.4
 */
public class MetrixLocationAssistant {
	private static final String LOCATION_MANAGER_CACHE_KEY = "METRIX_LOCATION_MANAGER";
	private static final String LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY = "METRIX_LOCATION_MANAGER_LAST_GEO_LOCATION";
    
	/**
	 * Starts the location manager service.
	 * 
	 * @param activity
	 *            the activity starting the service.
	 * @return TRUE if the manager was started, FALSE otherwise.
	 */
	public static boolean startLocationManager(Activity activity) {
		boolean fineLocationGranted = (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
		boolean coarseLocationGranted = (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

		if (fineLocationGranted || coarseLocationGranted) {
			LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

			LocationListener listener = new LocationListener() {
				@Override
				public void onLocationChanged(Location arg0) {
					if (MetrixPublicCache.instance.containsKey(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY)) {
						MetrixPublicCache.instance.removeItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY);
					}
					MetrixPublicCache.instance.addItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY, arg0);
				}

				@Override
				public void onProviderDisabled(String arg0) {
				}

				@Override
				public void onProviderEnabled(String provider) {
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
				}
			};

			if (fineLocationGranted) {
				manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 45000, 10, listener);
			} else {
				manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 45000, 10, listener);
			}

			MetrixPublicCache.instance.addItem(LOCATION_MANAGER_CACHE_KEY, manager);
			return true;
		}

		return false;
	}

	/**
	 * Starts the location manager service.
	 * 
	 * @param context
	 *            the context starting the service.
	 * @return TRUE if the manager was started, FALSE otherwise.
	 */
	public static boolean startLocationManager(Context context) {
		boolean fineLocationGranted = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
		boolean coarseLocationGranted = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

		if (fineLocationGranted || coarseLocationGranted) {
			LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

			LocationListener listener = new LocationListener() {
				@Override
				public void onLocationChanged(Location arg0) {
					if (MetrixPublicCache.instance.containsKey(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY)) {
						MetrixPublicCache.instance.removeItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY);
					}
					MetrixPublicCache.instance.addItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY, arg0);
				}

				@Override
				public void onProviderDisabled(String arg0) {
				}

				@Override
				public void onProviderEnabled(String provider) {
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
				}
			};

			if (fineLocationGranted) {
				manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
			} else {
				manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
			}

			MetrixPublicCache.instance.addItem(LOCATION_MANAGER_CACHE_KEY, manager);
			return true;
		}
		return false;
	}

	/**
	 * Stops the location manager service.
	 * 
	 * @return TRUE if the manager was stopped, FALSE otherwise.
	 */
	public static boolean stopLocationManager() {
		MetrixPublicCache.instance.removeItem(LOCATION_MANAGER_CACHE_KEY);
		MetrixPublicCache.instance.removeItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY);
		return true;
	}

	/**
	 * Gets the location manager from the cache.
	 * 
	 * @param activity
	 *            the activity requesting the manager.
	 * @return the LocationManager instance from the cache if it exists, null
	 *         otherwise.
	 */
	private static LocationManager getLocationManager(Activity activity) {
		Object object = MetrixPublicCache.instance.getItem(LOCATION_MANAGER_CACHE_KEY);

		if (object == null) {
			MetrixLocationAssistant.startLocationManager(activity);
			object = MetrixPublicCache.instance.getItem(LOCATION_MANAGER_CACHE_KEY);
		}

		return (LocationManager) object;
	}

	/**
	 * Gets the location manager from the cache.
	 * 
	 * @param context
	 *            the context requesting the manager.
	 * @return the LocationManager instance from the cache if it exists, null
	 *         otherwise.
	 */
	private static LocationManager getLocationManager(Context context) {
		Object object = MetrixPublicCache.instance.getItem(LOCATION_MANAGER_CACHE_KEY);

		if (object == null) {
			MetrixLocationAssistant.startLocationManager(context);
			object = MetrixPublicCache.instance.getItem(LOCATION_MANAGER_CACHE_KEY);
		}

		return (LocationManager) object;
	}

	/**
	 * Returns a Location indicating the data from the last known location fix
	 * obtained from the given provider. This can be done without starting the
	 * provider. Note that this location could be out-of-date, for example if
	 * the device was turned off and moved to another location. If the provider
	 * is currently disabled, null is returned.
	 * 
	 * @param activity the activity requesting the location.
	 * @return the current location,
	 */
	public static Location getCurrentLocation(Activity activity) {
		Object object = MetrixPublicCache.instance.getItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY);
		if (object != null) {
			Location location = (Location) object;
			LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [cache] - " + "Lat: " + String.valueOf(location.getLatitude()));
			LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [cache] - " + "Long: " + String.valueOf(location.getLongitude()));
			return location;
		} else {
			LocationManager manager = MetrixLocationAssistant.getLocationManager(activity);
			String providerName = LocationManager.NETWORK_PROVIDER;

			if (manager != null &&
					(ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
					ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
				Location location = manager.getLastKnownLocation(providerName);
				if (location != null) {
					LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [manager] - " + "Lat: " + String.valueOf(location.getLatitude()));
					LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [manager] - " + "Long: " + String.valueOf(location.getLongitude()));

					if (MetrixPublicCache.instance.containsKey(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY)) {
						MetrixPublicCache.instance.removeItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY);
					}
					MetrixPublicCache.instance.addItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY, location);
				}
				return location;
			}

			return null;
		}
	}

	/**
	 * Returns a Location indicating the data from the last known location fix
	 * obtained from the given provider. This can be done without starting the
	 * provider. Note that this location could be out-of-date, for example if
	 * the device was turned off and moved to another location. If the provider
	 * is currently disabled, null is returned.
	 * 
	 * @param context the context requesting the location.
	 * @return the current location,
	 */
	public static Location getCurrentLocation(Context context) {
		Object object = MetrixPublicCache.instance.getItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY);
		if (object != null) {
			Location location = (Location) object;
			LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [cache] - " + "Lat: " + String.valueOf(location.getLatitude()));
			LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [cache] - " + "Long: " + String.valueOf(location.getLongitude()));
			return location;
		} else {
			LocationManager manager = MetrixLocationAssistant.getLocationManager(context);			
			String providerName = LocationManager.NETWORK_PROVIDER;

			if (manager != null &&
					(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
					ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
				Location location = manager.getLastKnownLocation(providerName);
				if (location != null) {
					LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [manager] - " + "Lat: " + String.valueOf(location.getLatitude()));
					LogManager.getInstance().debug("com.metrix.architecture.assistants.MetrixLocationAssistant.getCurrentLocation [manager] - " + "Long: " + String.valueOf(location.getLongitude()));

					if (MetrixPublicCache.instance.containsKey(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY)) {
						MetrixPublicCache.instance.removeItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY);
					}
					MetrixPublicCache.instance.addItem(LOCATION_MANAGER_LAST_GEO_LOCATION_CACHE_KEY, location);
				}

				return location;
			}

			return null;
		}
	}
	
	/**
	 * This method takes two geopositions represented by latitude and longitude points along with a distance-based unit of measure
	 * and returns the distance between the two points represented as the unit of measure. For example, if the unit of measure is
	 * set to MetrixDistanceUnitOfMeasure.METERS, then the distance between the two points will be measured in and returned as 
	 * meters.
	 * 
	 * @param startLatitude The first geoposition latitude.
	 * @param startLongitude The first geoposition longitude.
	 * @param endLatitude The second geoposition latitude.
	 * @param endLongitude The second geoposition longitude.
	 * @param unitOfMeasure A value from the MetrixDistanceUnitOfMeasure enumerator defining how the distance between the two points should be measured.
	 * @return The distance between the two points.
	 * 
	 * @since 5.6 Patch 1
	 */
	public static double calculateDistanceBetweenLocations(double startLatitude, double startLongitude, double endLatitude, double endLongitude, MetrixDistanceUnitOfMeasure unitOfMeasure) {
		double distance = 0;
		float[] resultInMeters = new float[1];
		Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, resultInMeters); 
		
		if (unitOfMeasure == MetrixDistanceUnitOfMeasure.METERS) {
			distance = (double)resultInMeters[0];
		} else if (unitOfMeasure == MetrixDistanceUnitOfMeasure.KILOMETERS) {
			distance = ((double)resultInMeters[0] * 0.001);
		} else if (unitOfMeasure == MetrixDistanceUnitOfMeasure.FEET) {
			distance = ((double)resultInMeters[0] * 3.2808398950131235);
		} else if (unitOfMeasure == MetrixDistanceUnitOfMeasure.MILES) {
			distance = ((double)resultInMeters[0] * 0.0006213711922373339);
		} else {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("ValidUnitsOfMeasureSupp"));
		}
		
		return distance;
	}
	
	/**
	 * This method takes two geopositions represented by latitude and longitude points. It used the GPS_LOCATION_COMPARISON_TOLERANCE_UOM
	 * app param along with the positions to calculate the distance between the two points (using the calculateDistanceBetweenLocations
	 * method on this class). It then compares the resulting distance against the value of the GPS_LOCATION_COMPARISON_TOLERANCE app 
	 * param and returns a boolean indicating if the distance is less than or equal to the app param.
	 * 
	 * @param startLatitude The first geoposition latitude.
	 * @param startLongitude The first geoposition longitude.
	 * @param endLatitude The second geoposition latitude.
	 * @param endLongitude The second geoposition longitude.
	 * @return TRUE if the distance between the two points is less than or equal to the app param value, FALSE otherwise.
	 * 
	 * @since 5.6 Patch 1
	 */
	public static boolean distanceIsGreaterThanAllowedTolerance(String startLatitude, String startLongitude, String endLatitude, String endLongitude) {
		if (!MetrixStringHelper.isDouble(startLatitude)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("ParamMustContainAValue1Args", AndroidResourceHelper.getMessage("TheStartLatitude")));
		}

		if (!MetrixStringHelper.isDouble(startLongitude)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("ParamMustContainAValue1Args", AndroidResourceHelper.getMessage("TheStartLongitude")));
		}

		if (!MetrixStringHelper.isDouble(endLatitude)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("ParamMustContainAValue1Args", AndroidResourceHelper.getMessage("TheEndLatitude")));
		}

		if (!MetrixStringHelper.isDouble(endLongitude)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("ParamMustContainAValue1Args", AndroidResourceHelper.getMessage("TheEndLongitude")));
		}
		
		return distanceIsGreaterThanAllowedTolerance(Double.valueOf(startLatitude), Double.valueOf(startLongitude), Double.valueOf(endLatitude), Double.valueOf(endLongitude));
	}
	
	/**
	 * This method takes two geopositions represented by latitude and longitude points. It used the GPS_LOCATION_COMPARISON_TOLERANCE_UOM
	 * app param along with the positions to calculate the distance between the two points (using the calculateDistanceBetweenLocations
	 * method on this class). It then compares the resulting distance against the value of the GPS_LOCATION_COMPARISON_TOLERANCE app 
	 * param and returns a boolean indicating if the distance is less than or equal to the app param.
	 * 
	 * @param startLatitude The first geoposition latitude.
	 * @param startLongitude The first geoposition longitude.
	 * @param endLatitude The second geoposition latitude.
	 * @param endLongitude The second geoposition longitude.
	 * @return TRUE if the distance between the two points is less than or equal to the app param value, FALSE otherwise.
	 * 
	 * @since 5.6 Patch 1
	 */
	public static boolean distanceIsGreaterThanAllowedTolerance(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
		String toleranceParam = MetrixFloatHelper.convertNumericFromDBToForcedLocale(MobileApplication.getAppParam("GPS_LOCATION_COMPARISON_TOLERANCE"), Locale.US);
		String unitOfMeasureParam = MobileApplication.getAppParam("GPS_LOCATION_COMPARISON_TOLERANCE_UOM");

		if (MetrixStringHelper.isNullOrEmpty(toleranceParam)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheAppParamNeedsSet1Args", "GPS_LOCATION_COMPARISON_TOLERANCE"));
		}

		if (MetrixStringHelper.isNullOrEmpty(unitOfMeasureParam)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheAppParamNeedsSet1Args", "GPS_LOCATION_COMPARISON_TOLERANCE_UOM"));
		}
		
		MetrixDistanceUnitOfMeasure unitOfMeasure;
		
		if (unitOfMeasureParam.equalsIgnoreCase("feet")) {
			unitOfMeasure = MetrixDistanceUnitOfMeasure.FEET;
		} else if (unitOfMeasureParam.equalsIgnoreCase("miles")) {
			unitOfMeasure = MetrixDistanceUnitOfMeasure.MILES;
		} else if (unitOfMeasureParam.equalsIgnoreCase("meters")) {
			unitOfMeasure = MetrixDistanceUnitOfMeasure.METERS;
		} else if (unitOfMeasureParam.equalsIgnoreCase("kilometers")) {
			unitOfMeasure = MetrixDistanceUnitOfMeasure.KILOMETERS;
		} else {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("ValidUnitsOfMeasureSupp"));
		}
		
		double distance = calculateDistanceBetweenLocations(startLatitude, startLongitude, endLatitude, endLongitude, unitOfMeasure);
		
		if (distance > Double.valueOf(toleranceParam)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void setGeocodeForTaskStatus(Context context, MetrixFormDef mFormDef, ViewGroup mLayout) {
		// default the duration of the job based on the task type
		String value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='GPS_LOCATION_TASK_STATUS_UPDATE'");
		boolean updateTask = MetrixRoleHelper.isGPSFunctionEnabled("GPS_TASK");
		
		if (!MetrixStringHelper.isNullOrEmpty(value) && value.compareToIgnoreCase("Y") == 0) {
			if(updateTask) {
				Location currentLocation = MetrixLocationAssistant.getCurrentLocation(context);

				if (currentLocation != null) {
					try {
						MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "geocode_lat", Double.toString(currentLocation.getLatitude()));
						MetrixControlAssistant.setValue(mFormDef, mLayout, "task", "geocode_long", Double.toString(currentLocation.getLongitude()));
					} catch (Exception e) {
						LogManager.getInstance(context).error(e);
					}
				}
			}
		}
	}
}