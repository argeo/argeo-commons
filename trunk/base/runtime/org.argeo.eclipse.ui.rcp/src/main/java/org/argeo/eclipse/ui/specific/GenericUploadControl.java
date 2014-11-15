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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.argeo.ArgeoException;
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

/**
 * RCP specific composite that provides a control to upload a file. WARNING: for
 * the time being we set a GridLayout(2, false) on th eparent control.
 */
public class GenericUploadControl extends Composite {
	// private final static Log log = LogFactory
	// .getLog(GenericUploadControl.class);

	private FileDialog dialog;
	private Text filePath;

	public GenericUploadControl(Composite parent, int style, String browseLabel) {
		super(parent, style);
		createControl(this, browseLabel);

	}

	private void createControl(final Composite parent, String browseLabel) {
		parent.setLayout(new GridLayout(2, false));

		filePath = new Text(parent, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.FILL_HORIZONTAL);
		filePath.setEditable(false);
		filePath.setLayoutData(gd);

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
				filePath.setText(dialog.open());
			}
		};
		parent.layout();
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
				throw new ArgeoException("Unexpected error while "
						+ "reading file at path " + path, e);
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
}
