package org.argeo.internal.swt.specific.osgi;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SwtSpecificRcpActivator implements BundleActivator {
	private static Display display;
	private static Shell rootShell;
	private static UiThread uiThread;

	private static boolean debug = true;

	@Override
	public void start(BundleContext context) throws Exception {
		if (display != null)
			throw new IllegalStateException("SWT display already exists");
		uiThread = new UiThread();
		uiThread.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (display == null)
			return; // TODO log issue
		display.asyncExec(() -> rootShell.dispose());
		display.wake();
		uiThread.join(60 * 1000);
		uiThread = null;
		display = null;
	}

	static class UiThread extends Thread {

		@Override
		public void run() {
			boolean displayOwner = true;
			Display d = Display.getDefault();
			if (d.getThread() != UiThread.this) {
				displayOwner = false;
				// throw new IllegalStateException("There was already a default SWT display");
			}

			d.syncExec(() -> {
				try {
					rootShell = new Shell(d);
				} catch (SWTError e) {
					e.printStackTrace();
					display.dispose();
					return;
				}

				if (debug) {
					rootShell.setLayout(new FillLayout());
					new Label(rootShell, 0).setText("ROOT SHELL");
					rootShell.pack();
					rootShell.open();
				}
			});

			display = d;

			if (displayOwner) {
				while (!rootShell.isDisposed())
					if (!display.readAndDispatch())
						display.sleep();
				display.dispose();
			}
		}
	}
}
