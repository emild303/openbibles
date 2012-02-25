package com.konibee.bible;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class ConfigureWidget extends Activity implements OnClickListener{
	private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private List<String> bibleList = new ArrayList<String>();
	private ArrayAdapter<String> biblesAdapter;
	private List<String> bknList = new ArrayList<String>();
	
	private static final String TAG = "ConfigureWidget";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.configurewidget);
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		String currentBookLanguage = preference.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);		
		Spinner spnBookLanguage = (Spinner) findViewById(R.id.spnBookLanguage);
		
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File sdcard = Environment.getExternalStorageDirectory();
			File bookNameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
			if (!bookNameFolder.isDirectory()) {
				return;
			}
			File[] bookNameFiles = bookNameFolder.listFiles();
			if (bookNameFiles.length == 0) {
				bknList.add("English");
				bknList.add("Indonesia");
			} else {
				for (File file : bookNameFiles) {
					String name = file.getName().toLowerCase();
					if (!name.endsWith(".bkn")) continue;
					name = name.substring(0,1).toUpperCase() + name.substring(1);
					name = name.substring(0, name.length()-4);
					bknList.add(name);
				}
			}
		}
		
		String[] arrLanguage = new String[bknList.size()];
		arrLanguage = bknList.toArray(arrLanguage);
		ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, arrLanguage);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnBookLanguage.setAdapter(aa);
		spnBookLanguage.setSelection(0);
		for (int i=0; i < bknList.size(); i++) {
			if (bknList.get(i).equals(currentBookLanguage)) {
				spnBookLanguage.setSelection(i);
				break;
			}
		}
		DatabaseHelper databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();
		bibleList = databaseHelper.getBibleNameList();
		databaseHelper.close();
		Spinner spnBible = (Spinner) findViewById(R.id.spnBibleFile);
		bibleList.add(0, "<As Bookmarked>");
		biblesAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, bibleList);
		biblesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnBible.setAdapter(biblesAdapter);
		spnBible.setSelection(0);
		
		Button btnOK = (Button) findViewById(R.id.btnOK);
		btnOK.setOnClickListener(this);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnOK) {
//			Spinner spnBookLanguage = (Spinner) findViewById(R.id.spnBookLanguage);
//			String bookLanguage = Constants.LANG_ENGLISH;
//			if (spnBookLanguage.getSelectedItemPosition()==1) {
//				bookLanguage = Constants.LANG_BAHASA;
//			} 
			Spinner spnBookLanguage = (Spinner) findViewById(R.id.spnBookLanguage);
			String bookLanguage = (String) spnBookLanguage.getSelectedItem();
			
			DatabaseHelper databaseHelper = new DatabaseHelper(this);
			Spinner spnBible = (Spinner) findViewById(R.id.spnBibleFile);
			int position = spnBible.getSelectedItemPosition();
			String bibleFileName = Constants.AS_BOOKMARKED;
			if (position > 0) {
				databaseHelper.open();
				BibleVersion bibleVersion = databaseHelper.getBibleVersionByBibleName(bibleList.get(position));
				databaseHelper.close();
				bibleFileName = bibleVersion.getFileName();
			}
			WidgetModel model = new WidgetModel(bookLanguage, bibleFileName);
			WidgetModel.saveWidgetData(this, widgetId, model);
			MyAppWidget.updateMyWidget(this, widgetId);
			
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!isFinishing()) {
			Intent ret = new Intent();
			ret.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
			setResult(Activity.RESULT_CANCELED,ret);
		}
	}
}
