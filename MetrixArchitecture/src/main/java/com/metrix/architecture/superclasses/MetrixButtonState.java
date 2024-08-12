package com.metrix.architecture.superclasses;

import java.io.Serializable;

public class MetrixButtonState implements Serializable {
    public String mLabel;
    public boolean mIsEnabled;
    public boolean mIsVisible;
    public String mTag;

    public MetrixButtonState(String label, boolean isEnabled, boolean isVisible)
    {
        mLabel = label;
        mIsEnabled = isEnabled;
        mIsVisible = isVisible;
        mTag = null;
    }

    public MetrixButtonState(String label, boolean isEnabled, boolean isVisible, String tag)
    {
        mLabel = label;
        mIsEnabled = isEnabled;
        mIsVisible = isVisible;
        mTag = tag;
    }
}
