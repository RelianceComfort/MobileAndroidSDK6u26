package com.metrix.architecture.utilities;

import android.os.Parcel;
import android.os.Parcelable;

/***
 * Generic implementation of Parcelable interface
 * @author rawilk
 *
 * @param <T>
 */
public class MetrixParcelable<T> implements Parcelable {

	private T mValue;
	private static ClassLoader mClassLoader;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcelOut, int flags) {
        parcelOut.writeValue(mValue);
    }

    @SuppressWarnings("rawtypes")
	public static final Parcelable.Creator<MetrixParcelable> CREATOR = new Parcelable.Creator<MetrixParcelable>() {
    	
        public MetrixParcelable createFromParcel(Parcel in) {
            return new MetrixParcelable(in);
        }

        public MetrixParcelable[] newArray(int size) {
            return new MetrixParcelable[size];
        }
    };
    
    public MetrixParcelable(T value){
    	this.mValue = value;
    	if(this.mValue != null)
    		MetrixParcelable.mClassLoader = value.getClass().getClassLoader();
    } 
    
	@SuppressWarnings("unchecked")
	private MetrixParcelable(Parcel parcelIn) {
    	try{
    		mValue = (T)parcelIn.readValue(MetrixParcelable.mClassLoader);
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
	public T getValue(){
		return (T) mValue;
    }

}
