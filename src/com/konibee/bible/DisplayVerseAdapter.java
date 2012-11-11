package com.konibee.bible;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DisplayVerseAdapter extends ArrayAdapter<DisplayVerse> implements OnCreateContextMenuListener{
	private List<DisplayVerse> verseList = new ArrayList<DisplayVerse>();
	private int currentFontSize;
	private Context context;

	public DisplayVerseAdapter(Context context, int textViewResourceId,
			List<DisplayVerse> verseList, int currentFontSize) {
		super(context, textViewResourceId, verseList);
		this.context = context;
		this.verseList = verseList;
		this.currentFontSize = currentFontSize;
	}
	
	public void updateFontSize(int fontSize) {
		currentFontSize = fontSize;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row, null);
        }
        DisplayVerse verse = verseList.get(position);
        if (verse != null) {
            TextView txtVerse = (TextView) v.findViewById(R.id.txtVerse);
            txtVerse.setMovementMethod(LinkMovementMethod.getInstance());
            txtVerse.setText(parseVerseForDisplay(verse, v));
            txtVerse.setTextSize(currentFontSize);
        }
        v.setOnCreateContextMenuListener(this);
		return v;
	}
	
	private Spannable parseVerseForDisplay(DisplayVerse displayVerse, View v) {
        TextView txtBookmark = (TextView) v.findViewById(R.id.txtBookmark);
		StringBuffer spaceBeforeIcon = new StringBuffer();
        
		List<Integer> posVerseList = new ArrayList<Integer>();
		List<Integer> posTSBeginList = new ArrayList<Integer>();
		List<Integer> posTSEndList = new ArrayList<Integer>();
		List<Integer> posFRBeginList = new ArrayList<Integer>();
		List<Integer> posFREndList = new ArrayList<Integer>();
		List<Integer> posRFList = new ArrayList<Integer>();
		final List<String> textRFList = new ArrayList<String>();
		String verse = displayVerse.getVerse();
		
		verse = verse.replaceAll("<FI>", "[");
		verse = verse.replaceAll("<Fi>", "]");
		
		boolean insertLineBreak = displayVerse.isInsertLineBreak();
		int verseNumber = displayVerse.getVerseNumber();
		
		StringBuffer text = new StringBuffer();
		StringBuffer sb = new StringBuffer(verse);
		int posStartTag = sb.indexOf("<");
		boolean writeVerse = false;
		if (insertLineBreak) {
			text.append("\n");
			spaceBeforeIcon.append("\n");
		}
		boolean tsNotClose = false;
		while (posStartTag > -1) {
			int posEndTag = sb.indexOf(">");
			String tag = sb.substring(posStartTag+1, posEndTag);
			if (tag.equals("TS")) {
				if (posStartTag > 0) {					
					if (!writeVerse) {
						writeVerse = true;
						posVerseList.add(text.length());
						text.append(verseNumber).append(" ");								
					}
					text.append(sb.substring(0, posStartTag)).append("\n\n");
					spaceBeforeIcon.append("\n\n");
					
				} else if (!insertLineBreak && verseNumber > 1) {
					text.append("\n");
					spaceBeforeIcon.append("\n");
				}
				sb.delete(0, posEndTag+1);
				posTSBeginList.add(text.length());
				tsNotClose = true;
			} else if (tag.equals("Ts")) {
				if (posStartTag > 0) {
					text.append(sb.substring(0, posStartTag)).append("\n\n");
					spaceBeforeIcon.append("\n\n");
				} else if (tsNotClose) {
					text.append("\n\n");
					spaceBeforeIcon.append("\n\n");
				}
				sb.delete(0, posEndTag+1);
				posTSEndList.add(text.length());
				tsNotClose = false;
			} else if (tag.equals("FR")) {
				if (!writeVerse) {
					writeVerse = true;
					posVerseList.add(text.length());
					text.append(verseNumber).append(" ");								
				}
				if (posStartTag > 0) {
					text.append(sb.substring(0, posStartTag));
				}
				sb.delete(0, posEndTag+1);
				posFRBeginList.add(text.length());
			} else if (tag.equals("Fr")) {
				if (!writeVerse) {
					writeVerse = true;
					posVerseList.add(text.length());
					text.append(verseNumber).append(" ");								
				}
				if (posStartTag > 0) {
					text.append(sb.substring(0, posStartTag));
				}
				sb.delete(0, posEndTag+1);
				posFREndList.add(text.length());
				
			} else if (tag.equals("RF"))  {
				if (!writeVerse && !tsNotClose) {
					writeVerse = true;
					posVerseList.add(text.length());
					text.append(verseNumber).append(" ");								
				}
				if (posStartTag > 0) {
					text.append(sb.substring(0, posStartTag));
				}
				sb.delete(0, posEndTag+1);
				
				int posFREnd = sb.indexOf("<Rf>");
				if (posFREnd > -1) {
					posRFList.add(text.length());
					textRFList.add(sb.substring(0, posFREnd));
					sb.delete(0, posFREnd + "<Rf>".length());
					text.append("* ");
				} else {
					posRFList.add(text.length());
					textRFList.add(sb.substring(0, posFREnd));
					sb.delete(0, sb.length());
					text.append("* ");
				}
			} else {
				sb.delete(posStartTag, posEndTag+1);
			}
			posStartTag = sb.indexOf("<");
		}
		
		if (!writeVerse && sb.length() > 0) {
			posVerseList.add(text.length());
			text.append(verseNumber).append(" ");
		}
		text.append(sb.toString());
		
		Spannable spanText = new SpannableString(text.toString());
		for (int i=0; i < posVerseList.size(); i++) {
			int posVerse = posVerseList.get(i);
			String strI = String.valueOf(verseNumber);
//			spanText.setSpan(new ForegroundColorSpan(Color.BLUE), posVerse, posVerse+strI.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		for (int i=0; i<posTSBeginList.size(); i++) {
			int posTSBegin = posTSBeginList.get(i);
			int posTSEnd = posTSEndList.get(i);
//			spanText.setSpan(new ForegroundColorSpan(Color.GREEN), posTSBegin, posTSEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		for (int i=0; i<posFRBeginList.size(); i++) {
			int posFRBegin = posFRBeginList.get(i);
			int posFREnd = posFREndList.get(i);
//			spanText.setSpan(new ForegroundColorSpan(0xffff7777), posFRBegin, posFREnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		for (int i=0; i<posRFList.size(); i++) {
			int posRF = posRFList.get(i);
			final int j = i;
			spanText.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View widget) {
					BiblesOffline mainActivity = (BiblesOffline) context;
					TextView txtFootnote = (TextView) mainActivity.getFootnoteView().findViewById(R.id.txtFootnote);
					txtFootnote.setText(textRFList.get(j));
					mainActivity.getFootnoteDialog().show();
				}
				
				@Override
				public void updateDrawState(TextPaint ds) {
					super.updateDrawState(ds);
					ds.setUnderlineText(false);
				}
				
			}, posRF, posRF+2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		if (displayVerse.isBookmark()) {
			spaceBeforeIcon.append("    ");
			Spannable spanBookmark = new SpannableString(spaceBeforeIcon.toString());
			ImageSpan imageSpan = new ImageSpan(context, R.drawable.bookmark, ImageSpan.ALIGN_BOTTOM);
			spanBookmark.setSpan(imageSpan, spaceBeforeIcon.length()-4, spaceBeforeIcon.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			txtBookmark.setTextSize(currentFontSize);
			txtBookmark.setText(spanBookmark);
		} else {
			txtBookmark.setText("");
		}
		
		return spanText;
	}

	@Override
	public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        // empty implementation
	}

}
