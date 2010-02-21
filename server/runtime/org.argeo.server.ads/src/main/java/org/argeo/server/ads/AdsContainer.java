package org.argeo.server.ads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.directory.server.configuration.MutableServerStartupConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.jndi.ServerContextFactory;
import org.argeo.ArgeoException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

@SuppressWarnings("restriction")
public class AdsContainer implements InitializingBean, DisposableBean {
	private final static Log log = LogFactory.getLog(AdsContainer.class);

	private MutableServerStartupConfiguration configuration;
	private Properties environment = null;
	private File workingDirectory = new File(System
			.getProperty("java.io.tmpdir")
			+ File.separator + "argeo-apacheDirectoryServer");
	private List<Resource> ldifs = new ArrayList<Resource>();
	private File ldifDirectory;
	private Boolean deleteWorkingDirOnExit = false;

	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws Exception {

		log.info("Starting directory server with id '"
				+ configuration.getInstanceId() + "' in directory "
				+ workingDirectory.getAbsolutePath());

		if (deleteWorkingDirOnExit && workingDirectory.exists()) {
			log.warn("Found existing directory " + workingDirectory
					+ " deleting it...");
			FileUtils.deleteDirectory(workingDirectory);
		}
		configuration.setWorkingDirectory(workingDirectory);
		workingDirectory.mkdirs();

		if (ldifDirectory != null)
			configuration.setLdifDirectory(ldifDirectory);
		else
			configuration.setLdifDirectory(new File(workingDirectory
					.getAbsolutePath()
					+ File.separator + "ldif"));

		// Deals with provided LDIF files
		if (ldifs.size() > 0)
			configuration.getLdifDirectory().mkdirs();
		for (Resource ldif : ldifs) {
			File targetFile = new File(configuration.getLdifDirectory()
					.getAbsolutePath()
					+ File.separator + ldif.getFilename().replace(':', '_'));
			OutputStream output = null;
			try {
				output = new FileOutputStream(targetFile);
				IOUtils.copy(ldif.getInputStream(), output);
				if (log.isDebugEnabled())
					log.debug("Copied " + ldif + " to LDIF directory "
							+ configuration.getLdifDirectory());
			} finally {
				IOUtils.closeQuietly(output);
			}
		}

		Properties env = new Properties();
		env.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				ServerContextFactory.class.getName());
		Assert.notNull(environment);
		env.putAll(environment);
		env.putAll(configuration.toJndiEnvironment());

		try {
			new InitialDirContext(env);
		} catch (NamingException e) {
			throw new ArgeoException("Failed to start Apache Directory server",
					e);
		}
	}

	@SuppressWarnings("unchecked")
	public void destroy() throws Exception {
		ShutdownConfiguration shutdown = new ShutdownConfiguration(
				configuration.getInstanceId());

		Properties env = new Properties();
		env.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				ServerContextFactory.class.getName());
		Assert.notNull(environment);
		env.putAll(environment);
		env.putAll(shutdown.toJndiEnvironment());

		log.info("Shutting down directory server with id '"
				+ configuration.getInstanceId() + "'");

		try {
			new InitialContext(env);
		} catch (NamingException e) {
			throw new ArgeoException("Failed to stop Apache Directory server",
					e);
		}

		if (workingDirectory.exists() && deleteWorkingDirOnExit) {
			if (log.isDebugEnabled())
				log.debug("Delete Apache DS working dir " + workingDirectory);
			FileUtils.deleteDirectory(workingDirectory);
		}

	}

	public void setConfiguration(MutableServerStartupConfiguration configuration) {
		this.configuration = configuration;
	}

	public void setWorkingDirectory(File workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public void setEnvironment(Properties environment) {
		this.environment = environment;
	}

	public void setLdifs(List<Resource> ldifs) {
		this.ldifs = ldifs;
	}

	public void setLdifDirectory(File ldifDirectory) {
		this.ldifDirectory = ldifDirectory;
	}

	public void setDeleteWorkingDirOnExit(Boolean deleteWorkingDirOnExit) {
		this.deleteWorkingDirOnExit = deleteWorkingDirOnExit;
	}

}
