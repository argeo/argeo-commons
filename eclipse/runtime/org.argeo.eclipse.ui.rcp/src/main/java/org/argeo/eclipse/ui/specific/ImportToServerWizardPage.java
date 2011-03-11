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