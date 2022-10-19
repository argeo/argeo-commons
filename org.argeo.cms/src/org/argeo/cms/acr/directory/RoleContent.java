package org.argeo.cms.acr.directory;

import java.util.Dictionary;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.osgi.useradmin.UserDirectory;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

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
		if (clss.equals(Group.class))
			return (A) role;
		else if (clss.equals(User.class))
			return (A) role;
		else if (clss.equals(Role.class))
			return (A) role;
		return super.adapt(clss);
	}

}
