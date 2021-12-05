package org.argeo.cms.internal.kernel;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.stats.RepositoryStatistics;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;
import org.argeo.cms.internal.auth.CmsSessionImpl;

/**
 * Background thread started by the kernel, which gather statistics and
 * monitor/control other processes.
 */
class KernelThread extends Thread {
	private final static Log log = LogFactory.getLog(KernelThread.class);

	private RepositoryStatisticsImpl repoStats;

	/** The smallest period of operation, in ms */
	private final long PERIOD = 60 * 1000l;
	/** One ms in ns */
	private final static long m = 1000l * 1000l;
	private final static long M = 1024l * 1024l;

	private boolean running = true;

	private Log kernelStatsLog = LogFactory.getLog("argeo.stats.kernel");
	private Log nodeStatsLog = LogFactory.getLog("argeo.stats.node");

	@SuppressWarnings("unused")
	private long cycle = 0l;

	public KernelThread(ThreadGroup threadGroup, String name) {
		super(threadGroup, name);
	}

	private void doSmallestPeriod() {
		// Clean expired sessions
		CmsSessionImpl.closeInvalidSessions();

		if (kernelStatsLog.isDebugEnabled()) {
			StringBuilder line = new StringBuilder(64);
			line.append("§\t");
			long freeMem = Runtime.getRuntime().freeMemory() / M;
			long totalMem = Runtime.getRuntime().totalMemory() / M;
			long maxMem = Runtime.getRuntime().maxMemory() / M;
			double loadAvg = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
			// in min
			boolean min = true;
			long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / (1000 * 60);
			if (uptime > 24 * 60) {
				min = false;
				uptime = uptime / 60;
			}
			line.append(uptime).append(min ? " min" : " h").append('\t');
			line.append(loadAvg).append('\t').append(maxMem).append('\t').append(totalMem).append('\t').append(freeMem)
					.append('\t');
			kernelStatsLog.debug(line);
		}

		if (nodeStatsLog.isDebugEnabled()) {
			File dataDir = KernelUtils.getOsgiInstanceDir();
			long freeSpace = dataDir.getUsableSpace() / M;
			// File currentRoot = null;
			// for (File root : File.listRoots()) {
			// String rootPath = root.getAbsolutePath();
			// if (dataDir.getAbsolutePath().startsWith(rootPath)) {
			// if (currentRoot == null
			// || (rootPath.length() > currentRoot.getPath()
			// .length())) {
			// currentRoot = root;
			// }
			// }
			// }
			// long totalSpace = currentRoot.getTotalSpace();
			StringBuilder line = new StringBuilder(128);
			line.append("§\t").append(freeSpace).append(" MB left in " + dataDir);
			line.append('\n');
			if (repoStats != null)
				for (RepositoryStatistics.Type type : RepositoryStatistics.Type.values()) {
					long[] vals = repoStats.getTimeSeries(type).getValuePerMinute();
					long val = vals[vals.length - 1];
					line.append(type.name()).append('\t').append(val).append('\n');
				}
			nodeStatsLog.debug(line);
		}
	}

	@Override
	public void run() {
		if (log.isTraceEnabled())
			log.trace("Kernel thread started.");
		final long periodNs = PERIOD * m;
		while (running) {
			long beginNs = System.nanoTime();
			doSmallestPeriod();

			long waitNs = periodNs - (System.nanoTime() - beginNs);
			if (waitNs < 0)
				continue;
			// wait
			try {
				sleep(waitNs / m, (int) (waitNs % m));
			} catch (InterruptedException e) {
				// silent
			}
			cycle++;
		}
	}

	synchronized void destroyAndJoin() {
		running = false;
		notifyAll();
//		interrupt();
//		try {
//			join(PERIOD * 2);
//		} catch (InterruptedException e) {
//			// throw new CmsException("Kernel thread destruction was interrupted");
//			log.error("Kernel thread destruction was interrupted", e);
//		}
	}
}
