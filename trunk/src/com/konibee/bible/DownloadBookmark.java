package com.konibee.bible;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class DownloadBookmark extends ListActivity implements OnItemClickListener, android.content.DialogInterface.OnClickListener, OnCancelListener {
	private static final String TAG="DownloadBookmark";
	private static final int TIMEOUT = 15;  
	
	private ProgressDialog pdRepository;
	private ProgressDialog pdDownload;

	private List<DisplayDownload> allBookmarkList = Collections.synchronizedList(new ArrayList<DisplayDownload>());
	private List<DisplayDownload> bookmarkList = Collections.synchronizedList(new ArrayList<DisplayDownload>());
	private List<String> languageSet = new ArrayList<String>();
	private List<Map<String, String>> languageList = Collections.synchronizedList(new ArrayList<Map<String, String>>());
	private DisplayDownloadAdapter adapter;
	private SimpleAdapter languageAdapter;
	private int currentFontSize;
	
	private AlertDialog confirmDownload;
	private String homeUrl = "http://open-bibles.appspot.com/bibles/";
	private String downloadFileName;
	private String destinationFileName;
	
	private DatabaseHelper databaseHelper;
	
	private Handler handler = new Handler();
	
	private final String ERROR = "INVALID_URL";
	private final String MISSING_BOOKMARK_FILE = "MISSING_BOOKMARK_FILE";
	private final String SUCCESS = "SUCCESS";
	private final String CANCEL = "CANCEL";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloadbible);
		
		Spinner spinner = (Spinner) findViewById(R.id.spnLanguage);
		languageList.clear();
		Map<String, String> map = new HashMap<String, String>();
		map.put("languageName", "");
		languageList.add(map);
		
	    languageAdapter = new SimpleAdapter(this, languageList, android.R.layout.simple_spinner_item, 
    		new String[] {"languageName"}, new int[] {android.R.id.text1});
	    languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data,
                    String textRepresentation) {
                TextView textView = (TextView) view;
                textView.setText(textRepresentation);
                return true;
            }
        };
        languageAdapter.setViewBinder(viewBinder);
	    spinner.setAdapter(languageAdapter);
	    
	    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
            	refreshBookmarkList(parent, position);
            }
 
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }
        });
		
		databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();
		
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {		
			File sdcard = Environment.getExternalStorageDirectory();
			File bookmarkFolder = new File(sdcard.getPath() + Constants.BOOKMARK_FOLDER);
			if (!bookmarkFolder.isDirectory()) {
				boolean success = bookmarkFolder.mkdirs();			
				Log.d(TAG, "Creating bookmark directory success: " + success);
			}
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirmDownload);
		builder.setPositiveButton(getResources().getString(R.string.yes), this);
		builder.setNegativeButton(getResources().getString(R.string.no), this);
		confirmDownload = builder.create();
		
		readPreference();
		
		adapter = new DisplayDownloadAdapter(this, R.layout.rowdownload, bookmarkList, currentFontSize);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		
		try {
			URL url = new URL(homeUrl + "bookmark_index_all.txt");
			Log.d(TAG, "Accessing Bookmark Repository All");
			pdRepository = ProgressDialog.show(this, 
					getResources().getString(R.string.pleaseWait), 
					getResources().getString(R.string.loading), true, false);
			new LoadingRepositoryTask().execute(url);
		} catch (MalformedURLException e) {
			Toast.makeText(this, R.string.repositoryUrlNotValid, Toast.LENGTH_LONG).show();
		}
	}
	
	private void refreshBookmarkList(AdapterView<?> parent, int position) {
		final Map<String, String> data = (Map<String, String>) parent.getItemAtPosition(position);
        bookmarkList.clear();
        for (DisplayDownload bible : allBookmarkList) {
        	if (bible.getLanguage() != null && bible.getLanguage().equals(data.get("languageName"))) {
        		bookmarkList.add(bible);
        	}
        }
        adapter.notifyDataSetChanged();
	}

	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentFontSize = preference.getInt(Constants.FONT_SIZE, 14);
	}

	private class LoadingRepositoryTask extends AsyncTask<Object, Void, Object> {
		@Override
		protected Object doInBackground(Object... arg) {
			bookmarkList.clear();
			URL url = (URL) arg[0];
			HttpURLConnection conn = null;
			BufferedReader reader = null;
			try {
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(TIMEOUT * 1000);
				conn.setReadTimeout(TIMEOUT * 1000);
				conn.setDoInput(true);
				conn.setDoOutput(true);
			
				InputStream is= conn.getInputStream();
				if (is == null) {
					return -1;
				}
				int i = 0;
				if (is != null) {
					reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
					String line = null;
					
					byte[] bomUtf8 = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
					while ((line = reader.readLine()) != null) {
						String[] arrLine = line.split(";;");
						if (arrLine.length != 4) continue;
						if (i == 0) { //check for UTF-8 bom
							if (Character.toString(arrLine[0].charAt(0)).equals(new String(bomUtf8, "UTF-8"))) {
								arrLine[0] = arrLine[0].substring(1);
							}							
						}
						i++;						
						allBookmarkList.add(new DisplayDownload(arrLine[0], arrLine[1], arrLine[2], arrLine[3]));
						if (!languageSet.contains(arrLine[0])) {
							languageSet.add(arrLine[0]);
						}
					}
				}
				return String.valueOf(i);
			} catch (Exception e) {
				Log.d(TAG, "Error reading bookmark_index", e);
				return ERROR + Util.getRootCause(e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.d(TAG, "error in closing file", e);
					}
				}
				if (conn != null) {
					try {
						if (conn.getInputStream() != null)
							conn.getInputStream().close();
						if(conn.getErrorStream() != null)
							conn.getErrorStream().close();
					} catch (Exception e) {
						Log.d(TAG, "error in finally code", e);
					}
				}
				
				
			}
		}
		
		@Override
		protected void onPostExecute(Object result) {
			if (pdRepository != null) {
				String strResult = (String) result;
				if (strResult.startsWith(ERROR)) {
					String strMessage = "Error: " + strResult.substring(ERROR.length()) + ". Try again by pressing menu button and choose Refresh";
					Toast.makeText(DownloadBookmark.this, strMessage, Toast.LENGTH_LONG).show();
				} else {
					languageList.clear();
					for (String lang : languageSet) {
						Map<String, String> map = new HashMap<String, String>();
						map.put("languageName", lang);
						languageList.add(map);
					}
					languageAdapter.notifyDataSetChanged();
					
					Spinner spinner = (Spinner) findViewById(R.id.spnLanguage);
				    spinner.setSelection(0);
				    refreshBookmarkList(spinner, 0);
				}
				pdRepository.dismiss();
				pdRepository = null;
			}
		}
	}
	
	


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		DisplayDownload download = bookmarkList.get(position);
		downloadFileName = download.getFileName();
		destinationFileName = download.getName();
		
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG);
			return;
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		StringBuffer destFileName = new StringBuffer(sdcard.getPath());
		destFileName.append(Constants.BOOKMARK_FOLDER).append("/").append(destinationFileName).append(".bmk");
		File f = new File(destFileName.toString());
		String str;
		if (f.exists()) {
			str = getResources().getString(R.string.confirmImportOverwrite);
		} else {
			str = getResources().getString(R.string.confirmImportQuestion);
		}
		String confirmQuestion = String.format(str, download.getName());

		confirmDownload.setMessage(confirmQuestion);
		confirmDownload.show();
	}
	
	private DownloadTask downloadTask;

	@Override
	public void onClick(DialogInterface dlg, int buttonId) {
		if (dlg.equals(confirmDownload)) {
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				try {
					String state = Environment.getExternalStorageState();
					if (!Environment.MEDIA_MOUNTED.equals(state)) {
						Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG);
						return;
					}
					URL url = new URL(homeUrl + downloadFileName + ".bmk");
					Log.d(TAG, "Downloading file: " + url.getPath());
					pdDownload = ProgressDialog.show(this, 
							getResources().getString(R.string.pleaseWait), 
							getResources().getString(R.string.downloading), true, true, this);
					downloadTask = new DownloadTask();
					downloadTask.execute(url);
				} catch (MalformedURLException e) {
					Toast.makeText(this, R.string.downloadUrlNotValid, Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	public void onCancel(DialogInterface dlg) {
		if (dlg == pdDownload) {
			downloadTask.cancel(true);
		}
	}
	
	private class DownloadTask extends AsyncTask<Object, Void, Object> {		
		@Override
		protected Object doInBackground(Object... arg) {
			File sdcard = Environment.getExternalStorageDirectory();
			StringBuffer destFileName = new StringBuffer(sdcard.getPath());
			destFileName.append(Constants.BOOKMARK_FOLDER).append("/").append(destinationFileName).append(".bmk");
			
			URL url = (URL) arg[0];
			HttpURLConnection conn = null;
			InputStream stream = null; //to read
		    FileOutputStream out = null; //to write
		    double fileSize = 0;
		    double downloaded = 0; //
			try {
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(TIMEOUT * 1000);
				conn.setReadTimeout(TIMEOUT * 1000);
				conn.connect();					
				if (conn.getResponseCode() == 404) {
					conn.disconnect();
					return MISSING_BOOKMARK_FILE;
				}
				fileSize = conn.getContentLength();
				out = new FileOutputStream(destFileName.toString());
				final int fileSizeKb = (int) (fileSize / 1024) + 1;
				stream = conn.getInputStream();

				byte buffer[] = new byte[1024];
			    int read = -1;
			    int i = 0;
			    while ((read = stream.read(buffer))> -1) {
			    	i++;			    	
			        out.write(buffer, 0, read);			        
			        if (isCancelled()) {
			        	out.flush();
			        	handler.post(new Runnable() {
			        		public void run() {
			        			Toast.makeText(DownloadBookmark.this, R.string.downloadCancel, Toast.LENGTH_LONG).show();
			        		}
			        	});
			        	
			        	return CANCEL;
			        }
			        downloaded += read;
			        if (i % 5 == 0) {
			        	out.flush();
			        	final int downloadedKb = (int) (downloaded / 1024);
				        final int progress = (int) ((downloaded / fileSize) * 100);
				        handler.post(new Runnable() {
	                        public void run() {
	                        	StringBuffer sb = new StringBuffer(getResources().getString(R.string.downloading))
	                        		.append(" ").append(progress).append("%\n").append(downloadedKb).append(" / ").append(fileSizeKb).append(" KB");
	                        	pdDownload.setMessage(sb.toString());
	                        }
		                });
			        }
			    }
			    out.flush();
			    out.close();
			    
				handler.post(new Runnable() {
                    public void run() {
                		pdDownload.setMessage(getResources().getString(R.string.importing));
                    }
                });
			    
			    BufferedReader br = null;
				i = 0;
				try {
					br = new BufferedReader(new InputStreamReader(new FileInputStream(destFileName.toString()), "UTF-8"), 8192);
					
					byte[] bomUtf8 = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
					String line = br.readLine();
					if (line == null) return null;
					if (Character.toString(line.charAt(0)).equals(new String(bomUtf8))) {
						line = line.substring(1);
					} 
				
					Map<String, Long> categoryMap = databaseHelper.getCategoryMap();
					List<Bookmark> bookmarkList = databaseHelper.getAllBookmarksForImport();
					do {
						final int countImported = i;
						if (isCancelled()) {
							handler.post(new Runnable() {
				        		public void run() {
				    				String strCancelFormat = getResources().getString(R.string.importCancel);  
				    				String strCancelMsg = String.format(strCancelFormat, countImported);
				    				Toast.makeText(DownloadBookmark.this, strCancelMsg, Toast.LENGTH_LONG).show();
				        		}
				        	});
				        	
				        	return CANCEL;
						}
						
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
								Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
								editor.putString(Constants.POSITION_CATEGORY, category);
								editor.commit();
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
				return SUCCESS + String.valueOf(i);
			} catch (Exception e) {				
				Log.d(TAG, "error in reading/extract file", e);
				return ERROR + Util.getRootCause(e);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException ie) {
						Log.d(TAG, "error in closing file", ie);
					}
				}
				if (conn != null) {
					try {
						if (conn.getInputStream() != null)
							conn.getInputStream().close();
						if(conn.getErrorStream() != null)
							conn.getErrorStream().close();
					} catch (Exception e) {
						Log.d(TAG, "error in closing connection", e);
					}
				}
			}
		}
		
		
		@Override
		protected void onPostExecute(Object result) {
			String strResult = (String) result;
			
			if (pdDownload != null) {				 
				pdDownload.dismiss();
				pdDownload = null;
			}
			if (result == null) {
				Toast.makeText(DownloadBookmark.this, R.string.downloadCancel, Toast.LENGTH_LONG).show();
				return;
			}
			
			if (strResult.startsWith(SUCCESS)) {
				Integer importCount = Integer.valueOf(strResult.substring(SUCCESS.length()));
				String strSuccessFormat = getResources().getString(R.string.import_success);  
				String strSuccessMsg = String.format(strSuccessFormat, importCount);
				Toast.makeText(DownloadBookmark.this, strSuccessMsg, Toast.LENGTH_LONG).show();
				finish();
			} else if (strResult.startsWith(ERROR)) {
				String strMessage = "Error: " + strResult.substring(ERROR.length());
				Toast.makeText(DownloadBookmark.this, strMessage, Toast.LENGTH_LONG).show();			
			} else if (result.equals(MISSING_BOOKMARK_FILE)) {
				Toast.makeText(DownloadBookmark.this, R.string.missingBookmarkFile, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	protected void onResume() {
		super.onResume();
		databaseHelper.open();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//close database
		databaseHelper.close();
	}
	
	private final int menuRefresh = 1;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(Menu.NONE, menuRefresh, Menu.NONE, R.string.refresh);
		item.setIcon(R.drawable.menu_refresh);
		item = menu.add(Menu.NONE, R.id.help, Menu.NONE, R.string.help);
		item.setIcon(R.drawable.menu_help);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case menuRefresh:
			try {
				URL url = new URL(homeUrl + "bookmark_index_all.txt");
				Log.d(TAG, "Accessing Bookmark Repository All");
				pdRepository = ProgressDialog.show(this, 
						getResources().getString(R.string.pleaseWait), 
						getResources().getString(R.string.loading), true, false);
				new LoadingRepositoryTask().execute(url);
			} catch (MalformedURLException e) {
				Toast.makeText(this, R.string.repositoryUrlNotValid, Toast.LENGTH_LONG).show();
			}
		case R.id.help:
			Intent iHelp = new Intent(this, Help.class);
			iHelp.putExtra(Constants.FONT_SIZE, currentFontSize);
			iHelp.putExtra(Constants.HELP_CONTENT, R.string.help_downloadBookmark);
			startActivity(iHelp);
			return true;
		}
		return false;
	}

	
}
