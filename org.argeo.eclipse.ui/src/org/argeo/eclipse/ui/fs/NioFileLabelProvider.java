package org.argeo.eclipse.ui.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;

/** Expect a {@link Path} as input element */
public class NioFileLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = 2160026425187796930L;
	private final String propName;

	public NioFileLabelProvider(String propName) {
		this.propName = propName;
	}

	@Override
	public String getText(Object element) {
		Path path = (Path) element;
		try {
			switch (propName) {
			case FsUiConstants.PROPERTY_SIZE:
				return FsUiUtils.humanReadableByteCount(Files.size(path), false);
			case FsUiConstants.PROPERTY_LAST_MODIFIED:
				return Files.getLastModifiedTime(path).toString();
			case FsUiConstants.PROPERTY_TYPE:
				if (Files.isDirectory(path))
					return "Folder";
				else {
					String mimeType = Files.probeContentType(path);
					if (EclipseUiUtils.isEmpty(mimeType))
						return "Unknown";
					else
						return mimeType;
				}
			default:
				throw new IllegalArgumentException("Unsupported property " + propName);
			}
		} catch (IOException ioe) {
			throw new FsUiException("Cannot get property " + propName + " on " + path.toString());
		}
	}
}
