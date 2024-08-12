package com.metrix.metrixmobile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

//import com.aviary.android.feather.library.Constants;
//import com.aviary.android.feather.sdk.FeatherActivity;
import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.ui.widget.MetrixCarouselView;
import com.metrix.architecture.ui.widget.MetrixGalleryView;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixAttachmentUtil;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.system.DebriefActivity;

public class DebriefTaskAttachmentActivity extends DebriefActivity {
	
	protected Class<?> mParentActivity;
	
	private static String selectedFilePath;
	private static String attachmentId;
	private static String metrixRowId;
	private static String attachmentName;
	private static String fileExtension;
	private static String mobilePath;
	

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.up:
			Intent intent = null;
			if (mParentActivity != null) {
				intent = MetrixActivityHelper.createActivityIntent(this, mParentActivity);
			} else {
				intent = MetrixActivityHelper.createActivityIntent(this, JobList.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
			}
			MetrixActivityHelper.startNewActivity(this, intent);
			break;
		default:
			super.onClick(v);
		}
	}
	
	protected List<HashMap<String, String>> getAttachmentData() {

		try{
			StringBuilder query = new StringBuilder();
			query.append("select task_attachment.metrix_row_id, attachment.attachment_description, attachment.attachment_name, ");
			query.append("attachment.mobile_path, attachment.created_dttm, attachment.file_extension, attachment.metrix_row_id, attachment.attachment_id, attachment.on_demand ");
			query.append(" from task_attachment, attachment where (task_attachment.attachment_id = attachment.attachment_id and task_attachment.task_id = "
					+ MetrixCurrentKeysHelper.getKeyValue("task", "task_id") + ")");
			//get rid of SIGNATURE attachments
			query.append(" and (attachment.internal_type is null or (attachment.internal_type is not null and attachment.internal_type <> 'SIGNATURE')) ");
	
			MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
	
			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}
	
			String[] mFrom = new String[] { "task_attachment.metrix_row_id",
					"attachment.attachment_description",
					"attachment.attachment_name", "attachment.mobile_path",
					"attachment.created_dttm", "attachment.file_extension", "attachment.metrix_row_id", "attachment.attachment_id", "attachment.on_demand" };
	
			List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
	
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> row = new HashMap<String, String>();
	
				row.put(mFrom[0], cursor.getString(0));
				row.put(mFrom[1], cursor.getString(1));
				row.put(mFrom[2], cursor.getString(2));
				row.put(mFrom[3], cursor.getString(3));
				row.put(mFrom[4], cursor.getString(4));
				row.put(mFrom[5], cursor.getString(5));
				row.put(mFrom[6], cursor.getString(6));
				row.put(mFrom[7], cursor.getString(7));
				row.put(mFrom[8], cursor.getString(8));

				table.add(row);
				cursor.moveToNext();
			}
			cursor.close();
	
			return table;
			}
		catch(Exception e){
			LogManager.getInstance(this).error(e);
		}
		return null;
	}
	
	protected boolean deleteAction(HashMap<String, String> selectedItem, View view) {
		try {
			String metrixRowId = selectedItem.get("task_attachment.metrix_row_id");
			String attachmentId = MetrixDatabaseManager.getFieldStringValue("task_attachment", "attachment_id", "metrix_row_id=" + metrixRowId);
			String taskId = MetrixDatabaseManager.getFieldStringValue("task_attachment", "task_id", "metrix_row_id=" + metrixRowId);
	
			Hashtable<String, String> primaryKeys = new Hashtable<String, String>();
			primaryKeys.put("attachment_id", attachmentId);
			primaryKeys.put("task_id", taskId);
	
			if (MetrixUpdateManager.delete(this, "task_attachment", metrixRowId, primaryKeys, AndroidResourceHelper.getMessage("Attachment"), MetrixTransaction.getTransaction("task", "task_id"))) {
				if (view instanceof MetrixGalleryView)
					((MetrixGalleryView) view).removeItem(selectedItem);
				if (view instanceof MetrixCarouselView)
					((MetrixCarouselView) view).removeItem(selectedItem);
			}
			
			return true;
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
		return false;
	}
	
	protected boolean modifyAction(HashMap<String, String> selectedItem){
		
		try{
			
			selectedFilePath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + selectedItem.get("attachment.attachment_name");
			metrixRowId = selectedItem.get("attachment.metrix_row_id");
			attachmentId = selectedItem.get("attachment.attachment_id");
			attachmentName = selectedItem.get("attachment.attachment_name");
			mobilePath = selectedItem.get("attachment.mobile_path");
			fileExtension = selectedItem.get("attachment.file_extension");
			
			if (MetrixAttachmentHelper.checkImageFile(selectedFilePath)) {
				Intent dsPhotoEditorIntent = new Intent(this, DsPhotoEditorActivity.class);
				dsPhotoEditorIntent.setData(Uri.parse("file://" + selectedFilePath));

				// This is optional. By providing an output directory, the edited photo
				// will be saved in the specified folder on your device's external storage;
				// If this is omitted, the edited photo will be saved to a folder
				// named "DS_Photo_Editor" by default.
				// dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "YOUR_OUTPUT_IMAGE_FOLDER");

				// Optional customization: hide some tools you don't need as below
				int[] toolsToHide = {DsPhotoEditorActivity.TOOL_FILTER, DsPhotoEditorActivity.TOOL_FRAME,
						DsPhotoEditorActivity.TOOL_ROUND, DsPhotoEditorActivity.TOOL_EXPOSURE,
						DsPhotoEditorActivity.TOOL_CONTRAST, DsPhotoEditorActivity.TOOL_VIGNETTE,
						DsPhotoEditorActivity.TOOL_CROP, DsPhotoEditorActivity.TOOL_ORIENTATION,
						DsPhotoEditorActivity.TOOL_SATURATION, DsPhotoEditorActivity.TOOL_SHARPNESS,
						DsPhotoEditorActivity.TOOL_WARMTH, DsPhotoEditorActivity.TOOL_PIXELATE,
						DsPhotoEditorActivity.TOOL_STICKER};
				dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE, toolsToHide);

				startActivityForResult(dsPhotoEditorIntent, 200);
			}
			else {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("UnsupportedFileType"));
			}
		}
		catch(Exception e){
			LogManager.getInstance(this).error(e);
		}
		
		return true;

	}
	
	protected boolean modifyAction(String imagePath){
		
		try{
			selectedFilePath = imagePath;
			
			if (MetrixAttachmentHelper.checkImageFile(selectedFilePath)) {
				Intent dsPhotoEditorIntent = new Intent(this, DsPhotoEditorActivity.class);
				dsPhotoEditorIntent.setData(Uri.parse("file://" + selectedFilePath));

				// This is optional. By providing an output directory, the edited photo
				// will be saved in the specified folder on your device's external storage;
				// If this is omitted, the edited photo will be saved to a folder
				// named "DS_Photo_Editor" by default.
				// dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "YOUR_OUTPUT_IMAGE_FOLDER");

				// Optional customization: hide some tools you don't need as below
				int[] toolsToHide = {DsPhotoEditorActivity.TOOL_FILTER, DsPhotoEditorActivity.TOOL_FRAME,
						DsPhotoEditorActivity.TOOL_ROUND, DsPhotoEditorActivity.TOOL_EXPOSURE,
						DsPhotoEditorActivity.TOOL_CONTRAST, DsPhotoEditorActivity.TOOL_VIGNETTE,
						DsPhotoEditorActivity.TOOL_CROP, DsPhotoEditorActivity.TOOL_ORIENTATION,
						DsPhotoEditorActivity.TOOL_SATURATION, DsPhotoEditorActivity.TOOL_SHARPNESS,
						DsPhotoEditorActivity.TOOL_WARMTH, DsPhotoEditorActivity.TOOL_PIXELATE,
						DsPhotoEditorActivity.TOOL_STICKER};
				dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE, toolsToHide);

				startActivityForResult(dsPhotoEditorIntent, 200);
			}
			else {
				MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("UnsupportedFileType"));
			}
		}
		catch(Exception e){
			LogManager.getInstance(this).error(e);
		}
		
		return true;

	}
	
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data ) {
		try{
		    if( resultCode == RESULT_OK ) {
				switch (requestCode) {
					case 200:
						Uri outputUri = data.getData();

						if (outputUri != null) {
							savefile(outputUri);
						}

						break;
				}
		    }
		}
		catch(Exception e){
			LogManager.getInstance(this).error(e);
		}
	}
	
	private void savefile(Uri sourceURI) {

		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		String sourceFilename = "";

		try {
			final boolean isAndroidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
			if (isAndroidQ)
				sourceFilename = MetrixAttachmentUtil.getFilePathFromUri(getApplicationContext(),sourceURI);
			else
				sourceFilename = sourceURI.getPath();

			String destinationFilename = selectedFilePath;
			bis = new BufferedInputStream(new FileInputStream(sourceFilename));
			bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
			byte[] buf = new byte[1024];
			bis.read(buf);
			do {
				bos.write(buf);
			} while (bis.read(buf) != -1);
			
			MetrixSqlData attachmentData;
			DataField attachment_id;
			
			// The transaction should be created only if the file already sent to server and needs to change
			if(metrixRowId != null) {
				attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ metrixRowId);			
				attachmentId = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_id", "metrix_row_id="+ metrixRowId);
				attachment_id = new DataField("attachment_id", attachmentId);
				DataField metrix_row_id = new DataField("metrix_row_id", metrixRowId);
				attachmentData.dataFields.add(metrix_row_id);
									
				DataField attachment_name = new DataField("attachment_name", attachmentName);
				DataField file_extension = new DataField("file_extension", fileExtension);
				DataField mobile_path = new DataField("mobile_path", mobilePath);
	
				attachmentData.dataFields.add(attachment_id);			
				attachmentData.dataFields.add(attachment_name);
				attachmentData.dataFields.add(file_extension);
				attachmentData.dataFields.add(mobile_path);
				
				ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<MetrixSqlData>();
				attachmentTrans.add(attachmentData);
				
				MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
	
				boolean successful = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("Attachment"), this);
				if (!successful) {
					MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("DataErrorOnUpload"));
					return;
				}
			}
		} catch (IOException e) {
			LogManager.getInstance(this).error(e);
		} finally {
			try {
				if (bis != null)
					bis.close();
				if (bos != null)
					bos.close();
			} catch (IOException e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}
}
