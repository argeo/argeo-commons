package org.argeo.init;
//#! /usr/bin/java --source 17 @/usr/local/etc/freed/pid1/jvm.args

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.Console;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import sun.misc.Signal;

/** A minimalistic Linux init process. */
public class SysInitMain {
	final static AtomicInteger runLevel = new AtomicInteger(-1);

	private static Logger logger;

	private final static List<String> initDServices = Collections.synchronizedList(new ArrayList<>());

	private static Process xServer;

	public static void main(String... args) {
		try {
			final long pid = ProcessHandle.current().pid();
			// TODO find reference for PID 1 signal handling
			Signal.handle(new Signal("TERM"), (signal) -> {
				System.out.println("SIGTERM caught, doing nothing");
				// TODO reload?
				//System.exit(0);
			});
			Signal.handle(new Signal("INT"), (signal) -> {
				System.out.println("SIGINT caught, rebooting");
				shutdown(true);
				System.exit(0);
			});
			Signal.handle(new Signal("PWR"), (signal) -> {
				System.out.println("SIGPWR caught, shutting down");
				shutdown(false);
				System.exit(0);
			});
			Signal.handle(new Signal("HUP"), (signal) -> {
				System.out.println("SIGHUP caught, doing nothing");
				//System.exit(0);
			});

			boolean isSystemInit = pid == 1 || pid == 2;

			if (isSystemInit && args.length > 0 && ("1".equals(args[0]) //
					|| "single".equals(args[0]) //
					|| "emergency".equals(args[0]))) {
				runLevel.set(1);
				for (Object key : new TreeMap<>(System.getProperties()).keySet()) {
					System.out.println(key + "=" + System.getProperty(key.toString()));
				}
				System.out.println("Single user mode");
				System.out.flush();
				singleUserShell();
			} else {
				if (args.length == 0)
					runLevel.set(5);
				else
					runLevel.set(Integer.parseInt(args[0]));

				if (runLevel.get() == 0) {// shutting down the whole system
					initLogger();
					if (!isSystemInit) {
						logger.log(INFO, "Shutting down system...");
						shutdown(false);
						System.exit(0);
					} else {
						logger.log(ERROR, "Cannot start at run level " + runLevel.get());
						System.exit(1);
					}
				} else if (runLevel.get() == 6) {// reboot the whole system
					initLogger();
					if (!isSystemInit) {
						logger.log(INFO, "Rebooting the system...");
						shutdown(true);
					} else {
						logger.log(ERROR, "Cannot start at run level " + runLevel.get());
						System.exit(1);
					}
				}

//				logger.log(DEBUG, () -> "FREEd Init daemon starting with pid " + pid + " after "
//						+ ManagementFactory.getRuntimeMXBean().getUptime() + " ms");

				mountRootRw();
				// Logging to file will not work until / has been remounted rw
				// mount file systems
				initLogger();

				// hostname
				Path hostnameF = Paths.get("/etc/hostname");
				if (Files.exists(hostnameF)) {
					String hostname = Files.readString(hostnameF);
					int exitCode = new ProcessBuilder("/bin/hostname", hostname).start().waitFor();
					if (exitCode == 0)
						logger.log(DEBUG, () -> "Set hostname to " + hostname);
				}

				initSysctl();

				// hardware
				startInitDService("udev");
				// TODO mount all asynchronously in order to deal with network fs
				mountAll();

				startInitDService("qemu-agent");
				startInitDService("dbus",false);// TODO dbus fails to stop

				// networking (asychronous)
//				new Thread(() -> {
				startInitDService("networking");
				if (!waitForNetwork(60 * 1000))
					logger.log(ERROR, "No network available");

				// OpenSSH
				// TODO make it consistent with Java sshd
				startInitDService("ssh");

				// Chrony (time service)
				startInitDService("chrony");

				// NSS services
				startInitDService("nslcd", false);// TODO nslcd fails to stop
//				}, "Start network services").start();

				// user interface
				startInitDService("console-setup.sh");
				startInitDService("keyboard-setup.sh");
				startInitDService("x11-common");

				// login prompt
				// ServiceMain.addPostStart(() -> new LoginThread().start());
				new LoginThread().start();

				// GUI
				Path startxF = Paths.get("/usr/bin/startx");
				if (Files.exists(startxF) && runLevel.get() == 5) {
					xServer = new ProcessBuilder(startxF.toString()).start();
					logger.log(INFO, "X server started");
				}

				// init Argeo CMS
				logger.log(INFO, "FREEd Init daemon starting Argeo Init after "
						+ ManagementFactory.getRuntimeMXBean().getUptime() + " ms");
				RuntimeManagerMain.main(args);
			}
		} catch (Throwable e) {
			if (logger != null)
				logger.log(ERROR, "Unexpected exception in free-pid1 init, shutting down... ", e);
			else
				e.printStackTrace();
			singleUserShell();
			System.exit(1);
		} finally {
			stopInitDServices();
		}

		// TODO improve shutdown integration with Java
		// shutdown gracefully as we have reached this stage via Java/OSGi closing down
		shutdown(false);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// silent
		}
	}

	static void mountRootRw() {
		try {
			// fsck if needed
			// FIXME check why fsck -A makes the kernel crash
//			Path forceFsck = Paths.get("/forcefsck");
//			if (Files.exists(forceFsck)) {
//				Process fsck = new ProcessBuilder("/sbin/fsck", "-A").start();
//				logger.log(Level.INFO, "Start file system check...");
//				int exitCode = fsck.waitFor();
//				if (exitCode != 0)
//					throw new IllegalStateException("fsck failed");
//			}

			{// mount root FS read-write
				Process mountRootRw = new ProcessBuilder("/bin/mount", "-o", "rw,remount", "/").start();
				int exitCode = mountRootRw.waitFor();
				if (exitCode != 0)
					throw new IllegalStateException("Cannot remount root filesystem read-write");
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot mount file systems", e);
		} catch (InterruptedException e) {
			System.err.println("Mounting root file system read-write was interrupted");
		}
	}

	static void initLogger() {
		logger = System.getLogger(SysInitMain.class.getName());
	}

	static void singleUserShell() {
		ProcessBuilder pb = new ProcessBuilder("/bin/bash");
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
		try {
			Process singleUserShell = pb.start();
			singleUserShell.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void mountAll() {
		try {
			Process mountAll = new ProcessBuilder("/bin/mount", "-a").start();
			int exitCode = mountAll.waitFor();
			if (exitCode != 0)
				logger.log(Level.ERROR, "Cannot mount file systems");
			logger.log(Level.INFO, "File systems mounted");
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot mount file systems", e);
		} catch (InterruptedException e) {
			logger.log(Level.ERROR, "Mounting file systems was interrupted");
		}
	}

	static void initSysctl() {
		try {
			Path sysctlD = Paths.get("/etc/sysctl.d/");
			for (Path conf : Files.newDirectoryStream(sysctlD, "*.conf")) {
				try {
					new ProcessBuilder("/sbin/sysctl", "-p", conf.toString()).start();
				} catch (IOException e) {
					logger.log(Level.ERROR, "Cannot load sysctl " + conf);
				}
			}
		} catch (IOException e) {
			logger.log(Level.ERROR, "Cannot load sysctl");
		}
	}

	static void startInitDService(String serviceName) {
		startInitDService(serviceName, true);
	}

	static void startInitDService(String serviceName, boolean stopOnShutdown) {
		Path serviceInit = Paths.get("/usr/local/etc/init.d/", serviceName);
		if (!Files.exists(serviceInit))
			serviceInit = Paths.get("/etc/init.d/", serviceName);
		if (Files.exists(serviceInit))
			try {
				int exitCode = new ProcessBuilder(serviceInit.toString(), "start").start().waitFor();
				if (exitCode != 0)
					logger.log(ERROR, "Service " + serviceName + " dit not stop properly");
				else
					logger.log(INFO, "Service " + serviceName + " started");
				if (stopOnShutdown)
					initDServices.add(serviceName);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		else
			logger.log(WARNING, "Service " + serviceName + " not found and therefore not started");
	}

	static boolean waitForNetwork(long timeout) {
		long begin = System.currentTimeMillis();
		long duration = 0;
		boolean networkAvailable = false;
		try {
			networkAvailable: while (!networkAvailable) {
				duration = System.currentTimeMillis() - begin;
				if (duration > timeout)
					break networkAvailable;
				Enumeration<NetworkInterface> netInterfaces = null;
				try {
					netInterfaces = NetworkInterface.getNetworkInterfaces();
				} catch (SocketException e) {
					throw new IllegalStateException("Cannot list network interfaces", e);
				}
				if (netInterfaces != null) {
					while (netInterfaces.hasMoreElements()) {
						NetworkInterface netInterface = netInterfaces.nextElement();
						logger.log(DEBUG, "Interface:" + netInterface);
						for (InterfaceAddress addr : netInterface.getInterfaceAddresses()) {
							InetAddress inetAddr = addr.getAddress();
							logger.log(DEBUG, "  addr: " + inetAddr);
							if (!inetAddr.isLoopbackAddress() && !inetAddr.isLinkLocalAddress()) {
								try {
									if (inetAddr.isReachable((int) timeout)) {
										networkAvailable = true;
										duration = System.currentTimeMillis() - begin;
										logger.log(DEBUG,
												"Network available after " + duration + " ms. IP: " + inetAddr);
										break networkAvailable;
									}
								} catch (IOException e) {
									logger.log(ERROR, "Cannot check whether " + inetAddr + " is reachable", e);
								}
							}
						}
					}
				} else {
					throw new IllegalStateException("No network interface has been found");
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// silent
				}
			}
		} catch (Exception e) {
			logger.log(ERROR, "Cannot check whether network is available", e);
		}
		return networkAvailable;
	}

	static void shutdown(boolean reboot) {
		try {
			stopInitDServices();
			Path sysrqP = Paths.get("/proc/sys/kernel/sysrq");
			String current = Files.readString(sysrqP);
			if ("1".equals(current))
				return;// already shutting down
			Files.writeString(sysrqP, "1");
			Path sysrqTriggerP = Paths.get("/proc/sysrq-trigger");
			Files.writeString(sysrqTriggerP, "e");// send SIGTERM to all processes
			// TODO check processes effectively with ProcessHandle.of(1)
			try {
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				// silent
			}
			// Files.writeString(sysrqTriggerP, "i");// send SIGKILL to all processes
			Files.writeString(sysrqTriggerP, "s");// flush data to disk
			Files.writeString(sysrqTriggerP, "u");// unmount
			if (reboot)
				Files.writeString(sysrqTriggerP, "b");
			else {
				Files.writeString(sysrqTriggerP, "o");
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {
					// silent
				}
			}
		} catch (IOException e) {
			logger.log(ERROR, "Cannot shut down system", e);
		}
	}

	static void stopInitDServices() {
		for (int i = initDServices.size() - 1; i >= 0; i--) {
			String serviceName = initDServices.get(i);
			Path serviceInit = Paths.get("/usr/local/etc/init.d/", serviceName);
			if (!Files.exists(serviceInit))
				serviceInit = Paths.get("/etc/init.d/", serviceName);
			try {
				int exitCode = new ProcessBuilder(serviceInit.toString(), "stop").start().waitFor();
				if (exitCode != 0)
					logger.log(ERROR, "Service " + serviceName + " did not stop properly");
			} catch (InterruptedException | IOException e) {
				logger.log(ERROR, "Cannot stop service " + serviceName, e);
			}
		}
	}

	/** A thread watching the login prompt. */
	static class LoginThread extends Thread {
		private boolean systemShuttingDown = false;
		private Process process = null;

		public LoginThread() {
			super("FREEd login prompt");
			setDaemon(true);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				systemShuttingDown = true;
				if (process != null)
					process.destroy();
			}));
		}

		@Override
		public void run() {
			boolean getty = true;
			prompt: while (!systemShuttingDown) {
				try {
					if (getty) {
						ProcessBuilder pb = new ProcessBuilder("/usr/sbin/getty", "38400", "tty2");
						process = pb.start();
					} else {
						Console console = System.console();
						console.readLine(); // type return once to activate login prompt
						console.printf("login: ");
						String username = console.readLine();
						username = username.trim();
						if ("".equals(username))
							continue prompt;
						ProcessBuilder pb = new ProcessBuilder("su", "--login", username);
						pb.redirectError(ProcessBuilder.Redirect.INHERIT);
						pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
						pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
						process = pb.start();
					}
					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
						if (process != null)
							process.destroy();
					}));
					try {
						process.waitFor();
					} catch (InterruptedException e) {
						process.destroy();
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					process = null;
				}
			}
		}

	}
}
