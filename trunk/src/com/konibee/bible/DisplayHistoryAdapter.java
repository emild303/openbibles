package com.konibee.bible;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DisplayHistoryAdapter extends ArrayAdapter<Integer>{
	private List<Integer> historyList = new ArrayList<Integer>();
	private String currentBookLanguage; 

	public DisplayHistoryAdapter(Context context, int textViewResourceId,
			List<Integer> historyList, String currentBookLanguage) {
		super(context, textViewResourceId, historyList);
		this.historyList = historyList;
		this.currentBookLanguage = currentBookLanguage;
	}
	
	public void setCurrentBookLanguage(String currentBookLanguage) {
		this.currentBookLanguage = currentBookLanguage;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView v = (TextView) super.getView(position, convertView, parent);
		
		String[] arrBookChapter = Constants.arrVerseCount[historyList.get(position)].split(";");
  		int book = Integer.parseInt(arrBookChapter[0]);
  		int chapter = Integer.parseInt(arrBookChapter[1]);
  		StringBuffer sbBook = new StringBuffer();
		
//		if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//			sbBook.append(Constants.arrBookNameIndo[book - 1]).append(" ").append(chapter);
//		} else {
//			sbBook.append(Constants.arrBookName[book - 1]).append(" ").append(chapter);
//		}
  		sbBook.append(Constants.arrActiveBookName[book - 1]).append(" ").append(chapter);
		
		v.setText(sbBook.toString());
		return v;
	}
}
