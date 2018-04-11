package org.argeo.cms.e4.rap;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.cms.ui.CmsView;
import org.argeo.cms.ui.UxContext;
import org.argeo.cms.util.SimpleUxContext;
import org.argeo.cms.widgets.auth.CmsLoginShell;
import org.argeo.node.NodeConstants;
import org.eclipse.rap.e4.E4ApplicationConfig;
import org.eclipse.rap.e4.E4EntryPointFactory;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.widgets.Display;

public class CmsE4EntryPointFactory extends E4EntryPointFactory {

	public CmsE4EntryPointFactory(E4ApplicationConfig config) {
		super(config);
	}

	@Override
	public EntryPoint create() {
		// Subject subject = new Subject();
		EntryPoint ep = createEntryPoint();
		EntryPoint authEp = new EntryPoint() {

			@Override
			public int createUI() {
				Subject subject = new Subject();
//				boolean success = login(subject);
//				if (success)
					return Subject.doAs(subject, new PrivilegedAction<Integer>() {

						@Override
						public Integer run() {
//							RWT.setLocale(Locale.FRENCH);
							System.out.println("createUI");
							return ep.createUI();
						}

					});
//				else
//					return 1;
			}
		};
		return authEp;
	}

	protected EntryPoint createEntryPoint() {
		return super.create();
	}

	boolean login(Subject subject) {
		// Subject subject = Subject.getSubject(AccessController.getContext());
		Display display = new Display();
		CmsView cmsView = new E4CmsView(subject);
		CmsLoginShell loginShell = new CmsLoginShell(cmsView);
		loginShell.setSubject(subject);
		try {
			// try pre-auth
			LoginContext loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, subject, loginShell);
			loginContext.login();
		} catch (LoginException e) {
			loginShell.createUi();
			loginShell.open();

			while (!loginShell.getShell().isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} finally {
			display.dispose();
		}
		if (CurrentUser.getUsername(subject) == null)
			return false;
		return true;
	}

	class E4CmsView implements CmsView {
		private LoginContext loginContext;
		private UxContext uxContext;
		private Subject subject;

		public E4CmsView(Subject subject) {
			this.subject = subject;
			uxContext = new SimpleUxContext();
		}

		@Override
		public UxContext getUxContext() {
			return uxContext;
		}

		@Override
		public void navigateTo(String state) {
			// TODO Auto-generated method stub

		}

		@Override
		public void authChange(LoginContext loginContext) {
			if (loginContext == null)
				throw new CmsException("Login context cannot be null");
			// logout previous login context
			// if (this.loginContext != null)
			// try {
			// this.loginContext.logout();
			// } catch (LoginException e1) {
			// System.err.println("Could not log out: " + e1);
			// }
			this.loginContext = loginContext;
		}

		@Override
		public void logout() {
			if (loginContext == null)
				throw new CmsException("Login context should not bet null");
			try {
				CurrentUser.logoutCmsSession(loginContext.getSubject());
				loginContext.logout();
			} catch (LoginException e) {
				throw new CmsException("Cannot log out", e);
			}
		}

		@Override
		public void exception(Throwable e) {
			// TODO Auto-generated method stub

		}

		@Override
		public CmsImageManager getImageManager() {
			// TODO Auto-generated method stub
			return null;
		}

		protected Subject getSubject() {
			return subject;
		}

		@Override
		public boolean isAnonymous() {
			return CurrentUser.isAnonymous(getSubject());
		}
	}
}
