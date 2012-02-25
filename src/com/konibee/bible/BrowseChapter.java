package com.konibee.bible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

public class BrowseChapter extends ExpandableListActivity {
	private int bookNumber;
	private String bookName;
	private int numberOfChapter;

	private static final String GROUP = "GROUP";
	private static final String CHILD = "CHILD";

	private ExpandableListAdapter adapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browse);

        bookNumber = getIntent().getIntExtra("bookNumber", 1);
        bookName = getIntent().getStringExtra("bookName");
        numberOfChapter = getIntent().getIntExtra("numberOfChapter", 1);

        adapter = createAdapter();
        setListAdapter(adapter);

        if (adapter.getGroupCount() == 1) {
        	 getExpandableListView().expandGroup(0);
        }
}
    
    protected ExpandableListAdapter createAdapter()
    {
		int numGroups = numberOfChapter % 10 == 0 ? numberOfChapter / 10 : numberOfChapter / 10 + 1;
		
        List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
        
        for (int groupNo = 0; groupNo < numGroups; groupNo++) {
            Map<String, String> curGroupMap = new HashMap<String, String>();
            groupData.add(curGroupMap);
            
            StringBuffer groupName = new StringBuffer();
            int lastChapter = 0;
            if (groupNo + 1 == numGroups) {
            	lastChapter = numberOfChapter;
            } else {
            	lastChapter = 10 * (groupNo + 1);
            }
            groupName.append(bookName).append(" ").append(10*groupNo+1).append(" - ").append(lastChapter);
            curGroupMap.put(GROUP, groupName.toString());
            
            // add all chapters in current group to list
            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
            for (int chapter=groupNo*10 + 1; chapter<=lastChapter; chapter++) {
                Map<String, String> childMap = new HashMap<String, String>();
                children.add(childMap);
                childMap.put(CHILD, bookName + " " + chapter);
            }
            childData.add(children);
        }

        // Set up our adapter
        ExpandableListAdapter result = new SimpleExpandableListAdapter(
                this,
                groupData,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { GROUP },
                new int[] { android.R.id.text1 },
                childData,
                R.layout.listitemmedium,
                new String[] { CHILD },
                new int[] { android.R.id.text1 }
                );
                
    	return result;
    }
    
    @Override
    public void onGroupExpand(int groupPosition) {
    	getExpandableListView().setSelectedChild(groupPosition, 0, true);
    }

    @Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    	int chapter = groupPosition * 10 + childPosition + 1;
    	int chapterIndex = (Constants.arrBookStart[bookNumber-1]) + (chapter - 1);
    	Editor editor = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE).edit();
		editor.putInt(Constants.POSITION_CHAPTER, chapterIndex);
		editor.commit();
		Intent resultIntent = new Intent(this, BrowseBook.class);
    	setResult(Activity.RESULT_OK, resultIntent);
    	finish(); 
    	return true;
	}


    
}
