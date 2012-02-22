package org.argeo.jcr.ui.explorer.commands;

import java.net.URI;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.security.JcrKeyring;
import org.argeo.jcr.ui.explorer.JcrExplorerConstants;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.BundleContext;

/**
 * Connect to a remote repository and, if successful publish it as an OSGi
 * service.
 */
public class AddRemoteRepository extends AbstractHandler implements
		JcrExplorerConstants, ArgeoNames {

	private RepositoryFactory repositoryFactory;
	private BundleContext bundleContext;

	private JcrKeyring keyring;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String uri = null;
		if (event.getParameters().containsKey(PARAM_REPOSITORY_URI)) {
			// FIXME remove this
			uri = event.getParameter(PARAM_REPOSITORY_URI);
			if (uri == null)
				return null;

			try {
				Hashtable<String, String> params = new Hashtable<String, String>();
				params.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, uri);
				// by default we use the URI as alias
				params.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, uri);
				Repository repository = repositoryFactory.getRepository(params);
				bundleContext.registerService(Repository.class.getName(),
						repository, params);
			} catch (Exception e) {
				ErrorFeedback.show("Cannot add remote repository " + uri, e);
			}
		} else {
			RemoteRepositoryLoginDialog dlg = new RemoteRepositoryLoginDialog(
					Display.getDefault().getActiveShell());
			if (dlg.open() == Dialog.OK) {
				// uri = dlg.getUri();
			}
		}

		return null;
	}

	public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setKeyring(JcrKeyring keyring) {
		this.keyring = keyring;
	}

	class RemoteRepositoryLoginDialog extends TitleAreaDialog {
		private Text name;
		private Text uri;
		private Text username;
		private Text password;

		public RemoteRepositoryLoginDialog(Shell parentShell) {
			super(parentShell);
		}

		protected Point getInitialSize() {
			return new Point(600, 400);
		}

		protected Control createDialogArea(Composite parent) {
			Composite dialogarea = (Composite) super.createDialogArea(parent);
			dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true));
			Composite composite = new Composite(dialogarea, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					false));
			setMessage("Login to remote repository", IMessageProvider.NONE);
			name = createLT(composite, "Name", "remoteRepository");
			uri = createLT(composite, "URI",
					"http://localhost:7070/org.argeo.jcr.webapp/remoting/node");
			username = createLT(composite, "User", "");
			password = createLP(composite, "Password");
			parent.pack();
			return composite;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			Button test = createButton(parent, 2, "Test", false);
			test.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent arg0) {
					testConnection();
				}
			});
		}

		void testConnection() {
			Session session = null;
			try {
				URI checkedUri = new URI(uri.getText());
				String checkedUriStr = checkedUri.toString();

				Hashtable<String, String> params = new Hashtable<String, String>();
				params.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, checkedUriStr);
				// by default we use the URI as alias
				params.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS,
						checkedUriStr);
				Repository repository = repositoryFactory.getRepository(params);
				if (username.getText().trim().equals("")) {// anonymous
					session = repository.login();
				} else {
					// FIXME use getTextChars() when upgrading to 3.7
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=297412
					char[] pwd = password.getText().toCharArray();
					SimpleCredentials sc = new SimpleCredentials(
							username.getText(), pwd);
					session = repository.login(sc);
					MessageDialog.openInformation(getParentShell(), "Success",
							"Connection to '" + uri.getText() + "' successful");
				}
			} catch (Exception e) {
				ErrorFeedback.show(
						"Connection test failed for " + uri.getText(), e);
			} finally {
				JcrUtils.logoutQuietly(session);
			}
		}

		@Override
		protected void okPressed() {
			try {
				Session nodeSession = keyring.getSession();
				Node home = JcrUtils.getUserHome(nodeSession);
				Node remote = home.hasNode(ARGEO_REMOTE) ? home
						.getNode(ARGEO_REMOTE) : home.addNode(ARGEO_REMOTE);
				if (remote.hasNode(name.getText()))
					throw new ArgeoException(
							"There is already a remote repository named "
									+ name.getText());
				Node remoteRepository = remote.addNode(name.getText(),
						ArgeoTypes.ARGEO_REMOTE_REPOSITORY);
				remoteRepository.setProperty(ARGEO_URI, uri.getText());
				remoteRepository.setProperty(ARGEO_USER_ID, username.getText());
				Node pwd = remoteRepository.addNode(ARGEO_PASSWORD);
				keyring.set(pwd.getPath(), password.getText().toCharArray());
				nodeSession.save();
				MessageDialog.openInformation(
						getParentShell(),
						"Repository Added",
						"Remote repository '" + username.getText() + "@"
								+ uri.getText() + "' added");
				super.okPressed();
			} catch (Exception e) {
				ErrorFeedback.show("Cannot add remote repository", e);
			}
		}

		/** Creates label and text. */
		protected Text createLT(Composite parent, String label, String initial) {
			new Label(parent, SWT.NONE).setText(label);
			Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			text.setText(initial);
			return text;
		}

		protected Text createLP(Composite parent, String label) {
			new Label(parent, SWT.NONE).setText(label);
			Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER
					| SWT.PASSWORD);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			return text;
		}
	}
}
