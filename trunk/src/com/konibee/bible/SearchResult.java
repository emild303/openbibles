package com.konibee.bible;

public class SearchResult {
	private Integer book;
	private Integer chapter;
	private Integer verse;
	private String content;

	public Integer getBook() {
		return book;
	}
	public void setBook(Integer book) {
		this.book = book;
	}
	public Integer getChapter() {
		return chapter;
	}
	public void setChapter(Integer chapter) {
		this.chapter = chapter;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public Integer getVerse() {
		return verse;
	}
	public void setVerse(Integer verse) {
		this.verse = verse;
	}
	
}
