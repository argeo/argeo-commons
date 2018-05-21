package org.argeo.cms;

import org.argeo.cms.i18n.Localized;

public enum CmsMsg implements Localized {
	username, password, login, logout, register,
	// password
	changePassword, currentPassword, newPassword, repeatNewPassword, passwordChanged,
	// dialog
	close, cancel, ok,
	// wizard
	wizardBack, wizardNext, wizardFinish;
}
