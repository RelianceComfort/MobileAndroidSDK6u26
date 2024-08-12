package com.metrix.architecture.utilities;

public class Global {
	public static String MobileApplication = "MOBILEAPPLICATION";
	public static String MetrixLogId = "metrix_log_id";
	public static String MetrixRowId = "metrix_row_id";
	public static final String ATTACHMENT_TEMP_EXTENSION = ".tmp";
	public static boolean encodeUrl = false;
	public static boolean enableTimeZone = false;
	
	public enum MessageStatus {
		WAITING, READY, SENT, ERROR, WAITING_PREVIOUS
	}
	
	public enum MessageInStatus {
		LOADED, PROCESSED, ERROR
	}	
	
	public enum MessageType {
		Messages, AllReceipts, CreateReceipt, Initialization, InitRelatedMessages, AllMessages, CreateMessage;
	}
	
	public enum ActivityType {
		Upload, Download, Information, InitializationStarted, InitializationEnded, PasswordChangedFromServer;
	}	
	
	public enum UploadType {
		MESSAGE_SEQUENCE, TRANSACTION_INDEPENDENT, RELATIONSHIP_KEY;
	}
	
	public enum AttachmentType {
		Jpg, Jpeg, GIF, BMP, PNG,
		Doc, Docx, Csv, Xls, Xlsx, Ppt, Pptx,
		Txt, Log, Pdf, mp4, mp3, Wav, Htm, Html
	}
	
	public enum HyperlinkType {
		map
	}
	
	public enum ConnectionStatus {
		Connected, Disconnected, Unknown, Pause
	}	
	
	public enum ComparisonOperator {
		Greater,
		GreaterEqual,
		Less,
		LessEqual,
		Equal
	}
	
	public enum PlaceAddressType{
		
		DEFAULT("DEFAULT"),
		ALTERNATE("ALTERNATE"),
		BILL_TO("BILL TO"),
		CONTACT("CONTACT"),
		EMERGENCY("EMERGENCY"),
		SHIP_TO("SHIP_TO");
		
		private String value;
		
		PlaceAddressType(String value){
			this.value = value;
		}
		
		@Override
		public String toString(){
			return this.value.toString();
		}
	}
}

