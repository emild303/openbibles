package com.konibee.bible;

public class DisplayDownload {
	private String language;
	private String fileName;
	private String name;
	private String description;
	
	public DisplayDownload(String language, String fileName, String name, String description) {
		this.language = language;
		this.fileName = fileName;
		this.name = name;
		this.description = description;
	}
	
	public DisplayDownload(String fileName, String name, String description) {
		this.fileName = fileName;
		this.name = name;
		this.description = description;
	}
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	
	
	
	
}
