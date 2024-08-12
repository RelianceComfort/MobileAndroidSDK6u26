package com.metrix.architecture.utilities;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Display;
import android.view.WindowManager;

import com.metrix.architecture.utilities.Global.AttachmentType;

import java.io.File;
import java.io.FileInputStream;

@SuppressWarnings("deprecation")
public class MetrixAttachmentUtil {

	/**
	 * Checking the file is an image file.
	 */
	public static boolean isImageFile(String fileName) throws Exception {

		boolean status = false;
		String fileExtension = MetrixFileHelper.getFileType(fileName);

		if (!MetrixStringHelper.isNullOrEmpty(fileExtension)) {

			if (fileExtension.compareToIgnoreCase(AttachmentType.Jpeg.toString()) == 0
					|| fileExtension.compareToIgnoreCase(AttachmentType.Jpg
							.toString()) == 0
					|| fileExtension.compareToIgnoreCase(AttachmentType.PNG
							.toString()) == 0
					|| fileExtension.compareToIgnoreCase(AttachmentType.BMP
							.toString()) == 0
					|| fileExtension.compareToIgnoreCase(AttachmentType.GIF
							.toString()) == 0) {
				status = true;
			}
		}
		return status;
	}
	
	/**
	 * Checking the file is a video file.
	 */
	protected static boolean isVideoFile(String fileName) throws Exception{

		boolean status = false;
		String fileExtension = MetrixFileHelper.getFileType(fileName);

		if (!MetrixStringHelper.isNullOrEmpty(fileExtension)) {

			if (fileExtension.compareToIgnoreCase(AttachmentType.mp4
					.toString()) == 0) {
				status = true;
			}
		}
		return status;
	}
	
	/**
	 * Re-sampling image according to height X width.
	 */
	protected static Bitmap decodeFile(File imageFile, int requiredHeight, int requiredWidth) throws Exception {

		FileInputStream fileInputStreamIn = null;
		FileInputStream fileInputStreamOut = null;
		
		try{
			// Decode image size
			BitmapFactory.Options oSize = new BitmapFactory.Options();
			
			oSize.inJustDecodeBounds = true;
			oSize.inDither = false; // Disable Dithering mode
			oSize.inPurgeable = true; // Tell to gc that whether it needs free memory,// the Bitmap can be cleared
			oSize.inInputShareable = true;

			fileInputStreamIn = new FileInputStream(imageFile);
			BitmapFactory.decodeStream(fileInputStreamIn, null, oSize);
	
			// Find the correct scale value. It should be the power of 2.
			int width_tmp = oSize.outWidth, height_tmp = oSize.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp <= requiredWidth || height_tmp <= requiredHeight)
					break;
				width_tmp /= scale;
				height_tmp /= scale;
				scale *= 2;
			}
	
			// Decode with inSampleSize
			BitmapFactory.Options oPreview = new BitmapFactory.Options();
			oPreview.inSampleSize = scale;
			oPreview.inDither = false; // Disable Dithering mode
			oPreview.inPurgeable = true; // Tell to gc that whether it needs free memory,
			oPreview.inJustDecodeBounds = false;
			oPreview.inInputShareable = true;

			fileInputStreamOut = new FileInputStream(imageFile);
			return BitmapFactory.decodeStream(fileInputStreamOut, null, oPreview);
		}
		finally{
			//Got rid of EMFILE Exception(Too many file open)
			if(fileInputStreamIn != null)
				fileInputStreamIn.close();
			if(fileInputStreamOut != null)
				fileInputStreamOut.close();
		}

	}

	/**
	 * Re-sampling resource according to height X width.
	 */
	protected static Bitmap decodeResource(Context context, int imageResId, int requiredHeight, int requiredWidth) throws Exception {

		// Decode image size
		BitmapFactory.Options oSize = new BitmapFactory.Options();
		oSize.inJustDecodeBounds = true;
		oSize.inDither = false; // Disable Dithering mode
		oSize.inPurgeable = true; // Tell to gc that whether it needs free memory,
                                // the Bitmap can be cleared
		oSize.inInputShareable = true;

		BitmapFactory.decodeResource(context.getResources(), imageResId, oSize);

		// Find the correct scale value. It should be the power of 2.
		int width_tmp = oSize.outWidth, height_tmp = oSize.outHeight;
		int scale = 1;
		while (true) {
			if (width_tmp <= requiredWidth || height_tmp <= requiredHeight)
				break;
			width_tmp /= scale;
			height_tmp /= scale;
			scale *= 2;
		}

		// Decode with inSampleSize
		BitmapFactory.Options oPreview = new BitmapFactory.Options();
		oPreview.inSampleSize = scale;
		oPreview.inDither = false; // Disable Dithering mode
		oPreview.inPurgeable = true; // Tell to gc that whether it needs free memory,
		oPreview.inJustDecodeBounds = false;
		oPreview.inInputShareable = true;

		return BitmapFactory.decodeResource(context.getResources(), imageResId,
				oPreview);

	}

	/**
	 * Re-sampling resource without specifying height X width.
	 */
	protected static Bitmap decodeResource(Context context, int resourceId) throws Exception {
		// Decode image size
		BitmapFactory.Options oSize = new BitmapFactory.Options();
		oSize.inJustDecodeBounds = true;
		oSize.inDither = false; // Disable Dithering mode
		oSize.inPurgeable = true; // Tell to gc that whether it needs free memory, the Bitmap can be cleared
		oSize.inInputShareable = true;

		BitmapFactory.decodeResource(context.getResources(), resourceId, oSize);

		// The new size we want to scale to
		Display display = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		
		// Find the correct scale value. It should be the power of 2.
		int width_tmp = oSize.outWidth, height_tmp = oSize.outHeight;
		int requiredWidth = display.getWidth(); 
		int requiredHeight = display.getHeight();
		
		int scale = 1;
		while (true) {
			if (width_tmp <= requiredWidth || height_tmp <= requiredHeight)
				break;
			width_tmp /= scale;
			height_tmp /= scale;
			scale *= 2;
		}

		// Decode with inSampleSize
		BitmapFactory.Options oPreview = new BitmapFactory.Options();
		oPreview.inSampleSize = scale;
		oPreview.inDither = false; // Disable Dithering mode
		oPreview.inPurgeable = true; // Tell to gc that whether it needs free memory,
		oPreview.inJustDecodeBounds = false;
		oPreview.inInputShareable = true;

		return BitmapFactory.decodeResource(context.getResources(), resourceId,
				oPreview);
	}

	/**
	 * Re-sampling image without specifying height X width.
	 */
	public static Bitmap decodeFile(Context context, File imageFile) throws Exception {

		FileInputStream fileInputStreamIn = null;
		FileInputStream fileInputStreamOut = null;
		
		try{
			// Decode image size
			BitmapFactory.Options oSize = new BitmapFactory.Options();
			oSize.inJustDecodeBounds = true;
			oSize.inDither = false; // Disable Dithering mode
			oSize.inPurgeable = true; // Tell to gc that whether it needs free memory, the Bitmap can be cleared
			oSize.inInputShareable = true;

			fileInputStreamIn = new FileInputStream(imageFile);
			BitmapFactory.decodeStream(fileInputStreamIn, null, oSize);
	
			// The new size we want to scale to
			Display display = ((WindowManager) context
					.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			
			// Find the correct scale value. It should be the power of 2.
			int width_tmp = oSize.outWidth, height_tmp = oSize.outHeight;
			int requiredWidth = display.getWidth(); 
			int requiredHeight = display.getHeight();
	
			int scale = 1;
			while (true) {
				if (width_tmp <= requiredWidth || height_tmp <= requiredHeight)
					break;
				width_tmp /= scale;
				height_tmp /= scale;
				scale *= 2;
			}
	
			// Decode with inSampleSize
			BitmapFactory.Options oPreview = new BitmapFactory.Options();
			oPreview.inSampleSize = scale;
			oPreview.inDither = false; // Disable Dithering mode
			oPreview.inPurgeable = true; // Tell to gc that whether it needs free memory,
			oPreview.inJustDecodeBounds = false;
			oPreview.inInputShareable = true;

			fileInputStreamOut = new FileInputStream(imageFile);
			return BitmapFactory.decodeStream(fileInputStreamOut, null,
					oPreview);
		}
		finally{
			if(fileInputStreamIn != null)
				fileInputStreamIn.close();
			if(fileInputStreamOut != null)
				fileInputStreamOut.close();
		}

	}

	/**
	 * Re-sampling Video thumbnail without specifying height X width.
	 */
	protected static Bitmap decodeVideo(Context context, File videoFile) throws Exception {

		Bitmap thumbnailVideoBitmap = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath(),
				MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);

		// The new size we want to scale to
		Display display = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int requiredWidth = display.getWidth(); 
		int requiredHeight = display.getHeight();

		// Find the correct scale value. It should be the power of 2.
		int width_tmp = (int) thumbnailVideoBitmap.getWidth(), height_tmp = (int) thumbnailVideoBitmap
				.getHeight();
		
		int scale = 1;
		while (true) {
			if (width_tmp <= requiredWidth || height_tmp <= requiredHeight)
				break;
			width_tmp /= scale;
			height_tmp /= scale;
			scale *= 2;
		}
		
		return Bitmap.createScaledBitmap(thumbnailVideoBitmap, width_tmp,
				height_tmp, false);

	}
	
	/**
	 * Re-sampling Video thumbnail according to height X width.
	 */
	protected static Bitmap decodeVideo(File videoFile, int requiredHeight, int requiredWidth) throws Exception {

		Bitmap thumbnailVideoBitmap = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath(),
				MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);

		// Find the correct scale value. It should be the power of 2.
		int width_tmp = (int) thumbnailVideoBitmap.getWidth(), height_tmp = (int) thumbnailVideoBitmap
				.getHeight();
		
		int scale = 1;
		while (true) {
			if (width_tmp <= requiredWidth || height_tmp <= requiredHeight)
				break;
			width_tmp /= scale;
			height_tmp /= scale;
			scale *= 2;
		}

		return Bitmap.createScaledBitmap(thumbnailVideoBitmap, width_tmp,
				height_tmp, false);
	}
	
	// ---------------------------------------------------------------------------
	// DebriefTaskAttachment -> attachment preview

	/**
	 * Re-sampling image without specifying height X width.
	 */
	protected static Bitmap decodeAttachmentFile(Context context, File imageFile) throws Exception {

		FileInputStream fileInputStreamIn = null;
		FileInputStream fileInputStreamOut = null;
		
		try{
			// Decode image size
			BitmapFactory.Options oSize = new BitmapFactory.Options();
			oSize.inJustDecodeBounds = true;

			fileInputStreamIn = new FileInputStream(imageFile);
			BitmapFactory.decodeStream(fileInputStreamIn, null, oSize);
	
			// The new size we want to scale to
			Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			int width = display.getWidth();
			final int REQUIRED_SIZE = width;
	
			// Find the correct scale value. It should be the power of 2.
			int width_tmp = oSize.outWidth, height_tmp = oSize.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp < REQUIRED_SIZE || height_tmp < REQUIRED_SIZE)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}
	
			// Decode with inSampleSize
			BitmapFactory.Options oPreview = new BitmapFactory.Options();
			oPreview.inSampleSize = scale;
			oPreview.inJustDecodeBounds = false;
			fileInputStreamOut = new FileInputStream(imageFile);
			return BitmapFactory.decodeStream(fileInputStreamOut, null, oPreview);
		}
		finally{
			if(fileInputStreamIn != null)
				fileInputStreamIn.close();
			if(fileInputStreamOut != null)
				fileInputStreamOut.close();
		}
	}

	/**
	 * Re-sampling resource without specifying height X width.
	 */
	protected static Bitmap decodeAttachmentResource(Context context,
			int resourceId) throws Exception {

		Bitmap tempBitmap = BitmapFactory.decodeResource(
				context.getResources(), resourceId);

		// The new size we want to scale to
		Display display = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		int requiredWidth = display.getWidth();
		int requiredHeight = display.getHeight();

		return Bitmap.createScaledBitmap(tempBitmap, requiredWidth,
				requiredHeight / 2, false);
	}

	/**
	 * Re-sampling Video thumbnail without specifying height X width.
	 */
	protected static Bitmap decodeAttachmentVideo(Context context,
			File videoFile) throws Exception {

		Bitmap thumbnailVideoBitmap = ThumbnailUtils.createVideoThumbnail(
				videoFile.getAbsolutePath(),
				MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);

		// The new size we want to scale to
		Display display = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		int requiredWidth = display.getWidth();
		int requiredHeight = display.getHeight();

		return Bitmap.createScaledBitmap(thumbnailVideoBitmap,
				requiredWidth / 2, requiredHeight / 2, false);
	}
	/**
	 * Get full file path from Uri .
	 */
	public static  String getFilePathFromUri(Context context,Uri uri) {
		String[] projection = {MediaStore.Images.Media.DATA};
		String imagePath = "";
		try {
			Cursor cursor =  context.getContentResolver().query(uri, projection, null, null, null);
			if(cursor != null){
				if ( cursor.moveToFirst( ) ) {
					int columnIndex = cursor.getColumnIndex(projection[0]);
					imagePath = cursor.getString(columnIndex);
				}
				cursor.close( );
			}
		}
		catch(Exception e) {
			LogManager.getInstance().error(e);
		}
		return  imagePath;
	}
}
