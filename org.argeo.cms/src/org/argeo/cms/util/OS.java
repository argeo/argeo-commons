package org.argeo.cms.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Wrapper around system properties and portable Java APIS, for when OS specific
 * informations are needed.
 */
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

	public Path getSystemRoot() {
		if (isMSWindows()) {
			// TODO test it on MS Windows
			String systemDriveEnv = System.getenv("SystemDrive");
			Path systemRoot = null;
			for (File root : File.listRoots()) {
				if (root.getName().equals(systemDriveEnv)) {
					systemRoot = root.toPath();
					break;
				}
			}
			if (systemRoot == null)
				throw new IllegalStateException("Cannot find a system root");
			return systemRoot;
		} else {
			return Paths.get("/");
		}
	}

	public String[] getDefaultShellCommand() {
		if (!isMSWindows())
			return new String[] { "/bin/bash", "-l", "-i" };
		else
			return new String[] { "cmd.exe", "/C" };
	}

//	public static long getJvmPid() {
//		return ProcessHandle.current().pid();
////		String pidAndHost = ManagementFactory.getRuntimeMXBean().getName();
////		return Integer.parseInt(pidAndHost.substring(0, pidAndHost.indexOf('@')));
//	}

	/**
	 * Get the runtime directory. It will be the environment variable
	 * XDG_RUNTIME_DIR if it is set, or /run if the user is 'root', or
	 * ~/.cache/argeo if not.
	 */
	public static Path getRunDir() {
		Path runDir;
		String xdgRunDir = System.getenv("XDG_RUNTIME_DIR");
		if (xdgRunDir != null) {
			// TODO support multiple names
			runDir = Paths.get(xdgRunDir);
		} else {
			String username = System.getProperty("user.name");
			if (username.equals("root")) {
				runDir = Paths.get("/run");
			} else {
				Path homeDir = Paths.get(System.getProperty("user.home"));
				if (!Files.isWritable(homeDir)) {
					// typically, dameon's home (/usr/sbin) is not writable
					runDir = Paths.get("/tmp/" + username + "/run");
				} else {
					runDir = homeDir.resolve(".cache/argeo");
				}
			}
		}
		return runDir;
	}
}
