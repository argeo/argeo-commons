package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.CmsException;
import org.argeo.cms.KernelHeader;
import org.argeo.jackrabbit.OsgiJackrabbitRepositoryFactory;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.security.core.InternalAuthentication;
import org.argeo.security.crypto.PkiUtils;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Argeo CMS Kernel. Responsible for :
 * <ul>
 * <li>security</li>
 * <li>provisioning</li>
 * <li>transaction</li>
 * <li>logging</li>
 * <li>local and remote file systems access</li>
 * <li>OS access</li>
 * </ul>
 */
final class Kernel implements ServiceListener {

	private final static Log log = LogFactory.getLog(Kernel.class);

	private final BundleContext bundleContext = Activator.getBundleContext();

	ThreadGroup threadGroup = new ThreadGroup(Kernel.class.getSimpleName());
	JackrabbitNode node;
	OsgiJackrabbitRepositoryFactory repositoryFactory;
	NodeSecurity nodeSecurity;
	NodeHttp nodeHttp;
	private KernelThread kernelThread;

	private final Subject kernelSubject = new Subject();

	public Kernel() {
		URL url = getClass().getClassLoader().getResource(
				KernelConstants.JAAS_CONFIG);
		System.setProperty("java.security.auth.login.config",
				url.toExternalForm());
		createKeyStoreIfNeeded();

		CallbackHandler cbHandler = new CallbackHandler() {

			@Override
			public void handle(Callback[] callbacks) throws IOException,
					UnsupportedCallbackException {
				// alias
				((NameCallback) callbacks[1]).setName(KernelHeader.ROLE_KERNEL);
				// store pwd
				((PasswordCallback) callbacks[2]).setPassword("changeit"
						.toCharArray());
				// key pwd
				((PasswordCallback) callbacks[3]).setPassword("changeit"
						.toCharArray());
			}
		};
		try {
			LoginContext kernelLc = new LoginContext(
					KernelConstants.LOGIN_CONTEXT_KERNEL, kernelSubject,
					cbHandler);
			kernelLc.login();
		} catch (LoginException e) {
			throw new CmsException("Cannot log in kernel", e);
		}
	}

	final void init() {
		Subject.doAs(kernelSubject, new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				doInit();
				return null;
			}

		});
	}

	private void doInit() {
		ClassLoader currentContextCl = Thread.currentThread()
				.getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				Kernel.class.getClassLoader());
		long begin = System.currentTimeMillis();
		InternalAuthentication initAuth = new InternalAuthentication(
				KernelConstants.DEFAULT_SECURITY_KEY);
		SecurityContextHolder.getContext().setAuthentication(initAuth);

		try {
			// Jackrabbit node
			node = new JackrabbitNode(bundleContext);

			// JCR repository factory
			repositoryFactory = new OsgiJackrabbitRepositoryFactory();

			// Authentication
			nodeSecurity = new NodeSecurity(bundleContext, node);

			// Equinox dependency
			ExtendedHttpService httpService = waitForHttpService();
			nodeHttp = new NodeHttp(httpService, node, nodeSecurity);

			// Kernel thread
			kernelThread = new KernelThread(this);
			kernelThread.setContextClassLoader(Kernel.class.getClassLoader());
			kernelThread.start();

			// Publish services to OSGi
			nodeSecurity.publish();
			node.publish(repositoryFactory);
			bundleContext.registerService(RepositoryFactory.class,
					repositoryFactory, null);

			bundleContext.addServiceListener(Kernel.this);
		} catch (Exception e) {
			log.error("Cannot initialize Argeo CMS", e);
			throw new ArgeoException("Cannot initialize", e);
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
		}

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		log.info("## ARGEO CMS UP in " + (jvmUptime / 1000) + "."
				+ (jvmUptime % 1000) + "s ##");
		long initDuration = System.currentTimeMillis() - begin;
		if (log.isTraceEnabled())
			log.trace("Kernel initialization took " + initDuration + "ms");
		directorsCut(initDuration);
	}

	void destroy() {
		long begin = System.currentTimeMillis();

		kernelThread.destroyAndJoin();

		if (nodeHttp != null)
			nodeHttp.destroy();
		if (nodeSecurity != null)
			nodeSecurity.destroy();
		if (node != null)
			node.destroy();

		bundleContext.removeServiceListener(this);

		// Clean hanging threads from Jackrabbit
		TransientFileFactory.shutdown();

		try {
			LoginContext kernelLc = new LoginContext(
					KernelConstants.LOGIN_CONTEXT_KERNEL, kernelSubject);
			kernelLc.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log in kernel", e);
		}

		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS DOWN in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReference<?> sr = event.getServiceReference();
		Object jcrRepoAlias = sr
				.getProperty(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS);
		if (jcrRepoAlias != null) {// JCR repository
			String alias = jcrRepoAlias.toString();
			Repository repository = (Repository) bundleContext.getService(sr);
			Map<String, Object> props = new HashMap<String, Object>();
			for (String key : sr.getPropertyKeys())
				props.put(key, sr.getProperty(key));
			if (ServiceEvent.REGISTERED == event.getType()) {
				try {
					repositoryFactory.register(repository, props);
					nodeHttp.registerRepositoryServlets(alias, repository);
				} catch (Exception e) {
					throw new CmsException("Could not publish JCR repository "
							+ alias, e);
				}
			} else if (ServiceEvent.UNREGISTERING == event.getType()) {
				repositoryFactory.unregister(repository, props);
				nodeHttp.unregisterRepositoryServlets(alias);
			}
		}

	}

	private ExtendedHttpService waitForHttpService() {
		final ServiceTracker<ExtendedHttpService, ExtendedHttpService> st = new ServiceTracker<ExtendedHttpService, ExtendedHttpService>(
				bundleContext, ExtendedHttpService.class, null);
		st.open();
		ExtendedHttpService httpService;
		try {
			httpService = st.waitForService(1000);
		} catch (InterruptedException e) {
			httpService = null;
		}

		if (httpService == null)
			throw new CmsException("Could not find "
					+ ExtendedHttpService.class + " service.");
		return httpService;
	}

	private void createKeyStoreIfNeeded() {
		char[] ksPwd = "changeit".toCharArray();
		char[] keyPwd = Arrays.copyOf(ksPwd, ksPwd.length);
		File keyStoreFile = KernelUtils.getOsgiConfigurationFile("node.p12");
		if (!keyStoreFile.exists()) {
			try {
				KeyStore keyStore = PkiUtils.getKeyStore(keyStoreFile, ksPwd);
				X509Certificate cert = PkiUtils.generateSelfSignedCertificate(
						keyStore, new X500Principal(KernelHeader.ROLE_KERNEL),
						keyPwd);
				PkiUtils.saveKeyStore(keyStoreFile, ksPwd, keyStore);

			} catch (Exception e) {
				throw new CmsException("Cannot create key store "
						+ keyStoreFile, e);
			}
		}
	}

	final private static void directorsCut(long initDuration) {
		// final long ms = 128l + (long) (Math.random() * 128d);
		long ms = initDuration / 100;
		log.info("Spend " + ms + "ms"
				+ " reflecting on the progress brought to mankind"
				+ " by Free Software...");
		long beginNano = System.nanoTime();
		try {
			Thread.sleep(ms, 0);
		} catch (InterruptedException e) {
			// silent
		}
		long durationNano = System.nanoTime() - beginNano;
		final double M = 1000d * 1000d;
		double sleepAccuracy = ((double) durationNano) / (ms * M);
		if (log.isDebugEnabled())
			log.debug("Sleep accuracy: "
					+ String.format("%.2f", 100 - (sleepAccuracy * 100 - 100))
					+ " %");
	}
}