package org.argeo.eclipse.ui.specific;

import java.io.InputStream;

/**
 * Used for file download : subclasses must implement model specific methods to
 * get a byte array representing a file given is ID.
 * 
 * @author bsinou
 * 
 */
public interface FileProvider {

	public byte[] getByteArrayFileFromId(String fileId);

	public InputStream getInputStreamFromFileId(String fileId);

}
