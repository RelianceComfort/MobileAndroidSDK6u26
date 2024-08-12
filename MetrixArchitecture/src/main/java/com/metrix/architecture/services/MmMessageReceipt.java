package com.metrix.architecture.services;

import com.google.gson.annotations.SerializedName;

class MmMessageReceipt {
	@SerializedName("message_id")
	public int message_id;
	
	@SerializedName("person_id")	
	public String person_id;
	
	@SerializedName("device_sequence")
	public int device_sequence;
	
	@SerializedName("created_dttm")
	public String created_dttm;
}
