/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.security.ui.dialogs;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.ui.SecurityUiPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Base for login dialogs */
public abstract class AbstractLoginDialog extends TrayDialog implements
		CallbackHandler {

	private final static Log log = LogFactory.getLog(AbstractLoginDialog.class);

	private Thread modalContextThread = null;
	boolean processCallbacks = false;
	boolean isCancelled = false;
	Callback[] callbackArray;

	protected final Callback[] getCallbacks() {
		return this.callbackArray;
	}

	public abstract void internalHandle();

	public boolean isCancelled() {
		return isCancelled;
	}

	protected AbstractLoginDialog(Shell parentShell) {
		super(parentShell);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.security.auth.callback.CallbackHandler#handle(javax.security.auth
	 * .callback.Callback[])
	 */
	public void handle(final Callback[] callbacks) throws IOException {
		// clean previous usage
		if (processCallbacks) {
			// this handler was already used
			processCallbacks = false;
		}

		if (modalContextThread != null) {
			try {
				modalContextThread.join(1000);
			} catch (InterruptedException e) {
				// silent
			}
			modalContextThread = null;
		}

		// initialize
		this.callbackArray = callbacks;
		final Display display = Display.getDefault();
		display.syncExec(new Runnable() {

			public void run() {
				isCancelled = false;
				setBlockOnOpen(false);
				open();

				final Button okButton = getButton(IDialogConstants.OK_ID);
				okButton.setText("Login");
				okButton.addSelectionListener(new SelectionListener() {

					public void widgetSelected(final SelectionEvent event) {
						processCallbacks = true;
					}

					public void widgetDefaultSelected(final SelectionEvent event) {
						// nothing to do
					}
				});
				final Button cancel = getButton(IDialogConstants.CANCEL_ID);
				cancel.addSelectionListener(new SelectionListener() {

					public void widgetSelected(final SelectionEvent event) {
						isCancelled = true;
						processCallbacks = true;
					}

					public void widgetDefaultSelected(final SelectionEvent event) {
						// nothing to do
					}
				});
			}
		});
		try {
			ModalContext.setAllowReadAndDispatch(true); // Works for now.
			ModalContext.run(new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) {
					modalContextThread = Thread.currentThread();
					// Wait here until OK or cancel is pressed, then let it rip.
					// The event
					// listener
					// is responsible for closing the dialog (in the
					// loginSucceeded
					// event).
					while (!processCallbacks && (modalContextThread != null)
							&& (modalContextThread == Thread.currentThread())
							&& SecurityUiPlugin.getDefault() != null) {
						// Note: SecurityUiPlugin.getDefault() != null is false
						// when the OSGi runtime is shut down
						try {
							Thread.sleep(100);
							// if (display.isDisposed()) {
							// log.warn("Display is disposed, killing login dialog thread");
							// throw new ThreadDeath();
							// }
						} catch (final Exception e) {
							// do nothing
						}
					}
					processCallbacks = false;
					// Call the adapter to handle the callbacks
					if (!isCancelled())
						internalHandle();
					else
						// clear callbacks are when cancelling
						for (Callback callback : callbacks)
							if (callback instanceof PasswordCallback)
								((PasswordCallback) callback).setPassword(null);
							else if (callback instanceof NameCallback)
								((NameCallback) callback).setName(null);
				}
			}, true, new NullProgressMonitor(), Display.getDefault());
		} catch (ThreadDeath e) {
			isCancelled = true;
			log.debug("Thread " + Thread.currentThread().getId() + " died");
			throw e;
		} catch (Exception e) {
			isCancelled = true;
			IOException ioe = new IOException(
					"Unexpected issue in login dialog, see root cause for more details");
			ioe.initCause(e);
			throw ioe;
		} finally {
			// so that the modal thread dies
			processCallbacks = true;
			// try {
			// // wait for the modal context thread to gracefully exit
			// modalContextThread.join();
			// } catch (InterruptedException ie) {
			// // silent
			// }
			modalContextThread = null;
		}
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Authentication");
	}
}
