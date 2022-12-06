package org.eclipse.rap.fileupload;

import java.util.EventObject;

public abstract class FileUploadEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	protected FileUploadEvent(FileUploadHandler source) {
		super(source);
	}

	public abstract FileDetails[] getFileDetails();

	public abstract long getContentLength();

	public abstract long getBytesRead();

	public abstract Exception getException();

}
