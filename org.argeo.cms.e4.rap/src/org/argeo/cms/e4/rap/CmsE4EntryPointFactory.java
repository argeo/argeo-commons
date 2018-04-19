package org.argeo.cms.e4.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.eclipse.rap.e4.E4ApplicationConfig;
import org.eclipse.rap.e4.E4EntryPointFactory;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;

public class CmsE4EntryPointFactory extends E4EntryPointFactory {

	public CmsE4EntryPointFactory(E4ApplicationConfig config) {
		super(config);
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

	// private boolean login(Subject subject) {
	// Display display = new Display();
	// CmsView cmsView = new E4CmsView(subject);
	// CmsLoginShell loginShell = new CmsLoginShell(cmsView);
	// loginShell.setSubject(subject);
	// try {
	// // try pre-auth
	// LoginContext loginContext = new
	// LoginContext(NodeConstants.LOGIN_CONTEXT_USER, subject, loginShell);
	// loginContext.login();
	// } catch (LoginException e) {
	// loginShell.createUi();
	// loginShell.open();
	//
	// while (!loginShell.getShell().isDisposed()) {
	// if (!display.readAndDispatch())
	// display.sleep();
	// }
	// } finally {
	// display.dispose();
	// }
	// if (CurrentUser.getUsername(subject) == null)
	// return false;
	// return true;
	// }
	//
	// private class E4CmsView implements CmsView {
	// private LoginContext loginContext;
	// private UxContext uxContext;
	// private Subject subject;
	//
	// public E4CmsView(Subject subject) {
	// this.subject = subject;
	// uxContext = new SimpleUxContext();
	// }
	//
	// @Override
	// public UxContext getUxContext() {
	// return uxContext;
	// }
	//
	// @Override
	// public void navigateTo(String state) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void authChange(LoginContext loginContext) {
	// if (loginContext == null)
	// throw new CmsException("Login context cannot be null");
	// // logout previous login context
	// // if (this.loginContext != null)
	// // try {
	// // this.loginContext.logout();
	// // } catch (LoginException e1) {
	// // System.err.println("Could not log out: " + e1);
	// // }
	// this.loginContext = loginContext;
	// }
	//
	// @Override
	// public void logout() {
	// if (loginContext == null)
	// throw new CmsException("Login context should not bet null");
	// try {
	// CurrentUser.logoutCmsSession(loginContext.getSubject());
	// loginContext.logout();
	// } catch (LoginException e) {
	// throw new CmsException("Cannot log out", e);
	// }
	// }
	//
	// @Override
	// public void exception(Throwable e) {
	// log.error("Unexpected exception in Eclipse 4 RAP", e);
	// }
	//
	// @Override
	// public CmsImageManager getImageManager() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// protected Subject getSubject() {
	// return subject;
	// }
	//
	// @Override
	// public boolean isAnonymous() {
	// return CurrentUser.isAnonymous(getSubject());
	// }
	// }
}
