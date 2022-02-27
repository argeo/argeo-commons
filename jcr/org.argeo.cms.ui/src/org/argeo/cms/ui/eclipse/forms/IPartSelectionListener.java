package org.argeo.cms.ui.eclipse.forms;

import org.eclipse.jface.viewers.ISelection;

/**
 * Form parts can implement this interface if they want to be 
 * notified when another part on the same form changes selection 
 * state.
 * 
 * @see IFormPart
 * @since 1.0
 */
public interface IPartSelectionListener {
	/**
	 * Called when the provided part has changed selection state.
	 * 
	 * @param part
	 *            the selection source
	 * @param selection
	 *            the new selection
	 */
	public void selectionChanged(IFormPart part, ISelection selection);
}
