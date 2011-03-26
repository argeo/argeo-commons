package org.argeo;

/** The current operating system. */
public class OperatingSystem {
	public final static int NIX = 1;
	public final static int WINDOWS = 2;
	public final static int SOLARIS = 3;

	public final static int os;
	static {
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Win"))
			os = WINDOWS;
		else if (osName.startsWith("Solaris"))
			os = SOLARIS;
		else
			os = NIX;
	}

}
