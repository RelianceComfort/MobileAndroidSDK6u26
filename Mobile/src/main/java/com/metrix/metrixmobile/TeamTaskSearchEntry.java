package com.metrix.metrixmobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

/**
 * Created by RaWiLK on 2/23/2016.
 */
public class TeamTaskSearchEntry {

    private Context mContext;
    private AlertDialog mDialog;
    protected ViewGroup mLayout;
    private String mFilter;

    public TeamTaskSearchEntry() {

    }

    public TeamTaskSearchEntry(Context context, String filter) {
        this.mContext = context;
        this.mFilter = filter;
    }

    /**
     * Initialize the UI of the Dialog control
     */
    @SuppressLint("InflateParams")
    public void initDialog() {

        try {

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = (ViewGroup)inflater.inflate(R.layout.team_task_search_entry, null);


            mDialog = new AlertDialog.Builder(mContext)
                    //Way of automatically hiding of Dialog once you click on Positive button.
                    .setPositiveButton(AndroidResourceHelper.getMessage("Search"), null)
                    .setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), null).setView(mLayout)
                    .setTitle(AndroidResourceHelper.getMessage("TeamTaskSearchEntryTitle")).create();

            final TextView tvRadiusLabel = mLayout.findViewById(R.id.tvRadiusLabel);
            final TextView tvResultsLabel = mLayout.findViewById(R.id.tvResultsLabel);
            AndroidResourceHelper.setResourceValues(tvRadiusLabel, "Radius");
            AndroidResourceHelper.setResourceValues(tvResultsLabel, "Results");

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

            final Spinner sTask = (Spinner) mLayout.findViewById(R.id.spinTasks);
            ArrayAdapter<SpinnerKeyValuePair> placeAdapter = new ArrayAdapter<SpinnerKeyValuePair>(this.mContext, R.layout.spinner_item, placeLimits);
            placeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sTask.setAdapter(placeAdapter);
            sTask.setSelection(0, true); // set the first one as default value

            //Executed show() before accessing the buttons, otherwise the buttons will be null.
            mDialog.show();

            Button searchButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if(searchButton != null){
                searchButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mContext);

                        if (currentLocation != null) {
                            double geoLat = currentLocation.getLatitude();
                            double geoLong = currentLocation.getLongitude();

                            if (geoLat==0 || geoLong==0) {
                                Toast.makeText(mContext, AndroidResourceHelper.getMessage("CouldNotAcquireTheCurrentGPS"), Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            Toast.makeText(mContext, AndroidResourceHelper.getMessage("ActivateGPSFeatureOnThisDevice"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            String numTasks = MetrixControlAssistant.getValue(sTask);
                            String mileLimit = MetrixControlAssistant.getValue(sDistance);

                            Intent mapIntent = MetrixActivityHelper.createActivityIntent((Activity) mContext, TaskMap.class);
                            mapIntent.putExtra("DistanceLimit", mileLimit);
                            mapIntent.putExtra("TaskLimit", numTasks);
                            mapIntent.putExtra("Filter", mFilter);
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
}

