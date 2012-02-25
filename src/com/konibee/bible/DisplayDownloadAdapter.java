package com.konibee.bible;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DisplayDownloadAdapter extends ArrayAdapter<DisplayDownload> {
	private List<DisplayDownload> bibleList = new ArrayList<DisplayDownload>();
	private Context context;
	private int currentFontSize;

	public DisplayDownloadAdapter(Context context, int textViewResourceId,
			List<DisplayDownload> bibleList, int currentFontSize) {
		super(context, textViewResourceId, bibleList);
		this.context = context;
		this.bibleList = bibleList;
		this.currentFontSize = currentFontSize;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.rowdownload, null);
        }
        DisplayDownload bible = bibleList.get(position);
        if (bible != null) {
            TextView txtBibleName = (TextView) v.findViewById(R.id.txtBibleName);
            TextView txtBibleInfo = (TextView) v.findViewById(R.id.txtBibleInfo);
            
            txtBibleName.setTextSize(currentFontSize);
            txtBibleInfo.setTextSize(currentFontSize);
            
            txtBibleName.setText(bible.getName());
            txtBibleInfo.setText(Html.fromHtml(bible.getDescription()));
        }
        return v;
            
	}

}
