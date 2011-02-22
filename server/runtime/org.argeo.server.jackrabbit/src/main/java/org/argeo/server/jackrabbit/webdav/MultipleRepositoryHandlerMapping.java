package org.argeo.server.jackrabbit.webdav;

import java.util.Enumeration;
import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.argeo.jcr.RepositoryRegister;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.ServletWrappingController;

public class MultipleRepositoryHandlerMapping implements HandlerMapping,
		ApplicationContextAware, ServletContextAware {
	private final static Log log = LogFactory
			.getLog(MultipleRepositoryHandlerMapping.class);

	private ConfigurableApplicationContext applicationContext;
	private ServletContext servletContext;

	private RepositoryRegister repositoryRegister;

	public HandlerExecutionChain getHandler(HttpServletRequest request)
			throws Exception {
		log.debug(request);
		log.debug("getContextPath=" + request.getContextPath());
		log.debug("getServletPath=" + request.getServletPath());
		log.debug("getPathInfo=" + request.getPathInfo());

		String repositoryName = "repo";
		String pathPrefix = "/remoting/repo";
		String beanName = "remoting_" + repositoryName;

		if (!applicationContext.containsBean(beanName)) {
			Repository repository = repositoryRegister.getRepositories().get(
					repositoryName);
			JcrRemotingServlet jcrRemotingServlet = new JcrRemotingServlet(
					repository);
			Properties initParameters = new Properties();
			initParameters.setProperty(
					JCRWebdavServerServlet.INIT_PARAM_RESOURCE_PATH_PREFIX,
					pathPrefix);
			jcrRemotingServlet.init(new DelegatingServletConfig(beanName,
					initParameters));
			applicationContext.getBeanFactory().registerSingleton(beanName,
					jcrRemotingServlet);
		}
		HttpServlet remotingServlet = (HttpServlet) applicationContext
				.getBean(beanName);
		return new HandlerExecutionChain(remotingServlet);
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

	private class DelegatingServletConfig implements ServletConfig {
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

		public Enumeration getInitParameterNames() {
			return initParameters.keys();
		}
	}

}
