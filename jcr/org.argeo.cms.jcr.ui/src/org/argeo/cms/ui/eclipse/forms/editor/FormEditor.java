package org.argeo.cms.ui.eclipse.forms.editor;

import org.argeo.cms.ui.eclipse.forms.FormToolkit;
import org.eclipse.jface.dialogs.IPageChangeProvider;

/**
 * This class forms a base of multi-page form editors that typically use one or
 * more pages with forms and one page for raw source of the editor input.
 * <p>
 * Pages are added 'lazily' i.e. adding a page reserves a tab for it but does
 * not cause the page control to be created. Page control is created when an
 * attempt is made to select the page in question. This allows editors with
 * several tabs and complex pages to open quickly.
 * <p>
 * Subclasses should extend this class and implement <code>addPages</code>
 * method. One of the two <code>addPage</code> methods should be called to
 * contribute pages to the editor. One adds complete (standalone) editors as
 * nested tabs. These editors will be created right away and will be hooked so
 * that key bindings, selection service etc. is compatible with the one for the
 * standalone case. The other method adds classes that implement
 * <code>IFormPage</code> interface. These pages will be created lazily and
 * they will share the common key binding and selection service. Since 3.1,
 * FormEditor is a page change provider. It allows listeners to attach to it and
 * get notified when pages are changed. This new API in JFace allows dynamic
 * help to update on page changes.
 * 
 * @since 1.0
 */
// RAP [if] As RAP is still using workbench 3.4, the implementation of
// IPageChangeProvider is missing from MultiPageEditorPart. Remove this code
// with the adoption of workbench > 3.5
//public abstract class FormEditor extends MultiPageEditorPart  {
public abstract class FormEditor  implements
        IPageChangeProvider {
	private FormToolkit formToolkit;
	
	
public FormToolkit getToolkit() {
		return formToolkit;
	}

public void editorDirtyStateChanged() {
	
}

public FormPage getActivePageInstance() {
	return null;
}

	// RAP [if] As RAP is still using workbench 3.4, the implementation of
// IPageChangeProvider is missing from MultiPageEditorPart. Remove this code
// with the adoption of workbench > 3.5
//	private ListenerList pageListeners = new ListenerList();
//	
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.eclipse.jface.dialogs.IPageChangeProvider#addPageChangedListener(org.eclipse.jface.dialogs.IPageChangedListener)
//     */
//    public void addPageChangedListener(IPageChangedListener listener) {
//        pageListeners.add(listener);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.eclipse.jface.dialogs.IPageChangeProvider#removePageChangedListener(org.eclipse.jface.dialogs.IPageChangedListener)
//     */
//    public void removePageChangedListener(IPageChangedListener listener) {
//        pageListeners.remove(listener);
//    }
//    
//	private void firePageChanged(final PageChangedEvent event) {
//        Object[] listeners = pageListeners.getListeners();
//        for (int i = 0; i < listeners.length; ++i) {
//            final IPageChangedListener l = (IPageChangedListener) listeners[i];
//            SafeRunnable.run(new SafeRunnable() {
//                public void run() {
//                    l.pageChanged(event);
//                }
//            });
//        }
//    }
// RAPEND [if]
}
