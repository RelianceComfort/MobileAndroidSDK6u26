package com.metrix.architecture.slidingmenu;

public class MetrixSlidingMenuItem {

	private String mTitle;
    private String mCount; 
    private int mIconResourceId;
    private int mIndex;

    public MetrixSlidingMenuItem(String title, int iconResourceId) {
        this.mTitle = title;
        this.mIconResourceId = iconResourceId;
    }

    public MetrixSlidingMenuItem(String title, int iconResourceId, int index) {
        this.mTitle = title;
        this.mIconResourceId = iconResourceId;
        this.mIndex = index;
    }

    public MetrixSlidingMenuItem(String title, String count, int iconResourceId) {
        this.mTitle = title;
        this.mCount = count;
        this.mIconResourceId = iconResourceId;
    }

    public MetrixSlidingMenuItem(String title, String count, int iconResourceId, int index) {
        this.mTitle = title;
        this.mCount = count;
        this.mIconResourceId = iconResourceId;
        this.mIndex = index;
    }

    public String getTitle() { 
        return mTitle; 
    } 

    public void setTitle(String title) {
        this.mTitle = title; 
    }

    public int getIconResourceId() {
        return mIconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.mIconResourceId = iconResourceId;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

	public String getCount() {
		return mCount;
	}

	public void setCount(String count) {
		this.mCount = count;
	}
	
}
