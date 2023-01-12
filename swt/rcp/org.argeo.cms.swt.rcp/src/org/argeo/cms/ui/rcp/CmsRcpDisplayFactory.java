package org.argeo.cms.ui.rcp;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;

import org.argeo.api.cms.CmsApp;
import org.argeo.cms.util.OS;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;

/** Creates the SWT {@link Display} in a dedicated thread. */
public class CmsRcpDisplayFactory {
	private final static Logger logger = System.getLogger(CmsRcpDisplayFactory.class.getName());

	/** File name in a run directory */
	private final static String ARGEO_RCP_URL = "argeo.rcp.url";

	/** There is only one display in RCP mode */
	private static Display display;

	private CmsUiThread uiThread;

	private boolean shutdown = false;

	public void init() {
		uiThread = new CmsUiThread();
		uiThread.start();
		while (display == null)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// silent
			}
	}

	public void destroy() {
		shutdown = true;
		display.wake();
		try {
			uiThread.join();
		} catch (InterruptedException e) {
			// silent
		} finally {
			uiThread = null;
		}
	}

	class CmsUiThread extends Thread {

		public CmsUiThread() {
			super("CMS UI");
		}

		@Override
		public void run() {
			try {
				display = Display.getDefault();
				display.setRuntimeExceptionHandler((e) -> e.printStackTrace());
				display.setErrorHandler((e) -> e.printStackTrace());

				while (!shutdown) {
					if (!display.readAndDispatch())
						display.sleep();
				}
				display.dispose();
				display = null;
			} catch (UnsatisfiedLinkError e) {
				logger.log(Level.ERROR,
						"Cannot load SWT, either because the SWT DLLs are no in the java.library.path,"
								+ " or because the OSGi framework has been refreshed." + " Restart the application.",
						e);
			}
		}
	}

	public static Display getDisplay() {
		return display;
	}

	public static void openCmsApp(CmsApp cmsApp, String uiName, DisposeListener disposeListener) {
		CmsRcpDisplayFactory.getDisplay().syncExec(() -> {
			CmsRcpApp cmsRcpApp = new CmsRcpApp(uiName);
			cmsRcpApp.setCmsApp(cmsApp, null);
			cmsRcpApp.initRcpApp();
			if (disposeListener != null)
				cmsRcpApp.getShell().addDisposeListener(disposeListener);
		});
	}

	public static Path getUrlRunFile() {
		return OS.getRunDir().resolve(CmsRcpDisplayFactory.ARGEO_RCP_URL);
	}
}
