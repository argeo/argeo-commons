package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.osgi.useradmin.UserDirectory;
import org.argeo.util.directory.HierarchyUnit;
import org.osgi.service.useradmin.Role;

class HierarchyUnitContent extends AbstractContent {
	private HierarchyUnit hierarchyUnit;

	private DirectoryContentProvider provider;

	public HierarchyUnitContent(ProvidedSession session, DirectoryContentProvider provider,
			HierarchyUnit hierarchyUnit) {
		super(session);
		Objects.requireNonNull(hierarchyUnit);
		this.provider = provider;
		this.hierarchyUnit = hierarchyUnit;
	}

	@Override
	public ContentProvider getProvider() {
		return provider;
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
		if (parentHu == null) {
			return new DirectoryContent(getSession(), provider, hierarchyUnit.getDirectory());
		}
		return new HierarchyUnitContent(getSession(), provider, parentHu);
	}

	@Override
	public Iterator<Content> iterator() {
		List<Content> lst = new ArrayList<>();
		for (HierarchyUnit hu : hierarchyUnit.getDirectHierachyUnits(false))
			lst.add(new HierarchyUnitContent(getSession(), provider, hu));

		for (Role role : ((UserDirectory) hierarchyUnit.getDirectory()).getHierarchyUnitRoles(hierarchyUnit, null,
				false))
			lst.add(new RoleContent(getSession(), provider, this, role));
		return lst.iterator();
	}

	/*
	 * TYPING
	 */

	@Override
	public List<QName> getTypes() {
		List<QName> res = new ArrayList<>();
		res.add(CrName.COLLECTION.get());
		return res;
	}

	/*
	 * ACCESSOR
	 */
	HierarchyUnit getHierarchyUnit() {
		return hierarchyUnit;
	}

}
