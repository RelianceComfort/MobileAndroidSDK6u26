package com.metrix.architecture.ui.widget;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.R;

public class FileDialog extends AppCompatActivity implements SimpleRecyclerViewAdapter.OnItemClickListener {

	private static final String ITEM_KEY = "key";
	private static final String ITEM_IMAGE = "image";
	private static final String ROOT = "/";

	public static final String START_PATH = "START_PATH";
	public static final String RESULT_PATH = "RESULT_PATH";
	public static final String SELECTION_MODE = "SELECTION_MODE";

	private List<String> path = null;
	private TextView myPath;
	private EditText mFileName;
	private ArrayList<HashMap<String, Object>> mList;

	private Button selectButton;

	private LinearLayout layoutSelect;
	private LinearLayout layoutCreate;
	private InputMethodManager inputManager;
	private String parentPath;
	private String currentPath = ROOT;

	private int selectionMode = SelectionMode.MODE_CREATE;

	private File selectedFile;
	private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

	private RecyclerView recyclerView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED, getIntent());

		setContentView(com.metrix.metrixmobile.R.layout.file_dialog_main);
		myPath = (TextView) findViewById(com.metrix.metrixmobile.R.id.path);
		mFileName = (EditText) findViewById(com.metrix.metrixmobile.R.id.fdEditTextFile);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, 0);

		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		selectButton = (Button) findViewById(com.metrix.metrixmobile.R.id.fdButtonSelect);
		selectButton.setEnabled(false);
		selectButton.setVisibility(Button.GONE);
		selectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (selectedFile != null) {
					getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
					setResult(RESULT_OK, getIntent());
					
					String path = selectedFile.getPath().substring(0, selectedFile.getPath().lastIndexOf("/"));
					SettingsHelper.setLastFolder(getBaseContext(), path);
					finish();
				}
			}
		});

		final Button newButton = (Button) findViewById(com.metrix.metrixmobile.R.id.fdButtonNew);
		newButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setCreateVisible(v);

				mFileName.setText("");
				mFileName.requestFocus();
			}
		});

		selectionMode = getIntent().getIntExtra(SELECTION_MODE,
				SelectionMode.MODE_CREATE);
		if (selectionMode == SelectionMode.MODE_OPEN) {
			newButton.setEnabled(false);
		}

		layoutSelect = (LinearLayout) findViewById(com.metrix.metrixmobile.R.id.fdLinearLayoutSelect);
		layoutCreate = (LinearLayout) findViewById(com.metrix.metrixmobile.R.id.fdLinearLayoutCreate);
		layoutCreate.setVisibility(View.GONE);

		final Button cancelButton = (Button) findViewById(com.metrix.metrixmobile.R.id.fdButtonCancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setSelectVisible(v);
			}

		});
		final Button createButton = (Button) findViewById(com.metrix.metrixmobile.R.id.fdButtonCreate);
		createButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mFileName.getText().length() > 0) {
					getIntent().putExtra(RESULT_PATH,
							currentPath + "/" + mFileName.getText());
					setResult(RESULT_OK, getIntent());
					finish();
				}
			}
		});

		String startPath = getIntent().getStringExtra(START_PATH);
		if (MetrixStringHelper.isNullOrEmpty(startPath)) {
			startPath = SettingsHelper.getLastFolder(this);
		}
		
		if (startPath != null) {
			getDir(startPath);
		} else {
			getDir(ROOT);
		}
	}

	public void onStart() {
		List<ResourceValueObject> resourceStrings = new ArrayList<ResourceValueObject>();
		resourceStrings.add(new ResourceValueObject(R.id.fdButtonNew, "New"));
		resourceStrings.add(new ResourceValueObject(R.id.fdButtonSelect, "Select"));
		resourceStrings.add(new ResourceValueObject(R.id.textViewFilename, "FileName"));
		resourceStrings.add(new ResourceValueObject(R.id.fdButtonCancel, "Cancel"));
		resourceStrings.add(new ResourceValueObject(R.id.fdButtonCreate, "Create"));
		resourceStrings.add(new ResourceValueObject(R.id.empty, "NoData"));
		try {
			AndroidResourceHelper.setResourceValues(this, resourceStrings);
		} catch (Exception e) {
		}
		super.onStart();
	}

	private void getDir(String dirPath) {

		boolean useAutoSelection = dirPath.length() < currentPath.length();

		Integer position = lastPositions.get(parentPath);

		getDirImpl(dirPath);

//		if (position != null && useAutoSelection) {
//			getListView().setSelection(position);
//		}

	}

	private void getDirImpl(final String dirPath) {

		currentPath = dirPath;

		final List<String> item = new ArrayList<String>();
		path = new ArrayList<String>();
		mList = new ArrayList<HashMap<String, Object>>();

		File f = new File(currentPath);
		File[] files = f.listFiles();
		if (files == null) {
			currentPath = ROOT;
			f = new File(currentPath);
			files = f.listFiles();
		}
		myPath.setText(AndroidResourceHelper.getMessage("LocationColon1Arg", currentPath));

		if (!currentPath.equals(ROOT)) {

			item.add(ROOT);
			addItem(ROOT, com.metrix.metrixmobile.R.drawable.folder);
			path.add(ROOT);

			item.add("../");
			addItem("../", com.metrix.metrixmobile.R.drawable.folder);
			path.add(f.getParent());
			parentPath = f.getParent();

		}

		TreeMap<String, String> dirsMap = new TreeMap<String, String>();
		TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
		TreeMap<String, String> filesMap = new TreeMap<String, String>();
		TreeMap<String, String> filesPathMap = new TreeMap<String, String>();

		if(files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					String dirName = file.getName();
					dirsMap.put(dirName, dirName);
					dirsPathMap.put(dirName, file.getPath());
				} else {
					if (file.length() > 0) {
						filesMap.put(file.getName(), file.getName());
						filesPathMap.put(file.getName(), file.getPath());
					}
				}
			}
		}
		item.addAll(dirsMap.tailMap("").values());
		item.addAll(filesMap.tailMap("").values());
		path.addAll(dirsPathMap.tailMap("").values());
		path.addAll(filesPathMap.tailMap("").values());

		for (String dir : dirsMap.tailMap("").values()) {
			addItem(dir, com.metrix.metrixmobile.R.drawable.folder);
		}

		for (String file : filesMap.tailMap("").values()) {
			addItem(file, com.metrix.metrixmobile.R.drawable.file);
		}

		final SimpleRecyclerViewAdapter fileList = new SimpleRecyclerViewAdapter(mList,
				com.metrix.metrixmobile.R.layout.file_dialog_row,
				new String[] { ITEM_KEY, ITEM_IMAGE }, new int[] {
				com.metrix.metrixmobile.R.id.fdrowtext, com.metrix.metrixmobile.R.id.fdrowimage }, new int[]{}, null);
		fileList.setClickListener(this);
		recyclerView.setAdapter(fileList);
	}

	private void addItem(String fileName, int imageId) {
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put(ITEM_KEY, fileName);
		item.put(ITEM_IMAGE, imageId);
		mList.add(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			selectButton.setEnabled(false);

			if (layoutCreate.getVisibility() == View.VISIBLE) {
				layoutCreate.setVisibility(View.GONE);
				layoutSelect.setVisibility(View.VISIBLE);
			} else {
				if (!currentPath.equals(ROOT)) {
					getDir(parentPath);
				} else {
					return super.onKeyDown(keyCode, event);
				}
			}

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	private void setCreateVisible(View v) {
		layoutCreate.setVisibility(View.VISIBLE);
		layoutSelect.setVisibility(View.GONE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);
	}

	private void setSelectVisible(View v) {
		layoutCreate.setVisibility(View.GONE);
		layoutSelect.setVisibility(View.VISIBLE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);
	}

	@Override
	public void onSimpleRvItemClick(int position, Object item, View view) {
		File file = new File(path.get(position));

		setSelectVisible(view);

		if (file.isDirectory()) {
			selectButton.setEnabled(false);
			if (file.canRead()) {
				lastPositions.put(currentPath, position);
				getDir(path.get(position));
			} else {
				new AlertDialog.Builder(this)
						.setTitle(AndroidResourceHelper.getMessage("CantReadFolder1Arg", file.getName()))
						.setPositiveButton(AndroidResourceHelper.getMessage("OK"),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
														int which) {

									}
								}).show();
			}
		} else {
			selectedFile = file;
			getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
			setResult(RESULT_OK, getIntent());

			String path = selectedFile.getPath().substring(0, selectedFile.getPath().lastIndexOf("/"));
			SettingsHelper.setLastFolder(getBaseContext(), path);
			finish();

/*			selectedFile = file;
			v.setSelected(true);
			selectButton.setEnabled(true);
*/		}
	}
}