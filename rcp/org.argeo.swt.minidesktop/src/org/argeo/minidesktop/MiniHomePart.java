package org.argeo.minidesktop;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ToolBar;

/** A start page displaying network information and resources. */
public class MiniHomePart {

	public Control createUiPart(Composite parent, Object context) {
		parent.setLayout(new GridLayout(2, false));
		Display display = parent.getDisplay();

		// Apps
		Group appsGroup = new Group(parent, SWT.NONE);
		appsGroup.setText("Apps");
		appsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));
		ToolBar appsToolBar = new ToolBar(appsGroup, SWT.HORIZONTAL | SWT.FLAT);
		fillAppsToolBar(appsToolBar);

		// Host
		Group hostGroup = new Group(parent, SWT.NONE);
		hostGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		hostGroup.setText("Host");
		hostGroup.setLayout(new GridLayout(2, false));
		label(hostGroup, "Hostname: ");
		try {
			InetAddress defaultAddr = InetAddress.getLocalHost();
			String hostname = defaultAddr.getHostName();
			label(hostGroup, hostname);
			label(hostGroup, "Address: ");
			label(hostGroup, defaultAddr.getHostAddress());
		} catch (UnknownHostException e) {
			label(hostGroup, e.getMessage());
		}

		Enumeration<NetworkInterface> netInterfaces = null;
		try {
			netInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			label(hostGroup, "Interfaces: ");
			label(hostGroup, e.getMessage());
		}
		if (netInterfaces != null)
			while (netInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = netInterfaces.nextElement();
				byte[] hardwareAddress = null;
				try {
					hardwareAddress = netInterface.getHardwareAddress();
					if (hardwareAddress != null) {
						label(hostGroup, convertHardwareAddress(hardwareAddress));
						label(hostGroup, netInterface.getName());
						for (InterfaceAddress addr : netInterface.getInterfaceAddresses()) {
							label(hostGroup, cleanHostAddress(addr.getAddress().getHostAddress()));
							label(hostGroup, Short.toString(addr.getNetworkPrefixLength()));
						}
					}
				} catch (SocketException e) {
					label(hostGroup, e.getMessage());
				}
			}

		// Resources
		Group resGroup = new Group(parent, SWT.NONE);
		resGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		resGroup.setText("Resources");
		resGroup.setLayout(new GridLayout(3, false));

		Runtime runtime = Runtime.getRuntime();

		String maxMemoryStr = Long.toString(runtime.maxMemory() / (1024 * 1024)) + " MB";
		label(resGroup, "Max Java memory: ");
		label(resGroup, maxMemoryStr);
		label(resGroup, "Java version: " + Runtime.version().toString());

		label(resGroup, "Usable Java memory: ");
		Label totalMemory = label(resGroup, maxMemoryStr);
		ProgressBar totalOnMax = new ProgressBar(resGroup, SWT.SMOOTH);
		totalOnMax.setMaximum(100);
		label(resGroup, "Used Java memory: ");
		Label usedMemory = label(resGroup, maxMemoryStr);
		ProgressBar usedOnTotal = new ProgressBar(resGroup, SWT.SMOOTH);
		totalOnMax.setMaximum(100);
		new Thread() {
			@Override
			public void run() {
				while (!totalOnMax.isDisposed()) {
					display.asyncExec(() -> {
						if (totalOnMax.isDisposed())
							return;
						totalOnMax.setSelection(javaTotalOnMaxPerct(runtime));
						usedOnTotal.setSelection(javaUsedOnTotalPerct(runtime));
						totalMemory.setText(Long.toString(runtime.totalMemory() / (1024 * 1024)) + " MB");
						usedMemory.setText(
								Long.toString((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)) + " MB");
					});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}.start();
		return parent;
	}

	protected void fillAppsToolBar(ToolBar toolBar) {

	}

	protected int javaUsedOnTotalPerct(Runtime runtime) {
		return Math.toIntExact((runtime.totalMemory() - runtime.freeMemory()) * 100 / runtime.totalMemory());
	}

	protected int javaTotalOnMaxPerct(Runtime runtime) {
		return Math.toIntExact((runtime.totalMemory()) * 100 / runtime.maxMemory());
	}

	protected Label label(Composite parent, String text) {
		Label label = new Label(parent, SWT.WRAP);
		label.setText(text);
		return label;
	}

	protected String cleanHostAddress(String hostAddress) {
		// remove % from Ipv6 addresses
		int index = hostAddress.indexOf('%');
		if (index > 0)
			return hostAddress.substring(0, index);
		else
			return hostAddress;
	}

	protected String convertHardwareAddress(byte[] hardwareAddress) {
		if (hardwareAddress == null)
			return "";
		// from https://stackoverflow.com/a/2797498/7878010
		StringBuilder sb = new StringBuilder(18);
		for (byte b : hardwareAddress) {
			if (sb.length() > 0)
				sb.append(':');
			sb.append(String.format("%02x", b).toUpperCase());
		}
		return sb.toString();
	}
}
