package com.konibee.bible;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class SelectParallelBible extends Activity implements OnClickListener {
	private List<String> bibleNameList = new ArrayList<String>();
	private List<String> fileNameList = new ArrayList<String>();
	private ArrayAdapter<String> biblesAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectparallel);
		
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		String currentBibleFilename = preference.getString(Constants.POSITION_BIBLE_NAME, "");
		String currentBibleFilename2 = preference.getString(Constants.POSITION_BIBLE_NAME_2, "");
		
		DatabaseHelper databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();
		bibleNameList = new ArrayList<String>();
		fileNameList = new ArrayList<String>();
		databaseHelper.getBibleTranslationList(bibleNameList, fileNameList);
		databaseHelper.close();
		Spinner spnParallel1 = (Spinner) findViewById(R.id.spnParallel1);
		Spinner spnParallel2 = (Spinner) findViewById(R.id.spnParallel2);
		biblesAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, bibleNameList);
		biblesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnParallel1.setAdapter(biblesAdapter);
		spnParallel2.setAdapter(biblesAdapter);
		int selected = 0;
		if (currentBibleFilename != null && !"".equals(currentBibleFilename)) {
			for (int i = 0; i < fileNameList.size(); i++) {
				if (fileNameList.get(i).equals(currentBibleFilename)) {
					selected = i;
					break;
				}
			}
		}
			
		spnParallel1.setSelection(selected);
		
		selected = 0;
		if (currentBibleFilename2 != null && !"".equals(currentBibleFilename2)) {
			for (int i = 0; i < fileNameList.size(); i++) {
				if (fileNameList.get(i).equals(currentBibleFilename2)) {
					selected = i;
					break;
				}
			}
		}
		spnParallel2.setAdapter(biblesAdapter);
		spnParallel2.setSelection(selected);
		
		Button btnOK = (Button) findViewById(R.id.btnOK);
		btnOK.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnOK) {
			Spinner spnParallel1 = (Spinner) findViewById(R.id.spnParallel1);
			Spinner spnParallel2 = (Spinner) findViewById(R.id.spnParallel2);
			int pos1 = spnParallel1.getSelectedItemPosition();
			int pos2 = spnParallel2.getSelectedItemPosition();
			
			Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
			editor.putString(Constants.POSITION_BIBLE_NAME, fileNameList.get(pos1));
			editor.putString(Constants.POSITION_BIBLE_NAME_2, fileNameList.get(pos2));
			editor.putString(Constants.BIBLE_NAME, (String) spnParallel1.getSelectedItem());
			editor.commit();
			finish();
		}
	}
}
