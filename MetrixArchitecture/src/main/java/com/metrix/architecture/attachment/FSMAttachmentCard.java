package com.metrix.architecture.attachment;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class FSMAttachmentCard extends AttachmentAPIBaseActivity implements View.OnClickListener {
    public FloatingActionButton mCustomSaveButton;
    public boolean mSaveDisabledViaConfiguration = false;

    private HashMap<String, Integer> resourceData;
    private FloatingActionButton cancelButton;
    private String metrixRowId;

    private boolean attachmentFieldIsEnabled = false;
    private boolean isFromList = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resourceData = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("FSMAttachmentCardResources");

        setContentView(resourceData.get("R.layout.aapi_attachment_card"));

        initialScreenSetup();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == resourceData.get("R.id.custom_save")) {
            if (scriptEventConsumesClick(this, "BUTTON_SAVE"))
                return;

            MetrixTransaction transactionInfo = AttachmentWidgetManager.getTransactionInfo();
            MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("Attachment"));
            if (saveResult == MetrixSaveResult.ERROR)
                return;

            finish();
        } else if (viewId == resourceData.get("R.id.cancel")) {
            finish();
        } else
            super.onClick(v);
    }

    @Override
    public void resetFABOffset() {
        try {
            // Poke the FAB rendering first
            hideFABs(mFABList);
            showFABs();

            // Now that FABs are showing, set the offset on mLayout
            mLayout.setPadding(mLayout.getPaddingLeft(), mLayout.getPaddingTop(), mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mCustomSaveButton = (FloatingActionButton) findViewById(resourceData.get("R.id.custom_save"));
        cancelButton = (FloatingActionButton) findViewById(resourceData.get("R.id.cancel"));

        mCustomSaveButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        // Add all buttons to mFABList, so that client scripting can show/hide them and the scrolling framework will still operate well
        mFABList.add(mCustomSaveButton);
        mFABList.add(cancelButton);

        completeFABSetup();
    }

    @Override
    protected void defineForm() {
        MetrixTableDef attachmentDef = new MetrixTableDef("attachment", MetrixTransactionTypes.UPDATE);
        attachmentDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, metrixRowId, double.class));
        this.mFormDef = new MetrixFormDef(attachmentDef);
    }

    private void initialScreenSetup() {
        boolean shouldFinishImmediately = true;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            metrixRowId = extras.getString("MetrixRowID");
            if (!MetrixStringHelper.isNullOrEmpty(metrixRowId))
                shouldFinishImmediately = false;

            isFromList = extras.getBoolean("isFromList");
            attachmentFieldIsEnabled = extras.getBoolean("attachmentFieldIsEnabled");
        }

        if (shouldFinishImmediately) {
            LogManager.getInstance().error("Metrix Row ID not populated.  Leaving FSM Attachment Card...");
            finish();
        }

        mCustomSaveButton = (FloatingActionButton) findViewById(resourceData.get("R.id.custom_save"));
        mSaveDisabledViaConfiguration = ((isFromList && AttachmentWidgetManager.getListAllowModify() == false) || (!isFromList && !attachmentFieldIsEnabled));
        if (mSaveDisabledViaConfiguration) {
            mCustomSaveButton.hide();
            mCustomSaveButton.setTag(MetrixClientScriptManager.HIDDEN_BY_SCRIPT);
        } else {
            mCustomSaveButton.show();
        }
    }
}
