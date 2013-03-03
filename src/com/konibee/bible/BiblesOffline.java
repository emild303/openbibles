package com.konibee.bible;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BiblesOffline extends ListActivity implements OnClickListener,
		android.content.DialogInterface.OnClickListener, OnItemClickListener {
	private static final String TAG = "BiblesOffline";
	
	private DatabaseHelper databaseHelper;

	//persist
	private int currentChapterIdx;
	private String currentBibleFilename;
	private String currentBibleFilename2;
	private String currentBookLanguage;
	private int currentFontSize;
	private boolean isFullScreen;
	//not persist
	private String currentBibleName;
	
	private View footnoteView;
	private View bookmarkView;	
	private View copyToClipboardView;
	private AlertDialog footnoteDialog;
	private AlertDialog bookmarkDialog;
	private AlertDialog copyToClipboardDialog;
	
	private char copyOrShare; //'c' or 's'
	
	private ProgressDialog pd = null;
	
	private TextView txtEmpty;
	private TextView txtEmpty2;
	
	public View getFootnoteView() {
		return footnoteView;
	}
	public AlertDialog getFootnoteDialog() {
		return footnoteDialog;
	}
	
	private Handler handler = new Handler();
	private List<DisplayVerse> verseList = new ArrayList<DisplayVerse>();
	private DisplayVerseAdapter adapter;
	
	private List<DisplayVerse> verseParallelList = new ArrayList<DisplayVerse>();
	private DisplayVerseAdapter parallelAdapter;
	
	private boolean fromBookmarks;
	private int bookmarkVerseStart;
	private int bookmarkVerseEnd;
	private int bookmarkVerseMax;
	
	private boolean gotoDownloadBible = false;
	private boolean gotoBrowse = false;
	private boolean gotoSelectParallel = false;
	private boolean gotoPrefs = false;
	private boolean gotoDocuments = false;
	private int lastChapterIdx = 0;
	
	private AlertDialog dialogHistory;
	private ListView viewHistory;
	private List<Integer> historyList = new ArrayList<Integer>();
	private DisplayHistoryAdapter historyAdapter;
	
	private AlertDialog dialogBibles;
	private ListView viewBibles;
	private List<String> bibleList = new ArrayList<String>();
	private ArrayAdapter<String> biblesAdapter;
	
	private boolean isParallel;
	
	private class LoadingTask extends AsyncTask<Object, Void, Object> {
		@Override
		protected Object doInBackground(Object... arg) {
			readBibleBookName();
			String[] arrBibles = readBibleFiles();
			populateBibleList(arrBibles);
			return null;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			if (pd != null) {
				pd.dismiss();
			}
			displayBible(currentBibleFilename, currentChapterIdx);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		
		bookmarkVerseStart = 1;
		if (getIntent().getExtras() != null) {
			this.fromBookmarks = getIntent().getExtras().getBoolean(Constants.FROM_BOOKMARKS, false);
			if (fromBookmarks) {
				bookmarkVerseStart =  getIntent().getExtras().getInt(Constants.BOOKMARK_VERSE_START, 1);
			}
			boolean fromWidget = getIntent().getExtras().getBoolean(Constants.FROM_WIDGET, false);
			if (fromWidget) {
				//this is similar with from bookmarks
				if (Constants.arrActiveBookName[0] != null) {
					fromBookmarks = true;
				}
				bookmarkVerseStart = getIntent().getExtras().getInt(Constants.WIDGET_VERSE, 1);
				String bible = getIntent().getExtras().getString(Constants.WIDGET_BIBLE);
				int book = getIntent().getExtras().getInt(Constants.WIDGET_BOOK, 1);
				int chapter = getIntent().getExtras().getInt(Constants.WIDGET_CHAPTER, 1);
				Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
				int chapterIdx = Constants.arrBookStart[book-1] + chapter-1;
				editor.putInt(Constants.POSITION_CHAPTER, chapterIdx);
				editor.putString(Constants.POSITION_BIBLE_NAME, bible + ".ont");
				editor.commit();
			}
		}

		setContentView(R.layout.main);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		txtEmpty = (TextView) findViewById(R.id.txtEmpty);
		getListView().setEmptyView(txtEmpty);
		currentChapterIdx = -1;		

		databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();

		LayoutInflater li = LayoutInflater.from(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		footnoteView = li.inflate(R.layout.footnote, null);
		builder = new AlertDialog.Builder(this);
		builder.setTitle("Note");
		builder.setView(footnoteView);
		builder.setNeutralButton("Close", this);
		footnoteDialog = builder.create();
		
		bookmarkView = li.inflate(R.layout.bookmarkdialog, null);
		builder = new AlertDialog.Builder(this);
		builder.setTitle("Bookmark");
		builder.setView(bookmarkView);
		builder.setPositiveButton("OK", this);
		builder.setNeutralButton("Cancel", this);
		bookmarkDialog = builder.create();
		fillSpinnerCategory();
		Button btnPlus = (Button) bookmarkView.findViewById(R.id.btnPlus);
		btnPlus.setOnClickListener(this);
		Button btnMinus = (Button) bookmarkView.findViewById(R.id.btnMinus);
		btnMinus.setOnClickListener(this);
		
		copyToClipboardView = li.inflate(R.layout.copytoclipboarddialog, null);
		builder = new AlertDialog.Builder(this);
		builder.setTitle("Copy to clipboard");
		builder.setView(copyToClipboardView);
		builder.setPositiveButton("OK", this);
		builder.setNeutralButton("Cancel", this);
		copyToClipboardDialog = builder.create();
		fillSpinnerCategory();
		Button btnPlusClipboard = (Button) copyToClipboardView.findViewById(R.id.btnPlusClipboard);
		btnPlusClipboard.setOnClickListener(this);
		Button btnMinusClipboard = (Button) copyToClipboardView.findViewById(R.id.btnMinusClipboard);
		btnMinusClipboard.setOnClickListener(this);

		View prevButton = findViewById(R.id.btnPrev);
		prevButton.setOnClickListener(this);
		View nextButton = findViewById(R.id.btnNext);
		nextButton.setOnClickListener(this);
		View txtCurrent = findViewById(R.id.txtCurrent);
		txtCurrent.setOnClickListener(this);
		View btnFullscreen = findViewById(R.id.btnFullscreen);
		btnFullscreen.setOnClickListener(this);
		View btnZoomIn = findViewById(R.id.btnZoomIn);
		btnZoomIn.setOnClickListener(this);
		View btnZoomOut = findViewById(R.id.btnZoomOut);
		btnZoomOut.setOnClickListener(this);
		
		TextView txtBibleName = (TextView) findViewById(R.id.txtBibleName);
		txtBibleName.setOnClickListener(this);
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle(R.string.selectBible);
		biblesAdapter = new ArrayAdapter<String>(this, R.layout.listitemmedium, bibleList);
		viewBibles = new ListView(this);
		viewBibles.setAdapter(biblesAdapter);
		viewBibles.setOnItemClickListener(this);
		ad.setView(viewBibles);
		dialogBibles = ad.create();
		dialogBibles.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		readPreference();
		LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
		if (isFullScreen) {
			bottomBar.setVisibility(View.GONE);
		} else {
			bottomBar.setVisibility(View.VISIBLE);
		}
		applyParallel(isParallel);
		
		adapter = new DisplayVerseAdapter(this, R.layout.row, verseList, currentFontSize);
		setListAdapter(adapter);
		registerForContextMenu(getListView());
		
		txtEmpty2 = (TextView) findViewById(R.id.txtEmpty2);
		ListView listviewParallel = (ListView) findViewById(R.id.listviewParallel);
		parallelAdapter = new DisplayVerseAdapter(this, R.layout.row, verseParallelList, currentFontSize);
	    listviewParallel.setAdapter(parallelAdapter);
		listviewParallel.setEmptyView(txtEmpty2);

		updateBibleFontSize();
//		updateBookLanguage();

		//history dialog
		ad = new AlertDialog.Builder(this);
		ad.setTitle(R.string.history);
		viewHistory = new ListView(this);
		historyAdapter = new DisplayHistoryAdapter(this, R.layout.listitemmedium, historyList, currentBookLanguage);
		viewHistory.setAdapter(historyAdapter);
		viewHistory.setOnItemClickListener(this);
		ad.setView(viewHistory);		
		dialogHistory = ad.create();
		dialogHistory.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		if (!fromBookmarks) {
			// Show the ProgressDialog on this thread		
	        this.pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
	        if (txtEmpty != null) {
	        	txtEmpty.setText(getResources().getString(R.string.no_bibles));
	        }
	        new LoadingTask().execute((Object)null);
		} else {
			List<String> bibles = databaseHelper.getBibleNameList();
			populateBibleList(bibles);
			displayBible(currentBibleFilename, currentChapterIdx);
		}
		
		getListView().setOnScrollListener(new OnScrollListener() {
			private boolean scroll = false;
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL) {
					scroll = true;
				} else {
					scroll = false;
				}
			}
			
			@Override
			public void onScroll(AbsListView view, final int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {				
				if (view.equals(getListView()) && isParallel && scroll) {
					final ListView listviewParallel = (ListView) findViewById(R.id.listviewParallel);
					listviewParallel.post(new Runnable(){
						  public void run() {
						    listviewParallel.setSelection(firstVisibleItem);
						    listviewParallel.setSelected(false);
						  }});
				}
			}
		});
		
	}
	
	private void populateBibleList(String[] arrBibles) {
		List<String> bibles = new ArrayList<String>();
		if (arrBibles != null) {
			for (String bible : arrBibles) {
				bibles.add(bible);
			}
		}
		populateBibleList(bibles);
	}
	
	private void populateBibleList(List<String> bibles) {
		bibleList.clear();		
		for (String bible : bibles) {
			bibleList.add(bible);
		}
		biblesAdapter.notifyDataSetChanged();
		if (bibles.size() == 0) return;
		BibleVersion bibleVersion;
		if (currentBibleFilename == null || currentBibleFilename.equals("")) {
			bibleVersion = databaseHelper.getBibleVersionByBibleName(bibles.get(0));				
		} else {
			bibleVersion = databaseHelper.getBibleVersionByFileName(currentBibleFilename);				
			if (bibleVersion == null) {
				bibleVersion = databaseHelper.getBibleVersionByBibleName(bibles.get(0));
			}
		}
		currentBibleFilename = bibleVersion.getFileName();
		currentBibleName = bibleVersion.getBibleName();
		handler.post(new Runnable() {
			@Override
			public void run() {
				updateBibleInfo();
			}
		});
	}
	
	private void updateBibleFontSize() {
		adapter.updateFontSize(currentFontSize);
		adapter.notifyDataSetChanged();
		parallelAdapter.updateFontSize(currentFontSize);
		parallelAdapter.notifyDataSetChanged();
		
		TextView txtFootnote = (TextView) footnoteView.findViewById(R.id.txtFootnote);
		txtFootnote.setTextSize(currentFontSize);
	}
	
	private void updateBookLanguage() {
		TextView title = (TextView) findViewById(R.id.txtCurrent);
		String[] arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
  		int book = Integer.parseInt(arrBookChapter[0]);
  		int chapter = Integer.parseInt(arrBookChapter[1]);
		
  		title.setText(Constants.arrActiveBookName[book - 1] + " " + chapter);
		
	}

	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentChapterIdx = preference.getInt(Constants.POSITION_CHAPTER, 0);
		if (currentChapterIdx < 0 || currentChapterIdx >= Constants.arrVerseCount.length) {
			currentChapterIdx = 0;
		}
		currentBibleFilename = preference.getString(Constants.POSITION_BIBLE_NAME, "");
		currentBibleFilename2 = preference.getString(Constants.POSITION_BIBLE_NAME_2, "");
		currentFontSize = preference.getInt(Constants.FONT_SIZE, 18);
		isFullScreen = preference.getBoolean(Constants.FULL_SCREEN, false);
		isParallel = preference.getBoolean(Constants.PARALLEL, false);
		
		SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		currentBookLanguage = defaultPrefs.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
	}
	
	private void displayBible(String bibleFilename, 
			int chapterIndex) {
		displayBible(bibleFilename, chapterIndex, true);
	}

	private void displayBible(String bibleFilename, 
			int chapterIndex, boolean resetScroll) {
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			txtEmpty.setText(getResources().getString(R.string.sdcard_error));
			Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG).show();
			return;
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		
		if (chapterIndex == -1) { //no bible available
			TextView current = (TextView) findViewById(R.id.txtCurrent);
			current.setText("Error");
			return;
		}
		
		File file = new File(sdcard, Constants.BIBLE_FOLDER + "/" + bibleFilename);
		String indexFileName = file.getAbsolutePath().replaceAll(".ont", ".idx");
		File fIndex = new File(indexFileName);
		
		String[] arrBookChapter = Constants.arrVerseCount[chapterIndex]
				.split(";");
		int verseCount = Integer.parseInt(arrBookChapter[2]);
		
		List<Integer> bookmarkList = databaseHelper.getBookmarkVerseStartByChapterIndex(chapterIndex);

		verseList.clear();
		BufferedReader br = null;
		try {
			DataInputStream is = new DataInputStream(new FileInputStream(fIndex));
			is.skip(chapterIndex*4);			
			int startOffset = is.readInt();
			is.close();
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
			br.skip(startOffset);
			String line = "";
			boolean prevBreakParagraph = false;
			boolean verseNotAvailable = true;
			for (int i = 1; i <= verseCount; i++) {
				line = br.readLine();
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				boolean breakParagraph = false;
				
				line = line.replaceAll("<CL>", "\n");
				int posCM = line.indexOf("<CM>");
				if (posCM > -1) {
					if (!line.endsWith("<CM>")) {
						String afterCM = line.substring(posCM + "<CM>".length()).trim();
						if (afterCM.startsWith("<") && afterCM.endsWith(">")) {
							breakParagraph = true;
							line = line.substring(0, posCM) + afterCM;
						}
					} else {
						breakParagraph = true;
						line = line.substring(0, line.length()-"<CM>".length());
					}
				}
				
				line = line.replaceAll("<CM>", "\n\n");
				line = line.replaceAll("\n\n \n\n", "\n\n");
				line = line.replaceAll("\n\n\n\n", "\n\n");
				
				boolean bookmarked = false;
				if (bookmarkList.contains(Integer.valueOf(i))) {
					bookmarked = true;
				}
				
				if (prevBreakParagraph) {
					verseList.add(new DisplayVerse(i, line, bookmarked, true));
				} else {
					verseList.add(new DisplayVerse(i, line, bookmarked, false));
				}
				prevBreakParagraph = breakParagraph;
				verseNotAvailable = false;
			}
			
			if (verseNotAvailable) {
				if (chapterIndex < 929) {
					txtEmpty.setText(getResources().getString(R.string.no_ot));
				} else {
					txtEmpty.setText(getResources().getString(R.string.no_nt));
				}
			}
			
			adapter.notifyDataSetChanged();
			
			if (fromBookmarks) {
				getListView().post(new Runnable() {
					@Override
					public void run() {						
						getListView().setSelection(bookmarkVerseStart-1);
					}
				});
			} else if (resetScroll) {
				getListView().post(new Runnable() {
					@Override
					public void run() {
						getListView().setSelection(0);
					}
				});
			}
			
			//update history
			boolean alreadyInHistory = false;
			for (int i=0; i < historyList.size(); i++) {
				if (historyList.get(i).intValue() == chapterIndex) {
					alreadyInHistory = true;
					break;
				}
			}
			if (!alreadyInHistory) {
				if (historyList.size() < 10) {
					historyList.add(0);
				}
				for (int i=historyList.size()-1; i > 0; i--) {
					historyList.set(i, historyList.get(i-1).intValue());
				}
				historyList.set(0, chapterIndex);
				historyAdapter.notifyDataSetChanged();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {			
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//added parallel
		if (isParallel && currentBibleFilename2 != null && !"".equals(currentBibleFilename2)) {
			file = new File(sdcard, Constants.BIBLE_FOLDER + "/" + currentBibleFilename2);
			indexFileName = file.getAbsolutePath().replaceAll(".ont", ".idx");
			fIndex = new File(indexFileName);
			
			verseParallelList.clear();
			br = null;
			try {
				DataInputStream is = new DataInputStream(new FileInputStream(fIndex));
				is.skip(chapterIndex*4);			
				int startOffset = is.readInt();
				is.close();
				
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
				br.skip(startOffset);
				String line = "";
				boolean prevBreakParagraph = false;
				boolean verseNotAvailable = true;
				for (int i = 1; i <= verseCount; i++) {
					line = br.readLine();
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}
					boolean breakParagraph = false;
					
					line = line.replaceAll("<CL>", "\n");
					int posCM = line.indexOf("<CM>");
					if (posCM > -1) {
						if (!line.endsWith("<CM>")) {
							String afterCM = line.substring(posCM + "<CM>".length()).trim();
							if (afterCM.startsWith("<") && afterCM.endsWith(">")) {
								breakParagraph = true;
								line = line.substring(0, posCM) + afterCM;
							}
						} else {
							breakParagraph = true;
							line = line.substring(0, line.length()-"<CM>".length());
						}
					}
					
					line = line.replaceAll("<CM>", "\n\n");
					line = line.replaceAll("\n\n \n\n", "\n\n");
					line = line.replaceAll("\n\n\n\n", "\n\n");
					
					boolean bookmarked = false;
					
					if (prevBreakParagraph) {
						verseParallelList.add(new DisplayVerse(i, line, bookmarked, true));
					} else {
						verseParallelList.add(new DisplayVerse(i, line, bookmarked, false));
					}
					prevBreakParagraph = breakParagraph;
					verseNotAvailable = false;
				}
				
				if (verseNotAvailable) {
					if (chapterIndex < 929) {
						txtEmpty2.setText(getResources().getString(R.string.no_ot));
					} else {
						txtEmpty2.setText(getResources().getString(R.string.no_nt));
					}
				}
				
				parallelAdapter.notifyDataSetChanged();
				
				final ListView listviewParallel = (ListView) findViewById(R.id.listviewParallel);			
				if (fromBookmarks) {
					listviewParallel.post(new Runnable() {
						@Override
						public void run() {						
							listviewParallel.setSelection(bookmarkVerseStart-1);
						}
					});
				} else if (resetScroll) {
					listviewParallel.post(new Runnable() {
						@Override
						public void run() {
							listviewParallel.setSelection(0);
						}
					});
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		updateBookLanguage();
		
		if (fromBookmarks) {
			fromBookmarks = false;
		}
	}
	
	private void readBibleBookName() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File sdcard = Environment.getExternalStorageDirectory();
			File bookNameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
			if (!bookNameFolder.isDirectory()) {
				boolean success = bookNameFolder.mkdirs();
				if (!success) {
					return;
				}
			}
			File[] bookNameFiles = bookNameFolder.listFiles();
			if (bookNameFiles.length == 0) {
				try {
					File fEnglish = new File(bookNameFolder, Constants.LANG_ENGLISH + ".bkn");
					PrintWriter outEnglish = new PrintWriter(fEnglish, "UTF-8");
					for (String bookName : Constants.arrBookName) {
						String str = bookName;
						if ("Judges".equals(bookName)) {
							str = str + ";;Judg";
						} else if ("Jude".equals(bookName)) {
							str = str + ";;Jude";
						} else if ("John".equals(bookName)) {
							str = str + ";;Jn";
						} else if ("1 John".equals(bookName)) {
							str = str + ";;1 Jn";
						} else if ("2 John".equals(bookName)) {
							str = str + ";;2 Jn";
						} else if ("3 John".equals(bookName)) {
							str = str + ";;3 Jn";
						} else if ("Philemon".equals(bookName)) {
							str = str + ";;Phm";
						}
						outEnglish.println(str);
					}
					outEnglish.flush();
					outEnglish.close();
					
					File fIndo = new File(bookNameFolder, Constants.LANG_BAHASA + ".bkn");
					PrintWriter outIndo = new PrintWriter(fIndo, "UTF-8");
					for (String bookName : Constants.arrBookNameIndo) {
						outIndo.println(bookName);
					}
					outIndo.flush();
					outIndo.close();
					
				} catch (Exception e) {
					Log.d(TAG, "Error write bookname file", e);
				} 
			}
			
			bookNameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
			bookNameFiles = bookNameFolder.listFiles();
			File bookNameFile = new File(bookNameFolder, currentBookLanguage + ".bkn");
			if (bookNameFile.isFile()) {
				readBookNameFile(bookNameFile);
			} else {
				loadDefaultBookName();
			}
		}
	}

	private void readBookNameFile(File bookNameFile) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(bookNameFile), "UTF-8"), 8192);
			String line = null;
			int i = 0;
			while (((line = br.readLine()) != null) && (i < 66)) {
				line = line.trim();
				String bookName = line;
				String abbr = "";
				if (bookName.startsWith("1") || bookName.startsWith("2") || bookName.startsWith("3")) {
	            	abbr = bookName.substring(0,5);
	            } else {
	            	abbr = bookName.substring(0,3);
	            }
				if (line.indexOf(";;") > -1) {
					int pos = line.indexOf(";;");
					bookName = line.substring(0, pos);
					abbr = line.substring(pos + 2);
				}
				Constants.arrActiveBookName[i] = bookName;
				Constants.arrActiveBookAbbr[i] = abbr;
				i++;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error Read Book Name " + bookNameFile, e);
			loadDefaultBookName();
		}
	}
	
	private String[] readBibleFiles() {
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Log.d(TAG, "SD CARD not available");
			handler.post(new Runnable() {
				@Override
				public void run() {
					txtEmpty.setText(getResources().getString(R.string.sdcard_error));
					Toast.makeText(BiblesOffline.this, R.string.sdcardNotReady, Toast.LENGTH_LONG).show();
				}
			});
			return null;
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		File bibleFolder = new File(sdcard.getPath() + Constants.BIBLE_FOLDER);
		List<BibleVersion> bibleList = databaseHelper.getAllBibleVersion();
		String[] result = null;
		List<String> bibleNames = new ArrayList<String>();
		StringBuffer fileNames = new StringBuffer();

		if (!bibleFolder.isDirectory()) {
			boolean success = bibleFolder.mkdirs();			
			Log.d(TAG, "Creating bible directory success: " + success);
		} else {
			File[] arrFile = bibleFolder.listFiles();
			if (arrFile != null && arrFile.length > 0) {
				Log.d(TAG, "Found " + arrFile.length
						+ " file(s) in bible directory");
				for (final File bibleFile : arrFile) {
					if (!bibleFile.getName().toLowerCase().endsWith(".ont")) continue;
					fileNames.append(",'").append(bibleFile.getName()).append("'");
					boolean doIndexing = false;
					BibleVersion compareBible = new BibleVersion();
					compareBible.setFileName(bibleFile.getName());
					BibleVersion bibleOnDb = null;
					int indexBible = bibleList.indexOf(compareBible);
					if (indexBible == -1) {
						doIndexing = true;
						bibleOnDb = new BibleVersion();
						bibleOnDb.setId(-1L);
					} else {
						bibleOnDb = bibleList.get(indexBible);
						if (!bibleOnDb.getLastModified().equals(
								bibleFile.length())) {
							doIndexing = true;
						}
					}
					if (doIndexing) {
						Log.d(TAG, "Indexing " + bibleFile.getName());
						if (pd.isShowing()) {
							handler.post(new Runnable() {
		                        public void run() {
		                        		pd.setMessage("Indexing " + bibleFile.getName());
		                        }
			                });
						}
						String indexName = bibleFile.getAbsolutePath().replaceAll(".ont", ".idx");
						File fIndex = new File(indexName);

						DataOutputStream out = null;
						BufferedReader raf = null;
						InputStreamReader in = null;
						char[] bufChar = new char[8192];
						int offset = 0;	
						int startOffset = 0;
						try {
							out = new DataOutputStream(new FileOutputStream(fIndex));
							
							in = new InputStreamReader(new FileInputStream(bibleFile), "UTF-8");
							int numChar = in.read(bufChar);
							startOffset = startOffset + numChar;
							
							int j = 0;
							int verseCount = 1;
							int verseCountIdx = 0;		
							int totalVerse = Integer.valueOf(Constants.arrVerseCount[verseCountIdx].substring(Constants.arrVerseCount[verseCountIdx].lastIndexOf(";") + 1));
							
							byte[] bomUtf8 = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
							if (Character.toString(bufChar[0]).equals(new String(bomUtf8, "UTF-8"))) {
								offset = 1;
							}
							out.writeInt(offset);
							
							int prevBook = 0;
							boolean done = false;
							int eolLength = 0;
							while (numChar > 0 && !done) {
								for (int i=0; i < numChar; i++) {
									if (bufChar[i] == '\n') {
										if (eolLength == 0) {
											if (bufChar[i-1] == '\r') {
												eolLength = 2;
											} else {
												eolLength = 1;
											}
										}
										
										offset = startOffset - numChar + i;
										
										verseCount++;
										if (verseCount > totalVerse) {
											verseCount = 1;
											if (verseCountIdx < Constants.arrVerseCount.length-1) {
												final int book = Integer.valueOf(Constants.arrVerseCount[verseCountIdx].substring(0, Constants.arrVerseCount[verseCountIdx].indexOf(";")));
												if (prevBook != book) {
													prevBook = book;
													if (pd.isShowing()) {
														handler.post(new Runnable() {
									                        public void run() {
									                            pd.setMessage("Indexing " + bibleFile.getName() + " " + Constants.arrBookName[book-1]);
									                        }
										                });
													}
												}
												out.writeInt(offset + 1);
												verseCountIdx++;
												totalVerse = Integer.valueOf(Constants.arrVerseCount[verseCountIdx].substring(Constants.arrVerseCount[verseCountIdx].lastIndexOf(";") + 1));
											} else {
												done = true;
												break;
											}
										}
									}
								}																
								numChar = in.read(bufChar);
								startOffset = startOffset + numChar;
								j++;
							}
							in.close();
							in = null;
							if (!done) {
								throw new IndexOutOfBoundsException("Bible file " + bibleFile.getName()	+ " is not valid");
							}
							
							raf = new BufferedReader(new InputStreamReader(new FileInputStream(bibleFile), "UTF-8"), 8192);
							raf.skip(offset+1);
							String line = null;
							String bibleName = bibleFile.getName();
							String about=null;
							boolean startAbout = false;
							StringBuffer sbAbout = new StringBuffer();
							while ((line = raf.readLine()) != null) {
								if (line.startsWith("description=")) {
									bibleName = line.substring("description=".length());
								}
								if (line.startsWith("about=")) {
									sbAbout = sbAbout.append(line.substring("about=".length()));
									if (line.endsWith("\\")) {
										startAbout = true;
										sbAbout.delete(sbAbout.length()-1,sbAbout.length());
									}
								} else {
									if (startAbout) {
										sbAbout.append(" ").append(line);
										if (!line.endsWith("\\")) {
											startAbout = false;
										} else {
											sbAbout.delete(sbAbout.length()-1,sbAbout.length());
										}
									}
								}
							}
							if (sbAbout.length() == 0) {
								sbAbout.append(bibleName);
							}
							about = sbAbout.toString();
							
							bibleOnDb.setEolLength(eolLength);
							bibleOnDb.setFileName(bibleFile.getName());
							bibleOnDb.setLastModified(bibleFile.length());
							bibleOnDb.setBibleName(bibleName);
							bibleOnDb.setAbout(about);
							databaseHelper.saveOrUpdateBibleVersion(bibleOnDb);
							currentBibleName = bibleName;
							currentBibleFilename = bibleFile.getName();
							Log.d(TAG, "File " + bibleFile.getName()
									+ " indexed successfully");
							bibleNames.add(bibleName);
						} catch (Exception e) {
							Log.d(TAG, "Error reading file: " + bibleFile, e);
							bibleFile.delete();
						} finally {
							try {
								if (in != null) 
									in.close();
								if (out != null)
									out.close();
								if (raf != null)
									raf.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}
					} else {
						BibleVersion bibleVersion = bibleList.get(indexBible);
						bibleNames.add(bibleVersion.getBibleName());
						Log.d(TAG, "File " + bibleFile.getName() + " already indexed");
					}
				}
			}
		}
		if (bibleNames.size() > 0) {
			result = new String[bibleNames.size()];
			result = bibleNames.toArray(result);
			Arrays.sort(result);
		}
		databaseHelper.deleteInvalidBible(fileNames);
		return result;
	}

	@Override
	public void onClick(View v) {
		if (currentChapterIdx == -1) return;
		TextView txtVerse;
		String str;
		StringBuffer sb; 
		switch (v.getId()) {
		case R.id.btnNext:
			if (currentChapterIdx == Constants.arrVerseCount.length - 1) {
				currentChapterIdx = 0;
			} else {
				currentChapterIdx++;
			}
			displayBible(currentBibleFilename, currentChapterIdx);
			break;
		case R.id.btnPrev:
			if (currentChapterIdx == 0) {
				currentChapterIdx = Constants.arrVerseCount.length - 1;
			} else {
				currentChapterIdx--;
			}
			displayBible(currentBibleFilename, currentChapterIdx);
			break;
		case R.id.txtCurrent:
			if (bibleList == null || bibleList.size() == 0) {
				Toast.makeText(this, R.string.gotoNoBible, Toast.LENGTH_LONG).show();
				break;
			}
			gotoBrowse = true;
			lastChapterIdx = currentChapterIdx;
			startActivity(new Intent(this, GoTo.class));
			break;
		case R.id.btnFullscreen:
			LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
			if (bottomBar.getVisibility() == View.VISIBLE) {
				bottomBar.setVisibility(View.GONE);
				isFullScreen = true;
			} else {
				bottomBar.setVisibility(View.VISIBLE);
				isFullScreen = false;
			}
			break;
		case R.id.btnZoomIn:
			currentFontSize+=1;
			updateBibleFontSize();
			break;
		case R.id.btnZoomOut:
			currentFontSize-=1;
			updateBibleFontSize();
			break;	
		case R.id.btnPlus:
			if (bookmarkVerseEnd >= bookmarkVerseMax) return;
			bookmarkVerseEnd++;
			DisplayVerse verseToAdd = verseList.get(bookmarkVerseEnd-1);			
			txtVerse = (TextView) bookmarkView.findViewById(R.id.txtVerse);			
			str = txtVerse.getText().toString();
			sb = new StringBuffer(str);
			sb.append(" ");
			sb.append(Util.parseVerse(verseToAdd.getVerse()));
			txtVerse.setText(sb.toString());
			refreshBookNameOnBookmarkDialog();
			break;
		case R.id.btnMinus:
			if (bookmarkVerseEnd <= bookmarkVerseStart) return;
			DisplayVerse verseToDelete = verseList.get(bookmarkVerseEnd-1);
			bookmarkVerseEnd--;
			int lengthDelete = Util.parseVerse(verseToDelete.getVerse()).length() + 1;
			txtVerse = (TextView) bookmarkView.findViewById(R.id.txtVerse);			
			str = txtVerse.getText().toString();
			sb = new StringBuffer(str);
			sb.delete(sb.length()-lengthDelete, sb.length());
			txtVerse.setText(sb.toString());
			refreshBookNameOnBookmarkDialog();
			break;
		case R.id.btnPlusClipboard:
			if (bookmarkVerseEnd >= bookmarkVerseMax) return;
			bookmarkVerseEnd++;
			DisplayVerse verseToAddClipboard = verseList.get(bookmarkVerseEnd-1);			
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);			
			str = txtVerse.getText().toString();
			sb = new StringBuffer(str);
			sb.append(" ");
			sb.append(Util.parseVerse(verseToAddClipboard.getVerse()));
			txtVerse.setText(sb.toString());
			refreshBookNameOnCopyToClipboardDialog();
			break;
		case R.id.btnMinusClipboard:
			if (bookmarkVerseEnd <= bookmarkVerseStart) return;
			DisplayVerse verseToDeleteClipboard = verseList.get(bookmarkVerseEnd-1);
			bookmarkVerseEnd--;
			lengthDelete = Util.parseVerse(verseToDeleteClipboard.getVerse()).length() + 1;
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);			
			str = txtVerse.getText().toString();
			sb = new StringBuffer(str);
			sb.delete(sb.length()-lengthDelete, sb.length());
			txtVerse.setText(sb.toString());
			refreshBookNameOnCopyToClipboardDialog();
			break;
		case R.id.txtBibleName:
			if (!isParallel) {
				dialogBibles.show();
			} else {
				gotoSelectParallel = true;
				startActivity(new Intent(this, SelectParallelBible.class));
			}
			break;
		}
	}

	@Override
	protected void onDestroy() {
		//close database
		databaseHelper.close();
		super.onDestroy();
	}

	@Override
	public void onClick(DialogInterface v, int buttonId) {
		if (v.equals(bookmarkDialog)) {
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				Spinner spnCategory = (Spinner) bookmarkView.findViewById(R.id.spnCategory);
				TextView txtVerse = (TextView) bookmarkView.findViewById(R.id.txtVerse);
				Long categoryId = databaseHelper.getCategoryIdByCategoryName((String)spnCategory.getSelectedItem());
				SimpleDateFormat isoFormat = new SimpleDateFormat(Constants.DB_DATE_FORMAT);
				String[] arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
		  		int book = Integer.parseInt(arrBookChapter[0]);
		  		int chapter = Integer.parseInt(arrBookChapter[1]);
				Bookmark bm = new Bookmark();
				bm.setCategoryId(categoryId);
				bm.setBook(book);
				bm.setChapter(chapter);
				bm.setVerseStart(bookmarkVerseStart);
				bm.setVerseEnd(bookmarkVerseEnd);
				bm.setContent(txtVerse.getText().toString());
				bm.setBible(currentBibleFilename.substring(0, currentBibleFilename.length()-4));
				bm.setBookmarkDate(isoFormat.format(new Date()));
				databaseHelper.insertReplaceBookmark(bm);
				displayBible(currentBibleFilename, currentChapterIdx, false);
			}
		} else if (v.equals(copyToClipboardDialog)) {
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				TextView txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);
				TextView txtBook = (TextView) copyToClipboardView.findViewById(R.id.txtBook);
				if (copyOrShare == 'c') {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
					clipboard.setText(txtBook.getText() + " " + txtVerse.getText());
					String str = getResources().getString(R.string.copiedSuccess);
					String msg = String.format(str, txtBook.getText());
					Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
				} else if (copyOrShare == 's') {
					Intent i=new Intent(android.content.Intent.ACTION_SEND);
					i.setType("text/plain");
					i.putExtra(Intent.EXTRA_SUBJECT, txtBook.getText());
					i.putExtra(Intent.EXTRA_TEXT, txtBook.getText() + " " + txtVerse.getText());
					startActivity(Intent.createChooser(i, "Share"));
				}
			}
		}

	}

	private void updateBibleInfo() {
		TextView txtBibleName = (TextView) findViewById(R.id.txtBibleName);
		if (!isParallel) {
			txtBibleName.setText(currentBibleName);
		} else {
			String bible1 = currentBibleFilename;
			if (bible1 != null && bible1.length() > 4) {
				bible1 = bible1.substring(0, currentBibleFilename.length()-4);
				bible1 = bible1.toUpperCase();
			}
			String bible2 = currentBibleFilename2;
			if (bible2 != null && bible2.length() > 4) {
				bible2 = bible2.substring(0, currentBibleFilename2.length()-4);
				bible2 = bible2.toUpperCase();
			}
			txtBibleName.setText(bible1 + " / " + bible2);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// Save the current position
		Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		editor.putInt(Constants.POSITION_CHAPTER, currentChapterIdx);
		editor.putString(Constants.POSITION_BIBLE_NAME, currentBibleFilename);
		editor.putString(Constants.POSITION_BIBLE_NAME_2, currentBibleFilename2);
		editor.putString(Constants.BOOK_LANGUAGE, currentBookLanguage);
		editor.putInt(Constants.FONT_SIZE, currentFontSize);
		editor.putBoolean(Constants.FULL_SCREEN, isFullScreen);
		editor.putBoolean(Constants.PARALLEL, isParallel);
		editor.commit();
		//save history
		databaseHelper.saveHistory(historyList);
		//close database
		databaseHelper.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		databaseHelper.open();
		databaseHelper.getHistory(historyList);
		historyAdapter.notifyDataSetChanged();
		
		fillSpinnerCategory();
		if (gotoPrefs) {
			gotoPrefs = false;
			SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			if (!defaultPrefs.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH).equals(currentBookLanguage)) {
				currentBookLanguage = defaultPrefs.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
				
				File sdcard = Environment.getExternalStorageDirectory();
				File bookNameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
				File bookNameFile = new File(bookNameFolder, currentBookLanguage + ".bkn");
				if (bookNameFile.isFile()) {					
					readBookNameFile(bookNameFile);
				} else {
					loadDefaultBookName();
				}
				
				updateBookLanguage();
			}
		} else if (gotoDownloadBible) {
			gotoDownloadBible = false;
			this.pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
			txtEmpty.setText(getResources().getString(R.string.no_bibles));
			new LoadingTask().execute((Object)null);
		} else if (gotoBrowse) {
			gotoBrowse = false;
			SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
			currentChapterIdx = preference.getInt(Constants.POSITION_CHAPTER, 0);
			if (lastChapterIdx != currentChapterIdx) {
				displayBible(currentBibleFilename, currentChapterIdx);
			}
		} else if (gotoDocuments) {
			gotoDocuments = false;
			SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
			if (isFullScreen != preference.getBoolean(Constants.FULL_SCREEN, false)) {
				LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
				if (isFullScreen) {
					bottomBar.setVisibility(View.GONE);
				} else {
					bottomBar.setVisibility(View.VISIBLE);
				}
			}
			if (isParallel != preference.getBoolean(Constants.PARALLEL, false)) {
				applyParallel(isParallel);
			}
		} else if (gotoSelectParallel) {
			gotoSelectParallel = false;
			SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);			
			currentBibleFilename = preference.getString(Constants.POSITION_BIBLE_NAME, "");
			currentBibleFilename2 = preference.getString(Constants.POSITION_BIBLE_NAME_2, "");
//			currentBibleName = preference.getString(Constants.BIBLE_NAME, "");
			if ("".equals(currentBibleFilename2)) {
				isParallel = false;
				applyParallel(isParallel);
			}
			updateBibleInfo();
			displayBible(currentBibleFilename, currentChapterIdx, false);
		}
	}

	
	private void loadDefaultBookName() {
		currentBookLanguage = Constants.LANG_ENGLISH;
		Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		editor.putString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
		editor.commit();
		for (int i=0; i<66; i++) {
			Constants.arrActiveBookName[i] = Constants.arrBookName[i];
			String bookName = Constants.arrBookName[i];
			String abbr = "";
			if (bookName.startsWith("1") || bookName.startsWith("2") || bookName.startsWith("3")) {
            	abbr = bookName.substring(0,5);
            } else {
            	abbr = bookName.substring(0,3);
            }
			Constants.arrActiveBookAbbr[i] = abbr;
		}
	}
	
	// menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	private void applyParallel(boolean isParallel) {
		LinearLayout linearParallel = (LinearLayout) findViewById(R.id.linearParallel);
		int height = 0;
		LinearLayout line = (LinearLayout) findViewById(R.id.horizontalLine);
		if (isParallel) {
			height = (int) (getWindowManager().getDefaultDisplay().getHeight() * 0.45);
			line.setVisibility(View.VISIBLE);
		} else {
			line.setVisibility(View.GONE);
		}
		int width = getWindowManager().getDefaultDisplay().getWidth();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
		linearParallel.setLayoutParams(params);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.parallel:
				if (bibleList == null || bibleList.size() < 2) {
					Toast.makeText(this, R.string.needMoreTranslation, Toast.LENGTH_LONG).show();
					return true;
				}
				isParallel = !isParallel;				
				applyParallel(isParallel);
				if (isParallel) {
					displayBible(currentBibleFilename, currentChapterIdx);
				}
				updateBibleInfo();
				if (isParallel && (currentBibleFilename2 == null || "".equals(currentBibleFilename2))) {
					gotoSelectParallel = true;
					startActivity(new Intent(this, SelectParallelBible.class));
				}
				return true;
			case R.id.contactAuthor:
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("message/rfc822");
				i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"yohan.yudanara@gmail.com"});
				i.putExtra(Intent.EXTRA_SUBJECT, "[OpenBibles] Question");
				i.putExtra(Intent.EXTRA_TEXT   , "");
			    startActivity(Intent.createChooser(i, "Send mail..."));
				return true;
			case R.id.bookmark:
				String state = Environment.getExternalStorageState();
				if (!Environment.MEDIA_MOUNTED.equals(state)) {
					Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG).show();
					return true;
				}
				startActivity(new Intent(this, Bookmarks.class));
				return true;
			case R.id.find:
				Intent find = new Intent(this, Find.class);
				find.putExtra(Constants.CURRENT_BIBLE, currentBibleName);
				startActivity(find);
				return true;
			case R.id.about:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				String[] arrImport = new String[] {"About " + currentBibleName, "About Open Bibles"};
				ListView viewChooseAbout = new ListView(this);
				ad.setView(viewChooseAbout);		
				final AlertDialog dialogChooseAbout = ad.create();
				dialogChooseAbout.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
				viewChooseAbout.setAdapter(new ArrayAdapter<String>(this, R.layout.listitemmedium, arrImport));
				viewChooseAbout.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						dialogChooseAbout.dismiss();
						if (position==0) {
							Intent i = new Intent(BiblesOffline.this, AboutBible.class);
							i.putExtra(Constants.CURRENT_BIBLE, currentBibleName);
							startActivity(i);
						} else if (position==1) {
							startActivity(new Intent(BiblesOffline.this, About.class));
						}
					}
				});
				dialogChooseAbout.show();
				return true;	
			case R.id.download:
				gotoDownloadBible = true;
				startActivity(new Intent(this, DownloadBible.class));
				return true;
			case R.id.history:
				dialogHistory.show();
				return true;
			case R.id.help:
				Intent iHelp = new Intent(this, Help.class);
				iHelp.putExtra(Constants.FONT_SIZE, currentFontSize);
				iHelp.putExtra(Constants.HELP_CONTENT, R.string.help_main);
				startActivity(iHelp);
				return true;
			case R.id.document:
				state = Environment.getExternalStorageState();
				if (!Environment.MEDIA_MOUNTED.equals(state)) {
					Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG).show();
					return true;
				}
				startActivity(new Intent(this, Documents.class));
				return true;
			case R.id.settings:
				gotoPrefs = true;
				startActivity(new Intent(this, Prefs.class));
				return true;
			case R.id.downloadBookname:
				gotoPrefs = true;
				startActivity(new Intent(this, DownloadBookname.class));
				return true;
		}
		return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
		    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
		    Log.e(TAG, "bad menuInfo", e);
		    return;
		}
		
		DisplayVerse verse = verseList.get(info.position);
		if (verse == null) return;
		if (verse.isBookmark()) {
			menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.editBookmark);
			menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, R.string.removeBookmark);
			menu.add(Menu.NONE, Menu.FIRST+3, Menu.NONE, R.string.copyToClipboard);
			menu.add(Menu.NONE, Menu.FIRST+5, Menu.NONE, R.string.share);
		} else {
			menu.add(Menu.NONE, Menu.FIRST+2, Menu.NONE, R.string.addBookmark);
			menu.add(Menu.NONE, Menu.FIRST+4, Menu.NONE, R.string.copyToClipboard);
			menu.add(Menu.NONE, Menu.FIRST+6, Menu.NONE, R.string.share);
		}
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
	    
	    DisplayVerse verse = verseList.get(index);
	    if (verse == null) return false;
	    TextView txtVerse = (TextView) bookmarkView.findViewById(R.id.txtVerse);
		
		switch (item.getItemId()) {
		case Menu.FIRST : //edit Bookmark
			String[] arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
	  		int book = Integer.parseInt(arrBookChapter[0]);
	  		int chapter = Integer.parseInt(arrBookChapter[1]);
			Bookmark bm = databaseHelper.getBookmark(book, chapter, verse.getVerseNumber());
			bookmarkVerseStart = bm.getVerseStart();
			bookmarkVerseEnd = bm.getVerseEnd();
			txtVerse.setTextSize(currentFontSize);
			StringBuffer sb = new StringBuffer();
			for (int j=bookmarkVerseStart; j<=bookmarkVerseEnd; j++) {
				DisplayVerse v = verseList.get(index + (j-bookmarkVerseStart));
				sb.append(Util.parseVerse(v.getVerse())).append(" ");
			}
			txtVerse.setText(sb.substring(0, sb.length()-1));
			refreshBookNameOnBookmarkDialog();
			Spinner spnCategory = (Spinner) bookmarkView.findViewById(R.id.spnCategory);
			for (int i = 0; i < spnCategory.getAdapter().getCount(); i++) {
				String categoryName = (String) spnCategory.getAdapter().getItem(i);
				if (categoryName.equals(bm.getCategoryName())) {
					spnCategory.setSelection(i);
					break;
				}
			}
			bookmarkDialog.show();			
			return true;
		case Menu.FIRST+1 : //remove Bookmark
			final int verseNumber = verse.getVerseNumber();
			new AlertDialog.Builder(this)
		        .setTitle(R.string.removeBookmark)
		        .setMessage(R.string.reallyRemoveBookmark)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	String[] arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
		            	int book = Integer.parseInt(arrBookChapter[0]);
				  		int chapter = Integer.parseInt(arrBookChapter[1]);
		                databaseHelper.removeBookmark(book, chapter, verseNumber);
		                displayBible(currentBibleFilename, currentChapterIdx, false);
		            }
	
		        })
		        .setNegativeButton(R.string.no, null)
		        .show();
			return true;
		case Menu.FIRST+2 : //add Bookmark
			bookmarkVerseStart = verse.getVerseNumber();
			bookmarkVerseEnd = verse.getVerseNumber();
			txtVerse.setTextSize(currentFontSize);
			txtVerse.setText(Util.parseVerse(verse.getVerse()));
			refreshBookNameOnBookmarkDialog();
			bookmarkDialog.show();			
			return true;
		case Menu.FIRST+3 : //copy bookmarked verse to clipboard
			copyOrShare = 'c';
			arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
	  		book = Integer.parseInt(arrBookChapter[0]);
	  		chapter = Integer.parseInt(arrBookChapter[1]);
			bm = databaseHelper.getBookmark(book, chapter, verse.getVerseNumber());
			bookmarkVerseStart = bm.getVerseStart();
			bookmarkVerseEnd = bm.getVerseEnd();
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);
			txtVerse.setTextSize(currentFontSize);
			sb = new StringBuffer();
			for (int j=bookmarkVerseStart; j<=bookmarkVerseEnd; j++) {
				DisplayVerse v = verseList.get(index + (j-bookmarkVerseStart));
				sb.append(Util.parseVerse(v.getVerse())).append(" ");
			}
			txtVerse.setText(sb.substring(0, sb.length()-1));
			refreshBookNameOnCopyToClipboardDialog();
			copyToClipboardDialog.setTitle("Copy to clipboard");
			copyToClipboardDialog.show();			
			return true;
		case Menu.FIRST+4 : //copy unbookmarked verse to clipboard
			copyOrShare = 'c';
			bookmarkVerseStart = verse.getVerseNumber();
			bookmarkVerseEnd = verse.getVerseNumber();
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);
			txtVerse.setTextSize(currentFontSize);
			txtVerse.setText(Util.parseVerse(verse.getVerse()));
			refreshBookNameOnCopyToClipboardDialog();
			copyToClipboardDialog.setTitle("Copy to clipboard");
			copyToClipboardDialog.show();			
			return true;
		case Menu.FIRST+5: //share bookmarked
			copyOrShare = 's';
			arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
	  		book = Integer.parseInt(arrBookChapter[0]);
	  		chapter = Integer.parseInt(arrBookChapter[1]);
			bm = databaseHelper.getBookmark(book, chapter, verse.getVerseNumber());
			bookmarkVerseStart = bm.getVerseStart();
			bookmarkVerseEnd = bm.getVerseEnd();
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);
			txtVerse.setTextSize(currentFontSize);
			sb = new StringBuffer();
			for (int j=bookmarkVerseStart; j<=bookmarkVerseEnd; j++) {
				DisplayVerse v = verseList.get(index + (j-bookmarkVerseStart));
				sb.append(Util.parseVerse(v.getVerse())).append(" ");
			}
			txtVerse.setText(sb.substring(0, sb.length()-1));
			refreshBookNameOnCopyToClipboardDialog();
			copyToClipboardDialog.setTitle("Share verse");
			copyToClipboardDialog.show();
			return true;
		case Menu.FIRST+6 : //share unbookmarked
			copyOrShare = 's';
			bookmarkVerseStart = verse.getVerseNumber();
			bookmarkVerseEnd = verse.getVerseNumber();
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);
			txtVerse.setTextSize(currentFontSize);
			txtVerse.setText(Util.parseVerse(verse.getVerse()));
			refreshBookNameOnCopyToClipboardDialog();
			copyToClipboardDialog.setTitle("Share verse");
			copyToClipboardDialog.show();			
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void refreshBookNameOnBookmarkDialog() {
		String[] arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
  		int book = Integer.parseInt(arrBookChapter[0]);
  		int chapter = Integer.parseInt(arrBookChapter[1]);
  		bookmarkVerseMax = Integer.parseInt(arrBookChapter[2]);
  		StringBuffer sbBook = new StringBuffer();
		
//		if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//			sbBook.append(Constants.arrBookNameIndo[book - 1]).append(" ").append(chapter);
//		} else {
//			sbBook.append(Constants.arrBookName[book - 1]).append(" ").append(chapter);
//		}
  		sbBook.append(Constants.arrActiveBookName[book - 1]).append(" ").append(chapter);
		
		sbBook.append(":").append(bookmarkVerseStart);
		if (bookmarkVerseStart != bookmarkVerseEnd) {
			sbBook.append("-").append(bookmarkVerseEnd);
		}
		
		TextView txtBook = (TextView) bookmarkView.findViewById(R.id.txtBook);
		txtBook.setText(sbBook.toString());
	}
	
	private void refreshBookNameOnCopyToClipboardDialog() {
		String[] arrBookChapter = Constants.arrVerseCount[currentChapterIdx].split(";");
  		int book = Integer.parseInt(arrBookChapter[0]);
  		int chapter = Integer.parseInt(arrBookChapter[1]);
  		bookmarkVerseMax = Integer.parseInt(arrBookChapter[2]);
  		StringBuffer sbBook = new StringBuffer();
		
//		if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//			sbBook.append(Constants.arrBookNameIndo[book - 1]).append(" ").append(chapter);
//		} else {
//			sbBook.append(Constants.arrBookName[book - 1]).append(" ").append(chapter);
//		}
  		sbBook.append(Constants.arrActiveBookName[book - 1]).append(" ").append(chapter);
		
		sbBook.append(":").append(bookmarkVerseStart);
		if (bookmarkVerseStart != bookmarkVerseEnd) {
			sbBook.append("-").append(bookmarkVerseEnd);
		}
		
		TextView txtBook = (TextView) copyToClipboardView.findViewById(R.id.txtBook);
		txtBook.setText(sbBook.toString());
	}
	
	private void fillSpinnerCategory() {
		Spinner spnCategory = (Spinner) bookmarkView.findViewById(R.id.spnCategory);
		List<String> categoryList = databaseHelper.getBookmarkCategoryList();
		String[] arrCategory = new String[categoryList.size()];
		arrCategory = categoryList.toArray(arrCategory);
		ArrayAdapter<String> aaCategory = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, arrCategory);
		aaCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnCategory.setAdapter(aaCategory);
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == viewHistory) {			
			dialogHistory.dismiss();
			currentChapterIdx = historyList.get(position);
			displayBible(currentBibleFilename, currentChapterIdx);
		} else if (parent == viewBibles) {
			dialogBibles.dismiss();
			String bibleName = bibleList.get(position);
			if (!currentBibleName.equals(bibleName)) {
				BibleVersion bibleVersion = databaseHelper.getBibleVersionByBibleName(bibleName);
				currentBibleFilename = bibleVersion.getFileName();
				currentBibleName = bibleName;
				updateBibleInfo();
				displayBible(currentBibleFilename, currentChapterIdx, false);
			}
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  if (isParallel) {
		  applyParallel(isParallel);
	  }
	}
	
	
}