package com.konibee.bible;

public class Bookmark {
	private Long id;
	private Long categoryId;
	private Integer book;
	private Integer chapter;
	private Integer verseStart;
	private Integer verseEnd;
	private String content;
	private String bible;
	private String bookmarkDate;
	
	//not persist
	private String categoryName;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getCategoryId() {
		return categoryId;
	}
	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}
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
	public Integer getVerseStart() {
		return verseStart;
	}
	public void setVerseStart(Integer verseStart) {
		this.verseStart = verseStart;
	}
	public Integer getVerseEnd() {
		return verseEnd;
	}
	public void setVerseEnd(Integer verseEnd) {
		this.verseEnd = verseEnd;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getBible() {
		return bible;
	}
	public void setBible(String bible) {
		this.bible = bible;
	}
	public String getBookmarkDate() {
		return bookmarkDate;
	}
	public void setBookmarkDate(String bookmarkDate) {
		this.bookmarkDate = bookmarkDate;
	}
	public String getCategoryName() {
		return categoryName;
	}
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((book == null) ? 0 : book.hashCode());
		result = prime * result + ((chapter == null) ? 0 : chapter.hashCode());
		result = prime * result
				+ ((verseStart == null) ? 0 : verseStart.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bookmark other = (Bookmark) obj;
		if (book == null) {
			if (other.book != null)
				return false;
		} else if (!book.equals(other.book))
			return false;
		if (chapter == null) {
			if (other.chapter != null)
				return false;
		} else if (!chapter.equals(other.chapter))
			return false;
		if (verseStart == null) {
			if (other.verseStart != null)
				return false;
		} else if (!verseStart.equals(other.verseStart))
			return false;
		return true;
	}
	
	
}
