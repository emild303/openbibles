package com.konibee.bible;

public class DisplayVerse {
	private int chapterIndex;
	private int verseNumber;
	private String verse;	
	private boolean bookmark;
	private boolean insertLineBreak;
	
	public DisplayVerse(int verseNumber, String verse, boolean bookmark, boolean insertLineBreak) {
		this.verseNumber = verseNumber;
		this.verse = verse;
		this.bookmark = bookmark;
		this.insertLineBreak = insertLineBreak;
	}
	
	public DisplayVerse(int verseNumber, String verse, boolean bookmark, boolean insertLineBreak, int chapterIndex) {
		this.verseNumber = verseNumber;
		this.verse = verse;
		this.bookmark = bookmark;
		this.insertLineBreak = insertLineBreak;
		this.chapterIndex = chapterIndex;
	}
	
	public int getVerseNumber() {
		return verseNumber;
	}
	public void setVerseNumber(int verseNumber) {
		this.verseNumber = verseNumber;
	}
	public String getVerse() {
		return verse;
	}
	public void setVerse(String verse) {
		this.verse = verse;
	}
	public boolean isBookmark() {
		return bookmark;
	}
	public void setBookmark(boolean bookmark) {
		this.bookmark = bookmark;
	}
	public boolean isInsertLineBreak() {
		return insertLineBreak;
	}

	public void setInsertLineBreak(boolean insertLineBreak) {
		this.insertLineBreak = insertLineBreak;
	}

	public int getChapterIndex() {
		return chapterIndex;
	}

	public void setChapterIndex(int chapterIndex) {
		this.chapterIndex = chapterIndex;
	}
	
	
	
	
}
