package org.argeo.cms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

/** Gateway for user login, can also generate the related UI. */
public class CmsLogin {
	private final static Log log = LogFactory.getLog(CmsLogin.class);
	private AuthenticationManager authenticationManager;
	private String systemKey = "argeo";

	protected void logInAsAnonymous() {
		// TODO Better deal with anonymous authentication
		try {
			GrantedAuthority[] anonAuthorities = { new GrantedAuthorityImpl(
					"ROLE_ANONYMOUS") };
			UserDetails anonUser = new User("anonymous", "", true, true, true,
					true, anonAuthorities);
			AnonymousAuthenticationToken anonToken = new AnonymousAuthenticationToken(
					systemKey, anonUser, anonAuthorities);
			Authentication authentication = authenticationManager
					.authenticate(anonToken);
			SecurityContextHolder.getContext()
					.setAuthentication(authentication);
		} catch (Exception e) {
			throw new CmsException("Cannot authenticate", e);
		}
	}

	protected void logInWithPassword(String username, char[] password) {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				username, new String(password));
		Authentication authentication = authenticationManager
				.authenticate(token);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		if (log.isDebugEnabled())
			log.debug("Authenticated as " + authentication);
	}

	/*
	 * UI
	 */

	// @Override
	// public Control createUi(Composite parent, Node context)
	// throws RepositoryException {
	// Composite comp = new Composite(parent, SWT.NONE);
	// comp.setLayout(new GridLayout(1, true));
	// comp.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_LOGIN);
	// refreshUi(comp);
	// return comp;
	// }

	// protected void refreshUi(Composite comp) {
	// String username = SecurityContextHolder.getContext()
	// .getAuthentication().getName();
	// if (username.equals("anonymous"))
	// username = null;
	//
	// for (Control child : comp.getChildren()) {
	// child.dispose();
	// }
	//
	// Label l = new Label(comp, SWT.NONE);
	// l.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_LOGIN);
	// l.setData(RWT.MARKUP_ENABLED, true);
	// l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
	// if (username != null) {
	// l.setText("<b>" + username + "</b>");
	// l.addMouseListener(new UserListener());
	// } else {
	// l.setText("Log in");
	// l.addMouseListener(new LoginListener());
	// }
	//
	// comp.pack();
	// }

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setSystemKey(String systemKey) {
		this.systemKey = systemKey;
	}

	// private class UserListener extends MouseAdapter {
	// private static final long serialVersionUID = -3565359775509786183L;
	// private Control source;
	// private Shell dialog;
	//
	// @Override
	// public void mouseDown(MouseEvent e) {
	// source = ((Control) e.widget);
	// if (dialog != null) {
	// dialog.close();
	// dialog.dispose();
	// dialog = null;
	// } else {
	// dialog = createDialog(source);
	// }
	// }
	//
	// @SuppressWarnings("serial")
	// protected Shell createDialog(Control source) {
	// Shell dialog = new Shell(source.getDisplay(), SWT.NO_TRIM
	// | SWT.BORDER | SWT.ON_TOP);
	// dialog.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);
	// dialog.setLayout(new GridLayout(1, false));
	//
	// final CmsSession cmsSession = (CmsSession) source.getDisplay()
	// .getData(CmsSession.KEY);
	//
	// Label l = new Label(dialog, SWT.NONE);
	// l.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU_ITEM);
	// l.setText("Log out");
	// GridData lData = new GridData(SWT.FILL, SWT.FILL, true, false);
	// lData.widthHint = 120;
	// l.setLayoutData(lData);
	//
	// l.addMouseListener(new MouseAdapter() {
	// public void mouseDown(MouseEvent e) {
	// SecurityContextHolder.getContext().setAuthentication(null);
	// UserListener.this.dialog.close();
	// UserListener.this.dialog.dispose();
	// cmsSession.authChange();
	// }
	// });
	//
	// dialog.pack();
	// dialog.layout();
	// dialog.setLocation(source.toDisplay(
	// source.getSize().x - dialog.getSize().x, source.getSize().y));
	// dialog.open();
	// return dialog;
	// }
	// }
	//
	// private class LoginListener extends MouseAdapter {
	// private static final long serialVersionUID = 677115566708451462L;
	// private Control source;
	// private Shell dialog;
	//
	// @Override
	// public void mouseDown(MouseEvent e) {
	// source = ((Control) e.widget);
	// if (dialog != null) {
	// dialog.close();
	// dialog.dispose();
	// dialog = null;
	// } else {
	// dialog = createDialog(source);
	// }
	// }
	//
	// @SuppressWarnings("serial")
	// protected Shell createDialog(Control source) {
	// Integer textWidth = 150;
	// Shell dialog = new Shell(source.getDisplay(), SWT.NO_TRIM
	// | SWT.BORDER | SWT.ON_TOP);
	// dialog.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG);
	// dialog.setLayout(new GridLayout(2, false));
	//
	// new Label(dialog, SWT.NONE).setText("Username");
	// final Text username = new Text(dialog, SWT.BORDER);
	// username.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_USERNAME);
	// GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
	// gd.widthHint = textWidth;
	// username.setLayoutData(gd);
	//
	// new Label(dialog, SWT.NONE).setText("Password");
	// final Text password = new Text(dialog, SWT.BORDER | SWT.PASSWORD);
	// password.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_PASSWORD);
	// gd = new GridData(SWT.FILL, SWT.FILL, true, false);
	// gd.widthHint = textWidth;
	// password.setLayoutData(gd);
	//
	// dialog.pack();
	// dialog.layout();
	// dialog.setLocation(source.toDisplay(
	// source.getSize().x - dialog.getSize().x, source.getSize().y));
	// dialog.open();
	//
	// // Listeners
	// TraverseListener tl = new TraverseListener() {
	// public void keyTraversed(TraverseEvent e) {
	// if (e.detail == SWT.TRAVERSE_RETURN)
	// login(username.getText(), password.getTextChars());
	// }
	// };
	// username.addTraverseListener(tl);
	// password.addTraverseListener(tl);
	// return dialog;
	// }
	//
	// protected void login(String username, char[] password) {
	// CmsSession cmsSession = (CmsSession) source.getDisplay().getData(
	// CmsSession.KEY);
	// logInWithPassword(username, password);
	// dialog.close();
	// dialog.dispose();
	// refreshUi(source.getParent());
	// cmsSession.authChange();
	// }
	//
	// }
}
