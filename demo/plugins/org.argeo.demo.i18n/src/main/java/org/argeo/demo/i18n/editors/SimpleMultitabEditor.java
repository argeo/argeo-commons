package org.argeo.demo.i18n.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.demo.i18n.I18nDemoMessages;
import org.argeo.demo.i18n.I18nDemoPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;

/**
 * 
 * Container for the node editor page. At creation time, it takes a JCR Node
 * that cannot be changed afterwards.
 * 
 */
public class SimpleMultitabEditor extends FormEditor {

	private final static Log log = LogFactory
			.getLog(SimpleMultitabEditor.class);
	public final static String ID = I18nDemoPlugin.ID + ".simpleMultitabEditor";

	private SimplePage simplePage;
	private MultiSectionPage multiSectionPage;

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		// this.setPartName("Internationalized editor part name");
	}

	@Override
	protected void addPages() {
		try {
			simplePage = new SimplePage(this,
					I18nDemoMessages.get().SimpleMultitabEditor_SimplePageTitle);
			addPage(simplePage);

			multiSectionPage = new MultiSectionPage(
					this,
					I18nDemoMessages.get().SimpleMultitabEditor_MultiSectionPageTitle);
			addPage(multiSectionPage);

		} catch (PartInitException e) {
			throw new ArgeoException("Not able to add an empty page ", e);
		}
	}

	@Override
	public void doSaveAs() {
		// unused compulsory method
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			// Automatically commit all pages of the editor
			commitPages(true);
			firePropertyChange(PROP_DIRTY);
		} catch (Exception e) {
			throw new ArgeoException("Error while saving node", e);
		}

	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
}
