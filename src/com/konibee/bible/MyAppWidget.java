package com.konibee.bible;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.RemoteViews;

public class MyAppWidget extends AppWidgetProvider {
	private static final String TAG = "BiblesOffline";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		for (int i = 0; i < appWidgetIds.length; i++) {
			MyAppWidget.updateMyWidget(context, appWidgetIds[i]);
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int i = 0; i < appWidgetIds.length; i++) {
			WidgetModel.deleteWidgetData(context, appWidgetIds[i]);
		}
	}

	public static void updateMyWidget(Context context, int appWidgetId) {
		AppWidgetManager appWidgetManager =	AppWidgetManager.getInstance(context);
		WidgetModel model = WidgetModel.getWidgetData(context, appWidgetId);
		if (model == null) {
			model = new WidgetModel(Constants.LANG_ENGLISH, Constants.AS_BOOKMARKED);
		}
		
		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		databaseHelper.open();
		Bookmark bm = databaseHelper.getRandomBookmark();
		databaseHelper.close();
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
		if (bm != null) {
//			String currentBookLanguage = model.getBookLanguage();
			String bookName;
//            if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//            	bookName = Constants.arrBookNameIndo[bm.getBook()-1];
//            } else {
//            	bookName = Constants.arrBookName[bm.getBook()-1];
//            }
//            if (bookName.startsWith("1") || bookName.startsWith("2") || bookName.startsWith("3")) {
//            	bookName = bookName.substring(0,5);
//            } else {
//            	bookName = bookName.substring(0,3);
//            }
			bookName = model.getArrBookAbbr()[bm.getBook()-1];
            
			StringBuffer sbBook = new StringBuffer();
            sbBook.append(bookName).append(" ").append(bm.getChapter()).append(":").append(bm.getVerseStart());
            if (!bm.getVerseEnd().equals(bm.getVerseStart())) {
            	sbBook.append("-").append(bm.getVerseEnd());
            }
            
            String verse = bm.getContent();
            String bibleName = " (" + bm.getBible().toUpperCase() + ")";
            if (!Constants.AS_BOOKMARKED.equals(model.getBibleFileName())) {
            	String filename = model.getBibleFileName();
            	String state = Environment.getExternalStorageState();
        		if (Environment.MEDIA_MOUNTED.equals(state)) {
        			File sdcard = Environment.getExternalStorageDirectory();
        			File file = new File(sdcard, Constants.BIBLE_FOLDER + "/" + filename);
        			String indexFileName = file.getAbsolutePath().replaceAll(".ont", ".idx");
        			File fIndex = new File(indexFileName);
        			
        			if (file.isFile() && fIndex.isFile()) {
        				try {
	        				int chapterIndex = Constants.arrBookStart[bm.getBook()-1] + bm.getChapter()-1;
	        				DataInputStream is = new DataInputStream(new FileInputStream(fIndex));
	        				is.skip(chapterIndex*4);			
	        				int startOffset = is.readInt();
	        				is.close();
	        				
	        				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
	        				br.skip(startOffset);
	        				for (int i = 0; i < bm.getVerseStart()-1; i++) {
	        					br.readLine();
	        				}        				
	        				StringBuffer sbVerse = new StringBuffer();
	        				for (int i = bm.getVerseStart(); i <= bm.getVerseEnd(); i++) {
	        					sbVerse.append(br.readLine()).append(" "); 
	        				}
	        				br.close();
	        				if (sbVerse.length() > 0) {
	    						sbVerse.delete(sbVerse.length()-1, sbVerse.length());
	    					}
	    					verse = Util.parseVerse(sbVerse.toString());
	    					bm.setBible(filename.substring(0, filename.length()-4));
	    					bibleName = " (" + filename.substring(0, filename.length()-4).toUpperCase() + ")";
        				} catch (Exception e) {
        					Log.e(TAG, "Error reading bible file for widget", e);
        				}
        			}
        		}
            } 
            sbBook.append(bibleName);
            
            Spannable spanText = new SpannableString(sbBook.toString() + " " + verse);
            spanText.setSpan(new ForegroundColorSpan(Color.argb(255,0,0,120)), 0, sbBook.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, sbBook.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            views.setTextViewText(R.id.txtContent, spanText);
		} else {
			views.setTextViewText(R.id.txtContent, "[No bookmark available]");
		}
		if (bm != null) {
			Intent mainIntent = new Intent(context, BiblesOffline.class);
			mainIntent.setData(Uri.parse("file:///mainIntent" + appWidgetId));
			mainIntent.putExtra(Constants.FROM_WIDGET, true);
			mainIntent.putExtra(Constants.WIDGET_BIBLE, bm.getBible());
			mainIntent.putExtra(Constants.WIDGET_BOOK, bm.getBook());
			mainIntent.putExtra(Constants.WIDGET_CHAPTER, bm.getChapter());
			mainIntent.putExtra(Constants.WIDGET_VERSE, bm.getVerseStart());
			PendingIntent pendingMainIntent = PendingIntent.getActivity(context, 0,	mainIntent,	PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.txtContent, pendingMainIntent);
		}
		
		Intent nextIntent = new Intent(context, MyAppWidget.class);
		nextIntent.setData(Uri.parse("file:///nextIntent" + appWidgetId));
		nextIntent.setAction(Constants.NEXT_BOOKMARK_ACTION);
		nextIntent.putExtra(Constants.WIDGET_ID, appWidgetId);
		PendingIntent pendingNextIntent = PendingIntent.getBroadcast(context, 1, nextIntent, 0);
		views.setOnClickPendingIntent(R.id.txtNext, pendingNextIntent);
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onReceive(Context context,Intent intent) {
		super.onReceive(context, intent);
		if (intent.getAction().equals(Constants.NEXT_BOOKMARK_ACTION)) {
			if (intent.getExtras() == null) return;
			int appWidgetId = intent.getExtras().getInt(Constants.WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				updateMyWidget(context, appWidgetId);
			}
		}
	}
	
}
