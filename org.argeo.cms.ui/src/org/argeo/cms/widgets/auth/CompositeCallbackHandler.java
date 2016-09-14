package org.argeo.cms.widgets.auth;

import java.io.IOException;
import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A composite that can populate itself based on {@link Callback}s. It can be
 * used directly as a {@link CallbackHandler} or be used by one by calling the
 * {@link #createCallbackHandlers(Callback[])}.
 * <p>
 * Supported standard {@link Callback}s are:<br>
 * <ul>
 * <li>{@link PasswordCallback}</li>
 * <li>{@link NameCallback}</li>
 * <li>{@link TextOutputCallback}</li>
 * </ul>
 * </p>
 * <p>
 * Supported Argeo {@link Callback}s are:<br>
 * <ul>
 * <li>{@link LocaleChoice}</li>
 * </ul>
 * </p>
 */
public class CompositeCallbackHandler extends Composite implements
		CallbackHandler {
	private static final long serialVersionUID = -928223893722723777L;

	private boolean wasUsedAlready = false;
	private boolean isSubmitted = false;
	private boolean isCanceled = false;

	public CompositeCallbackHandler(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public synchronized void handle(final Callback[] callbacks)
			throws IOException, UnsupportedCallbackException {
		// reset
		if (wasUsedAlready && !isSubmitted() && !isCanceled()) {
			cancel();
			for (Control control : getChildren())
				control.dispose();
			isSubmitted = false;
			isCanceled = false;
		}

		for (Callback callback : callbacks)
			checkCallbackSupported(callback);
		// create controls synchronously in the UI thread
		getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				createCallbackHandlers(callbacks);
			}
		});

		if (!wasUsedAlready)
			wasUsedAlready = true;

//		while (!isSubmitted() && !isCanceled()) {
//			try {
//				wait(1000l);
//			} catch (InterruptedException e) {
//				// silent
//			}
//		}

//		cleanCallbacksAfterCancel(callbacks);
	}

	public void checkCallbackSupported(Callback callback)
			throws UnsupportedCallbackException {
		if (callback instanceof TextOutputCallback
				|| callback instanceof NameCallback
				|| callback instanceof PasswordCallback
				|| callback instanceof LocaleChoice) {
			return;
		} else {
			throw new UnsupportedCallbackException(callback);
		}
	}

	/**
	 * Set writable callbacks to null if the handle is canceled (check is done
	 * by the method)
	 */
	public void cleanCallbacksAfterCancel(Callback[] callbacks) {
		if (isCanceled()) {
			for (Callback callback : callbacks) {
				if (callback instanceof NameCallback) {
					((NameCallback) callback).setName(null);
				} else if (callback instanceof PasswordCallback) {
					PasswordCallback pCallback = (PasswordCallback) callback;
					char[] arr = pCallback.getPassword();
					if (arr != null) {
						Arrays.fill(arr, '*');
						pCallback.setPassword(null);
					}
				}
			}
		}
	}

	public void createCallbackHandlers(Callback[] callbacks) {
		Composite composite = this;
		for (int i = 0; i < callbacks.length; i++) {
			Callback callback = callbacks[i];
			if (callback instanceof TextOutputCallback) {
				createLabelTextoutputHandler(composite,
						(TextOutputCallback) callback);
			} else if (callback instanceof NameCallback) {
				createNameHandler(composite, (NameCallback) callback);
			} else if (callback instanceof PasswordCallback) {
				createPasswordHandler(composite, (PasswordCallback) callback);
			} else if (callback instanceof LocaleChoice) {
				createLocaleHandler(composite, (LocaleChoice) callback);
			}
		}
	}

	protected Text createNameHandler(Composite composite,
			final NameCallback callback) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(callback.getPrompt());
		final Text text = new Text(composite, SWT.SINGLE | SWT.LEAD
				| SWT.BORDER);
		if (callback.getDefaultName() != null) {
			// set default value, if provided
			text.setText(callback.getDefaultName());
			callback.setName(callback.getDefaultName());
		}
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.addModifyListener(new ModifyListener() {
			private static final long serialVersionUID = 7300032545287292973L;

			public void modifyText(ModifyEvent event) {
				callback.setName(text.getText());
			}
		});
		text.addSelectionListener(new SelectionListener() {
			private static final long serialVersionUID = 1820530045857665111L;

			@Override
			public void widgetSelected(SelectionEvent e) {
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				submit();
			}
		});

		text.addKeyListener(new KeyListener() {
			private static final long serialVersionUID = -8698107785092095713L;

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
		return text;
	}

	protected Text createPasswordHandler(Composite composite,
			final PasswordCallback callback) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(callback.getPrompt());
		final Text passwordText = new Text(composite, SWT.SINGLE | SWT.LEAD
				| SWT.PASSWORD | SWT.BORDER);
		passwordText
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		passwordText.addModifyListener(new ModifyListener() {
			private static final long serialVersionUID = -7099363995047686732L;

			public void modifyText(ModifyEvent event) {
				callback.setPassword(passwordText.getTextChars());
			}
		});
		passwordText.addSelectionListener(new SelectionListener() {
			private static final long serialVersionUID = 1820530045857665111L;

			@Override
			public void widgetSelected(SelectionEvent e) {
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				submit();
			}
		});
		return passwordText;
	}

	protected Combo createLocaleHandler(Composite composite,
			final LocaleChoice callback) {
		String[] labels = callback.getSupportedLocalesLabels();
		if (labels.length == 0)
			return null;
		Label label = new Label(composite, SWT.NONE);
		label.setText("Language");

		final Combo combo = new Combo(composite, SWT.READ_ONLY);
		combo.setItems(labels);
		combo.select(callback.getDefaultIndex());
		combo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		combo.addSelectionListener(new SelectionListener() {
			private static final long serialVersionUID = 38678989091946277L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				callback.setSelectedIndex(combo.getSelectionIndex());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		return combo;
	}

	protected Label createLabelTextoutputHandler(Composite composite,
			final TextOutputCallback callback) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(callback.getMessage());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		return label;
		// TODO: find a way to pass this information
		// int messageType = callback.getMessageType();
		// int dialogMessageType = IMessageProvider.NONE;
		// switch (messageType) {
		// case TextOutputCallback.INFORMATION:
		// dialogMessageType = IMessageProvider.INFORMATION;
		// break;
		// case TextOutputCallback.WARNING:
		// dialogMessageType = IMessageProvider.WARNING;
		// break;
		// case TextOutputCallback.ERROR:
		// dialogMessageType = IMessageProvider.ERROR;
		// break;
		// }
		// setMessage(callback.getMessage(), dialogMessageType);
	}

	synchronized boolean isSubmitted() {
		return isSubmitted;
	}

	synchronized boolean isCanceled() {
		return isCanceled;
	}

	protected synchronized void submit() {
		isSubmitted = true;
		notifyAll();
	}

	protected synchronized void cancel() {
		isCanceled = true;
		notifyAll();
	}
}
