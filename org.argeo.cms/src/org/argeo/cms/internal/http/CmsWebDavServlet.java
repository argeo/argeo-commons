package org.argeo.cms.internal.http;

import java.util.Map;

import javax.jcr.Repository;

import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.argeo.api.NodeConstants;

public class CmsWebDavServlet extends SimpleWebdavServlet {
	private static final long serialVersionUID = 7485800288686328063L;
	private Repository repository;

	@Override
	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository, Map<String, String> properties) {
		this.repository = repository;
		String alias = properties.get(NodeConstants.CN);
		if (alias != null)
			setSessionProvider(new CmsSessionProvider(alias));
	}

}
