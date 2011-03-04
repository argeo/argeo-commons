package org.argeo.eclipse.ui.specific;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.rwt.widgets.Upload;
import org.eclipse.rwt.widgets.UploadAdapter;
import org.eclipse.rwt.widgets.UploadEvent;
import org.eclipse.rwt.widgets.UploadItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class ImportFileSystemWizardPage extends WizardPage {
	private Log log = LogFactory.getLog(ImportFileSystemWizardPage.class);

	private Upload uploadFile;

	public ImportFileSystemWizardPage() {
		super("Import from file system");
		setDescription("Import files from the local file system into the JCR repository");
	}

	public void createControl(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		new Label(composite, SWT.NONE).setText("Pick up a file");

		uploadFile = new Upload(composite, SWT.BORDER);
		uploadFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		uploadFile.setBrowseButtonText("Open...");
		uploadFile.addUploadListener(new UploadManager(uploadFile));

		setControl(composite);
	}

	public String getObjectPath() {
		// NOTE Returns the full file name of the last uploaded file including
		// the file path as selected by the user on his local machine.
		// The full path including the directory and file drive are only
		// returned, if the browser supports reading this properties. In Firefox
		// 3, only the filename is returned.
		return uploadFile.getPath();
	}

	public String getObjectName() {
		return uploadFile.getUploadItem().getFileName();
	}

	public String getObjectType() {
		return "nt:file";
	}

	public void performFinish() {
		boolean success = uploadFile.performUpload();
		if (!success)
			throw new ArgeoException("Cannot upload file named "
					+ uploadFile.getPath());
	}

	protected void handleUploadFinished(final Upload upload) {
		if (log.isTraceEnabled()) {
			UploadItem uploadItem = upload.getUploadItem();
			log.trace("filename     : " + uploadItem.getFileName());
			log.trace("content-type : " + uploadItem.getContentType());
		}
	}

	public InputStream getFileInputStream() {
		return uploadFile.getUploadItem().getFileInputStream();
	}

	private class UploadManager extends UploadAdapter {
		private Upload upload;

		public UploadManager(Upload upload) {
			super();
			this.upload = upload;
		}

		public void uploadFinished(UploadEvent uploadEvent) {
			handleUploadFinished(upload);
		}

		public void uploadInProgress(UploadEvent uploadEvent) {
		}

		public void uploadException(UploadEvent uploadEvent) {
			Exception exc = uploadEvent.getUploadException();
			if (exc != null) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						"Error", exc.getMessage());
			}
		}
	}
}
