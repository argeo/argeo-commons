package org.argeo.jcr.mvc;

import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.RepositoryRegister;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

public abstract class MultipleRepositoryHandlerMapping implements
		HandlerMapping, ApplicationContextAware, ServletContextAware {
	private final static Log log = LogFactory
			.getLog(MultipleRepositoryHandlerMapping.class);

	private final static String MKCOL = "MKCOL";

	private ConfigurableApplicationContext applicationContext;
	private ServletContext servletContext;

	private RepositoryRegister repositoryRegister;

	/** Actually creates the servlet to be registered. */
	protected abstract HttpServlet createServlet(Repository repository,
			String pathPrefix) throws ServletException;

	public HandlerExecutionChain getHandler(HttpServletRequest request)
			throws Exception {
		if (log.isTraceEnabled()) {
			log.trace("getContextPath=" + request.getContextPath());
			log.trace("getServletPath=" + request.getServletPath());
			log.trace("getPathInfo=" + request.getPathInfo());
		}

		String pathInfo = request.getPathInfo();

		// tokenize path
		List<String> tokens = JcrUtils.tokenize(pathInfo);

		// check if repository can be found
		if (tokens.size() == 0
				|| (tokens.size() == 1 && tokens.get(0).equals("")))
			return null;
		// MKCOL on repository or root node doesn't make sense
		if ((tokens.size() == 1 || tokens.size() == 2)
				&& request.getMethod().equals(MKCOL))
			return null;
		String repositoryName = extractRepositoryName(tokens);
		String pathPrefix = request.getServletPath() + '/' + repositoryName;
		String beanName = pathPrefix;

		if (!applicationContext.containsBean(beanName)) {
			Repository repository = repositoryRegister.getRepositories().get(
					repositoryName);
			HttpServlet servlet = createServlet(repository, pathPrefix);
			applicationContext.getBeanFactory().registerSingleton(beanName,
					servlet);
			// TODO: unregister it as well
		}
		HttpServlet remotingServlet = (HttpServlet) applicationContext
				.getBean(beanName);
		HandlerExecutionChain hec = new HandlerExecutionChain(remotingServlet);
		return hec;
	}

	/** The repository name is the first part of the path info */
	protected String extractRepositoryName(List<String> pathTokens) {
		StringBuffer currName = new StringBuffer("");
		for (String token : pathTokens) {
			currName.append(token);
			if (repositoryRegister.getRepositories().containsKey(
					currName.toString()))
				return currName.toString();
			currName.append('/');
		}
		throw new ArgeoException("No repository can be found for request "
				+ pathTokens);
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setRepositoryRegister(RepositoryRegister repositoryRegister) {
		this.repositoryRegister = repositoryRegister;
	}

	protected class DelegatingServletConfig implements ServletConfig {
		private String name;
		private Properties initParameters;

		public DelegatingServletConfig(String name, Properties initParameters) {
			super();
			this.name = name;
			this.initParameters = initParameters;
		}

		public String getServletName() {
			return name;
		}

		public ServletContext getServletContext() {
			return servletContext;
		}

		public String getInitParameter(String paramName) {
			return initParameters.getProperty(paramName);
		}

		@SuppressWarnings("rawtypes")
		public Enumeration getInitParameterNames() {
			return initParameters.keys();
		}
	}
}
