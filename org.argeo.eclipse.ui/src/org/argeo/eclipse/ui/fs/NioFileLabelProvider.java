package org.argeo.eclipse.ui.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;

/** Expect a {@link Path} as input element */
public class NioFileLabelProvider extends ColumnLabelProvider {
	private final static FileTime EPOCH = FileTime.fromMillis(0);
	private static final long serialVersionUID = 2160026425187796930L;
	private final String propName;
	private DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd hh:mm");

	// TODO use new formatting
	// DateTimeFormatter formatter =
	// DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
	// .withLocale( Locale.UK )
	// .withZone( ZoneId.systemDefault() );
	public NioFileLabelProvider(String propName) {
		this.propName = propName;
	}

	@Override
	public String getText(Object element) {
		try {
			Path path;
			if (element instanceof ParentDir) {
//				switch (propName) {
//				case FsUiConstants.PROPERTY_SIZE:
//					return "-";
//				case FsUiConstants.PROPERTY_LAST_MODIFIED:
//					return "-";
//				// return Files.getLastModifiedTime(((ParentDir) element).getPath()).toString();
//				case FsUiConstants.PROPERTY_TYPE:
//					return "Folder";
//				}
				path = ((ParentDir) element).getPath();
			} else
				path = (Path) element;
			switch (propName) {
			case FsUiConstants.PROPERTY_SIZE:
				if (Files.isDirectory(path))
					return "-";
				else
					return FsUiUtils.humanReadableByteCount(Files.size(path), false);
			case FsUiConstants.PROPERTY_LAST_MODIFIED:
				if (Files.isDirectory(path))
					return "-";
				FileTime time = Files.getLastModifiedTime(path);
				if (time.equals(EPOCH))
					return "-";
				else
					return dateFormat.format(new Date(time.toMillis()));
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
			throw new FsUiException("Cannot get property " + propName + " on " + element);
		}
	}
}
