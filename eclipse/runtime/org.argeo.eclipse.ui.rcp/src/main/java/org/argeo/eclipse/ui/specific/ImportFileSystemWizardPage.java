package org.argeo.eclipse.ui.specific;

import java.io.InputStream;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public class ImportFileSystemWizardPage extends WizardPage {
	private DirectoryFieldEditor dfe;

	public ImportFileSystemWizardPage() {
		super("Import from file system");
		setDescription("Import files from the local file system into the JCR repository");
	}

	public void createControl(Composite parent) {
		dfe = new DirectoryFieldEditor("directory", "From", parent);
		setControl(dfe.getTextControl(parent));
	}

	public String getObjectPath() {
		System.out.println("dfe.getStringValue() : " + dfe.getStringValue());
		return dfe.getStringValue();
	}

	public String getObjectType() {
		return "nt:folder";
	}

	// Dummy methods : useless in RCP context but useful for RAP
	/** WARNING : always return null in RCP context */
	public String getObjectName() {
		return null;
	}

	/** WARNING : di nothing in RCP context */
	public void performFinish() {
	}

	/** WARNING : always return null in RCP context */
	public InputStream getFileInputStream() {
		return null;
	}

}
