package org.argeo.cms.e4.rap;

import java.util.Enumeration;

import org.argeo.api.cms.CmsLog;
import org.eclipse.rap.rwt.application.Application;
import org.osgi.framework.Bundle;

/** Simple RAP app which loads all e4xmi files. */
public class SimpleRapE4App extends AbstractRapE4App {
	private final static CmsLog log = CmsLog.getLog(SimpleRapE4App.class);

	private String baseE4xmi = "/e4xmi";

	@Override
	protected void addEntryPoints(Application application) {
		Bundle bundle = getBundleContext().getBundle();
		Enumeration<String> paths = bundle.getEntryPaths(baseE4xmi);
		while (paths.hasMoreElements()) {
			String p = paths.nextElement();
			if (p.endsWith(".e4xmi")) {
				String e4xmiPath = bundle.getSymbolicName() + '/' + p;
				// FIXME deal with base name
				String name=null;// = '/' + FilenameUtils.removeExtension(FilenameUtils.getName(p));
				addE4EntryPoint(application, name, e4xmiPath, getBaseProperties());
				if (log.isDebugEnabled())
					log.debug("Registered " + e4xmiPath + " as " + getContextName() + name);
			}
		}
	}

}
