package org.argeo.osgi.useradmin;

import java.security.MessageDigest;

class DigestUtils {
	static byte[] sha1(byte[] bytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			digest.update(bytes);
			byte[] checksum = digest.digest();
			return checksum;
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot SHA1 digest", e);
		}
	}

	private DigestUtils() {
	}

}
