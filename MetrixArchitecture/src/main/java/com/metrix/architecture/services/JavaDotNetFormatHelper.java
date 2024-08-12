package com.metrix.architecture.services;

import java.util.HashMap;

import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * Convert differences of the data format between .NET and Java 
 * @author elin
 *
 */
public class JavaDotNetFormatHelper {
	static HashMap<String, String> timeZoneTable;
	
	// The following is the dictionary map between the .NET Time Zone ID and Java Time Zone ID
	// The .NET only maps to the JAVA 's Time Zone ID with REGION 001
	static {
		timeZoneTable = new HashMap<String, String>();
		timeZoneTable.put("AUS Central Standard Time", "Australia/Darwin");
		timeZoneTable.put("AUS Eastern Standard Time", "Australia/Sydney");
		timeZoneTable.put("Afghanistan Standard Time", "Asia/Kabul");
		timeZoneTable.put("Alaskan Standard Time", "America/Anchorage");
		timeZoneTable.put("Arab Standard Time", "Asia/Riyadh");
		timeZoneTable.put("Argentina Standard Time", "America/Buenos_Aires");
		timeZoneTable.put("Atlantic Standard Time", "America/Halifax");
		timeZoneTable.put("Azerbaijan Standard Time", "Asia/Baku");
		timeZoneTable.put("Azores Standard Time", "Atlantic/Azores");
		timeZoneTable.put("Bahia Standard Time", "America/Bahia");
		timeZoneTable.put("Bangladesh Standard Time", "Asia/Dhaka");
		timeZoneTable.put("Canada Central Standard Time", "America/Regina");
		timeZoneTable.put("Cape Verde Standard Time", "Atlantic/Cape_Verde");
		timeZoneTable.put("Caucasus Standard Time", "Asia/Yerevan");
		timeZoneTable.put("Cen. Australia Standard Time", "Australia/Adelaide");
		timeZoneTable.put("Central America Standard Time", "America/Guatemala");
		timeZoneTable.put("Central Asia Standard Time", "Asia/Almaty");
		timeZoneTable.put("Central Brazilian Standard Time", "America/Cuiaba");
		timeZoneTable.put("Central Europe Standard Time", "Europe/Budapest");
		timeZoneTable.put("Central Pacific Standard Time", "Pacific/Guadalcanal");
		timeZoneTable.put("Central Standard Time", "America/Chicago");
		timeZoneTable.put("Central Standard Time (Mexico)", "America/Mexico_City");
		timeZoneTable.put("China Standard Time", "Asia/Shanghai");
		timeZoneTable.put("Dateline Standard Time", "Etc/GMT+12");
		timeZoneTable.put("E. Africa Standard Time", "Africa/Nairobi");
		timeZoneTable.put("E. Australia Standard Time", "Australia/Brisbane");
		timeZoneTable.put("E. Europe Standard Time", "Asia/Nicosia");
		timeZoneTable.put("E. South America Standard Time", "America/Sao_Paulo");
		timeZoneTable.put("Eastern Standard Time", "America/New_York");
		timeZoneTable.put("Egypt Standard Time", "Africa/Cairo");
		timeZoneTable.put("Ekaterinburg Standard Time", "Asia/Yekaterinburg");
		timeZoneTable.put("FLE Standard Time", "Europe/Kiev");
		timeZoneTable.put("Fiji Standard Time", "Pacific/Fiji");
		timeZoneTable.put("GMT Standard Time", "Europe/London");
		timeZoneTable.put("GTB Standard Time", "Europe/Athens");
		timeZoneTable.put("Georgian Standard Time", "Asia/Tbilisi");
		timeZoneTable.put("Greenland Standard Time", "America/Godthab");
		timeZoneTable.put("Greenwich Standard Time", "Atlantic/Reykjavik");
		timeZoneTable.put("Hawaiian Standard Time", "Pacific/Honolulu");
		timeZoneTable.put("India Standard Time", "Asia/Calcutta");
		timeZoneTable.put("Iran Standard Time", "Asia/Tehran");
		timeZoneTable.put("Israel Standard Time", "Asia/Jerusalem");
		timeZoneTable.put("Jordan Standard Time", "Asia/Amman");
		timeZoneTable.put("Kaliningrad Standard Time", "Europe/Kaliningrad");
		timeZoneTable.put("Korea Standard Time", "Asia/Seoul");
		timeZoneTable.put("Magadan Standard Time", "Asia/Magadan");
		timeZoneTable.put("Mauritius Standard Time", "Indian/Mauritius");
		timeZoneTable.put("Middle East Standard Time", "Asia/Beirut");
		timeZoneTable.put("Montevideo Standard Time", "America/Montevideo");
		timeZoneTable.put("Morocco Standard Time", "Africa/Casablanca");
		timeZoneTable.put("Mountain Standard Time", "America/Denver");
		timeZoneTable.put("Mountain Standard Time (Mexico)", "America/Chihuahua");
		timeZoneTable.put("Myanmar Standard Time", "Asia/Rangoon");
		timeZoneTable.put("N. Central Asia Standard Time", "Asia/Novosibirsk");
		timeZoneTable.put("Namibia Standard Time", "Africa/Windhoek");
		timeZoneTable.put("Nepal Standard Time", "Asia/Katmandu");
		timeZoneTable.put("New Zealand Standard Time", "Pacific/Auckland");
		timeZoneTable.put("Newfoundland Standard Time", "America/St_Johns");
		timeZoneTable.put("North Asia East Standard Time", "Asia/Irkutsk");
		timeZoneTable.put("North Asia Standard Time", "Asia/Krasnoyarsk");
		timeZoneTable.put("Pacific SA Standard Time", "America/Santiago");
		timeZoneTable.put("Pacific Standard Time", "America/Los_Angeles");
		timeZoneTable.put("Pacific Standard Time (Mexico)", "America/Santa_Isabel");
		timeZoneTable.put("Pakistan Standard Time", "Asia/Karachi");
		timeZoneTable.put("Paraguay Standard Time", "America/Asuncion");
		timeZoneTable.put("Romance Standard Time", "Europe/Paris");
		timeZoneTable.put("Russian Standard Time", "Europe/Moscow");
		timeZoneTable.put("SA Eastern Standard Time", "America/Cayenne");
		timeZoneTable.put("SA Pacific Standard Time", "America/Bogota");
		timeZoneTable.put("SA Western Standard Time", "America/La_Paz");
		timeZoneTable.put("SE Asia Standard Time", "Asia/Bangkok");
		timeZoneTable.put("Samoa Standard Time", "Pacific/Apia");
		timeZoneTable.put("Singapore Standard Time", "Asia/Singapore");
		timeZoneTable.put("South Africa Standard Time", "Africa/Johannesburg");
		timeZoneTable.put("Syria Standard Time", "Asia/Damascus");
		timeZoneTable.put("Taipei Standard Time", "Asia/Taipei");
		timeZoneTable.put("Tasmania Standard Time", "Australia/Hobart");
		timeZoneTable.put("Tokyo Standard Time", "Asia/Tokyo");
		timeZoneTable.put("Tonga Standard Time", "Pacific/Tongatapu");
		timeZoneTable.put("Turkey Standard Time", "Europe/Istanbul");
		timeZoneTable.put("US Eastern Standard Time", "America/Indianapolis");
		timeZoneTable.put("US Mountain Standard Time", "America/Phoenix");
		timeZoneTable.put("UTC", "Etc/GMT");
		timeZoneTable.put("UTC+12", "Etc/GMT-12");
		timeZoneTable.put("UTC-02", "Etc/GMT+2");
		timeZoneTable.put("UTC-11", "Etc/GMT+11");
		timeZoneTable.put("Ulaanbaatar Standard Time", "Asia/Ulaanbaatar");
		timeZoneTable.put("Venezuela Standard Time", "America/Caracas");
		timeZoneTable.put("Vladivostok Standard Time", "Asia/Vladivostok");
		timeZoneTable.put("W. Australia Standard Time", "Australia/Perth");
		timeZoneTable.put("W. Central Africa Standard Time", "Africa/Lagos");
		timeZoneTable.put("W. Europe Standard Time", "Europe/Berlin");
		timeZoneTable.put("West Asia Standard Time", "Asia/Tashkent");
		timeZoneTable.put("West Pacific Standard Time", "Pacific/Port_Moresby");
		timeZoneTable.put("Yakutsk Standard Time", "Asia/Yakutsk");
	}
	
	/**
	 * Convert the datetime format from .NET to Java
	 * @param inFormat
	 * @return
	 */
	public static String convertDateTimeFormatFromDotNet(String inFormat){
		if(MetrixStringHelper.isNullOrEmpty(inFormat))
			return inFormat;
			
		char[] stringArray;
				
		stringArray = inFormat.toCharArray();
		
		for(int index=0; index < stringArray.length; index++){
			switch(stringArray[index]){
				case 't':
					stringArray[index]= 'a'; 
				break;
				case 'F':
					stringArray[index]= 'S';
					break;
				default:					
				break;
			}
		}
		
		String outFormat = new String(stringArray);
		
		return outFormat;
	}
	
	/**
	 * Return the java Time Zone ID based on the .NET Time Zone ID
	 * @param inZone
	 * @return
	 */
	public static String convertTimeZoneIdFromDotNet(String inZone){
		String outZone = inZone;			
		
		if(timeZoneTable.containsKey(inZone))
			outZone = timeZoneTable.get(inZone);
		
		return outZone;
	}
}
