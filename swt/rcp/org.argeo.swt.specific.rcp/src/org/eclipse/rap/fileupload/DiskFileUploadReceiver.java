package org.eclipse.rap.fileupload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DiskFileUploadReceiver extends FileUploadReceiver {
	public File[] getTargetFiles() {
		return null;
	}

	@Override
	public void receive(InputStream stream, FileDetails details) throws IOException {
	}
}
