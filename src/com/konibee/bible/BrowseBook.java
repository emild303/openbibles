package com.konibee.bible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

public class BrowseBook extends ExpandableListActivity {
	private final String NAME = "NAME";
	private String currentBookLanguage;
	private ExpandableListAdapter adapter;
	private final int BROWSE_CHAPTER = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.browse);
        readPreference();
        adapter = createAdapter();
        setListAdapter(adapter); 
	}
	
	private void readPreference() {
		SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
		currentBookLanguage = preference.getString(Constants.BOOK_LANGUAGE, Constants.LANG_ENGLISH);
	}
	
	private ExpandableListAdapter createAdapter() {
        List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();

        Map<String, String> groupOT = new HashMap<String, String>();
        groupData.add(groupOT);
        groupOT.put(NAME, "Old Testament");
        List<Map<String, String>> otChild = new ArrayList<Map<String, String>>();
        childData.add(otChild);
        for (int i=0; i<39; i++) {
        	Map<String, String> bookMap = new HashMap<String, String>();
        	otChild.add(bookMap);
//        	if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//        		bookMap.put(NAME, Constants.arrBookNameIndo[i]);
//        	} else {
//        		bookMap.put(NAME, Constants.arrBookName[i]);
//        	}
        	bookMap.put(NAME, Constants.arrActiveBookName[i]);
        }
        
        Map<String, String> groupNT = new HashMap<String, String>();
        groupData.add(groupNT);
        groupNT.put(NAME, "New Testament");
        List<Map<String, String>> ntChild = new ArrayList<Map<String, String>>();
        childData.add(ntChild);
        for (int i=39; i<66; i++) {
        	Map<String, String> bookMap = new HashMap<String, String>();
        	ntChild.add(bookMap);
//        	if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//        		bookMap.put(NAME, Constants.arrBookNameIndo[i]);
//        	} else {
//        		bookMap.put(NAME, Constants.arrBookName[i]);
//        	}
        	bookMap.put(NAME, Constants.arrActiveBookName[i]);
        }
        
        ExpandableListAdapter result = new SimpleExpandableListAdapter(
            this,
            groupData,
            android.R.layout.simple_expandable_list_item_1,
            new String[] { NAME},
            new int[] { android.R.id.text1 },
            childData,
            R.layout.listitemmedium,
            new String[] { NAME},
            new int[] { android.R.id.text1 }
            );
 
    	return result;
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		int bookSelected;
		if (groupPosition == 0) {
			bookSelected = childPosition + 1;
		} else {
			bookSelected = childPosition + 40;
		}
		
		int numberOfChapter = 0;
		//check how many chapter
		if (bookSelected < 66) {
			int thisChapterStart = Constants.arrBookStart[bookSelected-1];
			int nextChapterStart = Constants.arrBookStart[bookSelected];
			numberOfChapter = nextChapterStart-thisChapterStart;
	    	if (numberOfChapter == 1) {
	        	int chapterIndex = (Constants.arrBookStart[bookSelected-1]);
	        	Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
	    		editor.putInt(Constants.POSITION_CHAPTER, chapterIndex);
	    		editor.commit();
	    		finish();
	    		return true;
	    	} 
		} else {
			//Revelation has 22 chapter
			numberOfChapter = 22;
		}
		
		String bookName;
//    	if (currentBookLanguage.equals(Constants.LANG_BAHASA)) {
//    		bookName =  Constants.arrBookNameIndo[bookSelected - 1];
//    	} else {
//    		bookName =  Constants.arrBookName[bookSelected - 1];
//    	}
		bookName =  Constants.arrActiveBookName[bookSelected - 1];

		Intent browseChapter = new Intent(this, BrowseChapter.class);
		browseChapter.putExtra("bookNumber", bookSelected);
		browseChapter.putExtra("bookName", bookName);
		browseChapter.putExtra("numberOfChapter", numberOfChapter);
    	startActivityForResult(browseChapter, BROWSE_CHAPTER);
    	return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BROWSE_CHAPTER && resultCode == Activity.RESULT_OK) {
			finish();
		}
	}
}
