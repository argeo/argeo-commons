package org.eclipse.rap.fileupload;

public interface FileDetails {
	String getContentType();

	long getContentLength();

	String getFileName();
}
