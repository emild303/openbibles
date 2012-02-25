package com.konibee.bible;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

public class DownloadDocument extends ListActivity implements OnItemClickListener, android.content.DialogInterface.OnClickListener, OnCancelListener {
	private static final String TAG = "DownloadDocument";
	private static final int TIMEOUT = 15;  
	
	private ProgressDialog pdRepository;
	private ProgressDialog pdDownload;
	
	private List<DisplayDownload> allDocumentList = Collections.synchronizedList(new ArrayList<DisplayDownload>());
	private List<DisplayDownload> documentList = Collections.synchronizedList(new ArrayList<DisplayDownload>());
	private List<String> languageSet = new ArrayList<String>();
	private List<Map<String, String>> languageList = Collections.synchronizedList(new ArrayList<Map<String, String>>());
	private DisplayDownloadAdapter adapter;
	private SimpleAdapter languageAdapter;
	private int currentFontSize;
	
	private AlertDialog confirmDownload;
	private String homeUrl = "http://open-bibles.appspot.com/bibles/";
	private String downloadFileName;
	
	private Handler handler = new Handler();
	
	private final String INVALID_FILE = "INVALID_FILE";
	private final String ERROR = "INVALID_URL";
	private final String FILE_CORRUPT = "FILE_CORRUPT";
	private final String MISSING_CHECKSUM = "MISSING_CHECKSUM";
	private final String MISSING_DOCUMENT_FILE = "MISSING_BIBLE_FILE";
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
            	refreshDocumentList(parent, position);
            }
 
            public void onNothingSelected(AdapterView<?> arg0) {
                //do nothing
            }
        });
		
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {		
			File sdcard = Environment.getExternalStorageDirectory();
			File downloadFolder = new File(sdcard.getPath() + Constants.DOWNLOAD_FOLDER);
			if (!downloadFolder.isDirectory()) {
				boolean success = downloadFolder.mkdirs();			
				Log.d(TAG, "Creating download directory success: " + success);
			}
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirmDownload);
		builder.setPositiveButton(getResources().getString(R.string.yes), this);
		builder.setNegativeButton(getResources().getString(R.string.no), this);
		confirmDownload = builder.create();
		
		readPreference();
		
		adapter = new DisplayDownloadAdapter(this, R.layout.rowdownload, documentList, currentFontSize);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		
		try {
			URL url = new URL(homeUrl + "document_index_all.txt");
			Log.d(TAG, "Accessing Bible Repository All");
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
			documentList.clear();
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
						allDocumentList.add(new DisplayDownload(arrLine[0], arrLine[1], arrLine[2], arrLine[3]));
						if (!languageSet.contains(arrLine[0])) {
							languageSet.add(arrLine[0]);
						}
					}
				}
				return String.valueOf(i);
			} catch (Exception e) {
				Log.d(TAG, "Error reading document_index", e);
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
					Toast.makeText(DownloadDocument.this, strMessage, Toast.LENGTH_LONG).show();
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
				    refreshDocumentList(spinner, 0);
				}
				pdRepository.dismiss();
				pdRepository = null;
			}
		}
	}
	
	private void refreshDocumentList(AdapterView<?> parent, int position) {
        final Map<String, String> data = (Map<String, String>) parent.getItemAtPosition(position);
        documentList.clear();
        for (DisplayDownload bible : allDocumentList) {
        	if (bible.getLanguage() != null && bible.getLanguage().equals(data.get("languageName"))) {
        		documentList.add(bible);
        	}
        }
        adapter.notifyDataSetChanged();
	}
	
	private class DownloadTask extends AsyncTask<Object, Void, Object> {		
		@Override
		protected Object doInBackground(Object... arg) {
			File sdcard = Environment.getExternalStorageDirectory();
			StringBuffer destFileName = new StringBuffer(sdcard.getPath());
			destFileName.append(Constants.DOWNLOAD_FOLDER).append("/").append(downloadFileName.toLowerCase()).append(".zip");
			StringBuffer extractedFileName = new StringBuffer(sdcard.getPath());
			extractedFileName.append(Constants.DOCUMENT_FOLDER).append("/");
			String crc32FileName = destFileName.toString().replaceAll(".zip", ".crc32");
			File destFile = new File(destFileName.toString());
			
			URL url = (URL) arg[0];
			URL urlCrc32 = (URL) arg[1];
			HttpURLConnection conn = null;
			InputStream stream = null; //to read
		    FileOutputStream out = null; //to write
		    InputStream isCrc32 = null;
		    double fileSize = 0;
		    double downloaded = 0; //
		    long prevCrc32 = -1;
		    BufferedOutputStream bufferOut = null;
		    BufferedInputStream isZip = null;
		    DataInputStream isSize = null;
		    DataOutputStream osSize = null;
			try {
				File fCrc32 = new File(crc32FileName);
				if (fCrc32.exists() && fCrc32.length() == 12) {
					isSize = new DataInputStream(new FileInputStream(fCrc32));
					prevCrc32 = isSize.readLong();					
					fileSize = isSize.readInt();
					isSize.close();
				}				
				
				//read crc32 file on server
				conn = (HttpURLConnection) urlCrc32.openConnection();
				conn.setConnectTimeout(TIMEOUT * 1000);
				conn.setReadTimeout(TIMEOUT * 1000);
				conn.connect();
				if (conn.getResponseCode() == 404) {
					conn.disconnect();
					return MISSING_CHECKSUM;
				}
				isCrc32 = conn.getInputStream();
				long crc32 = new DataInputStream(isCrc32).readLong();
				isCrc32.close();
				conn.disconnect();

				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(TIMEOUT * 1000);
				conn.setReadTimeout(TIMEOUT * 1000);
				if (crc32 != prevCrc32 || !destFile.exists()) {
					conn.connect();					
					if (conn.getResponseCode() == 404) {
						conn.disconnect();
						return MISSING_DOCUMENT_FILE;
					}
					fileSize = conn.getContentLength();
					Log.d(TAG, "filesize " + fileSize + url.getFile());
					osSize = new DataOutputStream(new FileOutputStream(fCrc32));
					osSize.writeLong(crc32);
					osSize.writeInt((int)fileSize);
					osSize.flush();
					osSize.close();
					out = new FileOutputStream(destFileName.toString());
				} else {
					downloaded = destFile.length();
					conn.setRequestProperty("Range", "bytes=" + (int)downloaded + "-");
					conn.connect();
					if (conn.getResponseCode() == 416) { // if range is invalid
						out = new FileOutputStream(destFileName.toString());
						conn.disconnect();
						conn = (HttpURLConnection) url.openConnection();
						conn.setConnectTimeout(TIMEOUT * 1000);
						conn.setReadTimeout(TIMEOUT * 1000);
						downloaded = 0;
					} else if (conn.getResponseCode() == 404) {
						conn.disconnect();
						return MISSING_DOCUMENT_FILE;
					} else {
						out = new FileOutputStream(destFileName.toString(), true);
					}
				}
				
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
			        			Toast.makeText(DownloadDocument.this, R.string.downloadCancel, Toast.LENGTH_LONG).show();
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

			    //validate checksum
			    isCrc32 = new BufferedInputStream(new FileInputStream(destFile));
				byte[] bytes = new byte[1024];
				int len = 0;
				Checksum checksum = new CRC32();
				while ((len = isCrc32.read(bytes)) >= 0) {
					checksum.update(bytes, 0, len);
				}
				isCrc32.close();
				if (checksum.getValue() != crc32) {
					return FILE_CORRUPT;
				}
			    
			    //extract file
			    ZipFile zipFile = new ZipFile(destFileName.toString());
			    if (zipFile.size() != 2) {
			    	return INVALID_FILE;
			    }
			    
			    handler.post(new Runnable() {
                    public void run() {
                    	pdDownload.setMessage(getResources().getString(R.string.extractingDocument));
                    }
                });
			    
			    @SuppressWarnings("rawtypes")
				Enumeration enumZip = zipFile.entries();
			    while (enumZip.hasMoreElements()) {
			    	ZipEntry entry = (ZipEntry) enumZip.nextElement();
			    	if (!entry.getName().endsWith(".toc") && !entry.getName().endsWith(".con")) {
				    	return INVALID_FILE;
				    }
			    	isZip = new BufferedInputStream(zipFile.getInputStream(entry));
				    byte[] bufferExtract = new byte[8192];
	                FileOutputStream outStream = new FileOutputStream(extractedFileName.toString() + entry.getName().toLowerCase());
	                bufferOut = new BufferedOutputStream(outStream, bufferExtract.length);
	                int size = -1;
	                while((size = isZip.read(bufferExtract, 0, bufferExtract.length)) != -1) {
	                    bufferOut.write(bufferExtract, 0, size);
	                }
	                bufferOut.flush();
	                outStream.close();
			    }
			    return SUCCESS;
			} catch (Exception e) {				
				Log.d(TAG, "error in reading/extract file", e);
				return ERROR + Util.getRootCause(e);
			} finally {
				if (isCrc32 != null) {
					try {
						isCrc32.close();
					} catch (IOException ie) {
						Log.d(TAG, "error in closing file", ie);
					}
				}
				if (isSize != null) {
					try {
						isSize.close();
					} catch (IOException ie) {
						Log.d(TAG, "error in closing file", ie);
					}
				}
				if (osSize != null) {
					try {
						osSize.close();
					} catch (IOException ie) {
						Log.d(TAG, "error in closing file", ie);
					}
				}
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
				if (isZip != null) {
					try {
						isZip.close();
					} catch (IOException ie) {
						Log.d(TAG, "error in closing file", ie);
					}
				}
				if (bufferOut != null) {
					try {
						bufferOut.close();
					} catch (IOException ie) {
						Log.d(TAG, "error in closing file", ie);
					}
				}
			}
		}
		
		
		@Override
		protected void onPostExecute(Object result) {
			if (result == null) {
				Toast.makeText(DownloadDocument.this, R.string.downloadCancel, Toast.LENGTH_LONG).show();
			}
			
			String strResult = (String) result;
			
			if (pdDownload != null) {				 
				pdDownload.dismiss();
				pdDownload = null;
			}
			if (result.equals(SUCCESS)) {
				String tocFileName = downloadFileName.toLowerCase() + ".toc";
				Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
				editor.putString(Constants.DOCUMENT_FILENAME, tocFileName);
				editor.putInt(Constants.DOCUMENT_POSITION, 0);
				editor.commit();
				finish();
			} else if (result.equals(INVALID_FILE)) {
				Toast.makeText(DownloadDocument.this, R.string.documentFileNotValid, Toast.LENGTH_LONG).show();
			} else if (strResult.startsWith(ERROR)) {
				String strMessage = "Error: " + strResult.substring(ERROR.length());
				Toast.makeText(DownloadDocument.this, strMessage, Toast.LENGTH_LONG).show();
			} else if (result.equals(FILE_CORRUPT)) {
				Toast.makeText(DownloadDocument.this, R.string.fileCorrupt, Toast.LENGTH_LONG).show();
			} else if (result.equals(MISSING_CHECKSUM)) {
				Toast.makeText(DownloadDocument.this, R.string.missingChecksum, Toast.LENGTH_LONG).show();
			} else if (result.equals(MISSING_DOCUMENT_FILE)) {
				Toast.makeText(DownloadDocument.this, R.string.missingDocumentFile, Toast.LENGTH_LONG).show();
			}
		}
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		DisplayDownload download = documentList.get(position);
		downloadFileName = download.getFileName();
		
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Toast.makeText(this, R.string.sdcardNotReady, Toast.LENGTH_LONG);
			return;
		}
		
		File sdcard = Environment.getExternalStorageDirectory();
		StringBuffer destFileName = new StringBuffer(sdcard.getPath());
		destFileName.append(Constants.DOCUMENT_FOLDER).append("/").append(downloadFileName.toLowerCase()).append(".toc");
		File f = new File(destFileName.toString());
		String str;
		if (f.exists()) {
			str = getResources().getString(R.string.confirmDownloadOverwrite);
		} else {
			str = getResources().getString(R.string.confirmDownloadQuestion);
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
					URL url = new URL(homeUrl + downloadFileName.toLowerCase() + ".zip");
					URL urlCrc32 = new URL(homeUrl + downloadFileName.toLowerCase() + ".crc32");
					Log.d(TAG, "Downloading file: " + url.getPath());
					pdDownload = ProgressDialog.show(this, 
							getResources().getString(R.string.pleaseWait), 
							getResources().getString(R.string.downloading), true, true, this);
					downloadTask = new DownloadTask();
					downloadTask.execute(url, urlCrc32);
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
				URL url = new URL(homeUrl + "document_index_all.txt");
				Log.d(TAG, "Accessing Bible Repository All");
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
			iHelp.putExtra(Constants.HELP_CONTENT, R.string.help_downloadDocument);
			startActivity(iHelp);
			return true;
		}
		return false;
	}
	
}


