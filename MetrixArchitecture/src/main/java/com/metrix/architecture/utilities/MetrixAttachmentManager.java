package com.metrix.architecture.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.widget.Toast;

/**
 * @author edlius
 *
 */
public class MetrixAttachmentManager {
	private static MetrixAttachmentManager mInstance = null;
	
	public static synchronized MetrixAttachmentManager getInstance(){
		if(mInstance == null){
			mInstance = new MetrixAttachmentManager();
		}
		
		return mInstance;
	}
	
	public MetrixAttachmentManager(){
	}
	
	public void downloadAttachment(String fileName) {
		String attachmentPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + fileName;
		Uri uriPath = Uri.fromFile(new File(attachmentPath));
		
		Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
		DownloadManager dm = (DownloadManager)mCtxt.getSystemService(Context.DOWNLOAD_SERVICE);
		
		String zippedFileName = MetrixFileHelper.getZippedFileName(fileName);
		String serviceUrl = SettingsHelper.getServiceAddress(mCtxt);
		String attachmentUrl = serviceUrl+"/attachments/"+SettingsHelper.getDeviceSequence(mCtxt)+"/"+zippedFileName;
		
		Request request = new Request(Uri.parse(attachmentUrl));
		request.setDestinationUri(uriPath);	
		
		long enqueue = dm.enqueue(request);
		LogManager.getInstance().debug("Metrix Mobile Attachment download_Id: " + enqueue);
	}
	
	public String getAttachmentPath(){
		String path = "";
	    try {
	        String state = Environment.getExternalStorageState();	        
	        
	        if( state.equals(Environment.MEDIA_MOUNTED) ) {
				Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
				File dir = new File( mCtxt.getExternalFilesDir(null), "attachments");
				if( !dir.exists() ) {
	                dir.mkdir();	                
	            }	            
	            
	            path = dir.getPath();
	        } else {
	            LogManager.getInstance().error("The disk drive for this application is not mounted!");
	        }
	    } catch(Exception e ) {
	    	LogManager.getInstance().error(e);
	    }

	    return path;
	}
	
	/**
	 * Decode a encoded 64 attachment string to binary bytes array and save to the file 
	 * @param attachment
	 * @param fileName
	 * @param overwrite
	 */
	public void saveAttachmentToFile(String attachment, String fileName, boolean overwrite){
		File attachmentFile = new File(fileName);
		
		if(attachmentFile.exists()&& overwrite == false){
			return;
		}
		
		byte[] data = MetrixStringHelper.decodeBase64(attachment);
		
		try {
			OutputStream out = new FileOutputStream(fileName);
			out.write(data);
			out.close();		
		}
		catch(IOException ex){
			LogManager.getInstance().error(ex);
		}
	}	
	
	public void deleteAttachmentFiles(String dir){
		File attachmentDir = new File(dir);
		
		if(attachmentDir.exists()==false || attachmentDir.isDirectory()==false){
			return;
		}		
		
		try {
			for(File attachment: attachmentDir.listFiles()){
				attachment.delete();
			}
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
	}
	
	public void deleteAllDesignerImageFiles(String dir) {
		File attachmentDir = new File(dir);	
		if (attachmentDir.exists() == false || attachmentDir.isDirectory() == false) {
			return;
		}		
		
		String mobileImageQuery = "select distinct image_id from metrix_image_view where image_category = 'MOBILE'";
		ArrayList<Hashtable<String, String>> imageIDQueryResult = MetrixDatabaseManager.getFieldStringValuesList(mobileImageQuery);
		if (imageIDQueryResult == null)
			return;
		
		ArrayList<String> mobileImageIDs = new ArrayList<String>();
		for (Hashtable<String, String> resultItem : imageIDQueryResult) {
			String imageID = resultItem.get("image_id");
			if (!MetrixStringHelper.isNullOrEmpty(imageID)) {
				mobileImageIDs.add(imageID);
			}
		}
		
		try {
			for (File candidateFile : attachmentDir.listFiles()) {
				if (mobileImageIDs.contains(candidateFile.getName()))
					candidateFile.delete();
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}
	
	/**
	 * This method allows you to open a PDF file located at the file path identified.
	 * @param context The application context.
	 * @param filePath The file path to the PDF file.
	 * @since 5.6
	 */
	public void openPDF(Context context, String filePath){
		File attachmentFile = new File(filePath);
		if(attachmentFile.exists()){
			Intent intent = new Intent();
			
			if(filePath.toLowerCase().contains(".pdf")) {                
                try {
					File file = new File(filePath);					
					intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(file), "application/pdf");
					intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					//intent.setDataAndType(Uri.parse(filePath), "application/pdf");							
					context.startActivity(intent);
				} catch (Exception e) {
					Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationPDF"), Toast.LENGTH_SHORT).show();
				}                                               
            }
		}
	}
	
	/**
	 * Save an attachment string to the file with original format (Most likely string is encoded 64 and saved as encoded 64)
	 * @param attachmentString
	 * @param fileName
	 * @since 5.6
	 */
	public void saveAttachmentStringToFile(String attachmentString, String fileName) {
		BufferedWriter writer = null;
		
		try {
			writer = new BufferedWriter(new FileWriter(fileName));		    
			
			try {
				writer.write(attachmentString);				
		    } catch (IOException e) {
		    	LogManager.getInstance().info("MetrixAttachmentManager.saveAttachmentStringToFile: "+e.getMessage());
		    }		    
		} catch (Exception e) {
			LogManager.getInstance().info("MetrixAttachmentManager.saveAttachmentStringToFile: I/O Error");
		}
		finally
		{
			try
			{
				if (writer != null)
					writer.close( );
			}
			catch ( IOException e)
			{
			}
		}
	}
		
	/**
	 * Gets a string representation of a file found at the path identified by the file name.
	 * @param fileName The path to the file to read.
	 * @return The string contents of the file.
	 * @since 5.6
	 */
	public String getAttachmentStringFromFile(String fileName) {
		StringBuilder total = new StringBuilder();
		
		try {
			String line;
			BufferedReader br = new BufferedReader(new FileReader(fileName));		    
			
			try {
				while ((line = br.readLine()) != null) {
				    total.append(line);
				}					  
		    } catch (EOFException e) {
		    	LogManager.getInstance().info("MetrixAttachmentManager.getAttachmentStringFromFile: End of file reached");
		    }
		    
		    br.close();
		} catch (IOException e) {
			LogManager.getInstance().info("MetrixAttachmentManager.getAttachmentStringFromFile: I/O Error");
		}
		
		return total.toString();
	}
	
	/**
	 * Delete a file based on the file name if it exists.
	 * @param fileName The location of the file to delete.
	 * @return TRUE if the file was deleted, FALSE otherwise.
	 * @since 5.6
	 */
	public boolean deleteAttachmentFile(String fileName){
		boolean deleted = true;
		
		File attachmentFile = new File(fileName);
		try {
			if(attachmentFile.exists())
				deleted = attachmentFile.delete();
		}
		catch(Exception ex) {
			deleted = false;
		}
		
		return deleted;
	}
	
	/**
	 * Save a picture to a png file
	 * @param filename The name of the file that the picture should be saved as.
	 * @param bitmap The bitmap representation of the picture.
	 * @param context The application context.
	 * @since 5.6
	 */
	public void savePicture(String filename, Bitmap b, Context ctx){
	    try {
			FileOutputStream fOut = null;

			File file = new File(filename);
			fOut = new FileOutputStream(file);
			
			b.compress(Bitmap.CompressFormat.PNG, 100, fOut);
			fOut.flush();		
			fOut.close();	    		    
	    } catch (Exception e) {
	    	LogManager.getInstance().error(e);
	    }
	}

	/**
	 * Return the bitmap from a file name
	 * @param filename The name of the file to load.
	 * @return A bitmap representation of the image.
	 * @since 5.6
	 */
	public Bitmap loadPicture(String fileName){
		Bitmap b = null;
	    
	    try { 
	    	b = BitmapFactory.decodeFile(fileName);

	    } catch (Exception e) {
	    	LogManager.getInstance().error(e);
	    }
	    
	    return b;
	}

	public boolean canFileBeSuccessfullySaved(long fileSizeInBytes){
		final String path = getAttachmentPath();
		final StatFs stat = new StatFs(path);
		final long availableMemory = stat.getAvailableBytes();
		return fileSizeInBytes <= availableMemory;
	}

	public boolean saveBitmapImageAttachment(Bitmap image, File outputFile, Context appContext){
	    if(image == null || outputFile == null || appContext == null)
	        return false;
	    try{
            FileOutputStream fOut = new FileOutputStream(outputFile);
            if(!canFileBeSuccessfullySaved(image.getByteCount())){
                Toast.makeText(appContext, AndroidResourceHelper.getMessage("NotEnoughFreeSpace"), Toast.LENGTH_LONG).show();
                return false;
            }

            image.compress(Bitmap.CompressFormat.JPEG, 60, fOut);
            fOut.flush();
            fOut.close();
            return true;
        }catch (IOException ex){
            Toast.makeText(appContext, AndroidResourceHelper.getMessage("ErrorSavingAttachment"), Toast.LENGTH_LONG).show();
        }
	    return false;
    }

    public boolean copyFileToNewLocation(File oldFile, File newFile, Context appContext){
		if(oldFile == null || newFile == null || appContext == null)
			return false;

		try {
			if (!canFileBeSuccessfullySaved(oldFile.length())) {
				Toast.makeText(appContext, AndroidResourceHelper.getMessage("NotEnoughFreeSpace"), Toast.LENGTH_LONG).show();
				return false;
			}
			if (oldFile.exists()) {
				MetrixFileHelper.copyFile(new FileInputStream(oldFile), new FileOutputStream(newFile));
			}
			return true;
		}catch (IOException ex){
			Toast.makeText(appContext, AndroidResourceHelper.getMessage("NotEnoughFreeSpace"), Toast.LENGTH_LONG).show();
			LogManager.getInstance().error(ex);
		}
		return false;
	}
}

