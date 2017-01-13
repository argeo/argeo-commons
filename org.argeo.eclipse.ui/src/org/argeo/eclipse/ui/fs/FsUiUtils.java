package org.argeo.eclipse.ui.fs;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Centralize additionnal utilitary methods to manage Java7 NIO files */
public class FsUiUtils {

	/**
	 * thanks to
	 * http://programming.guide/java/formatting-byte-size-to-human-readable-format.html
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static Path[] getChildren(Path parent, String filter, boolean showHiddenItems, boolean folderFirst,
			String orderProperty, boolean reverseOrder) {
		if (!Files.isDirectory(parent))
			return null;
		List<Pair> pairs = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, filter)) {
			loop: for (Path entry : stream) {
				if (!showHiddenItems)
					if (Files.isHidden(entry))
						continue loop;
				switch (orderProperty) {
				case FsUiConstants.PROPERTY_SIZE:
					if (folderFirst)
						pairs.add(new LPair(entry, Files.size(entry), Files.isDirectory(entry)));
					else
						pairs.add(new LPair(entry, Files.size(entry)));
					break;
				case FsUiConstants.PROPERTY_LAST_MODIFIED:
					if (folderFirst)
						pairs.add(new LPair(entry, Files.getLastModifiedTime(entry).toMillis(),
								Files.isDirectory(entry)));
					else
						pairs.add(new LPair(entry, Files.getLastModifiedTime(entry).toMillis()));
					break;
				case FsUiConstants.PROPERTY_NAME:
					if (folderFirst)
						pairs.add(new SPair(entry, entry.getFileName().toString(), Files.isDirectory(entry)));
					else
						pairs.add(new SPair(entry, entry.getFileName().toString()));
					break;
				default:
					throw new FsUiException("Unable to prepare sort for property " + orderProperty);
				}
			}
			Pair[] rows = pairs.toArray(new Pair[0]);
			Arrays.sort(rows);
			Path[] results = new Path[rows.length];
			if (reverseOrder) {
				int j = rows.length - 1;
				for (int i = 0; i < rows.length; i++)
					results[i] = rows[j - i].p;
			} else
				for (int i = 0; i < rows.length; i++)
					results[i] = rows[i].p;
			return results;
		} catch (IOException | DirectoryIteratorException e) {
			throw new FsUiException("Unable to filter " + parent + " children with filter " + filter, e);
		}
	}

	static abstract class Pair implements Comparable<Object> {
		Path p;
		Boolean i;
	};

	static class LPair extends Pair {
		long v;

		public LPair(Path path, long propValue) {
			p = path;
			v = propValue;
		}

		public LPair(Path path, long propValue, boolean isDir) {
			p = path;
			v = propValue;
			i = isDir;
		}

		public int compareTo(Object o) {
			if (i != null) {
				Boolean j = ((LPair) o).i;
				if (i.booleanValue() != j.booleanValue())
					return i.booleanValue() ? -1 : 1;
			}
			long u = ((LPair) o).v;
			return v < u ? -1 : v == u ? 0 : 1;
		}
	};

	static class SPair extends Pair {
		String v;

		public SPair(Path path, String propValue) {
			p = path;
			v = propValue;
		}

		public SPair(Path path, String propValue, boolean isDir) {
			p = path;
			v = propValue;
			i = isDir;
		}

		public int compareTo(Object o) {
			if (i != null) {
				Boolean j = ((SPair) o).i;
				if (i.booleanValue() != j.booleanValue())
					return i.booleanValue() ? -1 : 1;
			}
			String u = ((SPair) o).v;
			return v.compareTo(u);
		}
	};
}
