package com.konibee.bible;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class Prefs extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		final ListPreference listLanguage = (ListPreference) this.findPreference("bookLanguage");
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File sdcard = Environment.getExternalStorageDirectory();
			File bookNameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
			if (!bookNameFolder.isDirectory()) {
				return;
			}
			File[] bookNameFiles = bookNameFolder.listFiles();
			List<String> listFileDisplay = new ArrayList<String>(); 
			List<String> listFileValues = new ArrayList<String>();
			for (File file : bookNameFiles) {
				if (!file.getName().endsWith(".bkn")) continue;
				String display = file.getName().toLowerCase();
				display = display.substring(0,1).toUpperCase() + display.substring(1);
				display = display.substring(0, display.length()-4);
				listFileDisplay.add(display);
				listFileValues.add(display);
			}
			String[] arrDisplay = new String[listFileDisplay.size()];
			String[] arrValues = new String[listFileValues.size()];
			arrDisplay = listFileDisplay.toArray(arrDisplay);
			arrValues = listFileValues.toArray(arrValues);
			listLanguage.setEntries(arrDisplay);
			listLanguage.setEntryValues(arrValues);	
		}
		
		listLanguage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()	{
			public boolean onPreferenceChange(Preference p,
					Object newValue) {
				String str = getResources().getString(R.string.prefLangChanged);
				String msg = String.format(str, newValue);
				Toast.makeText(Prefs.this, msg, Toast.LENGTH_SHORT).show();
				return true;
			}
		});
	}
	
}
