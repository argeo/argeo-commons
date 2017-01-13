package org.argeo.eclipse.ui.fs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class AdvancedBrowserExample {

	private static String DUMMY_ABS_PATH = System.getProperty("user.home");
	private static String DUMMY_ABS_PATH2 = "/tmp";

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(EclipseUiUtils.noSpaceGridLayout());
		shell.setText("Simple Browser Example");

		AdvancedFsBrowser sftb = new AdvancedFsBrowser();
		Path path = Paths.get(DUMMY_ABS_PATH);
		Control body = sftb.createUi(shell, path);
		body.setLayoutData(EclipseUiUtils.fillAll());
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
