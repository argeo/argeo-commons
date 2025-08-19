module org.argeo.init {
	requires java.prefs;
	requires java.logging;
	requires java.management;

	requires jdk.jshell;
	requires jdk.unsupported;
	
	requires static org.eclipse.osgi;
}
