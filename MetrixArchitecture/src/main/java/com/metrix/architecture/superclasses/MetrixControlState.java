package com.metrix.architecture.superclasses;

import java.io.Serializable;
import java.util.ArrayList;

public class MetrixControlState implements Serializable {
    public boolean mIsEnabled;
    public boolean mIsRequired;
    public boolean mIsVisible;
    public String mValue;
    public ArrayList<Object> mSpinnerItems;

    public MetrixControlState(boolean isEnabled, boolean isRequired, boolean isVisible, String value) {
        mIsEnabled = isEnabled;
        mIsRequired = isRequired;
        mIsVisible = isVisible;
        mValue = value;
        mSpinnerItems = null;
    }
}
