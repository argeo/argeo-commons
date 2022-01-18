package org.argeo.cms.jcr.gcr;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.spi.ContentProvider;
import org.argeo.cms.jcr.CmsJcrUtils;

public class JcrContentProvider implements ContentProvider {
	private Repository jcrRepository;
	private Session jcrSession;

	public void init() {
		jcrSession = CmsJcrUtils.openDataAdminSession(jcrRepository, null);
	}

	public void setJcrRepository(Repository jcrRepository) {
		this.jcrRepository = jcrRepository;
	}

	@Override
	public Content get() {
		return null;
	}

	@Override
	public Content get(String relativePath) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
