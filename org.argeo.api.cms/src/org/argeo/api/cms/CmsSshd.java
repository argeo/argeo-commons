package org.argeo.api.cms;

import java.net.InetSocketAddress;

/** A local SSH server. */
public interface CmsSshd {
	final static String NODE_USERNAME_ALIAS = "user.name";
	final static String DEFAULT_SSH_HOST_KEY_PATH = "private/" + CmsConstants.NODE + ".ser";

	InetSocketAddress getAddress();
}
