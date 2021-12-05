package org.argeo.cms.jcr.internal.servlet;

import java.util.Map;

import javax.jcr.Repository;

import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.argeo.api.NodeConstants;

/** A {@link SimpleWebdavServlet} based on {@link CmsSessionProvider}. */
public class CmsWebDavServlet extends SimpleWebdavServlet {
	private static final long serialVersionUID = 7485800288686328063L;
	private Repository repository;

	public CmsWebDavServlet() {
	}

	public CmsWebDavServlet(String alias, Repository repository) {
		this.repository = repository;
		setSessionProvider(new CmsSessionProvider(alias));
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository, Map<String, String> properties) {
		this.repository = repository;
		String alias = properties.get(NodeConstants.CN);
		if (alias != null)
			setSessionProvider(new CmsSessionProvider(alias));
		else
			throw new IllegalArgumentException("Only aliased repositories are supported");
	}

}
