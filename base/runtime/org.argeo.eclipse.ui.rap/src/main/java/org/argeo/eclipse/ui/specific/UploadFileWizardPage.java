/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.eclipse.ui.specific;

import java.io.InputStream;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class UploadFileWizardPage extends WizardPage {
	// private final static Log log = LogFactory
	// .getLog(UploadFileWizardPage.class);
	private static final long serialVersionUID = 8251354244542973179L;
	public final static String FILE_ITEM_TYPE = "FILE";
	public final static String FOLDER_ITEM_TYPE = "FOLDER";

	private FileUpload fileUpload;

	public UploadFileWizardPage() {
		super("Import from file system");
		setDescription("Import files from the local file system to the server");
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		new Label(composite, SWT.NONE).setText("Pick up a file");
		fileUpload = new FileUpload(composite, SWT.BORDER);
		fileUpload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fileUpload.setText("Browse");
		setControl(composite);
	}

	public String getObjectPath() {
		// NOTE Returns the full file name of the last uploaded file including
		// the file path as selected by the user on his local machine.
		// The full path including the directory and file drive are only
		// returned, if the browser supports reading this properties. In Firefox
		// 3, only the filename is returned.
		return null;
	}

	public String getObjectName() {
		return fileUpload.getFileName();
	}

	public String getObjectType() {
		return FILE_ITEM_TYPE;
	}

	public void performFinish() {
		// boolean success = uploadFile.performUpload();
		// if (!success)
		// throw new ArgeoException("Cannot upload file named "
		// + uploadFile.getPath());
	}

	// protected void handleUploadFinished(final Upload upload) {
	// }

	public InputStream getFileInputStream() {
		return null;
	}

	public boolean getNeedsProgressMonitor() {
		return false;
	}

}
