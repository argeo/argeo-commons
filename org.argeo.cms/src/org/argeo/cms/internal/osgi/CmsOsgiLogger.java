package org.argeo.cms.internal.osgi;

import java.security.SignatureException;
import java.util.Enumeration;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.runtime.DirectoryConf;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/** Logs OSGi events. */
public class CmsOsgiLogger implements LogListener {
	private final static String WHITEBOARD_PATTERN_PROP = "osgi.http.whiteboard.servlet.pattern";
	private final static String CONTEXT_NAME_PROP = "contextName";

	private LogReaderService logReaderService;

	public void start() {
		if (logReaderService != null) {
			Enumeration<LogEntry> logEntries = logReaderService.getLog();
			while (logEntries.hasMoreElements())
				logged(logEntries.nextElement());
			logReaderService.addLogListener(this);
		}
	}

	public void stop() throws Exception {
		logReaderService.removeLogListener(this);
	}

	public String toString() {
		return "Node Logger";
	}

	//
	// OSGi LOGGER
	//
	@Override
	public void logged(LogEntry status) {
		String loggerName = status.getBundle().getSymbolicName();
		if (loggerName == null)
			loggerName = "org.argeo.ext.osgi";
		CmsLog pluginLog = CmsLog.getLog(loggerName);
		LogLevel severity = status.getLogLevel();
		if (severity.equals(LogLevel.ERROR) && pluginLog.isErrorEnabled()) {
			// FIXME Fix Argeo TP
			if (status.getException() instanceof SignatureException)
				return;
			pluginLog.error(msg(status), status.getException());
		} else if (severity.equals(LogLevel.WARN) && pluginLog.isWarnEnabled()) {
			if ("org.apache.felix.scr".equals(status.getBundle().getSymbolicName())
					&& (status.getException() != null && status.getException() instanceof InterruptedException)) {
				// do not print stacktraces by Felix SCR shutdown
				pluginLog.warn(msg(status));
			} else {
				pluginLog.warn(msg(status), status.getException());
			}
		} else if (severity.equals(LogLevel.INFO) && pluginLog.isDebugEnabled())
			pluginLog.debug(msg(status), status.getException());
		else if (severity.equals(LogLevel.DEBUG) && pluginLog.isTraceEnabled())
			pluginLog.trace(msg(status), status.getException());
		else if (severity.equals(LogLevel.TRACE) && pluginLog.isTraceEnabled())
			pluginLog.trace(msg(status), status.getException());
	}

	private String msg(LogEntry status) {
		StringBuilder sb = new StringBuilder();
		sb.append(status.getMessage());
		Bundle bundle = status.getBundle();
		if (bundle != null) {
			sb.append(" '" + bundle.getSymbolicName() + "'");
		}
		ServiceReference<?> sr = status.getServiceReference();
		if (sr != null) {
			sb.append(' ');
			String[] objectClasses = (String[]) sr.getProperty(Constants.OBJECTCLASS);
			if (isSpringApplicationContext(objectClasses)) {
				sb.append("{org.springframework.context.ApplicationContext}");
				Object symbolicName = sr.getProperty(Constants.BUNDLE_SYMBOLICNAME);
				if (symbolicName != null)
					sb.append(" " + Constants.BUNDLE_SYMBOLICNAME + ": " + symbolicName);
			} else {
				sb.append(arrayToString(objectClasses));
			}
			Object cn = sr.getProperty(CmsConstants.CN);
			if (cn != null)
				sb.append(" " + CmsConstants.CN + ": " + cn);
//			Object factoryPid = sr.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
//			if (factoryPid != null)
//				sb.append(" " + ConfigurationAdmin.SERVICE_FACTORYPID + ": " + factoryPid);
			// else {
			// Object servicePid = sr.getProperty(Constants.SERVICE_PID);
			// if (servicePid != null)
			// sb.append(" " + Constants.SERVICE_PID + ": " + servicePid);
			// }
			// servlets
			Object whiteBoardPattern = sr.getProperty(WHITEBOARD_PATTERN_PROP);
			if (whiteBoardPattern != null) {
				if (whiteBoardPattern instanceof String) {
					sb.append(" " + WHITEBOARD_PATTERN_PROP + ": " + whiteBoardPattern);
				} else {
					sb.append(" " + WHITEBOARD_PATTERN_PROP + ": " + arrayToString((String[]) whiteBoardPattern));
				}
			}
			// RWT
			Object contextName = sr.getProperty(CONTEXT_NAME_PROP);
			if (contextName != null)
				sb.append(" " + CONTEXT_NAME_PROP + ": " + contextName);

			// user directories
			Object baseDn = sr.getProperty(DirectoryConf.baseDn.name());
			if (baseDn != null)
				sb.append(" " + DirectoryConf.baseDn.name() + ": " + baseDn);

		}
		return sb.toString();
	}

	private String arrayToString(Object[] arr) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < arr.length; i++) {
			if (i != 0)
				sb.append(',');
			sb.append(arr[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private boolean isSpringApplicationContext(String[] objectClasses) {
		for (String clss : objectClasses) {
			if (clss.equals("org.eclipse.gemini.blueprint.context.DelegatedExecutionOsgiBundleApplicationContext")) {
				return true;
			}
		}
		return false;
	}

	public void setLogReaderService(LogReaderService logReaderService) {
		this.logReaderService = logReaderService;
	}

}
