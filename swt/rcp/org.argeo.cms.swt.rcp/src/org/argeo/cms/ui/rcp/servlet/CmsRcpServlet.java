package org.argeo.cms.ui.rcp.servlet;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsApp;
import org.argeo.cms.ui.rcp.CmsRcpDisplayFactory;

/** Open the related app when called. */
public class CmsRcpServlet extends HttpServlet {
	private static final long serialVersionUID = -3944472431354848923L;
	private final static Logger logger = System.getLogger(CmsRcpServlet.class.getName());

	private CmsApp cmsApp;

	public CmsRcpServlet(CmsApp cmsApp) {
		Objects.requireNonNull(cmsApp);
		this.cmsApp = cmsApp;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		String uiName = path != null ? path.substring(path.lastIndexOf('/') + 1) : "";
		CmsRcpDisplayFactory.openCmsApp(cmsApp, uiName, null);
		logger.log(Level.DEBUG, "Opened RCP UI  " + uiName + " of  CMS App " + req.getServletPath());
	}

}
