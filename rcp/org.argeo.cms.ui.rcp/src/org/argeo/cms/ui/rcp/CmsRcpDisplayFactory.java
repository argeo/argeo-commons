package org.argeo.cms.ui.rcp;

import java.nio.file.Path;

import org.argeo.api.cms.CmsApp;
import org.argeo.util.OS;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.EventAdmin;

/** Creates the SWT {@link Display} in a dedicated thread. */
public class CmsRcpDisplayFactory {
	/** File name in a run directory */
	private final static String ARGEO_RCP_URL = "argeo.rcp.url";

	/** There is only one display in RCP mode */
	private static Display display;

	private CmsUiThread uiThread;

	private boolean shutdown = false;

	public void init() {
		uiThread = new CmsUiThread();
		uiThread.start();

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
			display = Display.getDefault();

//			for (String contextName : cmsApps.keySet()) {
//				openCmsApp(contextName);
//			}

			while (!shutdown) {
				if (!display.readAndDispatch())
					display.sleep();
			}
			display.dispose();
			display = null;
		}
	}

	public static Display getDisplay() {
		return display;
	}

	public static void openCmsApp(EventAdmin eventAdmin, CmsApp cmsApp, String uiName) {
		CmsRcpDisplayFactory.getDisplay().syncExec(() -> {
			CmsRcpApp cmsRcpApp = new CmsRcpApp(uiName);
			cmsRcpApp.setEventAdmin(eventAdmin);
			cmsRcpApp.setCmsApp(cmsApp, null);
			cmsRcpApp.initRcpApp();
		});
	}

	public static Path getUrlRunFile() {
		return OS.getRunDir().resolve(CmsRcpDisplayFactory.ARGEO_RCP_URL);
	}
}
