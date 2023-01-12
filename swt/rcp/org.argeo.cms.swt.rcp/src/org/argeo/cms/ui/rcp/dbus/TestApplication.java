package org.argeo.cms.ui.rcp.dbus;

import java.util.List;
import java.util.Map;

import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.argeo.cms.freedesktop.FreeDesktopApplication;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

public class TestApplication implements FreeDesktopApplication {
	private DBusConnection dBusConnection;

	private final Display display;

	private Shell shell = null;

	private Text text;

	public TestApplication() throws DBusException {
		display = Display.getCurrent();

		/* Get a connection to the session bus so we can request a bus name */
		dBusConnection = DBusConnectionBuilder.forSessionBus().build();
//		m_conn = DBusConnectionBuilder.forAddress("unix:path=/tmp/dbus-80908265778467677465").build();
//		m_conn = DBusConnectionBuilder.forAddress("tcp:host=localhost,port=55556").build();
		/* Request a unique bus name */
		dBusConnection.requestBusName("org.argeo.TestApplication");
		/* Export this object onto the bus using the path '/' */
		dBusConnection.exportObject(getObjectPath(), this);
	}

	@Override
	public String getObjectPath() {
		return "/org/argeo/TestApplication";
	}

	@Override
	public void activate(Map<String, Variant<?>> platformData) {
		display.syncExec(() -> {
			shellVisible();
		});

	}

	protected void shellVisible() {
		if (shell == null || shell.isDisposed()) {
			shell = new Shell(display);
			shell.setLayout(new GridLayout());
			text = new Text(shell, SWT.MULTI | SWT.WRAP);
			text.setLayoutData(CmsSwtUtils.fillAll());
			text.setText("New shell\n");
			shell.open();
		} else {
		}
		shell.forceActive();
	}

	@Override
	public void open(List<String> uris, Map<String, Variant<?>> platformData) {
		display.syncExec(() -> {
			shellVisible();
			for (String uri : uris) {
				text.append(uri);
				text.append("\n");
			}
			shell.forceActive();
		});
	}

	@Override
	public void activateAction(String actionName, List<Variant<?>> parameter, Map<String, Variant<?>> platformData) {
		display.syncExec(() -> {
			shellVisible();
			text.append("Execute action '" + actionName + "' with arguments " + parameter);
			text.append("\n");
		});
	}

	public static void main(String[] args) throws DBusException {
		Display display = new Display();
		new TestApplication();
		while (!display.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
