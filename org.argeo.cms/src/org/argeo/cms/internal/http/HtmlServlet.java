package org.argeo.cms.internal.http;

import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argeo.cms.CmsException;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.fm.jcr.JcrModel;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.NodeConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class HtmlServlet extends HttpServlet {
	private static final long serialVersionUID = 2083925371199357045L;
	static String base = System.getProperty("user.home") + File.separator + "dev" + File.separator + "work"
			+ File.separator + "ftl";
	static Configuration cfg;
	static {
		try {
			cfg = new Configuration(Configuration.VERSION_2_3_28);
			cfg.setDirectoryForTemplateLoading(new File(base));
			cfg.setDefaultEncoding("UTF-8");
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
			cfg.setLogTemplateExceptions(false);
			cfg.setWrapUncheckedExceptions(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
	private ServiceTracker<Repository, Repository> repositorySt;

	private Repository repository;

	@Override
	public void init() throws ServletException {
		repositorySt = new ServiceTracker<Repository, Repository>(bc, Repository.class, null) {

			@Override
			public Repository addingService(ServiceReference<Repository> reference) {
				String cn = reference.getProperty(NodeConstants.CN).toString();
				Repository repo = super.addingService(reference);
				if (NodeConstants.NODE.equals(cn))
					repository = repo;
				return repo;
			}

		};
		repositorySt.open();
	}

	@Override
	public void destroy() {
		if (repositorySt != null)
			repositorySt.close();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		String servletPath = req.getServletPath();
		String[] p = path.split("/");

		String basePath =servletPath+'/'+p[1];
		String template = p[1] + ".ftl";
		StringBuilder sb = new StringBuilder();
		for (int i = 2; i < p.length; i++)
			sb.append('/').append(p[i]);

		Session session = null;
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
					new HttpRequestCallbackHandler(req, resp));
			lc.login();
			session = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<Session>() {

				@Override
				public Session run() throws Exception {
					Session session = repository.login();
					return session;
				}
			});

			Node node = session.getNode(sb.toString());

			Template t = cfg.getTemplate(template);
			Map<String, Object> root = new HashMap<>();
			root.put("node", new JcrModel(node));
			root.put("basePath", new SimpleScalar(basePath));

			t.process(root, resp.getWriter());

			resp.setContentType("text/html");
		} catch (Exception e) {
			throw new CmsException("Cannot log in", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

}
