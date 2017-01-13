package org.argeo.eclipse.ui.fs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SimpleBrowserExample {

	private static String DUMMY_ABS_PATH = System.getProperty("user.home");
	private static String DUMMY_ABS_PATH2 = "/tmp";

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(EclipseUiUtils.noSpaceGridLayout());
		shell.setText("Simple File system browser Example");

		SimpleFsBrowser sfb = new SimpleFsBrowser(shell, SWT.NO_FOCUS);
		Path path = Paths.get(DUMMY_ABS_PATH);
		Path path2 = Paths.get(DUMMY_ABS_PATH2);
		sfb.setInput(path, path2);
		sfb.setLayoutData(EclipseUiUtils.fillAll());
		sfb.layout(true, true);

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
