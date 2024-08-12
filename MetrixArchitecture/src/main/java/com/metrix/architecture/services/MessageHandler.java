package com.metrix.architecture.services;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.constants.MetrixTransactionTypesConverter;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixKeys;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.metadata.TableColumnDef;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.Global.MessageInStatus;
import com.metrix.architecture.utilities.Global.MessageStatus;
import com.metrix.architecture.utilities.Global.UploadType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDatabases;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixPasswordHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.TaskDeliveryStatusUpdateHelper;
import com.metrix.architecture.utilities.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A handler class to process the data for all mm_message tables
 *
 * @author elin
 *
 */
public class MessageHandler {

	private static final String IFS_GET_PREFIX = "perform_get_ifs_";

	public MessageHandler() {
	}

	public static ArrayList<MmMessageOut> getMessagesToSend() {
		ArrayList<MmMessageOut> messages = new ArrayList<MmMessageOut>();

		MetrixCursor cursor = null;

		String syncMethod = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("SYNC_TYPE"));
		try {
			if (syncMethod.compareToIgnoreCase(UploadType.RELATIONSHIP_KEY.toString()) == 0) {
				// over easy, as long as the primary key can be resolved by the
				// device, it is ready to go
				// It does not have to follow the message sequences on the
				// device.
				// sqlite data is case sensitive
				cursor = MetrixDatabaseManager.getRowsMC("mm_message_out", new String[] { "message_id", "person_id", "transaction_type", "table_name", "metrix_log_id", "message", "attachment", "created_dttm", "modified_dttm", "transaction_desc" }, "status='READY'");

				if (cursor == null || !cursor.moveToFirst()) {
					return null;
				}

				MmMessageOut message_out = new MmMessageOut();

				while (cursor.isAfterLast() == false) {
					message_out.message_id = cursor.getInt(0);
					message_out.person_id = cursor.getString(1);
					message_out.transaction_type = cursor.getString(2);
					message_out.table_name = cursor.getString(3);
					message_out.metrix_log_id = cursor.getInt(4);
					String messageValue = getMessageFromTransLogTable(message_out.transaction_type, message_out.table_name, message_out.metrix_log_id);
					String originalMessage = MetrixStringHelper.isNullOrEmpty(messageValue) ? cursor.getString(5) : messageValue;
					message_out.message = getAuthenticatedMessage(originalMessage);
					message_out.attachment = cursor.getString(6);
					message_out.created_dttm = cursor.getString(7);
					message_out.transaction_desc = cursor.getString(8);

					messages.add(message_out);
					cursor.moveToNext();
				}
			} else if (syncMethod.compareToIgnoreCase(UploadType.TRANSACTION_INDEPENDENT.toString()) == 0) {
				// Messages can be sent out based on the Transaction status, as
				// long as the early messages in the queue of the Transaction
				// are READY, the messages with the READY status can be sent
				// out.
				ArrayList<Hashtable<String, String>> transactionList = MetrixDatabaseManager.getFieldStringValuesList("mm_message_out", new String[] { "transaction_id" }, "status in ('" + MessageStatus.READY.toString() + "', '"
						+ MessageStatus.WAITING_PREVIOUS.toString() + "', '"
						+ MessageStatus.WAITING.toString() + "', '"
						+ MessageStatus.SENT.toString()+"')", null, null, "message_id", null);

				if (transactionList != null) {
					for (Hashtable<String, String> transactionRow : transactionList) {
						getMessagesFromTransaction(transactionRow.get("transaction_id"), messages, "READY", false);
					}
				}
			} else if (syncMethod.compareToIgnoreCase(UploadType.MESSAGE_SEQUENCE.toString()) == 0) {
				// message needs to be uploaded based on the exact sequences of
				// the message generated by the device
				getMessagesFromTransaction("0", messages, "READY", true);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// don't send any part_usage_serial or part_usage_lot messages. they're included with
		// part_usage messages. once the serial is 'READY'
		// that means that the part_usage was uploaded successfully, so we don't
		// need them anymore (delete them to clean
		// them up
		removePartUsageSerialMessages(messages);
		removePartUsageLotMessages(messages);

		return messages;
	}

	public static boolean messagesAreQueuedWithoutErrors() {
		String msgCountString = MetrixDatabaseManager.getFieldStringValue("select count(*) from (select message_id, status, transaction_id from mm_message_out " +
				"group by transaction_id having min(message_id) and status = 'READY')");
		int msgCount = Integer.parseInt(msgCountString);
		return (msgCount > 0);
	}

	public static void updateSentQueueSyncTime() {
		ArrayList<MmMessageOut> messages = new ArrayList<MmMessageOut>();

		MetrixCursor cursor = null;

		try {
			cursor = MetrixDatabaseManager.getRowsMC("mm_message_out", new String[] { "message_id", "person_id", "status"}, "status='SENT'");

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			MmMessageOut message_out = new MmMessageOut();

			while (cursor.isAfterLast() == false) {
				message_out.message_id = cursor.getInt(0);
				message_out.person_id = cursor.getString(1);
				message_out.status = cursor.getString(2);

				messages.add(message_out);
				cursor.moveToNext();
			}

			if (messages == null || messages.size() == 0) {
				return;
			}

			Iterator<MmMessageOut> iterator = messages.iterator();

			while (iterator.hasNext()) {
				MmMessageOut message = (MmMessageOut) iterator.next();
				if (message.status != null && message.status.compareToIgnoreCase("sent") == 0) {
					ArrayList<DataField> updateFields = new ArrayList<DataField>();

					updateFields.add(new DataField("synced_dttm", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS))));
					MetrixDatabaseManager.updateRow("mm_message_out", updateFields, "message_id=" + message.message_id);
					iterator.remove();
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

	}

	private static void removePartUsageSerialMessages(ArrayList<MmMessageOut> messages) {
		if (messages == null || messages.size() == 0) {
			return;
		}

		Iterator<MmMessageOut> iterator = messages.iterator();

		while (iterator.hasNext()) {
			MmMessageOut message = (MmMessageOut) iterator.next();
			if (message.table_name != null && message.table_name.compareToIgnoreCase("part_usage_serial") == 0) {
				MetrixDatabaseManager.deleteRow("part_usage_serial_log", "metrix_log_id=" + message.metrix_log_id);
				MetrixDatabaseManager.deleteRow("mm_message_out", "message_id=" + message.message_id);
				iterator.remove();
			}
		}
	}

	private static void removePartUsageLotMessages(ArrayList<MmMessageOut> messages) {
		if (messages == null || messages.size() == 0) {
			return;
		}

		Iterator<MmMessageOut> iterator = messages.iterator();

		while (iterator.hasNext()) {
			MmMessageOut message = (MmMessageOut) iterator.next();
			if (message.table_name != null && message.table_name.compareToIgnoreCase("part_usage_lot") == 0) {
				MetrixDatabaseManager.deleteRow("part_usage_lot_log", "metrix_log_id=" + message.metrix_log_id);
				MetrixDatabaseManager.deleteRow("mm_message_out", "message_id=" + message.message_id);
				iterator.remove();
			}
		}
	}

	private static void getMessagesFromTransaction(String transaction_id, ArrayList<MmMessageOut> messages, String messageStatus, boolean ignoreTransaction) {
		MetrixCursor cursor = null;
		String filter = "transaction_id=" + transaction_id;

		if (ignoreTransaction) {
			filter = "";
		}

		try {
			// sqlite data is case sensitive
			cursor = MetrixDatabaseManager.getRowsMC("mm_message_out", new String[] { "message_id", "person_id", "transaction_type", "table_name", "metrix_log_id", "message", "attachment", "created_dttm", "modified_dttm", "status", "synced_dttm" }, filter);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			int i = 0;

			while (cursor.isAfterLast() == false) {
				String status = cursor.getString(9);
				MmMessageOut message_out = new MmMessageOut();

				if (i == 0 && status.compareToIgnoreCase(MessageStatus.WAITING_PREVIOUS.toString()) == 0) {
					// all previous messages are resolved in the same
					// transaction queue. So it becomes the
					// first message in the queue
					message_out.message_id = cursor.getInt(0);
					message_out.person_id = cursor.getString(1);
					message_out.transaction_type = cursor.getString(2);
					message_out.table_name = cursor.getString(3) == null ? "" : cursor.getString(3);
					message_out.metrix_log_id = cursor.getInt(4);
					String messageValue = "";

					if (message_out.metrix_log_id != 0) {
						messageValue = getMessageFromTransLogTable(message_out.transaction_type, message_out.table_name, message_out.metrix_log_id);
					}

					String originalMessage = MetrixStringHelper.isNullOrEmpty(messageValue) ? cursor.getString(5) : messageValue;
					message_out.message = getAuthenticatedMessage(originalMessage);
					message_out.attachment = cursor.getString(6);
					message_out.created_dttm = cursor.getString(7);
					message_out.status = status;

					cursor.moveToNext();
					messages.add(message_out);

					return;
				}

				if (i == 0 && status.compareToIgnoreCase(MessageStatus.WAITING.toString()) == 0) {
					// all previous messages are processed in transaction queue. So WAITING becomes the
					// first message in the queue. Since no other response can change the status, we should remove the message.
					message_out.message_id = cursor.getInt(0);
					message_out.person_id = cursor.getString(1);
					message_out.transaction_type = cursor.getString(2);
					message_out.table_name = cursor.getString(3) == null ? "" : cursor.getString(3);
					message_out.metrix_log_id = cursor.getInt(4);
					message_out.modified_dttm = cursor.getString(8);
					message_out.status = status;
					message_out.synced_dttm = cursor.getString(10);

					// Check that this is the ABSOLUTE first message before proceeding to process timer for auto-delete
					// e.g., there may be a related INSERT message in ERROR queue that can be corrected on which this message is waiting for key resolution
					int count = MetrixDatabaseManager.getCount("mm_message_out", "message_id < " + cursor.getInt(0));
					if (count > 0) {
						i++;
						continue;
					}

					if (MetrixStringHelper.isNullOrEmpty(message_out.synced_dttm)) {
						cursor.moveToNext();

						ArrayList<DataField> updateFields = new ArrayList<DataField>();

						updateFields.add(new DataField("synced_dttm", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS))));
						updateFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS))));
						MetrixDatabaseManager.updateRow("mm_message_out", updateFields, "message_id=" + message_out.message_id);

						i++;
						continue;
					}

					Date syncDatetime = MetrixDateTimeHelper.convertDateTimeFromDBToDate(message_out.synced_dttm);
					Date modifiedDatetime = MetrixDateTimeHelper.convertDateTimeFromDBToDate(message_out.modified_dttm);

					long different = syncDatetime.getTime() - modifiedDatetime.getTime();
					long secondsInMilli = 1000;
					long minutesInMilli = secondsInMilli * 60;
					long elapsedMinutes = different / minutesInMilli;

					cursor.moveToNext();

					// WAITING message is in the Queue as first message longer than 2 minutes
					if(elapsedMinutes >= 2) {
						if (message_out.metrix_log_id != 0) {
							MetrixDatabaseManager.deleteRow("mm_message_out", "message_id="+message_out.message_id);
						}

						return;
					}
					else {
						ArrayList<DataField> updateFields = new ArrayList<DataField>();

						updateFields.add(new DataField("synced_dttm", MetrixDateTimeHelper.convertDateTimeFromUIToDB(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS))));
						MetrixDatabaseManager.updateRow("mm_message_out", updateFields, "message_id=" + message_out.message_id);

						i++;
						continue;
					}
				}

				if (status.compareToIgnoreCase(MessageStatus.SENT.toString()) == 0) {
					// all previous messages are resolved in the same
					// transaction queue. So it becomes the
					// first message in the queue
					message_out.message_id = cursor.getInt(0);
					message_out.person_id = cursor.getString(1);
					message_out.transaction_type = cursor.getString(2);
					message_out.table_name = cursor.getString(3) == null ? "" : cursor.getString(3);
					message_out.metrix_log_id = cursor.getInt(4);
					String messageValue = "";

					if (message_out.metrix_log_id != 0) {
						messageValue = getMessageFromTransLogTable(message_out.transaction_type, message_out.table_name, message_out.metrix_log_id);
					}

					String originalMessage = MetrixStringHelper.isNullOrEmpty(messageValue) ? cursor.getString(5) : messageValue;
					message_out.message = getAuthenticatedMessage(originalMessage);
					message_out.attachment = cursor.getString(6);
					message_out.created_dttm = cursor.getString(7);
					message_out.modified_dttm = cursor.getString(8);
					message_out.status = status;
					message_out.synced_dttm = cursor.getString(10);

					if(MetrixStringHelper.isNullOrEmpty(message_out.synced_dttm)) {
						cursor.moveToNext();
						i++;
						continue;
					}
					Date syncDatetime = MetrixDateTimeHelper.convertDateTimeFromDBToDate(message_out.synced_dttm);
					Date modifiedDatetime = MetrixDateTimeHelper.convertDateTimeFromDBToDate(message_out.modified_dttm);

					long different = syncDatetime.getTime() - modifiedDatetime.getTime();
					long secondsInMilli = 1000;
					long minutesInMilli = secondsInMilli * 60;
					long elapsedMinutes = different / minutesInMilli;

					cursor.moveToNext();

					if(elapsedMinutes >= 15) {
						messages.add(message_out);
						return;
					}
					else {
						i++;
						continue;
					}
				}

				// if the current message is not READY, skip the rest of the
				// messages
				if (status.compareToIgnoreCase(messageStatus) != 0) {
					return;
				}

				message_out.message_id = cursor.getInt(0);
				message_out.person_id = cursor.getString(1);
				message_out.transaction_type = cursor.getString(2);
				message_out.table_name = cursor.getString(3);
				message_out.metrix_log_id = cursor.getInt(4);
				String messageValue = getMessageFromTransLogTable(message_out.transaction_type, message_out.table_name, message_out.metrix_log_id);

				String originalMessage = MetrixStringHelper.isNullOrEmpty(messageValue) ? cursor.getString(5) : messageValue;

				message_out.message = getAuthenticatedMessage(originalMessage);
				message_out.attachment = cursor.getString(6);
				message_out.created_dttm = cursor.getString(7);
				message_out.status = status;

				cursor.moveToNext();

				messages.add(message_out);

				i++;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return;
	}

	public static String getAuthenticatedMessage(String originalMessage) {
		if(MetrixStringHelper.isNullOrEmpty(originalMessage)){
			return "";
		}

		String formattedMessage = originalMessage;

		try {
			JSONObject jsonOriginal = new JSONObject(originalMessage);

			if (jsonOriginal != null){
				JSONArray jElements = jsonOriginal.names();

				if (jElements != null && jElements.length() > 0) {
					String elementName = jElements.getString(0);
					JSONObject jElement = jsonOriginal.optJSONObject(elementName);

					JSONObject logonObj = new JSONObject();
					logonObj.put("session_id", SettingsHelper.getSessionId(MobileApplication.getAppContext()));
					logonObj.put("token_id", SettingsHelper.getTokenId(MobileApplication.getAppContext()));
					logonObj.put("person_id", User.getUser().personId);
					int deviceSequence = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
					logonObj.put("device_sequence", String.valueOf(deviceSequence));
					JSONObject authObj = new JSONObject();
					authObj.put("session_info", logonObj);
					jElement.put("authentication", authObj);

					formattedMessage = jsonOriginal.toString();
				}
			}
		} catch (JSONException je) {
			LogManager.getInstance().error(je);
		}

		return formattedMessage;
	}

	public static ArrayList<MmMessageReceipt> getReceiptsToSend() {
		ArrayList<MmMessageReceipt> messages = new ArrayList<MmMessageReceipt>();
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.getRowsMC("mm_message_receipt", new String[] { "message_id", "person_id", "created_dttm" }, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			while (cursor.isAfterLast() == false) {
				MmMessageReceipt message_receipt = new MmMessageReceipt();
				message_receipt.message_id = cursor.getInt(0);
				message_receipt.person_id = cursor.getString(1);
				message_receipt.created_dttm = cursor.getString(2);

				cursor.moveToNext();
				messages.add(message_receipt);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return messages;
	}

	public static boolean createReceipt(MmMessageIn clientMessageIn) {
		ArrayList<DataField> fields = new ArrayList<DataField>();
		fields.add(new DataField("message_id", clientMessageIn.server_message_id));
		fields.add(new DataField("person_id", clientMessageIn.person_id));
		fields.add(new DataField("created_dttm", clientMessageIn.created_dttm));

		return MetrixDatabaseManager.insertRow("mm_message_receipt", fields) > 0;
	}

	public static String getMessageFromTransLogTable(String trans_type, String tableName, int log_id) {
		// No table name in this message, skip query the log table
		if(MetrixStringHelper.isNullOrEmpty(tableName))
			return "";

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC("select * from " + tableName + "_log where metrix_log_id=" + log_id, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			HashMap<String, String> columnValuePair = new HashMap<String, String>();

			while (cursor.isAfterLast() == false) {

				String[] columnNames = cursor.getColumnNames();

				for (String column : columnNames) {
					if (column.compareToIgnoreCase("metrix_log_id") == 0 || column.compareToIgnoreCase("metrix_row_id") == 0)
						continue;

					int columnIndex = cursor.getColumnIndex(column);
					String value = cursor.getString(columnIndex);
					final int type = cursor.getType(columnIndex);

					if (type == Cursor.FIELD_TYPE_FLOAT && !MetrixStringHelper.isNullOrEmpty(value)
							&& !MetrixStringHelper.isInteger(value) && !MetrixStringHelper.isDouble(value)) {
						// The value is most likely to be in scientific notation

						String language = "en"; // default value is en
						String country = "US"; // default value is US

						if(!MetrixStringHelper.isNullOrEmpty(User.getUser().serverLocaleCode) && User.getUser().serverLocaleCode.contains("-")) {
							language = User.getUser().serverLocaleCode.split("-")[0];
							country = User.getUser().serverLocaleCode.split("-")[1];
						}

						final Locale serverLocale = new Locale(language, country);
						NumberFormat sNumericFormat = NumberFormat.getNumberInstance(serverLocale);
						((DecimalFormat) sNumericFormat).setNegativePrefix("-");
						sNumericFormat.setGroupingUsed(false);
						sNumericFormat.setMaximumFractionDigits(8);

						Number inNumber = null;
						try {
							inNumber = sNumericFormat.parse(value);
						} catch (Exception ex) {
							LogManager.getInstance().error(ex);
						}

						if (inNumber != null)
							value = sNumericFormat.format(inNumber);
						else
							value = "";
					}

					if (!MetrixStringHelper.isNullOrEmpty(value))
						columnValuePair.put(column, value);

//					else {
//						if(tableName.compareToIgnoreCase("attachment") == 0 && column.compareToIgnoreCase("attachment") == 0)
//						{
//							String filePath = "";
//							try {
//								filePath = cursor.getString(cursor.getColumnIndex("mobile_path"));
//								value = MetrixAttachmentManager.getInstance().getAttachmentStringFromFile(filePath+Global.ATTACHMENT_TEMP_EXTENSION);
//								MetrixAttachmentManager.getInstance().deleteAttachmentFile(filePath+Global.ATTACHMENT_TEMP_EXTENSION);
//							}
//							catch(Exception ex) {
//								LogManager.getInstance().error(ex.getMessage());
//							}
//							
//							if (!MetrixStringHelper.isNullOrEmpty(value))
//								columnValuePair.put(column, value);
//						}
//					}
				}

				cursor.moveToNext();
			}

			if (columnValuePair.size() > 0) {
				// JsonObject jsonObject = new JsonObject();
				// JsonObject sequential = new JsonObject();
				JsonObject update = new JsonObject();
				JsonObject table = new JsonObject();
				JsonObject nameValue = new JsonObject();

				for (String key : columnValuePair.keySet()) {
					String value = columnValuePair.get(key);

					if (trans_type.compareToIgnoreCase("insert") == 0) {
						// if it is insert transaction type, skip the primary
						// key values if the key is generated with negative
						// value before create the JSON message
						if (isMetrixTablePrimaryKey(tableName, key) && MetrixStringHelper.isNegativeValue(value)) {
							continue;
						}
					}

					nameValue.addProperty(key, value);
					table.add(tableName, nameValue);
				}

				if (tableName.compareToIgnoreCase("part_usage") == 0) {
					addSerialsToUsage(table, columnValuePair, tableName);
					addLotsToUsage(table, columnValuePair, tableName);
				}

				// add the transaction type insert/delete/update
				nameValue.addProperty(trans_type.toLowerCase(), "");
				table.add(tableName, nameValue);

				update.add("update_" + tableName, table);
				// sequential.add("sequential_dependent", update);
				// jsonObject.add("perform_batch", sequential);

				return update.toString();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return "";
	}

	private static void addLotsToUsage(JsonObject table, HashMap<String, String> partUsageColumns, String tableName) {
		MetrixCursor cursor = null;
		try {
			String puId = partUsageColumns.get("pu_id");
			cursor = MetrixDatabaseManager.rawQueryMC("select lot_id, quantity from part_usage_lot_log where pu_id=" + puId, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			JsonObject partUsageTable = null;
			if(table != null)
				partUsageTable = table.getAsJsonObject("part_usage");

			JsonArray nameValueArr = new JsonArray();
			while (cursor.isAfterLast() == false) {
				JsonObject nameValue = new JsonObject();
				nameValue.addProperty("lot_id", cursor.getString(0));
				nameValue.addProperty("quantity", cursor.getString(1));
				nameValueArr.add(nameValue);
				cursor.moveToNext();
			}

			if(partUsageTable != null)
				partUsageTable.add("part_usage_lot", nameValueArr);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static void addSerialsToUsage(JsonObject table, HashMap<String, String> partUsageColumns, String tableName) {
		MetrixCursor cursor = null;
		try {
			String puId = partUsageColumns.get("pu_id");
			cursor = MetrixDatabaseManager.rawQueryMC("select serial_id from part_usage_serial_log where pu_id=" + puId, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			JsonObject partUsageTable = null;
			if(table != null)
				partUsageTable = table.getAsJsonObject("part_usage");

			JsonArray nameValueArr = new JsonArray();
			while (cursor.isAfterLast() == false) {
				JsonObject nameValue = new JsonObject();
				nameValue.addProperty("serial_id", cursor.getString(0));
				nameValueArr.add(nameValue);
				cursor.moveToNext();
			}

			if(partUsageTable != null)
				partUsageTable.add("part_usage_serial", nameValueArr);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Create perform JSON message for M5 to process
	 * perform_batch{sequential_dependent{perform_method_name
	 *
	 * @param methodName
	 * @param columnValuePair
	 * @return String
	 */
	public static String createPerformJsonMessage(String methodName, Hashtable<String, String> columnValuePair) {
		if (columnValuePair.size() > 0) {
			JsonObject jsonMessage = new JsonObject();
			JsonObject sequential = new JsonObject();
			JsonObject perform_method = new JsonObject();
			JsonObject parameters = new JsonObject();
			JsonObject nameValue = new JsonObject();

			for (String key : columnValuePair.keySet()) {
				String value = columnValuePair.get(key);

				nameValue.addProperty(key, value);
				parameters.add("parameters", nameValue);
			}

			perform_method.add("perform_" + methodName, parameters);
			sequential.add("sequential_dependent", perform_method);
			jsonMessage.add("perform_batch", sequential);

			return jsonMessage.toString();
		}

		return "";
	}

	public static String createUpdateJsonMessage(String tableName, Hashtable<String, String> columnValuePair, String transactionType) {
		if (columnValuePair.size() > 0) {
			JsonObject update_method = new JsonObject();
			JsonObject table = new JsonObject();
			JsonObject nameValue = new JsonObject();

			for (String key : columnValuePair.keySet()) {
				String value = columnValuePair.get(key);

				nameValue.addProperty(key, value);
				table.add(tableName, nameValue);
			}

			// add the transaction type insert/delete/update
			nameValue.addProperty(transactionType.toLowerCase(), "");
			table.add(tableName, nameValue);

			update_method.add("update_" + tableName, table);
			return update_method.toString();
		}

		return "";
	}

	/**
	 * Update message status in mm_message_out table
	 *
	 * @param message
	 * @param status
	 */
	public static void updateMessageStatus(MmMessageOut message, com.metrix.architecture.utilities.Global.MessageStatus status) {
		ArrayList<DataField> updateFields = new ArrayList<DataField>();
		updateFields.add(new DataField("status", status.toString()));
		updateFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));

		MetrixDatabaseManager.updateRow("mm_message_out", updateFields, "message_id=" + message.message_id);
	}

	/**
	 * Update message status in mm_message tables
	 *
	 * @param tableName
	 * @param message_id
	 * @param status
	 */
	public static void updateMessageStatus(String tableName, int message_id, MessageStatus status) {
		ArrayList<DataField> updateFields = new ArrayList<DataField>();
		updateFields.add(new DataField("status", status.toString()));

		if(tableName.compareToIgnoreCase("mm_message_out") == 0)
			updateFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));

		MetrixDatabaseManager.updateRow(tableName, updateFields, "message_id=" + message_id);
	}

	/**
	 * Update message fields in mm_message tables
	 *
	 * @param tableName
	 * @param message_id
	 * @param fields
	 *            to be updated
	 */
	public static void updateMessageTableFields(String tableName, long message_id, Hashtable<String, String> fields) {
		ArrayList<DataField> updateFields = new ArrayList<DataField>();

		for (String fieldName : fields.keySet()) {
			updateFields.add(new DataField(fieldName, fields.get(fieldName)));
		}

		if(tableName.compareToIgnoreCase("mm_message_out") == 0)
			updateFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));

		MetrixDatabaseManager.updateRow(tableName, updateFields, "message_id=" + message_id);
	}

	public static void deleteMessage(String table_name, long message_id) {
		MetrixDatabaseManager.deleteRow(table_name, "message_id=" + message_id);
	}

	public static void deleteTransLog(Hashtable<String, String> messageOutFields) {
		if (messageOutFields != null) {
			String tableName = messageOutFields.get("table_name");
			String logId = messageOutFields.get("metrix_log_id");
			if (!MetrixStringHelper.isNullOrEmpty(tableName) && !MetrixStringHelper.isNullOrEmpty(logId)) {
				MetrixDatabaseManager.deleteRow(tableName + "_LOG", "metrix_log_id=" + logId);
			}
		}
	}

	public static void deleteMessageAndRelatedMessages(String messageId, String tableName, String messageIdIn, int logId) {
		try {
			String transactionType = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "transaction_type", "message_id="+messageId);

			MessageHandler.deleteMessage("mm_message_out", Long.parseLong(messageId));
			MessageHandler.deleteMessage("mm_message_in", Long.parseLong(messageIdIn));

			if (logId != -1) {
				ArrayList<Hashtable<String, String>> foreignKeys = MetrixDatabaseManager.getFieldStringValuesList("mm_foreign_keys", new String[] {
						"column_name", "foreign_table_name", "foreign_column_name" }, "table_name = '" + tableName + "'");

				if (foreignKeys != null) {
					for (Hashtable<String, String> foreignKey : foreignKeys) {
						String keyValue = MetrixDatabaseManager.getFieldStringValue(tableName + "_LOG", foreignKey.get("column_name"), "metrix_log_id = "
								+ Integer.toString(logId));
						ArrayList<Hashtable<String, String>> logIdsToRemove = MetrixDatabaseManager.getFieldStringValuesList(
								foreignKey.get("foreign_table_name") + "_LOG", new String[] { "metrix_log_id" }, foreignKey.get("foreign_column_name") + " = '"
										+ keyValue + "'");

						if (logIdsToRemove != null && logIdsToRemove.size() > 0) {
							for (Hashtable<String, String> logIdToRemove : logIdsToRemove) {
								if (!MetrixStringHelper.isNullOrEmpty(transactionType) && transactionType.equalsIgnoreCase("INSERT")) {
									String rowId = MetrixDatabaseManager.getFieldStringValue(foreignKey.get("foreign_table_name") + "_LOG", "metrix_row_id",
											"metrix_log_id = " + logIdToRemove.get("metrix_log_id"));
									MetrixDatabaseManager.deleteRow(foreignKey.get("foreign_table_name"),
											"metrix_row_id = " + rowId);
								}
								MetrixDatabaseManager.deleteRow(foreignKey.get("foreign_table_name") + "_LOG",
										"metrix_log_id = " + logIdToRemove.get("metrix_log_id"));

								MetrixDatabaseManager.deleteRow("mm_message_out", "table_name='" + foreignKey.get("foreign_table_name")
										+ "' and metrix_log_id = " + logIdToRemove.get("metrix_log_id"));
							}
						}
					}
				}

				// This message created a trans_log row
				Hashtable<String, String> messageOutFields = new Hashtable<String, String>();
				messageOutFields.put("table_name", tableName);
				messageOutFields.put("metrix_log_id", Integer.toString(logId));

				// get the other transaction based on the same metrix_row_id
				String dataRowId = MetrixDatabaseManager.getFieldStringValue(tableName+"_log", "metrix_row_id", "metrix_log_id="+logId);
				ArrayList<Hashtable<String, String>> logIdList = MetrixDatabaseManager.getFieldStringValuesList(tableName+"_log", new String[]{"metrix_log_id"}, "metrix_row_id="+dataRowId);

				// delete the current error records
				MessageHandler.deleteTransLog(messageOutFields);

				if (!MetrixStringHelper.isNullOrEmpty(transactionType) && transactionType.equalsIgnoreCase("INSERT")) {
					// delete all transaction messages
					if (logIdList != null && logIdList.size() > 0) {
						for (Hashtable<String, String> logIdToRemove : logIdList) {
							MetrixDatabaseManager.deleteRow(tableName + "_LOG",
									"metrix_log_id = " + logIdToRemove.get("metrix_log_id"));
							MetrixDatabaseManager.deleteRow("mm_message_out", "table_name='" + tableName
									+ "' and metrix_log_id = " + logIdToRemove.get("metrix_log_id"));
						}
					}

					MetrixDatabaseManager.deleteRow(tableName, "metrix_row_id = " + dataRowId);
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	public static boolean saveDownloadMessage(MmMessageIn clientMessageIn, MessageStatus status, int priority) {
		long retvalue = -1;

		try {
			ArrayList<DataField> fields = new ArrayList<DataField>();
			// fields.add(new DataField("message_id",
			// clientMessageIn.message_id));
			fields.add(new DataField("person_id", clientMessageIn.person_id));

			fields.add(new DataField("transaction_type", MetrixTransactionTypesConverter.toString(clientMessageIn.transaction_type)));
			fields.add(new DataField("message", clientMessageIn.message));
			fields.add(new DataField("related_message_id", clientMessageIn.related_message_id));
			fields.add(new DataField("status", clientMessageIn.status));
			fields.add(new DataField("retry_num", clientMessageIn.retry_num + 1));
			fields.add(new DataField("created_dttm", clientMessageIn.created_dttm));
			fields.add(new DataField("created_dttm", clientMessageIn.created_dttm));

			if (clientMessageIn.retry_num != 0) {
				// this is from the mm_message_in table
				return MetrixDatabaseManager.updateRow("mm_message_in", fields, "message_id=" + clientMessageIn.message_id);
			} else {
				// this message is just downloaded from mobile service.
				retvalue = MetrixDatabaseManager.insertRow("mm_message_in", fields);
				if (retvalue > 0)
					clientMessageIn.message_id = retvalue;
				return retvalue > 0;
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}
	}

	/**
	 * Process the message that received from remote server
	 *
	 * @param context
	 * @param clientMessageIn
	 * @return
	 */
	public static MetrixProcessStatus processDownloadMessages(Context context, MmMessageIn clientMessageIn, boolean enableConstraint) {
		MetrixProcessStatus processStatus = new MetrixProcessStatus();
		boolean updateResponse = false;
		boolean isPasswordUpdate = false;

		try {
			String[] statements;
			String matchResult = "";
			String messageToTest = "";

			if (clientMessageIn == null) {
				processStatus.mSuccessful = false;
				return processStatus;
				// return false;
			} else {
				statements = new String[1];
				statements[0] = clientMessageIn.message;
			}

			ArrayList<MetrixTableValues> tableDataCollection = new ArrayList<MetrixTableValues>();

			try {
				if (clientMessageIn.transaction_type == MetrixTransactionTypes.INSERT || clientMessageIn.transaction_type == MetrixTransactionTypes.UPDATE || clientMessageIn.transaction_type == MetrixTransactionTypes.DELETE) {
					String replicationResultExpression = "replication_result";
					String testMessage = "";

					if(clientMessageIn.message.length()> 100)
					{
						testMessage = clientMessageIn.message.substring(0, 100);
					}
					else {
						testMessage = clientMessageIn.message;
					}

					if (MetrixStringHelper.doRegularExpressionMatch(replicationResultExpression, testMessage)) {
						clientMessageIn.message = clientMessageIn.message.replace("u+0027", "'");
						clientMessageIn.message = clientMessageIn.message.replace("&gt;",">");
						clientMessageIn.message = clientMessageIn.message.replace("&lt;","<");
						clientMessageIn.message = clientMessageIn.message.replace("&amp;","&");

						JSONObject JsonMessage = new JSONObject(clientMessageIn.message);

						JSONObject jsonReplication = null;
						if(JsonMessage != null)
							jsonReplication = JsonMessage.optJSONObject("replication_result");

						if(jsonReplication != null && jsonReplication.has("rows")) {
							JSONArray jArrayRows = (JSONArray)jsonReplication.get("rows");
							ArrayList<String> rows = new ArrayList<String>();

							if(jArrayRows != null) {
								for (int m = 0; m < jArrayRows.length(); m++) {
									JSONObject jRow = jArrayRows.optJSONObject(m);
									if(jRow != null)
										rows.add(jRow.getString("row"));
								}
							}

							if (clientMessageIn.message.indexOf("u+0022") > -1) {
								for (int i = 0; i < rows.size(); i++) {
									rows.set(i, rows.get(i).replace("u+0022", "\""));
								}
							}

							boolean successful = MetrixDatabaseManager.executeSqlArray((ArrayList<String>) rows);
							updateMmMessageInOnSuccessfulProcessing(clientMessageIn, successful);
							processStatus.mSuccessful = successful;
							return processStatus;
						}
					} else {
						for (int i = 0; i < statements.length; i++) {
							// tableDataCollection = getTableValuesFromJsonMessage(statements[i]);
							// check if it is the table_hierarchy_select_result or
							// update_table_result
							statements[i] = statements[i].replace("\"\"", "null");

							if (statements[i].contains(getIfsGetPrefix())) {
								int startPos = statements[i].indexOf(getIfsGetPrefix()) + getIfsGetPrefix().length();
								int endPos = statements[i].indexOf("_result\"", startPos);
								String table_name = statements[i].substring(startPos, endPos);
								JSONObject jPerformTest = new JSONObject(statements[i]);
								JSONObject jtest = new JSONObject();
								if(jPerformTest != null)
									jtest.put(table_name + "_hierarchy_select_result", jPerformTest.get(getIfsGetPrefix() + table_name + "_result"));
								statements[i] = jtest.toString();
							}

							// use maximum 100 characters to test what type of
							// message is received to reduce the stack memory usage
							if (statements[i].length() > 100) {
								messageToTest = statements[i].substring(0, 100);
							} else {
								messageToTest = statements[i];
							}

							String hierarchy_select_expression = "[a-z_0-9]{1,}_hierarchy_select_result";
							String update_result_expression = "update_[a-z_0-9]{1,}_result";
							String metrix_response_expression = "metrix_response";
							String perform_login_expression = "perform_login_result";
							String perform_credit_expression = "perform_mobile_authorize_payment_result";
							String perform_batch_expression = "perform_batch_result";
							String perform_result_expression = "perform_[a-z_0-9]{1,}_result";

							if (MetrixStringHelper.doRegularExpressionMatch(hierarchy_select_expression, messageToTest) || MetrixStringHelper.doRegularExpressionMatch(update_result_expression, messageToTest)
									|| MetrixStringHelper.doRegularExpressionMatch(metrix_response_expression, messageToTest)
									|| MetrixStringHelper.doRegularExpressionMatch(perform_credit_expression, messageToTest)) {
								String hierarchySelect = MetrixStringHelper.getRegularExpressionMatch(hierarchy_select_expression, messageToTest);
								String updateResult = MetrixStringHelper.getRegularExpressionMatch(update_result_expression, messageToTest);

								String metrixResponse = MetrixStringHelper.getRegularExpressionMatch(metrix_response_expression, messageToTest);
								String creditResponse = MetrixStringHelper.getRegularExpressionMatch(perform_credit_expression, messageToTest);

								if (updateResult != null && updateResult != "") {
									matchResult = updateResult;
									updateResponse = true;
								} else if (hierarchySelect != null && hierarchySelect != "") {
									matchResult = hierarchySelect;
									String tableName = hierarchySelect.replace("_hierarchy_select_result", "");
									processStatus.mTableName = tableName;
								}
								else if (MetrixStringHelper.isNullOrEmpty(creditResponse) == false) {
									matchResult = creditResponse;
									processStatus.mSuccessful = true;
									return processStatus;
								}
								else if (MetrixStringHelper.isNullOrEmpty(metrixResponse) == false) {
									matchResult = metrixResponse;
								}

								JSONObject jResult = new JSONObject(statements[i]);
								// String table_name =
								// hierarchySelect.replace("_hierarchy_select_result", "");
								JSONObject jSelect = null;
								if(jResult != null)
									jSelect = jResult.optJSONObject(matchResult);

								if (jSelect != null) {
									JSONArray jTables = jSelect.names();

									if (jTables != null) {
										for (int m = 0; m < jTables.length(); m++) {
											String tableName = jTables.getString(m);
											if(tableName.compareToIgnoreCase("task") == 0) {
												processStatus.mTableName = tableName;
											}

											if(updateResponse) {
												processStatus.mTableName = tableName;
											}

											JSONObject jTable = jSelect.optJSONObject(tableName);

											if (jTable != null) {
												JSONArray jArrays = jTable.names();

												if (jArrays != null) {
													String jName = jArrays.getString(0);
													if (!MetrixStringHelper.isNullOrEmpty(jName) && jName.compareToIgnoreCase("error") == 0 || jName.compareToIgnoreCase("@type") == 0) {
														if (tryToAutoProcessInsertError(clientMessageIn)) {
															processStatus.mSuccessful = false;
															processStatus.mErrorMessage = "";
															return processStatus;
														} else {
															Hashtable<String, String> retry_field = new Hashtable<String, String>();
															// if retry_num update to -1, that means it is an error message
															// from M5
															retry_field.put("retry_num", "-1");
															retry_field.put("status", MessageInStatus.ERROR.toString());
															retry_field.put("message", clientMessageIn.message.replace("chr(13)chr(10)", "\r\n"));
															updateMessageTableFields("mm_message_in", clientMessageIn.message_id, retry_field);

															// update corresponding mm_message_out row to ERROR
															if (!MetrixStringHelper.isNullOrEmpty(clientMessageIn.related_message_id)) {
																updateMessageStatus("mm_message_out", Integer.valueOf(clientMessageIn.related_message_id), MessageStatus.ERROR);
															}

															processStatus.mSuccessful = false;
															processStatus.mErrorMessage = getErrorMessage(clientMessageIn.message);
															return processStatus;
														}
													}
												}
												handleJsonTable(jTable, tableName, tableDataCollection, updateResponse);

												// if it's the person table, try to see if anything is a password update
												if (tableName.compareToIgnoreCase("person") == 0 && !isPasswordUpdate) {
													isPasswordUpdate = thisMessageIsAPasswordChange(jTable);
												}
											} else {
												// Try to find jsonArray inside of the
												// jTables
												JSONArray jTableArray = jSelect.optJSONArray(tableName);

												if (jTableArray != null) {
													for (int n = 0; n < jTableArray.length(); n++) {
														JSONObject jArrayTable = jTableArray.optJSONObject(n);

														if (jArrayTable != null) {
															handleJsonTable(jArrayTable, tableName, tableDataCollection, updateResponse);

															// if it's the person table, try to see if anything is a password update
															if (tableName.compareToIgnoreCase("person") == 0 && !isPasswordUpdate) {
																isPasswordUpdate = thisMessageIsAPasswordChange(jTable);
															}
														}
													}
												}
											}
										}
									}
								}
							}
							else if (MetrixStringHelper.doRegularExpressionMatch(perform_batch_expression, messageToTest)){
								JSONObject jResult = new JSONObject(statements[i]);
								JSONObject jPerformBatch = jResult.getJSONObject(perform_batch_expression);

								if (jPerformBatch != null) {
									JSONArray jPerforms = jPerformBatch.names();

									for (int m = 0; m < jPerforms.length(); m++) {
										String performName = jPerforms.getString(m);
										JSONObject jPerform = jPerformBatch.optJSONObject(performName);

										if (jPerform != null) {
											JSONArray jArrays = jPerform.names();

											if (jArrays != null) {
												String jName = jArrays.getString(0);

												if(jName.compareToIgnoreCase("result")==0) {
													JSONObject jPerformResult = jPerform.optJSONObject("result");

													if(jPerformResult != null)
													{
														JSONArray jElements = jPerformResult.names();

														String jElement = jElements.getString(0);

														if (jElement.compareToIgnoreCase("error") == 0 || jElement.compareToIgnoreCase("@type") == 0) {
															if (tryToAutoProcessInsertError(clientMessageIn)) {
																processStatus.mSuccessful = false;
																processStatus.mErrorMessage = "";
																return processStatus;
															} else {
																Hashtable<String, String> retry_field = new Hashtable<String, String>();
																// if retry_num update to -1, that means it is an error message from M5
																retry_field.put("retry_num", "-1");
																retry_field.put("status", MessageInStatus.ERROR.toString());
																retry_field.put("message", clientMessageIn.message.replace("chr(13)chr(10)", "\r\n"));
																updateMessageTableFields("mm_message_in", clientMessageIn.message_id, retry_field);

																// update corresponding mm_message_out row to ERROR
																if (!MetrixStringHelper.isNullOrEmpty(clientMessageIn.related_message_id)) {
																	updateMessageStatus("mm_message_out", Integer.valueOf(clientMessageIn.related_message_id), MessageStatus.ERROR);
																}

																processStatus.mSuccessful = false;
																processStatus.mErrorMessage = getErrorMessage(clientMessageIn.message);
																return processStatus;
															}
														}
													}
												}
											}
										}
									}
								}
							}
							else if (MetrixStringHelper.doRegularExpressionMatch(perform_login_expression, messageToTest)) {
								// perform_login_result received, then should save
								// it to user settings
								JSONObject jResult = new JSONObject(statements[i]);

								JSONObject jPerformLogin = null;
								if(jResult != null)
									jPerformLogin = jResult.getJSONObject(perform_login_expression);

								JSONObject jInfo = null;
								if(jPerformLogin != null)
									jInfo = jPerformLogin.optJSONObject("session_info");

								if (jInfo != null) {
									String serverDateTimeFormat = jInfo.optString("date_time_format");
									SettingsHelper.saveStringSetting(context, SettingsHelper.SERVER_DATE_TIME_FORMAT, JavaDotNetFormatHelper.convertDateTimeFormatFromDotNet(serverDateTimeFormat), false);
									User.getUser().serverDateTimeFormat = SettingsHelper.getServerDateTimeFormat(context);

									String serverDateFormat = jInfo.optString("date_format");
									SettingsHelper.saveStringSetting(context, SettingsHelper.SERVER_DATE_FORMAT, JavaDotNetFormatHelper.convertDateTimeFormatFromDotNet(serverDateFormat), false);
									User.getUser().serverDateFormat = SettingsHelper.getServerDateFormat(context);

									String serverTimeFormat = jInfo.optString("time_format");
									SettingsHelper.saveStringSetting(context, SettingsHelper.SERVER_TIME_FORMAT, JavaDotNetFormatHelper.convertDateTimeFormatFromDotNet(serverTimeFormat), false);
									User.getUser().serverTimeFormat = SettingsHelper.getServerTimeFormat(context);

									String serverLocaleCode = jInfo.optString(SettingsHelper.SERVER_LOCALE_CODE.toLowerCase());
									SettingsHelper.saveStringSetting(context, SettingsHelper.SERVER_LOCALE_CODE, serverLocaleCode, false);
									User.getUser().serverLocaleCode = SettingsHelper.getServerLocaleCode(context);

									String serverNumericFormat = jInfo.optString("numeric_format");
									SettingsHelper.saveStringSetting(context, SettingsHelper.SERVER_NUMERIC_FORMAT, serverNumericFormat, false);
									User.getUser().serverNumericFormat = SettingsHelper.getServerNumericFormat(context);

									String serverTimeZoneOffset = jInfo.optString(SettingsHelper.SERVER_TIME_ZONE_OFFSET.toLowerCase());
									SettingsHelper.saveStringSetting(context, SettingsHelper.SERVER_TIME_ZONE_OFFSET, serverTimeZoneOffset, false);
									User.getUser().serverTimeZoneOffset = SettingsHelper.getServerTimeZoneOffset(context);

									String serverTimeZoneId = jInfo.optString(SettingsHelper.SERVER_TIME_ZONE_ID.toLowerCase());
									serverTimeZoneId = JavaDotNetFormatHelper.convertTimeZoneIdFromDotNet(serverTimeZoneId);
									SettingsHelper.saveStringSetting(context, SettingsHelper.SERVER_TIME_ZONE_ID, serverTimeZoneId, false);
									User.getUser().serverTimeZoneId = serverTimeZoneId;

									String tokenId = jInfo.optString(SettingsHelper.TOKEN_ID.toLowerCase());
									SettingsHelper.saveTokenId(context, tokenId);

									String sessionId = jInfo.optString(SettingsHelper.SESSION_ID.toLowerCase());
									SettingsHelper.saveSessionId(context, sessionId);
								}
								//continue;
							}
							else if (MetrixStringHelper.doRegularExpressionMatch(perform_result_expression, messageToTest)){
								JSONObject jResult = new JSONObject(statements[i]);
								String performResult = MetrixStringHelper.getRegularExpressionMatch(perform_result_expression, messageToTest);
								JSONObject jPerformRegularResult = jResult.getJSONObject(performResult);
								JSONObject jPerformResult = jPerformRegularResult.optJSONObject("result");

								if(jPerformResult != null)
								{
									JSONArray jElements = jPerformResult.names();

									String jElement = jElements.getString(0);

									if (jElement.compareToIgnoreCase("error") == 0 || jElement.compareToIgnoreCase("@type") == 0) {
										if (tryToAutoProcessInsertError(clientMessageIn)) {
											processStatus.mSuccessful = false;
											processStatus.mErrorMessage = "";
											return processStatus;
										} else {
											Hashtable<String, String> retry_field = new Hashtable<String, String>();
											// if retry_num update to -1, that means it is an error message from M5
											retry_field.put("retry_num", "-1");
											retry_field.put("status", MessageInStatus.ERROR.toString());
											retry_field.put("message", clientMessageIn.message.replace("chr(13)chr(10)", "\r\n"));
											updateMessageTableFields("mm_message_in", clientMessageIn.message_id, retry_field);

											// update corresponding mm_message_out row to ERROR
											if (!MetrixStringHelper.isNullOrEmpty(clientMessageIn.related_message_id)) {
												updateMessageStatus("mm_message_out", Integer.valueOf(clientMessageIn.related_message_id), MessageStatus.ERROR);
											}

											processStatus.mSuccessful = false;
											processStatus.mErrorMessage = getErrorMessage(clientMessageIn.message);
											return processStatus;
										}
									}
								}
							}
						}

						ArrayList<String> sqlStatements = new ArrayList<String>();
						// after preprocess data, start saving the data into the
						// database
						// sqlStatements = buildProcessSql("INSERT", "0",
						// tableDataCollection, false);
						// This is a server response for the client message, the
						// insert/update will be treated as update
						if (!MetrixStringHelper.isNullOrEmpty(clientMessageIn.related_message_id)) {
							// Only need to generate update statement and execute it locally
							if (clientMessageIn.transaction_type == MetrixTransactionTypes.UPDATE || clientMessageIn.transaction_type == MetrixTransactionTypes.INSERT) {
								sqlStatements = buildProcessSql(MetrixTransactionTypes.UPDATE, clientMessageIn.transaction_type, clientMessageIn.related_message_id, tableDataCollection, false);

								if (clientMessageIn.transaction_type == MetrixTransactionTypes.INSERT) {
									ArrayList<String> logStmtArray = buildProcessSqlForInsertPKResolution(clientMessageIn.related_message_id, tableDataCollection, false, true);
									if (logStmtArray != null && logStmtArray.size() > 0)
										sqlStatements.addAll(logStmtArray);
								}
							}
						} else {
							// This message is initiated by server
							sqlStatements = buildProcessSql(clientMessageIn.transaction_type, null, clientMessageIn.related_message_id, tableDataCollection, false);
						}

						boolean ret_value = false;
						if (updateResponse && thereAreSubsequentUpdates(clientMessageIn)) {
							sqlStatements.clear();

							// if this response is for an insert message, then we still want to update the primary key
							// so regenerate sqlStatements with this in mind...
							if (clientMessageIn.transaction_type == MetrixTransactionTypes.INSERT) {
								sqlStatements = buildProcessSqlForInsertPKResolution(clientMessageIn.related_message_id, tableDataCollection, false, false);
							}
						}

						/**This method is used to retrieve the original owner of an existing task**/
						String taskOriginalOwner = TaskDeliveryStatusUpdateHelper.getOriginalOwnerOfTaskReceivedMessage(clientMessageIn);

						ret_value = MetrixDatabaseManager.executeSqlArray(sqlStatements, enableConstraint);

						updateMmMessageInOnSuccessfulProcessing(clientMessageIn, ret_value);
						// updateTableCounterID(tableDataCollection);

						// Determine whether we should update the task with person/device received information
						TaskDeliveryStatusUpdateHelper.generateNewTaskReceivedMessage(clientMessageIn, taskOriginalOwner);

						// if we found a password update to THIS user somewhere in the mix, signal Sync to handle it
						if (isPasswordUpdate) {
							processStatus.mErrorMessage = "SERVER PASSWORD UPDATE";
						}

						processStatus.mSuccessful = ret_value;
						return processStatus;
					}
					// return ret_value;
				} else if (clientMessageIn.transaction_type == MetrixTransactionTypes.TRUNC) {
					ArrayList<String> sqlStatements = new ArrayList<String>();
					try {
						for (int i = 0; i < statements.length; i++) {
							JSONObject jResult = new JSONObject(statements[i]);

							if (jResult != null) {
								@SuppressWarnings("unchecked")
								Iterator<String> iter = jResult.keys();
								while (iter.hasNext()) {
									String tableName = (String) iter.next();
									String sqlStatement = "delete from " + tableName;
									sqlStatements.add(sqlStatement);

								}
							}
						}

						MetrixDatabaseManager.executeSqlArray(sqlStatements, enableConstraint);

						updateMmMessageInOnSuccessfulProcessing(clientMessageIn, true);

						processStatus.mSuccessful = true;
						return processStatus;
						// return ret_value;
						// return MetrixDatabaseManager
						// .executeSqlArray(sqlStatements);
					} catch (Exception ex) {
						LogManager.getInstance().error(ex);
					}
				}
			} catch (JsonParseException ex) {
				LogManager.getInstance().error(ex.getMessage() + " for parsing message:" + clientMessageIn.message);
			} catch (JSONException ex) {
				LogManager.getInstance().error(ex.getMessage() + " for message:" + clientMessageIn.message);
			} catch (Exception ex) {
				LogManager.getInstance().error(ex.getMessage() + " for message:" + clientMessageIn.message);
				Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
			}

			processStatus.mSuccessful = true;
			return processStatus;
			// return true;
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			processStatus.mSuccessful = false;
			return processStatus;
			// return false;
		}
	}

	private static boolean thisMessageIsAPasswordChange(JSONObject jTable) {
		try {
			if (jTable != null) {
				String currentUserPersonID = User.getUser().personId;
				String personCol = jTable.optString("person_id");
				personCol = personCol.replace("u+0027", "'");
				personCol = personCol.replace("u+0022", "\"").replace("u+005c", "\\");

				if (!MetrixStringHelper.isNullOrEmpty(personCol) && personCol.compareToIgnoreCase(currentUserPersonID) == 0) {
					String passCol = jTable.optString("password");
					if (!MetrixStringHelper.isNullOrEmpty(passCol)) {
						passCol = passCol.replace("u+0027", "'");
						passCol = passCol.replace("u+0022", "\"").replace("u+005c", "\\");

						String localDBpass = MetrixDatabaseManager.getFieldStringValue("person", "password", String.format("person_id = '%s'", currentUserPersonID));
						if (!MetrixStringHelper.isNullOrEmpty(localDBpass) && passCol.compareToIgnoreCase(localDBpass) != 0) {
							return true;
						}
					}
				}
			}
		} catch (JsonParseException ex) {
			LogManager.getInstance().error(ex);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return false;
	}

	private static void updateMmMessageInOnSuccessfulProcessing(MmMessageIn clientMessageIn, boolean ret_value) {
		Hashtable<String, String> retry_field = new Hashtable<String, String>();
		retry_field.put("retry_num",
				// if retry_num update to -1, that means it is an error
				// message from M5
				Integer.toString(clientMessageIn.retry_num + 1));
		if (ret_value) {
			retry_field.put("status", MessageInStatus.PROCESSED.toString());
		}
		updateMessageTableFields("mm_message_in", clientMessageIn.message_id, retry_field);
	}

	private static boolean thereAreSubsequentUpdates(MmMessageIn clientMessageIn) {
		if (MetrixStringHelper.isNullOrEmpty(clientMessageIn.related_message_id) ||
				clientMessageIn.related_message_id.compareToIgnoreCase("null") == 0) {
			return false;
		}

		String metrixLogId = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "metrix_log_id", "message_id = " + clientMessageIn.related_message_id);
		String tableName = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "table_name", "message_id = " + clientMessageIn.related_message_id) + "_LOG";
		String metrixRowId = MetrixDatabaseManager.getFieldStringValue(tableName, "metrix_row_id", "metrix_log_id = " + metrixLogId);

		if (MetrixDatabaseManager.getCount(tableName, "metrix_row_id = " + metrixRowId) > 1) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean tryToAutoProcessInsertError(MmMessageIn clientMessageIn) {
		try {
			if (clientMessageIn == null || MetrixStringHelper.isNullOrEmpty(clientMessageIn.related_message_id))
				return false;

			Hashtable<String, String> msgOutFields = MetrixDatabaseManager.getFieldStringValues("mm_message_out",
					new String[]{"metrix_log_id", "table_name", "transaction_type"}, "message_id = " + clientMessageIn.related_message_id);
			String metrixLogId = msgOutFields.get("metrix_log_id");
			String tableName = msgOutFields.get("table_name");
			String transType = msgOutFields.get("transaction_type");

			// only try to auto-process errors if this is an INSERT message
			if (MetrixStringHelper.isNullOrEmpty(transType) || !transType.equalsIgnoreCase("INSERT"))
				return false;

			// only try to auto-process errors if there is also a DELETE transaction pending for this row
			int deleteCount = MetrixDatabaseManager.getCount("mm_message_out",
					String.format("transaction_type = 'DELETE' and table_name = '%1$s' and metrix_log_id in (select metrix_log_id from %1$s_LOG where metrix_row_id in (select metrix_row_id from %1$s_LOG where metrix_log_id = %2$s))", tableName, metrixLogId));
			if (deleteCount > 0) {
				deleteMessageAndRelatedMessages(clientMessageIn.related_message_id, tableName, String.valueOf(clientMessageIn.message_id), Integer.parseInt(metrixLogId));
				return true;
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}

		return false;
	}

	public static String getErrorMessage(String jsonError) {
		String update_result_expression = "update_[a-z_0-9]{1,}_result";
		String metrix_response_expression = "metrix_response";
		String perform_batch_expression = "perform_batch_result";
		String perform_result_expression = "perform_[a-z_0-9]{1,}_result";
		String matchResult = "";
		String errorMessage = "";

		try {
			if ((MetrixStringHelper.isNullOrEmpty(jsonError) == false && jsonError.contains("perform_batch_result") == false) &&(MetrixStringHelper.doRegularExpressionMatch(update_result_expression, jsonError) || MetrixStringHelper.doRegularExpressionMatch(metrix_response_expression, jsonError) || MetrixStringHelper.doRegularExpressionMatch(perform_result_expression, jsonError))) {
				String updateResult = MetrixStringHelper.getRegularExpressionMatch(update_result_expression, jsonError);
				String metrixResponse = MetrixStringHelper.getRegularExpressionMatch(metrix_response_expression, jsonError);
				String performResponse = MetrixStringHelper.getRegularExpressionMatch(perform_result_expression, jsonError);

				if (!MetrixStringHelper.isNullOrEmpty(updateResult)) {
					matchResult = updateResult;
				}

				if (MetrixStringHelper.isNullOrEmpty(metrixResponse) == false) {
					matchResult = metrixResponse;
				}


				if (MetrixStringHelper.isNullOrEmpty(performResponse) == false) {
					matchResult = performResponse;
				}

				JSONObject jResult = new JSONObject(jsonError);

				JSONObject jSelect = null;
				if(jResult != null)
					jSelect = jResult.getJSONObject(matchResult);

				JSONArray jTables = null;
				if(jSelect != null) {
					jTables = jSelect.names();

					if (jTables != null) {
						for (int m = 0; m < jTables.length(); m++) {
							String tableName = jTables.getString(m);

							JSONObject jTable = jSelect.optJSONObject(tableName);

							if (jTable != null) {
								JSONArray jArrays = jTable.names();

								if (jArrays != null) {
									for (int n = 0; n < jArrays.length(); n++) {
										String jName = jArrays.getString(n);
										if (!MetrixStringHelper.isNullOrEmpty(jName) && jName.compareToIgnoreCase("error") == 0) {
											JSONObject jErrorObject = jTable.optJSONObject(jName);

											JSONArray jErrorInfo;
											if (jErrorObject != null) {
												jErrorInfo = jErrorObject.names();

												if (jErrorInfo != null && jErrorInfo.length() > 0) {
													JSONObject jErrorMessage = jErrorObject.optJSONObject(jErrorInfo.getString(0));
													if (jErrorMessage != null)
														errorMessage = jErrorMessage.getString("message");
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			else if(MetrixStringHelper.doRegularExpressionMatch(perform_batch_expression, jsonError)) {
				JSONObject jResult = new JSONObject(jsonError);
				JSONObject jPerformBatch = jResult.getJSONObject(perform_batch_expression);

				if (jPerformBatch != null) {
					JSONArray jPerforms = jPerformBatch.names();

					for (int m = 0; m < jPerforms.length(); m++) {
						String performName = jPerforms.getString(m);
						JSONObject jPerform = jPerformBatch.optJSONObject(performName);

						if (jPerform != null) {
							JSONArray jArrays = jPerform.names();

							if (jArrays != null) {
								String jName = jArrays.getString(0);

								if(jName.compareToIgnoreCase("result")==0) {
									JSONObject jPerformResult = jPerform.optJSONObject("result");

									if(jPerformResult != null)
									{
										JSONArray jElements = jPerformResult.names();

										if (jElements != null) {
											for (int n = 0; n < jElements.length(); n++) {
												String jElementName = jElements.getString(n);
												if (jElementName.compareToIgnoreCase("error") == 0) {
													JSONObject jErrorObject = jPerformResult.optJSONObject(jElementName);

													JSONArray jErrorInfo = jErrorObject.names();

													if (jErrorInfo.length() > 0) {
														JSONObject jErrorMessage = jErrorObject.optJSONObject(jErrorInfo.getString(0));
														errorMessage = jErrorMessage.getString("message");
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return errorMessage;
	}

	/**
	 * This method returns a list of MetrixTableValues constructed from the data in the 
	 * received jsonMessage.
	 *
	 * @param jsonMessage The message to convert into a list of MetrixTableValues.
	 * @return The list of MetrixTableValues.
	 * @since 5.6
	 */
	public static ArrayList<MetrixTableValues> getTableValuesFromJsonMessage(String jsonMessage){
		ArrayList<MetrixTableValues> tableDataCollection = new ArrayList<MetrixTableValues>();
		String currentMessage = jsonMessage;
		String messageToTest = "";
		String matchResult = "";

		try {
			// check if it is the table_hierarchy_select_result or update_table_result
			currentMessage = currentMessage.replace("\"\"", "null");

			if (currentMessage.contains(getIfsGetPrefix())) {
				int startPos = currentMessage.indexOf(getIfsGetPrefix()) + getIfsGetPrefix().length();
				int endPos = currentMessage.indexOf("_result\"", startPos);
				String table_name = currentMessage.substring(startPos, endPos);
				JSONObject jPerformTest = new JSONObject(currentMessage);
				JSONObject jtest = new JSONObject();
				if(jPerformTest != null)
					jtest.put(table_name + "_hierarchy_select_result", jPerformTest.get(getIfsGetPrefix() + table_name + "_result"));
				currentMessage = jtest.toString();
			}

			// use maximum 100 characters to test what type of
			// message is received to reduce the stack memory usage
			if (currentMessage.length() > 100) {
				messageToTest = currentMessage.substring(0, 100);
			} else {
				messageToTest = currentMessage;
			}

			String hierarchy_select_expression = "[a-z_0-9]{1,}_hierarchy_select_result";
			String update_result_expression = "update_[a-z_0-9]{1,}_result";
			String metrix_response_expression = "metrix_response";

			if (MetrixStringHelper.doRegularExpressionMatch(hierarchy_select_expression, messageToTest) || MetrixStringHelper.doRegularExpressionMatch(update_result_expression, messageToTest)
					|| MetrixStringHelper.doRegularExpressionMatch(metrix_response_expression, messageToTest)) {
				String hierarchySelect = MetrixStringHelper.getRegularExpressionMatch(hierarchy_select_expression, messageToTest);
				String updateResult = MetrixStringHelper.getRegularExpressionMatch(update_result_expression, messageToTest);
				boolean isUpdateResult = (updateResult != null && updateResult != "");

				String metrixResponse = MetrixStringHelper.getRegularExpressionMatch(metrix_response_expression, messageToTest);

				if (isUpdateResult) {
					matchResult = updateResult;
				} else if (hierarchySelect != null && hierarchySelect != "") {
					matchResult = hierarchySelect;
				}

				if (MetrixStringHelper.isNullOrEmpty(metrixResponse) == false) {
					matchResult = metrixResponse;
				}

				JSONObject jResult = new JSONObject(currentMessage);
				// String table_name =
				// hierarchySelect.replace("_hierarchy_select_result",
				// "");
				JSONObject jSelect = null;
				if(jResult != null)
					jSelect = jResult.optJSONObject(matchResult);

				if (jSelect != null) {
					JSONArray jTables = jSelect.names();

					if (jTables != null) {
						for (int m = 0; m < jTables.length(); m++) {
							String tableName = jTables.getString(m);

							JSONObject jTable = jSelect.optJSONObject(tableName);

							if (jTable != null) {
								JSONArray jArrays = jTable.names();

								if (jArrays != null) {
									String jName = jArrays.getString(0);
									if (!MetrixStringHelper.isNullOrEmpty(jName) && jName.compareToIgnoreCase("error") == 0) {
										return null;
										// return false;
									}
								}
								handleJsonTable(jTable, tableName, tableDataCollection, isUpdateResult);
							} else {
								// Try to find jsonArray inside of the
								// jTables
								JSONArray jTableArray = jSelect.optJSONArray(tableName);

								if (jTableArray != null) {
									for (int n = 0; n < jTableArray.length(); n++) {
										JSONObject jArrayTable = jTableArray.optJSONObject(n);

										if (jArrayTable != null) {
											handleJsonTable(jArrayTable, tableName, tableDataCollection, isUpdateResult);
										}
									}
								}
							}
						}
					}
				}
			}

		} catch (JsonParseException ex) {
			LogManager.getInstance().error(ex.getMessage() + " for parsing message:" + jsonMessage);
		} catch (JSONException ex) {
			LogManager.getInstance().error(ex.getMessage() + " for message:" + jsonMessage);
		} catch (Exception ex) {
			LogManager.getInstance().error(ex.getMessage() + " for message:" + jsonMessage);
		}

		return tableDataCollection;
	}

	/**
	 * Generate the array of the sql statements to update the local database for PK updates only.
	 *
	 * @param related_message_id
	 * @param tableDataCollection
	 * @param deleteTransactionLog
	 * @return
	 */
	public static ArrayList<String> buildProcessSqlForInsertPKResolution(String related_message_id, ArrayList<MetrixTableValues> tableDataCollection, boolean deleteTransactionLog, boolean onlyForLog) {
		LogManager.getInstance().debug("Parameters in focus: related_message_id " + related_message_id);

		ArrayList<String> sqlStatements = new ArrayList<String>();
		String openingChunk = "";
		String logOpenChunk = "";
		String origQuery = "";

		for (MetrixTableValues tableData : tableDataCollection) {
			StringBuilder sqlStatement = new StringBuilder();
			openingChunk = "update or ignore " + tableData.tableName + " set ";
			logOpenChunk = "update or ignore " + tableData.tableName + "_LOG set ";
			sqlStatement.append(openingChunk);

			boolean columnAdded = false;

			MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(tableData.tableName);

			if(tableKeys != null) {
				for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
					String columnName = key.getKey();

					if (columnAdded) {
						sqlStatement.append(", ");
					}
					sqlStatement.append(columnName);
					sqlStatement.append("=");

					sqlStatement.append("'");
					sqlStatement.append(tableData.nameValues.get(columnName));
					sqlStatement.append("'");
					columnAdded = true;
				}
			}

			ArrayList<String> keyNameList = MetrixCurrentKeysHelper.getKeyNames(tableData.tableName);
			if (keyNameList != null) {
				if(MetrixStringHelper.isNullOrEmpty(related_message_id))
					continue;

				String transaction_key_name = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "transaction_key_name", "message_id=" + related_message_id);
				for (String keyName : keyNameList) {
					String newValue = "";

					if (tableData.nameValues.containsKey(keyName)) {
						newValue = tableData.nameValues.get(keyName);
						Hashtable<String, String> oldFields = MetrixDatabaseManager.getFieldStringValues("mm_message_out", new String[] { "table_name", "metrix_log_id" }, "message_id=" + related_message_id);
						String tableName = oldFields.get("table_name");
						String logId = oldFields.get("metrix_log_id");

						LogManager.getInstance().debug("Got metrix_log_id " + logId + " and table_name " + tableName + " using mm_message_out.message_id " + related_message_id);

						String oldValue = MetrixDatabaseManager.getFieldStringValue(tableName + "_LOG", keyName, "metrix_log_id=" + logId);
						MetrixCurrentKeysHelper.setKeyValueFromSync(tableData.tableName, keyName, newValue, oldValue);
					}

					if (keyName.compareToIgnoreCase(transaction_key_name) == 0 && MetrixStringHelper.isNullOrEmpty(newValue) == false) {
						ArrayList<DataField> fields = new ArrayList<DataField>();
						fields.add(new DataField("transaction_key_id", newValue));
						MetrixDatabaseManager.updateRow("mm_message_out", fields, "message_id=" + related_message_id);
					}
				}
			}

			sqlStatement.append(" where ");
			boolean columnAdded2 = false;

			if (MetrixStringHelper.isNullOrEmpty(related_message_id) || related_message_id.compareToIgnoreCase("null") == 0) {
				if(tableKeys != null) {
					for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
						String columnName = key.getKey();

						if (columnAdded2) {
							sqlStatement.append(" and ");
						}
						sqlStatement.append(columnName);
						sqlStatement.append("=");

						sqlStatement.append("'");
						sqlStatement.append(tableData.nameValues.get(columnName));
						sqlStatement.append("'");
						columnAdded2 = true;
					}
				}
			} else {
				String row_id = getMetrixRowIdFromMessageId(tableData.tableName, related_message_id);
				sqlStatement.append("metrix_row_id=" + row_id);
			}

			// Manually update both the original row and the _LOG table entry/entries, depending on onlyForLog flag value
			origQuery = sqlStatement.toString();
			if (!onlyForLog)
				sqlStatements.add(origQuery);
			sqlStatements.add(origQuery.replace(openingChunk, logOpenChunk));

			if (deleteTransactionLog) {
				// TODO: Handle the corresponding the transaction_log table
				// the corresponding rows in transactionLog table should be
				// processed or deleted
			}

			// Handle updating mm_attachment_id_map if the current table is ATTACHMENT
			if (MetrixStringHelper.valueIsEqual(tableData.tableName.toUpperCase(), "ATTACHMENT")) {
				String newAttachmentId = tableData.nameValues.get("attachment_id");
				String oldAttachmentId = MetrixDatabaseManager.getFieldStringValue("attachment_LOG", "attachment_id",
						String.format("metrix_log_id in (select metrix_log_id from mm_message_out where table_name = 'attachment' and message_id = %s)", related_message_id));
				sqlStatements.add(String.format("update or ignore mm_attachment_id_map set positive_key = %1$s where negative_key = %2$s", newAttachmentId, oldAttachmentId));
			}
		}

		return sqlStatements;
	}

	/**
	 * Generate the array of the sql statement to update the local database
	 *
	 * @param transactionType
	 * @param related_message_id
	 * @param tableDataCollection
	 * @param deleteTransactionLog
	 * @return
	 */
	public static ArrayList<String> buildProcessSql(MetrixTransactionTypes transactionType, MetrixTransactionTypes originalTransactionType, String related_message_id, ArrayList<MetrixTableValues> tableDataCollection, boolean deleteTransactionLog) {
		LogManager.getInstance().debug("Parameters in focus: transactionType " + transactionType + ", originalTransactionType " + originalTransactionType + ", related_message_id " + related_message_id);

		ArrayList<String> sqlStatements = new ArrayList<String>();

		for (MetrixTableValues tableData : tableDataCollection) {
			if (transactionType != MetrixTransactionTypes.SELECT) {

				StringBuilder sqlStatement = new StringBuilder();

				if (transactionType == MetrixTransactionTypes.INSERT || transactionType == MetrixTransactionTypes.UPDATE) {
					// This is an update message that M5 initiated, client will determine the transaction type
					if (MetrixStringHelper.isNullOrEmpty(related_message_id))
					{
						StringBuilder checkStatement = new StringBuilder();
						boolean columnAdded = false;
						MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(tableData.tableName);

						if(tableKeys != null) {
							for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
								String columnName = key.getKey();

								if (columnAdded) {
									checkStatement.append(" and ");
								}
								checkStatement.append(columnName);
								checkStatement.append("=");

								checkStatement.append("'");
								checkStatement.append(tableData.nameValues.get(columnName));
								checkStatement.append("'");
								columnAdded = true;
							}
						}

						String row_id = MetrixDatabaseManager.getFieldStringValue(tableData.tableName,"metrix_row_id", checkStatement.toString());

						if(MetrixStringHelper.isNullOrEmpty(row_id)) {
							sqlStatement.append("insert or ignore into ");
							transactionType = MetrixTransactionTypes.INSERT;
						}
						else {
							sqlStatement.append("update or ignore ");
							transactionType = MetrixTransactionTypes.UPDATE;
						}
					}
					else {
						sqlStatement.append("update or ignore ");
						transactionType = MetrixTransactionTypes.UPDATE;
					}
				} else if (transactionType == MetrixTransactionTypes.DELETE) {
					sqlStatement.append("delete from ");
				}

				sqlStatement.append(tableData.tableName);

				if (transactionType == MetrixTransactionTypes.INSERT) {

					// rowId = MetrixDatabaseManager
					// .generateRowId(tableData.mTable_name);

					sqlStatement.append("(");
					sqlStatement.append(Global.MetrixRowId);
					for (Map.Entry<String, String> columnData : tableData.nameValues.entrySet()) {
						// ignore the Null value field if there are any
						String column_value = columnData.getValue();

						if (column_value.compareToIgnoreCase("null") != 0) {
							sqlStatement.append(", ");
							sqlStatement.append(columnData.getKey());
						}
					}

					sqlStatement.append(") values (");
					// sqlStatement.append(rowId);
					sqlStatement.append("null"); // use the null for primary key
					// to insert the data
					for (Map.Entry<String, String> columnData : tableData.nameValues.entrySet()) {
						String columnValue = columnData.getValue();

						// ignore the Null value field if there are any
						if (columnValue.compareToIgnoreCase("null") != 0) {
							sqlStatement.append(", ");

                            sqlStatement.append("'");
                            sqlStatement.append(columnValue.replace("'", "''"));
                            sqlStatement.append("'");
						}
					}

					sqlStatement.append(")");
				} else if (transactionType == MetrixTransactionTypes.UPDATE) {
					sqlStatement.append(" set ");
					boolean columnAdded = false;

					for (Map.Entry<String, String> columnData : tableData.nameValues.entrySet()) {
						String columnValue = columnData.getValue();
						String columnName = columnData.getKey();

						if (columnAdded) {
							sqlStatement.append(", ");
						}
						sqlStatement.append(columnName);
						sqlStatement.append("=");

						if(columnValue.equals("null"))
						{
							sqlStatement.append("null");

						}
						else {
							sqlStatement.append("'");
							sqlStatement.append(columnValue.replace("'", "''"));
							sqlStatement.append("'");
						}
						columnAdded = true;
					}

					if (originalTransactionType == MetrixTransactionTypes.INSERT) {
						ArrayList<String> keyNameList = MetrixCurrentKeysHelper.getKeyNames(tableData.tableName);
						if (keyNameList != null) {
							if(MetrixStringHelper.isNullOrEmpty(related_message_id))
								continue;

							String transaction_key_name = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "transaction_key_name", "message_id=" + related_message_id);
							for (String keyName : keyNameList) {
								String newValue = "";

								if (tableData.nameValues.containsKey(keyName)) {
									newValue = tableData.nameValues.get(keyName);
									Hashtable<String, String> oldFields = MetrixDatabaseManager.getFieldStringValues("mm_message_out", new String[] { "table_name", "metrix_log_id" }, "message_id=" + related_message_id);
									String tableName = oldFields.get("table_name");
									String logId = oldFields.get("metrix_log_id");

									LogManager.getInstance().debug("Got metrix_log_id " + logId + " and table_name " + tableName + " using mm_message_out.message_id " + related_message_id);

									String oldValue = MetrixDatabaseManager.getFieldStringValue(tableName + "_LOG", keyName, "metrix_log_id=" + logId);
									MetrixCurrentKeysHelper.setKeyValueFromSync(tableData.tableName, keyName, newValue, oldValue);
								}

								if (keyName.compareToIgnoreCase(transaction_key_name) == 0 && MetrixStringHelper.isNullOrEmpty(newValue) == false) {
									ArrayList<DataField> fields = new ArrayList<DataField>();
									fields.add(new DataField("transaction_key_id", newValue));
									MetrixDatabaseManager.updateRow("mm_message_out", fields, "message_id=" + related_message_id);
								}
							}
						}
					}
				}

				if ((transactionType == MetrixTransactionTypes.UPDATE) || (transactionType == MetrixTransactionTypes.DELETE)) {

					sqlStatement.append(" where ");
					boolean columnAdded = false;

					if (MetrixStringHelper.isNullOrEmpty(related_message_id)) {
						MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(tableData.tableName);

						if(tableKeys != null) {
							for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
								String columnName = key.getKey();

								if (columnAdded) {
									sqlStatement.append(" and ");
								}
								sqlStatement.append(columnName);
								sqlStatement.append("=");
								sqlStatement.append("'");
								sqlStatement.append(tableData.nameValues.get(columnName));
								sqlStatement.append("'");
								columnAdded = true;
							}
						}
					} else {
						String row_id = getMetrixRowIdFromMessageId(tableData.tableName, related_message_id);
						sqlStatement.append("metrix_row_id=" + row_id);
					}
				}
				sqlStatements.add(sqlStatement.toString());

				if (deleteTransactionLog) {
					// TODO: Handle the corresponding the transaction_log table
					// the corresponding rows in transactionLog table should be
					// processed or deleted
				}
			}
		}

		return sqlStatements;
	}

	private static String setNullFields(HashMap<String, TableColumnDef> tableStructure, MetrixTableValues tableData) {
		StringBuilder updateBuilder = new StringBuilder();

		Set<String> nullColumnSet = tableStructure.keySet();
		//nullColumnSet =	tableStructure.keySet();
		nullColumnSet.removeAll(tableData.nameValues.keySet());

		if(nullColumnSet != null && nullColumnSet.size()>0)
			for(String dataColumn : nullColumnSet) {
				TableColumnDef columnDef = tableStructure.get(dataColumn);
				if(columnDef.getColumn_primary_key() == false && columnDef.getColumn_notnull() == false)
					updateBuilder.append(", "+dataColumn+"=null");
			}
		//if(tableData.nameValues.keySet()

		return updateBuilder.toString();
	}

	/**
	 * Acquire the metrix_row_id based on the message_id of the mm_message_out
	 *
	 * @param tableName
	 * @param messageId
	 * @return
	 */
	private static String getMetrixRowIdFromMessageId(String tableName, String messageId) {
		String row_id = "";

		try {
			LogManager.getInstance().debug("Try to find log row for table " + tableName + " using mm_message_out.message_id " + messageId);
			String log_id = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "metrix_log_id", "message_id=" + messageId);
			row_id = MetrixDatabaseManager.getFieldStringValue(tableName + "_LOG", "metrix_row_id", "metrix_log_id=" + log_id);
		} catch (SQLException ex) {
			LogManager.getInstance().error(ex);
			throw ex;
		}
		return row_id;
	}

	/**
	 * Determine if it is primary key of the table
	 *
	 * @param tableName
	 * @param columnName
	 * @return
	 */
	public static boolean isMetrixTablePrimaryKey(String tableName, String columnName) {
		MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(tableName);
		for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
			String keyColumn = key.getKey();

			if (keyColumn.compareToIgnoreCase(columnName) == 0) {
				return true;
			}

		}
		return false;
	}

	/**
	 * Process the JSONObject to acquire all column/value pairs for particular
	 * table
	 *
	 * @param jTable
	 * @param tableName
	 * @param tableCollection
	 * @param isUpdateResult
	 */
	private static void handleJsonTable(JSONObject jTable, String tableName, ArrayList<MetrixTableValues> tableCollection, boolean isUpdateResult) {
		boolean saveAttachmentFile = false;
		boolean savePaymentStatus = false;

		try {
			if (jTable != null) {
				MetrixTableValues tableValues = new MetrixTableValues();

				tableValues.tableName = tableName;

				HashMap<String, MetrixTableStructure> tablesDefinition = MobileApplication.getTableDefinitionsFromCache();
				MetrixTableStructure tableDefinition = tablesDefinition.get(tableName);

				// this table name and column are attachment and length of the
				// attachment value is greater than 0
				if (tableName.compareToIgnoreCase("attachment") == 0) {
					String attachmentCol = jTable.optString("attachment");
					String onDemand = jTable.optString("on_demand");
					String attachmentId = jTable.optString("attachment_id");
					String downloadPath = jTable.optString("download_path");

					// Mobile Service created download_path dynamically for attachment file, if it is empty, client should not download
					if (attachmentCol.length() > 0 && MetrixStringHelper.isNullOrEmpty(downloadPath) == false && downloadPath.equals("null") == false)
						saveAttachmentFile = true;
					if(MetrixStringHelper.isNullOrEmpty(onDemand))
					{
						onDemand = "N";
					}

					if (saveAttachmentFile) {
						try {
							String mobilePath = jTable.optString("mobile_path");
							//String attachmentServiceUrl = jTable.optString("attachment_path");

							if(!mobilePath.toLowerCase().contains("signature")) {
								String serverAttachmentOriginal =jTable.optString("download_path");
								String serverAttachmentPath = MetrixStringHelper.filterJsonMessage(jTable.optString("download_path"));
								if (serverAttachmentOriginal != null && serverAttachmentOriginal.toLowerCase().contains("http"))
									serverAttachmentPath = serverAttachmentOriginal;

								String fileName = jTable.optString("attachment_name");

								if ((Boolean)MetrixPublicCache.instance.getItem("INIT_STARTED")) {
									@SuppressWarnings("unchecked")
									Hashtable<String, String> downloadtCache = (Hashtable<String, String>)MetrixPublicCache.instance.getItem("DOWNLOAD_PATH_ATTACHMENTS");
									if(downloadtCache == null) {
										downloadtCache = new Hashtable<String, String>();
									}

									if(downloadtCache.get(attachmentId) == null){
										downloadtCache.put(attachmentId, downloadPath);
										MetrixPublicCache.instance.addItem("DOWNLOAD_PATH_ATTACHMENTS", downloadtCache);
									}

									Hashtable<String, String> attachmentCache = (Hashtable<String, String>)MetrixPublicCache.instance.getItem("INITIALIZATION_ATTACHMENTS");
									if(attachmentCache == null) {
										attachmentCache = new Hashtable<String, String>();
									}

									if(attachmentCache.get(fileName) == null){
										attachmentCache.put(fileName, serverAttachmentPath);
										MetrixPublicCache.instance.addItem("INITIALIZATION_ATTACHMENTS", attachmentCache);
										if(onDemand.toUpperCase().equals("Y")==false) {
											long downloadId = downloadAttachment(serverAttachmentPath, fileName, attachmentId, false);
											LogManager.getInstance().debug("Metrix attachment Download Id " + downloadId);
										}
									}
								}
								else {
									// do not download attachment if this is an update_attachment_result, since the client already has latest version of the file
									if(onDemand.toUpperCase().equals("Y")==false && !isUpdateResult) {
										long downloadId = downloadAttachment(serverAttachmentPath, fileName, attachmentId, false);
										LogManager.getInstance().debug("Metrix attachment Download Id " + downloadId);
									}
								}
							}
							else {
								String fileName = jTable.optString("attachment_name");
								MetrixFileHelper.deleteFile(fileName);
							}
						} catch (Exception ex) {
							LogManager.getInstance().error(ex);
						}
					}
				}
				else if (tableName.compareToIgnoreCase("metrix_image_view") == 0) {
					try {
						String serverAttachmentPath = MetrixStringHelper.filterJsonMessage(jTable.optString("image_path")).replace("u+0026amp;", "&").replace("u+0026", "&");
						String fileName;
						if (serverAttachmentPath.contains("?"))
							fileName = serverAttachmentPath.substring( serverAttachmentPath.lastIndexOf('/')+1, serverAttachmentPath.indexOf('?'));
						else
							fileName = serverAttachmentPath.substring( serverAttachmentPath.lastIndexOf('/')+1, serverAttachmentPath.length() );
						if(fileName.contains("\\"))
							fileName = fileName.substring(fileName.lastIndexOf('\\') + 1, fileName.length());

						if ((Boolean)MetrixPublicCache.instance.getItem("INIT_STARTED")) {
							@SuppressWarnings("unchecked")
							Hashtable<String, String> attachmentCache = (Hashtable<String, String>)MetrixPublicCache.instance.getItem("INITIALIZATION_ATTACHMENTS");
							if(attachmentCache == null) {
								attachmentCache = new Hashtable<String, String>();
							}

							if(attachmentCache.get(fileName) == null){
								attachmentCache.put(fileName, serverAttachmentPath);
								long downloadId = downloadAttachment(serverAttachmentPath, fileName, "", false);
								LogManager.getInstance().debug("Metrix image view Download Id "+downloadId);
							}
						}
						else {
							long downloadId = downloadAttachment(serverAttachmentPath, fileName, "", false);
							LogManager.getInstance().debug("Metrix image view Download Id "+downloadId);
						}
					} catch (Exception ex) {
						LogManager.getInstance().error(ex);
					}
				}
				else if (tableName.compareToIgnoreCase("metrix_app_params") == 0) {
					String parmNameCol = jTable.optString("param_name");

					if(!MetrixStringHelper.isNullOrEmpty(parmNameCol) && parmNameCol.compareToIgnoreCase("MOBILE_ENCODE_URL_PARAM")==0){
						String colValue = jTable.optString("param_value");

						if(!MetrixStringHelper.isNullOrEmpty(colValue)){
							if(colValue.toLowerCase().contains("y"))
								Global.encodeUrl = true;
							else
								Global.encodeUrl = false;
						}
					}
					else if(!MetrixStringHelper.isNullOrEmpty(parmNameCol) && parmNameCol.compareToIgnoreCase("MOBILE_ENABLE_TIME_ZONE")==0){
						String colValue = jTable.optString("param_value");

						if(!MetrixStringHelper.isNullOrEmpty(colValue)){
							if(colValue.toLowerCase().contains("y"))
								Global.enableTimeZone= true;
							else
								Global.enableTimeZone = false;
						}
					}
				}
				else if (tableName.compareToIgnoreCase("payment") == 0) {
					savePaymentStatus = true;
				}
//				Refer : FSMZ-3829
//				else if (tableName.compareToIgnoreCase("task") == 0) {
//					String status = jTable.optString("status");
//
//					if (status.compareToIgnoreCase("CO") == 0 || status.compareToIgnoreCase("CL") == 0 || status.compareToIgnoreCase("CA") == 0) {
//						String taskId = jTable.optString("task_id");
//
//						String directory = MetrixAttachmentManager.getInstance().getAttachmentPath();
//
//						try {
//							ArrayList<Hashtable<String, String>> attachmentFiles = MetrixDatabaseManager.getFieldStringValuesList("select attachment.attachment_name from attachment join task_attachment on attachment.attachment_id = task_attachment.attachment_id where task_attachment.task_id = " + taskId);
//							if (attachmentFiles != null && attachmentFiles.size() > 0) {
//								for (Hashtable<String, String> attachmentFile : attachmentFiles) {
//									String fileName = attachmentFile.get("attachment_name");
//									File attachment = new File(directory + "/" + fileName);
//									if (attachment.exists() && !attachment.isDirectory()) {
//										attachment.delete();
//									}
//								}
//							}
//						} catch (Exception ex) {
//							LogManager.getInstance().error(ex);
//						}
//					}
//				}

				if (tableDefinition != null) {
					for (Map.Entry<String, TableColumnDef> column : tableDefinition.mColumns.entrySet()) {
						String name = column.getKey();

						if(jTable.has(name)==false)
							continue;

						String value = "";
						//
						if (saveAttachmentFile && name.compareToIgnoreCase("attachment") == 0) {
							continue;
						}
						else if (tableName.compareToIgnoreCase("attachment") == 0 && name.compareToIgnoreCase("mobile_path") == 0) {
							String mobilePath = jTable.optString("mobile_path");
							String fileName;

							if(!MetrixStringHelper.isNullOrEmpty(mobilePath) && mobilePath.compareToIgnoreCase("null") != 0) {
								fileName = mobilePath.substring(mobilePath.lastIndexOf("/") + 1);
								if(fileName.contains("\\"))
									fileName = fileName.substring(fileName.lastIndexOf('\\') + 1, fileName.length());
							} else {
								String serverAttachmentPath = MetrixStringHelper.filterJsonMessage(jTable.optString("attachment_path"));
								fileName = serverAttachmentPath.substring(serverAttachmentPath.lastIndexOf('/') + 1, serverAttachmentPath.length());
								if(fileName.contains("\\"))
									fileName = fileName.substring(fileName.lastIndexOf('\\') + 1, fileName.length());
							}

							value = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + fileName;
						}
						else if(savePaymentStatus){
//							if(name.compareToIgnoreCase("payment_id")!=0 && name.compareToIgnoreCase("payment_status")!=0 ){
//								continue;
//							}
//							else
							if(jTable.isNull(name))
								value = "null";
							else
								value = jTable.optString(name);

						}
						else {
							if(jTable.isNull(name))
								value = "null";
							else
								value = jTable.optString(name);
						}

						if (!MetrixStringHelper.isNullOrEmpty(value) && value.compareToIgnoreCase("null") != 0) {

							value = value.replace("u+0027", "'").replace("chr(13)", "\r").replace("chr(10)", "\n").replace("chr(9)", "\t");
							value = value.replace("u+0022", "\"").replace("u+005c", "\\");
							value = value.replace("&gt;",">").replace("&lt;","<").replace("&amp;","&");
							tableValues.nameValues.put(name, value);
						}
						else if(value.isEmpty()) {
							tableValues.nameValues.put(name, "");
						}
						else if(value.equalsIgnoreCase("null")) {
							tableValues.nameValues.put(name, "null");
						}
					}
					/*
					 * JSONArray jNames = jTable.names();
					 * 
					 * for (int k = 0; k < jNames.length(); k++) { String
					 * columnName = jNames.getString(k); String value =
					 * jTable.optString(columnName);
					 * 
					 * // filter out the metrix_row_num field generated by M5 if
					 * (columnName.compareToIgnoreCase("metrix_row_num") != 0) {
					 * tableValues.mName_values.put(columnName, value); } }
					 */
					tableCollection.add(tableValues);
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	public static boolean currentTransactionMessagesCompleted(String transactionKeyId, String transactionKeyName) {
		boolean messageCompleted = true;

		try
		{
			long currentTransactionId = MetrixUpdateManager.getTransactionId(transactionKeyId, transactionKeyName);

			ArrayList<Hashtable<String, String>> messageList = MetrixDatabaseManager.getFieldStringValuesList("mm_message_out", new String[] { "message_id", "status", "table_name" }, "transaction_id = " + currentTransactionId);

			if (messageList != null && messageList.size() > 0)
				messageCompleted = false;
		}
		catch(Exception ex)
		{
			messageCompleted = false;
		}

		return messageCompleted;
	}

	public static long downloadAttachment(String serverAttachmentPath, String fileName, String attachmentId, boolean isDatabase){
		String attachmentUrl = "";
		String localDownloadPath = "";
		String downloadedFileName = "";
		Context mCtxt = MetrixPublicCache.instance.getApplicationContext();

		if(serverAttachmentPath.toLowerCase().startsWith("http")) {
			try {
				attachmentUrl = serverAttachmentPath.replace("u+0026amp;", "&").replace("u+0026", "&").replace("\\", "/");
			}
			catch(Exception ex){
				LogManager.getInstance().error(ex);
			}
		}
		else {
			attachmentUrl = getDownloadUrl(attachmentId, fileName);
		}

		try {
			if (serverAttachmentPath.contains("?"))
			{
				String attachmentUrlLink = serverAttachmentPath.split("\\?", 2)[0];
				downloadedFileName = attachmentUrlLink.substring(attachmentUrlLink.lastIndexOf('/') + 1, attachmentUrlLink.length());
			}
			else
				downloadedFileName = attachmentUrl.substring( attachmentUrl.lastIndexOf('/')+1, attachmentUrl.length() );

			// This allows a file with non-ASCII characters in its name to match attachment_name in the DB and be accessed successfully.
			downloadedFileName = java.net.URLDecoder.decode(downloadedFileName, "UTF-8");
			localDownloadPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + downloadedFileName;

			if(isDatabase == false) {
				File localFile = new File(localDownloadPath);
				if(localFile.exists())
					localFile.delete();
			}
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
		Uri uriPath = Uri.fromFile(new File(localDownloadPath));
		//MetrixAttachmentManager.getInstance().saveAttachmentToFile(attachmentCol, attachmentPath, true);
		DownloadManager dm = (DownloadManager)mCtxt.getSystemService(Context.DOWNLOAD_SERVICE);

		Request request = new Request(Uri.parse(attachmentUrl));
		//use headers for authorize user
		request.addRequestHeader("token_id", SettingsHelper.getTokenId(MobileApplication.getAppContext()));
		request.addRequestHeader("session_id", SettingsHelper.getSessionId(MobileApplication.getAppContext()));
		String personID = MetrixStringHelper.encodeBase64ToUrlSafe(SettingsHelper.getActivatedUser(MobileApplication.getAppContext()));
		request.addRequestHeader("person_id", personID);
		int deviceSequence = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
		request.addRequestHeader("device_sequence", String.valueOf(deviceSequence));
		request.setDestinationUri(uriPath);

		long enqueue = dm.enqueue(request);
		return enqueue;
	}

	public static long downloadAttachmentWithListener(String serverAttachmentPath, String fileName, String attachmentId) {
		// Call to this method will return via MetrixAttachmentReceiver, which sets up MetrixPublicCache items to be handled in the originating screen.

		long enqueue = 0;

		try {

			String attachmentUrl = "";
			if (serverAttachmentPath.toLowerCase().startsWith("http")) {
				attachmentUrl = serverAttachmentPath.replace("u+0026amp;", "&").replace("u+0026", "&").replace("\\", "/");
			} else {
				attachmentUrl = getDownloadUrl(attachmentId, fileName);
			}

			String downloadedFileName = "";
			if (serverAttachmentPath.contains("?")) {
				String attachmentUrlLink = serverAttachmentPath.split("\\?", 2)[0];
				downloadedFileName = attachmentUrlLink.substring(attachmentUrlLink.lastIndexOf('/') + 1, attachmentUrlLink.length());
			} else {
				downloadedFileName = attachmentUrl.substring(attachmentUrl.lastIndexOf('/') + 1, attachmentUrl.length());
			}
			// This allows a file with non-ASCII characters in its name to match attachment_name in the DB and be accessed successfully.
			downloadedFileName = java.net.URLDecoder.decode(downloadedFileName, "UTF-8");

			String localDownloadPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + downloadedFileName;
			File localFile = new File(localDownloadPath);
			if (localFile.exists())
				localFile.delete();

			// Setup download file.
			Uri uriPath = Uri.fromFile(new File(localDownloadPath));
			String personID = MetrixStringHelper.encodeBase64ToUrlSafe(SettingsHelper.getActivatedUser(MobileApplication.getAppContext()));
			int deviceSequence = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(attachmentUrl));
			//use headers for authorize user
			request.addRequestHeader("token_id", SettingsHelper.getTokenId(MobileApplication.getAppContext()));
			request.addRequestHeader("session_id", SettingsHelper.getSessionId(MobileApplication.getAppContext()));
			request.addRequestHeader("person_id", personID);
			request.addRequestHeader("device_sequence", String.valueOf(deviceSequence));
			request.setDestinationUri(uriPath);
			//sets notification when download completed
			request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			request.setDescription(fileName);
			request.setTitle(attachmentId);

			Context mCtxt = MetrixPublicCache.instance.getApplicationContext();

			DownloadManager dm = (DownloadManager) mCtxt.getSystemService(Context.DOWNLOAD_SERVICE);
			enqueue = dm.enqueue(request);

		} catch (IllegalArgumentException e) {
			LogManager.getInstance().error(e);
		} catch(Exception ex){
			LogManager.getInstance().error(ex);
		}

		return enqueue;
	}

	public static void removeAttachmentMessages(MmMessageOut message) {
		boolean exceptionThrowed = false;

		if(message == null)
			return;

		try {
			String attachmentId = MetrixDatabaseManager.getFieldStringValue("attachment_log",
					"attachment_id", "metrix_log_id="+ message.metrix_log_id);

			if(MetrixStringHelper.isNullOrEmpty(attachmentId))
				return;

			MetrixDatabaseManager.begintransaction();
			MetrixDatabaseManager.deleteRow("attachment", "attachment_id="+attachmentId);
			MetrixDatabaseManager.deleteRow("attachment_log", "attachment_id="+attachmentId);
			MessageHandler.deleteMessage("mm_message_out", message.message_id);

            HashMap<String, MetrixTableStructure> tablesDefinition = MobileApplication.getTableDefinitionsFromCache();
            ArrayList<String> attachmentTableTypeList = new ArrayList<>();

            for (Map.Entry<String, MetrixTableStructure> e : tablesDefinition.entrySet()) {
                if (e.getKey().toLowerCase().endsWith("_attachment")) {
					// FSM has the naming convention for attachment related tables such as task_attachment, quote_attachment, request_attachment etc
                	// added tables related to attachment function, such as task_attachment, quote_attachment etc to the list.
                    attachmentTableTypeList.add(e.getKey().toLowerCase());
                }
            }

            for(String attachmentFunctionTableName : attachmentTableTypeList) {
            	String attachmentFunctionLogTableName = attachmentFunctionTableName+"_log";
            	// return all log rows based on attachment_id that is related to a particular function such as task_attachment
				ArrayList<Hashtable<String, String>> functionAttachmentLogIds = MetrixDatabaseManager.getFieldStringValuesList(attachmentFunctionLogTableName, new String[]{"metrix_log_id", "metrix_row_id"}, "attachment_id=" + attachmentId);

				if(functionAttachmentLogIds != null && functionAttachmentLogIds.size()>0) {
					for (Hashtable<String, String> functionAttachmentLogId : functionAttachmentLogIds) {
						String logId = functionAttachmentLogId.get("metrix_log_id");
						String rowId = functionAttachmentLogId.get("metrix_row_id");

						MetrixDatabaseManager.deleteRow(attachmentFunctionTableName, "metrix_row_id=" + rowId);
						MetrixDatabaseManager.deleteRow(attachmentFunctionLogTableName, "metrix_log_id=" + logId);

						String msgId = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "message_id", String.format("metrix_log_id=%s and table_name='%s'", logId, attachmentFunctionTableName));
						if (!MetrixStringHelper.isNullOrEmpty(msgId))
							MessageHandler.deleteMessage("mm_message_out", Long.parseLong(msgId));
					}
				}
			}
		} catch (Exception ex) {
			exceptionThrowed = true;
			LogManager.getInstance().error(ex);
		} finally {
			if (exceptionThrowed == false) {
				MetrixDatabaseManager.setTransactionSuccessful();
			}
			MetrixDatabaseManager.endTransaction();
		}
	}

	public static String getDownloadUrl(String attachmentId, String fileName) {
		Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
		String cloudDownload = SettingsHelper.getStringSetting(mCtxt, SettingsHelper.IS_AZURE);

		if (!MetrixStringHelper.isNullOrEmpty(cloudDownload) && cloudDownload.toUpperCase().equals("Y"))
		{
			if (!MetrixStringHelper.isNullOrEmpty(attachmentId)) {
				String blobStorage = SettingsHelper.getStringSetting(mCtxt, SettingsHelper.AZURE_ATTACHMENT_STORAGE_BASE);
				String attachmentUrl = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_path", "attachment_id=" + attachmentId);

				if (!MetrixStringHelper.isNullOrEmpty(blobStorage))
				 {
					if(!MetrixStringHelper.isNullOrEmpty(attachmentUrl)){
						try {
							attachmentUrl = java.net.URLDecoder.decode(attachmentUrl, "UTF-8").replace("\\", "/");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}

						if(attachmentUrl.contains("?"))
							attachmentUrl.substring(0, attachmentUrl.indexOf("?"));

						fileName = attachmentUrl.substring(attachmentUrl.lastIndexOf('/')+1, attachmentUrl.length());
						String encodedFileName = MetrixStringHelper.escapeDataString(fileName);
						attachmentUrl = String.format("%1$s%2$s", blobStorage, encodedFileName);
					}
					return  attachmentUrl;
				}
			}
		}
		else
		{
		    // if the environment is not a cloud environment, we will use unzipped file from now.
			String attachmentUrl = "";
			String serviceUrl = SettingsHelper.getServiceAddress(mCtxt);
			if (serviceUrl.endsWith("/")){
				attachmentUrl = serviceUrl + "download/" + MetrixStringHelper.escapeDataString(fileName);
			}else{
				attachmentUrl = serviceUrl + "/download/" + MetrixStringHelper.escapeDataString(fileName);
			}
			return attachmentUrl;
		}

		return "";
	}

	public static String getIfsGetPrefix() {
		return IFS_GET_PREFIX;
	}
}
