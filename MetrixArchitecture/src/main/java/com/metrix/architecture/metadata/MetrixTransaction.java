package com.metrix.architecture.metadata;

import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.Global.UploadType;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * Defines the meta data identifying a synchronization transaction. One possible
 * configuration of the app is to guarantee the sequential processing of all messages
 * by transaction. Each update performed therefore should identify the sync transaction
 * it belong to. Typically this is done by referencing the primary key of the parent
 * table. 
 */
public class MetrixTransaction {
	/**
	 * The sequential id of the transaction.
	 */
	public int mTransaction_id=0;
	
	/**
	 * The name of the transaction.
	 */
	public String transactionKeyId="";
	
	/**
	 * The name of the transaction key.
	 */
	public String transactionKeyName="";

	/**
	 * A convenience constructor.
	 */
	public MetrixTransaction(){
		mTransaction_id = MetrixUpdateManager.getTransactionId("", "");
	}
	
	/**
	 * A convenience constructor.
	 * 
	 * @param keyId The name of the transaction.
	 * @param keyName The name of the transaction key.
	 */
	public MetrixTransaction(String keyId, String keyName){
		mTransaction_id = MetrixUpdateManager.getTransactionId(keyId, keyName);
		transactionKeyId = keyId;
		transactionKeyName = keyName;
	}	
	
	/**
	 * A convenience constructor.
	 * 
	 * @param transactionId The sequential id of the transaction.
	 * @param keyId The name of the transaction.
	 * @param keyName The name of the transaction key.
	 */
	public MetrixTransaction(int transactionId, String keyId, String keyName){
		mTransaction_id = transactionId;
		transactionKeyId = keyId;
		transactionKeyName = keyName;
	}
	
	/**
	 * A convenience constructor.
	 * 
	 * @param parentTableName The name of the table.
	 * @param keyName The name of the transaction key.
	 * @return The associated MetrixTransaction.
	 */
	public static MetrixTransaction getTransaction(String parentTableName, String keyName){
		MetrixTransaction MetrixTransaction;
		
		if(MetrixStringHelper.isNullOrEmpty(parentTableName)||MetrixStringHelper.isNullOrEmpty(keyName))
		{
			return MetrixTransaction = new MetrixTransaction();
		}
		
		String syncMethod = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("SYNC_TYPE"));
		if (syncMethod.compareToIgnoreCase(UploadType.RELATIONSHIP_KEY.toString()) == 0||syncMethod.compareToIgnoreCase(UploadType.MESSAGE_SEQUENCE.toString()) == 0) {
			MetrixTransaction = new MetrixTransaction();
		}
		else {
			MetrixTransaction = new MetrixTransaction(MetrixCurrentKeysHelper.getKeyValue(parentTableName, keyName), keyName);
		}
		return MetrixTransaction;		
	}	
}
