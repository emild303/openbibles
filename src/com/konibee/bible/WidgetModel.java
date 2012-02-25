package com.konibee.bible;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class WidgetModel {
	private String bookLanguage;
	private String bibleFileName;
	private String[] arrBookAbbr = new String[66];
	
	private static final String TAG = "WidgetModel";
	
	private static final String PREFS_NAME = "com.konibee.bible.Widget";
	private static final String PREFS_PREFIX = "widget_";
	
	public WidgetModel(String bookLanguage, String bibleFileName) {
		this.bookLanguage = bookLanguage;
		this.bibleFileName = bibleFileName;
		
		File sdcard = Environment.getExternalStorageDirectory();
		File bookNameFolder = new File(sdcard.getPath() + Constants.BOOKNAME_FOLDER);
		File bookNameFile = new File(bookNameFolder, bookLanguage.toLowerCase() + ".bkn");
		if (bookNameFile.isFile()) {
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
					arrBookAbbr[i] = abbr;
					if (i==42) {
						System.out.println("constructor" + arrBookAbbr[i]);
					}
					i++;
				}
			} catch (Exception e) {
				Log.d(TAG, "Error read bookname file", e);
				for (int i=0; i<66; i++ ) {
					String bookName = Constants.arrBookName[i];
					String abbr = "";
					if (bookName.startsWith("1") || bookName.startsWith("2") || bookName.startsWith("3")) {
		            	abbr = bookName.substring(0,5);
		            } else {
		            	abbr = bookName.substring(0,3);
		            }
					arrBookAbbr[i] = abbr;
				}
			}
		}
	}
	
	public WidgetModel(String str) {
		String[] data = str.split("[|]");
		this.bookLanguage = data[0];
		this.bibleFileName = data[1];
		boolean abbrFound = false;
		if (data.length == 3) {
			String[] arrAbbr = data[2].split(";;");
			if (arrAbbr.length >= 66) {
				abbrFound = true;
				for (int i=0; i<66; i++) {
					arrBookAbbr[i] = arrAbbr[i];
				}
			}
		} 
		if (!abbrFound) {
			for (int i=0; i<66; i++) {
				String bookName = Constants.arrBookName[i];
				String abbr = "";
				if (bookName.startsWith("1") || bookName.startsWith("2") || bookName.startsWith("3")) {
	            	abbr = bookName.substring(0,5);
	            } else {
	            	abbr = bookName.substring(0,3);
	            }
				arrBookAbbr[i] = abbr; 
			}
		}
	}
	
	public String storageString() {
		StringBuffer sb = new StringBuffer(this.bookLanguage).append("|").append(this.bibleFileName).append("|");
		for (int i=0; i<66; i++) {
			sb.append(arrBookAbbr[i]).append(";;");
			if (i==42) {
				System.out.println("storage string" + arrBookAbbr[i]);
			}
		}
		return sb.toString();
	}
	
	public static void saveWidgetData(Context context,int widgetId,WidgetModel model) {
		SharedPreferences.Editor prefsEditor = context.getSharedPreferences(PREFS_NAME, 0).edit();
		prefsEditor.putString(PREFS_PREFIX + widgetId,model.storageString());
		prefsEditor.commit();
	}
	
	public static WidgetModel getWidgetData(Context context,int widget) {
		SharedPreferences prefs = context.getSharedPreferences (PREFS_NAME, 0);
		String ret = prefs.getString(PREFS_PREFIX + widget,"BAD");
		if (ret.equals("BAD")) return null;
		return new WidgetModel(ret);
	}
	
	public static void deleteWidgetData(Context context,int widgetId) {
		SharedPreferences.Editor prefsEditor = context.getSharedPreferences(PREFS_NAME, 0).edit();
		prefsEditor.remove(PREFS_PREFIX + widgetId);
		prefsEditor.commit();
	}

	public String getBookLanguage() {
		return bookLanguage;
	}

	public void setBookLanguage(String bookLanguage) {
		this.bookLanguage = bookLanguage;
	}

	public String getBibleFileName() {
		return bibleFileName;
	}

	public void setBibleFileName(String bibleFileName) {
		this.bibleFileName = bibleFileName;
	}

	public String[] getArrBookAbbr() {
		return arrBookAbbr;
	}

	public void setArrBookAbbr(String[] arrBookAbbr) {
		this.arrBookAbbr = arrBookAbbr;
	}
	
}
