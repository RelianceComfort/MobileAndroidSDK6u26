package com.metrix.architecture.ui.widget;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.viewpager.widget.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.BuildConfig;
import com.metrix.metrixmobile.R;


public class MetrixCarouselAdapter extends PagerAdapter {
	
	private Context mContext;
	private List<HashMap<String, String>> mList;
	private OnMetrixCarouselAdapterItemLongClickListner mMetrixCarouselAdapterItemLongClickListner;
	private MetrixCarouselView.OnMetrixCarouselViewItemClickListner mMetrixCarouselAdapterItemClickListner;
	private final String pageNumberPlaceHolder;
	
	public MetrixCarouselAdapter(Context context, List<HashMap<String, String>> list)
	{
		this.mContext = context;
		this.mList = list;
		this.pageNumberPlaceHolder = AndroidResourceHelper.getMessage("PageNumber");
	}
	
	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}
	
    @Override
    public int getCount() {
           return mList.size();
    }
    
    public Object getItem(int position){
    	return mList.get(position);
    }

	@Override
	public Object instantiateItem(ViewGroup collection, final int position) {

		try {

			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			final View convertView = inflater.inflate(
					R.layout.metrix_carousel_fullscreen_item, null);

			ImageView imageViewAttachment = (ImageView) convertView
					.findViewById(R.id.imgview_attachment);
			TextView textViewAttachmentNumber = (TextView) convertView
					.findViewById(R.id.txtview_attachment_number);
			TextView textViewAttachmentDescription = (TextView) convertView
					.findViewById(R.id.txtview_attachment_description);
			TextView textViewAttachmentDate = (TextView) convertView
					.findViewById(R.id.txtview_attachment_date);
			ImageView imageViewVideoIcon = (ImageView) convertView
					.findViewById(R.id.imageview_video_icon);

			HashMap<String, String> item = mList.get(position);
			if (item == null)
				return convertView;
			
			final String attachmentPath = MetrixAttachmentManager.getInstance().getAttachmentPath() + "/" + item.get("attachment.attachment_name");
			String attachmentDescription = item.get("attachment.attachment_description");
			if(MetrixStringHelper.isNullOrEmpty(attachmentDescription))
				attachmentDescription = item.get("attachment.attachment_name");
			String attachmentCreationDate = item.get("attachment.created_dttm");
			String onDemand = item.get("attachment.on_demand");

			convertView.setOnLongClickListener(new View.OnLongClickListener() {

				@Override
				public boolean onLongClick(View view) {

					if (mMetrixCarouselAdapterItemLongClickListner != null) {
						mMetrixCarouselAdapterItemLongClickListner
								.onMetrixCarouselAdapterItemLongClick(position);
					}
					return true;
				}

			});

			convertView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mMetrixCarouselAdapterItemClickListner != null) {
						mMetrixCarouselAdapterItemClickListner.onMetrixCarouselViewItemClick(position);
					}
					MetrixAttachmentHelper.showAttachment(mContext,
							attachmentPath, BuildConfig.APPLICATION_ID+".provider");
				}
			});

			textViewAttachmentNumber.setText(AndroidResourceHelper.formatMessage(pageNumberPlaceHolder, "PageNumber", (position + 1), mList.size()));
			textViewAttachmentDescription.setText(attachmentDescription);

			if (!MetrixStringHelper.isNullOrEmpty(attachmentCreationDate)) {
				textViewAttachmentDate
						.setText(MetrixDateTimeHelper.convertDateTimeFromDBToUI(
								attachmentCreationDate, new SimpleDateFormat("EEEE, MMM d, yyyy",
										Locale.getDefault())));
			}

			if(!MetrixStringHelper.isNullOrEmpty(onDemand) && onDemand.equalsIgnoreCase("Y")) {
				Bitmap bitmap = MetrixAttachmentHelper.getAttachmentResourceBitmap(this.mContext, R.drawable.download);
				imageViewAttachment.setImageBitmap(bitmap);
			}
			else {
				MetrixAttachmentHelper.showAttachmentFullScreenPreview(mContext,
						attachmentPath, imageViewAttachment);
			}

			MetrixAttachmentHelper.setVideoIcon(imageViewVideoIcon,
					attachmentPath, true);

			collection.addView(convertView, 0);
			return convertView;

		} catch (Exception e) {
			Toast.makeText(mContext, e.getLocalizedMessage(),
					Toast.LENGTH_SHORT).show();
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
    
    public boolean remove(HashMap<String, String> item) {
		return mList.remove(item);
	}
    
    public interface OnMetrixCarouselAdapterItemLongClickListner {
		public void onMetrixCarouselAdapterItemLongClick(int position);
	}
    
    public void setMetrixCarouselAdapterItemLongClickListner(OnMetrixCarouselAdapterItemLongClickListner carouselAdapterItemLongClickListner) {
    	mMetrixCarouselAdapterItemLongClickListner = carouselAdapterItemLongClickListner;
	}

	public void setMetrixCarouselViewItemClickListner(MetrixCarouselView.OnMetrixCarouselViewItemClickListner viewItemClickListner) {
		mMetrixCarouselAdapterItemClickListner = viewItemClickListner;
	}
}