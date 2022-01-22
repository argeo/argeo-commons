package org.argeo.api.acr;

/** When a feature is not supported by the underlying repository. */
public class ContentFeatureUnsupportedException extends UnsupportedOperationException {
	private static final long serialVersionUID = 3193936026343114949L;

	public ContentFeatureUnsupportedException() {
	}

	public ContentFeatureUnsupportedException(String message) {
		super(message);
	}

	public ContentFeatureUnsupportedException(Throwable cause) {
		super(cause);
	}

	public ContentFeatureUnsupportedException(String message, Throwable cause) {
		super(message, cause);
	}

}
