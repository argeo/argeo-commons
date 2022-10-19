package org.argeo.cms.ssh.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Objects;

import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;

/** Separate class in order to avoit static field from Apache SSHD. */
class DefaultClientIdentityLoader implements ClientIdentityLoader {
	@Override
	public boolean isValidLocation(NamedResource location) throws IOException {
		Path path = toPath(location);
		return Files.exists(path, IoUtils.EMPTY_LINK_OPTIONS);
	}

	@Override
	public Iterable<KeyPair> loadClientIdentities(SessionContext session, NamedResource location,
			FilePasswordProvider provider) throws IOException, GeneralSecurityException {
		Path path = toPath(location);
		PathResource resource = new PathResource(path);
		try (InputStream inputStream = resource.openInputStream()) {
			return SecurityUtils.loadKeyPairIdentities(session, resource, inputStream, provider);
		}
	}

	@Override
	public String toString() {
		return "DEFAULT";
	}

	private Path toPath(NamedResource location) {
		Objects.requireNonNull(location, "No location provided");

		Path path = Paths
				.get(ValidateUtils.checkNotNullAndNotEmpty(location.getName(), "No location value for %s", location));
		path = path.toAbsolutePath();
		path = path.normalize();
		return path;
	}

}
