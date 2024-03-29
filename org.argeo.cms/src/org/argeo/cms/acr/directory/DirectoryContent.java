package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.directory.CmsDirectory;
import org.argeo.api.cms.directory.HierarchyUnit;

class DirectoryContent extends AbstractDirectoryContent {
	private CmsDirectory directory;

	public DirectoryContent(ProvidedSession session, DirectoryContentProvider provider, CmsDirectory directory) {
		super(session, provider);
		this.directory = directory;
	}

	@Override
	Dictionary<String, Object> doGetProperties() {
		return directory.getProperties();
	}

	@Override
	public Iterator<Content> iterator() {
		List<Content> res = new ArrayList<>();
		for (Iterator<HierarchyUnit> it = directory.getDirectHierarchyUnits(false).iterator(); it.hasNext();) {
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

	@SuppressWarnings("unchecked")
	@Override
	public <A> A adapt(Class<A> clss) {
		if (clss.equals(HierarchyUnit.class))
			return (A) directory;
		if (clss.equals(CmsDirectory.class))
			return (A) directory;
		return super.adapt(clss);
	}

}
