package org.argeo.security.ui.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;

public class RapSecureWorkbenchWindowAdvisor extends
		SecureWorkbenchWindowAdvisor {
	private final static Log log = LogFactory
			.getLog(RapSecureWorkbenchWindowAdvisor.class);

	public RapSecureWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new SecureActionBarAdvisor(configurer, false);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		// configurer.setInitialSize(new Point(800, 600));

//		if (log.isDebugEnabled())
//			log.debug("CHAR ENCODING"
//					+ System.getProperty("file.encoding"));
		configurer.setShowCoolBar(true);
		configurer.setShowMenuBar(true);
		configurer.setShowStatusLine(false);
		configurer.setShowPerspectiveBar(true);
		configurer.setTitle("Argeo Secure UI"); //$NON-NLS-1$
		// Full screen, see
		// http://dev.eclipse.org/newslists/news.eclipse.technology.rap/msg02697.html
		configurer.setShellStyle(SWT.NONE);
		Rectangle bounds = Display.getDefault().getBounds();
		configurer.setInitialSize(new Point(bounds.width, bounds.height));
	}

}
