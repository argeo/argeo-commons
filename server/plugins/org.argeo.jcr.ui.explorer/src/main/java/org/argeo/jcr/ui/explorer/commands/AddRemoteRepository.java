package org.argeo.jcr.ui.explorer.commands;

import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.dialogs.SingleValue;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ui.explorer.JcrExplorerConstants;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.osgi.framework.BundleContext;

/**
 * Connect to a remote repository and, if usccessful publish it as an OSGi
 * service.
 */
public class AddRemoteRepository extends AbstractHandler implements
		JcrExplorerConstants {

	private RepositoryFactory repositoryFactory;
	private BundleContext bundleContext;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String uri;
		if (event.getParameters().containsKey(PARAM_REPOSITORY_URI))
			uri = event.getParameter(PARAM_REPOSITORY_URI);
		else
			uri = SingleValue
					.ask("URI",
							"Remote repository URI"
									+ " (e.g. http://localhost:7070/org.argeo.jcr.webapp/remoting/node)");

		if (uri == null)
			return null;

		try {
			Hashtable<String, String> params = new Hashtable<String, String>();
			params.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, uri);
			// by default we use the URI as alias
			params.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, uri);
			Repository repository = repositoryFactory.getRepository(params);
			bundleContext.registerService(Repository.class.getName(),
					repository, params);
		} catch (Exception e) {
			ErrorFeedback.show("Cannot add remote repository " + uri, e);
		}
		return null;
	}

	public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
