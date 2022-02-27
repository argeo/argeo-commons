package org.argeo.cms.osgi;

import java.util.Collection;

import javax.security.auth.Subject;

import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.CmsSessionId;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class CmsOsgiUtils {

	/** @return The {@link CmsSession} for this {@link Subject} or null. */
	public static CmsSession getCmsSession(BundleContext bc, Subject subject) {
		if (subject.getPrivateCredentials(CmsSessionId.class).isEmpty())
			return null;
		CmsSessionId cmsSessionId = subject.getPrivateCredentials(CmsSessionId.class).iterator().next();
		String uuid = cmsSessionId.getUuid().toString();
		Collection<ServiceReference<CmsSession>> sr;
		try {
			sr = bc.getServiceReferences(CmsSession.class, "(" + CmsSession.SESSION_UUID + "=" + uuid + ")");
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot get CMS session for uuid " + uuid, e);
		}
		ServiceReference<CmsSession> cmsSessionRef;
		if (sr.size() == 1) {
			cmsSessionRef = sr.iterator().next();
			return bc.getService(cmsSessionRef);
		} else if (sr.size() == 0) {
			return null;
		} else
			throw new IllegalStateException(sr.size() + " CMS sessions registered for " + uuid);
	}

	/** Singleton.*/
	private CmsOsgiUtils() {
	}
}
