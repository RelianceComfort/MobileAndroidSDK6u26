package com.metrix.architecture.attachment;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelperBase;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.HashMap;
import java.util.List;

public class FSMCarouselAdapter extends PagerAdapter {
    private HashMap<String, Object> resourceData;
    private Context contextInfo;
    private List<HashMap<String, String>> itemList;
    private final String pageNumberPlaceHolder;
    private static HashMap<String, Object> resourceCache;
    private FSMAttachmentFullScreen parentActivity;

    public FSMCarouselAdapter(Context context, List<HashMap<String, String>> list, FSMAttachmentFullScreen fullScreen) {
        this.contextInfo = context;
        this.itemList = list;
        this.pageNumberPlaceHolder = AndroidResourceHelper.getMessage("PageNumber");
        resourceData = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("FSMCarouselResources");
        try {
            parentActivity = fullScreen;
            if (resourceCache == null)
                resourceCache = (HashMap<String, Object>) MetrixPublicCache.instance.getItem("AttachmentHelperResources");
        } catch(Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    public Object getItem(int position){
        return itemList.get(position);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, final int position) {
        try {
            LayoutInflater inflater = (LayoutInflater) contextInfo.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View convertView = inflater.inflate((int)resourceData.get("R.layout.aapi_carousel_item"), null);

            ImageView filePreview = (ImageView) convertView.findViewById((int)resourceData.get("R.id.file_preview"));
            TextView tvAttachmentDate = (TextView) convertView.findViewById((int)resourceData.get("R.id.txtview_attachment_date"));
            TextView tvAttachmentDescription = (TextView) convertView.findViewById((int)resourceData.get("R.id.txtview_attachment_description"));
            TextView tvAttachmentName = (TextView) convertView.findViewById((int)resourceData.get("R.id.txtview_attachment_name"));
            TextView tvAttachmentNumber = (TextView) convertView.findViewById((int)resourceData.get("R.id.txtview_attachment_number"));

            HashMap<String, String> item = itemList.get(position);
            if (item == null)
                return convertView;

            String attachmentName = item.get("attachment.attachment_name");
            String attachmentDescription = item.get("attachment.attachment_description");
            String attachmentCreationDate = item.get("attachment.created_dttm");
            String attachmentOnDemand = item.get("attachment.on_demand");
            boolean isOnDemand = !MetrixStringHelper.isNullOrEmpty(attachmentOnDemand) && attachmentOnDemand.equalsIgnoreCase("Y");
            final String attachmentPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + attachmentName;

            filePreview.setOnClickListener(v -> {

                if(isOnDemand && parentActivity != null && !MetrixAttachmentHelper.isAttachmentExists(attachmentPath)) {
                    parentActivity.downloadOnDemandImage(item);
                } else {
                    final String buildConfigAppID = (String)resourceData.get("BuildConfig.APPLICATION_ID");
                    MetrixAttachmentHelper.showAttachment(contextInfo, attachmentPath, buildConfigAppID+".provider");
                }
            });

            tvAttachmentDescription.setText(attachmentDescription);
            tvAttachmentName.setText(attachmentName);

            if (itemList.size() > 1)
                tvAttachmentNumber.setText(AndroidResourceHelper.formatMessage(pageNumberPlaceHolder, "PageNumber", (position + 1), itemList.size()));
            else
                tvAttachmentNumber.setVisibility(View.GONE);    // Don't display pagination if there's only one result

            if (!MetrixStringHelper.isNullOrEmpty(attachmentCreationDate))
                tvAttachmentDate.setText(MetrixDateTimeHelper.convertDateTimeFromDBToUI(attachmentCreationDate));

            if(isOnDemand && !MetrixAttachmentHelper.isAttachmentExists(attachmentPath)) {
                Resources res = contextInfo.getResources();
                Bitmap bitmap = MetrixAttachmentHelperBase.attachmentDrawableToBitmap(contextInfo, res.getDrawable((int)resourceCache.get("R.drawable.download")));
                filePreview.setImageBitmap(bitmap);
            } else {
                MetrixAttachmentHelper.showAttachmentFullScreenPreview(contextInfo, attachmentPath, filePreview);
            }

            collection.addView(convertView, 0);
            return convertView;
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        return null;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        View currentView = collection.getChildAt(0);
        if (currentView instanceof ImageView) {
            Drawable drawable = ((ImageView) view).getDrawable();
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmapDrawable.getBitmap().recycle();
            }
        }
        collection.removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(View container, Object obj) {
        return container == obj;
    }
}
