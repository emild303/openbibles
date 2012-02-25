package com.konibee.bible;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Categories extends ListActivity implements OnClickListener, android.content.DialogInterface.OnClickListener {
	private DatabaseHelper databaseHelper;
	private String[] arrCategories;
	
	private View addCategoryView;
	private AlertDialog addCategoryDialog;
	
	private View renameCategoryView;
	private AlertDialog renameCategoryDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categories);
		databaseHelper = new DatabaseHelper(this);
		databaseHelper.open();
		
		arrCategories = databaseHelper.getCategoryNames();
		setListAdapter(new ArrayAdapter<String>(this,
			R.layout.listitemmedium, arrCategories));
		Button btnAddCategory = (Button) findViewById(R.id.btnAddCategory);
		btnAddCategory.setOnClickListener(this);
		
		LayoutInflater li = LayoutInflater.from(this);
		addCategoryView = li.inflate(R.layout.addcategory, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.addCategory);
		builder.setView(addCategoryView);
		builder.setPositiveButton("OK", this);
		builder.setNegativeButton("Cancel", this);
		addCategoryDialog = builder.create();
		
		renameCategoryView = li.inflate(R.layout.addcategory, null);
		builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.renameCategory);
		builder.setView(renameCategoryView);
		builder.setPositiveButton("OK", this);
		builder.setNegativeButton("Cancel", this);
		renameCategoryDialog = builder.create();
		
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		databaseHelper.open();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//close database
		databaseHelper.close();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnAddCategory:
				EditText edtCategory = (EditText) addCategoryView.findViewById(R.id.edtCategory);
				edtCategory.setText("");
				addCategoryDialog.show();				
				break;
		}
		
		
	}

	@Override
	public void onClick(DialogInterface dialog, int buttonId) {
		if (dialog.equals(addCategoryDialog)) { //add category
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				EditText edtCategory = (EditText) addCategoryView.findViewById(R.id.edtCategory);
				if (edtCategory.getText().toString().trim().equals("")) return;
				Long id = databaseHelper.getCategoryIdByCategoryName(edtCategory.getText().toString());
				if (id == null) {
					databaseHelper.insertCategory(edtCategory.getText().toString().trim());
					arrCategories = databaseHelper.getCategoryNames();
					setListAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, arrCategories));
					Toast.makeText(this, R.string.categoryAdded, Toast.LENGTH_LONG).show();
					scrollToCategoryName(edtCategory.getText().toString());
				} else {
					Toast.makeText(this, R.string.categoryExist, Toast.LENGTH_LONG).show();
				}
			}
		} else if (dialog.equals(renameCategoryDialog)) { //edit category
			if (buttonId == DialogInterface.BUTTON_POSITIVE) {
				int index = (Integer) renameCategoryView.getTag();
				EditText edtCategory = (EditText) renameCategoryView.findViewById(R.id.edtCategory);
				String newCategoryName = edtCategory.getText().toString().trim();
				String oldCategoryName = arrCategories[index];
				if (newCategoryName.equals("")) return;
				if (newCategoryName.equals(oldCategoryName)) return;
				Long id = databaseHelper.getCategoryIdByCategoryName(oldCategoryName);
				
				if (id == null) return;
				Long checkExists = databaseHelper.getCategoryIdByCategoryName(newCategoryName);
				if (checkExists == null) {
					databaseHelper.updateCategory(newCategoryName, id);
					arrCategories = databaseHelper.getCategoryNames();
					setListAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, arrCategories));
					Toast.makeText(this, R.string.categoryRenamed, Toast.LENGTH_LONG).show();
					scrollToCategoryName(edtCategory.getText().toString());
				} else {
					Toast.makeText(this, R.string.categoryExist, Toast.LENGTH_LONG).show();
				}
			}
		}
	}
	
	private void scrollToCategoryName(String categoryName) {
		for (int i = 0; i < arrCategories.length; i++) {
			final int j = i;
			if (arrCategories[i].equals(categoryName)) {
				getListView().post(new Runnable() {
					@Override
					public void run() {
						getListView().setSelection(j);
					}
				});
				return;
			}
		}
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.renameCategory);
		if (arrCategories.length > 1) {
			menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, R.string.removeCategory);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
	    final int index = info.position;
	    
		switch (item.getItemId()) {
		case Menu.FIRST : //rename category
			EditText edtCategory = (EditText) renameCategoryView.findViewById(R.id.edtCategory);
			edtCategory.setText(arrCategories[index]);
			renameCategoryView.setTag(index);
			renameCategoryDialog.show();
			return true;		
		case Menu.FIRST+1 : //remove category
			final String categoryName = arrCategories[index];
			new AlertDialog.Builder(this)
		        .setTitle(R.string.removeCategory)
		        .setMessage(R.string.reallyRemoveCategory)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                databaseHelper.removeCategory(categoryName);
		                arrCategories = databaseHelper.getCategoryNames();
						setListAdapter(new ArrayAdapter<String>(Categories.this,
							android.R.layout.simple_list_item_1, arrCategories));
						Toast.makeText(Categories.this, R.string.categoryRemoved, Toast.LENGTH_LONG).show();
						if (index < arrCategories.length) {
							getListView().post(new Runnable() {
								@Override
								public void run() {
									getListView().setSelection(index);
								}
							});
						} else {
							getListView().post(new Runnable() {
								@Override
								public void run() {
									getListView().setSelection(arrCategories.length-1);
								}
							});
						}
						
		            }
	
		        })
		        .setNegativeButton(R.string.no, null)
		        .show();
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.categories_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {		
			case R.id.clearAllCategory:
				new AlertDialog.Builder(this)
		        .setTitle(R.string.removeAllCategory)
		        .setMessage(R.string.reallyRemoveAllCategory)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                databaseHelper.removeAllCategoryAndBookmark();
		                arrCategories = databaseHelper.getCategoryNames();
						setListAdapter(new ArrayAdapter<String>(Categories.this,
							android.R.layout.simple_list_item_1, arrCategories));
						Toast.makeText(Categories.this, R.string.categoryAllRemoved, Toast.LENGTH_LONG).show();
		            }
		        })
		        .setNegativeButton(R.string.no, null)
		        .show();
				return true;
			case R.id.help:
				SharedPreferences preference = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
				int currentFontSize = preference.getInt(Constants.FONT_SIZE, 14);
				Intent iHelp = new Intent(this, Help.class);
				iHelp.putExtra(Constants.FONT_SIZE, currentFontSize);
				iHelp.putExtra(Constants.HELP_CONTENT, R.string.help_category);
				startActivity(iHelp);
				return true;
		}
		return false;
	}
	
}
