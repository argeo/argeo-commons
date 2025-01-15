package org.argeo.api.cli;

/**
 * Registered to a {@link NativeExec} in order to be notified of new lines of
 * its standard or error outputs.
 */
public interface NativeExecOutputListener {
	void newLine(NativeExec systemCall, String line, boolean isError);
}
