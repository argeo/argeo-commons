package org.argeo.cms.e4.parts;

import static org.argeo.cms.ui.util.CmsUiUtils.lbl;
import static org.argeo.cms.ui.util.CmsUiUtils.txt;

import java.security.AccessController;
import java.time.ZonedDateTime;

import javax.annotation.PostConstruct;
import javax.security.auth.Subject;

import org.argeo.cms.auth.CmsSession;
import org.argeo.cms.auth.CurrentUser;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** A canonical view of the logged in user. */
public class EgoDashboard {
	private BundleContext bc = FrameworkUtil.getBundle(EgoDashboard.class).getBundleContext();

	@PostConstruct
	public void createPartControl(Composite p) {
		p.setLayout(new GridLayout());
		String username = CurrentUser.getUsername();

		lbl(p, "<strong>" + CurrentUser.getDisplayName() + "</strong>");
		txt(p, username);
		lbl(p, "Roles:");
		roles: for (String role : CurrentUser.roles()) {
			if (username.equals(role))
				continue roles;
			txt(p, role);
		}

		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject != null) {
			CmsSession cmsSession = CmsSession.getCmsSession(bc, subject);
			ZonedDateTime loggedIndSince = cmsSession.getCreationTime();
			lbl(p, "Session:");
			txt(p, cmsSession.getUuid().toString());
			lbl(p, "Logged in since:");
			txt(p, loggedIndSince.toString());
		}
	}
}
