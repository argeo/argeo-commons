package org.argeo.eclipse.ui.jcr.commands;

import javax.jcr.Node;
import javax.jcr.Session;

import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/** Init the user home directory within the node */
public class InitUserHome extends AbstractHandler {
	private Session session;

	private String defaultHome = "home";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String userID = "<not yet logged in>";
		try {
			userID = session.getUserID();
			Node rootNode = session.getRootNode();
			Node homeNode;
			if (!rootNode.hasNode(defaultHome)) {
				homeNode = rootNode.addNode(defaultHome, ArgeoTypes.ARGEO_HOME);
			} else {
				homeNode = rootNode.getNode(defaultHome);
			}

			if (!homeNode.hasNode(userID)) {
				Node userHome = homeNode.addNode(userID);
				userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
				userHome.setProperty(ArgeoNames.ARGEO_USER_ID, userID);
			}
			session.save();
		} catch (Exception e) {
			Error.show("Cannot initialize home for user '" + userID + "'", e);
		}
		return null;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
