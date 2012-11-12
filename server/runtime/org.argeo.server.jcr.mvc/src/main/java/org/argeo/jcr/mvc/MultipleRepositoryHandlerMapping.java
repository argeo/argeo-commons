/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.jcr.mvc;

import java.util.Enumeration;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoJcrUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/** Handles multiple JCR servers with a single servlet. */
public abstract class MultipleRepositoryHandlerMapping implements
		HandlerMapping, ApplicationContextAware, ServletContextAware {
	private final static Log log = LogFactory
			.getLog(MultipleRepositoryHandlerMapping.class);

	// private final static String MKCOL = "MKCOL";

	private ConfigurableApplicationContext applicationContext;
	private ServletContext servletContext;

	// private RepositoryRegister repositoryRegister;
	private RepositoryFactory repositoryFactory;

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
		// List<String> tokens = JcrUtils.tokenize(pathInfo);
		// String[] tokens = extractPrefix(pathInfo);

		// check if repository can be found
		// if (tokens[0] == null || (tokens[1] == null && tokens[0].equals("")))
		// return null;

		// MKCOL on repository or root node doesn't make sense
		// if ((tokens.size() == 1 || tokens.size() == 2)
		// && request.getMethod().equals(MKCOL))
		// return null;

		// String repositoryAlias = extractRepositoryName(tokens);
		String repositoryAlias = extractRepositoryAlias(pathInfo);
		if (repositoryAlias.equals(""))
			return null;
		request.setAttribute(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS,
				repositoryAlias);
		String pathPrefix = request.getServletPath() + '/' + repositoryAlias;
		String beanName = pathPrefix;

		if (!applicationContext.containsBean(beanName)) {
			Repository repository = ArgeoJcrUtils.getRepositoryByAlias(
					repositoryFactory, repositoryAlias);
			// Repository repository = repositoryRegister.getRepositories().get(
			// repositoryAlias);
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

	/** Returns the first two token of the path */
	// protected String[] extractPrefix(String pathInfo) {
	// String[] res = new String[2];
	// StringTokenizer st = new StringTokenizer(pathInfo, "/");
	// if (st.hasMoreTokens())
	// res[0] = st.nextToken();
	// if (st.hasMoreTokens())
	// res[1] = st.nextToken();
	// return res;
	// }

	/** Returns the first token of the path */
	protected String extractRepositoryAlias(String pathInfo) {
		StringBuffer buf = new StringBuffer();
		for (int i = 1; i < pathInfo.length(); i++) {
			char c = pathInfo.charAt(i);
			if (c == '/')
				break;
			buf.append(c);
		}
		return buf.toString();
	}

	/** The repository name is the first part of the path info */
//	protected String extractRepositoryName(List<String> pathTokens) {
//		StringBuffer currName = new StringBuffer("");
//		for (String token : pathTokens) {
//			currName.append(token);
//			if (repositoryRegister.getRepositories().containsKey(
//					currName.toString()))
//				return currName.toString();
//			currName.append('/');
//		}
//		throw new ArgeoException("No repository can be found for request "
//				+ pathTokens);
//	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	// public void setRepositoryRegister(RepositoryRegister repositoryRegister)
	// {
	// this.repositoryRegister = repositoryRegister;
	// }

	public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
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
