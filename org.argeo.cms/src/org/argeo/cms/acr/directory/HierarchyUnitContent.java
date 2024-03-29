package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.DName;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.directory.CmsDirectory;
import org.argeo.api.cms.directory.CmsRole;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.directory.UserDirectory;

class HierarchyUnitContent extends AbstractDirectoryContent {
	private HierarchyUnit hierarchyUnit;

	public HierarchyUnitContent(ProvidedSession session, DirectoryContentProvider provider,
			HierarchyUnit hierarchyUnit) {
		super(session, provider);
		Objects.requireNonNull(hierarchyUnit);
		this.hierarchyUnit = hierarchyUnit;
	}

	@Override
	Dictionary<String, Object> doGetProperties() {
		return hierarchyUnit.getProperties();
	}

	@Override
	public QName getName() {
//		if (hierarchyUnit.getParent() == null) {// base DN
//			String baseDn = hierarchyUnit.getBasePath();
//			return new ContentName(baseDn);
//		}
		String name = hierarchyUnit.getHierarchyUnitName();
		return new ContentName(name);
	}

	@Override
	public Content getParent() {
		HierarchyUnit parentHu = hierarchyUnit.getParent();
		if (parentHu instanceof CmsDirectory) {
			return new DirectoryContent(getSession(), provider, hierarchyUnit.getDirectory());
		}
		return new HierarchyUnitContent(getSession(), provider, parentHu);
	}

	@Override
	public Iterator<Content> iterator() {
		List<Content> lst = new ArrayList<>();
		for (HierarchyUnit hu : hierarchyUnit.getDirectHierarchyUnits(false))
			lst.add(new HierarchyUnitContent(getSession(), provider, hu));

		for (CmsRole role : ((UserDirectory) hierarchyUnit.getDirectory()).getHierarchyUnitRoles(hierarchyUnit, null,
				false))
			lst.add(new RoleContent(getSession(), provider, this, role));
		return lst.iterator();
	}

	/*
	 * TYPING
	 */
	@Override
	public List<QName> getContentClasses() {
		List<QName> contentClasses = super.getContentClasses();
		contentClasses.add(DName.collection.qName());
		return contentClasses;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A adapt(Class<A> clss) {
		if (clss.equals(HierarchyUnit.class))
			return (A) hierarchyUnit;
		return super.adapt(clss);
	}

	/*
	 * ACCESSOR
	 */
	HierarchyUnit getHierarchyUnit() {
		return hierarchyUnit;
	}

}
