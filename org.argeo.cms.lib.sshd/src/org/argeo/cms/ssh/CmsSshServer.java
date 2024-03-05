package org.argeo.cms.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.gss.GSSAuthenticator;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.config.keys.DefaultAuthorizedKeysAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.jaas.JaasPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSshd;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;

public class CmsSshServer implements CmsSshd {
	private final static CmsLog log = CmsLog.getLog(CmsSshServer.class);

	private CmsState cmsState;
	private SshServer sshd = null;

	private int port;
	private String host;

	public void start() {
		String portStr = cmsState.getDeployProperty(CmsDeployProperty.SSHD_PORT.getProperty());
		if (portStr == null)
			return; // ignore
		port = Integer.parseInt(portStr);

		host = cmsState.getDeployProperty(CmsDeployProperty.HOST.getProperty());

		KeyPair nodeKeyPair = loadNodeKeyPair();

		try {
			// authorized keys
			String authorizedKeysStr = cmsState.getDeployProperty(CmsDeployProperty.SSHD_AUTHORIZEDKEYS.getProperty());
			Path authorizedKeysPath = authorizedKeysStr != null ? Paths.get(authorizedKeysStr)
					: AuthorizedKeysAuthenticator.getDefaultAuthorizedKeysFile();
			if (authorizedKeysStr != null && !Files.exists(authorizedKeysPath)) {
				Files.createFile(authorizedKeysPath);
				Set<PosixFilePermission> posixPermissions = new HashSet<>();
				posixPermissions.add(PosixFilePermission.OWNER_READ);
				posixPermissions.add(PosixFilePermission.OWNER_WRITE);
				Files.setPosixFilePermissions(authorizedKeysPath, posixPermissions);

				if (nodeKeyPair != null)
					try {
						String openSsshPublicKey = PublicKeyEntry.toString(nodeKeyPair.getPublic());
						try (Writer writer = Files.newBufferedWriter(authorizedKeysPath, StandardCharsets.US_ASCII,
								StandardOpenOption.APPEND)) {
							writer.write(openSsshPublicKey);
						}
					} catch (IOException e) {
						log.error("Cannot add node public key to SSH authorized keys", e);
					}
			}

			// create server
			sshd = SshServer.setUpDefaultServer();
			sshd.setPort(port);
			if (host != null)
				sshd.setHost(host);

			// host key
			if (nodeKeyPair != null) {
				sshd.setKeyPairProvider(KeyPairProvider.wrap(nodeKeyPair));
			} else {
				Path hostKeyPath = cmsState.getDataPath(DEFAULT_SSH_HOST_KEY_PATH);
				if (hostKeyPath == null) // TODO deal with no data area?
					throw new IllegalStateException("An SSH server key must be set");
				sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));
			}

			// tunnels
			sshd.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
			sshd.addPortForwardingEventListener(new PortForwardingEventListener() {

				@Override
				public void establishingExplicitTunnel(Session session, SshdSocketAddress local,
						SshdSocketAddress remote, boolean localForwarding) throws IOException {
					log.debug("Establishing tunnel " + local + ", " + remote);
				}

				@Override
				public void establishedExplicitTunnel(Session session, SshdSocketAddress local,
						SshdSocketAddress remote, boolean localForwarding, SshdSocketAddress boundAddress,
						Throwable reason) throws IOException {
					log.debug("Established tunnel " + local + ", " + remote + ", " + boundAddress);
				}

				@Override
				public void establishingDynamicTunnel(Session session, SshdSocketAddress local) throws IOException {
					log.debug("Establishing dynamic tunnel " + local);
				}

				@Override
				public void establishedDynamicTunnel(Session session, SshdSocketAddress local,
						SshdSocketAddress boundAddress, Throwable reason) throws IOException {
					log.debug("Established dynamic tunnel " + local);
				}

			});

			// Authentication
			// FIXME use strict, set proper permissions, etc.
			sshd.setPublickeyAuthenticator(
					new DefaultAuthorizedKeysAuthenticator("user.name", authorizedKeysPath, true));
			// sshd.setPublickeyAuthenticator(null);
			// sshd.setKeyboardInteractiveAuthenticator(null);
			JaasPasswordAuthenticator jaasPasswordAuthenticator = new JaasPasswordAuthenticator();
			jaasPasswordAuthenticator.setDomain(CmsAuth.NODE.getLoginContextName());
			sshd.setPasswordAuthenticator(jaasPasswordAuthenticator);

			boolean gssApi = false;
			if (gssApi) {
				Path krb5keyTab = cmsState.getDataPath("private/krb5.keytab");
				if (Files.exists(krb5keyTab)) {
					// FIXME experimental
					GSSAuthenticator gssAuthenticator = new GSSAuthenticator();
					gssAuthenticator.setKeytabFile(krb5keyTab.toString());
					gssAuthenticator.setServicePrincipalName("HTTP@" + host);
					sshd.setGSSAuthenticator(gssAuthenticator);
				}
			}

			// shell
			// TODO make it configurable
			sshd.setShellFactory(InteractiveProcessShellFactory.INSTANCE);
//			String[] shellCommand = OS.LOCAL.getDefaultShellCommand();
//			StringJoiner command = new StringJoiner(" ");
//			for (String str : shellCommand) {
//				command.add(str);
//			}
//			sshd.setShellFactory(new ProcessShellFactory(command.toString(), shellCommand));
			sshd.setCommandFactory(new ScpCommandFactory());

			// SFTP
			sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));

			// start
			sshd.start();

			log.debug(() -> "CMS SSH server started on port " + port + (host != null ? " of host " + host : ""));
		} catch (IOException e) {
			throw new RuntimeException("Cannot start SSH server on port " + port, e);
		}

	}

	public void stop() {
		if (sshd == null)
			return;
		try {
			sshd.stop();
			log.debug(() -> "CMS SSH server stopped on port " + port + (host != null ? " of host " + host : ""));
		} catch (IOException e) {
			throw new RuntimeException("Cannot stop SSH server", e);
		}

	}

	protected KeyPair loadNodeKeyPair() {
		try {
			char[] keyStorePassword = cmsState.getDeployProperty(CmsDeployProperty.SSL_PASSWORD.getProperty())
					.toCharArray();
			Path keyStorePath = Paths.get(cmsState.getDeployProperty(CmsDeployProperty.SSL_KEYSTORE.getProperty()));
			String keyStoreType = cmsState.getDeployProperty(CmsDeployProperty.SSL_KEYSTORETYPE.getProperty());

			KeyStore store = KeyStore.getInstance(keyStoreType, "SunJSSE");
			try (InputStream fis = Files.newInputStream(keyStorePath)) {
				store.load(fis, keyStorePassword);
			}
			return new KeyPair(store.getCertificate(CmsConstants.NODE).getPublicKey(),
					(PrivateKey) store.getKey(CmsConstants.NODE, keyStorePassword));
		} catch (IOException | KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException
				| CertificateException | IllegalArgumentException | UnrecoverableKeyException e) {
			if (log.isTraceEnabled())
				log.warn("Cannot add node public key to SSH authorized keys", e);
			else
				log.warn("Cannot add node public key to SSH authorized keys: " + e);
			return null;
		}

	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

	@Override
	public InetSocketAddress getAddress() {
		return new InetSocketAddress(host, port);
	}

}
