package org.eclipse.rap.rwt.application;

import java.util.Map;

import org.eclipse.rap.rwt.service.ResourceLoader;

public interface Application {
	public static enum OperationMode {
		JEE_COMPATIBILITY, SWT_COMPATIBILITY,
	}

	void setOperationMode(OperationMode operationMode);

	void addResource(String name, ResourceLoader resourceLoader);

	void setExceptionHandler(ExceptionHandler exceptionHandler);

	void addEntryPoint(String path, EntryPointFactory entryPointFactory,
			Map<String, String> properties);

	void addEntryPoint(String path, Class<? extends EntryPoint> entryPoint,
			Map<String, String> properties);

	void addStyleSheet(String themeId, String styleSheetLocation,
			ResourceLoader resourceLoader);

}
