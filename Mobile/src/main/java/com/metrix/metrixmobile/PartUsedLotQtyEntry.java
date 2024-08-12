package com.metrix.metrixmobile;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by RaWiLK on 6/15/2016.
 */
public class PartUsedLotQtyEntry {

    private Context mContext;
    private AlertDialog mDialog;
    protected ViewGroup mLayout;
    private LinearLayout mContent;
    protected ScrollView mScrollView;
    private List<HashMap<String, String>> mLotList;

    private OnPartUsedLotAlertDialogPositiveButtonClickListner mPartUsedLotOnAlertDialogPositiveButtonClickListner;

    public PartUsedLotQtyEntry(Context context, List<HashMap<String, String>> lotList) {
        this.mContext = context;
        this.mLotList = lotList;
    }

    /**
     * Initialize the UI of the Dialog control
     */
    @SuppressLint("InflateParams")
    public void initDialog() {
        try {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = (ViewGroup) inflater.inflate(R.layout.part_used_lot_entry, null);

            mScrollView = new ScrollView(mContext);
            mScrollView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.MATCH_PARENT));

            mContent = new LinearLayout(mContext);
            mContent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(30, 0, 30, 0);
            mContent.setOrientation(LinearLayout.VERTICAL);
            mContent.setLayoutParams(params);

            mDialog = new AlertDialog.Builder(mContext)
                    //Way of automatically hiding of Dialog once you click on Positive button.
                    .setPositiveButton(AndroidResourceHelper.getMessage("Add"), null)
                    .setNegativeButton(AndroidResourceHelper.getMessage("Cancel"), null)
                    .setTitle(AndroidResourceHelper.getMessage("PartUsedDialogTitle")).setMessage(AndroidResourceHelper.getMessage("PartUsedEnterQtyForEachLot")).create();

            for (int x = 0; x < mLotList.size(); x++) {

                HashMap<String, String> item = mLotList.get(x);
                String lotId = item.get("stock_lot_table.lot_id");
                String qtyOnHand = item.get("stock_lot_table.qty_on_hand");

                LinearLayout lotMainLayout = new LinearLayout(mContext);
                lotMainLayout.setOrientation(LinearLayout.VERTICAL);
                params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(25, 15, 25, 15);
                lotMainLayout.setLayoutParams(params);

                LinearLayout lotIdLayout = new LinearLayout(mContext);
                lotIdLayout.setOrientation(LinearLayout.HORIZONTAL);
                lotIdLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                lotIdLayout.setPadding(10, 0, 10, 5);

                TextView lotIdLabel = new TextView(mContext);
                lotIdLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
                lotIdLabel.setText(AndroidResourceHelper.getMessage("LotId"));
                lotIdLayout.addView(lotIdLabel, 0);

                TextView lotIdTextView = new TextView(mContext);
                lotIdTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
                lotIdTextView.setText(lotId);
                lotIdLayout.addView(lotIdTextView, 1);

                lotMainLayout.addView(lotIdLayout, 0);

                LinearLayout lotQtyOnHandLayout = new LinearLayout(mContext);
                lotQtyOnHandLayout.setOrientation(LinearLayout.HORIZONTAL);
                lotQtyOnHandLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                lotQtyOnHandLayout.setPadding(10, 0, 10, 5);

                TextView lotQtyOnHandLabel = new TextView(mContext);
                lotQtyOnHandLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
                lotQtyOnHandLabel.setText(AndroidResourceHelper.getMessage("PartUsedQtyOnHandLbl"));
                lotQtyOnHandLayout.addView(lotQtyOnHandLabel, 0);

                TextView lotQtyOnHandTextView = new TextView(mContext);
                lotQtyOnHandTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
                lotQtyOnHandTextView.setText(qtyOnHand);
                lotQtyOnHandLayout.addView(lotQtyOnHandTextView, 1);

                lotMainLayout.addView(lotQtyOnHandLayout, 1);

                EditText lotQtyRequiredEditText = new EditText(mContext);
                lotQtyRequiredEditText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                lotQtyRequiredEditText.setTag(lotId);
                lotQtyRequiredEditText.setHint(AndroidResourceHelper.getMessage("Quantity"));
                lotQtyRequiredEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                lotMainLayout.addView(lotQtyRequiredEditText, 2);

                mContent.addView(lotMainLayout, x);
            }

            //Executed show() before accessing the buttons, otherwise the buttons will be null.
            mScrollView.addView(mContent);
            mDialog.setView(mScrollView);
            mDialog.show();

            WindowManager windowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
            int height = windowManager.getDefaultDisplay().getHeight();
            int width = windowManager.getDefaultDisplay().getWidth();
            mDialog.getWindow().setLayout((width - 50), (int)(height * 0.9));

            Button addButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (addButton != null) {
                addButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        HashMap<String, String> item = null;
                        StringBuilder errorMessageBuilder = new StringBuilder();
                        List<HashMap<String, String>> lotListWithQty = new ArrayList<HashMap<String, String>>();
                        boolean errorFound = false;
                        int emptyCount = 0;

                        for (int x = 0; x < mLotList.size(); x++) {

                            item = mLotList.get(x);
                            String lotId = item.get("stock_lot_table.lot_id");
                            String strQtyOnHand = item.get("stock_lot_table.qty_on_hand");

                            long minQty = -1;
                            long qtyOnHand;
                            EditText minQtyRequiredTbx = (EditText) mContent.findViewWithTag(lotId);

                            if (minQtyRequiredTbx != null) {
                                String strMinQty = minQtyRequiredTbx.getText().toString();
                                if (!MetrixStringHelper.isNullOrEmpty(strMinQty)) {
                                    minQty = Long.parseLong(strMinQty);

                                    try {
                                        qtyOnHand = Long.parseLong(strQtyOnHand);
                                        if (minQty > qtyOnHand) {
                                            errorMessageBuilder.append(AndroidResourceHelper.getMessage("PartUsedEnteredLotQtyExceeds", lotId));
                                            errorMessageBuilder.append("\n");
                                            errorFound = true;
                                        } else {
                                            item.put("MinQty", String.valueOf(minQty));
                                            lotListWithQty.add(item);
                                        }
                                    } catch (Exception ex) {
                                        Toast.makeText(mContext, AndroidResourceHelper.getMessage("GeneralErrorOccurred"), Toast.LENGTH_SHORT).show();
                                        LogManager.getInstance().error(ex);
                                        return;
                                    }
                                }
                                else
                                    emptyCount = emptyCount + 1;
                            }
                        }

                        if(emptyCount == mLotList.size())
                        {
                            Toast.makeText(mContext, AndroidResourceHelper.getMessage("PrtUsdEnterQtyForAtLeastOneLot"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (errorFound) {
                            Toast.makeText(mContext, AndroidResourceHelper.getMessage(errorMessageBuilder.toString()), Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            mDialog.dismiss();
                        }

                        if (mPartUsedLotOnAlertDialogPositiveButtonClickListner != null) {
                            mPartUsedLotOnAlertDialogPositiveButtonClickListner.OnPartUsedLotAlertDialogPositiveButtonClick(v, lotListWithQty);
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

    public void setOnPartUsedLotAlertDialogPositiveButtonClickListner(OnPartUsedLotAlertDialogPositiveButtonClickListner onPartUsedLotAlertDialogPositiveButtonClickListner) {
        this.mPartUsedLotOnAlertDialogPositiveButtonClickListner = onPartUsedLotAlertDialogPositiveButtonClickListner;
    }

    public interface OnPartUsedLotAlertDialogPositiveButtonClickListner {
        void OnPartUsedLotAlertDialogPositiveButtonClick(View view, List<HashMap<String, String>> list);
    }
}

