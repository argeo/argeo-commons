package org.argeo.jcr;

import javax.jcr.Value;

/** The result of the comparison of two JCR properties. */
public class PropertyDiff {
	public final static Integer MODIFIED = 0;
	public final static Integer ADDED = 1;
	public final static Integer REMOVED = 2;

	private final Integer type;
	private final String relPath;
	private final Value referenceValue;
	private final Value newValue;

	public PropertyDiff(Integer type, String relPath, Value referenceValue,
			Value newValue) {
		super();

		if (type == MODIFIED) {
			if (referenceValue == null || newValue == null)
				throw new ArgeoJcrException(
						"Reference and new values must be specified.");
		} else if (type == ADDED) {
			if (referenceValue != null || newValue == null)
				throw new ArgeoJcrException(
						"New value and only it must be specified.");
		} else if (type == REMOVED) {
			if (referenceValue == null || newValue != null)
				throw new ArgeoJcrException(
						"Reference value and only it must be specified.");
		} else {
			throw new ArgeoJcrException("Unkown diff type " + type);
		}

		if (relPath == null)
			throw new ArgeoJcrException("Relative path must be specified");

		this.type = type;
		this.relPath = relPath;
		this.referenceValue = referenceValue;
		this.newValue = newValue;
	}

	public Integer getType() {
		return type;
	}

	public String getRelPath() {
		return relPath;
	}

	public Value getReferenceValue() {
		return referenceValue;
	}

	public Value getNewValue() {
		return newValue;
	}

}
