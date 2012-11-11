package com.konibee.bible;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class DownloadBookname extends ListActivity implements OnItemClickListener, android.content.DialogInterface.OnClickListener, OnCancelListener {
	private static final String TAG="DownloadBookname";
	private static final int TIMEOUT = 15;  
	
	private ProgressDialog pdRepository;
	private ProgressDialog pdDownload;

	private List<String> booknameList = Collections.synchronizedList(new ArrayList<String>());
	private int currentFontSize;
	
	private AlertDialog confirmDownload;
	private String homeUrl = "http://open-bibles.appspot.com/bibles/";
	private String downloadFilename;
	
	private DisplayLanguageAdapter adapter;
	
	private Handler handler = new Handler();
	
	private final String ERROR = "INVALID_URL";
	private final String MISSING_BOOKMARK_FILE = "MISSING_BOOKMARK_FILE";
	private final String SUCCESS = "SUCCESS";
	private final String CANCEL = "CANCEL";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloadbookname);
		
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {		
			File sdcard = Environment.getExternalStorageDirectory();
			File booknameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
			if (!booknameFolder.isDirectory()) {
				boolean success = booknameFolder.mkdirs();			
				Log.d(TAG, "Creating bookname directory success: " + success);
			}
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirmDownload);
		builder.setPositiveButton(getResources().getString(R.string.yes), this);
		builder.setNegativeButton(getResources().getString(R.string.no), this);
		confirmDownload = builder.create();
		
		readPreference();
		
		adapter = new DisplayLanguageAdapter(this, R.layout.rowdownload, booknameList, currentFontSize);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		
		try {
			URL url = new URL(homeUrl + "bookname_index.txt");
			Log.d(TAG, "Accessing Bookname Repository");
			pdRepository = ProgressDialog.show(this, 
					getResources().getString(R.string.pleaseWait), 
					getResources().getString(R.string.loading), true, false);
			new LoadingRepositoryTask().execute(url);
		} catch (MalformedURLException e) {
			Toast.makeText(this, R.string.repositoryUrlNotValid, Toast.LENGTH_LONG).show();
		}
	}
	
	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentFontSize = preference.getInt(Constants.FONT_SIZE, 14);
	}

	private class LoadingRepositoryTask extends AsyncTask<Object, Void, Object> {
		@Override
		protected Object doInBackground(Object... arg) {
			booknameList.clear();
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
						if (i == 0) { //check for UTF-8 bom
							if (Character.toString(line.charAt(0)).equals(new String(bomUtf8, "UTF-8"))) {
								line = line.substring(1);
							}							
						}
						i++;						
						booknameList.add(line.substring(0,1).toUpperCase() + line.substring(1));
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
					Toast.makeText(DownloadBookname.this, strMessage, Toast.LENGTH_LONG).show();
				} 
				pdRepository.dismiss();
				pdRepository = null;
			}
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String bookname = booknameList.get(position);
		downloadFilename = bookname.toLowerCase() + ".bkn";
		
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG);
			return;
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		StringBuffer destFileName = new StringBuffer(sdcard.getPath());
		destFileName.append(Constants.BOOKNAME_FOLDER).append("/").append(downloadFilename);
		File f = new File(destFileName.toString());
		String str;
		if (f.exists()) {
			str = getResources().getString(R.string.confirmDownloadOverwrite);
		} else {
			str = getResources().getString(R.string.confirmDownloadQuestion);
		}
		String confirmQuestion = String.format(str, bookname + " Book Name");

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
					URL url = new URL(homeUrl + downloadFilename);
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
			destFileName.append(Constants.BOOKNAME_FOLDER).append("/").append(downloadFilename);
			
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
			        			Toast.makeText(DownloadBookname.this, R.string.downloadCancel, Toast.LENGTH_LONG).show();
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
			    
							    
			    return SUCCESS;
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
				Toast.makeText(DownloadBookname.this, R.string.downloadCancel, Toast.LENGTH_LONG).show();
				return;
			}
			
			if (strResult.startsWith(SUCCESS)) {
				Editor editor = PreferenceManager.getDefaultSharedPreferences(DownloadBookname.this).edit();
				editor.putString(Constants.BOOK_LANGUAGE, downloadFilename.substring(0, downloadFilename.length()-4));
				editor.commit();
				finish();
			} else if (strResult.startsWith(ERROR)) {
				String strMessage = "Error: " + strResult.substring(ERROR.length());
				Toast.makeText(DownloadBookname.this, strMessage, Toast.LENGTH_LONG).show();			
			} else if (result.equals(MISSING_BOOKMARK_FILE)) {
				Toast.makeText(DownloadBookname.this, R.string.missingBookmarkFile, Toast.LENGTH_LONG).show();
			}
		}
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
				URL url = new URL(homeUrl + "bookname_index.txt");
				Log.d(TAG, "Accessing Bookname Repository All");
				pdRepository = ProgressDialog.show(this, 
						getResources().getString(R.string.pleaseWait), 
						getResources().getString(R.string.loading), true, false);
				new LoadingRepositoryTask().execute(url);
			} catch (MalformedURLException e) {
				Toast.makeText(this, R.string.repositoryUrlNotValid, Toast.LENGTH_LONG).show();
			}
			return true;
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
