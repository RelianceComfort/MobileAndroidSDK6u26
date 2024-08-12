package com.metrix.architecture.utilities;

/**
 * Created by hesplk on 5/3/2016.
 */
import android.os.Parcel;
import android.os.Parcelable;

public class GenericParcelable<T> implements Parcelable {

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
    public static final Parcelable.Creator<GenericParcelable> CREATOR = new Parcelable.Creator<GenericParcelable>() {

        public GenericParcelable createFromParcel(Parcel in) {
            return new GenericParcelable(in);
        }

        public GenericParcelable[] newArray(int size) {
            return new GenericParcelable[size];
        }
    };

    public GenericParcelable(T value){
        this.mValue = value;
        if(this.mValue != null)
            GenericParcelable.mClassLoader = value.getClass().getClassLoader();
    }

    @SuppressWarnings("unchecked")
    private GenericParcelable(Parcel parcelIn) {
        try{
            //reading the passed value
            mValue = (T)parcelIn.readValue(GenericParcelable.mClassLoader);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    //generic method of obtaining the parcel value
    public T getValue(){
        return (T) mValue;
    }

}
