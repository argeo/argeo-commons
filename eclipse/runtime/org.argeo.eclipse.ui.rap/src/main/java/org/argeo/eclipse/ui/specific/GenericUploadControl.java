package org.argeo.eclipse.ui.specific;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.eclipse.rwt.widgets.Upload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class GenericUploadControl extends Composite {
	private final static Log log = LogFactory
			.getLog(GenericUploadControl.class);

	private Upload upload;

	public GenericUploadControl(Composite parent, int style, String browseLabel) {
		super(parent, style);
		createControl(this, browseLabel);

	}

	private void createControl(Composite parent, String browseLabel) {
		parent.setLayout(new GridLayout(1, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		upload = new Upload(parent, SWT.BORDER);
		upload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		upload.setBrowseButtonText(browseLabel);
		// upload.addModifyListener(new UploadListener());
		parent.pack();
	}

	/**
	 * Wrap upload.getLastFileUploaded(). Gets the name of the last uploaded
	 * file. This method can be called even if the upload has not finished yet.
	 */
	public String getLastFileUploadedName() {
		return upload.getLastFileUploaded();
	}

	public boolean isControlEmpty() {
		String path = upload.getPath();
		if (log.isTraceEnabled())
			log.trace("UploadControl chosen path : " + path);
		if (path == null || "".equals(path.trim()))
			return true;
		else
			return false;
	}

	public byte[] performUpload() {
		boolean success = upload.performUpload();
		if (success) {
			if (upload.getUploadItem().getFileSize() == -1)
				throw new ArgeoException("File "
						+ upload.getUploadItem().getFileName()
						+ " has not been uploaded, its size is -1");

			InputStream inStream = null;
			byte[] fileBA = null;
			try {
				inStream = upload.getUploadItem().getFileInputStream();
				fileBA = IOUtils.toByteArray(inStream);
			} catch (Exception e) {
				throw new ArgeoException("Cannot read uploaded data", e);
			} finally {
				IOUtils.closeQuietly(inStream);
			}
			return fileBA;
		}
		return null;
	}

	public void addModifyListener(ModifyListener listener) {
		upload.addModifyListener(listener);
	}

	// private class UploadManager extends UploadAdapter {
	// private Upload upload;
	//
	// public UploadManager(Upload upload) {
	// super();
	// this.upload = upload;
	// }
	//
	// public void uploadFinished(UploadEvent uploadEvent) {
	// handleUploadFinished(upload);
	// }
	//
	// public void uploadInProgress(UploadEvent uploadEvent) {
	// }
	//
	// public void uploadException(UploadEvent uploadEvent) {
	// Exception exc = uploadEvent.getUploadException();
	// if (exc != null) {
	// MessageDialog.openError(Display.getCurrent().getActiveShell(),
	// "Error", exc.getMessage());
	// }
	// }
	//
	// }
	//

}
