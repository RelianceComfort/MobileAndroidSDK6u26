package com.metrix.architecture.services;

import com.metrix.architecture.constants.MetrixTransactionTypes;

public class MmMessageIn {
	public long message_id;
	public String server_message_id;
	public String person_id;
	public MetrixTransactionTypes transaction_type;
	public String related_message_id;
	public String message;
	public int retry_num;
	public String status;
	// public String attachment;
	public String created_dttm;
}
