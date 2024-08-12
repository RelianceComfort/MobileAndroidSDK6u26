package com.metrix.metrixmobile;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;

import java.io.File;

public class SignaturePad extends AppCompatActivity implements View.OnClickListener {
    private com.github.gcacace.signaturepad.views.SignaturePad signaturePad;
    private FloatingActionButton acceptBtn;
    private FloatingActionButton[] fabs;

    public static final String SIGNATURE_LOCATION = "SIGNATURE_LOCATION";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signature_pad);
        acceptBtn = findViewById(R.id.acceptBtn);
        final FloatingActionButton clearBtn = findViewById(R.id.clearBtn);
        final FloatingActionButton cancelBtn = findViewById(R.id.cancelBtn);

        acceptBtn.setEnabled(false);
        acceptBtn.setOnClickListener(this);
        clearBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);

        fabs = new FloatingActionButton[]{acceptBtn, clearBtn, cancelBtn};
        applyThemeToFABs();

        signaturePad = findViewById(R.id.signaturePad);
        signaturePad.setOnSignedListener(new com.github.gcacace.signaturepad.views.SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
                hideFABs();
            }

            @Override
            public void onSigned() {
                showFABs();
                acceptBtn.setEnabled(true);
            }

            @Override
            public void onClear() {
                acceptBtn.setEnabled(false);
            }
        });

        AndroidResourceHelper.setResourceValues(findViewById(R.id.tvSignatureLabel), "PlaceSignatureAboveLine");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.acceptBtn:
                signatureCompleted();
                break;

            case R.id.clearBtn:
                signaturePad.clear();
                break;

            case R.id.cancelBtn:
                if (signaturePad.isEmpty()) {
                    finish();
                } else {
                    new AlertDialog.Builder(v.getContext())
                            .setTitle(AndroidResourceHelper.getMessage("LeaveSignatureScreenTitle"))
                            .setMessage(AndroidResourceHelper.getMessage("LeaveSignatureScreenMessage"))
                            .setPositiveButton(AndroidResourceHelper.getMessage("Leave"), (d, p) -> finish())
                            .setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), null)
                            .show();
                }
                break;
        }
    }

    private void hideFABs() {
        for (FloatingActionButton fab : fabs) {
            fab.hide();
        }
    }

    private void showFABs() {
        for (FloatingActionButton fab : fabs) {
            fab.show();
        }
    }

    private void applyThemeToFABs() {
        String buttonColorStr = MetrixSkinManager.getPrimaryColor();
        if (!MetrixStringHelper.isNullOrEmpty(buttonColorStr) && buttonColorStr.charAt(0) != '#')
            buttonColorStr = "#" + buttonColorStr;

        final int buttonColor = MetrixStringHelper.isNullOrEmpty(buttonColorStr) ? ContextCompat.getColor(this, R.color.IFSPurple) : Color.parseColor(buttonColorStr);
        final int iconColor = ColorUtils.calculateLuminance(buttonColor) < 0.5 ? Color.WHITE : Color.BLACK;

        for (FloatingActionButton fab : fabs) {
            fab.setBackgroundTintList(ColorStateList.valueOf(buttonColor));
            fab.setColorFilter(iconColor);
            fab.setRippleColor(iconColor);
        }
    }

    private void signatureCompleted() {
        final Bitmap signature = signaturePad.getTransparentSignatureBitmap(true);

        // Delete any existing temp signature file
        final String filePath = MetrixAttachmentManager.getInstance().getAttachmentPath();
        final String fileName = filePath + "/" + "TempSignature.png";
        final File signatureFile = new File(fileName);
        if (signatureFile.exists())
            signatureFile.delete();

        // Save signature to a temp local file and pass it to the caller
        if (MetrixAttachmentManager.getInstance().canFileBeSuccessfullySaved(signature.getByteCount())) {
            MetrixAttachmentManager.getInstance().savePicture(fileName, signature, this);

            final Intent intent = new Intent();
            intent.putExtra(SIGNATURE_LOCATION, fileName);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else {
            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("NotEnoughFreeSpace"));
        }
    }

    public static void openForResult(@NonNull Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, SignaturePad.class);
        activity.startActivityForResult(intent, requestCode);
    }
}
