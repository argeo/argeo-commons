package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.argeo.api.acr.ArgeoNamespace;
import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.ContentNotFoundException;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.directory.CmsUserManager;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.directory.UserDirectory;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.cms.acr.ContentUtils;
import org.osgi.service.useradmin.User;

public class DirectoryContentProvider implements ContentProvider {
	private String mountPath;
	private String mountName;

	private CmsUserManager userManager;

	public DirectoryContentProvider(String mountPath, CmsUserManager userManager) {
		this.mountPath = mountPath;
		List<String> mountSegments = ContentUtils.toPathSegments(mountPath);
		this.mountName = mountSegments.get(mountSegments.size() - 1);
		this.userManager = userManager;
	}

	@Override
	public ProvidedContent get(ProvidedSession session, String relativePath) {
		List<String> segments = ContentUtils.toPathSegments(relativePath);
		if (segments.size() == 0)
			return new UserManagerContent(session);
		String userDirectoryName = segments.get(0);
		UserDirectory userDirectory = null;
		userDirectories: for (UserDirectory ud : userManager.getUserDirectories()) {
			if (userDirectoryName.equals(ud.getName())) {
				userDirectory = ud;
				break userDirectories;
			}
		}
		if (userDirectory == null)
			throw new ContentNotFoundException(session, mountPath + "/" + relativePath,
					"Cannot find user directory " + userDirectoryName);
		if (segments.size() == 1) {
			return new DirectoryContent(session, this, userDirectory);
		} else {
			List<String> relSegments = new ArrayList<>(segments);
			relSegments.remove(0);
			String pathWithinUserDirectory = ContentUtils.toPath(relSegments);
//			LdapName dn;
//			try {
//				dn = LdapNameUtils.toLdapName(userDirectoryDn);
//				for (int i = 1; i < segments.size(); i++) {
//					dn.add(segments.get(i));
//				}
//			} catch (InvalidNameException e) {
//				throw new IllegalStateException("Cannot interpret " + segments + " as DN", e);
//			}
			User user = (User) userDirectory.getRoleByPath(pathWithinUserDirectory);
			if (user != null) {
				HierarchyUnit parent = userDirectory.getHierarchyUnit(user);
				return new RoleContent(session, this, new HierarchyUnitContent(session, this, parent), user);
			}
			HierarchyUnit hierarchyUnit = userDirectory.getHierarchyUnit(pathWithinUserDirectory);
			if (hierarchyUnit == null)
				throw new ContentNotFoundException(session,
						mountPath + "/" + relativePath + "/" + pathWithinUserDirectory,
						"Cannot find " + pathWithinUserDirectory + " within " + userDirectoryName);
			return new HierarchyUnitContent(session, this, hierarchyUnit);
		}
	}

	@Override
	public boolean exists(ProvidedSession session, String relativePath) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getMountPath() {
		return mountPath;
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if (ArgeoNamespace.LDAP_DEFAULT_PREFIX.equals(prefix))
			return ArgeoNamespace.LDAP_NAMESPACE_URI;
		throw new IllegalArgumentException("Only prefix " + ArgeoNamespace.LDAP_DEFAULT_PREFIX + " is supported");
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		if (ArgeoNamespace.LDAP_NAMESPACE_URI.equals(namespaceURI))
			return Collections.singletonList(ArgeoNamespace.LDAP_DEFAULT_PREFIX).iterator();
		throw new IllegalArgumentException("Only namespace URI " + ArgeoNamespace.LDAP_NAMESPACE_URI + " is supported");
	}

	public void setUserManager(CmsUserManager userManager) {
		this.userManager = userManager;
	}

	UserManagerContent getRootContent(ProvidedSession session) {
		return new UserManagerContent(session);
	}

	/*
	 * COMMON UTILITIES
	 */
	class UserManagerContent extends AbstractContent {

		public UserManagerContent(ProvidedSession session) {
			super(session);
		}

		@Override
		public ContentProvider getProvider() {
			return DirectoryContentProvider.this;
		}

		@Override
		public QName getName() {
			return new ContentName(mountName);
		}

		@Override
		public Content getParent() {
			return null;
		}

		@Override
		public Iterator<Content> iterator() {
			List<Content> res = new ArrayList<>();
			for (UserDirectory userDirectory : userManager.getUserDirectories()) {
				DirectoryContent content = new DirectoryContent(getSession(), DirectoryContentProvider.this,
						userDirectory);
				res.add(content);
			}
			return res.iterator();
		}

	}
}
