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

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public class ImportToServerWizardPage extends WizardPage {

	public final static String FILE_ITEM_TYPE = "FILE";
	public final static String FOLDER_ITEM_TYPE = "FOLDER";

	private DirectoryFieldEditor dfe;

	public ImportToServerWizardPage() {
		super("Import from file system");
		setDescription("Import files from the local file system into the JCR repository");
	}

	public void createControl(Composite parent) {
		dfe = new DirectoryFieldEditor("directory", "From", parent);
		setControl(dfe.getTextControl(parent));
	}

	public String getObjectPath() {
		return dfe.getStringValue();
	}

	public String getObjectType() {
		return FOLDER_ITEM_TYPE;
	}

	public boolean getNeedsProgressMonitor() {
		return true;
	}

	// Dummy methods : useless in RCP context but useful for RAP
	/** WARNING : always return null in RCP context */
	public String getObjectName() {
		return null;
	}

	/** WARNING : do nothing in RCP context */
	public void performFinish() {
	}

	/** WARNING : always return null in RCP context */
	public InputStream getFileInputStream() {
		return null;
	}
}