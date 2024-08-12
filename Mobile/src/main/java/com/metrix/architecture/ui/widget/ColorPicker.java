package com.metrix.architecture.ui.widget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.metrixmobile.R;

public class ColorPicker extends Activity {
	private ViewGroup mParentLayout;
	private EditText mValueEditText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.color_picker);

		mValueEditText = (EditText) MetrixPublicCache.instance.getItem("colorPickerEditText");
		mParentLayout = (ViewGroup) MetrixPublicCache.instance.getItem("colorPickerParentLayout");
		
		GridView colorGrid = (GridView) findViewById(R.id.color_picker_grid);
		colorGrid.setAdapter(new ColorPickerAdapter(getBaseContext()));
		colorGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				boolean valueSet = true;
				try {
					ColorDrawable drawable = (ColorDrawable) view.getBackground();
					int colorInUse = drawable.getColor();
			        String colorString = String.format("%06X", (0xFFFFFF & colorInUse));
			        MetrixControlAssistant.setValue(mValueEditText.getId(), mParentLayout, colorString);
				} catch (Exception e) {
					valueSet = false;
					LogManager.getInstance().error(e);
				}
				
				if (valueSet)
					setResult(RESULT_OK, getIntent());
				else
					setResult(RESULT_CANCELED, getIntent());
				ColorPicker.this.finish();
			}
		});
	}
	
	public static class ColorPickerAdapter extends BaseAdapter {
		private Context context;
		private List<Integer> colorItems = new ArrayList<Integer>();
		private static final float ICON_WIDTH_DP = 32.0f;
		int gridColumnWidth;

		public ColorPickerAdapter(Context context) {
			this.context = context;

			// defines the width of each color square
			final float scale = context.getResources().getDisplayMetrics().density;
			gridColumnWidth = (int) (ICON_WIDTH_DP * scale + 0.5f);

			int colorCount = 96;
			int step = 256 / (colorCount / 6);
			int red = 0;
			int green = 0;
			int blue = 0;

			for (red = 255, green = 0, blue = 0; green <= 255; green += step)
				colorItems.add(Color.rgb(red, green, blue));

			for (red = 255, green = 255, blue = 0; red >= 0; red -= step)
				colorItems.add(Color.rgb(red, green, blue));

			for (red = 0, green = 255, blue = 0; blue <= 255; blue += step)
				colorItems.add(Color.rgb(red, green, blue));

			for (red = 0, green = 255, blue = 255; green >= 0; green -= step)
				colorItems.add(Color.rgb(red, green, blue));

			for (red = 0, green = 0, blue = 255; red <= 255; red += step)
				colorItems.add(Color.rgb(red, green, blue));

			for (red = 255, green = 0, blue = 255; blue >= 0; blue -= step)
				colorItems.add(Color.rgb(red, green, blue));

			for (int i = 255; i >= 0; i -= 11) {
				if (i <= 10) i = 0;		// make sure last option is true black
				colorItems.add(Color.rgb(i, i, i));
			}
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;

			if (convertView == null) {
				imageView = new ImageView(context);
				imageView.setLayoutParams(new GridView.LayoutParams(gridColumnWidth, gridColumnWidth));

			} else {
				imageView = (ImageView) convertView;
			}

			imageView.setBackgroundColor(colorItems.get(position));
			imageView.setId(position);

			return imageView;
		}

		public int getCount() {
			return colorItems.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}
	}	
}
