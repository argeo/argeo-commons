package org.argeo.cms.http;

import java.nio.file.Files;
import java.util.function.Supplier;

/**
 * Common media types used in HTTP applications. It can also be referenced for
 * files-based applications but
 * {@link Files#probeContentType(java.nio.file.Path)} should be considered in
 * such cases.
 * 
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTTP/MIME_types/Common_types">MDN
 *      Web Docs</a>
 */
public enum CommonMediaType implements Supplier<String> {
	// text
	TEXT_PLAIN("text/plain", true, "txt"), //
	TEXT_CSV("text/csv", true, "csv"), //
	// application
	APPLICATION_XML("application/xml", true, "xml"), //
	APPLICATION_JSON("application/json", true, "json"), //
	APPLICATION_ZIP("application/zip", false, "zip"), //
	APPLICATION_PDF("application/pdf", false, "pdf"), //
	// image
	IMAGE_JPEG("image/jpeg", false, "jpeg", "jpg"), //
	IMAGE_PNG("image/png", false, "png"), //
	// multipart
	MULTIPART_FORM_DATA("multipart/form-data", false), //
	;

	private final String type;
	private final boolean textBased;
	/** Default extension must be first. */
	private final String[] extensions;

	CommonMediaType(String type, boolean textBased, String... extensions) {
		this.type = type;
		this.textBased = textBased;
		this.extensions = extensions;
	}

	@Override
	public String get() {
		return type;
	}

	/** @deprecated Use {@link #get()}. */
	@Deprecated
	public String getType() {
		return type;
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getDefaultExtension() {
		if (extensions.length > 0)
			return extensions[0];
		else
			return null;
	}

//	@Deprecated
//	public String toHttpContentType(Charset charset) {
//		if (charset == null)
//			return type;
//		return type + "; charset=" + charset.name();
//	}
//
//	@Deprecated
//	public String toHttpContentType() {
//		if (type.startsWith("text/")) {
//			return toHttpContentType(StandardCharsets.UTF_8);
//		} else {
//			return type;
//		}
//	}

	public boolean isTextBased() {
		return textBased;
	}

	/*
	 * STATIC UTILITIES
	 */

	public static CommonMediaType find(String mediaType) {
		for (CommonMediaType entityMimeType : values()) {
			if (entityMimeType.type.equals(mediaType))
				return entityMimeType;
		}
		return null;
	}

	@Override
	public String toString() {
		return type;
	}

}
