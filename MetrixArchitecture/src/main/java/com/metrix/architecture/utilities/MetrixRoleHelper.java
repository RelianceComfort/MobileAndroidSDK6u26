package com.metrix.architecture.utilities;

import java.util.ArrayList;
import java.util.Hashtable;

import com.metrix.architecture.database.MetrixDatabaseManager;

public class MetrixRoleHelper {
	public static ArrayList<String> getRoles(String personId)
	{
		ArrayList<Hashtable<String, String>> roleResults = MetrixDatabaseManager.getFieldStringValuesList("person_role", new String[]{"user_role"}, "person_id="+ personId);	    
	    ArrayList<String> roleNames = new ArrayList<String>();
	    
	    for(Hashtable<String, String>row : roleResults) {
	        roleNames.add(row.get("user_role"));
	    }
	    return roleNames;
	}
	
	public static ArrayList<String> getRoles(String personId, String filter)
	{
		String roleFilter = new String();
	    if(MetrixStringHelper.isNullOrEmpty(filter)){
	        roleFilter = "person_id='"+personId+"'";
	    }
	    else {
	        roleFilter = "person_id='"+personId+"' and "+ filter;
	    }		
		
		ArrayList<Hashtable<String, String>> roleResults = MetrixDatabaseManager.getFieldStringValuesList("person_role", new String[]{"user_role"}, roleFilter);	    
	    ArrayList<String> roleNames = new ArrayList<String>();
	    	    
	    if(roleResults == null)
	    	return null;
	    
	    for(Hashtable<String, String>row : roleResults) {
	        roleNames.add(row.get("user_role"));
	    }
	    return roleNames;
	}
		

	public static ArrayList<Hashtable<String, String>> getFunctions(String roleName)
	{
		ArrayList<Hashtable<String, String>> functionResults = MetrixDatabaseManager.getFieldStringValuesList("role_function_map", new String[]{"function_id", "disabled"}, "user_role='"+ roleName+"'");
	    
	    return functionResults;
	}

	public static Hashtable<String, String> getFunctions(String roleName, String functionName)
	{
		String gpsFunctionName=functionName.toUpperCase();
		
		ArrayList<Hashtable<String, String>> functionResults = MetrixDatabaseManager.getFieldStringValuesList("role_function_map", new String[]{"function_id", "disabled"}, "user_role='"+ roleName+"' and function_id='"+gpsFunctionName+"'");
		Hashtable<String, String> row = new Hashtable<String, String>();
		if(functionResults!=null && functionResults.size()>0)
			return functionResults.get(0);
		else
			return row;
	}
	
	// GPS function naming convention is GPS_TABLENAME
	public static boolean isGPSFunctionEnabled(String functionName)
	{
	    boolean functionEnabled = true;	    
	    ArrayList<String> roles = MetrixRoleHelper.getRoles(User.getUser().personId, "");
	    
	    if(roles == null)
	    	return true;
	    
	    for(String roleName : roles){
	    	Hashtable<String, String> rowFunction = MetrixRoleHelper.getFunctions(roleName, functionName);
	        
	        if(rowFunction.isEmpty())
	            continue;
	        else {
	            String disabled = rowFunction.get("disabled");
	            
	            if(disabled.compareToIgnoreCase("Y")==0) {
	                functionEnabled = false;	                
	            }
	            else {
	                functionEnabled = true;
	                break;
	            }
	        }
	    }
	    
	    return functionEnabled;
	}
}
