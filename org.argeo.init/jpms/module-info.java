module org.argeo.init {
	provides java.lang.System.LoggerFinder with org.argeo.init.logging.ThinLoggerFinder;

	// Thin logging fallback
	requires static java.logging;

	// JShell
	requires static jdk.jshell;

	provides jdk.jshell.spi.ExecutionControlProvider with org.argeo.init.jshell.DirectExecutionControlProvider;

	// OSGi
	requires static org.eclipse.osgi;

	uses org.osgi.framework.connect.ConnectFrameworkFactory;

	// SYSINIT
	// for signal handling
	requires static jdk.unsupported;
	// for uptime
	requires static java.management;

	// Preferences (experimental)
	requires static java.prefs;

	//provides java.util.prefs.PreferencesFactory with org.argeo.init.prefs.ThinPreferencesFactory;
}
