package com.konibee.bible;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Documents extends Activity implements OnClickListener, OnItemClickListener {
	private static final String TAG = "Documents";
	
	private String currentFile;
	private int currentPosition;
	private String currentDocument;
	private int currentFontSize;
	private boolean isFullScreen;
	private String currentBibleFilename;
	private String currentBookLanguage;
	
	private Map<String, String> docMap;
	private ArrayAdapter<String> documentsAdapter;
	private ListView viewDocuments;
	private AlertDialog dialogDocuments;
	
	private ProgressDialog pd;
	
	private List<String> titleList = new ArrayList<String>();
	private List<Integer> lineCountList = new ArrayList<Integer>();
	private List<String> header1List = new ArrayList<String>();
	private List<String> header1ChildList = new ArrayList<String>();
	private List<String> header2List = new ArrayList<String>();
	private List<String> header2ChildList = new ArrayList<String>();
	
	private boolean gotoDownload = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.documents);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		readPreference();
		LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
		if (isFullScreen) {
			bottomBar.setVisibility(View.GONE);
		} else {
			bottomBar.setVisibility(View.VISIBLE);
		}
		
		TextView txtContent = (TextView) findViewById(R.id.txtContent);
		txtContent.setTextSize(currentFontSize);
		
		Button btnNext = (Button) findViewById(R.id.btnNext);
		btnNext.setOnClickListener(this);
		Button btnPrev = (Button) findViewById(R.id.btnPrev);
		btnPrev.setOnClickListener(this);
		
		if ("".equals(currentFile)) {
			btnPrev.setVisibility(View.INVISIBLE);
			btnNext.setVisibility(View.INVISIBLE);
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		File documentFolder = new File(sdcard.getPath() + Constants.DOCUMENT_FOLDER);
		if (!documentFolder.isDirectory()) {
			boolean success = documentFolder.mkdirs();
			Log.d(TAG, "Creating document directory success: " + success);
		}
		
		TextView txtDocumentName = (TextView) findViewById(R.id.txtDocumentName);
		txtDocumentName.setOnClickListener(this);
		TextView txtCurrent = (TextView) findViewById(R.id.txtCurrent);
		txtCurrent.setOnClickListener(this);

		View btnFullscreen = findViewById(R.id.btnFullscreen);
		btnFullscreen.setOnClickListener(this);
		View btnZoomIn = findViewById(R.id.btnZoomIn);
		btnZoomIn.setOnClickListener(this);
		View btnZoomOut = findViewById(R.id.btnZoomOut);
		btnZoomOut.setOnClickListener(this);
		
		//delete invalid last position on preference
		File[] arrTocFile = documentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.getName().endsWith(".toc")) return true;
				return false;
			}
		});
		List<String> listFilename = new ArrayList<String>();
		for (File tocFile : arrTocFile) {
			listFilename.add(tocFile.getName());
		}
		List<String> listOnPrefs = new ArrayList<String>();
		SharedPreferences posPreference = getSharedPreferences(Constants.POSITION_NAME, MODE_PRIVATE);
		Set<String> setKeys = posPreference.getAll().keySet();
		//copy to another list of string
		for (String key : setKeys) {
			listOnPrefs.add(key);
		}
		for (String key : listOnPrefs) {
			if (!listFilename.contains(key)) {
				posPreference.edit().remove(key);
			}
		}
		//end of delete invalid last position on preference
		
		if (!"".equals(currentFile)) {
			pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
			new LoadingTask(this).execute(currentFile);
		}
	}

	private void readPreference() {		
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentFile = preference.getString(Constants.DOCUMENT_FILENAME, "");		
		currentPosition = 0;
		currentFontSize = preference.getInt(Constants.FONT_SIZE, 18);
		if (!"".equals(currentFile)) {
			SharedPreferences posPreference = getSharedPreferences(Constants.POSITION_NAME, MODE_PRIVATE);
			currentPosition = posPreference.getInt(currentFile, 0);
		}
		isFullScreen = preference.getBoolean(Constants.FULL_SCREEN, false);
		currentBibleFilename = preference.getString(Constants.POSITION_BIBLE_NAME, "");
		currentBookLanguage = preference.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
		
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnNext) {
			if (titleList.size() == 0) return;
			currentPosition++;
			if (currentPosition == titleList.size()) {
				currentPosition = 0;
			}
			displayDocument();
		} else if (v.getId() == R.id.btnPrev) {
			if (titleList.size() == 0) return;
			currentPosition--;
			if (currentPosition == -1) {
				currentPosition = titleList.size()-1;
			}
			displayDocument();
		} else if (v.getId() == R.id.btnFullscreen ) {
			LinearLayout bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
			if (bottomBar.getVisibility() == View.GONE) {
				bottomBar.setVisibility(View.VISIBLE);
				isFullScreen = false;
			} else {
				bottomBar.setVisibility(View.GONE);
				isFullScreen = true;
			}
		} else if (v.getId() == R.id.btnZoomIn) {
			currentFontSize+=1;
			updateFontSize();
		} else if (v.getId() == R.id.btnZoomOut) {
			currentFontSize-=1;
			updateFontSize();
		} else if (v.getId() == R.id.txtDocumentName) {
			File sdcard = Environment.getExternalStorageDirectory();
			File documentFolder = new File(sdcard.getPath() + Constants.DOCUMENT_FOLDER);
			File[] arrTocFile = documentFolder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if (pathname.getName().endsWith(".toc")) return true;
					return false;
				}
			});
			if (arrTocFile.length == 0) {
				Toast.makeText(this, R.string.noDocumentFile, Toast.LENGTH_LONG).show();
				return;
			}
			
			docMap = new TreeMap<String, String>();
			for (File tocFile : arrTocFile) {
				String conFileName = tocFile.getAbsolutePath().replaceAll(".toc", ".con");
				File conFile = new File(conFileName);
				if (conFile.isFile()) {
					try {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF-8"), 512);
						String[] arrTitle= br.readLine().split(";;");
						String docTitle = arrTitle[0];
						br.close();
						docMap.put(tocFile.getName(), docTitle);
					} catch (Exception e) {
						Log.e(TAG, "Error reading title on " + tocFile.getName(), e);
					}
				}
			}
			if (docMap.size() == 0) {
				Toast.makeText(this, R.string.noDocumentFile, Toast.LENGTH_LONG).show();
				return;
			}
			
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setTitle(R.string.selectDocument);
			documentsAdapter = new ArrayAdapter<String>(this, R.layout.listitemmedium, new ArrayList<String>(docMap.values()));
			viewDocuments = new ListView(this);
			viewDocuments.setAdapter(documentsAdapter);
			viewDocuments.setOnItemClickListener(this);
			ad.setView(viewDocuments);				
			dialogDocuments = ad.create();
			dialogDocuments.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			dialogDocuments.show();
		} else if (v.getId() == R.id.txtCurrent) { //this is real spaghetti code
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ListView viewChoose = new ListView(this);
			ad.setView(viewChoose);		
			final AlertDialog dialogChoose = ad.create();
			if (header1List.size() == 0) {
				viewChoose.setAdapter(new ArrayAdapter<String>(this, R.layout.listitemmedium, titleList));
				viewChoose.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						dialogChoose.dismiss();
						currentPosition = position;
						displayDocument();
					}
				});
				dialogChoose.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
				dialogChoose.show();
			} else {
				viewChoose.setAdapter(new ArrayAdapter<String>(this, R.layout.listitemmedium, header1List));
				viewChoose.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						dialogChoose.dismiss();
						String childStr = header1ChildList.get(position);
						String[] arrChild = childStr.split(";;");
						String childType = arrChild[0];
						int numberOfChild = Integer.valueOf(arrChild[1]);
						final int childStart = Integer.valueOf(arrChild[2]);						
						if ("H2".equals(childType)) {
							if (numberOfChild > 1) {
								List<String> childList = new ArrayList<String>();
								for (int i = 0; i < numberOfChild; i++) {
									childList.add(header2List.get(childStart + i));
								}
								AlertDialog.Builder ad = new AlertDialog.Builder(Documents.this);
								ListView viewChoose = new ListView(Documents.this);
								ad.setView(viewChoose);
								final AlertDialog dialogChoose = ad.create();
								viewChoose.setAdapter(new ArrayAdapter<String>(Documents.this, R.layout.listitemmedium, childList));
								viewChoose.setOnItemClickListener(new OnItemClickListener() {
									@Override
									public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
										dialogChoose.dismiss();
										String childStr = header2ChildList.get(childStart + position);
										String[] arrChild = childStr.split(";;");
										String childType = arrChild[0];
										int numberOfChild = Integer.valueOf(arrChild[1]);
										final int childStart = Integer.valueOf(arrChild[2]);
										if ("C".equals(childType)) {
											if (numberOfChild > 1) {
												List<String> childList = new ArrayList<String>();
												for (int i = 0; i < numberOfChild; i++) {
													childList.add(titleList.get(childStart + i));
												}
												AlertDialog.Builder ad = new AlertDialog.Builder(Documents.this);
												ListView viewChoose = new ListView(Documents.this);
												ad.setView(viewChoose);
												final AlertDialog dialogChoose = ad.create();
												viewChoose.setAdapter(new ArrayAdapter<String>(Documents.this, R.layout.listitemmedium, childList));
												viewChoose.setOnItemClickListener(new OnItemClickListener() {
													@Override
													public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
														dialogChoose.dismiss();
														currentPosition = childStart + position;
														displayDocument();
													}
												});
												dialogChoose.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
												dialogChoose.show();
											} else {
												currentPosition = childStart;
												displayDocument();
											}
										}
									}
								});
								dialogChoose.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
								dialogChoose.show();
							}
						} else if ("C".equals(childType)) {
							if (numberOfChild > 1) {
								List<String> childList = new ArrayList<String>();
								for (int i = 0; i < numberOfChild; i++) {
									childList.add(titleList.get(childStart + i));
								}
								AlertDialog.Builder ad = new AlertDialog.Builder(Documents.this);
								ListView viewChoose = new ListView(Documents.this);
								ad.setView(viewChoose);
								final AlertDialog dialogChoose = ad.create();
								viewChoose.setAdapter(new ArrayAdapter<String>(Documents.this, R.layout.listitemmedium, childList));
								viewChoose.setOnItemClickListener(new OnItemClickListener() {
									@Override
									public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
										dialogChoose.dismiss();
										currentPosition = childStart + position;
										displayDocument();
									}
								});
								dialogChoose.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
								dialogChoose.show();
							} else {
								currentPosition = childStart;
								displayDocument();
							}
						}
					}
				});
				dialogChoose.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
				dialogChoose.show();
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == viewDocuments) {
			dialogDocuments.dismiss();
			List<String> listKey = new ArrayList<String>(docMap.keySet());
			String filename = listKey.get(position);
			if (filename.equals(currentFile)) return;
			
			Editor editor = getSharedPreferences(Constants.POSITION_NAME, MODE_PRIVATE).edit();
			editor.putInt(currentFile, currentPosition);
			editor.commit();
			pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
	        new LoadingTask(this).execute(filename);
		}
	}
	
	private class LoadingTask extends AsyncTask<Object, Void, Object> {
		private Context context;
		public LoadingTask(Context context) {
			this.context = context;
		}
		
		@Override
		protected Object doInBackground(Object... arg) {
			String tocFilename = (String) arg[0];
			File sdcard = Environment.getExternalStorageDirectory();
			File tocFile = new File(sdcard, Constants.DOCUMENT_FOLDER + "/" + tocFilename);
			if (!tocFile.isFile()) return "NOT SUCCESS";
			String conFileName = tocFile.getAbsolutePath().replaceAll(".toc", ".con");
			File conFile = new File(conFileName);
			String idxFileName = tocFile.getAbsolutePath().replaceAll(".toc", ".idx");
			File idxFile = new File(idxFileName);
			
			try {
			
				boolean reindex = false;
				if (!idxFile.isFile()) {
					reindex = true;
				} else {
					DataInputStream is = new DataInputStream(new FileInputStream(idxFile));
					int sizeToc = is.readInt();
					int sizeContent = is.readInt();
					is.close();
					if (sizeToc != tocFile.length() || sizeContent != conFile.length()) {
						reindex = true;
					}
				}
				
				titleList.clear();
				lineCountList.clear();
				header1List.clear();
				header1ChildList.clear();
				header2List.clear();
				header2ChildList.clear();
				if (reindex) {
					DataOutputStream out = new DataOutputStream(new FileOutputStream(idxFile));
					out.writeInt((int) tocFile.length());
					out.writeInt((int) conFile.length());
					
					char[] bufChar = new char[8192];
					InputStreamReader in = new InputStreamReader(new FileInputStream(conFile), "UTF-8");
					int numChar = in.read(bufChar);
					int startOffset = numChar;
					int offset = 0;	
					byte[] bomUtf8 = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
					if (Character.toString(bufChar[0]).equals(new String(bomUtf8, "UTF-8"))) {
						offset = 1;
					}
					
					int lineNumber = 1;
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF-8"), 8192);
					String[] arrTitle = br.readLine().split(";;");
					currentDocument = arrTitle[0];
					if (arrTitle.length >= 3) {
						String lang = arrTitle[2];
						if (lang.equals(currentBookLanguage)) {
							Constants.arrDocBookName = Constants.arrActiveBookName; 
							Constants.arrDocBookAbbr = Constants.arrActiveBookAbbr;
						} else {
							File bookNameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
							File bookNameFile = new File(bookNameFolder, lang.toLowerCase() + ".bkn");
							if (bookNameFile.isFile()) {
								try {
									BufferedReader brBookName = new BufferedReader(new InputStreamReader(
										new FileInputStream(bookNameFile), "UTF-8"), 8192);
									String line = null;
									int i = 0;
									while (((line = brBookName.readLine()) != null) && (i < 66)) {
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
										Constants.arrDocBookName[i] = bookName; 
										Constants.arrDocBookAbbr[i] = abbr;
										i++;
									}
								} catch (Exception e) {
									Log.d(TAG, "Error read bookname file", e);
								}
							}
						}
					}
					
					String line = null; 
					int charIndex = offset;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("H1")) {
							String[] arrLine = line.split(";;");
							header1List.add(arrLine[1]);
							StringBuffer sb = new StringBuffer();
							sb.append(arrLine[2]).append(";;").append(arrLine[3]).append(";;");
							if ("H2".equals(arrLine[2])) {
								sb.append(header2List.size());
							}
							if ("C".equals(arrLine[2])) {
								sb.append(titleList.size());
							}
							header1ChildList.add(sb.toString());
						}
						if (line.startsWith("H2")) {
							String[] arrLine = line.split(";;");
							header2List.add(arrLine[1]);
							StringBuffer sb = new StringBuffer();
							sb.append(arrLine[2]).append(";;").append(arrLine[3]).append(";;");
							if ("C".equals(arrLine[2])) {
								sb.append(titleList.size());
							}
							header2ChildList.add(sb.toString());
						}
						
						if (line.startsWith("C;;")) {
							String[] arrLine = line.split(";;");
							titleList.add(arrLine[1]);
							lineCountList.add(Integer.valueOf(arrLine[3]));
							int startLine = Integer.valueOf(arrLine[2]);
							
							while (lineNumber < startLine) {
								while (charIndex < numChar && lineNumber < startLine && numChar > -1) {
									if (bufChar[charIndex] == '\n') {
										lineNumber++;
										offset = startOffset - numChar + charIndex; 
									}
									charIndex++;
									if (charIndex == numChar) {
										numChar = in.read(bufChar);
										startOffset = startOffset + numChar;
										charIndex = 0;
									}
								}
								if (numChar == -1) {
									break;
								}
							}
							if (lineNumber == startLine) {
								if (lineNumber > 1) {
									out.writeInt(offset + 1);
								} else {
									out.writeInt(offset);
								}
							}
						}
					}
					br.close();
					out.close();
					currentFile = tocFilename;

					SharedPreferences posPreference = getSharedPreferences(Constants.POSITION_NAME, MODE_PRIVATE);
					currentPosition = posPreference.getInt(currentFile, 0);
				} else { //not reindex
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF-8"), 8192);
					String line = null; 
					String[] arrTitle= br.readLine().split(";;");
					String docTitle = arrTitle[0];
					currentDocument = docTitle;
					currentFile = tocFilename;
					SharedPreferences posPreference = getSharedPreferences(Constants.POSITION_NAME, MODE_PRIVATE);
					currentPosition = posPreference.getInt(currentFile, 0);
					while ((line = br.readLine()) != null) {
						if (line.startsWith("H1")) {
							String[] arrLine = line.split(";;");
							header1List.add(arrLine[1]);
							StringBuffer sb = new StringBuffer();
							sb.append(arrLine[2]).append(";;").append(arrLine[3]).append(";;");
							if ("H2".equals(arrLine[2])) {
								sb.append(header2List.size());
							}
							if ("C".equals(arrLine[2])) {
								sb.append(titleList.size());
							}
							header1ChildList.add(sb.toString());
						}
						if (line.startsWith("H2")) {
							String[] arrLine = line.split(";;");
							header2List.add(arrLine[1]);
							StringBuffer sb = new StringBuffer();
							sb.append(arrLine[2]).append(";;").append(arrLine[3]).append(";;");
							if ("C".equals(arrLine[2])) {
								sb.append(titleList.size());
							}
							header2ChildList.add(sb.toString());
						}
						if (line.startsWith("C;;")) {
							String[] arrLine = line.split(";;");
							titleList.add(arrLine[1]);
							lineCountList.add(Integer.valueOf(arrLine[3]));
						}
					}
				}
				return "SUCCESS";
			} catch (Exception e) {
				Log.e(TAG, "Error loading document", e);
				return "NOT SUCCESS";
			}
		}
		

		@Override
		protected void onPostExecute(Object result) {
			if (pd != null) {
				pd.dismiss();
			}
			context = null;
			if ("SUCCESS".equals(result)) {
				displayDocument();
			} else {
				Toast.makeText(context, R.string.errorLoadingDocument, Toast.LENGTH_LONG).show();
			}
			
		}
	}

	public void displayDocument() {
		if (titleList.size() > 0) {
			Button btnNext = (Button) findViewById(R.id.btnNext);
			btnNext.setVisibility(View.VISIBLE);
			Button btnPrev = (Button) findViewById(R.id.btnPrev);
			btnPrev.setVisibility(View.VISIBLE);
		}
		TextView txtDocumentName = (TextView) findViewById(R.id.txtDocumentName);
		txtDocumentName.setText(currentDocument);
		TextView txtCurrent = (TextView) findViewById(R.id.txtCurrent);
		if (titleList.size() <= currentPosition) {
			currentPosition = 0;
		}
		txtCurrent.setText(titleList.get(currentPosition));
		TextView txtContent = (TextView) findViewById(R.id.txtContent);
		
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			txtContent.setText(getResources().getString(R.string.sdcard_error));
			Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG).show();
			return;
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		File fileToc = new File(sdcard, Constants.DOCUMENT_FOLDER + "/" + currentFile);
		String contentFileName = fileToc.getAbsolutePath().replaceAll(".toc", ".con");
		String indexFileName = fileToc.getAbsolutePath().replaceAll(".toc", ".idx");
		File fIndex = new File(indexFileName);
		File fContent = new File(contentFileName);
		BufferedReader br = null;		
		try {
			DataInputStream is = new DataInputStream(new FileInputStream(fIndex));
			is.skip(currentPosition*4 + 2*4);			
			int startOffset = is.readInt();
			is.close();
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fContent), "UTF-8"), 8192);
			br.skip(startOffset);
			StringBuffer sb = new StringBuffer();
			for (int i=0; i < lineCountList.get(currentPosition); i++) {
				sb.append(br.readLine()).append("<br/>");
			}
			if (sb.length() > 0) {
				sb.delete(sb.length()-"<br/>".length(), sb.length());
			}
			
			Spannable text = new SpannableString(Html.fromHtml(sb.toString()));

			int posStartVerse = text.toString().indexOf("[[");
			while (posStartVerse > -1) {
				int posEndVerse = text.toString().indexOf("]]", posStartVerse);
				final String verse = text.toString().substring(posStartVerse + 2, posEndVerse);
				if (!"".equals(currentBibleFilename)) {
					text.setSpan(new ClickableSpan() {
						@Override
						public void onClick(View widget) {
							String openVerse = verse.replaceAll(String.valueOf((char)160), " ");
							Intent readBible = new Intent(Documents.this, ReadBible.class);
							readBible.putExtra(Constants.READ_BIBLE, openVerse);
							startActivity(readBible);
						}
					}, posStartVerse+2, posEndVerse, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					text.setSpan(new ForegroundColorSpan(Color.CYAN), posStartVerse+2, posEndVerse, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				
				text.setSpan(new ForegroundColorSpan(Color.BLACK), posStartVerse, posStartVerse+2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				text.setSpan(new ForegroundColorSpan(Color.BLACK), posEndVerse, posEndVerse+2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				posStartVerse = text.toString().indexOf("[[", posStartVerse + 1);
			}
			
			txtContent.setText(text);
			txtContent.setMovementMethod(LinkMovementMethod.getInstance());
			
			final ScrollView sv = (ScrollView) findViewById(R.id.svDocument);
			sv.post(new Runnable() {
				@Override
				public void run() {
					sv.scrollTo(0, 0);
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "Error reading document content", e);
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
	
	@Override
	protected void onPause() {
		super.onPause();
		// Save the current position
		Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		editor.putString(Constants.DOCUMENT_FILENAME, currentFile);
		editor.putInt(Constants.DOCUMENT_POSITION, currentPosition);
		editor.putInt(Constants.FONT_SIZE, currentFontSize);
		editor.putBoolean(Constants.FULL_SCREEN, isFullScreen);
		editor.commit();
		
		if (currentFile != null && !"".equals(currentFile)) {
			Editor editorPosition = getSharedPreferences(Constants.POSITION_NAME, MODE_PRIVATE).edit();
			editorPosition.putInt(currentFile, currentPosition);
			editorPosition.commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.documents_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.downloadDocument:
				gotoDownload = true;
				startActivity(new Intent(this, DownloadDocument.class));
				return true;
			case R.id.aboutDocument:
				Intent i = new Intent(this, AboutDocument.class);
				i.putExtra(Constants.CURRENT_FILE, currentFile);
				startActivity(i);
				return true;
		}
		return false;
	}
	
	private void updateFontSize() {
		TextView txtContent = (TextView) findViewById(R.id.txtContent);
		txtContent.setTextSize(currentFontSize);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (gotoDownload) {
			gotoDownload = false;
			readPreference();
			pd = ProgressDialog.show(this, getResources().getString(R.string.pleaseWait), getResources().getString(R.string.loading), true, false);
			new LoadingTask(this).execute(currentFile);
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
}

