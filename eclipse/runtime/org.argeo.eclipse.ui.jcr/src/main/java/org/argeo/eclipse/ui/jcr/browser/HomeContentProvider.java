package org.argeo.eclipse.ui.jcr.browser;

import javax.jcr.Session;

import org.argeo.eclipse.ui.jcr.SimpleNodeContentProvider;
import org.argeo.jcr.JcrUtils;

public class HomeContentProvider extends SimpleNodeContentProvider {

	public HomeContentProvider(Session session) {
		super(session, new String[] { JcrUtils.getUserHomePath(session) });
	}

}
