package org.argeo.cms.acr.fs;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

/** Describes what happendend during a sync operation. */
public class SyncResult<T> {
	private final Set<T> added = new TreeSet<>();
	private final Set<T> modified = new TreeSet<>();
	private final Set<T> deleted = new TreeSet<>();
	private final Set<Error> errors = new TreeSet<>();

	public Set<T> getAdded() {
		return added;
	}

	public Set<T> getModified() {
		return modified;
	}

	public Set<T> getDeleted() {
		return deleted;
	}

	public Set<Error> getErrors() {
		return errors;
	}

	public void addError(T sourcePath, T targetPath, Exception e) {
		Error error = new Error(sourcePath, targetPath, e);
		errors.add(error);
	}

	public boolean noModification() {
		return modified.isEmpty() && deleted.isEmpty() && added.isEmpty();
	}

	@Override
	public String toString() {
		if (noModification())
			return "No modification.";
		StringBuffer sb = new StringBuffer();
		for (T p : modified)
			sb.append("MOD ").append(p).append('\n');
		for (T p : deleted)
			sb.append("DEL ").append(p).append('\n');
		for (T p : added)
			sb.append("ADD ").append(p).append('\n');
		for (Error error : errors)
			sb.append(error).append('\n');
		return sb.toString();
	}

	public class Error implements Comparable<Error> {
		private final T sourcePath;// if null this is a failed delete
		private final T targetPath;
		private final Exception exception;
		private final Instant timestamp = Instant.now();

		public Error(T sourcePath, T targetPath, Exception e) {
			super();
			this.sourcePath = sourcePath;
			this.targetPath = targetPath;
			this.exception = e;
		}

		public T getSourcePath() {
			return sourcePath;
		}

		public T getTargetPath() {
			return targetPath;
		}

		public Exception getException() {
			return exception;
		}

		public Instant getTimestamp() {
			return timestamp;
		}

		@Override
		public int compareTo(Error o) {
			return timestamp.compareTo(o.timestamp);
		}

		@Override
		public int hashCode() {
			return timestamp.hashCode();
		}

		@Override
		public String toString() {
			return "ERR " + timestamp + (sourcePath == null ? "Deletion failed" : "Copy failed " + sourcePath) + " "
					+ targetPath + " " + exception.getMessage();
		}

	}
}
