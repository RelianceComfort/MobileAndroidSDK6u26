package com.metrix.architecture.metadata;

import java.util.ArrayList;
import java.util.List;

/*import android.R;
*/import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.metrix.architecture.superclasses.MetrixImplicitSaveSwipeActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * The MetrixSwipeKeysCollection is used by the client framework to support
 * swipe enabled screens.
 * 
 * @since 5.6
 */
public class MetrixSwipeKeysCollection implements Parcelable {
	private String mTableName;
	private List<MetrixSwipeKeys> mKeys = new ArrayList<MetrixSwipeKeys>();

	public static String SWIPE_KEYS = "METRIX_SWIPE_KEYS_COLLECTION";

	/**
	 * The MetrixSwipeKeysCollection default constructor.
	 * 
	 * @param tableName
	 *            The name of the database table the key columns and values are
	 *            for.
	 */
	public MetrixSwipeKeysCollection(String tableName) {
		if (MetrixStringHelper.isNullOrEmpty(tableName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTablenameParameterIsRequired"));
		}

		this.mTableName = tableName;
	}

	private MetrixSwipeKeysCollection(Parcel parcel) {
		this.mTableName = (String) parcel.readSerializable();
		parcel.readTypedList(mKeys, MetrixSwipeKeys.CREATOR);
	}

	/**
	 * The applySwipe method is used by the client framework to apply swipe
	 * functionality to a newly started activity if it's appropriate.
	 * 
	 * @param activity
	 *            The activity being started.
	 * @param intent
	 *            The intent used to launch the activity.
	 * @param layoutTitle
	 *            The TextView named R.id.layout_title on the activity's layout.
	 * @param gestureDetector
	 *            The mGestureDetector from activity superclass.
	 */
	@SuppressWarnings("deprecation")
	public static GestureDetector applySwipe(final Activity activity, final Intent intent, TextView layoutTitle, final int slideInRightAnim, final int slideInLeftAnim, final int slideOutRightAnim, final int slideOutLeftAnim) {
		if (layoutTitle != null && activity.getIntent().getExtras() != null
				&& activity.getIntent().getExtras().containsKey(MetrixSwipeKeysCollection.SWIPE_KEYS)) {
			Object item = activity.getIntent().getExtras().get(MetrixSwipeKeysCollection.SWIPE_KEYS);
			if (item != null && item instanceof MetrixSwipeKeysCollection) {
				final MetrixSwipeKeysCollection metrixSwipeKeysCollection = (MetrixSwipeKeysCollection) item;
				final int minScaledFlingVelocity = ViewConfiguration.get(activity).getScaledMinimumFlingVelocity() * 20;
				metrixSwipeKeysCollection.enhanceLayoutTitle(layoutTitle);

				return new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
						if (Math.abs(velocityX) > minScaledFlingVelocity && Math.abs(velocityY) < minScaledFlingVelocity) {
							if (metrixSwipeKeysCollection != null && metrixSwipeKeysCollection.size() > 0) {
								if (MetrixImplicitSaveSwipeActivity.class.isInstance(activity)) {
									MetrixImplicitSaveSwipeActivity swipeActivity = (MetrixImplicitSaveSwipeActivity) activity;
									if (!swipeActivity.implicitSwipeSave()) {
										return false;
									}
								}
								
								String tableName = metrixSwipeKeysCollection.getTableName();
								ArrayList<String> columnNames = metrixSwipeKeysCollection.getColumnNames();

								MetrixSwipeKeys currentKeys = new MetrixSwipeKeys();
								for (String columnName : columnNames) {
									currentKeys.addKey(columnName, MetrixCurrentKeysHelper.getKeyValue(tableName, columnName));
								}

								final boolean right = velocityX < 0;

								MetrixSwipeKeys nextKeys = new MetrixSwipeKeys();
								if (right) {
									nextKeys = metrixSwipeKeysCollection.getNext(currentKeys);
								} else {
									nextKeys = metrixSwipeKeysCollection.getPrevious(currentKeys);
								}

								for (String columnName : nextKeys.getColumnNames()) {
									String columnValue = nextKeys.getColumnValue(columnName);
									MetrixCurrentKeysHelper.setKeyValue(tableName, columnName, columnValue);
								}
								MetrixActivityHelper.startNewActivity(activity, intent);
								if (right) {
									activity.overridePendingTransition(slideInRightAnim, slideOutLeftAnim);
								} else {
									activity.overridePendingTransition(slideInLeftAnim, slideOutRightAnim);
								}
								activity.finish();
							}
						}
						return super.onFling(e1, e2, velocityX, velocityY);
					}
				});
			}
		}
		return null;
	}

	/**
	 * The addKeys method allows you to add an instance of MetrixSwipeKeys to
	 * the collection.
	 * 
	 * @param keys
	 *            The instance to add.
	 */
	public void addKeys(MetrixSwipeKeys keys) {
		if (keys == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheKeysParamIsReq"));
		}

		String idToFind = keys.getUniqueId();
		if (MetrixStringHelper.isNullOrEmpty(idToFind)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheKeysParamReqOne"));
		}

		this.mKeys.add(keys);
	}

	/**
	 * The getTableName method returns the name of the table the keys are for.
	 * 
	 * @return The table name.
	 */
	public String getTableName() {
		return this.mTableName;
	}

	/**
	 * The getColumnNames method returns the names of the primary key columns
	 * for the table referenced in this collection.
	 * 
	 * @return An ArrayList<String> of the primary key column names.
	 */
	public ArrayList<String> getColumnNames() {
		if (this.mKeys != null) {
			return this.mKeys.get(0).getColumnNames();
		} else {
			return new ArrayList<String>();
		}
	}

	/**
	 * The getPrevious method takes an instance of MetrixSwipeKeys, finds it in
	 * the collection and returns the previous instance.
	 * 
	 * @param keys
	 *            The MetrixSwipeKeys instance to search for.
	 * @return The MetrixSwipeKeys instance immediately previous to the received
	 *         instance in the collection.
	 */
	public MetrixSwipeKeys getPrevious(MetrixSwipeKeys keys) {
		if (keys == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheKeysParamIsReq"));
		}

		int position = this.getCurrentPosition(keys);
		if (position == -1) {
			return this.mKeys.get(0);
		} else if (position == 0) {
			return this.mKeys.get(this.mKeys.size() - 1);
		} else {
			return this.mKeys.get(position - 1);
		}
	}

	/**
	 * The getNext method takes an instance of MetrixSwipeKeys, finds it in the
	 * collection and returns the next instance.
	 * 
	 * @param keys
	 *            The MetrixSwipeKeys instance to search for.
	 * @return The MetrixSwipeKeys instance immediately after the received
	 *         instance in the collection.
	 */
	public MetrixSwipeKeys getNext(MetrixSwipeKeys keys) {
		if (keys == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheKeysParamIsReq"));
		}

		int position = this.getCurrentPosition(keys);
		if (position == -1) {
			return this.mKeys.get(0);
		} else if (position == this.mKeys.size() - 1) {
			return this.mKeys.get(0);
		} else {
			return this.mKeys.get(position + 1);
		}
	}

	/**
	 * The size method returns the number of MetrixSwipeKeys instances in the
	 * collection.
	 * 
	 * @return The number of MetrixSwipeKeys.
	 */
	public int size() {
		return this.mKeys.size();
	}

	/**
	 * The getCurrentPosition finds the position within the collection that the
	 * received MetrixSwipeKeys instance exists at.
	 * 
	 * @param keys
	 *            The instance to search for.
	 * @return The position of the instance in the collection. If the instance
	 *         isn't found, this will return -1.
	 */
	public int getCurrentPosition(MetrixSwipeKeys keys) {
		if (keys == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheKeysParamIsReq"));
		}

		String idToFind = keys.getUniqueId();
		if (MetrixStringHelper.isNullOrEmpty(idToFind)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheKeysParamReqOne"));
		} else {
			for (int i = 0; i < this.mKeys.size(); i++) {
				if (idToFind.compareTo(this.mKeys.get(i).getUniqueId()) == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * The enhanceLayoutTitle method takes and a TextView (which represents a
	 * screen's title). It determines the index of the current keys in the keys
	 * collection and modified the TextView adding (# of #).
	 * 
	 * @param layoutTitle
	 *            The TextView which serves as the screen's title.
	 */
	public void enhanceLayoutTitle(TextView layoutTitle) {
		if (this.size() > 0) {
			String tableName = this.getTableName();
			ArrayList<String> columnNames = this.getColumnNames();

			MetrixSwipeKeys currentKeys = new MetrixSwipeKeys();
			for (String columnName : columnNames) {
				currentKeys.addKey(columnName, MetrixCurrentKeysHelper.getKeyValue(tableName, columnName));
			}
			int currentKeyPosition = this.getCurrentPosition(currentKeys);
			StringBuilder sb = new StringBuilder(currentKeyPosition + 1);
			sb.append(" ").append(AndroidResourceHelper.getMessage("Of")).append(" ").append(this.size());
			layoutTitle.setText(sb.toString());
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		arg0.writeSerializable(this.mTableName);
		arg0.writeTypedList(this.mKeys);
	}

	public static final Parcelable.Creator<MetrixSwipeKeysCollection> CREATOR = new Parcelable.Creator<MetrixSwipeKeysCollection>() {

		@Override
		public MetrixSwipeKeysCollection createFromParcel(Parcel source) {
			return new MetrixSwipeKeysCollection(source);
		}

		@Override
		public MetrixSwipeKeysCollection[] newArray(int size) {
			return new MetrixSwipeKeysCollection[size];
		}
	};
}
