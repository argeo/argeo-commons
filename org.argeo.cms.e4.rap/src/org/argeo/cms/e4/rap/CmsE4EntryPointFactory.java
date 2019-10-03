package org.argeo.cms.e4.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.eclipse.rap.e4.E4ApplicationConfig;
import org.eclipse.rap.e4.E4EntryPointFactory;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;

public class CmsE4EntryPointFactory extends E4EntryPointFactory {
	public final static String DEFAULT_LIFECYCLE_URI = "bundleclass://org.argeo.cms.e4.rap/org.argeo.cms.e4.rap.CmsLoginLifecycle";

	public CmsE4EntryPointFactory(E4ApplicationConfig config) {
		super(config);
	}

	public CmsE4EntryPointFactory(String e4Xmi, String lifeCycleUri) {
		super(defaultConfig(e4Xmi, lifeCycleUri));
	}

	public CmsE4EntryPointFactory(String e4Xmi) {
		this(e4Xmi, DEFAULT_LIFECYCLE_URI);
	}

	public static E4ApplicationConfig defaultConfig(String e4Xmi, String lifeCycleUri) {
		E4ApplicationConfig config = new E4ApplicationConfig(e4Xmi, lifeCycleUri, null, null, false, true, true);
		return config;
	}

	@Override
	public EntryPoint create() {
		EntryPoint ep = createEntryPoint();
		EntryPoint authEp = new EntryPoint() {

			@Override
			public int createUI() {
				Subject subject = new Subject();
				return Subject.doAs(subject, new PrivilegedAction<Integer>() {

					@Override
					public Integer run() {
						// SPNEGO
						// HttpServletRequest request = RWT.getRequest();
						// String authorization = request.getHeader(HEADER_AUTHORIZATION);
						// if (authorization == null || !authorization.startsWith("Negotiate")) {
						// HttpServletResponse response = RWT.getResponse();
						// response.setStatus(401);
						// response.setHeader(HEADER_WWW_AUTHENTICATE, "Negotiate");
						// response.setDateHeader("Date", System.currentTimeMillis());
						// response.setDateHeader("Expires", System.currentTimeMillis() + (24 * 60 * 60
						// * 1000));
						// response.setHeader("Accept-Ranges", "bytes");
						// response.setHeader("Connection", "Keep-Alive");
						// response.setHeader("Keep-Alive", "timeout=5, max=97");
						// // response.setContentType("text/html; charset=UTF-8");
						// }

						JavaScriptExecutor jsExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
						Integer exitCode = ep.createUI();
						jsExecutor.execute("location.reload()");
						return exitCode;
					}

				});
			}
		};
		return authEp;
	}

	protected EntryPoint createEntryPoint() {
		return super.create();
	}
}
