package com.konibee.bible;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class GoTo extends Activity implements OnEditorActionListener, OnClickListener {
	private EditText edtBook;
	private Button btnBrowse;
	
	private String currentBookLanguage;
	private int currentChapterIdx;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gotobible);
		readPreference();
		edtBook = (EditText) findViewById(R.id.edtBook);
		edtBook.setOnEditorActionListener(this);
		btnBrowse = (Button) findViewById(R.id.btnBrowse);
		btnBrowse.setOnClickListener(this);
	}
	
	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentBookLanguage = preference.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
		currentChapterIdx = preference.getInt(Constants.POSITION_CHAPTER, 0);
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnBrowse) {
			finish();
			startActivity(new Intent(this, BrowseBook.class));
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER )) ||
	    		actionId == EditorInfo.IME_ACTION_DONE){
			InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	        in.hideSoftInputFromWindow(edtBook.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	        
	        String searchBook = edtBook.getText().toString().trim();
			if (searchBook.length() == 0) {
				finish();
				return true;
			}
			
			int goChapter = 0;
			int goBook = 0;
			char lastChar = searchBook.charAt(searchBook.length() - 1);
			String strChapter = "";
			if (lastChar >= '0' && lastChar <= '9') {
				do {
					strChapter = lastChar + strChapter;
					if (searchBook.length() > 1) {
						searchBook = searchBook.substring(0, searchBook.length() - 1);
						lastChar = searchBook.charAt(searchBook.length() - 1);
					} else {
						searchBook = "";
						break;
					}
				} while (lastChar >= '0' && lastChar <= '9');
				goChapter = Integer.parseInt(strChapter);
			}
			
			if (searchBook.length() > 0) {
				searchBook = searchBook.replaceAll(" ", "");
				searchBook = searchBook.toLowerCase();
				
				String[] firstSearch = null;
				String[] secondSearch = null;
				
//				if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//					firstSearch = Constants.arrBookNameIndo;
//					secondSearch = Constants.arrBookName;
//				} else {
//					firstSearch = Constants.arrBookName;
//					secondSearch = Constants.arrBookNameIndo;
//				}
				firstSearch = Constants.arrActiveBookName;
				secondSearch = Constants.arrActiveBookAbbr;
				
				for (int i = 0; i < firstSearch.length; i++) {
					String book = firstSearch[i].toLowerCase();
					book = book.replaceAll(" ", "");
					if (book.startsWith(searchBook)) {
						goBook = i + 1;
						break;
					}
				}
			
				if (goBook == 0) {
					for (int i = 0; i < secondSearch.length; i++) {
						String book = secondSearch[i].toLowerCase();
						book = book.replaceAll(" ", "");
						if (book.startsWith(searchBook)) {
							goBook = i + 1;
							break;
						}
					}	
				}
			}
				
			if (goBook > 0 || goChapter > 0) {
				if (goBook == 0) {
					String[] arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
					goBook = Integer.parseInt(arrBookChapter[0]);
				} 
				if (goChapter == 0) {
					goChapter = 1;
				}
				String bookChapter = goBook + ";" + goChapter;
				for (int i = Constants.arrBookStart[goBook - 1]; i < Constants.arrVerseCount.length; i++) {
					if (Constants.arrVerseCount[i].startsWith(bookChapter)) {
						currentChapterIdx = i;
						break;
					}
				}
			}
			
			Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
			editor.putInt(Constants.POSITION_CHAPTER, currentChapterIdx);
			editor.commit();
			finish();
	        return true;
	    }
		return false;
	}
}
