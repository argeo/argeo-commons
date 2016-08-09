package org.argeo.cms.internal.kernel;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.node.RepoConf;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public class RepositoryService implements ManagedService, MetaTypeProvider {
	private BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
	// private RepositoryContext repositoryContext = null;
	private ServiceRegistration<RepositoryContext> repositoryContextReg;

	@Override
	public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if (properties == null)
			return;

		if (repositoryContextReg != null) {
			shutdown();
		}

//		for (String key : LangUtils.keys(properties)) {
//			Object value = properties.get(key);
//			System.out.println(key + " : " + value.getClass().getName());
//		}

		try {
			RepositoryBuilder repositoryBuilder = new RepositoryBuilder();
			RepositoryContext repositoryContext = repositoryBuilder.createRepositoryContext(properties);
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, properties.get(RepoConf.labeledUri.name()));
			Object cn = properties.get(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS);
			if (cn != null) {
				props.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, cn);
			}
			repositoryContextReg = bc.registerService(RepositoryContext.class, repositoryContext, props);
		} catch (Exception e) {
			throw new ArgeoException("Cannot create Jackrabbit repository", e);
		}

	}

	public synchronized void shutdown() {
		if (repositoryContextReg == null)
			return;
		RepositoryContext repositoryContext = bc.getService(repositoryContextReg.getReference());
		repositoryContext.getRepository().shutdown();
		repositoryContextReg.unregister();
		repositoryContextReg = null;
	}

	/*
	 * METATYPE
	 */
	@Override
	public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
		return new RepoConf.OCD(locale);
		// return new EnumOCD<>(RepoConf.class);
		// return new JcrRepositoryOCD(locale);
	}

	@Override
	public String[] getLocales() {
		// TODO optimize?
		List<Locale> locales = Activator.getNodeState().getLocales();
		String[] res = new String[locales.size()];
		for (int i = 0; i < locales.size(); i++)
			res[i] = locales.get(i).toString();
		return res;
	}

	/*
	 * JACKRABBIT REPOSITORY
	 */

	// private RepositoryImpl repo() {
	// return repositoryContext.getRepository();
	// }
	//
	// @Override
	// public String[] getDescriptorKeys() {
	// return repo().getDescriptorKeys();
	// }
	//
	// @Override
	// public boolean isStandardDescriptor(String key) {
	// return repo().isStandardDescriptor(key);
	// }
	//
	// @Override
	// public boolean isSingleValueDescriptor(String key) {
	// return repo().isSingleValueDescriptor(key);
	// }
	//
	// @Override
	// public Value getDescriptorValue(String key) {
	// return repo().getDescriptorValue(key);
	// }
	//
	// @Override
	// public Value[] getDescriptorValues(String key) {
	// return repo().getDescriptorValues(key);
	// }
	//
	// @Override
	// public String getDescriptor(String key) {
	// return repo().getDescriptor(key);
	// }
	//
	// @Override
	// public Session login(Credentials credentials, String workspaceName)
	// throws LoginException, NoSuchWorkspaceException, RepositoryException {
	// return repo().login();
	// }
	//
	// @Override
	// public Session login(Credentials credentials) throws LoginException,
	// RepositoryException {
	// return repo().login(credentials);
	// }
	//
	// @Override
	// public Session login(String workspaceName) throws LoginException,
	// NoSuchWorkspaceException, RepositoryException {
	// return repo().login(workspaceName);
	// }
	//
	// @Override
	// public Session login() throws LoginException, RepositoryException {
	// return repo().login();
	// }
	//
	// @Override
	// public Session login(Credentials credentials, String workspaceName,
	// Map<String, Object> attributes)
	// throws LoginException, NoSuchWorkspaceException, RepositoryException {
	// return repo().login(credentials, workspaceName, attributes);
	// }

}
