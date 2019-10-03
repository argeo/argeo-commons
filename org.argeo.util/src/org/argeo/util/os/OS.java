package org.argeo.util.os;

import java.io.File;
import java.lang.management.ManagementFactory;

/** When OS specific informations are needed. */
public class OS {
	public final static OS LOCAL = new OS();

	private final String arch, name, version;

	/** The OS of the running JVM */
	protected OS() {
		arch = System.getProperty("os.arch");
		name = System.getProperty("os.name");
		version = System.getProperty("os.version");
	}

	public String getArch() {
		return arch;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public boolean isMSWindows() {
		// only MS Windows would use such an horrendous separator...
		return File.separatorChar == '\\';
	}

	public String[] getDefaultShellCommand() {
		if (!isMSWindows())
			return new String[] { "/bin/sh", "-l", "-i" };
		else
			return new String[] { "cmd.exe", "/C" };
	}

	public static Integer getJvmPid() {
		/*
		 * This method works on most platforms (including Linux). Although when Java 9
		 * comes along, there is a better way: long pid =
		 * ProcessHandle.current().getPid();
		 *
		 * See:
		 * http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-
		 * process-id
		 */
		String pidAndHost = ManagementFactory.getRuntimeMXBean().getName();
		return Integer.parseInt(pidAndHost.substring(0, pidAndHost.indexOf('@')));
	}
}
