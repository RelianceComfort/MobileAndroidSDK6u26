package com.metrix.metrixmobile;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.ui.widget.MetrixCarouselView;
import com.metrix.architecture.ui.widget.MetrixCarouselView.OnMetrixCarouselViewItemLongClickListner;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.PermissionHelper;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class QuoteQuoteAttachmentFullScreen extends QuoteQuoteAttachmentActivity implements OnMetrixCarouselViewItemLongClickListner {

	private MetrixCarouselView mMetrixCarouselView;
	private int selectedPosition = -1;
	private String attachmentName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.quote_quote_attachment_fullscreen);

		mLayout = (ViewGroup) findViewById(R.id.table_layout);
		mParentActivity = DebriefTaskAttachmentAdd.class;

		mMetrixCarouselView = (MetrixCarouselView) findViewById(R.id.attachment_pager);

		setListeners();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			selectedPosition = extras.getInt("position");
		}
	}

	public void onStart() {
		super.onStart();

		setupActionBar();
		populateCarousel();
	}

	@Override
	protected void onResume() {
		if (!PermissionHelper.checkPublicFilePermission(this)) {
			PermissionHelper.requestPublicFilePermission(this);
		} else {
			MetrixFileHelper.deletePublicFile(attachmentName);
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		if (PermissionHelper.checkPublicFilePermission(this)) {
			MetrixFileHelper.deletePublicFile(attachmentName);
		}

		super.onDestroy();
	}

	protected void setListeners() {
		if (!quoteIsAccepted()) {
			mMetrixCarouselView.setMetrixCarouselViewItemLongClickListner(this);
		}
	}


	private void populateCarousel() {

		List<HashMap<String, String>> data = getAttachmentData();
		if(data != null){

			mMetrixCarouselView.setDatasource(getApplicationContext(), data);
			mMetrixCarouselView.setCurrentItem(selectedPosition);
			attachmentName = data.get(selectedPosition).get("attachment.attachment_name");
		}

	}

	@Override
	public void onMetrixCarouselViewItemLongClick(final int position) {

		final HashMap<String, String> selectedItem = (HashMap<String, String>) mMetrixCarouselView.getItem(position);

		if(selectedItem != null){

			String attachmentPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + selectedItem.get("attachment.attachment_name");

			if(!MetrixStringHelper.isNullOrEmpty(attachmentPath)){

				File attachmentFile = new File(attachmentPath);
				if (attachmentFile.exists()) {

					boolean isImageFile = MetrixAttachmentHelper.checkImageFile(attachmentPath);

					DialogInterface.OnClickListener modifyListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							try {

								modifyAction(selectedItem);

							} catch (Exception e) {
								LogManager.getInstance().error(e);
							}
						}
					};

					DialogInterface.OnClickListener deleteListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							try {
															
								deleteAction(selectedItem, mMetrixCarouselView);
								
							} catch (Exception e) {
								LogManager.getInstance().error(e);
							}
						}
			
					};
					
					if(isImageFile)
						MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("AttachmentLCase"), modifyListener, deleteListener, this);
					else
						MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("AttachmentLCase"), deleteListener, null, this);
				}
			}
		}
		
	}
}
