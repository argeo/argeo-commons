package org.argeo.eclipse.ui.specific;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class GenericUploadControl extends Composite {
	private final static Log log = LogFactory
			.getLog(GenericUploadControl.class);

	private FileDialog dialog;
	private Text filePath;

	public GenericUploadControl(Composite parent, int style, String browseLabel) {
		super(parent, style);
		createControl(this, browseLabel);

	}

	private void createControl(final Composite parent, String browseLabel) {
		Composite composite = new Composite(parent, SWT.FILL);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		filePath = new Text(parent, SWT.BORDER | SWT.SINGLE);
		filePath.setEnabled(false);

		// Execute button
		Button execute = new Button(parent, SWT.PUSH);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.BEGINNING;
		execute.setLayoutData(gridData);
		execute.setText(browseLabel);

		// Button listener
		Listener executeListener = new Listener() {
			public void handleEvent(Event event) {
				dialog = new FileDialog(parent.getShell());
				dialog.open();

			}
		};

		execute.addListener(SWT.Selection, executeListener);
	}

	public boolean isControlEmpty() {
		String path = filePath.getText();
		if (path == null || "".equals(path.trim()))
			return true;
		else
			return false;
	}

	public byte[] performUpload() {
		String path = filePath.getText();
		if (path != null) {
			try {
				File file = new File(path);
				byte[] fileBA = FileUtils.readFileToByteArray(file);
				return fileBA;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public void addModifyListener(ModifyListener listener) {
		filePath.addModifyListener(listener);
	}
	
	/**
	 * Always returns null in an RCP environment
	 */
	public String getLastFileUploadedName() {
		return null;
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
