package com.metrix.architecture.attachment;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.metrix.architecture.utilities.MetrixPublicCache;

import java.util.HashMap;

public class AttachmentAdditionControl extends LinearLayout {
    private ImageButton mCameraBtn;
    private ImageButton mVideoBtn;
    private ImageButton mFileBtn;

    private static HashMap<String, Integer> resourceCache;

    public AttachmentAdditionControl(Context context) {
        super(context);
    }

    public AttachmentAdditionControl(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (resourceCache == null)
            resourceCache = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("AttachmentFieldResources");

        int layoutId = resourceCache.get("R.layout.attachment_addition_control");

        inflate(context, layoutId, this);

        mCameraBtn = findViewById(resourceCache.get("R.id.attachment_imagebtn_camera"));
        mVideoBtn = findViewById(resourceCache.get("R.id.attachment_imagebtn_video_camera"));
        mFileBtn = findViewById(resourceCache.get("R.id.attachment_imagebtn_file"));
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putInt("cameraVisible", mCameraBtn.getVisibility());
        bundle.putInt("videoVisible", mVideoBtn.getVisibility());
        bundle.putInt("fileVisible", mFileBtn.getVisibility());

        bundle.putBoolean("cameraEnabled", mCameraBtn.isEnabled());
        bundle.putBoolean("videoEnabled", mVideoBtn.isEnabled());
        bundle.putBoolean("fileEnabled", mFileBtn.isEnabled());

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle =  (Bundle)state;
            setCameraBtnVisibility(bundle.getInt("cameraVisible") == VISIBLE);
            setVideoBtnVisibility(bundle.getInt("videoVisible") == VISIBLE);
            setFileBtnVisibility(bundle.getInt("fileVisible") == VISIBLE);

            setCameraBtnEnabled(bundle.getBoolean("cameraEnabled"));
            setVideoBtnEnabled(bundle.getBoolean("videoEnabled"));
            setFileBtnEnabled(bundle.getBoolean("fileEnabled"));

            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    public boolean isCameraBtnVisible() {
        return mCameraBtn.getVisibility() == VISIBLE;
    }
    public void setCameraBtnVisibility(boolean isVisible) { mCameraBtn.setVisibility(isVisible ? VISIBLE : GONE); }

    public boolean isVideoBtnVisible() {
        return mVideoBtn.getVisibility() == VISIBLE;
    }
    public void setVideoBtnVisibility(boolean isVisible) { mVideoBtn.setVisibility(isVisible ? VISIBLE : GONE); }

    public boolean isFileBtnVisible() {
        return mFileBtn.getVisibility() == VISIBLE;
    }
    public void setFileBtnVisibility(boolean isVisible) { mFileBtn.setVisibility(isVisible ? VISIBLE : GONE); }

    public boolean isCameraBtnEnabled() {
        return mCameraBtn.isEnabled();
    }
    public void setCameraBtnEnabled(boolean isEnabled) { mCameraBtn.setEnabled(isEnabled); }

    public boolean isVideoBtnEnabled() {
        return mVideoBtn.isEnabled();
    }
    public void setVideoBtnEnabled(boolean isEnabled) {
        mVideoBtn.setEnabled(isEnabled);
    }

    public boolean isFileBtnEnabled() {
        return mFileBtn.isEnabled();
    }
    public void setFileBtnEnabled(boolean isEnabled) {
        mFileBtn.setEnabled(isEnabled);
    }

    public void setCameraBtnListener(OnClickListener listener) { mCameraBtn.setOnClickListener(listener); }
    public void setVideoBtnListener(OnClickListener listener) { mVideoBtn.setOnClickListener(listener); }
    public void setFileBtnListener(OnClickListener listener) { mFileBtn.setOnClickListener(listener); }
    public void setCameraControlId(String controlIdIn) { mCameraBtn.setContentDescription(controlIdIn); }
    public void setVideoControlId(String controlIdIn) { mVideoBtn.setContentDescription(controlIdIn); }
    public void setFileControlId(String controlIdIn) { mFileBtn.setContentDescription(controlIdIn); }
}
