package org.argeo.api.acr;

/**
 * When a content was requested which does not exists, equivalent to HTTP code
 * 404.
 */
public class ContentNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -8629074900713760886L;

	private final String path;

	public ContentNotFoundException(ContentSession session, String path, Throwable cause) {
		super(message(session, path), cause);
		this.path = path;
		// we don't keep reference to the session for security reasons
	}

	public ContentNotFoundException(ContentSession session, String path) {
		this(session, path, (String) null);
	}

	public ContentNotFoundException(ContentSession session, String path, String message) {
		super(message != null ? message : message(session, path));
		this.path = path;
		// we don't keep reference to the session for security reasons
	}

	private static String message(ContentSession session, String path) {
		return "Content " + path + "cannot be found.";
	}

	public String getPath() {
		return path;
	}
}
