package com.metrix.metrixmobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SkypeCommunicator;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class TeamMemberAction extends MetrixActivity implements MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener {
	private RecyclerView recyclerView;
	private String mPersonId = "";
	private TMARecyclerViewAdapter mEfficientAdapter;
	private TextView mTitle;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.team_member_action);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	public void onStart() {
		resourceStrings.add(new ResourceValueObject(R.id.ScnInfoTeamMbrContact, "ScnInfoTeamMbrContact"));

		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		populateList();
		this.helpText = AndroidResourceHelper.getMessage("ScnInfoTeamMbrList");
	}

	/**
	 * Set the default values for views for this activity.
	 */
	protected void defaultValues() {
		mPersonId = this.getIntent().getStringExtra("PERSON_ID");
		ArrayList<Hashtable<String, String>> rows = MetrixDatabaseManager.getFieldStringValuesList("select first_name, last_name from person where person_id='"
				+ mPersonId + "'");
		String firstName = MetrixStringHelper.getValueFromSqlResults(rows, "first_name", 0);
		String lastName = MetrixStringHelper.getValueFromSqlResults(rows, "last_name", 0);

		mTitle = (TextView) findViewById(R.id.titleContact);
		mTitle.setText(firstName + " " + lastName);
	}

	/**
	 * Define the listeners for this activity.
	 */
	protected void setListeners() {
		registerForContextMenu(recyclerView);
	}

	/**
	 * This method is responsible for setting up the meta data which the
	 * architecture uses for data binding and validation.
	 */
	protected void defineForm() {
	}

	/**
	 * Populate the job list with the tasks assigned to the user.
	 */
	private void populateList() {
		StringBuilder query = new StringBuilder();
		query.append("select person.first_name, person.last_name, person.job_title, person.email_address, person.phone, person.mobile_phone, person.person_id, person.voip_user_name");
		query.append(" from person");
		query.append(" where person_id = '");
		query.append(mPersonId);
		query.append("'");

		String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
		if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
			query.append(" limit " + maxRows);
		}

		boolean telephonyAvailable = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

		MetrixCursor cursor = null;
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					HashMap<String, String> row1 = new HashMap<String, String>();

					if (!MetrixStringHelper.isNullOrEmpty(cursor.getString(3))) {
						row1.put("action", "Email");
						row1.put("action_info", cursor.getString(3));

						table.add(row1);
					}

					HashMap<String, String> row2 = new HashMap<String, String>();
					if ((telephonyAvailable) && (!MetrixStringHelper.isNullOrEmpty(cursor.getString(4)))) {
						row2.put("action", "Phone");
						row2.put("action_info", cursor.getString(4));

						table.add(row2);
					}

					HashMap<String, String> row3 = new HashMap<String, String>();
					if ((telephonyAvailable) && (!MetrixStringHelper.isNullOrEmpty(cursor.getString(5)))) {
						row3.put("action", "Mobile Phone");
						row3.put("action_info", cursor.getString(5));

						table.add(row3);
					}

					HashMap<String, String> row4 = new HashMap<String, String>();
					if (!MetrixStringHelper.isNullOrEmpty(cursor.getString(7))) {
						row4.put("action", "Skype");
						row4.put("action_info", cursor.getString(7));

						table.add(row4);
					}

					cursor.moveToNext();
				}
			} else {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NoDataForSelectedFilter"));
				return;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		mEfficientAdapter = new TMARecyclerViewAdapter(this, table, this);
		recyclerView.setAdapter(mEfficientAdapter);
	}

	@Override
	public void onItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
		String action = listItemData.get("action");
		String actionInfo = listItemData.get("action_info");

		try {
			if (action.toLowerCase().contains("phone")) {
				if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
					String uri = "tel:" + actionInfo.trim();
					Intent intent = new Intent(Intent.ACTION_DIAL);
					intent.setData(Uri.parse(uri));
					// startActivity(intent);

					MetrixActivityHelper.startNewActivity(this, intent);
				}
				else
					MetrixUIHelper.showSnackbar(TeamMemberAction.this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NoTelephonyServiceAvailable"));
			} else if (action.toLowerCase().contains("skype")) {
				String uri = "ms-sfb://chat?id=" + actionInfo.trim();
				SkypeCommunicator.initiateSkypeUri(this, uri);
			} else if (action.toLowerCase().contains("sms")) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.putExtra("address", actionInfo.trim());
				intent.setType("vnd.android-dir/mms-sms");
				MetrixActivityHelper.startNewActivity(this, intent);
			} else {
				Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { actionInfo });
				emailIntent.setType("plain/text");
				MetrixActivityHelper.startNewActivity(this, Intent.createChooser(emailIntent, AndroidResourceHelper.getMessage("SendEmail")));
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ContactAttemptFailed"));
		}
	}

	public static class TMARecyclerViewAdapter extends RecyclerView.Adapter<TMARecyclerViewAdapter.TMAViewHolder> {
		private final List<HashMap<String, String>> data;
		private final MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener listener;
		private final String actionIconDescription;
		private final Activity activity;

		public TMARecyclerViewAdapter(Activity activity, List<HashMap<String, String>> data, MetadataRecyclerViewAdapter.MetrixRecyclerViewClickListener listener) {
			this.data = data;
			this.listener = listener;
			this.actionIconDescription = AndroidResourceHelper.getMessage("action_icon");
			this.activity = activity;
		}

		@NonNull
		@Override
		public TMAViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			final View view = LayoutInflater.from(activity).inflate(R.layout.team_list_action_item, parent, false);
			return new TMAViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull TMAViewHolder holder, int position) {
			String action_type = data.get(position).get("action");
			final int pos = position;
			if (!MetrixStringHelper.isNullOrEmpty(action_type)) {
				if (action_type.toLowerCase().contains("mobile phone")) {
					Glide.with(activity)
							.load(R.drawable.phonebook_call)
							.into(holder.mActionIcon);
					Glide.with(activity)
							.load(R.drawable.sms)
							.into(holder.mActionIcon2);

					holder.mActionIcon2.setOnClickListener((v) -> {
						try {
							String actionInfo = data.get(pos).get("action_info");
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
								Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
								smsIntent.addCategory(Intent.CATEGORY_DEFAULT);
								smsIntent.setType("vnd.android-dir/mms-sms");
								smsIntent.setData(Uri.parse("sms:" + actionInfo.trim()));
								activity.startActivity(smsIntent);
							} else {
								//For early versions, the old code remain
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.putExtra("address", actionInfo.trim());
								intent.setType("vnd.android-dir/mms-sms");
								activity.startActivity(intent);
							}
						} catch (Exception ex) {
							LogManager.getInstance().error(ex);
							MetrixUIHelper.showSnackbar(activity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("ContactAttemptFailed"));
						}
					});
				} else if (action_type.toLowerCase().contains("skype")) {
					Glide.with(activity)
							.load(R.drawable.skype)
							.into(holder.mActionIcon);
					holder.mActionIcon2.setVisibility(View.GONE);
				} else if (action_type.toLowerCase().contains("phone")) {
					Glide.with(activity)
							.load(R.drawable.phonebook_call)
							.into(holder.mActionIcon);
					holder.mActionIcon2.setVisibility(View.GONE);
				} else {
					Glide.with(activity)
							.load(R.drawable.phonebook_message)
							.into(holder.mActionIcon);
					holder.mActionIcon2.setVisibility(View.GONE);
				}
			}

			holder.mTitle.setText(data.get(position).get("action"));
			holder.mInfo.setText(data.get(position).get("action_info"));
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		class TMAViewHolder extends RecyclerView.ViewHolder {
			private final TextView mTitle;
			private final TextView mInfo;
			private final ImageView mActionIcon;
			private final ImageView mActionIcon2;

			public TMAViewHolder(View itemView) {
				super(itemView);
				mActionIcon = itemView.findViewById(R.id.person_action_icon);
				mActionIcon2 = itemView.findViewById(R.id.person_action_icon2);
				mTitle = itemView.findViewById(R.id.action_description);
				mInfo = itemView.findViewById(R.id.action_info);
				mActionIcon.setContentDescription(actionIconDescription);
				mActionIcon2.setContentDescription(actionIconDescription);

				itemView.setOnClickListener((v) -> {
					if (listener != null) {
						final int position = getAdapterPosition();
						if (position != RecyclerView.NO_POSITION)
							listener.onItemClick(position, data.get(position), v);
					}
				});
			}
		}
	}
}

