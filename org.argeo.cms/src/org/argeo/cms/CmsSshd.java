package org.argeo.cms;

import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.internal.runtime.KernelConstants;

/** Just a marker interface for the time being. */
public interface CmsSshd {
	final static String NODE_USERNAME_ALIAS = "user.name";
	final static String DEFAULT_SSH_HOST_KEY_PATH = KernelConstants.DIR_PRIVATE + '/' + CmsConstants.NODE + ".ser";
}
