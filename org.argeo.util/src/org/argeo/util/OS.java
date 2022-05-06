package org.argeo.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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

	public static long getJvmPid() {
		return ProcessHandle.current().pid();
//		String pidAndHost = ManagementFactory.getRuntimeMXBean().getName();
//		return Integer.parseInt(pidAndHost.substring(0, pidAndHost.indexOf('@')));
	}

	/**
	 * Get the runtime directory. It will be the environment variable
	 * XDG_RUNTIME_DIR if it is set, or ~/.cache/argeo if not.
	 */
	public static Path getRunDir() {
		Path runDir;
		String xdgRunDir = System.getenv("XDG_RUNTIME_DIR");
		if (xdgRunDir != null) {
			// TODO support multiple names
			runDir = Paths.get(xdgRunDir);
		} else {
			runDir = Paths.get(System.getProperty("user.home"), ".cache/argeo");
		}
		return runDir;
	}
}
