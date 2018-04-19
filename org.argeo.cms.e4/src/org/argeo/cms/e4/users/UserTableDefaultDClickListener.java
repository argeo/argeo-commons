package org.argeo.cms.e4.users;

import org.argeo.cms.e4.CmsE4Utils;
import org.argeo.naming.LdapAttrs;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.User;

/**
 * Default double click listener for the various user tables, will open the
 * clicked item in the editor
 */
public class UserTableDefaultDClickListener implements IDoubleClickListener {
	private final EPartService partService;

	public UserTableDefaultDClickListener(EPartService partService) {
		this.partService = partService;
	}

	public void doubleClick(DoubleClickEvent evt) {
		if (evt.getSelection().isEmpty())
			return;
		Object obj = ((IStructuredSelection) evt.getSelection()).getFirstElement();
		User user = (User) obj;

		String editorId = getEditorId(user);
		CmsE4Utils.openEditor(partService, editorId, LdapAttrs.uid.name(), user.getName());
	}

	protected String getEditorId(User user) {
		if (user instanceof Group)
			return "org.argeo.cms.e4.partdescriptor.groupEditor";
		else
			return "org.argeo.cms.e4.partdescriptor.userEditor";
	}
}
