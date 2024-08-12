package com.metrix.architecture.services;

class MmMessageOut {
	public int message_id;
	public String person_id;
	public String transaction_type;
	public String status;				// client side variable only
	public String table_name;			// client side variable only
	public int metrix_log_id; 			// client side variable only
	public String related_message_id; 	// Server side variable only
	public String message;
	public String attachment;
	public String transaction_desc; 	// client side variable only
	public String synced_dttm;
	public String created_dttm;
	public String modified_dttm;
}