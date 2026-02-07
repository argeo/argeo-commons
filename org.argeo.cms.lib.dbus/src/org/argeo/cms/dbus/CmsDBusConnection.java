package org.argeo.cms.dbus;

import java.io.Closeable;

public interface CmsDBusConnection extends Closeable {
	void requestBusName(String _busname);

	void exportObject(String _objectPath, Object _object);
	
	void callMethodAsync(Object _object, String _method, Object... _parameters);
}
