package org.argeo.cms.acr.directory;

import java.util.Dictionary;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.directory.CmsRole;
import org.argeo.api.cms.directory.UserDirectory;
import org.osgi.service.useradmin.Role;

class RoleContent extends AbstractDirectoryContent {

	private HierarchyUnitContent parent;
	private CmsRole role;

	public RoleContent(ProvidedSession session, DirectoryContentProvider provider, HierarchyUnitContent parent,
			CmsRole role) {
		super(session, provider);
		this.parent = parent;
		this.role = role;
	}

	@Override
	@Deprecated
	Dictionary<String, Object> doGetProperties() {
		return role.getProperties();
	}

	@Override
	public QName getName() {
		String name = ((UserDirectory) parent.getHierarchyUnit().getDirectory()).getRoleSimpleName(role);
		return new ContentName(name);
	}

	@Override
	public Content getParent() {
		return parent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A adapt(Class<A> clss) {
		if (CmsRole.class.isAssignableFrom(clss))
			return (A) role;
		// TODO do we need this?
//		if (Role.class.isAssignableFrom(clss))
//			return (A) role;
		return super.adapt(clss);
	}

}
