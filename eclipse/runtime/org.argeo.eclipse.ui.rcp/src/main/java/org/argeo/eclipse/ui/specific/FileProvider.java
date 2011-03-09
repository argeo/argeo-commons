package org.argeo.eclipse.ui.specific;

/**
 * Used for file download : subclasses must implement model specific methods to
 * get a byte array representing a file given is ID.
 * 
 * @author bsinou
 * 
 */
public interface FileProvider {

	public byte[] getByteArrayFileFromId(String fileId);

}
