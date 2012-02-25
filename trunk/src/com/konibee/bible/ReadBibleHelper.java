package com.konibee.bible;

public class ReadBibleHelper {
	private int chapterIndex;
	private int verseStart;
	private int verseEnd;
	
	public ReadBibleHelper(int chapterIndex, int verseStart, int verseEnd) {
		this.chapterIndex = chapterIndex;
		this.verseStart = verseStart;
		this.verseEnd = verseEnd;
	}
	
	public int getChapterIndex() {
		return chapterIndex;
	}
	public void setChapterIndex(int chapterIndex) {
		this.chapterIndex = chapterIndex;
	}
	public int getVerseStart() {
		return verseStart;
	}
	public void setVerseStart(int verseStart) {
		this.verseStart = verseStart;
	}
	public int getVerseEnd() {
		return verseEnd;
	}
	public void setVerseEnd(int verseEnd) {
		this.verseEnd = verseEnd;
	}
	
}
