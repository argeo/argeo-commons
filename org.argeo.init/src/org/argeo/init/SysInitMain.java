package org.argeo.init;
//#! /usr/bin/java --source 17 @/usr/local/etc/freed/pid1/jvm.args

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.Console;
import java.io.IOException;
import java.lang.System.Logger;
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
class SysInitMain {
	final static AtomicInteger runLevel = new AtomicInteger(-1);

	private final static Logger logger = System.getLogger(SysInitMain.class.getName());

	private final static List<String> initDServices = Collections.synchronizedList(new ArrayList<>());

	public static void main(String... args) {
		try {
			final long pid = ProcessHandle.current().pid();
			Signal.handle(new Signal("TERM"), (signal) -> {
				System.out.println("SIGTERM caught");
				System.exit(0);
			});
			Signal.handle(new Signal("INT"), (signal) -> {
				System.out.println("SIGINT caught");
				System.exit(0);
			});
			Signal.handle(new Signal("HUP"), (signal) -> {
				System.out.println("SIGHUP caught");
				System.exit(0);
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
				ProcessBuilder pb = new ProcessBuilder("/bin/bash");
				pb.redirectError(ProcessBuilder.Redirect.INHERIT);
				pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
				pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
				Process singleUserShell = pb.start();
				singleUserShell.waitFor();
			} else {
				if (args.length == 0)
					runLevel.set(5);
				else
					runLevel.set(Integer.parseInt(args[0]));

				if (runLevel.get() == 0) {// shutting down the whole system
					if (!isSystemInit) {
						logger.log(INFO, "Shutting down system...");
						shutdown(false);
						System.exit(0);
					} else {
						logger.log(ERROR, "Cannot start at run level " + runLevel.get());
						System.exit(1);
					}
				} else if (runLevel.get() == 6) {// reboot the whole system
					if (!isSystemInit) {
						logger.log(INFO, "Rebooting the system...");
						shutdown(true);
					} else {
						logger.log(ERROR, "Cannot start at run level " + runLevel.get());
						System.exit(1);
					}
				}

				logger.log(INFO, "FREEd Init daemon starting with pid " + pid + " after "
						+ ManagementFactory.getRuntimeMXBean().getUptime() + " ms");
				// hostname
				String hostname = Files.readString(Paths.get("/etc/hostname"));
				new ProcessBuilder("/usr/bin/hostname", hostname).start();
				logger.log(DEBUG, "Set hostname to " + hostname);
				// networking
				initSysctl();
				startInitDService("networking", true);
//				Thread.sleep(3000);// leave some time for network to start up
				if (!waitForNetwork(10 * 1000))
					logger.log(ERROR, "No network available");

				// OpenSSH
				// TODO make it coherent with Java sshd
				startInitDService("ssh", true);

				// NSS services
				startInitDService("nslcd", false);// Note: nslcd fails to stop

				// login prompt
				ServiceMain.addPostStart(() -> new LoginThread().start());

				// init Argeo CMS
				logger.log(INFO, "FREEd Init daemon starting Argeo Init after "
						+ ManagementFactory.getRuntimeMXBean().getUptime() + " ms");
				ServiceMain.main(args);
			}
		} catch (Throwable e) {
			logger.log(ERROR, "Unexpected exception in free-pid1 init, shutting down... ", e);
			System.exit(1);
		} finally {
			stopInitDServices();
		}
	}

	static void initSysctl() {
		try {
			Path sysctlD = Paths.get("/etc/sysctl.d/");
			for (Path conf : Files.newDirectoryStream(sysctlD, "*.conf")) {
				try {
					new ProcessBuilder("/usr/sbin/sysctl", "-p", conf.toString()).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void startInitDService(String serviceName, boolean stopOnShutdown) {
		Path serviceInit = Paths.get("/etc/init.d/", serviceName);
		if (Files.exists(serviceInit))
			try {
				int exitCode = new ProcessBuilder(serviceInit.toString(), "start").start().waitFor();
				if (exitCode != 0)
					logger.log(ERROR, "Service " + serviceName + " dit not stop properly");
				else
					logger.log(DEBUG, "Service " + serviceName + " started");
				if (stopOnShutdown)
					initDServices.add(serviceName);
//					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//						try {
//							new ProcessBuilder(serviceInit.toString(), "stop").start().waitFor();
//						} catch (IOException | InterruptedException e) {
//							e.printStackTrace();
//						}
//					}, "FREEd stop service " + serviceName));
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
			Files.writeString(sysrqP, "1");
			Path sysrqTriggerP = Paths.get("/proc/sysrq-trigger");
			Files.writeString(sysrqTriggerP, "e");// send SIGTERM to all processes
			// Files.writeString(sysrqTriggerP, "i");// send SIGKILL to all processes
			Files.writeString(sysrqTriggerP, "e");// flush data to disk
			Files.writeString(sysrqTriggerP, "u");// unmount
			if (reboot)
				Files.writeString(sysrqTriggerP, "b");
			else
				Files.writeString(sysrqTriggerP, "o");
		} catch (IOException e) {
			logger.log(ERROR, "Cannot shut down system", e);
		}
	}

	static void stopInitDServices() {
		for (int i = initDServices.size() - 1; i >= 0; i--) {
			String serviceName = initDServices.get(i);
			Path serviceInit = Paths.get("/etc/init.d/", serviceName);
			try {
				int exitCode = new ProcessBuilder(serviceInit.toString(), "stop").start().waitFor();
				if (exitCode != 0)
					logger.log(ERROR, "Service " + serviceName + " dit not stop properly");
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
					Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroy()));
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
