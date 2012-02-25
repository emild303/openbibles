package com.konibee.bible;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class Bookmarks extends ListActivity implements OnClickListener, OnItemSelectedListener, OnItemClickListener {
	private static final String[] arrSortBy = new String[] {Constants.SORT_BOOK, Constants.SORT_DATE_ASC, Constants.SORT_DATE_DESC};
	private static final String TAG = "Bookmarks";
	
	private DatabaseHelper databaseHelper;
	
	private View importView;
	private AlertDialog importDialog;
	private View exportView;
	private AlertDialog exportDialog;
	private View optionView;
	private AlertDialog optionDialog;
	
	private String currentSortBy;
	private String currentCategory;
	private String currentBible;
	
	private String currentBookLanguage;
	private int currentFontSize;
	
	private DisplayBookmarkAdapter adapter;
	private List<Bookmark> bookmarkList = new ArrayList<Bookmark>();
	
	private boolean gotoDownloadBookmark = false;
	private Handler handler = new Handler();
	
	private AlertDialog dialogImportFrom;
	private ListView viewImportFrom;
	
	private int selectCategory = 0;
	
	public void displayBookmarks(String categoryName, String sortBy, String bible, final boolean resetScroll) {
		databaseHelper.getBookmarkList(bookmarkList, categoryName, sortBy, bible);
		handler.post(new Runnable() {
            public void run() {
            	adapter.notifyDataSetChanged();
            	if (resetScroll) {
					getListView().setSelection(0);
            	}
            }
        });
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookmarks);
		
		File sdcard = Environment.getExternalStorageDirectory();
		File bookmarkFolder = new File(sdcard.getPath() + Constants.BOOKMARK_FOLDER);
		if (!bookmarkFolder.isDirectory()) {
			boolean success = bookmarkFolder.mkdirs();
			Log.d(TAG, "Creating bookmark directory success: " + success);
		}
		
		readPreference();
		currentSortBy = Constants.SORT_BOOK;
		currentBible = Constants.SHOW_BIBLE_AS_BOOKMARKED;
		
		databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();
		
		fillSpinnerCategory();
		
		LayoutInflater li = LayoutInflater.from(this);
		importView = li.inflate(R.layout.importbookmark, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.importBookmark_label);
		builder.setView(importView);
		builder.setPositiveButton("OK", this);
		builder.setNegativeButton("Cancel", this);
		importDialog = builder.create();
		
		exportView = li.inflate(R.layout.exportbookmark, null);
		builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.exportBookmark_label);
		builder.setView(exportView);
		builder.setPositiveButton("OK", this);
		builder.setNegativeButton("Cancel", this);
		exportDialog = builder.create();
		
		optionView = li.inflate(R.layout.viewoption, null);
		builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.viewOption);
		builder.setView(optionView);
		builder.setPositiveButton("OK", this);
		builder.setNegativeButton("Cancel", this);
		optionDialog = builder.create();
		Spinner spnSortBy = (Spinner) optionView.findViewById(R.id.spnSortBy);
		ArrayAdapter<String> aaSortBy = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, arrSortBy);
		aaSortBy.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnSortBy.setAdapter(aaSortBy);		
		Spinner spnBible = (Spinner) optionView.findViewById(R.id.spnBible);
		List<String> bibleList = databaseHelper.getBibleNameList();
		bibleList.add(0, "<As Bookmarked>");
		String[] arrBible = new String[bibleList.size()];
		arrBible = bibleList.toArray(arrBible);
		ArrayAdapter<String> aaBible = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, arrBible);
		aaBible.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnBible.setAdapter(aaBible);
		spnBible.setSelection(0);
		
		adapter = new DisplayBookmarkAdapter(this, R.layout.rowbookmark, bookmarkList, currentBookLanguage, currentFontSize);
		setListAdapter(adapter);
		registerForContextMenu(getListView());
		getListView().setOnItemClickListener(this);
		
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle(R.string.importFrom);
		String[] arrImport = new String[] {"SD Card", "Internet"};
		viewImportFrom = new ListView(this);
		viewImportFrom.setAdapter(new ArrayAdapter<String>(this, R.layout.listitemmedium, arrImport));
		viewImportFrom.setOnItemClickListener(this);
		ad.setView(viewImportFrom);		
		dialogImportFrom = ad.create();
	}
	
	private void fillSpinnerCategory() {
		Spinner spnCategory = (Spinner) findViewById(R.id.spnCategory);
		List<String> categoryList = databaseHelper.getBookmarkCategoryList();
		String[] arrCategory = new String[categoryList.size()];
		arrCategory = categoryList.toArray(arrCategory);
		ArrayAdapter<String> aaCategory = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, arrCategory);
		aaCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnCategory.setAdapter(aaCategory);
		spnCategory.setOnItemSelectedListener(this);

		int selection = 0;
		if (currentCategory != null && !currentCategory.equals("")) {
			int i = 0;
			for (String str : arrCategory) {
				if (str.equals(currentCategory)) {
					selection = i;
					break;
				}
				i++;
			}
		}
		if (categoryList.size() > 0) {
			spnCategory.setSelection(selection);
		}
	}

	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentCategory = preference.getString(Constants.POSITION_CATEGORY, "");
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
		if (gotoDownloadBookmark) {
			readPreference();
		}
		readPreference();
		fillSpinnerCategory();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		editor.putString(Constants.POSITION_CATEGORY, currentCategory);
		editor.commit();
		//close database
		databaseHelper.close();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bookmarks_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {		
			case R.id.viewOption:
				optionDialog.show();
				return true;
			case R.id.importBookmark:
				String state = Environment.getExternalStorageState();
				if (!Environment.MEDIA_MOUNTED.equals(state)) {
					Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG);
					return true;
				}
				dialogImportFrom.show();
				return true;
			case R.id.exportBookmark:
				exportDialog.show();
				return true;
			case R.id.manageCategory:
				startActivity(new Intent(this, Categories.class));
				return true;
			case R.id.clearBookmark:
				new AlertDialog.Builder(this)
		        .setTitle(R.string.clearBookmark)
		        .setMessage(R.string.reallyClearBookmark)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                databaseHelper.removeAllBookmarks(currentCategory);
		                handler.post(new Runnable() {
							@Override
							public void run() {
								fillSpinnerCategory();
							}	
		                });
		            }
		        })
		        .setNegativeButton(R.string.no, null)
		        .show();
				return true;
			case R.id.help:
				Intent iHelp = new Intent(this, Help.class);
				iHelp.putExtra(Constants.FONT_SIZE, currentFontSize);
				iHelp.putExtra(Constants.HELP_CONTENT, R.string.help_bookmark);
				startActivity(iHelp);
				return true;
		}
		return false;
	}

	private ProgressDialog pd = null;
	
	@Override
	public void onClick(DialogInterface dialog, int buttonId) {
		if (dialog.equals(importDialog)) {
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				String state = Environment.getExternalStorageState();
				if (!Environment.MEDIA_MOUNTED.equals(state)) {
					Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG);
					return;
				}
				pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.importing), true, false);
				Spinner spnBookmarkFile = (Spinner) importView.findViewById(R.id.spnBookmarkFile);
				new ImportTask(this).execute(spnBookmarkFile.getSelectedItem());
			}
		} else if (dialog.equals(exportDialog)) {
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				String state = Environment.getExternalStorageState();
				if (!Environment.MEDIA_MOUNTED.equals(state)) {
					Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG);
					return;
				}
				EditText edtFilename = (EditText) exportView.findViewById(R.id.edtFilename);
				String filename = edtFilename.getText().toString().trim();
				if (filename.equals("")) return;
				pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.exporting), true, false);
				new ExportTask(this).execute(filename);
			}
		} else if (dialog.equals(optionDialog)) {
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				Spinner spnSortBy = (Spinner) optionView.findViewById(R.id.spnSortBy);
				currentSortBy = (String) spnSortBy.getSelectedItem();
				Spinner spnBible = (Spinner) optionView.findViewById(R.id.spnBible);
				currentBible = (String) spnBible.getSelectedItem();
				this.pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
		        new LoadingBookmarkTask().execute(Boolean.FALSE);
			}
		}
	}
	
	private class ImportTask extends AsyncTask<Object, Void, Integer> {
		private Context context;
		public ImportTask(Context context) {
			this.context = context;
		}
		
		@Override
		protected Integer doInBackground(Object... arg) {
			String filename = (String) arg[0];
			filename = filename + ".bmk";
			File sdcard = Environment.getExternalStorageDirectory();
			File f = new File(sdcard.getPath() + Constants.BOOKMARK_FOLDER + "/" + filename);
			
			BufferedReader br = null;
			int i = 0;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"), 8192);
				
				byte[] bomUtf8 = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
				String line = br.readLine();
				if (line == null) return null;
				if (Character.toString(line.charAt(0)).equals(new String(bomUtf8))) {
					line = line.substring(1);
				} 
			
				Map<String, Long> categoryMap = databaseHelper.getCategoryMap();
				List<Bookmark> bookmarkList = databaseHelper.getAllBookmarksForImport();
				do {
					String[] arrLine = line.split(";;");
					if (arrLine.length != 7) continue;

					try {
						String category = arrLine[0];
						String book = arrLine[1];
						String chapter = arrLine[2];
						String verse = arrLine[3];
						String content = arrLine[4];
						String bibleName = arrLine[5];
						String date = arrLine[6];
						
						if (!categoryMap.keySet().contains(category)) {
							Long categoryId = databaseHelper.insertCategory(category);
							categoryMap.put(category, categoryId);
						}

						Bookmark bm = new Bookmark();
						bm.setCategoryId(categoryMap.get(category));
						bm.setBook(Integer.valueOf(book));
						bm.setChapter(Integer.valueOf(chapter));
						bm.setContent(content);
						bm.setBible(bibleName);
						bm.setBookmarkDate(date);
						if (!verse.contains("-")) {
							bm.setVerseStart(Integer.valueOf(verse));
							bm.setVerseEnd(Integer.valueOf(verse));
						} else {
							int posDash = verse.indexOf("-");
							bm.setVerseStart(Integer.valueOf(verse.substring(0, posDash)));
							bm.setVerseEnd(Integer.valueOf(verse.substring(posDash+1)));
						}
						if (bookmarkList.contains(bm)) continue;
						databaseHelper.insertBookmark(bm);
						bookmarkList.add(bm);
						
						if (i == 0) {
							currentCategory = category;
						}
					} catch (IllegalArgumentException iae) {
						iae.printStackTrace();
						continue;
					}
					i++;
				} while ((line = br.readLine()) != null);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			return Integer.valueOf(i);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (result != null) {
				String strSuccessFormat = getResources().getString(R.string.import_success);  
				String strSuccessMsg = String.format(strSuccessFormat, result);
				fillSpinnerCategory();
				if (pd != null) {
	                pd.dismiss();
	            }
				Toast.makeText(context, strSuccessMsg, Toast.LENGTH_LONG).show();
				context = null;
			} else {
				if (pd != null) {
	                pd.dismiss();
	            }
			}
		}
		
	}
	
	private class ExportTask extends AsyncTask<Object, Void, Integer> {
		private Context context;
		public ExportTask(Context context) {
			this.context = context;
		}
		
		@Override
		protected Integer doInBackground(Object... arg) {
			String filename = (String) arg[0];
			File sdcard = Environment.getExternalStorageDirectory();
			int posDot = filename.indexOf(".");
			if (posDot > -1) {
				filename = filename.substring(0, posDot);
			}
			filename = filename + ".bmk";
			File f = new File(sdcard.getPath() + Constants.BOOKMARK_FOLDER + "/" + filename);			
			
			PrintWriter pw = null;
			
			int i = 0;
			try {
				List<Bookmark> bookmarkList = databaseHelper.getAllBookmarksForExport();
				pw = new PrintWriter(f, "UTF-8");
				
				StringBuffer sb = new StringBuffer();
				
				for (Bookmark bm : bookmarkList) {
					i++;
					if (sb.length() > 0)
						sb.delete(0, sb.length());
					sb.append(bm.getCategoryName()).append(";;").append(bm.getBook()).append(";;")
					  .append(bm.getChapter()).append(";;").append(bm.getVerseStart())
					  .append("-").append(bm.getVerseEnd()).append(";;")					  
					  .append(bm.getContent()).append(";;").append(bm.getBible()).append(";;")
					  .append(bm.getBookmarkDate());
					pw.println(sb.toString());
					if (i%10 == 0) pw.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				pw.close();
			}
			return Integer.valueOf(i);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (pd != null) {
                pd.dismiss();
            }
			
			if (result != null) {
				String strSuccessFormat = getResources().getString(R.string.export_success);  
				String strSuccessMsg = String.format(strSuccessFormat, result);  
				Toast.makeText(context, strSuccessMsg, Toast.LENGTH_LONG).show();
				context = null;
			}
		}
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		Spinner spnCategory = (Spinner) findViewById(R.id.spnCategory);
		boolean resetScroll = false;
		if (!currentCategory.equals((String) spnCategory.getSelectedItem())) {
			currentCategory = (String) spnCategory.getSelectedItem();
			resetScroll = true;
		}
		this.pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
		new LoadingBookmarkTask().execute(Boolean.valueOf(resetScroll));
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// do nothing
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == getListView()) {
		   Bookmark bm = bookmarkList.get(position);
		   if (bm == null) return;
		   Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		   int chapterIdx = Constants.arrBookStart[bm.getBook()-1] + bm.getChapter()-1;
		   editor.putInt(Constants.POSITION_CHAPTER, chapterIdx);
		   editor.putString(Constants.POSITION_BIBLE_NAME, bm.getBible() + ".ont");
		   editor.commit();
	       Intent showBibleActivity = new Intent(this, BiblesOffline.class);
	       showBibleActivity.putExtra(Constants.FROM_BOOKMARKS, true);
	       showBibleActivity.putExtra(Constants.BOOKMARK_VERSE_START, bm.getVerseStart());       
	       startActivity(showBibleActivity);
		} else if (parent == viewImportFrom) {
			dialogImportFrom.dismiss();
			if (position == 0) { //sdcard			
				File sdcard = Environment.getExternalStorageDirectory();
				File bookmarkFolder = new File(sdcard.getPath() + Constants.BOOKMARK_FOLDER);
				File[] arrFile = bookmarkFolder.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						if (pathname.getName().endsWith(".bmk")) return true;
						return false;
					}
				});
				if (arrFile.length == 0) {
					Toast.makeText(this, R.string.no_bookmark_file, Toast.LENGTH_LONG).show();
					return;
				}
				String[] arrFileName = new String[arrFile.length];
				int i = 0;
				for (File f : arrFile) {
					arrFileName[i] = f.getName().substring(0, f.getName().length()-4);
					i++;
				}
				
				Spinner spnBookmarkFile = (Spinner) importView.findViewById(R.id.spnBookmarkFile);
				ArrayAdapter<String> aaBookmarkFile = new ArrayAdapter<String>(this,
						android.R.layout.simple_spinner_item, arrFileName);
				aaBookmarkFile.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spnBookmarkFile.setAdapter(aaBookmarkFile);
				spnBookmarkFile.invalidate();
				importDialog.show();
			} else if (position == 1) { //internet
				gotoDownloadBookmark = true;
				Intent i = new Intent(this, DownloadBookmark.class);
				startActivity(i);
			}
		}
	}
	
	private class LoadingBookmarkTask extends AsyncTask<Object, Void, Integer> {
		@Override
		protected Integer doInBackground(Object... arg) {
			Boolean resetScroll = (Boolean) arg[0];
			displayBookmarks(currentCategory, currentSortBy, currentBible, resetScroll);			
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (pd != null) {
                pd.dismiss();
            }
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.changeCategory);
		menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, R.string.removeBookmark);
		menu.add(Menu.NONE, Menu.FIRST+2, Menu.NONE, R.string.copyToClipboard);
		menu.add(Menu.NONE, Menu.FIRST+3, Menu.NONE, R.string.share);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
		    info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
		    Log.e(TAG, "bad menuInfo", e);
		    return false;
		}
		
	    int index = info.position;
	    
	    final Bookmark bookmark = bookmarkList.get(index);
	    
	    switch (item.getItemId()) {
			case Menu.FIRST : //change category
				final String[] arrCategory = databaseHelper.getCategoryNames();
				
				for (int i = 0; i < arrCategory.length; i++) {
					selectCategory = i;
					if (arrCategory[i].equals(currentCategory)) {
						break;
					}
				}
				new AlertDialog.Builder(this)
			        .setTitle(R.string.pickCategory)
			        .setSingleChoiceItems(arrCategory, selectCategory, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {							
							selectCategory = which;
						}
					})
			        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			            @Override
			            public void onClick(DialogInterface dialog, int which) {
			            	databaseHelper.moveBookmark(arrCategory[selectCategory], bookmark.getId());
			            	Toast.makeText(Bookmarks.this, "Bookmark has been moved to '" + arrCategory[selectCategory] + "' category", Toast.LENGTH_LONG).show();
			                handler.post(new Runnable() {
			                    public void run() {
			                    	fillSpinnerCategory();
			                    }
			                });
			            }
		
			        })
			        .setNegativeButton(R.string.cancel, null)
			        .show();
				return true;
			case Menu.FIRST + 1 : //remove bookmark
				new AlertDialog.Builder(this)
			        .setTitle(R.string.removeBookmark)
			        .setMessage(R.string.reallyRemoveBookmark)
			        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			            @Override
			            public void onClick(DialogInterface dialog, int which) {
			                databaseHelper.removeBookmark(bookmark.getBook(), bookmark.getChapter(), bookmark.getVerseStart());
			                handler.post(new Runnable() {
			                    public void run() {
			                    	fillSpinnerCategory();
			                    }
			                });
			            }
		
			        })
			        .setNegativeButton(R.string.no, null)
			        .show();
				return true;
			case Menu.FIRST + 2 : //copy to clipboard
				String bookName;
//	            if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//	            	bookName = Constants.arrBookNameIndo[bookmark.getBook()-1];
//	            } else {
//	            	bookName = Constants.arrBookName[bookmark.getBook()-1];
//	            }
				bookName = Constants.arrActiveBookName[bookmark.getBook()-1];
	            StringBuffer sbBook = new StringBuffer();
	            sbBook.append(bookName).append(" ").append(bookmark.getChapter()).append(":").append(bookmark.getVerseStart());
	            if (!bookmark.getVerseEnd().equals(bookmark.getVerseStart())) {
	            	sbBook.append("-").append(bookmark.getVerseEnd());
	            }
				
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
				clipboard.setText(sbBook.toString() + " " + bookmark.getContent());
				String str = getResources().getString(R.string.copiedSuccess);
				String msg = String.format(str, sbBook.toString());
				Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
				return true;
			case Menu.FIRST + 3 : //share
//	            if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//	            	bookName = Constants.arrBookNameIndo[bookmark.getBook()-1];
//	            } else {
//	            	bookName = Constants.arrBookName[bookmark.getBook()-1];
//	            }
				bookName = Constants.arrActiveBookName[bookmark.getBook()-1];
	            sbBook = new StringBuffer();
	            sbBook.append(bookName).append(" ").append(bookmark.getChapter()).append(":").append(bookmark.getVerseStart());
	            if (!bookmark.getVerseEnd().equals(bookmark.getVerseStart())) {
	            	sbBook.append("-").append(bookmark.getVerseEnd());
	            }
	            Intent i=new Intent(android.content.Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_SUBJECT, sbBook.toString());
				i.putExtra(Intent.EXTRA_TEXT, sbBook.toString() + " " + bookmark.getContent());
				startActivity(Intent.createChooser(i, "Share"));
				return true;
	    }
	    return false;
	}
}
