package com.metrix.metrixmobile.system;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.NotificationCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixServiceManager;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuAdapter;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuItem;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MobileGlobal;

/**
 * Added an MetrixMap base class with abstract method for subclass to implement
 * setPopup window It is for Google Map Api v2 project
 *
 * @author edlius
 *
 */
public abstract class MetrixMap extends AppCompatActivity implements OnMarkerClickListener, OnClickListener, MetrixSlidingMenuAdapter.OnItemClickListener {

	public String helpText;
	protected boolean mShouldSetupMapOnCreate = true;
	protected GoogleMap map;
	protected LatLngBounds.Builder builder;
	protected Context context;
	protected ProgressDialog pDialog;
	protected Dialog popDialog;
	protected ImageView streetView;
	protected View mapView;
	protected CameraUpdate center;
	protected CameraUpdate boundZoom;
	protected URL imageUrl;
	protected String imageUrlString;
	protected Bitmap bmImg = null;
	protected Activity mCurrentActivity = null;

	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private DrawerLayout mDrawerLayout;
	private RecyclerView mMetrixSlidingMenu;
	private ActionBarDrawerToggle mDrawerToggle;
	private LinearLayout mDrawerLinearLayout;
	private ActionBar mSupportActionBar;

	protected boolean mHandlingErrors = false;
	private Activity mActivity;

	protected boolean mIsBound = false;
	protected IPostMonitor service = null;
	protected boolean mInitializationStarted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MetrixStringHelper.isNullOrEmpty(MobileApplication.ApplicationNullIfCrashed)) {
			finish();
		}

		super.onCreate(savedInstanceState);
		mCurrentActivity = this;
		MetrixServiceManager.setup(this);
		bindService();

		if (MapsInitializer.initialize(this) != ConnectionResult.SUCCESS) {
			MetrixUIHelper.showSnackbar(this, AndroidResourceHelper.getMessage("PlayServicesNotAvail"));
			return;
		}

		try {
			setContentView(R.layout.map_fragement);
			this.context = this.mActivity = this;

			if (mShouldSetupMapOnCreate)
				setupMap();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
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
			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final Global.ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					processPostListener(activityType, message);
				}
			});
		}
	};

	protected void processPostListener(Global.ActivityType activityType, String message) {
		if (activityType == Global.ActivityType.InitializationStarted) {
			mInitializationStarted = true;
			LogManager.getInstance(mCurrentActivity).debug("Initialization, run on the base activity.");
			mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("Initializing"));
		} else if (activityType == Global.ActivityType.InitializationEnded) {
			mInitializationStarted = false;
			mUIHelper.dismissLoadingDialog();
			User.setUser(User.getUser().personId, mCurrentActivity);
			Intent intent = MetrixActivityHelper.getInitialActivityIntent(mCurrentActivity);
			MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
		} else if (activityType == Global.ActivityType.PasswordChangedFromServer) {
			handleServerPasswordChange();
		}
	}

	@SuppressWarnings("deprecation")
	protected void handleServerPasswordChange() {
		SettingsHelper.saveStringSetting(mCurrentActivity, "SETTING_PASSWORD_UPDATED", "Y", false);
		MobileApplication.stopSync(mCurrentActivity);
		final AlertDialog mNewPassAlert = new AlertDialog.Builder(mCurrentActivity).create();
		mNewPassAlert.setCancelable(false);
		mNewPassAlert.setTitle(AndroidResourceHelper.getMessage("PasswordChangedTitle"));
		mNewPassAlert.setMessage(AndroidResourceHelper.getMessage("PasswordUpdatedRemotely"));
		mNewPassAlert.setButton(AndroidResourceHelper.getMessage("OKButton"), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (mNewPassAlert != null) {
					mNewPassAlert.dismiss();
				}

				try {
					Intent intent = new Intent();
					intent.setClass(mCurrentActivity, Class.forName("com.metrix.metrixmobile.system.Login"));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					mCurrentActivity.startActivity(intent);
				} catch (Exception e) {
					LogManager.getInstance().error(e);
				}
			}
		});
		mNewPassAlert.show();
	}

	private void setDefaultLocationSetting() {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;

		Location currentLocation = MetrixLocationAssistant.getCurrentLocation(this);

		if(currentLocation!=null)
			center=CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(),	currentLocation.getLongitude()));
		else
			center=CameraUpdateFactory.newLatLng(new LatLng((maxLat+minLat)/2,	(maxLon+minLon)/2));

		boundZoom = CameraUpdateFactory.zoomTo(10);
	}

	@SuppressLint("InflateParams")
	protected void setupActionBar(String barText) {
		mSupportActionBar = MetrixActionBarManager.getInstance().setupActionBar(this, R.layout.action_bar, true);
		String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();
		MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, barText, firstGradientText);

		String smallIconImageID = MetrixSkinManager.getSmallIconImageID();
		if (!MetrixStringHelper.isNullOrEmpty(smallIconImageID))
			MetrixActionBarManager.getInstance().setActionBarCustomizedIcon(smallIconImageID, getMetrixActionBar(), 24, 24);
//		else
//			MetrixActionBarManager.getInstance().setActionBarDefaultIcon(R.drawable.ifs_logo, getMetrixActionBar(), 24, 24);

		mDrawerLinearLayout = (LinearLayout) findViewById(R.id.drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (mDrawerLinearLayout != null && mDrawerLayout != null)
			if (mSupportActionBar != null) {
				MetrixSkinManager.setFirstGradientBackground(mSupportActionBar, 0);
				mDrawerToggle = MetrixSlidingMenuManager.getInstance().setUpSlidingDrawer(this, mSupportActionBar,
						mDrawerLinearLayout, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
						R.string.drawer_close, firstGradientText, R.drawable.ellipsis_vertical);
			}

		mMetrixSlidingMenu = findViewById(R.id.recyclerview_sliding_menu);
		MetrixListScreenManager.setupVerticalRecyclerView(mMetrixSlidingMenu, 0);

		ImageView up = (ImageView) findViewById(R.id.up);
		if (up != null) {
			up.setImageResource(R.drawable.up);
			up.setOnClickListener(this);
			up.setVisibility(View.GONE);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public boolean onMarkerClick(Marker arg0) {
		if (arg0.getSnippet() == null) {
			map.moveCamera(CameraUpdateFactory.zoomIn());
			return true;
		}

		setupPopup(arg0);

		return true;
	}

	protected void setupMap() {
		SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFrag.getMapAsync((m) -> {
			map = m;
			mapView = mapFrag.getView();
			setDefaultLocationSetting();
			setupMarkers();

			if (mapView.getViewTreeObserver().isAlive()) {
				mapView.getViewTreeObserver().addOnGlobalLayoutListener(
						new OnGlobalLayoutListener() {
							@SuppressWarnings("deprecation")
							@Override
							public void onGlobalLayout() {
								// remove the listener
								mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

								// CENTER is LatLng object with the center of the map
								map.moveCamera(center);
								map.animateCamera(boundZoom);
							}
						});
			}

			map.setOnMarkerClickListener(this);
		});
	}

	protected abstract void setupMarkers();

	protected abstract void setupPopup(Marker marker);

	public class loadSingleView extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setTitle(AndroidResourceHelper.getMessage("ConnectToServer"));
			pDialog.setMessage(AndroidResourceHelper.getMessage("ProcessCanTakeTime"));
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... args) {
			try {
				imageUrlString = args[0];
				imageUrl = new URL(imageUrlString);
				HttpURLConnection conn = (HttpURLConnection) imageUrl
						.openConnection();
				conn.setDoInput(true);
				conn.connect();
				InputStream is = conn.getInputStream();
				bmImg = BitmapFactory.decodeStream(is);

				imageUrl = null;
			} catch (IOException e) {
				LogManager.getInstance().error(e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(String args) {
			streetView = (ImageView) popDialog.findViewById(R.id.street_view);

			if (bmImg != null && streetView != null)
				streetView.setImageBitmap(bmImg);

			popDialog.show();
			pDialog.dismiss();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerToggle != null)
			mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerToggle != null)
			mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle != null) {
			if (mDrawerToggle.onOptionsItemSelected(item))
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		//boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerLinearLayout);
		//menu.findItem(R.id.action_search).setVisible(!drawerOpen);

		// provide ability to hide options menu (used by Mobile Designer when using standard Lookup)
		if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NoOptionsMenu")
				&& this.getIntent().getExtras().getBoolean("NoOptionsMenu")) {
			return false;
		}

		ArrayList<MetrixSlidingMenuItem> slidingMenuItems = new ArrayList<MetrixSlidingMenuItem>();
		MetrixSlidingMenuItem slidingMenuItemHelp = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("DesignerHelp"), R.drawable.sliding_menu_about);
		slidingMenuItems.add(slidingMenuItemHelp);

		final MetrixSlidingMenuAdapter slidingMenuAdapter = new MetrixSlidingMenuAdapter(R.layout.sliding_menu_item, R.id.textview_sliding_menu_item_name,
				R.id.textview_sliding_menu_item_count, R.id.imageview_sliding_menu_item_icon, slidingMenuItems, this);

		if (mMetrixSlidingMenu != null)
			mMetrixSlidingMenu.setAdapter(slidingMenuAdapter);
		return super.onPrepareOptionsMenu(menu);
	}

	public ActionBar getMetrixActionBar() {
		return mSupportActionBar;
	}

	@Override
	public void onSlidingMenuItemClick(MetrixSlidingMenuItem clickedItem) {
		String title = clickedItem.getTitle();
		if (!MetrixStringHelper.isNullOrEmpty(title)) {

			if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("DesignerHelp"))) {
				Intent intent = MetrixActivityHelper.createActivityIntent(mActivity, Help.class);
				String message = null;
				if (!MetrixStringHelper.isNullOrEmpty(helpText))
					message = helpText;
				else
					message = AndroidResourceHelper.getMessage("NoHelpDetailsAvailable");

				if (mHandlingErrors) {
					message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ErrorMess1Arg", MobileGlobal.mErrorInfo.errorMessage);
				}
				intent.putExtra("help_text", message);
				MetrixActivityHelper.startNewActivity(mActivity, intent);
			}
		}

		if (mDrawerLayout != null)
			mDrawerLayout.closeDrawer(mDrawerLinearLayout);
	}
}

