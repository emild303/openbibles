package com.konibee.bible;

public class BibleVersion {
	private Long id;
	private String fileName;
	private Long lastModified;
	private String bibleName;
	private Integer eolLength;
	private String about;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Long getLastModified() {
		return lastModified;
	}
	public void setLastModified(Long lastModified) {
		this.lastModified = lastModified;
	}
	public String getBibleName() {
		return bibleName;
	}
	public void setBibleName(String bibleName) {
		this.bibleName = bibleName;
	}
	public Integer getEolLength() {
		return eolLength;
	}
	public void setEolLength(Integer eolLength) {
		this.eolLength = eolLength;
	}
	public String getAbout() {
		return about;
	}
	public void setAbout(String about) {
		this.about = about;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
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
		BibleVersion other = (BibleVersion) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		return true;
	}
	
	
	
	
}
