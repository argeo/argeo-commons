package org.argeo.cms.acr.directory;

import java.util.Dictionary;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.directory.UserDirectory;
import org.osgi.service.useradmin.Role;

class RoleContent extends AbstractDirectoryContent {

	private HierarchyUnitContent parent;
	private Role role;

	public RoleContent(ProvidedSession session, DirectoryContentProvider provider, HierarchyUnitContent parent,
			Role role) {
		super(session, provider);
		this.parent = parent;
		this.role = role;
	}

	@Override
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
		if (Role.class.isAssignableFrom(clss))
			return (A) role;
		return super.adapt(clss);
	}

}
