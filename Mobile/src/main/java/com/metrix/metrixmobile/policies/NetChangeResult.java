package com.metrix.metrixmobile.policies;

public class NetChangeResult {
	public NetChangeResult(boolean meterRolled, double netChange) {
		this.meterRolled = meterRolled;
		this.netChange = netChange;
	}
	
	public boolean meterRolled = false;
	public double netChange;
}