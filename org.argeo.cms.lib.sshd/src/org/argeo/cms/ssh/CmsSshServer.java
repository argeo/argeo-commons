package org.argeo.cms.ssh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.gss.GSSAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.jaas.JaasPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.CmsSshd;

public class CmsSshServer implements CmsSshd {
	private final static CmsLog log = CmsLog.getLog(CmsSshServer.class);
	private static final String DEFAULT_SSH_HOST_KEY_PATH = CmsConstants.NODE + '/' + CmsConstants.NODE + ".ser";

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

		Path hostKeyPath = cmsState.getDataPath(DEFAULT_SSH_HOST_KEY_PATH);

		try {
			sshd = SshServer.setUpDefaultServer();
			sshd.setPort(port);
			if (host != null)
				sshd.setHost(host);
			if (hostKeyPath == null)
				throw new IllegalStateException("An SSH server key must be set");
			sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));
			// sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i",
			// "-l" }));
//			String[] shellCommand = OS.LOCAL.getDefaultShellCommand();
			// FIXME transfer args
//		sshd.setShellFactory(new ProcessShellFactory(shellCommand));
			sshd.setShellFactory(InteractiveProcessShellFactory.INSTANCE);
			sshd.setCommandFactory(new ScpCommandFactory());

			// tunnels
			sshd.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
			// sshd.setForwardingFilter(ForwardingFilter.asForwardingFilter(null, null,
			// TcpForwardingFilter.DEFAULT));
			// sshd.setForwarderFactory(DefaultForwarderFactory.INSTANCE);
//			TcpForwardingFilter tcpForwardingFilter = sshd.getTcpForwardingFilter();
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
			// sshd.setPublickeyAuthenticator(new DefaultAuthorizedKeysAuthenticator(true));
			sshd.setPublickeyAuthenticator(null);
			// sshd.setKeyboardInteractiveAuthenticator(null);
			JaasPasswordAuthenticator jaasPasswordAuthenticator = new JaasPasswordAuthenticator();
			jaasPasswordAuthenticator.setDomain(CmsAuth.NODE.getLoginContextName());
			sshd.setPasswordAuthenticator(jaasPasswordAuthenticator);

			Path krb5keyTab = cmsState.getDataPath("node/krb5.keytab");
			if (Files.exists(krb5keyTab)) {
				// FIXME experimental
				GSSAuthenticator gssAuthenticator = new GSSAuthenticator();
				gssAuthenticator.setKeytabFile(cmsState.getDataPath("node/krb5.keytab").toString());
				gssAuthenticator.setServicePrincipalName("HTTP@" + host);
				sshd.setGSSAuthenticator(gssAuthenticator);
			}

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

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
