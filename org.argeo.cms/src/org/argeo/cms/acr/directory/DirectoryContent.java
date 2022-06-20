package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.osgi.useradmin.HierarchyUnit;
import org.argeo.osgi.useradmin.UserDirectory;

class DirectoryContent extends AbstractContent {
	private UserDirectory directory;
	private DirectoryContentProvider provider;

	public DirectoryContent(ProvidedSession session, DirectoryContentProvider provider, UserDirectory directory) {
		super(session);
		this.provider = provider;
		this.directory = directory;
	}

	@Override
	public ContentProvider getProvider() {
		return provider;
	}

	@Override
	public Iterator<Content> iterator() {
		List<Content> res = new ArrayList<>();
		for (Iterator<HierarchyUnit> it = directory.getRootHierarchyUnits().iterator(); it.hasNext();) {
			res.add(new HierarchyUnitContent(getSession(), provider, it.next()));
		}
		return res.iterator();
	}

	@Override
	public QName getName() {
		return new ContentName(directory.getName());
	}

	@Override
	public Content getParent() {
		return provider.getRootContent(getSession());
	}

}
