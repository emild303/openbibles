package com.konibee.bible;

public class Util {
	public static String parseVerse(String verse) {
		StringBuffer sbVerse = new StringBuffer(verse);
		while (sbVerse.indexOf("<CM>") > -1) {
			int posDelete = sbVerse.indexOf("<CM>");
			sbVerse.delete(posDelete, posDelete + 4);
		}
		while (sbVerse.indexOf("<CL>") > -1) {
			int posDelete = sbVerse.indexOf("<CL>");
			sbVerse.delete(posDelete, posDelete + 4);
			sbVerse.insert(posDelete, " ");
		}
		while (sbVerse.indexOf("<FR>") > -1) {
			int posDelete = sbVerse.indexOf("<FR>");
			sbVerse.delete(posDelete, posDelete + 4);
		}
		while (sbVerse.indexOf("<Fr>") > -1) {
			int posDelete = sbVerse.indexOf("<Fr>");
			sbVerse.delete(posDelete, posDelete + 4);
		}
		while (sbVerse.indexOf("<RF>") > -1) {
			int posDelete = sbVerse.indexOf("<RF>");
			int posEndDelete = sbVerse.indexOf("<Rf>", posDelete);
			sbVerse.delete(posDelete, posEndDelete + 4);
		}
		while (sbVerse.indexOf("<TS>") > -1) {
			int posDelete = sbVerse.indexOf("<TS>");
			int posEndDelete = sbVerse.indexOf("<Ts>", posDelete);
			sbVerse.delete(posDelete, posEndDelete + 4);
		}
		return sbVerse.toString();
	}
	
	public static String getRootCause(Throwable t) {
		Throwable cause = t;
		Throwable subCause = cause.getCause();
		while (subCause != null && !subCause.equals(cause)) {
			cause = subCause;
			subCause = cause.getCause();
		}
		return cause.getMessage();
	}
}
