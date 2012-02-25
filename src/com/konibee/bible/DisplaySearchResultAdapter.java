package com.konibee.bible;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DisplaySearchResultAdapter extends ArrayAdapter<SearchResult> {
	private List<SearchResult> resultList = new ArrayList<SearchResult>();
	private List<String> wordsToSearch = new ArrayList<String>();
	private Context context;
	private String currentBookLanguage;
	private int currentFontSize;
	
	public DisplaySearchResultAdapter(Context context, int textViewResourceId,
			List<SearchResult> resultList, List<String> wordsToSearch, String currentBookLanguage, int currentFontSize) {
		super(context, textViewResourceId, resultList);
		this.context = context;
		this.resultList = resultList;
		this.wordsToSearch = wordsToSearch;
		this.currentBookLanguage = currentBookLanguage;
		this.currentFontSize = currentFontSize;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.rowbookmark, null);
        }
        SearchResult sr = resultList.get(position);
        if (sr != null) {
            TextView txtBook = (TextView) v.findViewById(R.id.txtBook);
            TextView txtContent = (TextView) v.findViewById(R.id.txtContent);
            
            txtBook.setTextSize(currentFontSize);
            txtContent.setTextSize(currentFontSize);
            
            String bookName;
            
//            if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//            	bookName = Constants.arrBookNameIndo[sr.getBook()-1];
//            } else {
//            	bookName = Constants.arrBookName[sr.getBook()-1];
//            }
            bookName = Constants.arrActiveBookName[sr.getBook()-1];
            
            StringBuffer sbBook = new StringBuffer();
            sbBook.append(bookName).append(" ").append(sr.getChapter()).append(":").append(sr.getVerse());
            txtBook.setText(sbBook.toString());
            
            txtContent.setText(parseVerseForDisplay(sr.getContent()));
        }
		return v;
	}

	private Spannable parseVerseForDisplay(String verse) {
		List<Integer> posStartList = new ArrayList<Integer>();
		List<Integer> posEndList = new ArrayList<Integer>();
		for (String word : wordsToSearch) {
			int i = 0;
			int posStart = verse.toLowerCase().indexOf(word.toLowerCase(), i);
			while (posStart > -1) {
				posStartList.add(posStart);
				posEndList.add(posStart + word.length());
				i = posStart + 1;
				if (i == verse.length()) break;
				posStart = verse.indexOf(word, i);
			}
		}
		Spannable spanText = new SpannableString(verse);
		for (int i=0; i<posStartList.size(); i++) {
			int posFRBegin = posStartList.get(i);
			int posFREnd = posEndList.get(i);
			spanText.setSpan(new ForegroundColorSpan(Color.GREEN), posFRBegin, posFREnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return spanText;
	}
}
