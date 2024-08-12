package com.metrix.metrixmobile;

import java.util.Hashtable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;
import com.metrix.metrixmobile.global.MobileGlobal;

/**
 * a Dialog helper class that can initialize the dialog UI and execute the
 * function of the positive button
 *
 */
public class StockSearchEntry implements OnClickListener{

	private Context mContext;
	private AlertDialog mDialog;
	protected ViewGroup mLayout;

	public StockSearchEntry() {

	}

	public StockSearchEntry(Context context) {
		this.mContext = context;
	}

	/**
	 * Initialize the UI of the Dialog control
	 */
	@SuppressLint("InflateParams")
	public void initDialog() {

		try {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = (ViewGroup)inflater.inflate(R.layout.stock_search_entry, null);
			AndroidResourceHelper.setResourceValues(mLayout.findViewById(R.id.tvPartLabel), "PartId", false);
			AndroidResourceHelper.setResourceValues(mLayout.findViewById(R.id.tvDistanceLabel), "Radius", false);
			AndroidResourceHelper.setResourceValues(mLayout.findViewById(R.id.tvPlacesLabel), "Results", false);
			AndroidResourceHelper.setResourceValues(mLayout.findViewById(R.id.tvQuantityLabel), "Quantity", false);

			mDialog = new AlertDialog.Builder(mContext)
					//Way of automatically hiding of Dialog once you click on Positive button.
					.setPositiveButton(AndroidResourceHelper.getMessage("StkSearchEtySearchBtnTxt"), null)
					.setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), null).setView(mLayout)
					.setTitle(AndroidResourceHelper.getMessage("StockSearchEntryTitle")).create();

			SpinnerKeyValuePair mileOptions[];
			mileOptions=new SpinnerKeyValuePair[5];

			String distanceUnitOfMeasure = AndroidResourceHelper.getMessage("Miles");
			if (MobileApplication.getAppParam("GEOCODE_DISTANCE_UNITS").compareToIgnoreCase("K") == 0) {
				distanceUnitOfMeasure = AndroidResourceHelper.getMessage("Kilometers");
			}

			mileOptions[0]=new SpinnerKeyValuePair("25 " + distanceUnitOfMeasure, "25");
			mileOptions[1]=new SpinnerKeyValuePair("50 " + distanceUnitOfMeasure, "50");
			mileOptions[2]=new SpinnerKeyValuePair("75 " + distanceUnitOfMeasure, "75");
			mileOptions[3]=new SpinnerKeyValuePair("100 " + distanceUnitOfMeasure, "100");
			mileOptions[4]=new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("NoLimit"), "10000");

			SpinnerKeyValuePair placeLimits[];
			placeLimits=new SpinnerKeyValuePair[3];

			placeLimits[0] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Places1Arg", "5"), "5");
			placeLimits[1] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Places1Arg", "10"), "10");
			placeLimits[2] = new SpinnerKeyValuePair(AndroidResourceHelper.getMessage("Places1Arg", "15"), "15");

			final Spinner sDistance = (Spinner) mLayout.findViewById(R.id.spinDistance);
			ArrayAdapter<SpinnerKeyValuePair> adapter = new ArrayAdapter<SpinnerKeyValuePair>(this.mContext, R.layout.spinner_item, mileOptions);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sDistance.setAdapter(adapter);
			sDistance.setSelection(0, true); // set the first one as default value

			final Spinner sPlace = (Spinner) mLayout.findViewById(R.id.spinPlaces);
			ArrayAdapter<SpinnerKeyValuePair> placeAdapter = new ArrayAdapter<SpinnerKeyValuePair>(this.mContext, R.layout.spinner_item, placeLimits);
			placeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sPlace.setAdapter(placeAdapter);
			sPlace.setSelection(0, true); // set the first one as default value

			// Set an EditText view to get user input
			final EditText input = (EditText) mLayout.findViewById(R.id.txtSearchPartId);
			input.setOnClickListener(this);

			final EditText minQtyRequiredTbx = (EditText) mLayout.findViewById(R.id.txtMinQtyRequired);
			minQtyRequiredTbx.setText("1");

			//Executed show() before accessing the buttons, otherwise the buttons will be null.
			mDialog.show();

			Button searchButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
			if(searchButton != null){
				searchButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {

						String searchedPartId = input.getText().toString();
						if (MetrixStringHelper.isNullOrEmpty(searchedPartId)) {
							Toast.makeText(mContext, AndroidResourceHelper.getMessage("StkSearchEtyNoPartIdEntered"), Toast.LENGTH_LONG).show();
							return;
						}

						Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mContext);

						if (currentLocation != null) {
							double geoLat = currentLocation.getLatitude();
							double geoLong = currentLocation.getLongitude();

							if (geoLat==0 || geoLong==0) {
								Toast.makeText(mContext, AndroidResourceHelper.getMessage("NoGPSLocation"), Toast.LENGTH_LONG).show();
								return;
							}
						}
						else {
							Toast.makeText(mContext, AndroidResourceHelper.getMessage("ActivateGPSFeature"), Toast.LENGTH_LONG).show();
							return;
						}

						try {
							String value = input.getText().toString();
							String numPlaces = MetrixControlAssistant.getValue(sPlace);
							String mileLimit = MetrixControlAssistant.getValue(sDistance);
							String minQtyRequired = minQtyRequiredTbx.getText().toString();

							performQuery(value, numPlaces, mileLimit, minQtyRequired);

							Intent mapIntent = MetrixActivityHelper.createActivityIntent((Activity) mContext, StockSearchMap.class);
							mapIntent.putExtra("Filter", "part_id='" + value + "'");
							mapIntent.putExtra("PriorActivity", mContext.getClass().getName());
							MetrixActivityHelper.startNewActivity((Activity) mContext, mapIntent);

							mDialog.dismiss();
						}
						catch(Exception ex){
							LogManager.getInstance().error(ex);
						}

					}
				});
			}

			Button cancelButton = mDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
			if(cancelButton != null){
				cancelButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						mDialog.dismiss();
					}
				});
			}

		}catch(Exception ex){
			LogManager.getInstance().error(ex);
		}

	}

	/**
	 * Create and save the MPM function to the mm_message_out table
	 *
	 * @param part_id
	 */
	private void performQuery(String part_id, String numPlaces, String mileLimit, String minQtyRequired) {
		Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mContext);

		if (currentLocation != null) {
			double geoLat = currentLocation.getLatitude();
			double geoLong = currentLocation.getLongitude();
			Hashtable<String, String> params = new Hashtable<String, String>();

			try {
				params.put("part_id", part_id);
				params.put("start_lat", MetrixFloatHelper.convertNumericFromUIToDB(MetrixFloatHelper.currentLocaleNumericValue(geoLat)));
				params.put("start_long", MetrixFloatHelper.convertNumericFromUIToDB(MetrixFloatHelper.currentLocaleNumericValue(geoLong)));
				params.put("number_place", numPlaces);
				params.put("mileage_limit", mileLimit);
				params.put("min_qty_required", minQtyRequired);

				MetrixPerformMessage performStockSearch = new MetrixPerformMessage("perform_get_closest_places_by_part_id", params);
				performStockSearch.save();
			}
			catch(Exception ex){
				LogManager.getInstance().error(ex);
			}
		} else {
			Toast.makeText(mContext, AndroidResourceHelper.getMessage("NoGPSFailedSearch"), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onClick(View v) {
		MetrixLookupDef lookupDef = new MetrixLookupDef("part");
		lookupDef.columnNames.add(new MetrixLookupColumnDef("part.internal_descriptn"));
		lookupDef.columnNames.add(new MetrixLookupColumnDef("part.part_id", R.id.txtSearchPartId));
		String item = AndroidResourceHelper.getMessage("Part");
		lookupDef.title = AndroidResourceHelper.getMessage("LookupHelpA", item.toLowerCase());
		lookupDef.initialSearchCriteria = MetrixControlAssistant.getValue(v.getId(), mLayout);

		Intent intent =  new Intent(mContext, com.metrix.metrixmobile.system.Lookup.class);
		MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
		MetrixPublicCache.instance.addItem("lookupParentLayout", mLayout);
		((Activity) mContext).startActivityForResult(intent, MobileGlobal.GET_LOOKUP_RESULT);
	}
}


