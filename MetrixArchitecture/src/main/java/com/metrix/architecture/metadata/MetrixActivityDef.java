package com.metrix.architecture.metadata;

import java.util.HashMap;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.constants.MetrixTransactionTypesConverter;
import com.metrix.architecture.utilities.AndroidResourceHelper;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains the meta data used to describe an activity. This is currently only used 
 * by the Architecture.
 *
 */
public class MetrixActivityDef implements Parcelable {
	public MetrixTransactionTypes TransactionType;
	public HashMap<String, String> Keys;

    public static final String METRIX_ACTIVITY_DEF = "MetrixActivityDef";

	@SuppressWarnings("unchecked")
	private MetrixActivityDef(Parcel parcel) {
		this.TransactionType = (MetrixTransactionTypes) parcel.readSerializable();
		this.Keys = (HashMap<String, String>) parcel.readSerializable();
	}
	
	public MetrixActivityDef() {
		this.Keys = new HashMap<String, String>();
	}

	public MetrixActivityDef(MetrixTransactionTypes transactionType) {
		this.TransactionType = transactionType;
		this.Keys = new HashMap<String, String>();
	}

	public MetrixActivityDef(MetrixTransactionTypes transactionType, String keyName, String keyValue) {
		this.TransactionType = transactionType;
		this.Keys = new HashMap<String, String>();
		this.Keys.put(keyName, keyValue);
	}
	
	public MetrixActivityDef(MetrixTransactionTypes transactionType, HashMap<String, String> keys) {
		this.TransactionType = transactionType;
		this.Keys = keys;
	}
	
	@Override
	public String toString() {
		return AndroidResourceHelper.getMessage("TransactionType1Args", MetrixTransactionTypesConverter.toString(this.TransactionType));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int arg1) {
		parcel.writeSerializable(this.TransactionType);
		parcel.writeSerializable(this.Keys);
	}
	
	public static final Parcelable.Creator<MetrixActivityDef> CREATOR = new Parcelable.Creator<MetrixActivityDef>() {

		@Override
		public MetrixActivityDef createFromParcel(Parcel source) {
			return new MetrixActivityDef(source);
		}

		@Override
		public MetrixActivityDef[] newArray(int size) {
			return new MetrixActivityDef[size];
		}
	};
}
