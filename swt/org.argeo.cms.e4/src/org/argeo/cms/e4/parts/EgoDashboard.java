package org.argeo.cms.e4.parts;

import java.time.ZonedDateTime;

import javax.annotation.PostConstruct;

import org.argeo.api.cms.CmsSession;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/** A canonical view of the logged in user. */
public class EgoDashboard {
//	private BundleContext bc = FrameworkUtil.getBundle(EgoDashboard.class).getBundleContext();

	@PostConstruct
	public void createPartControl(Composite p) {
		p.setLayout(new GridLayout());
		String username = CurrentUser.getUsername();

		CmsSwtUtils.lbl(p, "<strong>" + CurrentUser.getDisplayName() + "</strong>");
		CmsSwtUtils.txt(p, username);
		CmsSwtUtils.lbl(p, "Roles:");
		roles: for (String role : CurrentUser.roles()) {
			if (username.equals(role))
				continue roles;
			CmsSwtUtils.txt(p, role);
		}

//		Subject subject = Subject.getSubject(AccessController.getContext());
//		if (subject != null) {
		CmsSession cmsSession = CurrentUser.getCmsSession();
		ZonedDateTime loggedIndSince = cmsSession.getCreationTime();
		CmsSwtUtils.lbl(p, "Session:");
		CmsSwtUtils.txt(p, cmsSession.getUuid().toString());
		CmsSwtUtils.lbl(p, "Logged in since:");
		CmsSwtUtils.txt(p, loggedIndSince.toString());
//		}
	}
}
