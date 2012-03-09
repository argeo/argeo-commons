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
package org.argeo.server.ads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
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

/** Wraps an Apache Directory Server instance. */
@SuppressWarnings("restriction")
public class AdsContainer implements InitializingBean, DisposableBean {
	private final static Log log = LogFactory.getLog(AdsContainer.class);

	private MutableServerStartupConfiguration configuration;
	private Properties environment = null;
	private File workingDirectory = new File(
			System.getProperty("java.io.tmpdir") + File.separator
					+ "argeo-apacheDirectoryServer");
	private Boolean deleteWorkingDirOnExit = false;

	// LDIF
	private List<Resource> ldifs = new ArrayList<Resource>();
	private List<String> ignoredLdifAttributes = new ArrayList<String>();
	/** default is 'demo' */
	private String ldifPassword = "e1NIQX1pZVNWNTVRYytlUU9hWURSU2hhL0Fqek5USkU9";
	private String ldifPasswordAttribute = "userPassword";
	private File ldifDirectory;

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
					.getAbsolutePath() + File.separator + "ldif"));

		if (ignoredLdifAttributes.size() == 0) {
			ignoredLdifAttributes.add("entryUUID");
			ignoredLdifAttributes.add("structuralObjectClass");
			ignoredLdifAttributes.add("creatorsName");
			ignoredLdifAttributes.add("createTimestamp");
			ignoredLdifAttributes.add("entryCSN");
			ignoredLdifAttributes.add("modifiersName");
			ignoredLdifAttributes.add("modifyTimestamp");
		}

		// Process provided LDIF files
		if (ldifs.size() > 0)
			configuration.getLdifDirectory().mkdirs();
		for (Resource ldif : ldifs) {
			File targetFile = new File(configuration.getLdifDirectory()
					.getAbsolutePath()
					+ File.separator
					+ ldif.getFilename().replace(':', '_'));
			processLdif(ldif, targetFile);
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

	/**
	 * Processes an LDIF resource, filtering out attributes that cannot be
	 * imported in ADS and forcing a password.
	 */
	protected void processLdif(Resource ldif, File targetFile) {
		BufferedReader reader = null;
		Writer writer = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					ldif.getInputStream()));
			writer = new FileWriter(targetFile);
			String line = null;
			lines: while ((line = reader.readLine()) != null) {
				// comment and empty lines
				if (line.trim().equals("") || line.startsWith("#")) {
					writer.write(line);
					writer.write('\n');
					continue lines;
				}

				String[] tokens = line.split(":");
				String attribute = null;
				if (tokens != null && tokens.length > 1) {
					attribute = tokens[0].trim();
					if (ignoredLdifAttributes.contains(attribute))
						continue lines;// ignore

					if (attribute.equals("bdb_db_open")) {
						log.warn("Ignored OpenLDAP output\n" + line);
						continue lines;
					}

					if (ldifPassword != null
							&& attribute.equals(ldifPasswordAttribute)) {
						line = ldifPasswordAttribute + ":: " + ldifPassword;
					}

					writer.write(line);
					writer.write('\n');
				} else {
					log.warn("Ignored LDIF line\n" + line);
				}
			}
			if (log.isDebugEnabled())
				log.debug("Processed " + ldif + " to LDIF directory "
						+ configuration.getLdifDirectory());
		} catch (IOException e) {
			throw new ArgeoException("Cannot process LDIF " + ldif, e);
		} finally {
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(writer);
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

	public void setIgnoredLdifAttributes(List<String> ignoredLdifAttributes) {
		this.ignoredLdifAttributes = ignoredLdifAttributes;
	}

	public void setLdifPassword(String ldifPassword) {
		this.ldifPassword = ldifPassword;
	}

	public void setLdifPasswordAttribute(String ldifPasswordAttribute) {
		this.ldifPasswordAttribute = ldifPasswordAttribute;
	}

}
