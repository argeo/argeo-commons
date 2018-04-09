package org.argeo.cms.e4.users;

import org.argeo.cms.CmsException;
import org.argeo.naming.LdapAttrs;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
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

		String entityEditorId = getEditorId(user);
		MPart part = partService.createPart(entityEditorId);
		part.setLabel(user.toString());
		part.getPersistedState().put(LdapAttrs.uid.name(), user.getName());

		// the provided part is be shown
		partService.showPart(part, PartState.ACTIVATE);

		// IWorkbenchWindow iww = WorkbenchUiPlugin.getDefault().getWorkbench()
		// .getActiveWorkbenchWindow();
		// IWorkbenchPage iwp = iww.getActivePage();
		// UserEditorInput uei = new UserEditorInput(user.getName());
		// FIXME open editor

		try {
			// Works around the fact that dynamic setting of the editor icon
			// causes NPE after a login/logout on RAP
			// if (user instanceof Group)
			// iwp.openEditor(uei, UserEditor.GROUP_EDITOR_ID);
			// else
			// iwp.openEditor(uei, UserEditor.USER_EDITOR_ID);
		} catch (Exception pie) {
			throw new CmsException("Unable to open UserEditor for " + user, pie);
		}
	}

	protected String getEditorId(User user) {
		if (user instanceof Group)
			return "org.argeo.cms.e4.partdescriptor.groupEditor";
		else
			return "org.argeo.cms.e4.partdescriptor.userEditor";
	}
}
