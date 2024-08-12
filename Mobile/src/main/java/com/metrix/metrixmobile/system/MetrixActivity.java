package com.metrix.metrixmobile.system;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.graphics.ColorUtils;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.actionbar.MetrixActionBarResourceData;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixBarcodeAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.assistants.MetrixMessageAssistant;
import com.metrix.architecture.attachment.AttachmentAPIBaseActivity;
import com.metrix.architecture.attachment.AttachmentField;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixControlCase;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixDesignerResourceBaseData;
import com.metrix.architecture.designer.MetrixDesignerResourceData;
import com.metrix.architecture.designer.MetrixDesignerResourceHelpData;
import com.metrix.architecture.designer.MetrixFieldLookupManager;
import com.metrix.architecture.designer.MetrixFieldManager;
import com.metrix.architecture.designer.MetrixFilterSortManager;
import com.metrix.architecture.designer.MetrixGlobalMenuManager;
import com.metrix.architecture.designer.MetrixHomeMenuManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenItemManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.managers.MetrixManagerConstants;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.FSMNotificationContent;
import com.metrix.architecture.metadata.MetrixActivityDef;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.notification.FSMNotificationAssistant;
import com.metrix.architecture.notification.PushRegistrationManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.services.MetrixServiceManager;
import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.services.RemoteMessagesHandler.HandlerException;
import com.metrix.architecture.signature.SignatureField;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuAdapter;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuItem;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuManager;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuResourceData;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.superclasses.MetrixButtonState;
import com.metrix.architecture.superclasses.MetrixControlState;
import com.metrix.architecture.ui.widget.MapWidgetHolder;
import com.metrix.architecture.ui.widget.MapWidgetReadyCallback;
import com.metrix.architecture.ui.widget.MetrixQuickLinksBar;
import com.metrix.architecture.ui.widget.MobileUIHelper;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.Global.MessageType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixBarcodeScanResult;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDate;
import com.metrix.architecture.utilities.MetrixDateTime;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixTime;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.OnCreateMetrixActionViewListner;
import com.metrix.architecture.utilities.OnMetrixActionViewItemClickListner;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;
import com.metrix.metrixmobile.BuildConfig;
import com.metrix.metrixmobile.DebriefOverview;
import com.metrix.metrixmobile.OptionsMenu;
import com.metrix.metrixmobile.Profile;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.oidc.LogoutHandler;
import com.metrix.metrixmobile.policies.MeterReadingsPolicy;
import com.metrix.metrixmobile.survey.SurveyAttachmentQuestion;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import static com.metrix.metrixmobile.global.MobileGlobal.END_SESSION_RELOGIN_CODE;
import static com.metrix.metrixmobile.global.MobileGlobal.END_SESSION_REQUEST_CODE;

@SuppressLint("InflateParams")
public class MetrixActivity extends MetrixBaseActivity implements OnClickListener, OnFocusChangeListener,
		MetrixSlidingMenuAdapter.OnItemClickListener, OnMetrixActionViewItemClickListner,
		OnCreateMetrixActionViewListner {
	public static final int EDIT_FULLTEXT = 100;

	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	protected boolean mIsBound;
	protected IPostMonitor service = null;
	protected boolean mInitializationStarted = false;
	protected Activity mCurrentActivity = null;
	protected boolean mLookupReturn = false;
	protected MetrixFormDef mFormDef;
	protected ViewGroup mLayout;
	protected boolean mHandlingErrors = false;
	protected MetrixActivityDef mActivityDef = null;
	private Bundle mListViewStates;
	private boolean mFormDefined = false;
	protected static String designRefreshDBError;
	private static boolean navigateBack;
	private String itemToPaste = "";
	private int controlToPaste = 0;

	protected DrawerLayout mDrawerLayout;
	protected ActionBarDrawerToggle mDrawerToggle;
	protected LinearLayout mDrawerLinearLayout;
	protected ActionBar mSupportActionBar;
    private RecyclerView mMetrixSlidingMenu;
    protected OnCreateMetrixActionViewListner onCreateMetrixActionViewListner;

	private MetrixActionView mMetrixActionView;
	private HashMap<String, View> newlyAddingViewsMap;
	protected List<ResourceValueObject> resourceStrings;
	private boolean mLogoutReceiverInitialized = false;
	private BroadcastReceiver mLogoutReceiver;
	protected SparseArray<MapWidgetHolder> mapWidgets = new SparseArray<>();

	protected ViewGroup mCoordinatorLayout;
	protected List<FloatingActionButton> mFABList;
	protected List<FloatingActionButton> mFABsToShow;

	protected Handler fabHandler = new Handler();
	protected Runnable fabRunnable;
	protected long fabDelay = 1000;

	protected int mSplitActionBarHeight;

	public void onCreate(Bundle savedInstanceState) {
		resourceStrings = new ArrayList<ResourceValueObject>();

		if (MetrixStringHelper.isNullOrEmpty(MobileApplication.ApplicationNullIfCrashed)) {
			finish();
		}

		if (savedInstanceState != null) {
			//LCS #134373
			if(!navigateBack) {
				MetrixPublicCache.instance.addItem("orientationChange_Occurred", true);
			}
		}

		if (!MetrixPublicCache.instance.containsKey("scriptingControlAliasMap")) {
			cacheScriptingControlAliasMap();
		}

		super.onCreate(savedInstanceState);
		this.registerLogoutAcvitityReceiver();
		MetrixPublicCache.instance.removeItem("multiLookupDefResults");

		if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("HandleError")
				&& this.getIntent().getExtras().getBoolean("HandleError")) {
			mHandlingErrors = true;
		}

		if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey(MetrixActivityDef.METRIX_ACTIVITY_DEF)) {
			mActivityDef = (MetrixActivityDef) this.getIntent().getExtras().get(MetrixActivityDef.METRIX_ACTIVITY_DEF);
		}

		// Setup to get INIT notification from Service
		MetrixServiceManager.setup(this);
		mCurrentActivity = this;
		resourceStrings = new ArrayList<ResourceValueObject>();
		bindService();

		MetrixPublicCache.instance.addItem("magnifying_glass_id", R.drawable.magnifying_glass);
		populateArchitectureStringResources();
		MetrixGlobalMenuManager.populateDesignerGlobalMenuResources();
		populateMetrixCameraResources();
		populateAttachmentAPIResources();
		populateAttachmentHelperResources();
		populateAttachmentFieldResources();
		populateSignaturePadResources();
		populateSignatureFieldResources();
		populateMetrixSlidingMenuResources();
		populateMetrixActionBarResources();
		populateMetrixHyperlinkAttributes();
		populateLocalNotificationResources();

		//The overflow icon only appears on phones that have no menu hardware keys.
		//Phones with menu keys display the action overflow when the user presses the key.
		// @link("http://developer.android.com/design/patterns/actionbar.html")
		MetrixActionBarManager.getInstance().disableMenuButton(this);

		//this screen id will get assigned a value except -1, if the current activity is a code-less screen.
		if (this.getIntent().getExtras() != null) {
			if(this.getIntent().getExtras().containsKey("ScreenID")){
				codeLessScreenId = this.getIntent().getExtras().getInt("ScreenID");
				if(codeLessScreenId > -1){
					codeLessScreenName = MetrixScreenManager.getScreenName(codeLessScreenId);
					//Screen name is cached to be used in MetrixQuickLinksBar to hide the task status button.
					MetrixPublicCache.instance.addItem("currentScreen",codeLessScreenName);
					isCodelessScreen = true;
				}
			}

			//When LIST is in workflow by default -> we won't show the STANDARD linked screen, unless we press ADD/MODIFY
			if(this.getIntent().getExtras().containsKey("ShowListScreenLinkedScreenInTabletUIMode"))
				showListScreenLinkedScreenInTabletUIMode = this.getIntent().getExtras().getBoolean("ShowListScreenLinkedScreenInTabletUIMode");
		}

		//We should run Tablet Specific UI if we meet all the following conditions
		if(isTabletSpecificLandscapeUIRequired() && isTabletRunningInLandscapeMode() && isScreenWithPreviousItemList()){
			shouldRunTabletSpecificUIMode = true;
		}
	}

	@SuppressLint({ "InflateParams", "DefaultLocale", "CutPasteId" })
	public void onStart() {
		super.onStart();

		//Tablet UI Optimization
		setTabletSpecificOptimization();
		//End Tablet UI Optimization

		//Explicitly hides the keyboard since android:windowSoftInputMode="stateHidden" not working,
		//when navigating back to a previous activity with soft-keyboard stays open/visible
		MetrixActivityHelper.explicitlyHideSoftKeyboard(this.getWindow());

		LogManager.getInstance(this).info("{0} onStart()", mCurrentActivity.getLocalClassName());

		mSupportActionBar = MetrixActionBarManager.getInstance().setupActionBar(this, R.layout.action_bar, true);
		String firstGradientText = MetrixSkinManager.getFirstGradientTextColor();

		User user = User.getUser();
		String actionBarTitle = "";
		if (user != null)
			actionBarTitle = AndroidResourceHelper.getMessage("UserGreeting2Args", user.firstName, user.lastName);
		MetrixActionBarManager.getInstance().setupActionBarTitle(this, R.id.action_bar_title, actionBarTitle, firstGradientText);

		mDrawerLinearLayout = (LinearLayout) findViewById(R.id.drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		if(mDrawerLinearLayout != null && mDrawerLayout != null)
			if(mSupportActionBar != null){
				mDrawerToggle = MetrixSlidingMenuManager.getInstance().setUpSlidingDrawer(this, mSupportActionBar, mDrawerLinearLayout, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close, firstGradientText, R.drawable.ellipsis_vertical);
			}

		mMetrixSlidingMenu = findViewById(R.id.recyclerview_sliding_menu);
		if (mMetrixSlidingMenu != null)
			MetrixListScreenManager.setupVerticalRecyclerView(mMetrixSlidingMenu, 0);

		int coordinatorLayoutId = R.id.coordinator_layout;
		if(coordinatorLayoutId != 0 && MetrixPublicCache.instance.getItem("R.id.coordinator_layout") == null)
			MetrixPublicCache.instance.addItem("R.id.coordinator_layout", coordinatorLayoutId);

		MetrixSkinManager.setFirstGradientBackground(findViewById(R.id.row_count_bar), 0);

		String smallIconImageID = MetrixSkinManager.getSmallIconImageID();
		if (!MetrixStringHelper.isNullOrEmpty(smallIconImageID))
			MetrixActionBarManager.getInstance().setActionBarCustomizedIcon(smallIconImageID, getMetrixActionBar(), 24, 24);
//		else
//			MetrixActionBarManager.getInstance().setActionBarDefaultIcon(R.drawable.ifs_logo, getMetrixActionBar(), 24, 24);

		TextView actionBarError = (TextView) findViewById(R.id.action_bar_error);

		if (actionBarError != null) {
			ImageView warningImage = (ImageView) findViewById(R.id.image_warning);
			RelativeLayout splitActionBar = (RelativeLayout) findViewById(R.id.split_action_bar);
			try {
				if (actionBarError != null) {
					AndroidResourceHelper.setResourceValues(actionBarError, "ViewSyncErrors", false);
				}
			} catch (Exception e) {
				LogManager.getInstance().error(e);
			}
			if (MetrixMessageAssistant.errorMessagesExist() || (MetrixMessageAssistant.errorMessagesInExist() && displayInitAndBatchSyncError())) {
				actionBarError.setVisibility(View.VISIBLE);
				warningImage.setVisibility(View.VISIBLE);
				splitActionBar.setVisibility(View.VISIBLE);
				actionBarError.setOnClickListener(this);

				actionBarError.setText(MetrixMessageAssistant.getFirstErrorMessageType());

				// Get the line height and the max line count (should be 2). Also adding general padding for the top and bottom of the sync error bar.
				mSplitActionBarHeight = (actionBarError.getLineHeight() * actionBarError.getMaxLines()) + (2*(int)getResources().getDimension((R.dimen.md_margin)));

			} else {
				actionBarError.setVisibility(View.GONE);
				warningImage.setVisibility(View.GONE);
				splitActionBar.setVisibility(View.GONE);

				mSplitActionBarHeight = 0;
			}
		}

		TextView rowCountText = (TextView) findViewById(R.id.row_count);
		if (rowCountText != null) {
			if (!MetrixStringHelper.isNullOrEmpty(firstGradientText)) {
				rowCountText.setTextColor(Color.parseColor(firstGradientText));
			}
		}

		//Tablet UI optimization is needed & this is the special case where the codeless LIST screen is in Workflow.
		//We should render the codeless STANDARD screen next to it.
		if(isCodelessListScreenInTabletUIMode)
			mLayout = (ViewGroup) findViewById(R.id.table_layout_data);
		else
			//For all the other coded/codeless screens either Tablet UI optimization is needed/not
			mLayout = (ViewGroup) findViewById(R.id.table_layout);

		if (mLookupReturn == false && !MetrixPublicCache.instance.containsKey("onRestart_Occurred")) {
			// If we are not returning from a lookup, and we are not doing an activity restart,
			// do the form setup, value defaulting, scripted refresh event, etc.

			MetrixPublicCache.instance.addItem("initialScreenValuesSet", false);

			if (!mFormDefined)
			{
				defineForm();
				if (mLayout != null && mFormDef != null) {
					MetrixFieldManager.setInflatableLayouts(R.layout.template_layout_textview_checkbox, R.layout.template_layout_textview_textview, R.layout.template_layout_textview_editview, R.layout.template_layout_textview_spinner, R.layout.template_layout_textview_spinnertext, R.layout.template_layout_view_button, R.layout.template_layout_textview_metrixhyperlink,
							R.layout.template_layout_textview_checkbox_textview_checkbox, R.layout.template_layout_textview_checkbox_textview_textview, R.layout.template_layout_textview_checkbox_textview_editview, R.layout.template_layout_textview_checkbox_textview_spinner,R.layout.template_layout_textview_checkbox_textview_spinnertext, R.layout.template_layout_textview_checkbox_view_button, R.layout.template_layout_textview_checkbox_textview_metrixhyperlink,
							R.layout.template_layout_textview_textview_textview_checkbox, R.layout.template_layout_textview_textview_textview_textview, R.layout.template_layout_textview_textview_textview_editview, R.layout.template_layout_textview_textview_textview_spinner, R.layout.template_layout_textview_textview_textview_spinnertext, R.layout.template_layout_textview_textview_view_button, R.layout.template_layout_textview_textview_textview_metrixhyperlink,
							R.layout.template_layout_textview_editview_textview_checkbox, R.layout.template_layout_textview_editview_textview_textview, R.layout.template_layout_textview_editview_textview_editview, R.layout.template_layout_textview_editview_textview_spinner, R.layout.template_layout_textview_editview_textview_spinnertext, R.layout.template_layout_textview_editview_view_button,  R.layout.template_layout_textview_editview_textview_metrixhyperlink,
							R.layout.template_layout_textview_spinner_textview_checkbox, R.layout.template_layout_textview_spinner_textview_textview, R.layout.template_layout_textview_spinner_textview_editview, R.layout.template_layout_textview_spinner_textview_spinner, R.layout.template_layout_textview_spinner_textview_spinnertext, R.layout.template_layout_textview_spinner_view_button, R.layout.template_layout_textview_spinner_textview_metrixhyperlink,
							R.layout.template_layout_textview_spinnertext_textview_checkbox, R.layout.template_layout_textview_spinnertext_textview_textview, R.layout.template_layout_textview_spinnertext_textview_editview, R.layout.template_layout_textview_spinnertext_textview_spinner, R.layout.template_layout_textview_spinnertext_textview_spinnertext, R.layout.template_layout_textview_spinnertext_view_button, R.layout.template_layout_textview_spinnertext_textview_metrixhyperlink,
							R.drawable.textview_background, R.layout.template_spinner_readonly_item,
							R.layout.template_layout_view_button_textview_checkbox, R.layout.template_layout_view_button_textview_textview, R.layout.template_layout_view_button_textview_editview, R.layout.template_layout_view_button_textview_spinner, R.layout.template_layout_view_button_textview_spinnertext, R.layout.template_layout_view_button_view_button, R.layout.template_layout_view_button_textview_metrixhyperlink,
							R.layout.template_layout_textview_metrixhyperlink_textview_checkbox, R.layout.template_layout_textview_metrixhyperlink_textview_textview, R.layout.template_layout_textview_metrixhyperlink_textview_editview, R.layout.template_layout_textview_metrixhyperlink_textview_spinner, R.layout.template_layout_textview_metrixhyperlink_textview_spinnertext, R.layout.template_layout_textview_metrixhyperlink_view_button, R.layout.template_layout_textview_metrixhyperlink_textview_metrixhyperlink,
							R.layout.template_layout_standard_map,
							R.layout.template_layout_textview_attachment, R.layout.template_layout_textview_checkbox_textview_attachment, R.layout.template_layout_textview_textview_textview_attachment, R.layout.template_layout_textview_editview_textview_attachment, R.layout.template_layout_textview_spinner_textview_attachment, R.layout.template_layout_textview_spinnertext_textview_attachment, R.layout.template_layout_view_button_textview_attachment, R.layout.template_layout_textview_metrixhyperlink_textview_attachment,
							R.layout.template_layout_textview_attachment_textview_checkbox, R.layout.template_layout_textview_attachment_textview_textview, R.layout.template_layout_textview_attachment_textview_editview, R.layout.template_layout_textview_attachment_textview_spinner, R.layout.template_layout_textview_attachment_textview_spinnertext, R.layout.template_layout_textview_attachment_view_button, R.layout.template_layout_textview_attachment_textview_metrixhyperlink, R.layout.template_layout_textview_attachment_textview_attachment,
							R.layout.template_layout_textview_attachment_textview_signature,
							R.layout.template_layout_textview_signature, R.layout.template_layout_textview_checkbox_textview_signature, R.layout.template_layout_textview_textview_textview_signature, R.layout.template_layout_textview_editview_textview_signature, R.layout.template_layout_textview_spinner_textview_signature, R.layout.template_layout_textview_spinnertext_textview_signature, R.layout.template_layout_view_button_textview_signature, R.layout.template_layout_textview_metrixhyperlink_textview_signature,
							R.layout.template_layout_textview_signature_textview_checkbox, R.layout.template_layout_textview_signature_textview_textview, R.layout.template_layout_textview_signature_textview_editview, R.layout.template_layout_textview_signature_textview_spinner, R.layout.template_layout_textview_signature_textview_spinnertext, R.layout.template_layout_textview_signature_view_button, R.layout.template_layout_textview_signature_textview_metrixhyperlink, R.layout.template_layout_textview_signature_textview_signature,
							R.layout.template_layout_textview_signature_textview_attachment);

					final boolean shouldAddToCollection = !(this instanceof MetrixTabActivity);
					if(isCodelessScreen){
						//Tablet UI optimization is needed & this is the special case where the codeless LIST screen is in Workflow.
						//We should render the codeless STANDARD screen next to it.
						if(isCodelessListScreenInTabletUIMode) {
							MetrixFieldManager.addFieldsToScreen(this, mLayout, mFormDef, linkedScreenIdInTabletUIMode);
						} else {
							//Other codeless screens
							if (showListScreenLinkedScreenInTabletUIMode) {
								HashMap<String, String> currentScreenProperties = MetrixScreenManager.getScreenProperties(codeLessScreenId);
								//Check whether the current screen has a linked screen
								if (currentScreenProperties.containsKey("linked_screen_id")) {
									String strLinkedScreenId = currentScreenProperties.get("linked_screen_id");
									if (!MetrixStringHelper.isNullOrEmpty(strLinkedScreenId)) {
										//get the linked screen information
										int linkedScreenId = Integer.valueOf(strLinkedScreenId);
										MetrixFieldManager.addFieldsToScreen(this, mLayout, mFormDef, linkedScreenId);
										loadMapIfNeeded(linkedScreenId, mLayout, shouldAddToCollection);
									}
								}
							} else {
								MetrixFieldManager.addFieldsToScreen(this, mLayout, mFormDef, codeLessScreenId);
								loadMapIfNeeded(codeLessScreenId, mLayout, shouldAddToCollection);
							}
						}
					} else {
						if(isCodelessListScreenInTabletUIMode)
							MetrixFieldManager.addFieldsToScreen(this, mLayout, mFormDef, linkedScreenIdInTabletUIMode);
						else {
							if(showListScreenLinkedScreenInTabletUIMode){
								String activityName = standardActivityFullNameInTabletUIMode;
								//Keep the information of associate STANDARD screen
								if(!MetrixStringHelper.isNullOrEmpty(activityName))
								{
									linkedScreenIdInTabletUIMode = MetrixScreenManager.getScreenId(activityName);
									if(linkedScreenIdInTabletUIMode > -1) {
										MetrixFieldManager.addFieldsToScreen(this, mLayout, mFormDef, linkedScreenIdInTabletUIMode);
										loadMapIfNeeded(linkedScreenIdInTabletUIMode, mLayout, shouldAddToCollection);
									}
								}
							}
							else {
								MetrixFieldManager.addFieldsToScreen(this, mLayout, mFormDef);
								final int screenId = MetrixScreenManager.getScreenId(this);
								loadMapIfNeeded(screenId, mLayout, shouldAddToCollection);
							}
						}
					}

					if ("Y".equalsIgnoreCase(MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_BARCODE_SCANNING'")) &&
							getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
						wireupEditTextsForBarcodeScanning(mLayout);
					}
				}
				setupDateTimePickers();
				mFormDefined = true;
			}

			FloatingActionButton correctErrorsButton = (FloatingActionButton) findViewById(R.id.correct_error);
			if(correctErrorsButton != null)
				correctErrorsButton.setContentDescription("btnCorrect_error");
			FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
			if(addButton != null)
				addButton.setContentDescription("btnSave");
			FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.next);
			if(nextButton != null)
				nextButton.setContentDescription("btnNext");

			if (mFABList == null)
				mFABList = new ArrayList<FloatingActionButton>();

			if (mHandlingErrors) {
				for (MetrixTableDef tableDef : this.mFormDef.tables) {
					if (tableDef.tableName.compareToIgnoreCase(MobileGlobal.mErrorInfo.tableName) == 0) {
						tableDef.transactionType = MetrixTransactionTypes.CORRECT_ERROR;
						Iterator<String> iterator = MobileGlobal.mErrorInfo.primaryKeys.keySet().iterator();

						if (tableDef.constraints != null && tableDef.constraints.size() > 0) {
							tableDef.constraints.clear();
						}

						while (iterator.hasNext()) {
							String columnName = iterator.next();
							String columnValue = MobileGlobal.mErrorInfo.primaryKeys.get(columnName);
							tableDef.constraints.add(new MetrixConstraintDef(columnName, MetrixConstraintOperands.EQUALS, columnValue, String.class));
						}
					}
				}
				if (correctErrorsButton != null) {
					MetrixControlAssistant.setButtonVisibility(correctErrorsButton, View.VISIBLE);
					mFABList.add(correctErrorsButton);
					correctErrorsButton.setOnClickListener(this);
				}
				if (nextButton != null) {
					MetrixControlAssistant.setButtonVisibility(nextButton, View.GONE);
				}
				if (addButton != null) {
					MetrixControlAssistant.setButtonVisibility(addButton, View.GONE);
				}
			} else {
				if (correctErrorsButton != null) {
					MetrixControlAssistant.setButtonVisibility(correctErrorsButton, View.GONE);
				}
				if (nextButton != null) {
					MetrixControlAssistant.setButtonVisibility(nextButton, View.VISIBLE);
					mFABList.add(nextButton);
				}
				if (addButton != null) {
					MetrixControlAssistant.setButtonVisibility(addButton, View.VISIBLE);
					mFABList.add(addButton);
				}
			}

			MetrixFormManager.setupForm(this, mLayout, this.mFormDef);
			if(launchingSignatureField != null && (launchingSignatureField.metrixColumnDef != null || !MetrixStringHelper.isNullOrEmpty(launchingSignatureField.getSurveyQuestionId()))) {
				HashMap<String, Object> signatureFieldValue = new HashMap<>();
				if(launchingSignatureField.metrixColumnDef != null)
					signatureFieldValue.put("MetrixColumnDef", launchingSignatureField.metrixColumnDef);
				if(!MetrixStringHelper.isNullOrEmpty(launchingSignatureField.getSurveyQuestionId()))
					signatureFieldValue.put("QuestionId", launchingSignatureField.getSurveyQuestionId());
				signatureFieldValue.put("AttachmentId", launchingSignatureField.mHiddenAttachmentIdTextView.getText().toString());
				MetrixPublicCache.instance.addItem("LaunchingSignatureFieldValue", signatureFieldValue);
			}
			populateNonStandardControls();

			//checking whether there's a tab associated with the current activity
			if(!MetrixScreenManager.isTabAssociated(mLayout)){
				if(isCodelessScreen){
					//Tablet UI optimization is needed & this is the special case where the codeless LIST screen is in Workflow.
					//Label, Tip will be displayed in the center screen/codeless LIST screen.
					if(isCodelessListScreenInTabletUIMode){
						if(showListScreenLinkedScreenInTabletUIMode)
						{
							ViewGroup helpTiplayout = (ViewGroup) findViewById(R.id.table_layout_data);
							this.helpText = MetrixScreenManager.setLabelTipAndHelp(helpTiplayout, linkedScreenIdInTabletUIMode);
							ViewGroup hideListHelpTiplayout = (ViewGroup) findViewById(R.id.table_layout);
							if(hideListHelpTiplayout != null){
								View labelView = hideListHelpTiplayout.findViewWithTag("SCREEN_LABEL");
								if(labelView != null)labelView.setVisibility(View.GONE);
								View tipView = hideListHelpTiplayout.findViewWithTag("SCREEN_TIP");
								if(tipView != null)tipView.setVisibility(View.GONE);
							}

							ViewGroup panelTwoLayout = (ViewGroup) findViewById(R.id.table_layout_parent);
							if(panelTwoLayout != null){
								View splitActionBar = panelTwoLayout.findViewById(R.id.split_action_bar);
								if(splitActionBar != null)
									splitActionBar.setVisibility(View.GONE);
							}
						}
						else{
							ViewGroup helpTiplayout = (ViewGroup) findViewById(R.id.table_layout);
							this.helpText = MetrixScreenManager.setLabelTipAndHelp(helpTiplayout, codeLessScreenId);
						}
					}
					else {
						//For all the other codeless screens
						if(showListScreenLinkedScreenInTabletUIMode){
							HashMap<String, String> currentScreenProperties = MetrixScreenManager.getScreenProperties(codeLessScreenId);
							//Check whether the current screen has a linked screen
							if(currentScreenProperties.containsKey("linked_screen_id")){
								String strLinkedScreenId = currentScreenProperties.get("linked_screen_id");
								if(!MetrixStringHelper.isNullOrEmpty(strLinkedScreenId)){
									//get the linked screen information
									int linkedScreenId = Integer.valueOf(strLinkedScreenId);
									this.helpText = MetrixScreenManager.setLabelTipAndHelp(mLayout, linkedScreenId);
								}
							}
						}
						else
							this.helpText = MetrixScreenManager.setLabelTipAndHelp(mLayout, codeLessScreenId);
					}
				}
				else{
					if(isCodelessListScreenInTabletUIMode){
						if(showListScreenLinkedScreenInTabletUIMode)
						{
							ViewGroup helpTiplayout = (ViewGroup) findViewById(R.id.table_layout_data);
							this.helpText = MetrixScreenManager.setLabelTipAndHelp(helpTiplayout, linkedScreenIdInTabletUIMode);
							ViewGroup hideListHelpTiplayout = (ViewGroup) findViewById(R.id.table_layout);
							if(hideListHelpTiplayout != null){
								View labelView = hideListHelpTiplayout.findViewWithTag("SCREEN_LABEL");
								if(labelView != null)labelView.setVisibility(View.GONE);
								View tipView = hideListHelpTiplayout.findViewWithTag("SCREEN_TIP");
								if(tipView != null)tipView.setVisibility(View.GONE);
							}

							ViewGroup panelTwoLayout = (ViewGroup) findViewById(R.id.table_layout_parent);
							if(panelTwoLayout != null){
								View splitActionBar = panelTwoLayout.findViewById(R.id.split_action_bar);
								if(splitActionBar != null)
									splitActionBar.setVisibility(View.GONE);
							}
						}
						else{
							ViewGroup helpTiplayout = (ViewGroup) findViewById(R.id.table_layout);
							this.helpText = MetrixScreenManager.setLabelTipAndHelp(this, helpTiplayout);
						}
					} else {
						if(showListScreenLinkedScreenInTabletUIMode){
							String activityName = standardActivityFullNameInTabletUIMode;
							//Keep the information of associate STANDARD screen
							if(!MetrixStringHelper.isNullOrEmpty(activityName))
							{
								linkedScreenIdInTabletUIMode = MetrixScreenManager.getScreenId(activityName);
								if(linkedScreenIdInTabletUIMode > -1)
									this.helpText = MetrixScreenManager.setLabelTipAndHelp(mLayout, linkedScreenIdInTabletUIMode);
							}
						}
						else
							this.helpText = MetrixScreenManager.setLabelTipAndHelp(this, mLayout);
					}
				}
			}

			if (!mHandlingErrors) {
				//Note:defaultValues methods shouldn't be executed for code-less screens
				if (isCodelessScreen) {
					//Tablet UI optimization is needed & this is the special case where the codeless LIST screen is in Workflow.
					if (isCodelessListScreenInTabletUIMode)
						MetrixFieldManager.defaultValues(this, mFormDef, mLayout, linkedScreenIdInTabletUIMode);
					else
						//For all the other codeless screens
						MetrixFieldManager.defaultValues(this, mFormDef, mLayout, codeLessScreenId);
				} else {
					MetrixFieldManager.defaultValues(this, mFormDef, mLayout);
					defaultValues();
				}
			}

			activateMapsIfAny();

			//setupDateTimePickers();
			float scale = getResources().getDisplayMetrics().density;
			float btnCornerRadius = 4f * scale + 0.5f;
			setSkinBasedColorsOnRelevantControls(mLayout, MetrixSkinManager.getPrimaryColor(), MetrixSkinManager.getSecondaryColor(),
					MetrixSkinManager.getHyperlinkColor(), mLayout, true);
			//Tablet UI optimization is needed & this is the special case where the codeless LIST screen is in Workflow.
			if(isCodelessListScreenInTabletUIMode){
				//Applying skin effects for screen label, tip in codeless list screen in tablet landscape mode
				ViewGroup labelTiplayout = (ViewGroup) findViewById(R.id.table_layout);
				setSkinBasedColorsOnRelevantControls(labelTiplayout, MetrixSkinManager.getPrimaryColor(), MetrixSkinManager.getSecondaryColor(),
						MetrixSkinManager.getHyperlinkColor(), labelTiplayout, true);
			}
			setSkinBasedColorsOnButtons(((ViewGroup)findViewById(android.R.id.content)).getChildAt(0), MetrixSkinManager.getTopFirstGradientColor(), MetrixSkinManager.getBottomFirstGradientColor(),
					MetrixSkinManager.getFirstGradientTextColor(), btnCornerRadius, this);
			setListeners();

			boolean stopExec = runScreenRefreshScriptEvent();
			if (stopExec)
				return;

			cacheOnStartValues();

			Handler handler = new Handler(Looper.getMainLooper());
			handler.postDelayed(new Runnable() {
				public void run() {
					MetrixPublicCache.instance.addItem("initialScreenValuesSet", true);
				}
			}, 500);
		} else if (mLookupReturn == false && MetrixPublicCache.instance.containsKey("backButtonPress_Occurred") && !MetrixPublicCache.instance.containsKey("goingBackFromLookup")) {
			// If we are not returning from a lookup, but we are navigating back to this screen (one of many ways onRestart is called),
			// execute the screen refresh script event.
			boolean stopExec = runScreenRefreshScriptEvent();
			if (stopExec)
				return;
		} else {
			// it is returning from a lookup, rebind MetrixActionView for Barcode
			if (mLayout != null && "Y".equalsIgnoreCase(MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_BARCODE_SCANNING'")) &&
					getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
				wireupEditTextsForBarcodeScanning(mLayout);
			}
		}
		mLookupReturn = false;
		if (!(this instanceof Lookup))
			toggleOrientationLock(false);
		MetrixPublicCache.instance.removeItem("goingBackFromLookup");
		MetrixPublicCache.instance.removeItem("onRestart_Occurred");
		MetrixPublicCache.instance.removeItem("backButtonPress_Occurred");

		designRefreshDBError = (AndroidResourceHelper.getMessage("RefreshCustomDesignDBFail"));

		this.setOnCreateMetrixActionViewListner(this);

		// Ensure these cache items exist for client script execution when coming back from Attachment API
		if (!(this instanceof Lookup)) {
			MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
			MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);

			// Restore original values cache, if we've come back from Attachment Field usage
			if (MetrixPublicCache.instance.containsKey("originalValuesForAttachmentField")) {
				HashMap<String, String> originalValuesForAttachmentField = (HashMap) MetrixPublicCache.instance.getItem("originalValuesForAttachmentField");
				MetrixPublicCache.instance.addItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES, originalValuesForAttachmentField);
				MetrixPublicCache.instance.removeItem("originalValuesForAttachmentField");
			}
		}

		launchingSignatureField = null;

		AndroidResourceHelper.setResourceValues(mCurrentActivity, resourceStrings);
		if (MobileApplication.serverSidePasswordChangeOccurredInBackground()) {
			MetrixDatabaseManager.executeSql("update user_credentials set hidden_chg_occurred = null");
			handleServerPasswordChange();
			return;
		}
	}

	//Converted 585
	@SuppressLint("DefaultLocale")
	private boolean runScreenRefreshScriptEvent() {
		boolean stopExecution = false;

		// don't cache mFormDef/mLayout if we are on the Lookup activity itself,
		// since we want to retain mFormDef/mLayout from the launching activity
		if (!(this instanceof Lookup)) {
			MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
			MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
		}

		// don't run refresh screen event script, if we've just rotated the device
		if (!MetrixPublicCache.instance.containsKey("orientationChange_Occurred")) {
			int thisScreenID = -1;
			if (this.isCodelessScreen)
				thisScreenID = this.codeLessScreenId;
			else
				thisScreenID = MetrixScreenManager.getScreenId(this.getClass().getSimpleName());

			//This is when the device is in either PORTRAIT mode/LANDSCAPE mode without Tablet UI optimization.
			if(!shouldRunTabletSpecificUIMode){
				if (!(this instanceof Lookup)) {
					MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
					MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
				}
				Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(thisScreenID));
				if (result instanceof String) {
					String strResult = (String)result;
					stopExecution = (strResult == "STOP_EXECUTION");
				}
			}
			//LANDSCAPE mode with Tablet UI optimization
			else{
				//Codeless screen
				if(isCodelessScreen){
					//Scenario : codeless LIST screen in Workflow/middle/second panel, LINKED SCREEN is a STANDARD screen/third panel.
					if(isCodelessListScreenInTabletUIMode){
						//List screen
						if(!showListScreenLinkedScreenInTabletUIMode){
							ViewGroup listLayout = (ViewGroup) findViewById(R.id.table_layout);
							MetrixPublicCache.instance.addItem("theCurrentLayout", listLayout);
							MetrixPublicCache.instance.removeItem("theCurrentFormDef");
							Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(thisScreenID));
							if (!stopExecution && result instanceof String) {
								String strResult = (String)result;
								stopExecution = (strResult == "STOP_EXECUTION");
							}
						}

						if (linkedScreenAvailableInTabletUIMode && showListScreenLinkedScreenInTabletUIMode) {
							//Standard screen
							MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
							MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
							Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(linkedScreenIdInTabletUIMode));
							if (!stopExecution && result instanceof String) {
								String strResult = (String)result;
								stopExecution = (strResult == "STOP_EXECUTION");
							}
						}
					}
					//Scenario : codeless STANDARD screen in Workflow/middle/second panel, LINKED SCREEN is a LIST screen/third panel.
					else{
						//Standard screen
						MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
						MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
						Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(thisScreenID));
						if (!stopExecution && result instanceof String) {
							String strResult = (String)result;
							stopExecution = (strResult == "STOP_EXECUTION");
						}

						if (linkedScreenAvailableInTabletUIMode) {
							//List screen
							ViewGroup listLayout = (ViewGroup) findViewById(R.id.table_layout_list);
							MetrixPublicCache.instance.addItem("theCurrentLayout", listLayout);
							MetrixPublicCache.instance.removeItem("theCurrentFormDef");
							Object result2 = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(linkedScreenIdInTabletUIMode));
							if (!stopExecution && result2 instanceof String) {
								String strResult = (String)result2;
								stopExecution = (strResult == "STOP_EXECUTION");
							}
						}
					}
				}
				//Coded screen
				else{
					if (!(this instanceof Lookup)) {
						final String screenType = MetrixScreenManager.getScreenType(thisScreenID);
						if (screenType != null) {
							if (screenType.toLowerCase().contains("list")) {
								//Scenario : coded LIST screen in Workflow/middle/second panel, LINKED SCREEN is a STANDARD screen/third panel.
								//List screen
								if (!showListScreenLinkedScreenInTabletUIMode) {
									ViewGroup listLayout = (ViewGroup) findViewById(R.id.table_layout);
									MetrixPublicCache.instance.addItem("theCurrentLayout", listLayout);
									MetrixPublicCache.instance.removeItem("theCurrentFormDef");
									Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(thisScreenID));
									if (!stopExecution && result instanceof String) {
										String strResult = (String) result;
										stopExecution = (strResult == "STOP_EXECUTION");
									}
								}

								if (linkedScreenAvailableInTabletUIMode && showListScreenLinkedScreenInTabletUIMode) {
									//Standard screen
									MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
									MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
									Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(linkedScreenIdInTabletUIMode));
									if (!stopExecution && result instanceof String) {
										String strResult = (String) result;
										stopExecution = (strResult == "STOP_EXECUTION");
									}
								}
							} else if (screenType.toLowerCase().contains("standard")) {
								//Scenario : coded STANDARD screen is in Workflow/middle/second panel, LINKED SCREEN is a LIST screen/third panel.
								//Standard screen
								MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
								MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
								Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(thisScreenID));
								if (!stopExecution && result instanceof String) {
									String strResult = (String) result;
									stopExecution = (strResult == "STOP_EXECUTION");
								}

								if (linkedScreenAvailableInTabletUIMode) {
									//List screen
									ViewGroup listLayout = (ViewGroup) findViewById(R.id.table_layout_list);
									MetrixPublicCache.instance.addItem("theCurrentLayout", listLayout);
									MetrixPublicCache.instance.removeItem("theCurrentFormDef");
									Object result2 = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(linkedScreenIdInTabletUIMode));
									if (!stopExecution && result2 instanceof String) {
										String strResult = (String) result2;
										stopExecution = (strResult == "STOP_EXECUTION");
									}
								}
							}
						}
					}
				}

				//Reseting the MetrixPublicCache similar to when in PORTRAIT mode.
				MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
				MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
			}
		}
		MetrixPublicCache.instance.removeItem("orientationChange_Occurred");

		return stopExecution;
	}

	@SuppressLint("UseSparseArrays")
	private void cacheScriptingControlAliasMap() {
		HashMap<String, Integer> scriptingControlAliasMap = new HashMap<String, Integer>();
		HashMap<Integer, String> scriptingScreenItemAliasMap = new HashMap<Integer, String>();
		int addButtonID = R.id.save;
		int nextButtonID = R.id.next;
		int saveButtonID = R.id.update;
		int correctErrorButtonID = R.id.correct_error;
		scriptingControlAliasMap.put("BUTTON_ADD", addButtonID);
		scriptingControlAliasMap.put("BUTTON_NEXT", nextButtonID);
		scriptingControlAliasMap.put("BUTTON_SAVE", saveButtonID);
		scriptingControlAliasMap.put("BUTTON_LIST", R.id.view_previous_entries);
		scriptingControlAliasMap.put("QUICK_ACTIONBAR_STATUS", R.id.taskStatusButton);
		scriptingControlAliasMap.put("QUICK_ACTIONBAR_OVERVIEW", R.id.homeButton);
		scriptingControlAliasMap.put("QUICK_ACTIONBAR_ATTACHMENT", R.id.attachmentsButton);
		scriptingControlAliasMap.put("QUICK_ACTIONBAR_NOTES", R.id.notesButton);

		// also, be sure to include the badges
		// ... as making these action bar items invisible requires reference to both controls
		scriptingControlAliasMap.put("QUICK_ACTIONBAR_ATTACHMENT_BADGE", R.id.attachmentsBadge);
		scriptingControlAliasMap.put("QUICK_ACTIONBAR_NOTES_BADGE", R.id.notesBadge);

		// for screen items, we need to key on the view ID, and we only provide for three buttons (but we will add provision for error correction as a save button)
		scriptingScreenItemAliasMap.put(addButtonID, "BUTTON_ADD");
		scriptingScreenItemAliasMap.put(nextButtonID, "BUTTON_NEXT");
		scriptingScreenItemAliasMap.put(saveButtonID, "BUTTON_SAVE");
		scriptingScreenItemAliasMap.put(correctErrorButtonID, "BUTTON_SAVE");

		MetrixPublicCache.instance.addItem("scriptingControlAliasMap", scriptingControlAliasMap);
		MetrixPublicCache.instance.addItem("scriptingScreenItemAliasMap", scriptingScreenItemAliasMap);
	}

	protected boolean scriptEventConsumesListTap(Activity activity, View v, int screenId){
		ClientScriptDef tapEventScriptDef = MetrixScreenManager.getTapEventScriptDef(screenId);
		if (tapEventScriptDef == null)
			return false;

		MetrixPublicCache.instance.addItem("CurrentListRowContainer", v);
		MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), tapEventScriptDef);
		MetrixPublicCache.instance.removeItem("CurrentListRowContainer");
	    return true;
    }

	protected boolean scriptEventConsumesClick(Activity activity, View v) {
		boolean isConsumed = false;
		try {
			ClientScriptDef scriptDef = null;
			if (!shouldRunTabletSpecificUIMode) {
				if (isCodelessScreen)
					scriptDef = MetrixScreenItemManager.getEventForView(v, codeLessScreenId);
				else
					scriptDef = MetrixScreenItemManager.getEventForView(activity, v);

				if (scriptDef != null) {
					MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
					MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
					boolean success = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), scriptDef);
					if ((success && !scriptDef.mIsValidation) || !success)
						isConsumed = true;
				}
			} else {
				if (isCodelessScreen) {
					if (isCodelessListScreenInTabletUIMode) {
						//List screen
						scriptDef = MetrixScreenItemManager.getEventForView(v, codeLessScreenId);
						if (scriptDef != null) {
							ViewGroup listLayout = (ViewGroup) findViewById(R.id.table_layout);
							MetrixPublicCache.instance.addItem("theCurrentLayout", listLayout);
							MetrixPublicCache.instance.removeItem("theCurrentFormDef");
							boolean success = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), scriptDef);
							if ((success && !scriptDef.mIsValidation) || !success)
								isConsumed = true;
						}
					} else {
						//Standard screen
						scriptDef = MetrixScreenItemManager.getEventForView(v, codeLessScreenId);
						if (scriptDef != null) {
							MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
							MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
							boolean success = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), scriptDef);
							if ((success && !scriptDef.mIsValidation) || !success)
								isConsumed = true;
						}
					}

					MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
					MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
				} else {
					int currentScreenId = MetrixScreenManager.getScreenId(this.getClass().getSimpleName());
					String screenType = MetrixScreenManager.getScreenType(currentScreenId);
					if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD")) {
						scriptDef = MetrixScreenItemManager.getEventForView(v, currentScreenId);
						if (scriptDef != null) {
							MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
							MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
							boolean success = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), scriptDef);
							if ((success && !scriptDef.mIsValidation) || !success)
								isConsumed = true;
						}
					} else {
						//List screen
						scriptDef = MetrixScreenItemManager.getEventForView(v, linkedScreenIdInTabletUIMode);
						if (scriptDef != null) {
							ViewGroup listLayout = (ViewGroup) findViewById(R.id.table_layout_list);
							MetrixPublicCache.instance.addItem("theCurrentLayout", listLayout);
							boolean success = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), scriptDef);
							if ((success && !scriptDef.mIsValidation) || !success)
								isConsumed = true;
						}
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			isConsumed = true;
		}

		return isConsumed;
	}

	protected void setupDateTimePickers() {
			if (this.mFormDef != null && this.mFormDef.tables != null) {
			for (MetrixTableDef tableDef : this.mFormDef.tables) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					View view = MetrixControlAssistant.getControl(columnDef.id, mLayout);

					if (view != null && (columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixDateTime.class || columnDef.dataType == MetrixTime.class) && view instanceof EditText) {
						EditText editText = (EditText)view;

						if(editText.isEnabled()) {
							editText.setOnClickListener(this);
							editText.setOnFocusChangeListener(this);
							editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.calendar, 0);
						}
					}
				}
			}
		}
	}

	public static void setSkinBasedColorsOnRelevantControls(ViewGroup group, String primaryColor, String secondaryColor,
															String hyperlinkColor, ViewGroup initialGroup, boolean doParent) {
		if (!MetrixStringHelper.isNullOrEmpty(primaryColor) || !MetrixStringHelper.isNullOrEmpty(secondaryColor)
				|| !MetrixStringHelper.isNullOrEmpty(hyperlinkColor)) {
			if (group != null && group.getChildCount() > 0) {
				for (int i = 0; i < group.getChildCount(); i++) {
					View v = group.getChildAt(i);
					if (v != null && v instanceof TextView) {
						TextView tv = (TextView) v;
						String tag = (tv.getTag() != null) ? tv.getTag().toString() : "";
						if (!MetrixStringHelper.isNullOrEmpty(primaryColor)
								&& (MetrixStringHelper.valueIsEqual(tag, "SCREEN_LABEL")
								|| MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Title"))) {
							tv.setTextColor(Color.parseColor(primaryColor));
						} else if (!MetrixStringHelper.isNullOrEmpty(secondaryColor)
								&& MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Heading")) {
							tv.setTextColor(Color.parseColor(secondaryColor));
						} else if (!MetrixStringHelper.isNullOrEmpty(secondaryColor)
								&& MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Region")) {
							tv.setBackgroundColor(Color.parseColor(secondaryColor));
						} else if (!MetrixStringHelper.isNullOrEmpty(hyperlinkColor) && tv.getAutoLinkMask() > 0) {
							tv.setLinkTextColor(Color.parseColor(hyperlinkColor));
						}
					} else if (v != null && v instanceof ViewGroup && v != initialGroup) {
						setSkinBasedColorsOnRelevantControls((ViewGroup)v, primaryColor, secondaryColor, hyperlinkColor, initialGroup, false);
					}
				}
			}

			if (doParent && group != null) {
				ViewParent parent = group.getParent();
				if (parent != null && parent instanceof ViewGroup && parent != initialGroup) {
					setSkinBasedColorsOnRelevantControls((ViewGroup)parent, primaryColor, secondaryColor, hyperlinkColor, initialGroup, false);
				}
			}
		}
	}

	public static void setSkinBasedColorsOnButtons(View parent, String firstGradient1, String firstGradient2, String firstGradientText, float btnCornerRadius, Activity activity) {
		boolean someFirstGradientInformationExists = (!MetrixStringHelper.isNullOrEmpty(firstGradient1)
				|| !MetrixStringHelper.isNullOrEmpty(firstGradient2)
				|| !MetrixStringHelper.isNullOrEmpty(firstGradientText));

		if (parent != null && parent instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) parent;
			if (group != null && group.getChildCount() > 0) {
				for (int i = 0; i < group.getChildCount(); i++) {
					View v = group.getChildAt(i);
					if (v != null && v instanceof Button) {
						Button btn = (Button) v;
						String btnTag = (btn.getTag() != null) ? btn.getTag().toString() : "";
						String primaryColor = MetrixSkinManager.getPrimaryColor();
						if (MetrixStringHelper.valueIsEqual(btnTag, "ButtonBase.Normal.ActionBar")) {
							if (someFirstGradientInformationExists)
                                setMaterialDesignForButtons(btn,primaryColor, activity);
							else {
								setMaterialDesignForButtons(btn,primaryColor, activity);
//								btn.setBackgroundTintList(ContextCompat.getColorStateList(parent.getContext(), R.color.IFSPurple));
//								if (Build.VERSION.SDK_INT >=23)
//								    btn.setForeground(ContextCompat.getDrawable(parent.getContext(), R.drawable.light_ripple));
//								btn.setTextAppearance(activity, R.style.ButtonBase_Normal_ActionBar);
							}
						}
					} else if (v != null && v instanceof FloatingActionButton) {
						FloatingActionButton fab = (FloatingActionButton) v;
						String btnTag = (fab.getTag() != null) ? fab.getTag().toString() : "";
						if (MetrixStringHelper.valueIsEqual(btnTag, "FloatingActionBtnBase")) {
							setSkinBasedColorsOnFloatingActionButtons(fab, MetrixSkinManager.getPrimaryColor(), activity);
						}
					}else if (v != null && v instanceof ViewGroup)
						setSkinBasedColorsOnButtons(v, firstGradient1, firstGradient2, firstGradientText, btnCornerRadius, activity);
				}
			}
		}
	}

    public static void setMaterialDesignForButtons(Button btn,String buttonBackgroundColor, Activity activity){
		int buttonBackgroundColorInt;
		if (!MetrixStringHelper.isNullOrEmpty(buttonBackgroundColor)) {
			buttonBackgroundColor = buttonBackgroundColor.substring(1); // Get rid of the # character at the beginning
			buttonBackgroundColor = MetrixStringHelper.isNullOrEmpty(buttonBackgroundColor) ? "FFFFFF" : buttonBackgroundColor;
			buttonBackgroundColorInt = (int) Long.parseLong(buttonBackgroundColor, 16);
		} else {
			buttonBackgroundColorInt = activity.getBaseContext().getResources().getColor(R.color.IFSPurple);
			buttonBackgroundColor = String.format("%06X", (0xFFFFFF & buttonBackgroundColorInt));
		}

		String disabledColor = MetrixSkinManager.generateLighterVersionOfColor(buttonBackgroundColor, 0.4f, false);
		int disabledColorInt = (int)Long.parseLong(disabledColor, 16);
		int[] colors = new int[2];
		colors[0] = Color.rgb((buttonBackgroundColorInt >> 16) & 0xFF, (buttonBackgroundColorInt >> 8) & 0xFF, (buttonBackgroundColorInt >> 0) & 0xFF);
		colors[1] = Color.rgb((disabledColorInt >> 16) & 0xFF, (disabledColorInt >> 8) & 0xFF, (disabledColorInt >> 0) & 0xFF);
		int[][] states = new int[][] {
				new int[] { android.R.attr.state_enabled}, // enabled
				new int[] {-android.R.attr.state_enabled} // disabled
		};
		int[] stateColors = new int[] {
				colors[0],
				colors[1]
		};
		ColorStateList csl = new ColorStateList(states, stateColors);
		btn.setTextAppearance(activity, R.style.ButtonBase_Normal_ActionBar);
		btn.setBackgroundTintList(csl);
    }

    public static boolean isColorDark(int color){
		return ColorUtils.calculateLuminance(color) < 0.5;
    }

    public static void setSkinBasedColorsOnFloatingActionButtons(FloatingActionButton fab, String primaryColor, Activity activity) {
		String btnTag = (fab.getTag() != null) ? fab.getTag().toString() : "";
		if (MetrixStringHelper.valueIsEqual(btnTag, "FloatingActionBtnBase")) {
			int primaryColorInt;
			if (!MetrixStringHelper.isNullOrEmpty(primaryColor)) {
				primaryColor = primaryColor.substring(1); // Get rid of the # character at the beginning
				primaryColor = MetrixStringHelper.isNullOrEmpty(primaryColor) ? "FFFFFF" : primaryColor;
				primaryColorInt = (int) Long.parseLong(primaryColor, 16);
			} else {
				primaryColorInt = activity.getBaseContext().getResources().getColor(R.color.IFSPurple);
				primaryColor = String.format("%06X", (0xFFFFFF & primaryColorInt));
			}

			String disabledColor = MetrixSkinManager.generateLighterVersionOfColor(primaryColor, 0.4f, false);
			int disabledColorInt = (int)Long.parseLong(disabledColor, 16);

			int[] colors = new int[2];
			colors[0] = Color.rgb((primaryColorInt >> 16) & 0xFF, (primaryColorInt >> 8) & 0xFF, (primaryColorInt >> 0) & 0xFF);
			colors[1] = Color.rgb((disabledColorInt >> 16) & 0xFF, (disabledColorInt >> 8) & 0xFF, (disabledColorInt >> 0) & 0xFF);

			int[][] states = new int[][] {
					new int[] { android.R.attr.state_enabled}, // enabled
					new int[] {-android.R.attr.state_enabled} // disabled
			};

			int[] stateColors = new int[] {
					colors[0],
					colors[1]
			};

			ColorStateList csl = new ColorStateList(states, stateColors);
			fab.setBackgroundTintList(csl);
		}
    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		return super.dispatchTouchEvent(ev);
	}

	@Override
	protected void onPause() {
		// Save all list view positions
		if (mLayout != null) {
			mListViewStates = new Bundle();
			saveListViewStates(mLayout, mListViewStates);
		}

		super.onPause();
	}

	public void onStop() {
		super.onStop();
		if (!(this instanceof SyncServiceMonitor) || !mUIHelper.isDialogActive())
			this.unbindService();
	}

	public void onRestart() {
		super.onRestart();

		if (!(this instanceof SyncServiceMonitor) || !mUIHelper.isDialogActive())
			this.bindService();

		// only note that restart occurred if it is NOT being caused by popping the activity back stack
		// but do allow onRestart-related behavior if we are coming back from a Lookup activity
		if (!MetrixPublicCache.instance.containsKey("backButtonPress_Occurred"))
			MetrixPublicCache.instance.addItem("onRestart_Occurred", true);
		else if (MetrixPublicCache.instance.containsKey("goingBackFromLookup")) {
			MetrixPublicCache.instance.addItem("onRestart_Occurred", true);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		int currentScreenId;
		String currentActivityName = "";
		if (isCodelessScreen) {
			currentActivityName = codeLessScreenName;
			currentScreenId = codeLessScreenId;
		} else {
			currentActivityName = mCurrentActivity.getClass().getSimpleName();
			currentScreenId = MetrixScreenManager.getScreenId(currentActivityName);
		}

		if (currentScreenId > 0 && mFormDef != null && mFormDef.tables.size() > 0) {
			for (MetrixTableDef table : mFormDef.tables) {
				for (MetrixColumnDef col : table.columns) {
					String cacheKey = String.format("%1$s__%2$s__%3$s", currentActivityName, table.tableName, col.columnName);
					View thisCtrl = MetrixControlAssistant.getControl(mFormDef, mLayout, table.tableName, col.columnName);
					MetrixControlState thisCtrlState = new MetrixControlState(thisCtrl.isEnabled(), col.required, (thisCtrl.getVisibility() == View.VISIBLE), MetrixControlAssistant.getValue(mFormDef, mLayout, table.tableName, col.columnName));
					thisCtrlState.mSpinnerItems = MetrixControlAssistant.getSpinnerItems(thisCtrl);
					savedInstanceState.putSerializable(cacheKey, thisCtrlState);
				}
			}
		}

		saveBasicButtonState(savedInstanceState, currentActivityName);
		saveQuickActionBarState(savedInstanceState, currentActivityName);

		navigateBack = false;
	    super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (navigateBack) {
			navigateBack = false;
			return;
		}

		MetrixPublicCache.instance.addItem("initialScreenValuesSet", false);

		int currentScreenId;
		String currentActivityName = "";
		if (isCodelessScreen) {
			currentActivityName = codeLessScreenName;
			currentScreenId = codeLessScreenId;
		} else {
			currentActivityName = mCurrentActivity.getClass().getSimpleName();
			currentScreenId = MetrixScreenManager.getScreenId(currentActivityName);
		}

		restoreBasicButtonState(savedInstanceState, currentActivityName);
		restoreQuickActionBarState(savedInstanceState, currentActivityName);

		if (currentScreenId > 0 && mFormDef != null && mFormDef.tables.size() > 0) {
			for (MetrixTableDef table : mFormDef.tables) {
				for (MetrixColumnDef col : table.columns) {
					if(col.primaryKey) continue;
					String cacheKey = String.format("%1$s__%2$s__%3$s", currentActivityName, table.tableName, col.columnName);
					MetrixControlState thisCtrlState = (MetrixControlState)savedInstanceState.getSerializable(cacheKey);
					if (thisCtrlState != null) {
						MetrixControlAssistant.setEnabled(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mIsEnabled);
						MetrixControlAssistant.setRequired(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mIsRequired);
						MetrixControlAssistant.setVisibility(col.id, mLayout, ((thisCtrlState.mIsVisible) ? View.VISIBLE : View.GONE));
						if (col.labelId != null)
							MetrixControlAssistant.setVisibility(col.labelId, mLayout, ((thisCtrlState.mIsVisible) ? View.VISIBLE : View.GONE));
						if (thisCtrlState.mSpinnerItems != null)
							MetrixControlAssistant.populateSpinnerFromGenericList(this, findViewById(col.id), thisCtrlState.mSpinnerItems);
						View field = findViewById(col.id);

						if(field instanceof SignatureField) {
							if(MetrixPublicCache.instance.containsKey("LaunchingSignatureFieldValue")){
								HashMap<String, Object> launchingSignatureFieldValue = (HashMap<String, Object>)MetrixPublicCache.instance.getItem("LaunchingSignatureFieldValue");

								if(launchingSignatureFieldValue.containsKey("MetrixColumnDef")) {
									MetrixColumnDef launchingSignatureColumnDef = (MetrixColumnDef) launchingSignatureFieldValue.get("MetrixColumnDef");
									String launchingSignatureAttachmentId = (String) launchingSignatureFieldValue.get("AttachmentId");
									Integer launchingSignatureFieldId = launchingSignatureColumnDef.fieldId;

									if(launchingSignatureFieldId.equals(col.fieldId)) {
										SignatureField signatureField = (SignatureField) field;
										signatureField.mHiddenAttachmentIdTextView.setText(launchingSignatureAttachmentId);
										signatureField.setupFromConfiguration(this, col, table.tableName);
										MetrixPublicCache.instance.removeItem("LaunchingSignatureFieldValue");
									}
									else {
										MetrixControlAssistant.setValue(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mValue);
									}
								}
								else {
									MetrixControlAssistant.setValue(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mValue);
								}
							}
							else {
								MetrixControlAssistant.setValue(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mValue);
							}
						} else {
							MetrixControlAssistant.setValue(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mValue);
						}
					}
				}
			}
		}

		Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable() { public void run() { MetrixPublicCache.instance.addItem("initialScreenValuesSet", true); }}, 500);
	}

	private void saveListViewStates(ViewGroup viewGroup, Bundle bundle) {
		if (viewGroup == null) {
			return;
		}

		int childCount = viewGroup.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View view = viewGroup.getChildAt(i);

			if (view instanceof ListView) {

				int id = view.getId();
				if (id > 0) {
					bundle.putParcelable(String.valueOf(id), ((ListView) view).onSaveInstanceState());
				}

			} else if (view instanceof ViewGroup) {
				saveListViewStates((ViewGroup) view, bundle);
			}
		}
	}

	protected void saveBasicButtonState(Bundle savedInstanceState, String currentActivityName) {
		FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
		FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.update);
		FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.next);
		Button listButton = (Button) findViewById(R.id.view_previous_entries);

		MetrixButtonState currState;
		String cacheKey;

		if (addButton != null) {
			currState = new MetrixButtonState("", addButton.isEnabled(), (addButton.getVisibility() == View.VISIBLE));
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.save));
			savedInstanceState.putSerializable(cacheKey, currState);
		}
		if (saveButton != null) {
			currState = new MetrixButtonState("", saveButton.isEnabled(), (saveButton.getVisibility() == View.VISIBLE));
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.update));
			savedInstanceState.putSerializable(cacheKey, currState);
		}
		if (nextButton != null) {
			currState = new MetrixButtonState("", nextButton.isEnabled(), (nextButton.getVisibility() == View.VISIBLE));
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.next));
			savedInstanceState.putSerializable(cacheKey, currState);
		}
		if (listButton != null) {
			currState = new MetrixButtonState(listButton.getText().toString(), listButton.isEnabled(), (listButton.getVisibility() == View.VISIBLE));
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.view_previous_entries));
			savedInstanceState.putSerializable(cacheKey, currState);
		}
	}

	protected void restoreBasicButtonState(Bundle savedInstanceState, String currentActivityName) {
		FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
		FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.update);
		FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.next);
		Button listButton = (Button) findViewById(R.id.view_previous_entries);
		MetrixButtonState currState;
		String cacheKey;

		if (addButton != null) {
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.save));
			currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
			if (currState != null) {
				addButton.setEnabled(currState.mIsEnabled);
				MetrixControlAssistant.setButtonVisibility(addButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
			}
		}
		if (saveButton != null) {
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.update));
			currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
			if (currState != null) {
				saveButton.setEnabled(currState.mIsEnabled);
				MetrixControlAssistant.setButtonVisibility(saveButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
			}
		}
		if (nextButton != null) {
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.next));
			currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
			if (currState != null) {
				nextButton.setEnabled(currState.mIsEnabled);
				MetrixControlAssistant.setButtonVisibility(nextButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
			}
		}
		if (listButton != null) {
			cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.view_previous_entries));
			currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
			if (currState != null) {
				listButton.setText(currState.mLabel);
				listButton.setEnabled(currState.mIsEnabled);
				MetrixControlAssistant.setButtonVisibility(listButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
			}
		}
	}

	private void saveQuickActionBarState(Bundle savedInstanceState, String currentActivityName) {
		MetrixQuickLinksBar mqlb = (MetrixQuickLinksBar) findViewById(R.id.quick_links_bar);
		if (mqlb != null) {
			// we'll worry about the two badges on restore - they should map directly to button state
			ImageButton taskStatusButton = (ImageButton) findViewById(R.id.taskStatusButton);
			ImageButton overviewButton = (ImageButton) findViewById(R.id.homeButton);
			ImageButton attachmentsButton = (ImageButton) findViewById(R.id.attachmentsButton);
			ImageButton notesButton = (ImageButton) findViewById(R.id.notesButton);
			MetrixButtonState currState;
			String cacheKey;

			if (taskStatusButton != null) {
				currState = new MetrixButtonState("", taskStatusButton.isEnabled(), (taskStatusButton.getVisibility() == View.VISIBLE));
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_STATUS");
				savedInstanceState.putSerializable(cacheKey, currState);
			}
			if (overviewButton != null) {
				currState = new MetrixButtonState("", overviewButton.isEnabled(), (overviewButton.getVisibility() == View.VISIBLE));
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_OVERVIEW");
				savedInstanceState.putSerializable(cacheKey, currState);
			}
			if (attachmentsButton != null) {
				currState = new MetrixButtonState("", attachmentsButton.isEnabled(), (attachmentsButton.getVisibility() == View.VISIBLE));
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_ATTACHMENT");
				savedInstanceState.putSerializable(cacheKey, currState);
			}
			if (notesButton != null) {
				currState = new MetrixButtonState("", notesButton.isEnabled(), (notesButton.getVisibility() == View.VISIBLE));
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_NOTES");
				savedInstanceState.putSerializable(cacheKey, currState);
			}
		}
	}

	private void restoreQuickActionBarState(Bundle savedInstanceState, String currentActivityName) {
		MetrixQuickLinksBar mqlb = (MetrixQuickLinksBar) findViewById(R.id.quick_links_bar);
		if (mqlb != null) {
			ImageButton taskStatusButton = (ImageButton) findViewById(R.id.taskStatusButton);
			ImageButton overviewButton = (ImageButton) findViewById(R.id.homeButton);
			ImageButton attachmentsButton = (ImageButton) findViewById(R.id.attachmentsButton);
			ImageButton notesButton = (ImageButton) findViewById(R.id.notesButton);
			TextView attachmentsBadge = (TextView) findViewById(R.id.attachmentsBadge);
			TextView notesBadge = (TextView) findViewById(R.id.notesBadge);
			MetrixButtonState currState;
			String cacheKey;

			if (taskStatusButton != null) {
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_STATUS");
				currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
				if (currState != null) {
					taskStatusButton.setEnabled(currState.mIsEnabled);
					taskStatusButton.setVisibility((currState.mIsVisible) ? View.VISIBLE : View.GONE);
				}
			}
			if (overviewButton != null) {
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_OVERVIEW");
				currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
				if (currState != null) {
					overviewButton.setEnabled(currState.mIsEnabled);
					overviewButton.setVisibility((currState.mIsVisible) ? View.VISIBLE : View.GONE);
				}
			}
			if (attachmentsButton != null) {
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_ATTACHMENT");
				currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
				if (currState != null) {
					attachmentsButton.setEnabled(currState.mIsEnabled);
					attachmentsButton.setVisibility((currState.mIsVisible) ? View.VISIBLE : View.GONE);
					if (attachmentsBadge != null) {
						attachmentsBadge.setEnabled(currState.mIsEnabled);
						attachmentsBadge.setVisibility((currState.mIsVisible) ? View.VISIBLE : View.GONE);
					}
				}
			}
			if (notesButton != null) {
				cacheKey = String.format("%1$s__%2$s", currentActivityName, "QUICK_ACTIONBAR_NOTES");
				currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
				if (currState != null) {
					notesButton.setEnabled(currState.mIsEnabled);
					notesButton.setVisibility((currState.mIsVisible) ? View.VISIBLE : View.GONE);
					if (notesBadge != null) {
						notesBadge.setEnabled(currState.mIsEnabled);
						notesBadge.setVisibility((currState.mIsVisible) ? View.VISIBLE : View.GONE);
					}
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		handleAppStartupPushProcessing();
		handleNotificationIfExists();

		// Load all list view positions
		if (mLayout != null && mListViewStates != null) {

			for (String key : mListViewStates.keySet())
			{
				int id = Integer.valueOf(key);
				View view = mLayout.findViewById(id);

				if (view instanceof ListView)
				{
					Parcelable state = mListViewStates.getParcelable(key);
					((ListView) view).onRestoreInstanceState(state);
				}
			}
		}

		// (KEST-216) do hyperlink rendering here, because neither onCreate nor onStart will work
		// (onCreate - metadata-based fields don't exist ... onStart - rendering issues on rotation)
		setHyperlinkBehavior();
	}

	protected void wireupEditTextsForBarcodeScanning(ViewGroup viewGroup) {
		if (viewGroup == null) {
			return;
		}

		int childCount = viewGroup.getChildCount();

		for (int i = 0; i < childCount; i++) {
			View view = viewGroup.getChildAt(i);

			if (view instanceof EditText) {
				wireupEditTextForBarcodeScanning((EditText) view);
			} else if (view instanceof ViewGroup) {
				wireupEditTextsForBarcodeScanning((ViewGroup) view);
			}
		}
	}

	protected void wireupEditTextForBarcodeScanning(EditText editText) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			registerForMetrixActionView(editText, getMetrixActionBar().getCustomView());
		} else {
			editText.setCustomInsertionActionModeCallback(new android.view.ActionMode.Callback() {
				private final EditText mEditText = editText;
				private MenuItem barcodeItem;

				@Override
				public boolean onCreateActionMode(android.view.ActionMode mode,
												  Menu menu) {
					barcodeItem = menu
							.add(AndroidResourceHelper.getMessage("ScanBarcode"));
					return true;
				}

				@Override
				public boolean onPrepareActionMode(android.view.ActionMode mode,
												   Menu menu) {
					return false;
				}

				@Override
				public boolean onActionItemClicked(android.view.ActionMode mode,
												   MenuItem item) {
					if (item == barcodeItem) {
						MetrixPublicCache.instance.addItem("METRIX_VIEW_DISPLAYING_CONTEXT_MENU", mEditText.getId());
						toggleOrientationLock(true);
						MetrixBarcodeAssistant.scanBarcode(mEditText);
						return true;
					}
					return false;
				}

				@Override
				public void onDestroyActionMode(android.view.ActionMode mode) {

				}
			});
		}
	}

	protected void displaySaveButtonOnAddNextBar() {
		FloatingActionButton correctErrorsButton = (FloatingActionButton) findViewById(R.id.correct_error);
		FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
		FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.next);
		FloatingActionButton updateButton = (FloatingActionButton) findViewById(R.id.update);
		ViewGroup viewPreviousBar = (ViewGroup) findViewById(R.id.view_previous_entries_bar);

		if (nextButton != null) {
			MetrixControlAssistant.setButtonVisibility(nextButton, View.GONE);
		}

		if (addButton != null) {
			MetrixControlAssistant.setButtonVisibility(addButton, View.GONE);
		}

		if (correctErrorsButton != null) {
			MetrixControlAssistant.setButtonVisibility(correctErrorsButton, View.GONE);
		}

		if (viewPreviousBar != null) {
			MetrixControlAssistant.setButtonVisibility(viewPreviousBar, View.GONE);
		}

		MetrixControlAssistant.setButtonVisibility(updateButton, View.VISIBLE);
		updateButton.setOnClickListener(this);
	}

	private void cacheOnStartValues() {
		if (this.mFormDef != null && this.mFormDef.tables.size() > 0) {
			Hashtable<Integer, String> onStartValues = new Hashtable<Integer, String>();

			for (MetrixTableDef tableDef : this.mFormDef.tables) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					onStartValues.put(columnDef.id, MetrixControlAssistant.getValue(columnDef.id, mLayout));
				}
			}

			MetrixPublicCache.instance.addItem("MetrixOnStartValues", onStartValues);
		}
	}

	protected boolean anyOnStartValuesChanged() {
		return MetrixControlAssistant.anyOnStartValuesChanged(this.mFormDef, this.mLayout);
	}

	public void showIgnoreErrorDialog(String message, Class<?> nextActivity, boolean finishCurrentActivity, boolean advanceWorkflow) {
		if (!message.endsWith(".")) {
			message = message + ".";
		}
		message = message + " " + AndroidResourceHelper.getMessage("ContinueWithoutSaving");

		final Class<?> finalNextActivity = nextActivity;
		final boolean finalFinishCurrentActivity = finishCurrentActivity;
		final Activity currentActivity = this;
		final boolean finalAdvanceWorkflow = advanceWorkflow;

		new AlertDialog.Builder(this).setCancelable(false).setMessage(message).setPositiveButton(AndroidResourceHelper.getMessage("Yes"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (finalNextActivity != null) {
					if (finalFinishCurrentActivity) {
						Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, finalNextActivity);
						MetrixActivityHelper.startNewActivityAndFinish(currentActivity, intent);
					} else {
						Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, finalNextActivity);
						MetrixActivityHelper.startNewActivity(currentActivity, intent);
					}
				} else {
					// next activity and advance workflow should be mutually exclusive
					// finishCurrentActivity should still fire, regardless of whether we have a next activity specified

					if (finalAdvanceWorkflow)
						MetrixWorkflowManager.advanceWorkflow(currentActivity);

					if (finalFinishCurrentActivity)
						currentActivity.finish();
				}
			}
		}).setNegativeButton(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		}).create().show();
	}

	@Override
	protected void onDestroy() {
		unbindService();
		unregisterLogoutReceiever();
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
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					processPostListener(activityType, message);
				}
			});
		}
	};

	protected void processPostListener(ActivityType activityType, String message) {
		if (activityType == ActivityType.InitializationStarted) {
			mInitializationStarted = true;
			LogManager.getInstance(mCurrentActivity).debug("Initialization, run on the base activity.");
			mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("Initializing"));
		} else if (activityType == ActivityType.InitializationEnded) {
			mInitializationStarted = false;
			mUIHelper.dismissLoadingDialog();
			User.setUser(User.getUser().personId, mCurrentActivity);
			Intent intent = MetrixActivityHelper.getInitialActivityIntent(mCurrentActivity);
			MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
		} else if (activityType == ActivityType.PasswordChangedFromServer) {
			handleServerPasswordChange();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
        //boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerLinearLayout);
        //menu.findItem(R.id.action_search).setVisible(!drawerOpen);

		// provide ability to hide options menu (used by Mobile Designer when using standard Lookup)
		if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NoOptionsMenu")
				&& this.getIntent().getExtras().getBoolean("NoOptionsMenu")) {
			//Sliding Drawer icon is hidden, ActionBar Home icon (by default "IFS" icon) is disabled,
			//Even if you swipe from left edge of the screen -> sliding-menu shouldn't get visible.
			mSupportActionBar.setDisplayHomeAsUpEnabled(false);
			mSupportActionBar.setHomeButtonEnabled(false);
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			return false;
		}

		ArrayList<MetrixSlidingMenuItem> slidingMenuItems = new ArrayList<MetrixSlidingMenuItem>();
		MetrixSlidingMenuResourceData metrixSlidingMenuResourceData = (MetrixSlidingMenuResourceData) MetrixPublicCache.instance.getItem("MetrixSlidingMenuResourceData");
		if (metrixSlidingMenuResourceData != null) {
			HashMap<String, Integer> slidingMenuResources = metrixSlidingMenuResourceData.getSlidingMenuResourceIDs();

			if (MetrixApplicationAssistant.getMetaBooleanValue(this, "DemoBuild"))
				DemoOptionsMenu.onPrepareOptionsMenu(slidingMenuItems);
			else
				//passing mScreenId - in-order to cater code-less screens
				OptionsMenu.onPrepareOptionsMenu(this, slidingMenuItems);

			MetrixSlidingMenuAdapter slidingMenuAdapter = new MetrixSlidingMenuAdapter(slidingMenuResources.get("R.layout.sliding_menu_item"), slidingMenuResources.get("R.id.textview_sliding_menu_item_name"),
					slidingMenuResources.get("R.id.textview_sliding_menu_item_count"), slidingMenuResources.get("R.id.imageview_sliding_menu_item_icon"), slidingMenuItems, this);

	        if (mMetrixSlidingMenu != null)
				mMetrixSlidingMenu.setAdapter(slidingMenuAdapter);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.action_bar_help:
			Intent intent = MetrixActivityHelper.createActivityIntent(this, Help.class);

			String message = this.helpText;
			if (this.mHandlingErrors) {
				message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ErrorMess1Arg", MobileGlobal.mErrorInfo.errorMessage);
			}

			message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ScreenColon1Arg", this.mCurrentActivity.getClass().getSimpleName());

			intent.putExtra("help_text", message);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.action_bar_error:
			intent = MetrixActivityHelper.createActivityIntent(this, SyncServiceMonitor.class);
			intent.putExtra("default_tab", 4);
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		case R.id.action_bar_title:
			displayTitleOptions();
			break;
		case R.id.correct_error:
			MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
			MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, DebriefOverview.class, true, AndroidResourceHelper.getMessage("Task"));
			break;
		case R.id.btn_get_directions:
			final Object tag = ((View)v.getParent()).getTag();
			if (tag instanceof String) {
				final String geoTag = (String) tag;
				if (!MetrixStringHelper.isNullOrEmpty(geoTag)) {
					Uri.Builder builder = new Uri.Builder();
					builder.scheme("https")
							.authority("www.google.com")
							.appendPath("maps")
							.appendPath("dir")
							.appendPath("")
							.appendQueryParameter("api", "1")
							.appendQueryParameter("destination", geoTag);
					final String url = builder.build().toString();
					final Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				}
			}
			break;
		default:
			for (MetrixTableDef table : mFormDef.tables) {
				for (MetrixColumnDef columnDef : table.columns) {
					if (columnDef.id == v.getId()) {
						if (columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixTime.class || columnDef.dataType == MetrixDateTime.class) {
							showDialog(columnDef.id);
						} else {
							intent = new Intent(this, FullTextEdit.class);
							intent.putExtra("table_name", table.tableName);
							intent.putExtra("column_name", columnDef.columnName);
							intent.putExtra("column_value", MetrixControlAssistant.getValue(columnDef, mLayout));

							if (MetrixStringHelper.isNullOrEmpty(columnDef.friendlyName)) {
								intent.putExtra("friendly_name", "");
							} else {
								intent.putExtra("friendly_name", columnDef.friendlyName);
							}
							startActivityForResult(intent, EDIT_FULLTEXT);
						}
						break;
					}
				}
			}
			break;
		}
	}

	@SuppressWarnings("deprecation")
	protected void handleServerPasswordChange() {
		SettingsHelper.saveBooleanSetting(this, "REDISPLAY_PASSWORD_DIALOG", false);
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

				LogoutHandler logoutHandler = new LogoutHandler(mCurrentActivity);
				logoutHandler.signOut();
			}
		});
		if(!isFinishing()) {
			mNewPassAlert.show();
		}
		else {
			LogManager.getInstance().error("The current activity is finishing, display dialog on different screen.");
			SettingsHelper.saveBooleanSetting(this, "REDISPLAY_PASSWORD_DIALOG", true);
		}
	}

	private void displayTitleOptions() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(AndroidResourceHelper.getMessage("ActionsTitle"));

		final ArrayList<String> titleOptions = new ArrayList<String>();
		final String profile = AndroidResourceHelper.getMessage("ProfileTitleOpt"), closeApp = AndroidResourceHelper.getMessage("CloseAppTitleOpt");
		titleOptions.add(profile);
		titleOptions.add(closeApp);

		CharSequence[] items = titleOptions.toArray(new CharSequence[titleOptions.size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int pos) {

				String selectedOption = titleOptions.get(pos);

				if (selectedOption.compareToIgnoreCase(profile) == 0) {
					Intent intent = MetrixActivityHelper.createActivityIntent(MetrixActivity.this, Profile.class);
					MetrixActivityHelper.startNewActivity(MetrixActivity.this, intent);
				} else if (selectedOption.compareToIgnoreCase(closeApp) == 0) {
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onFocusChange(View view, boolean hasFocus)
	{
		if (hasFocus)
		{
			for (MetrixTableDef table : mFormDef.tables)
			{
				for (MetrixColumnDef columnDef : table.columns)
				{
					if (columnDef.id == view.getId())
					{
						if (columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixTime.class || columnDef.dataType == MetrixDateTime.class)
						{
							showDialog(columnDef.id);
						} else
						{
							Intent intent = new Intent(this, FullTextEdit.class);
							intent.putExtra("table_name", table.tableName);
							intent.putExtra("column_name", columnDef.columnName);
							intent.putExtra("column_value", MetrixControlAssistant.getValue(columnDef, mLayout));

							if (MetrixStringHelper.isNullOrEmpty(columnDef.friendlyName))
							{
								intent.putExtra("friendly_name", "");
							} else
							{
								intent.putExtra("friendly_name", columnDef.friendlyName);
							}

							startActivityForResult(intent, EDIT_FULLTEXT);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		if(mFormDef == null || mFormDef.tables == null)
			return null;

		for (MetrixTableDef table : mFormDef.tables) {
			for (MetrixColumnDef columnDef : table.columns) {
				if (columnDef.id == id) {
					if (columnDef.dataType == MetrixDate.class) {
						String value = MetrixControlAssistant.getValue(id, mLayout);
						Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, value);
						return new DatePickerDialog(this, MetrixApplicationAssistant.getSafeDialogThemeStyleID(), new DatePickerDialog.OnDateSetListener() {
							@Override
							public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
								try {
									MetrixControlAssistant.setValue(id, mLayout, MetrixDateTimeHelper.formatDate(year, monthOfYear, dayOfMonth));
								} catch (Exception e) {
									LogManager.getInstance().error(e);
								}
							}
						}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
					} else if (columnDef.dataType == MetrixTime.class) {
						String value = MetrixControlAssistant.getValue(id, mLayout);
						Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.TIME_FORMAT, value);
						return new TimePickerDialog(this, MetrixApplicationAssistant.getSafeDialogThemeStyleID(), new TimePickerDialog.OnTimeSetListener() {
							@Override
							public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
								try {
									MetrixControlAssistant.setValue(id, mLayout, MetrixDateTimeHelper.formatTime(hourOfDay, minute));
								} catch (Exception e) {
									LogManager.getInstance().error(e);
								}
							}
						}, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), MetrixDateTimeHelper.use24HourTimeFormat());
					} else if (columnDef.dataType == MetrixDateTime.class) {
						MobileUIHelper.showDateTimeDialog(this, id);
					}
					break;
				}
			}
		}
		return null;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case MobileGlobal.GET_LOOKUP_RESULT:
				mLookupReturn = true;
				break;
			case MetrixBarcodeAssistant.REQUEST_CODE: // This case should no longer be hit by baseline code but is kept for compatibility
				MetrixBarcodeScanResult scanResult = MetrixBarcodeAssistant.parseActivityResult(requestCode, resultCode, data);
				if (scanResult != null) {
					try {
						Object value = MetrixPublicCache.instance.getItem("METRIX_VIEW_DISPLAYING_CONTEXT_MENU");
						int intValue = ((Number) value).intValue();
						MetrixControlAssistant.setValue(intValue, mLayout, scanResult.getContents());
						mLookupReturn = true;
					} catch (Exception e) {
						LogManager.getInstance(this).error(e);
					}
				}
				break;
			case EDIT_FULLTEXT:
				if (resultCode == RESULT_OK) {
					String tableName = data.getStringExtra("table_name");
					String columnName = data.getStringExtra("column_name");
					String columnValue = data.getStringExtra("column_value");

					for (MetrixTableDef table : mFormDef.tables) {
						if (table.tableName.compareToIgnoreCase(tableName) == 0) {
							for (MetrixColumnDef columnDef : table.columns) {
								if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
									try {
										View view = MetrixControlAssistant.getControl(columnDef.id, mLayout);

										if (view instanceof EditText)
											MetrixControlAssistant.setTag(columnDef, mLayout, columnValue);
									} catch (Exception ex) {
										LogManager.getInstance().error(ex);
									}
									break;
								}
							}
						}
					}

				}
				break;
			case BASE_TAKE_SIGNATURE:
				if (resultCode == RESULT_OK) {
				} else if (resultCode == RESULT_CANCELED) {
					// Handle cancel ... do nothing
				}
				break;
			case BASE_TAKE_PICTURE:
			case BASE_TAKE_VIDEO:
			case BASE_SHOW_FILEDIALOG:
				// If we are here, it is because an Attachment Field is trying to add an attachment.
				if (resultCode == RESULT_OK) {
					String fileName = null;
					if (data != null)
						fileName = data.getStringExtra("RESULT_PATH");		// Populated only in the BASE_SHOW_FILEDIALOG case, null otherwise.

					if (MetrixPublicCache.instance.containsKey(AttachmentField.LAUNCHING_ATTACHMENT_FIELD_DATA)) {
						AttachmentField.LaunchingAttachmentFieldData attachmentFieldData = (AttachmentField.LaunchingAttachmentFieldData) MetrixPublicCache.instance
								.getItem(AttachmentField.LAUNCHING_ATTACHMENT_FIELD_DATA);
						MetrixPublicCache.instance.removeItem(AttachmentField.LAUNCHING_ATTACHMENT_FIELD_DATA);
						if (attachmentFieldData.isValid()) {
							AttachmentField attachmentField = null;
							if (attachmentFieldData.fromSurvey) {
								ViewGroup questionContainer = findViewById(R.id.survey_question_container);
								for (int i = 0; i < questionContainer.getChildCount(); i++) {
									View child = questionContainer.getChildAt(i);
									if (child instanceof SurveyAttachmentQuestion && attachmentFieldData.surveyQuestionId
											.equals(((SurveyAttachmentQuestion) child).getQuestion().getQuestionId())) {
										attachmentField = child.findViewById(R.id.survey_attachq_field);
									}
								}
							} else {
								attachmentField = findViewById(mFormDef.getId(attachmentFieldData.tableName, attachmentFieldData.columnName));
							}
							if (attachmentField != null) {
								AttachmentWidgetManager.openFromFieldForInsert(new WeakReference<>(this), attachmentField, requestCode, fileName);
							}
						}
					}
				} else if (resultCode == RESULT_CANCELED) {
					// Handle cancel ... do nothing
				}
					break;
			case BASE_ATTACHMENT_ADD:
			case BASE_ATTACHMENT_EDIT:
				if (resultCode == RESULT_OK) {
					AttachmentField.LaunchingAttachmentFieldData attachmentFieldData = new AttachmentField.LaunchingAttachmentFieldData(data);
					if (attachmentFieldData.isValid()) {
						AttachmentField attachmentField = null;
						if (attachmentFieldData.fromSurvey) {
							ViewGroup questionContainer = findViewById(R.id.survey_question_container);
							for (int i = 0; i < questionContainer.getChildCount(); i++) {
								View child = questionContainer.getChildAt(i);
								if (child instanceof SurveyAttachmentQuestion && attachmentFieldData.surveyQuestionId
										.equals(((SurveyAttachmentQuestion) child).getQuestion().getQuestionId())) {
									attachmentField = child.findViewById(R.id.survey_attachq_field);
								}
							}
						} else {
							attachmentField = findViewById(mFormDef.getId(attachmentFieldData.tableName, attachmentFieldData.columnName));
						}
						if (attachmentField != null) {
							attachmentField.mHiddenAttachmentIdTextView.setText(attachmentFieldData.attachmentId);
							attachmentField.updateFieldUI();
						}
					}
				}
				break;
			case SELECT_FILES:
				if (resultCode == RESULT_OK) {
					ArrayList<Uri> fileList = new ArrayList<Uri>();

					if (data.getClipData() != null) {
						int count = data.getClipData().getItemCount();
						int currentItem = 0;
						while (currentItem < count) {
							Uri fileUri = data.getClipData().getItemAt(currentItem).getUri();
							currentItem = currentItem + 1;
							fileList.add(fileUri);
						}
					} else if (data.getData() != null) {
						Uri fileUri = data.getData();
						fileList.add(fileUri);
					}

					if (fileList == null)
						return;

					if (fileList.size() > 1) {
						DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// user need to go to app settings to enable it
								try {
									for (Uri selectFilePath : fileList) {
										Boolean isSameFileNameExistInAttachmentFolder = false;
										String attachmentFileName = MetrixAttachmentHelper.transferFileToAttachmentFolder(selectFilePath);
										if (MetrixStringHelper.isNullOrEmpty(attachmentFileName))
											isSameFileNameExistInAttachmentFolder = true;
										String attachmentFilePath = MetrixAttachmentHelper.getFilePathFromAttachment(attachmentFileName);

										// Save file to attachment and get it to upload
										(new AttachmentAPIBaseActivity()).save(attachmentFilePath, isSameFileNameExistInAttachmentFolder, false);
									}
								} catch (Exception ex) {
									LogManager.getInstance().error(ex);
								}
							}
						};

						MetrixDialogAssistant.showAlertDialog(
								AndroidResourceHelper.getMessage("SelectFiles"),
								AndroidResourceHelper.getMessage("ConfirmMultiFileSelection", fileList.size()),
								AndroidResourceHelper.getMessage("Yes"),
								listener,
								AndroidResourceHelper.getMessage("No"),
								null,
								this
						);
					} else {
						try {
							if (fileList.size() == 1)
								for (Uri selectFilePath : fileList) {
									if (MetrixPublicCache.instance.containsKey(AttachmentField.LAUNCHING_ATTACHMENT_FIELD_DATA)) {
										AttachmentField.LaunchingAttachmentFieldData attachmentFieldData = (AttachmentField.LaunchingAttachmentFieldData) MetrixPublicCache.instance
												.getItem(AttachmentField.LAUNCHING_ATTACHMENT_FIELD_DATA);
										MetrixPublicCache.instance.removeItem(AttachmentField.LAUNCHING_ATTACHMENT_FIELD_DATA);
										if (attachmentFieldData.isValid()) {
											AttachmentField attachmentField = null;
											if (attachmentFieldData.fromSurvey) {
												ViewGroup questionContainer = findViewById(R.id.survey_question_container);
												for (int i = 0; i < questionContainer.getChildCount(); i++) {
													View child = questionContainer.getChildAt(i);
													if (child instanceof SurveyAttachmentQuestion && attachmentFieldData.surveyQuestionId
															.equals(((SurveyAttachmentQuestion) child).getQuestion().getQuestionId())) {
														attachmentField = child.findViewById(R.id.survey_attachq_field);
													}
												}
											} else {
												attachmentField = findViewById(mFormDef.getId(attachmentFieldData.tableName, attachmentFieldData.columnName));
											}
											if (attachmentField != null && selectFilePath != null) {
												AttachmentWidgetManager.openFromFieldForInsert(new WeakReference<>(this), attachmentField, requestCode, selectFilePath
														.toString());
											}
										}
									}
								}
						} catch (Exception ex) {
							LogManager.getInstance().error(ex);
						}
					}
				}
				break;
			case END_SESSION_REQUEST_CODE:
				if (resultCode == Activity.RESULT_OK) {
					LogoutHandler logoutHandler = new LogoutHandler(this);
					logoutHandler.clientLogout();
				}
				else {
					MetrixUIHelper.showSnackbar(this, "Log out failed, please try it again");
				}
				break;
			case END_SESSION_RELOGIN_CODE:
				if (resultCode == Activity.RESULT_OK) {
					LogoutHandler logoutHandler = new LogoutHandler(this);
					logoutHandler.relogin();
				}
				else {
					MetrixUIHelper.showSnackbar(this, "Sign out failed, please try it again");
				}
				break;
			default:
				mLookupReturn = false;
		}
	}

	public void reloadActivity() {
		Intent intent = MetrixActivityHelper.createActivityIntent(this, mCurrentActivity.getClass());
		intent.putExtras(this.getIntent().getExtras());
		MetrixActivityHelper.startNewActivityAndFinish(this, intent);
	}

	public boolean taskMeetsCompletionPrerequisites(String taskId) {
		int count = MetrixDatabaseManager.getCount("task_steps", String.format("task_id = %s and required='Y' and completed='N'", taskId));
		if (count > 0) {
			MetrixUIHelper.showSnackbar(this, AndroidResourceHelper.getMessage("JobCompletionMissingSteps"));
			return false;
		}

		if (MeterReadingsPolicy.displayMessageForOutstandingReadings(taskId, this)) {
			return false;
		}

		return true;
	}

	public static boolean doDesignRefresh(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		String dir = MetrixAttachmentManager.getInstance().getAttachmentPath();
		MetrixAttachmentManager.getInstance().deleteAllDesignerImageFiles(dir);

		ArrayList<String> mobileDesignTableNames = MobileApplication.getMobileDesignTableNames();
		ArrayList<String> mobileNoLogDesignTableNames = MobileApplication.getMobileNoLogDesignTableNames();
		ArrayList<String> statements = new ArrayList<String>();

		for (String tableName : mobileDesignTableNames) {
			statements.add("delete from " + tableName);
			statements.add("delete from " + tableName + "_log");
		}

		for (String tableName : mobileNoLogDesignTableNames) {
			statements.add("delete from " + tableName);
		}

		if (MetrixDatabaseManager.executeSqlArray(statements, false)) {
			Hashtable<String, String> params = new Hashtable<String, String>();
			String personId = User.getUser().personId;
			int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

			try {
				params.put("person_id", personId);
				params.put("device_sequence", String.valueOf(device_id));
				params.put("truncate_tables", "false");
				params.put("generate_end_message", "true");

				MetrixPerformMessage performGetMDS = new MetrixPerformMessage("perform_get_mobile_device_schema", params);
				performGetMDS.save();
			}
			catch (Exception ex) {
				LogManager.getInstance().error(ex);
				return false;
			}
		} else {
			MetrixUIHelper.showErrorDialogOnGuiThread(activity, designRefreshDBError);
			return false;
		}

		return true;
	}

	public static ArrayList<String> getSystemTableNames() {
		ArrayList<String> systemTableNames = new ArrayList<String>();

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("select name from sqlite_master where type ='table' AND name LIKE 'mm_message%' AND name <> 'mm_message_def_view'", null);
			// "select name from sqlite_master where type ='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE '%_log' AND name NOT LIKE 'mm_%'",

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			int i = 1;
			while (cursor.isAfterLast() == false) {

				String table_name = cursor.getString(0);
				systemTableNames.add(table_name);
				cursor.moveToNext();
				i = i + 1;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return systemTableNames;
	}

	public static void clearDesignerCaches() {
		MetrixClientScriptManager.clearClientScriptCache();
		MetrixFieldManager.clearDefaultValuesCache();
		MetrixFieldManager.clearFieldPropertiesCache();
		MetrixFieldLookupManager.clearFieldLookupCache();
		MetrixGlobalMenuManager.clearGlobalMenuItemCache();
		MetrixHomeMenuManager.clearHomeMenuItemCache();
		MetrixFilterSortManager.clearFilterSortCaches();
		MetrixListScreenManager.clearItemFieldPropertiesCache();
		MetrixScreenManager.clearScreenIdsCache();
		MetrixSkinManager.clearSkinItemsCache();
		MetrixWorkflowManager.clearAllWorkflowCaches();
		MetrixScreenManager.clearScreenPropertiesCache();
		MetrixTabScreenManager.clearTabScreensCache();
		/**clear login screen skin information**/
		SettingsHelper.clearLoginScreenSkinInfo();
	}

	protected static boolean mobileDesignChangesInProgress() {
		int masterCount = 0;
		ArrayList<String> mdTables = MobileApplication.getMobileDesignTableNames();
		for (String table : mdTables) {
			int tableCount = MetrixDatabaseManager.getCount(table + "_log", "");
			masterCount = masterCount + tableCount;
		}

		return masterCount > 0;
	}

	public static boolean ping(String baseServiceUrl, MetrixRemoteExecutor remoteExecutor) {
		String pingServiceUrl = MetrixSyncManager.generateRestfulServiceUrl(baseServiceUrl, MessageType.Messages, null, 0, null, null);

		try {
			String response = remoteExecutor.executeGet(pingServiceUrl).replace("\\", "");

			if (response != null) {
				if (response.contains("true"))
					return true;
			}
		} catch (HandlerException ex) {
			LogManager.getInstance().error(ex);
			return false;
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return false;
	}

	public static String getToken(String baseServiceUrl, MetrixRemoteExecutor remoteExecutor) {
		String getTokenServiceUrl = MetrixSyncManager.generateRestfulServiceUrl(baseServiceUrl, MessageType.Messages, null, 0, null, "token");

		try {
			String response = remoteExecutor.executeGet(getTokenServiceUrl).replace("\\", "").replace("\"", "");

			if (response != null) {
					return response;
			}
		} catch (HandlerException ex) {
			LogManager.getInstance().error(ex);
			return "";
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex);
			return "";
		}

		return "";
	}

	protected void populateArchitectureStringResources() {
		// Architecture string resources
		if (!MetrixPublicCache.instance.containsKey("MetrixArchitectureResourceStrings")) {
			Resources res = getResources();
			HashMap<String, String> metrixArchitectureResourceStrings = new HashMap<String, String>();

			MetrixPublicCache.instance.addItem("MetrixArchitectureResourceStrings", metrixArchitectureResourceStrings);
		}
	}

	protected void populateAttachmentAPIResources() {
		// Reuse MetrixDesignerResourceBaseData to set up the Action Bar in the base Attachment API class, and some common IDs
		if (!MetrixPublicCache.instance.containsKey("FSMAttachmentAPIBaseResources")) {
			HashMap<String, Integer> extraResourceIDs = new HashMap<>();
			extraResourceIDs.put("R.color.IFSPurple", R.color.IFSPurple);
			extraResourceIDs.put("R.dimen.fab_offset_difference", R.dimen.fab_offset_difference);
			extraResourceIDs.put("R.dimen.fab_offset_single", R.dimen.fab_offset_single);
			extraResourceIDs.put("R.dimen.md_margin", R.dimen.md_margin);
			extraResourceIDs.put("R.drawable.calendar", R.drawable.calendar);
			extraResourceIDs.put("R.id.coordinator_layout", R.id.coordinator_layout);
			extraResourceIDs.put("R.id.scroll_view", R.id.scroll_view);
			extraResourceIDs.put("R.id.split_action_bar", R.id.split_action_bar);
			extraResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraResourceIDs.put("R.style.ButtonBase_Normal_ActionBar", R.style.ButtonBase_Normal_ActionBar);

			MetrixDesignerResourceBaseData thisBaseData = new MetrixDesignerResourceBaseData(R.layout.action_bar, R.id.action_bar_title, R.id.action_bar_help, R.drawable.header_help_32x32, AndroidResourceHelper.getMessage("Attachments"),
					R.drawable.ifs_logo, extraResourceIDs, null);
			MetrixPublicCache.instance.addItem("FSMAttachmentAPIBaseResources", thisBaseData);
		}

		if (!MetrixPublicCache.instance.containsKey("FSMCarouselResources")) {
			//region #Ensure FSMCarousel* classes in architecture have all necessary resource IDs cached
			HashMap<String, Object> carouselResources = new HashMap<>();

			carouselResources.put("BuildConfig.APPLICATION_ID", BuildConfig.APPLICATION_ID);
			carouselResources.put("R.id.file_preview", R.id.file_preview);
			carouselResources.put("R.id.txtview_attachment_date", R.id.txtview_attachment_date);
			carouselResources.put("R.id.txtview_attachment_description", R.id.txtview_attachment_description);
			carouselResources.put("R.id.txtview_attachment_name", R.id.txtview_attachment_name);
			carouselResources.put("R.id.txtview_attachment_number", R.id.txtview_attachment_number);
			carouselResources.put("R.layout.aapi_carousel_item", R.layout.aapi_carousel_item);

			MetrixPublicCache.instance.addItem("FSMCarouselResources", carouselResources);
			//endregion
		}

		if (!MetrixPublicCache.instance.containsKey("FSMAttachmentAddResources")) {
			//region #Ensure FSMAttachmentAdd has all necessary resource IDs cached
			HashMap<String, Integer> attachmentAddResources = new HashMap<>();

			attachmentAddResources.put("R.id.cancel", R.id.cancel);
			attachmentAddResources.put("R.id.custom_save", R.id.custom_save);
			attachmentAddResources.put("R.id.file_preview", R.id.file_preview);
			attachmentAddResources.put("R.id.parent_table_layout", R.id.parent_table_layout);
			attachmentAddResources.put("R.layout.aapi_attachment_add", R.layout.aapi_attachment_add);

			MetrixPublicCache.instance.addItem("FSMAttachmentAddResources", attachmentAddResources);
			//endregion
		}

		if (!MetrixPublicCache.instance.containsKey("FSMAttachmentCardResources")) {
			//region #Ensure FSMAttachmentCard has all necessary resource IDs cached
			HashMap<String, Integer> attachmentCardResources = new HashMap<>();

			attachmentCardResources.put("R.id.cancel", R.id.cancel);
			attachmentCardResources.put("R.id.custom_save", R.id.custom_save);
			attachmentCardResources.put("R.layout.aapi_attachment_card", R.layout.aapi_attachment_card);

			MetrixPublicCache.instance.addItem("FSMAttachmentCardResources", attachmentCardResources);
			//endregion
		}

		if (!MetrixPublicCache.instance.containsKey("FSMAttachmentFullScreenResources")) {
			//region #Ensure FSMAttachmentFullScreen has all necessary resource IDs cached
			HashMap<String, Integer> attachmentFullScreenResources = new HashMap<>();

			attachmentFullScreenResources.put("R.id.attachment_pager", R.id.attachment_pager);
			attachmentFullScreenResources.put("R.id.edit_attachment", R.id.edit_attachment);
			attachmentFullScreenResources.put("R.id.view_card", R.id.view_card);
			attachmentFullScreenResources.put("R.layout.aapi_attachment_fullscreen", R.layout.aapi_attachment_fullscreen);

			MetrixPublicCache.instance.addItem("FSMAttachmentFullScreenResources", attachmentFullScreenResources);
			//endregion
		}

		if (!MetrixPublicCache.instance.containsKey("FSMAttachmentListResources")) {
			//region #Ensure FSMAttachmentList has all necessary resource IDs cached
			HashMap<String, Integer> attachmentListResources = new HashMap<>();

			attachmentListResources.put("R.color.IFSGold", R.color.IFSGold);
			attachmentListResources.put("R.drawable.rv_item_divider", R.drawable.rv_item_divider);
			attachmentListResources.put("R.id.attachment_add_ctrl", R.id.attachment_add_ctrl);
			attachmentListResources.put("R.id.attachment_thumbnail", R.id.attachment_thumbnail);
			attachmentListResources.put("R.id.recyclerView", R.id.recyclerView);
			attachmentListResources.put("R.id.table_layout", R.id.table_layout);
			attachmentListResources.put("R.layout.aapi_attachment_list", R.layout.aapi_attachment_list);
			attachmentListResources.put("R.layout.aapi_list_item", R.layout.aapi_list_item);
			attachmentListResources.put("R.layout.list_item_table_row", R.layout.list_item_table_row);
			attachmentListResources.put("R.id.add_next_bar", R.id.add_next_bar);
			attachmentListResources.put("R.id.next", R.id.next);
			attachmentListResources.put("R.id.save", R.id.save);

			MetrixPublicCache.instance.addItem("FSMAttachmentListResources", attachmentListResources);
			//endregion
		}
	}

	protected void populateAttachmentHelperResources() {
		// Attachment Helper resources
		if (!MetrixPublicCache.instance.containsKey("AttachmentHelperResources")) {
			HashMap<String, Object> attachmentHelperResources = new HashMap<String, Object>();

			attachmentHelperResources.put("R.drawable.attachment_video_icon_medium", R.drawable.attachment_video_icon_medium);
			attachmentHelperResources.put("R.drawable.attachment_video_icon_small", R.drawable.attachment_video_icon_small);
			attachmentHelperResources.put("R.drawable.audio_file", R.drawable.audio_file);
			attachmentHelperResources.put("R.drawable.country", R.drawable.country);
			attachmentHelperResources.put("R.drawable.detach", R.drawable.detach);
			attachmentHelperResources.put("R.drawable.document", R.drawable.document);
			attachmentHelperResources.put("R.drawable.document_alt", R.drawable.document_alt);
			attachmentHelperResources.put("R.drawable.download", R.drawable.download);
			attachmentHelperResources.put("R.drawable.error_alt", R.drawable.error_alt);
			attachmentHelperResources.put("R.drawable.excel_file", R.drawable.excel_file);
			attachmentHelperResources.put("R.drawable.pdf_file", R.drawable.pdf_file);
			attachmentHelperResources.put("R.drawable.tutorial", R.drawable.tutorial);
			attachmentHelperResources.put("R.drawable.video_file", R.drawable.video_file);
			attachmentHelperResources.put("R.drawable.word_file", R.drawable.word_file);

			MetrixPublicCache.instance.addItem("AttachmentHelperResources", attachmentHelperResources);
		}
	}

	protected void populateAttachmentFieldResources() {
		// Attachment Helper resources
		if (!MetrixPublicCache.instance.containsKey("AttachmentFieldResources")) {
			HashMap<String, Integer> attachmentFieldResources = new HashMap<>();

			// Resources for the Attachment Fields itself
			attachmentFieldResources.put("R.layout.aapi_attachment_field", R.layout.aapi_attachment_field);
			attachmentFieldResources.put("R.id.attachment_add_ctrl", R.id.attachment_add_ctrl);
			attachmentFieldResources.put("R.id.file_preview", R.id.file_preview);

			// Resources for the Attachment Addition Control
			attachmentFieldResources.put("R.layout.attachment_addition_control", R.layout.attachment_addition_control);
			attachmentFieldResources.put("R.id.attachment_imagebtn_camera", R.id.attachment_imagebtn_camera);
			attachmentFieldResources.put("R.id.attachment_imagebtn_video_camera", R.id.attachment_imagebtn_video_camera);
			attachmentFieldResources.put("R.id.attachment_imagebtn_file", R.id.attachment_imagebtn_file);

			MetrixPublicCache.instance.addItem("AttachmentFieldResources", attachmentFieldResources);
		}
	}

	protected void populateSignaturePadResources() {
		if (!MetrixPublicCache.instance.containsKey("FSMSignatureBaseResources")) {
			HashMap<String, Integer> extraResourceIDs = new HashMap<>();
			extraResourceIDs.put("R.color.IFSPurple", R.color.IFSPurple);
			extraResourceIDs.put("R.dimen.fab_offset_difference", R.dimen.fab_offset_difference);
			extraResourceIDs.put("R.dimen.fab_offset_single", R.dimen.fab_offset_single);
			extraResourceIDs.put("R.dimen.md_margin", R.dimen.md_margin);
			extraResourceIDs.put("R.drawable.calendar", R.drawable.calendar);
			extraResourceIDs.put("R.id.coordinator_layout", R.id.CoordinatorLayout);
			extraResourceIDs.put("R.id.split_action_bar", R.id.split_action_bar);
			extraResourceIDs.put("R.style.ButtonBase_Normal_ActionBar", R.style.ButtonBase_Normal_ActionBar);

			MetrixDesignerResourceBaseData thisBaseData = new MetrixDesignerResourceBaseData(R.layout.action_bar, R.id.action_bar_title, R.id.action_bar_help, R.drawable.header_help_32x32, "Signature",
					R.drawable.ifs_logo, extraResourceIDs, null);
			MetrixPublicCache.instance.addItem("FSMSignatureBaseResources", thisBaseData);
		}
		if(!MetrixPublicCache.instance.containsKey("FSMSignatureAddResources")) {
			//region #Ensure FSMSignatureAdd has all necessary resource IDs cached
			HashMap<String, Integer> signatureAddResources = new HashMap<>();

			signatureAddResources.put("R.id.acceptBtn", R.id.signatureAcceptBtn);
			signatureAddResources.put("R.id.clearBtn", R.id.signatureClearBtn);
			signatureAddResources.put("R.id.cancelBtn", R.id.signatureCancelBtn);
			signatureAddResources.put("R.id.signaturePad", R.id.SignaturePad);
			signatureAddResources.put("R.id.signatureLabel", R.id.signatureLabel);
			signatureAddResources.put("R.layout.fsm_signature_add", R.layout.fsm_signature_add);
			signatureAddResources.put("R.layout.coordinatorLayout", R.id.CoordinatorLayout);

			MetrixPublicCache.instance.addItem("FSMSignatureAddResources", signatureAddResources);
			//endregion
		}
	}
	protected  void populateSignatureFieldResources() {
		if (!MetrixPublicCache.instance.containsKey("SignatureFieldResources")) {
			HashMap<String, Integer> signatureFieldResources = new HashMap<>();

			// Resources for the Signature Fields itself
			signatureFieldResources.put("R.layout.signature_field", R.layout.signature_field);
			signatureFieldResources.put("R.id.signature_view", R.id.signatureView);
			signatureFieldResources.put("R.id.signature_label", R.id.signatureLabel);
			signatureFieldResources.put("R.id.signatureBlock", R.id.signatureBlock);

			MetrixPublicCache.instance.addItem("SignatureFieldResources", signatureFieldResources);
		}
	}

	protected void populateMetrixCameraResources() {
		if (!MetrixPublicCache.instance.containsKey("MetrixCameraResources")) {

			HashMap<String, Integer> cameraResourceStrings = new HashMap<String, Integer>();
			cameraResourceStrings.put("R.raw.camera_shutter_click", R.raw.camera_shutter_click);
			cameraResourceStrings.put("R.raw.camera_focused", R.raw.camera_focused);
			MetrixPublicCache.instance.addItem("MetrixCameraResources", cameraResourceStrings);
		}
	}

	protected void populateMetrixSlidingMenuResources() {
		if (!MetrixPublicCache.instance.containsKey("MetrixSlidingMenuResourceData")) {

			HashMap<String, Integer> slidingMenuResourceIDs = new HashMap<String, Integer>();
			slidingMenuResourceIDs.put("R.layout.sliding_menu_item", R.layout.sliding_menu_item);
			slidingMenuResourceIDs.put("R.id.textview_sliding_menu_item_name", R.id.textview_sliding_menu_item_name);
			slidingMenuResourceIDs.put("R.id.textview_sliding_menu_item_count", R.id.textview_sliding_menu_item_count);
			slidingMenuResourceIDs.put("R.id.imageview_sliding_menu_item_icon", R.id.imageview_sliding_menu_item_icon);

			slidingMenuResourceIDs.put("R.id.drawer", R.id.drawer);
			slidingMenuResourceIDs.put("R.id.drawer_layout", R.id.drawer_layout);
			slidingMenuResourceIDs.put("R.drawable.ic_drawer", R.drawable.ic_drawer);
			slidingMenuResourceIDs.put("R.id.recyclerview_sliding_menu", R.id.recyclerview_sliding_menu);
			slidingMenuResourceIDs.put("R.drawable.rv_global_menu_item_divider", 0);

			slidingMenuResourceIDs.put("R.string.drawer_open", R.string.drawer_open);
			slidingMenuResourceIDs.put("R.string.drawer_close", R.string.drawer_close);

			MetrixSlidingMenuResourceData metrixSlidingMenuResourceData = new MetrixSlidingMenuResourceData(slidingMenuResourceIDs, null);
			MetrixPublicCache.instance.addItem("MetrixSlidingMenuResourceData", metrixSlidingMenuResourceData);
		}
	}

	protected void populateMetrixActionBarResources() {
		if (!MetrixPublicCache.instance.containsKey("MetrixActionBarResourceData")) {
			HashMap<String, Integer> actionBarResourceIDs = new HashMap<String, Integer>();
			actionBarResourceIDs.put("R.drawable.ellipsis_vertical", R.drawable.ellipsis_vertical);

			MetrixActionBarResourceData metrixActionBarResourceData = new MetrixActionBarResourceData(actionBarResourceIDs, null);
			MetrixPublicCache.instance.addItem("MetrixActionBarResourceData", metrixActionBarResourceData);
		}
	}

	protected void populateMetrixHyperlinkAttributes() {
		if (!MetrixPublicCache.instance.containsKey("MetrixHyperlinkAttributeData")) {
			HashMap<String, Object> metrixHyperlinkResourceAttrbs = new HashMap<String, Object>();
			metrixHyperlinkResourceAttrbs.put("R.styleable.MetrixHyperlinkAttr", R.styleable.MetrixHyperlinkAttr);
			metrixHyperlinkResourceAttrbs.put("R.styleable.MetrixHyperlinkAttr_linkText", R.styleable.MetrixHyperlinkAttr_linkText);

			MetrixPublicCache.instance.addItem("MetrixHyperlinkAttributeData", metrixHyperlinkResourceAttrbs);
		}
	}

	protected void populateLocalNotificationResources() {
		if (!MetrixPublicCache.instance.containsKey("MetrixLocalNotificationResourceData")) {
			HashMap<String, Integer> localNotifResourceIDs = new HashMap<String, Integer>();
			localNotifResourceIDs.put("R.drawable.ifs_transparent_logo", R.drawable.ifs_transparent_logo);

			MetrixPublicCache.instance.addItem("MetrixLocalNotificationResourceData", localNotifResourceIDs);

			// Save to app settings as a backup for Background Sync to use
			SettingsHelper.saveIntegerSetting(this, "R.drawable.ifs_transparent_logo", R.drawable.ifs_transparent_logo);
		}
	}

	protected void populateNonStandardControls() {}

	@Override
	protected void defineForm() {}

	@Override
	protected void defaultValues() {}

	@Override
	protected void setListeners() {}

	@Override
	protected void setHyperlinkBehavior() {}

	@Override
	protected void displayPreviousCount() {}

	@Override
	protected void beforeStartForError() {}

	@Override
	protected void beforeUpdateForError() {}

	@Override
	public void onBackPressed() {
		if(!navigateBack)
			navigateBack = true;
		MetrixPublicCache.instance.addItem("backButtonPress_Occurred", true);

		if (MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "Lookup"))
			MetrixPublicCache.instance.addItem("goingBackFromLookup", true);

		super.onBackPressed();
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(mDrawerToggle != null)
        	mDrawerToggle.syncState();
    }

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mDrawerToggle != null)
        	mDrawerToggle.onConfigurationChanged(newConfig);
    }

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	protected void toggleOrientationLock(boolean doLock) {
    	// if !doLock AND cache indicates that we've lock orientation previously, unlock it
    	// otherwise if doLock, then lock orientation AND cache that we have done so
    	// the caching should prevent useless setRequestedOrientation calls
		if (!doLock && MetrixPublicCache.instance.containsKey("METRIX_ORIENTATION_LOCK_ACTIVE")) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			MetrixPublicCache.instance.removeItem("METRIX_ORIENTATION_LOCK_ACTIVE");
		} else if (doLock) {
			Display display = this.getWindowManager().getDefaultDisplay();
		    int rotation = display.getRotation();
		    int height;
		    int width;
		    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
		    	width = display.getWidth();
				height = display.getHeight();
			} else {
		        Point size = new Point();
		        display.getSize(size);
		        width = size.x;
				height = size.y;
			}

			switch (rotation) {
			    case Surface.ROTATION_90:
			        if (width > height)
			            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			        else
						this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
					break;
			    case Surface.ROTATION_180:
			        if (height > width)
			        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
			        else
			        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
					break;
				case Surface.ROTATION_270:
			        if (width > height)
			        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
			        else
			        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			        break;
			    default :
			        if (height > width)
			        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			        else
			        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		    }

			MetrixPublicCache.instance.addItem("METRIX_ORIENTATION_LOCK_ACTIVE", true);
		}
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		//hide soft input keyboard before do a actions
		if(getCurrentFocus()!=null) {
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}

    	if(mDrawerToggle != null){
    		if (mDrawerToggle.onOptionsItemSelected(item))
    			return true;
    	}
    	return super.onOptionsItemSelected(item);
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	protected ActionBar getMetrixActionBar() {
		return mSupportActionBar;
	}

	protected void registerForMetrixActionView(final View view, View anchorView){
		initMetrixActionView(anchorView);
		if(!view.isEnabled()) return;
		if(view instanceof ListView ){
			((ListView) view).setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> parent,
						View view, int position, long id) {
					if(onCreateMetrixActionViewListner != null){
						return onCreateMetrixActionViewListner.OnCreateMetrixActionView(view, position);
					}
					return false;
				}
			});
		}
		else{
			view.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					if(onCreateMetrixActionViewListner != null){
						return onCreateMetrixActionViewListner.OnCreateMetrixActionView(view);
					}
					return false;
				}
			});
		}
	}

	protected void initMetrixActionView(View anchorView) {
		mMetrixActionView = new MetrixActionView(this, anchorView);
		mMetrixActionView.setOnMetrixActionViewItemClickListner(this);
	}

	protected void setOnCreateMetrixActionViewListner(OnCreateMetrixActionViewListner onCreateMetrixActionViewListner){
		this.onCreateMetrixActionViewListner = onCreateMetrixActionViewListner;
	}

	protected MetrixActionView getMetrixActionView() {
		return mMetrixActionView;
	}

	@Override
	public boolean onMetrixActionViewItemClick(MenuItem menuItem) {
		if (menuItem.getTitle().toString().contains(AndroidResourceHelper.getMessage("ScanBarcode"))) {
			toggleOrientationLock(true);
			MetrixBarcodeAssistant.scanBarcode(mLayout.findViewById(menuItem.getItemId()));
		}
		else if(menuItem.getTitle().toString().contains(AndroidResourceHelper.getMessage("Paste"))){
			View view = findViewById(controlToPaste);
			if (view != null && view instanceof EditText) {
				EditText editText = ((EditText) view);

				if (!MetrixStringHelper.isNullOrEmpty(itemToPaste)) {
					boolean setTextToUpper = shouldSetTextToUpper(view);
					view.requestFocus();
					if (setTextToUpper) itemToPaste = itemToPaste.toUpperCase();
					editText.setText(itemToPaste);
					editText.setSelection(itemToPaste.length());
				}
			}
		}
		mMetrixActionView.getMenu().clear();
		return true;
	}

	@SuppressLint("NewApi") @Override
	public boolean OnCreateMetrixActionView(View view, Integer... position) {
		Menu menu = mMetrixActionView.getMenu();
      
		if (view instanceof EditText) {
			menu.clear();
			menu.add(0, view.getId(), 0, AndroidResourceHelper.getMessage("ScanBarcode"));
			int currentapiVersion = android.os.Build.VERSION.SDK_INT;

			if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				ClipData data =  clipboard.getPrimaryClip();
			        if(data != null && data.getItemCount()> 0){
						menu.add(0, view.getId(), 0, AndroidResourceHelper.getMessage("Paste"));
			        	itemToPaste = data.getItemAt(0).getText().toString();
			        }
			} else{
				//For API Level 10
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				itemToPaste = clipboard.getText().toString();
			}
			controlToPaste = view.getId();
			MetrixPublicCache.instance.addItem("METRIX_VIEW_DISPLAYING_CONTEXT_MENU", view.getId());
		}
		if (menu.hasVisibleItems()) {
			mMetrixActionView.show();
			return true;
		}
		return false;
	}

	protected void populateMobileDesignerResources() {
		MetrixGlobalMenuManager.populateDesignerGlobalMenuResources();

		Resources res = getResources();
		// Superclass resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerActivityResourceData")) {
			HashMap<String, Integer> extraBaseResourceIDs = new HashMap<String, Integer>();
			extraBaseResourceIDs.put("R.drawable.sliding_menu_skins", R.drawable.sliding_menu_skins);
			extraBaseResourceIDs.put("R.drawable.sliding_menu_close_app", R.drawable.sliding_menu_close_app);
			extraBaseResourceIDs.put("R.drawable.sliding_menu_about", R.drawable.sliding_menu_about);
			extraBaseResourceIDs.put("R.drawable.sliding_menu_designer", R.drawable.sliding_menu_designer);
			extraBaseResourceIDs.put("R.drawable.sliding_menu_preview", R.drawable.sliding_menu_preview);
			extraBaseResourceIDs.put("R.drawable.sliding_menu_categories", R.drawable.sliding_menu_categories);
			extraBaseResourceIDs.put("R.drawable.sliding_menu_sync", R.drawable.sliding_menu_sync);
			extraBaseResourceIDs.put("R.menu.main", R.menu.main);

            MetrixDesignerResourceBaseData thisBaseData = new MetrixDesignerResourceBaseData(R.layout.action_bar, R.id.action_bar_title, R.id.action_bar_help, R.drawable.header_help_32x32, AndroidResourceHelper.getMessage("MobileDesigner"),
                    R.drawable.ifs_logo, extraBaseResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerActivityResourceData", thisBaseData);
		}

		// Help popup resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerHelpResourceData")) {
			MetrixDesignerResourceHelpData thisHelpData = new MetrixDesignerResourceHelpData(R.layout.help, R.id.textView1, R.id.continueButton);
			MetrixPublicCache.instance.addItem("MetrixDesignerHelpResourceData", thisHelpData);
		}

		// Skin activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerSkinActivityResourceData")) {
			HashMap<String, Integer> extraSkinResourceIDs = new HashMap<String, Integer>();
			extraSkinResourceIDs.put("R.id.mm_skin__name", R.id.mm_skin__name);
			extraSkinResourceIDs.put("R.id.mm_skin__description", R.id.mm_skin__description);
			extraSkinResourceIDs.put("R.id.mm_skin__metrix_row_id", R.id.mm_skin__metrix_row_id);
			extraSkinResourceIDs.put("R.id.mm_skin__skin_id", R.id.mm_skin__skin_id);
			extraSkinResourceIDs.put("R.id.mm_skin__primary_color", R.id.mm_skin__primary_color);
			extraSkinResourceIDs.put("R.id.mm_skin__secondary_color", R.id.mm_skin__secondary_color);
			extraSkinResourceIDs.put("R.id.mm_skin__hyperlink_color", R.id.mm_skin__hyperlink_color);
			extraSkinResourceIDs.put("R.id.mm_skin__first_gradient", R.id.mm_skin__first_gradient);
			extraSkinResourceIDs.put("R.id.mm_skin__second_gradient", R.id.mm_skin__second_gradient);
			extraSkinResourceIDs.put("R.id.refresh_skins", R.id.refresh_skins);
			extraSkinResourceIDs.put("R.id.add_skin", R.id.add_skin);
            extraSkinResourceIDs.put("R.id.skins", R.id.skins);
            extraSkinResourceIDs.put("R.id.screen_info_metrix_designer_skin", R.id.screen_info_metrix_designer_skin);

			MetrixDesignerResourceData thisSkinData = new MetrixDesignerResourceData(R.layout.zzmd_skin, R.id.listview, R.layout.zzmd_skin_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesSkin"), extraSkinResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerSkinActivityResourceData", thisSkinData);
		}

		// Skin Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerSkinAddActivityResourceData")) {
			HashMap<String, Integer> extraSkinAddResourceIDs = new HashMap<String, Integer>();
			extraSkinAddResourceIDs.put("R.id.name", R.id.name);
			extraSkinAddResourceIDs.put("R.id.source_skin", R.id.source_skin);
			extraSkinAddResourceIDs.put("R.id.color_preview", R.id.color_preview);
			extraSkinAddResourceIDs.put("R.id.mm_skin__primary_color", R.id.mm_skin__primary_color);
			extraSkinAddResourceIDs.put("R.id.mm_skin__secondary_color", R.id.mm_skin__secondary_color);
			extraSkinAddResourceIDs.put("R.id.mm_skin__hyperlink_color", R.id.mm_skin__hyperlink_color);
			extraSkinAddResourceIDs.put("R.id.mm_skin__first_gradient", R.id.mm_skin__first_gradient);
			extraSkinAddResourceIDs.put("R.id.mm_skin__second_gradient", R.id.mm_skin__second_gradient);
			extraSkinAddResourceIDs.put("R.id.description", R.id.description);
			extraSkinAddResourceIDs.put("R.id.save", R.id.save);

            extraSkinAddResourceIDs.put("R.id.add_skin", R.id.add_skin);
            extraSkinAddResourceIDs.put("R.id.screen_info_metrix_designer_skin_add", R.id.screen_info_metrix_designer_skin_add);
            extraSkinAddResourceIDs.put("R.id.name_lbl", R.id.name_lbl);
            extraSkinAddResourceIDs.put("R.id.source_skin_lbl", R.id.source_skin_lbl);
            extraSkinAddResourceIDs.put("R.id.description_lbl", R.id.description_lbl);


			MetrixDesignerResourceData thisSkinAddData = new MetrixDesignerResourceData(R.layout.zzmd_skin_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesSkinAdd"), extraSkinAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerSkinAddActivityResourceData", thisSkinAddData);
		}

		// Skin Colors activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerSkinColorsActivityResourceData")) {
			HashMap<String, Integer> extraSkinColorsResourceIDs = new HashMap<String, Integer>();
			extraSkinColorsResourceIDs.put("R.id.mm_skin__primary_color", R.id.mm_skin__primary_color);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__primary_color_preview", R.id.mm_skin__primary_color_preview);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__secondary_color", R.id.mm_skin__secondary_color);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__secondary_color_preview", R.id.mm_skin__secondary_color_preview);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__hyperlink_color", R.id.mm_skin__hyperlink_color);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__hyperlink_color_preview", R.id.mm_skin__hyperlink_color_preview);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__first_gradient1", R.id.mm_skin__first_gradient1);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__first_gradient2", R.id.mm_skin__first_gradient2);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__first_gradient_text", R.id.mm_skin__first_gradient_text);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__first_gradient_preview", R.id.mm_skin__first_gradient_preview);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__second_gradient1", R.id.mm_skin__second_gradient1);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__second_gradient2", R.id.mm_skin__second_gradient2);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__second_gradient_text", R.id.mm_skin__second_gradient_text);
			extraSkinColorsResourceIDs.put("R.id.mm_skin__second_gradient_preview", R.id.mm_skin__second_gradient_preview);
			extraSkinColorsResourceIDs.put("R.id.primary_color_pick", R.id.primary_color_pick);
			extraSkinColorsResourceIDs.put("R.id.secondary_color_pick", R.id.secondary_color_pick);
			extraSkinColorsResourceIDs.put("R.id.hyperlink_color_pick", R.id.hyperlink_color_pick);
			extraSkinColorsResourceIDs.put("R.id.first_gradient1_pick", R.id.first_gradient1_pick);
			extraSkinColorsResourceIDs.put("R.id.first_gradient2_pick", R.id.first_gradient2_pick);
			extraSkinColorsResourceIDs.put("R.id.first_gradient_text_pick", R.id.first_gradient_text_pick);
			extraSkinColorsResourceIDs.put("R.id.second_gradient1_pick", R.id.second_gradient1_pick);
			extraSkinColorsResourceIDs.put("R.id.second_gradient2_pick", R.id.second_gradient2_pick);
			extraSkinColorsResourceIDs.put("R.id.second_gradient_text_pick", R.id.second_gradient_text_pick);
			extraSkinColorsResourceIDs.put("R.id.hex_keyboard_view", R.id.hex_keyboard_view);
			extraSkinColorsResourceIDs.put("R.xml.hexkey", R.xml.hexkey);
			extraSkinColorsResourceIDs.put("R.id.save", R.id.save);
			extraSkinColorsResourceIDs.put("R.id.view_images", R.id.view_images);
            extraSkinColorsResourceIDs.put("R.id.colors", R.id.colors);
            extraSkinColorsResourceIDs.put("R.id.screen_info_metrix_designer_skin_colors", R.id.screen_info_metrix_designer_skin_colors);
            extraSkinColorsResourceIDs.put("R.id.primary", R.id.primary);
            extraSkinColorsResourceIDs.put("R.id.secondary", R.id.secondary);
            extraSkinColorsResourceIDs.put("R.id.hyperlink", R.id.hyperlink);
            extraSkinColorsResourceIDs.put("R.id.first_gradient", R.id.first_gradient);
            extraSkinColorsResourceIDs.put("R.id.second_gradient", R.id.second_gradient);

			MetrixDesignerResourceData thisSkinColorsData = new MetrixDesignerResourceData(R.layout.zzmd_skin_colors, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesSkinColors"), extraSkinColorsResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerSkinColorsActivityResourceData", thisSkinColorsData);
		}

		// Skin Images activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerSkinImagesActivityResourceData")) {
			HashMap<String, Integer> extraSkinImagesResourceIDs = new HashMap<String, Integer>();
			extraSkinImagesResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraSkinImagesResourceIDs.put("R.id.small_icon_select", R.id.small_icon_select);
			extraSkinImagesResourceIDs.put("R.id.small_icon_clear", R.id.small_icon_clear);
			extraSkinImagesResourceIDs.put("R.id.mm_skin__icon_small_image_id", R.id.mm_skin__icon_small_image_id);
			extraSkinImagesResourceIDs.put("R.id.icon_small_image_preview", R.id.icon_small_image_preview);
			extraSkinImagesResourceIDs.put("R.id.large_icon_select", R.id.large_icon_select);
			extraSkinImagesResourceIDs.put("R.id.large_icon_clear", R.id.large_icon_clear);
			extraSkinImagesResourceIDs.put("R.id.mm_skin__icon_large_image_id", R.id.mm_skin__icon_large_image_id);
			extraSkinImagesResourceIDs.put("R.id.icon_large_image_preview", R.id.icon_large_image_preview);
			extraSkinImagesResourceIDs.put("R.id.save", R.id.save);
			extraSkinImagesResourceIDs.put("R.id.finish", R.id.finish);
			extraSkinImagesResourceIDs.put("R.drawable.no_image24x24", R.drawable.no_image24x24);
			extraSkinImagesResourceIDs.put("R.drawable.no_image80x80", R.drawable.no_image80x80);
			extraSkinImagesResourceIDs.put("R.drawable.no_image180x120", R.drawable.no_image180x120);
			extraSkinImagesResourceIDs.put("R.drawable.no_image120x180", R.drawable.no_image120x180);

            extraSkinImagesResourceIDs.put("R.id.images", R.id.images);
            extraSkinImagesResourceIDs.put("R.id.screen_info_metrix_designer_skin_images", R.id.screen_info_metrix_designer_skin_images);
            extraSkinImagesResourceIDs.put("R.id.small_icon", R.id.small_icon);
            extraSkinImagesResourceIDs.put("R.id.large_icon", R.id.large_icon);
            
			MetrixDesignerResourceData thisSkinImagesData = new MetrixDesignerResourceData(R.layout.zzmd_skin_images, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesSkinImages"), extraSkinImagesResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerSkinImagesActivityResourceData", thisSkinImagesData);
		}

		// Design Set activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerDesignSetActivityResourceData")) {
			HashMap<String, Integer> extraDesignSetResourceIDs = new HashMap<String, Integer>();
			extraDesignSetResourceIDs.put("R.id.mm_design_set__name", R.id.mm_design_set__name);
			extraDesignSetResourceIDs.put("R.id.mm_design_set__design_set_id", R.id.mm_design_set__design_set_id);
            extraDesignSetResourceIDs.put("R.id.design_sets", R.id.design_sets);
            extraDesignSetResourceIDs.put("R.id.screen_info_metrix_designer_design_set", R.id.screen_info_metrix_designer_design_set);


			MetrixDesignerResourceData thisDesignSetData = new MetrixDesignerResourceData(R.layout.zzmd_design_set, R.id.listview, R.layout.zzmd_design_set_list_item,
                    AndroidResourceHelper.getMessage("ScrnDescMetrixDesignerDesSet"), extraDesignSetResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerDesignSetActivityResourceData", thisDesignSetData);
		}

		// Design activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerDesignActivityResourceData")) {
			HashMap<String, Integer> extraDesignResourceIDs = new HashMap<String, Integer>();
			extraDesignResourceIDs.put("R.id.mm_design__name", R.id.mm_design__name);
			extraDesignResourceIDs.put("R.id.mm_design__design_id", R.id.mm_design__design_id);
			extraDesignResourceIDs.put("R.id.mm_design__design_set_id", R.id.mm_design__design_set_id);
			extraDesignResourceIDs.put("R.id.mm_design__parent_name", R.id.mm_design__parent_name);
			extraDesignResourceIDs.put("R.id.add_design", R.id.add_design);
            extraDesignResourceIDs.put("R.id.designs", R.id.designs);
            extraDesignResourceIDs.put("R.id.screen_info_metrix_designer_design", R.id.screen_info_metrix_designer_design);

			MetrixDesignerResourceData thisDesignData = new MetrixDesignerResourceData(R.layout.zzmd_design, R.id.listview, R.layout.zzmd_design_list_item,
                    AndroidResourceHelper.getMessage("ScrnDescMetrixDesignerDes"), extraDesignResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerDesignActivityResourceData", thisDesignData);
		}

		// Design Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerDesignAddActivityResourceData")) {
			HashMap<String, Integer> extraDesignAddResourceIDs = new HashMap<String, Integer>();
			extraDesignAddResourceIDs.put("R.id.name", R.id.name);
			extraDesignAddResourceIDs.put("R.id.source_design_id", R.id.source_design_id);
			extraDesignAddResourceIDs.put("R.id.source_design_description", R.id.source_design_description);
			extraDesignAddResourceIDs.put("R.id.source_design_description_label", R.id.source_design_description_label);
			extraDesignAddResourceIDs.put("R.id.source_revision_id", R.id.source_revision_id);
			extraDesignAddResourceIDs.put("R.id.source_revision_description", R.id.source_revision_description);
			extraDesignAddResourceIDs.put("R.id.source_revision_description_label", R.id.source_revision_description_label);
			extraDesignAddResourceIDs.put("R.id.description", R.id.description);
			extraDesignAddResourceIDs.put("R.id.save", R.id.save);

            extraDesignAddResourceIDs.put("R.id.add_design", R.id.add_design);
            extraDesignAddResourceIDs.put("R.id.screen_Info_metrix_designer_design_add", R.id.screen_Info_metrix_designer_design_add);
            extraDesignAddResourceIDs.put("R.id.name66cc700a", R.id.name66cc700a);
            extraDesignAddResourceIDs.put("R.id.design", R.id.design);
            extraDesignAddResourceIDs.put("R.id.revision", R.id.revision);
            extraDesignAddResourceIDs.put("R.id.description87969ca2", R.id.description87969ca2);

			MetrixDesignerResourceData thisDesignAddData = new MetrixDesignerResourceData(R.layout.zzmd_design_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScrnDescMetrixDesignerDesAdd"), extraDesignAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerDesignAddActivityResourceData", thisDesignAddData);
		}

		// Revision activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerRevisionActivityResourceData")) {
			HashMap<String, Integer> extraRevisionResourceIDs = new HashMap<String, Integer>();
			extraRevisionResourceIDs.put("R.layout.zzmd_revision_add_dialog", R.layout.zzmd_revision_add_dialog);
			extraRevisionResourceIDs.put("R.id.mm_revision__revision_id", R.id.mm_revision__revision_id);
			extraRevisionResourceIDs.put("R.id.mm_revision__revision_number", R.id.mm_revision__revision_number);
			extraRevisionResourceIDs.put("R.id.mm_revision__status", R.id.mm_revision__status);
			extraRevisionResourceIDs.put("R.id.mm_revision__description", R.id.mm_revision__description);
			extraRevisionResourceIDs.put("R.id.add_revision", R.id.add_revision);
			extraRevisionResourceIDs.put("R.id.revision_add_description", R.id.revision_add_description);
            extraRevisionResourceIDs.put("R.id.revisions", R.id.revisions);
            extraRevisionResourceIDs.put("R.id.screen_info_metrix_designer_revision", R.id.screen_info_metrix_designer_revision);
            extraRevisionResourceIDs.put("R.id.revision_add_dialog_title", R.id.revision_add_dialog_title);
            extraRevisionResourceIDs.put("R.id.revision_add_dialog_description", R.id.revision_add_dialog_description);

			MetrixDesignerResourceData thisRevisionData = new MetrixDesignerResourceData(R.layout.zzmd_revision, R.id.listview, R.layout.zzmd_revision_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesRev"), extraRevisionResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerRevisionActivityResourceData", thisRevisionData);
		}

		// Categories activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerCategoriesActivityResourceData")) {
			HashMap<String, Integer> extraCategoriesResourceIDs = new HashMap<String, Integer>();
			extraCategoriesResourceIDs.put("R.id.category_priority_icon", R.id.category_priority_icon);
			extraCategoriesResourceIDs.put("R.id.zzmd_category", R.id.zzmd_category);
			extraCategoriesResourceIDs.put("R.drawable.categories_menus", R.drawable.categories_menus);
			extraCategoriesResourceIDs.put("R.drawable.categories_screens", R.drawable.categories_screens);
			extraCategoriesResourceIDs.put("R.drawable.categories_workflows", R.drawable.categories_workflows);
			extraCategoriesResourceIDs.put("R.id.mm_revision__skin_id", R.id.mm_revision__skin_id);
			extraCategoriesResourceIDs.put("R.id.color_preview", R.id.color_preview);
			extraCategoriesResourceIDs.put("R.id.mm_skin__primary_color", R.id.mm_skin__primary_color);
			extraCategoriesResourceIDs.put("R.id.mm_skin__secondary_color", R.id.mm_skin__secondary_color);
			extraCategoriesResourceIDs.put("R.id.mm_skin__hyperlink_color", R.id.mm_skin__hyperlink_color);
			extraCategoriesResourceIDs.put("R.id.mm_skin__first_gradient", R.id.mm_skin__first_gradient);
			extraCategoriesResourceIDs.put("R.id.mm_skin__second_gradient", R.id.mm_skin__second_gradient);
			extraCategoriesResourceIDs.put("R.id.save_skin", R.id.save_skin);
            extraCategoriesResourceIDs.put("R.id.skin", R.id.skin);
            extraCategoriesResourceIDs.put("R.id.publish_revision", R.id.publish_revision);
            extraCategoriesResourceIDs.put("R.id.categories", R.id.categories);
            extraCategoriesResourceIDs.put("R.id.screen_info_metrix_designer_categories", R.id.screen_info_metrix_designer_categories);

			MetrixDesignerResourceData thisCategoriesData = new MetrixDesignerResourceData(R.layout.zzmd_categories, R.id.listview, R.layout.zzmd_categories_list_item,
                    AndroidResourceHelper.getMessage("ScrnDescMetrixDesCat"), extraCategoriesResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerCategoriesActivityResourceData", thisCategoriesData);
		}

		// Menus activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerMenusActivityResourceData")) {
			HashMap<String, Integer> extraMenusResourceIDs = new HashMap<String, Integer>();
			extraMenusResourceIDs.put("R.id.zzmd_menu__text", R.id.zzmd_menu__text);
			extraMenusResourceIDs.put("R.id.zzmd_menu__type", R.id.zzmd_menu__type);
			extraMenusResourceIDs.put("R.id.zzmd_menu__workflow_id", R.id.zzmd_menu__workflow_id);

            extraMenusResourceIDs.put("R.id.menus", R.id.menus);
            extraMenusResourceIDs.put("R.id.screen_info_metrix_designer_menus", R.id.screen_info_metrix_designer_menus);

			MetrixDesignerResourceData thisMenusData = new MetrixDesignerResourceData(R.layout.zzmd_menus, R.id.listview, R.layout.zzmd_menus_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesMenus"), extraMenusResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerMenusActivityResourceData", thisMenusData);
		}

		// Home Menu Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerHomeMenuAddActivityResourceData")) {
			HashMap<String, Integer> extraHomeMAResourceIDs = new HashMap<String, Integer>();
			extraHomeMAResourceIDs.put("R.id.zzmd_home_item_add_label", R.id.zzmd_home_item_add_label);
			extraHomeMAResourceIDs.put("R.id.zzmd_home_item_add_emphasis", R.id.zzmd_home_item_add_emphasis);
			extraHomeMAResourceIDs.put("R.id.item_name_label", R.id.item_name_label);
			extraHomeMAResourceIDs.put("R.id.item_name", R.id.item_name);
			extraHomeMAResourceIDs.put("R.id.label_label", R.id.label_label);
			extraHomeMAResourceIDs.put("R.id.label", R.id.label);
			extraHomeMAResourceIDs.put("R.id.label_description_label", R.id.label_description_label);
			extraHomeMAResourceIDs.put("R.id.label_description", R.id.label_description);
			extraHomeMAResourceIDs.put("R.id.count_script_label", R.id.count_script_label);
			extraHomeMAResourceIDs.put("R.id.count_script", R.id.count_script);
			extraHomeMAResourceIDs.put("R.id.count_script_description_label", R.id.count_script_description_label);
			extraHomeMAResourceIDs.put("R.id.count_script_description", R.id.count_script_description);
			extraHomeMAResourceIDs.put("R.id.screen_id_label", R.id.screen_id_label);
			extraHomeMAResourceIDs.put("R.id.screen_id", R.id.screen_id);
			extraHomeMAResourceIDs.put("R.id.tap_event_label", R.id.tap_event_label);
			extraHomeMAResourceIDs.put("R.id.tap_event", R.id.tap_event);
			extraHomeMAResourceIDs.put("R.id.tap_event_description_label", R.id.tap_event_description_label);
			extraHomeMAResourceIDs.put("R.id.tap_event_description", R.id.tap_event_description);
			extraHomeMAResourceIDs.put("R.id.image_id_label", R.id.image_id_label);
			extraHomeMAResourceIDs.put("R.id.image_id_select", R.id.image_id_select);
			extraHomeMAResourceIDs.put("R.id.image_id_clear", R.id.image_id_clear);
			extraHomeMAResourceIDs.put("R.id.image_id", R.id.image_id);
			extraHomeMAResourceIDs.put("R.id.image_id_preview", R.id.image_id_preview);
			extraHomeMAResourceIDs.put("R.id.description_label", R.id.description_label);
			extraHomeMAResourceIDs.put("R.id.description", R.id.description);
			extraHomeMAResourceIDs.put("R.id.save", R.id.save);
			extraHomeMAResourceIDs.put("R.drawable.no_image80x80", R.drawable.no_image80x80);

			HashMap<String, String> extraHomeMAResourceStrings = new HashMap<String, String>();

			MetrixDesignerResourceData thisHomeMAData = new MetrixDesignerResourceData(R.layout.zzmd_home_menu_add, 0, 0,
					AndroidResourceHelper.getMessage("AddHomeMenuItemHelp"), extraHomeMAResourceIDs, extraHomeMAResourceStrings);
			MetrixPublicCache.instance.addItem("MetrixDesignerHomeMenuAddActivityResourceData", thisHomeMAData);
		}

		// Home Menu Enabling activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerHomeMenuEnablingActivityResourceData")) {
			HashMap<String, Integer> extraHomeMEResourceIDs = new HashMap<String, Integer>();
			extraHomeMEResourceIDs.put("R.id.mm_home_item__item_id", R.id.mm_home_item__item_id);
			extraHomeMEResourceIDs.put("R.id.mm_home_item__item_name", R.id.mm_home_item__item_name);
			extraHomeMEResourceIDs.put("R.id.mm_home_item__display_order", R.id.mm_home_item__display_order);
			extraHomeMEResourceIDs.put("R.id.mm_home_item__metrix_row_id", R.id.mm_home_item__metrix_row_id);
			extraHomeMEResourceIDs.put("R.id.checkboxState", R.id.checkboxState);
			extraHomeMEResourceIDs.put("R.id.add", R.id.add);
			extraHomeMEResourceIDs.put("R.id.save", R.id.save);
			extraHomeMEResourceIDs.put("R.id.next", R.id.next);

            extraHomeMEResourceIDs.put("R.id.home_menu", R.id.home_menu);
            extraHomeMEResourceIDs.put("R.id.screen_info_metrix_designer_home_menu_enabling", R.id.screen_info_metrix_designer_home_menu_enabling);

			MetrixDesignerResourceData thisHomeMEData = new MetrixDesignerResourceData(R.layout.zzmd_home_menu_enabling, R.id.listview,
					R.layout.zzmd_home_menu_enabling_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesHomeMenuEnable"), extraHomeMEResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerHomeMenuEnablingActivityResourceData", thisHomeMEData);
		}

		// Home Menu Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerHomeMenuOrderActivityResourceData")) {
			HashMap<String, Integer> extraHomeMOResourceIDs = new HashMap<String, Integer>();
			extraHomeMOResourceIDs.put("R.id.mm_home_item__item_name", R.id.mm_home_item__item_name);
			extraHomeMOResourceIDs.put("R.id.mm_home_item__item_id", R.id.mm_home_item__item_id);
			extraHomeMOResourceIDs.put("R.id.mm_home_item__display_order", R.id.mm_home_item__display_order);
			extraHomeMOResourceIDs.put("R.id.mm_home_item__metrix_row_id", R.id.mm_home_item__metrix_row_id);
			extraHomeMOResourceIDs.put("R.id.save", R.id.save);
			extraHomeMOResourceIDs.put("R.id.finish", R.id.finish);

            extraHomeMOResourceIDs.put("R.id.home_menu_order", R.id.home_menu_order);
            extraHomeMOResourceIDs.put("R.id.screen_info_metrix_designer_home_menu_order", R.id.screen_info_metrix_designer_home_menu_order);


            MetrixDesignerResourceData thisHomeMOData = new MetrixDesignerResourceData(R.layout.zzmd_home_menu_order, R.id.listview,
                    R.layout.zzmd_home_menu_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesHomeMenuOrd"), extraHomeMOResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerHomeMenuOrderActivityResourceData", thisHomeMOData);
		}

		// Home Menu Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerHomeMenuPropActivityResourceData")) {
			HashMap<String, Integer> extraHomeMPResourceIDs = new HashMap<String, Integer>();
			extraHomeMPResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraHomeMPResourceIDs.put("R.layout.zzmd_prop_edittext_line", R.layout.zzmd_prop_edittext_line);
			extraHomeMPResourceIDs.put("R.layout.zzmd_prop_image_lookup_line", R.layout.zzmd_prop_image_lookup_line);
			extraHomeMPResourceIDs.put("R.layout.zzmd_prop_longedittext_line", R.layout.zzmd_prop_longedittext_line);
			extraHomeMPResourceIDs.put("R.layout.zzmd_prop_lookup_line", R.layout.zzmd_prop_lookup_line);
			extraHomeMPResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraHomeMPResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraHomeMPResourceIDs.put("R.id.zzmd_home_menu_prop_title", R.id.zzmd_home_menu_prop_title);
			extraHomeMPResourceIDs.put("R.id.save", R.id.save);
			extraHomeMPResourceIDs.put("R.id.finish", R.id.finish);
			extraHomeMPResourceIDs.put("R.drawable.no_image80x80", R.drawable.no_image80x80);

            extraHomeMPResourceIDs.put("R.id.screen_info_metrix_designer_home_menu_prop", R.id.screen_info_metrix_designer_home_menu_prop);

			MetrixDesignerResourceData thisHomeMPData = new MetrixDesignerResourceData(R.layout.zzmd_home_menu_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesHomeMenuProp"), extraHomeMPResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerHomeMenuPropActivityResourceData", thisHomeMPData);
		}

		// Global Menu Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerGlobalMenuAddActivityResourceData")) {
			HashMap<String, Integer> extraGlobalMAResourceIDs = new HashMap<String, Integer>();
			extraGlobalMAResourceIDs.put("R.id.zzmd_menu_item_add_label", R.id.zzmd_menu_item_add_label);
			extraGlobalMAResourceIDs.put("R.id.zzmd_menu_item_add_emphasis", R.id.zzmd_menu_item_add_emphasis);
			extraGlobalMAResourceIDs.put("R.id.item_name_label", R.id.item_name_label);
			extraGlobalMAResourceIDs.put("R.id.item_name", R.id.item_name);
			extraGlobalMAResourceIDs.put("R.id.label_label", R.id.label_label);
			extraGlobalMAResourceIDs.put("R.id.label", R.id.label);
			extraGlobalMAResourceIDs.put("R.id.label_description_label", R.id.label_description_label);
			extraGlobalMAResourceIDs.put("R.id.label_description", R.id.label_description);
			extraGlobalMAResourceIDs.put("R.id.count_script_label", R.id.count_script_label);
			extraGlobalMAResourceIDs.put("R.id.count_script", R.id.count_script);
			extraGlobalMAResourceIDs.put("R.id.count_script_description_label", R.id.count_script_description_label);
			extraGlobalMAResourceIDs.put("R.id.count_script_description", R.id.count_script_description);
			extraGlobalMAResourceIDs.put("R.id.screen_id_label", R.id.screen_id_label);
			extraGlobalMAResourceIDs.put("R.id.screen_id", R.id.screen_id);
			extraGlobalMAResourceIDs.put("R.id.tap_event_label", R.id.tap_event_label);
			extraGlobalMAResourceIDs.put("R.id.tap_event", R.id.tap_event);
			extraGlobalMAResourceIDs.put("R.id.tap_event_description_label", R.id.tap_event_description_label);
			extraGlobalMAResourceIDs.put("R.id.tap_event_description", R.id.tap_event_description);
			extraGlobalMAResourceIDs.put("R.id.icon_name_label", R.id.icon_name_label);
			extraGlobalMAResourceIDs.put("R.id.icon_name", R.id.icon_name);
			extraGlobalMAResourceIDs.put("R.id.icon_name_preview", R.id.icon_name_preview);
			extraGlobalMAResourceIDs.put("R.id.hide_if_zero_label", R.id.hide_if_zero_label);
			extraGlobalMAResourceIDs.put("R.id.hide_if_zero", R.id.hide_if_zero);
			extraGlobalMAResourceIDs.put("R.id.description_label", R.id.description_label);
			extraGlobalMAResourceIDs.put("R.id.description", R.id.description);
			extraGlobalMAResourceIDs.put("R.id.save", R.id.save);
			extraGlobalMAResourceIDs.put("R.drawable.no_image24x24", R.drawable.no_image24x24);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_about", R.drawable.sliding_menu_about);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_customers", R.drawable.sliding_menu_customers);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_calendar", R.drawable.sliding_menu_calendar);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_categories", R.drawable.sliding_menu_categories);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_shift", R.drawable.sliding_menu_shift);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_query", R.drawable.sliding_menu_query);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_designer", R.drawable.sliding_menu_designer);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_escalations", R.drawable.sliding_menu_escalations);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_jobs", R.drawable.sliding_menu_jobs);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_home", R.drawable.sliding_menu_home);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_quotes", R.drawable.sliding_menu_quotes);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_preview", R.drawable.sliding_menu_preview);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_profile", R.drawable.sliding_menu_profile);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_scan_stock", R.drawable.sliding_menu_scan_stock);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_receiving", R.drawable.sliding_menu_receiving);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_settings", R.drawable.sliding_menu_settings);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_close_app", R.drawable.sliding_menu_close_app);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_sync", R.drawable.sliding_menu_sync);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_team", R.drawable.sliding_menu_team);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_skins", R.drawable.sliding_menu_skins);
			extraGlobalMAResourceIDs.put("R.drawable.sliding_menu_stock", R.drawable.sliding_menu_stock);


            HashMap<String, String> extraGlobalMAResourceStrings = new HashMap<String, String>();

			MetrixDesignerResourceData thisGlobalMAData = new MetrixDesignerResourceData(R.layout.zzmd_global_menu_add, 0, 0,
					AndroidResourceHelper.getMessage("AddGlobalMenuItemHelp"), extraGlobalMAResourceIDs, extraGlobalMAResourceStrings);
			MetrixPublicCache.instance.addItem("MetrixDesignerGlobalMenuAddActivityResourceData", thisGlobalMAData);
		}

		// Global Menu Enabling activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerGlobalMenuEnablingActivityResourceData")) {
			HashMap<String, Integer> extraGlobalMEResourceIDs = new HashMap<String, Integer>();
			extraGlobalMEResourceIDs.put("R.id.mm_menu_item__item_id", R.id.mm_menu_item__item_id);
			extraGlobalMEResourceIDs.put("R.id.mm_menu_item__item_name", R.id.mm_menu_item__item_name);
			extraGlobalMEResourceIDs.put("R.id.mm_menu_item__display_order", R.id.mm_menu_item__display_order);
			extraGlobalMEResourceIDs.put("R.id.mm_menu_item__metrix_row_id", R.id.mm_menu_item__metrix_row_id);
			extraGlobalMEResourceIDs.put("R.id.checkboxState", R.id.checkboxState);
			extraGlobalMEResourceIDs.put("R.id.add", R.id.add);
			extraGlobalMEResourceIDs.put("R.id.save", R.id.save);
			extraGlobalMEResourceIDs.put("R.id.next", R.id.next);
            extraGlobalMEResourceIDs.put("R.id.global_menu", R.id.global_menu);
            extraGlobalMEResourceIDs.put("R.id.screen_info_metrix_designer_global_menu_enabling", R.id.screen_info_metrix_designer_global_menu_enabling);

			MetrixDesignerResourceData thisGlobalMEData = new MetrixDesignerResourceData(R.layout.zzmd_global_menu_enabling, R.id.listview,
					R.layout.zzmd_global_menu_enabling_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesGblMenuEnable"), extraGlobalMEResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerGlobalMenuEnablingActivityResourceData", thisGlobalMEData);
		}

		// Global Menu Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerGlobalMenuPropActivityResourceData")) {
			HashMap<String, Integer> extraGlobalMPResourceIDs = new HashMap<String, Integer>();
			extraGlobalMPResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraGlobalMPResourceIDs.put("R.layout.zzmd_prop_edittext_line", R.layout.zzmd_prop_edittext_line);
			extraGlobalMPResourceIDs.put("R.layout.zzmd_prop_icon_line", R.layout.zzmd_prop_icon_line);
			extraGlobalMPResourceIDs.put("R.layout.zzmd_prop_longedittext_line", R.layout.zzmd_prop_longedittext_line);
			extraGlobalMPResourceIDs.put("R.layout.zzmd_prop_lookup_line", R.layout.zzmd_prop_lookup_line);
			extraGlobalMPResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraGlobalMPResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraGlobalMPResourceIDs.put("R.id.zzmd_global_menu_prop_title", R.id.zzmd_global_menu_prop_title);
			extraGlobalMPResourceIDs.put("R.id.save", R.id.save);
			extraGlobalMPResourceIDs.put("R.id.finish", R.id.finish);
			extraGlobalMPResourceIDs.put("R.drawable.no_image24x24", R.drawable.no_image24x24);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_about", R.drawable.sliding_menu_about);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_customers", R.drawable.sliding_menu_customers);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_calendar", R.drawable.sliding_menu_calendar);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_categories", R.drawable.sliding_menu_categories);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_shift", R.drawable.sliding_menu_shift);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_query", R.drawable.sliding_menu_query);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_designer", R.drawable.sliding_menu_designer);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_escalations", R.drawable.sliding_menu_escalations);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_jobs", R.drawable.sliding_menu_jobs);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_home", R.drawable.sliding_menu_home);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_quotes", R.drawable.sliding_menu_quotes);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_preview", R.drawable.sliding_menu_preview);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_profile", R.drawable.sliding_menu_profile);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_scan_stock", R.drawable.sliding_menu_scan_stock);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_receiving", R.drawable.sliding_menu_receiving);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_settings", R.drawable.sliding_menu_settings);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_close_app", R.drawable.sliding_menu_close_app);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_sync", R.drawable.sliding_menu_sync);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_team", R.drawable.sliding_menu_team);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_skins", R.drawable.sliding_menu_skins);
			extraGlobalMPResourceIDs.put("R.drawable.sliding_menu_stock", R.drawable.sliding_menu_stock);
            extraGlobalMPResourceIDs.put("R.id.screen_info_metrix_designer_global_menu_prop", R.id.screen_info_metrix_designer_global_menu_prop);

			MetrixDesignerResourceData thisGlobalMPData = new MetrixDesignerResourceData(R.layout.zzmd_global_menu_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesGblMenuProp"), extraGlobalMPResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerGlobalMenuPropActivityResourceData", thisGlobalMPData);
		}

		// Global Menu Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerGlobalMenuOrderActivityResourceData")) {
			HashMap<String, Integer> extraGlobalMOResourceIDs = new HashMap<String, Integer>();
			extraGlobalMOResourceIDs.put("R.id.mm_menu_item__item_name", R.id.mm_menu_item__item_name);
			extraGlobalMOResourceIDs.put("R.id.mm_menu_item__item_id", R.id.mm_menu_item__item_id);
			extraGlobalMOResourceIDs.put("R.id.mm_menu_item__display_order", R.id.mm_menu_item__display_order);
			extraGlobalMOResourceIDs.put("R.id.mm_menu_item__metrix_row_id", R.id.mm_menu_item__metrix_row_id);
			extraGlobalMOResourceIDs.put("R.id.save", R.id.save);
			extraGlobalMOResourceIDs.put("R.id.finish", R.id.finish);
            extraGlobalMOResourceIDs.put("R.id.global_menu_order", R.id.global_menu_order);
            extraGlobalMOResourceIDs.put("R.id.screen_info_metrix_designer_global_menu_order", R.id.screen_info_metrix_designer_global_menu_order);


			MetrixDesignerResourceData thisGlobalMOData = new MetrixDesignerResourceData(R.layout.zzmd_global_menu_order, R.id.listview,
					R.layout.zzmd_global_menu_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesGblMenuOrd"), extraGlobalMOResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerGlobalMenuOrderActivityResourceData", thisGlobalMOData);
		}

		// Workflow Menu Enabling activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerWorkflowMenuEnablingActivityResourceData")) {
			HashMap<String, Integer> extraWorkflowMEResourceIDs = new HashMap<String, Integer>();
			extraWorkflowMEResourceIDs.put("R.id.zzmd_workflow_menu_enabling_title", R.id.zzmd_workflow_menu_enabling_title);
			extraWorkflowMEResourceIDs.put("R.id.mm_screen__screen_name", R.id.mm_screen__screen_name);
			extraWorkflowMEResourceIDs.put("R.id.mm_workflow_screen__screen_id", R.id.mm_workflow_screen__screen_id);
			extraWorkflowMEResourceIDs.put("R.id.mm_workflow_screen__jump_order", R.id.mm_workflow_screen__jump_order);
			extraWorkflowMEResourceIDs.put("R.id.mm_workflow_screen__metrix_row_id", R.id.mm_workflow_screen__metrix_row_id);
			extraWorkflowMEResourceIDs.put("R.id.checkboxState", R.id.checkboxState);
			extraWorkflowMEResourceIDs.put("R.id.save", R.id.save);
			extraWorkflowMEResourceIDs.put("R.id.next", R.id.next);
            extraWorkflowMEResourceIDs.put("R.id.screen_info_metrix_designer_workflow_menu_enabling", R.id.screen_info_metrix_designer_workflow_menu_enabling);

			MetrixDesignerResourceData thisWorkflowMEData = new MetrixDesignerResourceData(R.layout.zzmd_workflow_menu_enabling, R.id.listview,
					R.layout.zzmd_workflow_menu_enabling_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesWfMenuEnable"), extraWorkflowMEResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerWorkflowMenuEnablingActivityResourceData", thisWorkflowMEData);
		}

		// Workflow Menu Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerWorkflowMenuOrderActivityResourceData")) {
			HashMap<String, Integer> extraWorkflowMOResourceIDs = new HashMap<String, Integer>();
			extraWorkflowMOResourceIDs.put("R.id.zzmd_workflow_menu_order_title", R.id.zzmd_workflow_menu_order_title);
			extraWorkflowMOResourceIDs.put("R.id.mm_screen__screen_name", R.id.mm_screen__screen_name);
			extraWorkflowMOResourceIDs.put("R.id.mm_workflow_screen__screen_id", R.id.mm_workflow_screen__screen_id);
			extraWorkflowMOResourceIDs.put("R.id.mm_workflow_screen__jump_order", R.id.mm_workflow_screen__jump_order);
			extraWorkflowMOResourceIDs.put("R.id.mm_workflow_screen__metrix_row_id", R.id.mm_workflow_screen__metrix_row_id);
			extraWorkflowMOResourceIDs.put("R.id.save", R.id.save);
			extraWorkflowMOResourceIDs.put("R.id.finish", R.id.finish);
            extraWorkflowMOResourceIDs.put("R.id.screen_info_metrix_designer_workflow_menu_order", R.id.screen_info_metrix_designer_workflow_menu_order);

			MetrixDesignerResourceData thisWorkflowMOData = new MetrixDesignerResourceData(R.layout.zzmd_workflow_menu_order, R.id.listview,
					R.layout.zzmd_workflow_menu_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesWfMenuOrd"), extraWorkflowMOResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerWorkflowMenuOrderActivityResourceData", thisWorkflowMOData);
		}

		// Workflow Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerWorkflowAddActivityResourceData")) {
			HashMap<String, Integer> extraWorkflowAddResourceIDs = new HashMap<String, Integer>();
			extraWorkflowAddResourceIDs.put("R.id.base_workflow", R.id.base_workflow);
			extraWorkflowAddResourceIDs.put("R.id.type", R.id.type);
			extraWorkflowAddResourceIDs.put("R.id.description", R.id.description);
			extraWorkflowAddResourceIDs.put("R.id.save", R.id.save);

            extraWorkflowAddResourceIDs.put("R.id.add_workflow", R.id.add_workflow);
            extraWorkflowAddResourceIDs.put("R.id.screen_info_metrix_designer_workflow_add", R.id.screen_info_metrix_designer_workflow_add);
            extraWorkflowAddResourceIDs.put("R.id.base_workflow_lbl", R.id.base_workflow_lbl);
            extraWorkflowAddResourceIDs.put("R.id.type_lbl", R.id.type_lbl);
            extraWorkflowAddResourceIDs.put("R.id.description_lbl", R.id.description_lbl);

			MetrixDesignerResourceData thisWorkflowAddData = new MetrixDesignerResourceData(R.layout.zzmd_workflow_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesWfAdd"), extraWorkflowAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerWorkflowAddActivityResourceData", thisWorkflowAddData);
		}

		// Screen activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerScreenActivityResourceData")) {
			HashMap<String, Integer> extraScreenResourceIDs = new HashMap<String, Integer>();
			extraScreenResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraScreenResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraScreenResourceIDs.put("R.drawable.pencil_purple16x16", R.drawable.pencil_purple16x16);
			extraScreenResourceIDs.put("R.drawable.pencil_gray16x16", R.drawable.pencil_gray16x16);
			extraScreenResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraScreenResourceIDs.put("R.id.mm_screen__screen_name", R.id.mm_screen__screen_name);
			extraScreenResourceIDs.put("R.id.mm_screen__description", R.id.mm_screen__description);
			extraScreenResourceIDs.put("R.id.mm_screen__metrix_row_id", R.id.mm_screen__metrix_row_id);
			extraScreenResourceIDs.put("R.id.mm_screen__screen_id", R.id.mm_screen__screen_id);
			extraScreenResourceIDs.put("R.id.mm_screen__is_baseline", R.id.mm_screen__is_baseline);
			extraScreenResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraScreenResourceIDs.put("R.id.is_edited_icon", R.id.is_edited_icon);
			extraScreenResourceIDs.put("R.id.add_screen", R.id.add_screen);
            extraScreenResourceIDs.put("R.id.screens", R.id.screens);
            extraScreenResourceIDs.put("R.id.screen_info_metrix_designer_screen", R.id.screen_info_metrix_designer_screen);

			MetrixDesignerResourceData thisScreenData = new MetrixDesignerResourceData(R.layout.zzmd_screen, R.id.listview, R.layout.zzmd_screen_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesScn"), extraScreenResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerScreenActivityResourceData", thisScreenData);
		}

		// Screen Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerScreenAddActivityResourceData")) {
			HashMap<String, Integer> extraScreenAddResourceIDs = new HashMap<String, Integer>();
			extraScreenAddResourceIDs.put("R.id.screen_name", R.id.screen_name);
			extraScreenAddResourceIDs.put("R.id.screen_type", R.id.screen_type);
			extraScreenAddResourceIDs.put("R.id.primary_table_label", R.id.primary_table_label);
			extraScreenAddResourceIDs.put("R.id.primary_table", R.id.primary_table);
			extraScreenAddResourceIDs.put("R.id.workflow_label", R.id.workflow_label);
			extraScreenAddResourceIDs.put("R.id.workflow", R.id.workflow);
			extraScreenAddResourceIDs.put("R.id.tab_parent_label", R.id.tab_parent_label);
			extraScreenAddResourceIDs.put("R.id.tab_parent", R.id.tab_parent);
			extraScreenAddResourceIDs.put("R.id.menu_option_label", R.id.menu_option_label);
			extraScreenAddResourceIDs.put("R.id.menu_option", R.id.menu_option);
			extraScreenAddResourceIDs.put("R.id.tab_child_label", R.id.tab_child_label);
			extraScreenAddResourceIDs.put("R.id.tab_child", R.id.tab_child);
			extraScreenAddResourceIDs.put("R.id.description", R.id.description);
			extraScreenAddResourceIDs.put("R.id.save", R.id.save);

            extraScreenAddResourceIDs.put("R.id.add_screen", R.id.add_screen);
            extraScreenAddResourceIDs.put("R.id.screen_info_metrix_designer_screen_add", R.id.screen_info_metrix_designer_screen_add);
            extraScreenAddResourceIDs.put("R.id.name", R.id.name);
            extraScreenAddResourceIDs.put("R.id.description_label", R.id.description_label);
            extraScreenAddResourceIDs.put("R.id.screen_type_label", R.id.screen_type_label);

			MetrixDesignerResourceData thisScreenAddData = new MetrixDesignerResourceData(R.layout.zzmd_screen_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesScnAdd"), extraScreenAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerScreenAddActivityResourceData", thisScreenAddData);
		}

		// Screen Item activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerScreenItemActivityResourceData")) {
			HashMap<String, Integer> extraScreenItemResourceIDs = new HashMap<String, Integer>();
			extraScreenItemResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraScreenItemResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraScreenItemResourceIDs.put("R.drawable.pencil_purple16x16", R.drawable.pencil_purple16x16);
			extraScreenItemResourceIDs.put("R.drawable.pencil_gray16x16", R.drawable.pencil_gray16x16);
			extraScreenItemResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraScreenItemResourceIDs.put("R.id.zzmd_screen_item_title", R.id.zzmd_screen_item_title);
			extraScreenItemResourceIDs.put("R.id.mm_screen_item__item_name", R.id.mm_screen_item__item_name);
			extraScreenItemResourceIDs.put("R.id.mm_screen_item__description", R.id.mm_screen_item__description);
			extraScreenItemResourceIDs.put("R.id.mm_screen_item__metrix_row_id", R.id.mm_screen_item__metrix_row_id);
			extraScreenItemResourceIDs.put("R.id.mm_screen_item__item_id", R.id.mm_screen_item__item_id);
			extraScreenItemResourceIDs.put("R.id.mm_screen_item__is_baseline", R.id.mm_screen_item__is_baseline);
			extraScreenItemResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraScreenItemResourceIDs.put("R.id.is_edited_icon", R.id.is_edited_icon);
			extraScreenItemResourceIDs.put("R.id.add_screen_item", R.id.add_screen_item);

            extraScreenItemResourceIDs.put("R.id.screen_info_metrix_designer_screen_item", R.id.screen_info_metrix_designer_screen_item);

			MetrixDesignerResourceData thisScreenItemData = new MetrixDesignerResourceData(R.layout.zzmd_screen_item, R.id.listview, R.layout.zzmd_screen_item_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesScnItm"), extraScreenItemResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerScreenItemActivityResourceData", thisScreenItemData);
		}

		// Screen Item Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerScreenItemAddActivityResourceData")) {
			HashMap<String, Integer> extraScreenItemAddResourceIDs = new HashMap<String, Integer>();
			extraScreenItemAddResourceIDs.put("R.id.zzmd_screen_item_add_emphasis", R.id.zzmd_screen_item_add_emphasis);
			extraScreenItemAddResourceIDs.put("R.id.item_type", R.id.item_type);
			extraScreenItemAddResourceIDs.put("R.id.item_name", R.id.item_name);
			extraScreenItemAddResourceIDs.put("R.id.event_type", R.id.event_type);
			extraScreenItemAddResourceIDs.put("R.id.event", R.id.event);
			extraScreenItemAddResourceIDs.put("R.id.event_description_label", R.id.event_description_label);
			extraScreenItemAddResourceIDs.put("R.id.event_description", R.id.event_description);
			extraScreenItemAddResourceIDs.put("R.id.description", R.id.description);
			extraScreenItemAddResourceIDs.put("R.id.save", R.id.save);
            extraScreenItemAddResourceIDs.put("R.id.add_screen_item", R.id.add_screen_item);
            extraScreenItemAddResourceIDs.put("R.id.item_type_label", R.id.item_type_label);
            extraScreenItemAddResourceIDs.put("R.id.item_name_label", R.id.item_name_label);
            extraScreenItemAddResourceIDs.put("R.id.event_type_label", R.id.event_type_label);
            extraScreenItemAddResourceIDs.put("R.id.event_label", R.id.event_label);
            extraScreenItemAddResourceIDs.put("R.id.description_label", R.id.description_label);

			MetrixDesignerResourceData thisScreenItemAddData = new MetrixDesignerResourceData(R.layout.zzmd_screen_item_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesScnItmAdd"), extraScreenItemAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerScreenItemAddActivityResourceData", thisScreenItemAddData);
		}

		// Screen Item Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerScreenItemPropActivityResourceData")) {
			HashMap<String, Integer> extraScreenItemPropResourceIDs = new HashMap<String, Integer>();
			extraScreenItemPropResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraScreenItemPropResourceIDs.put("R.layout.zzmd_prop_edittext_line", R.layout.zzmd_prop_edittext_line);
			extraScreenItemPropResourceIDs.put("R.layout.zzmd_prop_longedittext_line", R.layout.zzmd_prop_longedittext_line);
			extraScreenItemPropResourceIDs.put("R.layout.zzmd_prop_lookup_line", R.layout.zzmd_prop_lookup_line);
			extraScreenItemPropResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraScreenItemPropResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);
			extraScreenItemPropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraScreenItemPropResourceIDs.put("R.id.zzmd_screen_item_prop_emphasis", R.id.zzmd_screen_item_prop_emphasis);
			extraScreenItemPropResourceIDs.put("R.id.save", R.id.save);
			extraScreenItemPropResourceIDs.put("R.id.finish", R.id.finish);
            extraScreenItemPropResourceIDs.put("R.id.screen_item_properties", R.id.screen_item_properties);

			MetrixDesignerResourceData thisScreenItemPropData = new MetrixDesignerResourceData(R.layout.zzmd_screen_item_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesScnItmProp"), extraScreenItemPropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerScreenItemPropActivityResourceData", thisScreenItemPropData);
		}

		// Filter/Sort Enabling activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFilterSortEnablingActivityResourceData")) {
			HashMap<String, Integer> extraFSEResourceIDs = new HashMap<String, Integer>();
			extraFSEResourceIDs.put("R.id.mm_filter_sort_item__item_id", R.id.mm_filter_sort_item__item_id);
			extraFSEResourceIDs.put("R.id.mm_filter_sort_item__item_name", R.id.mm_filter_sort_item__item_name);
			extraFSEResourceIDs.put("R.id.mm_filter_sort_item__display_order", R.id.mm_filter_sort_item__display_order);
			extraFSEResourceIDs.put("R.id.mm_filter_sort_item__metrix_row_id", R.id.mm_filter_sort_item__metrix_row_id);
			extraFSEResourceIDs.put("R.id.checkboxState", R.id.checkboxState);
			extraFSEResourceIDs.put("R.id.filter_sort_toggle", R.id.filter_sort_toggle);
			extraFSEResourceIDs.put("R.id.add", R.id.add);
			extraFSEResourceIDs.put("R.id.save", R.id.save);
			extraFSEResourceIDs.put("R.id.next", R.id.next);
			extraFSEResourceIDs.put("R.id.filter_sort_enabling_label", R.id.filter_sort_enabling_label);
			extraFSEResourceIDs.put("R.id.filter_sort_enabling_tip", R.id.filter_sort_enabling_tip);

			MetrixDesignerResourceData thisFSEData = new MetrixDesignerResourceData(R.layout.zzmd_filter_sort_enabling, R.id.listview,
					R.layout.zzmd_filter_sort_enabling_list_item,
					AndroidResourceHelper.getMessage("FilterSortEnableHelp"), extraFSEResourceIDs, null);
			MetrixPublicCache.instance.addItem("MetrixDesignerFilterSortEnablingActivityResourceData", thisFSEData);
		}

		// Filter/Sort Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFilterSortPropActivityResourceData")) {
			HashMap<String, Integer> extraFSPResourceIDs = new HashMap<String, Integer>();
			extraFSPResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraFSPResourceIDs.put("R.layout.zzmd_prop_edittext_imageview_line", R.layout.zzmd_prop_edittext_imageview_line);
			extraFSPResourceIDs.put("R.layout.zzmd_prop_lookup_line", R.layout.zzmd_prop_lookup_line);
			extraFSPResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);
			extraFSPResourceIDs.put("R.id.zzmd_filter_sort_prop_label", R.id.zzmd_filter_sort_prop_label);
			extraFSPResourceIDs.put("R.id.zzmd_filter_sort_prop_tip", R.id.zzmd_filter_sort_prop_tip);
			extraFSPResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraFSPResourceIDs.put("R.id.save", R.id.save);
			extraFSPResourceIDs.put("R.id.finish", R.id.finish);

			MetrixDesignerResourceData thisFSPData = new MetrixDesignerResourceData(R.layout.zzmd_filter_sort_prop, 0, 0,
					AndroidResourceHelper.getMessage("FilterSortPropHelp"), extraFSPResourceIDs, null);
			MetrixPublicCache.instance.addItem("MetrixDesignerFilterSortPropActivityResourceData", thisFSPData);
		}

		// Filter/Sort Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFilterSortAddActivityResourceData")) {
			HashMap<String, Integer> extraFSAResourceIDs = new HashMap<String, Integer>();
			extraFSAResourceIDs.put("R.id.zzmd_filter_sort_add_label", R.id.zzmd_filter_sort_add_label);
			extraFSAResourceIDs.put("R.id.zzmd_filter_sort_add_tip", R.id.zzmd_filter_sort_add_tip);
			extraFSAResourceIDs.put("R.id.item_name_label", R.id.item_name_label);
			extraFSAResourceIDs.put("R.id.item_name", R.id.item_name);
			extraFSAResourceIDs.put("R.id.item_type_label", R.id.item_type_label);
			extraFSAResourceIDs.put("R.id.item_type", R.id.item_type);
			extraFSAResourceIDs.put("R.id.label_label", R.id.label_label);
			extraFSAResourceIDs.put("R.id.label", R.id.label);
			extraFSAResourceIDs.put("R.id.label_description_label", R.id.label_description_label);
			extraFSAResourceIDs.put("R.id.label_description", R.id.label_description);
			extraFSAResourceIDs.put("R.id.content_label", R.id.content_label);
			extraFSAResourceIDs.put("R.id.content", R.id.content);
			extraFSAResourceIDs.put("R.id.content_button", R.id.content_button);
			extraFSAResourceIDs.put("R.id.content_description_label", R.id.content_description_label);
			extraFSAResourceIDs.put("R.id.content_description", R.id.content_description);
			extraFSAResourceIDs.put("R.id.is_default_label", R.id.is_default_label);
			extraFSAResourceIDs.put("R.id.is_default", R.id.is_default);
			extraFSAResourceIDs.put("R.id.full_filter_label", R.id.full_filter_label);
			extraFSAResourceIDs.put("R.id.full_filter", R.id.full_filter);
			extraFSAResourceIDs.put("R.id.save", R.id.save);

			MetrixDesignerResourceData thisFSAData = new MetrixDesignerResourceData(R.layout.zzmd_filter_sort_add, 0, 0,
					AndroidResourceHelper.getMessage("AddFilterSortHelp"), extraFSAResourceIDs, null);
			MetrixPublicCache.instance.addItem("MetrixDesignerFilterSortAddActivityResourceData", thisFSAData);
		}

		// Filter/Sort Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFilterSortOrderActivityResourceData")) {
			HashMap<String, Integer> extraFSOResourceIDs = new HashMap<String, Integer>();
			extraFSOResourceIDs.put("R.id.mm_filter_sort_item__item_name", R.id.mm_filter_sort_item__item_name);
			extraFSOResourceIDs.put("R.id.mm_filter_sort_item__item_id", R.id.mm_filter_sort_item__item_id);
			extraFSOResourceIDs.put("R.id.mm_filter_sort_item__display_order", R.id.mm_filter_sort_item__display_order);
			extraFSOResourceIDs.put("R.id.mm_filter_sort_item__metrix_row_id", R.id.mm_filter_sort_item__metrix_row_id);
			extraFSOResourceIDs.put("R.id.filter_sort_order_label", R.id.filter_sort_order_label);
			extraFSOResourceIDs.put("R.id.filter_sort_order_tip", R.id.filter_sort_order_tip);
			extraFSOResourceIDs.put("R.id.filter_sort_toggle", R.id.filter_sort_toggle);
			extraFSOResourceIDs.put("R.id.save", R.id.save);
			extraFSOResourceIDs.put("R.id.finish", R.id.finish);

			MetrixDesignerResourceData thisFSOData = new MetrixDesignerResourceData(R.layout.zzmd_filter_sort_order, R.id.listview,
					R.layout.zzmd_filter_sort_order_list_item,
					AndroidResourceHelper.getMessage("FilterSortOrderHelp"), extraFSOResourceIDs, null);
			MetrixPublicCache.instance.addItem("MetrixDesignerFilterSortOrderActivityResourceData", thisFSOData);
		}

		// Screen Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerScreenPropActivityResourceData")) {
			HashMap<String, Integer> extraScreenPropResourceIDs = new HashMap<String, Integer>();
			extraScreenPropResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraScreenPropResourceIDs.put("R.layout.zzmd_prop_crud_button_line", R.layout.zzmd_prop_crud_button_line);
			extraScreenPropResourceIDs.put("R.layout.zzmd_prop_edittext_line", R.layout.zzmd_prop_edittext_line);
			extraScreenPropResourceIDs.put("R.layout.zzmd_prop_longedittext_line", R.layout.zzmd_prop_longedittext_line);
			extraScreenPropResourceIDs.put("R.layout.zzmd_prop_lookup_line", R.layout.zzmd_prop_lookup_line);
			extraScreenPropResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraScreenPropResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);
			extraScreenPropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraScreenPropResourceIDs.put("R.id.zzmd_screen_prop_title", R.id.zzmd_screen_prop_title);
			extraScreenPropResourceIDs.put("R.id.save", R.id.save);
			extraScreenPropResourceIDs.put("R.id.view_items", R.id.view_items);
			extraScreenPropResourceIDs.put("R.id.view_fields", R.id.view_fields);
            extraScreenPropResourceIDs.put("R.id.screen_info_metrix_designer_screen_prop", R.id.screen_info_metrix_designer_screen_prop);

			MetrixDesignerResourceData thisScreenPropData = new MetrixDesignerResourceData(R.layout.zzmd_screen_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesScnProp"), extraScreenPropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerScreenPropActivityResourceData", thisScreenPropData);
		}

		// Screen Tab Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerScreenTabOrderActivityResourceData")) {
			HashMap<String, Integer> extraScreenTabOrderResourceIDs = new HashMap<String, Integer>();
			extraScreenTabOrderResourceIDs.put("R.id.zzmd_screen_tab_order_title", R.id.zzmd_screen_tab_order_title);
			extraScreenTabOrderResourceIDs.put("R.id.mm_screen__screen_name", R.id.mm_screen__screen_name);
			extraScreenTabOrderResourceIDs.put("R.id.mm_screen__screen_id", R.id.mm_screen__screen_id);
			extraScreenTabOrderResourceIDs.put("R.id.mm_screen__tab_order", R.id.mm_screen__tab_order);
			extraScreenTabOrderResourceIDs.put("R.id.mm_screen__metrix_row_id", R.id.mm_screen__metrix_row_id);
			extraScreenTabOrderResourceIDs.put("R.id.save", R.id.save);
			extraScreenTabOrderResourceIDs.put("R.id.finish", R.id.finish);
            extraScreenTabOrderResourceIDs.put("R.id.screen_info_metrix_designer_screen_tab_order", R.id.screen_info_metrix_designer_screen_tab_order);

			MetrixDesignerResourceData thisScreenTabOrderData = new MetrixDesignerResourceData(R.layout.zzmd_screen_tab_order, R.id.listview,
					R.layout.zzmd_screen_tab_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesScnTabOrd"), extraScreenTabOrderResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerScreenTabOrderActivityResourceData", thisScreenTabOrderData);
		}

		// Field activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldActivityResourceData")) {
			HashMap<String, Integer> extraFieldResourceIDs = new HashMap<String, Integer>();
			extraFieldResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraFieldResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraFieldResourceIDs.put("R.drawable.pencil_purple16x16", R.drawable.pencil_purple16x16);
			extraFieldResourceIDs.put("R.drawable.pencil_gray16x16", R.drawable.pencil_gray16x16);
			extraFieldResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraFieldResourceIDs.put("R.id.zzmd_field_title", R.id.zzmd_field_title);
			extraFieldResourceIDs.put("R.id.mm_field__field_name", R.id.mm_field__field_name);
			extraFieldResourceIDs.put("R.id.mm_field__description", R.id.mm_field__description);
			extraFieldResourceIDs.put("R.id.mm_field__metrix_row_id", R.id.mm_field__metrix_row_id);
			extraFieldResourceIDs.put("R.id.mm_field__field_id", R.id.mm_field__field_id);
			extraFieldResourceIDs.put("R.id.mm_field__is_baseline", R.id.mm_field__is_baseline);
			extraFieldResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraFieldResourceIDs.put("R.id.is_edited_icon", R.id.is_edited_icon);
			extraFieldResourceIDs.put("R.id.has_lookup_icon", R.id.has_lookup_icon);
			extraFieldResourceIDs.put("R.id.add_field", R.id.add_field);
			extraFieldResourceIDs.put("R.id.field_order", R.id.field_order);
			extraFieldResourceIDs.put("R.id.is_button_field_icon", R.id.is_button_field_icon);
            extraFieldResourceIDs.put("R.id.screen_info_metrix_designer_field", R.id.screen_info_metrix_designer_field);

			MetrixDesignerResourceData thisFieldData = new MetrixDesignerResourceData(R.layout.zzmd_field, R.id.listview, R.layout.zzmd_field_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFld"), extraFieldResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldActivityResourceData", thisFieldData);
		}

		// Field Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldAddActivityResourceData")) {
			HashMap<String, Integer> extraFieldAddResourceIDs = new HashMap<String, Integer>();
			extraFieldAddResourceIDs.put("R.id.add_field", R.id.add_field);
			extraFieldAddResourceIDs.put("R.id.zzmd_field_add_emphasis", R.id.zzmd_field_add_emphasis);
			extraFieldAddResourceIDs.put("R.id.custom_field", R.id.custom_field);
			extraFieldAddResourceIDs.put("R.id.is_custom_field", R.id.is_custom_field);
			extraFieldAddResourceIDs.put("R.id.table_name_label", R.id.table_name_label);
			extraFieldAddResourceIDs.put("R.id.table_name", R.id.table_name);
			extraFieldAddResourceIDs.put("R.id.custom_column_name_label", R.id.custom_column_name_label);
			extraFieldAddResourceIDs.put("R.id.custom_column_name", R.id.custom_column_name);
			extraFieldAddResourceIDs.put("R.id.description", R.id.description);
			extraFieldAddResourceIDs.put("R.id.description_label", R.id.description_label);
			extraFieldAddResourceIDs.put("R.id.save", R.id.save);
			extraFieldAddResourceIDs.put("R.id.is_button", R.id.is_button);
			extraFieldAddResourceIDs.put("R.id.is_button_label", R.id.is_button_label);
			extraFieldAddResourceIDs.put("R.id.mm_field__field_name", R.id.mm_field__field_name);
			extraFieldAddResourceIDs.put("R.id.checkboxState", R.id.checkboxState);
			extraFieldAddResourceIDs.put("R.id.listview_area", R.id.listview_area);
			extraFieldAddResourceIDs.put("R.id.is_select_all_fields", R.id.is_select_all_fields);
			extraFieldAddResourceIDs.put("R.id.select_all_fields_label", R.id.select_all_fields_label);
			extraFieldAddResourceIDs.put("R.id.attachment_field", R.id.attachment_field);
			extraFieldAddResourceIDs.put("R.layout.zzmd_field_add_attachment_dialog", R.layout.zzmd_field_add_attachment_dialog);
			extraFieldAddResourceIDs.put("R.id.field_add_attachment_dialog_title", R.id.field_add_attachment_dialog_title);
			extraFieldAddResourceIDs.put("R.id.faad_control_type_label", R.id.faad_control_type_label);
			extraFieldAddResourceIDs.put("R.id.faad_control_type", R.id.faad_control_type);
			extraFieldAddResourceIDs.put("R.id.faad_column_name_label", R.id.faad_column_name_label);
			extraFieldAddResourceIDs.put("R.id.faad_column_name", R.id.faad_column_name);
			extraFieldAddResourceIDs.put("R.id.faad_description_label", R.id.faad_description_label);
			extraFieldAddResourceIDs.put("R.id.faad_description", R.id.faad_description);
			extraFieldAddResourceIDs.put("R.layout.zzmd_field_add_desc_dialog", R.layout.zzmd_field_add_desc_dialog);
			extraFieldAddResourceIDs.put("R.id.field_add_description", R.id.field_add_description);
			extraFieldAddResourceIDs.put("R.id.field_name_label", R.id.field_name_label);
			extraFieldAddResourceIDs.put("R.id.scrollview", R.id.scrollview);
            extraFieldAddResourceIDs.put("R.id.save", R.id.save);

			MetrixDesignerResourceData thisFieldAddData = new MetrixDesignerResourceData(R.layout.zzmd_field_add, R.id.listview, R.layout.zzmd_add_field_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldAdd"), extraFieldAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldAddActivityResourceData", thisFieldAddData);
		}

		// Field Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldOrderActivityResourceData")) {
			HashMap<String, Integer> extraFieldOrderResourceIDs = new HashMap<String, Integer>();
			extraFieldOrderResourceIDs.put("R.id.zzmd_field_order_title", R.id.zzmd_field_order_title);
			extraFieldOrderResourceIDs.put("R.id.mm_field__field_name", R.id.mm_field__field_name);
			extraFieldOrderResourceIDs.put("R.id.mm_field__field_id", R.id.mm_field__field_id);
			extraFieldOrderResourceIDs.put("R.id.mm_field__display_order", R.id.mm_field__display_order);
			extraFieldOrderResourceIDs.put("R.id.mm_field__metrix_row_id", R.id.mm_field__metrix_row_id);
			extraFieldOrderResourceIDs.put("R.id.save", R.id.save);
			extraFieldOrderResourceIDs.put("R.id.finish", R.id.finish);
            extraFieldOrderResourceIDs.put("R.id.screen_info_metrix_designer_field_order", R.id.screen_info_metrix_designer_field_order);

			MetrixDesignerResourceData thisFieldOrderData = new MetrixDesignerResourceData(R.layout.zzmd_field_order, R.id.listview,
					R.layout.zzmd_field_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldOrd"), extraFieldOrderResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldOrderActivityResourceData", thisFieldOrderData);
		}

		// Field Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldPropActivityResourceData")) {
			HashMap<String, Integer> extraFieldPropResourceIDs = new HashMap<String, Integer>();
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_crud_button_line", R.layout.zzmd_prop_crud_button_line);
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_edittext_line", R.layout.zzmd_prop_edittext_line);
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_longedittext_line", R.layout.zzmd_prop_longedittext_line);
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_lookup_line", R.layout.zzmd_prop_lookup_line);
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);
			extraFieldPropResourceIDs.put("R.layout.zzmd_prop_edittext_imageview_line", R.layout.zzmd_prop_edittext_imageview_line);
			extraFieldPropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraFieldPropResourceIDs.put("R.id.zzmd_field_prop_emphasis", R.id.zzmd_field_prop_emphasis);
			extraFieldPropResourceIDs.put("R.id.save", R.id.save);
			extraFieldPropResourceIDs.put("R.id.finish", R.id.finish);
            extraFieldPropResourceIDs.put("R.id.screenTitle", R.id.screenTitle);

			MetrixDesignerResourceData thisFieldPropData = new MetrixDesignerResourceData(R.layout.zzmd_field_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldProp"), extraFieldPropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldPropActivityResourceData", thisFieldPropData);
		}

		// Field Lookup Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupAddActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupAddResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupAddResourceIDs.put("R.id.zzmd_field_lookup_add_emphasis", R.id.zzmd_field_lookup_add_emphasis);
            extraFieldLookupAddResourceIDs.put("R.id.title_txt", R.id.title_txt);
            extraFieldLookupAddResourceIDs.put("R.id.title_description_label", R.id.title_description_label);
			extraFieldLookupAddResourceIDs.put("R.id.title_description", R.id.title_description);
            extraFieldLookupAddResourceIDs.put("R.id.initial_search_chk", R.id.initial_search_chk);
            extraFieldLookupAddResourceIDs.put("R.id.initial_table_spn", R.id.initial_table_spn);
            extraFieldLookupAddResourceIDs.put("R.id.save", R.id.save);

            extraFieldLookupAddResourceIDs.put("R.id.add_field_lookup", R.id.add_field_lookup);
            extraFieldLookupAddResourceIDs.put("R.id.title_lbl", R.id.title_lbl);
            extraFieldLookupAddResourceIDs.put("R.id.initial_search_lbl", R.id.initial_search_lbl);
            extraFieldLookupAddResourceIDs.put("R.id.initial_table_lbl", R.id.initial_table_lbl);

			MetrixDesignerResourceData thisFieldLookupAddData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupAdd"), extraFieldLookupAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupAddActivityResourceData", thisFieldLookupAddData);
		}

		// Field Lookup Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupPropActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupPropResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupPropResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraFieldLookupPropResourceIDs.put("R.layout.zzmd_prop_lookup_line", R.layout.zzmd_prop_lookup_line);
			extraFieldLookupPropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraFieldLookupPropResourceIDs.put("R.id.zzmd_field_lookup_prop_emphasis", R.id.zzmd_field_lookup_prop_emphasis);
			extraFieldLookupPropResourceIDs.put("R.id.validate", R.id.validate);
			extraFieldLookupPropResourceIDs.put("R.id.view_tables", R.id.view_tables);
			extraFieldLookupPropResourceIDs.put("R.id.view_filters", R.id.view_filters);
			extraFieldLookupPropResourceIDs.put("R.id.view_orderby", R.id.view_orderby);
			extraFieldLookupPropResourceIDs.put("R.id.save", R.id.save);
            extraFieldLookupPropResourceIDs.put("R.id.screen_title", R.id.screen_title);

			MetrixDesignerResourceData thisFieldLookupPropData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupProp"), extraFieldLookupPropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupPropActivityResourceData", thisFieldLookupPropData);
		}

		// Field Lookup Table activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupTableActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupTableResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupTableResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraFieldLookupTableResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraFieldLookupTableResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraFieldLookupTableResourceIDs.put("R.id.zzmd_field_lookup_table_emphasis", R.id.zzmd_field_lookup_table_emphasis);
			extraFieldLookupTableResourceIDs.put("R.id.mm_field_lkup_table__table_name", R.id.mm_field_lkup_table__table_name);
			extraFieldLookupTableResourceIDs.put("R.id.mm_field_lkup_table__metrix_row_id", R.id.mm_field_lkup_table__metrix_row_id);
			extraFieldLookupTableResourceIDs.put("R.id.mm_field_lkup_table__lkup_table_id", R.id.mm_field_lkup_table__lkup_table_id);
			extraFieldLookupTableResourceIDs.put("R.id.mm_field_lkup_table__is_baseline", R.id.mm_field_lkup_table__is_baseline);
			extraFieldLookupTableResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraFieldLookupTableResourceIDs.put("R.id.add_table", R.id.add_table);
            extraFieldLookupTableResourceIDs.put("R.id.lookup_tables", R.id.lookup_tables);

			MetrixDesignerResourceData thisFieldLookupTableData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_table, R.id.listview, R.layout.zzmd_field_lookup_table_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupTbl"), extraFieldLookupTableResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupTableActivityResourceData", thisFieldLookupTableData);
		}

		// Field Lookup Table Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupTableAddActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupTableAddResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupTableAddResourceIDs.put("R.id.zzmd_field_lookup_table_add_emphasis", R.id.zzmd_field_lookup_table_add_emphasis);
			extraFieldLookupTableAddResourceIDs.put("R.id.table", R.id.table);
			extraFieldLookupTableAddResourceIDs.put("R.id.parent_table", R.id.parent_table);
			extraFieldLookupTableAddResourceIDs.put("R.id.parent_key_columns", R.id.parent_key_columns);
			extraFieldLookupTableAddResourceIDs.put("R.id.child_key_columns", R.id.child_key_columns);
			extraFieldLookupTableAddResourceIDs.put("R.id.save", R.id.save);
            extraFieldLookupTableAddResourceIDs.put("R.id.add_lookup_table", R.id.add_lookup_table);
            extraFieldLookupTableAddResourceIDs.put("R.id.table_lbl", R.id.table_lbl);
            extraFieldLookupTableAddResourceIDs.put("R.id.parent_table_lbl", R.id.parent_table_lbl);
            extraFieldLookupTableAddResourceIDs.put("R.id.parent_columns_lbl", R.id.parent_columns_lbl);
            extraFieldLookupTableAddResourceIDs.put("R.id.child_columns_lbl", R.id.child_columns_lbl);

			MetrixDesignerResourceData thisFieldLookupTableAddData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_table_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupTblAdd"), extraFieldLookupTableAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupTableAddActivityResourceData", thisFieldLookupTableAddData);
		}

		// Field Lookup Table Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupTablePropActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupTablePropResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupTablePropResourceIDs.put("R.layout.zzmd_prop_edittext_line", R.layout.zzmd_prop_edittext_line);
			extraFieldLookupTablePropResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraFieldLookupTablePropResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);

			extraFieldLookupTablePropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraFieldLookupTablePropResourceIDs.put("R.id.zzmd_field_lookup_table_prop_emphasis", R.id.zzmd_field_lookup_table_prop_emphasis);
			extraFieldLookupTablePropResourceIDs.put("R.id.save", R.id.save);
			extraFieldLookupTablePropResourceIDs.put("R.id.view_columns", R.id.view_columns);
            extraFieldLookupTablePropResourceIDs.put("R.id.lookup_table_properties", R.id.lookup_table_properties);

            MetrixDesignerResourceData thisFieldLookupTablePropData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_table_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupTblProp"), extraFieldLookupTablePropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupTablePropActivityResourceData", thisFieldLookupTablePropData);
		}

		// Field Lookup Column activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupColumnActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupColumnResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupColumnResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraFieldLookupColumnResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraFieldLookupColumnResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraFieldLookupColumnResourceIDs.put("R.id.zzmd_field_lookup_column_emphasis", R.id.zzmd_field_lookup_column_emphasis);
			extraFieldLookupColumnResourceIDs.put("R.id.mm_field_lkup_column__column_name", R.id.mm_field_lkup_column__column_name);
			extraFieldLookupColumnResourceIDs.put("R.id.mm_field_lkup_column__metrix_row_id", R.id.mm_field_lkup_column__metrix_row_id);
			extraFieldLookupColumnResourceIDs.put("R.id.mm_field_lkup_column__lkup_column_id", R.id.mm_field_lkup_column__lkup_column_id);
			extraFieldLookupColumnResourceIDs.put("R.id.mm_field_lkup_column__is_baseline", R.id.mm_field_lkup_column__is_baseline);
			extraFieldLookupColumnResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraFieldLookupColumnResourceIDs.put("R.id.add_column", R.id.add_column);
			extraFieldLookupColumnResourceIDs.put("R.id.column_order", R.id.column_order);

            extraFieldLookupColumnResourceIDs.put("R.id.lookup_columns", R.id.lookup_columns);
            extraFieldLookupColumnResourceIDs.put("R.id.add_column", R.id.add_column);
            extraFieldLookupColumnResourceIDs.put("R.id.column_order", R.id.column_order);

			MetrixDesignerResourceData thisFieldLookupColumnData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_column, R.id.listview, R.layout.zzmd_field_lookup_column_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupCol"), extraFieldLookupColumnResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupColumnActivityResourceData", thisFieldLookupColumnData);
		}

		// Field Lookup Column Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupColumnAddActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupColumnAddResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupColumnAddResourceIDs.put("R.id.zzmd_field_lookup_column_add_emphasis", R.id.zzmd_field_lookup_column_add_emphasis);
			extraFieldLookupColumnAddResourceIDs.put("R.id.column", R.id.column);
			extraFieldLookupColumnAddResourceIDs.put("R.id.linked_field_id", R.id.linked_field_id);
			extraFieldLookupColumnAddResourceIDs.put("R.id.always_hide", R.id.always_hide);
			extraFieldLookupColumnAddResourceIDs.put("R.id.save", R.id.save);

            extraFieldLookupColumnAddResourceIDs.put("R.id.add_lookup_column", R.id.add_lookup_column);
            extraFieldLookupColumnAddResourceIDs.put("R.id.column_lbl", R.id.column_lbl);
            extraFieldLookupColumnAddResourceIDs.put("R.id.linked_field_lbl", R.id.linked_field_lbl);
            extraFieldLookupColumnAddResourceIDs.put("R.id.always_hide_lbl", R.id.always_hide_lbl);

			MetrixDesignerResourceData thisFieldLookupColumnAddData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_column_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupColAdd"), extraFieldLookupColumnAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupColumnAddActivityResourceData", thisFieldLookupColumnAddData);
		}

		// Field Lookup Column Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupColumnOrderActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupColumnOrderResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupColumnOrderResourceIDs.put("R.id.zzmd_field_lookup_column_order_emphasis", R.id.zzmd_field_lookup_column_order_emphasis);
			extraFieldLookupColumnOrderResourceIDs.put("R.id.mm_field_lkup_column__composite_name", R.id.mm_field_lkup_column__composite_name);
			extraFieldLookupColumnOrderResourceIDs.put("R.id.mm_field_lkup_column__lkup_column_id", R.id.mm_field_lkup_column__lkup_column_id);
			extraFieldLookupColumnOrderResourceIDs.put("R.id.mm_field_lkup_column__applied_order", R.id.mm_field_lkup_column__applied_order);
			extraFieldLookupColumnOrderResourceIDs.put("R.id.mm_field_lkup_column__metrix_row_id", R.id.mm_field_lkup_column__metrix_row_id);
			extraFieldLookupColumnOrderResourceIDs.put("R.id.save", R.id.save);
			extraFieldLookupColumnOrderResourceIDs.put("R.id.finish", R.id.finish);

            extraFieldLookupColumnOrderResourceIDs.put("R.id.lookup_column_order", R.id.lookup_column_order);

			MetrixDesignerResourceData thisFieldLookupColumnOrderData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_column_order, R.id.listview, R.layout.zzmd_field_lookup_column_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupColOrd"), extraFieldLookupColumnOrderResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupColumnOrderActivityResourceData", thisFieldLookupColumnOrderData);
		}

		// Field Lookup Column Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupColumnPropActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupColumnPropResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupColumnPropResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraFieldLookupColumnPropResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraFieldLookupColumnPropResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);

			extraFieldLookupColumnPropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraFieldLookupColumnPropResourceIDs.put("R.id.zzmd_field_lookup_column_prop_emphasis", R.id.zzmd_field_lookup_column_prop_emphasis);
			extraFieldLookupColumnPropResourceIDs.put("R.id.save", R.id.save);
			extraFieldLookupColumnPropResourceIDs.put("R.id.finish", R.id.finish);
            extraFieldLookupColumnPropResourceIDs.put("R.id.lookup_column_properties", R.id.lookup_column_properties);

			MetrixDesignerResourceData thisFieldLookupColumnPropData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_column_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupColProp"), extraFieldLookupColumnPropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupColumnPropActivityResourceData", thisFieldLookupColumnPropData);
		}

		// Field Lookup Filter activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupFilterActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupFilterResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupFilterResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraFieldLookupFilterResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraFieldLookupFilterResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraFieldLookupFilterResourceIDs.put("R.id.zzmd_field_lookup_filter_emphasis", R.id.zzmd_field_lookup_filter_emphasis);
			extraFieldLookupFilterResourceIDs.put("R.id.mm_field_lkup_filter__composite_name", R.id.mm_field_lkup_filter__composite_name);
			extraFieldLookupFilterResourceIDs.put("R.id.mm_field_lkup_filter__metrix_row_id", R.id.mm_field_lkup_filter__metrix_row_id);
			extraFieldLookupFilterResourceIDs.put("R.id.mm_field_lkup_filter__lkup_filter_id", R.id.mm_field_lkup_filter__lkup_filter_id);
			extraFieldLookupFilterResourceIDs.put("R.id.mm_field_lkup_filter__is_baseline", R.id.mm_field_lkup_filter__is_baseline);
			extraFieldLookupFilterResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraFieldLookupFilterResourceIDs.put("R.id.add_filter", R.id.add_filter);
			extraFieldLookupFilterResourceIDs.put("R.id.filter_order", R.id.filter_order);

            extraFieldLookupFilterResourceIDs.put("R.id.lookup_filters", R.id.lookup_filters);
            extraFieldLookupFilterResourceIDs.put("R.id.add_filter", R.id.add_filter);
            extraFieldLookupFilterResourceIDs.put("R.id.filter_order", R.id.filter_order);

			MetrixDesignerResourceData thisFieldLookupFilterData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_filter, R.id.listview, R.layout.zzmd_field_lookup_filter_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupFltr"), extraFieldLookupFilterResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupFilterActivityResourceData", thisFieldLookupFilterData);
		}

		// Field Lookup Filter Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupFilterAddActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupFilterAddResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupFilterAddResourceIDs.put("R.id.zzmd_field_lookup_filter_add_emphasis", R.id.zzmd_field_lookup_filter_add_emphasis);
			extraFieldLookupFilterAddResourceIDs.put("R.id.logical_operator", R.id.logical_operator);
			extraFieldLookupFilterAddResourceIDs.put("R.id.left_parens", R.id.left_parens);
			extraFieldLookupFilterAddResourceIDs.put("R.id.table", R.id.table);
			extraFieldLookupFilterAddResourceIDs.put("R.id.column", R.id.column);
			extraFieldLookupFilterAddResourceIDs.put("R.id.operator", R.id.operator);
			extraFieldLookupFilterAddResourceIDs.put("R.id.right_operand_label", R.id.right_operand_label);
			extraFieldLookupFilterAddResourceIDs.put("R.id.right_operand", R.id.right_operand);
			extraFieldLookupFilterAddResourceIDs.put("R.id.right_operand_button", R.id.right_operand_button);
			extraFieldLookupFilterAddResourceIDs.put("R.id.right_operand_description_label", R.id.right_operand_description_label);
			extraFieldLookupFilterAddResourceIDs.put("R.id.right_operand_description", R.id.right_operand_description);
			extraFieldLookupFilterAddResourceIDs.put("R.id.no_quotes_label", R.id.no_quotes_label);
			extraFieldLookupFilterAddResourceIDs.put("R.id.no_quotes", R.id.no_quotes);
			extraFieldLookupFilterAddResourceIDs.put("R.id.right_parens", R.id.right_parens);
			extraFieldLookupFilterAddResourceIDs.put("R.id.save", R.id.save);

            extraFieldLookupFilterAddResourceIDs.put("R.id.add_lookup_filter", R.id.add_lookup_filter);
            extraFieldLookupFilterAddResourceIDs.put("R.id.logical_operator_lbl", R.id.logical_operator_lbl);
            extraFieldLookupFilterAddResourceIDs.put("R.id.left_parens_lbl", R.id.left_parens_lbl);
            extraFieldLookupFilterAddResourceIDs.put("R.id.table_lbl", R.id.table_lbl);
            extraFieldLookupFilterAddResourceIDs.put("R.id.column_lbl", R.id.column_lbl);
            extraFieldLookupFilterAddResourceIDs.put("R.id.operator_lbl", R.id.operator_lbl);
            extraFieldLookupFilterAddResourceIDs.put("R.id.right_parens_lbl", R.id.right_parens_lbl);

			MetrixDesignerResourceData thisFieldLookupFilterAddData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_filter_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupFltrAdd"), extraFieldLookupFilterAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupFilterAddActivityResourceData", thisFieldLookupFilterAddData);
		}

		// Field Lookup Filter Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupFilterOrderActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupFilterOrderResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupFilterOrderResourceIDs.put("R.id.zzmd_field_lookup_filter_order_emphasis", R.id.zzmd_field_lookup_filter_order_emphasis);
			extraFieldLookupFilterOrderResourceIDs.put("R.id.mm_field_lkup_filter__composite_name", R.id.mm_field_lkup_filter__composite_name);
			extraFieldLookupFilterOrderResourceIDs.put("R.id.mm_field_lkup_filter__lkup_filter_id", R.id.mm_field_lkup_filter__lkup_filter_id);
			extraFieldLookupFilterOrderResourceIDs.put("R.id.mm_field_lkup_filter__applied_order", R.id.mm_field_lkup_filter__applied_order);
			extraFieldLookupFilterOrderResourceIDs.put("R.id.mm_field_lkup_filter__metrix_row_id", R.id.mm_field_lkup_filter__metrix_row_id);
			extraFieldLookupFilterOrderResourceIDs.put("R.id.save", R.id.save);
			extraFieldLookupFilterOrderResourceIDs.put("R.id.finish", R.id.finish);
            extraFieldLookupFilterOrderResourceIDs.put("R.id.lookup_filter_order", R.id.lookup_filter_order);

			MetrixDesignerResourceData thisFieldLookupFilterOrderData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_filter_order, R.id.listview, R.layout.zzmd_field_lookup_filter_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupFltrOrd"), extraFieldLookupFilterOrderResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupFilterOrderActivityResourceData", thisFieldLookupFilterOrderData);
		}

		// Field Lookup Filter Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupFilterPropActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupFilterPropResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupFilterPropResourceIDs.put("R.layout.zzmd_prop_edittext_imageview_line", R.layout.zzmd_prop_edittext_imageview_line);
			extraFieldLookupFilterPropResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraFieldLookupFilterPropResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);
			extraFieldLookupFilterPropResourceIDs.put("R.layout.zzmd_prop_checkbox_line", R.layout.zzmd_prop_checkbox_line);
			extraFieldLookupFilterPropResourceIDs.put("R.layout.zzmd_prop_edittext_line", R.layout.zzmd_prop_edittext_line);

			extraFieldLookupFilterPropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraFieldLookupFilterPropResourceIDs.put("R.id.zzmd_field_lookup_filter_prop_emphasis", R.id.zzmd_field_lookup_filter_prop_emphasis);
			extraFieldLookupFilterPropResourceIDs.put("R.id.save", R.id.save);
			extraFieldLookupFilterPropResourceIDs.put("R.id.finish", R.id.finish);
            extraFieldLookupFilterPropResourceIDs.put("R.id.lookup_filter_properties", R.id.lookup_filter_properties);

			MetrixDesignerResourceData thisFieldLookupFilterPropData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_filter_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupFltrProp"), extraFieldLookupFilterPropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupFilterPropActivityResourceData", thisFieldLookupFilterPropData);
		}

		// Field Lookup Order By activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupOrderbyActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupOrderbyResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupOrderbyResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraFieldLookupOrderbyResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraFieldLookupOrderbyResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraFieldLookupOrderbyResourceIDs.put("R.id.zzmd_field_lookup_orderby_emphasis", R.id.zzmd_field_lookup_orderby_emphasis);
			extraFieldLookupOrderbyResourceIDs.put("R.id.mm_field_lkup_orderby__composite_name", R.id.mm_field_lkup_orderby__composite_name);
			extraFieldLookupOrderbyResourceIDs.put("R.id.mm_field_lkup_orderby__metrix_row_id", R.id.mm_field_lkup_orderby__metrix_row_id);
			extraFieldLookupOrderbyResourceIDs.put("R.id.mm_field_lkup_orderby__lkup_orderby_id", R.id.mm_field_lkup_orderby__lkup_orderby_id);
			extraFieldLookupOrderbyResourceIDs.put("R.id.mm_field_lkup_orderby__is_baseline", R.id.mm_field_lkup_orderby__is_baseline);
			extraFieldLookupOrderbyResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraFieldLookupOrderbyResourceIDs.put("R.id.add_orderby", R.id.add_orderby);
			extraFieldLookupOrderbyResourceIDs.put("R.id.orderby_order", R.id.orderby_order);
            extraFieldLookupOrderbyResourceIDs.put("R.id.lookup_order_by", R.id.lookup_order_by);

			MetrixDesignerResourceData thisFieldLookupOrderbyData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_orderby, R.id.listview, R.layout.zzmd_field_lookup_orderby_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupOrdBy"), extraFieldLookupOrderbyResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupOrderbyActivityResourceData", thisFieldLookupOrderbyData);
		}

		// Field Lookup Order By Add activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupOrderbyAddActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupOrderbyAddResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupOrderbyAddResourceIDs.put("R.id.zzmd_field_lookup_orderby_add_emphasis", R.id.zzmd_field_lookup_orderby_add_emphasis);
			extraFieldLookupOrderbyAddResourceIDs.put("R.id.table", R.id.table);
			extraFieldLookupOrderbyAddResourceIDs.put("R.id.column", R.id.column);
			extraFieldLookupOrderbyAddResourceIDs.put("R.id.sort_order", R.id.sort_order);
			extraFieldLookupOrderbyAddResourceIDs.put("R.id.save", R.id.save);

            extraFieldLookupOrderbyAddResourceIDs.put("R.id.add_lookup_order_by", R.id.add_lookup_order_by);
            extraFieldLookupOrderbyAddResourceIDs.put("R.id.table_lbl", R.id.table_lbl);
            extraFieldLookupOrderbyAddResourceIDs.put("R.id.column_lbl", R.id.column_lbl);
            extraFieldLookupOrderbyAddResourceIDs.put("R.id.sort_order_lbl", R.id.sort_order_lbl);

			MetrixDesignerResourceData thisFieldLookupOrderbyAddData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_orderby_add, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupOrdByAdd"), extraFieldLookupOrderbyAddResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupOrderbyAddActivityResourceData", thisFieldLookupOrderbyAddData);
		}

		// Field Lookup Filter Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupOrderbyOrderActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupOrderbyOrderResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupOrderbyOrderResourceIDs.put("R.id.zzmd_field_lookup_orderby_order_emphasis", R.id.zzmd_field_lookup_orderby_order_emphasis);
			extraFieldLookupOrderbyOrderResourceIDs.put("R.id.mm_field_lkup_orderby__composite_name", R.id.mm_field_lkup_orderby__composite_name);
			extraFieldLookupOrderbyOrderResourceIDs.put("R.id.mm_field_lkup_orderby__lkup_orderby_id", R.id.mm_field_lkup_orderby__lkup_orderby_id);
			extraFieldLookupOrderbyOrderResourceIDs.put("R.id.mm_field_lkup_orderby__applied_order", R.id.mm_field_lkup_orderby__applied_order);
			extraFieldLookupOrderbyOrderResourceIDs.put("R.id.mm_field_lkup_orderby__metrix_row_id", R.id.mm_field_lkup_orderby__metrix_row_id);
			extraFieldLookupOrderbyOrderResourceIDs.put("R.id.save", R.id.save);
			extraFieldLookupOrderbyOrderResourceIDs.put("R.id.finish", R.id.finish);
            extraFieldLookupOrderbyOrderResourceIDs.put("R.id.lookup_order_by_order", R.id.lookup_order_by_order);

			MetrixDesignerResourceData thisFieldLookupOrderbyOrderData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_orderby_order, R.id.listview, R.layout.zzmd_field_lookup_orderby_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupOrdByOrd"), extraFieldLookupOrderbyOrderResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupOrderbyOrderActivityResourceData", thisFieldLookupOrderbyOrderData);
		}

		// Field Lookup Order By Prop activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerFieldLookupOrderbyPropActivityResourceData")) {
			HashMap<String, Integer> extraFieldLookupOrderbyPropResourceIDs = new HashMap<String, Integer>();
			extraFieldLookupOrderbyPropResourceIDs.put("R.layout.zzmd_prop_spinner_line", R.layout.zzmd_prop_spinner_line);
			extraFieldLookupOrderbyPropResourceIDs.put("R.layout.zzmd_prop_textview_line", R.layout.zzmd_prop_textview_line);

			extraFieldLookupOrderbyPropResourceIDs.put("R.id.table_layout", R.id.table_layout);
			extraFieldLookupOrderbyPropResourceIDs.put("R.id.zzmd_field_lookup_orderby_prop_emphasis", R.id.zzmd_field_lookup_orderby_prop_emphasis);
			extraFieldLookupOrderbyPropResourceIDs.put("R.id.save", R.id.save);
			extraFieldLookupOrderbyPropResourceIDs.put("R.id.finish", R.id.finish);
            extraFieldLookupOrderbyPropResourceIDs.put("R.id.lookup_order_by_properties", R.id.lookup_order_by_properties);

            MetrixDesignerResourceData thisFieldLookupOrderbyPropData = new MetrixDesignerResourceData(R.layout.zzmd_field_lookup_orderby_prop, 0, 0,
                    AndroidResourceHelper.getMessage("ScnDescMxDesFldLkupOrdByProp"), extraFieldLookupOrderbyPropResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerFieldLookupOrderbyPropActivityResourceData", thisFieldLookupOrderbyPropData);
		}

		// Workflow activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerWorkflowActivityResourceData")) {
			HashMap<String, Integer> extraWorkflowResourceIDs = new HashMap<String, Integer>();
			extraWorkflowResourceIDs.put("R.drawable.circle_blue16x16", R.drawable.circle_blue16x16);
			extraWorkflowResourceIDs.put("R.drawable.circle_gray16x16", R.drawable.circle_gray16x16);
			extraWorkflowResourceIDs.put("R.drawable.transparent", R.drawable.transparent);
			extraWorkflowResourceIDs.put("R.id.mm_workflow__name", R.id.mm_workflow__name);
			extraWorkflowResourceIDs.put("R.id.mm_workflow__workflow_id", R.id.mm_workflow__workflow_id);
			extraWorkflowResourceIDs.put("R.id.mm_workflow__description", R.id.mm_workflow__description);
			extraWorkflowResourceIDs.put("R.id.mm_workflow__metrix_row_id", R.id.mm_workflow__metrix_row_id);
			extraWorkflowResourceIDs.put("R.id.mm_workflow__is_baseline", R.id.mm_workflow__is_baseline);
			extraWorkflowResourceIDs.put("R.id.is_added_icon", R.id.is_added_icon);
			extraWorkflowResourceIDs.put("R.id.add_workflow", R.id.add_workflow);
            extraWorkflowResourceIDs.put("R.id.workflows", R.id.workflows);
            extraWorkflowResourceIDs.put("R.id.screen_info_metrix_designer_workflow", R.id.screen_info_metrix_designer_workflow);

			MetrixDesignerResourceData thisWorkflowData = new MetrixDesignerResourceData(R.layout.zzmd_workflow, R.id.listview, R.layout.zzmd_workflow_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesWf"), extraWorkflowResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerWorkflowActivityResourceData", thisWorkflowData);
		}

		// Workflow Screen Enabling activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerWorkflowScreenEnablingActivityResourceData")) {
			HashMap<String, Integer> extraWorkflowSEResourceIDs = new HashMap<String, Integer>();
			extraWorkflowSEResourceIDs.put("R.id.zzmd_workflow_screen_enabling_title", R.id.zzmd_workflow_screen_enabling_title);
			extraWorkflowSEResourceIDs.put("R.id.mm_screen__screen_name", R.id.mm_screen__screen_name);
			extraWorkflowSEResourceIDs.put("R.id.mm_workflow_screen__screen_id", R.id.mm_workflow_screen__screen_id);
			extraWorkflowSEResourceIDs.put("R.id.mm_workflow_screen__step_order", R.id.mm_workflow_screen__step_order);
			extraWorkflowSEResourceIDs.put("R.id.mm_workflow_screen__metrix_row_id", R.id.mm_workflow_screen__metrix_row_id);
			extraWorkflowSEResourceIDs.put("R.id.checkboxState", R.id.checkboxState);
			extraWorkflowSEResourceIDs.put("R.id.save", R.id.save);
			extraWorkflowSEResourceIDs.put("R.id.next", R.id.next);
            extraWorkflowSEResourceIDs.put("R.id.screen_info_metrix_designer_workflow_screen_enabling", R.id.screen_info_metrix_designer_workflow_screen_enabling);

			MetrixDesignerResourceData thisWorkflowSEData = new MetrixDesignerResourceData(R.layout.zzmd_workflow_screen_enabling, R.id.listview,
					R.layout.zzmd_workflow_screen_enabling_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesWfScnEnable"), extraWorkflowSEResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerWorkflowScreenEnablingActivityResourceData", thisWorkflowSEData);
		}

		// Workflow Screen Order activity resources
		if (!MetrixPublicCache.instance.containsKey("MetrixDesignerWorkflowScreenOrderActivityResourceData")) {
			HashMap<String, Integer> extraWorkflowSOResourceIDs = new HashMap<String, Integer>();
			extraWorkflowSOResourceIDs.put("R.id.zzmd_workflow_screen_order_title", R.id.zzmd_workflow_screen_order_title);
			extraWorkflowSOResourceIDs.put("R.id.mm_screen__screen_name", R.id.mm_screen__screen_name);
			extraWorkflowSOResourceIDs.put("R.id.mm_workflow_screen__screen_id", R.id.mm_workflow_screen__screen_id);
			extraWorkflowSOResourceIDs.put("R.id.mm_workflow_screen__step_order", R.id.mm_workflow_screen__step_order);
			extraWorkflowSOResourceIDs.put("R.id.mm_workflow_screen__metrix_row_id", R.id.mm_workflow_screen__metrix_row_id);
			extraWorkflowSOResourceIDs.put("R.id.save", R.id.save);
			extraWorkflowSOResourceIDs.put("R.id.finish", R.id.finish);
            extraWorkflowSOResourceIDs.put("R.id.screen_info_metrix_designer_workflow_screen_order", R.id.screen_info_metrix_designer_workflow_screen_order);


			MetrixDesignerResourceData thisWorkflowOrderData = new MetrixDesignerResourceData(R.layout.zzmd_workflow_screen_order, R.id.listview,
					R.layout.zzmd_workflow_screen_order_list_item,
                    AndroidResourceHelper.getMessage("ScnDescMxDesWfScnOrd"), extraWorkflowSOResourceIDs, null);
            MetrixPublicCache.instance.addItem("MetrixDesignerWorkflowScreenOrderActivityResourceData", thisWorkflowOrderData);
		}
	}

	//Tablet UI Optimization
	protected boolean isTablet() {
		return getResources().getBoolean(R.bool.isTablet);
	}

	protected boolean isTabletRunningInLandscapeMode()
	{
		boolean status = false;
		try{
			if(isTablet())
			{
				int currentOrientation = getResources().getConfiguration().orientation;
				if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
					status = true;
			}
		}
		catch(Exception e)
		{
			status = false;
		}
		return status;
	}

	protected boolean isTabletSpecificLandscapeUIRequired(){
		return false;
	}

	protected boolean isScreenWithPreviousItemList(){
		return true;
	}

	protected boolean displayInitAndBatchSyncError(){
		boolean displaySyncError = false;

		String displaySyncErrorParam = MetrixDatabaseManager.getAppParam("MOBILE_SYNC_ERROR_DISPLAY");

		if(!MetrixStringHelper.isNullOrEmpty(displaySyncErrorParam) && displaySyncErrorParam.equalsIgnoreCase("Y"))
			displaySyncError = true;

		return displaySyncError;
	}

	protected void renderTabletSpecificLayoutControls() {
		boolean isMDA = (this instanceof MetadataDebriefActivity);
		ViewGroup viewPreviousBar = (ViewGroup) findViewById(R.id.view_previous_entries_bar);
		if (viewPreviousBar != null && (!isMDA || (isMDA && MetrixWorkflowManager.isScreenExistsInCurrentWorkflow(this, codeLessScreenId))))
			viewPreviousBar.setVisibility(View.GONE);
	}

	@SuppressLint("DefaultLocale")
	private void setTabletSpecificOptimization() {
		if(shouldRunTabletSpecificUIMode){
			renderTabletSpecificLayoutControls();

			//Codeless screens
			//In tablet landscape mode - we show a combine of two screens(Ex: LIST & STANDARD)
			if(isCodelessScreen){
				HashMap<String, String> currentScreenProperties = MetrixScreenManager.getScreenProperties(codeLessScreenId);
				//Check whether the current screen has a linked screen
				if(currentScreenProperties.containsKey("linked_screen_id")){
					String strLinkedScreenId = currentScreenProperties.get("linked_screen_id");
					if(!MetrixStringHelper.isNullOrEmpty(strLinkedScreenId)){
						//get the linked screen information
						linkedScreenIdInTabletUIMode = Integer.valueOf(strLinkedScreenId);
						if(linkedScreenIdInTabletUIMode > -1){
							linkedScreenAvailableInTabletUIMode = true;
							linkedScreenTypeInTabletUIMode = MetrixScreenManager.getScreenType(linkedScreenIdInTabletUIMode);
						}
					}
				}

				//If the current screen/if the current screen is in workflow & if it's a LIST type screen - if there's linked screen
				//we have to address some special requirements, so we mark it as a special case
				if(linkedScreenAvailableInTabletUIMode){
					String screenType = MetrixScreenManager.getScreenType(codeLessScreenId);
					if(!MetrixStringHelper.isNullOrEmpty(screenType)){
						if (screenType.toLowerCase().contains("list")) {
							isCodelessListScreenInTabletUIMode = true;
						}
					}
				}

				if(!linkedScreenAvailableInTabletUIMode)
				{
					//if there's no linked screen
					String currentScreenType = MetrixScreenManager.getScreenType(codeLessScreenId);
					if(!MetrixStringHelper.isNullOrEmpty(currentScreenType))
					{
						//if the current screen type is LIST, we hide the panel 2 which renders the STANDARD screen.
						if(currentScreenType.toLowerCase().contains("list")){
							ViewGroup linkedScreenLayout = (ViewGroup) findViewById(R.id.panel_two);
							if(linkedScreenLayout != null)
								linkedScreenLayout.setVisibility(View.GONE);
						}
						//if the current screen type is STANDARD, we hide the panel 3 which renders the LIST screen.
						else if(currentScreenType.toLowerCase().contains("standard")){
							ViewGroup linkedScreenLayout = (ViewGroup) findViewById(R.id.panel_three);
							if(linkedScreenLayout != null)
								linkedScreenLayout.setVisibility(View.GONE);

							LinearLayout panelTwo = (LinearLayout) findViewById(R.id.panel_two);
							if(panelTwo != null){
								LinearLayout.LayoutParams panelTwoParams = (LinearLayout.LayoutParams) panelTwo.getLayoutParams();
								panelTwoParams.weight = 0.8f;
								panelTwo.setLayoutParams(panelTwoParams);
							}
						}
					}
				}
			}
			//Coded screens
			//At the moment, the STANDARD screen will be always in the workflow and the associate LIST screen will not be in the workflow
			else{
				if(!MetrixStringHelper.isNullOrEmpty(listActivityFullNameInTabletUIMode)){
					int index = listActivityFullNameInTabletUIMode.lastIndexOf(".");
					String activityName = listActivityFullNameInTabletUIMode.substring((index + 1), listActivityFullNameInTabletUIMode.length());
					//Keep the information of associate LIST screen
					if(!MetrixStringHelper.isNullOrEmpty(activityName))
					{
						linkedScreenIdInTabletUIMode = MetrixScreenManager.getScreenId(activityName);
						if(linkedScreenIdInTabletUIMode > -1){
							linkedScreenAvailableInTabletUIMode = true;
							linkedScreenTypeInTabletUIMode = MetrixScreenManager.getScreenType(linkedScreenIdInTabletUIMode);
						}
					}
				}
				else{
					//This is special case...
					//Ex: TaskSteps screen
					String activityName = standardActivityFullNameInTabletUIMode;
					//Keep the information of associate STANDARD screen
					if(!MetrixStringHelper.isNullOrEmpty(activityName))
					{
						linkedScreenIdInTabletUIMode = MetrixScreenManager.getScreenId(activityName);
						if(linkedScreenIdInTabletUIMode > -1){
							linkedScreenAvailableInTabletUIMode = true;
							linkedScreenTypeInTabletUIMode = MetrixScreenManager.getScreenType(linkedScreenIdInTabletUIMode);
						}
					}

					//If the current screen/if the current screen is in workflow & if it's a LIST type screen - if there's linked screen
					//we have to address some special requirements, so we mark it as a special case
					if(linkedScreenAvailableInTabletUIMode){
						String screenType = MetrixScreenManager.getScreenType(this);
						if(!MetrixStringHelper.isNullOrEmpty(screenType)){
							if (screenType.toLowerCase().contains("list")) {
								isCodelessListScreenInTabletUIMode = true;
							}
						}
					}
				}
			}
		}
	}

	public void reloadFreshActivity() {
		Intent intent = MetrixActivityHelper.createActivityIntent(this, mCurrentActivity.getClass());
		if (isCodelessScreen)
			intent.putExtra("ScreenID", codeLessScreenId);
		if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"))
			intent.putExtra("NavigatedFromLinkedScreen", true);
		MetrixActivityHelper.startNewActivityAndFinish(this, intent);
	}
	//End Tablet UI Optimization

	/***
	 * This is to consume MetrixUIHelper in non-activity classes.
	 * @return MetrixUIHelper
	 */
	public MetrixUIHelper getMetrixUIHelper(){
		if(mUIHelper == null)
		{
			mUIHelper = new MetrixUIHelper(this);
			return mUIHelper;
		}
		return mUIHelper;
	}

    protected void setReceivingActionBarTitle() {
        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        if (actionBarTitle != null) {
            if (mHandlingErrors) {
                actionBarTitle.setText(AndroidResourceHelper.getMessage("ErrorActionBarTitle1Arg", MobileGlobal.mErrorInfo.transactionDescription));
            } else {
                String rcvId = MetrixCurrentKeysHelper.getKeyValue("receiving", "receiving_id");
                if (!MetrixStringHelper.isNullOrEmpty(rcvId)) {
                    actionBarTitle.setText(AndroidResourceHelper.getMessage("Receipt1Arg", rcvId));
                }
            }
        }
    }

	private boolean shouldSetTextToUpper(View view) {
		if (mFormDef != null) {
			for (MetrixTableDef tableDef : mFormDef.tables) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (view.getId() == columnDef.id) {
						if (columnDef.forceCase != MetrixControlCase.NONE) {
							if (columnDef.forceCase == MetrixControlCase.UPPER)
								return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void registerLogoutAcvitityReceiver () {
		if(mLogoutReceiverInitialized)
			return;
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.package.ACTION_LOGOUT");
		mLogoutReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("onReceive","Logout in progress");
				finish();
			}
		};
		if (Build.VERSION.SDK_INT >= 33) {
			registerReceiver(mLogoutReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			//noinspection UnspecifiedRegisterReceiverFlag
			registerReceiver(mLogoutReceiver, intentFilter);
		}
		mLogoutReceiverInitialized = true;
	}

	private void unregisterLogoutReceiever(){
		if(!mLogoutReceiverInitialized)
			return;
		unregisterReceiver(mLogoutReceiver);
		mLogoutReceiverInitialized = false;
	}

	@Override
	public void onSlidingMenuItemClick(MetrixSlidingMenuItem clickedItem) {
		final String title = clickedItem.getTitle();
		if(!MetrixStringHelper.isNullOrEmpty(title)){
			if (MetrixGlobalMenuManager.stringExistsInDesignerGlobalMenuResources(title)) {
				// make sure Designer has all necessary resources
				// and that we cache what activity launched it,
				// so that we know where to come back to when we close it
				populateMobileDesignerResources();
				Class<? extends Activity> currentActivityClass = mCurrentActivity.getClass();
				String fullName = currentActivityClass.getName();
				String simpleName = currentActivityClass.getSimpleName();
				String namespace = fullName.substring(0, fullName.lastIndexOf(simpleName) - 1);

				// Storing current activity extras in to MetrixPublicChache before going to designer - Fix for KEST-2516
				if(mCurrentActivity.getIntent().getExtras() != null) {
					MetrixPublicCache.instance.addItem("CurrentBundle", mCurrentActivity.getIntent().getExtras());
				}

				if (MetrixStringHelper.valueIsEqual(simpleName, "MetrixSurveyActivity")) {
					namespace = "com.metrix.metrixmobile";
					simpleName = "DebriefSurvey";
				}

				MetrixPublicCache.instance.addItem("MobileDesignerSourceActivityNamespace", namespace);
				MetrixPublicCache.instance.addItem("MobileDesignerSourceActivitySimpleName", simpleName);

				// Handle the codeless screen scenario
				if (MetrixPublicCache.instance.containsKey("MobileDesignerSourceCodelessActivityScreenId"))
					MetrixPublicCache.instance.removeItem("MobileDesignerSourceCodelessActivityScreenId");

				if (this.isCodelessScreen)
					MetrixPublicCache.instance.addItem("MobileDesignerSourceCodelessActivityScreenId", this.codeLessScreenId);

				// Remember linked screen behavior for later
				if (MetrixPublicCache.instance.containsKey("MobileDesignerSourceFromLinkedScreen"))
					MetrixPublicCache.instance.removeItem("MobileDesignerSourceFromLinkedScreen");

				if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"))
					MetrixPublicCache.instance.addItem("MobileDesignerSourceFromLinkedScreen", true);
			}

			if (MetrixApplicationAssistant.getMetaBooleanValue(mCurrentActivity, "DemoBuild"))
				DemoOptionsMenu.onOptionsItemSelected(mCurrentActivity, clickedItem, helpText, mHandlingErrors);
			else
				OptionsMenu.onOptionsItemSelected(mCurrentActivity, clickedItem, helpText, mHandlingErrors);
		}

		if(mDrawerLayout != null)
			mDrawerLayout.closeDrawer(mDrawerLinearLayout);
	}

	protected void hideFABs(List<FloatingActionButton> fabList) {
		if (mFABsToShow == null)
			mFABsToShow = new ArrayList<>();
		for (FloatingActionButton fab : fabList){
			if (fab.isOrWillBeShown()) {
				mFABsToShow.add(fab);
				fab.hide();
			}
		}
	}

	protected void showFABs() {
		for (FloatingActionButton fab : mFABsToShow){
			if (fab.isOrWillBeHidden()) {
				final Object tag = fab.getTag();
				if (!(tag instanceof String && MetrixStringHelper.valueIsEqual((String)tag, MetrixClientScriptManager.HIDDEN_BY_SCRIPT)))
					fab.show();
			}
		}
	}

	protected int generateOffsetForFABs(List<FloatingActionButton> fabList) {
		int offset = (int)getResources().getDimension((R.dimen.md_margin)) + mSplitActionBarHeight;
		int visibleFABs = 0;
		for (FloatingActionButton fab : fabList){
			if (fab.isOrWillBeShown()) {
				visibleFABs++;
			}
		}
		if (visibleFABs > 0)
			offset += (int)getResources().getDimension((R.dimen.fab_offset_single));

		if (visibleFABs > 1) {
			int extraFABs = visibleFABs - 1;
			offset += extraFABs * (int)getResources().getDimension((R.dimen.fab_offset_difference));
		}

		return offset;
	}

	public void resetFABOffset() {
		// For now, we do nothing by default in the general case (TODO for OSP-2144)
	}
	
	protected void loadMapIfNeeded(int screenId, ViewGroup layout, boolean shouldAddToCollection) {
		if (shouldAddToCollection) {
			final View mapContainer = layout.findViewById(R.id.map_widget_container);
			if (mapContainer != null) {
                final MapWidgetHolder holder = new MapWidgetHolder(mapContainer);
                holder.btnGetDirections.setOnClickListener(this);
                AndroidResourceHelper.setResourceValues(holder.btnGetDirections, "GetDirections");
                holder.mapView.onCreate(null);
                mapWidgets.put(screenId, holder);
            }
		}
	}

	protected void activateMapsIfAny() {
		if (mapWidgets.size() > 0) {
			MapsInitializer.initialize(getApplicationContext());
			for (int i = 0; i < mapWidgets.size(); i++) {
				final int screenId = mapWidgets.keyAt(i);
				final MapWidgetHolder holder = mapWidgets.get(screenId);
				holder.mapView.getMapAsync(new MapWidgetReadyCallback(screenId, holder, this));
			}
		}
	}

	public void handleNotificationIfExists() {
		try {
			if (MobileApplication.NotificationContent != null) {
				FSMNotificationContent nc = MobileApplication.NotificationContent;
				MobileApplication.NotificationContent = null;
				if (!MetrixStringHelper.isNullOrEmpty(nc.getClientScript())) {
					ClientScriptDef scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(nc.getClientScript());
					if (scriptDef != null) {
						FSMNotificationAssistant.populateDataPointCache(nc);
						MetrixClientScriptManager.executeScript(new WeakReference<Activity>(this), scriptDef);
						FSMNotificationAssistant.clearDataPointCache();
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void handleAppStartupPushProcessing() {
		try {
			if (MobileApplication.NeedsAppStartupPushProcessing) {
				PushRegistrationManager pushMgr = new PushRegistrationManager();
				pushMgr.initNotifications();
				FSMNotificationAssistant.recordUserNotificationPermission();
				MobileApplication.NeedsAppStartupPushProcessing = false;
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}
}
