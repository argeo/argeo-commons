package org.argeo.cms.e4;

import org.argeo.cms.swt.CmsException;
import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/** An Eclipse 4 {@link ContextFunction} based on an OSGi filter. */
public class OsgiFilterContextFunction extends ContextFunction {

	private BundleContext bc = FrameworkUtil.getBundle(OsgiFilterContextFunction.class).getBundleContext();

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		ServiceReference<?>[] srs;
		try {
			srs = bc.getServiceReferences((String) null, contextKey);
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Context key " + contextKey + " must be a valid osgi filter", e);
		}
		if (srs == null || srs.length == 0) {
			return IInjector.NOT_A_VALUE;
		} else {
			// return the first one
			return bc.getService(srs[0]);
		}
	}

}
