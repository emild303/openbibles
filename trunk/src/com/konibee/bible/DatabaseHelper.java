package com.konibee.bible;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class DatabaseHelper {
	private SQLiteDatabase db;
	private OpenHelper openHelper;
	private boolean isDbOpen;
	
	private static final String TAG = "DatabaseHelper";
	private static final String DEFAULT_CATEGORY = "Favorite";

	public DatabaseHelper(Context context) {
		this.openHelper = new OpenHelper(context);
	}

	public void close() {		
		try {
			db.close();
		} finally {
			isDbOpen = false;
		}
	}

	public void open() throws SQLiteException {		
		if (!isDbOpen || (db!=null && !db.isOpen())) {
			try {
				db = openHelper.getWritableDatabase();
			} catch (SQLiteException ex) {
				db = openHelper.getReadableDatabase();
			}
			isDbOpen = true;
		}
	}
	
	public void saveOrUpdateBibleVersion(BibleVersion r) {
		if (r.getId() == -1) {
			String insertSql = "INSERT INTO bible_version(file_name, last_modified, bible_name, eol_length, about) values (?,?,?,?,?)";
			db.execSQL(insertSql, new Object[] {r.getFileName(), r.getLastModified(), r.getBibleName(), r.getEolLength(), r.getAbout()});
			String selectId = "SELECT last_insert_rowid()";
			Cursor c = db.rawQuery(selectId, null);
			if (c.moveToNext()) {
				r.setId(c.getLong(0));
			}
			c.close();
		} else {
			String insertSql = "UPDATE bible_version set last_modified=?, bible_name=?, eol_length=?, about=? where id=?";
			db.execSQL(insertSql, new Object[] {r.getLastModified(), r.getBibleName(), r.getEolLength(), r.getAbout(), r.getId()});
		}
	}
	
	public BibleVersion getBibleVersionByFileName(String fileName) {
		Cursor cursor = db.rawQuery("SELECT id, file_name, last_modified, bible_name, eol_length FROM bible_version WHERE file_name=?", new String[] {fileName});
		BibleVersion result = null;
		while (cursor.moveToNext()) {
			result = new BibleVersion();
			result.setId(cursor.getLong(0));
			result.setFileName(cursor.getString(1));
			result.setLastModified(cursor.getLong(2));
			result.setBibleName(cursor.getString(3));
			result.setEolLength(cursor.getInt(4));
		}
		cursor.close();
		return result;
	}
	
	public BibleVersion getBibleVersionByBibleName(String bibleName) {
		Cursor cursor = db.rawQuery("SELECT id, file_name, last_modified, bible_name, eol_length FROM bible_version WHERE bible_name=?", new String[] {bibleName});
		BibleVersion result = null;
		while (cursor.moveToNext()) {
			result = new BibleVersion();
			result.setId(cursor.getLong(0));
			result.setFileName(cursor.getString(1));
			result.setLastModified(cursor.getLong(2));
			result.setBibleName(cursor.getString(3));
			result.setEolLength(cursor.getInt(4));
		}
		cursor.close();
		return result;
	}
	
	public String getAboutByBibleName(String bibleName) {
		Cursor cursor = db.rawQuery("SELECT about FROM bible_version WHERE bible_name=?", new String[] {bibleName});
		String result = null;
		while (cursor.moveToNext()) {
			result = cursor.getString(0);
		}
		cursor.close();
		return result;
	}
	
	public List<BibleVersion> getAllBibleVersion() {
		Cursor cursor = db.rawQuery("SELECT id, file_name, last_modified, bible_name, eol_length FROM bible_version", null);
		List<BibleVersion> result = new ArrayList<BibleVersion>();
		while (cursor.moveToNext()) {
			BibleVersion row = new BibleVersion();
			row.setId(cursor.getLong(0));
			row.setFileName(cursor.getString(1));
			row.setLastModified(cursor.getLong(2));
			row.setBibleName(cursor.getString(3));
			row.setEolLength(cursor.getInt(4));
			result.add(row);
		}
		cursor.close();
		return result;
	}
	
	public List<String> getBibleNameList() {
		Cursor cursor = db.rawQuery("SELECT bible_name FROM bible_version ORDER BY bible_name", null);
		List<String> result = new ArrayList<String>();
		while (cursor.moveToNext()) {
			result.add(cursor.getString(0));
		}
		cursor.close();
		return result;
	}
	
	public void getBibleTranslationList(List<String> bibleNameList, List<String> fileNameList) {
		if (bibleNameList == null || fileNameList == null) return;
		bibleNameList.clear();
		fileNameList.clear();
		Cursor cursor = db.rawQuery("SELECT bible_name, file_name FROM bible_version ORDER BY bible_name", null);
		while (cursor.moveToNext()) {
			bibleNameList.add(cursor.getString(0));
			fileNameList.add(cursor.getString(1));
		}
		cursor.close();
	}
	
	public List<String> getBookmarkCategoryList() {
		Cursor cursor = db.rawQuery("SELECT category_name FROM category ORDER BY category_name", null);
		List<String> result = new ArrayList<String>();
		while (cursor.moveToNext()) {
			result.add(cursor.getString(0));
		}
		cursor.close();
		return result;
	}

	private static class OpenHelper extends SQLiteOpenHelper {		
		private final String TAG = OpenHelper.class.getName();
		
		public OpenHelper(Context context) {
			super(context, "twbible.db", null, 6);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "Create database");
			db.execSQL("CREATE TABLE bible_version(id INTEGER PRIMARY KEY, file_name TEXT, last_modified INTEGER, bible_name TEXT, about TEXT, eol_length INTEGER);");
			db.execSQL("CREATE UNIQUE INDEX idx_bible_version_1 ON bible_version(file_name);");
			db.execSQL("CREATE TABLE category(id INTEGER PRIMARY KEY, category_name TEXT);");
			db.execSQL("CREATE UNIQUE INDEX idx_category_1 ON category(category_name);");
			db.execSQL("CREATE TABLE bookmark(id INTEGER PRIMARY KEY, category_id INTEGER, book INTEGER, chapter INTEGER, verse_start INTEGER, verse_end INTEGER, content TEXT, bible TEXT, bookmark_date TEXT);");
			db.execSQL("CREATE INDEX idx_bookmark_1 ON bookmark(category_id);");
			db.execSQL("CREATE INDEX idx_bookmark_2 ON bookmark(book, chapter, verse_start);");
			db.execSQL("CREATE TABLE read_history(id INTEGER PRIMARY KEY, chapter_index INTEGER);");
			db.execSQL("INSERT INTO CATEGORY(category_name) values ('" + DEFAULT_CATEGORY + "');");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "Upgrade database from " + oldVersion + " to " + newVersion);
			db.execSQL("DROP TABLE IF EXISTS bible_version");
			onCreate(db);
		}

	}


	public void deleteInvalidBible(StringBuffer fileNames) {
		if (fileNames.length() == 0) {
			db.execSQL("DELETE FROM bible_version");
		} else {
			fileNames.delete(0, 1);
			db.execSQL("DELETE from bible_version where file_name not in (" + fileNames.toString() + ")");
		}
	}
	
	public Long insertCategory(String categoryName) {
		String insertSql = "INSERT INTO category(category_name) values (?)";
		db.execSQL(insertSql, new Object[] {categoryName});
		String selectId = "SELECT last_insert_rowid()";
		Cursor c = db.rawQuery(selectId, null);
		Long result = null; 
		if (c.moveToNext()) {
			result = c.getLong(0);
		}
		c.close();
		return result;
	}
	
	public void clearAllBookmarks() {
		db.execSQL("DROP TABLE IF EXISTS bookmark");
		db.execSQL("DROP TABLE IF EXISTS category");
		db.execSQL("CREATE TABLE category(id INTEGER PRIMARY KEY, category_name TEXT);");
		db.execSQL("CREATE TABLE bookmark(id INTEGER PRIMARY KEY, category_id INTEGER, book INTEGER, chapter INTEGER, verse_start INTEGER, verse_end INTEGER, content TEXT, bible TEXT, bookmark_date TEXT);");
	}
	
	public void createBookmarkIndexes() {
		db.execSQL("CREATE UNIQUE INDEX idx_category_1 ON category(category_name);");
		db.execSQL("CREATE INDEX idx_bookmark_1 ON bookmark(category_id);");
		db.execSQL("CREATE INDEX idx_bookmark_2 ON bookmark(book, chapter, verse_start);");
	}

	
	public void insertBookmark(Bookmark bm) {
		String insertSql = "INSERT INTO bookmark(category_id, book, chapter, verse_start, verse_end, content, bible, bookmark_date)" +
			" values (?,?,?,?,?,?,?,?);";
		db.execSQL(insertSql, new Object[] {bm.getCategoryId(), bm.getBook(), bm.getChapter(), bm.getVerseStart(), bm.getVerseEnd(), 
			bm.getContent(), bm.getBible(), bm.getBookmarkDate()});
	}
	
	public void getBookmarkList(List<Bookmark> result, String categoryName, String sortBy, String bible) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT b.id, b.category_id, b.book, b.chapter, b.verse_start, b.verse_end, b.content, b.bible, b.bookmark_date")
		  .append(" FROM bookmark b")
		  .append(" INNER JOIN category c on c.id=b.category_id")
		  .append(" WHERE c.category_name=?")
		  .append(" ORDER BY book, chapter, verse_start");
		
		String bibleName = "";
		int prevOffset = 0;
		int prevChapterIdx = -1;
		DataInputStream is = null;
		BufferedReader br = null;
		int eolLength = 0;
		if (!bible.equals(Constants.SHOW_BIBLE_AS_BOOKMARKED)) {
			BibleVersion bv = getBibleVersionByBibleName(bible);
			eolLength = bv.getEolLength();
			File sdcard = Environment.getExternalStorageDirectory();
			File file = new File(sdcard, Constants.BIBLE_FOLDER + "/" + bv.getFileName());
			String indexFileName = file.getAbsolutePath().replaceAll(".ont", ".idx");
			File fIndex = new File(indexFileName);
			bibleName = file.getName().replaceAll(".ont", "").toLowerCase();

			try {
				is = new DataInputStream(new FileInputStream(fIndex));
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), 8192);
			} catch (Exception e) {
				e.printStackTrace();
				bible = Constants.SHOW_BIBLE_AS_BOOKMARKED;
			}
		}
		  
		StringBuffer sbVerse = new StringBuffer();
		int skipLength = 0;
		int startOffset = 0;
		int prevVerse = 1;
		Cursor cursor = db.rawQuery(sb.toString(), new String[] {categoryName});
		result.clear();
		while (cursor.moveToNext()) {
			Bookmark bm = new Bookmark();
			bm.setId(cursor.getLong(0));
			bm.setCategoryId(cursor.getLong(1));
			bm.setBook(cursor.getInt(2));
			bm.setChapter(cursor.getInt(3));
			bm.setVerseStart(cursor.getInt(4));
			bm.setVerseEnd(cursor.getInt(5));
			if (!bible.equals(Constants.SHOW_BIBLE_AS_BOOKMARKED)) {
				bm.setBible(bibleName);
				int chapterIdx = Constants.arrBookStart[bm.getBook()-1] + bm.getChapter()-1;
				int chapterIdxDiff = chapterIdx - prevChapterIdx;
				try {
					if (chapterIdxDiff > 0) {
						prevChapterIdx = chapterIdx;	
						is.skip((chapterIdxDiff-1)*4);
						startOffset = is.readInt();
						int offsetDiff = startOffset - prevOffset - skipLength;
						br.skip(offsetDiff);
						prevOffset = startOffset;
						prevVerse = 1;
						skipLength = 0;
					}
					
					while (prevVerse < bm.getVerseStart()) {
						prevVerse++;
						String line = br.readLine();
						skipLength = skipLength + line.length() + eolLength;
					}
					
					sbVerse.delete(0, sbVerse.length());
					while (prevVerse <= bm.getVerseEnd()) {
						prevVerse++;
						String line = br.readLine();
						sbVerse.append(line).append(" ");
						skipLength = skipLength + line.length() + eolLength;
					}
					if (sbVerse.length() > 0) {
						sbVerse.delete(sbVerse.length()-1, sbVerse.length());
					}
					bm.setContent(Util.parseVerse(sbVerse.toString()));
				} catch (Exception e) {
					Log.d(TAG, "Error reading bookmark bible file");
					e.printStackTrace();
					bible = Constants.SHOW_BIBLE_AS_BOOKMARKED;
				}
				
			} else {
				bm.setContent(cursor.getString(6));
				bm.setBible(cursor.getString(7));
			}
			bm.setBookmarkDate(cursor.getString(8));
			result.add(bm);
		}
		cursor.close();
		
		if (is != null) {
			try {
				is.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if (br != null) {
			try {
				br.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		if (sortBy.equals(Constants.SORT_DATE_ASC)) {
			Collections.sort(result, new BookmarkDateAscComparator());
		} else if (sortBy.equals(Constants.SORT_DATE_DESC)) {
			Collections.sort(result, new BookmarkDateDescComparator());
		}
	}
	
	public List<Integer> getBookmarkVerseStartByChapterIndex(int chapterIndex) {
		List<Integer> result = new ArrayList<Integer>();
		String[] arrBookChapter = Constants.arrVerseCount[chapterIndex].split(";");
		
		Cursor cursor = db.rawQuery("SELECT b.verse_start FROM bookmark b" +
			" WHERE b.book=? and b.chapter=?" +
			" ORDER BY b.verse_start", new String[] {arrBookChapter[0], arrBookChapter[1]});
		result.clear();
		while (cursor.moveToNext()) {
			result.add(cursor.getInt(0));
		}
		cursor.close();
		return result;
	}
	
	public Long getCategoryIdByCategoryName(String name) {
		Cursor cursor = db.rawQuery("SELECT id FROM category WHERE category_name=?", new String[] {name});
		Long result = null;
		while (cursor.moveToNext()) {
			result = cursor.getLong(0);
		}
		cursor.close();
		return result;
	}
	
	public String[] getCategoryNames() {
		Cursor cursor = db.rawQuery("SELECT category_name FROM category ORDER BY category_name", null);
		List<String> nameList = new ArrayList<String>();
		while (cursor.moveToNext()) {
			nameList.add(cursor.getString(0));
		}
		cursor.close();
		String[] result = new String[nameList.size()];
		result = nameList.toArray(result);
		return result;
	}

	public void removeBookmark(int book, int chapter, int verseNumber) {
		db.execSQL("DELETE FROM bookmark WHERE book=? AND chapter=? AND verse_start=?", 
			new String[] {String.valueOf(book), String.valueOf(chapter), String.valueOf(verseNumber)});
	}
	
	public void removeCategory(String categoryName) {
		db.execSQL("DELETE FROM category WHERE category_name=?", 
			new String[] {categoryName});
		
	}

	public void updateCategory(String categoryName, Long id) {
		db.execSQL("UPDATE CATEGORY SET category_name=? where id=?", new String[]{categoryName, id.toString()});
	}

	public void insertReplaceBookmark(Bookmark bm) {
		String deleteSql = ("DELETE FROM bookmark WHERE book=? AND chapter=? AND verse_start=?");
		db.execSQL(deleteSql, new Object[] {bm.getBook(), bm.getChapter(), bm.getVerseStart()});
		insertBookmark(bm);
	}
	
	public Bookmark getBookmark(int book, int chapter, int verseStart) {
		Cursor cursor = db.rawQuery("SELECT b.id, b.category_id, b.book, b.chapter, b.verse_start, b.verse_end, b.content, b.bible, b.bookmark_date, " +
			" c.category_name" +
			" FROM bookmark b" +
			" INNER JOIN category c on c.id=b.category_id" +
			" WHERE b.book=? and b.chapter=? and b.verse_start=?" , new String[] {String.valueOf(book), String.valueOf(chapter), String.valueOf(verseStart)});
		Bookmark result = null;
		while (cursor.moveToNext()) {
			result = new Bookmark();
			result.setId(cursor.getLong(0));
			result.setCategoryId(cursor.getLong(1));
			result.setBook(cursor.getInt(2));
			result.setChapter(cursor.getInt(3));
			result.setVerseStart(cursor.getInt(4));
			result.setVerseEnd(cursor.getInt(5));
			result.setContent(cursor.getString(6));
			result.setBible(cursor.getString(7));
			result.setBookmarkDate(cursor.getString(8));
			result.setCategoryName(cursor.getString(9));
		}
		cursor.close();
		return result;
	}
	
	

	public void removeAllCategoryAndBookmark() {
		clearAllBookmarks();
		createBookmarkIndexes();
		db.execSQL("INSERT INTO CATEGORY(category_name) values ('" + DEFAULT_CATEGORY + "');");		
	}
	
	public List<Bookmark> getAllBookmarksForImport() {
		List<Bookmark> result = new ArrayList<Bookmark>();
		Cursor cursor = db.rawQuery("SELECT b.book, b.chapter, b.verse_start FROM bookmark b", null);
		while (cursor.moveToNext()) {
			Bookmark bm = new Bookmark();
			bm.setBook(cursor.getInt(0));
			bm.setChapter(cursor.getInt(1));
			bm.setVerseStart(cursor.getInt(2));
			result.add(bm);
		}
		cursor.close();
		return result;
	}
	
	public List<Bookmark> getAllBookmarksForExport() {
		Cursor cursor = db.rawQuery("SELECT b.id, b.category_id, b.book, b.chapter, b.verse_start, b.verse_end, b.content, b.bible, b.bookmark_date, " +
				" c.category_name" +
				" FROM bookmark b" +
				" INNER JOIN category c on c.id=b.category_id" +
				" ORDER BY b.category_id, b.book, b.chapter, b.verse_start", null);
		List<Bookmark> result = new ArrayList<Bookmark>();
		while (cursor.moveToNext()) {
			Bookmark bm = new Bookmark();
			bm.setId(cursor.getLong(0));
			bm.setCategoryId(cursor.getLong(1));
			bm.setBook(cursor.getInt(2));
			bm.setChapter(cursor.getInt(3));
			bm.setVerseStart(cursor.getInt(4));
			bm.setVerseEnd(cursor.getInt(5));
			bm.setContent(cursor.getString(6));
			bm.setBible(cursor.getString(7));
			bm.setBookmarkDate(cursor.getString(8));
			bm.setCategoryName(cursor.getString(9));
			result.add(bm);
		}
		cursor.close();
		return result;
	}

	public Map<String, Long> getCategoryMap() {
		Map<String, Long> result = new HashMap<String, Long>();
		Cursor cursor = db.rawQuery("SELECT id, category_name FROM category", null);
		while (cursor.moveToNext()) {
			Long categoryId = cursor.getLong(0);
			String categoryName = cursor.getString(1);
			result.put(categoryName, categoryId);
		}
		cursor.close();
		return result;
	}
	
	public void getHistory(List<Integer> result) {
		result.clear();
		Cursor cursor = db.rawQuery("SELECT chapter_index FROM read_history ORDER BY id", null);
		while (cursor.moveToNext()) {
			result.add(cursor.getInt(0));
		}
		cursor.close();
		if (result.size() == 0) {
			result.add(0);
		}
	}
	
	public void saveHistory(List<Integer> historyList) {
		db.execSQL("DELETE FROM read_history");
		for (int i = 0; i < historyList.size(); i++) {
			db.execSQL("INSERT INTO read_history(chapter_index) values (" + historyList.get(i) + ")");
		}
	}

	public void removeAllBookmarks(String categoryName) {
		Long categoryId = getCategoryIdByCategoryName(categoryName);
		db.execSQL("DELETE FROM bookmark WHERE category_id=" + categoryId);
	}

	public void moveBookmark(String newCategoryName, Long id) {
		Long categoryId = getCategoryIdByCategoryName(newCategoryName);
		db.execSQL("UPDATE bookmark set category_id=? where id=?", new String[] {categoryId.toString(), id.toString()});
	}
	
	public Bookmark getRandomBookmark() {
		Cursor cursor = db.rawQuery("SELECT b.id, b.category_id, b.book, b.chapter, b.verse_start, b.verse_end, b.content, b.bible, b.bookmark_date "+			
			" FROM bookmark b" +
			" ORDER BY RANDOM() limit 1", null);
		Bookmark result = null;
		while (cursor.moveToNext()) {
			result = new Bookmark();
			result.setId(cursor.getLong(0));
			result.setCategoryId(cursor.getLong(1));
			result.setBook(cursor.getInt(2));
			result.setChapter(cursor.getInt(3));
			result.setVerseStart(cursor.getInt(4));
			result.setVerseEnd(cursor.getInt(5));
			result.setContent(cursor.getString(6));
			result.setBible(cursor.getString(7));
			result.setBookmarkDate(cursor.getString(8));
		}
		cursor.close();
		return result;
	}
}
