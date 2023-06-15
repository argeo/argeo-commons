package org.argeo.cms.acr.directory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.argeo.api.acr.ArgeoNamespace;
import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentNotFoundException;
import org.argeo.api.acr.spi.ContentProvider;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.api.cms.directory.CmsUserManager;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.directory.UserDirectory;
import org.argeo.cms.acr.AbstractSimpleContentProvider;
import org.argeo.cms.acr.ContentUtils;
import org.osgi.service.useradmin.User;

/** A {@link ContentProvider} based on a {@link CmsUserManager} service. */
public class DirectoryContentProvider extends AbstractSimpleContentProvider<CmsUserManager> {

	public DirectoryContentProvider(CmsUserManager service, String mountPath) {
		super(ArgeoNamespace.LDAP_NAMESPACE_URI, ArgeoNamespace.LDAP_DEFAULT_PREFIX, service, mountPath);
	}

	@Override
	protected Iterator<Content> firstLevel(ProvidedSession session) {
		List<Content> res = new ArrayList<>();
		for (UserDirectory userDirectory : getService().getUserDirectories()) {
			DirectoryContent content = new DirectoryContent(session, DirectoryContentProvider.this, userDirectory);
			res.add(content);
		}
		return res.iterator();
	}

	@Override
	public ProvidedContent get(ProvidedSession session, List<String> segments) {
		String userDirectoryName = segments.get(0);
		UserDirectory userDirectory = null;
		userDirectories: for (UserDirectory ud : getService().getUserDirectories()) {
			if (userDirectoryName.equals(ud.getName())) {
				userDirectory = ud;
				break userDirectories;
			}
		}
		if (userDirectory == null)
			throw new ContentNotFoundException(session, getMountPath() + "/" + ContentUtils.toPath(segments),
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
						getMountPath() + "/" + ContentUtils.toPath(segments) + "/" + pathWithinUserDirectory,
						"Cannot find " + pathWithinUserDirectory + " within " + userDirectoryName);
			return new HierarchyUnitContent(session, this, hierarchyUnit);
		}
	}

	@Override
	public boolean exists(ProvidedSession session, String relativePath) {
		// TODO optimise?
		return exists(session, relativePath);
	}

//	public void setUserManager(CmsUserManager userManager) {
//		this.userManager = userManager;
//	}

	CmsUserManager getUserManager() {
		return getService();
	}

	ServiceContent getRootContent(ProvidedSession session) {
		return new ServiceContent(session);
	}
}
