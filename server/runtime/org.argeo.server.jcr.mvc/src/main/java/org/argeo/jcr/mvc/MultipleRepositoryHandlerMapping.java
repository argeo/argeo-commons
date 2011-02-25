package org.argeo.jcr.mvc;

import java.util.Enumeration;
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

		String repositoryName = extractRepositoryName(request);
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
		return new HandlerExecutionChain(remotingServlet);
	}

	/** The repository name is the first part of the path info */
	protected String extractRepositoryName(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		// TODO: optimize by checking character by character
		String[] tokens = pathInfo.split("/");
		StringBuffer currName = new StringBuffer("");
		tokens: for (String token : tokens) {
			if (token.equals(""))
				continue tokens;
			currName.append(token);
			if (repositoryRegister.getRepositories().containsKey(
					currName.toString()))
				return currName.toString();
			currName.append('/');
		}
		throw new ArgeoException("No repository can be found for request "
				+ pathInfo);
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
