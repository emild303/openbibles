package com.konibee.bible;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ReadBible extends ListActivity implements OnClickListener, OnItemClickListener, android.content.DialogInterface.OnClickListener {
	private List<DisplayVerse> verseList = new ArrayList<DisplayVerse>();
	private List<String> verseListWholeChapter = new ArrayList<String>();
	private DisplayVerseAdapterRead adapter;
	
	private DatabaseHelper databaseHelper;
	private String currentBibleFilename;
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
	
	private int bookmarkVerseStart;
	private int bookmarkVerseEnd;
	private int bookmarkVerseMax;
		
	private AlertDialog dialogBibles;
	private ListView viewBibles;
	private List<String> bibleList = new ArrayList<String>();
	private ArrayAdapter<String> biblesAdapter;
	
	private ProgressDialog pd = null;
	
	private String openBible;
	
	private static final String TAG = "ReadBible";
	private Handler handler = new Handler();
	
	private TextView txtEmpty;
	
	private int chapterIndex;
	
	private class LoadingTask extends AsyncTask<Object, Void, Object> {
		@Override
		protected Object doInBackground(Object... arg) {
			List<String> bibleList = databaseHelper.getBibleNameList();
			if (bibleList.size() == 0) {
				txtEmpty.setText(R.string.no_bible_file);
				return null;
			}
			populateBibleList(bibleList);
			return null;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			if (pd != null) {
				pd.dismiss();
			}
			displayBible(currentBibleFilename, true);
		}
	}
	
	public AlertDialog getFootnoteDialog() {
		return footnoteDialog;
	}
	
	public View getFootnoteView() {
		return footnoteView;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.readbible);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();
		
		adapter = new DisplayVerseAdapterRead(this, R.layout.row, verseList, currentFontSize);
		setListAdapter(adapter);
		registerForContextMenu(getListView());
		
		readPreference();
		LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
		if (isFullScreen) {
			bottomBar.setVisibility(View.GONE);
		} else {
			bottomBar.setVisibility(View.VISIBLE);
		}
		
		View btnFullscreen = findViewById(R.id.btnFullscreen);
		btnFullscreen.setOnClickListener(this);
		View btnZoomIn = findViewById(R.id.btnZoomIn);
		btnZoomIn.setOnClickListener(this);
		View btnZoomOut = findViewById(R.id.btnZoomOut);
		btnZoomOut.setOnClickListener(this);
		TextView txtBibleName = (TextView) findViewById(R.id.txtBibleName);
		txtBibleName.setOnClickListener(this);
		txtEmpty = (TextView) findViewById(android.R.id.empty);
		
		if (getIntent().getExtras() == null) return;
		openBible = getIntent().getExtras().getString(Constants.READ_BIBLE);
		TextView txtCurrent = (TextView) findViewById(R.id.txtCurrent);
		txtCurrent.setText(openBible);
		txtBibleName.setText(currentBibleName);
		
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
		
		builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.selectBible);
		biblesAdapter = new ArrayAdapter<String>(this, R.layout.listitemmedium, bibleList);
		viewBibles = new ListView(this);
		viewBibles.setAdapter(biblesAdapter);
		viewBibles.setOnItemClickListener(this);
		builder.setView(viewBibles);				
		dialogBibles = builder.create();
		
		updateBibleFontSize();
		this.pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
		new LoadingTask().execute((Object)null);
	}
	
	@Override
	protected void onDestroy() {
		//close database
		databaseHelper.close();
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		editor.putBoolean(Constants.FULL_SCREEN, isFullScreen);
		editor.commit();
		//close database
		databaseHelper.close();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		databaseHelper.open();
	}

	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentBibleFilename = preference.getString(Constants.POSITION_BIBLE_NAME, "");
		currentFontSize = preference.getInt(Constants.FONT_SIZE, 18);
		isFullScreen = preference.getBoolean(Constants.FULL_SCREEN, false);
		
		SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		currentBookLanguage = defaultPrefs.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
	}

	@Override
	public void onClick(View v) {
		TextView txtVerse;
		String str;
		StringBuffer sb; 
		switch (v.getId()) {
		case R.id.btnFullscreen:
			LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
			if (bottomBar.getVisibility() == View.GONE) {
				bottomBar.setVisibility(View.VISIBLE);
				isFullScreen = false;
			} else {
				bottomBar.setVisibility(View.GONE);
				isFullScreen = true;
			}
			break;
		case R.id.btnPlus:
			if (bookmarkVerseEnd >= bookmarkVerseMax) return;
			bookmarkVerseEnd++;
			txtVerse = (TextView) bookmarkView.findViewById(R.id.txtVerse);			
			str = txtVerse.getText().toString();
			sb = new StringBuffer(str);
			sb.append(" ");
			sb.append(Util.parseVerse(verseListWholeChapter.get(bookmarkVerseEnd-1)));
			txtVerse.setText(sb.toString());
			refreshBookNameOnBookmarkDialog();
			break;
		case R.id.btnMinus:
			if (bookmarkVerseEnd <= bookmarkVerseStart) return;
			int lengthDelete = Util.parseVerse(verseListWholeChapter.get(bookmarkVerseEnd-1)).length() + 1;
			bookmarkVerseEnd--;
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
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);			
			str = txtVerse.getText().toString();
			sb = new StringBuffer(str);
			sb.append(" ");
			sb.append(Util.parseVerse(verseListWholeChapter.get(bookmarkVerseEnd-1)));
			txtVerse.setText(sb.toString());
			refreshBookNameOnCopyToClipboardDialog();
			break;
		case R.id.btnMinusClipboard:
			if (bookmarkVerseEnd <= bookmarkVerseStart) return;
			lengthDelete = Util.parseVerse(verseListWholeChapter.get(bookmarkVerseEnd-1)).length() + 1;
			bookmarkVerseEnd--;
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);			
			str = txtVerse.getText().toString();
			sb = new StringBuffer(str);
			sb.delete(sb.length()-lengthDelete, sb.length());
			txtVerse.setText(sb.toString());
			refreshBookNameOnCopyToClipboardDialog();
			break;
		case R.id.btnZoomIn:
			currentFontSize+=1;
			updateBibleFontSize();
			break;
		case R.id.btnZoomOut:
			currentFontSize-=1;
			updateBibleFontSize();
			break;
		case R.id.txtBibleName:
			dialogBibles.show();			
			break;
		}
		
	}
	
	private void updateBibleFontSize() {
		adapter.updateFontSize(currentFontSize);
		adapter.notifyDataSetChanged();
		TextView txtFootnote = (TextView) footnoteView.findViewById(R.id.txtFootnote);
		txtFootnote.setTextSize(currentFontSize);
	}
	
	private void updateBibleInfo() {
		TextView txtBibleName = (TextView) findViewById(R.id.txtBibleName);
		txtBibleName.setText(currentBibleName);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == viewBibles) {
			dialogBibles.dismiss();
			String bibleName = bibleList.get(position);
			if (!currentBibleName.equals(bibleName)) {
				BibleVersion bibleVersion = databaseHelper.getBibleVersionByBibleName(bibleName);
				currentBibleFilename = bibleVersion.getFileName();
				currentBibleName = bibleName;
				updateBibleInfo();
				displayBible(currentBibleFilename, false);
			}
		}
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
	
	private void displayBible(String bibleFilename, boolean resetScroll) {
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG).show();
			return;
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		
		File file = new File(sdcard, Constants.BIBLE_FOLDER + "/" + bibleFilename);
		String indexFileName = file.getAbsolutePath().replaceAll(".ont", ".idx");
		File fIndex = new File(indexFileName);
		
		String str = getResources().getString(R.string.invalidOpenBible);
		String msg = String.format(str, openBible);
		txtEmpty.setText(msg);
		
		String passage = openBible;
		List<ReadBibleHelper> helperList = new ArrayList<ReadBibleHelper>();
		passage = passage.trim().toLowerCase();
		passage = passage.replaceAll(";", ",");
		passage = passage.replaceAll(" ", "");
		String[] arrBible = passage.split(",");
		
		int goBook = 0;
		int goChapter = 1;
		int goVerse = 1;
		char chapterOrVerse = 'C';
		for (String bible : arrBible) {
			Log.d(TAG, "nama bible: " + bible);
			int toVerse = 0;
			bible = bible.trim();
			
			String searchBook = bible;
			char lastChar = searchBook.charAt(searchBook.length() - 1);
			String strChapterAndVerse = "";
			if (lastChar >= '0' && lastChar <= '9') {
				do {
					strChapterAndVerse = lastChar + strChapterAndVerse;
					if (searchBook.length() > 1) {
						searchBook = searchBook.substring(0, searchBook.length() - 1);
						lastChar = searchBook.charAt(searchBook.length() - 1);
					} else {
						searchBook = "";
						break;
					}
				} while ((lastChar >= '0' && lastChar <= '9') || lastChar == '-' || lastChar == ':');
				if (bible.contains("Romans")) {
					Log.d(TAG, "search book " + searchBook);
				}
			}
			if (!"".equals(searchBook)) {
				chapterOrVerse = 'C';;
//				String[] firstSearch = null;
//				String[] secondSearch = null;
				
//				if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//					firstSearch = Constants.arrBookNameIndo;
//					secondSearch = Constants.arrBookName;
//				} else {
//					firstSearch = Constants.arrBookName;
//					secondSearch = Constants.arrBookNameIndo;
//				}
				
				goBook = searchBookFrom(Constants.arrDocBookName, searchBook);
				if (goBook == 0) goBook = searchBookFrom(Constants.arrDocBookAbbr, searchBook);
				if (goBook == 0) {
					goBook = searchBookFrom(Constants.arrBookName, searchBook);
					if (goBook != 0) {
						Constants.arrDocBookName = Constants.arrBookName;
					}
				}
				if (goBook == 0) {
					goBook = searchBookFrom(Constants.arrBookNameIndo, searchBook);
					if (goBook != 0) {
						Constants.arrDocBookName = Constants.arrBookNameIndo;
					}
				}
			}
			
			if (goBook == 0) continue;
			if ("".equals(strChapterAndVerse)) {
				int lastChapter = 1;
				if (goBook == 66) {
					lastChapter = 22;
				} else {
					lastChapter = Constants.arrBookStart[goBook] - Constants.arrBookStart[goBook-1];
				}
				strChapterAndVerse = "1-" + lastChapter;
			}
			int posDash = strChapterAndVerse.indexOf("-");
			String afterDash = null;
			if (posDash > -1) {
				if (strChapterAndVerse.length() > posDash+1) {
					afterDash = strChapterAndVerse.substring(posDash+1);
				}
				strChapterAndVerse = strChapterAndVerse.substring(0, posDash);
			}
			if (strChapterAndVerse.indexOf(":") > -1) {
				int posColon = strChapterAndVerse.indexOf(":");
				String strBeforeColon = strChapterAndVerse.substring(0, posColon);
				String strAfterColon = strChapterAndVerse.substring(posColon + 1);
				try { 
					goChapter = Integer.valueOf(strBeforeColon);
					goVerse = Integer.valueOf(strAfterColon);
					if (posDash == -1) {
						toVerse = goVerse;
					}
					chapterOrVerse = 'V';	
				} catch (NumberFormatException e) {
					e.printStackTrace();
					continue;
				}	
			} else if (strChapterAndVerse.length() > 0){
				if (chapterOrVerse == 'C') {
					try { 
						goChapter = Integer.valueOf(strChapterAndVerse);
						goVerse = 1;
					} catch (NumberFormatException e) {
						e.printStackTrace();
						continue;
					}	
				} else if (chapterOrVerse == 'V') {
					try { 
						goVerse = Integer.valueOf(strChapterAndVerse);
						toVerse = goVerse;
					} catch (NumberFormatException e) {
						e.printStackTrace();
						continue;
					}
				}
			}
			String bookChapter = goBook + ";" + goChapter;
			int chapterIdx = -1;
			for (int i = Constants.arrBookStart[goBook - 1]; i < Constants.arrVerseCount.length; i++) {
				if (Constants.arrVerseCount[i].startsWith(bookChapter)) {
					chapterIdx = i;
					break;
				}
			}
			if (chapterIdx == -1) continue;
			
			ReadBibleHelper helper = new ReadBibleHelper(chapterIdx, goVerse, toVerse);
			helperList.add(helper);
			
			if (afterDash != null) {
				int toChapter = -1;
				toVerse = -1;
				strChapterAndVerse = afterDash;
				if (strChapterAndVerse.indexOf(":") > -1) {
					int posColon = strChapterAndVerse.indexOf(":");
					String strBeforeColon = strChapterAndVerse.substring(0, posColon);
					String strAfterColon = strChapterAndVerse.substring(posColon + 1);
					try { 
						toChapter = Integer.valueOf(strBeforeColon);
						toVerse = Integer.valueOf(strAfterColon);
						if (toChapter < goChapter) continue;
						if (toChapter == goChapter && toVerse <= goVerse) continue;
						if (toChapter == goChapter && toVerse > goVerse) {
							helper.setVerseEnd(toVerse);
							continue;
						}
						int addChapter = chapterIdx;
						while (toChapter > goChapter) {
							addChapter++;
							goVerse = 1;
							helper = new ReadBibleHelper(addChapter, goVerse, 0);
							helperList.add(helper);
							goChapter ++;
							if (toChapter == goChapter) {
								helper.setVerseEnd(toVerse);
							}
						}
						chapterOrVerse = 'V';	
					} catch (NumberFormatException e) {
						e.printStackTrace();
						continue;
					}	
				} else {
					if (chapterOrVerse == 'C') {
						try { 
							toChapter = Integer.valueOf(strChapterAndVerse);
							toVerse = 0;
							int addChapter = chapterIdx;
							while (toChapter > goChapter) {
								addChapter++;
								goVerse = 1;
								helper = new ReadBibleHelper(addChapter, goVerse, 0);
								helperList.add(helper);
								goChapter ++;
							}
						} catch (NumberFormatException e) {
							e.printStackTrace();
							continue;
						}	
					} else if (chapterOrVerse == 'V') {
						try { 
							toVerse = Integer.valueOf(strChapterAndVerse);
							helper.setVerseEnd(toVerse);
						} catch (NumberFormatException e) {
							e.printStackTrace();
							continue;
						}
					}
				}
				if (toChapter == -1 && toVerse == -1) continue;
			}
		}
		
		int prevChapterIdx = -1;
		List<Integer> bookmarkList = new ArrayList<Integer>();
		verseList.clear();
		int startOffset = 0;
		int verseCount = 0;
		int ctrVerse = 0;
		BufferedReader br = null;
		int prevVerseListIndex = -1;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
			for (ReadBibleHelper helper : helperList) {				
				if (helper.getChapterIndex() != prevChapterIdx) {			
					bookmarkList = databaseHelper.getBookmarkVerseStartByChapterIndex(helper.getChapterIndex());
					DataInputStream is = new DataInputStream(new FileInputStream(fIndex));
					is.skip(helper.getChapterIndex()*4);
					startOffset = is.readInt();
					is.close();
					if (prevChapterIdx != -1) {
						br.close();
						br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
					}
					br.skip(startOffset);
					String[] arrBookChapter = Constants.arrVerseCount[helper.getChapterIndex()].split(";");
			  		int book = Integer.parseInt(arrBookChapter[0]);
			  		int chapter = Integer.parseInt(arrBookChapter[1]);
			  		verseCount = Integer.parseInt(arrBookChapter[2]);
			  		StringBuffer sbLine = new StringBuffer();
//					if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//						sbLine.append(Constants.arrBookNameIndo[book - 1]).append(" ").append(chapter);
//					} else {
//						sbLine.append(Constants.arrBookName[book - 1]).append(" ").append(chapter);
//					}
			  		sbLine.append(Constants.arrDocBookName[book - 1]).append(" ").append(chapter);
					if (helper.getVerseStart() != 1 || helper.getVerseEnd() != 0) {
						sbLine.append(":").append(helper.getVerseStart());
						if (helper.getVerseStart() != helper.getVerseEnd()) {
							sbLine.append("-");
							if (helper.getVerseEnd() != 0) {
								sbLine.append(helper.getVerseEnd());
							} else {
								sbLine.append(verseCount);
							}
						}
					}
					if (prevChapterIdx != -1) {
						sbLine.insert(0, "\n");
					}
					prevVerseListIndex = verseList.size();
					verseList.add(new DisplayVerse(0, sbLine.toString(), false, false));
					prevChapterIdx = helper.getChapterIndex();
					ctrVerse = 0;
				} else {
					if (prevVerseListIndex != -1) {
						DisplayVerse dv = verseList.get(prevVerseListIndex);
						StringBuffer sbLine = new StringBuffer(dv.getVerse()); 
						if (helper.getVerseStart() != 1 || helper.getVerseEnd() != 0) {
							sbLine.append(", ").append(helper.getVerseStart());
							if (helper.getVerseStart() != helper.getVerseEnd()) {
								sbLine.append("-");
								if (helper.getVerseEnd() != 0) {
									sbLine.append(helper.getVerseEnd());
								} else {
									sbLine.append(verseCount);
								}
							}
							dv.setVerse(sbLine.toString());
						}
					}
				}
				
				String line = "";
				boolean prevBreakParagraph = false;
				int lastVerse;
				if (helper.getVerseEnd() == 0) {
					lastVerse = verseCount;
				} else {
					lastVerse = helper.getVerseEnd();
				}
				while (ctrVerse < lastVerse) {
					line = br.readLine();
					ctrVerse++;
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}
					boolean breakParagraph = false;
					if (ctrVerse < helper.getVerseStart()) continue;
					
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
					if (bookmarkList.contains(Integer.valueOf(ctrVerse))) {
						bookmarked = true;
					}
					
					if (prevBreakParagraph) {
						verseList.add(new DisplayVerse(ctrVerse, line, bookmarked, true, helper.getChapterIndex()));
					} else {
						verseList.add(new DisplayVerse(ctrVerse, line, bookmarked, false, helper.getChapterIndex()));
					}
					prevBreakParagraph = breakParagraph;
				}
			}
			
			adapter.notifyDataSetChanged();
			
			if (resetScroll) {
				getListView().post(new Runnable() {
					@Override
					public void run() {
						getListView().setSelection(0);
					}
				});
			}
				

		} catch (Exception e) {
			Log.e(TAG, "Error opening passage", e);
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

	private int searchBookFrom(String[] arrBookName, String searchBook) {
		if (arrBookName == null) return 0;
		for (int i = 0; i < arrBookName.length; i++) {			
			if (arrBookName[i] == null) break;
			
			String book = arrBookName[i].toLowerCase();
			if (i==42) {
				Log.d(TAG, "hoho " + book + " " + searchBook);
			}
			
			book = book.replaceAll(" ", "");
			if (book.startsWith(searchBook)) {
				return i + 1;
			}
		}
		return 0;
	}

	@Override
	public void onClick(DialogInterface v, int buttonId) {
		if (v.equals(bookmarkDialog)) {
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				Spinner spnCategory = (Spinner) bookmarkView.findViewById(R.id.spnCategory);
				TextView txtVerse = (TextView) bookmarkView.findViewById(R.id.txtVerse);
				Long categoryId = databaseHelper.getCategoryIdByCategoryName((String)spnCategory.getSelectedItem());
				SimpleDateFormat isoFormat = new SimpleDateFormat(Constants.DB_DATE_FORMAT);
				String[] arrBookChapter = Constants.arrVerseCount[chapterIndex].split(";");
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
				displayBible(currentBibleFilename, false);
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
		if (verse.getVerseNumber() == 0) return;
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
	    
	    
	    final DisplayVerse verse = verseList.get(index);
	    if (verse == null) return false;
	    TextView txtVerse = (TextView) bookmarkView.findViewById(R.id.txtVerse);
	    chapterIndex = verse.getChapterIndex();
	    int verseIndex = verse.getVerseNumber()-1;
		
		switch (item.getItemId()) {
		case Menu.FIRST : //edit Bookmark
			loadVerseListWholeChapter(verse.getChapterIndex());
			String[] arrBookChapter = Constants.arrVerseCount[verse.getChapterIndex()].split(";");
	  		int book = Integer.parseInt(arrBookChapter[0]);
	  		int chapter = Integer.parseInt(arrBookChapter[1]);
			Bookmark bm = databaseHelper.getBookmark(book, chapter, verse.getVerseNumber());
			bookmarkVerseStart = bm.getVerseStart();
			bookmarkVerseEnd = bm.getVerseEnd();
			txtVerse.setTextSize(currentFontSize);
			StringBuffer sb = new StringBuffer();
			for (int j=bookmarkVerseStart; j<=bookmarkVerseEnd; j++) {
				sb.append(Util.parseVerse(verseListWholeChapter.get(verseIndex + (j-bookmarkVerseStart)))).append(" ");
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
		            	String[] arrBookChapter = Constants.arrVerseCount[verse.getChapterIndex()].split(";");
		            	int book = Integer.parseInt(arrBookChapter[0]);
				  		int chapter = Integer.parseInt(arrBookChapter[1]);
		                databaseHelper.removeBookmark(book, chapter, verseNumber);
		                displayBible(currentBibleFilename, false);
		            }
	
		        })
		        .setNegativeButton(R.string.no, null)
		        .show();
			return true;
		case Menu.FIRST+2 : //add Bookmark
			loadVerseListWholeChapter(verse.getChapterIndex());
			bookmarkVerseStart = verse.getVerseNumber();
			bookmarkVerseEnd = verse.getVerseNumber();
			txtVerse.setTextSize(currentFontSize);
			txtVerse.setText(Util.parseVerse(verse.getVerse()));
			refreshBookNameOnBookmarkDialog();
			bookmarkDialog.show();			
			return true;
		case Menu.FIRST+3 : //copy bookmarked verse to clipboard
			loadVerseListWholeChapter(verse.getChapterIndex());
			copyOrShare = 'c';
			arrBookChapter = Constants.arrVerseCount[verse.getChapterIndex()].split(";");
	  		book = Integer.parseInt(arrBookChapter[0]);
	  		chapter = Integer.parseInt(arrBookChapter[1]);
			bm = databaseHelper.getBookmark(book, chapter, verse.getVerseNumber());
			bookmarkVerseStart = bm.getVerseStart();
			bookmarkVerseEnd = bm.getVerseEnd();
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);
			txtVerse.setTextSize(currentFontSize);
			sb = new StringBuffer();
			for (int j=bookmarkVerseStart; j<=bookmarkVerseEnd; j++) {
				sb.append(Util.parseVerse(verseListWholeChapter.get(verseIndex + (j-bookmarkVerseStart)))).append(" ");
			}
			txtVerse.setText(sb.substring(0, sb.length()-1));
			refreshBookNameOnCopyToClipboardDialog();
			copyToClipboardDialog.setTitle("Copy to clipboard");
			copyToClipboardDialog.show();			
			return true;
		case Menu.FIRST+4 : //copy unbookmarked verse to clipboard
			loadVerseListWholeChapter(verse.getChapterIndex());
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
			loadVerseListWholeChapter(verse.getChapterIndex());
			copyOrShare = 's';
			arrBookChapter = Constants.arrVerseCount[verse.getChapterIndex()].split(";");
	  		book = Integer.parseInt(arrBookChapter[0]);
	  		chapter = Integer.parseInt(arrBookChapter[1]);
			bm = databaseHelper.getBookmark(book, chapter, verse.getVerseNumber());
			bookmarkVerseStart = bm.getVerseStart();
			bookmarkVerseEnd = bm.getVerseEnd();
			txtVerse = (TextView) copyToClipboardView.findViewById(R.id.txtVerse);
			txtVerse.setTextSize(currentFontSize);
			sb = new StringBuffer();
			for (int j=bookmarkVerseStart; j<=bookmarkVerseEnd; j++) {
				sb.append(Util.parseVerse(verseListWholeChapter.get(verseIndex + (j-bookmarkVerseStart)))).append(" ");
			}
			txtVerse.setText(sb.substring(0, sb.length()-1));
			refreshBookNameOnCopyToClipboardDialog();
			copyToClipboardDialog.setTitle("Share verse");
			copyToClipboardDialog.show();
			return true;
		case Menu.FIRST+6 : //share unbookmarked
			loadVerseListWholeChapter(verse.getChapterIndex());
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
	
	private void loadVerseListWholeChapter(int chapterIndex) {
		verseListWholeChapter.clear();
		File sdcard = Environment.getExternalStorageDirectory();
		File file = new File(sdcard, Constants.BIBLE_FOLDER + "/" + currentBibleFilename);
		String indexFileName = file.getAbsolutePath().replaceAll(".ont", ".idx");
		File fIndex = new File(indexFileName);
		String[] arrBookChapter = Constants.arrVerseCount[chapterIndex].split(";");
		int verseCount = Integer.parseInt(arrBookChapter[2]);
		BufferedReader br = null;
		try {
			DataInputStream is = new DataInputStream(new FileInputStream(fIndex));
			is.skip(chapterIndex*4);			
			int startOffset = is.readInt();
			is.close();
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
			br.skip(startOffset);
			String line = "";
			for (int i = 1; i <= verseCount; i++) {
				line = br.readLine();
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				
				line = line.replaceAll("<CL>", "\n");
				int posCM = line.indexOf("<CM>");
				if (posCM > -1) {
					if (!line.endsWith("<CM>")) {
						String afterCM = line.substring(posCM + "<CM>".length()).trim();
						if (afterCM.startsWith("<") && afterCM.endsWith(">")) {
							line = line.substring(0, posCM) + afterCM;
						}
					} else {
						line = line.substring(0, line.length()-"<CM>".length());
					}
				}
				
				line = line.replaceAll("<CM>", "\n\n");
				line = line.replaceAll("\n\n \n\n", "\n\n");
				line = line.replaceAll("\n\n\n\n", "\n\n");
				
				verseListWholeChapter.add(line);
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
	
	private void refreshBookNameOnBookmarkDialog() {
		String[] arrBookChapter = Constants.arrVerseCount[chapterIndex].split(";");
  		int book = Integer.parseInt(arrBookChapter[0]);
  		int chapter = Integer.parseInt(arrBookChapter[1]);
  		bookmarkVerseMax = Integer.parseInt(arrBookChapter[2]);
  		StringBuffer sbBook = new StringBuffer();
		
//		if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//			sbBook.append(Constants.arrBookNameIndo[book - 1]).append(" ").append(chapter);
//		} else {
//			sbBook.append(Constants.arrBookName[book - 1]).append(" ").append(chapter);
//		}
  		sbBook.append(Constants.arrDocBookName[book - 1]).append(" ").append(chapter);
		
		sbBook.append(":").append(bookmarkVerseStart);
		if (bookmarkVerseStart != bookmarkVerseEnd) {
			sbBook.append("-").append(bookmarkVerseEnd);
		}
		
		TextView txtBook = (TextView) bookmarkView.findViewById(R.id.txtBook);
		txtBook.setText(sbBook.toString());
	}
	
	private void refreshBookNameOnCopyToClipboardDialog() {
		String[] arrBookChapter = Constants.arrVerseCount[chapterIndex].split(";");
  		int book = Integer.parseInt(arrBookChapter[0]);
  		int chapter = Integer.parseInt(arrBookChapter[1]);
  		bookmarkVerseMax = Integer.parseInt(arrBookChapter[2]);
  		StringBuffer sbBook = new StringBuffer();
		
//		if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//			sbBook.append(Constants.arrBookNameIndo[book - 1]).append(" ").append(chapter);
//		} else {
//			sbBook.append(Constants.arrBookName[book - 1]).append(" ").append(chapter);
//		}
		sbBook.append(Constants.arrDocBookName[book - 1]).append(" ").append(chapter);
		
		sbBook.append(":").append(bookmarkVerseStart);
		if (bookmarkVerseStart != bookmarkVerseEnd) {
			sbBook.append("-").append(bookmarkVerseEnd);
		}
		
		TextView txtBook = (TextView) copyToClipboardView.findViewById(R.id.txtBook);
		txtBook.setText(sbBook.toString());
	}
}
