package com.metrix.architecture.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import androidx.collection.LruCache;
import androidx.appcompat.app.ActionBar;

import android.provider.OpenableColumns;
import android.widget.ImageView;
import android.widget.Toast;

@SuppressWarnings("unchecked")
@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
public class MetrixAttachmentHelper extends MetrixAttachmentHelperBase {

	private static HashMap<String, Object> resourceCache;
	static {
		resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance
				.getItem("AttachmentHelperResources");
	}

	/**
	 * Show preview of the image according to width X height with async loading and using
	 * memory cache.
	 */
	@SuppressLint("DefaultLocale")
	public static void showGridPreview(Context context, String filePath, ImageView imageView, int requiredHeight, int requiredWidth, LruCache<String, Bitmap> memoryCache, String key) throws Exception {
		if(resourceCache == null) resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");
		AttachmentWorker attachmentWorker = new AttachmentWorker(context, filePath, imageView, requiredHeight, requiredWidth, key, memoryCache, resourceCache);
		attachmentWorker.execute();
	}

	/**
	 * Show preview of the image without specifying width X height
	 *
	 */
	@Deprecated
	public static void showFullScreenPreview(Context context, String filePath, ImageView imageView) {
		try{
			if(resourceCache == null) resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");
			Bitmap bitmap = loadBitmap(context, filePath, resourceCache);
			imageView.setImageBitmap(bitmap);
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
	}

	/**
	 * Add image to memory cache.
	 */
	public static void addBitmapToMemoryCache(String key, Bitmap bitmap, LruCache<String, Bitmap> memoryCache) {
		try{
			if (getBitmapFromMemCache(key, memoryCache) == null)
				memoryCache.put(key, bitmap);
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
	}

	/**
	 * Get image from memory cache.
	 */
	public static Bitmap getBitmapFromMemCache(String key, LruCache<String, Bitmap> memoryCache) {
		try{
			return memoryCache.get(key);
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
		return null;
	}

	/**
	 * Viewing the attachments such pdf, mp4 with the default application
	 */
	public static void showAttachment(Context context, String filePath, String authority){
		try{
			generateAttachment(context, filePath, authority);
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
	}

	/**
	 * Setting the video icon manually for the video thumbnails
	 */
	@SuppressWarnings("deprecation")
	public static void setVideoIcon(ImageView imageViewVideoIcon, String filePath, boolean fromCarousel) {
		try{
			if(resourceCache == null) resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");
			if (isVideoFile(filePath)) {
				if (fromCarousel)
					imageViewVideoIcon.setBackgroundResource((Integer) resourceCache.get("R.drawable.attachment_video_icon_medium"));
				else
					imageViewVideoIcon.setBackgroundResource((Integer) resourceCache.get("R.drawable.attachment_video_icon_small"));
			} else
				imageViewVideoIcon.setBackgroundDrawable(null);
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}

	}

	public static boolean checkImageFile(String filePath){
		try{
			return isImageFile(filePath);
		}
		catch(Exception e){
			LogManager.getInstance().error(e);
		}
		return false;

	}

	/***
	 * Sets the image of an image view, using the provided Image ID, with no in-code scaling.
	 *
	 * @return boolean value reflecting success of application
	 *
	 * @since 5.6.2
	 */
	public static boolean applyImageWithNoScale(String imageID, ImageView view) {
		try {
			String fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + imageID;
			File iconFile = new File(fullPath);
			if (iconFile.exists()) {
				Bitmap iconToUse = MetrixAttachmentUtil.decodeFile(view.getContext(), iconFile);
				if (iconToUse != null) {
					view.setImageBitmap(iconToUse);
					return true;
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}

	/***
	 * Sets the image of an image view, using the provided Image ID
	 * and also height and width (in dp).
	 *
	 * @return boolean value reflecting success of application
	 *
	 * @since 5.6.2
	 */
	public static boolean applyImageWithDPScale(String imageID, ImageView view, int requiredHeightDP, int requiredWidthDP) {
		try {
			String fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + imageID;
			File iconFile = new File(fullPath);
			if (iconFile.exists()) {
				Bitmap iconToUse = MetrixAttachmentUtil.decodeFile(view.getContext(), iconFile);
				if (iconToUse != null) {
					float scale = view.getResources().getDisplayMetrics().density;
					if (scale != 1.0f) {
						int requiredHeightPixels = (int) (requiredHeightDP * scale + 0.5f);
						int requiredWidthPixels = (int) (requiredWidthDP * scale + 0.5f);
						iconToUse = resizeBitmap(iconToUse, requiredHeightPixels, requiredWidthPixels);
					}
					view.setImageBitmap(iconToUse);
					return true;
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}

	/***
	 * Sets the image of an image view, using the provided Image ID
	 * and also height and width (in pixels).
	 *
	 * @return boolean value reflecting success of application
	 *
	 * @since 5.6.2
	 */
	public static boolean applyImageWithPixelScale(String imageID, ImageView view, int requiredHeightPixels, int requiredWidthPixels) {
		try {
			String fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + imageID;
			File iconFile = new File(fullPath);
			if (iconFile.exists()) {
				Bitmap iconToUse = MetrixAttachmentUtil.decodeFile(view.getContext(), iconFile);
				if (iconToUse != null) {
					iconToUse = resizeBitmap(iconToUse, requiredHeightPixels, requiredWidthPixels);
					view.setImageBitmap(iconToUse);
					return true;
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}

	public static Bitmap resizeBitmap(Bitmap bm, int newHeight, int newWidth) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
		return resizedBitmap;
	}

	public static boolean resizeImageFile(String sourceFilePath, String destFilePath, Context context){
		boolean success = resizeImageFile(sourceFilePath, destFilePath);
		if(context != null && !success){
			Toast.makeText(context, AndroidResourceHelper.getMessage("ErrorSavingAttachment"), Toast.LENGTH_LONG).show();
		}
		return success;
	}

	public static boolean resizeImageFile(String sourceFilePath, String destFilePath) {
		Size desiredFileSize = GetImageMaxResolution(sourceFilePath);
		try
		{
			int inWidth = 0;
			int inHeight = 0;

			InputStream in = new FileInputStream(sourceFilePath);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;

			BitmapFactory.decodeStream(in, null, options);
			in.close();
			in = null;

			// save width and height
			inWidth = options.outWidth;
			inHeight = options.outHeight;

			float ratioX = desiredFileSize.width / (float)inWidth;
			float ratioY = desiredFileSize.height / (float)inHeight;
			float ratio = Math.min(ratioX, ratioY);
			int newHeight = (int)(inHeight * ratio);
			int newWidth = (int)(inWidth * ratio);

			in = new FileInputStream(sourceFilePath);
			options = new BitmapFactory.Options();
			// calc rought re-size (this is no exact resize)
			options.inSampleSize = Math.max(inWidth/desiredFileSize.width, inHeight/desiredFileSize.height);
			// decode full image
			Bitmap roughBitmap = BitmapFactory.decodeStream(in, null, options);

			ExifInterface exif = new ExifInterface(sourceFilePath);
			// calc exact destination size
			Matrix m = new Matrix();
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
			if (orientation == 6) {
				int middleValue = newHeight;
				newHeight = newWidth;
				newWidth = middleValue;
				m.postRotate(90);
			}
			else if (orientation == 3) {
				m.postRotate(180);
			}
			else if (orientation == 8) {
				int middleValue = newHeight;
				newHeight = newWidth;
				newWidth = middleValue;
				m.postRotate(270);
			}

			Bitmap rotateBitmap = Bitmap.createBitmap(roughBitmap, 0, 0, roughBitmap.getWidth(), roughBitmap.getHeight(), m, true);

			RectF inRect = new RectF(0, 0, rotateBitmap.getWidth(), rotateBitmap.getHeight());
			RectF outRect = new RectF(0, 0, newWidth, newHeight);
			m.setRectToRect(inRect, outRect, Matrix.ScaleToFit.CENTER);
			float[] values = new float[9];
			m.getValues(values);

			Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotateBitmap, newWidth, newHeight, true);
			if(!MetrixAttachmentManager.getInstance().canFileBeSuccessfullySaved(resizedBitmap.getByteCount()))
				return false;

			FileOutputStream out = new FileOutputStream(destFilePath);
			resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
			roughBitmap.recycle();
			rotateBitmap.recycle();
			resizedBitmap.recycle();
		}
		catch (Exception e)
		{
			LogManager.getInstance().error(e);
			return false;
		}
		return true;
	}

	public static Size GetImageMaxResolution(String filePath) {
		Size imageSize = new Size(1024, 768);
		int origHeight = 0;
		int origWidth = 0;

		try {
			InputStream in = new FileInputStream(filePath);

			// decode image size (decode metadata only, not the whole image)
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(in, null, options);
			in.close();
			in = null;

			// save width and height
			origWidth = options.outWidth;
			origHeight = options.outHeight;

			String sizeType = MetrixDatabaseManager.getAppParam("CAMERA_PHOTO_SIZE");

			if (MetrixStringHelper.isNullOrEmpty(sizeType))
			{
				sizeType = "Extra Large";
			}

			if (sizeType.equalsIgnoreCase("Large"))
			{
				if(origWidth >= origHeight) {
					imageSize.width = 1024;
					imageSize.height = 768;
				}
				else {
					imageSize.width = 768;
					imageSize.height = 1024;
				}
				return imageSize;
			}

			else if (sizeType.equalsIgnoreCase("Medium"))
			{
				if (origWidth >= origHeight)
				{
					imageSize.width = 640;
					imageSize.height = 480;
				}
				else
				{
					imageSize.width = 480;
					imageSize.height = 640;
				}
				return imageSize;
			}
			else if (sizeType.equalsIgnoreCase("Small"))
			{
				if (origWidth >= origHeight)
				{
					imageSize.width = 320;
					imageSize.height = 240;
				}
				else
				{
					imageSize.width = 240;
					imageSize.height = 320;
				}
				return imageSize;
			}
			else {
				imageSize.height = origHeight;
				imageSize.width = origWidth;
				return imageSize;
			}
		}catch(IOException e)
		{
			LogManager.getInstance().error(e);
		}

		return imageSize;
	}

	//---------------------------------------------------------------
	//DebriefTaskAttachment -> preview
	public static void showAttachmentFullScreenPreview(Context context, String filePath, ImageView imageView) {
		try {
			if (resourceCache == null)
				resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");

			Bitmap bitmap = MetrixAttachmentHelper.loadAttachmentBitmap(context, filePath, resourceCache);
			imageView.setImageBitmap(bitmap);
		} catch(Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	public static void showAttachmentFieldPreview(Context context, String filePath, ImageView imageView, boolean isFileOnDemand) {
		try {
			if (resourceCache == null)
				resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");

			Bitmap bitmap;

			if (isFileOnDemand && !MetrixAttachmentHelper.isAttachmentExists(filePath)) {
				bitmap = MetrixAttachmentHelper.loadOnDemandPlaceholderBitmap(context, resourceCache);
			}
			else if (!MetrixStringHelper.isNullOrEmpty(filePath))
				bitmap = MetrixAttachmentHelper.loadAttachmentBitmap(context, filePath, resourceCache);
			else
				bitmap = MetrixAttachmentHelper.loadEmptyPlaceholderBitmap(context, resourceCache);

			imageView.setImageBitmap(bitmap);
		} catch(Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	public static void showAttachmentFieldPreview(Context context, String filePath, ImageView imageView) {
		try {
			if (resourceCache == null)
				resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");

			Bitmap bitmap;
			if (!MetrixStringHelper.isNullOrEmpty(filePath))
				bitmap = MetrixAttachmentHelper.loadAttachmentBitmap(context, filePath, resourceCache);
			else
				bitmap = MetrixAttachmentHelper.loadEmptyPlaceholderBitmap(context, resourceCache);

			imageView.setImageBitmap(bitmap);
		} catch(Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	public static String getFilePathFromAttachment(String fileName) {
		String filePath = "";

		try {
			File mediaStorageDir = new File(MobileApplication.getAppContext().getExternalFilesDir(null), "attachments");
			return mediaStorageDir.getPath() + File.separator + fileName;
		}
		catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return filePath;
	}

	static String getPublicFilePathFromAttachment(String fileName) {

		try {
			File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(MetrixApplicationAssistant.getApplicationAttachmentsDirectory()), "");

			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					LogManager.getInstance().error("IFS Mobile", "failed to create directory");
					return null;
				}
			}

			return mediaStorageDir.getPath() + File.separator + fileName;
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return "";
	}

	/**
	 *  Copy the original file to Attachment folder with file name according to AttachmentAPI standard
	 * @param fileOriginalPath
	 * @return attachmentFileName
	 */
	public static String transferFileToAttachmentFolder(Uri fileOriginalPath) {
		String fileOriginalName = queryFileName(MobileApplication.getAppContext(), fileOriginalPath);
		String fileExtension = MetrixFileHelper.getFileType(fileOriginalName);

		// Use standard file name pattern for destination
		String fileName = AttachmentWidgetManager.generateFileName(fileExtension);
		String filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);

		final File attachmentNewFile = new File(filePath);

		try {
			if (!createFileFromStream(MobileApplication.getAppContext().getContentResolver().openInputStream(fileOriginalPath), attachmentNewFile))
				return "";
		} catch (Exception ex) {
			return "";
		}

		return fileName;
	}

	/**
	 * Create file from input stream of file Uri
	 * @param inStream
	 * @param destination
	 */
	public static Boolean createFileFromStream(InputStream inStream, File destination) {
		Boolean fileCreated = true;
		try (OutputStream os = new FileOutputStream(destination)) {
			byte[] buffer = new byte[4096];
			int length;
			while ((length = inStream.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			os.flush();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			fileCreated = false;
		}

		return fileCreated;
	}

	public static boolean isAttachmentExists(String filePath) {
		File file = new File(filePath);
		return 	file.exists();
	}

	private static String queryFileName(Context context, Uri uri) {
		Cursor returnCursor =
				context.getContentResolver().query(uri, null, null, null, null);
		assert returnCursor != null;
		int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
		returnCursor.moveToFirst();
		String name = returnCursor.getString(nameIndex);
		returnCursor.close();
		return name;
	}

	/**
	 *  Copy the original file to Attachment folder with file name according to AttachmentAPI standard
	 * @param fileOriginalPath
	 * @return attachmentFileName
	 */
	public static String transferFileToAttachmentFolder(String fileOriginalPath) {
		String fileOriginalName = fileOriginalPath.substring(fileOriginalPath.lastIndexOf("/") + 1);
		String attachmentTestPath = MetrixAttachmentHelper.getFilePathFromAttachment(fileOriginalName);
		String fileExtension = MetrixFileHelper.getFileType(fileOriginalName);
		boolean isExistingFile = false;
		String fileName = "";
		String filePath = "";

		// OSP-3443 We might be able to remove this because we are saving files to the private
		//  directory and our File Picker currently doesn't get access to other files beside
		//  our own
		// Since we copy the original attachment to FSM managed attachment directory, verify we can do that first
		if (MetrixStringHelper.valueIsEqual(fileOriginalPath, attachmentTestPath)) {
			isExistingFile = true; // User has selected a file from the FSM managed attachment directory. No need to copy the file again.
			fileName = fileOriginalName;
			filePath = attachmentTestPath;
			return fileOriginalName;
		} else {
			// Use standard file name pattern for destination
			fileName = AttachmentWidgetManager.generateFileName(fileExtension);
			filePath = MetrixAttachmentHelper.getFilePathFromAttachment(fileName);

			final File attachmentNewFile = new File(filePath);
			final File attachmentOldFile = new File(fileOriginalPath);

			if (!MetrixAttachmentManager.getInstance().copyFileToNewLocation(attachmentOldFile, attachmentNewFile, MobileApplication.getAppContext()))
				return "";
			else
				return fileName;
		}
	}

	public static boolean applyActionBarIconWithDPScale(String imageID, ActionBar actionBar, int requiredHeightDP, int requiredWidthDP) {
		try {
			String fullPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + imageID;
			File iconFile = new File(fullPath);
			if (iconFile.exists()) {
				Bitmap iconToUse = MetrixAttachmentUtil.decodeFile(actionBar.getCustomView().getContext(), iconFile);
				if (iconToUse != null) {
					float scale = actionBar.getCustomView().getResources().getDisplayMetrics().density;
					if (scale != 1.0f) {
						int requiredHeightPixels = (int) (requiredHeightDP * scale + 0.5f);
						int requiredWidthPixels = (int) (requiredWidthDP * scale + 0.5f);
						iconToUse = resizeBitmap(iconToUse, requiredHeightPixels, requiredWidthPixels);
					}
					Drawable iconDrawable = new BitmapDrawable(actionBar.getCustomView().getResources(),iconToUse);
					actionBar.setIcon(iconDrawable);
					return true;
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}

	public static boolean applyActionBarDefaultIconWithDPScale(int imageID, ActionBar actionBar, int requiredHeightDP, int requiredWidthDP) {
		try {
			Bitmap iconToUse = MetrixAttachmentUtil.decodeResource(actionBar.getCustomView().getContext(), imageID);
			if (iconToUse != null) {
				float scale = actionBar.getCustomView().getResources().getDisplayMetrics().density;
				if (scale != 1.0f) {
					int requiredHeightPixels = (int) (requiredHeightDP * scale + 0.5f);
					int requiredWidthPixels = (int) (requiredWidthDP * scale + 0.5f);
					iconToUse = resizeBitmap(iconToUse, requiredHeightPixels, requiredWidthPixels);
				}
				Drawable iconDrawable = new BitmapDrawable(actionBar.getCustomView().getResources(),iconToUse);
				actionBar.setIcon(iconDrawable);
				return true;
			}

		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}
}

class Size {
	public int height;
	public int width;
	public Size(int widthIn, int heightIn) {
		this.height = heightIn;
		this.width = widthIn;
	}

}

