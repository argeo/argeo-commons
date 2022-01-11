package org.argeo.cms.internal.osgi;

import java.io.IOException;

/**
 * Workaround for killing Gogo shell by system shutdown.
 * 
 * @see https://issues.apache.org/jira/browse/FELIX-4208
 */
class GogoShellKiller extends Thread {

	public GogoShellKiller() {
		super("Gogo Shell Killer");
		setDaemon(true);
	}

	@Override
	public void run() {
		ThreadGroup rootTg = getRootThreadGroup(null);
		Thread gogoShellThread = findGogoShellThread(rootTg);
		if (gogoShellThread == null) // no need to bother if it is not here
			return;
		while (getNonDaemonCount(rootTg) > 2) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// silent
			}
		}
		gogoShellThread = findGogoShellThread(rootTg);
		if (gogoShellThread == null)
			return;
		System.exit(0);
		// No non-deamon threads left, forcibly halting the VM
		//Runtime.getRuntime().halt(0);
	}

	private ThreadGroup getRootThreadGroup(ThreadGroup tg) {
		if (tg == null)
			tg = Thread.currentThread().getThreadGroup();
		if (tg.getParent() == null)
			return tg;
		else
			return getRootThreadGroup(tg.getParent());
	}

	private int getNonDaemonCount(ThreadGroup rootThreadGroup) {
		Thread[] threads = new Thread[rootThreadGroup.activeCount()];
		rootThreadGroup.enumerate(threads);
		int nonDameonCount = 0;
		for (Thread t : threads)
			if (t != null && !t.isDaemon())
				nonDameonCount++;
		return nonDameonCount;
	}

	private Thread findGogoShellThread(ThreadGroup rootThreadGroup) {
		Thread[] threads = new Thread[rootThreadGroup.activeCount()];
		rootThreadGroup.enumerate(threads, true);
		for (Thread thread : threads) {
			if (thread.getName().equals("pipe-gosh --login --noshutdown"))
				return thread;
		}
		return null;
	}

}