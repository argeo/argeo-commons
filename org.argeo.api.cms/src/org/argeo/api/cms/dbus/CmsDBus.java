package org.argeo.api.cms.dbus;

/** Access to a DBus session or system bus. */
public interface CmsDBus {

	CmsDBusConnection openSessionConnection();

}
