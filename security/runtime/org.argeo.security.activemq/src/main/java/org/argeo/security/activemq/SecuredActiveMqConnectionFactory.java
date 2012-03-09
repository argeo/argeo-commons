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
package org.argeo.security.activemq;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

public class SecuredActiveMqConnectionFactory implements ConnectionFactory,
		InitializingBean, DisposableBean {

	public final static String AUTHMODE_UI = "ui";
	public final static String AUTHMODE_OS = "os";
	public final static String AUTHMODE_DEFAULT = AUTHMODE_OS;
	// private final static String LOGIN_CONFIG_PROPERTY =
	// "java.security.auth.login.config";

	private final static Log log = LogFactory
			.getLog(SecuredActiveMqConnectionFactory.class);

	private String keyStorePassword;
	private Resource keyStore;
	private String keyStoreType = "JKS";// "PKCS12"
	private String brokerURL;

	private String authenticationMode;

	private CachingConnectionFactory cachingConnectionFactory;

	public Connection createConnection() throws JMSException {
		return cachingConnectionFactory.createConnection();
	}

	public Connection createConnection(String userName, String password)
			throws JMSException {
		throw new UnsupportedOperationException();
	}

	public void afterPropertiesSet() throws Exception {
		ActiveMQSslConnectionFactory activeMQSslConnectionFactory = new ActiveMQSslConnectionFactory();
		prepareActiveMqSslConnectionFactory(activeMQSslConnectionFactory);
		activeMQSslConnectionFactory.setBrokerURL(brokerURL);
		UserCredentialsConnectionFactoryAdapter uccfa = new UserCredentialsConnectionFactoryAdapter();
		uccfa.setTargetConnectionFactory(activeMQSslConnectionFactory);
		cachingConnectionFactory = new CachingConnectionFactory();
		cachingConnectionFactory.setTargetConnectionFactory(uccfa);
		cachingConnectionFactory.setCacheConsumers(false);

		initConnectionFactoryCredentials(uccfa);
		cachingConnectionFactory.initConnection();
		log.info("Connected to " + brokerURL);
		uccfa.setUsername(null);
		uccfa.setPassword(null);

	}

	protected void initConnectionFactoryCredentials(
			final UserCredentialsConnectionFactoryAdapter uccfa) {
		if (authenticationMode == null)
			authenticationMode = AUTHMODE_DEFAULT;

		if (AUTHMODE_OS.equals(authenticationMode)) {
			// if (false) {
			// // Cache previous value of login conf location
			// String oldLoginConfLocation = System
			// .getProperty(LOGIN_CONFIG_PROPERTY);
			// // Find OS family
			// String osName = System.getProperty("os.name");
			// final String auth;
			// if (osName.startsWith("Windows"))
			// auth = "Windows";
			// else if (osName.startsWith("SunOS")
			// || osName.startsWith("Solaris"))
			// auth = "Solaris";
			// else
			// auth = "Unix";
			//
			// Subject subject;
			// // see http://old.nabble.com/osgi-and-jaas-td23485885.html
			// ClassLoader ccl = Thread.currentThread()
			// .getContextClassLoader();
			// try {
			// Thread.currentThread().setContextClassLoader(
			// getClass().getClassLoader());
			// URL url = getClass().getResource(
			// "/org/argeo/security/activemq/osLogin.conf");
			//
			// System.setProperty(LOGIN_CONFIG_PROPERTY, url.toString());
			// LoginContext lc = new LoginContext(auth);
			// lc.login();
			// subject = lc.getSubject();
			// } catch (LoginException le) {
			// throw new ArgeoException("OS authentication failed", le);
			// } finally {
			// if (oldLoginConfLocation != null)
			// System.setProperty(LOGIN_CONFIG_PROPERTY,
			// oldLoginConfLocation);
			// Thread.currentThread().setContextClassLoader(ccl);
			// }
			// // Extract user name
			// String osUsername = null;
			// for (Principal principal : subject.getPrincipals()) {
			// String className = principal.getClass().getName();
			// if ("Unix".equals(auth)
			// && "com.sun.security.auth.UnixPrincipal"
			// .equals(className))
			// osUsername = principal.getName();
			// else if ("Windows".equals(auth)
			// && "com.sun.security.auth.NTUserPrincipal"
			// .equals(className))
			// osUsername = principal.getName();
			// else if ("Solaris".equals(auth)
			// && "com.sun.security.auth.SolarisPrincipal"
			// .equals(className))
			// osUsername = principal.getName();
			// }
			//
			// if (osUsername == null)
			// throw new ArgeoException("Could not find OS user name");
			// }

			uccfa.setUsername(System.getProperty("user.name"));
			uccfa.setPassword(null);

		} else if (AUTHMODE_UI.equals(authenticationMode)) {
			try {
				UIManager.setLookAndFeel(new MetalLookAndFeel());
			} catch (UnsupportedLookAndFeelException e) {
				throw new ArgeoException("Cannot load look and feel", e);
			}
			UIManager.put("ClassLoader", getClass().getClassLoader());
			UserPasswordDialog dialog = new UserPasswordDialog() {
				private static final long serialVersionUID = -891646559691412088L;

				protected void useCredentials(String username, char[] password) {
					uccfa.setUsername(username);
					uccfa.setPassword(new String(password));
				}
			};
			dialog.setVisible(true);
		} else {
			throw new ArgeoException("Authentication mode '"
					+ authenticationMode + "' is not supported");
		}

	}

	protected void prepareActiveMqSslConnectionFactory(
			ActiveMQSslConnectionFactory connectionFactory) {
		try {
			KeyStore keyStoreKs = KeyStore.getInstance(keyStoreType);

			InputStream keyInput = keyStore.getInputStream();
			keyStoreKs.load(keyInput,
					keyStorePassword != null ? keyStorePassword.toCharArray()
							: null);
			keyInput.close();

			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStoreKs);

			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStoreKs, keyStorePassword.toCharArray());

			connectionFactory.setKeyAndTrustManagers(
					keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(),
					new SecureRandom());
		} catch (Exception e) {
			throw new ArgeoException(
					"Cannot initialize JMS connection factory", e);
		}

	}

	public void destroy() throws Exception {
		if (cachingConnectionFactory != null)
			cachingConnectionFactory.destroy();
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public void setKeyStore(Resource keyStore) {
		this.keyStore = keyStore;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public void setBrokerURL(String brokerUrl) {
		this.brokerURL = brokerUrl;
	}

	public void setAuthenticationMode(String authenticationMode) {
		this.authenticationMode = authenticationMode;
	}

}
