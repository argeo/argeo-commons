package org.argeo.cms;

import java.util.Locale;

import org.argeo.cms.i18n.LocaleUtils;
import org.argeo.cms.i18n.Localized;

public enum CmsMsg implements Localized {
	username, password, login, logout, register, changePassword, currentPassword, newPassword, repeatNewPassword, passwordChanged;

	public Object local(Locale locale) {
		return LocaleUtils.local(this, locale);
	}

	public String lead() {
		return LocaleUtils.lead(this);
	}

	public String lead(Locale locale) {
		return LocaleUtils.lead(local(locale).toString(), locale);
	}
}
