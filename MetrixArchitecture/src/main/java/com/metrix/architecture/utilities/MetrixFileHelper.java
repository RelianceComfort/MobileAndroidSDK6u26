package com.metrix.architecture.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.utilities.Global.AttachmentType;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

public class MetrixFileHelper {
	/**
	 * Creates the specified <code>toFile</code> as a byte for byte copy of the
	 * <code>fromFile</code>. If <code>toFile</code> already exists, then it
	 * will be replaced with a copy of <code>fromFile</code>. The name and path
	 * of <code>toFile</code> will be that of <code>toFile</code>.<br/>
	 * <br/>
	 * <i> Note: <code>fromFile</code> and <code>toFile</code> will be closed by
	 * this function.</i>
	 *
	 * @param fromFile
	 *            - FileInputStream for the file to copy from.
	 * @param toFile
	 *            - FileInputStream for the file to copy to.
	 */
	public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
		FileChannel fromChannel = null;
		FileChannel toChannel = null;
		try {
			fromChannel = fromFile.getChannel();
			toChannel = toFile.getChannel();
			fromChannel.transferTo(0, fromChannel.size(), toChannel);
		} finally {
			try {
				if (fromChannel != null) {
					fromChannel.close();
				}
			} finally {
				if (toChannel != null) {
					toChannel.close();
				}
			}
		}
	}

	private static void copyFile(InputStream input, OutputStream output) throws IOException {
		int BUFFER_SIZE = 1024 * 2;
		byte[] buffer = new byte[BUFFER_SIZE];

		BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
		BufferedOutputStream out = new BufferedOutputStream(output, BUFFER_SIZE);
		int n;
		try {
			while ((n = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
				out.write(buffer, 0, n);
			}
			out.flush();
		} finally {
			try {
				out.close();
			} catch (IOException ex) {
				LogManager.getInstance().error(ex);
			}
			try {
				in.close();
			} catch (IOException ex) {
				LogManager.getInstance().error(ex);
			}
		}
	}

	static File copyFileToPublic(File fileToCopy) {
		// External files access removed from Android 11 and up. No copies made to public folders
		if (Build.VERSION.SDK_INT >= 30) {
			return fileToCopy;
		}
		try {
			String outputFileString = MetrixAttachmentHelper.getPublicFilePathFromAttachment(fileToCopy.getName());
			File outputFile = new File(outputFileString);
			if(fileToCopy.exists() || outputFile.exists()) {
				FileInputStream inputStream = new FileInputStream(fileToCopy);
				FileOutputStream outputStream = new FileOutputStream(outputFile);
				copyFile(inputStream, outputStream);
			}
			return outputFile;
		}catch (IOException ex) {
			Toast.makeText(MobileApplication.getAppContext(), AndroidResourceHelper.getMessage("NotEnoughFreeSpace"), Toast.LENGTH_LONG).show();
			LogManager.getInstance().error(ex);
			return null;
		}
	}

	public static File copyFileUriToPrivate(Activity activity, Uri mediaUri) {
		try {
			String outputFileString = MetrixAttachmentHelper.getFilePathFromAttachment(mediaUri.getLastPathSegment());
			File outputFile = new File(outputFileString);

			InputStream inputStream = activity.getContentResolver().openInputStream(mediaUri);
			OutputStream outputStream = new FileOutputStream(outputFile);
			copyFile(inputStream, outputStream);

			// remove the photo taken after we copy the file to our apps private directory
			activity.getContentResolver().delete(mediaUri, null, null);

			return outputFile;
		}catch (IOException ex) {
			Toast.makeText(MobileApplication.getAppContext(), AndroidResourceHelper.getMessage("NotEnoughFreeSpace"), Toast.LENGTH_LONG).show();
			LogManager.getInstance().error(ex);
			return null;
		}
	}

	public static boolean deletePublicFile(String fileName) {
		// External files access removed from Android 11 and up. No public copies created
		if (Build.VERSION.SDK_INT < 30) {
			String publicFilePath = MetrixAttachmentHelper.getPublicFilePathFromAttachment(fileName);
			File fileToDelete = new File(publicFilePath);
			if (fileToDelete.exists()) {
				return fileToDelete.delete();
			}
		}
		return true;
	}

	public static void deletePublicFolder() {
		// External files access removed from Android 11 and up. No public folders created
		if (Build.VERSION.SDK_INT < 30) {
			try {
				File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(MetrixApplicationAssistant.getApplicationAttachmentsDirectory()), "");

				if (mediaStorageDir.exists()) {
					mediaStorageDir.delete();
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			}
		}
	}

	/**
	 * Delete files from a directory
	 * @param dir The directory to delete the files from.
	 * @since 5.6
	 */
	public static void deleteFiles(String dir){
		File attachmentDir = new File(dir);
		String[] files;
		if(attachmentDir.exists()==false || attachmentDir.isDirectory()==false){
			return;
		}
		else {
			files = attachmentDir.list();
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

	/**
	 * Delete files from a directory
	 * @param dir The directory to delete the files from.
	 * @since 5.6
	 */
	public static void deleteFiles(Activity activity, String dir, int requestCode){
		File attachmentDir = new File(dir);
		String[] files;
		if(attachmentDir.exists()==false || attachmentDir.isDirectory()==false){
			return;
		}
		else {
			files = attachmentDir.list();
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

	/**
	 * Delete a file by specifying a file name
	 * @param fileName The name of the file to delete.
	 * @since 5.6
	 */
	public static void deleteFile(String fileName){
		File fileEntity = new File(fileName);

		if(fileEntity.exists()==false){
			return;
		}

		try {
			fileEntity.delete();

		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}
	}

	public static ArrayList<String> getFilesFromDirectory(String directory, String filter) {
		ArrayList<String> fileList = new ArrayList<String> ();

		File fileDir = new File(directory);

		if(fileDir.exists()==false || fileDir.isDirectory()==false){
			return fileList;
		}

		try {
			for(File file: fileDir.listFiles()){
				if(MetrixStringHelper.isNullOrEmpty(filter)){
					fileList.add(file.getName());
				}
				else {
					if(file.getName().contains(filter))
						fileList.add(file.getName());
				}
			}
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}

		return fileList;
	}

	public static ArrayList<String> getExcludedFilesFromDirectory(String directory, String excludedFilter) {
		ArrayList<String> fileList = new ArrayList<String> ();

		File fileDir = new File(directory);

		if(fileDir.exists()==false || fileDir.isDirectory()==false){
			return fileList;
		}

		try {
			for(File file: fileDir.listFiles()){
				if(MetrixStringHelper.isNullOrEmpty(excludedFilter)){
					fileList.add(file.getName());
				}
				else {
					if(file.getName().contains(excludedFilter))
						fileList.add(file.getName());
				}
			}
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
		}

		return fileList;
	}

	public static String getApplicationPath(Context context){
		return context.getApplicationContext().getFilesDir().getAbsolutePath().replace("files", "");
	}

	public static String getDatabaseDirectoryPath(Context context){
		String dbPath = getApplicationPath(context)+"databases/";

		return dbPath;
	}

	public static String getMetrixDatabasePath(Context context){
		String dbPath = getApplicationPath(context)+"databases/metrix.sqlite";

		return dbPath;
	}

	public static String getFileType(String fileName) {
		String fileExtension = "";

		// if we've passed in a full path, isolate the file name itself
		String isolatedFileName = fileName;
		if (fileName.contains("/")) {
			isolatedFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		}

		if (!MetrixStringHelper.isNullOrEmpty(isolatedFileName)) {
			String[] filePieces = isolatedFileName.split("\\.");
			if (filePieces.length > 1) {
				fileExtension = filePieces[filePieces.length - 1];
			}

			if (fileExtension.compareToIgnoreCase(AttachmentType.Jpeg.toString()) == 0)
				fileExtension = AttachmentType.Jpeg.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.Jpg.toString()) == 0)
				fileExtension = AttachmentType.Jpg.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.PNG.toString()) == 0)
				fileExtension = AttachmentType.PNG.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.BMP.toString()) == 0)
				fileExtension = AttachmentType.BMP.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.GIF.toString()) == 0)
				fileExtension = AttachmentType.GIF.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.Doc.toString()) == 0)
				fileExtension = AttachmentType.Doc.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.Docx.toString()) == 0)
				fileExtension = AttachmentType.Docx.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.Pdf.toString()) == 0)
				fileExtension = AttachmentType.Pdf.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.Xls.toString()) == 0)
				fileExtension = AttachmentType.Xls.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.Xlsx.toString()) == 0)
				fileExtension = AttachmentType.Xlsx.toString();
			if (fileExtension.compareToIgnoreCase(AttachmentType.Txt.toString()) == 0)
				fileExtension = AttachmentType.Txt.toString();
		}

		return fileExtension;
	}

	public static String getZippedFileName(String fileName) {
		String fileNamePiece = "";

		if (!MetrixStringHelper.isNullOrEmpty(fileName)) {
			String[] filePieces = fileName.split("\\.");

			if (filePieces.length > 1) {
				int extensionPos = fileName.lastIndexOf("." ) + 1;
				fileNamePiece = fileName.substring(0, extensionPos);
			}
			else {
				fileNamePiece = fileName;
			}

			return fileNamePiece+"zip";
		}
		else
			return "";
	}

	public static String getZippedFileName(String fileName, boolean fileExtension) {
		if(fileExtension)
			return getZippedFileName(fileName);
		else
			return fileName+".zip";
	}

	public static String getAttachmentValidPath(String path) {

		if (!MetrixStringHelper.isNullOrEmpty(path) && path.length() > 0) {

			// if no path information for the file, then add /sdcard/ as default
			// folder.
			if (path.contains("/") == false) {
				path = MetrixAttachmentManager.getInstance()
						.getAttachmentPath() + "/" + path;
			}
			return path;
		}
		return path;
	}

	//	@SuppressWarnings("resource")
	public static String readFileContent(File file, int readLimit) throws IOException {
		FileInputStream fis;
		fis = new FileInputStream(file);
		StringBuffer fileContent = new StringBuffer("");
		int n=0;
		int readLength=0;

		byte[] buffer = new byte[1024];

		while ((n = fis.read(buffer)) != -1)
		{
			fileContent.append(new String(buffer, 0, n));
			readLength=readLength+n;

			// if length =0, read whole file
			if(readLimit!=0) {
				if(readLength>=readLimit)
					break;
			}
		}

		fis.close();

		if(readLimit==0)
			return fileContent.toString();
		else {
			if(readLength>=readLimit)
				return fileContent.substring(0, readLimit);
			else
				return fileContent.substring(0, readLength);
		}
	}

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(String fileName)
	{
		int currentBuild = Build.VERSION.SDK_INT;
		if (currentBuild < 23) {
			return Uri.fromFile(getOutputMediaFileFromAttachmentFolder(fileName));
		}
		else{
			return FileProvider.getUriForFile(MobileApplication.getAppContext(), MobileApplication.getAppContext().getPackageName() + ".provider", getOutputMediaFileFromAttachmentFolder(fileName));
		}
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFileFromAttachmentFolder(String fileName)
	{
		File mediaStorageDir;
		// External files access removed from Android 11 and up. No public folders accessible. Using app owned external folders instead.
		if (Build.VERSION.SDK_INT >= 30) {
			mediaStorageDir = new File(MetrixPublicCache.instance.getApplicationContext().getExternalFilesDir(null), "files");
		} else {
			mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
					MobileApplication.getAppContext().getPackageName()), "files");
		}

		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				LogManager.getInstance().error("IFS Mobile", "failed to create directory");
				return null;
			}
		}
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + fileName);
		return mediaFile;
	}

	public static String getPublicMediaFolderPath()
	{
		String folderPath = "";

		try {
			File mediaStoragePictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

			if (!mediaStoragePictureDir.exists()) {
				LogManager.getInstance().error("IFS Mobile", "failed to locate media picture directory");
			} else {
				String PicturePath = mediaStoragePictureDir.getAbsolutePath();
				folderPath = PicturePath.substring(0, PicturePath.lastIndexOf('/'));
			}
		}catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
		return folderPath;
	}
}
