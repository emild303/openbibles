package com.konibee.bible;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

public class AboutDocument extends Activity {
	private static final String TAG = "Documents";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.aboutbible);
		
		if (getIntent().getExtras() == null) return;
		String tocFileName = getIntent().getExtras().getString(Constants.CURRENT_FILE);
		String[] arrTitle;
		
		try {
			File sdcard = Environment.getExternalStorageDirectory();
			File tocFile = new File(sdcard, Constants.DOCUMENT_FOLDER + "/" + tocFileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF-8"), 512);
			arrTitle = br.readLine().split(";;");
			br.close();
		} catch (Exception e) {
			Log.e(TAG, "Error reading about document", e);
			arrTitle = new String[] {tocFileName};
		}
		setTitle(arrTitle[0]);
		
		TextView txtAboutBible = (TextView) findViewById(R.id.txtAboutBible);
		if (arrTitle.length >= 2) {
			txtAboutBible.setText(Html.fromHtml(arrTitle[1]));
		} else {
			txtAboutBible.setText("Document Information not available");
		}
	}
	
}

