package com.konibee.bible;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DisplayBookmarkAdapter extends ArrayAdapter<Bookmark> {
	private List<Bookmark> bookmarkList = new ArrayList<Bookmark>();
	private Context context;
	private String currentBookLanguage;
	private int currentFontSize;

	public DisplayBookmarkAdapter(Context context, int textViewResourceId,
			List<Bookmark> bookmarkList, String currentBookLanguage, int currentFontSize) {
		super(context, textViewResourceId, bookmarkList);
		this.context = context;
		this.bookmarkList = bookmarkList;
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
        Bookmark bm = bookmarkList.get(position);
        if (bm != null) {
            TextView txtBook = (TextView) v.findViewById(R.id.txtBook);
            TextView txtDate = (TextView) v.findViewById(R.id.txtDate);
            TextView txtContent = (TextView) v.findViewById(R.id.txtContent);
            
            txtBook.setTextSize(currentFontSize);
            txtDate.setTextSize(currentFontSize);
            txtContent.setTextSize(currentFontSize);
            
            String bookName;
//            if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//            	bookName = Constants.arrBookNameIndo[bm.getBook()-1];
//            } else {
//            	bookName = Constants.arrBookName[bm.getBook()-1];
//            }
            bookName = Constants.arrActiveBookName[bm.getBook()-1];
            
            StringBuffer sbBook = new StringBuffer();
            sbBook.append(bookName).append(" ").append(bm.getChapter()).append(":").append(bm.getVerseStart());
            if (!bm.getVerseEnd().equals(bm.getVerseStart())) {
            	sbBook.append("-").append(bm.getVerseEnd());
            }
            sbBook.append(" (").append(bm.getBible().toUpperCase()).append(")");
            txtBook.setText(sbBook.toString());
            
            SimpleDateFormat fmtDbDate = new SimpleDateFormat(Constants.DB_DATE_FORMAT);
            try {
				Date bookmarkDate = fmtDbDate.parse(bm.getBookmarkDate());
	            java.text.DateFormat fmtDate=DateFormat.getDateFormat(context);
	            StringBuffer sb = new StringBuffer();
	            sb.append(fmtDate.format(bookmarkDate));
	            txtDate.setText(sb.toString());
			} catch (ParseException e) {
				e.printStackTrace();
				txtDate.setText(bm.getBookmarkDate());
			}

            txtContent.setText(bm.getContent());
        }
		return v;
	}

	
}
