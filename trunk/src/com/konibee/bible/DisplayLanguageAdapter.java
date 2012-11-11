package com.konibee.bible;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DisplayLanguageAdapter extends ArrayAdapter<String> {
	private List<String> languageList = new ArrayList<String>();
	private Context context;
	private int currentFontSize;

	public DisplayLanguageAdapter(Context context, int textViewResourceId,
			List<String> languageList, int currentFontSize) {
		super(context, textViewResourceId, languageList);
		this.context = context;
		this.languageList = languageList;
		this.currentFontSize = currentFontSize;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.rowdownload, null);
        }
        String language = languageList.get(position);
        if (language != null) {
            TextView txtBibleName = (TextView) v.findViewById(R.id.txtBibleName);
            txtBibleName.setTextColor(Color.WHITE);
            txtBibleName.setTextSize(currentFontSize);
            txtBibleName.setText(language);
        }
        return v;
            
	}

}
