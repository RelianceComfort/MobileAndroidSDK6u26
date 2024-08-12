package com.metrix.architecture.utilities;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import androidx.core.content.FileProvider;

import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.metrix.architecture.utilities.Global.AttachmentType;

public class MetrixAttachmentHelperBase extends MetrixAttachmentUtil {

	/**
	 * load attachments with specific size(height X width).
	 */
	protected static Bitmap loadBitmap(Context context, String filePath, int requiredHeight, int requiredWidth, HashMap<String, Object> resourceCache) throws Exception {
		Bitmap bitmap = null;
		Resources res = context.getResources();

		if (!MetrixStringHelper.isNullOrEmpty(MetrixFileHelper.getAttachmentValidPath(filePath))) {
			File attachmentFile = new File(filePath);
			if (attachmentFile.exists()) {
				String fileExtension = MetrixFileHelper.getFileType(filePath);
				if (!MetrixStringHelper.isNullOrEmpty(fileExtension)) {
					if (isImageFile(filePath)) {
						bitmap = getImageBitmap(context, attachmentFile, requiredHeight, requiredWidth);
					} else if (isVideoFile(filePath)) {
						bitmap = getVideoBitmap(context, attachmentFile, requiredHeight, requiredWidth);
					} else {
						if (fileExtension.compareToIgnoreCase(AttachmentType.Pdf.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.pdf_file")), requiredHeight, requiredWidth);
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Xls.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Xlsx.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Csv.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.excel_file")), requiredHeight, requiredWidth);
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Doc.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Docx.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.word_file")), requiredHeight, requiredWidth);
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Ppt.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Pptx.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.tutorial")), requiredHeight, requiredWidth);
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Txt.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Log.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.document")), requiredHeight, requiredWidth);
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.mp3.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Wav.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.audio_file")), requiredHeight, requiredWidth);
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Htm.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Html.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.country")), requiredHeight, requiredWidth);
						} else {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.document_alt")), requiredHeight, requiredWidth);
						}
					}
				} else {
					// no file extension, but file exists, so we assume this is an image
					bitmap = getImageBitmap(context, attachmentFile, requiredHeight, requiredWidth);
				}
			} else {
				bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.error_alt")), requiredHeight, requiredWidth);
			}
		} else {
			bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.error_alt")), requiredHeight, requiredWidth);
		}

		// If somehow we get here and the bitmap is still null, generate an error placeholder.
		if (bitmap == null) {
			bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.error_alt")), requiredHeight, requiredWidth);
		}

		return bitmap;
	}

	/**
	 * load attachments without specifying a size
	 */
	protected static Bitmap loadBitmap(Context context, String filePath, HashMap<String, Object> resourceCache) throws Exception {
		Bitmap bitmap = null;
		Resources res = context.getResources();
		if (!MetrixStringHelper.isNullOrEmpty(MetrixFileHelper.getAttachmentValidPath(filePath))) {
			File attachmentFile = new File(filePath);
			if (attachmentFile.exists()) {
				String fileExtension = MetrixFileHelper.getFileType(filePath);
				if (!MetrixStringHelper.isNullOrEmpty(fileExtension)) {
					if (isImageFile(filePath)) {
						bitmap = getImageBitmap(context, attachmentFile);
					} else if (isVideoFile(filePath)) {
						bitmap = getVideoBitmap(context, attachmentFile);
					} else {
						if (fileExtension.compareToIgnoreCase(AttachmentType.Pdf.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.pdf_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Xls.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Xlsx.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Csv.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.excel_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Doc.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Docx.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.word_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Ppt.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Pptx.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.tutorial")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Txt.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Log.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.document")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.mp3.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Wav.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.audio_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Htm.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Html.toString()) == 0) {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.country")));
						} else {
							bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.document_alt")));
						}
					}
				} else {
					// no file extension, but file exists, so we assume this is an image
					bitmap = getImageBitmap(context, attachmentFile);
				}
			} else {
				bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.error_alt")));
			}
		} else {
			bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.error_alt")));
		}

		// If somehow we get here and the bitmap is still null, generate an error placeholder.
		if (bitmap == null) {
			bitmap = drawableToBitmap(res.getDrawable((int)resourceCache.get("R.drawable.error_alt")));
		}

		return bitmap;
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		Bitmap bitmap = null;

		if (drawable instanceof BitmapDrawable)	{
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if (bitmapDrawable.getBitmap() != null)
				return bitmapDrawable.getBitmap();
		}

		if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		else
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static Bitmap drawableToBitmap(Drawable drawable, int requiredHeight, int requiredWidth) {
		Bitmap bitmap = null;

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if (bitmapDrawable.getBitmap() != null)
				return bitmapDrawable.getBitmap();
		}

		if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		else
			bitmap = Bitmap.createBitmap(requiredWidth, requiredHeight, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static Bitmap attachmentDrawableToBitmap(Context context, Drawable drawable) {
		Bitmap bitmap = null;
		float aspectRatio = drawable.getIntrinsicHeight()/drawable.getIntrinsicWidth();

		// The new size we want to scale to
		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int requiredWidth = display.getWidth();

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if (bitmapDrawable.getBitmap() != null)
				return bitmapDrawable.getBitmap();
		}

		if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		else {
			int adjustedHeight = (int) aspectRatio * requiredWidth;
			bitmap = Bitmap.createBitmap(requiredWidth, adjustedHeight, Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	/**
	 * Get bitmap of the resource file according to width X height(ex: icon for PDF, XLS).
	 */
	private static Bitmap getResourceBitmap(Context context, int resourceId, int requiredHeight, int requiredWidth) throws Exception{
		return decodeResource(context, resourceId, requiredHeight, requiredWidth);
	}

	/**
	 * Get bitmap of the resource file without specifying width X height(ex: icon for PDF, XLS)
	 */
	private static Bitmap getResourceBitmap(Context context, int resourceId) throws Exception{
		return decodeResource(context, resourceId);
	}

	/**
	 * Get bitmap of the image file according to width X height.
	 */
	private static Bitmap getImageBitmap(Context context, File file, int requiredHeight, int requiredWidth) throws Exception {
		Bitmap bm = null;
		ExifInterface ei = new ExifInterface(file.getAbsolutePath());
		int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

		switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				bm = MetrixUIHelper.RotateBitmap(decodeFile(file, requiredHeight, requiredWidth), 90);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				bm = MetrixUIHelper.RotateBitmap(decodeFile(file, requiredHeight, requiredWidth), 180);
				break;
			default:
				bm = decodeFile(file, requiredHeight, requiredWidth);
				break;
		}

		return bm;
	}

	/**
	 * Get bitmap of the image file without specifying the height X width.
	 */
	private static Bitmap getImageBitmap(Context context, File file) throws Exception {
		Bitmap bm = null;

		ExifInterface ei = new ExifInterface(file.getAbsolutePath());
		int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

		switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				bm = MetrixUIHelper.RotateBitmap(decodeFile(context, file), 90);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				bm = MetrixUIHelper.RotateBitmap(decodeFile(context, file), 180);
				break;
			default:
				bm = decodeFile(context, file);
				break;
		}

		return bm;
	}

	/**
	 * Get bitmap of the video file preview without specifying the height X width.
	 */
	private static Bitmap getVideoBitmap(Context context, File file) throws Exception {
		return decodeVideo(context, file);
	}

	/**
	 * Get bitmap of the pdf file preview according to height X width.
	 */
	private static Bitmap getVideoBitmap(Context context, File file, int requiredHeight, int requiredWidth) throws Exception {
		return decodeVideo(file, requiredHeight, requiredWidth);
	}

	/**
	 * Get special formatted date for attachments.
	 * @throws ParseException
	 */
	@SuppressLint("SimpleDateFormat")
	protected static String getFormattedAttachmentDate(String attachmentCreationDate, String newDateFormat) throws ParseException {
		Date date = MetrixDateTimeHelper.convertDateTimeFromUIToDate(MetrixDateTimeHelper.convertDateTimeFromDBToUI(attachmentCreationDate));
		DateFormat dateFormat = new java.text.SimpleDateFormat(newDateFormat);
		return attachmentCreationDate = dateFormat.format(date);
	}

	/**
	 * Opening the attachment in the default application(Ex: pdf, mp4).
	 */
	protected static void generateAttachment(Context context, String filePath, String authority) throws Exception {
		if (!MetrixStringHelper.isNullOrEmpty(MetrixFileHelper.getAttachmentValidPath(filePath))) {
			File attachmentFile = new File(filePath);

			if (attachmentFile.exists()) {
				// External file access removed from Android 11 and up. No file copying but explicitly sending the FileProvider contentURI for all file types.
				if (Build.VERSION.SDK_INT >= 30) {
					Uri contentUri = FileProvider.getUriForFile(context, authority, attachmentFile);
					Intent intent = new Intent(Intent.ACTION_VIEW, contentUri);
					intent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
					try {
						context.startActivity(intent);
					} catch (Exception e) {
						String fileExtension = MetrixFileHelper.getFileType(filePath);
						if (isVideoFile(filePath)) {
							Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationVideo"), Toast.LENGTH_SHORT).show();
						} else if (AttachmentType.Pdf.toString().equalsIgnoreCase(fileExtension)) {
							Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationPDF"), Toast.LENGTH_SHORT).show();
						} else if (AttachmentType.Xls.toString().equalsIgnoreCase(fileExtension) ||
								AttachmentType.Xlsx.toString().equalsIgnoreCase(fileExtension) ||
								AttachmentType.Csv.toString().equalsIgnoreCase(fileExtension)) {
							Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationExcel"), Toast.LENGTH_SHORT).show();
						} else if (AttachmentType.Doc.toString().equalsIgnoreCase(fileExtension) ||
								AttachmentType.Docx.toString().equalsIgnoreCase(fileExtension)) {
							Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationWord"), Toast.LENGTH_SHORT).show();
						} else if (AttachmentType.Txt.toString().equalsIgnoreCase(fileExtension) ||
								AttachmentType.Log.toString().equalsIgnoreCase(fileExtension)) {
							Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationText"), Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationFileType", fileExtension), Toast.LENGTH_SHORT).show();
						}
						LogManager.getInstance().error(e);
					}
				} else {
					attachmentFile = MetrixFileHelper.copyFileToPublic(attachmentFile);
					String attachmentPath = attachmentFile.getAbsolutePath();
					filePath = attachmentPath;

					String fileExtension = MetrixFileHelper.getFileType(filePath);
					Uri contentUri = FileProvider.getUriForFile(context, authority, new File(filePath));
					if (isImageFile(attachmentPath)) {
						try {
							// Try to permit viewing of the image asset in a native app
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filePath));
							intent.setDataAndType(Uri.parse(filePath), "image/*");
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(intent);
						} catch (Exception e) {
							LogManager.getInstance().error(e);
						}
					} else if (isVideoFile(attachmentPath)) {
						//*.mp4 file types only
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filePath));
							intent.setDataAndType(Uri.parse(filePath), "video/mp4");
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(intent);
						} catch (Exception e) {
							Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationVideo"), Toast.LENGTH_SHORT).show();
						}
					} else {
						context.grantUriPermission(context.getPackageName(), contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
						if (fileExtension.compareToIgnoreCase(AttachmentType.Pdf.toString()) == 0) {
							try {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(contentUri, "application/pdf");
								intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
								context.startActivity(intent);
							} catch (Exception e) {
								Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationPDF"), Toast.LENGTH_SHORT).show();
							}
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Xls.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Xlsx.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Csv.toString()) == 0) {
							try {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(contentUri, "application/vnd.ms-excel");
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
								context.startActivity(intent);
							} catch (Exception e) {
								Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationExcel"), Toast.LENGTH_SHORT).show();
							}
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Doc.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Docx.toString()) == 0) {
							try {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(contentUri, "application/msword");
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
								context.startActivity(intent);
							} catch (Exception e) {
								Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationWord"), Toast.LENGTH_SHORT).show();
							}
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Pptx.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Ppt.toString()) == 0) {
							try {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(contentUri, "application/vnd.ms-powerpoint");
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
								context.startActivity(intent);
							} catch (Exception e) {
								Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationFileType", fileExtension), Toast.LENGTH_SHORT).show();
							}
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Txt.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Log.toString()) == 0) {
							try {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(contentUri, "text/plain");
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
								context.startActivity(intent);
							} catch (Exception e) {
								Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationText"), Toast.LENGTH_SHORT).show();
							}
						} else {
							//other than the stated attachment types, all the other file types will come to this section
							try {
								final MimeTypeMap map = MimeTypeMap.getSingleton();
								String type = map.getMimeTypeFromExtension(fileExtension);
								if (MetrixStringHelper.isNullOrEmpty(type))
									type = "application/*";
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(contentUri, type);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
								context.startActivity(intent);
							} catch (Exception e) {
								Toast.makeText(context, AndroidResourceHelper.getMessage("NoApplicationFileType", fileExtension), Toast.LENGTH_SHORT).show();
							}
						}
					}
				}
			}
		}
	}

	//--------------------------------------------------------------------
	//DebriefTaskAttachment -> preview

	/**
	 * load attachments with specific size(height X width).
	 */
	protected static Bitmap loadAttachmentBitmap(Context context, String filePath, HashMap<String, Object> resourceCache) throws Exception {
		Bitmap bitmap = null;
		Resources res = context.getResources();

		if (!MetrixStringHelper.isNullOrEmpty(MetrixFileHelper.getAttachmentValidPath(filePath))) {
			File attachmentFile = new File(filePath);
			if (attachmentFile.exists()) {
				String fileExtension = MetrixFileHelper.getFileType(filePath);
				if (!MetrixStringHelper.isNullOrEmpty(fileExtension)) {
					if (isImageFile(filePath)) {
						bitmap = getAttachmentImageBitmap(context, attachmentFile);
					} else if (isVideoFile(filePath)) {
						bitmap = getAttachmentVideoBitmap(context, attachmentFile);
					} else {
						if (fileExtension.compareToIgnoreCase(AttachmentType.Pdf.toString()) == 0) {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.pdf_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Xls.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Xlsx.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Csv.toString()) == 0) {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.excel_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Doc.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Docx.toString()) == 0) {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.word_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Ppt.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Pptx.toString()) == 0) {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.tutorial")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Txt.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Log.toString()) == 0) {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.document")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.mp3.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Wav.toString()) == 0) {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.audio_file")));
						} else if (fileExtension.compareToIgnoreCase(AttachmentType.Htm.toString()) == 0 || fileExtension.compareToIgnoreCase(AttachmentType.Html.toString()) == 0) {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.country")));
						} else {
							bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.document_alt")));
						}
					}
				}
			} else {
				bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.error_alt")));
			}
		} else {
			bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.error_alt")));
		}

		// If somehow we get here and the bitmap is still null, generate an error placeholder.
		if (bitmap == null) {
			bitmap = attachmentDrawableToBitmap(context, res.getDrawable((int)resourceCache.get("R.drawable.error_alt")));
		}

		return bitmap;
	}

	protected static Bitmap loadEmptyPlaceholderBitmap(Context context, HashMap<String, Object> resourceCache) {
		return attachmentDrawableToBitmap(context, context.getResources().getDrawable((int)resourceCache.get("R.drawable.detach")));
	}

	protected static Bitmap loadOnDemandPlaceholderBitmap(Context context, HashMap<String, Object> resourceCache) {
		return MetrixAttachmentHelperBase.attachmentDrawableToBitmap(context, context.getResources().getDrawable((int)resourceCache.get("R.drawable.download")));
	}

	/**
	 * Get bitmap of the resource file without specifying width X height(ex: icon for PDF, XLS)
	 */
	public static Bitmap getAttachmentResourceBitmap(Context context, int resourceId) throws Exception{
		return decodeAttachmentResource(context, resourceId);
	}

	/**
	 * Get bitmap of the video file preview without specifying the height X width.
	 */
	private static Bitmap getAttachmentVideoBitmap(Context context, File file) throws Exception {
		return decodeAttachmentVideo(context, file);
	}

	/**
	 * Get bitmap of the image file without specifying the height X width.
	 */
	private static Bitmap getAttachmentImageBitmap(Context context, File file) throws Exception {
		Bitmap bm = null;

		ExifInterface ei = new ExifInterface(file.getAbsolutePath());
		int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

		switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				bm = MetrixUIHelper.RotateBitmap(decodeAttachmentFile(context, file), 90);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				bm = MetrixUIHelper.RotateBitmap(decodeAttachmentFile(context, file), 180);
				break;
			default:
				bm = decodeAttachmentFile(context, file);
				break;
		}

		return bm;
	}
}
