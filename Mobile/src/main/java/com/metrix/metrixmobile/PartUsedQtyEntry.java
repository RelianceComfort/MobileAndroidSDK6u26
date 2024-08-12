package com.metrix.metrixmobile;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * Created by RaWiLK on 6/3/2016.
 */
public class PartUsedQtyEntry {

    private Context mContext;
    private AlertDialog mDialog;
    protected ViewGroup mLayout;
    private String mPartId;
    private String mQtyOnHand;

    private OnAlertDialogPositiveButtonClickListner mOnAlertDialogPositiveButtonClickListner;

    public PartUsedQtyEntry(Context context, String partId, String qtyOnHand) {
        this.mContext = context;
        this.mPartId = partId;
        this.mQtyOnHand = qtyOnHand;
    }

    /**
     * Initialize the UI of the Dialog control
     */
    @SuppressLint("InflateParams")
    public void initDialog() {
        try {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = (ViewGroup) inflater.inflate(R.layout.part_used_entry, null);
            AndroidResourceHelper.setResourceValues(mLayout.findViewById(R.id.PartUsedPartIdLbl62df0a19), "PartId", false);
            AndroidResourceHelper.setResourceValues(mLayout.findViewById(R.id.PartUsedQtyOnHandLblbf0ad70d), "QtyOnHand", false);
            AndroidResourceHelper.setResourceValues(mLayout.findViewById(R.id.txtMinQtyRequired), "Quantity", false);

            mDialog = new AlertDialog.Builder(mContext)
                    //Way of automatically hiding of Dialog once you click on Positive button.
                    .setPositiveButton(AndroidResourceHelper.getMessage("Add"), null)
                    .setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), null).setView(mLayout)
                    .setTitle(AndroidResourceHelper.getMessage("PartUsedDialogTitle")).setMessage(AndroidResourceHelper.getMessage("PartUsedDialogMessage")).create();


            final TextView partIdTbx = (TextView) mLayout.findViewById(R.id.txtPartId);
            if (partIdTbx != null)
                partIdTbx.setText(mPartId);

            final TextView qtyOnHandTbx = (TextView) mLayout.findViewById(R.id.txtAvbQty);
            if (qtyOnHandTbx != null)
                qtyOnHandTbx.setText(mQtyOnHand);

            final EditText minQtyRequiredTbx = (EditText) mLayout.findViewById(R.id.txtMinQtyRequired);
            if (minQtyRequiredTbx != null) {
                minQtyRequiredTbx.setText("1");
                minQtyRequiredTbx.setSelection(1);
            }

            //Executed show() before accessing the buttons, otherwise the buttons will be null.
            mDialog.show();

            Button addButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (addButton != null) {
                addButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        double minQty = -1;
                        if (minQtyRequiredTbx != null) {
                            String strMinQty = minQtyRequiredTbx.getText().toString();
                            if (MetrixStringHelper.isNullOrEmpty(strMinQty)) {
                                Toast.makeText(mContext, AndroidResourceHelper.getMessage("PartUsedQuantityIsReq"), Toast.LENGTH_SHORT).show();
                                return;
                            } else {
                                minQty = Double.parseDouble(strMinQty);
                                if (minQty <= 0) {
                                    Toast.makeText(mContext, AndroidResourceHelper.getMessage("PartUsedQuantityIsReq"), Toast.LENGTH_SHORT).show();
                                    return;
                                } else {
                                    double qtyOnHand = -1;
                                    TextView txtQtyOnHand = (TextView) mLayout.findViewById(R.id.txtAvbQty);
                                    if (txtQtyOnHand != null) {
                                        String strQtyOnHand = txtQtyOnHand.getText().toString();
                                        try {
                                            qtyOnHand = Double.parseDouble(strQtyOnHand);
                                            if (minQty > qtyOnHand) {
                                                Toast.makeText(mContext, AndroidResourceHelper.getMessage("PartUsedEnteredQtyExceeds"), Toast.LENGTH_SHORT).show();
                                                return;
                                            } else {
                                                mDialog.dismiss();
                                            }
                                        } catch (Exception ex) {
                                            Toast.makeText(mContext, AndroidResourceHelper.getMessage("GeneralErrorOccurred"), Toast.LENGTH_SHORT).show();
                                            LogManager.getInstance().error(ex);
                                            return;
                                        }
                                    }
                                }
                            }
                        }

                        if (mOnAlertDialogPositiveButtonClickListner != null) {
                            mOnAlertDialogPositiveButtonClickListner.OnAlertDialogPositiveButtonClick(v, String.valueOf(minQty));
                        }
                    }
                });
            }

            Button cancelButton = mDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (cancelButton != null) {
                cancelButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                    }
                });
            }

        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
        }
    }

    public void setOnAlertDialogPositiveButtonClickListner(OnAlertDialogPositiveButtonClickListner onAlertDialogPositiveButtonClickListner) {
        this.mOnAlertDialogPositiveButtonClickListner = onAlertDialogPositiveButtonClickListner;
    }

    public interface OnAlertDialogPositiveButtonClickListner {
        void OnAlertDialogPositiveButtonClick(View view, String qty);
    }
}
