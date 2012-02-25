package com.konibee.bible;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class Find extends ListActivity implements OnClickListener, OnItemClickListener {
	private DatabaseHelper databaseHelper;
	private ProgressDialog pd = null;
	
	private List<String> wordsToSearch = new ArrayList<String>();
	private boolean searchOldTestament;
	private boolean searchNewTestament;
	private String bibleName;
	private String bibleFileName;
	
	private String currentBookLanguage;
	private int currentFontSize;
	
	private DisplaySearchResultAdapter adapter;
	private List<SearchResult> resultList = new ArrayList<SearchResult>();
	
	private AlertDialog dialogTestament;
	private ListView viewTestament;
	
	private final static String TAG = "Find";
	
	private final int MAX_RESULT = 100;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.find);

		readPreference();
		
		databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();
		
		Spinner spnBible = (Spinner) findViewById(R.id.spnBible);
		List<String> bibleList = databaseHelper.getBibleNameList();
		String[] arrBible = new String[bibleList.size()];
		arrBible = bibleList.toArray(arrBible);
		ArrayAdapter<String> aaBible = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, arrBible);
		aaBible.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnBible.setAdapter(aaBible);
		if (getIntent().getExtras() != null) {
			String currentBibleName = getIntent().getExtras().getString(Constants.CURRENT_BIBLE);
			for (int i = 0; i < bibleList.size(); i++) {
				if (currentBibleName.equals(bibleList.get(i))) {
					spnBible.setSelection(i);
					break;
				}
			}
		}
		if (bibleList.size() == 0) {
			Toast.makeText(this, R.string.downloadBibleRequired, Toast.LENGTH_LONG).show();
		}
		
		Button btnSearch = (Button) findViewById(R.id.btnSearch);
		btnSearch.setOnClickListener(this);
		
		adapter = new DisplaySearchResultAdapter(this, R.layout.rowfind, resultList, wordsToSearch, currentBookLanguage, currentFontSize);
		setListAdapter(adapter);
		registerForContextMenu(getListView());
		
		getListView().setOnItemClickListener(this);
		
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle(R.string.searchIn);
		String[] arrTestament = new String[] {getResources().getString(R.string.bothTestament), 
				getResources().getString(R.string.oldTestament), getResources().getString(R.string.newTestament)};
		viewTestament = new ListView(this);
		viewTestament.setAdapter(new ArrayAdapter<String>(this, R.layout.listitemmedium, arrTestament));
		viewTestament.setOnItemClickListener(this);
		
		ad.setView(viewTestament);		
		dialogTestament = ad.create();
	}
	
	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentBookLanguage = preference.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
		currentFontSize = preference.getInt(Constants.FONT_SIZE, 14);
	}

	@Override
	protected void onDestroy() {
		databaseHelper.close();
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		databaseHelper.open();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnSearch:
				EditText edtSearch = (EditText) findViewById(R.id.edtSearch);
				Spinner spnBible = (Spinner) findViewById(R.id.spnBible);
				
				bibleName = (String) spnBible.getSelectedItem();
				if (bibleName == null) {
					Toast.makeText(this, R.string.bibleNameRequired, Toast.LENGTH_LONG).show();
					return;
				}
				StringBuffer searchText = new StringBuffer(edtSearch.getText().toString().trim()); 
				if (searchText.length() == 0) return;
				wordsToSearch.clear();
				while (searchText.indexOf("\"") > -1) {
					int posStartQuote = searchText.indexOf("\"");
					if (posStartQuote == searchText.length()-1) {
						searchText.delete(posStartQuote, searchText.length());
						continue;
					}
					int posEndQuote = searchText.indexOf("\"", posStartQuote+1);
					if (posEndQuote == -1) {
						String word = searchText.substring(posStartQuote+1);
						if (word.length() > 1) {
							int j = 0;
							for (String checkWord : wordsToSearch) {
								if (checkWord.length() < word.length()) {
									break;
								}
								j++;
							}
							wordsToSearch.add(j, word.toLowerCase());
						}
						searchText.delete(posStartQuote, searchText.length());	
					} else {
						String word = searchText.substring(posStartQuote+1, posEndQuote); 
						if (word.length() > 1) {
							int j = 0;
							for (String checkWord : wordsToSearch) {
								if (checkWord.length() < word.length()) {
									break;
								}
								j++;
							}
							wordsToSearch.add(j, word.toLowerCase());
						}
						searchText.delete(posStartQuote, posEndQuote+1);
					}
				}
				if (searchText.length() > 0) {
					String[] arrWords = searchText.toString().split(" ");
					for (String word : arrWords) {
						if (word.length() > 1) {
							int j = 0;
							for (String checkWord : wordsToSearch) {
								if (checkWord.length() < word.length()) {
									break;
								}
								j++;
							}
							wordsToSearch.add(j, word.toLowerCase());
						}
					}
				}
				if (wordsToSearch.size() == 0) return;
				InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		        in.hideSoftInputFromWindow(edtSearch.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				dialogTestament.show();
				break;
		}
	}	
	
	private class SearchingTask extends AsyncTask<Object, Void, Object> {
		private Context context;
		public SearchingTask(Context context) {
			this.context = context;
		}
		
		@Override
		protected Object doInBackground(Object... arg) {
			BibleVersion bible = databaseHelper.getBibleVersionByBibleName(bibleName);
			File sdcard = Environment.getExternalStorageDirectory();
			File file = new File(sdcard, Constants.BIBLE_FOLDER + "/" + bible.getFileName());
			bibleFileName = bible.getFileName();
			
			byte[] bomUtf8 = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
			
//			int book = 1;
//			int chapter = 1;
			resultList.clear();
			int verse = 0;
			int chapterIndex = 0;
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
				if (!searchOldTestament) {					
					String indexFileName = file.getAbsolutePath().replaceAll(".ont", ".idx");
					File fIndex = new File(indexFileName);
					DataInputStream is = new DataInputStream(new FileInputStream(fIndex));
					is.skip(929*4); // Mat 1
					int startOffset = is.readInt();
					is.close();
					br.skip(startOffset);
//					book = 40;
					chapterIndex = 929;
				}
				String line = null;
				String verseCount = Constants.arrVerseCount[chapterIndex];
				int maxVerse = Integer.valueOf(verseCount.substring(verseCount.lastIndexOf(";") + 1));				
				
				while ((line = br.readLine()) != null) {
					verse++;
					if (verse > maxVerse) {
						chapterIndex ++;
						verseCount = Constants.arrVerseCount[chapterIndex];
						maxVerse = Integer.valueOf(verseCount.substring(verseCount.lastIndexOf(";") + 1));
						verse = 1;
						if (chapterIndex == 929 && !searchNewTestament) {
							break;						
						}
						
					}
					
					boolean match = true;
//					Log.d(TAG, wordsToSearch.get(0));
					for (int i = 0; i < wordsToSearch.size(); i++) {
						if (line.toLowerCase().indexOf(wordsToSearch.get(i)) == -1) {
							match = false;
							break;
						}
					}
					if (!match) {
						continue;
					}
					String[] arrBookChapter = Constants.arrVerseCount[chapterIndex].split(";");
			  		int book = Integer.parseInt(arrBookChapter[0]);
			  		int chapter = Integer.parseInt(arrBookChapter[1]);
					
			  		if (chapterIndex == 0) {
			  			if (Character.toString(line.charAt(0)).equals(new String(bomUtf8, "UTF-8"))) {
							line = line.substring(1);
						}
			  		}
			  		
					SearchResult sr = new SearchResult();
					sr.setBook(book);
					sr.setChapter(chapter);
					sr.setVerse(verse);
					sr.setContent(Util.parseVerse(line));
					resultList.add(sr);
					if (resultList.size() == MAX_RESULT) {
						break;
					}
				}
				
			} catch (Exception e) { 
				Log.d(TAG, "Error searching in bible file");
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return resultList.size();
		}
		
		@Override
		protected void onPostExecute(Object result) {
			if (pd != null) {
                pd.dismiss();
            }
			Integer countResult = (Integer) result;
			adapter.notifyDataSetChanged();
			String strSuccessFormat = getResources().getString(R.string.search_success);  
			String strSuccessMsg = String.format(strSuccessFormat, countResult);
			if (countResult.intValue() < 2) {
				strSuccessMsg = strSuccessMsg.substring(0, strSuccessMsg.length()-1);
			}
			Toast.makeText(context, strSuccessMsg, Toast.LENGTH_LONG).show();
			context = null;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == getListView()) {
			SearchResult sr = resultList.get(position);
		    if (sr == null) return;
		    Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		    int chapterIdx = Constants.arrBookStart[sr.getBook()-1] + sr.getChapter()-1;
		    editor.putInt(Constants.POSITION_CHAPTER, chapterIdx);
		    editor.putString(Constants.POSITION_BIBLE_NAME, bibleFileName);
		    editor.commit();
	        Intent showBibleActivity = new Intent(this, BiblesOffline.class);
	        showBibleActivity.putExtra(Constants.FROM_BOOKMARKS, true);
	        showBibleActivity.putExtra(Constants.BOOKMARK_VERSE_START, sr.getVerse());       
	        startActivity(showBibleActivity);
		} else if (parent == viewTestament) {			
			dialogTestament.dismiss();
			
			searchOldTestament = false;
			searchNewTestament = false;
			if (position == 0 || position == 1) {
				searchOldTestament = true;
			}
			if (position == 0 || position == 2) {
				searchNewTestament = true;
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("Searching for: ");
			for (String word : wordsToSearch) {
				sb.append("'").append(word).append("', ");
			}
			sb.delete(sb.length()-2, sb.length());
			
			this.pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), sb.toString(), true, false);
	        new SearchingTask(this).execute((Object)null);
		};
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(Menu.NONE, R.id.help, Menu.NONE, R.string.help);
		item.setIcon(R.drawable.menu_help);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.help:
			Intent iHelp = new Intent(this, Help.class);
			iHelp.putExtra(Constants.FONT_SIZE, currentFontSize);
			iHelp.putExtra(Constants.HELP_CONTENT, R.string.help_find);
			startActivity(iHelp);
			return true;
		}
		return false;
	}

}
