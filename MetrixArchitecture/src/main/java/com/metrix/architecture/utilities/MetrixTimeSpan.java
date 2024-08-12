package com.metrix.architecture.utilities;

public class MetrixTimeSpan {
	public long mMilliseconds = 0;
	
	public MetrixTimeSpan() { }
	
	public MetrixTimeSpan(long ms) {
		mMilliseconds = ms;
	}
	
	public void addDays(long days) {
		mMilliseconds = mMilliseconds + (24 * 60 * 60 * 1000 * days);
	}
	
	public void addHours(long hours) {
		mMilliseconds = mMilliseconds + (60 * 60 * 1000 * hours);
	}
	
	public void addMinutes(long minutes) {
		mMilliseconds = mMilliseconds + (60 * 1000 * minutes);
	}
	
	public void addSeconds(long seconds) {
		mMilliseconds = mMilliseconds + (1000 * seconds);
	}
	
	public void addMilliseconds(long ms) {
		mMilliseconds = mMilliseconds + ms;
	}
}
