package org.argeo.cms.freedesktop;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * The org.freedesktop.Application interface.
 */
@DBusInterfaceName("org.freedesktop.Application")
public interface FreeDesktopApplication extends DBusInterface {

	@DBusMemberName(value = "Activate")
	void activate(Map<String, Variant<?>> platformData);

	@DBusMemberName(value = "Open")
	void open(List<String> uris, Map<String, Variant<?>> platformData);

	@DBusMemberName(value = "ActivateAction")
	void activateAction(String actionName, List<Variant<?>> parameter, Map<String, Variant<?>> platformData);

}